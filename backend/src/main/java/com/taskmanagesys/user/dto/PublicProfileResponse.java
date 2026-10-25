package com.taskmanagesys.user.dto;

import com.taskmanagesys.user.model.Role;

public record PublicProfileResponse(Long id, String firstName, String lastName, String phone, Role role, String avatarPath, boolean active) {}
