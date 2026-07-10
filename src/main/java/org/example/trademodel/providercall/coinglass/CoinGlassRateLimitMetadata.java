package org.example.trademodel.providercall.coinglass;

public record CoinGlassRateLimitMetadata(
        Integer apiKeyMaxLimit,
        Integer apiKeyUseLimit,
        Long retryAfterSeconds
) {
}
