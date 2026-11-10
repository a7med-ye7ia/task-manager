import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { TaskFilePage, fileViewerKind } from './TaskFilePage';
import { getTaskFileDetailApi, getTaskFileTextApi } from '../services/api';
import { resolveViewerType } from '../components/tasks/fileUtils';

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ user: { id: 2, role: 'EMPLOYEE' } }),
}));

vi.mock('../services/api', () => ({
  getTaskFileDetailApi: vi.fn().mockResolvedValue({
    id: 4,
    taskId: 10,
    originalName: 'readme.md',
    contentType: 'text/markdown',
    sizeBytes: 2,
    uploaderId: 2,
    uploaderName: 'A',
    createdAt: '2026-01-01',
    taskTitle: 'Task',
  }),
  getTaskFileTokenApi: vi.fn().mockResolvedValue('token'),
  getTaskFileTextApi: vi.fn().mockResolvedValue('# Hello'),
  getTaskFileZipEntriesApi: vi.fn(),
  getTaskFileContentUrl: vi.fn().mockReturnValue('/content'),
  deleteTaskFileApi: vi.fn(),
  downloadTaskFileApi: vi.fn().mockResolvedValue(new Blob(['test'], { type: 'text/plain' })),
  editTaskFileContentApi: vi.fn(),
}));

describe('TaskFilePage', () => {
  it('registry selects each supported viewer and download fallback', () => {
    expect(fileViewerKind('image/png')).toBe('image');
    expect(fileViewerKind('application/pdf')).toBe('pdf');
    expect(fileViewerKind('text/csv')).toBe('csv');
    expect(fileViewerKind('text/x-tex')).toBe('latex');
    expect(fileViewerKind('application/zip')).toBe('zip');
    expect(fileViewerKind('application/vnd.openxmlformats-officedocument.wordprocessingml.document')).toBe('docx');
    expect(fileViewerKind('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')).toBe('xlsx');
    expect(fileViewerKind('application/vnd.openxmlformats-officedocument.presentationml.presentation')).toBe('download');
  });

  it('viewer chosen by type', () => {
    expect(resolveViewerType({ originalName: 'pic.jpg', contentType: 'image/jpeg' })).toBe('image');
    expect(resolveViewerType({ originalName: 'manual.pdf', contentType: 'application/pdf' })).toBe('pdf');
    expect(resolveViewerType({ originalName: 'report.docx', contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })).toBe('docx');
    expect(resolveViewerType({ originalName: 'data.csv', contentType: 'text/csv' })).toBe('csv');
    expect(resolveViewerType({ originalName: 'app.log', contentType: 'text/plain' })).toBe('txt-log');
    expect(resolveViewerType({ originalName: 'readme.md', contentType: 'text/markdown' })).toBe('markdown');
    expect(resolveViewerType({ originalName: 'app.json', contentType: 'application/json' })).toBe('code');
    expect(resolveViewerType({ originalName: 'Main.java', contentType: 'text/plain' })).toBe('code');
    expect(resolveViewerType({ originalName: 'script.py', contentType: 'text/plain' })).toBe('code');
    expect(resolveViewerType({ originalName: 'archive.zip', contentType: 'application/zip' })).toBe('no-viewer');
    expect(resolveViewerType({ originalName: 'sheet.xlsx', contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })).toBe('no-viewer');
  });

  it('loads the shared text endpoint for code-like text extensions', async () => {
    vi.mocked(getTaskFileDetailApi).mockResolvedValueOnce({
      id: 6,
      taskId: 10,
      originalName: 'service.java',
      contentType: 'text/plain',
      sizeBytes: 12,
      uploaderId: 2,
      uploaderName: 'A',
      createdAt: '2026-01-01',
      taskTitle: 'Task',
    });
    vi.mocked(getTaskFileTextApi).mockResolvedValueOnce('class Service {}');
    render(
      <MemoryRouter initialEntries={['/tasks/10/files/6']}>
        <Routes>
          <Route path="/tasks/:taskId/files/:fileId" element={<TaskFilePage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText(/class Service/)).toBeInTheDocument());
  });

  it('CSV rendered as a table', async () => {
    vi.mocked(getTaskFileDetailApi).mockResolvedValueOnce({
      id: 7,
      taskId: 10,
      originalName: 'people.csv',
      contentType: 'text/plain',
      sizeBytes: 20,
      uploaderId: 2,
      uploaderName: 'A',
      createdAt: '2026-01-01',
      taskTitle: 'Task',
    });
    vi.mocked(getTaskFileTextApi).mockResolvedValueOnce('name,city\nأحمد,"New York, NY"');
    render(
      <MemoryRouter initialEntries={['/tasks/10/files/7']}>
        <Routes>
          <Route path="/tasks/:taskId/files/:fileId" element={<TaskFilePage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText('people.csv')).toBeInTheDocument());
    // Table is rendered with sticky header, row number #, and parsed cell
    await waitFor(() => {
      expect(screen.getByText('New York, NY')).toBeInTheDocument();
      expect(screen.getByText('name')).toBeInTheDocument();
      expect(document.querySelector('table.csv-real-table')).toBeInTheDocument();
    });
  });

  it('markdown does not render raw HTML', async () => {
    vi.mocked(getTaskFileDetailApi).mockResolvedValueOnce({
      id: 8,
      taskId: 10,
      originalName: 'readme.md',
      contentType: 'text/plain',
      sizeBytes: 20,
      uploaderId: 2,
      uploaderName: 'A',
      createdAt: '2026-01-01',
      taskTitle: 'Task',
    });
    vi.mocked(getTaskFileTextApi).mockResolvedValueOnce('# Title\n<script>alert(1)</script>');
    render(
      <MemoryRouter initialEntries={['/tasks/10/files/8']}>
        <Routes>
          <Route path="/tasks/:taskId/files/:fileId" element={<TaskFilePage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText('Title')).toBeInTheDocument());
    expect(document.querySelector('script')).toBeNull();
  });

  it('selects text viewer and shows navigation shell', async () => {
    render(
      <MemoryRouter initialEntries={['/tasks/10/files/4']}>
        <Routes>
          <Route path="/tasks/:taskId/files/:fileId" element={<TaskFilePage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText(/Hello/)).toBeInTheDocument());
    expect(screen.getByText('Back to task')).toBeInTheDocument();
  });

  it('renders LaTeX document preview with code and badge', async () => {
    vi.mocked(getTaskFileDetailApi).mockResolvedValueOnce({
      id: 5,
      taskId: 10,
      originalName: 'paper.tex',
      contentType: 'text/x-tex',
      sizeBytes: 42,
      uploaderId: 2,
      uploaderName: 'Author',
      createdAt: '2026-01-01',
      taskTitle: 'LaTeX Task',
    });
    vi.mocked(getTaskFileTextApi).mockResolvedValueOnce('\\documentclass{article}\n\\begin{document}\nHello LaTeX\n\\end{document}');

    render(
      <MemoryRouter initialEntries={['/tasks/10/files/5']}>
        <Routes>
          <Route path="/tasks/:taskId/files/:fileId" element={<TaskFilePage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('LaTeX Source')).toBeInTheDocument());
    expect(screen.getByText(/Hello LaTeX/)).toBeInTheDocument();
  });

  it('transitions loading to loaded and back to an error without a hooks mismatch', async () => {
    const originalError = console.error;
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.mocked(getTaskFileDetailApi).mockResolvedValueOnce({
      id: 4,
      taskId: 10,
      originalName: 'readme.md',
      contentType: 'text/markdown',
      sizeBytes: 2,
      uploaderId: 2,
      uploaderName: 'A',
      createdAt: '2026-01-01',
      taskTitle: 'Task',
    });
    render(
      <MemoryRouter initialEntries={['/tasks/10/files/4']}>
        <Routes>
          <Route path="/tasks/:taskId/files/:fileId" element={<TaskFilePage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText('readme.md')).toBeInTheDocument());
    vi.mocked(getTaskFileDetailApi).mockRejectedValueOnce(new Error('temporary failure'));
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => expect(screen.getByText('temporary failure')).toBeInTheDocument());
    expect(consoleSpy).not.toHaveBeenCalledWith(expect.stringContaining('Rendered more hooks'));
    consoleSpy.mockRestore();
    void originalError;
  });

  it('md opens the code view in Source', async () => {
    vi.mocked(getTaskFileDetailApi).mockResolvedValueOnce({
      id: 9,
      taskId: 10,
      originalName: 'guide.md',
      contentType: 'text/markdown',
      sizeBytes: 80,
      uploaderId: 2,
      uploaderName: 'DocAuthor',
      createdAt: '2026-01-01',
      taskTitle: 'Task Docs',
    });
    vi.mocked(getTaskFileTextApi).mockResolvedValueOnce('# Guide\nThis is a guide.\n```ts\nconst x = 1;\n```');

    render(
      <MemoryRouter initialEntries={['/tasks/10/files/9']}>
        <Routes>
          <Route path="/tasks/:taskId/files/:fileId" element={<TaskFilePage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('guide.md')).toBeInTheDocument());

    // Switch to Source tab
    const sourceBtn = screen.getByRole('button', { name: 'Markdown source' });
    fireEvent.click(sourceBtn);

    // Assert that the IDE-style code view is displayed with language badge "Markdown" and line numbers
    await waitFor(() => {
      expect(screen.getByText('Markdown')).toBeInTheDocument();
      expect(document.querySelector('.file-ide-viewer')).toBeInTheDocument();
      expect(document.querySelector('.ide-code-gutter')).toBeInTheDocument();
    });
  });
});
