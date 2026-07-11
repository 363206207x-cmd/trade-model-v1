package org.example.trademodel.ai;

import java.util.ArrayList;
import java.util.List;

public record AiProviderControlledSmokeResult(
        String provider,
        String diagnosticMode,
        String model,
        String authStatus,
        String httpStatusClass,
        AiProviderControlledSmokeErrorCategory errorCategory,
        AiProviderErrorReason providerErrorReason,
        String responseParseStatus,
        boolean tokenUsagePresent,
        boolean requestIdPresent,
        long timeoutLimitMs,
        long latencyMs,
        AiProviderControlledSmokeStatus status,
        int liveProviderCalls,
        AiProviderSchemaDiagnostic schemaDiagnostic,
        GeminiResponseShapeDiagnostic geminiResponseShapeDiagnostic,
        GeminiRequestDiagnostic geminiRequestDiagnostic
) {
    public List<String> sanitizedOutputLines() {
        List<String> requestLines = requestDiagnosticLines();
        GeminiExtractionDiagnostic extractionDiagnostic = geminiResponseShapeDiagnostic == null
                ? null : geminiResponseShapeDiagnostic.extractionDiagnostic();
        if (extractionDiagnostic != null) {
            List<String> lines = new ArrayList<>(requestLines);
            lines.addAll(List.of(
                    "GEMINI_EXTRACTION_DIAGNOSTIC_STATUS: " + extractionDiagnostic.status(),
                    "CANDIDATES_PRESENT: " + yesNo(extractionDiagnostic.candidatesPresent()),
                    "CANDIDATE_COUNT: " + Math.max(0, extractionDiagnostic.candidateCount()),
                    "CONTENT_PRESENT: " + yesNo(extractionDiagnostic.contentPresent()),
                    "PARTS_PRESENT: " + yesNo(extractionDiagnostic.partsPresent()),
                    "TEXT_NODE_PRESENT: " + yesNo(extractionDiagnostic.textNodePresent()),
                    "TEXT_LENGTH: " + Math.max(0, extractionDiagnostic.textLength()),
                    "EMPTY_TEXT: " + yesNo(extractionDiagnostic.emptyText()),
                    "EXTRACTED_JSON_PARSE_STATUS: " + extractionDiagnostic.jsonParseStatus(),
                    "GEMINI_OUTPUT_CLASS: " + extractionDiagnostic.outputClass().name()));
            return List.copyOf(lines);
        }
        if (geminiResponseShapeDiagnostic != null) {
            List<String> lines = new ArrayList<>(requestLines);
            lines.addAll(List.of(
                    "GEMINI_SCHEMA_DIAGNOSTIC_STATUS: FAILED",
                    "GEMINI_EXPECTED_FIELDS: " + joinCompact(geminiResponseShapeDiagnostic.expectedFields()),
                    "GEMINI_ACTUAL_FIELDS: " + joinCompact(geminiResponseShapeDiagnostic.actualFields()),
                    "GEMINI_MISSING_FIELDS: " + joinCompact(geminiResponseShapeDiagnostic.missingFields()),
                    "GEMINI_UNEXPECTED_FIELDS: " + joinCompact(geminiResponseShapeDiagnostic.unexpectedFields()),
                    "GEMINI_TYPE_MISMATCH: " + joinCompact(geminiResponseShapeDiagnostic.typeMismatchFields())));
            return List.copyOf(lines);
        }
        if (diagnosticMode != null) {
            List<String> lines = new ArrayList<>(requestLines);
            lines.addAll(List.of(
                    "AI_PROVIDER: GEMINI",
                    "AI_DIAGNOSTIC_MODE: " + display(diagnosticMode),
                    "AI_HTTP_STATUS_CLASS: " + display(httpStatusClass),
                    "AI_ERROR_CATEGORY: " + display(errorCategory == null ? null : errorCategory.name()),
                    "AI_RESPONSE_PARSE_STATUS: " + display(responseParseStatus),
                    "AI_LATENCY_MS: " + Math.max(0, latencyMs),
                    "LIVE_PROVIDER_CALLS: " + Math.max(0, liveProviderCalls),
                    "PRODUCTION_READINESS: BLOCKED"));
            return List.copyOf(lines);
        }
        List<String> lines = new ArrayList<>(requestLines);
        lines.addAll(List.of(
                "AI_PROVIDER: " + display(provider),
                "AI_MODEL: " + display(model),
                "AI_AUTH_STATUS: " + display(authStatus),
                "AI_HTTP_STATUS_CLASS: " + display(httpStatusClass),
                "AI_ERROR_CATEGORY: " + display(errorCategory == null ? null : errorCategory.name()),
                "AI_PROVIDER_ERROR_REASON: "
                        + display(providerErrorReason == null ? null : providerErrorReason.name()),
                "AI_RESPONSE_PARSE_STATUS: " + display(responseParseStatus),
                "AI_TOKEN_USAGE_PRESENT: " + yesNo(tokenUsagePresent),
                "AI_REQUEST_ID_PRESENT: " + yesNo(requestIdPresent),
                "AI_TIMEOUT_LIMIT_MS: " + Math.max(0, timeoutLimitMs),
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

    private List<String> requestDiagnosticLines() {
        if (geminiRequestDiagnostic == null) {
            return List.of();
        }
        return List.of(
                "GEMINI_REQUEST_DIAGNOSTIC: SANITIZED",
                "MODEL: " + display(geminiRequestDiagnostic.model()),
                "RESPONSE_MIME_TYPE: " + display(geminiRequestDiagnostic.responseMimeType()),
                "RESPONSE_SCHEMA_PRESENT: " + yesNo(geminiRequestDiagnostic.responseSchemaPresent()),
                "MAX_OUTPUT_TOKENS: " + Math.max(0, geminiRequestDiagnostic.maxOutputTokens()),
                "TEMPERATURE: " + display(geminiRequestDiagnostic.temperature()),
                "SYSTEM_INSTRUCTION_LENGTH: "
                        + Math.max(0, geminiRequestDiagnostic.systemInstructionLength()),
                "USER_INPUT_LENGTH: " + Math.max(0, geminiRequestDiagnostic.userInputLength()),
                "STOP_SEQUENCES_PRESENT: " + yesNo(geminiRequestDiagnostic.stopSequencesPresent()),
                "TOOLS_PRESENT: " + yesNo(geminiRequestDiagnostic.toolsPresent()));
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

    private static String joinCompact(List<String> values) {
        return values == null || values.isEmpty() ? "none" : String.join(",", values);
    }
}
