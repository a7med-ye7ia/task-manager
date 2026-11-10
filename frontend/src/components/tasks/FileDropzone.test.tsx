import { createRef } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { FileDropzone, FileDropzoneHandle } from './FileDropzone';
import { uploadTaskFilesApi } from '../../services/api';

vi.mock('../../services/api', async () => ({
  ...(await vi.importActual<typeof import('../../services/api')>('../../services/api')),
  uploadTaskFilesApi: vi.fn(),
}));

describe('FileDropzone', () => {
  it('opens by keyboard and accepts a drop', () => {
    const onFiles = vi.fn();
    render(<FileDropzone onFiles={onFiles} />);
    const zone = screen.getByRole('button');
    fireEvent.keyDown(zone, { key: 'Enter' });
    fireEvent.drop(zone, { dataTransfer: { files: [new File(['x'], 'a.txt')] } });
    expect(onFiles).toHaveBeenCalled();
  });

  it('renders upload and error state inside the area', () => {
    render(<FileDropzone onFiles={() => {}} state="uploading" progress={40} fileName="Uploading 2 of 3" />);
    expect(screen.getByText('Uploading 2 of 3')).toBeInTheDocument();
  });

  it('binds the background fill to the real progress value', () => {
    render(<FileDropzone onFiles={() => {}} state="uploading" progress={40} fileName="Uploading" />);
    expect(document.querySelector('.file-dropzone')).toHaveStyle('--fill: 40%');
  });

  it('shows one green outlined success document with a check', () => {
    render(<FileDropzone onFiles={() => {}} state="success" progress={100} />);
    expect(document.querySelector('.file-dropzone')).toHaveClass('is-success');
    expect(document.querySelector('.file-success-check')).toBeInTheDocument();
    expect(document.querySelectorAll('.file-fill-icon svg')).toHaveLength(1);
  });

  it('keeps the failure message and retry inside the rectangle with no outside error row', () => {
    render(<FileDropzone onFiles={() => {}} state="error" fileName="bad.exe" error="File content does not match an allowed file type" onRetry={() => {}} />);
    const zone = document.querySelector('.file-dropzone');
    expect(zone).toContainElement(screen.getByText('File content does not match an allowed file type'));
    expect(zone).toContainElement(screen.getByRole('button', { name: 'Retry' }));
    expect(document.querySelectorAll('.task-file-upload-row')).toHaveLength(0);
  });

  it('keeps the failure stack inside the dropzone with one shredder illustration', () => {
    render(<FileDropzone onFiles={() => {}} state="error" fileName="bad.exe" error="Rejected file" onRetry={() => {}} />);
    const zone = document.querySelector('.file-dropzone')!;
    expect(zone).toContainElement(screen.getByText('Upload failed'));
    expect(zone).toContainElement(screen.getByText('Rejected file'));
    expect(screen.getByTestId('file-shredder-icon')).toBeInTheDocument();
    expect(document.querySelector('.file-fill-error-badge')).toBeNull();
  });

  it('pauses and resumes from the zone without opening the picker', () => {
    const onPause = vi.fn(), onResume = vi.fn(), onFiles = vi.fn();
    const { rerender } = render(<FileDropzone onFiles={onFiles} state="uploading" progress={45} onPause={onPause} onCancel={() => {}} />);
    fireEvent.click(screen.getByRole('button'));
    expect(onPause).toHaveBeenCalled();
    expect(onFiles).not.toHaveBeenCalled();
    rerender(<FileDropzone onFiles={onFiles} state="paused" progress={45} onResume={onResume} onCancel={() => {}} />);
    fireEvent.click(screen.getByRole('button'));
    expect(onResume).toHaveBeenCalled();
  });

  it('stores files in a pending list when unsaved and uploads after save with progress UI', async () => {
    vi.mocked(uploadTaskFilesApi).mockImplementation(async (_taskId, files, _subtaskId, onProgress) => {
      onProgress?.(files[0], 100);
      return [{ id: 101, taskId: 5, subtaskId: 12, originalName: files[0].name, contentType: 'image/png', sizeBytes: files[0].size, uploaderId: 1, uploaderName: 'User', createdAt: '2026-11-09' }];
    });

    const ref = createRef<FileDropzoneHandle>();
    const onUploaded = vi.fn();
    render(<FileDropzone ref={ref} target="pending" label="Drop subtask files or click to choose" onUploaded={onUploaded} />);

    // Add a file to pending list
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const testFile = new File(['pngdata'], 'subtask-spec.png', { type: 'image/png' });
    fireEvent.change(input, { target: { files: [testFile] } });

    // File appears in pending list inside dropzone
    expect(await screen.findByText('subtask-spec.png')).toBeInTheDocument();
    expect(ref.current?.hasFiles()).toBe(true);

    // Call uploadPending after parent saves
    await ref.current?.uploadPending(5, 12);
    expect(uploadTaskFilesApi).toHaveBeenCalledWith(5, [testFile], 12, expect.any(Function), expect.any(Function));
    expect(onUploaded).toHaveBeenCalled();
  });

  it('one dropzone component is used in all three places', () => {
    // Shared dropzone component can render in direct upload mode (Files section),
    // pending file collection mode (Add task modal), and subtask draft attachment mode.
    const { rerender } = render(
      <FileDropzone target={{ taskId: 1 }} label="Drop files here or click to browse" />
    );
    expect(screen.getByText('Drop files here or click to browse')).toBeInTheDocument();

    rerender(
      <FileDropzone target="pending" label="Add task attachments" />
    );
    expect(screen.getByText('Add task attachments')).toBeInTheDocument();

    rerender(
      <FileDropzone target={{ taskId: 1, subtaskId: 42 }} label="Attach files to subtask" />
    );
    expect(screen.getByText('Attach files to subtask')).toBeInTheDocument();
  });

  it('renders with test id and pending files list has scroll container class', () => {
    const testFile = new File(['data'], 'test-spec.png', { type: 'image/png' });
    render(
      <FileDropzone
        target="pending"
        pendingFiles={[testFile]}
        label="Drop subtask files or click to choose"
      />
    );

    // Component renders with test id
    const dropzone = screen.getByTestId('file-dropzone');
    expect(dropzone).toBeInTheDocument();

    // Pending file list has the scroll container class
    const scrollContainer = dropzone.querySelector('.file-list-scrollable');
    expect(scrollContainer).toBeInTheDocument();
    expect(scrollContainer).toHaveClass('file-list-scrollable');
  });

  it('in buttonMode, renders single Attach files button and clicking it triggers the hidden file input click', () => {
    render(
      <FileDropzone
        buttonMode
        target="pending"
        pendingFiles={[]}
      />
    );

    // Dropzone area is NOT rendered
    expect(screen.queryByTestId('file-dropzone-box')).not.toBeInTheDocument();

    // Attach files button is rendered
    const attachButton = screen.getByTestId('attach-files-button');
    expect(attachButton).toBeInTheDocument();
    expect(attachButton).toHaveTextContent('Attach files');

    // Hidden input exists and clicking button triggers click on input
    const hiddenInput = screen.getByTestId('file-dropzone-input');
    expect(hiddenInput).toBeInTheDocument();
    const inputClickSpy = vi.spyOn(hiddenInput, 'click');

    fireEvent.click(attachButton);
    expect(inputClickSpy).toHaveBeenCalledTimes(1);
  });
});
