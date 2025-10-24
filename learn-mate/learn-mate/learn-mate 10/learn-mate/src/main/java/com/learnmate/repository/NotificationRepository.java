package com.learnmate.repository;

import com.learnmate.model.Notification;
import com.learnmate.model.SchoolClass;
import com.learnmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetUser(User user);
    List<Notification> findByTargetUserOrderByCreatedAtDesc(User user);
    List<Notification> findByTargetRole(String role);
    List<Notification> findByTargetClass(SchoolClass schoolClass);
    List<Notification> findByTargetClassAndTargetUserIsNull(SchoolClass schoolClass);
    List<Notification> findByCreatedByOrderByCreatedAtDesc(User createdBy);
    List<Notification> findByBroadcastKey(String broadcastKey);
    long countByTargetUserAndReadFalse(User user);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    void deleteByBroadcastKey(String broadcastKey);
}
