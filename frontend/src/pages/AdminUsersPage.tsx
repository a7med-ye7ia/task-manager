import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { User, UserStatus } from '../types';
import {
  getAdminUsersApi,
  resendInviteApi,
  deactivateUserApi,
  restoreUserApi,
  ApiError,
} from '../services/api';
import { UserModal } from '../components/users/UserModal';
import { UserAvatar } from '../components/avatar/UserAvatar';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { Input } from '../components/common/Input';
import {
  Search,
  UserPlus,
  Eye,
  Mail,
  UserX,
  RotateCcw,
  CheckCircle2,
  AlertCircle,
  Users as UsersIcon,
} from 'lucide-react';
import { Select } from '../components/common/Select';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { AppLoader, PageLoader, RequestError } from '../components/common/LoadingStates';

export const AdminUsersPage: React.FC = () => {
  const { isManager, isLoading: authLoading } = useAuth();

  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(0);

  // Filters
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [includeDeleted, setIncludeDeleted] = useState(false);

  // Notifications
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [loadError, setLoadError] = useState('');

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deactivateTarget, setDeactivateTarget] = useState<User | null>(null);

  const fetchUsers = useCallback(async (page = 0) => {
    setLoading(true);
    setNotice(null);
    setLoadError('');
    try {
      const res = await getAdminUsersApi({
        page,
        size: 15,
        search: search.trim() || undefined,
        role: roleFilter && roleFilter !== 'ALL' ? roleFilter : undefined,
        status: statusFilter && statusFilter !== 'ALL' ? statusFilter : undefined,
        includeDeleted,
      });
      setUsers(res.content || []);
      setTotalElements(res.totalElements ?? 0);
      setTotalPages(res.totalPages ?? 1);
      setCurrentPage(page);
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.getFormattedMessage() : 'Could not load users.');
    } finally {
      setLoading(false);
    }
  }, [search, roleFilter, statusFilter, includeDeleted]);

  useEffect(() => {
    if (isManager) {
      fetchUsers(0);
    }
  }, [isManager, fetchUsers]);

  if (authLoading) {
    return <AppLoader />;
  }

  if (!isManager) {
    return (
      <Card style={{ maxWidth: '440px', margin: '4rem auto', textAlign: 'center' }}>
        <h3 style={{ color: 'var(--danger)', marginBottom: '0.75rem' }}>Access Denied</h3>
        <p style={{ color: 'var(--text-muted)' }}>
          You must be signed in as a Manager to access the User Management directory.
        </p>
      </Card>
    );
  }

  const handleResendInvite = async (user: User) => {
    try {
      const res = await resendInviteApi(user.id);
      setNotice({ type: 'success', text: res.message });
      fetchUsers(currentPage);
    } catch (err) {
      if (err instanceof ApiError) {
        setNotice({ type: 'error', text: err.getFormattedMessage() });
      } else if (err instanceof Error) {
        setNotice({ type: 'error', text: err.message });
      } else {
        setNotice({ type: 'error', text: 'Failed to resend invitation.' });
      }
    }
  };

  const handleDeactivate = async (user: User) => {
    try {
      const res = await deactivateUserApi(user.id);
      setNotice({ type: 'success', text: res.message });
      fetchUsers(currentPage);
    } catch (err) {
      if (err instanceof ApiError) {
        setNotice({ type: 'error', text: err.getFormattedMessage() });
      } else if (err instanceof Error) {
        setNotice({ type: 'error', text: err.message });
      } else {
        setNotice({ type: 'error', text: 'Failed to deactivate user.' });
      }
    }
  };

  const handleRestore = async (user: User) => {
    try {
      const res = await restoreUserApi(user.id);
      setNotice({ type: 'success', text: res.message });
      fetchUsers(currentPage);
    } catch (err) {
      if (err instanceof ApiError) {
        setNotice({ type: 'error', text: err.getFormattedMessage() });
      } else if (err instanceof Error) {
        setNotice({ type: 'error', text: err.message });
      } else {
        setNotice({ type: 'error', text: 'Failed to restore user.' });
      }
    }
  };

  const renderStatusBadge = (status: UserStatus, isDeleted: boolean) => {
    if (isDeleted) {
      return <Badge variant="danger">Deleted</Badge>;
    }
    switch (status) {
      case 'ACTIVE':
        return <Badge variant="success">Active</Badge>;
      case 'INVITED':
        return <Badge variant="primary">Invited</Badge>;
      case 'PENDING_APPROVAL':
        return <Badge variant="warning">Pending Approval</Badge>;
      case 'PENDING_EMAIL':
        return <Badge variant="warning">Pending Email</Badge>;
      case 'DISABLED':
        return <Badge variant="danger">Disabled</Badge>;
      case 'REJECTED':
        return <Badge variant="danger">Rejected</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">User Management</h1>
          <p className="page-subtitle">Manage team accounts, invites, permissions, and roles</p>
        </div>
        <Button
          onClick={() => {
            setIsModalOpen(true);
          }}
          variant="primary"
          icon={<UserPlus size={18} />}
        >
          Invite Employee
        </Button>
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
      {loadError && users.length === 0 && <RequestError message={loadError} onRetry={() => void fetchUsers(currentPage)} />}
      {loadError && users.length > 0 && <div className="refresh-notice" role="status">{loadError}<button type="button" onClick={() => setLoadError('')}>Dismiss</button></div>}

      {/* Filter Bar */}
      <Card style={{ padding: '1.25rem' }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ flex: '1 1 240px' }}>
            <Input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by name or email..."
              icon={<Search size={18} />}
            />
          </div>

          <div className="admin-filter-field" style={{ width: '150px' }}>
            <Select value={roleFilter} onValueChange={setRoleFilter} placeholder="All roles" options={[{ value: 'ALL', label: 'All roles' }, { value: 'EMPLOYEE', label: 'Employee' }, { value: 'MANAGER', label: 'Manager' }]} ariaLabel="Role filter" />
          </div>

          <div className="admin-filter-field" style={{ width: '160px' }}>
            <Select value={statusFilter} onValueChange={setStatusFilter} placeholder="All statuses" options={[{ value: 'ALL', label: 'All statuses' }, { value: 'ACTIVE', label: 'Active' }, { value: 'INVITED', label: 'Invited' }, { value: 'PENDING_APPROVAL', label: 'Pending approval' }, { value: 'PENDING_EMAIL', label: 'Pending email' }, { value: 'DISABLED', label: 'Disabled' }, { value: 'REJECTED', label: 'Rejected' }]} ariaLabel="Status filter" />
          </div>

          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', cursor: 'pointer', userSelect: 'none' }}>
            <input
              type="checkbox"
              checked={includeDeleted}
              onChange={(e) => setIncludeDeleted(e.target.checked)}
              style={{ width: '16px', height: '16px', accentColor: 'var(--primary)' }}
            />
            Include deleted
          </label>
        </div>
      </Card>

      {/* Users Table */}
      <Card style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border)', color: 'var(--text-muted)' }}>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600 }}>User</th>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600 }}>Contact</th>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600 }}>Role</th>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600 }}>Hired Date</th>
                <th style={{ padding: '0.9rem 1.25rem', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '3.5rem', color: 'var(--text-muted)' }}>
                    <PageLoader label="Loading users" />
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '3.5rem', color: 'var(--text-muted)' }}>
                    <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '0.75rem' }}>
                      <UsersIcon size={32} color="var(--text-muted)" />
                    </div>
                    <strong>No users found matching the filter criteria.</strong>
                  </td>
                </tr>
              ) : (
                users.map((u) => {
                  const isDeleted = !!u.deletedAt;

                  return (
                    <tr
                      key={u.id}
                      style={{
                        borderBottom: '1px solid var(--border-light)',
                        opacity: isDeleted ? 0.6 : 1,
                        backgroundColor: isDeleted ? '#fafafa' : undefined,
                      }}
                    >
                      <td style={{ padding: '0.85rem 1.25rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
                          <UserAvatar
                            userId={u.id}
                            avatarPath={u.avatarPath}
                            firstName={u.firstName}
                            lastName={u.lastName}
                            size={38}
                          />
                          <div>
                            <Link to={`/admin/users/${u.id}`} style={{ fontWeight: 600, color: 'var(--text-main)' }}>
                              {u.firstName} {u.lastName}
                            </Link>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>ID: {u.id}</div>
                          </div>
                        </div>
                      </td>

                      <td style={{ padding: '0.85rem 1.25rem' }}>
                        <div style={{ fontWeight: 500 }}>{u.email}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{u.phone}</div>
                      </td>

                      <td style={{ padding: '0.85rem 1.25rem' }}>
                        <Badge variant={u.role === 'MANAGER' ? 'primary' : 'default'}>
                          {u.role}
                        </Badge>
                      </td>

                      <td style={{ padding: '0.85rem 1.25rem' }}>
                        {renderStatusBadge(u.status, isDeleted)}
                      </td>

                      <td style={{ padding: '0.85rem 1.25rem', color: 'var(--text-muted)' }}>
                        {u.hiredAt || '—'}
                      </td>

                      <td style={{ padding: '0.85rem 1.25rem', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '0.4rem' }}>
                          {!isDeleted && (
                            <>
                              <Link to={`/admin/users/${u.id}`}>
                              <Button
                                variant="outline"
                                size="sm"
                                title="Preview user"
                                style={{ padding: '0.35rem 0.5rem', height: '32px' }}
                              >
                                <Eye size={14} />
                              </Button>
                              </Link>

                              {u.status === 'INVITED' && (
                                <Button
                                  onClick={() => handleResendInvite(u)}
                                  variant="outline"
                                  size="sm"
                                  title="Resend Invitation Email"
                                  style={{ padding: '0.35rem 0.5rem', height: '32px' }}
                                >
                                  <Mail size={14} />
                                </Button>
                              )}

                              {u.status === 'ACTIVE' && (
                                <Button
                                  onClick={() => setDeactivateTarget(u)}
                                  variant="outline"
                                  size="sm"
                                  title="Deactivate User"
                                  style={{ padding: '0.35rem 0.5rem', height: '32px', color: 'var(--warning)' }}
                                >
                                  <UserX size={14} />
                                </Button>
                              )}

                            </>
                          )}

                          {(isDeleted || u.status === 'DISABLED') && (
                            <Button
                              onClick={() => handleRestore(u)}
                              variant="outline"
                              size="sm"
                              title="Restore User to ACTIVE"
                              style={{ padding: '0.35rem 0.5rem', height: '32px', color: 'var(--success)' }}
                            >
                              <RotateCcw size={14} />
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div
            style={{
              padding: '1rem 1.25rem',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              borderTop: '1px solid var(--border-light)',
              backgroundColor: '#f8fafc',
            }}
          >
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Total {totalElements} users • Page {currentPage + 1} of {totalPages}
            </span>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Button
                disabled={currentPage === 0}
                onClick={() => fetchUsers(currentPage - 1)}
                variant="outline"
                size="sm"
              >
                Previous
              </Button>
              <Button
                disabled={currentPage >= totalPages - 1}
                onClick={() => fetchUsers(currentPage + 1)}
                variant="outline"
                size="sm"
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Invite / Edit User Modal */}
      <UserModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
        }}
        onSaved={(msg) => {
          setNotice({ type: 'success', text: msg });
          fetchUsers(currentPage);
        }}
        userToEdit={null}
      />
      {deactivateTarget && <ConfirmDialog open title="Deactivate user" message={`Deactivate ${deactivateTarget.firstName} ${deactivateTarget.lastName}?`} confirmLabel="Deactivate" onClose={() => setDeactivateTarget(null)} onConfirm={() => { const user = deactivateTarget; setDeactivateTarget(null); handleDeactivate(user); }} />}
    </div>
  );
};
