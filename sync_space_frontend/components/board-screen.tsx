"use client";

import {
  DndContext,
  DragOverlay,
  PointerSensor,
  closestCorners,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import { SortableContext, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { useShell } from "@/components/app-shell";
import {
  addTaskComment,
  apiMessage,
  createColumn,
  createTask,
  deleteColumn,
  deleteSpace,
  deleteTask,
  getBoard,
  getTaskActivity,
  moveTask,
  removeMember,
  renameColumn,
  renameSpace,
  reorderColumns,
  updateTask,
} from "@/lib/api";
import { connectBoardSocket } from "@/lib/board-socket";
import type { Assignee, Board, Column, Member, Task, TaskActivity, TaskLabel, TaskPriority } from "@/lib/types";

export function BoardScreen({ spaceId }: { spaceId: string }) {
  const router = useRouter();
  const { user, refreshSpaces } = useShell();
  const [board, setBoard] = useState<Board | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState(false);
  const [editing, setEditing] = useState<Task | null>(null);
  const [dragging, setDragging] = useState<Task | null>(null);
  const [search, setSearch] = useState("");
  const [priorities, setPriorities] = useState<TaskPriority[]>([]);
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }));

  function applyBoard(next: Board) {
    setBoard((current) => (!current || next.revision >= current.revision ? next : current));
  }

  useEffect(() => {
    let cancelled = false;
    getBoard(spaceId)
      .then((next) => {
        if (!cancelled) {
          setBoard(next);
          setError(null);
        }
      })
      .catch((caught) => {
        if (!cancelled) {
          setError(apiMessage(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [spaceId]);

  useEffect(() => {
    return connectBoardSocket(
      spaceId,
      (event) => {
        if (event.type === "SPACE_DELETED") {
          void refreshSpaces();
          router.replace("/spaces");
          return;
        }
        if (event.type === "MEMBERS_CHANGED" || event.type === "SPACE_UPDATED") {
          void refreshSpaces();
        }
        if (event.type === "MEMBERS_CHANGED") {
          const stillMember = event.board.members.some((member) => member.userId === user.id);
          if (!stillMember) {
            void refreshSpaces();
            router.replace("/spaces");
            return;
          }
        }
        setBoard((current) => {
          if (!current || event.board.revision < current.revision) {
            return current;
          }
          return { ...event.board, role: current.role };
        });
      },
      setLive,
    );
  }, [spaceId, user.id, refreshSpaces, router]);

  useEffect(() => {
    if (!editing || !board) {
      return;
    }
    const exists = board.columns.some((column) => column.tasks.some((task) => task.id === editing.id));
    if (!exists) {
      setEditing(null);
    }
  }, [board, editing]);

  useEffect(() => {
    const onMembersChanged = (event: Event) => {
      const changedSpaceId = (event as CustomEvent<string>).detail;
      if (changedSpaceId !== spaceId) {
        return;
      }
      getBoard(spaceId)
        .then((next) => {
          setBoard(next);
          setError(null);
        })
        .catch((caught) => setError(apiMessage(caught)));
    };
    window.addEventListener("sync-space-members-changed", onMembersChanged);
    return () => window.removeEventListener("sync-space-members-changed", onMembersChanged);
  }, [spaceId]);

  async function run(action: () => Promise<Board>) {
    setError(null);
    try {
      applyBoard(await action());
    } catch (caught) {
      setError(apiMessage(caught));
    }
  }

  async function onDragEnd(event: DragEndEvent) {
    setDragging(null);
    if (!board || !event.over) {
      return;
    }
    const taskId = String(event.active.id);
    const from = board.columns.find((column) => column.tasks.some((task) => task.id === taskId));
    const to = findColumn(board, String(event.over.id));
    if (!from || !to) {
      return;
    }
    const fromIndex = from.tasks.findIndex((task) => task.id === taskId);
    const toIndex = String(event.over.id).startsWith("column:")
      ? from.id === to.id
        ? Math.max(from.tasks.length - 1, 0)
        : to.tasks.length
      : to.tasks.findIndex((task) => task.id === String(event.over?.id));
    if (toIndex < 0 || (from.id === to.id && fromIndex === toIndex)) {
      return;
    }
    const previous = board;
    setBoard(moveLocal(board, taskId, to.id, toIndex));
    try {
      const next = await moveTask(spaceId, taskId, { toColumnId: to.id, toIndex });
      applyBoard(next);
    } catch (caught) {
      setError(apiMessage(caught));
      try {
        setBoard(await getBoard(spaceId));
      } catch {
        setBoard(previous);
      }
    }
  }

  function onDragStart(event: DragStartEvent) {
    const task = board?.columns.flatMap((column) => column.tasks).find((item) => item.id === event.active.id);
    setDragging(task ?? null);
  }

  if (!board && !error) {
    return <p className="p-8 text-muted">Loading board…</p>;
  }
  const openTask = editing
    ? board?.columns.flatMap((column) => column.tasks).find((task) => task.id === editing.id)
    : undefined;

  if (!board) {
    return (
      <div className="p-8">
        <p className="text-red-700">{error}</p>
        <button
          type="button"
          className="mt-4 rounded-xl bg-accent px-4 py-2 text-sm font-medium text-accent-ink"
          onClick={() => {
            setError(null);
            getBoard(spaceId)
              .then((next) => {
                setBoard(next);
                setError(null);
              })
              .catch((caught) => setError(apiMessage(caught)));
          }}
        >
          Try again
        </button>
      </div>
    );
  }

  return (
    <div className="flex h-full min-h-screen flex-col">
      <header className="flex flex-wrap items-center gap-3 border-b border-line px-6 py-4">
        <SpaceTitle
          key={board.spaceName}
          name={board.spaceName}
          canEdit={board.role === "OWNER"}
          onSave={(name) => run(() => renameSpace(spaceId, name))}
        />
        <span
          className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${
            live ? "bg-emerald-100 text-emerald-800" : "bg-stone-200 text-stone-600"
          }`}
        >
          <span className={`h-1.5 w-1.5 rounded-full ${live ? "bg-emerald-600" : "bg-stone-400"}`} />
          {live ? "Live" : "Reconnecting"}
        </span>
        <People
          board={board}
          currentUserId={user.id}
          onRemove={(userId) => run(() => removeMember(spaceId, userId))}
        />
        {board.role === "OWNER" ? (
          <button
            type="button"
            className="ml-auto text-sm text-red-700"
            onClick={() => {
              if (window.confirm(`Delete ${board.spaceName}?`)) {
                void deleteSpace(spaceId).then(async () => {
                  await refreshSpaces();
                  router.replace("/spaces");
                });
              }
            }}
          >
            Delete space
          </button>
        ) : null}
      </header>
      {error ? <p className="px-6 pt-3 text-sm text-red-700">{error}</p> : null}
      <BoardFilters
        search={search}
        priorities={priorities}
        shown={shownCount(board, search, priorities)}
        total={taskCount(board)}
        onSearch={setSearch}
        onTogglePriority={(priority) =>
          setPriorities((current) =>
            current.includes(priority) ? current.filter((item) => item !== priority) : [...current, priority],
          )
        }
        onClear={() => {
          setSearch("");
          setPriorities([]);
        }}
      />
      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={onDragStart}
        onDragEnd={onDragEnd}
      >
        <div className="flex flex-1 gap-4 overflow-x-auto px-6 py-5">
          {visibleColumns(board, search, priorities).map((column, index) => (
            <ColumnLane
              key={column.id}
              column={column}
              filtering={isFiltering(search, priorities)}
              canMoveLeft={index > 0}
              canMoveRight={index < board.columns.length - 1}
              onRename={(name) => run(() => renameColumn(spaceId, column.id, name))}
              onDelete={() => {
                if (window.confirm(`Delete ${column.name} and its tasks?`)) {
                  void run(() => deleteColumn(spaceId, column.id));
                }
              }}
              onMove={(direction) => {
                const ids = board.columns.map((item) => item.id);
                const swap = direction === "left" ? index - 1 : index + 1;
                [ids[index], ids[swap]] = [ids[swap], ids[index]];
                void run(() => reorderColumns(spaceId, ids));
              }}
              onCreateTask={(title) => run(() => createTask(spaceId, column.id, title))}
              onOpen={setEditing}
              draggable={!isFiltering(search, priorities)}
            />
          ))}
          <AddColumn onCreate={(name) => run(() => createColumn(spaceId, name))} />
        </div>
        <DragOverlay>
          {dragging ? (
            <article className="w-72 rounded-2xl border border-line bg-paper p-3 shadow-lg">
              <TaskBody task={dragging} />
            </article>
          ) : null}
        </DragOverlay>
      </DndContext>
      {openTask ? (
        <TaskEditor
          key={openTask.id}
          task={openTask}
          members={board.members}
          spaceId={spaceId}
          revision={board.revision}
          onClose={() => setEditing(null)}
          onSave={async (draft) => {
            applyBoard(await updateTask(spaceId, openTask.id, draft));
            setEditing(null);
          }}
          onDelete={async () => {
            applyBoard(await deleteTask(spaceId, openTask.id));
            setEditing(null);
          }}
        />
      ) : null}
    </div>
  );
}

function SpaceTitle({
  name,
  canEdit,
  onSave,
}: {
  name: string;
  canEdit: boolean;
  onSave: (name: string) => void;
}) {
  const [value, setValue] = useState(name);
  return (
    <input
      value={value}
      disabled={!canEdit}
      onChange={(event) => setValue(event.target.value)}
      onBlur={() => {
        const trimmed = value.trim();
        if (trimmed && trimmed !== name) {
          onSave(trimmed);
        } else {
          setValue(name);
        }
      }}
      className="min-w-48 bg-transparent text-2xl font-semibold tracking-tight outline-none disabled:text-ink"
    />
  );
}

function ColumnName({ name, onRename }: { name: string; onRename: (name: string) => void }) {
  const [value, setValue] = useState(name);
  return (
    <input
      value={value}
      onChange={(event) => setValue(event.target.value)}
      onBlur={() => {
        const trimmed = value.trim();
        if (trimmed && trimmed !== name) {
          onRename(trimmed);
        } else {
          setValue(name);
        }
      }}
      className="min-w-0 flex-1 bg-transparent font-semibold outline-none"
    />
  );
}

function People({
  board,
  currentUserId,
  onRemove,
}: {
  board: Board;
  currentUserId: string;
  onRemove: (userId: string) => void;
}) {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="flex items-center gap-1 rounded-full border border-line bg-paper px-2 py-1"
      >
        {board.members.slice(0, 4).map((member) => (
          <Avatar key={member.userId} userId={member.userId} name={member.displayName} size="md" />
        ))}
      </button>
      {open ? (
        <div className="absolute top-11 left-0 z-20 w-80 rounded-2xl border border-line bg-paper p-4 shadow-xl">
          <p className="text-sm font-semibold">People</p>
          <ul className="mt-3 space-y-2">
            {board.members.map((member) => (
              <li key={member.userId} className="flex items-center justify-between gap-2 text-sm">
                <span className="flex min-w-0 items-center gap-2">
                  <Avatar userId={member.userId} name={member.displayName} size="md" />
                  <span className="min-w-0">
                    <span className="block font-medium">{member.displayName}</span>
                    <span className="block truncate text-xs text-muted">{member.email}</span>
                  </span>
                </span>
                {board.role === "OWNER" && member.userId !== currentUserId ? (
                  <button type="button" className="text-xs text-red-700" onClick={() => onRemove(member.userId)}>
                    Remove
                  </button>
                ) : (
                  <span className="text-xs text-muted">{member.role === "OWNER" ? "Owner" : "Member"}</span>
                )}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  );
}

function ColumnLane({
  column,
  filtering,
  canMoveLeft,
  canMoveRight,
  onRename,
  onDelete,
  onMove,
  onCreateTask,
  onOpen,
  draggable,
}: {
  column: Column;
  filtering: boolean;
  canMoveLeft: boolean;
  canMoveRight: boolean;
  onRename: (name: string) => void;
  onDelete: () => void;
  onMove: (direction: "left" | "right") => void;
  onCreateTask: (title: string) => void;
  onOpen: (task: Task) => void;
  draggable: boolean;
}) {
  const [title, setTitle] = useState("");
  const { setNodeRef } = useDroppable({ id: `column:${column.id}` });

  return (
    <section className="flex w-72 shrink-0 flex-col rounded-3xl bg-column p-3">
      <div className="mb-2 flex items-center gap-1">
        <ColumnName key={column.name} name={column.name} onRename={onRename} />
        <button type="button" disabled={!canMoveLeft} onClick={() => onMove("left")} className="px-1 text-muted disabled:opacity-30" aria-label="Move column left">
          ←
        </button>
        <button type="button" disabled={!canMoveRight} onClick={() => onMove("right")} className="px-1 text-muted disabled:opacity-30" aria-label="Move column right">
          →
        </button>
        <button type="button" onClick={onDelete} className="px-1 text-muted" aria-label="Delete column">
          ×
        </button>
      </div>
      <SortableContext items={column.tasks.map((task) => task.id)} strategy={verticalListSortingStrategy}>
        <div ref={setNodeRef} className="flex min-h-40 flex-1 flex-col gap-2">
          {column.tasks.length === 0 && filtering ? (
            <p className="px-1 py-2 text-sm text-muted">No matches</p>
          ) : null}
          {column.tasks.map((task) => (
            <TaskCard key={task.id} task={task} draggable={draggable} onOpen={() => onOpen(task)} />
          ))}
        </div>
      </SortableContext>
      <form
        className="mt-3 flex gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          const trimmed = title.trim();
          if (!trimmed) {
            return;
          }
          onCreateTask(trimmed);
          setTitle("");
        }}
      >
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Add a task"
          className="min-w-0 flex-1 rounded-xl border border-transparent bg-paper px-2 py-1.5 text-sm outline-none focus:border-accent"
        />
        <button type="submit" className="rounded-xl bg-ink px-2 text-sm text-white">
          Add
        </button>
      </form>
    </section>
  );
}

function TaskCard({ task, draggable, onOpen }: { task: Task; draggable: boolean; onOpen: () => void }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
    disabled: !draggable,
  });
  return (
    <article
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={`rounded-2xl border border-line bg-paper p-3 ${isDragging ? "opacity-40" : ""}`}
    >
      <div className="flex items-start gap-2">
        {draggable ? (
          <button
            type="button"
            className="mt-0.5 cursor-grab text-muted active:cursor-grabbing"
            aria-label="Drag task"
            {...attributes}
            {...listeners}
          >
            ::
          </button>
        ) : null}
        <button type="button" onClick={onOpen} className="min-w-0 flex-1 text-left">
          <TaskBody task={task} />
        </button>
      </div>
    </article>
  );
}

function TaskBody({ task }: { task: Task }) {
  return (
    <div className="min-w-0 flex-1">
      <div className="flex items-start justify-between gap-2">
        <p className="font-medium">{task.title}</p>
        <AssigneeStack assignees={task.assignees} />
      </div>
      <TaskMeta task={task} />
      {task.description ? <p className="mt-1 line-clamp-2 text-sm text-muted">{task.description}</p> : null}
    </div>
  );
}

function AddColumn({ onCreate }: { onCreate: (name: string) => void }) {
  const [name, setName] = useState("");
  return (
    <form
      className="w-72 shrink-0"
      onSubmit={(event) => {
        event.preventDefault();
        const trimmed = name.trim();
        if (!trimmed) {
          return;
        }
        onCreate(trimmed);
        setName("");
      }}
    >
      <input
        value={name}
        onChange={(event) => setName(event.target.value)}
        placeholder="Add a column"
        className="w-full rounded-2xl border border-dashed border-stone-400 bg-transparent px-3 py-3 outline-none focus:border-accent"
      />
    </form>
  );
}

function TaskEditor({
  task,
  members,
  spaceId,
  revision,
  onClose,
  onSave,
  onDelete,
}: {
  task: Task;
  members: Member[];
  spaceId: string;
  revision: number;
  onClose: () => void;
  onSave: (draft: {
    title: string;
    description: string;
    userIds: string[];
    labels: TaskLabel[];
    priority: TaskPriority;
  }) => Promise<void>;
  onDelete: () => Promise<void>;
}) {
  const [title, setTitle] = useState(task.title);
  const [description, setDescription] = useState(task.description ?? "");
  const [assignees, setAssignees] = useState(task.assignees);
  const [labels, setLabels] = useState(task.labels);
  const [priority, setPriority] = useState(task.priority);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [entries, setEntries] = useState<TaskActivity[] | null>(null);
  const [activityError, setActivityError] = useState<string | null>(null);
  const [comment, setComment] = useState("");
  const [posting, setPosting] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const requestSeq = useRef(0);

  useEffect(() => {
    const id = ++requestSeq.current;
    getTaskActivity(spaceId, task.id)
      .then((next) => {
        if (id === requestSeq.current) {
          setEntries(next);
          setActivityError(null);
        }
      })
      .catch((caught) => {
        if (id === requestSeq.current) {
          setActivityError(apiMessage(caught));
        }
      });
  }, [spaceId, task.id, revision]);

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [entries]);

  return (
    <div className="fixed inset-0 z-30 overflow-y-auto bg-ink/40 p-4">
      <div className="mx-auto flex min-h-full w-full max-w-xl items-center">
        <div className="w-full rounded-3xl bg-paper p-6">
          <form
            onSubmit={async (event) => {
              event.preventDefault();
              if (saving) {
                return;
              }
              setSaving(true);
              setError(null);
              try {
                await onSave({
                  title: title.trim(),
                  description,
                  userIds: assignees.map((person) => person.userId),
                  labels,
                  priority,
                });
              } catch (caught) {
                setError(apiMessage(caught));
                setSaving(false);
              }
            }}
          >
            <label className="block text-sm font-medium">
              Title
              <input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                className="mt-1 w-full rounded-xl border border-line px-3 py-2 outline-none focus:border-accent"
              />
            </label>
            <label className="mt-4 block text-sm font-medium">
              Description
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                rows={4}
                className="mt-1 w-full rounded-xl border border-line px-3 py-2 outline-none focus:border-accent"
              />
            </label>
            {error ? <p className="mt-2 text-sm text-red-700">{error}</p> : null}
            <LabelPicker
              labels={labels}
              disabled={saving}
              error={null}
              onToggle={(label) => {
                setLabels((current) =>
                  current.includes(label) ? current.filter((item) => item !== label) : [...current, label],
                );
              }}
            />
            <PriorityPicker
              priority={priority}
              disabled={saving}
              error={null}
              onSelect={setPriority}
            />
            <AssigneePicker
              members={members}
              assignees={assignees}
              disabled={saving}
              error={null}
              onToggle={(userId) => {
                setAssignees((current) => {
                  if (current.some((person) => person.userId === userId)) {
                    return current.filter((person) => person.userId !== userId);
                  }
                  const member = members.find((person) => person.userId === userId);
                  if (!member) {
                    return current;
                  }
                  return [...current, { userId: member.userId, displayName: member.displayName }];
                });
              }}
            />
            <div className="mt-5 flex items-center justify-between">
              <button type="button" className="text-sm text-red-700" onClick={() => void onDelete()}>
                Delete task
              </button>
              <div className="flex gap-2">
                <button type="button" onClick={onClose} className="rounded-xl px-3 py-2 text-sm">
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="rounded-xl bg-accent px-4 py-2 text-sm font-medium text-accent-ink disabled:opacity-50"
                >
                  {saving ? "Saving…" : "Save"}
                </button>
              </div>
            </div>
          </form>
          <section className="mt-6 border-t border-line pt-5">
            <h2 className="text-sm font-medium">Activity</h2>
            <div ref={listRef} className="mt-3 max-h-64 space-y-3 overflow-y-auto pr-1">
              {entries === null && !activityError ? <p className="text-sm text-muted">Loading activity…</p> : null}
              {activityError ? <p className="text-sm text-red-700">{activityError}</p> : null}
              {entries?.length === 0 ? <p className="text-sm text-muted">No activity yet.</p> : null}
              {entries?.map((entry) =>
                entry.kind === "COMMENT" ? (
                  <article key={entry.id} className="rounded-2xl bg-column px-3 py-2">
                    <div className="flex items-center gap-2">
                      <Avatar userId={entry.actorId} name={entry.actorName} />
                      <p className="text-sm font-medium">{entry.actorName}</p>
                      <time className="ml-auto text-xs text-muted" dateTime={entry.createdAt}>
                        {formatActivityTime(entry.createdAt)}
                      </time>
                    </div>
                    <p className="mt-1 whitespace-pre-wrap pl-8 text-sm">{entry.body}</p>
                  </article>
                ) : (
                  <p key={entry.id} className="text-sm text-muted">
                    <span className="font-medium text-ink">{entry.actorName}</span> {entry.summary}
                    <time className="ml-2 text-xs" dateTime={entry.createdAt}>
                      {formatActivityTime(entry.createdAt)}
                    </time>
                  </p>
                ),
              )}
            </div>
            <form
              className="mt-4"
              onSubmit={async (event) => {
                event.preventDefault();
                const body = comment.trim();
                if (!body || posting) {
                  return;
                }
                const id = ++requestSeq.current;
                setPosting(true);
                setActivityError(null);
                try {
                  const next = await addTaskComment(spaceId, task.id, body);
                  setComment("");
                  if (id === requestSeq.current) {
                    setEntries(next);
                  }
                } catch (caught) {
                  if (id === requestSeq.current) {
                    setActivityError(apiMessage(caught));
                  }
                } finally {
                  setPosting(false);
                }
              }}
            >
              <label className="block text-sm font-medium">
                Comment
                <textarea
                  value={comment}
                  onChange={(event) => setComment(event.target.value)}
                  rows={2}
                  placeholder="Leave a comment"
                  className="mt-1 w-full rounded-xl border border-line px-3 py-2 outline-none focus:border-accent"
                />
              </label>
              <div className="mt-2 flex justify-end">
                <button
                  type="submit"
                  disabled={posting || comment.trim().length === 0}
                  className="rounded-xl bg-accent px-4 py-2 text-sm font-medium text-accent-ink disabled:opacity-50"
                >
                  {posting ? "Posting…" : "Comment"}
                </button>
              </div>
            </form>
          </section>
        </div>
      </div>
    </div>
  );
}

function formatActivityTime(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(date);
}

const LABEL_META: Record<TaskLabel, { name: string; chip: string }> = {
  BUG: { name: "Bug", chip: "bg-rose-100 text-rose-900" },
  FEATURE: { name: "Feature", chip: "bg-teal-100 text-teal-900" },
  DESIGN: { name: "Design", chip: "bg-violet-100 text-violet-900" },
};

const PRIORITY_META: Record<TaskPriority, { name: string; chip: string }> = {
  LOW: { name: "Low", chip: "bg-stone-200 text-stone-800" },
  MEDIUM: { name: "Medium", chip: "bg-amber-100 text-amber-900" },
  HIGH: { name: "High", chip: "bg-rose-100 text-rose-900" },
};

const LABEL_ORDER: TaskLabel[] = ["BUG", "FEATURE", "DESIGN"];
const PRIORITY_ORDER: TaskPriority[] = ["LOW", "MEDIUM", "HIGH"];

function BoardFilters({
  search,
  priorities,
  shown,
  total,
  onSearch,
  onTogglePriority,
  onClear,
}: {
  search: string;
  priorities: TaskPriority[];
  shown: number;
  total: number;
  onSearch: (value: string) => void;
  onTogglePriority: (priority: TaskPriority) => void;
  onClear: () => void;
}) {
  const filtering = isFiltering(search, priorities);
  return (
    <div className="flex flex-wrap items-center gap-3 border-b border-line px-6 py-3">
      <label className="min-w-56 flex-1">
        <span className="sr-only">Search tasks</span>
        <input
          value={search}
          onChange={(event) => onSearch(event.target.value)}
          placeholder="Search tasks"
          className="w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-accent"
        />
      </label>
      <div className="flex items-center gap-2" role="group" aria-label="Filter by priority">
        {PRIORITY_ORDER.map((priority) => {
          const selected = priorities.includes(priority);
          return (
            <button
              key={priority}
              type="button"
              aria-pressed={selected}
              onClick={() => onTogglePriority(priority)}
              className={`rounded-full border px-3 py-1 text-sm ${
                selected ? `${PRIORITY_META[priority].chip} border-transparent` : "border-line bg-paper text-ink"
              }`}
            >
              {PRIORITY_META[priority].name}
            </button>
          );
        })}
      </div>
      {filtering ? (
        <>
          <p className="text-sm text-muted">
            {shown} of {total}
          </p>
          <button type="button" onClick={onClear} className="text-sm text-muted">
            Clear
          </button>
        </>
      ) : null}
    </div>
  );
}

function TaskMeta({ task }: { task: Task }) {
  return (
    <div className="mt-2 flex flex-wrap items-center gap-1.5">
      {task.labels.map((label) => (
        <span key={label} className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${LABEL_META[label].chip}`}>
          {LABEL_META[label].name}
        </span>
      ))}
      <span className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${PRIORITY_META[task.priority].chip}`}>
        {PRIORITY_META[task.priority].name}
      </span>
    </div>
  );
}

function LabelPicker({
  labels,
  disabled,
  error,
  onToggle,
}: {
  labels: TaskLabel[];
  disabled: boolean;
  error: string | null;
  onToggle: (label: TaskLabel) => void;
}) {
  return (
    <fieldset className="mt-4" disabled={disabled}>
      <legend className="text-sm font-medium">Labels</legend>
      <div className="mt-2 flex flex-wrap gap-2">
        {LABEL_ORDER.map((label) => {
          const selected = labels.includes(label);
          return (
            <button
              key={label}
              type="button"
              aria-pressed={selected}
              onClick={() => onToggle(label)}
              className={`rounded-full border px-2.5 py-1 text-sm font-medium ${
                selected ? `${LABEL_META[label].chip} border-transparent` : "border-line bg-paper text-ink"
              }`}
            >
              {LABEL_META[label].name}
            </button>
          );
        })}
      </div>
      {error ? <p className="mt-2 text-sm text-red-700">{error}</p> : null}
    </fieldset>
  );
}

function PriorityPicker({
  priority,
  disabled,
  error,
  onSelect,
}: {
  priority: TaskPriority;
  disabled: boolean;
  error: string | null;
  onSelect: (priority: TaskPriority) => void;
}) {
  return (
    <fieldset className="mt-4" disabled={disabled}>
      <legend className="text-sm font-medium">Priority</legend>
      <div className="mt-2 flex flex-wrap gap-2" role="radiogroup" aria-label="Priority">
        {PRIORITY_ORDER.map((option) => {
          const selected = option === priority;
          return (
            <button
              key={option}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onSelect(option)}
              className={`rounded-full border px-2.5 py-1 text-sm font-medium ${
                selected ? `${PRIORITY_META[option].chip} border-transparent` : "border-line bg-paper text-ink"
              }`}
            >
              {PRIORITY_META[option].name}
            </button>
          );
        })}
      </div>
      {error ? <p className="mt-2 text-sm text-red-700">{error}</p> : null}
    </fieldset>
  );
}

function isFiltering(search: string, priorities: TaskPriority[]) {
  return search.trim().length > 0 || priorities.length > 0;
}

function taskCount(board: Board) {
  return board.columns.reduce((sum, column) => sum + column.tasks.length, 0);
}

function shownCount(board: Board, search: string, priorities: TaskPriority[]) {
  return visibleColumns(board, search, priorities).reduce((sum, column) => sum + column.tasks.length, 0);
}

function visibleColumns(board: Board, search: string, priorities: TaskPriority[]) {
  return board.columns.map((column) => ({
    ...column,
    tasks: column.tasks.filter((task) => taskVisible(task, search, priorities)),
  }));
}

function taskVisible(task: Task, search: string, priorities: TaskPriority[]) {
  if (priorities.length > 0 && !priorities.includes(task.priority)) {
    return false;
  }
  const terms = search.trim().toLowerCase().split(/\s+/).filter(Boolean);
  if (terms.length === 0) {
    return true;
  }
  const haystack = [task.title, task.description ?? "", ...task.labels.map((label) => LABEL_META[label].name)]
    .join("\n")
    .toLowerCase();
  return terms.every((term) => haystack.includes(term));
}

function findColumn(board: Board, id: string) {
  if (id.startsWith("column:")) {
    return board.columns.find((column) => column.id === id.slice("column:".length));
  }
  return board.columns.find((column) => column.tasks.some((task) => task.id === id));
}

function moveLocal(board: Board, taskId: string, toColumnId: string, toIndex: number): Board {
  const columns = board.columns.map((column) => ({
    ...column,
    tasks: column.tasks.map((task) => ({ ...task })),
  }));
  const from = columns.find((column) => column.tasks.some((task) => task.id === taskId));
  const to = columns.find((column) => column.id === toColumnId);
  if (!from || !to) {
    return board;
  }
  const index = from.tasks.findIndex((task) => task.id === taskId);
  const [task] = from.tasks.splice(index, 1);
  const target = from.id === to.id ? from : to;
  target.tasks.splice(Math.max(0, Math.min(toIndex, target.tasks.length)), 0, {
    ...task,
    columnId: target.id,
  });
  return {
    ...board,
    columns: columns.map((column) => ({
      ...column,
      tasks: column.tasks.map((item, position) => ({ ...item, position })),
    })),
  };
}

function AssigneePicker({
  members,
  assignees,
  disabled,
  error,
  onToggle,
}: {
  members: Member[];
  assignees: Assignee[];
  disabled: boolean;
  error: string | null;
  onToggle: (userId: string) => void;
}) {
  return (
    <fieldset className="mt-4" disabled={disabled}>
      <legend className="text-sm font-medium">Assignees(click to assign)</legend>
      <ul className="mt-2 flex flex-wrap gap-2">
        {members.map((member) => {
          const selected = assignees.some((person) => person.userId === member.userId);
          return (
            <li key={member.userId}>
              <button
                type="button"
                aria-pressed={selected}
                onClick={() => onToggle(member.userId)}
                className={`flex items-center gap-2 rounded-full border px-2 py-1 text-sm ${
                  selected ? "border-accent bg-teal-50" : "border-line bg-paper"
                }`}
              >
                <Avatar userId={member.userId} name={member.displayName} />
                {member.displayName}
              </button>
            </li>
          );
        })}
      </ul>
      {error ? <p className="mt-2 text-sm text-red-700">{error}</p> : null}
    </fieldset>
  );
}

function AssigneeStack({ assignees }: { assignees: Assignee[] }) {
  if (assignees.length === 0) {
    return null;
  }
  const visible = assignees.slice(0, 3);
  const extra = assignees.length - visible.length;
  const names = assignees.map((person) => person.displayName).join(", ");
  return (
    <span className="flex shrink-0 items-center" title={names} aria-label={`Assigned to ${names}`}>
      {visible.map((person, index) => (
        <Avatar
          key={person.userId}
          userId={person.userId}
          name={person.displayName}
          className={index === 0 ? "" : "-ml-1.5"}
        />
      ))}
      {extra > 0 ? (
        <span className="-ml-1.5 inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-column px-1 text-[10px] font-semibold ring-2 ring-paper">
          +{extra}
        </span>
      ) : null}
    </span>
  );
}

function Avatar({
  userId,
  name,
  size = "sm",
  className = "",
}: {
  userId: string;
  name: string;
  size?: "sm" | "md";
  className?: string;
}) {
  const box = size === "sm" ? "h-6 w-6 text-[10px]" : "h-7 w-7 text-xs";
  return (
    <span
      title={name}
      className={`inline-flex shrink-0 items-center justify-center rounded-full font-semibold ring-2 ring-paper ${box} ${avatarTone(userId)} ${className}`}
    >
      {initials(name)}
    </span>
  );
}

function avatarTone(userId: string) {
  const tones = [
    "bg-teal-800 text-teal-50",
    "bg-amber-800 text-amber-50",
    "bg-rose-800 text-rose-50",
    "bg-sky-800 text-sky-50",
    "bg-violet-800 text-violet-50",
    "bg-stone-600 text-stone-50",
  ];
  let hash = 0;
  for (const char of userId) {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  }
  return tones[hash % tones.length];
}

function initials(name: string) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}
