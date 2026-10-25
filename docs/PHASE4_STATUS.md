# Phase 4 status

## Scope and planned files

- [x] Step 1 — comments persistence and visibility service: entity, repository, DTOs, service, and read/write visibility checks are implemented.
- [x] Step 2 — comment API, edit/delete permissions: task comment endpoints and ownership/manager checks are implemented. Sent-back reason comment coverage remains in the next backend step.
- [x] Step 3 — activity API and data mapping: activity endpoint and response mapping are implemented.
- [x] Step 4 — frontend comments, subtask selector, ten-second polling, activity tab, assignee confirmation flow, and task-pool layout are implemented.

## Requested compatibility fixes

- [x] Fix 1 backend — subtask create/edit/assignee APIs require `confirmAddMember` for outside-task users and add member plus assignee atomically.
- [x] Fix 2 — reorganize the task-pool filters and rows using page-specific CSS/grid layout without changing server filtering.

## Rules

- [x] BR-28 backend — visibility, plain-text length, edit/delete rules, and sent-back reason comments are enforced.
- [x] BR-29 activity: read-only activity tab backed by the existing activity log and the same visibility checks.

## Verification

- Frontend build: passed after one JSX and one target-library correction.
- Backend test command: started but produced no output and did not complete in the available run window; no backend result is claimed.

## Rework plan

- [x] Part 1 — comment-save handling now surfaces the backend reason, and integration coverage covers member/Lead/Manager success plus non-member rejection. The original runtime could not be reproduced because the local backend process produced no HTTP/log output.
- [x] Part 2 — task-page Comments/Activity tabs are replaced with responsive Chat and Activity side drawers; Escape and close controls are supported.
- [x] Part 3 — Members uses the existing people-picker search and confirmation behavior while retaining server permissions.
- [x] Final — SRS wording updated once, report updated, frontend build passed (1,738 modules), and no emoji characters were found in frontend/backend source. Maven and Vitest still do not complete in this environment.

## Chat refinements

- [x] Part 1 — floating chat button/panel replaces the header Chat action; activity remains in the header, both surfaces are above the navbar, and unread polling/read controls are implemented.
- [x] Part 2 — message actions are author-only on the server, activity entries are recorded, and integration coverage includes author success plus Manager rejection.
- [x] Part 3 — task read-position persistence, unread endpoints, migration, service test, final SRS wording, and verification are complete.

## Chat refinements 2

- [x] Part 1 — move Chat from the floating button into a route-scoped navbar slot and preserve drawer exclusivity/layering. Files: `AppLayout.tsx`, `TaskDetailPage.tsx`, `ChatDrawer.tsx`, and `task-drawer.css`.
- [x] Part 2 — return real author avatars, subtask chips, and verified @mentions. Files: `CommentMention.java`, repository, mention DTOs, `CommentService.java`, `TaskController.java`, `CommentResponse.java`, `CommentThread.tsx`, `api.ts`, `types/index.ts`, `V3__comment_mentions.sql`, and integration tests.
- [x] Part 3 — verify pool/read-only behavior and profile avatar menu refinements. Files: `TaskPoolPage.tsx`, `TaskDetailPage.tsx`, `ProfilePage.tsx`, page-specific styles, and tests. Original-image retention remains an assumption: the current crop endpoint re-crops the stored avatar representation.
- [x] Part 4 — update SRS/report, complete the BR-28/BR-29 checklist, run clean backend/frontend verification, scan for emojis, and commit.
