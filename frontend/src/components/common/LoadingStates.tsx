import React from "react";
import { ClipboardList, RefreshCw, AlertCircle } from "lucide-react";

export const Spinner: React.FC<{ size?: number }> = ({ size = 16 }) => (
  <span className="ui-spinner" style={{ width: size, height: size }} aria-hidden="true" />
);

export const AppLoader: React.FC = () => (
  <div className="app-loader" role="status" aria-label="Loading">
    <div className="app-loader-mark"><ClipboardList size={28} /></div>
    <strong>Task Management System</strong>
    <div className="app-loader-line" />
  </div>
);

export const PageLoader: React.FC<{ label?: string }> = ({ label = "Loading" }) => (
  <div className="page-loader" role="status" aria-label={label}>
    <Spinner size={22} />
    <span>{label}</span>
  </div>
);

export const Skeleton: React.FC<{ className?: string }> = ({ className = "" }) => (
  <span className={`ui-skeleton ${className}`} aria-hidden="true" />
);

export const TaskPoolSkeleton: React.FC = () => (
  <div className="task-list" aria-label="Loading tasks">
    {Array.from({ length: 4 }, (_, index) => (
      <div className="task-skeleton-row" key={index}>
        <div><Skeleton className="skeleton-title" /><Skeleton className="skeleton-meta" /></div>
        <Skeleton className="skeleton-badge" /><Skeleton className="skeleton-date" />
        <Skeleton className="skeleton-progress" />
      </div>
    ))}
  </div>
);

export const RequestError: React.FC<{ message: string; onRetry: () => void; compact?: boolean }> = ({ message, onRetry, compact = false }) => (
  <div className={compact ? "refresh-notice" : "request-error"} role="alert">
    <AlertCircle size={16} aria-hidden="true" />
    <span>{message}</span>
    <button type="button" onClick={onRetry} className="request-error-retry"><RefreshCw size={14} /> Try again</button>
  </div>
);
