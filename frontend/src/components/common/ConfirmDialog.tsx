import React from "react";
import { Modal } from "./Modal";
import { Button } from "./Button";
export const ConfirmDialog: React.FC<{
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onClose: () => void;
}> = ({
  open,
  title,
  message,
  confirmLabel = "Confirm",
  onConfirm,
  onClose,
}) => (
  <Modal isOpen={open} title={title} onClose={onClose}>
    <p style={{ marginBottom: "1.25rem" }}>{message}</p>
    <div style={{ display: "flex", justifyContent: "flex-end", gap: ".75rem" }}>
      <Button variant="outline" onClick={onClose}>
        Cancel
      </Button>
      <Button variant="danger" onClick={onConfirm}>
        {confirmLabel}
      </Button>
    </div>
  </Modal>
);
