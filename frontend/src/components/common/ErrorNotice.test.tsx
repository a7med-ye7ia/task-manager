import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ErrorNotice } from './ErrorNotice';

describe('ErrorNotice', () => {
  it('shows the server message and reference', () => {
    render(<ErrorNotice error={{ message: 'Upload rejected', fieldErrors: [], traceId: 'trace-9' }} />);
    expect(screen.getByText('Upload rejected')).toBeInTheDocument();
    expect(screen.getByText('Reference: trace-9')).toBeInTheDocument();
  });
});
