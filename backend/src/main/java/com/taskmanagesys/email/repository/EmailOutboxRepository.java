package com.taskmanagesys.email.repository;

import com.taskmanagesys.email.model.EmailOutbox;
import com.taskmanagesys.email.model.EmailStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    @Query("SELECT e FROM EmailOutbox e WHERE e.status = :status AND e.nextAttemptAt <= :now ORDER BY e.id ASC")
    List<EmailOutbox> findPendingEmails(EmailStatus status, Instant now, Pageable pageable);
    long deleteByStatusAndCreatedAtBefore(EmailStatus status, Instant cutoff);
}
