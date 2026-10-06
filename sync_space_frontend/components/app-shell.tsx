"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useState } from "react";

import { apiMessage, createSpace, getMe, listSpaces } from "@/lib/api";
import { clearSession, getToken } from "@/lib/auth";
import type { SpaceSummary, User } from "@/lib/types";

type ShellValue = {
  user: User;
  spaces: SpaceSummary[];
  refreshSpaces: () => Promise<void>;
};

const ShellContext = createContext<ShellValue | null>(null);

export function useShell() {
  const value = useContext(ShellContext);
  if (!value) {
    throw new Error("useShell must be used inside AppShell");
  }
  return value;
}

export function AppShell({
  children,
  activeSpaceId,
}: {
  children: React.ReactNode;
  activeSpaceId?: string;
}) {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [spaces, setSpaces] = useState<SpaceSummary[]>([]);
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const refreshSpaces = useCallback(async () => {
    setSpaces(await listSpaces());
  }, []);

  useEffect(() => {
    const onUnauthorized = () => router.replace("/login");
    window.addEventListener("sync-space-unauthorized", onUnauthorized);
    return () => window.removeEventListener("sync-space-unauthorized", onUnauthorized);
  }, [router]);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }
    let cancelled = false;
    Promise.all([getMe(), listSpaces()])
      .then(([me, spaceList]) => {
        if (!cancelled) {
          setUser(me);
          setSpaces(spaceList);
        }
      })
      .catch(() => {
        if (!cancelled) {
          router.replace("/login");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [router]);

  async function onCreate(event: React.FormEvent) {
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

  function logout() {
    clearSession();
    router.replace("/login");
  }

  if (!user) {
    return <p className="p-8 text-muted">Opening your spaces…</p>;
  }

  return (
    <ShellContext.Provider value={{ user, spaces, refreshSpaces }}>
      <div className="flex min-h-full flex-1">
        <aside className="flex w-72 shrink-0 flex-col bg-ink text-stone-100">
          <div className="px-5 pt-6 pb-4">
            <Link href="/spaces" className="text-lg font-semibold tracking-tight">
              Sync Space
            </Link>
            <p className="mt-1 text-sm text-stone-400">Boards that stay in step</p>
          </div>
          <nav className="flex-1 space-y-1 overflow-y-auto px-3">
            {spaces.map((space) => (
              <Link
                key={space.id}
                href={`/spaces/${space.id}`}
                className={`block rounded-xl px-3 py-2 text-sm ${
                  space.id === activeSpaceId
                    ? "bg-white/15 text-white"
                    : "text-stone-300 hover:bg-white/10"
                }`}
              >
                <span className="block truncate font-medium">{space.name}</span>
                <span className="text-xs text-stone-400">
                  {space.memberCount} {space.memberCount === 1 ? "person" : "people"}
                </span>
              </Link>
            ))}
          </nav>
          <form onSubmit={onCreate} className="space-y-2 px-4 py-4">
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="New space"
              className="w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm outline-none placeholder:text-stone-500 focus:border-teal-300"
            />
            {error ? <p className="text-xs text-red-300">{error}</p> : null}
            <button
              type="submit"
              className="w-full rounded-xl bg-accent px-3 py-2 text-sm font-medium text-accent-ink"
            >
              Create space
            </button>
          </form>
          <div className="flex items-center justify-between gap-3 border-t border-white/10 px-4 py-4">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">{user.displayName}</p>
              <p className="truncate text-xs text-stone-400">{user.email}</p>
            </div>
            <button type="button" onClick={logout} className="text-sm text-stone-300 hover:text-white">
              Log out
            </button>
          </div>
        </aside>
        <div className="min-w-0 flex-1">{children}</div>
      </div>
    </ShellContext.Provider>
  );
}
