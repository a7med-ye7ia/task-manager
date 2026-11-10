import React, { useEffect, useState } from "react";
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  Download,
  Edit3,
  Trash2,
} from "lucide-react";
import {
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import {
  deleteTaskFileApi,
  downloadTaskFileApi,
  getTaskFileDetailApi,
  getTaskFileTextApi,
  editTaskFileContentApi,
  ApiError,
} from "../services/api";
import { TaskFileDetail } from "../types";
import { Button } from "../components/common/Button";
import { Card } from "../components/common/Card";
import { ConfirmDialog } from "../components/common/ConfirmDialog";
import { ErrorNotice } from "../components/common/ErrorNotice";
import { PageLoader } from "../components/common/LoadingStates";
import { useAuth } from "../context/AuthContext";
import {
  formatDateLabel,
  formatSize,
  getFileBadgeLabel,
  isCodeEditorType,
  isEditableType,
  resolveViewerType,
} from "../components/tasks/fileUtils";
import { ImageViewer } from "../components/tasks/viewers/ImageViewer";
import { TextDocViewer } from "../components/tasks/viewers/TextDocViewer";
import { NoViewerFallback } from "../components/tasks/viewers/NoViewerFallback";
import { FileRenameModal } from "../components/tasks/FileRenameModal";

// Lazy-loaded heavy viewers
const PdfViewer = React.lazy(() => import("../components/tasks/viewers/PdfViewer").then((m) => ({ default: m.PdfViewer })));
const CsvViewer = React.lazy(() => import("../components/tasks/viewers/CsvViewer").then((m) => ({ default: m.CsvViewer })));
const MarkdownViewer = React.lazy(() => import("../components/tasks/viewers/MarkdownViewer").then((m) => ({ default: m.MarkdownViewer })));
const CodeIdeViewer = React.lazy(() => import("../components/tasks/viewers/CodeIdeViewer").then((m) => ({ default: m.CodeIdeViewer })));
const DocxViewer = React.lazy(() => import("../components/tasks/viewers/DocxViewer").then((m) => ({ default: m.DocxViewer })));
const FileEditor = React.lazy(() => import("../components/tasks/viewers/FileEditor").then((m) => ({ default: m.FileEditor })));

// Legacy helper preserved for existing unit tests
export const fileViewerKind = (contentType: string): string => {
  if (contentType.startsWith("image/")) return "image";
  if (contentType === "application/pdf") return "pdf";
  if (contentType.startsWith("video/")) return "video";
  if (contentType.startsWith("audio/")) return "audio";
  if (contentType === "text/csv") return "csv";
  if (contentType === "text/markdown") return "markdown";
  if (contentType === "text/x-tex") return "latex";
  if (contentType.includes("spreadsheet")) return "xlsx";
  if (contentType.includes("wordprocessing")) return "docx";
  if (contentType === "application/zip") return "zip";
  return "download";
};

export const TaskFilePage: React.FC = () => {
  const { taskId, fileId } = useParams();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth() as any;

  const id = Number(taskId);
  const fid = Number(fileId);

  const [detail, setDetail] = useState<TaskFileDetail | null>(null);
  const [blobUrl, setBlobUrl] = useState<string>("");
  const [textContent, setTextContent] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>("");
  const [is404, setIs404] = useState<boolean>(false);
  const [retryCount, setRetryCount] = useState<number>(0);

  // Edit & Rename states
  const [isEditing, setIsEditing] = useState<boolean>(params.get("edit") === "true");
  const [isDirty, setIsDirty] = useState<boolean>(false);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [is409, setIs409] = useState<boolean>(false);
  const [showEditMenu, setShowEditMenu] = useState<boolean>(false);
  const [renameModalOpen, setRenameModalOpen] = useState<boolean>(false);

  // Navigation / leave confirm dialog
  const [leaveConfirmOpen, setLeaveConfirmOpen] = useState<boolean>(false);
  const [pendingNavigation, setPendingNavigation] = useState<string | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState<boolean>(false);

  // Close edit menu on click outside
  useEffect(() => {
    if (!showEditMenu) return;
    const close = () => setShowEditMenu(false);
    window.addEventListener("click", close);
    return () => window.removeEventListener("click", close);
  }, [showEditMenu]);

  // 1. Fetch metadata and content
  useEffect(() => {
    if (!id || !fid) return;
    let active = true;

    void (async () => {
      setLoading(true);
      setError("");
      setIs404(false);

      try {
        const subtaskIdParam = Number(params.get("subtaskId")) || undefined;
        const d = await getTaskFileDetailApi(id, fid, subtaskIdParam);
        if (!active) return;
        setDetail(d);

        const viewerType = resolveViewerType(d);

        // Authenticated fetch based on viewer type
        if (viewerType === "image" || viewerType === "pdf") {
          const blob = await downloadTaskFileApi(id, fid);
          if (!active) return;
          const url = URL.createObjectURL(blob);
          setBlobUrl((prev) => {
            if (prev) URL.revokeObjectURL(prev);
            return url;
          });
        } else if (
          viewerType === "csv" ||
          viewerType === "txt-log" ||
          viewerType === "markdown" ||
          viewerType === "code"
        ) {
          const raw = await getTaskFileTextApi(id, fid);
          if (!active) return;
          setTextContent(raw);
        }
      } catch (cause) {
        if (!active) return;
        if (cause instanceof ApiError && cause.status === 404) {
          setIs404(true);
          setError("This file no longer exists");
        } else if (cause instanceof ApiError && cause.status === 403) {
          setError("You do not have access to this file");
        } else {
          setError(cause instanceof Error ? cause.message : "The file could not be loaded. Try again.");
        }
      } finally {
        if (active) setLoading(false);
      }
    })();

    return () => {
      active = false;
    };
  }, [id, fid, params, retryCount]);

  // Clean up blob URL on unmount
  useEffect(() => {
    return () => {
      if (blobUrl) URL.revokeObjectURL(blobUrl);
    };
  }, [blobUrl]);

  // Download handler
  const handleDownload = async () => {
    try {
      const blob = await downloadTaskFileApi(id, fid);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = detail?.originalName || "download";
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "The file could not be downloaded.");
    }
  };

  // Delete handler
  const handleDelete = async () => {
    try {
      await deleteTaskFileApi(id, fid);
      navigate(`/tasks/${id}`);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "File could not be deleted.");
    } finally {
      setDeleteConfirmOpen(false);
    }
  };

  // Save content
  const handleSaveContent = async (newContent: string) => {
    setIsSaving(true);
    setSaveError(null);
    setIs409(false);
    try {
      const updated = await editTaskFileContentApi(id, fid, newContent, detail?.updatedAt);
      setTextContent(newContent);
      if (detail) {
        setDetail({
          ...detail,
          updatedAt: updated.updatedAt || new Date().toISOString(),
          editedBy: updated.editedBy ?? user?.id ?? 1,
        });
      }
      setIsEditing(false);
      setIsDirty(false);
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 409) {
        setIs409(true);
        setSaveError("This file was modified by another user. Reload to view the latest version.");
      } else {
        setSaveError(cause instanceof Error ? cause.message : "The file could not be saved.");
      }
    } finally {
      setIsSaving(false);
    }
  };

  // Reload after 409 conflict
  const handleReload = async () => {
    setIs409(false);
    setSaveError(null);
    try {
      const freshText = await getTaskFileTextApi(id, fid);
      setTextContent(freshText);
      const subtaskIdParam = Number(params.get("subtaskId")) || undefined;
      const freshDetail = await getTaskFileDetailApi(id, fid, subtaskIdParam);
      setDetail(freshDetail);
      setIsDirty(false);
    } catch (cause) {
      setSaveError(cause instanceof Error ? cause.message : "Could not reload the file.");
    }
  };

  // Safe navigation interceptor when dirty
  const navigateWithDirtyCheck = (to: string) => {
    if (isDirty) {
      setPendingNavigation(to);
      setLeaveConfirmOpen(true);
    } else {
      navigate(to);
    }
  };

  const handleCancelEdit = () => {
    if (isDirty) {
      setPendingNavigation("CANCEL_EDIT");
      setLeaveConfirmOpen(true);
    } else {
      setIsEditing(false);
      setSaveError(null);
      setIs409(false);
    }
  };

  const handleConfirmLeave = () => {
    setLeaveConfirmOpen(false);
    setIsDirty(false);
    if (pendingNavigation === "CANCEL_EDIT") {
      setIsEditing(false);
      setSaveError(null);
      setIs409(false);
    } else if (pendingNavigation) {
      navigate(pendingNavigation);
    }
    setPendingNavigation(null);
  };

  const handleBackToTask = () => navigateWithDirtyCheck(`/tasks/${id}`);

  // 404 Not Found state
  if (is404) {
    return (
      <main className="page-container">
        <ErrorNotice error="This file no longer exists" />
        <Button
          type="button"
          variant="outline"
          icon={<ArrowLeft size={16} />}
          onClick={() => navigate(`/tasks/${id}`)}
        >
          Back to task
        </Button>
      </main>
    );
  }

  // General load error
  if (error && !detail) {
    return (
      <main className="page-container">
        <ErrorNotice error={error} />
        <div style={{ display: "flex", gap: "0.75rem", marginTop: "1rem" }}>
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              setError("");
              setRetryCount((v) => v + 1);
            }}
          >
            Try again
          </Button>
          <Button
            type="button"
            variant="outline"
            icon={<ArrowLeft size={16} />}
            onClick={() => navigate(`/tasks/${id}`)}
          >
            Back to task
          </Button>
        </div>
      </main>
    );
  }

  if (loading || !detail) {
    return <PageLoader label="Loading file" />;
  }

  const isManager = user?.role === "MANAGER";
  const isUploader = user?.id === detail.uploaderId;
  const isLead = Boolean(user?.role === "LEAD");
  // Permitted users: uploader, Lead, Manager
  const canManage = isManager || isLead || isUploader;
  const isText = isEditableType(detail);
  const isEdited = Boolean(detail.editedBy || (detail.updatedAt && detail.updatedAt !== detail.createdAt));

  const viewerType = resolveViewerType(detail);
  const isCode = isCodeEditorType(detail);
  const subtaskParam = detail.subtaskId ? `?subtaskId=${detail.subtaskId}` : "";

  const handleEditButtonClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (isText) {
      setShowEditMenu((prev) => !prev);
    } else {
      setRenameModalOpen(true);
    }
  };

  return (
    <main className="page-container file-page">
      {/* Header: file name with dir="auto", type badge, size, uploader, date, Download, Edit, Delete as allowed, Back and Next */}
      <div className="file-page-header">
        <div className="file-header-left">
          <Button
            type="button"
            variant="outline"
            size="sm"
            icon={<ArrowLeft size={15} />}
            onClick={handleBackToTask}
            aria-label="Back to task"
          >
            Back to task
          </Button>
        </div>

        <div className="file-header-title-area">
          <div className="file-header-title-row">
            <h1 dir="auto" className="file-main-title">{detail.originalName}</h1>
            <span className="file-type-badge">{getFileBadgeLabel(detail)}</span>
          </div>
          <p className="file-header-meta muted">
            {formatSize(detail.sizeBytes)} · Uploaded by {detail.uploaderName} · {formatDateLabel(detail.createdAt)}
            {isEdited && ` · edited ${formatDateLabel(detail.updatedAt)}`}
            {detail.taskTitle ? ` · ${detail.taskTitle}` : ""}
            {detail.subtaskTitle ? ` / ${detail.subtaskTitle}` : ""}
          </p>
        </div>

        <div className="file-header-actions">
          {/* Section navigation: Previous & Next */}
          <div className="file-section-nav">
            {detail.previousId ? (
              <Button
                type="button"
                variant="outline"
                size="sm"
                icon={<ChevronLeft size={15} />}
                onClick={() => navigateWithDirtyCheck(`/tasks/${id}/files/${detail.previousId}${subtaskParam}`)}
                aria-label="Previous file"
                title="Previous file in section"
              >
                Back
              </Button>
            ) : null}

            {detail.nextId ? (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => navigateWithDirtyCheck(`/tasks/${id}/files/${detail.nextId}${subtaskParam}`)}
                aria-label="Next file"
                title="Next file in section"
              >
                Next <ChevronRight size={15} style={{ marginInlineStart: 4 }} />
              </Button>
            ) : null}
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            icon={<Download size={15} />}
            onClick={() => void handleDownload()}
            aria-label="Download file"
            title="Download file"
          >
            Download
          </Button>

          {canManage && !isEditing && (
            <div className="header-edit-dropdown-wrap" style={{ position: "relative" }}>
              <Button
                type="button"
                variant="outline"
                size="sm"
                icon={<Edit3 size={15} />}
                onClick={handleEditButtonClick}
                aria-label="Edit file"
                title="Edit file"
              >
                Edit
              </Button>
              {showEditMenu && (
                <div
                  className="file-edit-menu"
                  onClick={(e) => e.stopPropagation()}
                  style={{
                    position: "absolute",
                    top: "100%",
                    right: 0,
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
                      setShowEditMenu(false);
                      setRenameModalOpen(true);
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
                      setShowEditMenu(false);
                      setIsEditing(true);
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
          )}

          {canManage && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              icon={<Trash2 size={15} />}
              onClick={() => setDeleteConfirmOpen(true)}
              aria-label="Delete file"
              title="Delete file"
            >
              Delete
            </Button>
          )}
        </div>
      </div>

      {/* Main viewer card */}
      <Card className="file-viewer-main-card">
        {error && (
          <div className="viewer-inline-error">
            <ErrorNotice error={error} />
          </div>
        )}

        <React.Suspense fallback={<PageLoader label="Loading viewer..." />}>
          {isEditing ? (
            <FileEditor
              file={detail}
              initialContent={textContent}
              isCodeEditor={isCode}
              onSave={handleSaveContent}
              onCancel={handleCancelEdit}
              isSaving={isSaving}
              saveError={saveError}
              is409={is409}
              onReload={handleReload}
              onDirtyChange={setIsDirty}
            />
          ) : (
            <>
              {viewerType === "image" && <ImageViewer file={detail} blobUrl={blobUrl} />}
              {viewerType === "pdf" && <PdfViewer file={detail} blobUrl={blobUrl} />}
              {viewerType === "csv" && <CsvViewer file={detail} textContent={textContent} />}
              {viewerType === "txt-log" && <TextDocViewer file={detail} textContent={textContent} />}
              {viewerType === "markdown" && <MarkdownViewer file={detail} textContent={textContent} />}
              {viewerType === "code" && <CodeIdeViewer file={detail} textContent={textContent} />}
              {viewerType === "docx" && (
                <DocxViewer
                  file={detail}
                  taskId={id}
                  fileId={fid}
                  onDownload={handleDownload}
                />
              )}
              {viewerType === "no-viewer" && (
                <NoViewerFallback file={detail} onDownload={handleDownload} />
              )}
            </>
          )}
        </React.Suspense>
      </Card>

      {/* Rename dialog (shared dialog) */}
      <FileRenameModal
        open={renameModalOpen}
        file={detail}
        taskId={id}
        onClose={() => setRenameModalOpen(false)}
        onRenamed={(updated) => {
          setDetail((prev) =>
            prev
              ? {
                  ...prev,
                  originalName: updated.originalName,
                  updatedAt: updated.updatedAt || prev.updatedAt,
                }
              : prev,
          );
        }}
      />

      {/* Shared ConfirmDialog for unsaved changes warning */}
      <ConfirmDialog
        open={leaveConfirmOpen}
        title="Discard unsaved changes?"
        message="You have unsaved changes that will be lost. Are you sure you want to discard them?"
        confirmLabel="Discard"
        onConfirm={handleConfirmLeave}
        onClose={() => {
          setLeaveConfirmOpen(false);
          setPendingNavigation(null);
        }}
      />

      {/* Shared ConfirmDialog for delete file */}
      <ConfirmDialog
        open={deleteConfirmOpen}
        title="Delete file"
        message={`Delete "${detail.originalName}"? This cannot be undone.`}
        confirmLabel="Delete"
        onConfirm={() => void handleDelete()}
        onClose={() => setDeleteConfirmOpen(false)}
      />
    </main>
  );
};
