import React, { useState } from 'react';
import { getAccessToken } from '../../services/api';

interface UserAvatarProps {
  userId?: number | string | null;
  avatarPath?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  size?: number; // size in px, default 36
  className?: string;
  style?: React.CSSProperties;
  cacheBust?: number | string; // pass timestamp on update
}

export const UserAvatar: React.FC<UserAvatarProps> = ({
  userId,
  avatarPath,
  firstName,
  lastName,
  size = 36,
  className = '',
  style,
  cacheBust,
}) => {
  const [loadError, setLoadError] = useState(false);
  const token = getAccessToken();

  const initials = `${firstName?.[0] || ''}${lastName?.[0] || ''}`.toUpperCase() || 'U';

  const avatarUrl = React.useMemo(() => {
    if (!avatarPath || !token) return null;
    const base = userId ? `/api/users/${userId}/avatar` : '/api/profile/avatar';
    const params = new URLSearchParams({ token });
    if (cacheBust) {
      params.append('t', String(cacheBust));
    }
    return `${base}?${params.toString()}`;
  }, [userId, avatarPath, token, cacheBust]);

  const circleStyle: React.CSSProperties = {
    width: `${size}px`,
    height: `${size}px`,
    borderRadius: '50%',
    backgroundColor: '#e2e8f0',
    color: '#475569',
    border: '1px solid var(--border, #e2e8f0)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 600,
    fontSize: `${Math.max(12, Math.round(size * 0.4))}px`,
    overflow: 'hidden',
    flexShrink: 0,
    userSelect: 'none',
    boxSizing: 'border-box',
    ...style,
  };

  if (!avatarUrl || loadError) {
    return (
      <div className={`user-avatar-placeholder ${className}`} style={circleStyle}>
        <span>{initials}</span>
      </div>
    );
  }

  return (
    <div className={`user-avatar-container ${className}`} style={circleStyle}>
      <img
        src={avatarUrl}
        alt={`${firstName || ''} ${lastName || ''}`}
        loading="lazy"
        onError={() => setLoadError(true)}
        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
      />
    </div>
  );
};
