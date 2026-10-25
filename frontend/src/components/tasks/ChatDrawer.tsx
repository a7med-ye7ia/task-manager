import React, { useEffect } from "react";
import { X, MessageCircle, Check } from "lucide-react";
import { CommentThread } from "./CommentThread";
import { Subtask, User, UserSummary } from "../../types";

export const ChatDrawer: React.FC<{
  open: boolean;
  onClose: () => void;
  taskId: number;
  subtasks: Subtask[];
  currentUserId?: number;
  canWrite: boolean;
  users: Array<User | UserSummary>;
  unread?: boolean;
  onMarkRead?: () => void;
}> = ({
  open,
  onClose,
  taskId,
  subtasks,
  currentUserId,
  canWrite, users,
  unread,
  onMarkRead,
}) => {
  useEffect(() => {
    if (!open) return;
    onMarkRead?.();
    const onKey = (event: KeyboardEvent) => event.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);
  if (!open) return null;
  return (
    <aside className="task-drawer" aria-label="Task chat">
      <header>
        <span>
          <MessageCircle size={18} /> Chat
        </span>
        <div className="drawer-header-actions">
          {unread && (
            <button type="button" onClick={onMarkRead} title="Mark all as read">
              <Check size={17} />
            </button>
          )}
          <button type="button" aria-label="Close chat" onClick={onClose}>
            <X size={19} />
          </button>
        </div>
      </header>
      <div className="task-drawer-body">
      <CommentThread
          taskId={taskId}
          subtasks={subtasks}
          currentUserId={currentUserId}
          canWrite={canWrite}
        users={users}
        />
      </div>
    </aside>
  );
};
