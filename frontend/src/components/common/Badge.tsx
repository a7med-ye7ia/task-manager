import React from 'react';

export interface BadgeProps {
  children: React.ReactNode;
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'primary';
  style?: React.CSSProperties;
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'default',
  style,
  className = '',
}) => {
  const variantStyles: Record<string, React.CSSProperties> = {
    default: { backgroundColor: '#f1f5f9', color: '#475569' },
    primary: { backgroundColor: 'var(--primary-light)', color: 'var(--primary)' },
    success: { backgroundColor: 'var(--success-bg)', color: 'var(--success)' },
    warning: { backgroundColor: 'var(--warning-bg)', color: 'var(--warning)' },
    danger: { backgroundColor: 'var(--danger-bg)', color: 'var(--danger)' },
  };

  return (
    <span
      className={`status-badge ${className}`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '0.2rem 0.65rem',
        borderRadius: 'var(--radius-badge)',
        fontSize: '0.75rem',
        fontWeight: 600,
        ...variantStyles[variant],
        ...style,
      }}
    >
      {children}
    </span>
  );
};
