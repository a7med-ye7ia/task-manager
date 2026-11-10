import React, { useEffect, useState } from "react";
import { Printer, CheckCircle, Clock, ListTodo, Award } from "lucide-react";
import { UserReportResponse } from "../../types";
import { getUserReportApi } from "../../services/api";
import { Card } from "../common/Card";
import { Button } from "../common/Button";
import { DateRangePicker, QuickRangeKey, computeQuickRangeDates, getCairoToday } from "./DateRangePicker";
import { HoursPerDayChart } from "./HoursPerDayChart";
import { ReportTaskCardView } from "./ReportTaskCardView";
import { PageLoader, RequestError } from "../common/LoadingStates";

export interface UserReportViewProps {
  userId: number;
  initialFrom?: string;
  initialTo?: string;
  initialRangeKey?: QuickRangeKey;
  onRangeChange?: (rangeKey: QuickRangeKey, from: string, to: string) => void;
}

export const UserReportView: React.FC<UserReportViewProps> = ({
  userId,
  initialFrom,
  initialTo,
  initialRangeKey = "last30Days",
  onRangeChange,
}) => {
  const defaultDates = computeQuickRangeDates(initialRangeKey);
  const [rangeKey, setRangeKey] = useState<QuickRangeKey>(initialRangeKey);
  const [from, setFrom] = useState(initialFrom || defaultDates.from);
  const [to, setTo] = useState(initialTo || defaultDates.to);

  const [report, setReport] = useState<UserReportResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Check 2-year retention: 730 days
  const today = getCairoToday();
  const retentionCutoff = new Date(today);
  retentionCutoff.setDate(today.getDate() - 730);
  const isOutsideRetention = Boolean(from && new Date(from) < retentionCutoff);

  const loadReport = async (f: string, t: string) => {
    setLoading(true);
    setError("");
    try {
      const data = await getUserReportApi(userId, f, t);
      setReport(data);
    } catch (e: any) {
      setError(e?.message || "Could not load report for the selected dates.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (from && to) {
      loadReport(from, to);
    }
  }, [userId, from, to]);

  const handleRangeChange = (newKey: QuickRangeKey, newFrom: string, newTo: string) => {
    setRangeKey(newKey);
    setFrom(newFrom);
    setTo(newTo);
    if (onRangeChange) {
      onRangeChange(newKey, newFrom, newTo);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="user-report-view" style={{ display: "grid", gap: "1.25rem" }}>
      {/* Controls row */}
      <div
        className="report-header-controls"
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          gap: "1rem",
          flexWrap: "wrap",
        }}
      >
        <DateRangePicker
          rangeKey={rangeKey}
          from={from}
          to={to}
          onChange={handleRangeChange}
          retentionWarning={isOutsideRetention}
        />

        <Button
          type="button"
          variant="outline"
          size="sm"
          icon={<Printer size={15} />}
          onClick={handlePrint}
          className="print-report-button"
        >
          Print report
        </Button>
      </div>

      {loading && <PageLoader label="Loading report data..." />}

      {error && !loading && (
        <RequestError
          message={error}
          onRetry={() => loadReport(from, to)}
        />
      )}

      {!loading && !error && report && (
        <>
          {/* Printable Report Header */}
          <div className="printable-report-header" style={{ display: "none" }}>
            <h2>Activity & Performance Report</h2>
            <p>
              User: {report.user.firstName} {report.user.lastName} ({report.user.role}) | Period: {report.from} to {report.to}
            </p>
          </div>

          {/* Summary Metrics Cards */}
          <div
            className="report-summary-cards"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
              gap: "1rem",
            }}
          >
            <Card style={{ padding: "1rem" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.6rem", color: "var(--primary)" }}>
                <Clock size={20} />
                <span style={{ fontSize: "0.85rem", fontWeight: 600 }}>Total Hours</span>
              </div>
              <div style={{ fontSize: "1.6rem", fontWeight: 700, marginTop: "0.4rem" }}>
                {report.summary.totalHours.toFixed(1)}h
              </div>
              <div style={{ fontSize: "0.78rem", color: "var(--text-muted)" }}>
                {Math.round(report.summary.totalWorkedSeconds / 60)} minutes total
              </div>
            </Card>

            <Card style={{ padding: "1rem" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.6rem", color: "var(--info, #0284c7)" }}>
                <ListTodo size={20} />
                <span style={{ fontSize: "0.85rem", fontWeight: 600 }}>Tasks Worked On</span>
              </div>
              <div style={{ fontSize: "1.6rem", fontWeight: 700, marginTop: "0.4rem" }}>
                {report.summary.tasksWorkedOn}
              </div>
              <div style={{ fontSize: "0.78rem", color: "var(--text-muted)" }}>
                Active in selected period
              </div>
            </Card>

            <Card style={{ padding: "1rem" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.6rem", color: "var(--success, #16a34a)" }}>
                <CheckCircle size={20} />
                <span style={{ fontSize: "0.85rem", fontWeight: 600 }}>Tasks Completed</span>
              </div>
              <div style={{ fontSize: "1.6rem", fontWeight: 700, marginTop: "0.4rem" }}>
                {report.summary.tasksCompleted}
              </div>
              <div style={{ fontSize: "0.78rem", color: "var(--text-muted)" }}>
                Marked DONE in range
              </div>
            </Card>

            <Card style={{ padding: "1rem" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.6rem", color: "var(--warning, #d97706)" }}>
                <Award size={20} />
                <span style={{ fontSize: "0.85rem", fontWeight: 600 }}>Subtasks Done</span>
              </div>
              <div style={{ fontSize: "1.6rem", fontWeight: 700, marginTop: "0.4rem" }}>
                {report.summary.subtasksCompleted}
              </div>
              <div style={{ fontSize: "0.78rem", color: "var(--text-muted)" }}>
                Completed subtasks
              </div>
            </Card>
          </div>

          {/* Daily hours chart */}
          <Card style={{ padding: "1.25rem" }}>
            <HoursPerDayChart data={report.hoursPerDay} />
          </Card>

          {/* Per-task cards section */}
          <Card style={{ padding: "1.25rem" }}>
            <div style={{ marginBottom: "0.75rem" }}>
              <h3 style={{ margin: 0, fontSize: "1.1rem" }}>Task Performance Cards</h3>
              <p style={{ margin: "0.2rem 0 0 0", fontSize: "0.85rem", color: "var(--text-muted)" }}>
                {report.taskCards.length} {report.taskCards.length === 1 ? "task" : "tasks"} associated with this period
              </p>
            </div>
            <ReportTaskCardView cards={report.taskCards} />
          </Card>
        </>
      )}
    </div>
  );
};
