package com.taskmanagesys.notification;

import com.taskmanagesys.email.service.EmailOutboxService;
import com.taskmanagesys.notification.model.Notification;
import com.taskmanagesys.notification.repository.NotificationRepository;
import com.taskmanagesys.notification.service.NotificationService;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.user.model.ManagerEmailPrefs;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.repository.ManagerEmailPrefsRepository;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
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
        User manager = new User(); manager.setRole(Role.MANAGER); manager.setEmail("manager@example.com");
        ManagerEmailPrefs preference = new ManagerEmailPrefs(manager); preference.setEmailsEnabled(false);
        when(users.findById(7L)).thenReturn(Optional.of(manager));
        when(prefs.findById(7L)).thenReturn(Optional.of(preference));
        NotificationService service = new NotificationService(notifications, members, users, prefs, outbox);

        service.taskReview(4L, 2L, 7L, "Review task");

        verify(notifications).save(any(Notification.class));
        verify(outbox, never()).queueEmail(any(), any(), any());
    }

    @Test
    void mentionNeverNotifiesTheAuthor() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(notifications, mock(TaskMemberRepository.class), mock(UserRepository.class), mock(ManagerEmailPrefsRepository.class), mock(EmailOutboxService.class));
        service.mention(4L, 7L, 7L, "Task", "Author");
        verifyNoInteractions(notifications);
    }
}
