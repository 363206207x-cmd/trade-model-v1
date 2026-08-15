package org.example.trademodel.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractSafeAiProviderClient implements AiProviderClient {
    private final AiOrchestratorProperties properties;
    private final AiHttpTransport transport;
    private final ObjectMapper objectMapper;
    private final AiPromptBuilder promptBuilder;
    private final AiProviderResponseParser responseParser;
    private final AiDecisionChainPromptBuilder decisionChainPromptBuilder;
    private final AiDecisionChainResponseParser decisionChainResponseParser;
    private volatile boolean contractVerified;

    protected AbstractSafeAiProviderClient(AiOrchestratorProperties properties,
                                           AiHttpTransport transport,
                                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.transport = transport;
        this.objectMapper = objectMapper;
        this.promptBuilder = new AiPromptBuilder(objectMapper, properties);
        this.responseParser = new AiProviderResponseParser(objectMapper);
        this.decisionChainPromptBuilder = new AiDecisionChainPromptBuilder(objectMapper, properties);
        this.decisionChainResponseParser = new AiDecisionChainResponseParser(objectMapper);
    }

    @Override
    public AiProviderReadiness readiness() {
        AiProviderProperties providerProperties = providerProperties();
        boolean enabled = providerProperties.isEnabled();
        boolean validModelSelection = validModelSelection(providerProperties);
        boolean configured = providerProperties.hasKeyAndModel() && validModelSelection
                && !blank(providerProperties.getBaseUrl());
        String configuredModel = providerProperties.getConfiguredModel();
        String effectiveModel = effectiveModelForReadiness(providerProperties);
        AiModelReadinessStatus modelStatus;
        if (!validModelSelection) {
            modelStatus = AiModelReadinessStatus.MODEL_UNAVAILABLE;
        } else if (enabled && configured && contractVerified) {
            modelStatus = AiModelReadinessStatus.MODEL_ACTIVE;
        } else {
            modelStatus = AiModelReadinessStatus.MODEL_CONFIGURED;
        }
        if (!enabled) {
            return readiness(false, configured, configuredModel, effectiveModel, false,
                    null, modelStatus, List.of("PROVIDER_DISABLED"));
        }
        if (!configured) {
            String reason = validModelSelection
                    ? "PROVIDER_NOT_CONFIGURED" : "MODEL_NOT_CONFIGURED";
            return readiness(true, false, configuredModel, effectiveModel, false,
                    null, modelStatus, List.of(reason));
        }
        List<String> reasons = modelStatus == AiModelReadinessStatus.MODEL_ACTIVE
                ? List.of("MODEL_CALL_VERIFIED")
                : List.of("MODEL_AVAILABILITY_UNVERIFIED");
        return readiness(true, true, configuredModel, effectiveModel, false,
                null, modelStatus, reasons);
    }

    private AiProviderReadiness readiness(boolean enabled, boolean configured,
                                          String configuredModel, String effectiveModel,
                                          boolean fallbackUsed, String fallbackReason,
                                          AiModelReadinessStatus modelStatus,
                                          List<String> reasonCodes) {
        boolean ready = enabled && configured && modelStatus == AiModelReadinessStatus.MODEL_ACTIVE;
        return new AiProviderReadiness(provider(), role(), enabled, configured, ready,
                configuredModel, effectiveModel, fallbackUsed, fallbackReason,
                modelStatus, reasonCodes);
    }

    @Override
    public AiProviderReviewResult review(AiProviderRequest request) {
        return review(request, properties.getRequestTimeoutMs());
    }

    @Override
    public AiProviderReviewResult review(AiProviderRequest request, long timeoutOverrideMs) {
        return reviewWithModel(request, timeoutOverrideMs, providerProperties().getEffectiveModel());
    }

    @Override
    public AiProviderReviewResult verifyExactModel(String model, long timeoutOverrideMs) {
        if (blank(model)) {
            return failure(AiProviderCallStatus.FAILED, "EXACT_MODEL_MISSING", 0L);
        }
        AiProviderRequest request = provider() == AiProviderName.GEMINI
                ? AiProviderControlledSmoke.fixedGeminiReviewRequest()
                : AiProviderControlledSmoke.fixedSchemaOnlyRequest();
        return reviewWithModel(request, timeoutOverrideMs, model);
    }

    protected AiProviderReviewResult reviewWithModel(AiProviderRequest request, long timeoutOverrideMs,
                                                     String selectedModel) {
        long started = System.nanoTime();
        AiPromptBuilder.PromptPayload prompt = promptBuilder.build(request, role());
        try {
            AiHttpRequest httpRequest = buildHttpRequest(prompt.dataJson(), timeoutOverrideMs, selectedModel);
            AiHttpResponse response = transport.post(httpRequest);
            long latencyMs = elapsedMs(started);
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                return httpFailure(response, latencyMs);
            }

            ProviderPayload providerPayload = extractPayload(response);
            AiProviderReviewResult result = responseParser.parse(provider(), role(), providerPayload.content());
            enrichParsedResult(result, providerPayload);
            result.setProviderRequestId(providerPayload.providerRequestId());
            result.setLatencyMs(latencyMs);
            result.setInputTokens(providerPayload.inputTokens());
            result.setOutputTokens(providerPayload.outputTokens());
            result.setTotalTokens(totalTokens(providerPayload.inputTokens(), providerPayload.outputTokens(),
                    providerPayload.totalTokens()));
            result.setCalculatedCostUsd(calculateCost(providerPayload.inputTokens(), providerPayload.outputTokens()));
            if (prompt.truncated() && result.successful()) {
                result.setReasonCodes(appendReason(result.getReasonCodes(), "PROMPT_TRUNCATED"));
            }
            if (result.successful()) {
                contractVerified = true;
            }
            return result;
        } catch (GeminiInteractionContractException e) {
            AiProviderReviewResult result = failure(
                    AiProviderCallStatus.INVALID_RESPONSE, e.reason().name(), elapsedMs(started));
            result.setGeminiInteractionDiagnostic(e.diagnostic());
            return result;
        } catch (JsonProcessingException e) {
            return failure(AiProviderCallStatus.INVALID_RESPONSE, "PROVIDER_RESPONSE_SCHEMA", elapsedMs(started));
        } catch (HttpTimeoutException e) {
            return timeout(started);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return timeout(started);
        } catch (IOException e) {
            return failure(AiProviderCallStatus.FAILED, "PROVIDER_IO_FAILURE", elapsedMs(started));
        } catch (Exception e) {
            return failure(AiProviderCallStatus.FAILED, "PROVIDER_FAILURE", elapsedMs(started));
        }
    }

    @Override
    public AiDecisionChainResult executeDecisionChain(AiDecisionChainRequest request, long timeoutOverrideMs) {
        long started = System.nanoTime();
        if (request == null || request.getRole() == null) {
            return chainFailure(null, AiProviderCallStatus.FAILED,
                    "DECISION_CHAIN_REQUEST_INVALID", started);
        }
        AiDecisionChainPromptBuilder.PromptPayload prompt = decisionChainPromptBuilder.build(request);
        if (prompt.truncated()) {
            return chainFailure(request.getRole(), AiProviderCallStatus.FAILED,
                    "PROMPT_INPUT_TOO_LARGE", started);
        }
        try {
            String model = providerProperties().getEffectiveModel();
            AiHttpRequest httpRequest = buildDecisionChainHttpRequest(
                    prompt.dataJson(), request.getRole(), timeoutOverrideMs, model);
            AiHttpResponse response = transport.post(httpRequest);
            long latencyMs = elapsedMs(started);
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                AiProviderReviewResult failure = httpFailure(response, latencyMs);
                return chainFailure(request.getRole(), failure);
            }
            ProviderPayload payload = extractPayload(response);
            String raw = payload.rawContent() == null ? payload.content() : payload.rawContent();
            AiDecisionChainResult result = decisionChainResponseParser.parse(provider(), request.getRole(), raw);
            result.setProviderRequestId(payload.providerRequestId());
            result.setLatencyMs(latencyMs);
            result.setInputTokens(payload.inputTokens());
            result.setOutputTokens(payload.outputTokens());
            result.setTotalTokens(totalTokens(payload.inputTokens(), payload.outputTokens(), payload.totalTokens()));
            result.setCalculatedCostUsd(calculateCost(payload.inputTokens(), payload.outputTokens()));
            result.setSelectedModel(model);
            return result;
        } catch (HttpTimeoutException exception) {
            return chainFailure(request.getRole(), AiProviderCallStatus.TIMEOUT,
                    "PROVIDER_TIMEOUT", started);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return chainFailure(request.getRole(), AiProviderCallStatus.TIMEOUT,
                    "PROVIDER_INTERRUPTED", started);
        } catch (Exception exception) {
            return chainFailure(request.getRole(), AiProviderCallStatus.FAILED,
                    "PROVIDER_FAILURE", started);
        }
    }

    private AiDecisionChainResult chainFailure(AiDecisionChainRole role,
                                               AiProviderCallStatus status,
                                               String reason,
                                               long startedNanos) {
        AiDecisionChainResult result = AiDecisionChainResult.failed(provider(), role, status, reason);
        result.setLatencyMs(elapsedMs(startedNanos));
        result.setSelectedModel(providerProperties() == null
                ? null : providerProperties().getEffectiveModel());
        return result;
    }

    protected AiHttpRequest buildDecisionChainHttpRequest(String promptJson,
                                                          AiDecisionChainRole role,
                                                          long timeoutOverrideMs,
                                                          String selectedModel) throws Exception {
        return buildHttpRequest(promptJson, timeoutOverrideMs, selectedModel);
    }

    protected abstract AiHttpRequest buildHttpRequest(String promptJson, long timeoutOverrideMs,
                                                      String selectedModel) throws Exception;

    protected abstract ProviderPayload extractPayload(AiHttpResponse response) throws Exception;

    protected boolean validModelSelection(AiProviderProperties providerProperties) {
        return providerProperties != null && providerProperties.hasValidModelSelection();
    }

    protected String effectiveModelForReadiness(AiProviderProperties providerProperties) {
        return providerProperties == null ? null : providerProperties.getEffectiveModel();
    }

    protected void enrichParsedResult(AiProviderReviewResult result, ProviderPayload providerPayload) {
        // Provider-specific diagnostics may add sanitized metadata without retaining response values.
    }

    protected AiProviderReviewResult httpFailure(AiHttpResponse response, long latencyMs) {
        if (response.getStatusCode() == 401 || response.getStatusCode() == 403) {
            return failure(AiProviderCallStatus.FAILED, "PROVIDER_AUTH_FAILURE", latencyMs);
        }
        if (response.getStatusCode() == 404) {
            return failure(AiProviderCallStatus.FAILED, "PROVIDER_MODEL_NOT_FOUND", latencyMs);
        }
        if (isBillingOrCreditsFailure(response.getBody())) {
            return failure(AiProviderCallStatus.FAILED, "PROVIDER_BILLING_OR_CREDITS", latencyMs);
        }
        if (response.getStatusCode() == 429) {
            return failure(AiProviderCallStatus.RATE_LIMITED, "PROVIDER_RATE_LIMITED", latencyMs);
        }
        return failure(AiProviderCallStatus.FAILED,
                "PROVIDER_HTTP_" + response.getStatusCode(), latencyMs);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode readTree(String body) throws Exception {
        return objectMapper.readTree(body == null ? "" : body);
    }

    protected AiHttpRequest baseRequest(String url, String body, long timeoutOverrideMs) {
        AiHttpRequest request = new AiHttpRequest();
        request.setUrl(url);
        request.setBody(body);
        request.setTimeout(Duration.ofMillis(Math.max(1, timeoutOverrideMs)));
        return request;
    }

    protected Map<String, String> jsonHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    protected int maxOutputTokens() {
        return Math.max(1, properties.getMaxOutputTokens());
    }

    protected static String joinUrl(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    protected static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText(null);
    }

    protected static Long longValue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    protected static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private AiProviderReviewResult timeout(long started) {
        return failure(AiProviderCallStatus.TIMEOUT, "PROVIDER_TIMEOUT", elapsedMs(started));
    }

    protected final AiProviderReviewResult failure(AiProviderCallStatus status, String code, long latencyMs) {
        AiProviderReviewResult result = AiProviderReviewResult.skipped(provider(), role(), status, code);
        result.setLatencyMs(latencyMs);
        result.setTimeout(status == AiProviderCallStatus.TIMEOUT);
        result.setRateLimited(status == AiProviderCallStatus.RATE_LIMITED);
        result.setErrorCode(code);
        return result;
    }

    protected BigDecimal calculateCost(Long inputTokens, Long outputTokens) {
        BigDecimal input = costPart(inputTokens, providerProperties().getInputCostPerMillionUsd());
        BigDecimal output = costPart(outputTokens, providerProperties().getOutputCostPerMillionUsd());
        return input.add(output).setScale(8, RoundingMode.HALF_UP);
    }

    private static BigDecimal costPart(Long tokens, BigDecimal ratePerMillion) {
        if (tokens == null || tokens <= 0 || ratePerMillion == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(tokens)
                .multiply(ratePerMillion)
                .divide(BigDecimal.valueOf(1_000_000L), 12, RoundingMode.HALF_UP);
    }

    protected static Long totalTokens(Long input, Long output, Long total) {
        if (total != null) {
            return total;
        }
        if (input == null && output == null) {
            return null;
        }
        return (input == null ? 0L : input) + (output == null ? 0L : output);
    }

    protected static long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000L);
    }

    protected static List<String> appendReason(List<String> reasonCodes, String code) {
        java.util.ArrayList<String> copy = new java.util.ArrayList<>(reasonCodes == null ? List.of() : reasonCodes);
        copy.add(code);
        return copy;
    }

    private static boolean isBillingOrCreditsFailure(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String normalized = body.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("insufficient_quota")
                || normalized.contains("exceeded your current quota")
                || normalized.contains("quota exhausted")
                || normalized.contains("run out of credits")
                || normalized.contains("insufficient credit")
                || normalized.contains("credit balance")
                || normalized.contains("billing")
                || normalized.contains("payment_required");
    }

    private AiDecisionChainResult chainFailure(AiDecisionChainRole role, AiProviderReviewResult failure) {
        AiDecisionChainResult result = AiDecisionChainResult.failed(provider(), role,
                failure == null ? AiProviderCallStatus.FAILED : failure.getCallStatus(),
                failure == null ? "PROVIDER_FAILURE" : failure.getErrorCode());
        if (failure != null) {
            result.setLatencyMs(failure.getLatencyMs());
            result.setSelectedModel(failure.getSelectedModel());
        }
        return result;
    }

    protected record ProviderPayload(String content, String providerRequestId,
                                     Long inputTokens, Long outputTokens, Long totalTokens,
                                     GeminiResponseShapeDiagnostic geminiResponseShapeDiagnostic,
                                     GeminiInteractionDiagnostic geminiInteractionDiagnostic,
                                     String rawContent) {
        protected ProviderPayload(String content, String providerRequestId,
                                  Long inputTokens, Long outputTokens, Long totalTokens,
                                  GeminiResponseShapeDiagnostic geminiResponseShapeDiagnostic,
                                  GeminiInteractionDiagnostic geminiInteractionDiagnostic) {
            this(content, providerRequestId, inputTokens, outputTokens, totalTokens,
                    geminiResponseShapeDiagnostic, geminiInteractionDiagnostic, content);
        }
    }
}
