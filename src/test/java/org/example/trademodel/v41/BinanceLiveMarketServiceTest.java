package org.example.trademodel.v41;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.service.PositionMonitorScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

class BinanceLiveMarketServiceTest {

    @Test
    void duplicateAndOutOfOrderMarkEventsDoNotOverwriteNewPrice() {
        DashboardLiveEventService events = mock(DashboardLiveEventService.class);
        BinanceLiveMarketService service = new BinanceLiveMarketService(new ObjectMapper(), events, false);

        service.process(mark(1002, "100.20"));
        service.process(mark(1002, "99.00"));
        service.process(mark(1001, "98.00"));

        ArgumentCaptor<DashboardLiveEvent> captor = ArgumentCaptor.forClass(DashboardLiveEvent.class);
        verify(events, times(1)).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ASSET_PRICE_UPDATED");
        assertThat(captor.getValue().payload().get("latestPrice").toString()).isEqualTo("100.20");
    }

    @Test
    void streamIncludesSnapshotBackedDepthAndClosedIntervalInputs() {
        String uri = invokeStreamUri();
        assertThat(uri).contains("btcusdt@markPrice@1s", "btcusdt@bookTicker",
                "btcusdt@depth@500ms", "btcusdt@kline_1m", "btcusdt@kline_5m",
                "adausdt@markPrice@1s", "adausdt@bookTicker", "adausdt@depth@500ms")
                .doesNotContain("dogeusdt");
    }

    @Test
    void liveKlineShockSuspendsPlanAndRequestsImmediateReadOnlyPositionMonitor() {
        DashboardLiveEventService events = mock(DashboardLiveEventService.class);
        PositionMonitorScheduler monitor = mock(PositionMonitorScheduler.class);
        BinanceLiveMarketService service = new BinanceLiveMarketService(new ObjectMapper(), events, false);
        service.setPositionMonitorScheduler(monitor);
        long eventTime = System.currentTimeMillis();
        var context = new DashboardLiveEventService.StructuralContext("BTCUSDT", "BULLISH",
                new BigDecimal("0.50"), new BigDecimal("98.00"), 88,
                "analysis-live-1", "decision-live-1", "trace-live-1",
                Instant.ofEpochMilli(eventTime).minusSeconds(60),
                Instant.ofEpochMilli(eventTime).plusSeconds(3600), true);
        when(events.structuralContexts("BTCUSDT"))
                .thenReturn(List.of(new DashboardLiveEventService.ScopedStructuralContext(7L, context)));

        service.process(kline(eventTime, "1m", "100.00", "99.40"));

        ArgumentCaptor<DashboardLiveEvent> captor = ArgumentCaptor.forClass(DashboardLiveEvent.class);
        verify(events, times(2)).publishToUser(eq(7L), captor.capture());
        assertThat(captor.getAllValues()).extracting(DashboardLiveEvent::eventType)
                .containsExactly("ASSET_RISK_UPDATED", "PLAN_STATE_CHANGED");
        assertThat(captor.getAllValues().get(0).payload())
                .containsEntry("realtimeState", "RAPID_DROP")
                .containsEntry("effectiveExecutionState", "SUSPENDED");
        assertThat(captor.getAllValues().get(1).payload()).containsEntry("planState", "SUSPENDED");
        verify(monitor).requestImmediateRiskMonitor("ADVERSE_MOVE_WARNING");
    }

    @Test
    void laterUpdateInsideSameOpenKlineCanRaiseShock() {
        DashboardLiveEventService events = mock(DashboardLiveEventService.class);
        BinanceLiveMarketService service = new BinanceLiveMarketService(new ObjectMapper(), events, false);
        long eventTime = System.currentTimeMillis();
        long closeTime = eventTime + 60_000L;
        var context = new DashboardLiveEventService.StructuralContext("BTCUSDT", "BULLISH",
                new BigDecimal("0.50"), new BigDecimal("98.00"), 88,
                "analysis-live-1", "decision-live-1", "trace-live-1",
                Instant.ofEpochMilli(eventTime).minusSeconds(60),
                Instant.ofEpochMilli(eventTime).plusSeconds(3600), true);
        when(events.structuralContexts("BTCUSDT"))
                .thenReturn(List.of(new DashboardLiveEventService.ScopedStructuralContext(7L, context)));

        service.process(kline(eventTime, closeTime, "1m", "100.00", "99.95"));
        service.process(kline(eventTime + 1_000L, closeTime, "1m", "100.00", "99.40"));

        verify(events, times(2)).publishToUser(eq(7L), org.mockito.ArgumentMatchers.any());
    }

    private static String mark(long eventTime, String price) {
        return "{\"stream\":\"btcusdt@markPrice@1s\",\"data\":{\"s\":\"BTCUSDT\",\"E\":"
                + eventTime + ",\"p\":\"" + price + "\",\"i\":\"100.00\"}}";
    }

    private static String kline(long eventTime, String interval, String open, String close) {
        return kline(eventTime, eventTime, interval, open, close);
    }

    private static String kline(long eventTime, long closeTime, String interval, String open, String close) {
        return "{\"stream\":\"btcusdt@kline_" + interval + "\",\"data\":{\"s\":\"BTCUSDT\",\"E\":"
                + eventTime + ",\"k\":{\"i\":\"" + interval + "\",\"T\":" + closeTime
                + ",\"o\":\"" + open + "\",\"c\":\"" + close + "\"}}}";
    }

    private static String invokeStreamUri() {
        try {
            var method = BinanceLiveMarketService.class.getDeclaredMethod("streamUri");
            method.setAccessible(true);
            return (String) method.invoke(null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }
}
