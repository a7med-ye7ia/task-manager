import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { PasswordRuleList } from './PasswordRuleList';

describe('PasswordRuleList Component', () => {
  it('renders all 5 password rules including special character', () => {
    render(<PasswordRuleList password="" />);
    expect(screen.getByText('8 to 64 characters')).toBeInTheDocument();
    expect(screen.getByText('At least one uppercase letter (A-Z)')).toBeInTheDocument();
    expect(screen.getByText('At least one lowercase letter (a-z)')).toBeInTheDocument();
    expect(screen.getByText('At least one number (0-9)')).toBeInTheDocument();
    expect(screen.getByText('At least one special character')).toBeInTheDocument();
  });

  it('marks special character rule as unmet with X icon when password has no special character', () => {
    render(<PasswordRuleList password="Password123" />);
    // Length, uppercase, lowercase, digit pass
    expect(screen.getByTestId('check-length')).toBeInTheDocument();
    expect(screen.getByTestId('check-uppercase')).toBeInTheDocument();
    expect(screen.getByTestId('check-lowercase')).toBeInTheDocument();
    expect(screen.getByTestId('check-digit')).toBeInTheDocument();
    // Special character fails
    expect(screen.getByTestId('x-special')).toBeInTheDocument();
  });

  it('marks special character rule as met with Check icon when special character is provided', () => {
    render(<PasswordRuleList password="Password123!" />);
    expect(screen.getByTestId('check-special')).toBeInTheDocument();
    expect(screen.queryByTestId('x-special')).not.toBeInTheDocument();
  });
});
