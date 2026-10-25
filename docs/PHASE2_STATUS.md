# Phase 2 Status: Tasks Core

Scope: BR-14 to BR-26, Section 11 Phase 2, Part B (Initials avatar restoration), and Part C (Users preview & User details page `/admin/users/:id`).

## Audit Checklist (Updated 2026-10-26)

### Step 1: Entities, Repositories, Deadline/Progress/Status Service Rules & Tests (BR-15 to BR-23)
- [x] Entities: `Task.java`, `TaskMember.java`, `Subtask.java`, `SubtaskAssignee.java`, `Checkpoint.java`, `TaskActivity.java` (in `backend/src/main/java/com/taskmanagesys/task/model/`) — **Done**
- [x] Repositories: `TaskRepository.java`, `SubtaskRepository.java`, `TaskMemberRepository.java`, `SubtaskAssigneeRepository.java`, `CheckpointRepository.java`, `TaskActivityRepository.java` (in `backend/src/main/java/com/taskmanagesys/task/repository/`) — **Done**
- [x] Business rule engine: `TaskRulesService.java` (deadlines BR-15 to BR-20, transitions BR-21, progress BR-22/23) — **Done**
- [x] Unit tests: `TaskRulesUnitTest.java` (in `backend/src/test/java/com/taskmanagesys/task/`) — **Done (10/10 passed)**

### Step 2: Task and Subtask API with Manager-only Permissions & Activity Log (BR-24 to BR-26)
- [x] Task & Subtask DTOs (`backend/src/main/java/com/taskmanagesys/task/dto/`) — **Done**
- [x] TaskService & SubtaskService with Manager access control (`backend/src/main/java/com/taskmanagesys/task/service/`) — **Done**
- [x] Task activity log persistence on create/edit/status/delete (`task_activity` table) — **Done**
- [x] TaskController & SubtaskController (`/api/tasks`, `/api/tasks/{taskId}/subtasks`) — **Done**
- [x] Integration tests: `TaskIntegrationTest.java` — **Done**

### Step 3: Checkpoints, Subtask Assignees & BR-14 Auto-Membership
- [x] Checkpoint API (task-level & subtask-level add, toggle done, delete) — **Done**
- [x] Subtask assignees API (add, remove) with BR-14 auto-membership on `task_members` as `MEMBER` — **Done**
- [x] Activity logging for checkpoints and assignee changes — **Done**
- [x] Integration tests covering checkpoints and BR-14 auto-membership — **Done**

### Step 4: Frontend Tasks Core UI
- [x] Navigation: Manager sees "Tasks" in navbar (`AppLayout.tsx`) — **Done**
- [x] Tasks page: Manager "New task" button, status filter, priority badges, deadline, progress bar — **Done**
- [x] Create/Edit Task modal: title, description, priority, deadline, members, optional Lead — **Done**
- [x] Task Detail page: progress, subtasks, checkpoints, status actions, edit and delete — **Done**
- [x] Subtask and checkpoint creation controls — **Done**

### Part B: Default Avatar Look
- [ ] Restore earlier initials avatar look (neutral background `#e2e8f0`, dark text `var(--text-muted)` / `#64748b`, 1px solid `var(--border)`, circular, 512x512 crop output, pencil icon on profile) in `UserAvatar.tsx` — **Missing**
- [ ] Confirm single shared component used across navbar, profile, users list, user details, and task screens — **Missing**

### Part C: Users List Actions & User Details Page (`/admin/users/:id`, Manager only)
- [x] Users list: Preview action and name link; edit/delete moved to user details — **Done**
- [ ] Backend endpoint `GET /api/users/{id}` (Manager-only, 403 for employee, 401 unauth) — **Missing**
- [ ] Backend endpoint `GET /api/users/{id}/tasks` (Manager-only, returns tasks with progress from BR-22/BR-23) — **Missing**
- [ ] Backend management endpoints: Manager cannot delete, deactivate, or change role of their own account, and last active Manager cannot be deleted or demoted — **Missing**
- [x] Frontend page `UserDetailsPage.tsx` with management actions and task summary — **Done**
- [ ] Backend tests for user details, user tasks, permissions, progress values, and guard on last active manager / self-demotion — **Missing**

### Part D: Final Verification & Docs
- [ ] Update `docs/SRS.md` with rule BR-49, updated users list description, and avatar note — **Missing**
- [ ] All tests pass & clean build — **Missing**
