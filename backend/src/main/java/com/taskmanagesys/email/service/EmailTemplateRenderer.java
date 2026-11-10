package com.taskmanagesys.email.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EmailTemplateRenderer {

    private final String appBaseUrl;

    public EmailTemplateRenderer(@Value("${app.base-url:http://localhost:5173}") String appBaseUrl) {
        this.appBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
    }

    public record RenderedEmail(String subject, String html, String plainText) {}

    public RenderedEmail render(String template, Map<String, Object> payload) {
        if (template == null || template.trim().isEmpty()) {
            throw new IllegalArgumentException("Email template cannot be null or empty");
        }
        Map<String, Object> data = payload != null ? payload : Map.of();
        String normalized = template.trim().toUpperCase().replace('-', '_');

        return switch (normalized) {
            case "ACCOUNT_INVITE" -> renderAccountInvite(data);
            case "VERIFY_EMAIL" -> renderVerifyEmail(data);
            case "CONFIRM_NEW_EMAIL" -> renderConfirmNewEmail(data);
            case "NEW_SIGNUP_REQUEST", "SIGNUP_REQUEST" -> renderNewSignupRequest(data);
            case "SIGNUP_APPROVED" -> renderSignupApproved(data);
            case "SIGNUP_REJECTED" -> renderSignupRejected(data);
            case "PASSWORD_RESET" -> renderPasswordReset(data);
            case "PASSWORD_CHANGED" -> renderPasswordChanged(data);
            case "ACCOUNT_ALREADY_EXISTS" -> renderAccountAlreadyExists(data);
            case "TASK_ASSIGNED" -> renderTaskAssigned(data);
            case "SUBTASK_ASSIGNED" -> renderSubtaskAssigned(data);
            case "SUBTASK_DONE" -> renderSubtaskDone(data);
            case "TASK_REVIEW" -> renderTaskReview(data);
            case "TASK_DONE" -> renderTaskDone(data);
            case "TASK_SENT_BACK" -> renderTaskSentBack(data);
            case "TASK_RESTORED" -> renderTaskRestored(data);
            case "ARCHIVE_WARNING" -> renderArchiveWarning(data);
            case "LEAD_VACANT" -> renderLeadVacant(data);
            case "MENTION" -> renderMention(data);
            case "SELF_REPORTED_CREATED" -> renderSelfReportedCreated(data);
            case "SELF_REPORTED_REVIEW" -> renderSelfReportedReview(data);
            default -> throw new IllegalArgumentException("Unknown email template: " + template);
        };
    }

    private RenderedEmail renderAccountInvite(Map<String, Object> data) {
        String token = (String) data.getOrDefault("token", "");
        String link = appBaseUrl + "/set-password?token=" + token;
        String subject = "Account invitation: Task Management System";
        String heading = "Welcome to Task Management System";
        String sentence = "You have been invited by a Manager to join the Task Management System.";
        Map<String, String> details = new LinkedHashMap<>();
        if (data.containsKey("email")) details.put("Email", String.valueOf(data.get("email")));
        if (data.containsKey("role")) details.put("Role", String.valueOf(data.get("role")));
        details.put("Link validity", "48 hours");
        return build(subject, heading, sentence, details, link, "Set Your Password", "This invitation link is valid for 48 hours. If you did not expect this invitation, please ignore this email.");
    }

    private RenderedEmail renderVerifyEmail(Map<String, Object> data) {
        String token = (String) data.getOrDefault("token", "");
        String link = appBaseUrl + "/verify-email?token=" + token;
        String subject = "Verify your email: Task Management System";
        String heading = "Verify Your Email Address";
        String sentence = "Please verify your email address to submit your registration for Manager approval.";
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Link validity", "24 hours");
        return build(subject, heading, sentence, details, link, "Verify Email", "This verification link is valid for 24 hours.");
    }

    private RenderedEmail renderConfirmNewEmail(Map<String, Object> data) {
        String token = (String) data.getOrDefault("token", "");
        String link = appBaseUrl + "/verify-email?token=" + token;
        String subject = "Confirm your new email: Task Management System";
        String heading = "Confirm New Email Address";
        String sentence = "A request was made to update your email address. Please click below to confirm this address.";
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Link validity", "24 hours");
        return build(subject, heading, sentence, details, link, "Confirm New Email", "This link is valid for 24 hours. If you did not request this change, please contact a Manager immediately.");
    }

    private RenderedEmail renderNewSignupRequest(Map<String, Object> data) {
        String name = String.valueOf(data.getOrDefault("name", "An applicant"));
        String email = String.valueOf(data.getOrDefault("email", ""));
        String link = appBaseUrl + "/admin/pending";
        String subject = "New sign-up request: " + name;
        String heading = "New Sign-up Request";
        String sentence = name + " has verified their email and is waiting for account approval.";
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Applicant", name);
        if (!email.isBlank()) details.put("Email", email);
        return build(subject, heading, sentence, details, link, "Review Requests", "Review and approve or reject in the Manager portal.");
    }

    private RenderedEmail renderSignupApproved(Map<String, Object> data) {
        String link = appBaseUrl + "/login";
        String subject = "Your sign-up request was approved: Task Management System";
        String heading = "Account Approved";
        String sentence = "A Manager has reviewed and approved your sign-up request. You can now sign in.";
        return build(subject, heading, sentence, Map.of(), link, "Sign In to Your Account", "Welcome to the team!");
    }

    private RenderedEmail renderSignupRejected(Map<String, Object> data) {
        String reason = (String) data.get("reason");
        String subject = "Your sign-up request was rejected: Task Management System";
        String heading = "Registration Request Reviewed";
        String sentence = "A Manager reviewed your sign-up request and decided not to approve it.";
        Map<String, String> details = new LinkedHashMap<>();
        if (reason != null && !reason.trim().isEmpty()) {
            details.put("Reason", reason.trim());
        }
        return build(subject, heading, sentence, details, appBaseUrl + "/login", "Go to Sign In", "If you believe this was an error, please reach out to your manager.");
    }

    private RenderedEmail renderPasswordReset(Map<String, Object> data) {
        String token = (String) data.getOrDefault("token", "");
        String link = appBaseUrl + "/reset-password?token=" + token;
        String subject = "Password reset request: Task Management System";
        String heading = "Reset Your Password";
        String sentence = "A password reset was requested for your account. Click the button below to choose a new password.";
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Link validity", "30 minutes");
        return build(subject, heading, sentence, details, link, "Reset Password", "This link is valid for 30 minutes. If you did not request this, please ignore this email.");
    }

    private RenderedEmail renderPasswordChanged(Map<String, Object> data) {
        String subject = "Your password was changed: Task Management System";
        String heading = "Password Changed Successfully";
        String sentence = "The password for your account has been changed successfully. All other active sessions have been signed out.";
        return build(subject, heading, sentence, Map.of(), appBaseUrl + "/login", "Sign In", "If you did not make this change, please contact a Manager immediately.");
    }

    private RenderedEmail renderAccountAlreadyExists(Map<String, Object> data) {
        String subject = "Security notification: sign-up attempt";
        String heading = "Sign-up Attempt Detected";
        String sentence = "Someone attempted to sign up for an account using your email address.";
        return build(subject, heading, sentence, Map.of(), appBaseUrl + "/login", "Go to Sign In", "If this was you, you can sign in directly or reset your password. If not, no action is required.");
    }

    private RenderedEmail renderTaskAssigned(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Untitled Task")));
        String actorName = String.valueOf(data.getOrDefault("actorName", "A manager"));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "New task assigned: " + title;
        String heading = "New Task Assignment";
        String sentence = actorName + " added you to the task “" + title + "”.";
        Map<String, String> details = buildTaskDetails(data, title);
        return build(subject, heading, sentence, details, link, "View Task", null);
    }

    private RenderedEmail renderSubtaskAssigned(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("subtaskTitle", "Subtask")));
        String taskTitle = String.valueOf(data.getOrDefault("taskTitle", "Task"));
        String actorName = String.valueOf(data.getOrDefault("actorName", "A team member"));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "New subtask assigned: " + title;
        String heading = "New Subtask Assignment";
        String sentence = actorName + " assigned you to subtask “" + title + "”.";
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Subtask", title);
        if (data.containsKey("taskTitle")) details.put("Task", taskTitle);
        if (data.containsKey("priority")) details.put("Priority", String.valueOf(data.get("priority")));
        if (data.containsKey("deadline")) details.put("Deadline", String.valueOf(data.get("deadline")));
        return build(subject, heading, sentence, details, link, "View Task", null);
    }

    private RenderedEmail renderSubtaskDone(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("subtaskTitle", "Subtask")));
        String actorName = String.valueOf(data.getOrDefault("actorName", "An assignee"));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "Subtask completed: " + title;
        String heading = "Subtask Completed";
        String sentence = actorName + " completed subtask “" + title + "”.";
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Subtask", title);
        if (data.containsKey("taskTitle")) details.put("Task", String.valueOf(data.get("taskTitle")));
        return build(subject, heading, sentence, details, link, "View Task", null);
    }

    private RenderedEmail renderTaskReview(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Task")));
        String actorName = String.valueOf(data.getOrDefault("actorName", "A team member"));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "Task ready for review: " + title;
        String heading = "Task in Review";
        String sentence = actorName + " moved task “" + title + "” to REVIEW.";
        Map<String, String> details = buildTaskDetails(data, title);
        return build(subject, heading, sentence, details, link, "Review Task", null);
    }

    private RenderedEmail renderTaskDone(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Task")));
        String actorName = String.valueOf(data.getOrDefault("actorName", "A manager"));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "Task completed: " + title;
        String heading = "Task Completed";
        String sentence = actorName + " marked task “" + title + "” as DONE.";
        Map<String, String> details = buildTaskDetails(data, title);
        return build(subject, heading, sentence, details, link, "View Task", null);
    }

    private RenderedEmail renderTaskSentBack(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Task")));
        String actorName = String.valueOf(data.getOrDefault("actorName", "A manager"));
        String reason = (String) data.get("reason");
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "Task sent back: " + title;
        String heading = "Task Sent Back";
        String sentence = actorName + " sent back task “" + title + "” with feedback.";
        Map<String, String> details = buildTaskDetails(data, title);
        if (reason != null && !reason.trim().isEmpty()) {
            details.put("Reason for send back", reason.trim());
        }
        return build(subject, heading, sentence, details, link, "View Task", null);
    }

    private RenderedEmail renderTaskRestored(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Task")));
        String actorName = String.valueOf(data.getOrDefault("actorName", "A manager"));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "Task restored: " + title;
        String heading = "Task Restored";
        String sentence = actorName + " restored task “" + title + "” from the archive.";
        Map<String, String> details = buildTaskDetails(data, title);
        return build(subject, heading, sentence, details, link, "View Task", null);
    }

    private RenderedEmail renderArchiveWarning(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Task")));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "Archive warning: " + title;
        String heading = "Task Scheduled for Permanent Deletion";
        String sentence = "Task “" + title + "” has been completed for 27 days and will be permanently deleted in 3 days.";
        Map<String, String> details = buildTaskDetails(data, title);
        return build(subject, heading, sentence, details, link, "View Task", "Restore the task before deletion if work is ongoing.");
    }

    private RenderedEmail renderLeadVacant(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Task")));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "Task has no Lead: " + title;
        String heading = "Task Lead Vacant";
        String sentence = "The Lead for task “" + title + "” was deactivated. A new Lead must be assigned.";
        Map<String, String> details = buildTaskDetails(data, title);
        return build(subject, heading, sentence, details, link, "Assign Lead", null);
    }

    private RenderedEmail renderMention(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Task")));
        String actorName = String.valueOf(data.getOrDefault("actorName", data.getOrDefault("authorName", "A team member")));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "You were mentioned in: " + title;
        String heading = "New Mention";
        String sentence = actorName + " mentioned you in a comment on “" + title + "”.";
        Map<String, String> details = buildTaskDetails(data, title);
        return build(subject, heading, sentence, details, link, "View Comment", null);
    }

    private RenderedEmail renderSelfReportedCreated(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Task")));
        String creatorName = String.valueOf(data.getOrDefault("creatorName", data.getOrDefault("actorName", "An employee")));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "Self-reported task created: " + title;
        String heading = "Self-Reported Task Created";
        String sentence = creatorName + " created a self-reported task “" + title + "”.";
        Map<String, String> details = buildTaskDetails(data, title);
        details.put("Creator", creatorName);
        return build(subject, heading, sentence, details, link, "View Task", null);
    }

    private RenderedEmail renderSelfReportedReview(Map<String, Object> data) {
        String title = String.valueOf(data.getOrDefault("title", data.getOrDefault("taskTitle", "Task")));
        String actorName = String.valueOf(data.getOrDefault("actorName", data.getOrDefault("creatorName", "The creator")));
        Object taskId = data.get("taskId");
        String link = taskId != null ? appBaseUrl + "/tasks/" + taskId : appBaseUrl + "/tasks";
        String subject = "Self-reported task ready for review: " + title;
        String heading = "Self-Reported Task in Review";
        String sentence = actorName + " moved self-reported task “" + title + "” to REVIEW.";
        Map<String, String> details = buildTaskDetails(data, title);
        return build(subject, heading, sentence, details, link, "Review Task", null);
    }

    private Map<String, String> buildTaskDetails(Map<String, Object> data, String title) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Task", title);
        if (data.containsKey("priority")) details.put("Priority", String.valueOf(data.get("priority")));
        if (data.containsKey("deadline")) details.put("Deadline", String.valueOf(data.get("deadline")));
        return details;
    }

    private RenderedEmail build(
            String subject,
            String heading,
            String sentence,
            Map<String, String> details,
            String buttonLink,
            String buttonText,
            String footerNote
    ) {
        String appName = "Task Management System";

        // HTML building with inline CSS and table layout (max-width 560px, rounded button, no emojis)
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>");
        html.append("<body style=\"margin:0; padding:20px; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color:#f8fafc; color:#1e293b; line-height:1.6;\">");
        html.append("<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width:560px; margin:0 auto; background-color:#ffffff; border:1px solid #e2e8f0; border-radius:8px; overflow:hidden;\">");

        // Header strip
        html.append("<tr><td style=\"padding:24px 28px 16px 28px; border-bottom:1px solid #f1f5f9;\">");
        html.append("<div style=\"font-size:13px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:#2563eb; margin-bottom:4px;\">").append(HtmlUtils.htmlEscape(appName)).append("</div>");
        html.append("<h2 style=\"margin:0; font-size:20px; font-weight:700; color:#0f172a;\">").append(HtmlUtils.htmlEscape(heading)).append("</h2>");
        html.append("</td></tr>");

        // Main content
        html.append("<tr><td style=\"padding:24px 28px;\">");
        html.append("<p style=\"margin:0 0 16px 0; font-size:15px; color:#334155;\">").append(HtmlUtils.htmlEscape(sentence)).append("</p>");

        // Details block table
        if (details != null && !details.isEmpty()) {
            html.append("<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"margin:20px 0; background-color:#f8fafc; border:1px solid #e2e8f0; border-radius:6px; font-size:14px;\">");
            for (Map.Entry<String, String> entry : details.entrySet()) {
                html.append("<tr>");
                html.append("<td style=\"padding:8px 12px; font-weight:600; color:#64748b; width:35%; border-bottom:1px solid #e2e8f0;\">").append(HtmlUtils.htmlEscape(entry.getKey())).append("</td>");
                html.append("<td style=\"padding:8px 12px; color:#0f172a; border-bottom:1px solid #e2e8f0;\">").append(HtmlUtils.htmlEscape(entry.getValue())).append("</td>");
                html.append("</tr>");
            }
            html.append("</table>");
        }

        // Primary Button
        if (buttonLink != null && buttonText != null) {
            html.append("<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:24px 0;\">");
            html.append("<tr><td>");
            html.append("<a href=\"").append(HtmlUtils.htmlEscape(buttonLink)).append("\" style=\"background-color:#2563eb; color:#ffffff; padding:12px 24px; text-decoration:none; border-radius:6px; font-weight:600; font-size:14px; display:inline-block;\">");
            html.append(HtmlUtils.htmlEscape(buttonText));
            html.append("</a>");
            html.append("</td></tr></table>");
        }

        if (footerNote != null && !footerNote.isBlank()) {
            html.append("<p style=\"margin:16px 0 0 0; font-size:13px; color:#64748b;\">").append(HtmlUtils.htmlEscape(footerNote)).append("</p>");
        }

        html.append("</td></tr>");

        // Footer
        html.append("<tr><td style=\"padding:16px 28px; background-color:#f8fafc; border-top:1px solid #f1f5f9; font-size:12px; color:#94a3b8; text-align:center;\">");
        html.append("Task Management System");
        html.append("</td></tr>");

        html.append("</table></body></html>");

        // Plain text building
        StringBuilder plain = new StringBuilder();
        plain.append(appName).append("\n");
        plain.append(heading).append("\n\n");
        plain.append(sentence).append("\n\n");
        if (details != null && !details.isEmpty()) {
            for (Map.Entry<String, String> entry : details.entrySet()) {
                plain.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            plain.append("\n");
        }
        if (buttonLink != null && buttonText != null) {
            plain.append(buttonText).append(": ").append(buttonLink).append("\n\n");
        }
        if (footerNote != null && !footerNote.isBlank()) {
            plain.append(footerNote).append("\n\n");
        }
        plain.append("Task Management System");

        return new RenderedEmail(subject, html.toString(), plain.toString());
    }
}
