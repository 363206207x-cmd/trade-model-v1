package org.example.trademodel.market.client;

import java.math.BigDecimal;
import java.util.Optional;

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
}
