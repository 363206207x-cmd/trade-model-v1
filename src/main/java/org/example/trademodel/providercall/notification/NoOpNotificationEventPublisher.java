package org.example.trademodel.providercall.notification;

import org.springframework.stereotype.Service;

@Service
public class NoOpNotificationEventPublisher implements NotificationEventPublisher {
    @Override
    public NotificationPublishResult publish(NotificationEvent event) {
        return new NotificationPublishResult(false, false, "NOTIFICATION_DELIVERY_DISABLED");
    }
}
