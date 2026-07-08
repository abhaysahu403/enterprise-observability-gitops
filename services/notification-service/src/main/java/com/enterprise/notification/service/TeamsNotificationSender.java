package com.enterprise.notification.service;

import com.enterprise.notification.entity.Notification;
import com.enterprise.notification.entity.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock Microsoft Teams sender, simulating delivery via an incoming webhook.
 */
@Component
public class TeamsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(TeamsNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.TEAMS;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("[MOCK TEAMS] to={} message='{}'", notification.getRecipientName(), notification.getMessage());
        return ThreadLocalRandom.current().nextInt(100) < 85;
    }
}
