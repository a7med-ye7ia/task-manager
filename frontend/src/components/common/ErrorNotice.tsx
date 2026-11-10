import React from "react";
import { AlertCircle, RefreshCw, X } from "lucide-react";
import { ApiError } from "../../services/api";

export type UiError = {
  message: string;
  fieldErrors: { field: string; message: string }[];
  traceId?: string;
};

export function toUiError(error: unknown): UiError {
  if (error instanceof ApiError) {
    if (error.status === 0)
      return {
        message:
          "Cannot reach the server. Check your connection and try again.",
        fieldErrors: [],
      };
    if (error.status === 401)
      return {
        message: "Your session has expired. Sign in again.",
        fieldErrors: error.fieldErrors || [],
        traceId: error.traceId,
      };
    if (error.status === 403)
      return {
        message: "You do not have permission to do that.",
        fieldErrors: error.fieldErrors || [],
        traceId: error.traceId,
      };
    return {
      message: error.message || "Something went wrong. Try again.",
      fieldErrors: error.fieldErrors || [],
      traceId: error.traceId,
    };
  }
  return { message: "Something went wrong. Try again.", fieldErrors: [] };
}

export const ErrorNotice: React.FC<{
  error?: UiError | string;
  onRetry?: () => void;
  onDismiss?: () => void;
  compact?: boolean;
}> = ({ error, onRetry, onDismiss, compact }) => {
  if (!error) return null;
  const value: UiError =
    typeof error === "string" ? { message: error, fieldErrors: [] } : error;
  return (
    <div
      role="alert"
      className={compact ? "refresh-notice error-notice" : "error-notice"}
    >
      <AlertCircle size={16} aria-hidden="true" />
      <span>
        {value.message}
        {value.traceId && (
          <>
            <br />
            <small>Reference: {value.traceId}</small>
          </>
        )}
      </span>
      {onRetry && (
        <button type="button" className="request-error-retry" onClick={onRetry}>
          <RefreshCw size={14} /> Try again
        </button>
      )}
      {onDismiss && (
        <button
          type="button"
          className="icon-button"
          aria-label="Dismiss error"
          onClick={onDismiss}
        >
          <X size={15} />
        </button>
      )}
    </div>
  );
};
