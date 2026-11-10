export const PenWriteIcon = ({ size = 16 }: { size?: number }) => (
    <svg className="pen-icon" width={size} height={size} viewBox="0 0 24 24"
      fill="none" stroke="currentColor" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path className="pen-line" d="M3 21h18" />
      <g className="pen-body">
        <path d="M17 3l4 4L8 20l-5 1 1-5z" />
      </g>
    </svg>
  );

  export const DownloadArrowIcon = ({ size = 16 }: { size?: number }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round"
      strokeLinejoin="round" aria-hidden="true">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
      <g className="dl-arrow">
        <path d="M12 3v12" />
        <path d="m7 10 5 5 5-5" />
      </g>
    </svg>
  );