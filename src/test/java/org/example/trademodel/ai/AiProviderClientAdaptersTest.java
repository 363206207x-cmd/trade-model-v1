package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
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
        assertThat(result.getTotalTokens()).isEqualTo(18);
        assertThat(transport.lastRequest.getUrl()).isEqualTo("https://api.openai.test/v1/responses");
        assertThat(transport.lastRequest.getHeaders()).containsEntry("Authorization", "Bearer openai-key");
        assertThat(transport.lastRequest.getBody()).doesNotContain("\"tools\"");
        assertThat(transport.lastRequest.getBody()).doesNotContain("openai-key");
    }

    @Test
    void geminiAdapter_mapsGenerateContentWithHeaderKeyAndNoTools() throws Exception {
        AiOrchestratorProperties properties = properties();
        configure(properties.getGemini(), "gemini-key", "gemini-test", "https://gemini.test");
        FakeTransport transport = FakeTransport.responding(new AiHttpResponse(200, """
                {"responseId":"gemini-resp","candidates":[{"content":{"parts":[{"text":"{\\"stance\\":\\"ABSTAIN\\",\\"conflictLevel\\":\\"NONE\\",\\"reasonCodes\\":[\\"NO_EDGE\\"],\\"summary\\":\\"no clear issue\\"}"}]}}],"usageMetadata":{"promptTokenCount":13,"candidatesTokenCount":5,"totalTokenCount":18}}
                """, Map.of()));
        GeminiProviderClient client = new GeminiProviderClient(properties, transport, objectMapper);

        AiProviderReviewResult result = client.review(request());

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.SUCCESS);
        assertThat(result.getProviderRequestId()).isEqualTo("gemini-resp");
        assertThat(result.getInputTokens()).isEqualTo(13);
        assertThat(transport.lastRequest.getUrl()).isEqualTo("https://gemini.test/v1beta/models/gemini-test:generateContent");
        assertThat(transport.lastRequest.getHeaders()).containsEntry("x-goog-api-key", "gemini-key");
        assertThat(transport.lastRequest.getBody()).doesNotContain("\"tools\"");
    }

    @Test
    void xaiAdapter_mapsChatCompletionWithNoTools() throws Exception {
        AiOrchestratorProperties properties = properties();
        configure(properties.getXai(), "xai-key", "grok-test", "https://xai.test");
        FakeTransport transport = FakeTransport.responding(new AiHttpResponse(200, """
                {"id":"xai-resp","choices":[{"message":{"content":"{\\"stance\\":\\"CHALLENGE\\",\\"conflictLevel\\":\\"MAJOR\\",\\"reasonCodes\\":[\\"MTF_CONFLICT\\"],\\"summary\\":\\"multi timeframe conflict\\"}"}}],"usage":{"prompt_tokens":21,"completion_tokens":9,"total_tokens":30}}
                """, Map.of()));
        XaiProviderClient client = new XaiProviderClient(properties, transport, objectMapper);

        AiProviderReviewResult result = client.review(request());

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.SUCCESS);
        assertThat(result.challengesRule()).isTrue();
        assertThat(result.getProviderRequestId()).isEqualTo("xai-resp");
        assertThat(transport.lastRequest.getUrl()).isEqualTo("https://xai.test/v1/chat/completions");
        assertThat(transport.lastRequest.getBody()).doesNotContain("\"tools\"");
        assertThat(transport.lastRequest.getBody()).doesNotContain("xai-key");
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

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.FAILED);
        assertThat(result.isFallback()).isTrue();
    }

    private static AiOrchestratorProperties properties() {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setMaxInputChars(4000);
        properties.setMaxOutputTokens(200);
        return properties;
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
