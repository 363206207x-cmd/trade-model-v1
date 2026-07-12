package org.example.trademodel.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.List;
import java.util.Map;

public class AiProviderControlledSmoke {
    static final String EXTERNAL_CALL_GATE = "AI_PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS";
    static final String HARNESS_ENTRY_GATE = "AI_PROVIDER_SMOKE_HARNESS_ENTRY";
    static final String HARNESS_ENTRY_CONFIRMATION = "I_CONFIRM_SINGLE_PROVIDER_SMOKE";
    static final String CALL_COUNT_FILE = "AI_PROVIDER_SMOKE_CALL_COUNT_FILE";
    static final String TARGET = "AI_PROVIDER_SMOKE_TARGET";
    static final String DIAGNOSTIC_GATE = "AI_PROVIDER_SMOKE_DIAGNOSTIC";
    static final String GEMINI_DIAGNOSTIC_MODE = "GEMINI_DIAGNOSTIC_MODE";
    static final int DEFAULT_SMOKE_MAX_OUTPUT_TOKENS = 128;
    static final int GEMINI_SMOKE_MAX_OUTPUT_TOKENS = 512;
    static final long DEFAULT_SMOKE_TIMEOUT_MS = 15_000L;
    static final long GEMINI_SMOKE_TIMEOUT_MS = 30_000L;

    private final ObjectMapper objectMapper;

    public AiProviderControlledSmoke(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiProviderControlledSmokeResult run(Map<String, String> environment, AiHttpTransport transport) {
        Map<String, String> env = environment == null ? Map.of() : environment;
        String targetValue = trim(env.get(TARGET));
        boolean diagnosticRequested = enabled(env.get(DIAGNOSTIC_GATE));
        GeminiDiagnosticMode diagnosticMode = diagnosticRequested
                ? GeminiDiagnosticMode.parse(env.get(GEMINI_DIAGNOSTIC_MODE)) : null;
        String diagnosticLabel = diagnosticRequested
                ? diagnosticMode == null ? "--" : diagnosticMode.name() : null;
        if (diagnosticRequested && targetValue.isBlank()) {
            targetValue = AiProviderName.GEMINI.name();
        }
        AiProviderName provider = provider(targetValue);
        String providerName = provider == null ? targetValue : provider.name();
        String model = provider == null ? null : model(env, provider);
        long timeoutLimitMs = smokeTimeoutMs(provider);

        if (diagnosticRequested && (diagnosticMode == null || provider != AiProviderName.GEMINI)) {
            return result(AiProviderName.GEMINI.name(), model, "NOT_CHECKED", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.FAIL_INVALID_TARGET, 0,
                    null, diagnosticLabel);
        }
        if (!enabled(env.get(EXTERNAL_CALL_GATE))) {
            return result(providerName, model, "NOT_CHECKED", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.SKIPPED_EXTERNAL_CALLS_DISABLED, 0,
                    null, diagnosticLabel);
        }
        if (!HARNESS_ENTRY_CONFIRMATION.equals(env.get(HARNESS_ENTRY_GATE))) {
            return result(providerName, model, "NOT_CHECKED", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.SKIPPED_HARNESS_ENTRY_MISSING, 0,
                    null, diagnosticLabel);
        }
        if (provider == null) {
            return result(providerName, model, "NOT_CHECKED", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.FAIL_INVALID_TARGET, 0,
                    null, diagnosticLabel);
        }
        if (!enabled(env.get("TRADE_MODEL_AI_ENABLED"))
                || !enabled(env.get(providerEnabledVariable(provider)))) {
            return result(provider.name(), model, "PROVIDER_DISABLED", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.SKIPPED_PROVIDER_DISABLED, 0,
                    null, diagnosticLabel);
        }

        String apiKey = env.get(apiKeyVariable(provider));
        if (apiKey == null || apiKey.isBlank()) {
            return result(provider.name(), model, "MISSING", "NOT_RUN", "NOT_RUN",
                    false, false, 0, AiProviderControlledSmokeStatus.SKIPPED_MISSING_API_KEY, 0,
                    null, diagnosticLabel);
        }

        CountingTransport countingTransport = new CountingTransport(transport, env.get(CALL_COUNT_FILE));
        AiProviderClient providerClient = client(provider, model, apiKey, countingTransport, timeoutLimitMs);
        AiProviderRequest smokeRequest = provider == AiProviderName.GEMINI
                ? fixedGeminiReviewRequest() : fixedSchemaOnlyRequest();
        if (provider == AiProviderName.GEMINI && !validGeminiReviewFixture(smokeRequest)) {
            return result(provider.name(), model, "KEY_PRESENT_NOT_EXPOSED", "NOT_RUN", "FAIL",
                    false, false, 0, AiProviderControlledSmokeStatus.FAIL_RESPONSE_SCHEMA, 0,
                    null, diagnosticLabel);
        }
        AiProviderReviewResult review = diagnosticMode == null
                ? providerClient.review(smokeRequest, timeoutLimitMs)
                : runGeminiDiagnostic((GeminiProviderClient) providerClient, countingTransport,
                        diagnosticMode, model, timeoutLimitMs, smokeRequest);
        int statusCode = countingTransport.statusCode();
        AiProviderControlledSmokeStatus status = classify(review, statusCode);
        boolean parsed = review != null && review.successful();
        boolean tokenUsage = review != null && (review.getInputTokens() != null
                || review.getOutputTokens() != null || review.getTotalTokens() != null);
        boolean requestId = review != null && review.getProviderRequestId() != null
                && !review.getProviderRequestId().isBlank();
        long latency = review == null || review.getLatencyMs() == null ? 0L : review.getLatencyMs();
        GeminiRequestDiagnostic requestDiagnostic = provider == AiProviderName.GEMINI
                ? GeminiRequestDiagnostic.analyze(objectMapper, model, countingTransport.request()) : null;

        return result(provider.name(), model, "KEY_PRESENT_NOT_EXPOSED", httpStatusClass(statusCode, status),
                parsed ? "PASS" : "FAIL", tokenUsage, requestId, latency, status,
                countingTransport.requestCount(), review, diagnosticLabel, requestDiagnostic);
    }

    private AiProviderReviewResult runGeminiDiagnostic(
            GeminiProviderClient client, CountingTransport transport,
            GeminiDiagnosticMode mode, String model, long timeoutLimitMs,
            AiProviderRequest smokeRequest) {
        long started = System.nanoTime();
        try {
            AiHttpRequest request = client.buildControlledSmokeHttpRequest(
                    smokeRequest, timeoutLimitMs, model);
            applyDiagnosticMode(client, request, mode);
            AiHttpResponse response = transport.post(request);
            long latencyMs = elapsedMs(started);
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                return client.httpFailure(response, latencyMs);
            }

            var payload = mode == GeminiDiagnosticMode.A
                    ? client.extractDiagnosticPayload(response)
                    : client.extractPayload(response);
            AiProviderReviewResult result = switch (mode) {
                case A -> plainTextDiagnostic(payload.content(), latencyMs);
                case B, C -> roleSchemaDiagnostic(client, payload, latencyMs);
            };
            result.setProviderRequestId(payload.providerRequestId());
            result.setInputTokens(payload.inputTokens());
            result.setOutputTokens(payload.outputTokens());
            result.setTotalTokens(payload.totalTokens());
            return result;
        } catch (HttpTimeoutException exception) {
            return client.failure(AiProviderCallStatus.TIMEOUT, "PROVIDER_TIMEOUT", elapsedMs(started));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return client.failure(AiProviderCallStatus.TIMEOUT, "PROVIDER_TIMEOUT", elapsedMs(started));
        } catch (JsonProcessingException exception) {
            return client.failure(AiProviderCallStatus.INVALID_RESPONSE,
                    "PROVIDER_RESPONSE_SCHEMA", elapsedMs(started));
        } catch (IOException exception) {
            return client.failure(AiProviderCallStatus.FAILED, "PROVIDER_IO_FAILURE", elapsedMs(started));
        } catch (Exception exception) {
            return client.failure(AiProviderCallStatus.FAILED, "PROVIDER_FAILURE", elapsedMs(started));
        }
    }

    private void applyDiagnosticMode(
            GeminiProviderClient client, AiHttpRequest request, GeminiDiagnosticMode mode) throws Exception {
        ObjectNode body = (ObjectNode) objectMapper.readTree(request.getBody());
        ObjectNode generation = (ObjectNode) body.path("generationConfig");
        if (mode == GeminiDiagnosticMode.A) {
            generation.remove(List.of("responseMimeType", "responseJsonSchema"));
            setDiagnosticInstruction(body, "Return one short plain-text capability response.");
        } else if (mode == GeminiDiagnosticMode.B) {
            generation.remove("responseJsonSchema");
        }
        request.setBody(objectMapper.writeValueAsString(body));
        if (mode == GeminiDiagnosticMode.C) {
            client.applyStrictSchemaForDiagnostic(request);
        }
    }

    private static void setDiagnosticInstruction(ObjectNode body, String instruction) {
        ((ObjectNode) body.path("systemInstruction").path("parts").get(0)).put("text", instruction);
    }

    private static AiProviderReviewResult plainTextDiagnostic(String content, long latencyMs) {
        if (content == null || content.isBlank()) {
            return diagnosticInvalid("GEMINI_DIAGNOSTIC_EMPTY_TEXT", latencyMs);
        }
        return diagnosticSuccess("GEMINI_DIAGNOSTIC_PLAIN_TEXT", latencyMs);
    }

    private AiProviderReviewResult roleSchemaDiagnostic(
            GeminiProviderClient client, AbstractSafeAiProviderClient.ProviderPayload payload,
            long latencyMs) {
        AiProviderReviewResult result = new AiProviderResponseParser(objectMapper).parse(
                AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW, payload.content());
        client.enrichParsedResult(result, payload);
        result.setLatencyMs(latencyMs);
        return result;
    }

    private static AiProviderReviewResult diagnosticSuccess(String reasonCode, long latencyMs) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(AiProviderName.GEMINI);
        result.setRole(AiProviderRole.GEMINI_CONSISTENCY_REVIEW);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setStance(AiReviewStance.ABSTAIN);
        result.setConflictLevel(AiReviewConflictLevel.NONE);
        result.setReasonCodes(List.of(reasonCode));
        result.setSummary(reasonCode);
        result.setLatencyMs(latencyMs);
        return result;
    }

    private static AiProviderReviewResult diagnosticInvalid(String reasonCode, long latencyMs) {
        AiProviderReviewResult result = AiProviderReviewResult.skipped(
                AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                AiProviderCallStatus.INVALID_RESPONSE, reasonCode);
        result.setErrorCode(reasonCode);
        result.setLatencyMs(latencyMs);
        return result;
    }

    private AiProviderClient client(AiProviderName provider, String model, String apiKey,
                                    AiHttpTransport transport, long timeoutLimitMs) {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setRequestTimeoutMs((int) timeoutLimitMs);
        properties.setOverallTimeoutMs((int) timeoutLimitMs);
        properties.setMaxInputChars(4_000);
        properties.setMaxOutputTokens(provider == AiProviderName.GEMINI
                ? GEMINI_SMOKE_MAX_OUTPUT_TOKENS : DEFAULT_SMOKE_MAX_OUTPUT_TOKENS);
        properties.setDailyBudgetUsd(BigDecimal.ZERO);
        properties.setPerAnalysisBudgetUsd(BigDecimal.ZERO);

        AiProviderProperties providerProperties = switch (provider) {
            case OPENAI -> properties.getOpenai();
            case GEMINI -> properties.getGemini();
            case XAI -> properties.getXai();
        };
        providerProperties.setEnabled(true);
        providerProperties.setApiKey(apiKey);
        if (provider == AiProviderName.OPENAI) {
            GptFinalModelRoutingProperties routing = providerProperties.getGptFinal();
            routing.setFastModel(model);
            routing.setReasoningModel(model);
            routing.setFallbackModels(List.of("gpt-5.5", "gpt-5.4"));
            routing.setFallbackEnabled(false);
        } else {
            providerProperties.setModel(model);
        }
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

    static AiProviderRequest fixedGeminiReviewRequest() {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("controlled-gemini-review-smoke");
        request.setTraceId("controlled-gemini-review-smoke");
        request.setSymbol("BTCUSDT");
        request.setTimeframe("15m");
        request.setRuleMarketBias("BULLISH");
        request.setRuleConfidence("MEDIUM");
        request.setRuleRiskLevel("MEDIUM");
        request.setRuleWorthOpening(Boolean.FALSE);
        request.setDataQualityScore(82);
        request.setTrendStructureScore(68);
        request.setMultiTimeframeState("5m BULLISH; 15m BULLISH; 1h NEUTRAL");
        request.setExternalContextState("ABSENT");
        request.setEvidenceSummary(
                "Multi-timeframe summary: 5m and 15m support bullish structure; 1h remains neutral.");
        request.setScoreSummary(
                "Rule decision summary: BULLISH with MEDIUM confidence and MEDIUM risk; worthOpening=false.");
        request.setDecisionFacts(Map.ofEntries(
                Map.entry("roleContext", "GEMINI_REVIEW"),
                Map.entry("multiTimeframeSummary",
                        "5m and 15m support bullish structure; 1h remains neutral."),
                Map.entry("ruleDecisionSummary",
                        "BULLISH, MEDIUM confidence, MEDIUM risk, worthOpening=false."),
                Map.entry("conflictReviewRequest",
                        "Review consistency between the multi-timeframe evidence and rule decision."),
                Map.entry("outputInstruction", "Return ONLY JSON."),
                Map.entry("requiredOutputFields",
                        List.of("stance", "conflictLevel", "reasonCodes", "summary")),
                Map.entry("insufficientEvidenceFallback", Map.of(
                        "stance", "ABSTAIN",
                        "conflictLevel", "NONE",
                        "reasonCodes", List.of("INSUFFICIENT_DATA"),
                        "summary", "Insufficient evidence")),
                Map.entry("reviewOnly", true),
                Map.entry("manualReviewOnly", true),
                Map.entry("notTradeInstruction", true),
                Map.entry("notExecutable", true),
                Map.entry("notAutoTrading", true),
                Map.entry("notOrderExecution", true),
                Map.entry("notUserPositionCreation", true),
                Map.entry("notPositionMutation", true),
                Map.entry("notStateMachineOverride", true),
                Map.entry("ruleDirectionPreserved", true)));
        return request;
    }

    static boolean validGeminiReviewFixture(AiProviderRequest request) {
        if (request == null || trim(request.getSymbol()).isBlank()
                || trim(request.getEvidenceSummary()).isBlank()
                || trim(request.getScoreSummary()).isBlank()) {
            return false;
        }
        Map<String, Object> facts = request.getDecisionFacts();
        return "GEMINI_REVIEW".equals(facts.get("roleContext"))
                && nonBlankFact(facts, "multiTimeframeSummary")
                && nonBlankFact(facts, "ruleDecisionSummary")
                && nonBlankFact(facts, "conflictReviewRequest")
                && "Return ONLY JSON.".equals(facts.get("outputInstruction"))
                && List.of("stance", "conflictLevel", "reasonCodes", "summary")
                .equals(facts.get("requiredOutputFields"))
                && Map.of(
                        "stance", "ABSTAIN",
                        "conflictLevel", "NONE",
                        "reasonCodes", List.of("INSUFFICIENT_DATA"),
                        "summary", "Insufficient evidence")
                .equals(facts.get("insufficientEvidenceFallback"));
    }

    private static boolean nonBlankFact(Map<String, Object> facts, String key) {
        Object value = facts.get(key);
        return value instanceof String text && !text.isBlank();
    }

    private static AiProviderControlledSmokeStatus classify(AiProviderReviewResult review, int statusCode) {
        if (review != null && review.successful()) {
            return AiProviderControlledSmokeStatus.PASS;
        }
        String code = review == null ? "" : trim(review.getErrorCode());
        if ("PROVIDER_AUTH_FAILURE".equals(code)) {
            return AiProviderControlledSmokeStatus.FAIL_AUTH;
        }
        if (AiProviderErrorReason.GEMINI_AUTH_REJECTED.name().equals(code)) {
            return AiProviderControlledSmokeStatus.FAIL_AUTH;
        }
        if ("PROVIDER_BILLING_OR_CREDITS".equals(code)) {
            return AiProviderControlledSmokeStatus.FAIL_BILLING_OR_CREDITS;
        }
        if ("PROVIDER_MODEL_NOT_FOUND".equals(code)) {
            return AiProviderControlledSmokeStatus.FAIL_MODEL_NOT_FOUND;
        }
        if (AiProviderErrorReason.GEMINI_MODEL_CAPABILITY_ERROR.name().equals(code)) {
            return AiProviderControlledSmokeStatus.FAIL_MODEL_NOT_FOUND;
        }
        if ("PROVIDER_RATE_LIMITED".equals(code)
                || AiProviderErrorReason.GEMINI_RATE_LIMITED.name().equals(code)
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
            case OPENAI -> "gpt-5.6-luna";
            case GEMINI -> "gemini-2.5-pro";
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
        return provider == AiProviderName.OPENAI
                ? "TRADE_MODEL_AI_OPENAI_GPT_FINAL_FAST_MODEL"
                : "TRADE_MODEL_AI_" + provider.name() + "_MODEL";
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

    private static long smokeTimeoutMs(AiProviderName provider) {
        if (provider == null) {
            return 0L;
        }
        return provider == AiProviderName.GEMINI
                ? GEMINI_SMOKE_TIMEOUT_MS
                : DEFAULT_SMOKE_TIMEOUT_MS;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static long elapsedMs(long started) {
        return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }

    private static String httpStatusClass(int statusCode, AiProviderControlledSmokeStatus status) {
        if (status == AiProviderControlledSmokeStatus.FAIL_TIMEOUT) {
            return "TIMEOUT";
        }
        if (statusCode < 100 || statusCode > 599) {
            return "NOT_AVAILABLE";
        }
        return statusCode / 100 + "XX";
    }

    private static AiProviderControlledSmokeErrorCategory errorCategory(
            String provider, AiProviderControlledSmokeStatus status, AiProviderReviewResult review) {
        AiProviderErrorReason reason = providerErrorReason(provider, review);
        if (reason != null) {
            return switch (reason) {
                case GEMINI_HTTP_400_INVALID_REQUEST -> AiProviderControlledSmokeErrorCategory.INVALID_REQUEST;
                case GEMINI_STRUCTURED_OUTPUT_UNSUPPORTED ->
                        AiProviderControlledSmokeErrorCategory.SCHEMA_UNSUPPORTED;
                case GEMINI_MODEL_CAPABILITY_ERROR ->
                        AiProviderControlledSmokeErrorCategory.MODEL_CAPABILITY_ERROR;
                case GEMINI_AUTH_REJECTED -> AiProviderControlledSmokeErrorCategory.AUTH;
                case GEMINI_RATE_LIMITED -> AiProviderControlledSmokeErrorCategory.RATE_LIMIT;
                case GEMINI_HTTP_5XX_INTERNAL ->
                        AiProviderControlledSmokeErrorCategory.PROVIDER_INTERNAL_ERROR;
                case GEMINI_UNKNOWN_PROVIDER_ERROR ->
                        AiProviderControlledSmokeErrorCategory.UNKNOWN_PROVIDER_ERROR;
            };
        }
        return switch (status) {
            case FAIL_TIMEOUT -> AiProviderControlledSmokeErrorCategory.TIMEOUT;
            case FAIL_AUTH -> AiProviderControlledSmokeErrorCategory.AUTH;
            case FAIL_MODEL_NOT_FOUND -> AiProviderControlledSmokeErrorCategory.MODEL_NOT_FOUND;
            case FAIL_RATE_LIMIT -> AiProviderControlledSmokeErrorCategory.RATE_LIMIT;
            case FAIL_RESPONSE_SCHEMA -> AiProviderControlledSmokeErrorCategory.RESPONSE_SCHEMA;
            case FAIL_BILLING_OR_CREDITS, FAIL_PROVIDER_HTTP, FAIL_PROVIDER_IO, FAIL_UNEXPECTED ->
                    AiProviderControlledSmokeErrorCategory.PROVIDER_ERROR;
            default -> null;
        };
    }

    private static AiProviderErrorReason providerErrorReason(
            String provider, AiProviderReviewResult review) {
        if (!AiProviderName.GEMINI.name().equalsIgnoreCase(trim(provider)) || review == null) {
            return null;
        }
        try {
            return AiProviderErrorReason.valueOf(trim(review.getErrorCode()));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static AiProviderControlledSmokeResult result(
            String provider, String model, String authStatus, String httpStatusClass,
            String parseStatus, boolean tokenUsage, boolean requestId, long latency,
            AiProviderControlledSmokeStatus status, int calls) {
        return result(provider, model, authStatus, httpStatusClass, parseStatus, tokenUsage,
                requestId, latency, status, calls, null, null);
    }

    private static AiProviderControlledSmokeResult result(
            String provider, String model, String authStatus, String httpStatusClass,
            String parseStatus, boolean tokenUsage, boolean requestId, long latency,
            AiProviderControlledSmokeStatus status, int calls,
            AiProviderReviewResult review) {
        return result(provider, model, authStatus, httpStatusClass, parseStatus, tokenUsage,
                requestId, latency, status, calls, review, null);
    }

    private static AiProviderControlledSmokeResult result(
            String provider, String model, String authStatus, String httpStatusClass,
            String parseStatus, boolean tokenUsage, boolean requestId, long latency,
            AiProviderControlledSmokeStatus status, int calls,
            AiProviderReviewResult review, String diagnosticMode) {
        return result(provider, model, authStatus, httpStatusClass, parseStatus, tokenUsage,
                requestId, latency, status, calls, review, diagnosticMode, null);
    }

    private static AiProviderControlledSmokeResult result(
            String provider, String model, String authStatus, String httpStatusClass,
            String parseStatus, boolean tokenUsage, boolean requestId, long latency,
            AiProviderControlledSmokeStatus status, int calls,
            AiProviderReviewResult review, String diagnosticMode,
            GeminiRequestDiagnostic requestDiagnostic) {
        return new AiProviderControlledSmokeResult(provider, diagnosticMode, model, authStatus, httpStatusClass,
                errorCategory(provider, status, review), providerErrorReason(provider, review),
                parseStatus, tokenUsage, requestId, smokeTimeoutMs(provider(provider)), latency,
                status, calls,
                review == null ? null : review.getSchemaDiagnostic(),
                review == null ? null : review.getGeminiResponseShapeDiagnostic(),
                requestDiagnostic);
    }

    private enum GeminiDiagnosticMode {
        A,
        B,
        C;

        private static GeminiDiagnosticMode parse(String value) {
            try {
                return valueOf(trim(value).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
    }

    private static final class CountingTransport implements AiHttpTransport {
        private final AiHttpTransport delegate;
        private final Path callCountFile;
        private int requestCount;
        private int statusCode;
        private AiHttpRequest request;

        private CountingTransport(AiHttpTransport delegate, String callCountFile) {
            this.delegate = delegate;
            this.callCountFile = trim(callCountFile).isBlank() ? null : Path.of(callCountFile);
        }

        @Override
        public AiHttpResponse post(AiHttpRequest request) throws IOException, InterruptedException {
            if (requestCount >= 1) {
                throw new IOException("CONTROLLED_SMOKE_REQUEST_LIMIT");
            }
            this.request = request;
            writeExternalCallMarker();
            requestCount++;
            AiHttpResponse response = delegate.post(request);
            statusCode = response == null ? 0 : response.getStatusCode();
            return response;
        }

        private void writeExternalCallMarker() throws IOException {
            if (callCountFile == null) {
                return;
            }
            if (!Files.isRegularFile(callCountFile, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("CONTROLLED_SMOKE_CALL_COUNT_FILE_INVALID");
            }
            Files.writeString(callCountFile, "1\n", StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        private int requestCount() {
            return requestCount;
        }

        private int statusCode() {
            return statusCode;
        }

        private AiHttpRequest request() {
            return request;
        }
    }
}
