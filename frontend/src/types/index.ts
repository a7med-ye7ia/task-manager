export type Role = 'MANAGER' | 'EMPLOYEE';

export type UserStatus =
  | 'INVITED'
  | 'PENDING_EMAIL'
  | 'PENDING_APPROVAL'
  | 'ACTIVE'
  | 'REJECTED'
  | 'DISABLED';

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  status: number;
  code: string;
  message: string;
  fieldErrors?: FieldError[];
  timestamp?: string;
  path?: string;
  traceId?: string;
}

export interface ManagerEmailPrefs {
  emailsEnabled: boolean;
  onSignupRequest: boolean;
  onTaskCompleted: boolean;
  onArchiveWarning: boolean;
}

export interface NotificationItem { id: number; type: string; taskId?: number | null; message: string; read: boolean; createdAt: string; }
export interface PublicProfile { id: number; firstName: string; lastName: string; phone: string; role: Role; avatarPath?: string | null; active: boolean; }

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: Role;
  status: UserStatus;
  avatarPath?: string | null;
  hiredAt?: string | null;
  deactivatedAt?: string | null;
  deletedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}
export interface UserSummary { id: number; firstName: string; lastName: string; avatarPath?: string | null; role: Role; }

export interface UserProfileResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: Role;
  status: UserStatus;
  avatarPath?: string | null;
  hiredAt?: string | null;
  emailPreferences?: ManagerEmailPrefs | null;
  emailPrefs?: ManagerEmailPrefs | null;
}

export interface LoginResponse {
  accessToken: string;
  user: User;
  managerEmailPrefs?: ManagerEmailPrefs | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export interface PendingUserResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: Role;
  status: UserStatus;
  createdAt?: string;
  hiredAt?: string | null;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE';
export type SubtaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface TaskMember { user: UserSummary; role: 'LEAD' | 'MEMBER'; }
export interface Checkpoint { id: number; taskId: number; subtaskId?: number | null; title: string; isDone: boolean; position: number; }
export interface Subtask {
  id: number; taskId: number; title: string; description?: string | null; deadline: string;
  status: SubtaskStatus; progress: number; assignees: UserSummary[]; checkpoints: Checkpoint[];
}
export interface Task {
  id: number; title: string; description?: string | null; deadline: string; deadlineManual: boolean;
  overdue: boolean; priority: TaskPriority; status: TaskStatus; progress: number; lead?: UserSummary | null;
  members: TaskMember[]; subtasks: Subtask[]; checkpoints: Checkpoint[];
  createdAt?: string; updatedAt?: string; createdBy?: UserSummary | null; doneAt?: string | null; purgeAt?: string | null;
  mentionUnreadCount?: number;
  unreadCount?: number;
}
export interface MentionedUser { id: number; firstName: string; lastName: string; avatarPath?: string | null; role?: Role; active: boolean; }
export interface CommentResponse { id: number; taskId: number; subtaskId?: number | null; authorId?: number | null; authorName: string; authorAvatarPath?: string | null; author?: UserSummary | null; body: string; deleted: boolean; edited: boolean; createdAt: string; editedAt?: string | null; mentionedUserIds?: number[]; mentionedUsers?: MentionedUser[]; }
export interface TaskActivityResponse { id: number; taskId: number; actorId?: number | null; action: string; details?: string | null; createdAt: string; }
export interface UnreadResponse { chatUnread: boolean; activityUnread: boolean; chatCount: number; activityCount: number; }
export interface UserTaskSummary {
  id: number; title: string; status: TaskStatus; priority: TaskPriority; deadline: string;
  overdue: boolean; userRole: 'LEAD' | 'MEMBER'; progress: number;
}
