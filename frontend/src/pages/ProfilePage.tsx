import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "../context/AuthContext";
import {
  updateProfileApi,
  changePasswordApi,
  uploadAvatarApi,
  removeAvatarApi,
  updateEmailPreferencesApi,
  getProfileApi,
  ApiError,
  getAccessToken,
} from "../services/api";
import { UserProfileResponse, ManagerEmailPrefs } from "../types";
import { AvatarCropModal } from "../components/avatar/AvatarCropModal";
import { UserAvatar } from "../components/avatar/UserAvatar";
import { Card } from "../components/common/Card";
import { Button } from "../components/common/Button";
import { Input } from "../components/common/Input";
import { PasswordInput } from "../components/common/PasswordInput";
import { PasswordRuleList } from "../components/common/PasswordRuleList";
import { Badge } from "../components/common/Badge";
import { ConfirmDialog } from "../components/common/ConfirmDialog";
import { FieldError } from "../components/common/FieldError";
import { PageLoader } from "../components/common/LoadingStates";
import "../styles/profile-avatar.css";
import {
  Pencil,
  Menu,
  ImageUp,
  Trash2,
  User,
  Mail,
  Phone,
  Lock,
  Bell,
  CheckCircle2,
  AlertCircle,
} from "lucide-react";

type Msg = { type: "success" | "error" | "warning"; text: string } | null;

// Single place that controls how form-level messages (success / error / warning) look.
const STATUS_STYLES = {
  success: { bg: "var(--success-bg)", border: "#a7f3d0", color: "#065f46" },
  error: { bg: "var(--danger-bg)", border: "#fecaca", color: "var(--danger)" },
  warning: { bg: "#fffbeb", border: "#fde68a", color: "#92400e" },
};

const StatusMessage: React.FC<{ message: Msg }> = ({ message }) => {
  if (!message) return null;
  const s = STATUS_STYLES[message.type];
  return (
    <div
      role={message.type === "error" ? "alert" : "status"}
      style={{
        padding: "0.75rem 1rem",
        borderRadius: "var(--radius-button)",
        marginBottom: "1.25rem",
        fontSize: "0.85rem",
        backgroundColor: s.bg,
        border: `1px solid ${s.border}`,
        color: s.color,
        display: "flex",
        alignItems: "center",
        gap: "0.5rem",
      }}
    >
      {message.type === "success" ? (
        <CheckCircle2 size={16} />
      ) : (
        <AlertCircle size={16} />
      )}
      <span>{message.text}</span>
    </div>
  );
};

// Warnings ("nothing changed") disappear on their own after a few seconds.
const useAutoDismissWarning = (message: Msg, clear: () => void) => {
  useEffect(() => {
    if (message?.type !== "warning") return;
    const timer = window.setTimeout(clear, 4000);
    return () => window.clearTimeout(timer);
  }, [message]);
};

// Maps server-side field errors ({ field, message }[]) to the given form fields.
const mapFieldErrors = (err: unknown, allowed: string[]) => {
  const mapped: Record<string, string> = {};
  const items = (err as any)?.fieldErrors as
    | { field: string; message: string }[]
    | undefined;
  (items || []).forEach((item) => {
    if (allowed.includes(item.field)) mapped[item.field] = item.message;
  });
  return mapped;
};

const samePrefs = (a: ManagerEmailPrefs, b: ManagerEmailPrefs) =>
  a.emailsEnabled === b.emailsEnabled &&
  a.onSignupRequest === b.onSignupRequest &&
  a.onTaskCompleted === b.onTaskCompleted &&
  a.onArchiveWarning === b.onArchiveWarning;

export const ProfilePage: React.FC = () => {
  const {
    user: authUser,
    refreshProfile,
    isAuthenticated,
    isLoading: authLoading,
  } = useAuth();

  const [profile, setProfile] = useState<UserProfileResponse | null>(authUser);
  const [loading, setLoading] = useState(!authUser);

  // Profile Form State
  const [profileForm, setProfileForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
  });
  const [profileMessage, setProfileMessage] = useState<Msg>(null);
  const [profileErrors, setProfileErrors] = useState<Record<string, string>>(
    {},
  );
  const [profileSubmitting, setProfileSubmitting] = useState(false);

  // Avatar State
  const [avatarToCropSrc, setAvatarToCropSrc] = useState<string | null>(null);
  const [avatarMessage, setAvatarMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const [avatarSubmitting, setAvatarSubmitting] = useState(false);
  const [avatarTimestamp, setAvatarTimestamp] = useState<number>(Date.now());
  const [removeAvatarConfirm, setRemoveAvatarConfirm] = useState(false);
  const [avatarMenuOpen, setAvatarMenuOpen] = useState(false);
  const avatarMenuRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const close = (event: MouseEvent | KeyboardEvent) => {
      if (event instanceof KeyboardEvent && event.key === "Escape")
        setAvatarMenuOpen(false);
      else if (
        event instanceof MouseEvent &&
        avatarMenuRef.current &&
        !avatarMenuRef.current.contains(event.target as Node)
      )
        setAvatarMenuOpen(false);
    };
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", close);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", close);
    };
  }, []);

  // Password Form State
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [passwordMessage, setPasswordMessage] = useState<Msg>(null);
  const [passwordErrors, setPasswordErrors] = useState<Record<string, string>>(
    {},
  );
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);

  // Email Preferences State (Managers only)
  const [emailPrefs, setEmailPrefs] = useState<ManagerEmailPrefs>({
    emailsEnabled: true,
    onSignupRequest: true,
    onTaskCompleted: false,
    onArchiveWarning: false,
  });
  const [savedPrefs, setSavedPrefs] = useState<ManagerEmailPrefs | null>(null);
  const [prefsMessage, setPrefsMessage] = useState<Msg>(null);
  const [prefsSubmitting, setPrefsSubmitting] = useState(false);

  useAutoDismissWarning(profileMessage, () => setProfileMessage(null));
  useAutoDismissWarning(prefsMessage, () => setPrefsMessage(null));

  useEffect(() => {
    if (authUser) {
      setProfile(authUser);
      setProfileForm({
        firstName: authUser.firstName || "",
        lastName: authUser.lastName || "",
        email: authUser.email || "",
        phone: authUser.phone || "",
      });
      if (authUser.emailPreferences) {
        setEmailPrefs(authUser.emailPreferences);
        setSavedPrefs(authUser.emailPreferences);
      }
      setLoading(false);
    }
  }, [authUser]);

  useEffect(() => {
    if (!authUser && isAuthenticated) {
      getProfileApi()
        .then((data) => {
          setProfile(data);
          setProfileForm({
            firstName: data.firstName || "",
            lastName: data.lastName || "",
            email: data.email || "",
            phone: data.phone || "",
          });
          if (data.emailPreferences) {
            setEmailPrefs(data.emailPreferences);
            setSavedPrefs(data.emailPreferences);
          }
        })
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [authUser, isAuthenticated, avatarTimestamp]);

  if (authLoading || loading) {
    return (
      <PageLoader label="Loading profile" />
    );
  }

  if (!profile) {
    return (
      <div
        style={{
          textAlign: "center",
          padding: "3rem",
          color: "var(--text-muted)",
        }}
      >
        User profile not found.
      </div>
    );
  }

  const clearProfileError = (key: string) =>
    setProfileErrors((current) => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });

  const clearPasswordError = (key: string) =>
    setPasswordErrors((current) => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileMessage(null);

    const errs: Record<string, string> = {};
    if (!profileForm.firstName.trim())
      errs.firstName = "First name is required.";
    if (!profileForm.lastName.trim()) errs.lastName = "Last name is required.";
    if (!profileForm.email.trim()) errs.email = "Email address is required.";
    else if (!/^\S+@\S+\.\S+$/.test(profileForm.email.trim()))
      errs.email = "Enter a valid email address.";
    if (!profileForm.phone.trim()) errs.phone = "Phone number is required.";
    setProfileErrors(errs);
    if (Object.keys(errs).length) return;

    const unchanged =
      profileForm.firstName.trim() === (profile.firstName || "").trim() &&
      profileForm.lastName.trim() === (profile.lastName || "").trim() &&
      profileForm.email.trim().toLowerCase() ===
        (profile.email || "").trim().toLowerCase() &&
      profileForm.phone.trim() === (profile.phone || "").trim();
    if (unchanged) {
      setProfileMessage({
        type: "warning",
        text: "No changes were made. Your profile information is already up to date.",
      });
      return;
    }

    setProfileSubmitting(true);

    try {
      const res = await updateProfileApi(profileForm);
      setProfileMessage({ type: "success", text: res.message });
      await refreshProfile();
    } catch (err) {
      const mapped = mapFieldErrors(err, [
        "firstName",
        "lastName",
        "email",
        "phone",
      ]);
      if (Object.keys(mapped).length) {
        setProfileErrors(mapped);
      } else if (err instanceof ApiError) {
        setProfileMessage({ type: "error", text: err.getFormattedMessage() });
      } else if (err instanceof Error) {
        setProfileMessage({
          type: "error",
          text: "Profile could not be updated. Please try again.",
        });
      } else {
        setProfileMessage({ type: "error", text: "Failed to update profile." });
      }
    } finally {
      setProfileSubmitting(false);
    }
  };

  const handleAvatarFileSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    setAvatarMessage(null);
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 2 * 1024 * 1024) {
      setAvatarMessage({
        type: "error",
        text: "The image must be 2 MB or smaller",
      });
      e.target.value = "";
      return;
    }

    const ext = file.name.split(".").pop()?.toLowerCase();
    if (!["jpg", "jpeg", "png", "webp"].includes(ext || "")) {
      setAvatarMessage({
        type: "error",
        text: "Allowed formats are JPG, PNG, and WebP.",
      });
      e.target.value = "";
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      setAvatarToCropSrc(reader.result as string);
      e.target.value = "";
    };
    reader.readAsDataURL(file);
  };
  const handleEditAvatar = async () => {
    setAvatarMenuOpen(false);
    if (!profile.avatarPath) return;
    try {
      const response = await fetch("/api/profile/avatar", {
        headers: { Authorization: `Bearer ${getAccessToken() ?? ""}` },
      });
      const blob = await response.blob();
      const reader = new FileReader();
      reader.onload = () => setAvatarToCropSrc(reader.result as string);
      reader.readAsDataURL(blob);
    } catch {
      setAvatarMessage({
        type: "error",
        text: "The current photo could not be opened.",
      });
    }
  };

  const handleCropComplete = async (croppedBlob: Blob) => {
    setAvatarToCropSrc(null);
    setAvatarMessage(null);
    setAvatarSubmitting(true);

    try {
      const croppedFile = new File([croppedBlob], "avatar.jpg", {
        type: "image/jpeg",
      });
      const res = await uploadAvatarApi(croppedFile);
      setAvatarMessage({ type: "success", text: res.message });
      setAvatarTimestamp(Date.now());
      await refreshProfile();
    } catch (err) {
      if (err instanceof ApiError) {
        setAvatarMessage({ type: "error", text: err.getFormattedMessage() });
      } else if (err instanceof Error) {
        setAvatarMessage({
          type: "error",
          text: "Avatar could not be uploaded. Please try again.",
        });
      } else {
        setAvatarMessage({
          type: "error",
          text: "Failed to upload avatar image.",
        });
      }
    } finally {
      setAvatarSubmitting(false);
    }
  };

  const handleRemoveAvatar = async () => {
    setRemoveAvatarConfirm(false);
    setAvatarSubmitting(true);
    setAvatarMessage(null);
    try {
      const updated = await removeAvatarApi();
      setProfile((current) =>
        current ? { ...current, avatarPath: updated.avatarPath } : current,
      );
      setAvatarTimestamp(Date.now());
      await refreshProfile();
      setAvatarMessage({ type: "success", text: "Photo removed." });
    } catch {
      setAvatarMessage({
        type: "error",
        text: "The photo could not be removed. Please try again.",
      });
    } finally {
      setAvatarSubmitting(false);
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordMessage(null);

    const errs: Record<string, string> = {};
    if (!passwordForm.currentPassword)
      errs.currentPassword = "Current password is required.";
    if (!passwordForm.newPassword)
      errs.newPassword = "New password is required.";
    if (!passwordForm.confirmPassword)
      errs.confirmPassword = "Please confirm the new password.";
    else if (passwordForm.newPassword !== passwordForm.confirmPassword)
      errs.confirmPassword = "New passwords do not match.";
    setPasswordErrors(errs);
    if (Object.keys(errs).length) return;

    setPasswordSubmitting(true);

    try {
      const res = await changePasswordApi(
        passwordForm.currentPassword,
        passwordForm.newPassword,
        passwordForm.confirmPassword,
      );
      setPasswordMessage({ type: "success", text: res.message });
      setPasswordForm({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      });
    } catch (err) {
      const mapped = mapFieldErrors(err, [
        "currentPassword",
        "newPassword",
        "confirmPassword",
      ]);
      if (Object.keys(mapped).length) {
        setPasswordErrors(mapped);
      } else if (err instanceof ApiError) {
        setPasswordMessage({ type: "error", text: err.getFormattedMessage() });
      } else if (err instanceof Error) {
        setPasswordMessage({
          type: "error",
          text: "Password could not be changed. Please try again.",
        });
      } else {
        setPasswordMessage({
          type: "error",
          text: "Failed to change password.",
        });
      }
    } finally {
      setPasswordSubmitting(false);
    }
  };

  const handlePrefsSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPrefsMessage(null);

    if (savedPrefs && samePrefs(savedPrefs, emailPrefs)) {
      setPrefsMessage({
        type: "warning",
        text: "No changes were made. Your email preferences are already up to date.",
      });
      return;
    }

    setPrefsSubmitting(true);

    try {
      const res = await updateEmailPreferencesApi(emailPrefs);
      setPrefsMessage({ type: "success", text: res.message });
      setSavedPrefs(emailPrefs);
    } catch (err) {
      if (err instanceof ApiError) {
        setPrefsMessage({ type: "error", text: err.getFormattedMessage() });
      } else if (err instanceof Error) {
        setPrefsMessage({ type: "error", text: err.message });
      } else {
        setPrefsMessage({
          type: "error",
          text: "Failed to update email preferences.",
        });
      }
    } finally {
      setPrefsSubmitting(false);
    }
  };

  return (
    <div
      style={{
        maxWidth: "840px",
        margin: "0 auto",
        display: "flex",
        flexDirection: "column",
        gap: "2rem",
      }}
    >
      <div className="page-header">
        <h1 className="page-title">User Profile</h1>
        <p className="page-subtitle">
          Manage personal details, avatar, password, and email preferences
        </p>
      </div>

      {/* Profile & Avatar Card */}
      <Card>
        <div
          className="profile-main-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "180px 1fr",
            gap: "2.5rem",
          }}
        >
          {/* Avatar Area */}
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              textAlign: "center",
            }}
          >
            <div
              style={{
                position: "relative",
                width: "128px",
                height: "128px",
                marginBottom: "0.75rem",
              }}
            >
              <UserAvatar
                userId={profile.id}
                avatarPath={profile.avatarPath}
                firstName={profile.firstName}
                lastName={profile.lastName}
                size={128}
                cacheBust={avatarTimestamp}
              />

              <div
                ref={avatarMenuRef}
                style={{
                  position: "absolute",
                  bottom: "2px",
                  right: "2px",
                  color: "#ffffff",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  cursor: avatarSubmitting ? "not-allowed" : "pointer",
                  transition: "transform 0.15s ease",
                }}
              >
                <button
                  type="button"
                  title="Avatar menu"
                  aria-label="Avatar menu"
                  onClick={() => setAvatarMenuOpen((v) => !v)}
                  className="responsive-form-grid"
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: "50%",
                    background: "var(--primary-gradient)",
                    color: "#fff",
                    border: "2px solid #fff",
                    cursor: avatarSubmitting ? "not-allowed" : "pointer",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <Menu size={17} />
                </button>
                {avatarMenuOpen && (
                  <div className="avatar-menu" role="menu">
                    <label role="menuitem" className="avatar-menu-item">
                      <ImageUp size={15} />
                      {profile.avatarPath ? "Change photo" : "Upload photo"} 
                      <input
                        type="file"
                        accept=".jpg,.jpeg,.png,.webp"
                        onChange={(e) => {
                          setAvatarMenuOpen(false);
                          handleAvatarFileSelected(e);
                        }}
                        disabled={avatarSubmitting}
                      />
                    </label>
                    {profile.avatarPath && (
                      <>
                        <button
                          type="button"
                          className="avatar-menu-item"
                          onClick={handleEditAvatar}
                        >
                          <Pencil size={15} />
                          Edit
                        </button>
                        <button
                          type="button"
                          className="avatar-menu-item danger"
                          onClick={() => {
                            setAvatarMenuOpen(false);
                            setRemoveAvatarConfirm(true);
                          }}
                        >
                          <Trash2 size={15} />
                          Delete photo
                        </button>
                      </>
                    )}
                  </div>
                )}
              </div>
            </div>

            <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>
              JPG, PNG, WebP up to 2 MB
            </span>

            {avatarSubmitting && (
              <span
                style={{
                  fontSize: "0.75rem",
                  color: "var(--primary)",
                  marginTop: "0.25rem",
                  fontWeight: 600,
                }}
              >
                Saving avatar...
              </span>
            )}

            {avatarMessage && (
              <div
                style={{
                  marginTop: "0.75rem",
                  fontSize: "0.8rem",
                  display: "flex",
                  alignItems: "center",
                  gap: "0.35rem",
                  color:
                    avatarMessage.type === "success"
                      ? "var(--success)"
                      : "var(--danger)",
                }}
              >
                {avatarMessage.type === "success" ? (
                  <CheckCircle2 size={14} />
                ) : (
                  <AlertCircle size={14} />
                )}
                <span>{avatarMessage.text}</span>
              </div>
            )}
          </div>

          {/* Profile Details Form */}
          <div>
            <h3
              style={{
                fontSize: "1.25rem",
                fontWeight: 700,
                marginBottom: "0.35rem",
                color: "var(--text-main)",
              }}
            >
              Personal Information
            </h3>
            <p
              style={{
                color: "var(--text-muted)",
                fontSize: "0.85rem",
                marginBottom: "1.5rem",
              }}
            >
              Update your basic account details and contact information.
            </p>

            <StatusMessage message={profileMessage} />

            <form
              noValidate
              onSubmit={handleProfileSubmit}
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "1.2rem",
              }}
            >
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "1rem",
                }}
              >
                <Input
                  label="First Name"
                  required
                  value={profileForm.firstName}
                  onChange={(e) => {
                    setProfileForm({
                      ...profileForm,
                      firstName: e.target.value,
                    });
                    clearProfileError("firstName");
                  }}
                  icon={<User size={18} />}
                  error={profileErrors.firstName}
                />
                <Input
                  label="Last Name"
                  required
                  value={profileForm.lastName}
                  onChange={(e) => {
                    setProfileForm({
                      ...profileForm,
                      lastName: e.target.value,
                    });
                    clearProfileError("lastName");
                  }}
                  icon={<User size={18} />}
                  error={profileErrors.lastName}
                />
              </div>

              <Input
                label="Email Address"
                type="email"
                required
                value={profileForm.email}
                onChange={(e) => {
                  setProfileForm({ ...profileForm, email: e.target.value });
                  clearProfileError("email");
                }}
                icon={<Mail size={18} />}
                helperText="Changing email will send a confirmation link to the new address."
                error={profileErrors.email}
              />

              <Input
                label="Phone Number"
                type="tel"
                required
                value={profileForm.phone}
                onChange={(e) => {
                  setProfileForm({ ...profileForm, phone: e.target.value });
                  clearProfileError("phone");
                }}
                icon={<Phone size={18} />}
                error={profileErrors.phone}
              />

              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginTop: "0.5rem",
                }}
              >
                <Button
                  type="submit"
                  variant="primary"
                  isLoading={profileSubmitting}
                >
                  Save Changes
                </Button>

                <div
                  style={{
                    display: "flex",
                    gap: "0.5rem",
                    alignItems: "center",
                  }}
                >
                  <Badge variant="primary">{profile.role}</Badge>
                  <Badge variant="success">{profile.status}</Badge>
                </div>
              </div>
            </form>
          </div>
        </div>
      </Card>

      {/* Change Password Card */}
      <Card>
        <h3
          style={{
            fontSize: "1.25rem",
            fontWeight: 700,
            marginBottom: "0.35rem",
            color: "var(--text-main)",
          }}
        >
          Change Password
        </h3>
        <p
          style={{
            color: "var(--text-muted)",
            fontSize: "0.85rem",
            marginBottom: "1.5rem",
          }}
        >
          Update your account password. All other active sessions on other
          devices will be revoked.
        </p>

        <StatusMessage message={passwordMessage} />

        <form
          noValidate
          onSubmit={handlePasswordSubmit}
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "1.25rem",
            maxWidth: "440px",
          }}
        >
          <div>
            <PasswordInput
              label="Current Password"
              required
              value={passwordForm.currentPassword}
              onChange={(e) => {
                setPasswordForm({
                  ...passwordForm,
                  currentPassword: e.target.value,
                });
                clearPasswordError("currentPassword");
              }}
              placeholder="••••••••"
            />
            <FieldError message={passwordErrors.currentPassword} />
          </div>

          <div>
            <PasswordInput
              label="New Password"
              required
              value={passwordForm.newPassword}
              onChange={(e) => {
                setPasswordForm({
                  ...passwordForm,
                  newPassword: e.target.value,
                });
                clearPasswordError("newPassword");
              }}
              placeholder="Min 8 chars"
            />
            <FieldError message={passwordErrors.newPassword} />
            <PasswordRuleList password={passwordForm.newPassword} />
          </div>

          <div>
            <PasswordInput
              label="Confirm New Password"
              required
              value={passwordForm.confirmPassword}
              onChange={(e) => {
                setPasswordForm({
                  ...passwordForm,
                  confirmPassword: e.target.value,
                });
                clearPasswordError("confirmPassword");
              }}
              placeholder="Re-enter new password"
            />
            <FieldError message={passwordErrors.confirmPassword} />
          </div>

          <Button
            type="submit"
            variant="primary"
            isLoading={passwordSubmitting}
            icon={<Lock size={17} />}
            style={{ alignSelf: "flex-start" }}
          >
            Update Password
          </Button>
        </form>
      </Card>

      {/* Manager Email Preferences */}
      {profile.role === "MANAGER" && (
        <Card>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.6rem",
              marginBottom: "0.35rem",
            }}
          >
            <Bell size={20} color="var(--primary)" />
            <h3
              style={{
                fontSize: "1.25rem",
                fontWeight: 700,
                margin: 0,
                color: "var(--text-main)",
              }}
            >
              Manager Email Notification Preferences
            </h3>
          </div>
          <p
            style={{
              color: "var(--text-muted)",
              fontSize: "0.85rem",
              marginBottom: "1.5rem",
            }}
          >
            Configure which event types trigger automated emails to your
            registered inbox.
          </p>

          <StatusMessage message={prefsMessage} />

          <form
            noValidate
            onSubmit={handlePrefsSubmit}
            style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.85rem",
                padding: "1rem",
                borderRadius: "var(--radius-button)",
                backgroundColor: "var(--bg-main)",
                border: "1px solid var(--border)",
              }}
            >
              <input
                type="checkbox"
                id="master-emails"
                checked={emailPrefs.emailsEnabled}
                onChange={(e) =>
                  setEmailPrefs({
                    ...emailPrefs,
                    emailsEnabled: e.target.checked,
                  })
                }
                style={{
                  width: "18px",
                  height: "18px",
                  accentColor: "var(--primary)",
                  cursor: "pointer",
                }}
              />
              <label
                htmlFor="master-emails"
                style={{
                  fontWeight: 600,
                  fontSize: "0.95rem",
                  cursor: "pointer",
                }}
              >
                Enable system email notifications (Master toggle)
              </label>
            </div>

            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "0.85rem",
                opacity: emailPrefs.emailsEnabled ? 1 : 0.5,
                paddingLeft: "0.5rem",
              }}
            >
              <label
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.75rem",
                  cursor: "pointer",
                  fontSize: "0.9rem",
                }}
              >
                <input
                  type="checkbox"
                  disabled={!emailPrefs.emailsEnabled}
                  checked={emailPrefs.onSignupRequest}
                  onChange={(e) =>
                    setEmailPrefs({
                      ...emailPrefs,
                      onSignupRequest: e.target.checked,
                    })
                  }
                  style={{
                    width: "16px",
                    height: "16px",
                    accentColor: "var(--primary)",
                  }}
                />
                Notify on pending account sign-up requests
              </label>

              <label
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.75rem",
                  cursor: "pointer",
                  fontSize: "0.9rem",
                }}
              >
                <input
                  type="checkbox"
                  disabled={!emailPrefs.emailsEnabled}
                  checked={emailPrefs.onTaskCompleted}
                  onChange={(e) =>
                    setEmailPrefs({
                      ...emailPrefs,
                      onTaskCompleted: e.target.checked,
                    })
                  }
                  style={{
                    width: "16px",
                    height: "16px",
                    accentColor: "var(--primary)",
                  }}
                />
                Notify when a task is completed
              </label>

              <label
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.75rem",
                  cursor: "pointer",
                  fontSize: "0.9rem",
                }}
              >
                <input
                  type="checkbox"
                  disabled={!emailPrefs.emailsEnabled}
                  checked={emailPrefs.onArchiveWarning}
                  onChange={(e) =>
                    setEmailPrefs({
                      ...emailPrefs,
                      onArchiveWarning: e.target.checked,
                    })
                  }
                  style={{
                    width: "16px",
                    height: "16px",
                    accentColor: "var(--primary)",
                  }}
                />
                Notify on task archive warning (3 days ahead)
              </label>
            </div>

            <Button
              type="submit"
              variant="primary"
              isLoading={prefsSubmitting}
              style={{ alignSelf: "flex-start", marginTop: "0.5rem" }}
            >
              Save Email Preferences
            </Button>
          </form>
        </Card>
      )}

      {avatarToCropSrc && (
        <AvatarCropModal
          imageSrc={avatarToCropSrc}
          onCropComplete={handleCropComplete}
          onCancel={() => setAvatarToCropSrc(null)}
        />
      )}
      {removeAvatarConfirm && (
        <ConfirmDialog
          open
          title="Remove photo"
          message="Remove your profile photo?"
          confirmLabel="Remove photo"
          onClose={() => setRemoveAvatarConfirm(false)}
          onConfirm={handleRemoveAvatar}
        />
      )}
    </div>
  );
};
  
