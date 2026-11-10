import React, { useState } from 'react';
import { Button } from '../../common/Button';
import { TaskFileDetail } from '../../../types';
import { WrapText } from 'lucide-react';
import { formatSize } from '../fileUtils';

export interface TextDocViewerProps {
  file: TaskFileDetail;
  textContent: string;
}

export const TextDocViewer: React.FC<TextDocViewerProps> = ({ file, textContent }) => {
  const [wrap, setWrap] = useState<boolean>(true);

  const lines = textContent.split(/\r?\n/);

  return (
    <div className="file-text-doc-viewer">
      <div className="viewer-toolbar">
        <div className="doc-toolbar-meta">
          <span className="doc-line-count muted">
            {lines.length.toLocaleString()} {lines.length === 1 ? 'line' : 'lines'}
          </span>
          <span className="doc-size muted">· {formatSize(file.sizeBytes)}</span>
        </div>
        <Button
          type="button"
          size="sm"
          variant="outline"
          icon={<WrapText size={15} />}
          onClick={() => setWrap((w) => !w)}
          aria-label={wrap ? 'Disable line wrap' : 'Enable line wrap'}
          title={wrap ? 'No wrap' : 'Wrap lines'}
        >
          {wrap ? 'No wrap' : 'Wrap lines'}
        </Button>
      </div>

      <div className={`doc-paper-container ${wrap ? 'is-wrapped' : 'no-wrap'}`}>
        <div className="doc-text-body">
          {lines.map((line, index) => (
            <div key={index} className="doc-line" dir="auto">
              <span className="doc-line-number" aria-hidden="true">
                {index + 1}
              </span>
              <span className="doc-line-text" dir="auto">
                {line || '\u00a0'}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
