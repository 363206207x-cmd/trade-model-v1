package org.example.trademodel.service;

import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;

public interface PublicOhlcvProvider {

    PublicOhlcvProviderResult fetchClosedBars(String symbol, String timeframe, int limit, String ingestionRunId);
}
