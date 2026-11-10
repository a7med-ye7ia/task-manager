import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { UserDetailsPage } from './UserDetailsPage';
import { getAdminUserApi, getUserTasksApi, getProfileApi } from '../services/api';

vi.mock('../services/api', async () => ({
  ...(await vi.importActual<typeof import('../services/api')>('../services/api')),
  getAdminUserApi: vi.fn(),
  getUserTasksApi: vi.fn(),
  getProfileApi: vi.fn(),
}));

describe('UserDetailsPage Layout & No Duplicated Fields', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getProfileApi).mockResolvedValue({
      id: 1,
      role: 'MANAGER',
      firstName: 'Admin',
      lastName: 'Manager',
    } as any);
  });

  it('renders header card with avatar, name, badges and actions only, without duplicating email/phone', async () => {
    vi.mocked(getAdminUserApi).mockResolvedValue({
      id: 2,
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@gmail.com',
      phone: '+201012345678',
      role: 'EMPLOYEE',
      status: 'ACTIVE',
      hiredAt: '2025-01-15',
      createdAt: '2025-01-15T10:00:00Z',
    } as any);
    vi.mocked(getUserTasksApi).mockResolvedValue([
      { id: 10, title: 'Task 1', status: 'TODO', priority: 'HIGH', userRole: 'LEAD', deadline: '2026-12-01' },
      { id: 11, title: 'Task 2', status: 'DONE', priority: 'MEDIUM', userRole: 'MEMBER', deadline: '2026-11-01' },
    ] as any);

    render(
      <MemoryRouter initialEntries={['/admin/users/2']}>
        <Routes>
          <Route path="/admin/users/:id" element={<UserDetailsPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
    });

    // Overview tab should render the sectioned layout
    expect(screen.getByTestId('user-overview-tab')).toBeInTheDocument();
    expect(screen.getByText('Contact')).toBeInTheDocument();
    expect(screen.getByText('Account')).toBeInTheDocument();
    expect(screen.getByText('Work')).toBeInTheDocument();

    // Email and phone appear in the Contact section of Overview tab
    expect(screen.getByText('john.doe@gmail.com')).toBeInTheDocument();
    expect(screen.getByText('+201012345678')).toBeInTheDocument();

    // Verify email appears exactly once on the page (only in Contact section, not in header)
    const emailOccurrences = screen.getAllByText('john.doe@gmail.com');
    expect(emailOccurrences).toHaveLength(1);

    // Verify phone appears exactly once
    const phoneOccurrences = screen.getAllByText('+201012345678');
    expect(phoneOccurrences).toHaveLength(1);

    // Work section shows computed metrics
    expect(screen.getByText('Active Tasks')).toBeInTheDocument();
    expect(screen.getByText('Tasks Led')).toBeInTheDocument();
    expect(screen.getByText('Tasks Done')).toBeInTheDocument();
  });
});
