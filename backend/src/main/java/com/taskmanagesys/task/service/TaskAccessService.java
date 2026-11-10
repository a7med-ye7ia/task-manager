package com.taskmanagesys.task.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskMemberRole;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.SubtaskAssigneeRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TaskAccessService {
    private final UserRepository users;
    private final TaskMemberRepository members;
    private final SubtaskAssigneeRepository assignees;
    private final SubtaskRepository subtasks;

    public TaskAccessService(UserRepository users, TaskMemberRepository members, SubtaskAssigneeRepository assignees, SubtaskRepository subtasks) {
        this.users = users;
        this.members = members;
        this.assignees = assignees;
        this.subtasks = subtasks;
    }

    public boolean manager(Long userId) {
        return users.findById(userId).map(u -> u.getRole() == Role.MANAGER).orElse(false);
    }

    public boolean lead(Long taskId, Long userId) {
        return members.findByTaskIdAndUserId(taskId, userId).map(m -> m.getRole() == TaskMemberRole.LEAD).orElse(false);
    }

    public boolean member(Long taskId, Long userId) {
        return members.existsByTaskIdAndUserId(taskId, userId);
    }

    public boolean managerOrLead(Long taskId, Long userId) {
        return manager(userId) || lead(taskId, userId);
    }

    public void requireManager(Long userId) {
        if (!manager(userId)) deny();
    }

    public void requireManagerOrLead(Long taskId, Long userId) {
        if (!managerOrLead(taskId, userId)) deny();
    }

    public void requireMember(Long taskId, Long userId) {
        if (!manager(userId) && !member(taskId, userId)) deny();
    }

    public void requireSubtaskAssigneeOrLead(Long taskId, Long subtaskId, Long userId) {
        if (subtasks.findById(subtaskId).map(s -> !s.getTask().getId().equals(taskId)).orElse(true)) deny();
        if (!managerOrLead(taskId, userId) && !assignees.existsBySubtaskIdAndUserId(subtaskId, userId)) deny();
    }

    public boolean canMarkDone(Task task, Long userId) {
        if (task.isSelfReported()) {
            return manager(userId);
        }
        return managerOrLead(task.getId(), userId);
    }

    public void requireDonePermission(Task task, Long userId) {
        if (!canMarkDone(task, userId)) deny();
    }

    public boolean canSendBack(Task task, Long userId) {
        if (task.isSelfReported()) {
            return manager(userId);
        }
        return managerOrLead(task.getId(), userId);
    }

    public void requireSendBackPermission(Task task, Long userId) {
        if (!canSendBack(task, userId)) deny();
    }

    public boolean canRestore(Task task, Long userId) {
        if (task.isSelfReported()) {
            return manager(userId);
        }
        return managerOrLead(task.getId(), userId);
    }

    public void requireRestorePermission(Task task, Long userId) {
        if (!canRestore(task, userId)) deny();
    }

    public boolean canDelete(Task task, Long userId, boolean hasTimeSessions) {
        if (manager(userId)) return true;
        if (task.isSelfReported() && task.getCreatedBy() != null && task.getCreatedBy().getId().equals(userId)) {
            return task.getStatus() == TaskStatus.TODO && !hasTimeSessions;
        }
        return false;
    }

    public void requireDeletePermission(Task task, Long userId, boolean hasTimeSessions) {
        if (!canDelete(task, userId, hasTimeSessions)) deny();
    }

    private void deny() {
        throw new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "You do not have permission to perform this action on this task");
    }
}
