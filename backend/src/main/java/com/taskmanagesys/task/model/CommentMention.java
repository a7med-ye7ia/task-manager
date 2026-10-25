package com.taskmanagesys.task.model;

import com.taskmanagesys.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comment_mentions")
public class CommentMention {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comment_id", nullable = false) private Comment comment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "seen_at") private Instant seenAt;
    public CommentMention() {}
    public CommentMention(Comment comment, User user) { this.comment = comment; this.user = user; }
    public Long getId() { return id; }
    public Comment getComment() { return comment; }
    public User getUser() { return user; }
    public Instant getSeenAt() { return seenAt; }
    public void setSeenAt(Instant seenAt) { this.seenAt = seenAt; }
}
