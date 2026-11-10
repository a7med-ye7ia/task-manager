import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPasswordApi, ApiError } from '../services/api';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Mail, ArrowLeft } from 'lucide-react';
import { ErrorNotice } from '../components/common/ErrorNotice';

export const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await forgotPasswordApi(email.trim().toLowerCase());
      setSubmitted(true);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.getFormattedMessage());
      } else {
        setError('Failed to send reset link. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '440px', margin: '3.5rem auto' }}>
      <Card style={{ padding: '2.5rem' }}>
        <h2 style={{ marginBottom: '0.35rem', textAlign: 'center', fontSize: '1.6rem', fontWeight: 700, letterSpacing: '-0.02em' }}>
          Reset Password
        </h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.75rem', textAlign: 'center' }}>
          Enter your email to receive a password recovery link
        </p>

        {submitted ? (
          <div style={{ textAlign: 'center' }}>
            <div
              style={{
                width: '64px',
                height: '64px',
                backgroundColor: 'var(--primary-light)',
                color: 'var(--primary)',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1.25rem',
              }}
            >
              <Mail size={32} />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--text-main)' }}>
              Check Your Inbox
            </h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '1.75rem' }}>
              If an active account exists with this address, a reset link has been sent. It expires in 30 minutes.
            </p>
            <Link to="/login" className="btn btn-primary" style={{ width: '100%', height: 'var(--input-height)', padding: '0 1.25rem', fontSize: '0.9rem', borderRadius: 'var(--radius-button)' }}>
              Back to Sign In
            </Link>
          </div>
        ) : (
          <form noValidate onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {error && (
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
                <ErrorNotice error={error} />
              </div>
            )}

            <Input
              label="Email address"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@gmail.com"
              icon={<Mail size={18} />}
            />

            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={isSubmitting}
              style={{ marginTop: '0.5rem', width: '100%' }}
            >
              Send Reset Link
            </Button>

            <div style={{ textAlign: 'center', marginTop: '0.5rem' }}>
              <Link
                to="/login"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                  color: 'var(--text-muted)',
                  fontSize: '0.875rem',
                  fontWeight: 500,
                }}
              >
                <ArrowLeft size={16} /> Back to Sign In
              </Link>
            </div>
          </form>
        )}
      </Card>
    </div>
  );
};
