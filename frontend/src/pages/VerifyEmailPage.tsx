import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { verifyEmailApi, ApiError } from '../services/api';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { CheckCircle2, XCircle } from 'lucide-react';
import { PageLoader } from '../components/common/LoadingStates';

export const VerifyEmailPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [loading, setLoading] = useState(true);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setErrorMessage('Missing verification token in link.');
      setLoading(false);
      return;
    }

    const runVerify = async () => {
      try {
        const res = await verifyEmailApi(token);
        setSuccessMessage(res.message);
      } catch (err) {
        if (err instanceof ApiError) {
          setErrorMessage(err.message);
        } else {
          setErrorMessage('Email verification failed. The link may have expired or is invalid.');
        }
      } finally {
        setLoading(false);
      }
    };

    runVerify();
  }, [token]);

  return (
    <div style={{ maxWidth: '440px', margin: '3.5rem auto' }}>
      <Card style={{ padding: '2.5rem', textAlign: 'center' }}>
        {loading ? (
          <PageLoader label="Verifying email" />
        ) : successMessage ? (
          <div>
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
            <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.75rem', color: '#065f46' }}>
              Email Verified!
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', lineHeight: 1.5, marginBottom: '1.75rem' }}>
              {successMessage}
            </p>
            <Link to="/login">
              <Button variant="primary" size="md" style={{ width: '100%' }}>
                Go to Sign In
              </Button>
            </Link>
          </div>
        ) : (
          <div>
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
            <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.75rem', color: '#991b1b' }}>
              Verification Failed
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', lineHeight: 1.5, marginBottom: '1.75rem' }}>
              {errorMessage}
            </p>
            <Link to="/login">
              <Button variant="primary" size="md" style={{ width: '100%' }}>
                Return to Sign In
              </Button>
            </Link>
          </div>
        )}
      </Card>
    </div>
  );
};
