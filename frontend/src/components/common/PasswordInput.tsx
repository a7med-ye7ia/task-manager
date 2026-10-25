import React, { useState } from 'react';
import { Eye, EyeOff, Lock, AlertCircle } from 'lucide-react';

export interface PasswordInputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: string;
  error?: string;
  helperText?: string;
}

export const PasswordInput: React.FC<PasswordInputProps> = ({
  label,
  error,
  helperText,
  id,
  className = '',
  style,
  ...rest
}) => {
  const [showPassword, setShowPassword] = useState(false);
  const inputId = id || (label ? label.toLowerCase().replace(/[^a-z0-9]/g, '-') : undefined);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', width: '100%' }}>
      {label && (
        <label
          htmlFor={inputId}
          style={{
            display: 'block',
            marginBottom: '0.4rem',
            fontWeight: 600,
            fontSize: '0.85rem',
            color: 'var(--text-main)',
          }}
        >
          {label}
        </label>
      )}

      <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
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
          <Lock size={18} />
        </span>

        <input
          {...rest}
          id={inputId}
          type={showPassword ? 'text' : 'password'}
          className={`form-input ${error ? 'input-error' : ''} ${className}`}
          style={{
            paddingLeft: '2.5rem',
            paddingRight: '2.5rem',
            ...style,
          }}
        />

        <button
          type="button"
          onClick={() => setShowPassword((prev) => !prev)}
          tabIndex={-1}
          aria-label={showPassword ? 'Hide password' : 'Show password'}
          style={{
            position: 'absolute',
            right: '0.75rem',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: '0.25rem',
            color: 'var(--text-muted)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: '6px',
          }}
        >
          {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>

      {error ? (
        <span
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.35rem',
            color: 'var(--danger)',
            fontSize: '0.775rem',
            marginTop: '0.35rem',
          }}
        >
          <AlertCircle size={14} style={{ flexShrink: 0 }} />
          {error}
        </span>
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
