package org.example.trademodel.ai;

import java.util.ArrayList;
import java.util.List;

public record AiProviderControlledSmokeResult(
        String provider,
        String model,
        String authStatus,
        String httpStatusClass,
        AiProviderControlledSmokeErrorCategory errorCategory,
        String responseParseStatus,
        boolean tokenUsagePresent,
        boolean requestIdPresent,
        long latencyMs,
        AiProviderControlledSmokeStatus status,
        int liveProviderCalls,
        AiProviderSchemaDiagnostic schemaDiagnostic
) {
    public List<String> sanitizedOutputLines() {
        List<String> lines = new ArrayList<>(List.of(
                "AI_PROVIDER: " + display(provider),
                "AI_MODEL: " + display(model),
                "AI_AUTH_STATUS: " + display(authStatus),
                "AI_HTTP_STATUS_CLASS: " + display(httpStatusClass),
                "AI_ERROR_CATEGORY: " + display(errorCategory == null ? null : errorCategory.name()),
                "AI_RESPONSE_PARSE_STATUS: " + display(responseParseStatus),
                "AI_TOKEN_USAGE_PRESENT: " + yesNo(tokenUsagePresent),
                "AI_REQUEST_ID_PRESENT: " + yesNo(requestIdPresent),
                "AI_LATENCY_MS: " + Math.max(0, latencyMs),
                "AI_PROVIDER_LIVE_SMOKE: " + status.name(),
                "LIVE_PROVIDER_CALLS: " + Math.max(0, liveProviderCalls),
                "PRODUCTION_READINESS: BLOCKED"));
        if (schemaDiagnostic != null) {
            lines.add("GEMINI_SCHEMA_DIAGNOSTIC: FIELD_NAMES_AND_TYPES_ONLY");
            lines.add("EXPECTED_FIELDS: " + join(schemaDiagnostic.expectedFields()));
            lines.add("ACTUAL_FIELDS: " + join(schemaDiagnostic.actualFields()));
            lines.add("MISSING_FIELDS: " + join(schemaDiagnostic.missingFields()));
            lines.add("UNEXPECTED_FIELDS: " + join(schemaDiagnostic.unexpectedFields()));
            lines.add("TYPE_MISMATCH_FIELDS: " + join(schemaDiagnostic.typeMismatchFields()));
        }
        return List.copyOf(lines);
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private static String yesNo(boolean value) {
        return value ? "YES" : "NO";
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? "none" : String.join(", ", values);
    }
}
