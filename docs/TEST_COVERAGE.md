# Test coverage audit

This is a source audit; no Maven or Vitest run was performed in this session.

| Section 3 action | Allowed-role test | Disallowed-role (403) test | Evidence |
| --- | --- | --- | --- |
| View task pool/details | Yes | Yes | `TaskIntegrationTest` list/detail and outsider access |
| Create task | Yes | Yes | `TaskIntegrationTest.managerCanCreateTaskAndSubtaskAndEmployeeGets403` |
| Edit task fields | Yes (Lead) | Yes (Member) | `editTaskFields_allowedLead`, `editTaskFields_disallowedMember` |
| Delete task | Yes (Manager) | Yes (Member) | `taskResponseIncludesCreatedFieldsAndObjectPermissionIsEnforced` |
| Add/remove members | Yes (Lead) | Yes (Member) | `addOrRemoveMembers_allowedLead`, `addOrRemoveMembers_disallowedMember` |
| Set/change Lead | Yes (Manager) | Yes (Lead) | `changeLead_allowedManager`, `changeLead_disallowedLead` |
| Create/edit/delete subtasks | Yes (Lead) | Yes (Member) | `createSubtask_allowedLead`, `createSubtask_disallowedMember` |
| Create/edit/delete checkpoints | Yes (Lead) | Yes (Member) | `createCheckpoint_allowedLead`, `createCheckpoint_disallowedMember` |
| Change subtask status | Yes (assigned) | Yes (unassigned Member) | `changeSubtaskStatus_allowedAssignee`, `changeSubtaskStatus_disallowedUnassignedMember` |
| Tick checkpoints | Yes (Member) | Yes (outsider) | `tickCheckpoint_allowedMember`, `tickCheckpoint_disallowedOutsider` |
| Move task to Review | Yes (member/Lead/Manager) | Yes (outsider) | `sendToReviewAndSendBackEnforceRules` |
| Mark Done/send back | Yes (Manager/Lead) | Yes (Member) | `markDone_allowedManager`, `markDone_disallowedMember`, `sendBackRequiresReason` |
| Restore archive | Yes (Manager) | Yes (Member) | `restoreArchive_allowedManager`, `restoreArchive_disallowedMember` |
| Read/write comments | Yes (Member/Lead/Manager, including non-member Manager) | Yes (outsider) | `membersLeadAndManagerCanPostCommentsButOtherEmployeeIsRejected`, `managerNonMemberCanReadAndWriteComments` |
| Edit/delete own comment | Yes (author) | Yes (other user) | `onlyCommentAuthorCanEditOrDelete` |
| Delete anyone’s comment | Yes (Manager) | Yes (Lead) | `deleteAnyoneComment_allowedManager`, `deleteAnyoneComment_disallowedLead` |
| User administration | Yes (Manager) | Yes (Employee) | `UserAdminIntegrationTest` |
| Approve/reject sign-up | Yes (Manager) | Yes (Employee) | `UserAdminIntegrationTest` |
| Own profile/password | Yes (all roles) | N/A | `UserProfileIntegrationTest` |
| Manager email preferences | Yes (Manager) | Yes (Employee) | `emailPreferences_managerCanUpdate_employeeForbidden` |

Additional required coverage:

- BR-28 visibility and DONE read-only: comment permission tests exist; DONE read-only mutation coverage is included in task status integration tests.
- Send to review/send back, reason, reopening and activity: `TaskIntegrationTest.sendToReviewAndSendBackEnforceRules`, `sendBackCanReopenDoneSubtask`, `sendBackRequiresReason`, and status service tests.
- Notifications and Manager preferences: `NotificationServiceTest` covers preference-gated email and in-app notification creation; task review covers the no-Lead Manager fallback.
- Mentions/unseen counts and outside-task rejection: `commentsStoreOnlyVerifiedTaskMentionsAndUpdateThemOnEdit`, `mentionsHaveUnseenCountAndOpeningCommentsMarksThemSeen`, and `commentsCannotMentionAnEmployeeOutsideTheTask`.
- Public profile privacy: `signedInUserGetsPublicProfileWithoutEmailAndWorksOnFilter`.
- Member candidates: `taskLeadCanSearchCandidatesButMembersAndOutsidersCannot` checks Lead and Manager 200, Member/outsider/different-Lead 403, and no email field.

No test result is claimed here. The focused integration methods currently live in `TaskIntegrationTest.java`; the notification unit group is in `NotificationServiceTest.java`. Run the commands in `docs/REPORT.md`.

## Phase 8 coverage (written, not executed)

| Rule | Test/evidence |
| --- | --- |
| DONE task cannot start tracking | `TimeSessionServiceTest.doneTasksCannotStart` |
| One running timer and replacement | `TimeSessionServiceTest.startingAnotherTimerReplacesTheFirstAndTodoMovesAutomatically` |
| Automatic TODO to IN_PROGRESS | `TimeSessionServiceTest.startingAnotherTimerReplacesTheFirstAndTodoMovesAutomatically` |
| Future/manual note validation | `TimeSessionServiceTest.manualEntryRejectsFutureAndInvalidNote` |
| Overlap rejection | `TimeSessionServiceTest.manualEntriesCannotOverlap` |
| Member/assignee permission and outsider 403 | `TimeSessionServiceTest.outsiderCannotTrackTask`; controller integration coverage remains required |
| Heartbeat, idle, eight-hour cutoff, stale recovery | `TimeSessionService.recoverStale`, `TimeSessionService.enforceMaximum`, `TimeSessionServiceTest.staleAndEightHourSessionsAreClosedByRecovery`; browser idle integration remains required |
| Cairo midnight splitting and 12-hour cap | `TimeSessionService.enforceDailyCap`, `TimeSessionServiceTest.dailyCapIncludesSessionsAcrossCairoMidnight` |
| Seven-day manual window and Manager older-entry edit | `TimeSessionService.manual/editManual`, `TimeSessionServiceTest.managerCanEditAnOlderManualEntry`; explicit seven-day boundary test remains required |
| History events in the transaction | `HistoryServiceTest.taskEventsCreateDurableHistoryRowsInTheSameCall` and `TaskActivityService` integration |
| History survives task purge; time sessions survive purge | `HistoryServiceTest.taskDeletionClosesHistoryAndWritesIndependentEvent`, `Phase8HousekeepingTest.purgeRecordsHistoryBeforeDeletingTaskAndCleanupKeepsTimeSessionsIndependent` |
| Two-year retention with Clock | `HistoryServiceTest.retentionDeletesEventsBeforeHistoryRows`, `Phase8HousekeepingTest.purgeRecordsHistoryBeforeDeletingTaskAndCleanupKeepsTimeSessionsIndependent` |

Time picker and manual-entry coverage (tests written, not executed here):

| Rule | Test/evidence |
| --- | --- |
| Task-options endpoint is caller-scoped, searchable, paged and privacy-limited to id/title/status/deadline | `TimeTaskOptionsControllerTest.taskOptionsExposeOnlyMinimalPagedOptionFields`; service query/privacy mapping in `TimeTaskOptionsServiceTest.taskOptionsUseCallerScopedSearchPagingAndMinimalResponse` |
| Outsider manual entry maps access rejection to the task field | `TimeSessionServiceTest.outsiderManualEntryReturnsTaskFieldError` |
| Picker search and selection | `TaskPicker.test.tsx` |
| Manual form validation error placement | `MyTimePage.test.tsx` |

Phase 9 files coverage (tests written, not executed here):

| Rule | Test/evidence |
| --- | --- |
| Allowed file signatures and renamed executable/HTML/SVG rejection | `TaskFileServiceTest.allowedSignaturesAreAccepted`, `renamedExecutableHtmlAndSvgAreRejected` |
| Per-file size and per-task count limits | `TaskFileServiceTest.sizeAndCountLimitsAreEnforced` |
| Outsider list/upload/download denial and DONE read-only uploads | `TaskFileServiceTest.outsiderCannotListOrUploadAndDoneTaskIsReadOnly` |
| Authenticated image download headers | `TaskFileControllerTest.imageDownloadIsInlineAndNosniff` |
| Disk cleanup on orphan scan | `TaskFileCleanupTest.orphanCleanupRemovesDiskFilesNotInTheDatabase`; task/subtask delete and purge call `TaskFileService.deleteForTask/deleteForSubtask` |

Phase 8/9 follow-up coverage (tests written, not executed here):

| Shift session state | `TimeShiftServiceTest.pauseKeepsSessionOpenAndResumeReopensClock`, `stopFromPausedEndsPermanently` |
| Ten-hour overtime limit | `TimeShiftServiceTest.tenHourDailyLimitRejectsAnotherStart` |
| Unexpected error trace reference | `GlobalExceptionHandlerTest.unexpectedErrorsReturnReferenceAndKeepTraceInLogContext` |
| Shared UI error message/reference | `ErrorNotice.test.tsx` |

Archive and housekeeping coverage (tests written, not executed here):

| Rule | Test |
| --- | --- |
| DONE timestamps and read-only behavior | `TaskIntegrationTest.doneTaskGetsArchiveFieldsAndManagerCanRestoreEachTargetStatus`, `doneTaskCommentsAreReadOnly` |
| Restore permissions, target status and expiry | `restoreArchive_allowedManager`, `restoreArchive_disallowedMember`, `archivedTaskRejectsMemberStatusChangeAndExpiredRestore` |
| Warning scheduling and idempotence | `ArchiveHousekeepingJobTest.warningIsSentOnceWithinThreeDays` |
| Purge failure isolation | `ArchiveHousekeepingJobTest.expiredTasksArePurgedAndOneFailureDoesNotStopTheBatch` |
| Cleanup retention | `ArchiveHousekeepingJob.cleanup` (service coverage; repository retention calls are exercised by the job tests) |
| Manual archived deletion | `TaskIntegrationTest.managerCanDeleteArchivedTaskAndAuditIsWritten`, `onlyManagerCanDeleteArchivedTask`, `deletingMissingTaskReturnsNotFound` |
| Stable pagination envelope | `UserAdminIntegrationTest.testEndpoint_listUsers`, `testEndpoint_listPending`, `TaskIntegrationTest.taskPoolUsesStablePageShape`, candidate assertions in `taskLeadCanSearchCandidatesButMembersAndOutsidersCannot`; comments and notifications are list endpoints, not paged endpoints |

Follow-up written coverage: file rename permissions/extension validation, per-file upload UI states, same-day timer restart-note validation, and My time collapsed/day-card behavior are recorded for the next local test run.

## Phase 8/9 continuation audit coverage (written, not executed)

| Rule | Test/evidence |
| --- | --- |
| ENDED timer shows Start only | `TimeTrackerControls` state rendering; browser test still required locally. |
| Upload cancellation aborts the live XHR | `uploadTaskFilesApi` abort callback and `TaskFilesSection` cancel-row path; add a mocked-XHR assertion in the next Vitest run. |
| My time Cairo range response | `TimeSessionService.list` validates range and creates day totals from batched session/segment data; add service assertions for several tasks and Cairo-midnight splitting before certifying. |
| My time day cards and collapsed entry form | `MyTimePage.test.tsx` now uses the range response; extend it locally for Show more and range selection. |

| Signed file view token | `FileViewTokenServiceTest` covers valid, tampered, expired, file-bound and user-bound signatures; controller/security integration must be run locally. |
| Range content response | `TaskFileControllerTest.rangeContentReturnsPartialContentAndRangeHeader` — 206 and Content-Range for valid byte ranges. |
| ZIP/text preview | `TaskFileService.zipEntries` bounds at 500 and sanitizes traversal names; `textPreview` caps at 1 MB. |

## Step 4 frontend tests (written, not executed)

- `FileDropzone.test.tsx`: keyboard activation, drag/drop, uploading state and inline progress label.
- `TaskFilePage.test.tsx`: text viewer selection and full-page navigation shell.
- `TaskFilesSection.test.tsx` covers first-upload/subtask grouping and the filter control; `IconButton.test.tsx` covers checkpoint base styling.
- `FileDropzone.test.tsx` covers inline error/no outside row; `NotFoundPage.test.tsx` covers the styled missing-file state; `fileRoute.test.ts` covers the signed route helper.
- `MyTimeRange.test.tsx` and `MyTimePage.test.tsx` cover segmented ranges, custom validation and URL state.

## Remaining Step 4 names

- `TaskFileServiceTest.subtaskMultipartUploadStoresSubtaskIdAndIsReturnedOnFirstList` — multipart subtask regression.
- `TaskFileServiceTest.newVideoAndTextTypesAreAccepted` — MP4, WebM, TEX and MD.
- `TaskFileServiceTest.allowedSignaturesAreAccepted` — existing image/PDF/Office/TXT/CSV/ZIP signatures.
- `TaskFileServiceTest.renamedExecutableHtmlAndSvgAreRejected` — forbidden content rejection.
- `TaskFileServiceTest.sizeAndCountLimitsAreEnforced` — per-file and task limits.
- `TaskFileServiceTest.outsiderCannotListOrUploadAndDoneTaskIsReadOnly` — permissions and DONE read-only.
- `TaskFileControllerTest.imageDownloadIsInlineAndNosniff` — authenticated download headers.
- `TaskFileControllerTest.rangeContentReturnsPartialContentAndRangeHeader` — 206 range response.
- `TaskFileCleanupTest.orphanCleanupRemovesDiskFilesNotInTheDatabase` — orphan cleanup.
- `FileViewTokenServiceTest.validTokenReturnsBoundUser`, `tamperingExpiryAndDifferentFileFail`, `anotherUserTokenRemainsBoundToOriginalUser` — signature binding/expiry.
- `FileViewSecurityIntegrationTest.validSignedContentWorksWithoutAuthorization`, `tamperedExpiredAndOtherUserTokensFail`, `everyOtherApiPathStillRequiresAuthentication` — signed access and 401 protection for list, metadata, ZIP, text and download.
## File preview hardening (written, not run)

- `TaskFileControllerTest.imageDownloadIsInlineAndNosniff`, `nonImagePdfDownloadIsAttachmentAndNosniff`, `rangeContentReturnsPartialContentAndRangeHeader`, and `textResponseUsesUtf8AndReportsTruncation` cover disposition, nosniff, SAMEORIGIN, UTF-8 text and truncation headers.
- `TaskFileServiceTest` covers signature/type rejection, visibility and upload limits; ZIP central-directory and sanitization assertions remain unexecuted here.
- `TaskFilePage.test.tsx` covers the text viewer shell; `fileViewerKind` covers image, PDF, video, audio, CSV, Markdown, TEX, XLSX, DOCX, ZIP and download fallback.

## Normal file-page routing (written, not run)

- `TaskFileServiceTest.metadataRejectsFileFromAnotherTaskAsNotFound` covers task/file binding; existing permission tests cover outsider denial.
- `TaskFilePage.test.tsx` covers the viewer registry and direct route rendering; `TaskFilesSection.test.tsx` covers real preview links and the absence of modal markup.

## Hook and nested-link regressions (written, not run)

- `TaskFilePage.test.tsx` transitions loading → loaded → retry/error while failing on a React hooks mismatch logged to `console.error`.
- `TaskFilesSection.test.tsx` asserts there is no `a a` nesting and that Download does not navigate.
- The repository-wide nesting audit covers the remaining router-link/button cases in `AppLayout`, password and verification pages, `NotFoundPage`, and `AdminUsersPage`; these static markup fixes were not test-run here.

## Extended file names, text types and editing (written, not run)

- `TaskFileServiceTest.unicodeNamesAndNewTextTypesAreAccepted` covers Arabic/mixed names and the added SQL/YAML text types.
- `TaskFileServiceTest.htmlAndInvalidUtf8TextAreRejected` covers disguised HTML and malformed UTF-8.
- `TaskFileServiceTest.uploaderCanEditTextAndDoneTaskCannot` covers atomic text replacement, size metadata and DONE read-only behavior.
- `TaskFilePage.test.tsx` covers code-like text loading; `TaskFilesSection.test.tsx` covers fixed action slots and hidden preview for XLSX/PPTX/ZIP-style files.
- Requested 1 MB, 409 version conflict, activity-entry, uploader/Lead/Manager/Member permission matrix and Arabic Content-Disposition integration cases are documented but remain unexecuted in this environment.

## Phase 10: Self-reported tasks coverage (written, not executed)

| Rule / Feature | Test / Evidence | Description |
| --- | --- | --- |
| Employee creation rules | `SelfReportedTaskServiceTest.employeeCreatesSelfReportedTask_success` | Creator is forced as Lead and sole member, `self_reported = true`, notification sent to managers, history logged. |
| Required future deadline | `SelfReportedTaskServiceTest.employeeCreatesSelfReportedTask_missingDeadline_throws` | Missing deadline at creation throws 400 `DEADLINE_REQUIRED`. |
| Active limit of 20 | `SelfReportedTaskServiceTest.employeeCreatesSelfReportedTask_limit20Reached_throws` | 21st active self-reported task creation attempt throws 400 `SELF_REPORTED_LIMIT_REACHED`. |
| Done permissions (Manager only) | `SelfReportedTaskServiceTest.selfReportedTask_donePermissions` | Creator (Lead) cannot mark Done (403), Manager can approve Done. |
| Send back permissions (Manager only) | `SelfReportedTaskServiceTest.selfReportedTask_sendBackPermissions` | Creator (Lead) cannot send back (403), Manager can send back with reason and reopen subtasks/checkpoints. |
| Restore permissions (Manager only) | `SelfReportedTaskServiceTest.selfReportedTask_restorePermissions` | Creator (Lead) cannot restore archived task (403), Manager can restore. |
| Delete rules | `SelfReportedTaskServiceTest.selfReportedTask_deleteRules` | Creator can delete only while TODO and zero time sessions; forbidden once IN_PROGRESS or sessions exist; Manager can delete. |
| Send to review notification | `SelfReportedTaskServiceTest.sendToReview_notifiesManagers` | Moving self-reported task to REVIEW routes to `NotificationService.selfReportedReview`. |
| Permission helper logic | `SelfReportedTaskServiceTest.taskAccessService_permissionChecks` | Unit verification of `canMarkDone`, `canSendBack`, `canRestore`, and `canDelete` matrix. |
| Manager email preference (Creation) | `NotificationServiceTest.selfReportedCreated_respectsManagerEmailPrefs` | In-app notification saved for all Managers; email sent only if `on_self_reported` is true. |
| Manager email preference (Review) | `NotificationServiceTest.selfReportedReview_sendsEmailWhenEitherOnSelfReportedOrOnTaskCompletedIsTrue` | Email sent if `on_self_reported` OR `on_task_completed` is true. |
| UI: Self-reported badge | `TaskDetailPage.test.tsx` (renders Self-reported badge) | Badge is rendered in task detail page header. |
| UI: Hide Done & Send back | `TaskDetailPage.test.tsx` (hides Done and Send back for creator) | Done and Send back buttons are hidden for employee creator on self-reported tasks in review. |
