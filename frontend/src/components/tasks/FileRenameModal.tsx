import React, { useEffect, useId, useRef, useState } from 'react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { FieldError } from '../common/FieldError';
import { splitFileName } from './fileUtils';
import { TaskFile } from '../../types';
import { renameTaskFileApi } from '../../services/api';

export interface FileRenameModalProps {
  open: boolean;
  file: TaskFile | null;
  taskId: number;
  onClose: () => void;
  onRenamed: (updated: TaskFile) => void;
}

export const FileRenameModal: React.FC<FileRenameModalProps> = ({
  open,
  file,
  taskId,
  onClose,
  onRenamed,
}) => {
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [draftBase, setDraftBase] = useState('');
  const [touched, setTouched] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const { base: currentBase, ext } = file
    ? splitFileName(file.originalName)
    : { base: '', ext: '' };

  useEffect(() => {
    if (open && file) {
      const { base } = splitFileName(file.originalName);
      setDraftBase(base);
      setTouched(false);
      setApiError(null);
      // Preselect the current base name
      const timer = window.setTimeout(() => {
        if (inputRef.current) {
          inputRef.current.focus();
          inputRef.current.select();
        }
      }, 50);
      return () => window.clearTimeout(timer);
    }
  }, [open, file]);

  const trimmed = draftBase.trim();
  const isUnchanged = trimmed === currentBase;
  const isTooLong = (trimmed + ext).length > 150;
  // Forbidden characters: path separators, colons, wildcard/quote characters, control, or bidi overrides
  const hasForbiddenChars = /[\\/:*?"<>|\u0000-\u001f\u202A-\u202E\u2066-\u2069]/.test(draftBase);

  let validationError: string | null = null;
  if (touched || draftBase.length > 0) {
    if (!trimmed) {
      validationError = 'Name cannot be empty.';
    } else if (hasForbiddenChars) {
      validationError = 'Name contains forbidden characters.';
    } else if (isTooLong) {
      validationError = 'The file name must be 150 characters or less.';
    }
  }

  const isSaveDisabled =
    isSaving ||
    !trimmed ||
    isUnchanged ||
    isTooLong ||
    hasForbiddenChars;

  const handleSave = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (isSaveDisabled || !file) return;

    setIsSaving(true);
    setApiError(null);

    try {
      const fullNewName = trimmed + ext;
      const updated = await renameTaskFileApi(taskId, file.id, fullNewName);
      onRenamed(updated);
      onClose();
    } catch (err) {
      setApiError(err instanceof Error ? err.message : 'The file could not be renamed.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Modal isOpen={open} onClose={onClose} title="Rename file">
      <form onSubmit={handleSave} noValidate>
        <div style={{ marginBottom: '1.25rem' }}>
          <label htmlFor={inputId} className="field-label" style={{ display: 'block', marginBottom: '0.4rem' }}>
            New name
          </label>
          <div className="file-rename-input-wrap" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
            <input
              ref={inputRef}
              id={inputId}
              type="text"
              className="form-input"
              value={draftBase}
              maxLength={150}
              aria-label="New base name"
              onChange={(e) => {
                setDraftBase(e.target.value);
                setTouched(true);
                setApiError(null);
              }}
              style={{ flex: 1 }}
            />
            {ext && (
              <span
                className="file-rename-ext-suffix"
                style={{
                  padding: '0.45rem 0.65rem',
                  background: 'var(--bg-subtle, #f1f5f9)',
                  border: '1px solid var(--border)',
                  borderRadius: '8px',
                  color: 'var(--text-muted)',
                  fontSize: '0.88rem',
                  userSelect: 'none',
                }}
              >
                {ext}
              </span>
            )}
          </div>

          {(validationError || apiError) && (
            <FieldError message={validationError || apiError || undefined} />
          )}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
          <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={isSaveDisabled}
            isLoading={isSaving}
          >
            Save
          </Button>
        </div>
      </form>
    </Modal>
  );
};
