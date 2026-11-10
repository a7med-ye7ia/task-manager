import React, { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { setPasswordApi, ApiError } from '../services/api';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { PasswordInput } from '../components/common/PasswordInput';
import { PasswordRuleList } from '../components/common/PasswordRuleList';
import { CheckCircle2 } from 'lucide-react';
import { ErrorNotice } from '../components/common/ErrorNotice';

export const SetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!token) {
      setGeneralError('Invalid or missing invitation token.');
      return false;
    }

    if (!password) {
      errors.password = 'Password is required';
    } else if (password.length < 8 || password.length > 64) {
      errors.password = 'Password must be between 8 and 64 characters';
    } else if (
      !/[A-Z]/.test(password) ||
      !/[a-z]/.test(password) ||
      !/[0-9]/.test(password) ||
      !/[^a-zA-Z0-9]/.test(password)
    ) {
      errors.password = 'Password must include uppercase, lowercase, a number, and a special character';
    }

    if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setGeneralError(null);

    if (!validate()) return;

    setIsSubmitting(true);

    try {
      await setPasswordApi(token!, password, confirmPassword);
      setSuccess(true);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.fieldErrors && err.fieldErrors.length > 0) {
          const map: Record<string, string> = {};
          err.fieldErrors.forEach((f) => {
            map[f.field] = f.message;
          });
          setFieldErrors(map);
        } else {
          setGeneralError(err.message);
        }
      } else {
        setGeneralError('Unable to activate account. The invitation link may have expired or is invalid.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '440px', margin: '3.5rem auto' }}>
      <Card style={{ padding: '2.5rem' }}>
        <h2 style={{ marginBottom: '0.35rem', textAlign: 'center', fontSize: '1.6rem', fontWeight: 700, letterSpacing: '-0.02em' }}>
          Set Your Password
        </h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.75rem', textAlign: 'center' }}>
          Welcome! Create your account password to activate your access
        </p>

        {success ? (
          <div style={{ textAlign: 'center' }}>
            <div
              style={{
                width: '64px',
                height: '64px',
                backgroundColor: 'var(--success-bg)',
                color: 'var(--success)',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1.25rem',
              }}
            >
              <CheckCircle2 size={36} />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', color: '#065f46' }}>
              Account Activated!
            </h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '1.75rem' }}>
              Your password has been successfully set. You can now sign in to your workspace.
            </p>
            <Link to="/login" className="btn btn-primary" style={{ width: '100%', height: 'var(--input-height)', padding: '0 1.25rem', fontSize: '0.9rem', borderRadius: 'var(--radius-button)' }}>
              Sign In
            </Link>
          </div>
        ) : (
          <form noValidate onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {generalError && (
              <div
                style={{
                  padding: '0.85rem 1rem',
                  borderRadius: 'var(--radius-button)',
                  backgroundColor: 'var(--danger-bg)',
                  border: '1px solid #fecaca',
                  color: 'var(--danger)',
                  fontSize: '0.875rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <ErrorNotice error={generalError} />
              </div>
            )}

            <div>
              <PasswordInput
                label="New Password"
                required
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  setFieldErrors((prev) => ({ ...prev, password: '' }));
                }}
                placeholder="Min 8 characters"
                error={fieldErrors.password}
              />
              <PasswordRuleList password={password} />
            </div>

            <PasswordInput
              label="Confirm Password"
              required
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                setFieldErrors((prev) => ({ ...prev, confirmPassword: '' }));
              }}
              placeholder="Re-enter password"
              error={fieldErrors.confirmPassword}
            />

            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={isSubmitting}
              style={{ marginTop: '0.5rem', width: '100%' }}
            >
              Set Password and Sign In
            </Button>
          </form>
        )}
      </Card>
    </div>
  );
};
