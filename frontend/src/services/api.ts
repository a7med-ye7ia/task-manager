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
  TimeSession,
  TimeTaskOption,
  TimeSubtaskOption,
  TaskFile,
  UserReportResponse,
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
  if (!body.trim()) return undefined as T;
  const contentType = response.headers.get('content-type')?.toLowerCase() || '';
  if (contentType.includes('json')) return JSON.parse(body) as T;
  if (!contentType && /^[\[{]/.test(body.trim())) {
    try { return JSON.parse(body) as T; } catch { /* treat as text */ }
  }
  return body as unknown as T;
}

export async function getText(url: string): Promise<string> {
  return request<string>(url);
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

export async function getUserTasksApi(userId: number, from?: string, to?: string): Promise<UserTaskSummary[]> {
  const query = new URLSearchParams();
  if (from) query.set('from', from);
  if (to) query.set('to', to);
  const qStr = query.toString();
  return request<UserTaskSummary[]>(`/api/users/${userId}/tasks${qStr ? `?${qStr}` : ''}`);
}

export async function getUserReportApi(userId: number, from?: string, to?: string): Promise<UserReportResponse> {
  const query = new URLSearchParams();
  if (from) query.set('from', from);
  if (to) query.set('to', to);
  const qStr = query.toString();
  return request<UserReportResponse>(`/api/users/${userId}/report${qStr ? `?${qStr}` : ''}`);
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

export async function listTimeSessionsApi(from?: string, to?: string): Promise<import('../types').TimeRange> {
  const query = new URLSearchParams(); if (from) query.set('from', from); if (to) query.set('to', to);
  return request<import('../types').TimeRange>(`/api/time-sessions?${query}`);
}
export async function startTimeSessionApi(taskId: number, subtaskId?: number, confirmSwitch = false, confirmOvertime = false, startNote?: string): Promise<TimeSession> {
  return request<TimeSession>('/api/time-sessions/start', { method: 'POST', body: JSON.stringify({ taskId, subtaskId, confirmSwitch, confirmOvertime, startNote }) });
}
export async function pauseTimeSessionApi(id: number): Promise<TimeSession> { return request<TimeSession>(`/api/time-sessions/${id}/pause`, { method: 'POST' }); }
export async function resumeTimeSessionApi(id: number): Promise<TimeSession> { return request<TimeSession>(`/api/time-sessions/${id}/resume`, { method: 'POST' }); }
export async function stopTimeSessionApi(id: number): Promise<TimeSession> { return request<TimeSession>(`/api/time-sessions/${id}/stop`, { method: 'POST' }); }
export async function heartbeatTimeSessionApi(id: number): Promise<TimeSession> { return request<TimeSession>(`/api/time-sessions/${id}/heartbeat`, { method: 'POST' }); }
export async function addManualTimeSessionApi(data: { taskId: number; subtaskId?: number; startedAt: string; endedAt: string; note: string }): Promise<TimeSession> { return request<TimeSession>('/api/time-sessions/manual', { method: 'POST', body: JSON.stringify(data) }); }
export async function editManualTimeSessionApi(id: number, data: { taskId: number; subtaskId?: number; startedAt: string; endedAt: string; note: string }): Promise<TimeSession> { return request<TimeSession>(`/api/time-sessions/${id}/manual`, { method: 'PUT', body: JSON.stringify(data) }); }
export async function deleteManualTimeSessionApi(id: number): Promise<void> { return request<void>(`/api/time-sessions/${id}/manual`, { method: 'DELETE' }); }
export async function getTimeTaskOptionsApi(query = '', page = 0): Promise<PageResponse<TimeTaskOption>> { const params = new URLSearchParams({ query, page: String(page) }); return request<PageResponse<TimeTaskOption>>(`/api/time-sessions/task-options?${params}`); }
export async function getTimeSubtaskOptionsApi(taskId: number): Promise<TimeSubtaskOption[]> { return request<TimeSubtaskOption[]>(`/api/time-sessions/task-options/${taskId}/subtasks`); }
export async function listTaskFilesApi(taskId: number): Promise<TaskFile[]> { return request<TaskFile[]>(`/api/tasks/${taskId}/files`); }
export async function getTaskFileTokenApi(taskId:number,fileId:number):Promise<string>{return request<string>(`/api/tasks/${taskId}/files/${fileId}/view-token`);}
export function getTaskFileContentUrl(taskId:number,fileId:number,token:string):string{return `${apiBaseUrl}/api/tasks/${taskId}/files/${fileId}/content?token=${encodeURIComponent(token)}`;}
export async function getTaskFileDetailApi(taskId:number,fileId:number,subtaskId?:number):Promise<import('../types').TaskFileDetail>{return request(`/api/tasks/${taskId}/files/${fileId}/metadata${subtaskId ? `?subtaskId=${subtaskId}` : ''}`);}
export async function getTaskFileTextApi(taskId:number,fileId:number):Promise<string>{return getText(`/api/tasks/${taskId}/files/${fileId}/text`);}
export async function getTaskFileZipEntriesApi(taskId:number,fileId:number):Promise<{name:string;size:number}[]>{return request(`/api/tasks/${taskId}/files/${fileId}/zip-entries`);}
export async function uploadTaskFilesApi(taskId: number, files: File[], subtaskId?: number, onProgress?: (file: File, percent: number) => void, onAbortReady?: (abort: () => void) => void): Promise<TaskFile[]> {
  const results: TaskFile[] = [];
  for (const file of files) {
    const body = new FormData(); body.append('files', file); if (subtaskId !== undefined) body.append('subtaskId', String(subtaskId));
    const saved = await new Promise<TaskFile[]>((resolve, reject) => {
      const xhr = new XMLHttpRequest(); xhr.open('POST', `${apiBaseUrl}/api/tasks/${taskId}/files`); xhr.withCredentials = true; xhr.timeout = 120000; onAbortReady?.(() => xhr.abort());
      if (currentAccessToken) xhr.setRequestHeader('Authorization', `Bearer ${currentAccessToken}`);
      xhr.upload.onprogress = (event) => { if (event.lengthComputable) onProgress?.(file, Math.max(1, Math.round(event.loaded / event.total * 100))); };
      xhr.onload = () => { let payload: any = undefined; try { payload = xhr.responseText ? JSON.parse(xhr.responseText) : undefined; } catch { /* handled below */ } if (xhr.status >= 200 && xhr.status < 300) resolve(payload as TaskFile[]); else reject(new ApiError({ status: xhr.status, code: payload?.code || 'FILE_UPLOAD_FAILED', message: payload?.message || 'File could not be uploaded.', fieldErrors: payload?.fieldErrors || [], traceId: payload?.traceId })); };
      xhr.onerror = () => reject(new ApiError({ status: 0, code: 'NETWORK_ERROR', message: 'Cannot reach the server. Check your connection.', fieldErrors: [] }));
      xhr.ontimeout = () => reject(new ApiError({ status: 0, code: 'UPLOAD_TIMEOUT', message: 'The upload timed out. Try again.', fieldErrors: [] }));
      xhr.onabort = () => reject(new ApiError({ status: 0, code: 'UPLOAD_CANCELLED', message: 'Upload was cancelled.', fieldErrors: [] })); xhr.send(body);
    });
    results.push(...(saved || [])); onProgress?.(file, 100);
  }
  window.dispatchEvent(new CustomEvent('task-files-changed', { detail: { taskId } }));
  return results;
}
export async function deleteTaskFileApi(taskId: number, fileId: number): Promise<void> { return request<void>(`/api/tasks/${taskId}/files/${fileId}`, { method: 'DELETE' }); }
export async function renameTaskFileApi(taskId: number, fileId: number, name: string): Promise<TaskFile> { return request<TaskFile>(`/api/tasks/${taskId}/files/${fileId}`, { method: 'PATCH', body: JSON.stringify({ name }) }); }
export async function editTaskFileContentApi(taskId: number, fileId: number, content: string, expectedUpdatedAt?: string): Promise<TaskFile> { return request<TaskFile>(`/api/tasks/${taskId}/files/${fileId}/content`, { method: 'PUT', body: JSON.stringify({ content, expectedUpdatedAt }) }); }
export async function downloadTaskFileApi(taskId: number, fileId: number): Promise<Blob> { const response = await fetch(`${apiBaseUrl}/api/tasks/${taskId}/files/${fileId}/download`, { headers: currentAccessToken ? { Authorization: `Bearer ${currentAccessToken}` } : {}, credentials: 'include' }); if (!response.ok) throw new ApiError({ status: response.status, code: 'FILE_DOWNLOAD_FAILED', message: 'File could not be downloaded', fieldErrors: [] }); return response.blob(); }

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
