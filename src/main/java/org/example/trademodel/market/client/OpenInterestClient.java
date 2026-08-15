package org.example.trademodel.market.client;

import java.math.BigDecimal;
import java.util.Optional;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.UnifiedSourceStatus;

/**
 * USDⓈ-M perpetual open interest (minimal single-field fetch).
 *
 * @see org.example.trademodel.market.client.impl.BinanceUsdtMOpenInterestClient
 */
public interface OpenInterestClient {

    /**
     * Best-effort {@code openInterest} for the symbol's USDT perpetual (same symbol layout as spot USDT pair).
     */
    Optional<BigDecimal> fetchOpenInterest(String assetSymbol);

    default ProviderAdapterResponse<BigDecimal> fetchOpenInterestResult(String assetSymbol) {
        Optional<BigDecimal> value = fetchOpenInterest(assetSymbol);
        return value == null || value.isEmpty()
                ? ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                "OPEN_INTEREST_UNAVAILABLE", null)
                : ProviderAdapterResponse.ready(value.get(), java.time.Instant.now());
    }
}
