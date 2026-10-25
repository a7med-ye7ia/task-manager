package com.taskmanagesys.user.dto;

public class RejectSignupRequest {
    private String reason;

    public RejectSignupRequest() {}

    public RejectSignupRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
