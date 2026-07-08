package com.enterprise.notification.service;

import com.enterprise.notification.entity.Notification;
import com.enterprise.notification.entity.NotificationChannel;

/**
 * Strategy interface for delivering a notification over a specific channel.
 * All implementations in this demo are mocks: they log the outgoing message
 * and simulate a delivery outcome instead of calling a real provider.
 */
public interface NotificationSender {

    NotificationChannel channel();

    /**
     * Attempts delivery. Returns true on simulated success, false on
     * simulated failure (a BusinessException is not thrown here so the
     * caller can uniformly handle retry bookkeeping).
     */
    boolean send(Notification notification);
}
