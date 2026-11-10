import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ErrorNotice, toUiError } from './ErrorNotice'; 

type TimerChipProps = {
  taskId: number;
  taskTitle: string;
  state: 'RUNNING' | 'PAUSED';
  workedSeconds: number; 
  receivedAt: number;    
  error?: unknown;
};

function useTick(active: boolean) {
  const [, setTick] = useState(0);
  useEffect(() => {
    if (!active) return;
    const id = window.setInterval(() => setTick((t) => t + 1), 1000);
    return () => window.clearInterval(id);
  }, [active]);
}

export function formatDuration(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  const h = Math.floor(s / 3600);
  const mm = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
  const ss = String(s % 60).padStart(2, '0');
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}

function ClockIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="10" />
      <line className="clock-hand clock-hand-hour" x1="12" y1="12" x2="12" y2="8" />
      <line className="clock-hand clock-hand-minute" x1="12" y1="12" x2="12" y2="6" />
    </svg>
  );
}

export function TimerChip({
  taskId,
  taskTitle,
  state,
  workedSeconds,
  receivedAt,
  error,
}: TimerChipProps) {
  const running = state === 'RUNNING';
  useTick(running); // العد بيحصل جوه الـ chip بس، مش في الـ navbar كله

  const seconds = workedSeconds + (running ? (Date.now() - receivedAt) / 1000 : 0);
  const label = running ? 'running' : 'paused';

  return (
    <div className="running-time-wrap">
      <Link
        to={`/tasks/${taskId}`}
        className={`running-time-chip ${running ? 'is-running' : 'is-paused'}`}
        title={`${taskTitle} (${label})`}
        aria-label={`Timer ${label} on ${taskTitle}`}
      >
        <span className="chip-slot">
          <span className="chip-icon">
            <ClockIcon />
          </span>
          <span className="chip-time" aria-hidden="true">
            {formatDuration(seconds)}
          </span>
        </span>
      </Link>
      {error ? <ErrorNotice error={toUiError(error)} compact /> : null}
    </div>
  );
}