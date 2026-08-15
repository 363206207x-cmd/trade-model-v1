package org.example.trademodel.market.client;

import java.math.BigDecimal;
import java.util.Optional;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.UnifiedSourceStatus;

/**
 * USDⓈ-M perpetual last funding rate (minimal single-field fetch).
 */
public interface PerpFundingRateClient {

    /**
     * Best-effort {@code lastFundingRate} for the symbol's USDT perpetual (same symbol layout as spot USDT pair).
     */
    Optional<BigDecimal> fetchLastFundingRate(String assetSymbol);

    default ProviderAdapterResponse<BigDecimal> fetchLastFundingRateResult(String assetSymbol) {
        Optional<BigDecimal> value = fetchLastFundingRate(assetSymbol);
        return value == null || value.isEmpty()
                ? ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                "FUNDING_UNAVAILABLE", null)
                : ProviderAdapterResponse.ready(value.get(), java.time.Instant.now());
    }
}
