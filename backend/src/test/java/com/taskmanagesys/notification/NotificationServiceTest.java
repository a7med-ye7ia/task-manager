package com.taskmanagesys.notification;

import com.taskmanagesys.email.service.EmailOutboxService;
import com.taskmanagesys.notification.model.Notification;
import com.taskmanagesys.notification.repository.NotificationRepository;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.user.model.ManagerEmailPrefs;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.ManagerEmailPrefsRepository;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Test
    void managerPreferenceDisablesEmailButKeepsInAppNotification() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        TaskMemberRepository members = mock(TaskMemberRepository.class);
        UserRepository users = mock(UserRepository.class);
        ManagerEmailPrefsRepository prefs = mock(ManagerEmailPrefsRepository.class);
        EmailOutboxService outbox = mock(EmailOutboxService.class);
        TaskRepository tasks = mock(TaskRepository.class);
        User manager = new User(); manager.setId(7L); manager.setRole(Role.MANAGER); manager.setEmail("manager@example.com");
        ManagerEmailPrefs preference = new ManagerEmailPrefs(manager); preference.setEmailsEnabled(false);
        when(users.findById(7L)).thenReturn(Optional.of(manager));
        when(prefs.findById(7L)).thenReturn(Optional.of(preference));
        NotificationService service = new NotificationService(notifications, members, users, prefs, outbox, tasks);

        service.taskReview(4L, 2L, 7L, "Review task");

        verify(notifications).save(any(Notification.class));
        verify(outbox, never()).queueEmail(any(), any(), any());
    }

    @Test
    void mentionNeverNotifiesTheAuthor() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(notifications, mock(TaskMemberRepository.class), mock(UserRepository.class), mock(ManagerEmailPrefsRepository.class), mock(EmailOutboxService.class), mock(TaskRepository.class));
        service.mention(4L, 7L, 7L, "Task", "Author");
        verifyNoInteractions(notifications);
    }

    @Test
    void selfReportedCreated_respectsManagerEmailPrefs() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        TaskMemberRepository members = mock(TaskMemberRepository.class);
        UserRepository users = mock(UserRepository.class);
        ManagerEmailPrefsRepository prefs = mock(ManagerEmailPrefsRepository.class);
        EmailOutboxService outbox = mock(EmailOutboxService.class);
        TaskRepository tasks = mock(TaskRepository.class);

        User manager1 = new User(); manager1.setId(1L); manager1.setRole(Role.MANAGER); manager1.setEmail("m1@example.com");
        User manager2 = new User(); manager2.setId(2L); manager2.setRole(Role.MANAGER); manager2.setEmail("m2@example.com");

        ManagerEmailPrefs pref1 = new ManagerEmailPrefs(manager1); pref1.setEmailsEnabled(true); pref1.setOnSelfReported(true);
        ManagerEmailPrefs pref2 = new ManagerEmailPrefs(manager2); pref2.setEmailsEnabled(true); pref2.setOnSelfReported(false);

        when(users.findByRoleAndStatusAndDeletedAtIsNull(Role.MANAGER, UserStatus.ACTIVE)).thenReturn(List.of(manager1, manager2));
        when(users.findById(1L)).thenReturn(Optional.of(manager1));
        when(users.findById(2L)).thenReturn(Optional.of(manager2));
        when(prefs.findById(1L)).thenReturn(Optional.of(pref1));
        when(prefs.findById(2L)).thenReturn(Optional.of(pref2));

        NotificationService service = new NotificationService(notifications, members, users, prefs, outbox, tasks);
        service.selfReportedCreated(100L, 99L, "Employee Guy", "New Feature");

        // Both get in-app notifications
        verify(notifications, times(2)).save(any(Notification.class));
        // Only manager 1 (onSelfReported=true) gets queued email
        verify(outbox, times(1)).queueEmail(eq("m1@example.com"), eq("self-reported-created"), any());
        verify(outbox, never()).queueEmail(eq("m2@example.com"), any(), any());
    }

    @Test
    void selfReportedReview_sendsEmailWhenEitherOnSelfReportedOrOnTaskCompletedIsTrue() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        TaskMemberRepository members = mock(TaskMemberRepository.class);
        UserRepository users = mock(UserRepository.class);
        ManagerEmailPrefsRepository prefs = mock(ManagerEmailPrefsRepository.class);
        EmailOutboxService outbox = mock(EmailOutboxService.class);
        TaskRepository tasks = mock(TaskRepository.class);

        User manager1 = new User(); manager1.setId(1L); manager1.setRole(Role.MANAGER); manager1.setEmail("m1@example.com");
        User manager2 = new User(); manager2.setId(2L); manager2.setRole(Role.MANAGER); manager2.setEmail("m2@example.com");

        // Manager 1 has onSelfReported=false, onTaskCompleted=true -> should receive email
        ManagerEmailPrefs pref1 = new ManagerEmailPrefs(manager1); pref1.setEmailsEnabled(true); pref1.setOnSelfReported(false); pref1.setOnTaskCompleted(true);
        // Manager 2 has onSelfReported=false, onTaskCompleted=false -> should not receive email
        ManagerEmailPrefs pref2 = new ManagerEmailPrefs(manager2); pref2.setEmailsEnabled(true); pref2.setOnSelfReported(false); pref2.setOnTaskCompleted(false);

        when(users.findByRoleAndStatusAndDeletedAtIsNull(Role.MANAGER, UserStatus.ACTIVE)).thenReturn(List.of(manager1, manager2));
        when(users.findById(1L)).thenReturn(Optional.of(manager1));
        when(users.findById(2L)).thenReturn(Optional.of(manager2));
        when(prefs.findById(1L)).thenReturn(Optional.of(pref1));
        when(prefs.findById(2L)).thenReturn(Optional.of(pref2));

        NotificationService service = new NotificationService(notifications, members, users, prefs, outbox, tasks);
        service.selfReportedReview(100L, 99L, "New Feature");

        verify(notifications, times(2)).save(any(Notification.class));
        verify(outbox, times(1)).queueEmail(eq("m1@example.com"), eq("self-reported-review"), any());
        verify(outbox, never()).queueEmail(eq("m2@example.com"), any(), any());
    }
}
