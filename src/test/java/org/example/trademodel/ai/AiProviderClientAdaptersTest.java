package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderClientAdaptersTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void openAiAdapter_mapsResponsesApiWithoutToolsOrRawKeys() throws Exception {
        AiOrchestratorProperties properties = properties();
        configure(properties.getOpenai(), "openai-key", "gpt-test", "https://api.openai.test");
        FakeTransport transport = FakeTransport.responding(new AiHttpResponse(200, """
                {"id":"resp-openai","output_text":"{\\"stance\\":\\"SUPPORT\\",\\"conflictLevel\\":\\"NONE\\",\\"reasonCodes\\":[\\"OK\\"],\\"summary\\":\\"clean\\"}","usage":{"input_tokens":11,"output_tokens":7,"total_tokens":18}}
                """, Map.of()));
        OpenAiProviderClient client = new OpenAiProviderClient(properties, transport, objectMapper);

        AiProviderReviewResult result = client.review(request());

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.SUCCESS);
        assertThat(result.getProviderRequestId()).isEqualTo("resp-openai");
        assertThat(result.getInputTokens()).isEqualTo(11);
        assertThat(result.getOutputTokens()).isEqualTo(7);
        assertThat(result.getTotalTokens()).isEqualTo(18);
        assertThat(transport.lastRequest.getUrl()).isEqualTo("https://api.openai.test/v1/responses");
        assertThat(transport.lastRequest.getHeaders()).containsEntry("Authorization", "Bearer openai-key");
        assertThat(transport.lastRequest.getBody())
                .contains("\"model\":\"gpt-5.6-luna\"")
                .contains("\"instructions\"")
                .contains("\"input\"")
                .contains("\"max_output_tokens\":200")
                .contains("\"reasoning\":{\"effort\":\"high\"}");
        assertThat(transport.lastRequest.getBody()).doesNotContain("\"tools\"");
        assertThat(transport.lastRequest.getBody()).doesNotContain("openai-key");
    }

    @Test
    void openAiAdapter_mapsResponsesOutputContentFallback() {
        AiOrchestratorProperties properties = properties();
        configure(properties.getOpenai(), "openai-key", "gpt-test", "https://api.openai.test");
        OpenAiProviderClient client = new OpenAiProviderClient(properties,
                FakeTransport.responding(new AiHttpResponse(200, """
                        {"output":[{"type":"message","content":[{"type":"output_text","text":"{\\"stance\\":\\"ABSTAIN\\",\\"conflictLevel\\":\\"NONE\\",\\"reasonCodes\\":[\\"OUTPUT_FALLBACK\\"],\\"summary\\":\\"review only\\"}"}]}],"usage":{"input_tokens":3,"output_tokens":2,"total_tokens":5}}
                        """, Map.of("x-request-id", List.of("request-output-fallback")))), objectMapper);

        AiProviderReviewResult result = client.review(request());

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.SUCCESS);
        assertThat(result.getProviderRequestId()).isEqualTo("request-output-fallback");
        assertThat(result.getReasonCodes()).contains("OUTPUT_FALLBACK");
        assertThat(result.getTotalTokens()).isEqualTo(5);
    }

    @Test
    void openAiAdapter_usesPerCallTimeoutOverrideForHttpRequest() throws Exception {
        AiOrchestratorProperties properties = properties();
        configure(properties.getOpenai(), "openai-key", "gpt-test", "https://api.openai.test");
        FakeTransport transport = FakeTransport.responding(new AiHttpResponse(200, """
                {"id":"resp-openai","output_text":"{\\"stance\\":\\"SUPPORT\\",\\"conflictLevel\\":\\"NONE\\",\\"reasonCodes\\":[\\"OK\\"],\\"summary\\":\\"clean\\"}","usage":{"input_tokens":11,"output_tokens":7,"total_tokens":18}}
                """, Map.of()));
        OpenAiProviderClient client = new OpenAiProviderClient(properties, transport, objectMapper);

        client.review(request(), 1234);

        assertThat(transport.lastRequest.getTimeout().toMillis()).isEqualTo(1234);
    }

    @Test
    void geminiAdapter_mapsInteractionsWithHeaderKeyAndNoTools() throws Exception {
        AiOrchestratorProperties properties = properties();
        configure(properties.getGemini(), "gemini-key", "gemini-test", "https://gemini.test");
        FakeTransport transport = FakeTransport.responding(new AiHttpResponse(200, """
                {"id":"gemini-resp","status":"completed","steps":[{"type":"model_output","content":[{"type":"text","text":"{\\"stance\\":\\"ABSTAIN\\",\\"conflictLevel\\":\\"NONE\\",\\"reasonCodes\\":[\\"NO_EDGE\\"],\\"summary\\":\\"no clear issue\\"}"}]}],"usage":{"total_input_tokens":13,"total_output_tokens":5,"total_tokens":18}}
                """, Map.of()));
        GeminiProviderClient client = new GeminiProviderClient(properties, transport, objectMapper);

        AiProviderReviewResult result = client.review(request());

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.SUCCESS);
        assertThat(result.getProviderRequestId()).isEqualTo("gemini-resp");
        assertThat(result.getInputTokens()).isEqualTo(13);
        assertThat(result.getOutputTokens()).isEqualTo(5);
        assertThat(result.getTotalTokens()).isEqualTo(18);
        assertThat(transport.lastRequest.getUrl()).isEqualTo("https://gemini.test/v1/interactions");
        assertThat(transport.lastRequest.getHeaders()).containsEntry("x-goog-api-key", "gemini-key");
        assertThat(transport.lastRequest.getBody()).contains("\"model\":\"models/gemini-test\"");
        assertThat(transport.lastRequest.getBody()).doesNotContain("\"tools\"");
    }

    @Test
    void xaiAdapter_mapsResponsesApiWithNoTools() throws Exception {
        AiOrchestratorProperties properties = properties();
        configure(properties.getXai(), "xai-key", "grok-test", "https://xai.test");
        FakeTransport transport = FakeTransport.responding(new AiHttpResponse(200, """
                {"id":"xai-response","output":[{"type":"message","content":[{"type":"output_text","text":"{\\"stance\\":\\"CHALLENGE\\",\\"conflictLevel\\":\\"MAJOR\\",\\"reasonCodes\\":[\\"MTF_CONFLICT\\"],\\"summary\\":\\"multi timeframe conflict\\"}"}]}],"usage":{"input_tokens":21,"output_tokens":9,"total_tokens":30}}
                """, Map.of("x-request-id", java.util.List.of("xai-request"))));
        XaiProviderClient client = new XaiProviderClient(properties, transport, objectMapper);

        AiProviderReviewResult result = client.review(request());

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.SUCCESS);
        assertThat(result.challengesRule()).isTrue();
        assertThat(result.getProviderRequestId()).isEqualTo("xai-request");
        assertThat(result.getInputTokens()).isEqualTo(21);
        assertThat(result.getOutputTokens()).isEqualTo(9);
        assertThat(result.getTotalTokens()).isEqualTo(30);
        assertThat(client.readiness().getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_ACTIVE);
        assertThat(client.readiness().isReady()).isTrue();
        assertThat(client.readiness().getReasonCodes()).containsExactly("MODEL_CALL_VERIFIED");
        assertThat(client.readiness().getReasonCodes())
                .doesNotContain("MODEL_AVAILABILITY_UNVERIFIED");
        assertThat(transport.lastRequest.getUrl()).isEqualTo("https://xai.test/v1/responses");
        assertThat(transport.lastRequest.getBody()).contains("\"max_output_tokens\":200");
        assertThat(transport.lastRequest.getBody()).contains("\"effort\":\"low\"");
        assertThat(transport.lastRequest.getBody()).doesNotContain("\"temperature\"");
        assertThat(transport.lastRequest.getBody()).doesNotContain("\"tools\"");
        assertThat(transport.lastRequest.getBody()).doesNotContain("xai-key");
    }

    @Test
    void openAiAdapter_prefersOfficialRequestIdHeader() {
        AiOrchestratorProperties properties = properties();
        configure(properties.getOpenai(), "openai-key", "gpt-test", "https://api.openai.test");
        OpenAiProviderClient client = new OpenAiProviderClient(properties,
                FakeTransport.responding(new AiHttpResponse(200, """
                        {"id":"response-id","output_text":"{\\"stance\\":\\"SUPPORT\\",\\"conflictLevel\\":\\"NONE\\",\\"reasonCodes\\":[\\"OK\\"],\\"summary\\":\\"clean\\"}","usage":{"input_tokens":1,"output_tokens":1,"total_tokens":2}}
                        """, Map.of("x-request-id", java.util.List.of("request-id")))), objectMapper);

        assertThat(client.review(request()).getProviderRequestId()).isEqualTo("request-id");
    }

    @Test
    void adapter_mapsProvider429ToRateLimitedFallback() {
        AiOrchestratorProperties properties = properties();
        configure(properties.getOpenai(), "openai-key", "gpt-test", "https://api.openai.test");
        OpenAiProviderClient client = new OpenAiProviderClient(properties,
                FakeTransport.responding(new AiHttpResponse(429, "{}", Map.of())),
                objectMapper);

        AiProviderReviewResult result = client.review(request());

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.RATE_LIMITED);
        assertThat(result.isRateLimited()).isTrue();
        assertThat(result.isFallback()).isTrue();
    }

    @Test
    void adapter_mapsAuthenticationModelAndBillingFailuresWithoutRawBody() {
        AiOrchestratorProperties properties = properties();
        configure(properties.getOpenai(), "openai-key", "gpt-test", "https://api.openai.test");

        assertThat(reviewStatus(properties, 401, "secret provider body").getErrorCode())
                .isEqualTo("PROVIDER_AUTH_FAILURE");
        assertThat(reviewStatus(properties, 403, "billing text must not escape").getErrorCode())
                .isEqualTo("PROVIDER_AUTH_FAILURE");
        assertThat(reviewStatus(properties, 404, "model missing").getErrorCode())
                .isEqualTo("PROVIDER_MODEL_NOT_FOUND");
        AiProviderReviewResult billing = reviewStatus(properties, 402, "insufficient credits: private detail");
        assertThat(billing.getErrorCode()).isEqualTo("PROVIDER_BILLING_OR_CREDITS");
        assertThat(billing.getSummary()).doesNotContain("private detail");
    }

    @Test
    void adapter_distinguishesRateLimitFromBillingOnHttp429WithoutRawBody() {
        AiOrchestratorProperties properties = properties();
        configure(properties.getOpenai(), "openai-key", "gpt-test", "https://api.openai.test");

        AiProviderReviewResult rateLimit = reviewStatus(properties, 429, "too many requests private detail");
        AiProviderReviewResult insufficientQuota = reviewStatus(
                properties, 429, "insufficient_quota private detail");
        AiProviderReviewResult exceededQuota = reviewStatus(
                properties, 429, "You exceeded your current quota private detail");
        AiProviderReviewResult billing = reviewStatus(
                properties, 402, "credit balance billing private detail");

        assertThat(rateLimit.getErrorCode()).isEqualTo("PROVIDER_RATE_LIMITED");
        assertThat(rateLimit.getCallStatus()).isEqualTo(AiProviderCallStatus.RATE_LIMITED);
        assertThat(insufficientQuota.getErrorCode()).isEqualTo("PROVIDER_BILLING_OR_CREDITS");
        assertThat(exceededQuota.getErrorCode()).isEqualTo("PROVIDER_BILLING_OR_CREDITS");
        assertThat(billing.getErrorCode()).isEqualTo("PROVIDER_BILLING_OR_CREDITS");
        assertThat(List.of(rateLimit, insufficientQuota, exceededQuota, billing))
                .allSatisfy(result -> assertThat(result.getSummary()).doesNotContain("private detail"));
    }

    @Test
    void adapter_mapsTimeoutToTimeoutFallback() {
        AiOrchestratorProperties properties = properties();
        configure(properties.getOpenai(), "openai-key", "gpt-test", "https://api.openai.test");
        OpenAiProviderClient client = new OpenAiProviderClient(properties,
                FakeTransport.throwing(new HttpTimeoutException("slow")),
                objectMapper);

        AiProviderReviewResult result = client.review(request());

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.TIMEOUT);
        assertThat(result.isTimeout()).isTrue();
        assertThat(result.isFallback()).isTrue();
    }

    @Test
    void adapter_mapsMalformedProviderJsonToFailureFallback() {
        AiOrchestratorProperties properties = properties();
        configure(properties.getOpenai(), "openai-key", "gpt-test", "https://api.openai.test");
        OpenAiProviderClient client = new OpenAiProviderClient(properties,
                FakeTransport.responding(new AiHttpResponse(200, "{", Map.of())),
                objectMapper);

        AiProviderReviewResult result = client.review(request());

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.isFallback()).isTrue();
    }

    private static AiOrchestratorProperties properties() {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setMaxInputChars(4000);
        properties.setMaxOutputTokens(200);
        properties.getOpenai().getGptFinal().setFastModel("gpt-5.6-luna");
        properties.getOpenai().getGptFinal().setReasoningModel("gpt-5.6-sol");
        properties.getOpenai().getGptFinal().setFallbackModels(List.of("gpt-5.5", "gpt-5.4"));
        properties.getOpenai().getGptFinal().setFallbackEnabled(false);
        return properties;
    }

    private AiProviderReviewResult reviewStatus(AiOrchestratorProperties properties, int status, String body) {
        OpenAiProviderClient client = new OpenAiProviderClient(properties,
                FakeTransport.responding(new AiHttpResponse(status, body, Map.of())), objectMapper);
        return client.review(request());
    }

    private static void configure(AiProviderProperties properties, String key, String model, String baseUrl) {
        properties.setEnabled(true);
        properties.setApiKey(key);
        properties.setModel(model);
        properties.setBaseUrl(baseUrl);
        properties.setInputCostPerMillionUsd(java.math.BigDecimal.ONE);
        properties.setOutputCostPerMillionUsd(java.math.BigDecimal.ONE);
    }

    private static AiProviderRequest request() {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("analysis-1");
        request.setTraceId("trace-1");
        request.setSymbol("BTCUSDT");
        request.setTimeframe("1m");
        request.setRuleMarketBias("BULLISH");
        request.setRuleConfidence("HIGH");
        request.setRuleRiskLevel("LOW");
        request.setRuleWorthOpening(Boolean.TRUE);
        request.setMultiTimeframeState("ALIGNED");
        request.setExternalContextState("ABSENT");
        request.setEvidenceSummary("review-only evidence");
        request.setScoreSummary("score=90");
        return request;
    }

    private static final class FakeTransport implements AiHttpTransport {
        private final AiHttpResponse response;
        private final IOException exception;
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
            this.lastRequest = request;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
