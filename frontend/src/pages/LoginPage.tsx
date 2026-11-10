import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../services/api';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { PasswordInput } from '../components/common/PasswordInput';
import { Mail, LogIn } from 'lucide-react';
import { ErrorNotice } from '../components/common/ErrorNotice';

export const LoginPage: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sessionNotice] = useState(() => { const notice = sessionStorage.getItem('authNotice'); sessionStorage.removeItem('authNotice'); return notice; });

  React.useEffect(() => {
    if (isAuthenticated) {
      navigate('/tasks', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const routeState = location.state as { from?: { pathname: string }; notice?: string } | null;
  const from = routeState?.from?.pathname || '/';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.getFormattedMessage());
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('An unexpected error occurred during sign in. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '440px', margin: '3.5rem auto' }}>
      <Card style={{ padding: '2.5rem' }}>
        <h2 style={{ marginBottom: '0.35rem', textAlign: 'center', fontSize: '1.6rem', fontWeight: 700, letterSpacing: '-0.02em' }}>
          Sign In
        </h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.75rem', textAlign: 'center' }}>
          Enter your credentials to access your workspace
        </p>

        {(routeState?.notice || sessionNotice || error) && (
          <div
            style={{
              padding: '0.85rem 1rem',
              marginBottom: '1.25rem',
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
            <ErrorNotice error={routeState?.notice || sessionNotice || (error === 'Full authentication is required to access this resource' ? 'Your session expired. Sign in again.' : error || undefined)} />
          </div>
        )}

        <form noValidate onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <Input
            label="Email address"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="name@gmail.com"
            icon={<Mail size={18} />}
          />

          <div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '-1.4rem', position: 'relative', zIndex: 1 }}>
              <Link
                to="/forgot-password"
                style={{ fontSize: '0.8rem', color: 'var(--primary)', fontWeight: 500 }}
              >
                Forgot password?
              </Link>
            </div>
            <PasswordInput
              label="Password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </div>

          <Button
            type="submit"
            variant="primary"
            size="md"
            isLoading={isSubmitting}
            icon={<LogIn size={18} />}
            style={{ marginTop: '0.5rem', width: '100%' }}
          >
            Sign In
          </Button>
        </form>

        <div style={{ marginTop: '2rem', textAlign: 'center', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Don't have an account?{' '}
          <Link to="/signup" style={{ color: 'var(--primary)', fontWeight: 600 }}>
            Sign up
          </Link>
        </div>
      </Card>
    </div>
  );
};
