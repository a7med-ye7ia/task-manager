import React, { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { CalendarDays, Pencil, Trash2, UserX, RotateCcw } from "lucide-react";
import { PublicProfile, User, UserTaskSummary } from "../types";
import {
  ApiError,
  deactivateUserApi,
  deleteUserApi,
  editUserApi,
  getAdminUserApi,
  getUserTasksApi,
  getPublicProfileApi,
  request,
  restoreUserApi,
} from "../services/api";
import { UserAvatar } from "../components/avatar/UserAvatar";
import { Card } from "../components/common/Card";
import { Button } from "../components/common/Button";
import { Badge } from "../components/common/Badge";
import { Modal } from "../components/common/Modal";
import { Select } from "../components/common/Select";
import { ConfirmDialog } from "../components/common/ConfirmDialog";
import { ProgressBar } from "./TaskPoolPage";
import { useAuth } from "../context/AuthContext";
import { PageLoader, RequestError } from "../components/common/LoadingStates";
import { UserReportView } from "../components/reports/UserReportView";
import { DateRangePicker, QuickRangeKey, computeQuickRangeDates } from "../components/reports/DateRangePicker";

export const UserDetailsPage: React.FC = () => {
  const { id } = useParams();
  const location = useLocation();
  const viewerMode = location.pathname.startsWith("/users/");
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();
  const [params, setParams] = useSearchParams();

  // Tab & Date Range URL state
  const tabParam = params.get("tab");
  const tab = tabParam === "tasks" ? "tasks" : tabParam === "report" ? "report" : "overview";

  const rangeParam = (params.get("range") || "last30Days") as QuickRangeKey;
  const defaultDates = computeQuickRangeDates(rangeParam);
  const fromParam = params.get("from") || defaultDates.from;
  const toParam = params.get("to") || defaultDates.to;

  const [user, setUser] = useState<User | null>(null);
  const [tasks, setTasks] = useState<UserTaskSummary[]>([]);
  const [error, setError] = useState("");
  const [refreshNotice, setRefreshNotice] = useState("");
  const [edit, setEdit] = useState(false);
  const [form, setForm] = useState<any>({});
  const [deleteConfirm, setDeleteConfirm] = useState(false);
  const [publicProfile, setPublicProfile] = useState<PublicProfile | null>(null);
  const [publicFilter, setPublicFilter] = useState("ALL");

  const load = async () => {
    try {
      if (viewerMode) {
        const [u, t] = await Promise.all([
          getPublicProfileApi(Number(id)),
          request<UserTaskSummary[]>(`/api/users/${id}/public/tasks?filter=${publicFilter}`),
        ]);
        setPublicProfile(u);
        setTasks(t);
        return;
      }
      const [u, t] = await Promise.all([
        getAdminUserApi(Number(id)),
        getUserTasksApi(Number(id), tab === "tasks" ? fromParam : undefined, tab === "tasks" ? toParam : undefined),
      ]);
      setUser(u);
      setTasks(t);
      setForm(u);
      setError("");
      setRefreshNotice("");
    } catch (e) {
      const message = e instanceof ApiError ? e.getFormattedMessage() : "Could not load the user.";
      if (viewerMode ? publicProfile : user) setRefreshNotice("Profile could not be refreshed.");
      else setError(message);
    }
  };

  useEffect(() => {
    if (viewerMode && currentUser?.id === Number(id)) {
      navigate("/profile", { replace: true });
      return;
    }
    load();
  }, [id, viewerMode, publicFilter, currentUser?.id, tab, fromParam, toParam]);

  const handleTabChange = (nextTab: "overview" | "tasks" | "report") => {
    const next = new URLSearchParams(params);
    if (nextTab === "overview") {
      next.delete("tab");
    } else {
      next.set("tab", nextTab);
    }
    setParams(next, { replace: true });
  };

  const handleRangeChange = (newKey: QuickRangeKey, newFrom: string, newTo: string) => {
    const next = new URLSearchParams(params);
    next.set("range", newKey);
    next.set("from", newFrom);
    next.set("to", newTo);
    setParams(next, { replace: true });
  };

  if (error && !user && !publicProfile) return <RequestError message={error} onRetry={() => void load()} />;
  if (!user && !publicProfile) return <PageLoader label="Loading profile" />;

  const refreshNoticeView = refreshNotice && (
    <div className="refresh-notice" role="status">
      {refreshNotice}
      <button type="button" onClick={() => setRefreshNotice("")}>Dismiss</button>
    </div>
  );

  if (viewerMode && publicProfile) {
    return (
      <div style={{ display: "grid", gap: "1.25rem" }}>
        {refreshNoticeView}
        <Link to="/tasks" style={{ color: "var(--primary)" }}>Back to tasks</Link>
        <Card>
          <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <UserAvatar
              userId={publicProfile.id}
              avatarPath={publicProfile.avatarPath}
              firstName={publicProfile.firstName}
              lastName={publicProfile.lastName}
              size={72}
            />
            <div>
              <h1 className="page-title" style={{ margin: 0 }}>
                {publicProfile.active ? `${publicProfile.firstName} ${publicProfile.lastName}` : "This user is no longer active"}
              </h1>
              {publicProfile.active && (
                <div style={{ display: "flex", gap: ".5rem", alignItems: "center", marginTop: "0.4rem" }}>
                  <Badge variant={publicProfile.role === "MANAGER" ? "primary" : "default"}>{publicProfile.role === "MANAGER" ? "Manager" : "Employee"}</Badge>
                  <Badge variant="success">Active</Badge>
                </div>
              )}
            </div>
          </div>
        </Card>
        {publicProfile.active && (
          <Card>
            <h2 style={{ margin: "0 0 1rem 0", fontSize: "1.1rem" }}>Contact</h2>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "1rem" }}>
              <div>
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Phone</span>
                <div style={{ fontWeight: 600, fontSize: "1rem" }}>{publicProfile.phone || "—"}</div>
              </div>
            </div>
          </Card>
        )}
        {publicProfile.active && (
          <Card>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
              <h2>Works on</h2>
              <Select
                value={publicFilter}
                onValueChange={setPublicFilter}
                options={[
                  { value: "ALL", label: "All" },
                  { value: "LEADS", label: "Leads" },
                  { value: "COMMON", label: "Common" },
                ]}
              />
            </div>
            <div style={{ display: "grid", gap: ".5rem" }}>
              {tasks.map((t) => (
                <Link key={t.id} to={`/tasks/${t.id}`} className="task-row">
                  <span>{t.title}</span>
                  <Badge>{t.userRole === "LEAD" ? "Lead" : "Member"}</Badge>
                </Link>
              ))}
              {tasks.length === 0 && <p className="empty-state">No tasks found.</p>}
            </div>
          </Card>
        )}
      </div>
    );
  }

  if (!user) return <PageLoader label="Loading profile" />;

  const active = tasks.filter((t) => t.status !== "DONE").length;
  const done = tasks.filter((t) => t.status === "DONE").length;
  const tasksLed = tasks.filter((t) => t.userRole === "LEAD").length;
  const now = new Date();
  const currentYearMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  const doneThisMonth = tasks.filter((t) => t.status === "DONE" && (!t.deadline || t.deadline.startsWith(currentYearMonth))).length;

  const run = async (action: () => Promise<unknown>) => {
    try {
      await action();
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.getFormattedMessage() : "Could not save this change.");
    }
  };

  return (
    <div style={{ display: "grid", gap: "1.25rem" }}>
      {refreshNoticeView}
      <Link to="/admin/users" style={{ color: "var(--primary)" }}>
        Back to users
      </Link>

      {/* Top Header Card */}
      <Card>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            gap: "1rem",
            flexWrap: "wrap",
            alignItems: "center",
          }}
        >
          <div style={{ display: "flex", gap: "1rem", alignItems: "center" }}>
            <UserAvatar
              userId={user.id}
              avatarPath={user.avatarPath}
              firstName={user.firstName}
              lastName={user.lastName}
              size={88}
            />
            <div>
              <h1 className="page-title" style={{ margin: 0 }}>
                {user.firstName} {user.lastName}
              </h1>
              <div style={{ display: "flex", gap: ".5rem", alignItems: "center", marginTop: "0.4rem" }}>
                <Badge variant={user.role === "MANAGER" ? "primary" : "default"}>
                  {user.role}
                </Badge>
                <Badge variant={user.status === "ACTIVE" ? "success" : "danger"}>
                  {user.deletedAt ? "DELETED" : user.status}
                </Badge>
              </div>
            </div>
          </div>
          <div style={{ display: "flex", gap: ".5rem", flexWrap: "wrap" }}>
            <Button
              variant="outline"
              size="sm"
              icon={<Pencil size={15} />}
              onClick={() => setEdit(true)}
            >
              Edit details
            </Button>
            {user.status === "ACTIVE" && !user.deletedAt && (
              <Button
                variant="outline"
                size="sm"
                icon={<UserX size={15} />}
                onClick={() => run(() => deactivateUserApi(user.id))}
              >
                Deactivate
              </Button>
            )}
            {(user.status === "DISABLED" || user.deletedAt) && (
              <Button
                variant="outline"
                size="sm"
                icon={<RotateCcw size={15} />}
                onClick={() => run(() => restoreUserApi(user.id))}
              >
                Reactivate
              </Button>
            )}
            {!user.deletedAt && (
              <Button
                variant="danger"
                size="sm"
                icon={<Trash2 size={15} />}
                onClick={() => setDeleteConfirm(true)}
              >
                Delete
              </Button>
            )}
          </div>
        </div>
      </Card>

      {/* Profile Tabs */}
      <div className="profile-tab-bar">
        <button
          type="button"
          className={`profile-tab-button ${tab === "overview" ? "active" : ""}`}
          onClick={() => handleTabChange("overview")}
        >
          Overview
        </button>
        <button
          type="button"
          className={`profile-tab-button ${tab === "tasks" ? "active" : ""}`}
          onClick={() => handleTabChange("tasks")}
        >
          Tasks
        </button>
        <button
          type="button"
          className={`profile-tab-button ${tab === "report" ? "active" : ""}`}
          onClick={() => handleTabChange("report")}
        >
          Report
        </button>
      </div>

      {/* Tab: Overview */}
      {tab === "overview" && (
        <div style={{ display: "grid", gap: "1.25rem" }} data-testid="user-overview-tab">
          {/* Section: Contact */}
          <Card>
            <h2 style={{ margin: "0 0 1rem 0", fontSize: "1.1rem" }}>Contact</h2>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
                gap: "1.25rem",
              }}
            >
              <div>
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Email</span>
                <div
                  style={{
                    fontWeight: 600,
                    fontSize: "1rem",
                    wordBreak: "break-all",
                    overflowWrap: "anywhere",
                  }}
                >
                  {user.email}
                </div>
              </div>
              <div>
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Phone</span>
                <div style={{ fontWeight: 600, fontSize: "1rem" }}>{user.phone || "—"}</div>
              </div>
            </div>
          </Card>

          {/* Section: Account */}
          <Card>
            <h2 style={{ margin: "0 0 1rem 0", fontSize: "1.1rem" }}>Account</h2>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
                gap: "1.25rem",
              }}
            >
              <div>
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Role</span>
                <div style={{ fontWeight: 600, fontSize: "1rem" }}>{user.role}</div>
              </div>
              <div>
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Status</span>
                <div style={{ fontWeight: 600, fontSize: "1rem" }}>{user.deletedAt ? "DELETED" : user.status}</div>
              </div>
              <div>
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Hired Date</span>
                <div style={{ fontWeight: 600, fontSize: "1rem" }}>{user.hiredAt || "—"}</div>
              </div>
              {user.deletedAt && (
                <div>
                  <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Deactivated Date</span>
                  <div style={{ fontWeight: 600, fontSize: "1rem", color: "var(--danger)" }}>
                    {user.deletedAt.split("T")[0]}
                  </div>
                </div>
              )}
              <div>
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Member Since</span>
                <div style={{ fontWeight: 600, fontSize: "1rem" }}>
                  {user.createdAt ? user.createdAt.split("T")[0] : "—"}
                </div>
              </div>
            </div>
          </Card>

          {/* Section: Work */}
          <Card>
            <h2 style={{ margin: "0 0 1rem 0", fontSize: "1.1rem" }}>Work</h2>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
                gap: "1rem",
                marginBottom: "1.25rem",
              }}
            >
              <div
                style={{
                  background: "var(--bg-subtle, #f8fafc)",
                  padding: "1rem",
                  borderRadius: "8px",
                  border: "1px solid var(--border)",
                }}
              >
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Active Tasks</span>
                <div style={{ fontWeight: 700, fontSize: "1.5rem", color: "var(--primary)" }}>{active}</div>
              </div>
              <div
                style={{
                  background: "var(--bg-subtle, #f8fafc)",
                  padding: "1rem",
                  borderRadius: "8px",
                  border: "1px solid var(--border)",
                }}
              >
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Tasks Led</span>
                <div style={{ fontWeight: 700, fontSize: "1.5rem", color: "var(--primary)" }}>{tasksLed}</div>
              </div>
              <div
                style={{
                  background: "var(--bg-subtle, #f8fafc)",
                  padding: "1rem",
                  borderRadius: "8px",
                  border: "1px solid var(--border)",
                }}
              >
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Done This Month</span>
                <div style={{ fontWeight: 700, fontSize: "1.5rem", color: "var(--success)" }}>{doneThisMonth}</div>
              </div>
              <div
                style={{
                  background: "var(--bg-subtle, #f8fafc)",
                  padding: "1rem",
                  borderRadius: "8px",
                  border: "1px solid var(--border)",
                }}
              >
                <span style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>Total Done</span>
                <div style={{ fontWeight: 700, fontSize: "1.5rem", color: "var(--success)" }}>{done}</div>
              </div>
            </div>
            <div
              style={{
                display: "flex",
                gap: "0.75rem",
                flexWrap: "wrap",
                borderTop: "1px solid var(--border-light)",
                paddingTop: "1rem",
              }}
            >
              <Button variant="outline" size="sm" onClick={() => handleTabChange("tasks")}>
                Go to Tasks tab &rarr;
              </Button>
              <Button variant="outline" size="sm" onClick={() => handleTabChange("report")}>
                Go to Report tab &rarr;
              </Button>
            </div>
          </Card>
        </div>
      )}

      {/* Tab: Tasks */}
      {tab === "tasks" && (
        <Card>
          <div style={{ display: "grid", gap: "1rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "0.75rem" }}>
              <h2 style={{ margin: 0 }}>Tasks</h2>
              <span style={{ color: "var(--text-muted)", fontSize: "0.9rem" }}>
                {active} active · {done} done in range
              </span>
            </div>

            <DateRangePicker
              rangeKey={rangeParam}
              from={fromParam}
              to={toParam}
              onChange={handleRangeChange}
            />

            {tasks.length === 0 ? (
              <div className="empty-state">
                <p>No task assignments open in the selected date range.</p>
              </div>
            ) : (
              <div style={{ display: "grid", gap: ".6rem" }}>
                {tasks.map((t) => (
                  <Link
                    key={t.id}
                    to={`/tasks/${t.id}`}
                    style={{ color: "inherit", textDecoration: "none" }}
                  >
                    <div
                      className="user-task-row"
                      style={{
                        padding: ".85rem",
                        border: "1px solid var(--border-light)",
                        borderRadius: 12,
                        display: "grid",
                        gridTemplateColumns: "2fr 1fr 1fr 1.5fr",
                        gap: ".75rem",
                        alignItems: "center",
                      }}
                    >
                      <strong>{t.title}</strong>
                      <Badge variant={t.status === "DONE" ? "success" : "primary"}>
                        {t.status.replace("_", " ")}
                      </Badge>
                      <div>
                        <Badge variant={t.priority === "HIGH" ? "danger" : "default"}>
                          {t.priority}
                        </Badge>
                        <div
                          style={{
                            fontSize: ".8rem",
                            color: t.overdue ? "var(--danger)" : "var(--text-muted)",
                            marginTop: ".3rem",
                          }}
                        >
                          <CalendarDays size={13} /> {t.deadline}
                        </div>
                      </div>
                      <div>
                        <small>{t.userRole}</small>
                        <div
                          style={{
                            display: "flex",
                            gap: ".5rem",
                            alignItems: "center",
                          }}
                        >
                          <div style={{ flex: 1 }}>
                            <ProgressBar value={t.progress} />
                          </div>
                          {t.progress}%
                        </div>
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>
        </Card>
      )}

      {/* Tab: Report */}
      {tab === "report" && (
        <UserReportView
          userId={user.id}
          initialFrom={fromParam}
          initialTo={toParam}
          initialRangeKey={rangeParam}
          onRangeChange={handleRangeChange}
        />
      )}

      {edit && (
        <Modal isOpen title="Edit user" onClose={() => setEdit(false)}>
          <form
            noValidate
            onSubmit={(e) => {
              e.preventDefault();
              run(async () => {
                await editUserApi(user.id, form);
                setEdit(false);
              });
            }}
            style={{ display: "grid", gap: ".8rem" }}
          >
            {["firstName", "lastName", "email", "phone"].map((k) => (
              <label key={k} className="field-label">
                {k.replace(/([A-Z])/g, " $1")}
                <input
                  className="form-input"
                  value={form[k] || ""}
                  onChange={(e) => setForm({ ...form, [k]: e.target.value })}
                />
              </label>
            ))}
            <label className="field-label">
              Role
              <Select
                value={form.role || ""}
                onValueChange={(role) => setForm({ ...form, role })}
                options={[
                  { value: "EMPLOYEE", label: "Employee" },
                  { value: "MANAGER", label: "Manager" },
                ]}
                ariaLabel="Role"
              />
            </label>
            <Button type="submit">Save changes</Button>
          </form>
        </Modal>
      )}

      {deleteConfirm && (
        <ConfirmDialog
          open
          title="Delete user"
          message="Soft-delete this user?"
          confirmLabel="Delete"
          onClose={() => setDeleteConfirm(false)}
          onConfirm={() => {
            setDeleteConfirm(false);
            run(async () => {
              await deleteUserApi(user.id);
              navigate("/admin/users");
            });
          }}
        />
      )}
    </div>
  );
};
