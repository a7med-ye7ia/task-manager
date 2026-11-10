import React, { useState } from 'react';
import { Button } from '../../common/Button';
import { TaskFileDetail } from '../../../types';
import { ZoomIn, ZoomOut, Maximize2, RotateCw } from 'lucide-react';

export interface ImageViewerProps {
  file: TaskFileDetail;
  blobUrl: string;
}

export const ImageViewer: React.FC<ImageViewerProps> = ({ file, blobUrl }) => {
  const [zoom, setZoom] = useState<number>(1);
  const [rotation, setRotation] = useState<number>(0);

  const handleZoomIn = () => setZoom((z) => Math.min(4, Math.round((z + 0.25) * 100) / 100));
  const handleZoomOut = () => setZoom((z) => Math.max(0.25, Math.round((z - 0.25) * 100) / 100));
  const handleFit = () => {
    setZoom(1);
    setRotation(0);
  };
  const handleRotate = () => setRotation((r) => (r + 90) % 360);

  return (
    <div className="file-image-viewer">
      <div className="viewer-toolbar">
        <Button
          type="button"
          size="sm"
          variant="outline"
          icon={<ZoomIn size={15} />}
          onClick={handleZoomIn}
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
          onClick={handleZoomOut}
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
          onClick={handleFit}
          title="Fit to screen"
          aria-label="Fit to screen"
        >
          Fit
        </Button>
        <Button
          type="button"
          size="sm"
          variant="outline"
          icon={<RotateCw size={15} />}
          onClick={handleRotate}
          title="Rotate 90 degrees"
          aria-label="Rotate"
        >
          Rotate
        </Button>
        <span className="viewer-zoom-level muted">{Math.round(zoom * 100)}%</span>
      </div>

      <div className="image-viewport">
        <img
          src={blobUrl}
          alt={file.originalName}
          className="viewer-image-element"
          style={{
            transform: `scale(${zoom}) rotate(${rotation}deg)`,
            transformOrigin: 'center center',
            transition: 'transform 0.15s ease',
          }}
        />
      </div>
    </div>
  );
};
