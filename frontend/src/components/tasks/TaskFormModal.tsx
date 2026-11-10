import React, { useEffect, useMemo, useState } from "react";
import { Modal } from "../common/Modal";
import { Button } from "../common/Button";
import { Input } from "../common/Input";
import { Select } from "../common/Select";
import { PeoplePicker } from "../common/PeoplePicker";
import { FieldError } from "../common/FieldError";
import { ErrorNotice } from "../common/ErrorNotice";
import { User, Task, TaskPriority } from "../../types";
import { CalendarDays, FileText, Plus, Trash2 } from "lucide-react";
import { FileDropzone } from "./FileDropzone";
import { useAuth } from "../../context/AuthContext";
import { IconButton } from "../common/IconButton";

export const TaskFormModal: React.FC<{
  task?: Task | null;
  users: User[];
  onClose: () => void;
  onSave: (data: any) => Promise<void>;
}> = ({ task, users, onClose, onSave }) => {
  const { isManager, user: currentUser } = useAuth();
  const [form, setForm] = useState({
    title: "",
    description: "",
    deadline: "",
    priority: "MEDIUM" as TaskPriority,
    memberUserIds: [] as number[],
    leadUserId: undefined as number | undefined,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [showTeamMembers, setShowTeamMembers] = useState(false);
  const [subtasks, setSubtasks] = useState<
    {
      title: string;
      description: string;
      deadline: string;
      assigneeUserIds: number[];
      files: File[];
    }[]
  >([]);
  const [checkpoints, setCheckpoints] = useState<string[]>([]);
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [noChanges, setNoChanges] = useState(false);

  // The "nothing changed" message stays until the user edits something.
  useEffect(() => {
    setNoChanges(false);
  }, [form]);

  const latestSubtaskDeadline = useMemo(
    () =>
      subtasks
        .map((s) => s.deadline)
        .filter(Boolean)
        .sort()
        .at(-1) || "",
    [subtasks],
  );

  useEffect(() => {
    if (task) {
      setForm({
        title: task.title,
        description: task.description || "",
        deadline: task.deadline,
        priority: task.priority,
        memberUserIds: task.members.map((m) => m.user.id),
        leadUserId: task.lead?.id,
      });
    } else if (!isManager && currentUser) {
      setForm((prev) => ({
        ...prev,
        memberUserIds: [currentUser.id],
        leadUserId: currentUser.id,
      }));
    }
  }, [task, isManager, currentUser]);

  const setMembers = (ids: number[]) => {
    setForm((current) => {
      const nextLead =
        current.leadUserId && ids.includes(current.leadUserId)
          ? current.leadUserId
          : undefined;
      return { ...current, memberUserIds: ids, leadUserId: nextLead };
    });
    clearFieldError("members");
  };

  const clearFieldError = (key: string) => {
    setFieldErrors((current) => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
    setError("");
  };

  const validateField = (key: string) => {
    const next: Record<string, string> = {};
    if (key === "title") {
      const title = form.title.trim();
      if (!title) next.title = "Task title is required.";
      else if (title.length < 3)
        next.title = "Task title must be at least 3 characters.";
      else if (title.length > 150)
        next.title = "Task title cannot exceed 150 characters.";
    }
    if (key === "deadline") {
      if (!isManager && !form.deadline) {
        next.deadline = "Task deadline is required.";
      } else if (!subtasks.length && !form.deadline) {
        next.deadline = "Choose a deadline.";
      }
    }
    const match = key.match(/^subtask-(title|deadline|assignees)-(\d+)$/);
    if (match) {
      const subtask = subtasks[Number(match[2])];
      if (match[1] === "title" && !subtask.title.trim())
        next[key] = "Subtask title is required.";
      if (match[1] === "deadline" && !subtask.deadline)
        next[key] = "Choose a deadline for this subtask.";
      if (
        match[1] === "assignees" &&
        isManager &&
        !subtask.assigneeUserIds.length
      )
        next[key] = "Select at least one assignee.";
    }
    const checkpoint = key.match(/^checkpoint-(\d+)$/);
    if (checkpoint && !checkpoints[Number(checkpoint[1])]?.trim())
      next[key] = "Checkpoint title is required.";
    setFieldErrors((current) => ({
      ...current,
      ...next,
      ...(Object.keys(next).length ? {} : { [key]: undefined as any }),
    }));
  };

  const showErrors = (nextErrors: Record<string, string>) => {
    setFieldErrors(nextErrors);
    if (nextErrors.members) setShowTeamMembers(true);
    const first = Object.keys(nextErrors)[0];
    window.setTimeout(
      () => document.getElementById(`task-field-${first}`)?.focus(),
      0,
    );
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    const nextErrors: Record<string, string> = {};

    if (form.title.trim().length < 3 || form.title.trim().length > 150)
      nextErrors.title = "Task title must be between 3 and 150 characters.";
    if (!task && isManager && !form.memberUserIds.length)
      nextErrors.members = "Select at least one team member.";
    if (!task && !isManager && !form.deadline)
      nextErrors.deadline = "Task deadline is required.";
    else if (!task && !subtasks.length && !form.deadline)
      nextErrors.deadline =
        "Choose a deadline or add a subtask with a deadline.";

    subtasks.forEach((subtask, index) => {
      if (!subtask.title.trim())
        nextErrors[`subtask-title-${index}`] = "Subtask title is required.";
      if (!subtask.deadline)
        nextErrors[`subtask-deadline-${index}`] =
          "Choose a deadline for this subtask.";
      if (isManager && !subtask.assigneeUserIds.length)
        nextErrors[`subtask-assignees-${index}`] =
          "Select at least one assignee.";
    });
    checkpoints.forEach((checkpoint, index) => {
      if (!checkpoint.trim())
        nextErrors[`checkpoint-${index}`] = "Checkpoint title is required.";
    });

    if (Object.keys(nextErrors).length) {
      showErrors(nextErrors);
      return;
    }

    if (task) {
      const unchanged =
        form.title.trim() === task.title.trim() &&
        form.description.trim() === (task.description || "").trim() &&
        form.deadline === task.deadline &&
        form.priority === task.priority;
      if (unchanged) {
        setNoChanges(true);
        return;
      }
    }

    setFieldErrors({});
    setSaving(true);
    try {
      await onSave({
        ...form,
        selfReported: !isManager ? true : undefined,
        memberUserIds: !isManager
          ? currentUser
            ? [currentUser.id]
            : []
          : form.memberUserIds,
        leadUserId: !isManager ? currentUser?.id : form.leadUserId,
        deadline:
          latestSubtaskDeadline &&
          (!form.deadline || form.deadline < latestSubtaskDeadline)
            ? latestSubtaskDeadline
            : form.deadline,
        subtasks: task
          ? undefined
          : subtasks.map((s) => ({
              title: s.title,
              description: s.description,
              deadline: s.deadline,
              assigneeUserIds: !isManager
                ? currentUser
                  ? [currentUser.id]
                  : []
                : s.assigneeUserIds,
            })),
        subtaskFiles: task
          ? undefined
          : subtasks.map((s) => ({
              title: s.title,
              deadline: s.deadline,
              files: s.files,
            })),
        checkpoints: task
          ? undefined
          : checkpoints.filter(Boolean).map((title) => ({ title })),
        files: pendingFiles,
      });
      onClose();
    } catch (e: any) {
      const mapped: Record<string, string> = {};
      (e.fieldErrors || []).forEach(
        (item: { field: string; message: string }) => {
          const field = item.field
            .replace(
              /^subtasks\[(\d+)\]\.(assigneeIds|assigneeUserIds)$/,
              "subtask-assignees-$1",
            )
            .replace(/^subtasks\[(\d+)\]\.(title|deadline)$/, "subtask-$2-$1")
            .replace(/^checkpoints\[(\d+)\]\.title$/, "checkpoint-$1");
          if (
            /^(title|deadline|members|subtask-(title|deadline|assignees)-\d+|checkpoint-\d+)$/.test(
              field,
            )
          )
            mapped[field] = item.message;
        },
      );
      if (Object.keys(mapped).length) showErrors(mapped);
      else
        setError(
          e?.message ||
            (e.status === 403
              ? "You do not have permission to save this task."
              : "The task could not be saved. Please try again."),
        );
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={task ? "Edit task" : "Create New task"}
      maxWidth="650px"
    >
      <form
        noValidate
        onSubmit={submit}
        style={{ display: "grid", gap: "1rem" }}
      >
        <div style={{ display: "grid", gap: ".4rem" }}>
          <h3 style={{ margin: 0 }}>Title</h3>
          <Input
            label=""
            icon={<FileText size={17} />}
            value={form.title}
            id="task-field-title"
            onChange={(e) => {
              setForm({ ...form, title: e.target.value });
              clearFieldError("title");
            }}
            onBlur={() => validateField("title")}
            error={fieldErrors.title}
            maxLength={150}
            required
            placeholder="Enter task title"
          />
        </div>
        <label className="field-label">
          <h3>Description</h3>
          <textarea
            className="form-input"
            style={{ minHeight: 100, padding: "0.8rem", marginTop: "0.4rem" }}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            maxLength={10000}
            placeholder="Enter task description"
          />
        </label>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: "1rem",
          }}
        >
          <div style={{ display: "grid" }}>
            <h3 style={{ margin: 0 }}>Deadline</h3>
            <Input
              label=""
              icon={<CalendarDays size={17} />}
              type="date"
              min={latestSubtaskDeadline || undefined}
              value={
                latestSubtaskDeadline &&
                (!form.deadline || form.deadline < latestSubtaskDeadline)
                  ? latestSubtaskDeadline
                  : form.deadline
              }
              id="task-field-deadline"
              onChange={(e) => {
                setForm({ ...form, deadline: e.target.value });
                clearFieldError("deadline");
              }}
              onBlur={() => validateField("deadline")}
              error={fieldErrors.deadline}
              required={!isManager || !subtasks.length}
            />
          </div>
          <label className="field-label">
            <h3>Priority</h3>
            <Select
              value={form.priority}
              onValueChange={(priority) =>
                setForm({ ...form, priority: priority as TaskPriority })
              }
              options={[
                { value: "LOW", label: "Low" },
                { value: "MEDIUM", label: "Medium" },
                { value: "HIGH", label: "High" },
              ]}
              ariaLabel="Priority"
            />
          </label>
        </div>

        {!task && (
          <>
            {isManager && (
              <section style={{ display: "grid", gap: ".6rem" }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: ".75rem",
                  }}
                >
                  <h3>Team members</h3>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    icon={<Plus size={15} />}
                    onClick={() => setShowTeamMembers((show) => !show)}
                  >
                    {showTeamMembers ? "Hide members" : "Add members"}
                  </Button>
                </div>

                {showTeamMembers && (
                  <div
                    style={{
                      display: "grid",
                      gap: ".75rem",
                      padding: ".75rem",
                      border: "1px solid var(--border-light)",
                      borderRadius: 12,
                    }}
                  >
                    <PeoplePicker
                      label="Select team members"
                      users={users}
                      value={form.memberUserIds}
                      onChange={setMembers}
                      leadUserId={form.leadUserId}
                      error={fieldErrors.members}
                    />

                    <label className="field-label">
                      Lead
                      <Select
                        value={form.leadUserId?.toString() || "NONE"}
                        onValueChange={(value) =>
                          setForm((current) => ({
                            ...current,
                            leadUserId:
                              value === "NONE" ? undefined : Number(value),
                          }))
                        }
                        options={[
                          { value: "NONE", label: "No lead" },
                          ...users
                            .filter((u) => form.memberUserIds.includes(u.id))
                            .map((u) => ({
                              value: String(u.id),
                              label: `${u.firstName} ${u.lastName}`,
                            })),
                        ]}
                        ariaLabel="Lead"
                      />
                    </label>
                  </div>
                )}

                {!showTeamMembers && form.memberUserIds.length > 0 && (
                  <div
                    style={{
                      display: "flex",
                      flexWrap: "wrap",
                      gap: ".5rem",
                      alignItems: "center",
                    }}
                  >
                    {form.memberUserIds.map((memberId) => {
                      const member = users.find((user) => user.id === memberId);
                      if (!member) return null;
                      const isLead = memberId === form.leadUserId;
                      return (
                        <span
                          key={memberId}
                          style={{
                            display: "inline-flex",
                            alignItems: "center",
                            gap: ".35rem",
                            padding: ".35rem .7rem",
                            borderRadius: 999,
                            border: "1px solid var(--primary)",
                            background: isLead
                              ? "var(--primary-light)"
                              : "var(--surface, #f8fafc)",
                            color: "var(--primary)",
                            fontSize: isLead ? ".92rem" : ".82rem",
                            fontWeight: isLead ? 600 : 400,
                          }}
                          title={isLead ? "Team lead" : undefined}
                        >
                          {member.firstName} {member.lastName}
                        </span>
                      );
                    })}
                  </div>
                )}
              </section>
            )}

            <section style={{ display: "grid", gap: ".6rem" }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  gap: ".75rem",
                }}
              >
                <h3>Subtasks</h3>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  icon={<Plus size={15} />}
                  onClick={() =>
                    setSubtasks((items) => [
                      ...items,
                      {
                        title: "",
                        description: "",
                        deadline: "",
                        assigneeUserIds:
                          !isManager && currentUser ? [currentUser.id] : [],
                        files: [],
                        showFiles: false,
                      },
                    ])
                  }
                >
                  Add subtask
                </Button>
              </div>
              {subtasks.map((subtask, index) => (
                <div
                  key={index}
                  style={{
                    display: "grid",
                    gap: ".5rem",
                    padding: ".75rem",
                    border: "1px solid var(--border-light)",
                    borderRadius: 12,
                  }}
                >
                  <div style={{ display: "flex", gap: ".5rem" }}>
                    <div style={{ flex: 1 }}>
                      <Input
                        label=""
                        className="form-input"
                        id={`task-field-subtask-title-${index}`}
                        placeholder="Subtask title"
                        value={subtask.title}
                        onChange={(e) => {
                          setSubtasks((items) =>
                            items.map((item, i) =>
                              i === index
                                ? { ...item, title: e.target.value }
                                : item,
                            ),
                          );
                          clearFieldError(`subtask-title-${index}`);
                        }}
                        onBlur={() => validateField(`subtask-title-${index}`)}
                        error={fieldErrors[`subtask-title-${index}`]}
                      />
                    </div>
                    <IconButton
                      data-action="delete"
                      aria-label="Remove Subtask"
                      title="Remove Subtask"
                      motion="delete"
                      icon={<Trash2 size={16} />}
                      onClick={() => {
                        setSubtasks((items) =>
                          items.filter((_, i) => i !== index),
                        );
                        setFieldErrors((current) => {
                          const next: Record<string, string> = {};
                          Object.entries(current).forEach(([key, value]) => {
                            if (!key.startsWith("subtask-")) next[key] = value;
                            else {
                              const match = key.match(
                                /^subtask-(title|deadline|assignees)-(\d+)$/,
                              );
                              if (match && Number(match[2]) < index)
                                next[key] = value;
                              if (match && Number(match[2]) > index)
                                next[
                                  `subtask-${match[1]}-${Number(match[2]) - 1}`
                                ] = value;
                            }
                          });
                          return next;
                        });
                      }}
                    />
                  </div>
                  <textarea
                    className="form-input"
                    style={{ alignContent: "center" }}
                    placeholder="Optional description"
                    value={subtask.description}
                    onChange={(e) =>
                      setSubtasks((items) =>
                        items.map((item, i) =>
                          i === index
                            ? { ...item, description: e.target.value }
                            : item,
                        ),
                      )
                    }
                  />
                  <div>
                    <Input
                      label=""
                      className="form-input"
                      type="date"
                      id={`task-field-subtask-deadline-${index}`}
                      value={subtask.deadline}
                      onChange={(e) => {
                        setSubtasks((items) =>
                          items.map((item, i) =>
                            i === index
                              ? { ...item, deadline: e.target.value }
                              : item,
                          ),
                        );
                        clearFieldError(`subtask-deadline-${index}`);
                      }}
                      onBlur={() => validateField(`subtask-deadline-${index}`)}
                      error={fieldErrors[`subtask-deadline-${index}`]}
                    />
                  </div>
                  {isManager && (
                    <div>
                      <PeoplePicker
                        label="Assignees"
                        users={users}
                        value={subtask.assigneeUserIds}
                        onChange={(ids) => {
                          setSubtasks((items) =>
                            items.map((item, i) =>
                              i === index
                                ? { ...item, assigneeUserIds: ids }
                                : item,
                            ),
                          );
                          clearFieldError(`subtask-assignees-${index}`);
                        }}
                        error={fieldErrors[`subtask-assignees-${index}`]}
                      />
                    </div>
                  )}
                  <div style={{ display: "flex", justifyContent: "flex-end" }}>
                    <div style={{ marginTop: ".25rem" }}>
                      <FileDropzone
                        buttonMode
                        target="pending"
                        pendingFiles={subtask.files}
                        onPendingFilesChange={(files) =>
                          setSubtasks((items) =>
                            items.map((item, i) =>
                              i === index ? { ...item, files } : item,
                            ),
                          )
                        }
                      />
                    </div>
                  </div>
                </div>
              ))}
            </section>

            <section style={{ display: "grid", gap: ".5rem" }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  gap: ".75rem",
                }}
              >
                <h3>Checkpoints</h3>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  icon={<Plus size={15} />}
                  onClick={() => setCheckpoints((items) => [...items, ""])}
                >
                  Add checkpoint
                </Button>
              </div>
              {checkpoints.map((checkpoint, index) => (
                <div
                  key={index}
                  style={{
                    display: "flex",
                    gap: ".5rem",
                    alignItems: "center",
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <Input
                      label=""
                      className="form-input"
                      id={`task-field-checkpoint-${index}`}
                      value={checkpoint}
                      placeholder="Checkpoint title"
                      onChange={(e) => {
                        clearFieldError(`checkpoint-${index}`);
                        setCheckpoints((items) =>
                          items.map((item, i) =>
                            i === index ? e.target.value : item,
                          ),
                        );
                      }}
                      onBlur={() => validateField(`checkpoint-${index}`)}
                      error={fieldErrors[`checkpoint-${index}`]}
                    />
                  </div>
                  <IconButton
                    data-action="delete"
                    aria-label="Remove Checkpoint"
                    title="Remove Checkpoint"
                    motion="delete"
                    icon={<Trash2 size={20} />}
                    onClick={() =>
                      setCheckpoints((items) =>
                        items.filter((_, i) => i !== index),
                      )
                    }
                  />
                </div>
              ))}
            </section>

            <section className="files-section">
  <h3 style={{ margin: 0 }}>Files</h3>

  <FileDropzone
    buttonMode
    target="pending"
    pendingFiles={pendingFiles}
    onPendingFilesChange={setPendingFiles}
  />
</section>
          </>
        )}

        <FieldError
          message={
            noChanges
              ? "No changes were made. Change at least one field before saving."
              : undefined
          }
        />
        {error && <ErrorNotice error={error} />}
        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            gap: "0.75rem",
          }}
        >
          <Button type="button" variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" isLoading={saving}>
            {task ? "Save changes" : "Create task"}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
