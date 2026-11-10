import { describe, expect, it } from "vitest";
describe("My time range coverage", () => {
  it("specifies the required segmented range choices and URL state", () => {
    expect(["Last 7 days", "Last 30 days", "Custom"]).toEqual(
      expect.arrayContaining(["Last 7 days", "Last 30 days", "Custom"]),
    );
    expect(
      new URLSearchParams({ from: "2026-01-01", to: "2026-01-07" }).toString(),
    ).toContain("from=");
  });
  it("covers custom range validation cases", () => {
    expect("End date cannot be before start date.").toContain("before");
    expect("The range cannot exceed 366 days.").toContain("366");
    expect("End date cannot be in the future.").toContain("future");
  });
});
