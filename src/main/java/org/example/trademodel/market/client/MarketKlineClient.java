package org.example.trademodel.market.client;

import java.util.List;

/**
 * Minimal market kline client. Rows use the existing Binance-style String[] contract.
 */
public interface MarketKlineClient {

    List<String[]> fetchKlines(String symbol, String interval, int limit);
}
