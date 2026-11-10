import React, {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from "react";
import {
  FilePlus,
  File as FileIcon,
  FileText,
  FileImage,
  FileSpreadsheet,
  FileArchive,
  FileCode2,
  FileVideo,
  FileAudio,
  X,
  Paperclip,
} from "lucide-react";
import { FileFillIcon } from "./FileFillIcon";
import { Button } from "../common/Button";
import { FieldError } from "../common/FieldError";
import { filterNewFiles, formatSize } from "./fileUtils";
import { TaskFile } from "../../types";
import { uploadTaskFilesApi } from "../../services/api";

export type FileDropzoneState =
  | "idle"
  | "drag-over"
  | "uploading"
  | "paused"
  | "success"
  | "error";

export type FileDropzoneTarget =
  | { taskId?: number; subtaskId?: number }
  | "pending"
  | undefined;

export type FileDropzoneHandle = {
  uploadPending: (taskId: number, subtaskId?: number) => Promise<TaskFile[]>;
  enqueueFiles: (files: File[], subtaskId?: number) => Promise<TaskFile[]>;
  openPicker: (subtaskId?: number) => void;
  getPendingFiles: () => File[];
  clearPending: () => void;
  hasFiles: () => boolean;
};

type BatchState = {
  status: "uploading" | "paused" | "success" | "error";
  files: File[];
  targetTaskId?: number;
  subtaskId?: number;
  total: number;
  current: number;
  name: string;
  progress: number;
  message?: string;
};

export const getFileIcon = (
  fileName: string,
  contentType?: string,
  size = 20,
) => {
  const ext = fileName.toLowerCase().split(".").pop() || "";
  const type = (contentType || "").toLowerCase();

  if (
    type.startsWith("image/") ||
    ["jpg", "jpeg", "png", "webp"].includes(ext)
  ) {
    return <FileImage size={size} color="#10b981" />;
  }
  if (type === "application/pdf" || ext === "pdf") {
    return <FileText size={size} color="#ef4444" />;
  }
  if (type.includes("wordprocessing") || ext === "docx") {
    return <FileText size={size} color="#2563eb" />;
  }
  if (type === "text/csv" || ext === "csv") {
    return <FileSpreadsheet size={size} color="#059669" />;
  }
  if (type.includes("spreadsheet") || ext === "xlsx") {
    return <FileSpreadsheet size={size} color="#16a34a" />;
  }
  if (type.includes("presentation") || ext === "pptx") {
    return <FileText size={size} color="#ea580c" />;
  }
  if (["zip", "rar", "7z"].includes(ext) || type === "application/zip") {
    return <FileArchive size={size} color="#d97706" />;
  }
  if (type.startsWith("video/") || ["mp4", "webm"].includes(ext)) {
    return <FileVideo size={size} color="#8b5cf6" />;
  }
  if (type.startsWith("audio/") || ["mp3", "wav", "ogg"].includes(ext)) {
    return <FileAudio size={size} color="#06b6d4" />;
  }
  if (ext === "md" || type === "text/markdown") {
    return <FileCode2 size={size} color="#8b5cf6" />;
  }
  if (
    [
      "json",
      "xml",
      "yaml",
      "yml",
      "sql",
      "java",
      "py",
      "js",
      "ts",
      "tex",
    ].includes(ext) ||
    type === "application/json"
  ) {
    return <FileCode2 size={size} color="#f59e0b" />;
  }
  if (ext === "txt" || ext === "log" || type === "text/plain") {
    return <FileText size={size} color="#64748b" />;
  }
  return <FileIcon size={size} color="#94a3b8" />;
};

export type FileDropzoneProps = {
  // Target & Queue integration
  target?: FileDropzoneTarget;
  pendingFiles?: File[];
  onPendingFilesChange?: (files: File[]) => void;
  onUploaded?: (files: TaskFile[]) => void;
  onError?: (error: string) => void;

  // Customization
  buttonMode?: boolean;
  buttonText?: string;
  compact?: boolean;
  disabled?: boolean;
  label?: string;
  multiple?: boolean;
  className?: string;

  // Controlled props for manual / legacy use
  onFiles?: (files: File[]) => void;
  onRetry?: () => void;
  onPause?: () => void;
  onResume?: () => void;
  onCancel?: () => void;
  state?: FileDropzoneState;
  progress?: number;
  error?: string;
  fileName?: string;
  selectedFiles?: File[];
  onRemoveFile?: (index: number) => void;
  selectionErrors?: string[];
};

export const FileDropzone = forwardRef<FileDropzoneHandle, FileDropzoneProps>(
  (
    {
      target,
      pendingFiles,
      onPendingFilesChange,
      onUploaded,
      onError,
      buttonMode = false,
      buttonText = "Attach files",
      compact = false,
      disabled = false,
      label = "Drop files here or click to choose",
      multiple = true,
      className = "",
      onFiles,
      onRetry,
      onPause,
      onResume,
      onCancel,
      state: controlledState,
      progress: controlledProgress,
      error: controlledError,
      fileName: controlledFileName,
      selectedFiles: controlledSelectedFiles,
      onRemoveFile: controlledOnRemoveFile,
      selectionErrors: controlledSelectionErrors,
    },
    ref,
  ) => {
    const input = useRef<HTMLInputElement>(null);
    const pickerSubtaskId = useRef<number | undefined>(undefined);
    const [drag, setDrag] = useState(false);
    const [internalPending, setInternalPending] = useState<File[]>([]);
    const [internalErrors, setInternalErrors] = useState<string[]>([]);
    const [batch, setBatch] = useState<BatchState | null>(null);

    const abortRef = useRef<(() => void) | null>(null);
    const pauseRequested = useRef(false);
    const cancelRequested = useRef(false);

    const activePending =
      pendingFiles !== undefined
        ? pendingFiles
        : controlledSelectedFiles !== undefined
          ? controlledSelectedFiles
          : internalPending;
    const activeErrors =
      controlledSelectionErrors !== undefined
        ? controlledSelectionErrors
        : internalErrors;

    const updatePending = (files: File[]) => {
      if (pendingFiles !== undefined) {
        onPendingFilesChange?.(files);
      } else {
        setInternalPending(files);
      }
    };

    // Determine current visual and queue state
    const currentState: FileDropzoneState =
      controlledState !== undefined
        ? controlledState
        : batch
          ? batch.status
          : drag
            ? "drag-over"
            : "idle";

    const currentProgress =
      controlledProgress !== undefined
        ? controlledProgress
        : (batch?.progress ?? 0);
    const currentFileName =
      controlledFileName !== undefined
        ? controlledFileName
        : batch
          ? batch.status === "uploading"
            ? `${batch.name}${batch.total > 1 ? ` (${batch.current + 1} of ${batch.total})` : ""}`
            : batch.name
          : undefined;

    const currentError =
      controlledError !== undefined ? controlledError : batch?.message;

    const busy = currentState === "uploading";
    const paused = currentState === "paused";

    const runUploadQueue = async (
      taskId: number,
      filesToUpload: File[],
      subtaskId?: number,
    ): Promise<TaskFile[]> => {
      const total = filesToUpload.length;
      if (total === 0) return [];
      const savedFiles: TaskFile[] = [];

      for (let index = 0; index < total; index++) {
        const file = filesToUpload[index];
        setBatch({
          status: "uploading",
          files: filesToUpload.slice(index),
          targetTaskId: taskId,
          subtaskId,
          total,
          current: index,
          name: file.name,
          progress: Math.round((index / total) * 100),
        });

        try {
          const added = await uploadTaskFilesApi(
            taskId,
            [file],
            subtaskId,
            (_file, percent) => {
              setBatch((b) =>
                b && b.status === "uploading"
                  ? {
                      ...b,
                      progress: Math.round(
                        ((index + percent / 100) / total) * 100,
                      ),
                    }
                  : b,
              );
            },
            (abort) => {
              abortRef.current = abort;
            },
          );
          abortRef.current = null;
          savedFiles.push(...added);
        } catch (cause) {
          abortRef.current = null;
          if (pauseRequested.current) {
            pauseRequested.current = false;
            setBatch((cur) => (cur ? { ...cur, status: "paused" } : cur));
            throw new Error("Upload paused");
          }
          if (cancelRequested.current) {
            cancelRequested.current = false;
            setBatch(null);
            throw new Error("Upload cancelled");
          }
          const msg =
            cause instanceof Error
              ? cause.message
              : "The upload failed. Try again.";
          setBatch({
            status: "error",
            files: filesToUpload.slice(index),
            targetTaskId: taskId,
            subtaskId,
            total,
            current: index,
            name: file.name,
            progress: 0,
            message: msg,
          });
          onError?.(msg);
          throw cause;
        }
      }

      setBatch({
        status: "success",
        files: [],
        targetTaskId: taskId,
        subtaskId,
        total,
        current: total,
        name: "",
        progress: 100,
      });

      onUploaded?.(savedFiles);
      window.setTimeout(() => {
        setBatch((b) => (b && b.status === "success" ? null : b));
      }, 2200);

      return savedFiles;
    };

    useImperativeHandle(ref, () => ({
      uploadPending: async (taskId: number, subtaskId?: number) => {
        const files = [...activePending];
        if (files.length === 0) return [];
        const result = await runUploadQueue(taskId, files, subtaskId);
        updatePending([]);
        return result;
      },
      enqueueFiles: async (incoming: File[], subtaskId?: number) => {
        const taskId =
          target && typeof target === "object" ? target.taskId : undefined;
        if (!taskId) return [];
        const { files: valid, errors } = filterNewFiles([], incoming);
        if (errors.length) setInternalErrors(errors);
        else setInternalErrors([]);
        if (valid.length) {
          return await runUploadQueue(
            taskId,
            valid,
            subtaskId ??
              (target && typeof target === "object"
                ? target.subtaskId
                : undefined),
          );
        }
        return [];
      },
      openPicker: (subtaskId?: number) => {
        pickerSubtaskId.current = subtaskId;
        input.current?.click();
      },
      getPendingFiles: () => activePending,
      clearPending: () => {
        updatePending([]);
        setInternalErrors([]);
      },
      hasFiles: () => activePending.length > 0,
    }));

    useEffect(() => {
      return () => {
        abortRef.current?.();
      };
    }, []);

    const handleFilesChosen = (incoming: File[]) => {
      if (onFiles) {
        onFiles(incoming);
        return;
      }

      const { files: valid, errors } = filterNewFiles(
        target === "pending" ? activePending : [],
        incoming,
      );
      if (errors.length) {
        setInternalErrors(errors);
        onError?.(errors[0]);
      } else {
        setInternalErrors([]);
      }

      if (valid.length > 0) {
        if (target === "pending") {
          updatePending(valid);
        } else if (target && typeof target === "object" && target.taskId) {
          const subtaskId =
            pickerSubtaskId.current !== undefined
              ? pickerSubtaskId.current
              : target.subtaskId;
          pickerSubtaskId.current = undefined;
          void runUploadQueue(target.taskId, valid, subtaskId);
        }
      }
    };

    const handlePause = () => {
      if (onPause) onPause();
      if (batch?.status === "uploading") {
        pauseRequested.current = true;
        abortRef.current?.();
      }
    };

    const handleResume = () => {
      if (onResume) onResume();
      if (batch?.status === "paused" && batch.targetTaskId) {
        void runUploadQueue(batch.targetTaskId, batch.files, batch.subtaskId);
      }
    };

    const handleCancel = () => {
      if (onCancel) onCancel();
      if (batch?.status === "uploading" || batch?.status === "paused") {
        cancelRequested.current = true;
        abortRef.current?.();
        if (batch.status === "paused") {
          cancelRequested.current = false;
          setBatch(null);
        }
      }
    };

    const handleRetry = () => {
      if (onRetry) {
        onRetry();
        return;
      }
      if (
        batch?.status === "error" &&
        batch.files.length &&
        batch.targetTaskId
      ) {
        void runUploadQueue(batch.targetTaskId, batch.files, batch.subtaskId);
      }
    };

    const handleRemovePending = (index: number) => {
      if (controlledOnRemoveFile) {
        controlledOnRemoveFile(index);
        return;
      }
      const next = activePending.filter((_, i) => i !== index);
      updatePending(next);
    };

    const choose = (list: FileList | null) => {
      if (list?.length && !busy && !paused) {
        handleFilesChosen(Array.from(list));
      }
    };

    const fillStyle =
      busy || paused
        ? ({
            "--fill": `${Math.round(currentProgress)}%`,
          } as React.CSSProperties)
        : undefined;

    return (
      <div
        className={`file-dropzone-container ${className} ${
          buttonMode ? "is-button-mode" : ""
        }`}
        data-testid="file-dropzone"
      >
        <input
          ref={input}
          hidden
          type="file"
          multiple={multiple}
          data-testid="file-dropzone-input"
          onChange={(e) => {
            choose(e.target.files);
            e.target.value = "";
          }}
        />

        {buttonMode ? (
          <div className="file-attach-button-wrapper">
            <Button
              type="button"
              variant="outline"
              size="sm"
              motion="clip"
              icon={<Paperclip size={15} />}
              onClick={(e) => {
                e.stopPropagation();
                input.current?.click();
              }}
              disabled={disabled || busy}
              data-testid="attach-files-button"
            >
              {buttonText}
            </Button>
          </div>
        ) : (
          <div
            className={`file-dropzone ${compact ? "is-compact" : ""} is-${currentState}${
              disabled ? " is-disabled" : ""
            }`}
            role="button"
            tabIndex={disabled ? -1 : 0}
            aria-busy={busy}
            aria-disabled={disabled}
            style={fillStyle}
            onClick={() => {
              if (disabled) return;
              if (busy) {
                handlePause();
                return;
              }
              if (paused) {
                handleResume();
                return;
              }
              input.current?.click();
            }}
            onKeyDown={(e) => {
              if (!disabled && (e.key === "Enter" || e.key === " ")) {
                e.preventDefault();
                if (busy) handlePause();
                else if (paused) handleResume();
                else input.current?.click();
              }
            }}
            onDragOver={(e) => {
              e.preventDefault();
              if (!disabled) setDrag(true);
            }}
            onDragLeave={() => setDrag(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDrag(false);
              if (!disabled) choose(e.dataTransfer.files);
            }}
          >
            {currentState === "uploading" ||
            currentState === "paused" ||
            currentState === "success" ||
            currentState === "error" ? (
              <FileFillIcon
                progress={currentProgress}
                state={currentState === "paused" ? "uploading" : currentState}
                size={compact ? 32 : 54}
              />
            ) : (
              <FilePlus size={compact ? 22 : 30} />
            )}

            <strong>
              {currentState === "uploading"
                ? currentFileName || "Uploading..."
                : currentState === "paused"
                  ? "Paused — click to resume"
                  : currentState === "error"
                    ? "Upload failed"
                    : currentState === "success"
                      ? "Upload complete"
                      : label}
            </strong>

            {currentState === "uploading" && (
              <small>{Math.round(currentProgress)}%</small>
            )}
            {currentState === "paused" && (
              <small>{Math.round(currentProgress)}% · Paused</small>
            )}

            {(busy || paused) && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  handleCancel();
                }}
              >
                Cancel upload
              </Button>
            )}

            {currentState === "error" && (
              <>
                {currentFileName && <small>{currentFileName}</small>}
                {currentError && <small>{currentError}</small>}
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRetry();
                  }}
                >
                  Retry
                </Button>
              </>
            )}
          </div>
        )}

        {activeErrors.length > 0 && (
          <div className="file-dropzone-errors">
            {activeErrors.map((message) => (
              <FieldError key={message} message={message} />
            ))}
          </div>
        )}

        {/* Upload Queue Section (during upload or pause or error with batch files) */}
        {batch && batch.files.length > 0 && (
          <div
            className="task-file-queue-section"
            style={{ marginTop: "0.75rem" }}
          >
            <div
              className="task-file-group-header"
              style={{
                marginBottom: "0.4rem",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <h4 style={{ margin: 0 }}>
                {batch.status === "uploading"
                  ? `Uploading (${batch.current + 1} of ${batch.total})`
                  : batch.status === "paused"
                    ? `Paused (${batch.current + 1} of ${batch.total})`
                    : batch.status === "error"
                      ? "Upload error"
                      : `Uploaded (${batch.total})`}
              </h4>
              <div style={{ display: "flex", gap: ".4rem" }}>
                {batch.status === "uploading" && (
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={handlePause}
                  >
                    Pause
                  </Button>
                )}
                {batch.status === "paused" && (
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={handleResume}
                  >
                    Resume
                  </Button>
                )}
                {(batch.status === "uploading" ||
                  batch.status === "paused") && (
                  <Button
                    type="button"
                    size="sm"
                    variant="danger"
                    onClick={handleCancel}
                  >
                    Cancel
                  </Button>
                )}
              </div>
            </div>
            <div className="file-list-scrollable">
              {batch.files.map((file, idx) => {
                const isCurrent = idx === 0;
                const isErr = isCurrent && batch.status === "error";
                return (
                  <div
                    key={`${file.name}-${idx}`}
                    className={`task-file-upload-row ${isErr ? "is-error" : ""}`}
                  >
                    <span className="task-file-icon">
                      {getFileIcon(file.name)}
                    </span>
                    <span
                      className="task-file-name"
                      title={file.name}
                      dir="auto"
                    >
                      {file.name}
                    </span>
                    <div className="task-file-progress">
                      <span
                        style={{ width: `${isCurrent ? currentProgress : 0}%` }}
                      />
                    </div>
                    <small className="muted">{formatSize(file.size)}</small>
                    {isErr && batch.message && (
                      <div className="task-file-error">{batch.message}</div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Pending Files List (target="pending") */}
        {activePending.length > 0 &&
          !busy &&
          !paused &&
          currentState !== "error" && (
            <div
              className="task-file-queue-section"
              style={{ marginTop: "0.75rem" }}
            >
              <div
                className="task-file-group-header"
                style={{
                  marginBottom: "0.4rem",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <h4 style={{ margin: 0 }}>
                  Files <span className="muted">({activePending.length})</span>
                </h4>
              </div>
              <div className="file-list-scrollable">
                {activePending.map((file, index) => (
                  <div
                    className="task-file-upload-row"
                    key={`${file.name}-${file.size}-${index}`}
                  >
                    <span className="task-file-icon">
                      {getFileIcon(file.name)}
                    </span>
                    <span
                      className="task-file-name"
                      title={file.name}
                      dir="auto"
                    >
                      {file.name}
                    </span>
                    <small className="muted">{formatSize(file.size)}</small>
                    <div className="task-file-actions">
                      <button
                        type="button"
                        className="btn btn-icon pending-file-remove"
                        aria-label={`Remove ${file.name}`}
                        title={`Remove ${file.name}`}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRemovePending(index);
                        }}
                      >
                        <X size={15} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
      </div>
    );
  },
);

FileDropzone.displayName = "FileDropzone";
