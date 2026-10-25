import React, { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { resetPasswordApi, ApiError } from '../services/api';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { PasswordInput } from '../components/common/PasswordInput';
import { PasswordRuleList } from '../components/common/PasswordRuleList';
import { CheckCircle2, XCircle, AlertCircle } from 'lucide-react';

export const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isExpiredOrInvalid, setIsExpiredOrInvalid] = useState(!token);
  const [success, setSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!token) {
      setIsExpiredOrInvalid(true);
      return false;
    }

    if (!newPassword) {
      errors.newPassword = 'New password is required';
    } else if (newPassword.length < 8 || newPassword.length > 64) {
      errors.newPassword = 'Password must be between 8 and 64 characters';
    } else if (
      !/[A-Z]/.test(newPassword) ||
      !/[a-z]/.test(newPassword) ||
      !/[0-9]/.test(newPassword)
    ) {
      errors.newPassword = 'Password must include uppercase, lowercase, and a number';
    }

    if (newPassword !== confirmPassword) {
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
      await resetPasswordApi(token!, newPassword, confirmPassword);
      setSuccess(true);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === 'TOKEN_EXPIRED' || err.code === 'TOKEN_INVALID') {
          setIsExpiredOrInvalid(true);
          setGeneralError(err.message);
        } else if (err.fieldErrors && err.fieldErrors.length > 0) {
          const map: Record<string, string> = {};
          err.fieldErrors.forEach((f) => {
            map[f.field] = f.message;
          });
          setFieldErrors(map);
        } else {
          setGeneralError(err.message);
        }
      } else {
        setGeneralError('An unexpected error occurred while resetting your password.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '440px', margin: '3.5rem auto' }}>
      <Card style={{ padding: '2.5rem' }}>
        <h2 style={{ marginBottom: '0.35rem', textAlign: 'center', fontSize: '1.6rem', fontWeight: 700, letterSpacing: '-0.02em' }}>
          Choose New Password
        </h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.75rem', textAlign: 'center' }}>
          Create a new secure password for your account
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
              Password Reset Complete
            </h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '1.75rem' }}>
              Your password has been successfully updated. All previous sessions have been signed out.
            </p>
            <Link to="/login">
              <Button variant="primary" size="md" style={{ width: '100%' }}>
                Sign In
              </Button>
            </Link>
          </div>
        ) : isExpiredOrInvalid ? (
          <div style={{ textAlign: 'center' }}>
            <div
              style={{
                width: '64px',
                height: '64px',
                backgroundColor: 'var(--danger-bg)',
                color: 'var(--danger)',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1.25rem',
              }}
            >
              <XCircle size={36} />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', color: '#991b1b' }}>
              Link Expired or Invalid
            </h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '1.75rem' }}>
              {generalError ||
                'This password reset link is invalid or has already expired. Password reset links are valid for 30 minutes.'}
            </p>
            <Link to="/forgot-password">
              <Button variant="primary" size="md" style={{ width: '100%' }}>
                Request New Link
              </Button>
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
                <AlertCircle size={18} style={{ flexShrink: 0 }} />
                <span>{generalError}</span>
              </div>
            )}

            <div>
              <PasswordInput
                label="New Password"
                required
                value={newPassword}
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  setFieldErrors((prev) => ({ ...prev, newPassword: '' }));
                }}
                placeholder="Min 8 characters"
                error={fieldErrors.newPassword}
              />
              <PasswordRuleList password={newPassword} />
            </div>

            <PasswordInput
              label="Confirm Password"
              required
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                setFieldErrors((prev) => ({ ...prev, confirmPassword: '' }));
              }}
              placeholder="Re-enter new password"
              error={fieldErrors.confirmPassword}
            />

            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={isSubmitting}
              style={{ marginTop: '0.5rem', width: '100%' }}
            >
              Save New Password
            </Button>
          </form>
        )}
      </Card>
    </div>
  );
};
