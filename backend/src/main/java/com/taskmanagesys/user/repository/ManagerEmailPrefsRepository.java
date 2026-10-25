package com.taskmanagesys.user.repository;

import com.taskmanagesys.user.model.ManagerEmailPrefs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerEmailPrefsRepository extends JpaRepository<ManagerEmailPrefs, Long> {
}
