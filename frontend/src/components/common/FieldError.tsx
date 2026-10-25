import React from "react";
import { AlertCircle } from "lucide-react";

// Single source of truth for how every validation error looks in the app.
// Input, PeoplePicker and any other field render this component, so the
// look only needs to be changed here.
export const FieldError: React.FC<{ message?: string; id?: string }> = ({
  message,
  id,
}) =>
  message ? (
    <div
      id={id}
      role="alert"
      style={{
        background: "none",
        border: 0,
        padding: 0,
        borderRadius: 0,
        boxShadow: "none",
        display: "flex",
        alignItems: "center",
        gap: "0.35rem",
        color: "var(--danger)",
        fontSize: "0.775rem",
        marginTop: "0.35rem",
      }}
    >
      <AlertCircle size={14} aria-hidden="true" style={{ flexShrink: 0 }} />
      {message}
    </div>
  ) : null;