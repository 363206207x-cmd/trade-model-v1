package org.example.trademodel.ai;

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

    protected AbstractSafeAiProviderClient(AiOrchestratorProperties properties,
                                           AiHttpTransport transport,
                                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.transport = transport;
        this.objectMapper = objectMapper;
        this.promptBuilder = new AiPromptBuilder(objectMapper, properties);
        this.responseParser = new AiProviderResponseParser(objectMapper);
    }

    @Override
    public AiProviderReadiness readiness() {
        AiProviderProperties providerProperties = providerProperties();
        boolean enabled = providerProperties.isEnabled();
        boolean configured = providerProperties.hasKeyAndModel() && !blank(providerProperties.getBaseUrl());
        if (!enabled) {
            return new AiProviderReadiness(provider(), role(), false, configured, false,
                    providerProperties.getModel(), List.of("PROVIDER_DISABLED"));
        }
        if (!configured) {
            return new AiProviderReadiness(provider(), role(), true, false, false,
                    providerProperties.getModel(), List.of("PROVIDER_NOT_CONFIGURED"));
        }
        return new AiProviderReadiness(provider(), role(), true, true, true,
                providerProperties.getModel(), List.of());
    }

    @Override
    public AiProviderReviewResult review(AiProviderRequest request) {
        return review(request, properties.getRequestTimeoutMs());
    }

    @Override
    public AiProviderReviewResult review(AiProviderRequest request, long timeoutOverrideMs) {
        long started = System.nanoTime();
        AiPromptBuilder.PromptPayload prompt = promptBuilder.build(request, role());
        try {
            AiHttpRequest httpRequest = buildHttpRequest(prompt.dataJson(), timeoutOverrideMs);
            AiHttpResponse response = transport.post(httpRequest);
            long latencyMs = elapsedMs(started);
            if (response.getStatusCode() == 429) {
                return failure(AiProviderCallStatus.RATE_LIMITED, "PROVIDER_RATE_LIMITED", latencyMs);
            }
            if (response.getStatusCode() == 401 || response.getStatusCode() == 403) {
                return failure(AiProviderCallStatus.FAILED, "PROVIDER_AUTH_FAILURE", latencyMs);
            }
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                return failure(AiProviderCallStatus.FAILED, "PROVIDER_HTTP_" + response.getStatusCode(), latencyMs);
            }

            ProviderPayload providerPayload = extractPayload(response);
            AiProviderReviewResult result = responseParser.parse(provider(), role(), providerPayload.content());
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
            return result;
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

    protected abstract AiHttpRequest buildHttpRequest(String promptJson, long timeoutOverrideMs) throws Exception;

    protected abstract ProviderPayload extractPayload(AiHttpResponse response) throws Exception;

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

    private AiProviderReviewResult failure(AiProviderCallStatus status, String code, long latencyMs) {
        AiProviderReviewResult result = AiProviderReviewResult.skipped(provider(), role(), status, code);
        result.setLatencyMs(latencyMs);
        result.setTimeout(status == AiProviderCallStatus.TIMEOUT);
        result.setRateLimited(status == AiProviderCallStatus.RATE_LIMITED);
        result.setErrorCode(code);
        return result;
    }

    private BigDecimal calculateCost(Long inputTokens, Long outputTokens) {
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

    private static Long totalTokens(Long input, Long output, Long total) {
        if (total != null) {
            return total;
        }
        if (input == null && output == null) {
            return null;
        }
        return (input == null ? 0L : input) + (output == null ? 0L : output);
    }

    private static long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000L);
    }

    private static List<String> appendReason(List<String> reasonCodes, String code) {
        java.util.ArrayList<String> copy = new java.util.ArrayList<>(reasonCodes == null ? List.of() : reasonCodes);
        copy.add(code);
        return copy;
    }

    protected record ProviderPayload(String content, String providerRequestId,
                                     Long inputTokens, Long outputTokens, Long totalTokens) {
    }
}
