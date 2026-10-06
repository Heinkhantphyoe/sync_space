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
import { useEffect, useState } from "react";

import { useShell } from "@/components/app-shell";
import {
  apiMessage,
  createColumn,
  createTask,
  deleteColumn,
  deleteSpace,
  deleteTask,
  getBoard,
  moveTask,
  removeMember,
  renameColumn,
  renameSpace,
  reorderColumns,
  updateTask,
} from "@/lib/api";
import { connectBoardSocket } from "@/lib/board-socket";
import type { Board, Column, Task } from "@/lib/types";

export function BoardScreen({ spaceId }: { spaceId: string }) {
  const router = useRouter();
  const { user, refreshSpaces } = useShell();
  const [board, setBoard] = useState<Board | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState(false);
  const [editing, setEditing] = useState<Task | null>(null);
  const [dragging, setDragging] = useState<Task | null>(null);
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
      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={onDragStart}
        onDragEnd={onDragEnd}
      >
        <div className="flex flex-1 gap-4 overflow-x-auto px-6 py-5">
          {board.columns.map((column, index) => (
            <ColumnLane
              key={column.id}
              column={column}
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
            />
          ))}
          <AddColumn onCreate={(name) => run(() => createColumn(spaceId, name))} />
        </div>
        <DragOverlay>
          {dragging ? (
            <article className="w-72 rounded-2xl border border-line bg-paper p-3 shadow-lg">
              <p className="font-medium">{dragging.title}</p>
            </article>
          ) : null}
        </DragOverlay>
      </DndContext>
      {editing ? (
        <TaskEditor
          task={editing}
          onClose={() => setEditing(null)}
          onSave={async (title, description) => {
            applyBoard(await updateTask(spaceId, editing.id, title, description));
            setEditing(null);
          }}
          onDelete={async () => {
            applyBoard(await deleteTask(spaceId, editing.id));
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
          <span
            key={member.userId}
            title={member.displayName}
            className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-column text-xs font-semibold"
          >
            {initials(member.displayName)}
          </span>
        ))}
      </button>
      {open ? (
        <div className="absolute top-11 left-0 z-20 w-80 rounded-2xl border border-line bg-paper p-4 shadow-xl">
          <p className="text-sm font-semibold">People</p>
          <ul className="mt-3 space-y-2">
            {board.members.map((member) => (
              <li key={member.userId} className="flex items-center justify-between gap-2 text-sm">
                <span>
                  <span className="font-medium">{member.displayName}</span>
                  <span className="block text-xs text-muted">{member.email}</span>
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
  canMoveLeft,
  canMoveRight,
  onRename,
  onDelete,
  onMove,
  onCreateTask,
  onOpen,
}: {
  column: Column;
  canMoveLeft: boolean;
  canMoveRight: boolean;
  onRename: (name: string) => void;
  onDelete: () => void;
  onMove: (direction: "left" | "right") => void;
  onCreateTask: (title: string) => void;
  onOpen: (task: Task) => void;
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
          {column.tasks.map((task) => (
            <TaskCard key={task.id} task={task} onOpen={() => onOpen(task)} />
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

function TaskCard({ task, onOpen }: { task: Task; onOpen: () => void }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: task.id });
  return (
    <article
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={`rounded-2xl border border-line bg-paper p-3 ${isDragging ? "opacity-40" : ""}`}
    >
      <div className="flex items-start gap-2">
        <button
          type="button"
          className="mt-0.5 cursor-grab text-muted active:cursor-grabbing"
          aria-label="Drag task"
          {...attributes}
          {...listeners}
        >
          ::
        </button>
        <button type="button" onClick={onOpen} className="min-w-0 flex-1 text-left">
          <p className="font-medium">{task.title}</p>
          {task.description ? <p className="mt-1 line-clamp-2 text-sm text-muted">{task.description}</p> : null}
        </button>
      </div>
    </article>
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
  onClose,
  onSave,
  onDelete,
}: {
  task: Task;
  onClose: () => void;
  onSave: (title: string, description: string) => Promise<void>;
  onDelete: () => Promise<void>;
}) {
  const [title, setTitle] = useState(task.title);
  const [description, setDescription] = useState(task.description ?? "");
  const [error, setError] = useState<string | null>(null);

  return (
    <div className="fixed inset-0 z-30 flex items-center justify-center bg-ink/40 p-4">
      <form
        className="w-full max-w-lg rounded-3xl bg-paper p-6"
        onSubmit={async (event) => {
          event.preventDefault();
          try {
            await onSave(title.trim(), description);
          } catch (caught) {
            setError(apiMessage(caught));
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
            rows={5}
            className="mt-1 w-full rounded-xl border border-line px-3 py-2 outline-none focus:border-accent"
          />
        </label>
        {error ? <p className="mt-2 text-sm text-red-700">{error}</p> : null}
        <div className="mt-5 flex items-center justify-between">
          <button type="button" className="text-sm text-red-700" onClick={() => void onDelete()}>
            Delete task
          </button>
          <div className="flex gap-2">
            <button type="button" onClick={onClose} className="rounded-xl px-3 py-2 text-sm">
              Cancel
            </button>
            <button type="submit" className="rounded-xl bg-accent px-4 py-2 text-sm font-medium text-accent-ink">
              Save
            </button>
          </div>
        </div>
      </form>
    </div>
  );
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

function initials(name: string) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}
