package org.example.trademodel.providercall.notification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Test-only collector. It has no Spring registration and cannot send external messages. */
public class InMemoryNotificationEventCollector implements NotificationEventPublisher {
    private final Map<String, NotificationEvent> events = new ConcurrentHashMap<>();

    @Override
    public NotificationPublishResult publish(NotificationEvent event) {
        if (event == null || event.dedupKey() == null || event.dedupKey().isBlank()) {
            return new NotificationPublishResult(false, false, "INELIGIBLE_EVENT");
        }
        NotificationEvent existing = events.putIfAbsent(event.dedupKey(), event);
        return existing == null
                ? new NotificationPublishResult(true, false, "COLLECTED_FOR_TEST")
                : new NotificationPublishResult(false, true, "DUPLICATE_DEDUP_KEY");
    }

    public List<NotificationEvent> snapshot() {
        return events.values().stream().sorted((left, right) -> left.dedupKey().compareTo(right.dedupKey())).toList();
    }
}
