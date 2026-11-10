import React, { useEffect, useState } from "react";
import { Search, X } from "lucide-react";
import { FieldError } from "./FieldError";
import { ErrorNotice } from "./ErrorNotice";
import { Badge } from "./Badge";
import { getTimeTaskOptionsApi } from "../../services/api";
import { ApiError } from "../../services/api";
import { TimeTaskOption } from "../../types";

export const TaskPicker: React.FC<{
  value?: TimeTaskOption | null;
  onChange: (task: TimeTaskOption | null) => void;
  error?: string;
  label?: string;
}> = ({ value, onChange, error, label = "Task" }) => {
  const [query, setQuery] = useState("");
  const [options, setOptions] = useState<TimeTaskOption[]>([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(true);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const load = async (requestedPage = 0, requestedQuery = query) => {
    setLoading(true);
    try {
      const response = await getTimeTaskOptionsApi(
        requestedQuery,
        requestedPage,
      );
      setOptions((current) =>
        requestedPage === 0
          ? response.content
          : [...current, ...response.content],
      );
      setPage(response.page);
      setLast(response.last);
      setLoadError("");
    } catch (e) {
      setLoadError(
        e instanceof ApiError ? e.message : "Tasks could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    const timer = window.setTimeout(() => void load(0, query), 250);
    return () => window.clearTimeout(timer);
  }, [query]);
  return (
    <div style={{ display: "grid", gap: ".45rem" }}>
      <label style={{ fontWeight: 600 }}>{label}</label>
      {value && (
        <span
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: ".45rem",
            width: "fit-content",
            padding: ".3rem .5rem",
            border: "1px dashed var(--primary)",
            borderRadius: 999,
            color: "var(--primary)",
          }}
        >
          <span>{value.title}</span>
          <button
            type="button"
            aria-label={`Clear ${value.title}`}
            title="Clear task"
            onClick={() => onChange(null)}
            className="chip-remove"
          >
            <X size={14} />
          </button>
        </span>
      )}
      <div
        style={{
          border: `1px solid ${error ? "var(--danger)" : "var(--border)"}`,
          borderRadius: 12,
          overflow: "hidden",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: ".45rem",
            padding: ".45rem",
            borderBottom: "1px solid var(--border-light)",
          }}
        >
          <Search size={16} aria-hidden="true" />
          <input
            className="form-input"
            aria-label={`Search ${label}`}
            placeholder="Search tasks"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            style={{ height: 36, minWidth: 0, flex: 1 }}
          />
        </div>
        <div style={{ maxHeight: 210, overflowY: "auto", padding: ".35rem" }}>
          {options.length ? (
            options.map((task) => (
              <button
                type="button"
                key={task.id}
                onClick={() => onChange(task)}
                style={{
                  display: "flex",
                  width: "100%",
                  alignItems: "center",
                  justifyContent: "space-between",
                  gap: ".7rem",
                  padding: ".55rem .5rem",
                  border: 0,
                  borderRadius: 8,
                  background:
                    value?.id === task.id
                      ? "var(--primary-light)"
                      : "transparent",
                  cursor: "pointer",
                  textAlign: "left",
                }}
              >
                <span style={{ minWidth: 0 }}>
                  <strong
                    style={{
                      display: "block",
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {task.title}
                  </strong>
                  <small className="muted">
                    {task.deadline || "No deadline"}
                  </small>
                </span>
                <Badge variant="default">{task.status.replace("_", " ")}</Badge>
              </button>
            ))
          ) : (
            <p
              style={{
                padding: ".6rem",
                color: "var(--text-muted)",
                margin: 0,
              }}
            >
              {loading ? "Loading tasks…" : "No tasks found"}
            </p>
          )}
          {!last && (
            <button
              type="button"
              className="text-button"
              onClick={() => void load(page + 1)}
              disabled={loading}
            >
              {loading ? "Loading…" : "More tasks"}
            </button>
          )}
        </div>
      </div>
      {loadError && <ErrorNotice error={loadError} compact />}
      <FieldError message={error} />
    </div>
  );
};
