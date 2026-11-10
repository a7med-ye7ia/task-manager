import React from "react";
import { ClipboardList } from "lucide-react";
import { ErrorNotice } from './ErrorNotice';

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
  </div>
);

export const Skeleton: React.FC<{ className?: string; style?: React.CSSProperties }> = ({ className = "", style }) => (
  <span className={`ui-skeleton ${className}`} style={style} aria-hidden="true" />
);

export const TaskPoolSkeleton: React.FC = () => (
  <div className="task-list" role="status" aria-label="Loading tasks">
    {Array.from({ length: 4 }, (_, index) => (
      <div className="task-skeleton-row" key={index}>
        <div><Skeleton className="skeleton-title" /><Skeleton className="skeleton-meta" /></div>
        <Skeleton className="skeleton-badge" /><Skeleton className="skeleton-date" />
        <Skeleton className="skeleton-progress" />
      </div>
    ))}
  </div>
);

export const TaskDetailSkeleton: React.FC = () => (
  <div className="task-detail-skeleton" role="status" aria-label="Loading task">
    <div className="card" style={{ padding: '1.5rem', marginBottom: '1.25rem' }}>
      <Skeleton className="skeleton-title" style={{ width: '45%', height: '28px', marginBottom: '0.75rem' }} />
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
        <Skeleton className="skeleton-badge" style={{ width: '85px', height: '24px' }} />
        <Skeleton className="skeleton-badge" style={{ width: '85px', height: '24px' }} />
      </div>
      <Skeleton className="skeleton-meta" style={{ width: '60%', height: '14px' }} />
    </div>
    <div style={{ display: 'grid', gap: '1.25rem' }}>
      <div className="card" style={{ padding: '1.25rem' }}>
        <Skeleton style={{ width: '120px', height: '20px', marginBottom: '1rem' }} />
        <Skeleton style={{ width: '100%', height: '40px', marginBottom: '0.5rem' }} />
        <Skeleton style={{ width: '100%', height: '40px' }} />
      </div>
      <div className="card" style={{ padding: '1.25rem' }}>
        <Skeleton style={{ width: '100px', height: '20px', marginBottom: '1rem' }} />
        <Skeleton style={{ width: '100%', height: '36px', marginBottom: '0.5rem' }} />
        <Skeleton style={{ width: '100%', height: '36px' }} />
      </div>
      <div className="card" style={{ padding: '1.25rem' }}>
        <Skeleton style={{ width: '80px', height: '20px', marginBottom: '1rem' }} />
        <Skeleton style={{ width: '100%', height: '60px' }} />
      </div>
    </div>
  </div>
);

export const RequestError: React.FC<{ message: string; onRetry: () => void; compact?: boolean }> = ({ message, onRetry, compact = false }) => (
  <ErrorNotice error={message} onRetry={onRetry} compact={compact} />
);
