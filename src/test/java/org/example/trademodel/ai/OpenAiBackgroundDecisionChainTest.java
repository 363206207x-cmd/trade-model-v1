package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiBackgroundDecisionChainTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitsNativeBackgroundResponseWithExactBoundedContract() throws Exception {
        QueueTransport transport = new QueueTransport().post(response(200,
                objectMapper.createObjectNode()
                        .put("id", "resp-background-1")
                        .put("status", "queued")
                        .put("model", "gpt-5.6-sol")));
        OpenAiProviderClient client = new OpenAiProviderClient(properties(), transport, objectMapper);

        AiDecisionChainResult result = client.submitDecisionChainBackground(request(), 30_000);

        assertThat(result.getTaskState()).isEqualTo(AiBackgroundTaskState.QUEUED);
        assertThat(result.getProviderRequestId()).isEqualTo("resp-background-1");
        assertThat(result.getBackgroundMode()).isEqualTo("PROVIDER_NATIVE");
        assertThat(transport.posts).hasSize(1);
        AiHttpRequest submitted = transport.posts.get(0);
        assertThat(submitted.getUrl()).isEqualTo("https://api.openai.test/v1/responses");
        assertThat(submitted.getTimeout().toMillis()).isEqualTo(30_000);
        assertThat(submitted.getHeaders()).containsKeys("Authorization", "Idempotency-Key");
        assertThat(submitted.getHeaders().get("Idempotency-Key")).hasSize(64);
        JsonNode body = objectMapper.readTree(submitted.getBody());
        assertThat(body.path("model").asText()).isEqualTo("gpt-5.6-sol");
        assertThat(body.path("background").asBoolean()).isTrue();
        assertThat(body.path("store").asBoolean()).isTrue();
        assertThat(body.path("max_output_tokens").asInt()).isEqualTo(4000);
        assertThat(body.path("reasoning").path("effort").asText()).isEqualTo("medium");
        assertThat(body.path("text").path("verbosity").asText()).isEqualTo("low");
        assertThat(body.path("text").path("format").path("strict").asBoolean()).isTrue();
        assertThat(body.path("text").path("format").path("schema")
                .path("additionalProperties").asBoolean()).isFalse();
        assertThat(submitted.getBody()).doesNotContain("\"tools\"", "test-openai-key");
    }

    @Test
    void applicationWorkerFallbackKeepsExactSolModelAndStructuredContract() throws Exception {
        AiOrchestratorProperties properties = properties();
        properties.getOpenai().setModel("gpt-5.6-luna");
        QueueTransport transport = new QueueTransport().post(completed("resp-worker-1"));
        OpenAiProviderClient client = new OpenAiProviderClient(properties, transport, objectMapper);

        AiDecisionChainResult result = client.executeDecisionChain(request(), 180_000);

        assertThat(result.successful()).isTrue();
        assertThat(result.getTaskState()).isEqualTo(AiBackgroundTaskState.SUCCEEDED);
        assertThat(result.getBackgroundMode()).isEqualTo("APPLICATION_PERSISTED_WORKER");
        assertThat(result.getSelectedModel()).isEqualTo("gpt-5.6-sol");
        JsonNode body = objectMapper.readTree(transport.posts.get(0).getBody());
        assertThat(body.path("model").asText()).isEqualTo("gpt-5.6-sol");
        assertThat(body.has("background")).isFalse();
        assertThat(body.path("reasoning").path("effort").asText()).isEqualTo("medium");
        assertThat(body.path("text").path("verbosity").asText()).isEqualTo("low");
        assertThat(body.path("text").path("format").path("strict").asBoolean()).isTrue();
    }

    @Test
    void pollsSameProviderResponseThroughQueuedRunningAndCompletedWithUsage() throws Exception {
        QueueTransport transport = new QueueTransport()
                .get(response(200, objectMapper.createObjectNode()
                        .put("id", "resp-background-2").put("status", "queued")))
                .get(response(200, objectMapper.createObjectNode()
                        .put("id", "resp-background-2").put("status", "in_progress")))
                .get(completed("resp-background-2"));
        OpenAiProviderClient client = new OpenAiProviderClient(properties(), transport, objectMapper);

        AiDecisionChainResult queued = client.pollDecisionChainBackground(
                request(), "resp-background-2", 30_000);
        AiDecisionChainResult running = client.pollDecisionChainBackground(
                request(), "resp-background-2", 30_000);
        AiDecisionChainResult completed = client.pollDecisionChainBackground(
                request(), "resp-background-2", 30_000);

        assertThat(queued.getTaskState()).isEqualTo(AiBackgroundTaskState.QUEUED);
        assertThat(running.getTaskState()).isEqualTo(AiBackgroundTaskState.RUNNING);
        assertThat(completed.successful()).isTrue();
        assertThat(completed.getTaskState()).isEqualTo(AiBackgroundTaskState.SUCCEEDED);
        assertThat(completed.getInputTokens()).isEqualTo(120);
        assertThat(completed.getOutputTokens()).isEqualTo(80);
        assertThat(completed.getReasoningTokens()).isEqualTo(31);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(transport.gets).allSatisfy(call ->
                assertThat(call.getUrl()).endsWith("/v1/responses/resp-background-2"));
    }

    @Test
    void classifiesTruncationBackgroundRejectionAndRetryableFailuresWithoutRawBodies() {
        OpenAiProviderClient truncatedClient = clientWithGet(response(200,
                objectMapper.createObjectNode()
                        .put("id", "resp-truncated")
                        .put("status", "incomplete")
                        .set("incomplete_details", objectMapper.createObjectNode()
                                .put("reason", "max_output_tokens"))));
        AiDecisionChainResult truncated = truncatedClient.pollDecisionChainBackground(
                request(), "resp-truncated", 30_000);
        assertThat(truncated.getErrorCode()).isEqualTo("OUTPUT_TRUNCATED");
        assertThat(truncated.getTaskState()).isEqualTo(AiBackgroundTaskState.FAILED);
        assertThat(truncated.isRetryable()).isFalse();

        AiDecisionChainResult unsupported = clientWithPost(new AiHttpResponse(400,
                "background mode is not supported for this model", Map.of()))
                .submitDecisionChainBackground(request(), 30_000);
        assertThat(unsupported.getErrorCode()).isEqualTo("BACKGROUND_NOT_SUPPORTED");
        assertThat(unsupported.isRetryable()).isFalse();

        for (int status : List.of(408, 429, 500, 503)) {
            AiDecisionChainResult transientFailure = clientWithPost(
                    new AiHttpResponse(status, "private provider detail", Map.of()))
                    .submitDecisionChainBackground(request(), 30_000);
            assertThat(transientFailure.isRetryable()).as("HTTP %s", status).isTrue();
            assertThat(transientFailure.getAuditOutput()).isNullOrEmpty();
        }
        for (int status : List.of(400, 401, 403, 404)) {
            AiDecisionChainResult terminalFailure = clientWithPost(
                    new AiHttpResponse(status, "private provider detail", Map.of()))
                    .submitDecisionChainBackground(request(), 30_000);
            assertThat(terminalFailure.isRetryable()).as("HTTP %s", status).isFalse();
        }
        AiDecisionChainResult emptyBadRequest = clientWithPost(
                new AiHttpResponse(400, null, Map.of()))
                .submitDecisionChainBackground(request(), 30_000);
        assertThat(emptyBadRequest.getErrorCode()).isEqualTo("PROVIDER_HTTP_400");
    }

    @Test
    void malformedCompletedOutputFailsClosedWithDurableClassification() {
        var malformedResponse = objectMapper.createObjectNode();
        malformedResponse.put("id", "resp-malformed");
        malformedResponse.put("status", "completed");
        malformedResponse.put("model", "gpt-5.6-sol");
        malformedResponse.put("output_text", "{not-valid-json");
        AiDecisionChainResult malformed = clientWithGet(response(200, malformedResponse))
                .pollDecisionChainBackground(request(), "resp-malformed", 30_000);

        assertThat(malformed.successful()).isFalse();
        assertThat(malformed.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(malformed.getTaskState()).isEqualTo(AiBackgroundTaskState.FAILED);
        assertThat(malformed.getErrorCode()).isEqualTo("INVALID_RESPONSE_PARSE");
        assertThat(malformed.getFailureClassification()).isEqualTo("INVALID_RESPONSE_PARSE");
        assertThat(malformed.getProviderRequestId()).isEqualTo("resp-malformed");
    }

    @Test
    void submitTimeoutIsRetryableAndCancellationUsesSameProviderResponse() {
        QueueTransport timeout = new QueueTransport();
        timeout.postException = new HttpTimeoutException("private timeout detail");
        AiDecisionChainResult result = new OpenAiProviderClient(properties(), timeout, objectMapper)
                .submitDecisionChainBackground(request(), 30_000);

        assertThat(result.getErrorCode()).isEqualTo("OPENAI_ACK_TIMEOUT");
        assertThat(result.isRetryable()).isTrue();
        assertThat(result.getAuditOutput()).isNullOrEmpty();

        QueueTransport cancel = new QueueTransport().post(new AiHttpResponse(200, "{}", Map.of()));
        assertThat(new OpenAiProviderClient(properties(), cancel, objectMapper)
                .cancelDecisionChainBackground("resp-cancel", 30_000)).isTrue();
        assertThat(cancel.posts.get(0).getUrl())
                .isEqualTo("https://api.openai.test/v1/responses/resp-cancel/cancel");
    }

    private OpenAiProviderClient clientWithPost(AiHttpResponse response) {
        return new OpenAiProviderClient(properties(), new QueueTransport().post(response), objectMapper);
    }

    private OpenAiProviderClient clientWithGet(AiHttpResponse response) {
        return new OpenAiProviderClient(properties(), new QueueTransport().get(response), objectMapper);
    }

    private AiHttpResponse completed(String id) {
        var root = objectMapper.createObjectNode();
        root.put("id", id);
        root.put("status", "completed");
        root.put("model", "gpt-5.6-sol");
        root.put("output_text", AiDecisionChainContractTest.gptPayload());
        var usage = root.putObject("usage");
        usage.put("input_tokens", 120);
        usage.put("output_tokens", 80);
        usage.put("total_tokens", 200);
        usage.putObject("output_tokens_details").put("reasoning_tokens", 31);
        return response(200, root);
    }

    private AiHttpResponse response(int status, JsonNode body) {
        return new AiHttpResponse(status, body.toString(), Map.of());
    }

    private static AiOrchestratorProperties properties() {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setMaxInputChars(100_000);
        properties.getOpenai().setEnabled(true);
        properties.getOpenai().setApiKey("test-openai-key");
        properties.getOpenai().setBaseUrl("https://api.openai.test");
        properties.getOpenai().setModel("gpt-5.6-sol");
        properties.getOpenai().getGptFinal().setReasoningModel("gpt-5.6-sol");
        properties.getOpenai().setInputCostPerMillionUsd(BigDecimal.ONE);
        properties.getOpenai().setOutputCostPerMillionUsd(BigDecimal.ONE);
        return properties;
    }

    private static AiDecisionChainRequest request() {
        AiDecisionChainRequest request = new AiDecisionChainRequest();
        request.setRole(AiDecisionChainRole.GPT_FINAL);
        request.setAnalysisId("analysis-background-1");
        request.setTraceId("trace-background-1");
        request.setCandidateId("candidate-background-1");
        request.setSymbol("ADAUSDT");
        request.setTimeframe("5m");
        request.setRuleVersion("V41");
        request.setInput(Map.of(
                "analysis", Map.of("analysisId", "analysis-background-1", "symbol", "ADAUSDT",
                        "timeframe", "5m"),
                "decisionBundle", Map.of("ruleDirection", "BULLISH", "dataQuality", 91),
                "evidence", List.of(),
                "scores", List.of()));
        return request;
    }

    private static final class QueueTransport implements AiHttpTransport {
        private final Deque<AiHttpResponse> postResponses = new ArrayDeque<>();
        private final Deque<AiHttpResponse> getResponses = new ArrayDeque<>();
        private final List<AiHttpRequest> posts = new ArrayList<>();
        private final List<AiHttpRequest> gets = new ArrayList<>();
        private IOException postException;

        private QueueTransport post(AiHttpResponse response) {
            postResponses.add(response);
            return this;
        }

        private QueueTransport get(AiHttpResponse response) {
            getResponses.add(response);
            return this;
        }

        @Override
        public AiHttpResponse post(AiHttpRequest request) throws IOException {
            posts.add(request);
            if (postException != null) throw postException;
            return postResponses.removeFirst();
        }

        @Override
        public AiHttpResponse get(AiHttpRequest request) {
            gets.add(request);
            return getResponses.removeFirst();
        }
    }
}
