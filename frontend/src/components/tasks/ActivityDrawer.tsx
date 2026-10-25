import React, { useEffect } from "react";
import { History, X, Check } from "lucide-react";
import { ActivityTab } from "./ActivityTab";
export const ActivityDrawer: React.FC<{ open: boolean; onClose: () => void; taskId: number; unread?: boolean; onMarkRead?: () => void }> = ({ open, onClose, taskId, unread, onMarkRead }) => {
  useEffect(() => { if (!open) return; onMarkRead?.(); const onKey = (event: KeyboardEvent) => event.key === "Escape" && onClose(); window.addEventListener("keydown", onKey); return () => window.removeEventListener("keydown", onKey); }, [open, onClose]);
  if (!open) return null;
  return <aside className="task-drawer" aria-label="Task activity"><header><span><History size={18} /> Activity</span><div className="drawer-header-actions">{unread && <button type="button" onClick={onMarkRead} title="Mark all as read"><Check size={17} /></button>}<button type="button" aria-label="Close activity" onClick={onClose}><X size={19} /></button></div></header><div className="task-drawer-body"><ActivityTab taskId={taskId} /></div></aside>;
};
