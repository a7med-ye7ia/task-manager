import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { TaskDetailPage } from './TaskDetailPage';
import { getTaskApi, getTaskUnreadApi, getProfileApi } from '../services/api';

vi.mock('../services/api', async () => ({
  ...(await vi.importActual<typeof import('../services/api')>('../services/api')),
  getTaskApi: vi.fn(),
  getAdminUsersApi: vi.fn(),
  getTaskUnreadApi: vi.fn(),
  listTaskFilesApi: vi.fn().mockResolvedValue([]),
  getProfileApi: vi.fn().mockResolvedValue({ id: 1, role: 'MANAGER', firstName: 'Admin', lastName: 'User' }),
}));

describe('TaskDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getProfileApi).mockResolvedValue({ id: 1, role: 'MANAGER', firstName: 'Admin', lastName: 'User' } as any);
    vi.mocked(getTaskUnreadApi).mockResolvedValue({
      chatUnread: false,
      activityUnread: false,
      chatCount: 0,
      activityCount: 0,
    });
  });

  it('renders only one skeleton loader while loading and no small loading span', () => {
    // Keep getTaskApi pending
    vi.mocked(getTaskApi).mockImplementation(() => new Promise(() => {}));

    render(
      <MemoryRouter initialEntries={['/tasks/42']}>
        <Routes>
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    // Assert that the skeleton loader is present
    const skeleton = screen.getByRole('status', { name: 'Loading task' });
    expect(skeleton).toBeInTheDocument();
    expect(skeleton).toHaveClass('task-detail-skeleton');

    // Assert no second loader or small loading text/span exists
    expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    expect(screen.queryByText('Loading task')).not.toBeInTheDocument();
    expect(document.querySelectorAll('[role="status"]')).toHaveLength(1);
  });

  it('renders the task page with shared subtask dropzone once loaded', async () => {
    vi.mocked(getTaskApi).mockResolvedValue({
      id: 42,
      title: 'Implementation Task',
      description: 'Task description',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      deadline: '2026-12-01',
      createdBy: 1,
      createdAt: '2026-11-01T10:00:00Z',
      updatedAt: '2026-11-01T10:00:00Z',
      progress: 0,
      lead: { id: 1, firstName: 'Admin', lastName: 'User', role: 'MANAGER' },
      members: [],
      subtasks: [],
      checkpoints: [],
      files: [],
    } as any);

    render(
      <MemoryRouter initialEntries={['/tasks/42']}>
        <Routes>
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Implementation Task')).toBeInTheDocument();
    });

    // Subtasks card is present
    expect(screen.getByText('Subtasks')).toBeInTheDocument();
    // Files section with dropzone is present
    expect(screen.getByText('Files')).toBeInTheDocument();
  });

  it('renders the same FileDropzone component in Add subtask and FileSection with same test id', async () => {
    vi.mocked(getTaskApi).mockResolvedValue({
      id: 42,
      title: 'Implementation Task',
      description: 'Task description',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      deadline: '2026-12-01',
      createdBy: 1,
      createdAt: '2026-11-01T10:00:00Z',
      updatedAt: '2026-11-01T10:00:00Z',
      progress: 0,
      lead: { id: 1, firstName: 'Admin', lastName: 'User', role: 'MANAGER' },
      members: [],
      subtasks: [],
      checkpoints: [],
      files: [],
    } as any);

    render(
      <MemoryRouter initialEntries={['/tasks/42']}>
        <Routes>
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Implementation Task')).toBeInTheDocument();
    });

    // Open Subtasks card accordion to show Add subtask form
    const subtaskHeaderBtn = screen.getByRole('button', { name: 'Show subtasks' });
    fireEvent.click(subtaskHeaderBtn);

    // Both Add subtask and FileSection render FileDropzone with data-testid="file-dropzone"
    const dropzones = await screen.findAllByTestId('file-dropzone');
    expect(dropzones.length).toBeGreaterThanOrEqual(1);

    // Add subtask label is present inside one of the shared dropzone instances
    expect(screen.getByText('Drop subtask files or click to choose')).toBeInTheDocument();
  });

  it('renders Self-reported badge on self-reported tasks', async () => {
    vi.mocked(getTaskApi).mockResolvedValue({
      id: 50,
      title: 'Employee Task',
      description: 'Desc',
      status: 'TODO',
      priority: 'MEDIUM',
      deadline: '2026-12-05',
      selfReported: true,
      createdBy: { id: 5, firstName: 'Employee', lastName: 'User', role: 'EMPLOYEE' },
      createdAt: '2026-11-01T10:00:00Z',
      updatedAt: '2026-11-01T10:00:00Z',
      progress: 0,
      lead: { id: 5, firstName: 'Employee', lastName: 'User', role: 'EMPLOYEE' },
      members: [{ user: { id: 5, firstName: 'Employee', lastName: 'User', role: 'EMPLOYEE' }, role: 'LEAD' }],
      subtasks: [],
      checkpoints: [],
      files: [],
    } as any);

    render(
      <MemoryRouter initialEntries={['/tasks/50']}>
        <Routes>
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Employee Task')).toBeInTheDocument();
    });

    expect(screen.getByText('Self-reported')).toBeInTheDocument();
  });

  it('hides Done and Send back buttons for employee creator on self-reported task in review', async () => {
    vi.mocked(getProfileApi).mockResolvedValue({ id: 5, role: 'EMPLOYEE', firstName: 'Employee', lastName: 'User' } as any);
    vi.mocked(getTaskApi).mockResolvedValue({
      id: 51,
      title: 'Review Task',
      description: 'Desc',
      status: 'REVIEW',
      priority: 'MEDIUM',
      deadline: '2026-12-05',
      selfReported: true,
      createdBy: { id: 5, firstName: 'Employee', lastName: 'User', role: 'EMPLOYEE' },
      createdAt: '2026-11-01T10:00:00Z',
      updatedAt: '2026-11-01T10:00:00Z',
      progress: 100,
      lead: { id: 5, firstName: 'Employee', lastName: 'User', role: 'EMPLOYEE' },
      members: [{ user: { id: 5, firstName: 'Employee', lastName: 'User', role: 'EMPLOYEE' }, role: 'LEAD' }],
      subtasks: [],
      checkpoints: [],
      files: [],
    } as any);

    render(
      <MemoryRouter initialEntries={['/tasks/51']}>
        <Routes>
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Review Task')).toBeInTheDocument();
    });

    // Mark done and Send back buttons should NOT be present for creator
    expect(screen.queryByRole('button', { name: 'Mark done' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Send back' })).not.toBeInTheDocument();
  });

  it('renders Promote to Lead icon button for non-lead member and triggers promotion', async () => {
    vi.mocked(getProfileApi).mockResolvedValue({ id: 1, role: 'MANAGER', firstName: 'Admin', lastName: 'User' } as any);
    vi.mocked(getTaskApi).mockResolvedValue({
      id: 60,
      title: 'Promote Task',
      description: 'Desc',
      status: 'IN_PROGRESS',
      priority: 'MEDIUM',
      deadline: '2026-12-05',
      createdBy: 1,
      createdAt: '2026-11-01T10:00:00Z',
      updatedAt: '2026-11-01T10:00:00Z',
      progress: 0,
      lead: { id: 1, firstName: 'Admin', lastName: 'User', role: 'MANAGER' },
      members: [
        { user: { id: 1, firstName: 'Admin', lastName: 'User', role: 'MANAGER' }, role: 'LEAD' },
        { user: { id: 2, firstName: 'John', lastName: 'Doe', role: 'EMPLOYEE' }, role: 'MEMBER' },
      ],
      subtasks: [],
      checkpoints: [],
      files: [],
    } as any);

    render(
      <MemoryRouter initialEntries={['/tasks/60']}>
        <Routes>
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Promote Task')).toBeInTheDocument();
    });

    // Expand Members section
    const membersHeaderBtn = screen.getByRole('button', { name: 'Show members' });
    fireEvent.click(membersHeaderBtn);

    // Verify Make Lead text button is gone and promote icon button is present
    expect(screen.queryByRole('button', { name: 'Make Lead' })).not.toBeInTheDocument();
    const promoteBtn = screen.getByRole('button', { name: 'Promote John Doe to Lead' });
    expect(promoteBtn).toBeInTheDocument();
    expect(promoteBtn).toHaveClass('btn-promote-lead');
  });

  it('renders paperclip icon button on subtask card next to delete button', async () => {
    vi.mocked(getProfileApi).mockResolvedValue({ id: 1, role: 'MANAGER', firstName: 'Admin', lastName: 'User' } as any);
    vi.mocked(getTaskApi).mockResolvedValue({
      id: 61,
      title: 'Subtask Attachment Task',
      description: 'Desc',
      status: 'IN_PROGRESS',
      priority: 'MEDIUM',
      deadline: '2026-12-05',
      createdBy: 1,
      createdAt: '2026-11-01T10:00:00Z',
      updatedAt: '2026-11-01T10:00:00Z',
      progress: 50,
      lead: { id: 1, firstName: 'Admin', lastName: 'User', role: 'MANAGER' },
      members: [{ user: { id: 1, firstName: 'Admin', lastName: 'User', role: 'MANAGER' }, role: 'LEAD' }],
      subtasks: [
        {
          id: 101,
          title: 'Design API Schema',
          deadline: '2026-12-02',
          status: 'IN_PROGRESS',
          progress: 50,
          assignees: [],
        },
      ],
      checkpoints: [],
      files: [],
    } as any);

    render(
      <MemoryRouter initialEntries={['/tasks/61']}>
        <Routes>
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Subtask Attachment Task')).toBeInTheDocument();
    });

    // Open Subtasks accordion
    const subtaskHeaderBtn = screen.getByRole('button', { name: 'Show subtasks' });
    fireEvent.click(subtaskHeaderBtn);

    // Verify subtask card renders paperclip button next to delete button
    const clipBtn = screen.getByRole('button', { name: 'Add files to Design API Schema' });
    expect(clipBtn).toBeInTheDocument();
    expect(clipBtn).toHaveAttribute('title', 'Add files to Design API Schema');

    const deleteBtn = screen.getByRole('button', { name: 'Delete subtask' });
    expect(deleteBtn).toBeInTheDocument();
  });
});
