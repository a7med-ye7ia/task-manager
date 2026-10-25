package com.taskmanagesys.user.repository;

import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.taskmanagesys.task.dto.MemberCandidateResponse;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'MANAGER' AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    long countActiveManagers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'PENDING_APPROVAL' AND u.deletedAt IS NULL")
    long countPendingApprovals();

    Page<User> findByStatusAndDeletedAtIsNull(UserStatus status, Pageable pageable);
    List<User> findByRoleAndStatusAndDeletedAtIsNull(Role role, UserStatus status);

    @Query("SELECT u FROM User u JOIN ManagerEmailPrefs p ON u.id = p.userId " +
           "WHERE u.role = 'MANAGER' AND u.status = 'ACTIVE' AND u.deletedAt IS NULL " +
           "AND p.emailsEnabled = true AND p.onSignupRequest = true")
    List<User> findManagersForSignupNotification();

    @Query("SELECT new com.taskmanagesys.task.dto.MemberCandidateResponse(u.id, u.firstName, u.lastName, u.avatarPath) FROM User u WHERE u.status = 'ACTIVE' AND u.deletedAt IS NULL AND u.role IN ('EMPLOYEE','MANAGER') AND (:query = '' OR lower(concat(u.firstName, ' ', u.lastName)) like lower(concat('%', :query, '%'))) AND u.id NOT IN (SELECT m.userId FROM TaskMember m WHERE m.task.id = :taskId)")
    Page<MemberCandidateResponse> findMemberCandidates(@Param("taskId") Long taskId, @Param("query") String query, Pageable pageable);
}
