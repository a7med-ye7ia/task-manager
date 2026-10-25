import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { PendingUserResponse, Role } from '../types';
import {
  getAdminPendingUsersApi,
  approveSignupApi,
  rejectSignupApi,
  ApiError,
} from '../services/api';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { Sparkles, Check, X, AlertCircle, CheckCircle2 } from 'lucide-react';
import { Select } from '../components/common/Select';
import { AppLoader, PageLoader, RequestError } from '../components/common/LoadingStates';

export const AdminPendingPage: React.FC = () => {
  const { isManager, refreshPendingCount, isLoading: authLoading } = useAuth();

  const [applicants, setApplicants] = useState<PendingUserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [loadError, setLoadError] = useState('');

  // Approval Modal
  const [approveUser, setApproveUser] = useState<PendingUserResponse | null>(null);
  const [approveRole, setApproveRole] = useState<Role>('EMPLOYEE');
  const [isApproving, setIsApproving] = useState(false);

  // Rejection Modal
  const [rejectUser, setRejectUser] = useState<PendingUserResponse | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [isRejecting, setIsRejecting] = useState(false);

  const fetchPending = useCallback(async () => {
    setLoading(true);
    setNotice(null);
    setLoadError('');
    try {
      const res = await getAdminPendingUsersApi(0, 50);
      setApplicants(res.content || []);
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.getFormattedMessage() : 'Could not load pending requests.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isManager) {
      fetchPending();
    }
  }, [isManager, fetchPending]);

  if (authLoading) {
    return <AppLoader />;
  }

  if (!isManager) {
    return (
      <Card style={{ maxWidth: '440px', margin: '4rem auto', textAlign: 'center' }}>
        <h3 style={{ color: 'var(--danger)', marginBottom: '0.75rem' }}>Access Denied</h3>
        <p style={{ color: 'var(--text-muted)' }}>
          Only Managers can view and review pending account sign-up requests.
        </p>
      </Card>
    );
  }

  const handleConfirmApprove = async () => {
    if (!approveUser) return;
    setIsApproving(true);
    try {
      const res = await approveSignupApi(approveUser.id, approveRole);
      setNotice({ type: 'success', text: res.message });
      setApproveUser(null);
      await fetchPending();
      await refreshPendingCount();
    } catch (err) {
      if (err instanceof ApiError) {
        setNotice({ type: 'error', text: err.getFormattedMessage() });
      } else if (err instanceof Error) {
        setNotice({ type: 'error', text: err.message });
      } else {
        setNotice({ type: 'error', text: 'Failed to approve application.' });
      }
    } finally {
      setIsApproving(false);
    }
  };

  const handleConfirmReject = async () => {
    if (!rejectUser) return;
    setIsRejecting(true);
    try {
      const res = await rejectSignupApi(rejectUser.id, rejectReason.trim() || undefined);
      setNotice({ type: 'success', text: res.message });
      setRejectUser(null);
      setRejectReason('');
      await fetchPending();
      await refreshPendingCount();
    } catch (err) {
      if (err instanceof ApiError) {
        setNotice({ type: 'error', text: err.getFormattedMessage() });
      } else if (err instanceof Error) {
        setNotice({ type: 'error', text: err.message });
      } else {
        setNotice({ type: 'error', text: 'Failed to reject application.' });
      }
    } finally {
      setIsRejecting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      <div className="page-header">
        <h1 className="page-title">Pending Sign-Up Requests</h1>
        <p className="page-subtitle">
          Review self-registered account applications awaiting Manager approval
        </p>
      </div>

      {notice && (
        <div
          style={{
            padding: '0.85rem 1rem',
            borderRadius: 'var(--radius-button)',
            fontSize: '0.875rem',
            backgroundColor: notice.type === 'success' ? 'var(--success-bg)' : 'var(--danger-bg)',
            border: `1px solid ${notice.type === 'success' ? '#a7f3d0' : '#fecaca'}`,
            color: notice.type === 'success' ? '#065f46' : 'var(--danger)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          {notice.type === 'success' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
          <span>{notice.text}</span>
        </div>
      )}
      {loadError && applicants.length === 0 && <RequestError message={loadError} onRetry={() => void fetchPending()} />}
      {loadError && applicants.length > 0 && <div className="refresh-notice" role="status">{loadError}<button type="button" onClick={() => setLoadError('')}>Dismiss</button></div>}

      {/* Table Card */}
      <Card style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)' }}>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600 }}>Applicant</th>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600 }}>Email</th>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600 }}>Phone</th>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600 }}>Registered</th>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600, textAlign: 'right' }}>Review Action</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '3.5rem', color: 'var(--text-muted)' }}>
                    <PageLoader label="Loading pending requests" />
                  </td>
                </tr>
              ) : applicants.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '3.5rem', color: 'var(--text-muted)' }}>
                    <div style={{ marginBottom: '0.5rem', display: 'flex', justifyContent: 'center' }}>
                      <Sparkles size={32} color="var(--primary)" />
                    </div>
                    <strong style={{ fontSize: '1rem', color: 'var(--text-main)' }}>No pending sign-up requests</strong>
                    <p style={{ marginTop: '0.25rem', fontSize: '0.85rem' }}>
                      All applicant registrations have been processed.
                    </p>
                  </td>
                </tr>
              ) : (
                applicants.map((app) => (
                  <tr key={app.id} style={{ borderBottom: '1px solid var(--border-light)' }}>
                    <td style={{ padding: '0.85rem 1.25rem', fontWeight: 600, color: 'var(--text-main)' }}>
                      {app.firstName} {app.lastName}
                    </td>
                    <td style={{ padding: '0.85rem 1.25rem' }}>{app.email}</td>
                    <td style={{ padding: '0.85rem 1.25rem', color: 'var(--text-muted)' }}>{app.phone}</td>
                    <td style={{ padding: '0.85rem 1.25rem', color: 'var(--text-muted)' }}>
                      {app.createdAt ? new Date(app.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td style={{ padding: '0.85rem 1.25rem', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <Button
                          onClick={() => {
                            setApproveUser(app);
                            setApproveRole('EMPLOYEE');
                          }}
                          variant="primary"
                          size="sm"
                          icon={<Check size={15} />}
                        >
                          Approve
                        </Button>
                        <Button
                          onClick={() => {
                            setRejectUser(app);
                            setRejectReason('');
                          }}
                          variant="outline"
                          size="sm"
                          icon={<X size={15} />}
                          style={{ color: 'var(--danger)' }}
                        >
                          Reject
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Approve Modal */}
      <Modal
        isOpen={!!approveUser}
        onClose={() => setApproveUser(null)}
        title="Approve Sign-Up Application"
        maxWidth="440px"
      >
        <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
          Assign a system role for <strong>{approveUser?.firstName} {approveUser?.lastName}</strong> ({approveUser?.email}).
        </p>

        <div style={{ marginBottom: '1.5rem' }}>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.4rem' }}>
            System Role
          </label>
          <Select value={approveRole} onValueChange={(role) => setApproveRole(role as Role)} options={[{ value: 'EMPLOYEE', label: 'Employee' }, { value: 'MANAGER', label: 'Manager' }]} ariaLabel="System role" />
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
          <Button
            variant="outline"
            onClick={() => setApproveUser(null)}
            disabled={isApproving}
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleConfirmApprove}
            isLoading={isApproving}
          >
            Confirm Approval
          </Button>
        </div>
      </Modal>

      {/* Reject Modal */}
      <Modal
        isOpen={!!rejectUser}
        onClose={() => setRejectUser(null)}
        title="Reject Sign-Up Application"
        maxWidth="440px"
      >
        <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
          Are you sure you want to reject the application for <strong>{rejectUser?.firstName} {rejectUser?.lastName}</strong> ({rejectUser?.email})?
        </p>

        <div style={{ marginBottom: '1.5rem' }}>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.4rem' }}>
            Reason (Optional)
          </label>
          <textarea
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="e.g. Incomplete application or unverified details"
            rows={3}
            style={{
              width: '100%',
              padding: '0.75rem',
              borderRadius: 'var(--radius-input)',
              border: '1px solid var(--border)',
              fontFamily: 'inherit',
              fontSize: '0.9rem',
              outline: 'none',
            }}
          />
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
          <Button
            variant="outline"
            onClick={() => setRejectUser(null)}
            disabled={isRejecting}
          >
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={handleConfirmReject}
            isLoading={isRejecting}
          >
            Reject Application
          </Button>
        </div>
      </Modal>
    </div>
  );
};
