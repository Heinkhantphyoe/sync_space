"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { useShell } from "@/components/app-shell";
import { apiMessage, createSpace, listMyTasks } from "@/lib/api";
import type { AssignedTask } from "@/lib/types";

export function SpacesHome() {
  const router = useRouter();
  const { spaces, refreshSpaces } = useShell();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [tasks, setTasks] = useState<AssignedTask[] | null>(null);
  const [tasksError, setTasksError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listMyTasks()
      .then((next) => {
        if (!cancelled) {
          setTasks(next);
          setTasksError(null);
        }
      })
      .catch((caught) => {
        if (!cancelled) {
          setTasksError(apiMessage(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      return;
    }
    setError(null);
    try {
      const space = await createSpace(trimmed);
      setName("");
      await refreshSpaces();
      router.push(`/spaces/${space.id}`);
    } catch (caught) {
      setError(apiMessage(caught));
    }
  }

  return (
    <main className="px-8 py-10">
      <h1 className="text-3xl font-semibold tracking-tight">Your spaces</h1>
      <p className="mt-2 max-w-xl text-muted">
        Each space is a shared board. Create one, invite someone who already has an account, and move work together.
      </p>
      <form onSubmit={onSubmit} className="mt-6 flex max-w-xl gap-2">
        <input
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="Space name"
          className="flex-1 rounded-xl border border-line bg-paper px-3 py-2.5 outline-none focus:border-accent"
        />
        <button type="submit" className="rounded-xl bg-accent px-4 py-2.5 font-medium text-accent-ink">
          Create
        </button>
      </form>
      {error ? <p className="mt-2 text-sm text-red-700">{error}</p> : null}
      <section className="mt-10">
        <h2 className="text-lg font-semibold">My work</h2>
        {tasksError ? <p className="mt-2 text-sm text-red-700">{tasksError}</p> : null}
        {tasks === null && !tasksError ? <p className="mt-3 text-sm text-muted">Loading your tasks…</p> : null}
        {tasks?.length === 0 ? <p className="mt-3 text-sm text-muted">Nothing is assigned to you.</p> : null}
        {tasks && tasks.length > 0 ? (
          <ul className="mt-3 divide-y divide-line overflow-hidden rounded-2xl border border-line bg-paper">
            {tasks.map((task) => {
              const overdue = isOverdue(task);
              return (
                <li key={task.id}>
                  <Link
                    href={`/spaces/${task.spaceId}?task=${task.id}`}
                    className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 hover:bg-column"
                  >
                    <span>
                      <span className="block font-medium">{task.title}</span>
                      <span className="text-sm text-muted">
                        {task.spaceName} · {task.columnName}
                      </span>
                    </span>
                    <span className={`text-sm ${overdue ? "font-medium text-rose-700" : "text-muted"}`}>
                      {task.dueOn ? `${overdue ? "Overdue" : "Due"} ${formatDue(task.dueOn)}` : priorityName(task.priority)}
                    </span>
                  </Link>
                </li>
              );
            })}
          </ul>
        ) : null}
      </section>
      {spaces.length === 0 ? (
        <p className="mt-10 text-muted">No spaces yet. Create the first one above.</p>
      ) : (
        <ul className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {spaces.map((space) => (
            <li key={space.id}>
              <Link
                href={`/spaces/${space.id}`}
                className="block rounded-2xl border border-line bg-paper p-5 transition hover:-translate-y-0.5 hover:border-accent"
              >
                <p className="text-lg font-semibold">{space.name}</p>
                <p className="mt-2 text-sm text-muted">
                  {space.role === "OWNER" ? "Owner" : "Member"} · {space.memberCount}{" "}
                  {space.memberCount === 1 ? "person" : "people"}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}

function isOverdue(task: AssignedTask) {
  if (!task.dueOn || task.columnName.trim().toLowerCase() === "done") {
    return false;
  }
  const today = new Date();
  const iso = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  return task.dueOn < iso;
}

function formatDue(iso: string) {
  const [year, month, day] = iso.split("-").map(Number);
  if (!year || !month || !day) {
    return iso;
  }
  return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", year: "numeric" }).format(
    new Date(year, month - 1, day),
  );
}

function priorityName(priority: AssignedTask["priority"]) {
  if (priority === "HIGH") {
    return "High";
  }
  if (priority === "LOW") {
    return "Low";
  }
  return "Medium";
}
