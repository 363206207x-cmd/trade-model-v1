package org.example.trademodel.market.client.impl;

import org.example.trademodel.market.client.MarketKlineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Kline provider chain: Binance first, OKX only when Binance is unavailable or empty.
 */
@Primary
@Service
public class FallbackMarketKlineClient implements MarketKlineClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackMarketKlineClient.class);

    private final BinanceMarketKlineClient binanceMarketKlineClient;
    private final OkxMarketKlineClient okxMarketKlineClient;

    public FallbackMarketKlineClient(BinanceMarketKlineClient binanceMarketKlineClient,
                                     OkxMarketKlineClient okxMarketKlineClient) {
        this.binanceMarketKlineClient = binanceMarketKlineClient;
        this.okxMarketKlineClient = okxMarketKlineClient;
    }

    @Override
    public List<String[]> fetchKlines(String symbol, String interval, int limit) {
        List<String[]> primary = binanceMarketKlineClient.fetchKlines(symbol, interval, limit);
        if (primary != null && !primary.isEmpty()) {
            return primary;
        }
        List<String[]> fallback = okxMarketKlineClient.fetchKlines(symbol, interval, limit);
        if (fallback != null && !fallback.isEmpty()) {
            log.info("[market-kline] using fallback provider={} symbol={} interval={} rows={}",
                    OkxMarketKlineClient.PROVIDER, symbol, interval, fallback.size());
            return fallback;
        }
        return List.of();
    }
}
