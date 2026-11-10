import React, { useEffect, useRef, useState } from 'react';
import { Button } from '../../common/Button';
import { TaskFileDetail } from '../../../types';
import { Download, AlertCircle } from 'lucide-react';
import { downloadTaskFileApi } from '../../../services/api';
import { Spinner } from '../../common/LoadingStates';

export interface DocxViewerProps {
  file: TaskFileDetail;
  taskId: number;
  fileId: number;
  onDownload: () => void;
}

export const DocxViewer: React.FC<DocxViewerProps> = ({ file: _file, taskId, fileId, onDownload }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [renderError, setRenderError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function loadDocx() {
      setLoading(true);
      setRenderError(null);

      try {
        // 1. Authenticated fetch returning Blob
        const blob = await downloadTaskFileApi(taskId, fileId);
        // 2. Convert to ArrayBuffer
        const arrayBuffer = await blob.arrayBuffer();

        if (!active || !containerRef.current) return;

        // 3. Dynamic import of docx-preview
        const docxModule: any = await import(
          /* @vite-ignore */ 'https://cdn.jsdelivr.net/npm/docx-preview@0.3.3/+esm'
        );
        const renderAsync = docxModule.renderAsync || docxModule.default?.renderAsync;

        if (!renderAsync) {
          throw new Error('docx-preview renderAsync function not found');
        }

        // Clear previous content
        containerRef.current.innerHTML = '';

        // 4. Render into DOM container with page-like layout
        await renderAsync(arrayBuffer, containerRef.current, undefined, {
          inWrapper: true,
          ignoreWidth: false,
          ignoreHeight: false,
          experimental: true,
          trimXmlDeclaration: true,
        });

        if (active) setLoading(false);
      } catch (err) {
        if (active) {
          setLoading(false);
          setRenderError(
            err instanceof Error
              ? `Unable to preview DOCX: ${err.message}`
              : 'DOCX preview could not be rendered in this browser.'
          );
        }
      }
    }

    void loadDocx();

    return () => {
      active = false;
    };
  }, [taskId, fileId]);

  return (
    <div className="file-docx-viewer">
      {loading && (
        <div className="docx-loading-state">
          <Spinner size={24} />
          <p className="muted">Rendering DOCX document...</p>
        </div>
      )}

      {renderError && (
        <div className="docx-error-state">
          <div className="docx-error-message">
            <AlertCircle size={20} className="text-danger" />
            <span>{renderError}</span>
          </div>
          <p className="muted">Download the file to open it in Microsoft Word or LibreOffice.</p>
          <Button
            type="button"
            variant="primary"
            icon={<Download size={16} />}
            onClick={onDownload}
          >
            Download DOCX file
          </Button>
        </div>
      )}

      <div
        ref={containerRef}
        className="docx-page-container"
        style={{ display: loading || renderError ? 'none' : 'block' }}
      />
    </div>
  );
};
