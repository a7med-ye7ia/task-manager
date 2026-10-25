# Business-rule checklist

Source audit; tests are not claimed as run in this session.

| BR | Status | Implementation / proof |
|---|---|---|
| BR-1 | Done | `TaskMember`, `TaskService`, task integration permission tests |
| BR-2 | Done | `TaskService.changeLead`, role tests |
| BR-3 | Done | `TaskService.changeLead`, Lead tests |
| BR-4 | Done | `UserAdminService`, user-admin tests |
| BR-5 | Done | `TaskAccessService`, Manager non-member tests |
| BR-6 | Done | `UserAdminService`, admin UI/tests |
| BR-7 | Done | `AuthService`, signup integration tests |
| BR-8 | Done | `AuthService`, JWT/cookie configuration |
| BR-9 | Done | `AuthService`, forgot-password tests |
| BR-10 | Done | `UserService`, profile tests |
| BR-11 | Done | `UserService`, email-change integration test |
| BR-12 | Done | `AvatarService`, avatar UI/tests |
| BR-13 | Done | user admin service/controllers |
| BR-14 | Done | `SubtaskService`, assignee confirmation tests |
| BR-15 | Done | `TaskRulesService`, task form/service tests |
| BR-16 | Done | `TaskRulesService` deadline tests |
| BR-17 | Done | `TaskRulesService` deadline tests |
| BR-18 | Done | `TaskRulesService` deadline tests |
| BR-19 | Done | `TaskRulesService` deadline tests |
| BR-20 | Done | `TaskRulesService` validation |
| BR-21 | Done | `TaskService`/`SubtaskService`, status tests |
| BR-22 | Done | computed progress mapping and task UI |
| BR-23 | Done | subtask/checkpoint progress mapping |
| BR-24 | Done | task member creation and permission tests |
| BR-25 | Done | task detail status controls |
| BR-26 | Done | `TaskActivityService` activity tests |
| BR-27 | Done | `TaskPoolPage`, pool endpoint and pagination tests |
| BR-28 | Done | `CommentService`, comment integration tests |
| BR-29 | Done | activity drawer and activity service |
| BR-30 | Done | archive fields/read-only checks and UI |
| BR-31 | Done | restore endpoint/dialog and integration tests |
| BR-32 | Done | `ArchiveHousekeepingJob`, migration and unit tests |
| BR-33 | Done | `AuditLogService`, retention job and deletion audit |
| BR-34 | Done | housekeeping repositories/job |
| BR-35 | Done | notification pipeline/outbox |
| BR-36 | Done | manager preferences and notification service |
| BR-37 | Done | notification bell/controller/UI |
| BR-38 | Done | `EmailSenderJob` HTML/plain templates |
| BR-39 | Done | SMTP environment configuration |
| BR-40 | Done | frontend/backend validation envelope |
| BR-41 | Partial | security fixes in `AuthService`, `SecurityConfig`; full runtime audit awaits test run |
| BR-42 | Partial | `TaskIntegrationTest`, coverage matrix written; Maven not run |
| BR-43 | Partial | source/UI scan required before final certification |
| BR-44 | Done | shared styles/components |
| BR-45 | Done | SRS copy and UI contains no technical status indicator |
| BR-46 | Done | password inputs and visibility controls |
| BR-47 | Done | password requirement feedback components |
| BR-48 | Done | avatar crop/upload/remove flows |
| BR-49 | Done | user details/profile routes |
| BR-50 | Done | shared `Select` and people picker |
| BR-51 | Done | mention storage, unseen counts, links and tests |
| BR-52 | Done | public profile and `PersonProfileLink` |
| BR-53 | Done | task empty/read-only sections |
| BR-54 | Done | task-scoped candidate endpoint/picker |

Remaining certification is limited to the explicit Phase 7 checks marked Partial above and the user-run test commands.
