import { Client } from "@stomp/stompjs";

import { getToken } from "@/lib/auth";
import type { BoardEvent } from "@/lib/types";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

function wsUrl() {
  return API_URL.replace(/^http/, "ws");
}

export function connectBoardSocket(
  spaceId: string,
  onEvent: (event: BoardEvent) => void,
  onLive: (live: boolean) => void,
) {
  const token = getToken();
  if (!token) {
    onLive(false);
    return () => {};
  }

  const client = new Client({
    brokerURL: `${wsUrl()}/ws?token=${encodeURIComponent(token)}&spaceId=${encodeURIComponent(spaceId)}`,
    reconnectDelay: 2000,
    onConnect: () => {
      onLive(true);
      client.subscribe(`/topic/spaces/${spaceId}`, (message) => {
        onEvent(JSON.parse(message.body) as BoardEvent);
      });
    },
    onDisconnect: () => onLive(false),
    onWebSocketClose: () => onLive(false),
    onStompError: () => onLive(false),
  });
  client.activate();

  return () => {
    void client.deactivate();
  };
}
