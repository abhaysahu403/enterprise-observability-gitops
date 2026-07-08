package com.enterprise.notification.service;

import com.enterprise.notification.entity.Notification;
import com.enterprise.notification.entity.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock SMS sender, simulating delivery via a telecom gateway API.
 */
@Component
public class SmsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("[MOCK SMS] to={} message='{}'", notification.getRecipientContact(), notification.getMessage());
        return ThreadLocalRandom.current().nextInt(100) < 80;
    }
}
