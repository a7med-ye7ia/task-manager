import React, { useState } from 'react';
import { Button } from '../../common/Button';
import { TaskFileDetail } from '../../../types';
import { ChevronLeft, ChevronRight, ZoomIn, ZoomOut, Maximize2 } from 'lucide-react';

export interface PdfViewerProps {
  file: TaskFileDetail;
  blobUrl: string;
}

export const PdfViewer: React.FC<PdfViewerProps> = ({ file, blobUrl }) => {
  const [page, setPage] = useState<number>(1);
  const [zoom, setZoom] = useState<number>(100);

  const prevPage = () => setPage((p) => Math.max(1, p - 1));
  const nextPage = () => setPage((p) => p + 1);
  const zoomIn = () => setZoom((z) => Math.min(300, z + 25));
  const zoomOut = () => setZoom((z) => Math.max(50, z - 25));
  const fit = () => setZoom(100);

  // Construct standard PDF parameter URL hash (#page=N&zoom=Z)
  const pdfSource = `${blobUrl}#page=${page}&zoom=${zoom}`;

  return (
    <div className="file-pdf-viewer">
      <div className="viewer-toolbar">
        <div className="viewer-toolbar-group">
          <Button
            type="button"
            size="sm"
            variant="outline"
            icon={<ChevronLeft size={15} />}
            onClick={prevPage}
            disabled={page <= 1}
            aria-label="Previous page"
            title="Previous page"
          >
            Prev
          </Button>
          <span className="pdf-page-display">Page {page}</span>
          <Button
            type="button"
            size="sm"
            variant="outline"
            icon={<ChevronRight size={15} />}
            onClick={nextPage}
            aria-label="Next page"
            title="Next page"
          >
            Next
          </Button>
        </div>

        <div className="viewer-toolbar-group">
          <Button
            type="button"
            size="sm"
            variant="outline"
            icon={<ZoomIn size={15} />}
            onClick={zoomIn}
            title="Zoom in"
            aria-label="Zoom in"
          >
            Zoom in
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            icon={<ZoomOut size={15} />}
            onClick={zoomOut}
            title="Zoom out"
            aria-label="Zoom out"
          >
            Zoom out
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            icon={<Maximize2 size={15} />}
            onClick={fit}
            title="Fit zoom"
            aria-label="Fit zoom"
          >
            Fit
          </Button>
          <span className="viewer-zoom-level muted">{zoom}%</span>
        </div>
      </div>

      <div className="pdf-frame-wrapper">
        <iframe
          key={`${page}-${zoom}`}
          src={pdfSource}
          title={file.originalName}
          className="pdf-embed-frame"
        />
      </div>
    </div>
  );
};
