package com.taskmanagesys.task.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.model.TaskMemberRole;
import com.taskmanagesys.task.repository.SubtaskAssigneeRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TaskAccessService {
    private final UserRepository users; private final TaskMemberRepository members; private final SubtaskAssigneeRepository assignees; private final SubtaskRepository subtasks;
    public TaskAccessService(UserRepository users, TaskMemberRepository members, SubtaskAssigneeRepository assignees, SubtaskRepository subtasks) { this.users = users; this.members = members; this.assignees = assignees; this.subtasks = subtasks; }
    public boolean manager(Long userId) { return users.findById(userId).map(u -> u.getRole() == Role.MANAGER).orElse(false); }
    public boolean lead(Long taskId, Long userId) { return members.findByTaskIdAndUserId(taskId, userId).map(m -> m.getRole() == TaskMemberRole.LEAD).orElse(false); }
    public boolean member(Long taskId, Long userId) { return members.existsByTaskIdAndUserId(taskId, userId); }
    public boolean managerOrLead(Long taskId, Long userId) { return manager(userId) || lead(taskId, userId); }
    public void requireManager(Long userId) { if (!manager(userId)) deny(); }
    public void requireManagerOrLead(Long taskId, Long userId) { if (!managerOrLead(taskId, userId)) deny(); }
    public void requireMember(Long taskId, Long userId) { if (!manager(userId) && !member(taskId, userId)) deny(); }
    public void requireSubtaskAssigneeOrLead(Long taskId, Long subtaskId, Long userId) { if (subtasks.findById(subtaskId).map(s -> !s.getTask().getId().equals(taskId)).orElse(true)) deny(); if (!managerOrLead(taskId, userId) && !assignees.existsBySubtaskIdAndUserId(subtaskId, userId)) deny(); }
    private void deny() { throw new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "You do not have permission to perform this action on this task"); }
}
