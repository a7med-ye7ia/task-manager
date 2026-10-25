import React from "react";
import { Link } from "react-router-dom";

export const PersonProfileLink: React.FC<{
  userId?: number | null;
  currentUserId?: number;
  active?: boolean;
  children: React.ReactNode;
  className?: string;
}> = ({ userId, currentUserId, active = true, children, className }) => {
  if (!userId || !active) return <>{children}</>;
  const href = userId === currentUserId ? "/profile" : `/users/${userId}`;
  return <Link to={href} className={className}>{children}</Link>;
};
