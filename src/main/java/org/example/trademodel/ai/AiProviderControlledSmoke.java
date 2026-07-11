package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

public class AiProviderControlledSmoke {
    static final String EXTERNAL_CALL_GATE = "AI_PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS";
    static final String TARGET = "AI_PROVIDER_SMOKE_TARGET";
    static final int MAX_OUTPUT_TOKENS = 128;
    static final long REQUEST_TIMEOUT_MS = 5_000L;

    private final ObjectMapper objectMapper;

    public AiProviderControlledSmoke(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiProviderControlledSmokeResult run(Map<String, String> environment, AiHttpTransport transport) {
        Map<String, String> env = environment == null ? Map.of() : environment;
        String targetValue = trim(env.get(TARGET));
        AiProviderName provider = provider(targetValue);
        String providerName = provider == null ? targetValue : provider.name();
        String model = provider == null ? null : model(env, provider);

        if (!enabled(env.get(EXTERNAL_CALL_GATE))) {
            return result(providerName, model, "NOT_CHECKED", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.SKIPPED_EXTERNAL_CALLS_DISABLED, 0);
        }
        if (provider == null) {
            return result(providerName, model, "NOT_CHECKED", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.FAIL_INVALID_TARGET, 0);
        }
        if (!enabled(env.get("TRADE_MODEL_AI_ENABLED"))
                || !enabled(env.get(providerEnabledVariable(provider)))) {
            return result(provider.name(), model, "PROVIDER_DISABLED", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.SKIPPED_PROVIDER_DISABLED, 0);
        }

        String apiKey = env.get(apiKeyVariable(provider));
        if (apiKey == null || apiKey.isBlank()) {
            return result(provider.name(), model, "MISSING", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.SKIPPED_MISSING_API_KEY, 0);
        }

        CountingTransport countingTransport = new CountingTransport(transport);
        AiProviderReviewResult review = client(provider, model, apiKey, countingTransport)
                .review(fixedSchemaOnlyRequest(), REQUEST_TIMEOUT_MS);
        int statusCode = countingTransport.statusCode();
        AiProviderControlledSmokeStatus status = classify(review, statusCode);
        boolean parsed = review != null && review.successful();
        boolean tokenUsage = review != null && (review.getInputTokens() != null
                || review.getOutputTokens() != null || review.getTotalTokens() != null);
        boolean requestId = review != null && review.getProviderRequestId() != null
                && !review.getProviderRequestId().isBlank();
        long latency = review == null || review.getLatencyMs() == null ? 0L : review.getLatencyMs();

        return result(provider.name(), model, "KEY_PRESENT_NOT_EXPOSED", httpStatusClass(statusCode),
                parsed ? "PASS" : "FAIL", tokenUsage, requestId, latency, status,
                countingTransport.requestCount());
    }

    private AiProviderClient client(AiProviderName provider, String model, String apiKey,
                                    AiHttpTransport transport) {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setRequestTimeoutMs((int) REQUEST_TIMEOUT_MS);
        properties.setOverallTimeoutMs((int) REQUEST_TIMEOUT_MS);
        properties.setMaxInputChars(4_000);
        properties.setMaxOutputTokens(MAX_OUTPUT_TOKENS);
        properties.setDailyBudgetUsd(BigDecimal.ZERO);
        properties.setPerAnalysisBudgetUsd(BigDecimal.ZERO);

        AiProviderProperties providerProperties = switch (provider) {
            case OPENAI -> properties.getOpenai();
            case GEMINI -> properties.getGemini();
            case XAI -> properties.getXai();
        };
        providerProperties.setEnabled(true);
        providerProperties.setApiKey(apiKey);
        providerProperties.setModel(model);
        providerProperties.setBaseUrl(baseUrl(provider));
        providerProperties.setRequestsPerMinute(1);

        return switch (provider) {
            case OPENAI -> new OpenAiProviderClient(properties, transport, objectMapper);
            case GEMINI -> new GeminiProviderClient(properties, transport, objectMapper);
            case XAI -> new XaiProviderClient(properties, transport, objectMapper);
        };
    }

    private static AiProviderRequest fixedSchemaOnlyRequest() {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("controlled-ai-schema-smoke");
        request.setTraceId("controlled-ai-schema-smoke");
        request.setSymbol("NON_MARKET_SMOKE");
        request.setTimeframe("NOT_APPLICABLE");
        request.setRuleMarketBias("NOT_APPLICABLE");
        request.setRuleConfidence("NOT_APPLICABLE");
        request.setRuleRiskLevel("NOT_APPLICABLE");
        request.setRuleWorthOpening(Boolean.FALSE);
        request.setMultiTimeframeState("NOT_APPLICABLE");
        request.setExternalContextState("ABSENT");
        request.setEvidenceSummary("Schema-only safety review with no market data and no trading instruction.");
        request.setScoreSummary("NOT_APPLICABLE");
        request.setDecisionFacts(Map.of(
                "reviewOnly", true,
                "manualReviewOnly", true,
                "notTradeInstruction", true,
                "notExecutable", true,
                "notAutoTrading", true,
                "notOrderExecution", true,
                "notUserPositionCreation", true,
                "notPositionMutation", true,
                "notStateMachineOverride", true,
                "ruleDirectionPreserved", true));
        return request;
    }

    private static AiProviderControlledSmokeStatus classify(AiProviderReviewResult review, int statusCode) {
        if (review != null && review.successful()) {
            return AiProviderControlledSmokeStatus.PASS;
        }
        String code = review == null ? "" : trim(review.getErrorCode());
        if ("PROVIDER_AUTH_FAILURE".equals(code)) {
            return AiProviderControlledSmokeStatus.FAIL_AUTH;
        }
        if ("PROVIDER_BILLING_OR_CREDITS".equals(code)) {
            return AiProviderControlledSmokeStatus.FAIL_BILLING_OR_CREDITS;
        }
        if ("PROVIDER_MODEL_NOT_FOUND".equals(code)) {
            return AiProviderControlledSmokeStatus.FAIL_MODEL_NOT_FOUND;
        }
        if ("PROVIDER_RATE_LIMITED".equals(code)
                || review != null && review.getCallStatus() == AiProviderCallStatus.RATE_LIMITED) {
            return AiProviderControlledSmokeStatus.FAIL_RATE_LIMIT;
        }
        if ("PROVIDER_TIMEOUT".equals(code)
                || review != null && review.getCallStatus() == AiProviderCallStatus.TIMEOUT) {
            return AiProviderControlledSmokeStatus.FAIL_TIMEOUT;
        }
        if ("PROVIDER_IO_FAILURE".equals(code)) {
            return AiProviderControlledSmokeStatus.FAIL_PROVIDER_IO;
        }
        if (statusCode >= 200 && statusCode < 300
                || review != null && review.getCallStatus() == AiProviderCallStatus.INVALID_RESPONSE) {
            return AiProviderControlledSmokeStatus.FAIL_RESPONSE_SCHEMA;
        }
        if (statusCode >= 300) {
            return AiProviderControlledSmokeStatus.FAIL_PROVIDER_HTTP;
        }
        return AiProviderControlledSmokeStatus.FAIL_UNEXPECTED;
    }

    private static AiProviderName provider(String value) {
        if (value == null || value.isBlank() || value.contains(",") || value.contains("*")) {
            return null;
        }
        try {
            return AiProviderName.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String model(Map<String, String> env, AiProviderName provider) {
        String configured = trim(env.get(modelVariable(provider)));
        if (!configured.isBlank()) {
            return configured;
        }
        return switch (provider) {
            case OPENAI -> "gpt-5.6-sol";
            case GEMINI -> "gemini-3.5-flash";
            case XAI -> "grok-4.5";
        };
    }

    private static String baseUrl(AiProviderName provider) {
        return switch (provider) {
            case OPENAI -> "https://api.openai.com";
            case GEMINI -> "https://generativelanguage.googleapis.com";
            case XAI -> "https://api.x.ai";
        };
    }

    private static String providerEnabledVariable(AiProviderName provider) {
        return "TRADE_MODEL_AI_" + provider.name() + "_ENABLED";
    }

    private static String modelVariable(AiProviderName provider) {
        return "TRADE_MODEL_AI_" + provider.name() + "_MODEL";
    }

    private static String apiKeyVariable(AiProviderName provider) {
        return switch (provider) {
            case OPENAI -> "OPENAI_API_KEY";
            case GEMINI -> "GEMINI_API_KEY";
            case XAI -> "XAI_API_KEY";
        };
    }

    private static boolean enabled(String value) {
        return "true".equalsIgnoreCase(trim(value));
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String httpStatusClass(int statusCode) {
        if (statusCode < 100) {
            return "NOT_AVAILABLE";
        }
        return statusCode / 100 + "XX";
    }

    private static AiProviderControlledSmokeResult result(
            String provider, String model, String authStatus, String httpStatusClass,
            String parseStatus, boolean tokenUsage, boolean requestId, long latency,
            AiProviderControlledSmokeStatus status, int calls) {
        return new AiProviderControlledSmokeResult(provider, model, authStatus, httpStatusClass,
                parseStatus, tokenUsage, requestId, latency, status, calls);
    }

    private static final class CountingTransport implements AiHttpTransport {
        private final AiHttpTransport delegate;
        private int requestCount;
        private int statusCode;

        private CountingTransport(AiHttpTransport delegate) {
            this.delegate = delegate;
        }

        @Override
        public AiHttpResponse post(AiHttpRequest request) throws IOException, InterruptedException {
            if (requestCount >= 1) {
                throw new IOException("CONTROLLED_SMOKE_REQUEST_LIMIT");
            }
            requestCount++;
            AiHttpResponse response = delegate.post(request);
            statusCode = response == null ? 0 : response.getStatusCode();
            return response;
        }

        private int requestCount() {
            return requestCount;
        }

        private int statusCode() {
            return statusCode;
        }
    }
}
