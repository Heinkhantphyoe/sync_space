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

export type Task = {
  id: string;
  columnId: string;
  title: string;
  description: string | null;
  position: number;
  createdBy: string;
  updatedAt: string;
  assignees: Assignee[];
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
  | "UNASSIGNED";

export type TaskActivity = {
  id: string;
  kind: TaskActivityKind;
  actorId: string;
  actorName: string;
  summary: string | null;
  body: string | null;
  createdAt: string;
};
