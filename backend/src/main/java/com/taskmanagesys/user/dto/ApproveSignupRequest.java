package com.taskmanagesys.user.dto;

import com.taskmanagesys.user.model.Role;

public class ApproveSignupRequest {
    private Role role = Role.EMPLOYEE;

    public ApproveSignupRequest() {}

    public ApproveSignupRequest(Role role) {
        this.role = role != null ? role : Role.EMPLOYEE;
    }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role != null ? role : Role.EMPLOYEE; }
}
