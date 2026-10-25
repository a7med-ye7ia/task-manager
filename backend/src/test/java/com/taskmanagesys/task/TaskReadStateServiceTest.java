package com.taskmanagesys.task;

import com.taskmanagesys.task.model.Comment;
import com.taskmanagesys.task.model.TaskActivity;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskReadState;
import com.taskmanagesys.task.repository.CommentRepository;
import com.taskmanagesys.task.repository.CommentMentionRepository;
import com.taskmanagesys.task.repository.TaskActivityRepository;
import com.taskmanagesys.task.repository.TaskReadStateRepository;
import com.taskmanagesys.task.service.TaskAccessService;
import com.taskmanagesys.task.service.TaskReadStateService;
import com.taskmanagesys.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskReadStateServiceTest {
    @Mock TaskReadStateRepository states; @Mock CommentRepository comments; @Mock TaskActivityRepository activities; @Mock CommentMentionRepository mentions; @Mock TaskAccessService access;
    @Test void unreadExcludesOwnItemsAndMarkReadStoresLatestIds() {
        TaskReadStateService service = new TaskReadStateService(states, comments, activities, mentions, access);
        when(states.findByIdTaskIdAndIdUserId(7L, 3L)).thenReturn(Optional.of(new TaskReadState(7L, 3L)));
        Comment own = mock(Comment.class); when(own.getId()).thenReturn(1L); User author = mock(User.class); when(author.getId()).thenReturn(3L); when(own.getAuthor()).thenReturn(author);
        Comment other = mock(Comment.class); when(other.getId()).thenReturn(2L); User otherAuthor = mock(User.class); when(otherAuthor.getId()).thenReturn(4L); when(other.getAuthor()).thenReturn(otherAuthor);
        when(comments.findByTaskIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(own, other));
        when(activities.findByTaskIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        assertEquals(1, service.unread(7L, 3L).chatCount());
        service.markChatRead(7L, 3L);
        verify(states).save(any(TaskReadState.class));
    }

    @Test void unreadCountsAreBatchedPerTaskAndExcludeOtherUsers() {
        TaskReadStateService service = new TaskReadStateService(states, comments, activities, mentions, access);
        Task taskA = mock(Task.class); when(taskA.getId()).thenReturn(7L);
        Task taskB = mock(Task.class); when(taskB.getId()).thenReturn(8L);
        when(states.findAllById(any())).thenReturn(List.of());
        Comment currentTaskComment = mock(Comment.class); when(currentTaskComment.getId()).thenReturn(11L); when(currentTaskComment.getTask()).thenReturn(taskA);
        User otherAuthor = mock(User.class); when(otherAuthor.getId()).thenReturn(4L); when(currentTaskComment.getAuthor()).thenReturn(otherAuthor);
        Comment otherTaskComment = mock(Comment.class); when(otherTaskComment.getId()).thenReturn(12L); when(otherTaskComment.getTask()).thenReturn(taskB); when(otherTaskComment.getAuthor()).thenReturn(otherAuthor);
        when(comments.findByTaskIdInOrderByIdAsc(List.of(7L, 8L))).thenReturn(List.of(currentTaskComment, otherTaskComment));
        TaskActivity activity = mock(TaskActivity.class); when(activity.getId()).thenReturn(21L); when(activity.getTaskId()).thenReturn(7L); when(activity.getActorId()).thenReturn(4L);
        when(activities.findByTaskIdInOrderByIdAsc(List.of(7L, 8L))).thenReturn(List.of(activity));
        when(mentions.findUnseenByUserAndTaskIds(3L, List.of(7L, 8L))).thenReturn(List.of());

        Map<Long, Long> counts = service.unreadCounts(List.of(7L, 8L), 3L);

        assertEquals(2L, counts.get(7L));
        assertEquals(1L, counts.get(8L));
        verify(mentions).findUnseenByUserAndTaskIds(3L, List.of(7L, 8L));
    }

    @Test void unreadMentionCountsAreScopedToTheCurrentUserAndTask() {
        TaskReadStateService service = new TaskReadStateService(states, comments, activities, mentions, access);
        Task taskA = mock(Task.class); when(taskA.getId()).thenReturn(7L);
        Comment commentA = mock(Comment.class); when(commentA.getTask()).thenReturn(taskA);
        com.taskmanagesys.task.model.CommentMention mentionA = mock(com.taskmanagesys.task.model.CommentMention.class);
        when(mentionA.getComment()).thenReturn(commentA);
        when(mentions.findUnseenByUserAndTaskIds(3L, List.of(7L, 8L))).thenReturn(List.of(mentionA));

        Map<Long, Long> counts = service.unreadMentionCounts(List.of(7L, 8L), 3L);

        assertEquals(1L, counts.get(7L));
        assertFalse(counts.containsKey(8L));
        verify(mentions).findUnseenByUserAndTaskIds(3L, List.of(7L, 8L));
    }
}
