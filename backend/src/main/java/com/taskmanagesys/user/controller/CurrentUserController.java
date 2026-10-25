package com.taskmanagesys.user.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.user.dto.UserDto;
import com.taskmanagesys.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class CurrentUserController {
    private final UserService users;
    public CurrentUserController(UserService users) { this.users = users; }
    @DeleteMapping("/avatar")
    public ResponseEntity<UserDto> removeAvatar(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(UserDto.from(users.removeAvatar(principal.getId())));
    }
}
