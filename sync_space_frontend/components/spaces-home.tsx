"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { useShell } from "@/components/app-shell";
import { apiMessage, createSpace } from "@/lib/api";

export function SpacesHome() {
  const router = useRouter();
  const { spaces, refreshSpaces } = useShell();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

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
