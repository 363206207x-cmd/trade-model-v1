package org.example.trademodel.market.client;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * USDⓈ-M perpetual last funding rate (minimal single-field fetch).
 */
public interface PerpFundingRateClient {

    /**
     * Best-effort {@code lastFundingRate} for the symbol's USDT perpetual (same symbol layout as spot USDT pair).
     */
    Optional<BigDecimal> fetchLastFundingRate(String assetSymbol);
}
