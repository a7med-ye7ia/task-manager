import React from "react";

export type QuickRangeKey = "thisWeek" | "last30Days" | "thisMonth" | "custom";

export function getCairoToday(): Date {
  // Current time in Africa/Cairo
  const now = new Date();
  const cairoStr = now.toLocaleString("en-US", { timeZone: "Africa/Cairo" });
  return new Date(cairoStr);
}

export function formatYmd(d: Date): string {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function computeQuickRangeDates(key: QuickRangeKey): { from: string; to: string } {
  const today = getCairoToday();
  const to = formatYmd(today);

  if (key === "thisWeek") {
    // Monday of this week
    const dayOfWeek = today.getDay(); // 0 is Sunday, 1 is Monday...
    const diff = (dayOfWeek + 6) % 7; // days since Monday
    const monday = new Date(today);
    monday.setDate(today.getDate() - diff);
    return { from: formatYmd(monday), to };
  }

  if (key === "thisMonth") {
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    return { from: formatYmd(firstDay), to };
  }

  if (key === "last30Days") {
    const past = new Date(today);
    past.setDate(today.getDate() - 29);
    return { from: formatYmd(past), to };
  }

  // fallback custom
  const past = new Date(today);
  past.setDate(today.getDate() - 29);
  return { from: formatYmd(past), to };
}

export interface DateRangePickerProps {
  rangeKey: QuickRangeKey;
  from: string;
  to: string;
  onChange: (rangeKey: QuickRangeKey, from: string, to: string) => void;
  retentionWarning?: boolean;
}

export const DateRangePicker: React.FC<DateRangePickerProps> = ({
  rangeKey,
  from,
  to,
  onChange,
  retentionWarning,
}) => {
  const selectQuickRange = (key: QuickRangeKey) => {
    if (key === "custom") {
      onChange("custom", from, to);
    } else {
      const dates = computeQuickRangeDates(key);
      onChange(key, dates.from, dates.to);
    }
  };

  return (
    <div className="date-range-picker-container" style={{ display: "grid", gap: "0.75rem" }}>
      <div
        className="quick-ranges-row"
        style={{
          display: "flex",
          gap: "0.5rem",
          flexWrap: "wrap",
          alignItems: "center",
        }}
      >
        <span style={{ fontSize: "0.85rem", fontWeight: 600, color: "var(--text-muted)" }}>
          Range:
        </span>
        <button
          type="button"
          className={`filter-pill ${rangeKey === "thisWeek" ? "active" : ""}`}
          onClick={() => selectQuickRange("thisWeek")}
        >
          This week
        </button>
        <button
          type="button"
          className={`filter-pill ${rangeKey === "last30Days" ? "active" : ""}`}
          onClick={() => selectQuickRange("last30Days")}
        >
          Last 30 days
        </button>
        <button
          type="button"
          className={`filter-pill ${rangeKey === "thisMonth" ? "active" : ""}`}
          onClick={() => selectQuickRange("thisMonth")}
        >
          This month
        </button>
        <button
          type="button"
          className={`filter-pill ${rangeKey === "custom" ? "active" : ""}`}
          onClick={() => selectQuickRange("custom")}
        >
          Custom
        </button>
      </div>

      <div
        className="custom-dates-row"
        style={{
          display: "flex",
          gap: "1rem",
          alignItems: "center",
          flexWrap: "wrap",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <label htmlFor="report-from-date" style={{ fontSize: "0.85rem", fontWeight: 600 }}>
            From:
          </label>
          <input
            id="report-from-date"
            type="date"
            className="form-input"
            style={{ padding: "0.35rem 0.6rem", fontSize: "0.88rem" }}
            value={from}
            onChange={(e) => onChange("custom", e.target.value, to)}
          />
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <label htmlFor="report-to-date" style={{ fontSize: "0.85rem", fontWeight: 600 }}>
            To:
          </label>
          <input
            id="report-to-date"
            type="date"
            className="form-input"
            style={{ padding: "0.35rem 0.6rem", fontSize: "0.88rem" }}
            value={to}
            onChange={(e) => onChange("custom", from, e.target.value)}
          />
        </div>
      </div>

      {retentionWarning && (
        <div
          role="alert"
          style={{
            padding: "0.6rem 0.9rem",
            backgroundColor: "var(--warning-bg, #fef3c7)",
            color: "var(--warning-text, #92400e)",
            borderRadius: "8px",
            fontSize: "0.85rem",
          }}
        >
          Selected dates are outside the 2-year retention period. Data before this period is not available.
        </div>
      )}
    </div>
  );
};
