import React, { useEffect, useRef, useState } from "react";
import { Pencil, Trash2 } from "lucide-react";
import { Button } from "../common/Button";
import { Select } from "../common/Select";
import { ConfirmDialog } from "../common/ConfirmDialog";
import { FieldError } from "../common/FieldError";
import { PersonProfileLink } from "../common/PersonProfileLink";
import { UserAvatar } from "../avatar/UserAvatar";
import { PageLoader, RequestError } from "../common/LoadingStates";
import { CommentResponse, Subtask, User, UserSummary } from "../../types";
import {
  ApiError,
  createTaskCommentApi,
  deleteTaskCommentApi,
  getTaskCommentsApi,
  updateTaskCommentApi,
} from "../../services/api";

// Forces any avatar to a circle. A <span> (not a <div>) is used on purpose so the
// ".comment-message > div" bubble rule (background, padding, 12px radius) never matches it.
const AvatarCircle: React.FC<{ size: number; children: React.ReactNode }> = ({
  size,
  children,
}) => (
  <span
    style={{
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      flexShrink: 0,
      width: size,
      height: size,
      borderRadius: "50%",
      overflow: "hidden",
      lineHeight: 0,
    }}
  >
    {children}
  </span>
);

// Textarea whose text is vertically centered: one line always sits in the middle of a 44px box,
// and the box grows with the content (a textarea cannot be centered with align-items).
const CenteredTextarea: React.FC<React.TextareaHTMLAttributes<HTMLTextAreaElement>> = ({
  style,
  value,
  ...rest
}) => {
  const ref = useRef<HTMLTextAreaElement>(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${Math.max(44, el.scrollHeight)}px`;
  }, [value]);
  return (
    <textarea
      {...rest}
      ref={ref}
      value={value}
      rows={1}
      style={{
        boxSizing: "border-box",
        minHeight: 44,
        lineHeight: "22px",
        padding: "11px 1rem",
        resize: "none",
        overflow: "hidden",
        ...style,
      }}
    />
  );
};

export const CommentThread: React.FC<{
  taskId: number;
  subtasks: Subtask[];
  currentUserId?: number;
  canWrite: boolean;
  users: Array<User | UserSummary>;
}> = ({ taskId, subtasks, currentUserId, canWrite, users }) => {
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [body, setBody] = useState("");
  const [subtaskId, setSubtaskId] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [refreshNotice, setRefreshNotice] = useState("");
  const [editError, setEditError] = useState("");
  const [editBody, setEditBody] = useState("");
  const [editing, setEditing] = useState<number | null>(null);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [confirm, setConfirm] = useState<CommentResponse | null>(null);
  const renderMentionSuggestions = (text: string, setText: (value: string) => void) => {
    const match = text.match(/(?:^|\s)@([^\s@]*)$/);
    if (!match) return null;
    const query = match[1].toLowerCase();
    const options = users.filter(
      (u) =>
        u.id !== currentUserId &&
        `${u.firstName} ${u.lastName}`.toLowerCase().includes(query),
    );
    if (!options.length) return null;
    return (
      <div className="mention-suggestions">
        {options.map((u) => (
          <button
            type="button"
            key={u.id}
            onClick={(event) => {
              event.stopPropagation();
              setText(text.replace(/@[^\s@]*$/, `@${u.firstName} ${u.lastName} `));
            }}
          >
            <AvatarCircle size={24}>
              <UserAvatar
                userId={u.id}
                avatarPath={u.avatarPath}
                firstName={u.firstName}
                lastName={u.lastName}
                size={24}
              />
            </AvatarCircle>
            {u.firstName} {u.lastName}
          </button>
        ))}
      </div>
    );
  };
  const escapeRe = (t: string) => t.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  // Mentioned user ids are computed from the final text, so removed or old mentions are never sent.
  const idsFromBody = (text: string) =>
    users
      .filter((u) => text.includes(`@${u.firstName} ${u.lastName}`))
      .map((u) => u.id);
  const renderBody = (comment: CommentResponse, text: string) => {
    const tokens = (comment.mentionedUsers ?? []).map((u) => `@${u.firstName} ${u.lastName}`);
    const regex = tokens.length ? new RegExp(`(${tokens.sort((a, b) => b.length - a.length).map(escapeRe).join("|")})(?=$|[\\s.,!?;:])`, "g") : null;
    return regex
      ? text.split(regex).map((part, index) => {
          const mentioned = (comment.mentionedUsers ?? []).find(
            (u) => `@${u.firstName} ${u.lastName}` === part,
          );
          if (!mentioned) return <React.Fragment key={index}>{part}</React.Fragment>;
          const me = mentioned.id === currentUserId;
          return (
            <PersonProfileLink key={index} userId={mentioned.id} currentUserId={currentUserId} active={mentioned.active} className="comment-mention-link">
            <span
              key={index}
              className={me ? "comment-mention current" : "comment-mention"}
              style={{
                color: "var(--primary)",
                fontWeight: me ? 700 : 600,
                background: me ? "var(--primary-soft)" : "transparent",
                borderRadius: 6,
                padding: me ? "0 .25rem" : 0,
              }}
            >
              {part}
            </span>
            </PersonProfileLink>
          );
        })
      : text;
  };

  const endRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const close = () => setActiveId(null);
    const key = (event: KeyboardEvent) => event.key === "Escape" && close();
    document.addEventListener("click", close);
    document.addEventListener("keydown", key);
    return () => {
      document.removeEventListener("click", close);
      document.removeEventListener("keydown", key);
    };
  }, []);
  const load = async () => {
    setLoading(comments.length === 0);
    try {
      setComments(await getTaskCommentsApi(taskId));
      setError("");
      setRefreshNotice("");
    } catch (e) {
      const message = e instanceof ApiError ? e.getFormattedMessage() : "Comments could not be loaded.";
      if (comments.length) setRefreshNotice("Comments could not be refreshed."); else setError(message);
    } finally { setLoading(false); }
  };
  useEffect(() => {
    load();
    const timer = window.setInterval(load, 10000);
    return () => window.clearInterval(timer);
  }, [taskId]);
  useEffect(() => {
    const end = endRef.current;
    if (end && typeof end.scrollIntoView === "function") end.scrollIntoView({ block: "nearest" });
  }, [comments.length]);
  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!body.trim() || body.trim().length > 2000) {
      setError("Enter 1 to 2000 characters.");
      return;
    }
    try {
      await createTaskCommentApi(taskId, {
        body,
        subtaskId: subtaskId ? Number(subtaskId) : undefined, mentionedUserIds: idsFromBody(body),
      });
      setBody("");
      setSubtaskId("");
      await load();
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.getFormattedMessage()
          : "Comment could not be saved.",
      );
    }
  };
  const saveEdit = async (c: CommentResponse) => {
    if (!editBody.trim() || editBody.trim().length > 2000) {
      setEditError("Enter 1 to 2000 characters.");
      return;
    }
    if (editBody.trim() === c.body.trim()) {
      setEditError("No changes were made. Edit the message.");
      return;
    }
    try {
      await updateTaskCommentApi(taskId, c.id, editBody, idsFromBody(editBody));
      setEditing(null);
      setEditError("");
      await load();
    } catch (e) {
      setEditError(
        e instanceof ApiError
          ? e.getFormattedMessage()
          : "Comment could not be saved.",
      );
    }
  };
  const renderComment = (c: CommentResponse) => {
    const names = c.authorName.split(" ");
    const mine = c.authorId === currentUserId;
    const active = activeId === c.id;
    return (
      <div
        key={c.id}
        className={`comment-message ${mine ? "mine" : ""}`}
        onClick={(event) => {
          event.stopPropagation();
          if (mine) setActiveId(active ? null : c.id);
        }}
      >
        {!mine ? <PersonProfileLink userId={c.authorId} currentUserId={currentUserId} className="comment-author-link"><AvatarCircle size={50}>
          <UserAvatar
            userId={c.authorId ?? 0}
            avatarPath={c.authorAvatarPath}
            firstName={names[0]}
            lastName={names.slice(1).join(" ")}
            size={50}
          />
        </AvatarCircle></PersonProfileLink> : <span onClick={(event) => event.stopPropagation()} style={{ cursor: "default" }}><AvatarCircle size={50}>
          <UserAvatar userId={c.authorId ?? 0} avatarPath={c.authorAvatarPath} firstName={names[0]} lastName={names.slice(1).join(" ")} size={50} />
        </AvatarCircle></span>}
        <div>
          {!mine ? <PersonProfileLink userId={c.authorId} currentUserId={currentUserId} className="comment-author-link"><strong>{c.authorName}</strong></PersonProfileLink> : <strong onClick={(event) => event.stopPropagation()} style={{ cursor: "default" }}>{c.authorName}</strong>}
          <small>{new Date(c.createdAt).toLocaleString()}</small>
          {c.subtaskId && <span className="comment-subtask-chip">{subtasks.find(s => s.id === c.subtaskId)?.title ?? "Subtask"}</span>}
          {editing === c.id ? (
            <>
              {renderMentionSuggestions(editBody, setEditBody)}
              <CenteredTextarea
                className="form-input"
                style={{marginTop :"0.8rem"}}
                value={editBody}
                onClick={(e) => e.stopPropagation()}
                onChange={(e) => {
                  setEditBody(e.target.value);
                  setEditError("");
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    void saveEdit(c);
                  }
                }}
              />
              <FieldError message={editError} />
              <span className="comment-actions">
                <Button
                  size="sm"
                  onClick={async (event) => {
                    event.stopPropagation();
                    await saveEdit(c);
                  }}
                >
                  Save
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={(event) => {
                    event.stopPropagation();
                    setEditing(null);
                    setEditError("");
                  }}
                >
                  Cancel
                </Button>
              </span>
            </>
          ) : (
            <>
              <p>
                {renderBody(c, c.body)}
                {c.edited && !c.deleted ? " (edited)" : ""}
              </p>
              {!c.deleted && mine && active && (
                <span className="comment-actions">
                  <Button
                    size="sm"
                    variant="outline"
                    icon={<Pencil size={13} />}
                    title="Edit"
                    onClick={(event) => {
                      event.stopPropagation();
                      setEditing(c.id);
                      setEditError("");
                      setEditBody(c.body);
                    }}
                  >
                    Edit
                  </Button>
                  <Button
                    size="sm"
                    variant="danger"
                    icon={<Trash2 size={13} />}
                    title="Delete"
                    onClick={(event) => {
                      event.stopPropagation();
                      setConfirm(c);
                    }}
                  >
                    Delete
                  </Button>
                </span>
              )}
            </>
          )}
        </div>
      </div>
    );
  };
  return (
    <div className="comment-thread">
      <h2>Chat</h2>
      {loading ? <PageLoader label="Loading comments" /> : error && !comments.length ? <RequestError message={error} onRetry={() => void load()} /> : <>{refreshNotice && <div className="refresh-notice" role="status">{refreshNotice}<button type="button" onClick={() => setRefreshNotice("")}>Dismiss</button></div>}
      <div className="comment-list">
        {comments.length === 0 && (
          <p className="empty-state">No messages yet.</p>
        )}
        {comments.map(renderComment)}
        <div ref={endRef} />
      </div></>}
      {canWrite && (
        <form noValidate onSubmit={save} className="comment-composer">
          {subtasks.length > 0 && <Select
            value={subtaskId || "NONE"}
            onValueChange={(v) => setSubtaskId(v === "NONE" ? "" : v)}
            options={[
              { value: "NONE", label: "General task comment" },
              ...subtasks.map((s) => ({
                value: String(s.id),
                label: `Subtask: ${s.title}`,
              })),
            ]}
          />}
          {renderMentionSuggestions(body, setBody)}
          <textarea
            className="form-input"
            style={{alignContent : "center"}}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                void save(e as unknown as React.FormEvent);
              }
            }}
            placeholder="Write a message"
          />
          <div>
            <Button type="submit">Send</Button>
          </div>
        </form>
      )}
      {confirm && (
        <ConfirmDialog
          open
          title="Delete comment"
          message="Delete this comment?"
          confirmLabel="Delete"
          onClose={() => setConfirm(null)}
          onConfirm={async () => {
            try {
              await deleteTaskCommentApi(taskId, confirm.id);
              await load();
            } catch (e) {
              setError(
                e instanceof ApiError
                  ? e.getFormattedMessage()
                  : "Comment could not be deleted.",
              );
            } finally {
              setConfirm(null);
            }
          }}
        />
      )}
    </div>
  );
};
