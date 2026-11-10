import React, { useContext, useEffect, useRef, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { Card } from "../components/common/Card";
import {
  ArrowLeft,
  Check,
  Plus,
  Pencil,
  Trash2,
  MessageCircle,
  History,
  X,
  Minus,
  Crown,
  Paperclip,
} from "lucide-react";
import { Checkpoint, Task, User } from "../types";
import {
  changeTaskStatusApi,
  sendTaskToReviewApi,
  sendTaskBackApi,
  addTaskMemberApi,
  removeTaskMemberApi,
  changeTaskLeadApi,
  createCheckpointApi,
  createSubtaskApi,
  deleteCheckpointApi,
  deleteSubtaskApi,
  deleteTaskApi,
  getAdminUsersApi,
  getTaskApi,
  toggleCheckpointApi,
  updateSubtaskApi,
  updateTaskApi,
  getTaskUnreadApi,
  markTaskChatReadApi,
  markTaskActivityReadApi,
  getTaskMemberCandidatesApi,
  restoreTaskApi,
  uploadTaskFilesApi,
  listTaskFilesApi,
} from "../services/api";
import { useAuth } from "../context/AuthContext";
import { Button } from "../components/common/Button";
import { Badge } from "../components/common/Badge";
import { Input } from "../components/common/Input";
import { ProgressBar } from "./TaskPoolPage";
import { TaskFormModal } from "../components/tasks/TaskFormModal";
import { Select } from "../components/common/Select";
import { PeoplePicker } from "../components/common/PeoplePicker";
import { ConfirmDialog } from "../components/common/ConfirmDialog";
import { Modal } from "../components/common/Modal";
import { FieldError } from "../components/common/FieldError";
import { ChatDrawer } from "../components/tasks/ChatDrawer";
import { ActivityDrawer } from "../components/tasks/ActivityDrawer";
import "../styles/task-drawer.css";
import { TaskNavbarChatContext } from "../components/layout/AppLayout";
import { PersonProfileLink } from "../components/common/PersonProfileLink";
import { TaskDetailSkeleton, RequestError } from "../components/common/LoadingStates";
import { TimeTrackerControls } from "../components/tasks/TimeTrackerControls";
import { TaskFilesSection, TaskFilesSectionHandle } from "../components/tasks/TaskFilesSection";
import { FileDropzone, FileDropzoneHandle, getFileIcon } from "../components/tasks/FileDropzone";
import { formatSize } from "../components/tasks/fileUtils";
import { ErrorNotice } from "../components/common/ErrorNotice";
import { IconButton } from "../components/common/IconButton";
import { TaskFile } from "../types";
export const TaskDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isManager, user: currentUser } = useAuth();
  const setChatSlot = useContext(TaskNavbarChatContext);
  const subtaskDropzoneRef = useRef<FileDropzoneHandle>(null);
  const filesSectionRef = useRef<TaskFilesSectionHandle>(null);
  const [task, setTask] = useState<Task | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [edit, setEdit] = useState(false);
  const [subtaskTitle, setSubtaskTitle] = useState("");
  const [subtaskDeadline, setSubtaskDeadline] = useState("");
  const [assignees, setAssignees] = useState<number[]>([]);
  const [subtaskFiles, setSubtaskFiles] = useState<File[]>([]);
  const [subtaskSavedFiles, setSubtaskSavedFiles] = useState<TaskFile[]>([]);
  const [checkpoint, setCheckpoint] = useState("");
  const [error, setError] = useState("");
  const [refreshNotice, setRefreshNotice] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{
    subtaskTitle?: string;
    subtaskDeadline?: string;
    subtaskAssignees?: string;
    checkpoint?: string;
    reason?: string;
  }>({});
  const isCheckpointDone = (c: Checkpoint & { done?: boolean }) =>
    Boolean(c.done ?? c.isDone);
  const [confirm, setConfirm] = useState<null | {
    title: string;
    message: string;
    action: () => void;
  }>(null);
  const [sendBack, setSendBack] = useState(false);
  const [reason, setReason] = useState("");
  const [reopenSubtasks, setReopenSubtasks] = useState<number[]>([]);
  const [reopenCheckpoints, setReopenCheckpoints] = useState<number[]>([]);
  const [drawer, setDrawer] = useState<"chat" | "activity" | null>(null);
  const [outsideAssignee, setOutsideAssignee] = useState<User | null>(null);
  const [confirmAddMember, setConfirmAddMember] = useState(false);
  const [memberToAdd, setMemberToAdd] = useState<User | null>(null);
  const [restoreStatus, setRestoreStatus] = useState<
    "TODO" | "IN_PROGRESS" | "REVIEW"
  >("TODO");
  const [restoreDialog, setRestoreDialog] = useState(false);
  const [restoreError, setRestoreError] = useState("");
  const [restoring, setRestoring] = useState(false);
  const [deleteArchivedDialog, setDeleteArchivedDialog] = useState(false);
  const [deleteArchivedError, setDeleteArchivedError] = useState("");
  const [deletingArchived, setDeletingArchived] = useState(false);
  const [showMembers, setShowMembers] = useState(true);
  const [showAddMember, setShowAddMember] = useState(false);
  const [showAddSubtask, setShowAddSubtask] = useState(false);
  const [showSubtasks, setShowSubtasks] = useState(false);
  const [showCheckpoints, setShowCheckpoints] = useState(false);
  const sendToReview = () => {
    if (!task) return;
    const unfinished = task.subtasks.some((s) => s.status !== "DONE");
    if (unfinished) return;
    const unticked = task.checkpoints.filter(
      (c) => !isCheckpointDone(c),
    ).length;
    const actionFn = () => action(() => sendTaskToReviewApi(task.id));
    if (unticked > 0) {
      setConfirm({
        title: "Send to review",
        message: `${unticked} checkpoints are not ticked`,
        action: actionFn,
      });
    } else actionFn();
  };
  const [unread, setUnread] = useState({
    chatUnread: false,
    activityUnread: false,
    chatCount: 0,
    activityCount: 0,
  });
  const load = async () => {
    try {
      const t = await getTaskApi(Number(id));
      setTask(t);
      setUsers(
        t.members.map((m) => ({
          ...m.user,
          email: "",
          phone: "",
          role: m.user.role || "EMPLOYEE",
          status: "ACTIVE",
        })) as User[],
      );
      if (isManager)
        setUsers(
          (await getAdminUsersApi({ size: 100, status: "ACTIVE" })).content,
        );
      setError("");
      setRefreshNotice("");
    } catch (e: any) {
      if (task) setRefreshNotice("Task could not be refreshed.");
      else setError("The task could not be loaded.");
    }
  };
  useEffect(() => {
    load();
  }, [id, isManager]);
  useEffect(() => {
    if (!id) return;
    const refresh = () => {
      if (document.hidden) return;
      getTaskUnreadApi(Number(id))
        .then(setUnread)
        .catch(() => undefined);
    };
    refresh();
    const timer = window.setInterval(refresh, 10000);
    return () => window.clearInterval(timer);
  }, [id]);
  useEffect(() => {
    if (!id) return;
    const fetchFiles = () => {
      listTaskFilesApi(Number(id))
        .then(setSubtaskSavedFiles)
        .catch(() => undefined);
    };
    fetchFiles();
    const handler = (event: Event) => {
      if ((event as CustomEvent).detail?.taskId === Number(id)) fetchFiles();
    };
    window.addEventListener("task-files-changed", handler);
    return () => window.removeEventListener("task-files-changed", handler);
  }, [id]);
  useEffect(() => {
    if (!setChatSlot) return;
    const canUse = Boolean(
      task &&
      (isManager ||
        task.lead?.id === currentUser?.id ||
        task.members.some((m) => m.user.id === currentUser?.id)),
    );
    if (canUse)
      setChatSlot(
        <Button
          className={`navbar-chat-button${unread.chatUnread && drawer !== "chat" ? " has-unread" : ""}`}
          variant="outline"
          size="sm"
          icon={
            drawer === "chat" ? <X size={15} /> : <MessageCircle size={15} />
          }
          onClick={() => setDrawer((v) => (v === "chat" ? null : "chat"))}
        >
          Chat{unread.chatUnread && <span className="unread-dot" />}
        </Button>,
      );
    else setChatSlot(null);
    return () => setChatSlot(null);
  }, [
    setChatSlot,
    task,
    isManager,
    currentUser?.id,
    drawer,
    unread.chatUnread,
  ]);
  if (error && !task)
    return <RequestError message={error} onRetry={() => void load()} />;
  if (!task) return <TaskDetailSkeleton />;
  const isLead = task.lead?.id === currentUser?.id;
  const isMember = task.members.some(
    (member) => member.user.id === currentUser?.id,
  );
  const canEditTask = (isManager || isLead) && task.status !== "DONE";
  const canDeleteTask = isManager || (Boolean(task.selfReported) && task.createdBy?.id === currentUser?.id && task.status === "TODO");
  const canActOnTask = canEditTask || isMember;
  const subtasksOpen = canEditTask ? showSubtasks : true;
  const checkpointsOpen = canEditTask ? showCheckpoints : true;
  const action = async (fn: () => Promise<unknown>) => {
    try {
      await fn();
      await load();
    } catch (e: any) {
      setError("This change could not be saved. Please try again.");
    }
  };
  const memberIds = task.members.map((m) => m.user.id);
  const loadMemberCandidates = async (query: string): Promise<User[]> => {
    const response = await getTaskMemberCandidatesApi(task.id, query);
    return response.content.map(
      (candidate) =>
        ({
          ...candidate,
          email: "",
          phone: "",
          role: "EMPLOYEE",
          status: "ACTIVE",
        }) as User,
    );
  };
  return (
    <div
      className="task-detail-page"
      style={{ display: "grid", gap: "1.25rem" }}
    >
      <div className="page-header">
        <Link
          to="/tasks"
          style={{
            color: "var(--primary)",
            fontSize: "0.9rem",
            marginBottom: "0.5rem",
            display: "inline-flex",
            alignItems: "center",
            gap: "0.35rem",
            fontWeight: 500,
          }}
        >
          <ArrowLeft size={16} /> Back to Task Pool
        </Link>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            gap: "1rem",
            alignItems: "start",
            flexWrap: "wrap",
          }}
        >
          <div>
            <h1 className="page-title">
              {task.title}{" "}
              {task.status === "DONE" && (
                <Badge variant="default">Archived</Badge>
              )}
              {task.selfReported && (
                <Badge variant="default">Self-reported</Badge>
              )}
            </h1>
            {!canActOnTask && <Badge variant="default">View only</Badge>}
            <p className="page-subtitle">
              Deadline: {task.deadline} · {task.priority} priority
            </p>
            <small style={{ color: "var(--text-muted)" }}>
              Created by{" "}
              {task.createdBy
                ? `${task.createdBy.firstName} ${task.createdBy.lastName}`
                : "Unknown"}{" "}
              on{" "}
              {task.createdAt
                ? new Intl.DateTimeFormat("en-GB", {
                    timeZone: "Africa/Cairo",
                    day: "2-digit",
                    month: "short",
                    year: "numeric",
                  }).format(new Date(task.createdAt))
                : "—"}
            </small>
            {refreshNotice && (
              <div className="refresh-notice" role="status">
                {refreshNotice}
                <button type="button" onClick={() => setRefreshNotice("")}>
                  Dismiss
                </button>
              </div>
            )}
            {task.status === "DONE" && (
              <div className="form-error" style={{ marginTop: ".75rem" }}>
                This task is read-only. It will be deleted on{" "}
                {task.purgeAt
                  ? new Intl.DateTimeFormat("en-GB", {
                      timeZone: "Africa/Cairo",
                      day: "2-digit",
                      month: "short",
                      year: "numeric",
                    }).format(new Date(task.purgeAt))
                  : "a later date"}
                .
              </div>
            )}
          </div>
          <div style={{ display: "flex", gap: ".5rem", flexWrap: "wrap" }}>
            {canEditTask && (
              <Button
                variant="outline"
                size="sm"
                icon={<Pencil size={15} />}
                onClick={() => setEdit(true)}
              >
                Edit task
              </Button>
            )}
            {(isManager || (isLead && !task.selfReported)) && task.status === "DONE" && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setRestoreError("");
                  setRestoreDialog(true);
                }}
              >
                Restore
              </Button>
            )}

            {canActOnTask && (
              <Button
                className="activity-header-button"
                variant="outline"
                size="sm"
                icon={<History size={15} />}
                onClick={() =>
                  setDrawer((v) => (v === "activity" ? null : "activity"))
                }
              >
                Activity
                {unread.activityUnread && <span className="unread-dot" />}
              </Button>
            )}
            {canActOnTask && task.status !== "DONE" && (
              <TimeTrackerControls taskId={task.id} />
            )}
            {isManager && task.status === "DONE" && (
              <Button
                variant="danger"
                size="sm"
                icon={<Trash2 size={15} />}
                onClick={() => {
                  setDeleteArchivedError("");
                  setDeleteArchivedDialog(true);
                }}
              >
                Delete now
              </Button>
            )}
            {canDeleteTask && task.status !== "DONE" && (
              <Button
                variant="danger"
                size="sm"
                icon={<Trash2 size={15} />}
                onClick={() =>
                  setConfirm({
                    title: "Delete task",
                    message: "Delete this task and everything inside it?",
                    action: () => action(() => deleteTaskApi(task.id)),
                  })
                }
              >
                Delete
              </Button>
            )}
          </div>
        </div>
      </div>
      <Card>
        <div style={{ display: "grid", gap: ".7rem" }}>
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <strong>Overall progress</strong>
            <span>{task.progress}%</span>
          </div>
          <ProgressBar value={task.progress} />
          {task.description && (
            <p style={{ color: "var(--text-muted)", whiteSpace: "pre-wrap" }}>
              {task.description}
            </p>
          )}
          {canActOnTask && task.status !== "DONE" && (
            <div
              style={{
                display: "flex",
                flexWrap: "wrap",
                justifyContent: "space-between",
                alignItems: "center",
                gap: ".5rem",
                width: "100%",
                marginTop: "0.8rem",
              }}
            >
              <div style={{ display: "flex", gap: ".5rem", flexWrap: "wrap" }}>
                <Badge variant="primary">{task.status.replace("_", " ")}</Badge>
                {task.lead && (
                  <span style={{ color: "var(--text-muted)" }}>
                    Lead: {task.lead.firstName} {task.lead.lastName}
                  </span>
                )}
              </div>

              <div style={{ display: "flex", gap: ".5rem", flexWrap: "wrap" }}>
                {task.status === "TODO" && (
                  <Button
                    size="sm"
                    onClick={() =>
                      action(() => changeTaskStatusApi(task.id, "IN_PROGRESS"))
                    }
                  >
                    Start work
                  </Button>
                )}
                {(task.status === "TODO" || task.status === "IN_PROGRESS") &&
                  task.subtasks.every((s) => s.status === "DONE") && (
                    <Button size="sm" onClick={sendToReview}>
                      Send to review
                    </Button>
                  )}
                {task.status === "REVIEW" && (isManager || !task.selfReported) && (
                  <Button
                    size="sm"
                    onClick={() =>
                      action(() => changeTaskStatusApi(task.id, "DONE"))
                    }
                  >
                    Mark done
                  </Button>
                )}
                {task.status === "REVIEW" && (isManager || !task.selfReported) && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => {
                      setReason("");
                      setFieldErrors((p) => ({ ...p, reason: undefined }));
                      setSendBack(true);
                    }}
                  >
                    Send back
                  </Button>
                )}
              </div>
            </div>
          )}
        </div>
      </Card>
      {
        <Card>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              gap: ".75rem",
              marginBottom: showMembers ? "1rem" : 0,
            }}
          >
            <h2 style={{ margin: 0 }}>Members</h2>
            {canEditTask && (
              <Button
                type="button"
                size="sm"
                variant="outline"
                style={{ border: "none" }}
                icon={showMembers ? <Minus size={15} /> : <Plus size={15} />}
                onClick={() => setShowMembers((v) => !v)}
              ></Button>
            )}
          </div>
          {showMembers && (
            <>
              <div className="task-member-list">
                {task.members.map((m) => (
                  <div className="task-member-row" key={m.user.id}>
                    <PersonProfileLink
                      userId={m.user.id}
                      currentUserId={currentUser?.id}
                    >
                      <span>
                        {m.user.firstName} {m.user.lastName}
                      </span>
                    </PersonProfileLink>
                    <Badge variant={m.role === "LEAD" ? "primary" : "default"}>
                      {m.role === "LEAD" ? "Lead" : "Member"}
                    </Badge>
                    <span
                      style={{
                        marginLeft: "auto",
                        display: "flex",
                        gap: ".35rem",
                      }}
                    >
                      {canEditTask && m.role !== "LEAD" && (
                        <IconButton
                          aria-label={`Promote ${m.user.firstName} ${m.user.lastName} to Lead`}
                          title={`Promote ${m.user.firstName} ${m.user.lastName} to Lead`}
                          className="btn-promote-lead"
                          icon={<Crown size={16} />}
                          onClick={() =>
                            action(() => changeTaskLeadApi(task.id, m.user.id))
                          }
                        />
                      )}
                      {canEditTask && (
                        <Button type="button" variant="icon" size="icon" motion="delete"
                        aria-label="Remove member" title="Remove member" icon={<Trash2 size={16} />}
                      
                          onClick={() =>
                            setConfirm({
                              title: "Remove member",
                              message: `Remove ${m.user.firstName} ${m.user.lastName}?`,
                              action: () =>
                                action(() =>
                                  removeTaskMemberApi(task.id, m.user.id),
                                ),
                            })
                          }
                        >
                        </Button>
                      )}
                    </span>
                  </div>
                ))}
              </div>
              {canEditTask && (
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: ".75rem",
                    marginBottom: showAddMember ? ".75rem" : 0,
                  }}
                >
                  <h3 style={{ margin: 0 }}>Add member</h3>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    style={{ border: "none" }}
                    icon={
                      showAddMember ? <Minus size={15} /> : <Plus size={15} />
                    }
                    onClick={() => setShowAddMember((v) => !v)}
                  ></Button>
                </div>
              )}
              {canEditTask && showAddMember && (
                <PeoplePicker
                  label=""
                  users={users}
                  value={[]}
                  taskMemberIds={memberIds}
                  addMode
                  onChange={() => {}}
                  onOutsideSelect={setMemberToAdd}
                  loadOptions={loadMemberCandidates}
                />
              )}
            </>
          )}
        </Card>
      }
      <Card>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: ".75rem",
            marginBottom: subtasksOpen ? "1rem" : 0,
          }}
        >
          <h2 style={{ margin: 0 }}>Subtasks</h2>
          {canEditTask && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              style={{ border: "none" }}
              aria-label={showSubtasks ? "Hide subtasks" : "Show subtasks"}
              icon={showSubtasks ? <Minus size={15} /> : <Plus size={15} />}
              onClick={() => setShowSubtasks((v) => !v)}
            />
          )}
        </div>
        {!canEditTask && task.subtasks.length === 0 && (
          <p className="empty-state">There are no subtasks yet</p>
        )}
        {subtasksOpen && task.subtasks.length > 0 && (
          <div style={{ display: "grid", gap: ".75rem" }}>
            {task.subtasks.map((s) => (
              <div
                key={s.id}
                style={{
                  padding: ".85rem",
                  border: "1px solid var(--border-light)",
                  borderRadius: 12,
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    gap: ".5rem",
                  }}
                >
                  <strong>{s.title}</strong>
                  <span>{s.status.replace("_", " ")}</span>
                </div>
                <small style={{ color: "var(--text-muted)" }}>
                  Due {s.deadline} ·{" "}
                  {s.assignees
                    .map((a) => `${a.firstName} ${a.lastName}`)
                    .join(", ") || "Unassigned"}
                </small>
                <div
                  style={{
                    display: "flex",
                    gap: ".5rem",
                    alignItems: "center",
                    marginTop: ".5rem",
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <ProgressBar value={s.progress} />
                  </div>
                  <span>{s.progress}%</span>
                  {canActOnTask && task.status !== "DONE" && (
                    <>
                      <div style={{ width: 140 }}>
                        <Select
                          value={s.status}
                          onValueChange={(status) =>
                            action(() =>
                              updateSubtaskApi(task.id, s.id, {
                                status: status as any,
                              }),
                            )
                          }
                          options={[
                            { value: "TODO", label: "To do" },
                            { value: "IN_PROGRESS", label: "In progress" },
                            { value: "DONE", label: "Done" },
                          ]}
                          ariaLabel="Subtask status"
                        />
                      </div>
                      {canActOnTask && (
                        <IconButton
                          aria-label={`Add files to ${s.title}`}
                          title={`Add files to ${s.title}`}
                          motion="clip"
                          icon={<Paperclip size={16} />}
                          onClick={() => filesSectionRef.current?.openPicker(s.id)}
                        />
                      )}
                      {canEditTask && (
                        <Button type="button" variant="icon" size="icon" motion="delete"
                        aria-label="Delete subtask" title="Delete subtask" icon={<Trash2 size={16} />}
                        onClick={() =>
                            setConfirm({
                              title: "Delete subtask",
                              message: "Delete this subtask?",
                              action: () =>
                                action(() => deleteSubtaskApi(task.id, s.id)),
                            })
                          }
                        >
                        </Button>
                      )}
                    </>
                  )}
                </div>
                {subtaskSavedFiles.filter((f) => f.subtaskId === s.id).length > 0 && (
                  <div style={{ marginTop: ".6rem" }}>
                    <div style={{ marginBottom: ".3rem" }}>
                      <small className="muted">
                        Files ({subtaskSavedFiles.filter((f) => f.subtaskId === s.id).length})
                      </small>
                    </div>
                    <div className="file-list-scrollable">
                      {subtaskSavedFiles
                        .filter((f) => f.subtaskId === s.id)
                        .map((file) => (
                          <div key={file.id} className="task-file-row">
                            <span className="task-file-icon">
                              {getFileIcon(file.originalName, file.contentType)}
                            </span>
                            <span className="task-file-name" title={file.originalName} dir="auto">
                              {file.originalName}
                            </span>
                            <small className="muted">{formatSize(file.sizeBytes)}</small>
                          </div>
                        ))}
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
        {canEditTask && task.status !== "DONE" && (
          <>
            {showSubtasks && task.subtasks.length > 0 && (
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  gap: ".75rem",
                  marginTop: "1rem",
                }}
              >
                <h3 style={{ margin: 0 }}>Add subtask</h3>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  aria-label={
                    showAddSubtask
                      ? "Hide add subtask form"
                      : "Show add subtask form"
                  }
                  style={{ border: "none" }}
                  icon={
                    showAddSubtask ? <Minus size={15} /> : <Plus size={15} />
                  }
                  onClick={() => setShowAddSubtask((v) => !v)}
                />
              </div>
            )}
            {showSubtasks && (task.subtasks.length === 0 || showAddSubtask) && (
              <form
                noValidate
                onSubmit={(e) => {
                  e.preventDefault();

                  const errs: typeof fieldErrors = {};
                  if (!subtaskTitle.trim())
                    errs.subtaskTitle = "Subtask title is required.";
                  if (!subtaskDeadline)
                    errs.subtaskDeadline = "Deadline is required.";
                  if (!assignees.length)
                    errs.subtaskAssignees = "Select at least one assignee.";

                  setFieldErrors((p) => ({
                    ...p,
                    subtaskTitle: errs.subtaskTitle,
                    subtaskDeadline: errs.subtaskDeadline,
                    subtaskAssignees: errs.subtaskAssignees,
                  }));
                  if (Object.keys(errs).length) return;

                  action(async () => {
                    const created = await createSubtaskApi(task.id, {
                      title: subtaskTitle.trim(),
                      deadline: subtaskDeadline,
                      assigneeUserIds: assignees,
                      confirmAddMember,
                    });
                    if (subtaskDropzoneRef.current?.hasFiles()) {
                      await subtaskDropzoneRef.current.uploadPending(
                        task.id,
                        created.id,
                      );
                    }
                  });
                  setSubtaskTitle("");
                  setSubtaskDeadline("");
                  setAssignees([]);
                  setSubtaskFiles([]);
                  setConfirmAddMember(false);
                }}
                style={{ display: "grid", gap: ".75rem", marginTop: "1rem" }}
              >
                <div>
                  <Input
                    label=""
                    placeholder="New subtask"
                    value={subtaskTitle}
                    onChange={(e) => {
                      setSubtaskTitle(e.target.value);
                      setFieldErrors((p) => ({
                        ...p,
                        subtaskTitle: undefined,
                      }));
                    }}
                    error={fieldErrors.subtaskTitle}
                  />
                </div>

                <div>
                  <Input
                    label=""
                    type="date"
                    value={subtaskDeadline}
                    onChange={(e) => {
                      setSubtaskDeadline(e.target.value);
                      setFieldErrors((p) => ({
                        ...p,
                        subtaskDeadline: undefined,
                      }));
                    }}
                    error={fieldErrors.subtaskDeadline}
                  />
                </div>

                <div>
                  <PeoplePicker
                    label="Assignees"
                    users={users}
                    value={assignees}
                    taskMemberIds={memberIds}
                    onOutsideSelect={setOutsideAssignee}
                    loadOptions={loadMemberCandidates}
                    onChange={(v) => {
                      setAssignees(v);
                      setFieldErrors((p) => ({
                        ...p,
                        subtaskAssignees: undefined,
                      }));
                    }}
                    error={fieldErrors.subtaskAssignees}
                  />
                </div>
                <div style={{ display : "flex" , justifyContent :"flex-end", marginTop: "0.25rem" }}>
                <FileDropzone
                    ref={subtaskDropzoneRef}
                    buttonMode
                    target="pending"
                    pendingFiles={subtaskFiles}
                    onPendingFilesChange={setSubtaskFiles}
                  />
                  <Button type="submit" icon={<Plus size={16} />} style={{marginRight :"0.5rem" , marginLeft :"0.5rem"}}>
                    Add subtask
                  </Button>
                </div>
              </form>
            )}
          </>
        )}
      </Card>
      <Card>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: ".75rem",
            marginBottom: checkpointsOpen ? "1rem" : 0,
          }}
        >
          <h2 style={{ margin: 0 }}>Checkpoints</h2>
          {canEditTask && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              style={{ border: "none" }}
              aria-label={
                showCheckpoints ? "Hide checkpoints" : "Show checkpoints"
              }
              icon={showCheckpoints ? <Minus size={15} /> : <Plus size={15} />}
              onClick={() => setShowCheckpoints((v) => !v)}
            />
          )}
        </div>
        {checkpointsOpen && (
          <>
            {!canEditTask && task.checkpoints.length === 0 && (
              <p className="empty-state">There are no checkpoints yet</p>
            )}
            {task.checkpoints.length > 0 && (
              <div style={{ display: "grid", gap: ".45rem" }}>
                {task.checkpoints.map((c) => {
                  const done = isCheckpointDone(c);
                  return (
                    <div
                      key={c.id}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.2rem",
                        margin: "0.4rem",
                      }}
                    >
                      <span
                        style={{
                          textDecoration: done ? "line-through" : "none",
                          flex: 1,
                          color: done ? "var(--text-muted)" : "",
                        }}
                      >
                        {c.title}
                      </span>

                      {canEditTask && task.status !== "DONE" && (
                        <div
                          style={{
                            display: "flex",
                            gap: "0.4rem",
                            alignItems: "center",
                          }}
                        >
                          <Button
                            type="button"
                            variant="icon"
                            size="icon"
                            motion="check"
                            aria-label="Save name"
                            title="Save name"
                            icon={<Check size={16} />}
                            onClick={() =>
                              action(() => toggleCheckpointApi(task.id, c.id))
                            }
                          />

                          <Button
                            type="button"
                            variant="icon"
                            size="icon"
                            motion="delete"
                            aria-label="Delete check point"
                            title="Delete check point"
                            icon={<Trash2 size={16} />}
                            onClick={() =>
                              action(() => deleteCheckpointApi(task.id, c.id))
                            }
                          />
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
            {canEditTask && (
              <form
                noValidate
                onSubmit={(e) => {
                  e.preventDefault();
                  if (!checkpoint.trim()) {
                    setFieldErrors((p) => ({
                      ...p,
                      checkpoint: "Checkpoint title is required.",
                    }));
                    return;
                  }
                  action(() => createCheckpointApi(task.id, checkpoint));
                  setCheckpoint("");
                }}
                style={{
                  display: "flex",
                  gap: ".5rem",
                  marginTop: task.checkpoints.length > 0 ? "1rem" : 0,
                  alignItems: "flex-start",
                }}
              >
                <div style={{ flex: 1 }}>
                  <Input
                    label=""
                    placeholder="New checkpoint"
                    value={checkpoint}
                    onChange={(e) => {
                      setCheckpoint(e.target.value);
                      setFieldErrors((p) => ({ ...p, checkpoint: undefined }));
                    }}
                    error={fieldErrors.checkpoint}
                  />
                </div>
                <Button type="submit" icon={<Plus size={16} />}>
                  Add
                </Button>
              </form>
            )}
          </>
        )}
      </Card>
      {canActOnTask && (
        <TaskFilesSection
          ref={filesSectionRef}
          taskId={task.id}
          subtasks={task.subtasks.map((subtask) => ({
            id: subtask.id,
            title: subtask.title,
          }))}
          currentUserId={currentUser?.id}
          canUpload={task.status !== "DONE"}
          canDelete={isManager || isLead}
        />
      )}
      {edit && (
        <TaskFormModal
          task={task}
          users={users}
          onClose={() => setEdit(false)}
          onSave={async (data) => {
            await updateTaskApi(task.id, {
              title: data.title,
              description: data.description,
              deadline: data.deadline,
              priority: data.priority,
            });
            if (data.files?.length) {
              try {
                await uploadTaskFilesApi(task.id, data.files);
              } catch {
                throw new Error(
                  "Task saved, but one or more files could not be uploaded.",
                );
              }
            }
            await load();
          }}
        />
      )}
      {confirm && (
        <ConfirmDialog
          open
          title={confirm.title}
          message={confirm.message}
          onClose={() => setConfirm(null)}
          onConfirm={() => {
            const fn = confirm.action;
            setConfirm(null);
            fn();
          }}
          confirmLabel="Delete"
        />
      )}
      {restoreDialog && (
        <Modal
          isOpen
          title="Restore archived task"
          onClose={() => {
            if (!restoring) setRestoreDialog(false);
          }}
        >
          <p>
            This returns the task to work and keeps its subtasks, comments and
            activity.
          </p>
          {restoreError && <ErrorNotice error={restoreError} />}
          <label
            className="field-label"
            style={{ display: "grid", gap: ".4rem", marginTop: "1rem" }}
          >
            Restore status
            <Select
              ariaLabel="Restore status"
              value={restoreStatus}
              onValueChange={(value) =>
                setRestoreStatus(value as typeof restoreStatus)
              }
              options={[
                { value: "TODO", label: "To do" },
                { value: "IN_PROGRESS", label: "In progress" },
                { value: "REVIEW", label: "Review" },
              ]}
            />
          </label>
          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              gap: ".75rem",
              marginTop: "1.25rem",
            }}
          >
            <Button
              variant="outline"
              disabled={restoring}
              onClick={() => setRestoreDialog(false)}
            >
              Cancel
            </Button>
            <Button
              disabled={restoring}
              onClick={async () => {
                setRestoring(true);
                setRestoreError("");
                try {
                  await restoreTaskApi(task.id, restoreStatus);
                  await load();
                  setRestoreDialog(false);
                } catch {
                  setRestoreError(
                    "The task could not be restored. Please try again.",
                  );
                } finally {
                  setRestoring(false);
                }
              }}
            >
              Restore
            </Button>
          </div>
        </Modal>
      )}
      {deleteArchivedDialog && (
        <Modal
          isOpen
          title="Delete archived task now"
          onClose={() => {
            if (!deletingArchived) setDeleteArchivedDialog(false);
          }}
        >
          <p>
            This cannot be undone. The task, its subtasks, checkpoints,
            comments, activity and notifications will be removed now instead of
            on the automatic deletion date.
          </p>
          {deleteArchivedError && <ErrorNotice error={deleteArchivedError} />}
          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              gap: ".75rem",
              marginTop: "1.25rem",
            }}
          >
            <Button
              variant="outline"
              disabled={deletingArchived}
              onClick={() => setDeleteArchivedDialog(false)}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              disabled={deletingArchived}
              onClick={async () => {
                setDeletingArchived(true);
                setDeleteArchivedError("");
                try {
                  await deleteTaskApi(task.id);
                  setDeleteArchivedDialog(false);
                  navigate("/tasks?tab=archived");
                } catch {
                  setDeleteArchivedError(
                    "The task could not be deleted. Please try again.",
                  );
                } finally {
                  setDeletingArchived(false);
                }
              }}
            >
              Delete now
            </Button>
          </div>
        </Modal>
      )}
      {sendBack && (
        <Modal isOpen title="Send task back" onClose={() => setSendBack(false)}>
          <form
            noValidate
            onSubmit={(e) => {
              e.preventDefault();
              if (reason.trim().length < 3) {
                setFieldErrors((p) => ({
                  ...p,
                  reason: "Enter a reason of at least 3 characters.",
                }));
                return;
              }
              action(() =>
                sendTaskBackApi(task.id, {
                  reason,
                  subtaskIds: reopenSubtasks,
                  checkpointIds: reopenCheckpoints,
                }),
              ).then(() => setSendBack(false));
            }}
          >
            <label className="field-label">
              <h4 style={{ marginBottom: "1rem", marginTop: "0" }}>Reason</h4>
              <textarea
                autoFocus
                className="form-input"
                value={reason}
                style={{ alignContent: "center" }}
                onChange={(e) => {
                  setReason(e.target.value);
                  setFieldErrors((p) => ({ ...p, reason: undefined }));
                }}
              />
            </label>
            <FieldError message={fieldErrors.reason} />
            <div style={{ display: "grid", gap: ".4rem", marginTop: "1rem" }}>
              <strong>Reopen subtasks</strong>
              {task.subtasks
                .filter((s) => s.status === "DONE")
                .map((s) => (
                  <label key={s.id}>
                    <input
                      type="checkbox"
                      checked={reopenSubtasks.includes(s.id)}
                      onChange={(e) =>
                        setReopenSubtasks((v) =>
                          e.target.checked
                            ? [...v, s.id]
                            : v.filter((id) => id !== s.id),
                        )
                      }
                    />{" "}
                    {s.title}
                  </label>
                ))}
              <strong>Reopen checkpoints</strong>
              {task.checkpoints.filter(isCheckpointDone).map((c) => (
                <label key={c.id}>
                  <input
                    type="checkbox"
                    checked={reopenCheckpoints.includes(c.id)}
                    onChange={(e) =>
                      setReopenCheckpoints((v) =>
                        e.target.checked
                          ? [...v, c.id]
                          : v.filter((id) => id !== c.id),
                      )
                    }
                  />{" "}
                  {c.title}
                </label>
              ))}
            </div>
            <div style={{ textAlign: "right", marginTop: "1rem" }}>
              <Button type="submit">Send back</Button>
            </div>
          </form>
        </Modal>
      )}
      <ChatDrawer
        open={drawer === "chat"}
        onClose={() => setDrawer(null)}
        taskId={task.id}
        subtasks={task.subtasks}
        currentUserId={currentUser?.id}
        canWrite={canActOnTask && task.status !== "DONE"}
        users={task.members.map((m) => m.user)}
        unread={unread.chatUnread}
        onMarkRead={() =>
          markTaskChatReadApi(task.id).then(() =>
            setUnread((v) => ({ ...v, chatUnread: false, chatCount: 0 })),
          )
        }
      />
      <ActivityDrawer
        open={drawer === "activity"}
        onClose={() => setDrawer(null)}
        taskId={task.id}
        unread={unread.activityUnread}
        onMarkRead={() =>
          markTaskActivityReadApi(task.id).then(() =>
            setUnread((v) => ({
              ...v,
              activityUnread: false,
              activityCount: 0,
            })),
          )
        }
      />
      {outsideAssignee && (
        <ConfirmDialog
          open
          title="Add subtask assignee"
          message={`${outsideAssignee.firstName} ${outsideAssignee.lastName} does not work on this task. Assign this subtask to them and add them to the task?`}
          confirmLabel="Assign and add"
          onClose={() => setOutsideAssignee(null)}
          onConfirm={() => {
            setAssignees((v) => [...v, outsideAssignee.id]);
            setConfirmAddMember(true);
            setOutsideAssignee(null);
          }}
        />
      )}
      {memberToAdd && (
        <ConfirmDialog
          open
          title="Add member"
          message={`${memberToAdd.firstName} ${memberToAdd.lastName} is not on this task yet. Add them to the task?`}
          confirmLabel="Add to task"
          onClose={() => setMemberToAdd(null)}
          onConfirm={() => {
            const id = memberToAdd.id;
            setMemberToAdd(null);
            void action(() => addTaskMemberApi(task.id, id));
          }}
        />
      )}
    </div>
  );
};
