import React, { createContext, useEffect, useRef, useState } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom";
import {
  getNotificationsApi,
  getNotificationUnreadCountApi,
  markAllNotificationsReadApi,
  markNotificationReadApi,
  clearAllNotificationsApi,
} from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import { UserAvatar } from "../avatar/UserAvatar";
import { Button } from "../common/Button";
import { ConfirmDialog } from "../common/ConfirmDialog";
import {
  AlertCircle,
  Bell,
  BellOff,
  ClipboardList,
  LogOut,
  X,
} from "lucide-react";
import { PageLoader, RequestError } from "../common/LoadingStates";

export const TaskNavbarChatContext = createContext<React.Dispatch<
  React.SetStateAction<React.ReactNode | null>
> | null>(null);

export const AppLayout: React.FC = () => {
  const { user, isAuthenticated, isManager, pendingCount, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [chatSlot, setChatSlot] = useState<React.ReactNode | null>(null);
  const [notifications, setNotifications] = useState<
    import("../../types").NotificationItem[]
  >([]);
  const [notificationCount, setNotificationCount] = useState(0);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notificationError, setNotificationError] = useState("");
  const [clearingNotifications, setClearingNotifications] = useState(false);
  const [signOutConfirmOpen, setSignOutConfirmOpen] = useState(false);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const notificationWrapperRef = useRef<HTMLDivElement>(null);
  const loadNotifications = async () => {
    if (!isAuthenticated) return;
    setNotificationsLoading(true);
    try {
      const [items, count] = await Promise.all([
        getNotificationsApi(),
        getNotificationUnreadCountApi(),
      ]);
      setNotifications(items);
      setNotificationCount(count);
      setNotificationError("");
    } catch { setNotificationError(notifications.length ? "Notifications could not be refreshed." : "Notifications could not be loaded."); }
    finally { setNotificationsLoading(false); }
  };
  useEffect(() => {
    void loadNotifications();
    const timer = window.setInterval(loadNotifications, 30000);
    return () => window.clearInterval(timer);
  }, [isAuthenticated]);
  useEffect(() => {
    if (!notificationsOpen) return;
    const closeOnOutsidePointer = (event: PointerEvent) => {
      if (!notificationWrapperRef.current?.contains(event.target as Node)) {
        setNotificationsOpen(false);
        setNotificationError("");
      }
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setNotificationsOpen(false);
        setNotificationError("");
      }
    };
    document.addEventListener("pointerdown", closeOnOutsidePointer);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutsidePointer);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [notificationsOpen]);
  useEffect(() => {
    setNotificationsOpen(false);
    setNotificationError("");
  }, [location.pathname, location.search]);

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const handleClearAll = async () => {
    setClearingNotifications(true);
    setNotificationError("");
    try {
      await clearAllNotificationsApi();
      setNotifications([]);
      setNotificationCount(0);
    } catch (error) {
      console.error("Clear all notifications failed", {
        status: (error as { status?: number })?.status,
        code: (error as { code?: string })?.code,
        error,
      });
      setNotificationError(
        (error as { message?: string })?.message ||
          "Notifications could not be cleared. Please try again.",
      );
    } finally {
      setClearingNotifications(false);
    }
  };

  return (
    <TaskNavbarChatContext.Provider value={setChatSlot}>
      <div className="app-container">
        <header className="app-header">
          <div className="header-content">
            <Link to="/" className="brand-link">
              <div
                style={{
                  width: "38px",
                  height: "38px",
                  borderRadius: "10px",
                  background: "var(--primary-gradient)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "#fff",
                  boxShadow: "var(--shadow-sm)",
                }}
              >
                <ClipboardList size={22} />
              </div>
              <span>Task Manager</span>
            </Link>

            <nav className="nav-links">
              {isAuthenticated && (
                <NavLink
                  to="/tasks"
                  className={({ isActive }) =>
                    `nav-link ${isActive ? "active" : ""}`
                  }
                >
                  Home
                </NavLink>
              )}

              {isManager && (
                <>
                  <NavLink
                    to="/admin/pending"
                    className={({ isActive }) =>
                      `nav-link ${isActive ? "active" : ""}`
                    }
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: "0.4rem",
                    }}
                  >
                    Pending Requests
                    {pendingCount > 0 && (
                      <span
                        style={{
                          backgroundColor: "var(--danger)",
                          color: "#fff",
                          borderRadius: "var(--radius-badge)",
                          padding: "0.1rem 0.5rem",
                          fontSize: "0.7rem",
                          fontWeight: 700,
                        }}
                      >
                        {pendingCount}
                      </span>
                    )}
                  </NavLink>

                  <NavLink
                    to="/admin/users"
                    className={({ isActive }) =>
                      `nav-link ${isActive ? "active" : ""}`
                    }
                  >
                    Users
                  </NavLink>
                </>
              )}

              {isAuthenticated && (
                <NavLink
                  to="/profile"
                  className={({ isActive }) =>
                    `nav-link ${isActive ? "active" : ""}`
                  }
                >
                  Profile
                </NavLink>
              )}
            </nav>
            <div
              className="header-actions"
              style={{ display: "flex", alignItems: "center", gap: "0.85rem" }}
            >
              {isAuthenticated && user ? (
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.85rem",
                  }}
                >
                  <Link
                    to="/profile"
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.65rem",
                      textDecoration: "none",
                      color: "inherit",
                    }}
                  >
                    <UserAvatar
                      userId={user.id}
                      avatarPath={user.avatarPath}
                      firstName={user.firstName}
                      lastName={user.lastName}
                      size={36}
                    />
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        fontSize: "0.85rem",
                        lineHeight: 1.2,
                      }}
                    >
                      <span style={{ fontWeight: 600 }}>{user.firstName}</span>
                      <span
                        style={{
                          fontSize: "0.7rem",
                          color: "var(--text-muted)",
                        }}
                      >
                        {user.role}
                      </span>
                    </div>
                  </Link>

                  {chatSlot}
                  <div
                    ref={notificationWrapperRef}
                    style={{ position: "relative" }}
                  >
                    <Button
                      variant="outline"
                      size="sm"
                      icon={<Bell size={15} />}
                      onClick={() => {
                        setNotificationError("");
                        setNotificationsOpen((v) => !v);
                      }}
                      aria-label="Notifications"
                      className={`bell-shake-btn${notificationCount > 0 && !notificationsOpen ? " has-unread" : ""}`}
                    >
                      {notificationCount > 0 && (
                        <span className="notification-count bell-shake-btn">
                          {notificationCount > 99 ? "99+" : notificationCount}
                        </span>
                      )}
                    </Button>
                    {notificationsOpen && (
                      <div
                        className="notification-popover"
                        role="dialog"
                        aria-label="Notifications"
                      >
                        <div className="notification-popover-header">
                          <strong>Notifications</strong>
                          {notifications.length > 0 && (
                            <div className="notification-actions">
                              <button
                                type="button"
                                disabled={notificationCount === 0}
                                onClick={async () => {
                                  try {
                                    await markAllNotificationsReadApi();
                                    setNotifications((items) =>
                                      items.map((item) => ({
                                        ...item,
                                        read: true,
                                      })),
                                    );
                                    setNotificationCount(0);
                                    setNotificationError("");
                                  } catch (error) {
                                    console.error(
                                      "Mark all notifications as read failed",
                                      {
                                        status: (error as { status?: number })
                                          ?.status,
                                        code: (error as { code?: string })
                                          ?.code,
                                        error,
                                      },
                                    );
                                    setNotificationError(
                                      (error as { message?: string })
                                        ?.message ||
                                        "Notifications could not be updated. Please try again.",
                                    );
                                  }
                                }}
                              >
                                Read all
                              </button>
                              <button
                                type="button"
                                className="notification-clear-action"
                                disabled={clearingNotifications}
                                onClick={handleClearAll}
                              >
                                {clearingNotifications
                                  ? "Clearing..."
                                  : "Clear all"}
                              </button>
                            </div>
                          )}
                        </div>
                        {notificationError && notifications.length > 0 && (
                          <div
                            className="notification-inline-error"
                            role="alert"
                          >
                            <AlertCircle size={16} aria-hidden="true" />
                            <span>{notificationError}</span>
                            <button type="button" className="request-error-retry" onClick={() => void loadNotifications()}>Try again</button>
                            <button
                              type="button"
                              aria-label="Dismiss notification error"
                              onClick={() => setNotificationError("")}
                            >
                              <X size={14} />
                            </button>
                          </div>
                        )}
                        {notificationsLoading && notifications.length === 0 ? <PageLoader label="Loading notifications" /> : notificationError && notifications.length === 0 ? <RequestError message={notificationError} onRetry={() => void loadNotifications()} /> : notifications.length === 0 ? (
                          <div className="notification-empty-state">
                            <BellOff size={28} aria-hidden="true" />
                            <span>No notifications</span>
                          </div>
                        ) : (
                          notifications.map((n) => (
                            <Link
                              key={n.id}
                              to={n.taskId ? `/tasks/${n.taskId}` : "/"}
                              className={`notification-item ${n.read ? "" : "unread"}`}
                              onClick={async () => {
                                if (!n.read) {
                                  try {
                                    await markNotificationReadApi(n.id);
                                    await loadNotifications();
                                  } catch {}
                                }
                                setNotificationsOpen(false);
                              }}
                            >
                              <span>{n.message}</span>
                              <small>
                                {new Date(n.createdAt).toLocaleString()}
                              </small>
                            </Link>
                          ))
                        )}
                      </div>
                    )}
                  </div>
                  <Button
                    onClick={() => setSignOutConfirmOpen(true)}
                    variant="outline"
                    size="sm"
                    className="logout-btn"
                    icon={<LogOut size={15} />}
                  >
                    <span className="desktop-only">Sign Out</span>
                  </Button>
                </div>
              ) : (
                <div style={{ display: "flex", gap: "0.65rem" }}>
                  <Link to="/login">
                    <Button variant="outline" size="sm">
                      Sign In
                    </Button>
                  </Link>
                  <Link to="/signup">
                    <Button variant="primary" size="sm">
                      Sign Up
                    </Button>
                  </Link>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="main-content">
          <Outlet />
        </main>

        <footer className="app-footer">Task Management System</footer>
      </div>
      <ConfirmDialog
        open={signOutConfirmOpen}
        title="Sign out"
        message="Are you sure you want to sign out?"
        confirmLabel="Sign out"
        onClose={() => setSignOutConfirmOpen(false)}
        onConfirm={async () => {
          setSignOutConfirmOpen(false);
          await handleLogout();
        }}
      />
    </TaskNavbarChatContext.Provider>
  );
};
