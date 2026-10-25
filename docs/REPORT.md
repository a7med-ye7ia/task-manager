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
