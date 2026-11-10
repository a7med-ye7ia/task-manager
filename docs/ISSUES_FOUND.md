# Issues found in Phase 8 and Phase 9 browser review

- **(a) Pause and Stop failed.** The timer treated every pause as a completed session by setting `ended_at`, so the client could not represent an open paused session or resume it. The service now stores explicit RUNNING/PAUSED/ENDED state and worked seconds, adds resume, and the UI confirms Stop.
- **(b) My time task search failed.** The paged repository query ordered a `Page` using a correlated `TimeSession` subquery; that query is not safe for the generated count query and produced the server error surfaced by the picker. The task-options query now uses stable task ordering while keeping member/subtask-assignee scope and title search.
- **(c) File uploads did not persist reliably.** The file workflow lacked the complete post-save interface and multipart limits were still avatar-sized. The flow now uses the private upload directory, canonical content types, cleanup on row failure, 10 MB/60 MB limits, and explicit response/error handling.
- **(d) Files was only a button.** The page lacked the grouped file surface. It now has count/size, drop zone, type icons, grouped task/subtask rows, progress, errors, download/preview/delete actions, and a lightbox.
- **(e) Errors were inconsistent plain text.** `ErrorNotice` and `toUiError` are now the shared presentation path for reviewed page, panel, timer, picker, file and form errors; backend 500 responses include a trace reference.

Backend tests and Vitest were not run in this session.

Follow-up: the drop zone had no activation handlers and upload errors were outside file rows; the timer had no restart-note path; and My time still rendered the removed task filter. These are addressed in the follow-up implementation.

Current upload validation root cause: `TaskFileService.saveOne` selected the validator from the filename extension and required a short extension-specific header, so declared MIME types were not authoritative and incomplete signatures produced a generic error. Validation now detects allowed types from the bytes first, rejects empty or incomplete input with a specific message, and reports an extension mismatch with the detected type. The shared `FileDropzone`, `FileFillIcon`, `IconButton`, `Button`, `ErrorNotice`, `Select`, and `NotFoundState` are reused.

## Continuation audit

- **(f) Stop controls remained visible.** `TimeTrackerControls` considered any session for the page task active, including `ENDED`. The controls now require a non-ended state, so stopping removes the stored open-session state and restores Start.
- **(g) Upload cancellation was cosmetic.** Removing an uploading row did not abort its XMLHttpRequest, so the request could finish after the UI had removed it. The upload client now registers an abort callback per row, handles abort and timeout outcomes, and the cancel action invokes it before removing the row.
- **(h) The My time redesign had no supporting response model.** The endpoint still returned a flat list, preventing per-day worked/regular/overtime totals and a URL-selected range. It now returns Cairo-day entries and totals from the existing endpoint, bounded to 366 days.

## Signed file viewing

The previous download endpoint required the bearer token and read the entire file, which cannot support native browser media viewers or safe cross-origin file URLs. Step 2 now issues a separate 60-second HMAC URL tied to user, task and file, validates the token before permission checks, supports byte ranges, and keeps the authenticated download endpoint unchanged.

The subtask-upload bug was a stale UI list, not a multipart or persistence failure: the dialog sent `subtaskId`, the controller and service saved it, and the list query returned it. The independently mounted Files section did not reload after the dialog’s direct upload completed. `uploadTaskFilesApi` now emits `task-files-changed`, and `TaskFilesSection` reloads the server list for that task.

The remaining upload visual defect was caused by `FileFillIcon` layering a second file glyph and an X badge over the dropzone content. It now uses one stationary SVG document with a clipped liquid fill for real progress, and a single shredder-style SVG for failures. `FileDropzone`, `Button`, `IconButton`, `ErrorNotice`, `Select`, and `NotFoundState` remain shared.

The follow-up animation review found that the liquid document was still the wrong visual model. The upload state now uses a horizontal `--fill` background whose width is set directly from the XMLHttpRequest progress callback; success swaps to one green outlined document/check SVG, and failure keeps the single shredder SVG with a restrained red tint. CSS shimmer, scale-in, and check-draw effects all stop under `prefers-reduced-motion`.

Loading investigation: `AppLoader` is the session-restore/first-app loader in `App.tsx`; `PageLoader` is an inline route/data loader used by pages and the file preview. They are intentionally different responsibilities, not competing global overlays. The file page keeps `PageLoader` inside its page while metadata/token loads, so it does not add a second full-screen app loader.

## Current Files UI correction

The browser mismatch was caused by the Files section retaining the old per-upload error row/native action buttons and the file page using plain links for error/download states. The fix keeps upload failures, filename, message and Retry inside `FileDropzone`, removes failed rows, and uses the shared `NotFoundState`, `ErrorNotice`, `Button`, `Select`, `FileDropzone` and `FileFillIcon`. File-card Preview/Download/Edit/Delete reuse the checkpoint delete button’s `cp-btn` base through `IconButton`; only the inner icon animations differ and reduced motion disables them.
## File preview type handling (2026-11-10)

- PDF embedding was blocked by Spring Security's global `X-Frame-Options: DENY`; signed content now overrides only that response to `SAMEORIGIN`.
- Text previews are UTF-8 `text/plain` with an `X-Content-Truncated` marker; the client parser now distinguishes JSON and text.
- CSV uses lazy Papa Parse with row/column caps. ZIP metadata is read from the central directory, capped at 1000 entries, and traversal names are sanitized.
- DOCX HTML is sanitized with DOMPurify before rendering. Markdown is displayed as safe text/source and uploaded HTML is never rendered.

## Normal file-page routing (2026-11-10)

- The route `/tasks/:taskId/files/:fileId` was registered correctly inside `AppLayout` and `Protected`; the wildcard did not shadow it. The inconsistency was the file section's split behavior: image/PDF cards opened `FilePreviewDialog`, while rich formats navigated. The modal path is now removed and every previewable card/eye control is a real link to the single page route.
- API calls are task-scoped: metadata, token, signed content, text, ZIP entries and download all carry both task id and file id. A file id belonging to another task now returns 404; a valid file without visibility returns 403. Text and ZIP responses are not parsed as JSON.

## Hook and nested-link regressions (2026-11-10)

- `TaskFilePage` called `useMemo` after loading/error early returns, so the hook count changed when metadata arrived. The memo is now declared before all returns and uses safe unloaded data.
- `FileCard` wrapped the whole card in a `Link` while rendering an eye `Link` inside it. The card is now a `div`; only the filename/icon and eye are links, and empty card space invokes the existing navigation callback. Download remains a button.
- A repository-wide nesting audit also found authentication, error, layout, and administration pages wrapping `Button` elements in `Link` elements. Those wrappers now use the existing `btn` classes directly on router links, preserving navigation without invalid interactive-element nesting.

## Extended file handling (2026-11-10)

- Arabic names were converted to underscores by `TaskFileService.sanitize`, which used an ASCII-only regular expression. It now preserves Unicode letters/digits and emits an ASCII fallback plus RFC 5987 UTF-8 filename in `Content-Disposition`.
- The upload detector treated only TXT/CSV/TEX/MD/JSON as text and did not reject malformed UTF-8. The validator now covers LOG/XML/YAML/SQL/JAVA/PY/JS/TS, rejects NUL/invalid UTF-8 and HTML/SVG-looking content, and stores these as `text/plain`.
- File-card actions now reserve the Preview slot and keep Preview, Download, Edit, Delete in fixed order; XLSX, PPTX and ZIP no longer advertise previews.
- Text editing uses `PUT /api/tasks/{taskId}/files/{fileId}/content`, atomic replacement, `updated_at`/`edited_by`, optimistic conflict checks and activity logging.
