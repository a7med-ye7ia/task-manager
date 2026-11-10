import React from "react";
import { DailyHours } from "../../types";

export const HoursPerDayChart: React.FC<{
  data: DailyHours[];
}> = ({ data }) => {
  if (!data || data.length === 0) {
    return (
      <div style={{ padding: "1.5rem", textAlign: "center", color: "var(--text-muted)" }}>
        No activity recorded in this period.
      </div>
    );
  }

  const maxHours = Math.max(1, ...data.map((d) => d.hours));

  return (
    <div className="hours-per-day-chart" style={{ display: "grid", gap: "0.5rem" }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          fontSize: "0.85rem",
          fontWeight: 600,
          color: "var(--text-muted)",
        }}
      >
        <span>Hours per day</span>
        <span>Peak: {maxHours.toFixed(1)}h</span>
      </div>

      <div
        className="chart-bars-scroll"
        style={{
          display: "flex",
          alignItems: "flex-end",
          gap: data.length > 31 ? "3px" : "6px",
          height: "140px",
          padding: "10px 4px 4px",
          background: "var(--surface-sunken, #f8fafc)",
          borderRadius: "8px",
          border: "1px solid var(--border-light, #e2e8f0)",
          overflowX: "auto",
        }}
      >
        {data.map((d) => {
          const heightPercent = Math.max(4, Math.round((d.hours / maxHours) * 100));
          const hasTime = d.hours > 0;
          const dateObj = new Date(d.date + "T00:00:00");
          const dateLabel = dateObj.toLocaleDateString("en-US", {
            month: "short",
            day: "numeric",
          });

          return (
            <div
              key={d.date}
              title={`${d.date}: ${d.hours.toFixed(1)}h (${Math.round(d.workedSeconds / 60)} min)`}
              aria-label={`${dateLabel}: ${d.hours.toFixed(1)}h`}
              style={{
                flex: "1 0 auto",
                minWidth: data.length > 31 ? "8px" : "16px",
                maxWidth: "28px",
                height: "100%",
                display: "flex",
                flexDirection: "column",
                justifyContent: "flex-end",
                alignItems: "center",
                cursor: "pointer",
              }}
            >
              <div
                style={{
                  width: "100%",
                  height: `${heightPercent}%`,
                  backgroundColor: hasTime ? "var(--primary, #3b82f6)" : "var(--border, #cbd5e1)",
                  opacity: hasTime ? 1 : 0.4,
                  borderRadius: "4px 4px 0 0",
                  transition: "height 0.2s ease",
                }}
              />
            </div>
          );
        })}
      </div>

      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          fontSize: "0.75rem",
          color: "var(--text-muted)",
          padding: "0 4px",
        }}
      >
        <span>{data[0]?.date}</span>
        {data.length > 1 && <span>{data[data.length - 1]?.date}</span>}
      </div>
    </div>
  );
};
