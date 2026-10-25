# Phase and rule audit

This audit records the implementation state before the status-rule continuation. File references are the proof locations.

## Phase 1

- [x] Authentication, sign-up approval, password reset and profile: Done — `backend/src/main/java/com/taskmanagesys/auth`, `backend/src/main/java/com/taskmanagesys/user`, `frontend/src/pages/LoginPage.tsx`, `SignUpPage.tsx`, `ProfilePage.tsx`.
- [x] Avatar upload/removal and default avatar: Done — `AvatarService.java`, `CurrentUserController.java`, `ProfilePage.tsx`.
- [x] User administration and shared validation errors: Done — `UserController.java`, `UserModal.tsx`, `FieldError.tsx`.

## Phase 2 (BR-14 to BR-26)

- [x] BR-14 to BR-20 deadline, members, subtasks and checkpoints: Done — `TaskService.java`, `SubtaskService.java`, `TaskRulesService.java`, `TaskFormModal.tsx`.
- [x] BR-21 status and progress rules: Done — `TaskService.java`, `SubtaskService.java`, `TaskDetailPage.tsx`, `docs/SRS.md` (the new Send to review/Send back endpoints are implemented in this continuation).
- [x] BR-22 to BR-26 progress, edit/delete and activity logging: Done — task services, `CheckpointService.java`, `TaskActivityService.java`.

## Phase 3 (BR-1 to BR-5, BR-24, BR-27, BR-42)

- [x] Lead/member role rules and server object checks: Done — `TaskAccessService.java`, `TaskService.java`, `SubtaskService.java`, `CheckpointService.java`.
- [x] BR-27 pool filters, search, sort and page size 20: Done — `TaskController.java`, `TaskService.java`, `TaskPoolPage.tsx`, `task-pool.css`.
- [x] Read-only non-member task view and home summaries: Done — `TaskDetailPage.tsx`, `HomePage.tsx`, `TaskPoolPage.tsx`.
- [ ] BR-42 complete row-by-row permission certification: Partial — integration coverage is in `TaskIntegrationTest.java`; the final Maven test process produced no output and was stopped after the environment timeout.

## Phase 4 (BR-28 to BR-29)

- [x] Chat comments, edit/delete, subtask selector and ten-second polling: Done — `CommentService.java`, `CommentController` routes in `TaskController.java`, `CommentThread.tsx`, `ChatDrawer.tsx`.
- [x] Activity drawer and unread state: Done — `TaskActivityService.java`, `ActivityDrawer.tsx`, task read-state services.

## Phase 5 (BR-35 to BR-42 and BR-51 to BR-53)

- [x] Notification event coverage: Done for Phase 1–5 account/task events — `NotificationService.java`, `AuthService.java`, `UserService.java`, `UserAdminService.java`; archive-warning scheduling remains Phase 6.
- [x] Mentions, unseen counts and public profiles: Done — `MentionService.java`, `CommentMention.java`, `PersonProfileLink.tsx`, `UserDetailsPage.tsx`.

## UI and extra audits

- [x] Browser dialogs: Done — no `window.alert`, `window.confirm`, `window.prompt`, or bare dialog calls remain under `frontend/src`.
- [x] Native selects: Done — no `<select>` elements remain under `frontend/src`; pages use `Select.tsx`.
- [x] Form `noValidate`: Done — every frontend `<form>` currently has `noValidate`.
- [x] Emoji policy: Done — no emoji characters found under `frontend/src`.
- [x] Mutation error-state audit: Done — checked task status/member/subtask/checkpoint/comment mutations, avatar/profile/password/email flows, sign-up/admin invite/approval/rejection, notification read actions, and pool/task loading. Each has a red inline/page error or an intentional read-only failure path; raw backend text is not rendered.
- [x] Send to review and Send back rules: Done — `TaskService.java`, `SendBackTaskRequest.java`, `TaskController.java`, `TaskDetailPage.tsx`, `services/api.ts`; backend test certification remains in the final verification step.

## Remaining verification

- [x] Add focused status integration tests and attempt the complete backend test suite — `TaskIntegrationTest.java`; Maven produced no output before timeout.
- [x] Run the frontend production build after all edits — passed, 1,741 modules.
- [x] Update `docs/REPORT.md` with final counts, assumptions and any unresolved legacy notification coverage.

## Current continuation

- [x] BR-54 task-scoped member-candidate search — `MemberCandidateResponse.java`, `UserRepository.java`, `TaskService.java`, `TaskController.java`, `PeoplePicker.tsx`, `TaskDetailPage.tsx`.
- [x] Per-task unseen mention counts in server pool rows and home cards — `TaskResponse.java`, `TaskService.java`, `TaskPoolPage.tsx`.
- [x] Server-only pool pagination — `TaskController.java`, `TaskService.java`, `TaskPoolPage.tsx` (legacy client fallback removed).
