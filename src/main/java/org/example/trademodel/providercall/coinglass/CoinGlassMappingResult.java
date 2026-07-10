package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.UnifiedSourceStatus;

import java.time.Instant;

public record CoinGlassMappingResult<T>(
        T payload,
        UnifiedSourceStatus status,
        Instant providerDataTime,
        String reasonCode
) {
    public static <T> CoinGlassMappingResult<T> ready(T payload, Instant providerDataTime) {
        return new CoinGlassMappingResult<>(payload, UnifiedSourceStatus.READY, providerDataTime, null);
    }

    public static <T> CoinGlassMappingResult<T> failed(UnifiedSourceStatus status, String reasonCode) {
        return new CoinGlassMappingResult<>(null, status, null, reasonCode);
    }
}
