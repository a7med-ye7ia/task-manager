import React from "react";

type Variant = "error" | "notfound";

export const TaskIllustration: React.FC<{ variant: Variant }> = ({ variant }) => {
  const isError = variant === "error";
  const accent = isError ? "var(--danger)" : "var(--primary)";
  const accentBg = isError ? "var(--danger-bg)" : "var(--primary-light)";

  return (
    <svg
      viewBox="0 0 320 220"
      width="100%"
      role="img"
      aria-label={isError ? "A task board where one task failed" : "A task board with a missing task"}
      style={{ maxWidth: 300, height: "auto", display: "block", margin: "0 auto 1rem" }}
    >
      <ellipse cx="160" cy="118" rx="140" ry="92" style={{ fill: "var(--primary-light)" }} />

      <rect x="74" y="34" width="176" height="150" rx="18" transform="rotate(-6 162 109)"
        style={{ fill: "var(--bg-card)", stroke: "var(--border)", strokeWidth: 2 }} />
      <rect x="64" y="30" width="190" height="160" rx="18"
        style={{ fill: "var(--bg-card)", stroke: "var(--border)", strokeWidth: 2 }} />

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

      {/* row 3: failed (error) or missing (404, dashed) */}
      {isError ? (
        <>
          <circle cx="94" cy="164" r="11" style={{ fill: accentBg, stroke: accent, strokeWidth: 2 }} />
          <path d="M89 159 l10 10 M99 159 l-10 10" fill="none" strokeWidth="2.5" strokeLinecap="round" style={{ stroke: accent }} />
          <rect x="114" y="160" width="98" height="8" rx="4" style={{ fill: accentBg }} />
        </>
      ) : (
        <>
          <circle cx="94" cy="164" r="11" strokeDasharray="4 4"
            style={{ fill: "none", stroke: accent, strokeWidth: 2 }} />
          <rect x="114" y="160" width="98" height="8" rx="4" strokeDasharray="5 4"
            style={{ fill: "none", stroke: accent, strokeWidth: 1.5 }} />
        </>
      )}

      {/* badge */}
      {isError ? (
        <>
          <circle cx="246" cy="48" r="22" style={{ fill: "var(--warning-bg)", stroke: "var(--warning)", strokeWidth: 2 }} />
          <path d="M246 37 v14" strokeWidth="3.5" strokeLinecap="round" style={{ stroke: "var(--warning)" }} />
          <circle cx="246" cy="58" r="2.2" style={{ fill: "var(--warning)" }} />
        </>
      ) : (
        <>
          <circle cx="246" cy="48" r="22" style={{ fill: accentBg, stroke: accent, strokeWidth: 2 }} />
          <path d="M239 42 a7 7 0 1 1 10 6.5 c-2 1.2 -3 2.2 -3 4.5" fill="none" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" style={{ stroke: accent }} />
          <circle cx="246" cy="60" r="2.2" style={{ fill: accent }} />
        </>
      )}
    </svg>
  );
};