"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { apiMessage, login, register } from "@/lib/api";
import { saveSession } from "@/lib/auth";

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const router = useRouter();
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const isRegister = mode === "register";

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      const session = isRegister
        ? await register(email, password, displayName)
        : await login(email, password);
      saveSession(session.token, session.user);
      router.replace("/spaces");
    } catch (caught) {
      setError(apiMessage(caught));
    } finally {
      setPending(false);
    }
  }

  return (
    <main className="flex min-h-full flex-1 items-center justify-center px-4 py-16">
      <div className="w-full max-w-md">
        <p className="text-sm font-medium tracking-[0.18em] text-accent uppercase">
          Sync Space
        </p>
        <h1 className="mt-3 text-4xl font-semibold tracking-tight text-ink">
          {isRegister ? "Create your account" : "Welcome back"}
        </h1>
        <p className="mt-2 text-muted">
          {isRegister
            ? "Start a board and invite the people already on Sync Space."
            : "Sign in to open your spaces."}
        </p>
        <form
          onSubmit={onSubmit}
          className="mt-8 space-y-4 rounded-3xl border border-line bg-paper p-6 shadow-[0_20px_50px_-30px_rgba(28,25,23,0.45)]"
        >
          {isRegister ? (
            <label className="block text-sm font-medium text-ink">
              Name
              <input
                required
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                className="mt-1 w-full rounded-xl border border-line bg-white px-3 py-2.5 outline-none focus:border-accent"
              />
            </label>
          ) : null}
          <label className="block text-sm font-medium text-ink">
            Email
            <input
              required
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="mt-1 w-full rounded-xl border border-line bg-white px-3 py-2.5 outline-none focus:border-accent"
            />
          </label>
          <label className="block text-sm font-medium text-ink">
            Password
            <input
              required
              type="password"
              minLength={8}
              autoComplete={isRegister ? "new-password" : "current-password"}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-1 w-full rounded-xl border border-line bg-white px-3 py-2.5 outline-none focus:border-accent"
            />
          </label>
          {error ? <p className="text-sm text-red-700">{error}</p> : null}
          <button
            type="submit"
            disabled={pending}
            className="w-full rounded-xl bg-accent px-4 py-2.5 font-medium text-accent-ink disabled:opacity-60"
          >
            {pending ? "Please wait…" : isRegister ? "Create account" : "Sign in"}
          </button>
        </form>
        <p className="mt-4 text-sm text-muted">
          {isRegister ? "Already have an account?" : "New here?"}{" "}
          <Link
            href={isRegister ? "/login" : "/register"}
            className="font-medium text-ink underline-offset-4 hover:underline"
          >
            {isRegister ? "Sign in" : "Create an account"}
          </Link>
        </p>
      </div>
    </main>
  );
}
