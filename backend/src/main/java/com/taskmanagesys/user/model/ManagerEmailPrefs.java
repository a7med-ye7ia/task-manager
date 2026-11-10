package com.taskmanagesys.user.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "manager_email_prefs")
public class ManagerEmailPrefs implements Persistable<Long> {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "emails_enabled", nullable = false)
    private boolean emailsEnabled = true;

    @Column(name = "on_signup_request", nullable = false)
    private boolean onSignupRequest = true;

    @Column(name = "on_task_completed", nullable = false)
    private boolean onTaskCompleted = false;

    @Column(name = "on_archive_warning", nullable = false)
    private boolean onArchiveWarning = false;

    @Column(name = "on_self_reported", nullable = false)
    private boolean onSelfReported = true;

    @Transient
    private boolean isNew = true;

    public ManagerEmailPrefs() {}

    public ManagerEmailPrefs(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
        this.emailsEnabled = true;
        this.onSignupRequest = true;
        this.onTaskCompleted = false;
        this.onArchiveWarning = false;
        this.onSelfReported = true;
        this.isNew = true;
    }

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public boolean isEmailsEnabled() {
        return emailsEnabled;
    }

    public void setEmailsEnabled(boolean emailsEnabled) {
        this.emailsEnabled = emailsEnabled;
    }

    public boolean isOnSignupRequest() {
        return onSignupRequest;
    }

    public void setOnSignupRequest(boolean onSignupRequest) {
        this.onSignupRequest = onSignupRequest;
    }

    public boolean isOnTaskCompleted() {
        return onTaskCompleted;
    }

    public void setOnTaskCompleted(boolean onTaskCompleted) {
        this.onTaskCompleted = onTaskCompleted;
    }

    public boolean isOnArchiveWarning() {
        return onArchiveWarning;
    }

    public void setOnArchiveWarning(boolean onArchiveWarning) {
        this.onArchiveWarning = onArchiveWarning;
    }

    public boolean isOnSelfReported() {
        return onSelfReported;
    }

    public void setOnSelfReported(boolean onSelfReported) {
        this.onSelfReported = onSelfReported;
    }
}
