package org.example.trademodel.providercall.coinglass;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record CoinGlassClientResponse(
        int httpStatus,
        String providerStatusCode,
        JsonNode data,
        CoinGlassRateLimitMetadata rateLimit,
        Instant fetchTime,
        String endpointCapabilityId,
        String errorCode
) {
    public boolean successful() {
        return httpStatus >= 200 && httpStatus < 300
                && "0".equals(providerStatusCode)
                && data != null;
    }
}
