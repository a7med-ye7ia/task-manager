import React from 'react';
import { Button } from '../../common/Button';
import { TaskFileDetail } from '../../../types';
import { Download } from 'lucide-react';
import { formatSize, getFileBadgeLabel } from '../fileUtils';

export interface NoViewerFallbackProps {
  file: TaskFileDetail;
  onDownload: () => void;
}

export const NoViewerFallback: React.FC<NoViewerFallbackProps> = ({ file, onDownload }) => {
  const badge = getFileBadgeLabel(file);

  return (
    <div className="file-no-viewer-fallback">
      <div className="no-viewer-card">
        <span className="no-viewer-badge">{badge}</span>
        <h3 className="no-viewer-title" dir="auto">{file.originalName}</h3>
        <p className="no-viewer-meta muted">
          {formatSize(file.sizeBytes)} · Uploaded by {file.uploaderName}
        </p>
        <p className="no-viewer-info">
          In-browser preview is not available for this file type ({badge}). Please download the file to open it with your desktop application.
        </p>
        <Button
          type="button"
          variant="primary"
          icon={<Download size={16} />}
          onClick={onDownload}
        >
          Download file
        </Button>
      </div>
    </div>
  );
};
