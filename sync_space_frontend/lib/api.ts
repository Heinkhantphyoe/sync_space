import { clearSession, getToken } from "@/lib/auth";
import type { AuthResponse, Board, SpaceSummary, TaskActivity, User } from "@/lib/types";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) {
    headers.set("Content-Type", "application/json");
  }
  const token = getToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (
    response.status === 401 &&
    path !== "/api/auth/login" &&
    path !== "/api/auth/register"
  ) {
    clearSession();
    if (typeof window !== "undefined") {
      window.dispatchEvent(new Event("sync-space-unauthorized"));
    }
    throw new ApiError("Login required", 401);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const data = text ? (JSON.parse(text) as { message?: string }) : null;
  if (!response.ok) {
    throw new ApiError(data?.message ?? "Request failed", response.status);
  }
  return data as T;
}

export function register(email: string, password: string, displayName: string) {
  return request<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ email, password, displayName }),
  });
}

export function login(email: string, password: string) {
  return request<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function getMe() {
  return request<User>("/api/auth/me");
}

export function listSpaces() {
  return request<SpaceSummary[]>("/api/spaces");
}

export function createSpace(name: string) {
  return request<SpaceSummary>("/api/spaces", {
    method: "POST",
    body: JSON.stringify({ name }),
  });
}

export function renameSpace(spaceId: string, name: string) {
  return request<Board>(`/api/spaces/${spaceId}`, {
    method: "PATCH",
    body: JSON.stringify({ name }),
  });
}

export function deleteSpace(spaceId: string) {
  return request<void>(`/api/spaces/${spaceId}`, { method: "DELETE" });
}

export function inviteMember(spaceId: string, email: string) {
  return request<Board>(`/api/spaces/${spaceId}/members`, {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export function removeMember(spaceId: string, userId: string) {
  return request<Board>(`/api/spaces/${spaceId}/members/${userId}`, {
    method: "DELETE",
  });
}

export function getBoard(spaceId: string) {
  return request<Board>(`/api/spaces/${spaceId}/board`);
}

export function createColumn(spaceId: string, name: string) {
  return request<Board>(`/api/spaces/${spaceId}/columns`, {
    method: "POST",
    body: JSON.stringify({ name }),
  });
}

export function renameColumn(spaceId: string, columnId: string, name: string) {
  return request<Board>(`/api/spaces/${spaceId}/columns/${columnId}`, {
    method: "PATCH",
    body: JSON.stringify({ name }),
  });
}

export function reorderColumns(spaceId: string, columnIds: string[]) {
  return request<Board>(`/api/spaces/${spaceId}/columns/reorder`, {
    method: "POST",
    body: JSON.stringify({ columnIds }),
  });
}

export function deleteColumn(spaceId: string, columnId: string) {
  return request<Board>(`/api/spaces/${spaceId}/columns/${columnId}`, {
    method: "DELETE",
  });
}

export function createTask(spaceId: string, columnId: string, title: string) {
  return request<Board>(`/api/spaces/${spaceId}/tasks`, {
    method: "POST",
    body: JSON.stringify({ columnId, title }),
  });
}

export function updateTask(
  spaceId: string,
  taskId: string,
  title: string,
  description: string,
) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}`, {
    method: "PATCH",
    body: JSON.stringify({ title, description }),
  });
}

export function deleteTask(spaceId: string, taskId: string) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}`, {
    method: "DELETE",
  });
}

export function moveTask(
  spaceId: string,
  taskId: string,
  body: { toColumnId: string; toIndex: number },
) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}/move`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function getTaskActivity(spaceId: string, taskId: string) {
  return request<TaskActivity[]>(`/api/spaces/${spaceId}/tasks/${taskId}/activity`);
}

export function addTaskComment(spaceId: string, taskId: string, body: string) {
  return request<TaskActivity[]>(`/api/spaces/${spaceId}/tasks/${taskId}/comments`, {
    method: "POST",
    body: JSON.stringify({ body }),
  });
}

export function apiMessage(error: unknown) {
  return error instanceof ApiError ? error.message : "Something went wrong";
}
