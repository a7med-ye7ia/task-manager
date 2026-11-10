import React from 'react';
import { File } from 'lucide-react';

type Props = { progress: number; state?: 'idle' | 'uploading' | 'success' | 'error'; size?: number };

const SuccessFile: React.FC<{ size: number }> = ({ size }) => (
  <svg viewBox="0 0 100 100" width={size} height={size} role="img" aria-label="Upload complete">
    <path d="M20 8h37l23 23v61H20z M57 8v25h23" fill="none" stroke="currentColor" strokeWidth="5" strokeLinejoin="round" />
    <path d="m31 60 12 12 27-29" fill="none" stroke="currentColor" strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" className="file-success-check" />
  </svg>
);

const Shredder: React.FC<{ size: number }> = ({ size }) => (
  <svg viewBox="0 0 100 100" width={size} height={size} role="img" aria-label="Upload failed" data-testid="file-shredder-icon">
    <path d="M18 31h64l-5 12H23z" fill="currentColor" opacity=".25" />
    <path d="M25 46h50l-5 36H30z" fill="none" stroke="currentColor" strokeWidth="6" strokeLinejoin="round" />
    <path d="M33 52v22m11-22v22m11-22v22m11-22v22M29 31l-5-9m47 9 5-9M21 85h58" stroke="currentColor" strokeWidth="6" strokeLinecap="round" />
  </svg>
);

export const FileFillIcon: React.FC<Props> = ({ progress, state = 'idle', size = 46 }) => {
  const level = Math.max(0, Math.min(100, Math.round(progress)));
  if (state === 'error') return <span className="file-fill-icon is-error" style={{ width: size, height: size }}><Shredder size={size} /></span>;
  if (state === 'success') return <span className="file-fill-icon is-success" style={{ width: size, height: size }}><SuccessFile size={size} /></span>;
  return <span className={`file-fill-icon is-${state}`} style={{ width: size, height: size }} aria-label={state === 'uploading' ? `${level} percent uploaded` : undefined}><File size={size} strokeWidth={1.6} aria-hidden="true" /></span>;
};
