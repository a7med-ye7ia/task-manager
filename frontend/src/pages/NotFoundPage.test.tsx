import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { NotFoundState } from './NotFoundPage';

describe('file not-found state', () => {
  it('uses the styled 404 state and real back button', () => {
    const onBack = vi.fn();
    render(<NotFoundState title="Error 404" message="This file is not available" backLabel="Back to task" onBack={onBack} />);
    expect(screen.getByText('Error 404')).toBeInTheDocument();
    expect(screen.getByText('This file is not available')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Back to task' }));
    expect(onBack).toHaveBeenCalledOnce();
  });
});
