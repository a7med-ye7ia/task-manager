import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi, beforeEach } from "vitest";

const api = vi.hoisted(() => ({
  getTaskPoolApi: vi.fn(), getAdminUsersApi: vi.fn(), getMentionUnreadCountApi: vi.fn(), createTaskApi: vi.fn(),
  getTaskApi: vi.fn(), getTaskUnreadApi: vi.fn(), getAdminUsersApiDetail: vi.fn(), restoreTaskApi: vi.fn(),
}));
vi.mock("../services/api", () => ({ ...api, getAdminUsersApi: api.getAdminUsersApi, getTaskPoolApi: api.getTaskPoolApi, getMentionUnreadCountApi: api.getMentionUnreadCountApi, createTaskApi: api.createTaskApi, getTaskApi: api.getTaskApi, getTaskUnreadApi: api.getTaskUnreadApi, restoreTaskApi: api.restoreTaskApi }));
vi.mock("../context/AuthContext", () => ({ useAuth: () => ({ isManager: true, user: { id: 1, firstName: "Manager", lastName: "User" }, pendingCount: 0 }) }));

import { TaskPoolPage } from "./TaskPoolPage";
import { TaskDetailPage } from "./TaskDetailPage";

const archivedTask = { id: 9, title: "Archived task", description: null, deadline: "2026-12-01", deadlineManual: false, overdue: false, priority: "MEDIUM", status: "DONE", progress: 100, lead: { id: 1, firstName: "Manager", lastName: "User", role: "MANAGER" }, members: [{ user: { id: 1, firstName: "Manager", lastName: "User", role: "MANAGER" }, role: "LEAD" }], subtasks: [], checkpoints: [], purgeAt: "2026-12-01T00:00:00Z", createdAt: "2026-10-01T00:00:00Z" };

beforeEach(() => {
  vi.clearAllMocks();
  api.getMentionUnreadCountApi.mockResolvedValue({ count: 0 });
  api.getAdminUsersApi.mockResolvedValue({ content: [] });
  api.getTaskUnreadApi.mockResolvedValue({ chatUnread: false, activityUnread: false, chatCount: 0, activityCount: 0 });
});

describe("archive UI", () => {
  it("selects the Archived tab from the URL", async () => {
    api.getTaskPoolApi.mockResolvedValue({ content: [], totalElements: 0, totalPages: 1 });
    render(<MemoryRouter initialEntries={["/tasks?tab=archived"]}><TaskPoolPage /></MemoryRouter>);
    expect(screen.getByRole("tab", { name: "Archived" })).toHaveAttribute("aria-selected", "true");
    await waitFor(() => expect(api.getTaskPoolApi).toHaveBeenCalledWith(expect.objectContaining({ archived: true })));
  });

  it("opens restore dialog only after clicking Restore", async () => {
    api.getTaskApi.mockResolvedValue(archivedTask);
    render(<MemoryRouter initialEntries={["/tasks/9"]}><TaskDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole("button", { name: "Restore" })).toBeInTheDocument());
    expect(screen.queryByRole("combobox", { name: "Restore status" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Restore" }));
    expect(screen.getByRole("combobox", { name: "Restore status" })).toBeInTheDocument();
  });
});
