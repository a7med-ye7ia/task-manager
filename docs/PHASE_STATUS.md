# Phase 1–3 implementation status

Status is based on the current source, not on planned work.

## Phase 1

| Requirement / screen | Status | Files |
| --- | --- | --- |
| BR-6–13 authentication, account lifecycle and profiles | Partial | `backend/src/main/java/com/taskmanagesys/auth`, `frontend/src/pages/{Login,SignUp,Profile}Page.tsx` |
| BR-40–41 validation and security | Partial | `backend/src/main/java/com/taskmanagesys/common`, `frontend/src/services/api.ts` |
| Avatar upload/crop and removal (BR-48) | Partial | `AvatarCropModal.tsx`, `AvatarService.java`, `CurrentUserController.java`, `ProfilePage.tsx` |
| User list, details and pending sign-ups (BR-49) | Partial | `AdminUsersPage.tsx`, `UserDetailsPage.tsx`, `AdminPendingPage.tsx` |

## Phase 2

| Requirement / endpoint / screen | Status | Files |
| --- | --- | --- |
| BR-14 members and subtask assignees | Partial | `SubtaskService.java`, `TaskFormModal.tsx`, `TaskDetailPage.tsx` |
| BR-15–20 deadline rules | Partial | `TaskRulesService.java`, `TaskService.java`, `TaskFormModal.tsx` |
| BR-21 status flow | Partial | `TaskService.java`, `SubtaskService.java`, `TaskDetailPage.tsx` |
| BR-22–23 progress | Done | `TaskRulesService.java`, `TaskService.java` |
| BR-24–26 task/member editing and activity | Partial | `TaskService.java`, `SubtaskService.java`, `CheckpointService.java` |
| Task creation/detail screens | Partial | `TaskFormModal.tsx`, `TaskDetailPage.tsx` |

## Phase 3

| Requirement / endpoint / screen | Status | Files |
| --- | --- | --- |
| BR-1–5 task roles and Lead | Partial | `TaskAccessService.java`, `TaskService.java`, `TaskController.java` |
| Permission matrix/object checks | Partial | `TaskAccessService.java`, task controllers/services; guessed subtask/checkpoint IDs now return forbidden and DONE restore is Lead/Manager-only |
| BR-27 pool filters, sorting and pagination | Partial | `TaskPoolPage.tsx`, `TaskController.java`, `TaskService.java` (server endpoint, deadline range, archived toggle and page-size 20 added; full query efficiency still needs review) |
| Read-only task page and member management | Partial | `TaskDetailPage.tsx`, `TaskService.java` (role-aware actions and member/Lead controls added; full matrix still incomplete) |
| Home lists | Partial | `App.tsx`, `TaskPoolPage.tsx` |
| BR-42 permission integration tests | Partial | `backend/src/test/java/com/taskmanagesys/task/TaskIntegrationTest.java` now covers Lead/Member task, subtask, checkpoint and status boundaries; complete table matrix remains |
