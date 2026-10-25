import React, { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
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

export const UserDetailsPage: React.FC = () => {
  const { id } = useParams();
  const location = useLocation();
  const viewerMode = location.pathname.startsWith("/users/");
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();
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
        const [u, t] = await Promise.all([getPublicProfileApi(Number(id)), request<UserTaskSummary[]>(`/api/users/${id}/public/tasks?filter=${publicFilter}`)]);
        setPublicProfile(u); setTasks(t); return;
      }
      const [u, t] = await Promise.all([getAdminUserApi(Number(id)), getUserTasksApi(Number(id))]);
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
    if (viewerMode && currentUser?.id === Number(id)) { navigate("/profile", { replace: true }); return; }
    load();
  }, [id, viewerMode, publicFilter, currentUser?.id]);
  if (error && !user && !publicProfile) return <RequestError message={error} onRetry={() => void load()} />;
  if (!user && !publicProfile) return <PageLoader label="Loading profile" />;
  const refreshNoticeView = refreshNotice && <div className="refresh-notice" role="status">{refreshNotice}<button type="button" onClick={() => setRefreshNotice("")}>Dismiss</button></div>;
  if (viewerMode && publicProfile) return <div style={{ display: "grid", gap: "1.25rem" }}>{refreshNoticeView}<Link to="/tasks" style={{ color: "var(--primary)" }}>Back to tasks</Link><Card><div style={{ display: "flex", alignItems: "center", gap: "1rem" }}><UserAvatar userId={publicProfile.id} avatarPath={publicProfile.avatarPath} firstName={publicProfile.firstName} lastName={publicProfile.lastName} size={72} /><div><h1 className="page-title">{publicProfile.active ? `${publicProfile.firstName} ${publicProfile.lastName}` : "This user is no longer active"}</h1>{publicProfile.active && <><p>{publicProfile.phone}</p><Badge>{publicProfile.role === "MANAGER" ? "Manager" : "Employee"}</Badge></>}</div></div></Card>{publicProfile.active && <Card><div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}><h2>Works on</h2><Select value={publicFilter} onValueChange={setPublicFilter} options={[{value:"ALL",label:"All"},{value:"LEADS",label:"Leads"},{value:"COMMON",label:"Common"}]} /></div><div style={{ display: "grid", gap: ".5rem" }}>{tasks.map(t => <Link key={t.id} to={`/tasks/${t.id}`} className="task-row"><span>{t.title}</span><Badge>{t.userRole === "LEAD" ? "Lead" : "Member"}</Badge></Link>)}{tasks.length === 0 && <p className="empty-state">No tasks found.</p>}</div></Card>}</div>;
  if (!user) return <PageLoader label="Loading profile" />;
  const active = tasks.filter((t) => t.status !== "DONE").length;
  const done = tasks.filter((t) => t.status === "DONE").length;
  const run = async (action: () => Promise<unknown>) => {
    try {
      await action();
      await load();
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.getFormattedMessage()
          : "Could not save this change.",
      );
    }
  };
  return (
    <div style={{ display: "grid", gap: "1.25rem" }}>{refreshNoticeView}
      <Link to="/admin/users" style={{ color: "var(--primary)" }}>
        Back to users
      </Link>
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
              <h1 className="page-title">
                {user.firstName} {user.lastName}
              </h1>
              <p className="page-subtitle">
                {user.email} · {user.phone}
              </p>
              <div style={{ display: "flex", gap: ".5rem" }}>
                <Badge
                  variant={user.role === "MANAGER" ? "primary" : "default"}
                >
                  {user.role}
                </Badge>
                <Badge
                  variant={user.status === "ACTIVE" ? "success" : "danger"}
                >
                  {user.deletedAt ? "DELETED" : user.status}
                </Badge>
                <span style={{ color: "var(--text-muted)" }}>
                  Hired {user.hiredAt || "—"}
                </span>
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
            )}{" "}
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
      <Card>
        <div style={{ display: "flex", justifyContent: "space-between" }}>
          <h2>Tasks</h2>
          <span style={{ color: "var(--text-muted)" }}>
            {active} active · {done} done
          </span>
        </div>
        {tasks.length === 0 ? (
          <div className="empty-state">
            <p>No task assignments yet.</p>
          </div>
        ) : (
          <div style={{ display: "grid", gap: ".6rem", marginTop: "1rem" }}>
            {tasks.map((t) => (
              <Link
                key={t.id}
                to={`/tasks/${t.id}`}
                style={{ color: "inherit", textDecoration: "none" }}
              >
                <div className="user-task-row"
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
                    <Badge
                      variant={t.priority === "HIGH" ? "danger" : "default"}
                    >
                      {t.priority}
                    </Badge>
                    <div
                      style={{
                        fontSize: ".8rem",
                        color: t.overdue
                          ? "var(--danger)"
                          : "var(--text-muted)",
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
      </Card>
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
      {deleteConfirm && <ConfirmDialog open title="Delete user" message="Soft-delete this user?" confirmLabel="Delete" onClose={() => setDeleteConfirm(false)} onConfirm={() => { setDeleteConfirm(false); run(async () => { await deleteUserApi(user.id); navigate('/admin/users'); }); }} />}
    </div>
  );
};
