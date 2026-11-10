import React, { forwardRef, useEffect, useImperativeHandle, useRef, useState } from "react";
import {
  FileArchive,
  FileCode2,
  File as FileIcon,
  FileImage,
  FileSpreadsheet,
  FileText,
  Minus,
  Paperclip,
  Plus,
  Trash2,
  Eye,
} from "lucide-react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { TaskFile } from "../../types";
import {
  deleteTaskFileApi,
  downloadTaskFileApi,
  listTaskFilesApi,
} from "../../services/api";
import { Card } from "../common/Card";
import { ConfirmDialog } from "../common/ConfirmDialog";
import { ErrorNotice } from "../common/ErrorNotice";
import { Select } from "../common/Select";
import { FileDropzone, FileDropzoneHandle } from "./FileDropzone";
import {
  formatDateLabel,
  formatSize,
  isEditableType,
  isPreviewable,
} from "./fileUtils";
import { Button } from "../common/Button";
import { IconButton } from "../common/IconButton";
import { DownloadArrowIcon, PenWriteIcon } from "../common/Icons";
import { FileRenameModal } from "./FileRenameModal";

export const canPreviewFile = (file: TaskFile): boolean => isPreviewable(file);

export const canEditFile = (file: TaskFile): boolean => isEditableType(file);

// Icon library with type-specific color and no emojis (BR-56 / Card requirement)
const fileIcon = (file: TaskFile) => {
  const ext = file.originalName.toLowerCase().split(".").pop() || "";
  const type = (file.contentType || "").toLowerCase();

  if (type.startsWith("image/") || ["jpg", "jpeg", "png", "webp"].includes(ext)) {
    return <FileImage size={22} color="#10b981" />;
  }
  if (type === "application/pdf" || ext === "pdf") {
    return <FileText size={22} color="#ef4444" />;
  }
  if (type.includes("wordprocessing") || ext === "docx") {
    return <FileText size={22} color="#2563eb" />;
  }
  if (type === "text/csv" || ext === "csv") {
    return <FileSpreadsheet size={22} color="#059669" />;
  }
  if (type.includes("spreadsheet") || ext === "xlsx") {
    return <FileSpreadsheet size={22} color="#16a34a" />;
  }
  if (type.includes("presentation") || ext === "pptx") {
    return <FileText size={22} color="#ea580c" />;
  }
  if (["zip", "rar", "7z"].includes(ext) || type === "application/zip") {
    return <FileArchive size={22} color="#d97706" />;
  }
  if (ext === "md" || type === "text/markdown") {
    return <FileCode2 size={22} color="#8b5cf6" />;
  }
  if (
    ["json", "xml", "yaml", "yml", "sql", "java", "py", "js", "ts", "tex"].includes(ext) ||
    type === "application/json"
  ) {
    return <FileCode2 size={22} color="#f59e0b" />;
  }
  if (ext === "txt" || ext === "log" || type === "text/plain") {
    return <FileText size={22} color="#64748b" />;
  }
  return <FileIcon size={22} color="#94a3b8" />;
};

const FileCard: React.FC<{
  file: TaskFile;
  canManage: boolean;
  onOpen: () => void;
  onEditContent: () => void;
  onOpenRename: () => void;
  onDownload: () => void;
  onDelete: () => void;
  previewable: boolean;
  href?: string;
}> = ({
  file,
  canManage,
  onOpen,
  onEditContent,
  onOpenRename,
  onDownload,
  onDelete,
  previewable,
  href,
}) => {
  const isEdited = Boolean(file.editedBy || (file.updatedAt && file.updatedAt !== file.createdAt));
  const isText = isEditableType(file);
  const [showMenu, setShowMenu] = useState(false);

  // Close menu on outside click
  useEffect(() => {
    if (!showMenu) return;
    const close = () => setShowMenu(false);
    window.addEventListener("click", close);
    return () => window.removeEventListener("click", close);
  }, [showMenu]);

  const handleEditClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (isText) {
      setShowMenu((prev) => !prev);
    } else {
      onOpenRename();
    }
  };

  return (
    <div
      className="task-file-card"
      onClick={(event) => {
        if ((event.target as HTMLElement).closest("button, a, input, select, textarea")) return;
        if (previewable) onOpen();
        else onDownload();
      }}
    >
      {href ? (
        <Link
          className="task-file-name-link"
          to={href}
          onClick={(e) => e.stopPropagation()}
          aria-label={`Open ${file.originalName}`}
        >
          <span className="task-file-icon">{fileIcon(file)}</span>
        </Link>
      ) : (
        <span className="task-file-icon">{fileIcon(file)}</span>
      )}

      <div className="task-file-card-main">
        {href ? (
          <Link
            className="task-file-name-link"
            to={href}
            onClick={(e) => e.stopPropagation()}
          >
            <strong title={file.originalName} dir="auto">
              {file.originalName}
            </strong>
          </Link>
        ) : (
          <strong title={file.originalName} dir="auto">
            {file.originalName}
          </strong>
        )}
        <small className="muted">
          {formatSize(file.sizeBytes)} · {file.uploaderName} ·{" "}
          {formatDateLabel(file.createdAt)}
          {isEdited && ` · edited ${formatDateLabel(file.updatedAt)}`}
        </small>
      </div>

      {/* ONE container: single flex row, align-items center, four shared IconButtons */}
      <div className="task-file-actions" onClick={(e) => e.stopPropagation()}>
        {previewable ? (
          <IconButton
            data-action="preview"
            aria-label="Preview file"
            title="Preview file"
            motion="preview"
            icon={<Eye size={16} />}
            onClick={(e) => {
              e.stopPropagation();
              onOpen();
            }}
          />
        ) : (
          <div className="task-file-action-placeholder" aria-hidden="true" />
        )}

        <IconButton
          data-action="download"
          aria-label="Download file"
          title="Download file"
          motion="download"
          icon={<DownloadArrowIcon size={16} />}
          onClick={(e) => {
            e.stopPropagation();
            onDownload();
          }}
        />

        {canManage ? (
          <IconButton
            data-action="edit"
            aria-label="Edit file"
            title="Edit file"
            motion="edit"
            icon={<PenWriteIcon size={16} />}
            onClick={handleEditClick}
          />
        ) : (
          <div className="task-file-action-placeholder" aria-hidden="true" />
        )}

        {canManage ? (
          <IconButton
            data-action="delete"
            aria-label="Delete file"
            title="Delete file"
            motion="delete"
            icon={<Trash2 size={16} />}
            onClick={(e) => {
              e.stopPropagation();
              onDelete();
            }}
          />
        ) : (
          <div className="task-file-action-placeholder" aria-hidden="true" />
        )}

        {showMenu && canManage && (
          <div
            className="file-edit-menu"
            onClick={(e) => e.stopPropagation()}
            style={{
              position: "absolute",
              top: "100%",
              right: 40,
              zIndex: 20,
              background: "var(--bg-card, #fff)",
              border: "1px solid var(--border)",
              borderRadius: "8px",
              boxShadow: "var(--shadow-md)",
              minWidth: "125px",
              padding: "0.25rem 0",
              display: "flex",
              flexDirection: "column",
            }}
          >
            <button
              type="button"
              className="file-edit-menu-item"
              onClick={() => {
                setShowMenu(false);
                onOpenRename();
              }}
              style={{
                padding: "0.45rem 0.75rem",
                textAlign: "start",
                background: "none",
                border: "none",
                cursor: "pointer",
                fontSize: "0.85rem",
                color: "var(--text-main)",
              }}
            >
              Rename
            </button>
            <button
              type="button"
              className="file-edit-menu-item"
              onClick={() => {
                setShowMenu(false);
                onEditContent();
              }}
              style={{
                padding: "0.45rem 0.75rem",
                textAlign: "start",
                background: "none",
                border: "none",
                cursor: "pointer",
                fontSize: "0.85rem",
                color: "var(--text-main)",
              }}
            >
              Edit content
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export type TaskFilesSectionHandle = {
  openPicker: (subtaskId?: number) => void;
};

export interface TaskFilesSectionProps {
  taskId: number;
  subtasks: { id: number; title: string }[];
  currentUserId?: number;
  canUpload: boolean;
  canDelete: boolean;
}

export const TaskFilesSection = forwardRef<
  TaskFilesSectionHandle,
  TaskFilesSectionProps
>(({ taskId, subtasks, currentUserId, canUpload, canDelete }, ref) => {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const [files, setFiles] = useState<TaskFile[]>([]);
  const [listError, setListError] = useState("");
  const [confirm, setConfirm] = useState<TaskFile | null>(null);
  const [renamingFile, setRenamingFile] = useState<TaskFile | null>(null);
  const [userOpen, setUserOpen] = useState<boolean | null>(null);
  const dropzoneRef = useRef<FileDropzoneHandle>(null);

  useImperativeHandle(ref, () => ({
    openPicker: (subtaskId?: number) => {
      setUserOpen(true);
      dropzoneRef.current?.openPicker(subtaskId);
    },
  }));

  const options = [
    { value: "all", label: "All files" },
    { value: "task", label: "Task files" },
    ...subtasks.map((subtask) => ({
      value: String(subtask.id),
      label: subtask.title,
    })),
  ];
  const rawFilter = params.get("fileFilter") || "all";
  const filter = options.some((option) => option.value === rawFilter)
    ? rawFilter
    : "all";
  const target =
    filter !== "all" && filter !== "task" ? Number(filter) : undefined;
  const targetTitle = subtasks.find((subtask) => subtask.id === target)?.title;
  const open = userOpen ?? files.length > 0;

  const load = async () => {
    try {
      setFiles(await listTaskFilesApi(taskId));
      setListError("");
    } catch {
      setListError("Files could not be loaded.");
    }
  };
  useEffect(() => {
    void load();
  }, [taskId]);

  useEffect(() => {
    const handler = (event: Event) => {
      if ((event as CustomEvent).detail?.taskId === taskId) void load();
    };
    window.addEventListener("task-files-changed", handler);
    return () => window.removeEventListener("task-files-changed", handler);
  }, [taskId]);

  const merge = (added: TaskFile[]) =>
    setFiles((current) => {
      const ids = new Set(current.map((file) => file.id));
      return [...current, ...added.filter((file) => !ids.has(file.id))];
    });

  const downloadTaskFiles = async (selected: TaskFile[]) => {
    for (const file of selected) {
      try {
        const blob = await downloadTaskFileApi(taskId, file.id);
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = file.originalName;
        anchor.click();
        URL.revokeObjectURL(url);
      } catch (cause) {
        setListError(
          cause instanceof Error ? cause.message : "The file could not be downloaded.",
        );
      }
    }
  };

  const remove = async () => {
    if (!confirm) return;
    try {
      await deleteTaskFileApi(taskId, confirm.id);
      setFiles((current) => current.filter((file) => file.id !== confirm.id));
    } catch (cause) {
      setListError(
        cause instanceof Error
          ? cause.message
          : "The file could not be deleted.",
      );
    } finally {
      setConfirm(null);
    }
  };

  // Allowed to edit & delete: uploader, Lead, Manager, not on DONE tasks (canUpload is false on DONE)
  const isAllowedToManage = (file: TaskFile) =>
    canUpload && (canDelete || file.uploaderId === currentUserId);

  const groupsAll = [
    {
      key: "task",
      title: "Task files",
      subtaskId: undefined as number | undefined,
      list: files.filter((file) => !file.subtaskId),
    },
    ...subtasks.map((subtask) => ({
      key: String(subtask.id),
      title: subtask.title,
      subtaskId: subtask.id as number | undefined,
      list: files.filter((file) => file.subtaskId === subtask.id),
    })),
  ];
  const groups = groupsAll
    .filter((group) => filter === "all" || group.key === filter)
    .filter((group) => canUpload || group.list.length > 0);
  const total = files.reduce((sum, file) => sum + file.sizeBytes, 0);
  const summary = files.length
    ? `${files.length} ${files.length === 1 ? "file" : "files"} · ${formatSize(total)}`
    : "No files yet";
  const dropLabel = targetTitle
    ? `Drop files for "${targetTitle}" or click to choose`
    : "Drop files here or click to choose";

  const buildViewerUrl = (file: TaskFile, editMode = false) => {
    const query = new URLSearchParams({ fileFilter: filter });
    if (file.subtaskId) query.set("subtaskId", String(file.subtaskId));
    if (editMode) query.set("edit", "true");
    return `/tasks/${taskId}/files/${file.id}?${query.toString()}`;
  };

  return (
    <Card>
      <div
        className="task-file-header"
        style={{ marginBottom: open ? "1rem" : 0 }}
      >
        <div>
          <h2 style={{ margin: 0 }}>Files</h2>
          <small className="muted">{summary}</small>
        </div>
        <div className="task-file-toolbar">
          {open && subtasks.length > 0 && (
            <div style={{ width: 190 }}>
              <Select
                value={filter}
                ariaLabel="File filter"
                options={options}
                onValueChange={(value) =>
                  setParams(
                    (previous) => {
                      const next = new URLSearchParams(previous);
                      next.set("fileFilter", value);
                      return next;
                    },
                    { replace: true },
                  )
                }
              />
            </div>
          )}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              gap: ".75rem",
            }}
          >
            <Button
              type="button"
              size="sm"
              variant="outline"
              aria-label={userOpen ? "Hide Files" : "Show Files"}
              style={{ border: "none" }}
              icon={userOpen ? <Minus size={15} /> : <Plus size={15} />}
              onClick={() => setUserOpen((v) => !v)}
            />
          </div>
        </div>
      </div>

      {canUpload && (
        <div style={{ display: open ? "block" : "none" }}>
          <FileDropzone
            ref={dropzoneRef}
            label={dropLabel}
            target={{ taskId, subtaskId: target }}
            onUploaded={merge}
            onError={setListError}
          />
        </div>
      )}

      {open && (
        <>
          {listError && <ErrorNotice error={listError} compact />}
          <div className="task-file-groups">
            {groups.map((group) => (
              <section className="task-file-group" key={group.key}>
                <div className="task-file-group-header">
                  <h4>
                    {group.title}{" "}
                    <span className="muted">({group.list.length})</span>
                  </h4>
                  {canUpload && (
                    <IconButton
                      aria-label={`Add files to ${group.title}`}
                      title={`Add files to ${group.title}`}
                      motion="clip"
                      icon={<Paperclip size={16} />}
                      onClick={() => dropzoneRef.current?.openPicker(group.subtaskId)}
                    />
                  )}
                </div>
                {group.list.length ? (
                  <div
                    className={
                      group.key === "task"
                        ? group.list.length > 8
                          ? "file-list-scrollable"
                          : "task-file-list"
                        : "file-list-scrollable"
                    }
                  >
                    {group.list.map((file) => {
                      const previewable = isPreviewable(file);
                      const canManage = isAllowedToManage(file);
                      const viewerHref = previewable ? buildViewerUrl(file) : undefined;

                      return (
                        <FileCard
                          key={file.id}
                          file={file}
                          canManage={canManage}
                          previewable={previewable}
                          href={viewerHref}
                          onOpen={() => {
                            if (previewable) navigate(buildViewerUrl(file));
                            else void downloadTaskFiles([file]);
                          }}
                          onEditContent={() => navigate(buildViewerUrl(file, true))}
                          onOpenRename={() => setRenamingFile(file)}
                          onDownload={() => void downloadTaskFiles([file])}
                          onDelete={() => setConfirm(file)}
                        />
                      );
                    })}
                  </div>
                ) : (
                  <div className="empty-state">
                    <p>No files</p>
                  </div>
                )}
              </section>
            ))}
            {!groups.length && <p className="muted">No files yet</p>}
          </div>
        </>
      )}

      {/* Shared FileRenameModal */}
      <FileRenameModal
        open={Boolean(renamingFile)}
        file={renamingFile}
        taskId={taskId}
        onClose={() => setRenamingFile(null)}
        onRenamed={(updated) => {
          setFiles((current) =>
            current.map((item) =>
              item.id === updated.id
                ? { ...item, originalName: updated.originalName, updatedAt: updated.updatedAt }
                : item,
            ),
          );
        }}
      />

      <ConfirmDialog
        open={!!confirm}
        title="Delete file"
        message={
          confirm
            ? `Delete "${confirm.originalName}"? This cannot be undone.`
            : ""
        }
        confirmLabel="Delete"
        onConfirm={() => void remove()}
        onClose={() => setConfirm(null)}
      />
    </Card>
  );
});
