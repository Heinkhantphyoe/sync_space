export type Role = "OWNER" | "MEMBER";

export type User = {
  id: string;
  email: string;
  displayName: string;
};

export type AuthResponse = {
  token: string;
  user: User;
};

export type InviteOutcome = {
  status: "JOINED" | "PENDING";
  board: Board;
};

export type PendingInvite = {
  id: string;
  email: string;
  createdAt: string;
};

export type SpaceSummary = {
  id: string;
  name: string;
  role: Role;
  memberCount: number;
};

export type Assignee = {
  userId: string;
  displayName: string;
};

export type TaskLabel = "BUG" | "FEATURE" | "DESIGN";

export type TaskPriority = "LOW" | "MEDIUM" | "HIGH";

export type Task = {
  id: string;
  columnId: string;
  title: string;
  description: string | null;
  position: number;
  createdBy: string;
  updatedAt: string;
  assignees: Assignee[];
  priority: TaskPriority;
  labels: TaskLabel[];
  dueOn: string | null;
};

export type Column = {
  id: string;
  name: string;
  position: number;
  tasks: Task[];
};

export type Member = {
  userId: string;
  displayName: string;
  email: string;
  role: Role;
};

export type BoardState = {
  spaceId: string;
  spaceName: string;
  revision: number;
  columns: Column[];
  members: Member[];
};

export type Board = BoardState & {
  role: Role;
};

export type BoardEvent = {
  type: string;
  board: BoardState;
};

export type TaskActivityKind =
  | "CREATED"
  | "MOVED"
  | "RENAMED"
  | "DESCRIPTION_CHANGED"
  | "COMMENT"
  | "ASSIGNED"
  | "UNASSIGNED"
  | "LABELED"
  | "UNLABELED"
  | "PRIORITY_CHANGED"
  | "DUE_CHANGED";

export type AssignedTask = {
  id: string;
  title: string;
  priority: TaskPriority;
  dueOn: string | null;
  spaceId: string;
  spaceName: string;
  columnName: string;
};

export type TaskActivity = {
  id: string;
  kind: TaskActivityKind;
  actorId: string;
  actorName: string;
  summary: string | null;
  body: string | null;
  createdAt: string;
};
