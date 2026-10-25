import React, { useEffect, useMemo, useState } from "react";
import { Search, X } from "lucide-react";
import { User } from "../../types";
import { UserAvatar } from "../avatar/UserAvatar";
import { FieldError } from "./FieldError";

const ChipRemove: React.FC<{ label: string; onClick: () => void }> = ({
  label,
  onClick,
}) => {
  const [active, setActive] = useState(false);
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      onMouseEnter={() => setActive(true)}
      onMouseLeave={() => setActive(false)}
      onFocus={() => setActive(true)}
      onBlur={() => setActive(false)}
      style={{
        display: "inline-flex",
        border: 0,
        borderRadius: 99,
        background: active ? "var(--danger)" : "transparent",
        color: active ? "#fff" : "inherit",
        cursor: "pointer",
        padding: 1,
      }}
    >
      <X size={14} />
    </button>
  );
};

export const PeoplePicker: React.FC<{
  users: User[];
  value: number[];
  onChange: (ids: number[]) => void;
  label?: string;
  error?: string;
  leadUserId?: number;
  taskMemberIds?: number[];
  onOutsideSelect?: (user: User) => void;
  // "Add to task" mode: by default list only people who are NOT on the task.
  // Searching still finds everyone, and people already on the task show as checked.
  addMode?: boolean;
  loadOptions?: (query: string) => Promise<User[]>;
}> = ({ users, value, onChange, label = "People", error, leadUserId, taskMemberIds, onOutsideSelect, addMode, loadOptions }) => {
  const [query, setQuery] = useState("");
  const [remoteUsers, setRemoteUsers] = useState<User[]>([]);
  const [remoteError, setRemoteError] = useState("");

  useEffect(() => {
    if (!loadOptions) return;
    const timer = window.setTimeout(() => {
      loadOptions(query).then((result) => { setRemoteError(""); setRemoteUsers(result); }).catch(() => setRemoteError("People could not be loaded. Please try again."));
    }, 250);
    return () => window.clearTimeout(timer);
  }, [loadOptions, query]);

  const availableUsers = useMemo(() => {
    const byId = new Map<number, User>();
    [...users, ...remoteUsers].forEach((user) => byId.set(user.id, user));
    return [...byId.values()];
  }, [users, remoteUsers]);

  const active = useMemo(
    () =>
      availableUsers.filter(
        (u) =>
          (u.status === undefined || u.status === "ACTIVE") &&
          (!taskMemberIds ||
            (addMode ? !taskMemberIds.includes(u.id) : taskMemberIds.includes(u.id)) ||
            query.trim().length > 0) &&
          `${u.firstName} ${u.lastName} ${u.email}`
            .toLowerCase()
            .includes(query.toLowerCase()),
      ),
    [availableUsers, query, taskMemberIds, addMode],
  );

  const selected = availableUsers.filter((u) => value.includes(u.id));

  const toggle = (id: number) =>
    onChange(
      value.includes(id) ? value.filter((item) => item !== id) : [...value, id],
    );

  return (
    <div style={{ display: "grid", gap: ".5rem" }}>
      <h3 style={{ margin: 0 }}>{label}</h3>

      {selected.length > 0 && (
        <div style={{ display: "flex", gap: ".45rem", flexWrap: "wrap" }}>
          {selected.map((user) => {
            const isLead = user.id === leadUserId;

            return (
              <span
                key={user.id}
                title={isLead ? "Team lead" : undefined}
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: ".35rem",
                  padding: ".28rem .45rem .28rem .35rem",
                  border: "1px dashed var(--primary)",
                  borderRadius: 999,
                  fontSize: isLead ? ".85rem" : ".82rem",
                  fontWeight: isLead ? 600 : 400,
                  color: "var(--primary)",
                  background: isLead
                    ? "var(--primary-light)"
                    : "var(--primary-soft)",
                }}
              >
                <span>
                  {user.firstName} {user.lastName}
                </span>
                <ChipRemove
                  label={`Remove ${user.firstName} ${user.lastName}`}
                  onClick={() => toggle(user.id)}
                />
              </span>
            );
          })}
        </div>
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
            padding: ".45rem",
            borderBottom: "1px solid var(--border-light)",
            display: "flex",
            alignItems: "center",
            gap: ".5rem",
          }}
        >
          <Search
            size={16}
            style={{
              flexShrink: 0,
              color: "var(--text-muted)",
            }}
          />

          <input
            className="form-input"
            aria-label={`Search ${label}`}
            placeholder="Search people"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            style={{
              flex: 1,
              minWidth: 0,
              height: 36,
              paddingLeft: ".65rem",
            }}
          />
        </div>

        <div style={{ maxHeight: 180, overflowY: "auto", padding: ".35rem" }}>
          {active.length ? (
            active.map((user) => (
              <label
                key={user.id}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: ".55rem",
                  padding: ".4rem",
                  cursor: "pointer",
                  borderRadius: 8,
                }}
              >
                <input
                  type="checkbox"
                  checked={
                    value.includes(user.id) ||
                    Boolean(addMode && taskMemberIds?.includes(user.id))
                  }
                  disabled={Boolean(addMode && taskMemberIds?.includes(user.id))}
                  onChange={() => {
                    if (taskMemberIds && !taskMemberIds.includes(user.id) && !value.includes(user.id)) {
                      onOutsideSelect?.(user);
                      return;
                    }
                    toggle(user.id);
                  }}
                />
                <UserAvatar
                  userId={user.id}
                  avatarPath={user.avatarPath}
                  firstName={user.firstName}
                  lastName={user.lastName}
                  size={26}
                />
                <span>
                  {user.firstName} {user.lastName}
                  {taskMemberIds && !addMode && !taskMemberIds.includes(user.id) && <small style={{ display: "block", color: "var(--text-muted)" }}>Not on this task</small>}
                  {taskMemberIds && addMode && taskMemberIds.includes(user.id) && <small style={{ display: "block", color: "var(--text-muted)" }}>Already on this task</small>}
                </span>
              </label>
            ))
          ) : (
            <p
              style={{
                padding: ".5rem",
                color: "var(--text-muted)",
                fontSize: ".85rem",
              }}
            >
              {query.trim()
                ? `No results for "${query.trim()}"`
                : addMode
                  ? "Everyone is already on this task."
                  : "No people available."}
            </p>
          )}
        </div>
      </div>

      <FieldError message={error || remoteError} />
    </div>
  );
};
