package com.enterprise.notification.service;

import com.enterprise.notification.dto.NotificationResponse;
import com.enterprise.notification.dto.NotificationSendRequest;
import com.enterprise.notification.entity.Notification;
import com.enterprise.notification.entity.NotificationChannel;
import com.enterprise.notification.entity.NotificationStatus;
import com.enterprise.notification.repository.NotificationRepository;
import com.enterprise.shared.dto.PageResponse;
import com.enterprise.shared.exception.BusinessException;
import com.enterprise.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationService(NotificationRepository notificationRepository, List<NotificationSender> senderList) {
        this.notificationRepository = notificationRepository;
        this.senders = senderList.stream().collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
    }

    @Transactional
    public NotificationResponse send(NotificationSendRequest request) {
        NotificationChannel channel = parseChannel(request.getChannel());

        Notification notification = new Notification();
        notification.setRecipientEmployeeId(request.getRecipientEmployeeId());
        notification.setRecipientName(request.getRecipientName());
        notification.setRecipientContact(request.getRecipientContact());
        notification.setChannel(channel);
        notification.setSubject(request.getSubject());
        notification.setMessage(request.getMessage());
        notification.setStatus(NotificationStatus.PENDING);

        Notification saved = notificationRepository.save(notification);
        attemptDelivery(saved);
        return NotificationResponse.from(saved);
    }

    private void attemptDelivery(Notification notification) {
        NotificationSender sender = senders.get(notification.getChannel());
        notification.setLastAttemptAt(Instant.now());

        boolean success;
        try {
            success = sender.send(notification);
        } catch (Exception e) {
            success = false;
            notification.setFailureReason(e.getMessage());
        }

        if (success) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notification.setFailureReason(null);
            log.info("Notification delivered: id={} channel={} recipient={}",
                    notification.getId(), notification.getChannel(), notification.getRecipientName());
        } else {
            notification.setRetryCount(notification.getRetryCount() + 1);
            if (notification.getFailureReason() == null) {
                notification.setFailureReason("Simulated delivery failure from mock " + notification.getChannel() + " provider");
            }
            notification.setStatus(notification.getRetryCount() < notification.getMaxRetries()
                    ? NotificationStatus.RETRYING
                    : NotificationStatus.FAILED);
            log.warn("Notification delivery failed: id={} channel={} attempt={}/{} status={}",
                    notification.getId(), notification.getChannel(), notification.getRetryCount(),
                    notification.getMaxRetries(), notification.getStatus());
        }

        notificationRepository.save(notification);
    }

    @Transactional
    public NotificationResponse retry(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new BusinessException("ALREADY_SENT", "Notification was already delivered successfully");
        }
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            throw new BusinessException("MAX_RETRIES_EXCEEDED", "Maximum retry attempts already reached");
        }

        attemptDelivery(notification);
        return NotificationResponse.from(notification);
    }

    /** Invoked by the scheduled retry job to automatically retry all RETRYING notifications. */
    @Transactional
    public void retryAllPending() {
        List<Notification> pending = notificationRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.RETRYING, Integer.MAX_VALUE);
        for (Notification n : pending) {
            if (n.getRetryCount() < n.getMaxRetries()) {
                attemptDelivery(n);
            }
        }
        if (!pending.isEmpty()) {
            log.info("Automatic retry sweep processed {} notification(s)", pending.size());
        }
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id) {
        return NotificationResponse.from(notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> history(Long employeeId, String status, Pageable pageable) {
        Page<Notification> page;
        if (employeeId != null) {
            page = notificationRepository.findByRecipientEmployeeId(employeeId, pageable);
        } else if (status != null) {
            page = notificationRepository.findByStatus(parseStatus(status), pageable);
        } else {
            page = notificationRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> statusSummary() {
        return Map.of(
                "PENDING", notificationRepository.countByStatus(NotificationStatus.PENDING),
                "SENT", notificationRepository.countByStatus(NotificationStatus.SENT),
                "RETRYING", notificationRepository.countByStatus(NotificationStatus.RETRYING),
                "FAILED", notificationRepository.countByStatus(NotificationStatus.FAILED)
        );
    }

    @Transactional
    public int cleanupOldSentNotifications(int retentionDays) {
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 86400L);
        int deleted = notificationRepository.deleteSentNotificationsOlderThan(cutoff);
        log.info("Notification cleanup removed {} record(s) older than {} days", deleted, retentionDays);
        return deleted;
    }

    private NotificationChannel parseChannel(String value) {
        try {
            return NotificationChannel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_CHANNEL", "Unknown notification channel: " + value);
        }
    }

    private NotificationStatus parseStatus(String value) {
        try {
            return NotificationStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "Unknown notification status: " + value);
        }
    }
}
