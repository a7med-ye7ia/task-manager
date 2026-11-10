import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TaskPicker } from './TaskPicker';
import { getTimeTaskOptionsApi } from '../../services/api';

vi.mock('../../services/api', () => ({
  getTimeTaskOptionsApi: vi.fn(),
}));

describe('TaskPicker', () => {
  it('searches and selects a task option', async () => {
    vi.mocked(getTimeTaskOptionsApi).mockResolvedValue({
      content: [{ id: 12, title: 'Prepare report', status: 'IN_PROGRESS', deadline: '2026-11-20' }],
      page: 0, size: 20, totalPages: 1, totalElements: 1, first: true, last: true,
    });
    const onChange = vi.fn();
    render(<TaskPicker value={null} onChange={onChange} />);
    fireEvent.change(screen.getByRole('textbox', { name: /search task/i }), { target: { value: 'report' } });
    await waitFor(() => expect(screen.getByRole('button', { name: /prepare report/i })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /prepare report/i }));
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ id: 12 }));
  });
});
