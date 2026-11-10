export const formatSize = (bytes: number): string =>
  bytes >= 1048576
    ? `${(bytes / 1048576).toFixed(1)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`;

export const splitFileName = (name: string) => {
  const dot = name.lastIndexOf('.');
  return dot > 0 ? { base: name.slice(0, dot), ext: name.slice(dot) } : { base: name, ext: '' };
};

export const getFileExtension = (name: string): string => {
  const dot = name.lastIndexOf('.');
  return dot >= 0 ? name.slice(dot + 1).toLowerCase() : '';
};

// Keep these in sync with the backend limits.
const ALLOWED_EXTENSIONS = [
  'jpg', 'jpeg', 'png', 'webp', 'pdf', 'docx', 'xlsx', 'pptx',
  'txt', 'csv', 'zip', 'mp4', 'webm', 'tex', 'md', 'json',
  'xml', 'yaml', 'yml', 'log', 'sql', 'java', 'py', 'js', 'ts',
];
const VIDEO_EXTENSIONS = ['mp4', 'webm'];
const MAX_FILES = 30;

export function checkFile(file: File): string | null {
  const ext = getFileExtension(file.name);
  if (!ALLOWED_EXTENSIONS.includes(ext)) return `${file.name}: this file type is not allowed.`;
  if (file.size === 0) return `${file.name}: the file is empty.`;
  const limit = VIDEO_EXTENSIONS.includes(ext) ? 100 : 10;
  if (file.size > limit * 1048576) return `${file.name}: larger than ${limit} MB.`;
  return null;
}

export function filterNewFiles(current: File[], incoming: File[]): { files: File[]; errors: string[] } {
  const errors: string[] = [];
  const next = [...current];
  for (const file of incoming) {
    const problem = checkFile(file);
    if (problem) { errors.push(problem); continue; }
    if (next.some((f) => f.name === file.name && f.size === file.size)) continue;
    if (next.length >= MAX_FILES) { errors.push(`You can attach up to ${MAX_FILES} files.`); break; }
    next.push(file);
  }
  return { files: next, errors };
}

// Pairs draft subtasks (with their files) with the subtasks the server created.
export function matchSubtaskFiles(
  created: { id: number; title: string; deadline: string }[],
  drafts: { title: string; deadline: string; files: File[] }[],
): { subtaskId: number; files: File[] }[] {
  const pool = [...created];
  const result: { subtaskId: number; files: File[] }[] = [];
  for (const draft of drafts) {
    if (!draft.files.length) continue;
    const at = pool.findIndex((s) => s.title.trim() === draft.title.trim() && s.deadline === draft.deadline);
    if (at < 0) continue;
    result.push({ subtaskId: pool[at].id, files: draft.files });
    pool.splice(at, 1);
  }
  return result;
}

// Preview slot: shown for images, PDF, DOCX, CSV, TXT, LOG, MD, JSON, XML, YAML, SQL and code.
// Hidden (placeholder only) for ZIP, XLSX, PPTX.
export function isPreviewable(file: { originalName: string; contentType?: string }): boolean {
  const ext = getFileExtension(file.originalName);
  const type = (file.contentType || '').toLowerCase();

  if (['zip', 'rar', '7z', 'xlsx', 'pptx'].includes(ext)) return false;
  if (type.includes('spreadsheet') || type.includes('presentation') || type === 'application/zip') return false;

  return (
    type.startsWith('image/') ||
    type === 'application/pdf' ||
    type.startsWith('video/') ||
    type.startsWith('audio/') ||
    type === 'text/csv' ||
    type === 'text/markdown' ||
    type === 'text/plain' ||
    type === 'application/json' ||
    type.includes('wordprocessing') ||
    [
      'jpg', 'jpeg', 'png', 'webp', 'pdf', 'docx', 'csv', 'txt', 'log',
      'md', 'json', 'xml', 'yaml', 'yml', 'sql', 'java', 'py', 'js', 'ts', 'tex',
    ].includes(ext)
  );
}

// Edit slot: shown for text-based types (TXT, LOG, MD, CSV, JSON, XML, YAML, SQL, JAVA, PY, JS, TS)
export function isEditableType(file: { originalName: string; contentType?: string }): boolean {
  const ext = getFileExtension(file.originalName);
  const type = (file.contentType || '').toLowerCase();
  const editableExtensions = [
    'txt', 'log', 'md', 'csv', 'json', 'xml', 'yaml', 'yml', 'sql', 'java', 'py', 'js', 'ts', 'tex',
  ];
  if (editableExtensions.includes(ext)) return true;
  return (
    type === 'text/plain' ||
    type === 'text/csv' ||
    type === 'text/markdown' ||
    type === 'application/json' ||
    type === 'text/x-tex'
  );
}

// Distinguish code vs document editor in edit mode:
// CodeMirror 6 for code, JSON, XML, YAML, SQL and MD source; text editor for TXT, LOG and CSV
export function isCodeEditorType(file: { originalName: string }): boolean {
  const ext = getFileExtension(file.originalName);
  return ['json', 'xml', 'yaml', 'yml', 'sql', 'java', 'py', 'js', 'ts', 'tex', 'md'].includes(ext);
}

export type FileViewerType =
  | 'image'
  | 'pdf'
  | 'docx'
  | 'csv'
  | 'txt-log'
  | 'markdown'
  | 'code'
  | 'no-viewer';

export function resolveViewerType(file: { originalName: string; contentType?: string }): FileViewerType {
  const ext = getFileExtension(file.originalName);
  const type = (file.contentType || '').toLowerCase();

  // No-viewer types: ZIP, XLSX, PPTX
  if (['zip', 'rar', '7z', 'xlsx', 'pptx'].includes(ext)) return 'no-viewer';
  if (type.includes('spreadsheet') || type.includes('presentation') || type === 'application/zip') return 'no-viewer';

  // Specific formats by extension or type
  if (['jpg', 'jpeg', 'png', 'webp'].includes(ext) || type.startsWith('image/')) return 'image';
  if (ext === 'pdf' || type === 'application/pdf') return 'pdf';
  if (ext === 'docx' || type.includes('wordprocessing')) return 'docx';
  if (ext === 'csv' || type === 'text/csv') return 'csv';
  if (ext === 'md' || ext === 'markdown' || type === 'text/markdown') return 'markdown';

  // Code IDE formats (JSON, XML, YAML, SQL, JAVA, PY, JS, TS, TEX)
  if (
    ['json', 'xml', 'yaml', 'yml', 'sql', 'java', 'py', 'js', 'ts', 'tex'].includes(ext) ||
    type === 'application/json' ||
    type === 'text/x-tex'
  ) {
    return 'code';
  }

  // Document formats: TXT and LOG
  if (ext === 'txt' || ext === 'log' || type === 'text/plain') {
    return 'txt-log';
  }

  return 'no-viewer';
}

export function getFileBadgeLabel(file: { originalName: string; contentType?: string }): string {
  const ext = getFileExtension(file.originalName).toUpperCase();
  if (ext) return ext;
  const type = (file.contentType || '').toLowerCase();
  if (type.startsWith('image/')) return 'IMAGE';
  if (type === 'application/pdf') return 'PDF';
  return 'FILE';
}

export function formatDateLabel(isoString?: string | null): string {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '';
    return d.toLocaleDateString('en-GB');
  } catch {
    return '';
  }
}
