import { clearSession, getToken } from "@/lib/auth";
import type {
  AppNotification,
  AuthResponse,
  AssignedTask,
  Board,
  InviteOutcome,
  PendingInvite,
  SpaceSummary,
  TaskActivity,
  TaskLabel,
  TaskPriority,
  User,
} from "@/lib/types";

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

export function listMyTasks() {
  return request<AssignedTask[]>("/api/me/tasks");
}

export function listNotifications() {
  return request<AppNotification[]>("/api/me/notifications");
}

export function markNotificationRead(notificationId: string) {
  return request<AppNotification>(`/api/me/notifications/${notificationId}/read`, { method: "POST" });
}

export function markAllNotificationsRead() {
  return request<void>("/api/me/notifications/read", { method: "POST" });
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
  return request<InviteOutcome>(`/api/spaces/${spaceId}/members`, {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export function listInvites(spaceId: string) {
  return request<PendingInvite[]>(`/api/spaces/${spaceId}/invites`);
}

export function cancelInvite(spaceId: string, inviteId: string) {
  return request<void>(`/api/spaces/${spaceId}/invites/${inviteId}`, { method: "DELETE" });
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

export function setTaskAssignees(spaceId: string, taskId: string, userIds: string[]) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}/assignees`, {
    method: "PATCH",
    body: JSON.stringify({ userIds }),
  });
}

export function setTaskLabels(spaceId: string, taskId: string, labels: TaskLabel[]) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}/labels`, {
    method: "PATCH",
    body: JSON.stringify({ labels }),
  });
}

export function setTaskPriority(spaceId: string, taskId: string, priority: TaskPriority) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}/priority`, {
    method: "PATCH",
    body: JSON.stringify({ priority }),
  });
}

export function updateTask(
  spaceId: string,
  taskId: string,
  draft: {
    title: string;
    description: string;
    userIds: string[];
    labels: TaskLabel[];
    priority: TaskPriority;
    dueOn: { value: string | null };
  },
) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}`, {
    method: "PATCH",
    body: JSON.stringify(draft),
  });
}

export function createCheck(spaceId: string, taskId: string, title: string) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}/checks`, {
    method: "POST",
    body: JSON.stringify({ title }),
  });
}

export function updateCheck(spaceId: string, taskId: string, checkId: string, done: boolean) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}/checks/${checkId}`, {
    method: "PATCH",
    body: JSON.stringify({ done }),
  });
}

export function deleteCheck(spaceId: string, taskId: string, checkId: string) {
  return request<Board>(`/api/spaces/${spaceId}/tasks/${taskId}/checks/${checkId}`, {
    method: "DELETE",
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
