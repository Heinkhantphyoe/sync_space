"use client";

import { use } from "react";

import { AppShell } from "@/components/app-shell";
import { BoardScreen } from "@/components/board-screen";

export default function SpacePage({ params }: { params: Promise<{ spaceId: string }> }) {
  const { spaceId } = use(params);
  return (
    <AppShell activeSpaceId={spaceId}>
      <BoardScreen key={spaceId} spaceId={spaceId} />
    </AppShell>
  );
}
