package org.example.trademodel.providercall.notification;

public record NotificationPublishResult(boolean accepted, boolean duplicate, String reasonCode) {
}
