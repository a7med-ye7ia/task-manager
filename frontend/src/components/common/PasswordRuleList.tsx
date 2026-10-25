import React from 'react';
import { Check, X } from 'lucide-react';

interface PasswordRuleListProps {
  password: string;
}

interface RuleItem {
  id: string;
  label: string;
  passed: boolean;
}

export const PasswordRuleList: React.FC<PasswordRuleListProps> = ({ password }) => {
  const rules: RuleItem[] = [
    {
      id: 'length',
      label: '8 to 64 characters',
      passed: password.length >= 8 && password.length <= 64,
    },
    {
      id: 'uppercase',
      label: 'At least one uppercase letter (A-Z)',
      passed: /[A-Z]/.test(password),
    },
    {
      id: 'lowercase',
      label: 'At least one lowercase letter (a-z)',
      passed: /[a-z]/.test(password),
    },
    {
      id: 'digit',
      label: 'At least one number (0-9)',
      passed: /[0-9]/.test(password),
    },
  ];

  return (
    <div
      style={{
        marginTop: '0.5rem',
        marginBottom: '0.75rem',
        padding: '0.75rem 1rem',
        borderRadius: 'var(--radius-button)',
        background: '#f8fafc',
        border: '1px solid var(--border)',
        fontSize: '0.8rem',
      }}
    >
      <div style={{ fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.4rem' }}>
        Password requirements:
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
        {rules.map((rule) => {
          const color = rule.passed ? 'var(--success)' : 'var(--danger)';
          return (
            <div
              key={rule.id}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.45rem',
                color,
                transition: 'color 0.15s ease-in-out',
                fontWeight: 500,
              }}
            >
              {rule.passed ? (
                <Check size={14} strokeWidth={2.5} style={{ flexShrink: 0 }} />
              ) : (
                <X size={14} strokeWidth={2.5} style={{ flexShrink: 0 }} />
              )}
              <span>{rule.label}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
};
