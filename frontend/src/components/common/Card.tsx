import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
}

export const Card: React.FC<CardProps> = ({ children, className = '', style, ...rest }) => {
  return (
    <div
      {...rest}
      className={`card ${className}`}
      style={{
        borderRadius: 'var(--radius-card)',
        backgroundColor: 'var(--bg-card)',
        border: '1px solid var(--border)',
        boxShadow: 'var(--shadow-sm)',
        padding: '1.75rem',
        ...style,
      }}
    >
      {children}
    </div>
  );
};
