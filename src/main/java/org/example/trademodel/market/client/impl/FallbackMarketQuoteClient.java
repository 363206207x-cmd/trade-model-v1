package org.example.trademodel.market.client.impl;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Market quote chain: Binance first, OKX only when Binance is unavailable.
 */
@Primary
@Service
public class FallbackMarketQuoteClient implements MarketQuoteClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackMarketQuoteClient.class);

    private final BinanceMarketQuoteClient binanceMarketQuoteClient;
    private final OkxMarketQuoteClient okxMarketQuoteClient;

    public FallbackMarketQuoteClient(BinanceMarketQuoteClient binanceMarketQuoteClient,
                                     OkxMarketQuoteClient okxMarketQuoteClient) {
        this.binanceMarketQuoteClient = binanceMarketQuoteClient;
        this.okxMarketQuoteClient = okxMarketQuoteClient;
    }

    @Override
    public Optional<MarketQuoteSnapshot> fetch24hTicker(String assetSymbol) {
        Optional<MarketQuoteSnapshot> primary = binanceMarketQuoteClient.fetch24hTicker(assetSymbol);
        if (primary.isPresent()) {
            return primary;
        }
        Optional<MarketQuoteSnapshot> fallback = okxMarketQuoteClient.fetch24hTicker(assetSymbol);
        fallback.ifPresent(snapshot -> log.info(
                "[market-quote] using fallback provider={} symbol={}",
                snapshot.getProvider(), snapshot.getSymbolNormalized()));
        return fallback;
    }
}
