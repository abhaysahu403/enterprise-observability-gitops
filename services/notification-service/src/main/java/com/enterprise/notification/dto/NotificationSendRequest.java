package com.enterprise.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NotificationSendRequest {

    @NotNull(message = "Recipient employee id is required")
    private Long recipientEmployeeId;

    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    private String recipientContact;

    @NotBlank(message = "Channel is required")
    private String channel;

    private String subject;

    @NotBlank(message = "Message is required")
    private String message;

    public Long getRecipientEmployeeId() { return recipientEmployeeId; }
    public void setRecipientEmployeeId(Long recipientEmployeeId) { this.recipientEmployeeId = recipientEmployeeId; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientContact() { return recipientContact; }
    public void setRecipientContact(String recipientContact) { this.recipientContact = recipientContact; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
