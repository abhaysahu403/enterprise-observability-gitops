package com.enterprise.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduledJobs.class);

    private final NotificationService notificationService;

    @Value("${app.notification.retention-days:30}")
    private int retentionDays;

    public NotificationScheduledJobs(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Retries all notifications currently in RETRYING state every 5 minutes. */
    @Scheduled(fixedDelayString = "${app.notification.retry-sweep-interval-ms:300000}")
    public void retrySweep() {
        try {
            notificationService.retryAllPending();
        } catch (Exception e) {
            log.error("Notification retry sweep failed", e);
        }
    }

    /** Cleans up old SENT notifications nightly at 03:00, per the spec's "notification cleanup" job. */
    @Scheduled(cron = "${app.notification.cleanup-cron:0 0 3 * * *}")
    public void cleanup() {
        try {
            notificationService.cleanupOldSentNotifications(retentionDays);
        } catch (Exception e) {
            log.error("Notification cleanup job failed", e);
        }
    }
}
