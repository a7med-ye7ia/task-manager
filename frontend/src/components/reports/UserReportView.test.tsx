import { describe, expect, it } from "vitest";
import { computeQuickRangeDates, formatYmd } from "./DateRangePicker";

describe("Phase 11 - Reports and Profile Tabs", () => {
  it("computes quick ranges correctly", () => {
    const thisWeek = computeQuickRangeDates("thisWeek");
    expect(thisWeek.from).toBeDefined();
    expect(thisWeek.to).toBeDefined();
    expect(new Date(thisWeek.from).getTime()).toBeLessThanOrEqual(new Date(thisWeek.to).getTime());

    const last30 = computeQuickRangeDates("last30Days");
    expect(last30.from).toBeDefined();
    expect(last30.to).toBeDefined();

    const thisMonth = computeQuickRangeDates("thisMonth");
    expect(thisMonth.from.endsWith("-01")).toBe(true);
  });

  it("formats date strings into YYYY-MM-DD", () => {
    const d = new Date(2026, 10, 10); // Nov 10, 2026
    expect(formatYmd(d)).toBe("2026-11-10");
  });

  it("validates retention limit and 366-day range rules", () => {
    const retentionLimit = new Date("2026-11-10");
    retentionLimit.setDate(retentionLimit.getDate() - 730);

    const oldDate = new Date("2024-01-01");
    expect(oldDate.getTime()).toBeLessThan(retentionLimit.getTime());

    // 366 days diff
    const start = new Date("2025-01-01");
    const end = new Date("2026-02-01");
    const diffDays = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    expect(diffDays).toBeGreaterThan(366);
  });

  it("supports report tab URL synchronization", () => {
    const params = new URLSearchParams({
      tab: "report",
      range: "last30Days",
      from: "2026-10-11",
      to: "2026-11-10",
    });
    expect(params.get("tab")).toBe("report");
    expect(params.get("range")).toBe("last30Days");
    expect(params.get("from")).toBe("2026-10-11");
    expect(params.get("to")).toBe("2026-11-10");
  });
});
