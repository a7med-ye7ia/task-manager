# Manual browser checks

1. Sign out, open `/`, and confirm redirect to Sign in; sign up and verify the approval flow.
2. As Manager, invite an employee, approve a sign-up, and review Manager email preferences.
3. Use Forgot password and complete the reset link flow.
4. Create a task with subtasks, assignees and checkpoints; verify deadline and progress behavior.
5. Move work to review, send it back with a reason, reopen items, and complete it.
6. Open Chat, add a comment and mention a task member; verify the unseen badge and activity drawer.
7. Open the Active and Archived pool tabs, filter/search/sort, paginate, and verify deletion countdowns.
8. Restore an archived task, then use Delete now as Manager and confirm the shared irreversible dialog.
9. Open a member avatar/name and verify viewer profile privacy; open your own profile link.
10. Check the layout at 360px width for header, pool, task page, dialogs, profile and users pages; tab through icon buttons and confirm visible focus.

## Phase 8/9 file and time continuation

- Upload a file to a subtask from the subtask form and confirm it appears in the correct group on the first upload.
- Force an invalid upload and confirm the error and Retry action remain inside the dropzone.
- Open image, PDF, MP4/WebM, audio, TXT/CSV/TEX/MD, ZIP, DOCX/XLSX and PPTX files; confirm the appropriate viewer or safe download fallback.
- Seek within a video and verify the signed URL expires after one minute.
- Use My time Last 7 days, Last 30 days and Custom; verify invalid, over-366-day and future-end errors.

## Phase 10: Self-reported tasks

- Sign in as an Employee, click "New task" in Task Pool; verify team member and Lead pickers are hidden and you are forced as Lead.
- Verify deadline is required (cannot submit without a future or today deadline).
- Create task, verify "Self-reported" badge appears in pool card and task details.
- Send task to review; confirm Done and Send back buttons are hidden for the creator.
- Sign in as a Manager; verify the task appears under "Waiting for my review" and Manager can Mark Done or Send Back.
- Sign in as Manager, open Profile -> Email preferences, toggle "Notify on self-reported tasks" and verify save.

## Phase 11: Reports and Profile Tabs

- Sign in as an Employee, open `/profile`; confirm Overview, Tasks, and Report tabs appear.
- On Tasks tab, select date range (e.g. "This month"); confirm task list filters tasks open in range and URL updates with `tab=tasks&range=thisMonth`.
- On Report tab, switch quick ranges ("This week", "Last 30 days", "This month", "Custom"); confirm URL reflects `tab=report&range=...&from=...&to=...`.
- Verify summary cards show total hours, tasks worked on, tasks completed, subtasks completed, and checkpoints ticked.
- Verify hours-per-day bar chart renders with hover tooltips and daily breakdown.
- Verify task cards show worked hours, events timeline, subtask/checkpoint counts, and short summary.
- Click "Next" and "Back" buttons on task cards; confirm card counter updates ("Card 1 of N").
- Click "Expand" on a task card; verify full-page modal view with complete event timeline.
- Select a date range older than 2 years; confirm clear retention warning banner appears.
- Select "Custom" range with end date before start date or exceeding 366 days; confirm inline validation error.
- Click "Print report" (or browser `Ctrl+P`); confirm navbar, buttons, and controls are hidden, all task cards expand cleanly for print/Save as PDF.
- Sign in as a Manager, open an employee's details (`/users/:id`); confirm Overview, Tasks, and Report tabs are present.
- Sign in as a colleague (regular Employee) and open another employee's profile (`/users/:id`); confirm viewer mode shows only public profile and "Works on" list without Report tab.
