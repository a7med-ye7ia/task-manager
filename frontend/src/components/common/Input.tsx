import React from 'react';
import { FieldError } from './FieldError';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  icon?: React.ReactNode;
  error?: string;
  helperText?: string;
}

export const Input: React.FC<InputProps> = ({
  label,
  icon,
  error,
  helperText,
  id,
  className = '',
  style,
  ...rest
}) => {
  const inputId = id || (label ? label.toLowerCase().replace(/[^a-z0-9]/g, '-') : undefined);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', width: '100%' }}>
      {label && (
        <label
          htmlFor={inputId}
          style={{
            fontSize: '0.85rem',
            fontWeight: 600,
            marginBottom: '0.4rem',
            color: 'var(--text-main)',
          }}
        >
          {label}
        </label>
      )}

      <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
        {icon && (
          <span
            style={{
              position: 'absolute',
              left: '0.9rem',
              color: 'var(--text-muted)',
              display: 'flex',
              alignItems: 'center',
              pointerEvents: 'none',
            }}
          >
            {icon}
          </span>
        )}

        <input
          {...rest}
          id={inputId}
          className={`form-input ${error ? 'input-error' : ''} ${className}`}
          style={{
            paddingLeft: icon ? '2.5rem' : '1rem',
            ...style,
          }}
        />
      </div>

      {error ? (
        <FieldError message={error} />
      ) : helperText ? (
        <span
          style={{
            color: 'var(--text-muted)',
            fontSize: '0.75rem',
            marginTop: '0.35rem',
          }}
        >
          {helperText}
        </span>
      ) : null}
    </div>
  );
};