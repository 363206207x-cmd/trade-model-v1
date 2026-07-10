package org.example.trademodel.service;

import java.util.List;

public interface DecisionOhlcvSnapshotSource {
    List<String[]> readClosedBars(String symbol, String timeframe, int limit, String traceId);
}
