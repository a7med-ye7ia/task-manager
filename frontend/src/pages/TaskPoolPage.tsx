import React, { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { FolderKanban, Plus, CalendarDays, Search, X, Trash2 } from "lucide-react";
import "../styles/task-pool.css";
import { Task, User } from "../types";
import {
  getTaskPoolApi,
  createTaskApi,
  uploadTaskFilesApi,
  getAdminUsersApi,
  deleteTaskApi,
} from "../services/api";
import { useAuth } from "../context/AuthContext";
import { Card } from "../components/common/Card";
import { Button } from "../components/common/Button";
import { Badge } from "../components/common/Badge";
import { TaskFormModal } from "../components/tasks/TaskFormModal";
import { Select } from "../components/common/Select";
import { PersonProfileLink } from "../components/common/PersonProfileLink";
import { Modal } from "../components/common/Modal";
import { RequestError, TaskPoolSkeleton } from "../components/common/LoadingStates";

const badge = (value: string) => (
  <Badge
    variant={
      value === "DONE"
        ? "success"
        : value === "HIGH"
          ? "danger"
          : value === "REVIEW"
            ? "warning"
            : "primary"
    }
  >
    {value.replace("_", " ")}
  </Badge>
);
const cairoDate = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("en-GB", {
        timeZone: "Africa/Cairo",
        day: "2-digit",
        month: "short",
        year: "numeric",
      }).format(new Date(value))
    : "—";
export const ProgressBar: React.FC<{ value: number }> = ({ value }) => (
  <div
    style={{
      height: 8,
      background: "#e2e8f0",
      borderRadius: 99,
      overflow: "hidden",
    }}
  >
    <div
      style={{
        height: "100%",
        width: `${value}%`,
        background: "var(--primary-gradient)",
        borderRadius: 99,
      }}
    />
  </div>
);

export const TaskPoolPage: React.FC = () => {
  const { isManager, user: currentUser, pendingCount } = useAuth();
  const [params, setParams] = useSearchParams();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [total, setTotal] = useState(0);
  const [users, setUsers] = useState<User[]>([]);
  const [status, setStatus] = useState("");
  const [priority, setPriority] = useState("");
  const [search, setSearch] = useState("");
  const [deadlineFrom, setDeadlineFrom] = useState("");
  const [deadlineTo, setDeadlineTo] = useState("");
  const [sort, setSort] = useState("deadline");
  const [myTasks, setMyTasks] = useState(params.get("review") === "1");
  const [iLead, setILead] = useState(params.get("review") === "1");
  const [reviewOnly, setReviewOnly] = useState(params.get("review") === "1");
  const [tab, setTab] = useState<"active" | "archived">(params.get("tab") === "archived" ? "archived" : "active");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");
  const [refreshNotice, setRefreshNotice] = useState("");
  const [deleteTaskId, setDeleteTaskId] = useState<number | null>(null);
  const [deleteError, setDeleteError] = useState("");
  const [deleting, setDeleting] = useState(false);
  const tasksRef = useRef<Task[]>([]);
  const load = async () => {
    const hasData = tasksRef.current.length > 0;
    setLoading(!hasData);
    setRefreshNotice("");
    try {
      const response = await getTaskPoolApi({
        page,
        search,
        status: reviewOnly ? "REVIEW" : status === "ALL" ? "" : status,
        priority: priority === "ALL" ? "" : priority,
        deadlineFrom,
        deadlineTo,
        myTasks: reviewOnly && isManager ? false : myTasks,
        iLead: reviewOnly && !isManager ? true : (reviewOnly && isManager ? false : iLead),
        archived: tab === "archived",
        sort,
      });
      tasksRef.current = response.content;
      setTasks(response.content);
      setTotal(response.totalElements);
      setTotalPages(response.totalPages);
      setError("");
    } catch {
      if (!hasData) setError("Tasks could not be loaded.");
      else setRefreshNotice("Tasks could not be refreshed.");
    } finally {
      setLoading(false);
    }
    if (isManager) {
      try { setUsers((await getAdminUsersApi({ size: 100, status: "ACTIVE" })).content); }
      catch { /* task data remains usable when the manager directory refresh fails */ }
    }
  };
  useEffect(() => {
    void load();
    const refreshOnFocus = () => {
      if (!document.hidden) void load();
    };
    window.addEventListener("focus", refreshOnFocus);
    const timer = window.setInterval(refreshOnFocus, 30000);
    return () => {
      window.removeEventListener("focus", refreshOnFocus);
      window.clearInterval(timer);
    };
  }, [
    isManager,
    page,
    search,
    status,
    priority,
    deadlineFrom,
    deadlineTo,
    myTasks,
    iLead,
    tab,
    sort,
    reviewOnly,
  ]);
  useEffect(() => {
    const next = new URLSearchParams(params);
    reviewOnly ? next.set("review", "1") : next.delete("review");
    setParams(next, { replace: true });
  }, [reviewOnly]);
  useEffect(() => {
    const next = params.get("review") === "1";
    setReviewOnly((current) => (current === next ? current : next));
  }, [params]);
  useEffect(() => {
    const next = params.get("tab") === "archived" ? "archived" : "active";
    setTab((current) => (current === next ? current : next));
  }, [params]);
  const selectTab = (next: "active" | "archived") => {
    setTab(next);
    setPage(0);
    const nextParams = new URLSearchParams(params);
    next === "archived" ? nextParams.set("tab", "archived") : nextParams.delete("tab");
    setParams(nextParams, { replace: false });
  };
  const filtered = tasks;
  const visible = tasks;
  const hasFilters = Boolean(
    search ||
    status ||
    priority ||
    deadlineFrom ||
    deadlineTo ||
    sort !== "deadline" ||
    myTasks ||
    iLead,
  );
  const clearFilters = () => {
    setSearch("");
    setStatus("");
    setPriority("");
    setDeadlineFrom("");
    setDeadlineTo("");
    setSort("deadline");
    setMyTasks(false);
    setILead(false);
    setReviewOnly(false);
    setPage(0);
  };
  const myTasksCount = tasks.filter(
    (t) =>
      t.members.some((m) => m.user.id === currentUser?.id) ||
      t.subtasks.some((s) => s.assignees.some((a) => a.id === currentUser?.id)),
  ).length;
  const ledTasksCount = tasks.filter(
    (t) => t.lead?.id === currentUser?.id,
  ).length;
  const shownCount = iLead ? ledTasksCount : myTasksCount;
  return (
    <div style={{ display: "grid", gap: "1.5rem" }}>
      <div
        className="page-header"
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          gap: "1rem",
          flexWrap: "wrap",
        }}
      >
        <div>
          <h1 className="page-title">Tasks</h1>
          <p className="page-subtitle">
            Showing {tasks.length} of {total} tasks
          </p>
        </div>
        <Button icon={<Plus size={18} />} onClick={() => setShowForm(true)}>
          New task
        </Button>
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit,minmax(220px,1fr))",
          gap: "1rem",
        }}
      >
        <Card
          className={myTasks ? "home-card-active" : ""}
          onClick={() => {
            const next = !myTasks;
            setMyTasks(next);
            if (!next) {
              setILead(false);
              setReviewOnly(false);
            }
            setPage(0);
          }}
          style={{ cursor: "pointer", position: "relative" }}
        >
          <h2>{myTasks ? (iLead ? "Tasks I lead" : "My tasks") : "All tasks"}</h2>
          <p>
            {myTasks
              ? `${shownCount} ${
                  iLead
                    ? shownCount === 1
                      ? "led task"
                      : "led tasks"
                    : shownCount === 1
                      ? "assigned task"
                      : "assigned tasks"
                }`
              : "Show every task"}
          </p>
          <button
            type="button"
            className={`filter-pill ${iLead ? "active" : ""}`}
            onClick={(e) => {
              e.stopPropagation();
              if (reviewOnly) return;
              const next = !iLead;
              setILead(next);
              setMyTasks(next);
              setPage(0);
            }}
            style={{ position: "absolute", right: ".75rem", bottom: ".75rem" }}
          >
            I lead
          </button>
        </Card>
        <Card
          className={reviewOnly ? "home-card-active" : ""}
          onClick={() => {
            const next = !reviewOnly;
            setReviewOnly(next);
            if (next) {
              setMyTasks(true);
              setILead(true);
            }
            setPage(0);
          }}
          style={{ cursor: "pointer" }}
        >
          <h2>Waiting for my review</h2>
          <p>
            {
              tasks.filter(
                (t) =>
                  t.status === "REVIEW" &&
                  (isManager || (t.lead?.id === currentUser?.id && !t.selfReported)),
              ).length
            }{" "}
            {tasks.filter(
              (t) =>
                t.status === "REVIEW" &&
                (isManager || (t.lead?.id === currentUser?.id && !t.selfReported)),
            ).length === 1
              ? "task awaiting review"
              : "tasks awaiting review"}
          </p>
        </Card>
        {isManager && (
          <Link
            to="/admin/pending"
            style={{
              textDecoration: "none",
              color: "inherit",
              display: "block",
            }}
          >
            <Card>
              <h2>Pending sign-ups</h2>
              <p>{pendingCount} requests</p>
            </Card>
          </Link>
        )}
      </div>
      <div role="tablist" aria-label="Task pool tabs" style={{ display: "flex", gap: ".5rem" }}>
        <button type="button" role="tab" aria-selected={tab === "active"} className={`filter-pill ${tab === "active" ? "active" : ""}`} onClick={() => selectTab("active")}>Active</button>
        <button type="button" role="tab" aria-selected={tab === "archived"} className={`filter-pill ${tab === "archived" ? "active" : ""}`} onClick={() => selectTab("archived")}>Archived</button>
      </div>
      <Card
        className="task-filter-card"
        style={{
          padding: "1rem",
          display: "flex",
          gap: "0.75rem",
          alignItems: "center",
        }}
      >
        <div className="task-search">
          <Search size={17} />
          <input
            className="form-input"
            placeholder="Search title"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </div>
        <div className="task-filter-grid">
          <label>
            Deadline from
            <input
              className="form-input"
              type="date"
              aria-label="Deadline from"
              value={deadlineFrom}
              onChange={(e) => {
                setDeadlineFrom(e.target.value);
                setPage(0);
              }}
            />
          </label>
          <label>
            Deadline to
            <input
              className="form-input"
              type="date"
              aria-label="Deadline to"
              value={deadlineTo}
              onChange={(e) => {
                setDeadlineTo(e.target.value);
                setPage(0);
              }}
            />
          </label>
          <label>
            Status
            <Select
              value={status}
              onValueChange={setStatus}
              placeholder="All statuses"
              options={[
                { value: "ALL", label: "All statuses" },
                { value: "TODO", label: "To do" },
                { value: "IN_PROGRESS", label: "In progress" },
                { value: "REVIEW", label: "Review" },
                { value: "DONE", label: "Done" },
              ]}
            />
          </label>
          <label>
            Priority
            <Select
              value={priority || "ALL"}
              onValueChange={(v) => {
                setPriority(v === "ALL" ? "" : v);
                setPage(0);
              }}
              placeholder="All priorities"
              options={[
                { value: "ALL", label: "All priorities" },
                { value: "LOW", label: "Low" },
                { value: "MEDIUM", label: "Medium" },
                { value: "HIGH", label: "High" },
              ]}
            />
          </label>
          <label>
            Sort by
            <Select
              value={sort}
              onValueChange={setSort}
              options={[
                { value: "deadline", label: "Sort deadline" },
                { value: "priority", label: "Sort priority" },
                { value: "created", label: "Sort created" },
              ]}
            />
          </label>
        </div>
        <div className="task-filter-actions">
          {myTasks && (
            <button
              type="button"
              className="filter-pill active"
              style={iLead || reviewOnly ? { cursor: "default" } : undefined}
              onClick={() => {
                if (iLead || reviewOnly) return;
                setMyTasks(false);
                setPage(0);
              }}
            >
              My tasks {!(iLead || reviewOnly) && <X size={13} />}
            </button>
          )}
          {iLead && (
            <button
              type="button"
              className="filter-pill active"
              style={reviewOnly ? { cursor: "default" } : undefined}
              onClick={() => {
                if (reviewOnly) return;
                setILead(false);
                setPage(0);
              }}
            >
              I lead {!reviewOnly && <X size={13} />}
            </button>
          )}
          {reviewOnly && (
            <button
              type="button"
              className="filter-pill active"
              onClick={() => {
                setReviewOnly(false);
                setPage(0);
              }}
            >
              Waiting for review <X size={13} />
            </button>
          )}
          {hasFilters && (
            <button
              type="button"
              className="clear-filters"
              onClick={clearFilters}
            >
              Clear filters
            </button>
          )}
        </div>
      </Card>
      {error && tasks.length === 0 && <RequestError message={error} onRetry={() => void load()} />}
      {refreshNotice && tasks.length > 0 && <div className="refresh-notice" role="status">{refreshNotice}<button type="button" onClick={() => setRefreshNotice("")}>Dismiss</button></div>}
      {loading ? (
        <TaskPoolSkeleton />
      ) : filtered.length === 0 && !error ? (
        <Card>
          <div className="empty-state">
            <FolderKanban size={42} />
            <h3>{tab === "archived" ? "No archived tasks" : "No active tasks"}</h3>
            <p>{tab === "archived" ? "Archived tasks will appear here after they are completed." : "Active tasks will appear here when they are created."}</p>
          </div>
        </Card>
      ) : (
        <>
          <div className="task-column-header">
            <span>Task</span>
            <span>Status</span>
            <span>Deadline</span>
            <span>Progress</span>
          </div>
          <div className="task-list">
            {visible.map((task) => (
              <Link
                key={task.id}
                to={`/tasks/${task.id}`}
                style={{ textDecoration: "none", color: "inherit" }}
              >
                <Card
                  className="task-row"
                  style={{
                    display: "grid",
                    gridTemplateColumns: isManager && tab === "archived" ? "minmax(220px, 2fr) 1fr 1fr 1.5fr 40px" : "minmax(220px, 2fr) 1fr 1fr 1.5fr",
                    gap: "1rem",
                    marginBottom : "1rem",
                    alignItems: "center",
                    position: "relative",
                  }}
                >
                  {task.mentionUnreadCount ? (
                    <span
                      className="task-unread-badge"
                      role="status"
                      aria-label={`${task.mentionUnreadCount} unread mentions`}
                      title={`${task.mentionUnreadCount} unread mentions`}
                    >
                      {task.mentionUnreadCount > 9 ? "9+" : task.mentionUnreadCount}
                    </span>
                  ) : null}
                  <div>
                    <div
                      style={{
                        display: "flex",
                        gap: "0.5rem",
                        alignItems: "center",
                        flexWrap: "wrap",
                      }}
                    >
                      <strong>{task.title}</strong>
                      {badge(task.priority)}
                      {task.selfReported && (
                        <Badge variant="default">Self-reported</Badge>
                      )}
                    </div>
                    <small style={{ color: "var(--text-muted)" }}>
                      Lead:{" "}
                      {task.lead ? <PersonProfileLink userId={task.lead.id} currentUserId={currentUser?.id}>{`${task.lead.firstName} ${task.lead.lastName}`}</PersonProfileLink> : "Unassigned"}{" "}
                      · Created {cairoDate(task.createdAt)} {task.status === "DONE" && task.purgeAt ? `· Deletes in ${Math.max(0, Math.ceil((new Date(task.purgeAt).getTime() - Date.now()) / 86400000))} days` : ""}
                    </small>
                  </div>
                  <div>{badge(task.status)}</div>
                  <div
                    style={{
                      color: task.overdue
                        ? "var(--danger)"
                        : "var(--text-muted)",
                      display: "flex",
                      gap: ".35rem",
                      alignItems: "center",
                    }}
                  >
                    <CalendarDays size={16} />
                    {task.overdue ? "Overdue: " : ""}
                    {task.deadline}
                  </div>
                  <div>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        fontSize: ".8rem",
                        marginBottom: ".35rem",
                      }}
                    >
                      <span>Progress</span>
                      <span>{task.progress}%</span>
                    </div>
                    <ProgressBar value={task.progress} />
                  </div>
                  {isManager && tab === "archived" && <button type="button" className="task-row-icon-action" title="Delete now" aria-label="Delete now" onClick={(event) => { event.preventDefault(); event.stopPropagation(); setDeleteError(""); setDeleteTaskId(task.id); }}><Trash2 size={16} /></button>}
                </Card>
              </Link>
            ))}
          </div>
        </>
      )}
      {totalPages > 1 && (
        <div
          style={{ display: "flex", justifyContent: "center", gap: ".75rem" }}
        >
          <Button
            variant="outline"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Previous
          </Button>
          <span style={{ padding: ".6rem" }}>
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="outline"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      )}
      {showForm && (
        <TaskFormModal
          users={users}
          onClose={() => setShowForm(false)}
          onSave={async (data) => {
            const created = await createTaskApi(data);
            if (data.files?.length) {
              try { await uploadTaskFilesApi(created.id, data.files); }
              catch { throw new Error("Task saved, but one or more files could not be uploaded."); }
            }
            await load();
          }}
        />
      )}
      {deleteTaskId !== null && (
        <Modal isOpen title="Delete archived task now" onClose={() => { if (!deleting) setDeleteTaskId(null); }}>
          <p>This cannot be undone. The task and everything inside it will be removed now instead of on the automatic deletion date.</p>
          {deleteError && <div className="form-error" role="alert">{deleteError}</div>}
          <div style={{ display: "flex", justifyContent: "flex-end", gap: ".75rem", marginTop: "1.25rem" }}>
            <Button variant="outline" disabled={deleting} onClick={() => setDeleteTaskId(null)}>Cancel</Button>
            <Button variant="danger" disabled={deleting} onClick={async () => { setDeleting(true); setDeleteError(""); try { await deleteTaskApi(deleteTaskId); setDeleteTaskId(null); await load(); } catch { setDeleteError("The task could not be deleted. Please try again."); } finally { setDeleting(false); } }}>Delete now</Button>
          </div>
        </Modal>
      )}
    </div>
  );
};
