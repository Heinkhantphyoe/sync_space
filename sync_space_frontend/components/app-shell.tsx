"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useState } from "react";

import {
  apiMessage,
  cancelInvite,
  createSpace,
  getMe,
  inviteMember,
  listInvites,
  listNotifications,
  listSpaces,
  markAllNotificationsRead,
  markNotificationRead,
} from "@/lib/api";
import { clearSession, getToken } from "@/lib/auth";
import { connectUserSocket } from "@/lib/board-socket";
import type { AppNotification, PendingInvite, SpaceSummary, User } from "@/lib/types";

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
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [inviteNotice, setInviteNotice] = useState<string | null>(null);
  const [pendingInvites, setPendingInvites] = useState<PendingInvite[]>([]);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [showNotes, setShowNotes] = useState(false);

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

  const activeSpace = spaces.find((space) => space.id === activeSpaceId) ?? null;
  const unread = notifications.filter((note) => !note.read).length;

  useEffect(() => {
    if (!user) {
      return;
    }
    let cancelled = false;
    listNotifications()
      .then((next) => {
        if (!cancelled) {
          setNotifications(next);
        }
      })
      .catch(() => {});
    const stop = connectUserSocket(user.id, (notification) => {
      setNotifications((current) =>
        [notification, ...current.filter((item) => item.id !== notification.id)].slice(0, 30),
      );
    });
    return () => {
      cancelled = true;
      stop();
    };
  }, [user]);

  useEffect(() => {
    setInviteEmail("");
    setInviteError(null);
    setInviteNotice(null);
    setPendingInvites([]);
    const space = spaces.find((item) => item.id === activeSpaceId);
    if (!activeSpaceId || space?.role !== "OWNER") {
      return;
    }
    let cancelled = false;
    listInvites(activeSpaceId)
      .then((next) => {
        if (!cancelled) {
          setPendingInvites(next);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setPendingInvites([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [activeSpaceId, spaces]);

  async function onInvite(event: React.FormEvent) {
    event.preventDefault();
    if (!activeSpace || activeSpace.role !== "OWNER") {
      return;
    }
    const email = inviteEmail.trim();
    if (!email) {
      return;
    }
    setInviteError(null);
    setInviteNotice(null);
    try {
      const result = await inviteMember(activeSpace.id, email);
      setInviteEmail("");
      setInviteNotice(
        result.status === "PENDING"
          ? `${email} will join when they create an account.`
          : `${email} can open this space now.`,
      );
      if (result.status === "PENDING") {
        setPendingInvites(await listInvites(activeSpace.id));
      } else {
        setPendingInvites((current) => current.filter((invite) => invite.email !== email.toLowerCase()));
        window.dispatchEvent(new CustomEvent("sync-space-members-changed", { detail: activeSpace.id }));
      }
      await refreshSpaces();
    } catch (caught) {
      setInviteError(apiMessage(caught));
    }
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
          {activeSpace?.role === "OWNER" ? (
            <form onSubmit={onInvite} className="space-y-2 px-4 pt-3">
              <p className="text-xs text-stone-400">Invite to {activeSpace.name}</p>
              <input
                type="email"
                value={inviteEmail}
                onChange={(event) => setInviteEmail(event.target.value)}
                placeholder="Email address"
                className="w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm outline-none placeholder:text-stone-500 focus:border-teal-300"
              />
              {pendingInvites.length > 0 ? (
                <ul className="space-y-1">
                  {pendingInvites.map((invite) => (
                    <li key={invite.id} className="flex items-center justify-between gap-2 text-xs text-stone-300">
                      <span className="truncate">{invite.email}</span>
                      <button
                        type="button"
                        className="shrink-0 text-stone-400 hover:text-white"
                        onClick={() => {
                          void cancelInvite(activeSpace.id, invite.id)
                            .then(() => setPendingInvites((current) => current.filter((item) => item.id !== invite.id)))
                            .catch((caught) => setInviteError(apiMessage(caught)));
                        }}
                      >
                        Cancel
                      </button>
                    </li>
                  ))}
                </ul>
              ) : null}
              {inviteError ? <p className="text-xs text-red-300">{inviteError}</p> : null}
              {inviteNotice ? <p className="text-xs text-emerald-300">{inviteNotice}</p> : null}
              <button
                type="submit"
                className="w-full rounded-xl border border-white/15 px-3 py-2 text-sm font-medium text-white hover:bg-white/10"
              >
                Invite
              </button>
            </form>
          ) : null}
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
          <div className="border-t border-white/10 px-4 pt-3">
            <button
              type="button"
              className="text-sm font-medium text-stone-100"
              aria-expanded={showNotes}
              onClick={() => setShowNotes((current) => !current)}
            >
              Alerts{unread > 0 ? ` (${unread})` : ""}
            </button>
            {showNotes ? (
              <div className="mt-2 max-h-64 space-y-2 overflow-y-auto pb-2">
                {notifications.length === 0 ? <p className="text-xs text-stone-400">No alerts yet.</p> : null}
                {notifications.map((note) => (
                  <button
                    key={note.id}
                    type="button"
                    className={`block w-full rounded-xl px-2 py-2 text-left text-xs ${
                      note.read ? "text-stone-400" : "bg-white/10 text-white"
                    }`}
                    onClick={() => {
                      void markNotificationRead(note.id).then((updated) => {
                        setNotifications((current) =>
                          current.map((item) => (item.id === updated.id ? updated : item)),
                        );
                      });
                      setShowNotes(false);
                      router.push(`/spaces/${note.spaceId}?task=${note.taskId}`);
                    }}
                  >
                    <span className="block font-medium">
                      {note.actorName} {note.kind === "ASSIGNED" ? "assigned you" : "commented"}
                    </span>
                    <span className="block truncate">
                      {note.spaceName} · {note.taskTitle}
                    </span>
                    {note.kind === "COMMENT" ? <span className="block truncate">{note.body}</span> : null}
                  </button>
                ))}
                {unread > 0 ? (
                  <button
                    type="button"
                    className="text-xs text-stone-300"
                    onClick={() => {
                      void markAllNotificationsRead().then(() => {
                        setNotifications((current) => current.map((item) => ({ ...item, read: true })));
                      });
                    }}
                  >
                    Mark all read
                  </button>
                ) : null}
              </div>
            ) : null}
          </div>
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
