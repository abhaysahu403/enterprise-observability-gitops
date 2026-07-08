package com.enterprise.notification.service;

import com.enterprise.notification.entity.Notification;
import com.enterprise.notification.entity.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock email sender. In a real deployment this would call an SMTP relay or
 * a provider API (SES, SendGrid, etc). Here it simply logs the send and
 * simulates a ~90% success rate so the retry mechanism has something to do.
 */
@Component
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("[MOCK EMAIL] to={} subject='{}' body='{}'",
                notification.getRecipientContact() != null ? notification.getRecipientContact() : notification.getRecipientName(),
                notification.getSubject(), notification.getMessage());
        return ThreadLocalRandom.current().nextInt(100) < 90;
    }
}
