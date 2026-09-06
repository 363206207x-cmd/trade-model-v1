package org.example.trademodel.v41;

import java.time.Instant;
import java.util.Map;

public record DashboardLiveEvent(String eventId, String eventType, String symbol,
                                 long snapshotVersion, Instant eventTime, Instant serverTime,
                                 Map<String, Object> payload) {
}
