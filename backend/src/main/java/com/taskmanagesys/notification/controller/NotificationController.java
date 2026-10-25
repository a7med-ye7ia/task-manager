package com.taskmanagesys.notification.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.notification.dto.NotificationResponse;
import com.taskmanagesys.notification.repository.NotificationRepository;
import com.taskmanagesys.notification.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository notifications;
    private final NotificationService notificationService;
    public NotificationController(NotificationRepository notifications, NotificationService notificationService) { this.notifications=notifications; this.notificationService=notificationService; }
    @GetMapping public List<NotificationResponse> list(@AuthenticationPrincipal UserPrincipal user) { return notifications.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0,20)).stream().map(NotificationResponse::from).toList(); }
    @GetMapping("/unread-count") public long unread(@AuthenticationPrincipal UserPrincipal user) { return notifications.countByUserIdAndReadFalse(user.getId()); }
    @PostMapping("/{id}/read") public void read(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) { notifications.findByIdAndUserId(id,user.getId()).ifPresent(n -> { n.setRead(true); notifications.save(n); }); }
    @PostMapping("/read-all") public void readAll(@AuthenticationPrincipal UserPrincipal user) { notificationService.markAllRead(user.getId()); }
    @DeleteMapping public ResponseEntity<Void> clearAll(@AuthenticationPrincipal UserPrincipal user) { notificationService.clearAll(user.getId()); return ResponseEntity.noContent().build(); }
}
