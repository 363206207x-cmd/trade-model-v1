package org.example.trademodel.v41;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class DashboardLiveEventServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void brokenConnectionAndCompletionCannotAbortAnalysisPublishOrOtherSubscribers() throws Exception {
        DashboardLiveEventService service = new DashboardLiveEventService();
        SseEmitter disconnected = mock(SseEmitter.class);
        SseEmitter healthy = mock(SseEmitter.class);
        doThrow(new IOException("closed client")).when(disconnected).send(any(SseEmitter.SseEventBuilder.class));
        doThrow(new IllegalStateException("already completed")).when(disconnected).complete();
        Map<Long, CopyOnWriteArrayList<SseEmitter>> subscriptions =
                (Map<Long, CopyOnWriteArrayList<SseEmitter>>) ReflectionTestUtils.getField(service, "subscribers");
        subscriptions.put(7L, new CopyOnWriteArrayList<>(java.util.List.of(disconnected, healthy)));
        Instant now = Instant.now();
        var event = new DashboardLiveEvent("event-live-1", "ASSET_DIRECTION_UPDATED", "ETHUSDT",
                1L, now, now, Map.of("analysisId", "run-live-1"));

        assertThatCode(() -> service.publish(event)).doesNotThrowAnyException();
        verify(healthy).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(subscriptions.get(7L)).containsExactly(healthy);
        assertThat(service.latest("ETHUSDT").orElseThrow()).isEqualTo(event);
        assertThatCode(service::heartbeat).doesNotThrowAnyException();
        verify(disconnected, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void authenticatedDashboardStreamDoesNotExpireWhileHeartbeatKeepsConnectionAlive() {
        DashboardLiveEventService service = new DashboardLiveEventService();

        var emitter = service.subscribe(7L);

        assertThat(emitter.getTimeout()).isZero();
    }

    @Test
    void rejectsDuplicateAndOutOfOrderSnapshotsPerSymbol() {
        DashboardLiveEventService service = new DashboardLiveEventService();
        var newer = new DashboardLiveEvent("event-2", "ASSET_PRICE_UPDATED", "BTCUSDT", 2L,
                Instant.now(), Instant.now(), Map.of("latestPrice", new BigDecimal("80000")));
        var older = new DashboardLiveEvent("event-1", "ASSET_PRICE_UPDATED", "BTCUSDT", 1L,
                Instant.now(), Instant.now(), Map.of("latestPrice", new BigDecimal("79900")));

        assertThat(service.publish(newer)).isTrue();
        assertThat(service.publish(older)).isFalse();
        assertThat(service.publish(newer)).isFalse();
        assertThat(service.latest("BTCUSDT").orElseThrow().snapshotVersion()).isEqualTo(2L);
    }

    @Test
    void keepsLatestStructuralContextPerUserAndSymbol() {
        DashboardLiveEventService service = new DashboardLiveEventService();
        Instant now = Instant.now();
        var first = new DashboardLiveEventService.StructuralContext("BTCUSDT", "BULLISH",
                new BigDecimal("100"), new BigDecimal("70000"), 82,
                "analysis-1", "decision-1", "trace-1", now, now.plusSeconds(3600), true);
        var older = new DashboardLiveEventService.StructuralContext("BTCUSDT", "BEARISH",
                new BigDecimal("100"), new BigDecimal("90000"), 70,
                "analysis-old", "decision-old", "trace-old", now.minusSeconds(1), now.plusSeconds(3600), true);

        service.recordStructuralContext(7L, first);
        service.recordStructuralContext(7L, older);

        assertThat(service.structuralContexts("BTCUSDT")).singleElement().satisfies(scoped -> {
            assertThat(scoped.userId()).isEqualTo(7L);
            assertThat(scoped.context().decisionId()).isEqualTo("decision-1");
            assertThat(scoped.context().structuralDirection()).isEqualTo("BULLISH");
        });
    }
}
