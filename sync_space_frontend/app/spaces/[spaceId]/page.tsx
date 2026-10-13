"use client";

import { use } from "react";

import { AppShell } from "@/components/app-shell";
import { BoardScreen } from "@/components/board-screen";

export default function SpacePage({
  params,
  searchParams,
}: {
  params: Promise<{ spaceId: string }>;
  searchParams: Promise<{ task?: string | string[] }>;
}) {
  const { spaceId } = use(params);
  const query = use(searchParams);
  const task = Array.isArray(query.task) ? query.task[0] : query.task;
  return (
    <AppShell activeSpaceId={spaceId}>
      <BoardScreen key={spaceId} spaceId={spaceId} initialTaskId={task} />
    </AppShell>
  );
}
