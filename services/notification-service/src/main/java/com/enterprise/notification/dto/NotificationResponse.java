package com.enterprise.notification.dto;

import com.enterprise.notification.entity.Notification;

import java.time.Instant;

public class NotificationResponse {

    private Long id;
    private Long recipientEmployeeId;
    private String recipientName;
    private String channel;
    private String subject;
    private String message;
    private String status;
    private int retryCount;
    private int maxRetries;
    private String failureReason;
    private Instant lastAttemptAt;
    private Instant sentAt;
    private Instant createdAt;

    public static NotificationResponse from(Notification n) {
        NotificationResponse dto = new NotificationResponse();
        dto.id = n.getId();
        dto.recipientEmployeeId = n.getRecipientEmployeeId();
        dto.recipientName = n.getRecipientName();
        dto.channel = n.getChannel().name();
        dto.subject = n.getSubject();
        dto.message = n.getMessage();
        dto.status = n.getStatus().name();
        dto.retryCount = n.getRetryCount();
        dto.maxRetries = n.getMaxRetries();
        dto.failureReason = n.getFailureReason();
        dto.lastAttemptAt = n.getLastAttemptAt();
        dto.sentAt = n.getSentAt();
        dto.createdAt = n.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getRecipientEmployeeId() { return recipientEmployeeId; }
    public String getRecipientName() { return recipientName; }
    public String getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public String getFailureReason() { return failureReason; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
