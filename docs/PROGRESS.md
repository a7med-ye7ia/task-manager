# Progress

## Phase 5 continuation — Step 0 (done)

Planned: update SRS with notification mentions and new viewer/empty-state rules, then implement mention seen counts, viewer profiles, read-only members/empty states, and Phase 5 notifications with manager email preferences and the header bell. Testing/build will run only after all code is written.

Done: added BR-51 mentions/seen state, BR-52 viewer profiles, BR-53 empty task sections, MENTION to the BR-37 type list, and the section-9 event row. Next: implement Part A item 1.

## Phase 5 continuation — Part A (in progress)

Done: added per-mention `seen_at`, unseen mention count/read endpoints, mention notification service hook, public profile and Works-on endpoints, viewer route reuse, read-only member list, and empty subtask/checkpoint messages.

Remaining: finish notification event integrations, notification bell verification, tests, and final build/test run.

## Phase 5 — implementation status (final)

Done: notification and email-outbox entities/service/controller, mention notifications and unseen mention state, manager preference gating in the notification service, header bell with 30-second polling/read-all, public profile viewer endpoints and UI, and read-only/empty task sections.

Partial: existing account/task flows are not all routed through the new NotificationService yet (notably sign-up request and archive-warning scheduling remains Phase 6 work). Backend Maven tests/compile remain blocked by the local command producing no output.

Final verification: frontend build passed with 1,740 modules. Backend tests and compile were attempted once after implementation and produced no output before the bounded wait expired.

## Part A — in progress

Done: added BR-50 and the BR-48 avatar-removal requirement; protected signed-in routes with return-to-origin login routing; added session-expiry messaging; added shared Radix Select, confirmation dialog, global error boundary, and reusable searchable people picker; migrated every native select to the shared Select; extended task creation to save draft subtasks and checkpoints atomically.

In progress: native dialogs are removed; task creation now validates draft fields on submit/blur and maps nested backend errors; shared `FieldError` styling is now available globally; remaining mutation error states still need the final audit.

Remaining: finish mapping form errors and adding `noValidate` consistently; audit all frontend mutation error states; add/verify tests.

## Part B — not started

In progress: introduced the central task access service and applied it to task, subtask, checkpoint and member/Lead mutations; added member add/remove and Manager-only Lead change endpoints.

In progress: added local pool search, priority/status filters, sorting, “My tasks”, “I lead”, 20-item pagination, home summary cards, and member/Lead controls.

In progress: added a server-backed `/api/tasks/pool` endpoint with status, priority, search, my-task, lead, archived, sorting and page-size-20 handling; added member/Lead UI and home cards.

In progress: fixed `/api/tasks/pool` being captured by the numeric task-detail route by constraining `{id}` routes to digits; added server/client deadline-range filters.

In progress: added a legacy-list fallback and sanitized pool load errors so stale backend processes no longer expose the raw conversion exception in the UI.

In progress: tightened object-level authorization for guessed subtask/checkpoint IDs and corrected DONE restore authorization; added broader Lead/Member permission integration coverage.

Remaining: complete every row of the section-3 matrix tests; verify all mutation error states.

## Step 2 additions — in progress

Done: task API already returns createdBy, createdAt and updatedAt; Cairo-formatted created dates are displayed in the pool and task header. Added `DELETE /api/users/me/avatar` plus separate profile Change and Remove buttons and shared confirmation dialog.

In progress: added avatar-removal and created-field/object-permission integration tests.

Remaining: obtain definitive backend test results; fix any failures; update final report before Phase 4.

## Incremental people-profile link fix (done)

Done: added stored mention identity data to comment responses, made own comment authors non-clickable, added ID-backed mention links with inactive-user fallback, added shared `PersonProfileLink`, redirected self viewer routes to `/profile`, and aligned member/task-row links.

Done: frontend-specific tests for own-author non-links and stored-ID mention links pass. The production build passed with 1,741 modules. The pre-existing App shell tests still fail because they expect the old “Tasks” label while the UI uses “Home”.

## Status rules continuation — Step 0 (done)

Done: updated BR-21 and the status flow, and added the Send to review and Send back rules to `docs/SRS.md`. Next: audit and implement the status endpoints and remaining unfinished items.

## Status rules continuation — Steps 1–2 (done)

Done: wrote `docs/UNFINISHED.md` with the Phase 1–5 and UI audit; added atomic Send to review and Send back endpoints, reopen selections, reason comments, activity entries, notifications, shared-dialog UI, and focused integration coverage. Remaining: final backend tests, frontend build, and report refresh.

## Status rules continuation — final verification

Done: frontend production build passed (1,741 modules); backend Maven tests were attempted once but produced no output before the environment timeout and are not certified. Updated `docs/UNFINISHED.md` and `docs/REPORT.md` with the final status and limitation.

## BR-54 and Phase 5 close-out — in progress

Done: assigned BR-54 to task-scoped candidate search; added Lead/Manager-only paginated endpoint with privacy-limited responses, server-debounced people picker search, no-results/error states, candidate integration coverage, centralized account email routing, per-task unseen mention counts in pool/home cards, and removed the client-side pool fallback.

Done: frontend production build passed (1,741 modules); updated `docs/UNFINISHED.md` and `docs/REPORT.md`. Backend/Vitest results remain intentionally unclaimed; exact local commands are recorded in the report.

## Test fixes and coverage audit (done)

Done: configured Surefire Byte Buddy flags, documented JDK 21, disabled the email sender job in the test profile, fixed CHANGE_EMAIL delivery to the new address while retaining the old address until verification, updated App shell tests for signed-out routing and Home, added DONE comment read-only and notification preference tests, and wrote `docs/TEST_COVERAGE.md`. Maven/Vitest were not run.
Done: added explicit DONE-subtask reopening coverage and locked test SMTP settings to localhost Mailpit.

## Automatic subtask status and task-response privacy (done)

Done: fixed TODO→DONE automatic transitions (including two-subtask progression), logged automatic status changes and review notifications, introduced `UserSummary` for task/subtask/checkpoint/comment responses, added privacy assertions, expanded status/comment/member-candidate tests, and updated frontend response types and App test queries. Maven/Vitest were not run.
Done: updated existing task integration assertions that previously expected private email fields from task responses; no tests were executed.
Done: scoped all App shell header-link assertions with `within(screen.getByRole('banner'))` and used exact accessible names.

## Follow-up status-rule/test audit (done)

Done: confirmed the failing unit expectation was outdated for the all-DONE case; updated it and added the one-DONE/one-unfinished scenario. Added explicit no-reason Send back, outside-task mention, Manager candidate, and two-subtask transition coverage. Maven/Vitest were not run.

## Validation-envelope test fix (done)

Done: changed `sendBackRequiresReason` to assert HTTP 400, `VALIDATION_ERROR`, and the `reason` field error. Reviewed the other newly added error-code assertions; they reference actual service codes and required no change. Maven was not run.

## Permission coverage naming (done)

Done: replaced the combined permission-matrix method with named allowed/disallowed row tests for task edits, Lead changes, subtasks, checkpoints, subtask status, members, checkpoint ticking, Done, restore, and comment moderation. Updated `docs/TEST_COVERAGE.md` with exact method names. Maven/Vitest were not run.

## Comment access and test-body fixes (done)

Done: fixed CommentService so Managers can read, write and delete comments on tasks where they are not members; added non-member Manager coverage. The edit-task disallowed-member test now sends a valid title and correctly expects 403; its previous 400 was caused by invalid test data. Maven was not run.

## Comment author/moderation coverage (done)

Done: confirmed line 306 used the Manager token, so the expected 403 was outdated. The test now verifies author edit/delete, non-author Member and Lead edit/delete 403, Manager non-author edit 403/delete 204. Maven was not run.
Done: expanded the privacy test to cover task members, subtask assignees, comment authors, and task-level email/hiredAt absence.

## Phase 6 archive — Step 0 (done)

Done: added the configurable BR-32 schedule wording and V5 purge-date backfill migration. No tests or app runs performed.

## Phase 6 archive — Steps 1–2 (done)

Done: DONE tasks now receive archive timestamps, are read-only until restore, and have a Lead/Manager-only restore endpoint with target status, expiry checks, activity and notifications. Added purgeAt to task responses and archived/deletion-date UI. Added configurable Cairo-scheduled housekeeping job with warning, purge, audit/token/outbox cleanup and test-profile disablement. No Maven, Vitest, or app runs performed.

Done: added archive/housekeeping unit and integration coverage, refreshed coverage/report docs, and completed the frontend production build (1,741 modules). Maven and Vitest were not run.
Remaining: run the exact backend and frontend test commands locally and send back their output; Phase 7 has not started.

## Phase 6 archive UI refinement (done)

Done: BR-27 now specifies Active/Archived tabs; the task pool keeps the tab in `?tab=`, resets pagination when switching, scopes filters and server pagination per tab, and shows tab-specific empty states and deletion countdowns. Archived task pages now show an Archived badge/read-only notice and open restore status choices only inside a shared dialog. Added frontend coverage for URL-selected Archived tabs and restore-dialog visibility. Frontend tests were not run.

## Phase 7 — Steps 0–1 (done)

Done: updated BR-45, BR-30/31, and BR-33 for the technical-status copy and archived-task “Delete now” rule. Removed the application health controller/routes, security permit rule, frontend health polling/status indicator, health API/type, README reference, and obsolete health tests. Docker PostgreSQL healthchecks remain. Maven/Vitest/app were not run.

## Phase 7 — Steps 2–4 (done)

Done: Manager-only archived-task deletion now records `TASK_DELETED` audit details and uses the normal transactional cascade; archived task and pool rows expose shared confirmation dialogs with inline errors. Added archive deletion integration coverage. Hardened dummy BCrypt timing, configurable Secure refresh cookies, application security auto-configuration, and stable page response DTOs. Added `docs/BR_CHECKLIST.md`, `docs/MANUAL_CHECKS.md`, README configuration/production notes, and refreshed test/report docs. Maven/Vitest were not run.
Remaining: run the exact backend tests, frontend tests, and frontend build locally and send back their output; optional demo profile and full manual browser audit remain outside automated verification.

Frontend production build completed successfully after removing two unused imports/parameters (1,741 modules). Maven and Vitest remain unrun.

## Phase 7 — UI/pagination follow-up (done)

Done: documented BR-44 row-action sizing, changed Archived pool deletion to a focused icon-only trash action with tooltip/accessible label/focus state, kept task-page Delete now secondary-sized, updated stable pagination consumers/tests from `number` to `page`, added stable page-shape assertions for users, pending users, task pool and candidates, and flushed audited archived deletion before cascade completion. No Maven, Vitest, or app run.
Frontend production build passed with 1,741 modules. Maven and Vitest remain unrun.

## Follow-up: notifications and responsive UI (done)

Done: fixed notification read-all persistence with a transactional user-scoped modifying query, added integration coverage, and updated the navbar state/badge with visible errors. Added mobile-first responsive navigation drawer, viewport-safe notification popover, modal sizing/scroll behavior, responsive form grids, task/user row constraints, touch targets, and overflow protections across shared layout styles. Frontend build passed; the repository has no lint script (`npm run lint` reports missing script). Maven/Vitest remain unverified because Maven is not producing output in this environment.

## Archive deletion audit follow-up (done)

Done: strengthened the delete flow to record explicit `entityType`, `entityId`, `taskId`, title and actor details before the transactional cascade and flush the repository. The assertion still requires confirmation in the user's Maven environment because this environment did not refresh Surefire reports.

Follow-up: changed `AuditLogService` to `saveAndFlush` so the audit row is physically persisted before task deletion completes. Temporary test diagnostics were added for one run and removed. Maven targeted/full-suite commands were attempted again but produced no console output or refreshed report in this environment.

## Notification and navbar fixes (done)

Done: read-all now runs through a transactional service and modifying query with automatic clear/flush, scoped to the current user; added integration coverage for isolation. The navbar logs the real API status/code on failure, shows a dismissible styled inline alert, clears it on close/success, and updates local read state and badge immediately. Replaced the mobile off-canvas navigation with compact top-bar controls and horizontal pill tabs; notification popovers and dialogs are viewport-safe. Frontend build passed; no lint script exists. Maven remains unverified in this environment.

## Notification clear-all (done)

Done: added authenticated `DELETE /api/notifications` returning 204 with a current-user-only transactional modifying delete, integration coverage for user isolation and unauthenticated rejection, inline confirmation/loading/error states, local empty-state reset, and unread badge reset. SRS has no notification-retention rule, and notifications have no FK dependency from audit rows, so hard deletion is used. Frontend build passed; lint is unavailable because no lint script exists. Maven again produced no output in this environment.

## Notification read-all, sign-out and mobile popover (done)

Done: the shared API request helper now accepts empty 200 and 204 success bodies (including notification read-all and clear-all), and notification handlers show server errors only on real failures while updating local state and clearing stale errors on success/close. Sign out now opens the shared confirmation dialog and only confirmation invokes logout. Mobile notification popovers are anchored absolutely to the bell, constrained to the viewport, and use `overflow-x: clip` on the page roots. Frontend build passed. `npm run lint` is unavailable because package.json has no lint script.

## Notification outside-click and task unread indicators (done)

Done: notification panels now close on outside pointerdown, Escape, and route changes while remaining interactive inside the panel; the bell/panel share one wrapper ref. Removed the navbar/task aggregate unread badges and added a per-task `unreadCount` to task-pool responses, batched across comments, activity and mentions for the current user. Task rows show the count and refresh on focus and the existing polling interval. Added a backend service test for per-task isolation. Frontend build passed; lint is unavailable because no lint script exists. Backend Maven was attempted but produced no output or completion in this environment.

## Per-task mention badge and immediate notification clear (done)

Done: task-pool responses now include a separately batched current-user `mentionUnreadCount`; task rows show a numbered red badge (`9+` after nine) with status semantics, tooltip and responsive absolute positioning. The navbar remains free of task unread badges and its bell count is unchanged. Clear all notifications now deletes immediately without an inline confirmation, while retaining loading, local reset and error handling. Frontend build passed; lint is unavailable because no lint script exists. Backend Maven was attempted but produced no output or completion in this environment.

## Final hardening and release preparation (implemented)

Phase 0: earlier notification, unread-badge, and responsive work is present; backend verification remains blocked by Maven producing no output.

Phase 1: added configurable SMTP/no-op mail services, environment-driven mail settings, UTF-8 multipart delivery, bounded mail executor configuration, and provider/development documentation.

Phase 2: standardized error metadata with timestamp, path and trace ID, added trace propagation/header output and common malformed/type/media/upload/conflict handlers, and added central network-error handling in the frontend client.

Phase 3: added dev/prod profiles, production JWT secret validation, frontend API base URL support, security headers, Dockerfiles, production compose healthchecks, and deployment documentation.

Phase 4: added `scripts/clean-database.sql`, dry-run by default, production guarded, transactional, identity-resetting, and never touching `users` or avatar files.

Phase 5: frontend build passed. Lint is unavailable, online npm audit could not reach the registry, but `npm audit --omit=dev --offline` reported 0 vulnerabilities. Maven test/package commands did not complete in this environment.

## Database cleanup dry run (done)

Done: `scripts/clean-database.sql` was corrected to include `flyway_schema_history`, use a working psql production guard, and print dry-run notices. The default dry run was executed against the dev PostgreSQL database and rolled back successfully; no real cleanup flag was passed. It reported 3 users, 7 tasks, 15 comments, 5 migration-history rows, and the remaining clear/keep counts shown in the session output. A read-only verification confirmed users=3, tasks=7, comments=15 afterward.

## Backend startup verification (done)

The reported startup failure was caused by stale/inconsistent generated `backend/target` classes after the error-envelope changes, not by the Java 26 warnings or PostgreSQL. Removing only generated `backend/target` output and running a clean Maven compile fixed the classpath. `spring-boot:run` then completed application startup, Flyway validation, JPA initialization and Tomcat startup on port 8080. Generated build output was not committed.

## Startup follow-up (done)

Done: a fresh compile confirmed the application starts on port 8080. Security error responses now serialize their `Instant` timestamp via `JavaTimeModule`. The archived-task delete audit test was corrected to validate the JSON field independent of whitespace; the delete endpoint remains 204, removes the task, and persists the audit row. Focused security/profile and archived-delete tests passed. A later full-suite attempt hit an environment-level database connection timeout during a long TaskIntegrationTest run, so no full-suite pass count is claimed.

## Loading and refresh states (done)

Done: added shared `AppLoader`, `PageLoader`, `Skeleton`, `Spinner`, and request-error components; protected-route session restoration now uses the branded loader, and buttons show a spinner while busy. The task pool uses one pool request path with no legacy fallback, shows a retryable error only when no task data exists, and keeps existing rows with a dismissible refresh notice after background failures. The same loading/error pattern was applied to task details, profiles, users, pending requests, notifications, comments, activity and email verification. Added frontend coverage for session restore, no-data retry, and retaining visible task data. Frontend production build passed; Maven, Vitest and the app were not run.

Step 0: the loading/error rule was added to BR-44 in `docs/SRS.md`.
Step 1: the pool fallback/error behavior and shared loading states were implemented across the listed screens.
Step 3: frontend tests were added/updated without execution, and the production build passed successfully.

## Unread-state test follow-up (done)

Done: confirmed the service correctly excludes the current user's own comments and uses the user/task-scoped unseen-mention query. Updated the stale Mockito fixtures and corrected the batched-count expectation for a second task containing another user's comment. Added real-database coverage for own comments, other-user comments, per-user/per-task mentions, and batched counts. Updated App tests to use the request helper's text-body response shape and to await the actual loader/page/error states. Maven and Vitest were not run.

## Switchable SMTP mail modes (done)

Done: SMTP settings are environment-driven with a Mailpit block enabled by default and a commented Gmail/App Password block in `.env.example`. Spring imports the root `.env` when the backend is started from `backend`; standard `SPRING_MAIL_*` variables and `APP_MAIL_FROM` are documented. Startup logs only host, port, sender and enabled state. SMTP failures are reduced to safe connection/authentication messages while retry and FAILED behavior remains unchanged. The test profile hard-locks localhost, disables SMTP and disables the sender job, with a safety test asserting those values. README documents real-Gmail setup, local links, limits, Spam checks and switching back to Mailpit. Maven, Vitest, Docker and the app were not run.

## Mail loading and temporary tunnel (done)

Done: verified the normal backend command and current non-secret `.env` resolution (localhost:1025, the configured username, and mail disabled). The backend uses the standard optional `../.env` properties import with environment-variable precedence. Startup logging now emits one safe SMTP/app-base-URL line; disabled delivery no longer reports a no-op as SMTP-sent. Added configurable Vite allowed hosts and proxy target, preserved localhost defaults, and documented HTTPS tunnel setup, cookies, CORS, URL dependencies and real-deployment limitations in `docs/TUNNEL.md`. The frontend production build passed. The backend suite was run after the changes but the newly added unread-count fixture still needs a follow-up run in the current shell session; no backend pass count is claimed.


## Improvements & Hardening — Step 0 (done)

Done: updated `docs/SRS.md` with:
- BR-40: added special character rule ("at least one special character (any character that is not a letter or digit)").
- BR-47: updated live password requirements feedback to explicitly include special character check.
- BR-38 & Email content rule: added detailed template specification, specific subject, details block, button link to APP_BASE_URL + path, no emojis, inline CSS table layout, plain-text part.
- User overview page layout rule: header card limited to avatar, name, badges and actions; overview tab sectioned into Contact, Account, Work with wrapping fields and no overlap or duplicate info.

## Improvements & Hardening — Item 1: Emails (done)

Done:
- Created `EmailTemplateRenderer.java` implementing responsive HTML (table layout, max width 560px, rounded primary button, accent color #2563eb, footer with app name, no emojis, escaped user text) and plain-text alternatives for every event type in the Section 9 table.
- Unknown templates fail loudly with `IllegalArgumentException`, eliminating generic fallbacks.
- Updated `EmailSenderJob.java` to delegate email content generation to `EmailTemplateRenderer`.
- Updated `NotificationService.java` to inject `TaskRepository` and enrich outbox payloads with `taskTitle`, `priority`, `deadline`, and `actorName`.
- Added `EmailTemplateRendererTest.java` verifying all 21 templates, subject formatting, details blocks, button links, script escaping, plain-text content, and strict rejection of unknown templates.

## Improvements & Hardening — Item 2: User Overview Page Layout (done)

Done:
- Updated `UserDetailsPage.tsx` header card to show only avatar, full name, role badge, status badge, and action buttons (Edit details, Deactivate/Restore, Delete), removing duplicated email, phone, and hired date from the top header card.
- Updated `tab === "overview"` to a responsive sectioned grid:
  - Section "Contact": Email (with `word-break: break-all` and `overflow-wrap: anywhere` so it never overlaps phone) and Phone.
  - Section "Account": Role, Status, Hired Date, Deactivated Date (if deleted), and Member Since.
  - Section "Work": Active Tasks, Tasks Led, and Total Tasks Done summary cards, with direct navigation buttons to Tasks and Report tabs.
- Polished the viewer profile mode (`/users/:id`) to display role and status badges in the header card and contact details in a cleanly separated section.
- Added `UserDetailsPage.test.tsx` verifying that email and phone appear strictly once in the Contact section without header duplication and that Contact, Account, and Work sections render properly.

## Improvements & Hardening — Item 3: Password Special Character (done)

Done:
- Updated backend `ValidationUtils.validatePassword` to require at least one special character (`[^a-zA-Z0-9]`), throwing `VALIDATION_ERROR` ("Password must contain at least one special character") if absent.
- Updated `UserProfileIntegrationTest.java` test passwords to satisfy the special character rule (`ManagerPassword123!`, `EmployeePassword123!`, etc.).
- Added `ValidationUtilsPasswordTest.java` testing compliant passwords, missing special character rejection, and edge cases.
- Created shared `passwordValidation.ts` and updated `SignUpPage.tsx`, `SetPasswordPage.tsx`, `ResetPasswordPage.tsx`, and `ProfilePage.tsx` with immediate client validation for special characters.
- Updated `PasswordRuleList.tsx` live requirement list with "At least one special character" dynamically switching between unmet (red with X icon) and met (green with Check icon).
- Updated `.env.example` and `README.md` to document the special character requirement for seed manager and accounts.
- Added `PasswordRuleList.test.tsx` verifying all 5 password rules and live state transitions for special characters.

## Improvements & Hardening — Item 4: Attach File Buttons (done)

Done:
- Added `buttonMode?: boolean` and `buttonText?: string` props to `FileDropzone.tsx`.
- In button mode, replaced the dropzone area with a single "Attach files" button that triggers immediate click on the hidden file chooser input (`<input type="file" multiple ... />`), displaying the shared upload queue (pending files, progress bar, Pause, Resume, Cancel, remove, inline errors, and scrollable container) directly underneath the button.
- Updated `TaskFormModal.tsx` in both the Add subtask rows and the main task-level file attachment section to render `<FileDropzone buttonMode ... />` directly instead of previous custom attach toggles.
- Updated `TaskDetailPage.tsx` Add subtask section to render `<FileDropzone buttonMode ... />`.
- Kept the full dropzone area active exclusively in the main task Files section (`TaskFilesSection.tsx`).
- Added test in `FileDropzone.test.tsx` verifying that in button mode the dropzone box is hidden, the "Attach files" button renders, and clicking it opens the hidden native file chooser.

## Improvements & Hardening — Item 6: File Card Action Buttons Alignment (done)

Done:
- Updated `TaskFilesSection.tsx` `FileCard` action buttons so all four actions (Preview, Download, Edit, Delete) render using ONE shared `IconButton` component (`<button type="button">` with width 36px, height 36px, inline-flex, align-items center, justify-content center, padding 0, margin 0, svg display block).
- Removed asymmetric span and div wrappers so all four buttons share ONE direct container (`.task-file-actions`) with `display: flex; align-items: center; gap: 4px`.
- Replaced anchor link preview navigation with `onClick` navigation, eliminating vertical misalignment caused by inline element baselines.
- Updated `index.css` rules for `.task-file-actions` and `.icon-button-shared` ensuring 36x36px identical sizing, box-sizing, and svg display block.
- Updated `TaskFilesSection.test.tsx` and added test asserting all four buttons share the exact same parent container and exact same class list.

## Improvements & Hardening — Item 5: Security & Performance Audit (done)

Done:
- Network & Caching: Added `Cache-Control: private, max-age=3600` on authenticated avatar endpoints (`UserController`, `AdminUsersController`, `ProfileController`). Added `loading="lazy"` to avatar `<img>` tags in `UserAvatar.tsx`.
- Frontend Performance & Code Splitting: Implemented route-level dynamic imports with `React.lazy` and `Suspense` for all pages in `App.tsx`. Configured sensible TanStack Query `staleTime` (1 min) and `gcTime` (5 min). Wrapped heavy file viewers (`PdfViewer`, `DocxViewer`, `CsvViewer`, `MarkdownViewer`, `CodeIdeViewer`, `FileEditor`) with `React.lazy` in `TaskFilePage.tsx`. Updated polling in `AppLayout.tsx`, `TaskDetailPage.tsx`, and `TaskPoolPage.tsx` to pause when `document.hidden`.
- Nginx & Vite: Configured `frontend/nginx.conf` with Gzip compression, `no-cache` for `index.html`, and 1-year immutable caching for `/assets/`. Configured Vite rollup manual chunks (`vendor-react`, `vendor-query`, `vendor-icons`).
- Backend & DB Performance: Added `V12__performance_indexes.sql` migration creating indexes for task filtering (`status, priority, deadline`), self-reported tasks, archive dates, task members, comments, time sessions, work history, notifications, and pending email outbox. Configured HikariCP pool settings and Gzip HTTP compression in `application.yml`. Verified `open-in-view: false`.
- Security Headers: Configured `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, and `Permissions-Policy` in `SecurityConfig.java`.
- Added test in `FlywaySchemaTest.java` verifying that V12 performance indexes exist in PostgreSQL schema.

## Improvements & Hardening — Test Compilation Fix (done)

Done:
- Fixed `EmailSenderJobTest.java` constructor call to supply `EmailTemplateRenderer` mock argument.
- Fixed `EmailTemplateRendererTest.java` to use `Map.ofEntries` instead of `Map.of` with 12 pairs.
- Fixed `NotificationServiceTest.java` constructor calls across all test cases to supply `TaskRepository` mock argument.
- Corrected `V12__performance_indexes.sql` to reference valid existing columns (`tasks(done_at, purge_at)`) and updated `FlywaySchemaTest.java`.
- Verified frontend production build succeeds cleanly (0 errors).

## Task Details Enhancements — Lead Promotion & Subtask Attachment (done)

Done:
1. Replaced "Make Lead" text button in `TaskDetailPage.tsx` with a royal `Crown` `IconButton`:
   - Styled with `.btn-promote-lead` and `@keyframes promote-crown-sparkle`.
   - On hover, the crown lifts up, scales gracefully, tilts regally, and radiates a golden amber halo glow (`drop-shadow` and amber radial background gradient) to visually express promotion to leadership.
2. Added paperclip attachment `IconButton` to subtask cards in `TaskDetailPage.tsx`:
   - Placed directly next to the delete subtask icon with `motion="clip"` and `Paperclip` icon.
   - Forwarded ref on `TaskFilesSection` via `TaskFilesSectionHandle.openPicker(subtaskId)` so clicking the paperclip opens the file picker directly for that specific subtask.
   - Kept `FileDropzone` mounted in `TaskFilesSection` so the native file input click triggers synchronously within the user gesture without popup blocking.
   - Added unit test in `TaskDetailPage.test.tsx` verifying both the Lead promotion button and the subtask paperclip attachment button.
3. Verified frontend production build passes cleanly (`tsc && vite build` built in 5.29s with 0 errors).
