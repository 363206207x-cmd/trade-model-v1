package org.example.trademodel.ai;

import java.util.List;

public record AiProviderControlledSmokeResult(
        String provider,
        String model,
        String authStatus,
        String httpStatusClass,
        String responseParseStatus,
        boolean tokenUsagePresent,
        boolean requestIdPresent,
        long latencyMs,
        AiProviderControlledSmokeStatus status,
        int liveProviderCalls
) {
    public List<String> sanitizedOutputLines() {
        return List.of(
                "AI_PROVIDER: " + display(provider),
                "AI_MODEL: " + display(model),
                "AI_AUTH_STATUS: " + display(authStatus),
                "AI_HTTP_STATUS_CLASS: " + display(httpStatusClass),
                "AI_RESPONSE_PARSE_STATUS: " + display(responseParseStatus),
                "AI_TOKEN_USAGE_PRESENT: " + yesNo(tokenUsagePresent),
                "AI_REQUEST_ID_PRESENT: " + yesNo(requestIdPresent),
                "AI_LATENCY_MS: " + Math.max(0, latencyMs),
                "AI_PROVIDER_LIVE_SMOKE: " + status.name(),
                "LIVE_PROVIDER_CALLS: " + Math.max(0, liveProviderCalls),
                "PRODUCTION_READINESS: BLOCKED");
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private static String yesNo(boolean value) {
        return value ? "YES" : "NO";
    }
}
