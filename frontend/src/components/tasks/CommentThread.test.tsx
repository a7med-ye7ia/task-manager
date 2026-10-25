import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { CommentThread } from "./CommentThread";
import * as api from "../../services/api";

vi.mock("../../services/api", async () => {
  const actual = await vi.importActual<typeof import("../../services/api")>("../../services/api");
  return { ...actual, getTaskCommentsApi: vi.fn(), createTaskCommentApi: vi.fn(), updateTaskCommentApi: vi.fn(), deleteTaskCommentApi: vi.fn() };
});

const member = (id: number, firstName: string, lastName: string) => ({ id, firstName, lastName, email: `${id}@example.com`, phone: "", role: "EMPLOYEE" as const, status: "ACTIVE" as const, avatarPath: null });

describe("CommentThread profile links", () => {
  it("does not link the signed-in author's avatar or name", async () => {
    vi.mocked(api.getTaskCommentsApi).mockResolvedValue([{ id: 1, taskId: 10, authorId: 7, authorName: "Current User", authorAvatarPath: null, body: "Hello", deleted: false, edited: false, createdAt: new Date().toISOString(), mentionedUserIds: [], mentionedUsers: [] }]);
    render(<MemoryRouter><CommentThread taskId={10} subtasks={[]} currentUserId={7} canWrite={false} users={[member(7, "Current", "User")]}/></MemoryRouter>);
    await waitFor(() => expect(screen.getByText("Current User")).toBeInTheDocument());
    expect(screen.getByText("Current User").closest("a")).toBeNull();
  });

  it("renders stored-id mentions as profile links", async () => {
    vi.mocked(api.getTaskCommentsApi).mockResolvedValue([{ id: 2, taskId: 10, authorId: 8, authorName: "Other User", authorAvatarPath: null, body: "Hi @Shared Name", deleted: false, edited: false, createdAt: new Date().toISOString(), mentionedUserIds: [9], mentionedUsers: [{ id: 9, firstName: "Shared", lastName: "Name", avatarPath: null, active: true }] }]);
    render(<MemoryRouter><CommentThread taskId={10} subtasks={[]} currentUserId={7} canWrite={false} users={[member(7, "Current", "User"), member(8, "Other", "User")]}/></MemoryRouter>);
    await waitFor(() => expect(screen.getByText("@Shared Name")).toBeInTheDocument());
    expect(screen.getByText("@Shared Name").closest("a")).toHaveAttribute("href", "/users/9");
  });
});
