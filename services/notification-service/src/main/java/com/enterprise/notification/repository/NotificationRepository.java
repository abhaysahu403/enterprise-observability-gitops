package com.enterprise.notification.repository;

import com.enterprise.notification.entity.Notification;
import com.enterprise.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientEmployeeId(Long employeeId, Pageable pageable);

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);

    long countByStatus(NotificationStatus status);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.status = 'SENT' AND n.createdAt < :cutoff")
    int deleteSentNotificationsOlderThan(@Param("cutoff") Instant cutoff);
}
