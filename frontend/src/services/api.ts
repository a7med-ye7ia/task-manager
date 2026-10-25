import {
  LoginResponse,
  UserProfileResponse,
  User,
  PageResponse,
  PendingUserResponse,
  ManagerEmailPrefs,
  ApiErrorResponse,
  Task,
  TaskPriority,
  TaskStatus,
  Subtask,
  SubtaskStatus,
  Checkpoint,
  UserTaskSummary,
  CommentResponse,
  TaskActivityResponse,
  UnreadResponse,
  NotificationItem,
  PublicProfile,
} from '../types';

let currentAccessToken: string | null = localStorage.getItem('accessToken');
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

export function setAccessToken(token: string | null) {
  currentAccessToken = token;
  if (token) {
    localStorage.setItem('accessToken', token);
  } else {
    localStorage.removeItem('accessToken');
  }
}

export function getAccessToken(): string | null {
  return currentAccessToken;
}

async function parseResponseBody<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }
  const body = await response.text();
  return body.trim() ? (JSON.parse(body) as T) : (undefined as T);
}

export class ApiError extends Error {
  status: number;
  code: string;
  fieldErrors?: { field: string; message: string }[];
  traceId?: string;

  constructor(errorResponse: ApiErrorResponse) {
    super(errorResponse.message || 'An unexpected error occurred');
    this.name = 'ApiError';
    this.status = errorResponse.status;
    this.code = errorResponse.code;
    this.fieldErrors = errorResponse.fieldErrors;
    this.traceId = errorResponse.traceId;
  }

  getFormattedMessage(): string {
    if (this.fieldErrors && this.fieldErrors.length > 0) {
      const details = this.fieldErrors.map((f) => `${f.field}: ${f.message}`).join(', ');
      return this.message ? `${this.message} (${details})` : details;
    }
    return this.message || 'Request failed';
  }
}

export async function request<T>(
  url: string,
  options: RequestInit = {},
  retryOn401 = true
): Promise<T> {
  const requestUrl = `${apiBaseUrl}${url}`;
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (currentAccessToken) {
    headers.set('Authorization', `Bearer ${currentAccessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(requestUrl, {
      ...options,
      headers,
      credentials: 'include', // Ensures httpOnly refreshToken cookie travels
    });
  } catch {
    throw new ApiError({
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'Cannot reach the server. Check your connection.',
      fieldErrors: [],
    });
  }

  if (response.status === 401 && retryOn401 && !url.includes('/api/auth/')) {
    try {
      const refreshed = await refreshAuthToken();
      if (refreshed.accessToken) {
        setAccessToken(refreshed.accessToken);
        headers.set('Authorization', `Bearer ${refreshed.accessToken}`);
        const retryResponse = await fetch(requestUrl, {
          ...options,
          headers,
          credentials: 'include',
        });
        if (!retryResponse.ok) {
          let errJson: any = {};
          try {
            errJson = await retryResponse.json();
          } catch {
            // ignore
          }
          const message =
            errJson.message ||
            errJson.detail ||
            errJson.error ||
            retryResponse.statusText ||
            `Request failed with status ${retryResponse.status}`;

          throw new ApiError({
            status: retryResponse.status,
            code: errJson.code || 'UNKNOWN_ERROR',
            message,
            fieldErrors: errJson.fieldErrors || [],
            traceId: errJson.traceId,
          });
        }
        return parseResponseBody<T>(retryResponse);
      }
    } catch {
      setAccessToken(null);
      sessionStorage.setItem('authNotice', 'Your session expired. Sign in again.');
      window.dispatchEvent(new CustomEvent('auth:expired'));
    }
  }

  if (!response.ok) {
    let errorJson: any = {};
    try {
      errorJson = await response.json();
    } catch {
      // ignore
    }

    const message =
      errorJson.message ||
      errorJson.detail ||
      errorJson.error ||
      response.statusText ||
      `Request failed with status ${response.status}`;

    throw new ApiError({
      status: response.status,
      code: errorJson.code || 'UNKNOWN_ERROR',
      message,
      fieldErrors: errorJson.fieldErrors || [],
      traceId: errorJson.traceId,
    });
  }

  return parseResponseBody<T>(response);
}

// Auth
export async function loginApi(email: string, password: string): Promise<LoginResponse> {
  const res = await request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  setAccessToken(res.accessToken);
  return res;
}

export async function signUpApi(data: {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  password: string;
}): Promise<{ message: string }> {
  return request<{ message: string }>('/api/auth/signup', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function verifyEmailApi(token: string): Promise<{ message: string }> {
  return request<{ message: string }>(`/api/auth/verify-email?token=${encodeURIComponent(token)}`, {
    method: 'GET',
  });
}

export async function setPasswordApi(
  token: string,
  password: string,
  confirmPassword?: string
): Promise<{ message: string }> {
  return request<{ message: string }>('/api/auth/set-password', {
    method: 'POST',
    body: JSON.stringify({ token, password, confirmPassword }),
  });
}

export async function forgotPasswordApi(email: string): Promise<{ message: string }> {
  return request<{ message: string }>('/api/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export async function resetPasswordApi(
  token: string,
  newPassword: string,
  confirmPassword?: string
): Promise<{ message: string }> {
  return request<{ message: string }>('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword, confirmPassword }),
  });
}

export async function refreshAuthToken(): Promise<{ accessToken: string }> {
  return request<{ accessToken: string }>('/api/auth/refresh', {
    method: 'POST',
  }, false);
}

export async function logoutApi(): Promise<void> {
  try {
    await request('/api/auth/logout', { method: 'POST' }, false);
  } finally {
    setAccessToken(null);
  }
}

// Profile
export async function getProfileApi(): Promise<UserProfileResponse> {
  return request<UserProfileResponse>('/api/profile');
}

export async function updateProfileApi(data: {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
}): Promise<{ message: string; emailChanged: boolean }> {
  return request<{ message: string; emailChanged: boolean }>('/api/profile', {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function changePasswordApi(
  currentPassword: string,
  newPassword: string,
  confirmPassword?: string
): Promise<{ message: string }> {
  return request<{ message: string }>('/api/profile/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword, confirmPassword }),
  });
}

export async function uploadAvatarApi(file: File): Promise<{ message: string; avatarPath: string }> {
  const formData = new FormData();
  formData.append('file', file);
  return request<{ message: string; avatarPath: string }>('/api/profile/avatar', {
    method: 'POST',
    body: formData,
  });
}
export async function removeAvatarApi(): Promise<User> { return request<User>('/api/users/me/avatar', { method: 'DELETE' }); }
export async function getNotificationsApi(): Promise<NotificationItem[]> { return request<NotificationItem[]>('/api/notifications'); }
export async function getNotificationUnreadCountApi(): Promise<number> { return request<number>('/api/notifications/unread-count'); }
export async function markNotificationReadApi(id: number): Promise<void> { await request<void>(`/api/notifications/${id}/read`, { method: 'POST' }); }
export async function markAllNotificationsReadApi(): Promise<void> { await request<void>('/api/notifications/read-all', { method: 'POST' }); }
export async function clearAllNotificationsApi(): Promise<void> { await request<void>('/api/notifications', { method: 'DELETE' }); }
export async function getMentionUnreadCountApi(): Promise<{ count: number }> { return request<{ count: number }>('/api/mentions/unread'); }
export async function getPublicProfileApi(id: number): Promise<PublicProfile> { return request<PublicProfile>(`/api/users/${id}/public`); }

export async function updateEmailPreferencesApi(
  prefs: ManagerEmailPrefs
): Promise<{ message: string }> {
  return request<{ message: string }>('/api/profile/email-preferences', {
    method: 'PUT',
    body: JSON.stringify(prefs),
  });
}

// Admin Users
export async function getAdminUsersApi(params: {
  page?: number;
  size?: number;
  search?: string;
  role?: string;
  status?: string;
  includeDeleted?: boolean;
}): Promise<PageResponse<User>> {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', params.page.toString());
  if (params.size !== undefined) query.set('size', params.size.toString());
  if (params.search) query.set('search', params.search);
  if (params.role) query.set('role', params.role);
  if (params.status) query.set('status', params.status);
  if (params.includeDeleted) query.set('includeDeleted', 'true');

  return request<PageResponse<User>>(`/api/admin/users?${query.toString()}`);
}

export async function getAdminUserApi(id: number): Promise<User> {
  return request<User>(`/api/admin/users/${id}`);
}

export async function inviteUserApi(data: {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: 'EMPLOYEE' | 'MANAGER';
  hiredAt?: string;
}): Promise<User> {
  return request<User>('/api/admin/users', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function resendInviteApi(userId: number): Promise<{ message: string }> {
  return request<{ message: string }>(`/api/admin/users/${userId}/resend-invite`, {
    method: 'POST',
  });
}

export async function editUserApi(
  userId: number,
  data: {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    role: 'EMPLOYEE' | 'MANAGER';
  }
): Promise<User> {
  return request<User>(`/api/admin/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deactivateUserApi(userId: number): Promise<{ message: string }> {
  return request<{ message: string }>(`/api/admin/users/${userId}/deactivate`, {
    method: 'POST',
  });
}

export async function deleteUserApi(userId: number): Promise<{ message: string }> {
  return request<{ message: string }>(`/api/admin/users/${userId}`, {
    method: 'DELETE',
  });
}

export async function restoreUserApi(userId: number): Promise<{ message: string }> {
  return request<{ message: string }>(`/api/admin/users/${userId}/restore`, {
    method: 'POST',
  });
}

export async function getUserTasksApi(userId: number): Promise<UserTaskSummary[]> {
  return request<UserTaskSummary[]>(`/api/users/${userId}/tasks`);
}

// Tasks
export async function getTaskPoolApi(params: { page?:number; search?:string; status?:string; priority?:string; deadlineFrom?:string; deadlineTo?:string; myTasks?:boolean; iLead?:boolean; archived?:boolean; sort?:string }): Promise<PageResponse<Task>> { const query=new URLSearchParams(); Object.entries(params).forEach(([k,v])=>{if(v!==undefined&&v!==''&&v!==false) query.set(k,String(v));}); return request<PageResponse<Task>>(`/api/tasks/pool?${query}`); }
export async function getTaskApi(id: number): Promise<Task> { return request<Task>(`/api/tasks/${id}`); }
export async function getTaskMemberCandidatesApi(taskId: number, query = "", page = 0): Promise<PageResponse<{ id: number; firstName: string; lastName: string; avatarPath?: string | null }>> {
  return request(`/api/tasks/${taskId}/member-candidates?query=${encodeURIComponent(query)}&page=${page}`);
}
export async function createTaskApi(data: { title: string; description?: string; deadline?: string; priority: TaskPriority; memberUserIds: number[]; leadUserId?: number; subtasks?: { title: string; description?: string; deadline: string; assigneeUserIds: number[] }[]; checkpoints?: { title: string }[] }): Promise<Task> {
  return request<Task>('/api/tasks', { method: 'POST', body: JSON.stringify(data) });
}
export async function updateTaskApi(id: number, data: Partial<Pick<Task, 'title' | 'description' | 'deadline' | 'priority'>>): Promise<Task> {
  return request<Task>(`/api/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) });
}
export async function deleteTaskApi(id: number): Promise<void> { return request<void>(`/api/tasks/${id}`, { method: 'DELETE' }); }
export async function changeTaskStatusApi(id: number, status: TaskStatus, reason?: string): Promise<Task> {
  return request<Task>(`/api/tasks/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status, reason }) });
}
export async function restoreTaskApi(id: number, targetStatus: TaskStatus): Promise<Task> {
  return request<Task>(`/api/tasks/${id}/restore?targetStatus=${encodeURIComponent(targetStatus)}`, { method: 'POST' });
}
export async function sendTaskToReviewApi(id: number): Promise<Task> { return request<Task>(`/api/tasks/${id}/send-to-review`, { method: 'POST' }); }
export async function sendTaskBackApi(id: number, data: { reason: string; subtaskIds: number[]; checkpointIds: number[] }): Promise<Task> { return request<Task>(`/api/tasks/${id}/send-back`, { method: 'POST', body: JSON.stringify(data) }); }
export async function createSubtaskApi(taskId: number, data: { title: string; description?: string; deadline: string; assigneeUserIds: number[]; confirmAddMember?: boolean }): Promise<Subtask> {
  return request<Subtask>(`/api/tasks/${taskId}/subtasks`, { method: 'POST', body: JSON.stringify(data) });
}
export async function updateSubtaskApi(taskId: number, subtaskId: number, data: { title?: string; description?: string; deadline?: string; status?: SubtaskStatus; assigneeUserIds?: number[]; confirmAddMember?: boolean }): Promise<Subtask> {
  return request<Subtask>(`/api/tasks/${taskId}/subtasks/${subtaskId}`, { method: 'PUT', body: JSON.stringify(data) });
}
export async function getTaskCommentsApi(taskId: number): Promise<CommentResponse[]> { return request<CommentResponse[]>(`/api/tasks/${taskId}/comments`); }
export async function createTaskCommentApi(taskId: number, data: { body: string; subtaskId?: number; mentionedUserIds?: number[] }): Promise<CommentResponse> { return request<CommentResponse>(`/api/tasks/${taskId}/comments`, { method: 'POST', body: JSON.stringify(data) }); }
export async function updateTaskCommentApi(taskId: number, commentId: number, body: string, mentionedUserIds?: number[]): Promise<CommentResponse> { return request<CommentResponse>(`/api/tasks/${taskId}/comments/${commentId}`, { method: 'PUT', body: JSON.stringify({ body, mentionedUserIds }) }); }
export async function deleteTaskCommentApi(taskId: number, commentId: number): Promise<void> { return request<void>(`/api/tasks/${taskId}/comments/${commentId}`, { method: 'DELETE' }); }
export async function getTaskActivityApi(taskId: number): Promise<TaskActivityResponse[]> { return request<TaskActivityResponse[]>(`/api/tasks/${taskId}/activity`); }
export async function getTaskUnreadApi(taskId: number): Promise<UnreadResponse> { return request<UnreadResponse>(`/api/tasks/${taskId}/unread`); }
export async function markTaskChatReadApi(taskId: number): Promise<void> { return request<void>(`/api/tasks/${taskId}/read/chat`, { method: 'POST' }); }
export async function markTaskActivityReadApi(taskId: number): Promise<void> { return request<void>(`/api/tasks/${taskId}/read/activity`, { method: 'POST' }); }
export async function deleteSubtaskApi(taskId: number, subtaskId: number): Promise<void> { return request<void>(`/api/tasks/${taskId}/subtasks/${subtaskId}`, { method: 'DELETE' }); }
export async function createCheckpointApi(taskId: number, title: string, subtaskId?: number): Promise<Checkpoint> {
  return request<Checkpoint>(`/api/tasks/${taskId}/checkpoints`, { method: 'POST', body: JSON.stringify({ title, subtaskId }) });
}
export async function toggleCheckpointApi(taskId: number, checkpointId: number): Promise<Checkpoint> { return request<Checkpoint>(`/api/tasks/${taskId}/checkpoints/${checkpointId}/toggle`, { method: 'PATCH' }); }
export async function deleteCheckpointApi(taskId: number, checkpointId: number): Promise<void> { return request<void>(`/api/tasks/${taskId}/checkpoints/${checkpointId}`, { method: 'DELETE' }); }
export async function addTaskMemberApi(taskId:number,userId:number):Promise<Task>{ return request<Task>(`/api/tasks/${taskId}/members/${userId}`,{method:'POST'}); }
export async function removeTaskMemberApi(taskId:number,userId:number):Promise<Task>{ return request<Task>(`/api/tasks/${taskId}/members/${userId}`,{method:'DELETE'}); }
export async function changeTaskLeadApi(taskId:number,userId:number):Promise<Task>{ return request<Task>(`/api/tasks/${taskId}/lead/${userId}`,{method:'PUT'}); }

// Admin Pending
export async function getAdminPendingUsersApi(
  page = 0,
  size = 20
): Promise<PageResponse<PendingUserResponse>> {
  return request<PageResponse<PendingUserResponse>>(
    `/api/admin/pending?page=${page}&size=${size}`
  );
}

export async function getAdminPendingCountApi(): Promise<{ count: number }> {
  return request<{ count: number }>('/api/admin/pending/count');
}

export async function approveSignupApi(
  userId: number,
  role: 'EMPLOYEE' | 'MANAGER' = 'EMPLOYEE'
): Promise<{ message: string }> {
  return request<{ message: string }>(`/api/admin/pending/${userId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ role }),
  });
}

export async function rejectSignupApi(
  userId: number,
  reason?: string
): Promise<{ message: string }> {
  return request<{ message: string }>(`/api/admin/pending/${userId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  });
}
