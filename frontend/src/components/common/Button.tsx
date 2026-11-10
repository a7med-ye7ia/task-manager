import React from 'react';
import { Spinner } from './LoadingStates';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'outline' | 'danger' | 'ghost' | 'icon';
  size?: 'sm' | 'md' | 'lg' | 'icon';
  icon?: React.ReactNode;
  isLoading?: boolean;
  motion?: 'preview' | 'download' | 'edit' | 'delete' | 'check' | 'cancel' | 'clip' | 'toggle';
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  icon,
  isLoading = false,
  motion,
  className = '',
  disabled,
  style,
  ...rest
}) => {
  const sizeStyles: Record<string, React.CSSProperties> = {
    sm: { height: '36px', padding: '0 0.85rem', fontSize: '0.8rem', borderRadius: '10px' },
    md: { height: 'var(--input-height)', padding: '0 1.25rem', fontSize: '0.9rem', borderRadius: 'var(--radius-button)' },
    lg: { height: '50px', padding: '0 1.5rem', fontSize: '1rem', borderRadius: '14px' },
    icon: { width: '34px', height: '34px', padding: 0, borderRadius: '10px' },
  };
  
  const variantClass = variant === 'primary'
    ? 'btn-primary'
    : variant === 'outline'
    ? 'btn-outline'
    : variant === 'danger'
    ? 'btn-danger'
    : variant === 'icon'
    ? 'btn-icon'
    : 'btn-ghost';

  return (
    <button
      {...rest}
      disabled={disabled || isLoading}
      className={`btn ${variantClass}${motion ? ` btn-icon--${motion}` : ''} ${className}`}
      style={{
        ...sizeStyles[size],
        ...style,
      }}
    >
      {isLoading ? (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
          <Spinner size={15} />
          <span>Loading</span>
        </span>
      ) : (
        <>
          {icon && <span style={{ display: 'flex', alignItems: 'center' }}>{icon}</span>}
          {children}
        </>
      )}
    </button>
  );
};
