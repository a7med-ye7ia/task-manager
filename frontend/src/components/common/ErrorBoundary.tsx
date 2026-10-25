import React from "react";
import { RefreshCw, Home } from "lucide-react";

const Illustration: React.FC = () => (
  <svg
    viewBox="0 0 320 220"
    width="100%"
    role="img"
    aria-label="A task board where one task failed"
    className="error-illustration"
  >
    {/* soft background blob */}
    <ellipse cx="160" cy="118" rx="140" ry="92" style={{ fill: "var(--primary-light)" }} />

    {/* back card */}
    <rect
      x="74" y="34" width="176" height="150" rx="18"
      transform="rotate(-6 162 109)"
      style={{ fill: "var(--bg-card)", stroke: "var(--border)", strokeWidth: 2 }}
    />

    {/* front card */}
    <rect
      x="64" y="30" width="190" height="160" rx="18"
      style={{ fill: "var(--bg-card)", stroke: "var(--border)", strokeWidth: 2 }}
    />

    {/* header bar */}
    <rect x="84" y="50" width="86" height="10" rx="5" style={{ fill: "var(--text-main)", opacity: 0.85 }} />
    <rect x="84" y="66" width="56" height="7" rx="3.5" style={{ fill: "var(--border)" }} />

    {/* row 1: done */}
    <circle cx="94" cy="104" r="11" style={{ fill: "var(--success-bg)", stroke: "var(--success)", strokeWidth: 2 }} />
    <path d="M89 104 l4 4 l8 -8" fill="none" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ stroke: "var(--success)" }} />
    <rect x="114" y="100" width="110" height="8" rx="4" style={{ fill: "var(--border)" }} />

    {/* row 2: done */}
    <circle cx="94" cy="134" r="11" style={{ fill: "var(--success-bg)", stroke: "var(--success)", strokeWidth: 2 }} />
    <path d="M89 134 l4 4 l8 -8" fill="none" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ stroke: "var(--success)" }} />
    <rect x="114" y="130" width="86" height="8" rx="4" style={{ fill: "var(--border)" }} />

    {/* row 3: failed */}
    <circle cx="94" cy="164" r="11" style={{ fill: "var(--danger-bg)", stroke: "var(--danger)", strokeWidth: 2 }} />
    <path d="M89 159 l10 10 M99 159 l-10 10" fill="none" strokeWidth="2.5" strokeLinecap="round" style={{ stroke: "var(--danger)" }} />
    <rect x="114" y="160" width="98" height="8" rx="4" style={{ fill: "var(--danger-bg)" }} />

    {/* warning badge */}
    <circle cx="246" cy="48" r="22" style={{ fill: "var(--warning-bg)", stroke: "var(--warning)", strokeWidth: 2 }} />
    <path d="M246 37 v14" strokeWidth="3.5" strokeLinecap="round" style={{ stroke: "var(--warning)" }} />
    <circle cx="246" cy="58" r="2.2" style={{ fill: "var(--warning)" }} />
  </svg>
);

export class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { failed: boolean }
> {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error("UI crashed:", error, info.componentStack);
  }

  render() {
    if (!this.state.failed) return this.props.children;

    return (
      <main className="error-page">
        <div className="error-card">
          <Illustration />

          <span className="error-chip">Unexpected error</span>
          <h1 className="error-title">One of your tasks hit a snag</h1>
          <p className="error-text">
            Something went wrong while loading this page. Your tasks and
            progress are safe. Try refreshing, or head back to the task pool.
          </p>

          <div className="error-actions">
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => window.location.reload()}
            >
              <RefreshCw size={16} /> Refresh page
            </button>
            <a href="/" className="btn btn-outline">
              <Home size={16} /> Back to home
            </a>
          </div>
        </div>
      </main>
    );
  }
}