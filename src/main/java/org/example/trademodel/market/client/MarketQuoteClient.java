package org.example.trademodel.market.client;

import java.util.Optional;

import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.providercall.instrument.MarketType;

public interface MarketQuoteClient {

    /**
     * Best-effort 24h ticker for the given base asset symbol (e.g. BTC, ETH). Empty if unavailable.
     */
    Optional<MarketQuoteSnapshot> fetch24hTicker(String assetSymbol);

    /**
     * Market-identity aware quote read. Existing clients remain spot-only by
     * default; providers that support perpetual data must override explicitly.
     */
    default Optional<MarketQuoteSnapshot> fetch24hTicker(String assetSymbol, MarketType marketType) {
        return marketType == MarketType.SPOT ? fetch24hTicker(assetSymbol) : Optional.empty();
    }
}
