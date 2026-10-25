package com.taskmanagesys.notification.repository;

import com.taskmanagesys.notification.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    long countByUserIdAndReadFalse(Long userId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    java.util.Optional<Notification> findByIdAndUserId(Long id, Long userId);
    List<Notification> findByUserIdAndReadFalse(Long userId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    int markAllReadByUserId(@Param("userId") Long userId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification n where n.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
