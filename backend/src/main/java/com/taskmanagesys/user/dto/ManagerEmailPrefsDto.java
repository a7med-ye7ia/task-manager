package com.taskmanagesys.user.dto;

import com.taskmanagesys.user.model.ManagerEmailPrefs;

public class ManagerEmailPrefsDto {
    private boolean emailsEnabled;
    private boolean onSignupRequest;
    private boolean onTaskCompleted;
    private boolean onArchiveWarning;

    public ManagerEmailPrefsDto() {}

    public ManagerEmailPrefsDto(boolean emailsEnabled, boolean onSignupRequest, boolean onTaskCompleted, boolean onArchiveWarning) {
        this.emailsEnabled = emailsEnabled;
        this.onSignupRequest = onSignupRequest;
        this.onTaskCompleted = onTaskCompleted;
        this.onArchiveWarning = onArchiveWarning;
    }

    public static ManagerEmailPrefsDto from(ManagerEmailPrefs prefs) {
        if (prefs == null) return null;
        return new ManagerEmailPrefsDto(
                prefs.isEmailsEnabled(),
                prefs.isOnSignupRequest(),
                prefs.isOnTaskCompleted(),
                prefs.isOnArchiveWarning()
        );
    }

    public boolean isEmailsEnabled() { return emailsEnabled; }
    public void setEmailsEnabled(boolean emailsEnabled) { this.emailsEnabled = emailsEnabled; }

    public boolean isOnSignupRequest() { return onSignupRequest; }
    public void setOnSignupRequest(boolean onSignupRequest) { this.onSignupRequest = onSignupRequest; }

    public boolean isOnTaskCompleted() { return onTaskCompleted; }
    public void setOnTaskCompleted(boolean onTaskCompleted) { this.onTaskCompleted = onTaskCompleted; }

    public boolean isOnArchiveWarning() { return onArchiveWarning; }
    public void setOnArchiveWarning(boolean onArchiveWarning) { this.onArchiveWarning = onArchiveWarning; }
}
