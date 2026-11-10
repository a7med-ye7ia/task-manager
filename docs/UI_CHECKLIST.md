# UI Checklist

## 1. FILE CARD ACTION ROW
- **Status**: Done
- **Implementation**:
  - `frontend/src/components/tasks/TaskFilesSection.tsx` (`FileCard` component)
  - `frontend/src/index.css` (`.task-file-actions`, `.task-file-action-slot`, `.task-file-action-placeholder`)
- **Details**:
  - Actions aligned in ONE flex row at the right end of the card, vertically centered (`.task-file-actions`).
  - Four fixed-width slots (34px each) in exact order:
    1. Preview slot: shown for images, PDF, DOCX, CSV, TXT, LOG, MD, JSON, XML, YAML, SQL and code; hidden (invisible placeholder) for ZIP, XLSX, PPTX.
    2. Download slot: always visible.
    3. Edit slot: shown for text-based types (TXT, LOG, MD, CSV, JSON, XML, YAML, SQL, JAVA, PY, JS, TS) to uploader, Lead, Manager, not on DONE tasks; placeholder when unavailable.
    4. Delete slot: shown to people allowed to delete (uploader, Lead, Manager, not on DONE tasks); placeholder when unavailable.
  - Buttons never shift position across cards due to fixed 34px placeholder slots.
  - Clicking card body acts as Preview when available, otherwise Download.

## 2. VIEWER PER FILE TYPE
- **Status**: Done
- **Implementation**:
  - `frontend/src/pages/TaskFilePage.tsx` (`TaskFilePage` container component)
  - Header with file name (`dir="auto"`), type badge, size, uploader, created date, edited date, Download, Edit, Delete as allowed, Back and Next navigation between files of the same section.
  - Authenticated client requests (`downloadTaskFileApi`, `getTaskFileTextApi`), object URLs with unmount revocation, red inline error notice.
  - Never uses `dangerouslySetInnerHTML` for file content.
  - Lazy-loads libraries dynamically.

### Sub-items & Implementing Viewer Files:
- **a) Image Viewer**:
  - **Status**: Done
  - **File**: `frontend/src/components/tasks/viewers/ImageViewer.tsx` (`ImageViewer` component)
  - **Features**: Zoom in/out (up to 400%), fit to screen, rotate 90 degrees clockwise, object URL with cleanup.
- **b) PDF Viewer**:
  - **Status**: Done
  - **File**: `frontend/src/components/tasks/viewers/PdfViewer.tsx` (`PdfViewer` component)
  - **Features**: Embedded viewer iframe with page navigation (Prev/Next/Page display), zoom controls (Zoom in, Zoom out, Fit), object URL with cleanup.
- **c) CSV Viewer**:
  - **Status**: Done
  - **File**: `frontend/src/components/tasks/viewers/CsvViewer.tsx` (`CsvViewer` component)
  - **Features**: Real data table (papaparse lazy loader with internal RFC 4180 fallback handling quotes, UTF-8, and BOM), sticky header (`position: sticky`), row numbers column `#`, horizontal scroll container, `dir="auto"` on every table cell, live search box, first 1,000 rows truncation note with total count, total row and column counts.
- **d) TXT and LOG Viewer**:
  - **Status**: Done
  - **File**: `frontend/src/components/tasks/viewers/TextDocViewer.tsx` (`TextDocViewer` component)
  - **Features**: Document-style paper card view, comfortable reading typography, line wrap toggle, `dir="auto"` per line, line numbers and size metadata.
- **e) MD (Markdown) Viewer**:
  - **Status**: Done
  - **File**: `frontend/src/components/tasks/viewers/MarkdownViewer.tsx` (`MarkdownViewer` component)
  - **Features**: Two tabs: Preview (safe Markdown renderer supporting GFM headings, tables, task lists, bold, italics, blockquotes, code blocks; raw HTML strictly stripped/disabled; links with `target="_blank"` and `rel="noopener noreferrer"`) and Source (code view with line numbers and wrap toggle).
- **f) Code IDE Viewer (JSON, XML, YAML, SQL, JAVA, PY, JS, TS, TEX)**:
  - **Status**: Done
  - **File**: `frontend/src/components/tasks/viewers/CodeIdeViewer.tsx` (`CodeIdeViewer` component)
  - **Features**: IDE-style dark theme (`#1e1e2e`), line numbers always on the left gutter, syntax highlighting for keywords, strings, numbers, and comments, top bar with language badge, line count and size, monospace font, JSON pretty-print formatting toggle, JSON validity indicator (Valid / Invalid syntax), Arabic / RTL text inside lines correctly displayed (`dir="auto"`).
- **g) DOCX Viewer**:
  - **Status**: Done
  - **File**: `frontend/src/components/tasks/viewers/DocxViewer.tsx` (`DocxViewer` component)
  - **Features**: In-browser rendering using `docx-preview` (`renderAsync`) into DOM container with page-like layout; authenticated fetch via `downloadTaskFileApi` converted to ArrayBuffer; clear error state with prominent Download button if rendering fails.
- **h) XLSX, PPTX, ZIP (No In-Browser Viewer)**:
  - **Status**: Done
  - **File**: `frontend/src/components/tasks/viewers/NoViewerFallback.tsx` (`NoViewerFallback` component)
  - **Features**: No in-browser preview; card preview slot is hidden placeholder and clicking downloads immediately; viewer page displays informative card with badge, metadata, and prominent Download button.

## 3. EDIT MODE
- **Status**: Done
- **Implementation**:
  - `frontend/src/components/tasks/viewers/FileEditor.tsx` (`FileEditor` component)
  - `frontend/src/pages/TaskFilePage.tsx` (`TaskFilePage` component integration)
- **Features**:
  - Viewer Edit button opens editor (code editor mode for code, JSON, XML, YAML, SQL, MD source; text editor mode for TXT, LOG, CSV).
  - Save and Cancel buttons with loading state (`isSaving`).
  - Warning through shared `ConfirmDialog` when attempting to cancel, leave, or navigate away with unsaved changes.
  - Red inline errors on save failure.
  - Save calls PUT `/api/tasks/:taskId/files/:fileId/content` with expected `updatedAt` version.
  - HTTP 409 conflict detection displays red error banner with a "Reload" button that fetches fresh content.
  - After saving, card and viewer show "edited" with the updated date.

## 4. CARDS
- **Status**: Done
- **Implementation**:
  - `frontend/src/components/tasks/TaskFilesSection.tsx` (`FileCard` & `fileIcon`)
- **Features**:
  - Type-specific icon from Lucide icon library (no emojis) with distinct colors (green for images, red for PDF, blue for DOCX, forest green for CSV, green for XLSX, orange-red for PPTX, amber for ZIP, purple for Markdown, amber/code for programming languages, slate for TXT/LOG).
  - File name with `dir="auto"`.
  - Size, uploader name, creation date.
  - Displays "edited" with the date when `updatedAt !== createdAt` or `editedBy` is present.

## 5. CODE VERIFICATION AUDIT
- **Status**: Done
- **Implementation**:
  - Code walkthrough conducted for all 10 types (`jpg`, `pdf`, `docx`, `csv`, `txt`, `md`, `json`, `java`, `zip`, `xlsx`):
    - Card appearance, slot visibility, click handling, viewer resolution, API requests made, and result rendering verified.
    - Full breakdown documented in `docs/REPORT.md`.
