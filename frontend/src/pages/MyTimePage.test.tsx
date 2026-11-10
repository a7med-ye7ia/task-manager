import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { MyTimePage } from './MyTimePage';
import { listTimeSessionsApi, getTimeTaskOptionsApi } from '../services/api';

vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ isAuthenticated: true }) }));

vi.mock('../services/api', async () => {
  const actual = await vi.importActual<typeof import('../services/api')>('../services/api');
  return { ...actual, listTimeSessionsApi: vi.fn(), getTimeTaskOptionsApi: vi.fn() };
});

describe('MyTimePage validation', () => {
  it('places the required task error under the task picker', async () => {
    vi.mocked(listTimeSessionsApi).mockResolvedValue({ from: '2026-11-01', to: '2026-11-07', workedSeconds: 0, regularSeconds: 0, overtimeSeconds: 0, daysWorked: 0, days: [] });
    vi.mocked(getTimeTaskOptionsApi).mockResolvedValue({ content: [], page: 0, size: 20, totalPages: 0, totalElements: 0, first: true, last: true });
    render(<MemoryRouter><MyTimePage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('button', { name: /add manual entry/i })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /add manual entry/i }));
    fireEvent.click(screen.getByRole('button', { name: /add entry/i }));
    expect(screen.getByText('Choose a task.')).toBeInTheDocument();
  });

  it('shows compact day cards and expands remaining entries', async () => {
    vi.mocked(listTimeSessionsApi).mockResolvedValue({ from: '2026-11-01', to: '2026-11-07', workedSeconds: 300, regularSeconds: 300, overtimeSeconds: 0, daysWorked: 1, days: [{ date: '2026-11-05', workedSeconds: 300, regularSeconds: 300, overtimeSeconds: 0, entries: Array.from({ length: 5 }, (_, index) => ({ id: index + 1, taskId: 1, taskTitle: 'Planning', startedAt: '2026-11-05T08:00:00Z', endedAt: '2026-11-05T08:01:00Z', durationSeconds: 60, workedSeconds: 60, source: 'MANUAL' as const, state: 'ENDED' as const, editable: true })) }] });
    vi.mocked(getTimeTaskOptionsApi).mockResolvedValue({ content: [], page: 0, size: 20, totalPages: 0, totalElements: 0, first: true, last: true });
    render(<MemoryRouter><MyTimePage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Planning')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Show 1 more' }));
    expect(screen.getAllByText('Planning')).toHaveLength(5);
  });
});
