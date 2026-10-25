package com.taskmanagesys.user.dto;

import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;

import java.time.LocalDate;

public class UserProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private String avatarPath;
    private LocalDate hiredAt;
    private ManagerEmailPrefsDto emailPreferences;

    public UserProfileResponse() {}

    public static UserProfileResponse from(User user, ManagerEmailPrefsDto emailPreferences) {
        UserProfileResponse res = new UserProfileResponse();
        res.setId(user.getId());
        res.setFirstName(user.getFirstName());
        res.setLastName(user.getLastName());
        res.setEmail(user.getEmail());
        res.setPhone(user.getPhone());
        res.setRole(user.getRole());
        res.setStatus(user.getStatus());
        res.setAvatarPath(user.getAvatarPath());
        res.setHiredAt(user.getHiredAt());
        res.setEmailPreferences(emailPreferences);
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public LocalDate getHiredAt() { return hiredAt; }
    public void setHiredAt(LocalDate hiredAt) { this.hiredAt = hiredAt; }

    public ManagerEmailPrefsDto getEmailPreferences() { return emailPreferences; }
    public void setEmailPreferences(ManagerEmailPrefsDto emailPreferences) { this.emailPreferences = emailPreferences; }
}
