package org.example.trademodel.v41;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DashboardLiveEventService {
    public static final long STREAM_TIMEOUT_MILLIS = 0L;
    private final Map<String, DashboardLiveEvent> latestByIdentity = new ConcurrentHashMap<>();
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, ScopedStructuralContext> structuralContexts = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        CopyOnWriteArrayList<SseEmitter> userSubscribers = subscribers.computeIfAbsent(userId,
                ignored -> new CopyOnWriteArrayList<>());
        userSubscribers.add(emitter);
        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ignored -> remove.run());
        latestByIdentity.values().stream()
                .sorted(java.util.Comparator.comparing(DashboardLiveEvent::symbol)
                        .thenComparing(DashboardLiveEvent::eventType))
                .forEach(event -> send(emitter, event));
        return emitter;
    }

    public synchronized boolean publish(DashboardLiveEvent event) {
        if (!valid(event)) return false;
        String symbol = normalize(event.symbol());
        String identity = identity(symbol, event.eventType());
        DashboardLiveEvent current = latestByIdentity.get(identity);
        if (current != null && event.snapshotVersion() <= current.snapshotVersion()) return false;
        latestByIdentity.put(identity, event);
        subscribers.forEach((userId, emitters) -> emitters.forEach(emitter -> send(emitter, event)));
        return true;
    }

    public boolean publishToUser(Long userId, DashboardLiveEvent event) {
        if (userId == null || !valid(event)) return false;
        subscribers.getOrDefault(userId, new CopyOnWriteArrayList<>())
                .forEach(emitter -> send(emitter, event));
        return true;
    }

    public Optional<DashboardLiveEvent> latest(String symbol) {
        String normalized = normalize(symbol);
        return latestByIdentity.values().stream()
                .filter(event -> normalized != null && normalized.equals(normalize(event.symbol())))
                .max(java.util.Comparator.comparingLong(DashboardLiveEvent::snapshotVersion));
    }

    public Optional<DashboardLiveEvent> latest(String symbol, String eventType) {
        return Optional.ofNullable(latestByIdentity.get(identity(normalize(symbol), eventType)));
    }

    public List<DashboardLiveEvent> latestEvents() {
        return List.copyOf(latestByIdentity.values());
    }

    public synchronized void recordStructuralContext(Long userId, StructuralContext context) {
        if (userId == null || userId < 0 || context == null || normalize(context.symbol()) == null
                || context.calculatedAt() == null) return;
        String key = userId + "|" + normalize(context.symbol());
        structuralContexts.compute(key, (ignored, current) -> current == null
                || !current.context().calculatedAt().isAfter(context.calculatedAt())
                ? new ScopedStructuralContext(userId, context) : current);
    }

    /** Called only by the position-monitor lifecycle. A null user replaces the full active-position snapshot. */
    public synchronized void replacePositionStructuralContexts(Long userId, List<ScopedStructuralContext> contexts) {
        structuralContexts.entrySet().removeIf(entry -> userId == null || userId.equals(entry.getValue().userId()));
        for (ScopedStructuralContext scoped : contexts) {
            if (scoped != null && (userId == null || userId.equals(scoped.userId()))) {
                recordStructuralContext(scoped.userId(), scoped.context());
            }
        }
    }

    public synchronized List<ScopedStructuralContext> structuralContexts(String symbol) {
        String normalized = normalize(symbol);
        if (normalized == null) return List.of();
        return structuralContexts.values().stream()
                .filter(item -> normalized.equals(normalize(item.context().symbol())))
                .toList();
    }

    @Scheduled(fixedRate = 15_000L)
    public void heartbeat() {
        DashboardLiveEvent heartbeat = new DashboardLiveEvent("heartbeat-" + System.currentTimeMillis(),
                "SYSTEM_STATUS_UPDATED", "SYSTEM", System.currentTimeMillis(), Instant.now(), Instant.now(),
                Map.of("state", "UP", "version", "V41-HOME-SSE-1"));
        subscribers.forEach((userId, emitters) -> emitters.forEach(emitter -> send(emitter, heartbeat)));
    }

    private void send(SseEmitter emitter, DashboardLiveEvent event) {
        try {
            emitter.send(SseEmitter.event().id(event.eventId()).name(event.eventType()).data(event));
        } catch (IOException | IllegalStateException failure) {
            subscribers.forEach((userId, emitters) -> emitters.remove(emitter));
            try {
                emitter.complete();
            } catch (IllegalStateException alreadyClosed) {
                // The subscriber is detached. A closed browser must not abort analysis publication.
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(userId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) subscribers.remove(userId, emitters);
    }

    private static boolean valid(DashboardLiveEvent event) {
        return event != null && event.snapshotVersion() > 0 && normalize(event.symbol()) != null
                && event.eventId() != null && !event.eventId().isBlank()
                && event.eventType() != null && !event.eventType().isBlank();
    }

    private static String identity(String symbol, String eventType) {
        return String.valueOf(normalize(symbol)) + "|" + String.valueOf(normalize(eventType));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public record StructuralContext(String symbol, String structuralDirection,
                                    BigDecimal atr1h, BigDecimal invalidationLevel,
                                    Integer baseConfidence, String analysisId,
                                    String decisionId, String traceId,
                                    Instant calculatedAt, Instant expiresAt,
                                    boolean derivativesFresh) {
    }

    public record ScopedStructuralContext(Long userId, StructuralContext context) {
    }
}
