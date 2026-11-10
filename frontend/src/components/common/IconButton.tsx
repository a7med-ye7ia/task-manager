import React from 'react';
import { Button, ButtonProps } from './Button';

export interface IconButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  icon: React.ReactNode;
  'aria-label': string;
  title?: string;
  motion?: ButtonProps['motion'];
  className?: string;
}

export const IconButton: React.FC<IconButtonProps> = ({
  icon,
  'aria-label': ariaLabel,
  title,
  motion,
  className = '',
  ...props
}) => {
  return (
    <Button
      type="button"
      variant="icon"
      size="icon"
      motion={motion}
      aria-label={ariaLabel}
      title={title || ariaLabel}
      icon={icon}
      className={`icon-button-shared ${className}`}
      {...props}
    />
  );
};
