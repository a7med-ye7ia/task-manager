import React, { useEffect, useState } from "react";
import { Minus, Pencil, Plus, Trash2 } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { Button } from "../components/common/Button";
import { Card } from "../components/common/Card";
import { FieldError } from "../components/common/FieldError";
import { Input } from "../components/common/Input";
import { Select } from "../components/common/Select";
import { TaskPicker } from "../components/common/TaskPicker";
import { ConfirmDialog } from "../components/common/ConfirmDialog";
import { PageLoader, RequestError } from "../components/common/LoadingStates";
import { ErrorNotice } from "../components/common/ErrorNotice";
import {
  ApiError,
  addManualTimeSessionApi,
  deleteManualTimeSessionApi,
  editManualTimeSessionApi,
  getTimeSubtaskOptionsApi,
  listTimeSessionsApi,
} from "../services/api";
import {
  TimeRange,
  TimeRangeEntry,
  TimeSubtaskOption,
  TimeTaskOption,
} from "../types";
const today = () =>
  new Intl.DateTimeFormat("en-CA", { timeZone: "Africa/Cairo" }).format(
    new Date(),
  );
const fmt = (s: number) =>
  s < 60
    ? "less than 1 min"
    : `${Math.floor(s / 3600)}h ${Math.floor((s % 3600) / 60)}m`;
const seven = () => {
  const d = new Date(`${today()}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() - 6);
  return d.toISOString().slice(0, 10);
};
type Form = {
  task: TimeTaskOption | null;
  subtaskId: string;
  startedAt: string;
  endedAt: string;
  note: string;
};
const blank: Form = {
  task: null,
  subtaskId: "",
  startedAt: "",
  endedAt: "",
  note: "",
};
export const MyTimePage: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [params, setParams] = useSearchParams();
  const [data, setData] = useState<TimeRange | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<Form>(blank);
  const [subs, setSubs] = useState<TimeSubtaskOption[]>([]);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [editing, setEditing] = useState<TimeRangeEntry | null>(null);
  const [remove, setRemove] = useState<TimeRangeEntry | null>(null);
  const [customFrom, setCustomFrom] = useState(params.get("from") || seven());
  const [customTo, setCustomTo] = useState(params.get("to") || today());
  const [rangeError, setRangeError] = useState("");
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [customOpen, setCustomOpen] = useState(false);
  const from = params.get("from") || seven(),
    to = params.get("to") || today();
  const thirtyStart = (() => {
    const d = new Date(`${today()}T12:00:00Z`);
    d.setUTCDate(d.getUTCDate() - 29);
    return d.toISOString().slice(0, 10);
  })();
  const presetMode =
    to !== today()
      ? "custom"
      : from === seven()
        ? "seven"
        : from === thirtyStart
          ? "thirty"
          : "custom";
  const mode = customOpen ? "custom" : presetMode;
  const load = async () => {
    setLoading(true);
    try {
      setData(await listTimeSessionsApi(from, to));
      setError("");
    } catch {
      setError("Time entries could not be loaded.");
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    if (isAuthenticated) void load();
  }, [isAuthenticated, from, to]);
  useEffect(() => {
    const h = () => void load();
    window.addEventListener("time-session-changed", h);
    return () => window.removeEventListener("time-session-changed", h);
  }, [from, to]);
  useEffect(() => {
    if (form.task)
      void getTimeSubtaskOptionsApi(form.task.id)
        .then(setSubs)
        .catch(() => setSubs([]));
    else setSubs([]);
  }, [form.task?.id]);
  const update = (key: keyof Form, value: unknown) => {
    setForm((f) => ({ ...f, [key]: value }));
    setFields((f) => ({ ...f, [key]: "" }));
  };
  const toggleCustom = () => {
    if (customOpen) {
      setCustomOpen(false);
      setRangeError("");
    } else {
      setCustomFrom(from);
      setCustomTo(to);
      setCustomOpen(true);
    }
  };
  const choose = (value: string) => {
    setCustomOpen(false);
    setRangeError("");
    const end = today();
    const d = new Date(`${end}T12:00:00Z`);
    d.setUTCDate(d.getUTCDate() - (value === "seven" ? 6 : 29));
    setParams((p) => {
      p.set("from", d.toISOString().slice(0, 10));
      p.set("to", end);
      return p;
    });
  };
  const apply = () => {
    setRangeError("");
    if (!customFrom || !customTo) {
      setRangeError("Start and end dates are required.");
      return;
    }
    if (customTo < customFrom) {
      setRangeError("End date cannot be before start date.");
      return;
    }
    if ((Date.parse(customTo) - Date.parse(customFrom)) / 86400000 + 1 > 366) {
      setRangeError("The range cannot exceed 366 days.");
      return;
    }
    if (customTo > today()) {
      setRangeError("End date cannot be in the future.");
      return;
    }
    setParams((p) => {
      p.set("from", customFrom);
      p.set("to", customTo);
      return p;
    });
  };
  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    const f: Record<string, string> = {};
    if (!form.task) f.task = "Choose a task.";
    if (!form.startedAt) f.startedAt = "Start time is required.";
    if (!form.endedAt) f.endedAt = "End time is required.";
    if (form.note.trim().length < 3 || form.note.trim().length > 200)
      f.note = "Use 3 to 200 characters.";
    if (Object.keys(f).length) {
      setFields(f);
      return;
    }
    try {
      const payload = {
        taskId: form.task!.id,
        subtaskId: form.subtaskId ? Number(form.subtaskId) : undefined,
        startedAt: new Date(form.startedAt).toISOString(),
        endedAt: new Date(form.endedAt).toISOString(),
        note: form.note.trim(),
      };
      if (editing) await editManualTimeSessionApi(editing.id, payload);
      else await addManualTimeSessionApi(payload);
      setForm(blank);
      setEditing(null);
      setOpen(false);
      await load();
    } catch (x) {
      if (x instanceof ApiError) {
        const mapped: Record<string, string> = {};
        x.fieldErrors?.forEach(
          (v) => (mapped[v.field === "taskId" ? "task" : v.field] = v.message),
        );
        setFields(mapped);
      }
    }
  };
  if (loading && !data) return <PageLoader label="Loading time entries" />;
  if (error && !data) return <RequestError message={error} onRetry={load} />;
  return (
    <main className="page-container my-time-page">
      <h1 style={{ margin: "0.5rem" }}>My Attendance</h1>
      {error && <ErrorNotice error={error} />}
      <Card>
        <button
          type="button"
          className="section-toggle"
          onClick={() => setOpen((v) => !v)}
        >
          <h2>{editing ? "Edit manual entry" : "Add manual entry"}</h2>
          {open ? <Minus size={14}/> : <Plus size={14}/>}
        </button>
        {open && (
          <form noValidate onSubmit={save} style={{ margin: "0.2rem" }}>
            <TaskPicker
              value={form.task}
              onChange={(v) => {
                update("task", v);
                update("subtaskId", "");
              }}
              error={fields.task}
            />
            <div className="my-time-form-row">
              <div>
                <label>Subtask</label>
                <Select
                  value={form.subtaskId || "whole"}
                  onValueChange={(v) =>
                    update("subtaskId", v === "whole" ? "" : v)
                  }
                  options={[
                    { value: "whole", label: "Whole task" },
                    ...subs.map((s) => ({
                      value: String(s.id),
                      label: s.title,
                    })),
                  ]}
                  ariaLabel="Subtask"
                />
                <FieldError message={fields.subtaskId} />
              </div>
              <div>
                <Input
                  label="Start"
                  type="datetime-local"
                  value={form.startedAt}
                  onChange={(e) => update("startedAt", e.target.value)}
                />
                <FieldError message={fields.startedAt} />
              </div>
              <div>
                <Input
                  label="End"
                  type="datetime-local"
                  value={form.endedAt}
                  onChange={(e) => update("endedAt", e.target.value)}
                />
                <FieldError message={fields.endedAt} />
              </div>
            </div>
            <Input
              label="Note"
              value={form.note}
              onChange={(e) => update("note", e.target.value)}
              placeholder="write your note"
            />
            <FieldError message={fields.note} />
            <div
              style={{
                display: "flex",
                justifyContent: "flex-end",
                width: "100%",
              }}
            >
              <Button type="submit" style={{ marginTop: "1rem" }}>
                {editing ? "Save changes" : "Add entry"}
              </Button>
            </div>
          </form>
        )}
      </Card>
      <Card className="my-time-entries">
  <div className="my-time-filter">
    <div>
      <h2>Entries</h2>
      <p className="muted">
        {data &&
          `${fmt(data.workedSeconds)} total · ${data.daysWorked} days worked · ${fmt(data.overtimeSeconds)} overtime`}
      </p>
    </div>
    <div className="time-filter-wrapper">
  <div className="segmented-control">
    <button
      type="button"
      className={mode === "seven" ? "is-selected" : ""}
      onClick={() => choose("seven")}
    >
      Last 7 days
    </button>

    <button
      type="button"
      className={mode === "thirty" ? "is-selected" : ""}
      onClick={() => choose("thirty")}
    >
      Last 30 days
    </button>

    <button
      type="button"
      className={mode === "custom" ? "is-selected" : ""}
      aria-pressed={mode === "custom"}
      aria-expanded={customOpen}
      onClick={toggleCustom}
    >
      Custom
    </button>
  </div>

  {customOpen && (
    <div className="my-time-custom-range">
      <Input
        label="Start date"
        type="date"
        value={customFrom}
        onChange={(e) => setCustomFrom(e.target.value)}
      />

      <Input
        label="End date"
        type="date"
        value={customTo}
        onChange={(e) => setCustomTo(e.target.value)}
      />

      <Button type="button" size="sm" onClick={apply}>
        Apply
      </Button>

      <FieldError message={rangeError} />
    </div>
  )}
</div>
  </div>

  {data?.days.length ? (
    <div className="my-time-day-grid">
      {data.days.map((day) => {
        const shown = expanded.has(day.date)
          ? day.entries
          : day.entries.slice(0, 4);
        return (
          <div key={day.date} className="my-time-day-card">
            <h3>{day.date}</h3>
            <strong>{fmt(day.workedSeconds)}</strong>
            {shown.map((entry) => (
                  <div className="my-time-entry" key={entry.id}>
                    <b>{entry.taskTitle}</b>
                    <div>
                      {new Date(entry.startedAt).toLocaleTimeString("en-GB", {
                        timeZone: "Africa/Cairo",
                        hour: "2-digit",
                        minute: "2-digit",
                      })}{" "}
                      · {fmt(entry.workedSeconds)}{" "}
                      <span className="my-time-source">{entry.source}</span>
                      {entry.editable && (
                        <>
                          <button
                            className="icon-button"
                            aria-label="Edit time entry"
                            onClick={() => {
                              setEditing(entry);
                              setOpen(true);
                            }}
                          >
                            <Pencil size={14} />
                          </button>
                          <button
                            className="icon-button"
                            aria-label="Delete time entry"
                            onClick={() => setRemove(entry)}
                          >
                            <Trash2 size={14} />
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                ))}
                {day.entries.length > 4 && (
                  <button
                    className="text-button"
                    type="button"
                    onClick={() =>
                      setExpanded((s) => {
                        const n = new Set(s);
                        n.has(day.date) ? n.delete(day.date) : n.add(day.date);
                        return n;
                      })
                    }
                  >
                    {expanded.has(day.date)
                      ? "Show less"
                      : `Show ${day.entries.length - 4} more`}
                  </button>
                )}
          </div>
        );
      })}
    </div>
  ) : (
    <p className="muted my-time-empty">No time entries in this range.</p>
  )}
</Card>
        
      <ConfirmDialog
        open={!!remove}
        title="Delete time entry"
        message="Delete this manual time entry?"
        confirmLabel="Delete"
        onConfirm={async () => {
          if (remove) {
            await deleteManualTimeSessionApi(remove.id);
            setRemove(null);
            await load();
          }
        }}
        onClose={() => setRemove(null)}
      />
    </main>
  );
};