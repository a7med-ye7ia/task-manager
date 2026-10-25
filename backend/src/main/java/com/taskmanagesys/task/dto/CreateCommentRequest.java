package com.taskmanagesys.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateCommentRequest {
    @NotBlank @Size(min = 1, max = 2000)
    private String body;
    private Long subtaskId;
    private List<Long> mentionedUserIds;
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Long getSubtaskId() { return subtaskId; }
    public void setSubtaskId(Long subtaskId) { this.subtaskId = subtaskId; }
    public List<Long> getMentionedUserIds() { return mentionedUserIds; }
    public void setMentionedUserIds(List<Long> mentionedUserIds) { this.mentionedUserIds = mentionedUserIds; }
}
