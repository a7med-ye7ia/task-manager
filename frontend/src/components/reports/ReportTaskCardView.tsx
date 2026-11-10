import React, { useState } from "react";
import { ChevronLeft, ChevronRight, Maximize2, Calendar } from "lucide-react";
import { ReportTaskCard } from "../../types";
import { Badge } from "../common/Badge";
import { Button } from "../common/Button";
import { Modal } from "../common/Modal";

export const ReportTaskCardView: React.FC<{
  cards: ReportTaskCard[];
}> = ({ cards }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [modalCard, setModalCard] = useState<ReportTaskCard | null>(null);

  if (!cards || cards.length === 0) {
    return (
      <div style={{ padding: "1.5rem", textAlign: "center", color: "var(--text-muted)" }}>
        No tasks found in this period.
      </div>
    );
  }

  const safeIndex = Math.min(Math.max(0, currentIndex), cards.length - 1);
  const currentCard = cards[safeIndex];

  const renderCardContent = (card: ReportTaskCard, isFullModal = false) => {
    return (
      <div style={{ display: "grid", gap: "0.85rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "1rem", flexWrap: "wrap" }}>
          <div>
            <h3 style={{ margin: "0 0 0.35rem 0", fontSize: isFullModal ? "1.25rem" : "1.05rem" }}>
              {card.title}
            </h3>
            <div style={{ display: "flex", gap: "0.4rem", flexWrap: "wrap", alignItems: "center" }}>
              {card.status && <Badge variant={card.status === "DONE" ? "success" : "default"}>{card.status}</Badge>}
              {card.priority && <Badge variant={card.priority === "HIGH" ? "danger" : "default"}>{card.priority}</Badge>}
              {card.role && <Badge variant="primary">{card.role === "LEAD" ? "Lead" : "Member"}</Badge>}
              {card.selfReported && <Badge variant="default">Self-reported</Badge>}
              {card.deadline && (
                <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", display: "inline-flex", alignItems: "center", gap: "0.25rem" }}>
                  <Calendar size={13} /> Due: {card.deadline}
                </span>
              )}
            </div>
          </div>

          <div style={{ textAlign: "right" }}>
            <span style={{ fontSize: "1.15rem", fontWeight: 700, color: "var(--primary)" }}>
              {card.hours.toFixed(1)} hrs
            </span>
            <div style={{ fontSize: "0.78rem", color: "var(--text-muted)" }}>
              logged in range
            </div>
          </div>
        </div>

        <p style={{ margin: 0, fontSize: "0.9rem", color: "var(--text-main, #1e293b)", fontStyle: "italic", background: "var(--surface-sunken, #f8fafc)", padding: "0.6rem 0.85rem", borderRadius: "6px" }}>
          {card.summary}
        </p>

        {card.events && card.events.length > 0 && (
          <div style={{ marginTop: "0.25rem" }}>
            <h4 style={{ margin: "0 0 0.5rem 0", fontSize: "0.88rem", color: "var(--text-muted)" }}>
              Events in range ({card.events.length})
            </h4>
            <div style={{ display: "grid", gap: "0.4rem", maxHeight: isFullModal ? "320px" : "180px", overflowY: "auto", paddingRight: "4px" }}>
              {card.events.map((ev, i) => {
                const dateStr = new Date(ev.occurredAt).toLocaleDateString("en-US", {
                  timeZone: "Africa/Cairo",
                  month: "short",
                  day: "numeric",
                  hour: "2-digit",
                  minute: "2-digit",
                });
                return (
                  <div
                    key={i}
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      fontSize: "0.82rem",
                      padding: "0.3rem 0.5rem",
                      backgroundColor: "var(--surface, #ffffff)",
                      border: "1px solid var(--border-light, #e2e8f0)",
                      borderRadius: "4px",
                    }}
                  >
                    <span style={{ fontWeight: 500 }}>
                      {ev.eventType === "SUBTASK_DONE" && `Subtask completed: ${ev.subtaskTitle || "Subtask"}`}
                      {ev.eventType === "CHECKPOINT_TICKED" && `Checkpoint ticked (${ev.count || 1})`}
                      {ev.eventType === "COMMENT_ADDED" && `Comment posted (${ev.count || 1})`}
                      {ev.eventType === "SENT_TO_REVIEW" && "Sent to review"}
                      {ev.eventType === "TASK_SENT_BACK" && "Sent back from review"}
                      {ev.eventType === "TASK_DONE" && "Task marked Done"}
                      {ev.eventType === "TASK_RESTORED" && "Task restored"}
                      {ev.eventType === "JOINED" && "Joined task"}
                      {ev.eventType === "LEFT" && "Left task"}
                      {!["SUBTASK_DONE", "CHECKPOINT_TICKED", "COMMENT_ADDED", "SENT_TO_REVIEW", "TASK_SENT_BACK", "TASK_DONE", "TASK_RESTORED", "JOINED", "LEFT"].includes(ev.eventType) && ev.eventType}
                    </span>
                    <span style={{ color: "var(--text-muted)", fontSize: "0.75rem" }}>
                      {dateStr}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="report-task-cards-container" style={{ display: "grid", gap: "1rem" }}>
      {/* Interactive single-card view with Back / Next */}
      <div className="interactive-card-wrapper">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "0.5rem",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <span style={{ fontWeight: 600, fontSize: "0.9rem" }}>
              Card {safeIndex + 1} of {cards.length}
            </span>
          </div>

          <div style={{ display: "flex", gap: "0.4rem", alignItems: "center" }}>
            <Button
              type="button"
              variant="outline"
              size="sm"
              icon={<Maximize2 size={13} />}
              onClick={() => setModalCard(currentCard)}
              title="Open full page"
            >
              Full card
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={safeIndex <= 0}
              onClick={() => setCurrentIndex((idx) => Math.max(0, idx - 1))}
              aria-label="Previous card"
            >
              <ChevronLeft size={16} /> Back
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={safeIndex >= cards.length - 1}
              onClick={() => setCurrentIndex((idx) => Math.min(cards.length - 1, idx + 1))}
              aria-label="Next card"
            >
              Next <ChevronRight size={16} />
            </Button>
          </div>
        </div>

        <div
          className="report-task-card"
          style={{
            padding: "1rem",
            border: "1px solid var(--border-light, #e2e8f0)",
            borderRadius: "10px",
            background: "var(--card-bg, #ffffff)",
          }}
        >
          {renderCardContent(currentCard)}
        </div>
      </div>

      {/* Print-only all-cards container */}
      <div className="print-all-cards" style={{ display: "none" }}>
        {cards.map((c, i) => (
          <div
            key={i}
            className="print-task-card"
            style={{
              padding: "0.85rem",
              marginBottom: "1rem",
              border: "1px solid #cbd5e1",
              borderRadius: "8px",
              pageBreakInside: "avoid",
            }}
          >
            <div style={{ fontSize: "0.75rem", color: "#64748b", marginBottom: "0.25rem" }}>
              Task {i + 1} of {cards.length}
            </div>
            {renderCardContent(c, true)}
          </div>
        ))}
      </div>

      {/* Full card modal */}
      {modalCard && (
        <Modal
          isOpen
          title="Task Report Card"
          onClose={() => setModalCard(null)}
          maxWidth="640px"
        >
          <div style={{ padding: "0.5rem 0" }}>
            {renderCardContent(modalCard, true)}
          </div>
        </Modal>
      )}
    </div>
  );
};
