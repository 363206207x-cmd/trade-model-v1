package org.example.trademodel.providercall.notification;

public interface NotificationEventPublisher {
    NotificationPublishResult publish(NotificationEvent event);
}
