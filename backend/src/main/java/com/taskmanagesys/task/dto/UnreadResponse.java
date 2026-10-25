package com.taskmanagesys.task.dto;

public record UnreadResponse(boolean chatUnread, boolean activityUnread, long chatCount, long activityCount) {}
