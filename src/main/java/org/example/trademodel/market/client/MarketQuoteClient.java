package org.example.trademodel.market.client;

import java.util.Optional;

import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.UnifiedSourceStatus;
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

    default ProviderAdapterResponse<MarketQuoteSnapshot> fetch24hTickerResult(
            String assetSymbol, MarketType marketType) {
        Optional<MarketQuoteSnapshot> value = fetch24hTicker(assetSymbol, marketType);
        if (value == null || value.isEmpty()) {
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                    "QUOTE_UNAVAILABLE", null);
        }
        MarketQuoteSnapshot snapshot = value.get();
        java.time.Instant observedAt = snapshot.getFetchedAtEpochMillis() > 0
                ? java.time.Instant.ofEpochMilli(snapshot.getFetchedAtEpochMillis())
                : java.time.Instant.now();
        return ProviderAdapterResponse.ready(snapshot, observedAt);
    }
}
