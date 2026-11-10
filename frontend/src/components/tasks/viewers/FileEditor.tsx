import React, { useEffect, useState } from 'react';
import { Button } from '../../common/Button';
import { TaskFileDetail } from '../../../types';
import { ErrorNotice } from '../../common/ErrorNotice';
import { RotateCcw, Save, X, WrapText } from 'lucide-react';
import { formatSize, getFileBadgeLabel } from '../fileUtils';

export interface FileEditorProps {
  file: TaskFileDetail;
  initialContent: string;
  isCodeEditor: boolean;
  onSave: (content: string) => Promise<void>;
  onCancel: () => void;
  isSaving: boolean;
  saveError: string | null;
  is409: boolean;
  onReload: () => Promise<void>;
  onDirtyChange: (dirty: boolean) => void;
}

export const FileEditor: React.FC<FileEditorProps> = ({
  file,
  initialContent,
  isCodeEditor,
  onSave,
  onCancel,
  isSaving,
  saveError,
  is409,
  onReload,
  onDirtyChange,
}) => {
  const [draft, setDraft] = useState<string>(initialContent);
  const [wrap, setWrap] = useState<boolean>(true);

  // Synchronize draft when initialContent changes (e.g. after reload or save)
  useEffect(() => {
    setDraft(initialContent);
  }, [initialContent]);

  const isDirty = draft !== initialContent;

  useEffect(() => {
    onDirtyChange(isDirty);
  }, [isDirty, onDirtyChange]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Enable tab indentation inside code editor
    if (e.key === 'Tab' && isCodeEditor) {
      e.preventDefault();
      const textarea = e.currentTarget;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const next = draft.substring(0, start) + '  ' + draft.substring(end);
      setDraft(next);
      requestAnimationFrame(() => {
        textarea.selectionStart = textarea.selectionEnd = start + 2;
      });
    }
    // Ctrl+S / Cmd+S shortcuts
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault();
      if (!isSaving && isDirty) {
        void onSave(draft);
      }
    }
  };

  const lineCount = draft.split('\n').length;
  const badge = getFileBadgeLabel(file);

  return (
    <div className={`file-editor-container ${isCodeEditor ? 'is-code-mode' : 'is-text-mode'}`}>
      <div className="editor-topbar">
        <div className="editor-meta">
          <span className="editor-mode-badge">{isCodeEditor ? `${badge} Editor` : 'Text Editor'}</span>
          <span className="editor-status muted">
            {lineCount.toLocaleString()} {lineCount === 1 ? 'line' : 'lines'} · {formatSize(draft.length)}
            {isDirty && <strong className="unsaved-indicator"> · Unsaved changes</strong>}
          </span>
        </div>

        <div className="editor-actions">
          <Button
            type="button"
            size="sm"
            variant="outline"
            icon={<WrapText size={14} />}
            onClick={() => setWrap((w) => !w)}
            title={wrap ? 'No wrap' : 'Wrap lines'}
            aria-label={wrap ? 'Disable line wrap' : 'Enable line wrap'}
          >
            {wrap ? 'No wrap' : 'Wrap'}
          </Button>

          <Button
            type="button"
            size="sm"
            variant="outline"
            icon={<X size={14} />}
            onClick={onCancel}
            disabled={isSaving}
            aria-label="Cancel editing"
          >
            Cancel
          </Button>

          <Button
            type="button"
            size="sm"
            variant="primary"
            icon={<Save size={14} />}
            isLoading={isSaving}
            disabled={isSaving || !isDirty}
            onClick={() => void onSave(draft)}
            aria-label="Save file content"
          >
            {isSaving ? 'Saving...' : 'Save'}
          </Button>
        </div>
      </div>

      {saveError && (
        <div className="editor-error-banner">
          <ErrorNotice error={saveError} />
          {is409 && (
            <div className="editor-reload-row">
              <span className="conflict-text">
                Another user updated this file. Reload to view the latest version.
              </span>
              <Button
                type="button"
                size="sm"
                variant="outline"
                icon={<RotateCcw size={14} />}
                onClick={() => void onReload()}
              >
                Reload
              </Button>
            </div>
          )}
        </div>
      )}

      <div className={`editor-surface ${wrap ? 'is-wrapped' : 'no-wrap'}`}>
        <textarea
          aria-label="File content"
          className="editor-textarea"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={handleKeyDown}
          spellCheck={false}
          dir="auto"
          autoFocus
        />
      </div>
    </div>
  );
};
