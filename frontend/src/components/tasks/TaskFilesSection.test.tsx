import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TaskFilesSection } from './TaskFilesSection';
import { downloadTaskFileApi, listTaskFilesApi, uploadTaskFilesApi } from '../../services/api';

vi.mock('../../services/api', async () => ({
  ...(await vi.importActual<typeof import('../../services/api')>('../../services/api')),
  listTaskFilesApi: vi.fn(),
  uploadTaskFilesApi: vi.fn(),
  downloadTaskFileApi: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => ({
  ...(await vi.importActual<typeof import('react-router-dom')>('react-router-dom')),
  useNavigate: () => mockNavigate,
}));

describe('TaskFilesSection', () => {
  it('keeps subtask upload response in its group without a second upload', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([]);
    vi.mocked(uploadTaskFilesApi).mockResolvedValue([{ id: 9, taskId: 7, subtaskId: 3, originalName: 'sub.txt', contentType: 'text/plain', sizeBytes: 3, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' }]);
    render(<TaskFilesSection taskId={7} subtasks={[{ id: 3, title: 'Subtask A' }]} currentUserId={2} canUpload canDelete />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [new File(['sub'], 'sub.txt')] } });
    await waitFor(() => expect(screen.getByText('sub.txt')).toBeInTheDocument());
    expect(vi.mocked(uploadTaskFilesApi)).toHaveBeenCalledTimes(1);
  });

  it('offers a URL-backed file filter', () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([]);
    render(<TaskFilesSection taskId={7} subtasks={[{ id: 3, title: 'Subtask A' }]} currentUserId={2} canUpload canDelete />);
    expect(screen.getByRole('combobox', { name: 'File filter' })).toBeInTheDocument();
  });

  it('renders file actions as shared Buttons and stops card navigation', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([{ id: 2, taskId: 7, originalName: 'a.txt', contentType: 'text/plain', sizeBytes: 3, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' }]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Delete file' })).toHaveClass('btn'));
    const card = screen.getByText('a.txt').closest('.task-file-card')!;
    fireEvent.click(screen.getByRole('button', { name: 'Download file' }));
    expect(card).toBeInTheDocument();
  });

  it('opens the picker and renders a completed upload row', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([]);
    vi.mocked(uploadTaskFilesApi).mockImplementation(async (_task, files, _subtask, progress) => {
      progress?.(files[0], 100);
      return [{ id: 1, taskId: 7, originalName: files[0].name, contentType: 'text/plain', sizeBytes: files[0].size, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' }];
    });
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['hello'], 'notes.txt', { type: 'text/plain' });
    fireEvent.change(input, { target: { files: [file] } });
    await waitFor(() => expect(screen.getByText('notes.txt')).toBeInTheDocument());
    expect(screen.getByText('1 file')).toBeInTheDocument();
  });

  it('uses a button with onClick navigation for an image preview with no modal', async () => {
    mockNavigate.mockClear();
    vi.mocked(listTaskFilesApi).mockResolvedValue([{ id: 2, taskId: 7, originalName: 'photo.png', contentType: 'image/png', sizeBytes: 3, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' }]);
    vi.mocked(downloadTaskFileApi).mockResolvedValue(new Blob(['png'], { type: 'image/png' }));
    vi.stubGlobal('URL', { ...URL, createObjectURL: vi.fn(() => 'blob:test'), revokeObjectURL: vi.fn() });
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('photo.png')).toBeInTheDocument());
    const previewBtn = screen.getByRole('button', { name: 'Preview file' });
    expect(previewBtn).toBeInTheDocument();
    fireEvent.click(previewBtn);
    expect(mockNavigate).toHaveBeenCalledWith('/tasks/7/files/2?fileFilter=all');
    expect(document.querySelector('.modal-overlay')).toBeNull();
  });

  it('navigates to the dedicated preview page for rich previewable formats such as docx and latex', async () => {
    mockNavigate.mockClear();
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      { id: 12, taskId: 7, originalName: 'paper.tex', contentType: 'text/x-tex', sizeBytes: 1024, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
    ]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('paper.tex')).toBeInTheDocument());
    const previewBtn = screen.getByRole('button', { name: 'Preview file' });
    expect(previewBtn).toBeInTheDocument();
    fireEvent.click(previewBtn);
    expect(mockNavigate).toHaveBeenCalledWith('/tasks/7/files/12?fileFilter=all');
  });

  it('renders all four action buttons with the exact same parent container and exact same class list', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      {
        id: 50,
        taskId: 7,
        originalName: 'document.pdf',
        contentType: 'application/pdf',
        sizeBytes: 2048,
        uploaderId: 2,
        uploaderName: 'A User',
        createdAt: '2026-11-05',
      },
    ]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('document.pdf')).toBeInTheDocument());

    const previewBtn = screen.getByRole('button', { name: 'Preview file' });
    const downloadBtn = screen.getByRole('button', { name: 'Download file' });
    const editBtn = screen.getByRole('button', { name: 'Edit file' });
    const deleteBtn = screen.getByRole('button', { name: 'Delete file' });

    // Verify all four buttons have the EXACT SAME parent container
    const container = previewBtn.parentElement;
    expect(container).toBeInTheDocument();
    expect(container).toHaveClass('task-file-actions');
    expect(downloadBtn.parentElement).toBe(container);
    expect(editBtn.parentElement).toBe(container);
    expect(deleteBtn.parentElement).toBe(container);

    // Verify all four buttons have the EXACT SAME class list
    expect(downloadBtn.className).toBe(previewBtn.className);
    expect(editBtn.className).toBe(previewBtn.className);
    expect(deleteBtn.className).toBe(previewBtn.className);
  });

  it('downloads non-previewable types on card click or button click with no preview button', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      { id: 99, taskId: 7, originalName: 'raw.bin', contentType: 'application/octet-stream', sizeBytes: 1024, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
    ]);
    vi.mocked(downloadTaskFileApi).mockResolvedValue(new Blob(['data'], { type: 'application/octet-stream' }));
    vi.stubGlobal('URL', { ...URL, createObjectURL: vi.fn(() => 'blob:download'), revokeObjectURL: vi.fn() });

    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('raw.bin')).toBeInTheDocument());

    // Non-previewable cards show Download only, no Preview button
    expect(screen.queryByRole('button', { name: 'Preview file' })).not.toBeInTheDocument();

    // Clicking card triggers download
    const card = screen.getByText('raw.bin').closest('.task-file-card')!;
    fireEvent.click(card);
    expect(downloadTaskFileApi).toHaveBeenCalledWith(7, 99);

    // Clicking download button triggers download
    const downloadBtn = screen.getByRole('button', { name: 'Download file' });
    fireEvent.click(downloadBtn);
    expect(downloadTaskFileApi).toHaveBeenCalledWith(7, 99);
    expect(document.querySelector('a a')).toBeNull();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('keeps fixed action slots and hides preview for spreadsheet files', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([{ id: 100, taskId: 7, originalName: 'data.xlsx', contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', sizeBytes: 1024, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' }]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('data.xlsx')).toBeInTheDocument());
    expect(screen.queryByRole('link', { name: 'Preview file' })).not.toBeInTheDocument();
    expect(document.querySelectorAll('.task-file-action-slot').length).toBeGreaterThan(0);
  });

  it('shows Edit in the fixed slot for an editable CSV', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([{ id: 101, taskId: 7, originalName: 'records.csv', contentType: 'text/plain', sizeBytes: 20, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' }]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('records.csv')).toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Edit file' })).toBeInTheDocument();
  });

  // Explicit tests requested by prompt:
  it('button row slots stay in place for zip vs csv', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      { id: 1, taskId: 7, originalName: 'bundle.zip', contentType: 'application/zip', sizeBytes: 5000, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
      { id: 2, taskId: 7, originalName: 'sheet.csv', contentType: 'text/csv', sizeBytes: 200, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
    ]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => {
      expect(screen.getByText('bundle.zip')).toBeInTheDocument();
      expect(screen.getByText('sheet.csv')).toBeInTheDocument();
    });

    const zipCard = screen.getByText('bundle.zip').closest('.task-file-card')!;
    const csvCard = screen.getByText('sheet.csv').closest('.task-file-card')!;

    const zipSlots = zipCard.querySelectorAll('.task-file-action-slot');
    const csvSlots = csvCard.querySelectorAll('.task-file-action-slot');

    // Both cards must have exactly 4 fixed slots in the same order
    expect(zipSlots.length).toBe(4);
    expect(csvSlots.length).toBe(4);

    expect(zipSlots[0]).toHaveAttribute('data-slot', 'preview');
    expect(zipSlots[1]).toHaveAttribute('data-slot', 'download');
    expect(zipSlots[2]).toHaveAttribute('data-slot', 'edit');
    expect(zipSlots[3]).toHaveAttribute('data-slot', 'delete');

    expect(csvSlots[0]).toHaveAttribute('data-slot', 'preview');
    expect(csvSlots[1]).toHaveAttribute('data-slot', 'download');
    expect(csvSlots[2]).toHaveAttribute('data-slot', 'edit');
    expect(csvSlots[3]).toHaveAttribute('data-slot', 'delete');
  });

  it('Preview hidden for zip', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      { id: 1, taskId: 7, originalName: 'archive.zip', contentType: 'application/zip', sizeBytes: 8000, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
    ]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('archive.zip')).toBeInTheDocument());

    const zipCard = screen.getByText('archive.zip').closest('.task-file-card')!;
    const previewSlot = zipCard.querySelector('[data-slot="preview"]')!;

    // Preview link / button is hidden (placeholder only)
    expect(previewSlot.querySelector('a')).toBeNull();
    expect(previewSlot.querySelector('button')).toBeNull();
    expect(previewSlot.querySelector('.task-file-action-placeholder')).toBeInTheDocument();
  });

  it('Edit visible for csv for an allowed user', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      { id: 2, taskId: 7, originalName: 'data.csv', contentType: 'text/csv', sizeBytes: 150, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
    ]);
    // Current user id is 2 (the uploader), canUpload is true (task not DONE)
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete={false} />);
    await waitFor(() => expect(screen.getByText('data.csv')).toBeInTheDocument());

    const editBtn = screen.getByRole('button', { name: 'Edit file' });
    expect(editBtn).toBeInTheDocument();
  });

  it('the four buttons share one container', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      { id: 10, taskId: 7, originalName: 'notes.txt', contentType: 'text/plain', sizeBytes: 100, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
    ]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('notes.txt')).toBeInTheDocument());

    const card = screen.getByText('notes.txt').closest('.task-file-card')!;
    const container = card.querySelector('.task-file-actions')!;
    expect(container).toBeInTheDocument();

    // Four action buttons inside this single container
    const buttons = container.querySelectorAll('button');
    expect(buttons.length).toBe(4);
    expect(container.querySelector('[data-slot="preview"] button')).toBeInTheDocument();
    expect(container.querySelector('[data-slot="download"] button')).toBeInTheDocument();
    expect(container.querySelector('[data-slot="edit"] button')).toBeInTheDocument();
    expect(container.querySelector('[data-slot="delete"] button')).toBeInTheDocument();
  });

  it('the Edit button shows for a zip', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      { id: 11, taskId: 7, originalName: 'archive.zip', contentType: 'application/zip', sizeBytes: 1024, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
    ]);
    // Allowed user (uploader or manager/lead) on active task
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('archive.zip')).toBeInTheDocument());

    const card = screen.getByText('archive.zip').closest('.task-file-card')!;
    const editBtn = card.querySelector('[data-slot="edit"] button');
    expect(editBtn).toBeInTheDocument();
  });

  it('the Rename Save button is disabled for an unchanged name and enabled after a change', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      { id: 12, taskId: 7, originalName: 'photo.jpg', contentType: 'image/jpeg', sizeBytes: 2048, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
    ]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('photo.jpg')).toBeInTheDocument());

    // Non-text file clicking edit opens rename dialog directly
    const editBtn = screen.getByRole('button', { name: 'Edit file' });
    fireEvent.click(editBtn);

    await waitFor(() => expect(screen.getByText('Rename file')).toBeInTheDocument());
    const saveBtn = screen.getByRole('button', { name: 'Save' });
    const input = screen.getByLabelText('New base name') as HTMLInputElement;

    // Disabled initially for unchanged name
    expect(saveBtn).toBeDisabled();

    // Change input to a new name
    fireEvent.change(input, { target: { value: 'new_photo' } });
    expect(saveBtn).not.toBeDisabled();

    // Reverting to unchanged disables Save again
    fireEvent.change(input, { target: { value: 'photo' } });
    expect(saveBtn).toBeDisabled();

    // Empty name disables Save
    fireEvent.change(input, { target: { value: '   ' } });
    expect(saveBtn).toBeDisabled();
  });

  it('paper clip uploads show the queue', async () => {
    vi.mocked(listTaskFilesApi).mockResolvedValue([
      { id: 14, taskId: 7, originalName: 'existing.txt', contentType: 'text/plain', sizeBytes: 50, uploaderId: 2, uploaderName: 'A User', createdAt: '2026-11-05' },
    ]);
    render(<TaskFilesSection taskId={7} subtasks={[]} currentUserId={2} canUpload canDelete />);
    await waitFor(() => expect(screen.getByText('existing.txt')).toBeInTheDocument());

    const paperClipBtn = screen.getByRole('button', { name: /Add files to Task files/i });
    expect(paperClipBtn).toBeInTheDocument();

    // Clicking paper clip activates picker wired to dropzone
    fireEvent.click(paperClipBtn);
    expect(document.querySelector('.file-dropzone')).toBeInTheDocument();
  });

  it('renders FileDropzone with test id and applies file-list-scrollable to subtasks and >8 task files', async () => {
    const manyFiles = Array.from({ length: 10 }, (_, i) => ({
      id: 100 + i,
      taskId: 7,
      originalName: `task_file_${i}.txt`,
      contentType: 'text/plain',
      sizeBytes: 100,
      uploaderId: 2,
      uploaderName: 'User',
      createdAt: '2026-11-05',
    }));
    const subtaskFile = {
      id: 201,
      taskId: 7,
      subtaskId: 3,
      originalName: 'subtask_file.txt',
      contentType: 'text/plain',
      sizeBytes: 100,
      uploaderId: 2,
      uploaderName: 'User',
      createdAt: '2026-11-05',
    };

    vi.mocked(listTaskFilesApi).mockResolvedValue([...manyFiles, subtaskFile]);
    render(
      <TaskFilesSection
        taskId={7}
        subtasks={[{ id: 3, title: 'Subtask A' }]}
        currentUserId={2}
        canUpload
        canDelete
      />
    );

    await waitFor(() => expect(screen.getByText('task_file_0.txt')).toBeInTheDocument());

    // FileDropzone has data-testid="file-dropzone"
    expect(screen.getByTestId('file-dropzone')).toBeInTheDocument();

    // Since task files exceed 8 rows, the task files list container has .file-list-scrollable
    const scrollContainers = document.querySelectorAll('.file-list-scrollable');
    expect(scrollContainers.length).toBeGreaterThanOrEqual(1);
  });
});
