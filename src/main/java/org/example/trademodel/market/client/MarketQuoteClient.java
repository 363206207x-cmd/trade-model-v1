package org.example.trademodel.market.client;

import java.util.Optional;

import org.example.trademodel.market.dto.MarketQuoteSnapshot;

public interface MarketQuoteClient {

    /**
     * Best-effort 24h ticker for the given base asset symbol (e.g. BTC, ETH). Empty if unavailable.
     */
    Optional<MarketQuoteSnapshot> fetch24hTicker(String assetSymbol);
}
