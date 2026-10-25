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
