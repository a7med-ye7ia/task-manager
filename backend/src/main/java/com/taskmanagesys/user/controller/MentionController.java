package com.taskmanagesys.user.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.task.dto.MentionCountResponse;
import com.taskmanagesys.task.service.MentionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mentions")
public class MentionController {
    private final MentionService mentions;
    public MentionController(MentionService mentions) { this.mentions = mentions; }
    @GetMapping("/unread") public MentionCountResponse unread(@AuthenticationPrincipal UserPrincipal user) { return new MentionCountResponse(mentions.totalUnseen(user.getId())); }
}
