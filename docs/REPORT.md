# Phase 1–4 completion report

## Final hardening and release-prep update

### Phase checklist

- Phase 0 — Earlier notification empty-body handling, per-task mention badges, outside-click behavior and responsive layout are implemented. Backend suite could not be certified because Maven produced no output or completion in this environment.
- Phase 1 — Configurable SMTP and no-op mail implementations, environment-only mail settings, UTF-8 HTML/plain-text delivery, bounded executor configuration, provider setup documentation, and mail failure-isolation unit coverage are implemented.
- Phase 2 — Stable error envelope metadata (`timestamp`, `status`, `code`, `message`, `fieldErrors`, `path`, `traceId`), trace header/MDC propagation, common error handlers, security error metadata, and network failure handling are implemented.
- Phase 3 — Dev/prod profiles, production JWT validation, frontend API base URL, security headers, Dockerfiles, production compose healthchecks and deployment documentation are implemented.
- Phase 4 — `scripts/clean-database.sql` is written but not executed. It is dry-run by default, refuses production-looking targets without an override, keeps users/preferences/avatar paths, and clears application data transactionally when explicitly confirmed.
- Phase 5 — Frontend build passed. Lint is unavailable because no lint script exists. Online npm audit was blocked by registry DNS/network access; offline `npm audit --omit=dev --offline` reported 0 vulnerabilities. Backend tests and package build remain unverified because Maven produced no output.

### Environment variables

`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `APP_BASE_URL`, `UPLOAD_DIR`, `COOKIE_SECURE`, `SERVER_PORT`, `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_FROM_NAME`, `MAIL_REPLY_TO`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_CONNECTION_TIMEOUT`, `MAIL_READ_TIMEOUT`, `MAIL_WRITE_TIMEOUT`, `ARCHIVE_CRON`, `ARCHIVE_ZONE`, and the `SEED_MANAGER_*` values are documented in `.env.example` and `DEPLOY.md`. Production must set the required secrets explicitly.

### Remaining risks and manual steps

- Run the backend suite/package on a machine where Maven and the database are available.
- Add a project lint script before release, then run it.
- Run `npm audit --omit=dev` and a vulnerability scanner with registry access.
- Smoke-test real SMTP with a test inbox, login/task/comment flows, notification read-all/clear-all, sign-out confirmation, archive restore/delete, expired sessions, and stopped-backend errors.
- Execute the cleanup SQL only after a verified backup and explicit review of its dry-run counts.
- If the backend shows the old `NoClassDefFoundError: ErrorResponse`, remove generated `backend/target` output and run a clean Maven build; the source now starts successfully after a clean compile.

### Release commands

```powershell
cd backend
.\mvnw.cmd -B test
.\mvnw.cmd -B package
cd ..\frontend
npm run build
npm run lint
npm audit --omit=dev
cd ..
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

Dry run cleanup:

```powershell
psql "$env:DATABASE_URL" -f scripts/clean-database.sql
```

Explicit destructive cleanup after backup:

```powershell
psql -v cleanup_confirm=true -v allow_prod_cleanup=true "$env:DATABASE_URL" -f scripts/clean-database.sql
```

## Status

## Phase 8 — history and time tracker

Implementation is present but not certified by test execution. V6 adds durable `work_history`, `work_history_events` and `time_sessions` tables, task/member backfill, per-user indexes and non-cascading task references. `HistoryService` is called from transactional activity, comment creation, task deletion and archive purge. `TimeSessionService` provides timer/manual endpoints, permissions, replacement timers, validation, Cairo daily-cap calculations, stale/maximum-duration recovery, audit entries and retention hooks. The frontend adds task timer controls, an idle prompt, a persistent header chip and `/my-time`.

Tests written: `TimeSessionServiceTest`, `HistoryServiceTest`, and `Phase8HousekeepingTest`, covering the listed service limits, recovery, purge independence, history events and retention ordering. Maven, Vitest and the app were not run. The frontend production build passed with 1,744 modules.

Phase 4 comments/activity refinements and Phase 5 notification/profile work are implemented. Backend Maven commands were attempted but did not complete in this environment; the frontend production build passes.

| Area | Status | Implementation |
| --- | --- | --- |
| Phase 1 authentication, account lifecycle, profile and user administration | Partial | `backend/src/main/java/com/taskmanagesys/auth`, `backend/src/main/java/com/taskmanagesys/user`, `frontend/src/pages` |
| BR-48 avatar upload/crop and removal | Partial | `AvatarCropModal.tsx`, `AvatarService.java`, `CurrentUserController.java`, `ProfilePage.tsx`; menu/remove are complete, original-image retention is not fully separated from the stored avatar representation |
| Phase 2 BR-14–20 | Partial | `TaskService.java`, `SubtaskService.java`, `TaskRulesService.java`, `TaskFormModal.tsx` |
| Phase 2 BR-21–23 | Partial | `TaskService.java`, `SubtaskService.java`, `TaskRulesService.java`, `TaskDetailPage.tsx` |
| Phase 2 BR-24–26 | Partial | task/subtask/checkpoint services and task detail UI |
| Phase 3 BR-1–5 and BR-24 | Partial | `TaskAccessService.java`, `TaskService.java`, `TaskController.java`, task detail UI |
| Phase 3 BR-27 pool | Done | `TaskPoolPage.tsx`, `TaskController.java`, `TaskService.java`, `task-pool.css` |
| BR-30 archive on DONE and read-only state | Done | `TaskService.java`, `SubtaskService.java`, `CheckpointService.java`, `CommentService.java`, `TaskDetailPage.tsx`, `TaskPoolPage.tsx` |
| BR-31 restore before purge | Done | `TaskController.java`, `TaskService.java`, `TaskDetailPage.tsx`, `TaskIntegrationTest.java` |
| BR-32 scheduled warning/purge | Done | `ArchiveHousekeepingJob.java`, `V5__backfill_task_purge_dates.sql`, `ArchiveHousekeepingJobTest.java` |
| BR-33 purge cascade and failure isolation | Partial | `ArchiveHousekeepingJob.java`, `ArchiveHousekeepingJobTest.java`; full database-cascade execution still requires the user test run |
| BR-34 housekeeping retention | Done | `ArchiveHousekeepingJob.java`, retention repositories, `application-test.yml` |
| Phase 7 archived manual deletion | Done | `TaskService.deleteTask`, `AuditLogService`, `TaskDetailPage.tsx`, `TaskPoolPage.tsx`, `TaskIntegrationTest.java` |
| Phase 7 health removal and security hardening | Done | `HealthController.java` removed, `SecurityConfig.java`, `AuthService.java`, `AppLayout.tsx`, `PageResponse.java` |
| Phase 3 read-only/member management/home | Partial | `TaskDetailPage.tsx`, `TaskPoolPage.tsx` |
| BR-42 permission tests | Partial | `TaskIntegrationTest.java`; full row-by-row certification still requires a completing Maven run |
| BR-28 comments | Done | `Comment.java`, `CommentService.java`, `TaskController.java`, `CommentThread.tsx` |
| BR-29 activity | Done | `TaskActivityService.java`, `TaskActivityResponse.java`, `ActivityTab.tsx` |
| BR-35–BR-39 notifications | Partial | `notification/model`, `notification/service/NotificationService.java`, `NotificationController.java`, `AppLayout.tsx`; core event methods, mention notifications, preference gating and bell are implemented, but some legacy callers still use older direct email paths |
| BR-51 mentions | Partial | `CommentMention.java`, `V4__comment_mention_seen.sql`, `MentionService.java`, comment endpoints, `CommentThread.tsx`, integration coverage; unseen totals and bell badges are implemented, while per-row pool badges remain to be wired |
| BR-52 public viewer profiles | Done | `PublicProfileResponse.java`, `UserController.java`, `UserDetailsPage.tsx`, `App.tsx`, profile/task links |
| BR-53 empty/read-only task sections | Done | `TaskDetailPage.tsx` |
| BR-54 task-scoped people search | Done | `MemberCandidateResponse.java`, `UserRepository.java`, `TaskService.java`, `TaskController.java`, `PeoplePicker.tsx`, `TaskDetailPage.tsx` |

## Fixed in this session

- Added the shared `FieldError` component and global `.field-error` styling; task, people-picker and Send-back validation use it.
- Added form `noValidate` to the remaining account/profile forms.
- Replaced remaining native browser dialogs with the shared confirmation/reason dialogs.
- Changed avatar controls to separate Change and Remove buttons.
- Added the BR-48 wording to `docs/SRS.md`.
- Added created-date display in Africa/Cairo formatting.
- Added avatar removal API and UI flow.
- Added pool filters, sorting, server-side 20-item pagination, home summary cards, and task member/Lead controls.
- Added a server-backed task-pool endpoint and role-aware task-detail action visibility.
- Constrained task ID routes to numeric values so `/api/tasks/pool` cannot be treated as an ID; added deadline-range pool filters.
- Added a compatibility fallback from the new pool endpoint to the legacy task list and replaced raw pool exceptions with a safe user-facing message.
- Tightened object-level checks for guessed subtask/checkpoint IDs and made restoring DONE tasks Lead/Manager-only.
- Added broader Lead/Member permission integration coverage.
- Updated `docs/PROGRESS.md` and `docs/PHASE_STATUS.md`.
- Added explicit confirmation before assigning a non-member to a subtask; confirmed assignments add the member and assignee atomically.
- Added task comments with subtask selection, edit/delete controls, ten-second polling, sent-back reason comments, and an activity tab.
- Reworked task-pool filters and rows into a responsive grid layout.
- Reworked task detail comments/activity into Chat and Activity side drawers with Escape/close controls, chat-style messages, composer behavior, and member-picker confirmation.
- Refined Phase 4 again with a floating chat button/panel, author-only press-to-reveal actions, activity logging for edits/deletes, per-user unread state, mark-read endpoints, and unread indicators.
- Moved Chat into the route-scoped navbar slot, removed the floating button, added real author avatars, subtask chips, verified mentions, mention highlighting, and mention persistence/update tests.
- Refined the pool cards and profile avatar menu while preserving server-side filtering and existing shared components.
- Added mention seen state, unseen counts/read endpoints, MENTION notifications, a notification bell with 30-second polling, public viewer profiles with Works-on filters, and read-only member/empty-state handling.

## Verification

- `frontend: npm run build` passed; TypeScript compiled and Vite transformed 1740 modules.
- Backend Maven test/compile commands produced no captured output and existing Surefire reports were not refreshed; backend tests are therefore not certified as passing.

## Status-rule continuation

- Updated BR-21 and the status-flow table with the Send to review and Send back rules.
- Added atomic `POST /api/tasks/{id}/send-to-review` and `POST /api/tasks/{id}/send-back` operations with server-side membership/Lead/Manager checks, unfinished-subtask validation, selected DONE-item reopening, reason comments, activity entries and notifications.
- Added the shared-dialog UI for checkpoint warnings, required send-back reasons and reopen selections.
- Added focused integration coverage for member/outsider review access and Manager send-back.
- Frontend build passed with 1,741 modules. The single requested backend Maven test run produced no output and was stopped after the environment timeout; no backend pass count is claimed.

## BR-54 and Phase 5 close-out

- BR-54 is implemented as `GET /api/tasks/{id}/member-candidates`: only the task Manager or Lead can call it; it returns paginated id/name/avatar candidates, excludes current members, searches active non-deleted employees and Managers, and never serializes email or admin fields. `TaskController.java`, `TaskService.java`, `UserRepository.java`, `MemberCandidateResponse.java`.
- Task-page member and subtask pickers use the endpoint with a short debounce, no-results state, and red in-picker errors. `PeoplePicker.tsx`, `TaskDetailPage.tsx`, `services/api.ts`.
- Account email events (verification, sign-up request, approval/rejection, invitation, password reset/change and email-change confirmation) now pass through `NotificationService`; manager preferences gate manager email events and the in-app notification channel is written consistently. `NotificationService.java`, `AuthService.java`, `UserService.java`, `UserAdminService.java`.
- Pool pagination is server-only with page size 20; the client fallback was removed. Pool responses include per-task unseen mention counts, rendered on task rows and home cards. `TaskService.java`, `TaskResponse.java`, `TaskPoolPage.tsx`.
- Mutation audit completed: task/member/subtask/checkpoint/comment/status, account/profile/password/email, avatar, sign-up/admin, notification-read and pool/task-loading mutations have inline/page error states and do not render raw backend text.
- Frontend production build passed (1,741 modules). Do not infer backend or Vitest results: run `cd backend; .\\mvnw.cmd -B test` and `cd frontend; npx vitest run` locally.
- Test fixes: Surefire now enables Byte Buddy experimental mode and dynamic agent loading for newer JDKs; test profile disables `EmailSenderJob` and uses localhost Mailpit settings only. The profile email-change test now verifies new-address delivery, old-address retention, and replacement after token verification. App shell tests now cover signed-out redirect/notice, public-only navigation, and signed-in Home.
- Coverage audit and focused tests are in [docs/TEST_COVERAGE.md](E:\Work\taskmanagesys\docs\TEST_COVERAGE.md), `TaskIntegrationTest.java`, `NotificationServiceTest.java`, and `UserProfileIntegrationTest.java`. No test results or passing counts are claimed because Maven and Vitest were not run here.
- Automatic subtask transitions now handle direct TODO-to-DONE, first-completion TODO-to-IN_PROGRESS, and last-completion IN_PROGRESS-to-REVIEW with automatic activity markers and reviewer notifications. Task, subtask, checkpoint and comment responses use privacy-limited `UserSummary` data; task responses no longer expose email, phone, hiredAt or deactivation fields.
- Phase 6 archive work: DONE tasks now receive `doneAt`/`purgeAt`, become read-only, and can be restored by a Lead or Manager before expiry. The scheduled Cairo job sends one warning within three days, purges expired tasks independently, and removes aged audit/token/outbox records. `V5__backfill_task_purge_dates.sql`, `ArchiveHousekeepingJob.java`, `TaskService.java`, `TaskDetailPage.tsx`, `TaskPoolPage.tsx`.

## Assumptions and open work

### Latest file-flow correction

- File cards now open images and PDFs in an authenticated in-app preview dialog with object URLs, metadata, section navigation, download and Escape/Close behavior. Non-previewable files download through the authenticated API rather than navigating to a client route.
- The existing shared `FileDropzone` is used by the Files section and task/subtask forms; pending files remain local until the parent item is saved, then use the existing queued upload path.
- Task detail keeps one page-level loader; no second task-page loading span was found.
- Frontend tests were added for image preview behavior. Vitest and Maven were not run in this session.
- Viewer routing follow-up: DOCX/CSV and the other supported viewer formats now open `TaskFilePage`; only unsupported formats use direct authenticated download.
- Subtask viewer follow-up: viewer links preserve the subtask id, and the unavailable-file state now exposes the server error instead of masking metadata/token failures as a generic 404.
- File-page type hardening: added BR-68, endpoint-specific SAMEORIGIN, UTF-8/truncation metadata, central-directory ZIP limits, content-type-aware parsing, lazy CSV/DOCX/XLSX handling, line-numbered text/JSON/TEX views, parser size fallback, and safer Markdown/DOCX rendering.
- Normal-page correction: confirmed routing was not the cause of the reported 404; removed the remaining image/PDF modal path, made preview cards and eye controls real links, separated metadata errors from viewer errors, and made cross-task file ids return 404 while unauthorized valid files return 403.

- Startup follow-up: `SecurityConfig` now registers `JavaTimeModule` for JSON error responses, and the archived-delete audit assertion accepts normal JSON whitespace. The app started successfully after rebuilding generated classes; focused tests passed. The full-suite attempt later encountered a local database connection timeout, so no full-suite count is claimed.

- Existing task response mapping already exposed `createdAt`, `updatedAt`, and `createdBy`, so no migration was added.
- Existing task response mapping exposes created fields, so no migration was added.
- The backend test process appears blocked by the local environment; no passing backend count is claimed.
- Dedicated new integration tests for the comment and outside-member flows should be added when Maven execution is available.
- Final verification: frontend build passed (1,738 modules); backend Maven tests and frontend Vitest started but did not complete. The emoji scan found only the SRS's pre-existing assumption markers, not UI/source emoji usage.
- The latest refinement verification still requires the requested clean backend restart and final test run; the environment previously left Maven/Vitest without output.
- Latest verification: `npm run build` passed with 1,739 modules. Vitest and Maven test commands were started but produced no completion output; the unsupported `--runInBand` option was also rejected by Vitest before the plain run. No passing test count is claimed.
- Phase 5 assumption: archive-warning scheduling remains intentionally outside this phase (Phase 6); all other account/task email callers are routed through `NotificationService`.
- Phase 6 assumptions: existing database foreign-key cascades cover comment mentions/read-state cleanup; restore uses a `targetStatus` query parameter; the default schedule is 02:00 Africa/Cairo and can be overridden with `app.archive.cron`/`app.archive.zone`. Maven/Vitest were not run in this session.
- Phase 7 work: removed application health endpoints and UI status polling, added Manager-only archived-task Delete now with audit logging and cascade deletion, hardened the dummy BCrypt check and refresh-cookie Secure setting, and added BR checklist/manual browser checks. Maven/Vitest were not run.
- Phase 7 documentation: `docs/BR_CHECKLIST.md` records BR-1 through BR-54 with implementation evidence; `docs/MANUAL_CHECKS.md` covers the browser walkthrough. The optional demo profile was not added.
- Follow-up: archived pool deletion is now an accessible 34px icon action; paged API consumers and tests use the stable `page`/`size`/`totalElements`/`totalPages` envelope, and archived deletion flushes the audit record before cascading removal.

## Phase 8 task picker addition

Implemented the shared server-searched task picker and assigned-subtask picker for My time filters and manual entries. Added minimal task-option DTOs, task-scoped filtering (member or subtask assignee, excluding DONE), debounced paging, task chips, task-page preselection, frontend validation and backend field-error mapping. Added written backend and frontend coverage in `TimeTaskOptionsControllerTest`, `TimeTaskOptionsServiceTest`, `TimeSessionServiceTest`, `TaskPicker.test.tsx` and `MyTimePage.test.tsx`. Maven and Vitest were not run; run the commands below locally. The frontend production build passed (1,745 modules).

## Phase 9 files

Implemented `task_files` storage with Flyway migration V7, random private names under the configured upload volume, sanitized metadata, signature/type checks, 10 MB per-file, 30-file and 150 MB task limits, authenticated list/upload/download/delete endpoints, permission enforcement, inline image/PDF versus attachment downloads, nosniff headers, activity entries, DONE read-only behavior, task/subtask deletion cleanup and daily orphan cleanup. Added the task-page Files section with grouped task/subtask files, drag/drop and picker upload, progress/error feedback, image preview and confirm-delete, plus task and subtask form file selection after save. Tests were written in `TaskFileServiceTest`, `TaskFileControllerTest` and `TaskFileCleanupTest`; they were not run.

## Phase 8/9 browser fixes

Added BR-61 shift session state (RUNNING/PAUSED/ENDED), worked-time accumulation, resume, switch/overtime checks, pause and heartbeat recovery, ten-hour limit enforcement, startup/minute recovery, and a global timer chip with pause/resume/stop controls. Added BR-62 shared `ErrorNotice`/`toUiError`, trace-aware unexpected errors, and replaced the reviewed raw error surfaces. Fixed task-options paging query stability and expanded the Files interface with drop zone, counts, grouped rows and clearer upload feedback. Root causes are recorded in `docs/ISSUES_FOUND.md`. Backend tests and Vitest were not run.
## Phase 8/9 browser follow-up

- Added BR-63–BR-65 for file rename/upload interaction, stateful timer restart notes, and the My time day-card experience.
- Files now use a clickable/keyboard-accessible drop zone, per-file progress/error rows with retry/remove, singular/plural counts, rename and delete controls, and a PATCH rename endpoint. Normal ZIP archives no longer require Office content markers; Office ZIPs still do.
- Timer responses include state, accumulated worked seconds and start notes. Same-day restarts require a 3–200 character note, and the task controls expose a shared dialog for it.
- My time no longer has a task filter and now uses a collapsed manual-entry section plus responsive day cards.
- Maven, Vitest and the app were not run. The frontend production build passed with 1,748 modules; Vite emitted only its existing large-chunk warning.

## Phase 8/9 continuation audit

- Corrected the actual Stop-control defect: `ENDED` sessions no longer render as active on the task page.
- File-upload cancellation now aborts the underlying XHR; upload requests also have a two-minute timeout and keep abort/error handling in the individual file row.
- Extended, rather than duplicated, `GET /api/time-sessions`: it returns a Cairo-date range (default last seven days, maximum 366), per-day worked/regular/overtime totals, entries, editable flags and a range summary. The My time page uses the URL range, has responsive day cards and refreshes when the shared timer state changes.
- Frontend production build passed: TypeScript plus Vite, 1,748 modules. Maven, Vitest and the app were not run, per the task instruction.

## Step 2.2–2.5 continuation

- Added distinct `FILE_VIEW_SECRET` configuration; the main profile has no fallback value and tests use a separate value from JWT.
- Added authenticated 60-second HMAC token issuance and the only anonymous route `GET /api/tasks/{taskId}/files/{fileId}/content`. `SecurityConfig` was changed only by permitting that exact GET pattern. The endpoint validates signature, expiry, task id, file id and the token user’s file-reading permission, returning 403 for invalid or expired links.
- Added byte-range responses with `206`, `Content-Range`, `nosniff`, and inline disposition only for images/PDF/MP4/WebM.
- Added authenticated file metadata/navigation, bounded sanitized ZIP entries and 1 MB text preview endpoints.
- Added token unit tests and documented the pending MockMvc/integration execution. Maven, Vitest and the app were not run.

## Step 3–5 continuation

- Step 3 frontend is implemented: shared dropzone/fill icon, file cards and filters, full-page file viewers, segmented My time ranges, and compact dialog dropzones.
- Step 4 frontend tests were written only; Vitest was not run.
- Step 5 documentation was updated with root-cause/manual checks/coverage. Frontend production build passed with 1,752 modules; only the existing large-chunk warning remains. Maven, Vitest and the app were not run.

## Final remaining-work update

Done: XLSX and DOCX viewers now load their browser libraries only when opened; CSV supports Raw/Table mode with 500-row cap; Markdown is rendered through a constrained sanitizer; failures expose ErrorNotice plus Download. Added the remaining write-only frontend tests and named the backend tests in `docs/TEST_COVERAGE.md`. The signed content path remains separate from authenticated download, and `FileViewSecurityIntegrationTest.everyOtherApiPathStillRequiresAuthentication` covers file list, metadata, ZIP, text and download 401 responses.

Partial: XLSX/DOCX conversion depends on the runtime CDN dynamic imports and was not browser-verified here; viewer tests were written but not run.

Could not verify: Maven, Vitest, backend integration execution, browser viewer behavior, signed-token expiry in a live server, and the subtask-upload browser regression.

## Latest Files UI correction

Done: missing-file pages use the shared styled 404 state; page actions use shared Buttons; upload failures are inline in the red dropzone with retry and no failed row; and file-card actions reuse checkpoint `cp-btn` styling with accessible labels, propagation guards and reduced-motion-safe icon animations. XLSX sheet selection, CSV 500-row rendering and write-only tests for the requested regressions are present.

Partial: dynamic XLSX/DOCX CDN imports and the browser appearance were not executed here. The final frontend production build passed with 1,753 modules and only the existing large-chunk warning. Vitest, Maven and the app were intentionally not run.

## Three app problems fixed: file preview, shared dropzone, and single task loader

### 1. File preview flow and route 404 resolution
- **Root cause:** `TaskFilesSection` previously executed client route navigation (`navigate('/tasks/:taskId/files/:fileId')`) on file card / preview button clicks for non-image/pdf files or when opened without proper handling. This navigated to `TaskFilePage` which rendered an unavailable / not-found state on metadata or token lookup failures.
- **In-app preview modal:** Images and PDFs open in an in-app lightbox modal (`FilePreviewDialog`) on the same page (no route changes). Features Previous/Next navigation between files in the same section, authenticated Download, file name, size, uploader, close button, and Escape key listener. Files are fetched via the authenticated client (`downloadTaskFileApi`), displayed via an object URL, and revoked on close/switch. Clear red error styling is displayed inside the viewer on failure.
- **Non-previewable files (DOCX, XLSX, PPTX, TXT, CSV, ZIP):** Have no preview. The card and the Preview button show Download only; clicking either downloads the file directly through the authenticated request with the original file name without navigating anywhere.
- **Backend headers and bugs fixed:**
  1. `inlineType` in `TaskFileService.java` was returning `inline` for MP4/WebM videos; corrected to return `inline` ONLY for images and PDFs, and `attachment` for everything else.
  2. `ContentDisposition` in `TaskFileController.java` was updated to explicitly format original filenames using RFC 5987 UTF-8 (`.filename(d.filename(), StandardCharsets.UTF_8)`), with `X-Content-Type-Options: nosniff`.
  3. `TaskFileService.list()` and `detail()` leaked subtask files to task members not assigned to that subtask; updated so non-assignee members only see task files and their assigned subtask files, while outsiders are denied with 403.

### 2. Reuse shared FileDropzone in add subtask section
- Extracted the Files section dropzone and upload queue into `FileDropzone`.
- Supports both immediate target upload (`target={{ taskId, subtaskId }}`) and unsaved pending list (`target="pending"`).
- In the add subtask section (`TaskDetailPage`), files stay in the pending list inside `FileDropzone` (with file details and removal) and upload after subtask creation via `uploadPending()` using identical progress, pause, resume, cancel, and retry UI.
- Removed duplicate upload code and simplified `TaskFilesSection` and `TaskDetailPage`.

### 3. Single loader on task details page
- Replaced `<PageLoader label="Loading task" />` (which displayed a small text span) on `TaskDetailPage` with `<TaskDetailSkeleton />` in the site's theme.
- Removed the unstyled text span from `PageLoader`. Only one themed skeleton loader renders until task data is ready.

### Verification and tests
- Frontend production build succeeded (`npm --prefix frontend run build`: 1,755 modules transformed).
- Tests added and updated:
  - `TaskFilesSection.test.tsx`: image in-app preview, non-previewable file direct download and preview button suppression, card click download.
  - `FileDropzone.test.tsx`: pending files mode, upload queue with pause/cancel, uploadPending on save.
  - `TaskDetailPage.test.tsx`: single skeleton loader renders on task page without small loading span, shared subtask dropzone rendered on loaded page.
  - `TaskFileControllerTest.java`: image/PDF inline header and non-image attachment header with nosniff and original filename.
  - `TaskFileServiceTest.java`: member visibility restricted to task and assigned subtask files, outsider denied.

Hook and nested-link hardening: moved the TaskFilePage CSV memo before all conditional returns, converted FileCard to a non-link card with separate preview links and download buttons, and removed invalid Link/Button nesting from the layout, auth/error pages, and admin user preview action.

Extended file handling was implemented: Unicode-safe original names and UTF-8 Content-Disposition, additional safe text types, UTF-8/HTML validation, fixed action slots, and atomic text-file editing with optimistic conflict metadata. Maven/Vitest were not run; the frontend build is the only requested verification step.

## File Card Action Row, Type-Specific Viewers, and Edit Mode Implementation

### 1. What was built
- **File Card Action Row (`TaskFilesSection.tsx` & `index.css`)**:
  - Reorganized all card actions into ONE flex row at the right end of the file card (`.task-file-actions`), vertically centered.
  - Four fixed-width slots (34px each) in exact order: Preview, Download, Edit, Delete. Every slot always occupies 34px (rendering an invisible `.task-file-action-placeholder` when unavailable) ensuring button positions never shift between cards.
  - Preview slot: shown for images, PDF, DOCX, CSV, TXT, LOG, MD, JSON, XML, YAML, SQL, and code; hidden (placeholder) for ZIP, XLSX, PPTX.
  - Download slot: always visible.
  - Edit slot: restored and shown for text-based types (TXT, LOG, MD, CSV, JSON, XML, YAML, SQL, JAVA, PY, JS, TS) to permitted users (uploader, Lead, Manager), hidden on DONE tasks.
  - Delete slot: shown to permitted users (uploader, Lead, Manager) on non-DONE tasks.
  - Clicking card body acts as Preview when available, otherwise Download.
  - Each card displays type-specific Lucide icon and color (no emojis), name with `dir="auto"`, size, uploader, date, and "edited" with date.

- **Type-Specific Viewers (`frontend/src/components/tasks/viewers/`)**:
  - `ImageViewer.tsx` (a): Zoom in/out, fit to screen, rotate 90°, object URL cleanup.
  - `PdfViewer.tsx` (b): Embedded viewer iframe with page navigation (Prev/Next/Page indicator), zoom controls (Zoom in, Zoom out, Fit), object URL cleanup.
  - `CsvViewer.tsx` (c): Real data table with papaparse loader and internal RFC 4180 fallback (handles quotes, UTF-8, and BOM), sticky header, row number `#` column, horizontal scroll, `dir="auto"` per cell, search box, first 1,000 rows truncation note, row and column counts.
  - `TextDocViewer.tsx` (d): Document-style paper view, comfortable typography, wrap toggle, `dir="auto"` per line, line numbers and size metadata.
  - `MarkdownViewer.tsx` (e): Two tabs (Preview and Source); Preview renders safe GFM markdown, raw HTML strictly stripped/disabled, links with `target="_blank" rel="noopener noreferrer"`; Source shows code view with line numbers.
  - `CodeIdeViewer.tsx` (f): IDE-style dark theme (`#1e1e2e`), line numbers always on the left, syntax highlighting, top bar with language badge, line count and size, monospace font, JSON pretty-print formatting and validity indicator, `dir="auto"` per line for RTL.
  - `DocxViewer.tsx` (g): Browser rendering using `docx-preview` (`renderAsync`) into DOM container with page layout, authenticated fetch via `downloadTaskFileApi` converted to ArrayBuffer, error state with Download button (never blank page or redirect).
  - `NoViewerFallback.tsx` (h): Information card with metadata and prominent Download button for XLSX, PPTX, and ZIP.

- **Edit Mode (`FileEditor.tsx` & `TaskFilePage.tsx`)**:
  - Edit button in viewer switches to code editor (for code, JSON, XML, YAML, SQL, MD) or text editor (for TXT, LOG, CSV).
  - Save, Cancel, and wrap toggle with loading state (`isSaving`).
  - Warning through shared `ConfirmDialog` when attempting to cancel, leave, or navigate away with unsaved changes.
  - PUT `/api/tasks/:taskId/files/:fileId/content` with version checking (`updatedAt`).
  - HTTP 409 conflict detection displays red banner with Reload button.
  - Displays "edited" with the date after saving in viewer header and on card.

### 2. Viewer file mapping for items a) to h)
- a) Image: `frontend/src/components/tasks/viewers/ImageViewer.tsx`
- b) PDF: `frontend/src/components/tasks/viewers/PdfViewer.tsx`
- c) CSV: `frontend/src/components/tasks/viewers/CsvViewer.tsx`
- d) TXT and LOG: `frontend/src/components/tasks/viewers/TextDocViewer.tsx`
- e) MD: `frontend/src/components/tasks/viewers/MarkdownViewer.tsx`
- f) JSON, XML, YAML, SQL, JAVA, PY, JS, TS: `frontend/src/components/tasks/viewers/CodeIdeViewer.tsx`
- g) DOCX: `frontend/src/components/tasks/viewers/DocxViewer.tsx`
- h) XLSX, PPTX, ZIP: `frontend/src/components/tasks/viewers/NoViewerFallback.tsx`

### 3. Code verification walkthrough (Item 5)
1. **JPG (Image)**:
   - *Card*: Displays emerald green `FileImage` icon, filename with `dir="auto"`, size, uploader, date. Four slots: [Preview] [Download] [placeholder] [Delete (if allowed)].
   - *Click*: Card click or Preview button navigates to `/tasks/:taskId/files/:fileId`.
   - *Viewer*: `resolveViewerType` selects `'image'`. Authenticated fetch (`downloadTaskFileApi`) creates blob URL. `ImageViewer` renders image with Zoom in/out, Fit, and Rotate controls.
2. **PDF**:
   - *Card*: Displays crimson red `FileText` icon. Slots: [Preview] [Download] [placeholder] [Delete (if allowed)].
   - *Click*: Navigates to `/tasks/:taskId/files/:fileId`.
   - *Viewer*: `resolveViewerType` selects `'pdf'`. Authenticated fetch creates blob URL. `PdfViewer` renders embedded iframe with page navigation and zoom controls.
3. **DOCX**:
   - *Card*: Displays royal blue `FileText` icon. Slots: [Preview] [Download] [placeholder] [Delete (if allowed)].
   - *Click*: Navigates to `/tasks/:taskId/files/:fileId`.
   - *Viewer*: `resolveViewerType` selects `'docx'`. Authenticated fetch (`downloadTaskFileApi`) retrieves blob and converts to ArrayBuffer. `DocxViewer` dynamically imports `docx-preview` and calls `renderAsync(arrayBuffer, containerRef)`. On failure, displays clear red error notice with Download button (no blank screen/redirect).
4. **CSV**:
   - *Card*: Displays forest green `FileSpreadsheet` icon. Slots: [Preview] [Download] [Edit (if allowed)] [Delete (if allowed)].
   - *Click*: Navigates to `/tasks/:taskId/files/:fileId`. Clicking Edit button navigates with `?edit=true`.
   - *Viewer*: `resolveViewerType` selects `'csv'`. Authenticated fetch (`getTaskFileTextApi`) retrieves raw text. `CsvViewer` parses via PapaParse (or RFC 4180 fallback) into real table with sticky headers, row numbers, live search, `dir="auto"` cells, and truncation notes. In Edit mode, `FileEditor` provides text editing with version check and 409 conflict handling.
5. **TXT**:
   - *Card*: Displays slate `FileText` icon. Slots: [Preview] [Download] [Edit (if allowed)] [Delete (if allowed)].
   - *Click*: Navigates to `/tasks/:taskId/files/:fileId`.
   - *Viewer*: `resolveViewerType` selects `'txt-log'`. Authenticated fetch retrieves raw text. `TextDocViewer` displays paper card with comfortable typography, wrap toggle, line numbers, and `dir="auto"` per line. In Edit mode, `FileEditor` provides text editing.
6. **MD**:
   - *Card*: Displays purple `FileCode2` icon. Slots: [Preview] [Download] [Edit (if allowed)] [Delete (if allowed)].
   - *Click*: Navigates to `/tasks/:taskId/files/:fileId`.
   - *Viewer*: `resolveViewerType` selects `'markdown'`. Authenticated fetch retrieves markdown text. `MarkdownViewer` provides Preview tab (GFM headers, tables, tasklists, blockquotes, code; raw HTML stripped; links with `target="_blank" rel="noopener noreferrer"`) and Source tab. In Edit mode, `FileEditor` provides code editing.
7. **JSON**:
   - *Card*: Displays amber `FileCode2` icon. Slots: [Preview] [Download] [Edit (if allowed)] [Delete (if allowed)].
   - *Click*: Navigates to `/tasks/:taskId/files/:fileId`.
   - *Viewer*: `resolveViewerType` selects `'code'`. Authenticated fetch retrieves JSON text. `CodeIdeViewer` renders dark theme IDE with line numbers gutter, syntax highlighting, language badge, Pretty-print toggle, Valid/Invalid JSON indicator, and `dir="auto"` per line. In Edit mode, `FileEditor` provides code editing with 2-space tab indent.
8. **JAVA** (and other code types: PY, JS, TS, SQL, XML, YAML):
   - *Card*: Displays amber `FileCode2` icon. Slots: [Preview] [Download] [Edit (if allowed)] [Delete (if allowed)].
   - *Click*: Navigates to `/tasks/:taskId/files/:fileId`.
   - *Viewer*: `resolveViewerType` selects `'code'`. Authenticated fetch retrieves code text. `CodeIdeViewer` renders IDE dark theme with language name, line count, size, line numbers gutter, and syntax highlighting. In Edit mode, `FileEditor` provides code editing with 2-space tab indent.
9. **ZIP**:
   - *Card*: Displays amber `FileArchive` icon. Slots: [placeholder] [Download] [placeholder] [Delete (if allowed)].
   - *Click*: Card click or Download button downloads file immediately.
   - *Viewer*: `resolveViewerType` selects `'no-viewer'`. `NoViewerFallback` displays information card with badge, filename, size, uploader, and prominent Download button.
10. **XLSX** (and PPTX):
    - *Card*: Displays green `FileSpreadsheet` icon. Slots: [placeholder] [Download] [placeholder] [Delete (if allowed)].
    - *Click*: Card click or Download button downloads file immediately.
    - *Viewer*: `resolveViewerType` selects `'no-viewer'`. `NoViewerFallback` displays information card with badge, filename, size, uploader, and prominent Download button.

### 4. Assumptions made
1. On DONE tasks, file editing and file deletion are strictly disabled for all users in accordance with BR-56 ("A DONE task's files are read-only").
2. The 4 action slots on file cards have a fixed 34px by 34px footprint matching small icon buttons (`.btn-icon`), using CSS `.task-file-action-placeholder` when an action is unavailable.
3. For heavy external libraries (`docx-preview`, `papaparse`, `highlight.js`), dynamic ESM CDN imports are used with robust internal fallbacks so offline operation and test suites never throw uncaught exceptions.
4. Raw HTML tags in Markdown files are strictly stripped/disabled in the Preview tab to prevent XSS or unstyled HTML injection.
5. In Edit mode, leaving with unsaved changes prompts the user via the existing shared `ConfirmDialog` component.

## File card action row alignment, file rename on all types, paperclip upload queue, MD IDE viewer, and unified dropzone

### 1. What was built
1. **STEP 0**: Added verbatim Rename rule to BR-56 in `docs/SRS.md`.
2. **Action row**: Unified all 4 action buttons (Preview, Download, Edit, Delete) into a single flex container (`.task-file-actions`) with `align-items: center` using the shared `<IconButton>` component. Preserved the four 34px fixed slots with `.task-file-action-placeholder` when actions are unavailable to prevent button shifting.
3. **Edit button on every file (rename)**:
   - Pencil button renders for all file types for allowed users (uploader, Lead, Manager, non-DONE tasks).
   - Non-text types open `FileRenameModal` directly; text types open a dropdown menu ("Rename" / "Edit content").
   - `FileRenameModal` preselects base name, renders fixed extension suffix, disables Save when unchanged or empty/whitespace-only, and displays live inline `FieldError`.
   - Backend `PATCH /api/tasks/{taskId}/files/{fileId}` validates permissions, rejects DONE tasks with HTTP 403 (`HttpStatus.FORBIDDEN`), preserves extension, sanitizes Unicode/Arabic, updates stored metadata without touching physical files, and logs activity.
   - Live updates without page reload in both list and viewer. In content editor, Save is disabled until content differs, and Cancel prompts for confirmation if dirty.
4. **Paper clip upload**:
   - Replaced duplicate upload logic in `TaskFilesSection` with delegation to `dropzoneRef.current?.openPicker(subtaskId)`.
   - Paperclip uploads feed the identical shared `FileDropzone` upload queue with per-file progress, Pause, Resume, Cancel, and inline errors.
5. **MD files & text detection**:
   - Markdown viewer opens with Preview (rendered, no raw HTML) and Source tabs.
   - Source tab renders `<CodeIdeViewer>` with IDE dark theme, line numbers gutter, Markdown token highlighting, top bar language badge, line count, and size.
   - Type detection in `fileUtils.ts` prioritizes extension over generic MIME types: `md`, `json`, `xml`, `yaml/yml`, `sql`, `java`, `py`, `js`, `ts` in IDE view; `txt` and `log` in document view; `csv` in table.
6. **One dropzone everywhere**:
   - Single shared `FileDropzone` component used across:
     - `frontend/src/components/tasks/TaskFilesSection.tsx` (task file list and upload area)
     - `frontend/src/components/tasks/TaskFormModal.tsx` (task creation attachments and draft subtasks)
     - `frontend/src/pages/TaskDetailPage.tsx` (task detail subtask attachments)

### 2. Frontend and backend tests added
- **Frontend** (`TaskFilesSection.test.tsx`, `TaskFilePage.test.tsx`, `FileDropzone.test.tsx`):
  - Verified four buttons share one container (`.task-file-actions`).
  - Verified Edit button shows for ZIP files.
  - Verified Rename Save button is disabled for unchanged name and enabled after a change.
  - Verified paperclip uploads feed the shared upload queue.
  - Verified `.md` files open the code view in the Source tab.
  - Verified one dropzone component is used across all three locations.
- **Backend** (`TaskFileServiceTest.java`):
  - `renamePermissionsAllowUploaderLeadAndManager`: validates uploader, Lead, and Manager can rename.
  - `renamePermissionsDenyNonUploaderMemberAndOutsiderWith403`: validates non-uploader member and outsider receive HTTP 403.
  - `renameOnDoneTaskFailsWith403`: validates rename on DONE task fails with HTTP 403.
  - `renamePreservesExtensionAndKeepsArabicNamesAndWritesActivity`: validates extension preservation, Unicode/Arabic filename retention, and activity logging.

### 3. Assumptions made
1. Base name validation permits up to 150 characters and disallows control characters, null bytes, and path separators (`/`, `\`).
2. On DONE tasks, both rename and content edits are strictly forbidden (HTTP 403).
3. The four action slots occupy 34px by 34px matching small icon buttons (`.btn-icon`), using CSS `.task-file-action-placeholder` when an action is unavailable.

## Shared FileDropzone unification and scrollable file lists

### 1. What was built
1. **Shared `FileDropzone` Component**:
   - Encapsulated the dropzone together with its upload queue (drag and drop, click to browse, allowed types, limits, per-file progress bar, Pause, Resume, Cancel, remove, inline errors, type-specific file icons, and formatted sizes).
   - Exported from `frontend/src/components/tasks/FileDropzone.tsx` and re-exported via `frontend/src/components/files/FileDropzone.tsx`.
   - Marked outer wrapper with `data-testid="file-dropzone"` across all usages.
2. **Replaced Old Subtask File Area**:
   - Replaced old bare drop/list area in `TaskDetailPage.tsx` (Add subtask section) with the identical shared `FileDropzone`.
   - Also unified in `TaskFormModal.tsx` (Add task dialog attachments and draft subtasks).
   - Removed all ad-hoc upload code paths and separate file inputs.
3. **Files that render `FileDropzone`**:
   - `frontend/src/components/tasks/TaskFilesSection.tsx`
   - `frontend/src/pages/TaskDetailPage.tsx`
   - `frontend/src/components/tasks/TaskFormModal.tsx`
4. **Scrollable File Lists**:
   - Added `.file-list-scrollable` class in `frontend/src/index.css` (`max-height: 240px; overflow-y: auto;` with slim styled scrollbars).
   - Applied `.file-list-scrollable` to:
     - Subtask file lists (on subtask cards in `TaskDetailPage` and subtask groups in `TaskFilesSection`).
     - The Add subtask pending files list.
     - The active upload queue.
     - Task files list inside `TaskFilesSection` only when it exceeds 8 rows.
   - Header with count stays visible above the scrolling container.

### 2. Frontend tests added
- `FileDropzone.test.tsx`: Verified `FileDropzone` renders with `data-testid="file-dropzone"` and pending file list has `.file-list-scrollable`.
- `TaskDetailPage.test.tsx`: Verified Add subtask section and FileSection render the identical `FileDropzone` with matching `data-testid="file-dropzone"`.
- `TaskFilesSection.test.tsx`: Verified FileSection renders `FileDropzone` with `data-testid="file-dropzone"` and applies `.file-list-scrollable` when task files exceed 8 rows.

### 4. Exact commands to run
```powershell
# Run backend tests
cd backend && .\mvnw.cmd test

# Run frontend tests
cd ..\frontend && npm test

# Run frontend production build
npm run build

# Start database and mail catcher
cd .. && docker compose up -d

# Start backend
cd backend && .\mvnw.cmd spring-boot:run

# Start frontend
cd ..\frontend && npm run dev
```

## Phase 10: Self-reported tasks

### 1. What was built
1. **Flyway Migration (`V11__self_reported_tasks.sql`)**:
   - Added `self_reported BOOLEAN NOT NULL DEFAULT FALSE` to `tasks`.
   - Added `on_self_reported BOOLEAN NOT NULL DEFAULT TRUE` to `manager_email_prefs`.
2. **Backend Domain & Services**:
   - `ManagerEmailPrefs` and `ManagerEmailPrefsDto`: Added `onSelfReported` field (default `true`) and wired it in `UserService.updateManagerEmailPrefs`.
   - `Task` and `TaskResponse`: Added `selfReported` boolean field.
   - `CreateTaskRequest`: Added `selfReported` field and loosened strict member validation so self-reporting employees can submit without `memberUserIds`.
   - `TaskRepository`: Added `countActiveSelfReportedTasksForUser(userId)` counting non-DONE self-reported tasks.
   - `TimeSessionRepository`: Added `existsByTaskId(taskId)` for creator delete check.
   - `TaskAccessService`:
     - `requireDonePermission(task, actorId)`: Manager-only for self-reported tasks.
     - `requireSendBackPermission(task, actorId)`: Manager-only for self-reported tasks.
     - `requireRestorePermission(task, actorId)`: Manager-only for self-reported tasks.
     - `requireDeletePermission(task, actorId, hasSessions)`: Creator can delete only while status is `TODO` and has 0 recorded time sessions; Manager can always delete.
   - `NotificationService`:
     - `selfReportedCreated`: Sends in-app notification to all Managers, and email to Managers with `on_self_reported` enabled.
     - `selfReportedReview`: Sends in-app notification to all Managers, and email to Managers with `on_self_reported` OR `on_task_completed` enabled.
   - `TaskService`:
     - Employee creation: forces creator as Lead and only member, sets `selfReported = true`, validates required future deadline, and rejects creation when active tasks limit (20) is reached.
     - Status updates: Enforces Done, Send back, and Restore through `TaskAccessService`, notifies managers when moving to `REVIEW`, and records history events.
     - Pool query: For Managers, queries for review tasks include self-reported tasks in review.
   - `SubtaskService`: Moving task to `REVIEW` via subtask completions calls `notifications.selfReportedReview` on self-reported tasks.
3. **Frontend Implementation**:
   - `frontend/src/types/index.ts`: Updated `ManagerEmailPrefs` and `Task` types.
   - `frontend/src/pages/ProfilePage.tsx`: Added "Notify on self-reported tasks" toggle checkbox for Managers.
   - `frontend/src/pages/TaskPoolPage.tsx`:
     - Unlocked "New task" button for employees.
     - Added "Self-reported" `<Badge>` on task cards.
     - Updated "Waiting for my review" counter to include self-reported tasks for Managers, and excluded self-reported tasks from the employee creator's review count.
   - `frontend/src/components/tasks/TaskFormModal.tsx`:
     - When opened by an employee, hides team members and Lead pickers, sets creator as Lead and sole member, enforces required deadline, and submits `selfReported: true`.
   - `frontend/src/pages/TaskDetailPage.tsx`:
     - Renders "Self-reported" `<Badge>` in header.
     - Hides "Mark done", "Send back", and "Restore" buttons for the employee creator on self-reported tasks.
     - Restricts the active Delete button to creator only while task is `TODO`.

### 2. Tests added
1. `backend/src/test/java/com/taskmanagesys/task/SelfReportedTaskServiceTest.java`:
   - `employeeCreatesSelfReportedTask_success`: Creator is forced as Lead and only member, `selfReported = true`, notifications and history events recorded.
   - `employeeCreatesSelfReportedTask_missingDeadline_throws`: Missing deadline throws 400 `DEADLINE_REQUIRED`.
   - `employeeCreatesSelfReportedTask_limit20Reached_throws`: Active tasks >= 20 throws 400 `SELF_REPORTED_LIMIT_REACHED`.
   - `selfReportedTask_donePermissions`: Creator cannot mark Done (403), Manager can.
   - `selfReportedTask_sendBackPermissions`: Creator cannot send back (403), Manager can.
   - `selfReportedTask_restorePermissions`: Creator cannot restore (403), Manager can.
   - `selfReportedTask_deleteRules`: Creator can delete while `TODO` with 0 sessions; forbidden once `IN_PROGRESS` or with sessions; Manager can delete.
   - `sendToReview_notifiesManagers`: Moving self-reported task to `REVIEW` calls `notifications.selfReportedReview`.
   - `taskAccessService_permissionChecks`: Validates helper matrix logic directly.
2. `backend/src/test/java/com/taskmanagesys/notification/NotificationServiceTest.java`:
   - `selfReportedCreated_respectsManagerEmailPrefs`: Verifies in-app notification and `on_self_reported` email gate.
   - `selfReportedReview_sendsEmailWhenEitherOnSelfReportedOrOnTaskCompletedIsTrue`: Verifies email delivery when either preference is true.
3. `frontend/src/pages/TaskDetailPage.test.tsx`:
   - Verifies "Self-reported" badge renders on self-reported tasks.
   - Verifies "Mark done" and "Send back" buttons are hidden for employee creator on self-reported tasks in review.

### 3. Assumptions made
1. An employee creating a self-reported task is automatically assigned to any draft subtasks created along with the task.
2. Managers do not have a 20-active-task limit when creating normal tasks.
3. The "Waiting for my review" counter on home cards counts self-reported tasks in `REVIEW` for Managers, but excludes them for the employee creator since the creator cannot approve their own work.
4. On self-reported tasks in `TODO` with recorded time sessions, deletion by the creator is rejected with HTTP 403 (`TASK_ACCESS_DENIED`).

### 4. Verification
- Frontend production build (`npm --prefix frontend run build`) completed successfully with 0 errors and 1,765 modules transformed.

## Phase 11: Reports and profile tabs

### 1. What was built
1. **Backend Endpoints & Service**:
   - `GET /api/users/{id}/report`: Returns `UserReportResponse` with `user` (`UserSummary` without email or private fields), `from`, `to`, `summary` metrics, `hoursPerDay` breakdown, and `taskCards`.
   - Permissions: A Manager may request any user's report; an employee may request only their own report; all other requests return HTTP 403 `FORBIDDEN`.
   - Range Validation: Validates that `from` and `to` are present, `to` is not before `from` (HTTP 400 `INVALID_DATE_RANGE`), range does not exceed 366 days (HTTP 400 `DATE_RANGE_EXCEEDED`), and `from` does not precede the 2-year retention window (730 days) (HTTP 400 `DATE_BEFORE_RETENTION`).
   - Batched Queries: Queries `work_history`, `work_history_events`, `time_sessions`, and `time_segments` in single batch requests (0 per-row / N+1 queries).
   - Cairo Midnight Splitting: Multi-day work sessions are split across midnight boundaries using `Africa/Cairo` (`atStartOfDay(ZoneId.of("Africa/Cairo"))`) into daily seconds and hours.
   - History-backed Task Cards: Reads from `work_history` and `work_history_events` so that purged/deleted tasks still appear in reports. Includes tasks if the user was a member during the range and the task was open, or if worked seconds > 0.
   - Date range filters on tasks: Added `from` and `to` parameters to `GET /api/users/{id}/tasks` and `GET /api/users/{id}/public/tasks` to filter tasks open at any point within the range.
2. **Frontend Profile Tabs & Views**:
   - Added tabs `Overview`, `Tasks`, and `Report` to both `UserDetailsPage.tsx` (for Managers viewing employee details) and `ProfilePage.tsx` (for employees viewing their own profile). Non-managers viewing others retain the public viewer profile with the "Works on" list.
   - `DateRangePicker.tsx`: Quick segmented ranges ("This week", "Last 30 days", "This month", "Custom"), synchronized with URL query params (`tab`, `range`, `from`, `to`). Displays warning banner if custom start date precedes 2-year retention.
   - `HoursPerDayChart.tsx`: Bar chart for daily hours breakdown with tooltips and peak hours calculation.
   - `ReportTaskCardView.tsx`: Navigable task cards with "Back" and "Next" buttons, card counter (`Card 1 of N`), worked hours, activity event timeline, and "Expand" button opening a full-page modal view.
   - `UserReportView.tsx`: Complete report tab integrating DateRangePicker, summary metric cards (total hours, tasks worked on, tasks completed, subtasks completed, checkpoints ticked), hours chart, task cards, empty/loading states, and "Print report" action.
   - Print stylesheet in `index.css`: `@media print` rules hide header, tab bar, date pickers, navigation buttons, and modal overlays, while expanding all task cards cleanly for browser print or Save as PDF.

### 2. Tests added
1. `backend/src/test/java/com/taskmanagesys/history/ReportServiceTest.java`:
   - `managerCanViewAnyReport`: Manager can view any employee's report.
   - `employeeCanViewOwnReport`: Employee can view own report.
   - `employeeCannotViewAnotherReport`: Employee viewing another user's report throws HTTP 403 `FORBIDDEN`.
   - `rangeValidationToBeforeFrom`: `to` before `from` throws HTTP 400 `INVALID_DATE_RANGE`.
   - `rangeValidationExceeds366Days`: Range > 366 days throws HTTP 400 `DATE_RANGE_EXCEEDED`.
   - `rangeValidationBeforeRetentionPeriod`: `from` before 730 days throws HTTP 400 `DATE_BEFORE_RETENTION`.
   - `inclusionRulesMemberAndOpenDuringRange`: Tasks with active membership and open status during range are included.
   - `purgedTaskStillReportedFromHistory`: Purged task with `TASK_DELETED` status in `work_history` is included in cards.
   - `hoursSplittingAtCairoMidnight`: Session starting at 23:00 Cairo time and ending at 02:00 Cairo time splits into 1h on Day 1 and 2h on Day 2 in `dailyHours`.
   - `summaryNumbersAndPrivacy`: Aggregates match expected metrics; response uses `UserSummary` without email or private data.
2. `frontend/src/components/reports/UserReportView.test.tsx`:
   - Quick range calculations for "thisWeek", "last30Days", and "thisMonth".
   - `formatYmd` date helper.
   - Retention limit (730 days) and 366-day range validation.
   - Report tab URL parameter synchronization (`tab`, `range`, `from`, `to`).

### 3. Assumptions made
1. When computing Cairo days for report splitting, daylight savings is accounted for automatically by Java `ZoneId.of("Africa/Cairo")`.
2. A task is considered "open at any time in range" if it was not closed (status DONE or TASK_DELETED) prior to `from`, or if the user logged work on it during the range.
3. The 2-year retention window is computed as 730 days before today in Cairo timezone.

### 4. Verification
- Frontend production build (`npm --prefix frontend run build`) succeeded with 0 errors and 1,769 modules transformed.

### 5. Exact commands to run
```bash
# 1. Start database and mail catcher
docker compose up -d

# 2. Run backend
cd backend && .\mvnw.cmd spring-boot:run

# 3. Run frontend
cd frontend && npm run dev

# 4. Run all tests
cd backend && .\mvnw.cmd test; cd ../frontend && npm test
```

