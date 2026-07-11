package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderControlledSmokeTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiProviderControlledSmoke smoke = new AiProviderControlledSmoke(objectMapper);

    @Test
    void controlledLiveSmokeEntryPoint() {
        Map<String, String> environment = System.getenv();
        if (!"I_CONFIRM_SINGLE_PROVIDER_SMOKE".equals(
                environment.get("AI_PROVIDER_SMOKE_HARNESS_ENTRY"))) {
            skipped().sanitizedOutputLines().forEach(System.out::println);
            return;
        }

        AiProviderControlledSmokeResult result = smoke.run(environment, new JdkAiHttpTransport());
        result.sanitizedOutputLines().forEach(System.out::println);
        assertThat(result.liveProviderCalls()).isLessThanOrEqualTo(1);
    }

    @Test
    void defaultExecutionMakesZeroExternalCalls() {
        FakeTransport transport = FakeTransport.responding(validOpenAiResponse(true, true));

        AiProviderControlledSmokeResult result = smoke.run(Map.of(), transport);

        assertThat(result.status()).isEqualTo(
                AiProviderControlledSmokeStatus.SKIPPED_EXTERNAL_CALLS_DISABLED);
        assertThat(result.liveProviderCalls()).isZero();
        assertThat(transport.calls).isZero();
    }

    @Test
    void closedExternalGateWinsBeforeTargetValidation() {
        FakeTransport transport = FakeTransport.responding(validOpenAiResponse(true, true));
        Map<String, String> environment = Map.of("AI_PROVIDER_SMOKE_TARGET", "ALL");

        AiProviderControlledSmokeResult result = smoke.run(environment, transport);

        assertThat(result.status()).isEqualTo(
                AiProviderControlledSmokeStatus.SKIPPED_EXTERNAL_CALLS_DISABLED);
        assertThat(transport.calls).isZero();
    }

    @Test
    void missingKeyAndDisabledProviderNeverCallNetwork() {
        FakeTransport transport = FakeTransport.responding(validOpenAiResponse(true, true));

        AiProviderControlledSmokeResult missingKey = smoke.run(enabled("OPENAI", false), transport);
        Map<String, String> disabled = enabled("OPENAI", true);
        disabled.put("TRADE_MODEL_AI_OPENAI_ENABLED", "false");
        AiProviderControlledSmokeResult providerDisabled = smoke.run(disabled, transport);

        assertThat(missingKey.status()).isEqualTo(AiProviderControlledSmokeStatus.SKIPPED_MISSING_API_KEY);
        assertThat(providerDisabled.status()).isEqualTo(
                AiProviderControlledSmokeStatus.SKIPPED_PROVIDER_DISABLED);
        assertThat(transport.calls).isZero();
    }

    @ParameterizedTest
    @CsvSource({"ALL", "MULTI", "THREE", "'OPENAI,GEMINI'", "'*'"})
    void illegalTargetsFailClosedWithoutNetwork(String target) {
        FakeTransport transport = FakeTransport.responding(validOpenAiResponse(true, true));
        Map<String, String> environment = enabled(target, true);

        AiProviderControlledSmokeResult result = smoke.run(environment, transport);

        assertThat(result.status()).isEqualTo(AiProviderControlledSmokeStatus.FAIL_INVALID_TARGET);
        assertThat(result.liveProviderCalls()).isZero();
        assertThat(transport.calls).isZero();
    }

    @ParameterizedTest
    @CsvSource({"OPENAI", "GEMINI", "XAI"})
    void eachAllowedProviderUsesExactlyOneRequest(String target) {
        FakeTransport transport = FakeTransport.responding(validResponse(target, true, true));

        AiProviderControlledSmokeResult result = smoke.run(enabled(target, true), transport);

        assertThat(result.status()).isEqualTo(AiProviderControlledSmokeStatus.PASS);
        assertThat(result.liveProviderCalls()).isEqualTo(1);
        assertThat(transport.calls).isEqualTo(1);
        assertThat(result.tokenUsagePresent()).isTrue();
        assertThat(result.requestIdPresent()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "401,secret body,FAIL_AUTH",
            "403,billing body,FAIL_AUTH",
            "402,insufficient credits private detail,FAIL_BILLING_OR_CREDITS",
            "404,model missing private detail,FAIL_MODEL_NOT_FOUND",
            "429,rate limit private detail,FAIL_RATE_LIMIT",
            "500,provider private detail,FAIL_PROVIDER_HTTP"
    })
    void providerHttpFailuresMapDeterministically(int status, String body,
                                                  AiProviderControlledSmokeStatus expected) {
        FakeTransport transport = FakeTransport.responding(new AiHttpResponse(status, body, Map.of()));

        AiProviderControlledSmokeResult result = smoke.run(enabled("OPENAI", true), transport);

        assertThat(result.status()).isEqualTo(expected);
        assertThat(result.liveProviderCalls()).isEqualTo(1);
        assertThat(String.join("\n", result.sanitizedOutputLines())).doesNotContain(body);
    }

    @Test
    void timeoutAndIoFailuresMapWithoutRetry() {
        FakeTransport timeout = FakeTransport.throwing(new HttpTimeoutException("private timeout detail"));
        FakeTransport io = FakeTransport.throwing(new IOException("private IO detail"));

        AiProviderControlledSmokeResult timeoutResult = smoke.run(enabled("OPENAI", true), timeout);
        AiProviderControlledSmokeResult ioResult = smoke.run(enabled("OPENAI", true), io);

        assertThat(timeoutResult.status()).isEqualTo(AiProviderControlledSmokeStatus.FAIL_TIMEOUT);
        assertThat(timeoutResult.httpStatusClass()).isEqualTo("TIMEOUT");
        assertThat(timeoutResult.errorCategory()).isEqualTo(AiProviderControlledSmokeErrorCategory.TIMEOUT);
        assertThat(ioResult.status()).isEqualTo(AiProviderControlledSmokeStatus.FAIL_PROVIDER_IO);
        assertThat(ioResult.errorCategory()).isEqualTo(AiProviderControlledSmokeErrorCategory.PROVIDER_ERROR);
        assertThat(timeout.calls).isEqualTo(1);
        assertThat(io.calls).isEqualTo(1);
    }

    @Test
    void geminiTimeoutIsClassifiedWithoutResponseDetails() {
        FakeTransport transport = FakeTransport.throwing(new HttpTimeoutException("private Gemini timeout"));

        AiProviderControlledSmokeResult result = smoke.run(enabled("GEMINI", true), transport);

        assertThat(result.status()).isEqualTo(AiProviderControlledSmokeStatus.FAIL_TIMEOUT);
        assertThat(result.httpStatusClass()).isEqualTo("TIMEOUT");
        assertThat(result.errorCategory()).isEqualTo(AiProviderControlledSmokeErrorCategory.TIMEOUT);
        assertThat(result.timeoutLimitMs()).isEqualTo(30_000L);
        assertThat(transport.calls).isEqualTo(1);
        assertThat(String.join("\n", result.sanitizedOutputLines()))
                .contains(
                        "AI_HTTP_STATUS_CLASS: TIMEOUT",
                        "AI_ERROR_CATEGORY: TIMEOUT",
                        "AI_TIMEOUT_LIMIT_MS: 30000")
                .doesNotContain("private Gemini timeout");
    }

    @Test
    void geminiInvalidSchemaRequestIsSanitizedAsInvalidRequest() {
        assertGeminiHttpFailure(400, "INVALID_ARGUMENT",
                "responseJsonSchema is invalid PRIVATE_INVALID_SCHEMA_DETAIL",
                AiProviderControlledSmokeStatus.FAIL_PROVIDER_HTTP,
                AiProviderControlledSmokeErrorCategory.INVALID_REQUEST,
                AiProviderErrorReason.GEMINI_HTTP_400_INVALID_REQUEST);
    }

    @Test
    void geminiUnsupportedStructuredOutputIsClassifiedWithoutBodyExposure() {
        assertGeminiHttpFailure(400, "INVALID_ARGUMENT",
                "structured output is unsupported PRIVATE_UNSUPPORTED_DETAIL",
                AiProviderControlledSmokeStatus.FAIL_PROVIDER_HTTP,
                AiProviderControlledSmokeErrorCategory.SCHEMA_UNSUPPORTED,
                AiProviderErrorReason.GEMINI_STRUCTURED_OUTPUT_UNSUPPORTED);
    }

    @Test
    void geminiModelCapabilityFailureIsClassifiedWithoutBodyExposure() {
        assertGeminiHttpFailure(404, "NOT_FOUND", "PRIVATE_MODEL_CAPABILITY_DETAIL",
                AiProviderControlledSmokeStatus.FAIL_MODEL_NOT_FOUND,
                AiProviderControlledSmokeErrorCategory.MODEL_CAPABILITY_ERROR,
                AiProviderErrorReason.GEMINI_MODEL_CAPABILITY_ERROR);
    }

    @Test
    void geminiAuthFailureIsClassifiedWithoutBodyExposure() {
        assertGeminiHttpFailure(403, "PERMISSION_DENIED", "PRIVATE_AUTH_DETAIL",
                AiProviderControlledSmokeStatus.FAIL_AUTH,
                AiProviderControlledSmokeErrorCategory.AUTH,
                AiProviderErrorReason.GEMINI_AUTH_REJECTED);
    }

    @Test
    void geminiRateLimitIsClassifiedWithoutBodyExposure() {
        assertGeminiHttpFailure(429, "RESOURCE_EXHAUSTED", "PRIVATE_RATE_DETAIL",
                AiProviderControlledSmokeStatus.FAIL_RATE_LIMIT,
                AiProviderControlledSmokeErrorCategory.RATE_LIMIT,
                AiProviderErrorReason.GEMINI_RATE_LIMITED);
    }

    @Test
    void geminiServerFailureIsSanitizedAsProviderInternalError() {
        assertGeminiHttpFailure(500, "INTERNAL", "PRIVATE_INTERNAL_DETAIL",
                AiProviderControlledSmokeStatus.FAIL_PROVIDER_HTTP,
                AiProviderControlledSmokeErrorCategory.PROVIDER_INTERNAL_ERROR,
                AiProviderErrorReason.GEMINI_HTTP_5XX_INTERNAL);
    }

    @Test
    void geminiSuccessUsesGenerateContentContractAndSmokeOnlyTimeout() throws Exception {
        FakeTransport transport = FakeTransport.responding(validGeminiResponse(true, true));

        AiProviderControlledSmokeResult result = smoke.run(enabled("GEMINI", true), transport);

        assertThat(result.status()).isEqualTo(AiProviderControlledSmokeStatus.PASS);
        assertThat(result.httpStatusClass()).isEqualTo("2XX");
        assertThat(result.errorCategory()).isNull();
        assertThat(result.responseParseStatus()).isEqualTo("PASS");
        assertThat(transport.lastRequest.getUrl()).isEqualTo(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent");
        assertThat(transport.lastRequest.getHeaders())
                .containsEntry("x-goog-api-key", "test-gemini-key");
        assertThat(result.timeoutLimitMs()).isEqualTo(30_000L);
        assertThat(transport.lastRequest.getTimeout().toMillis()).isEqualTo(30_000L);
        assertThat(transport.calls).isEqualTo(1);

        var body = objectMapper.readTree(transport.lastRequest.getBody());
        assertThat(body.path("systemInstruction").path("parts").isArray()).isTrue();
        assertThat(body.path("contents").isArray()).isTrue();
        assertThat(body.path("contents").get(0).path("role").asText()).isEqualTo("user");
        assertThat(body.path("generationConfig").path("maxOutputTokens").asInt()).isEqualTo(128);
        assertThat(body.path("generationConfig").has("temperature")).isTrue();
        assertThat(body.path("generationConfig").path("responseMimeType").asText())
                .isEqualTo("application/json");
        assertThat(body.path("generationConfig").path("responseJsonSchema").path("required"))
                .hasSize(4);

        String application = Files.readString(Path.of("src/main/resources/application.yml"));
        assertThat(application).contains("request-timeout-ms: ${TRADE_MODEL_AI_REQUEST_TIMEOUT_MS:5000}");
    }

    @Test
    void productionRequestTimeoutRemainsFiveSeconds() throws Exception {
        String application = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(application).contains("request-timeout-ms: ${TRADE_MODEL_AI_REQUEST_TIMEOUT_MS:5000}");
        assertThat(application).doesNotContain("request-timeout-ms: ${TRADE_MODEL_AI_REQUEST_TIMEOUT_MS:30000}");
    }

    @Test
    void invalidGeminiFixtureFailsSmokeResponseSchemaWithUsageAndRequestIdPreserved() {
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + "\"Natural language instead of the required JSON contract.\"}]}}],"
                + "\"usageMetadata\":{\"promptTokenCount\":4,\"candidatesTokenCount\":8,"
                + "\"totalTokenCount\":12},\"responseId\":\"test-response-id\"}";
        FakeTransport transport = FakeTransport.responding(new AiHttpResponse(200, body, Map.of()));

        AiProviderControlledSmokeResult result = smoke.run(enabled("GEMINI", true), transport);

        assertThat(result.status()).isEqualTo(AiProviderControlledSmokeStatus.FAIL_RESPONSE_SCHEMA);
        assertThat(result.httpStatusClass()).isEqualTo("2XX");
        assertThat(result.responseParseStatus()).isEqualTo("FAIL");
        assertThat(result.errorCategory()).isEqualTo(AiProviderControlledSmokeErrorCategory.RESPONSE_SCHEMA);
        assertThat(result.tokenUsagePresent()).isTrue();
        assertThat(result.requestIdPresent()).isTrue();
    }

    @Test
    void geminiSchemaDiagnosticExposesOnlyFieldNamesAndTypes() throws Exception {
        String providerText = "{\"stance\":\"ABSTAIN\","
                + "\"reasonCodes\":\"PRIVATE_REASON_VALUE\","
                + "\"summary\":\"PRIVATE_SUMMARY_VALUE\","
                + "\"unexpected\":\"PRIVATE_EXTRA_VALUE\"}";
        String body = objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(Map.of("text", providerText))))),
                "usageMetadata", Map.of(
                        "promptTokenCount", 4,
                        "candidatesTokenCount", 8,
                        "totalTokenCount", 12),
                "responseId", "private-response-id"));

        AiProviderControlledSmokeResult result = smoke.run(
                enabled("GEMINI", true),
                FakeTransport.responding(new AiHttpResponse(200, body, Map.of())));
        String output = String.join("\n", result.sanitizedOutputLines());

        assertThat(result.status()).isEqualTo(AiProviderControlledSmokeStatus.FAIL_RESPONSE_SCHEMA);
        assertThat(output).contains(
                "GEMINI_SCHEMA_DIAGNOSTIC: FIELD_NAMES_AND_TYPES_ONLY",
                "EXPECTED_FIELDS: stance, conflictLevel, reasonCodes, summary",
                "ACTUAL_FIELDS: stance, reasonCodes, summary, unexpected",
                "MISSING_FIELDS: conflictLevel",
                "UNEXPECTED_FIELDS: unexpected",
                "TYPE_MISMATCH_FIELDS: reasonCodes expected ARRAY got STRING");
        assertThat(output).doesNotContain(
                providerText,
                "PRIVATE_REASON_VALUE",
                "PRIVATE_SUMMARY_VALUE",
                "PRIVATE_EXTRA_VALUE",
                "private-response-id",
                "test-gemini-key",
                "x-goog-api-key");
    }

    @Test
    void malformedOrMissingTextFailsResponseSchema() {
        FakeTransport malformed = FakeTransport.responding(new AiHttpResponse(200, "{", Map.of()));
        FakeTransport missingText = FakeTransport.responding(new AiHttpResponse(200,
                "{\"id\":\"response\",\"usage\":{\"input_tokens\":1}}", Map.of()));

        assertThat(smoke.run(enabled("OPENAI", true), malformed).status())
                .isEqualTo(AiProviderControlledSmokeStatus.FAIL_RESPONSE_SCHEMA);
        assertThat(smoke.run(enabled("OPENAI", true), missingText).status())
                .isEqualTo(AiProviderControlledSmokeStatus.FAIL_RESPONSE_SCHEMA);
    }

    @Test
    void missingUsageAndRequestIdRemainExplicitlyAbsent() {
        FakeTransport transport = FakeTransport.responding(validOpenAiResponse(false, false));

        AiProviderControlledSmokeResult result = smoke.run(enabled("OPENAI", true), transport);

        assertThat(result.status()).isEqualTo(AiProviderControlledSmokeStatus.PASS);
        assertThat(result.tokenUsagePresent()).isFalse();
        assertThat(result.requestIdPresent()).isFalse();
        assertThat(result.sanitizedOutputLines()).contains(
                "AI_TOKEN_USAGE_PRESENT: NO",
                "AI_REQUEST_ID_PRESENT: NO");
    }

    @Test
    void sanitizedOutputNeverContainsKeysHeadersBodiesOrRequestIdValues() {
        String fakeKey = "test-openai-key";
        Map<String, String> environment = enabled("OPENAI", true);
        environment.put("OPENAI_API_KEY", fakeKey);
        String privateRequestId = "private-request-id";
        String privateBodyMarker = "private-provider-body-marker";
        FakeTransport transport = FakeTransport.responding(new AiHttpResponse(500,
                privateBodyMarker, Map.of("x-request-id", List.of(privateRequestId))));

        String output = String.join("\n", smoke.run(environment, transport).sanitizedOutputLines());

        assertThat(output)
                .doesNotContain(fakeKey)
                .doesNotContain("Bearer")
                .doesNotContain("Authorization")
                .doesNotContain("x-goog-api-key")
                .doesNotContain(privateBodyMarker)
                .doesNotContain(privateRequestId);
    }

    @Test
    void shellHarnessDefaultsToSkipAndForcesAllSchedulersOff() throws Exception {
        String script = Files.readString(Path.of("scripts/ai-provider-controlled-smoke.sh"));
        for (String scheduler : List.of(
                "TRADE_MODEL_SCHEDULERS_ENABLED",
                "TRADE_MODEL_PUSH_RECHECK_SCHEDULER_ENABLED",
                "TRADE_MODEL_POSITION_SYNC_SCHEDULER_ENABLED",
                "TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED",
                "TRADE_MODEL_MARKET_DATA_SCHEDULER_ENABLED",
                "TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED",
                "TRADE_MODEL_WATCHLIST_SCHEDULER_ENABLED",
                "TRADE_MODEL_ANALYSIS_SCHEDULER_ENABLED",
                "TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED")) {
            assertThat(script).contains("export " + scheduler + "=false");
        }
        assertThat(script).doesNotContain("source trade-model.local-secret");
        assertThat(script).contains(
                "export TRADE_MODEL_AI_REQUEST_TIMEOUT_MS=15000",
                "export TRADE_MODEL_AI_OVERALL_TIMEOUT_MS=15000",
                "timeout_limit_ms=30000");

        ProcessBuilder processBuilder = new ProcessBuilder("bash", "scripts/ai-provider-controlled-smoke.sh");
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().remove("AI_PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS");
        processBuilder.environment().remove("AI_PROVIDER_SMOKE_HARNESS_ENTRY");
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());

        assertThat(process.waitFor()).isZero();
        assertThat(output).contains(
                "AI_PROVIDER_LIVE_SMOKE: SKIPPED_EXTERNAL_CALLS_DISABLED",
                "LIVE_PROVIDER_CALLS: 0",
                "REAL_KEYS_READ: 0",
                "PRODUCTION_READINESS: BLOCKED");
    }

    @Test
    void configurationAndRoleSafetyContractsRemainFailClosed() throws Exception {
        String application = Files.readString(Path.of("src/main/resources/application.yml"));
        assertThat(application).contains(
                "enabled: ${TRADE_MODEL_AI_ENABLED:false}",
                "enabled: ${TRADE_MODEL_AI_OPENAI_ENABLED:false}",
                "enabled: ${TRADE_MODEL_AI_GEMINI_ENABLED:false}",
                "enabled: ${TRADE_MODEL_AI_XAI_ENABLED:false}",
                "daily-budget-usd: ${TRADE_MODEL_AI_DAILY_BUDGET_USD:0}",
                "per-analysis-budget-usd: ${TRADE_MODEL_AI_PER_ANALYSIS_BUDGET_USD:0}",
                "fast-model: ${TRADE_MODEL_AI_OPENAI_GPT_FINAL_FAST_MODEL:gpt-5.6-luna}",
                "reasoning-model: ${TRADE_MODEL_AI_OPENAI_GPT_FINAL_REASONING_MODEL:gpt-5.6-sol}",
                "${TRADE_MODEL_AI_OPENAI_GPT_FINAL_FALLBACK_GPT55_MODEL:gpt-5.5}",
                "${TRADE_MODEL_AI_OPENAI_GPT_FINAL_FALLBACK_GPT54_MODEL:gpt-5.4}",
                "model: ${TRADE_MODEL_AI_GEMINI_MODEL:gemini-3.5-flash}",
                "model: ${TRADE_MODEL_AI_XAI_MODEL:grok-4.5}",
                "priority: ${TRADE_MODEL_AI_GPT_FINAL_PRIORITY:QUALITY_FIRST}",
                "priority: ${TRADE_MODEL_AI_GEMINI_REVIEW_PRIORITY:BALANCED}",
                "priority: ${TRADE_MODEL_AI_GROK_CHALLENGE_PRIORITY:CHALLENGE_FIRST}");

        AiOrchestratorResult safety = new AiOrchestratorResult();
        assertThat(safety.isReviewOnly()).isTrue();
        assertThat(safety.isManualReviewOnly()).isTrue();
        assertThat(safety.isNotTradeInstruction()).isTrue();
        assertThat(safety.isNotExecutable()).isTrue();
        assertThat(safety.isNotAutoTrading()).isTrue();
        assertThat(safety.isNotOrderExecution()).isTrue();
        assertThat(safety.isNotUserPositionCreation()).isTrue();
        assertThat(safety.isNotPositionMutation()).isTrue();
        assertThat(safety.isNotStateMachineOverride()).isTrue();
        assertThat(safety.isRuleDirectionPreserved()).isTrue();

        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/ai/AiProviderControlledSmoke.java"));
        assertThat(source).doesNotContain(
                "DecisionResultService", "ExecutionPlanService", "UserPositionService",
                "PositionMonitorService", "PushService", "Telegram", "OrderService");
        String dashboard = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));
        assertThat(dashboard).contains("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
    }

    private static AiProviderControlledSmokeResult skipped() {
        return new AiProviderControlledSmokeResult("--", "--", "NOT_CHECKED", "NOT_RUN", null, null, "NOT_RUN",
                false, false, 0L, 0L,
                AiProviderControlledSmokeStatus.SKIPPED_EXTERNAL_CALLS_DISABLED, 0, null);
    }

    private void assertGeminiHttpFailure(
            int statusCode, String providerStatus, String privateMessage,
            AiProviderControlledSmokeStatus expectedStatus,
            AiProviderControlledSmokeErrorCategory expectedCategory,
            AiProviderErrorReason expectedReason) {
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "error", Map.of(
                            "code", statusCode,
                            "status", providerStatus,
                            "message", privateMessage)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
        FakeTransport transport = FakeTransport.responding(
                new AiHttpResponse(statusCode, body, Map.of()));

        AiProviderControlledSmokeResult result = smoke.run(enabled("GEMINI", true), transport);
        String output = String.join("\n", result.sanitizedOutputLines());

        assertThat(result.status()).isEqualTo(expectedStatus);
        assertThat(result.httpStatusClass()).isEqualTo(statusCode / 100 + "XX");
        assertThat(result.errorCategory()).isEqualTo(expectedCategory);
        assertThat(result.providerErrorReason()).isEqualTo(expectedReason);
        assertThat(result.liveProviderCalls()).isEqualTo(1);
        assertThat(transport.calls).isEqualTo(1);
        assertThat(output).contains(
                "AI_ERROR_CATEGORY: " + expectedCategory.name(),
                "AI_PROVIDER_ERROR_REASON: " + expectedReason.name());
        assertThat(output).doesNotContain(body, privateMessage, "x-goog-api-key");
    }

    private static Map<String, String> enabled(String target, boolean includeKey) {
        Map<String, String> environment = new HashMap<>();
        environment.put("AI_PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS", "true");
        environment.put("AI_PROVIDER_SMOKE_TARGET", target);
        environment.put("TRADE_MODEL_AI_ENABLED", "true");
        if ("OPENAI".equals(target)) {
            environment.put("TRADE_MODEL_AI_OPENAI_ENABLED", "true");
            if (includeKey) environment.put("OPENAI_API_KEY", "test-openai-key");
        } else if ("GEMINI".equals(target)) {
            environment.put("TRADE_MODEL_AI_GEMINI_ENABLED", "true");
            if (includeKey) environment.put("GEMINI_API_KEY", "test-gemini-key");
        } else if ("XAI".equals(target)) {
            environment.put("TRADE_MODEL_AI_XAI_ENABLED", "true");
            if (includeKey) environment.put("XAI_API_KEY", "test-xai-key");
        }
        return environment;
    }

    private static AiHttpResponse validResponse(String target, boolean usage, boolean requestId) {
        return switch (target) {
            case "GEMINI" -> validGeminiResponse(usage, requestId);
            case "XAI" -> validXaiResponse(usage, requestId);
            default -> validOpenAiResponse(usage, requestId);
        };
    }

    private static AiHttpResponse validOpenAiResponse(boolean usage, boolean requestId) {
        String body = "{\"output_text\":\"" + reviewPayload() + "\""
                + (usage ? ",\"usage\":{\"input_tokens\":4,\"output_tokens\":8,\"total_tokens\":12}" : "")
                + "}";
        Map<String, List<String>> headers = requestId
                ? Map.of("x-request-id", List.of("test-request-id")) : Map.of();
        return new AiHttpResponse(200, body, headers);
    }

    private static AiHttpResponse validGeminiResponse(boolean usage, boolean requestId) {
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + reviewPayload()
                + "\"}]}}]"
                + (usage ? ",\"usageMetadata\":{\"promptTokenCount\":4,\"candidatesTokenCount\":8,"
                + "\"totalTokenCount\":12}" : "")
                + (requestId ? ",\"responseId\":\"test-response-id\"" : "")
                + "}";
        return new AiHttpResponse(200, body, Map.of());
    }

    private static AiHttpResponse validXaiResponse(boolean usage, boolean requestId) {
        String body = "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\","
                + "\"text\":\"" + reviewPayload() + "\"}]}]"
                + (usage ? ",\"usage\":{\"input_tokens\":4,\"output_tokens\":8,\"total_tokens\":12}" : "")
                + "}";
        Map<String, List<String>> headers = requestId
                ? Map.of("x-request-id", List.of("test-request-id")) : Map.of();
        return new AiHttpResponse(200, body, headers);
    }

    private static String reviewPayload() {
        return "{\\\"stance\\\":\\\"ABSTAIN\\\",\\\"conflictLevel\\\":\\\"NONE\\\","
                + "\\\"reasonCodes\\\":[\\\"SCHEMA_OK\\\"],\\\"summary\\\":\\\"schema review only\\\"}";
    }

    private static final class FakeTransport implements AiHttpTransport {
        private final AiHttpResponse response;
        private final IOException exception;
        private int calls;
        private AiHttpRequest lastRequest;

        private FakeTransport(AiHttpResponse response, IOException exception) {
            this.response = response;
            this.exception = exception;
        }

        static FakeTransport responding(AiHttpResponse response) {
            return new FakeTransport(response, null);
        }

        static FakeTransport throwing(IOException exception) {
            return new FakeTransport(null, exception);
        }

        @Override
        public AiHttpResponse post(AiHttpRequest request) throws IOException {
            calls++;
            lastRequest = request;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
