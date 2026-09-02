package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class OpenAiProviderClient extends AbstractSafeAiProviderClient {
    private final AiOrchestratorProperties properties;
    private final AiHttpTransport backgroundTransport;
    private final ObjectMapper backgroundObjectMapper;
    private final AiDecisionChainPromptBuilder backgroundPromptBuilder;
    private final AiDecisionChainResponseParser backgroundResponseParser;
    private final OpenAiModelRouter modelRouter = new OpenAiModelRouter();
    private volatile OpenAiModelRoutingDecision lastRoutingDecision;

    public OpenAiProviderClient(AiOrchestratorProperties properties,
                                AiHttpTransport transport,
                                ObjectMapper objectMapper) {
        super(properties, transport, objectMapper);
        this.properties = properties;
        this.backgroundTransport = transport;
        this.backgroundObjectMapper = objectMapper;
        this.backgroundPromptBuilder = new AiDecisionChainPromptBuilder(objectMapper, properties);
        this.backgroundResponseParser = new AiDecisionChainResponseParser(objectMapper);
    }

    @Override
    public boolean supportsNativeBackgroundDecisionChain() {
        return true;
    }

    @Override
    public AiDecisionChainResult submitDecisionChainBackground(AiDecisionChainRequest request,
                                                                long timeoutOverrideMs) {
        long started = System.nanoTime();
        AiDecisionChainPromptBuilder.PromptPayload prompt = backgroundPromptBuilder.build(request);
        if (prompt.truncated()) {
            return backgroundFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, "PROMPT_INPUT_TOO_LARGE", false, started);
        }
        try {
            Map<String, Object> body = decisionChainBody(
                    request.getRole(), prompt.dataJson(), reasoningModel());
            body.put("background", true);
            body.put("store", true);

            AiHttpRequest httpRequest = backgroundRequest("/v1/responses", json(body), timeoutOverrideMs);
            httpRequest.getHeaders().put("Idempotency-Key", prompt.inputHash());
            AiHttpResponse response = backgroundTransport.post(httpRequest);
            AiDecisionChainResult result = parseBackgroundResponse(request, response, started);
            result.setSubmittedAt(OffsetDateTime.now(ZoneOffset.UTC));
            return result;
        } catch (HttpTimeoutException exception) {
            return backgroundFailure(request, AiProviderCallStatus.TIMEOUT,
                    AiBackgroundTaskState.FAILED, "OPENAI_ACK_TIMEOUT", true, started);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return backgroundFailure(request, AiProviderCallStatus.TIMEOUT,
                    AiBackgroundTaskState.CANCELLED, "OPENAI_SUBMIT_INTERRUPTED", false, started);
        } catch (IOException exception) {
            return backgroundFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, "PROVIDER_IO_FAILURE", true, started);
        } catch (Exception exception) {
            return backgroundFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, "PROVIDER_FAILURE", false, started);
        }
    }

    @Override
    public AiDecisionChainResult executeDecisionChain(AiDecisionChainRequest request,
                                                       long timeoutOverrideMs) {
        long started = System.nanoTime();
        if (request == null || request.getRole() == null) {
            return applicationWorkerFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, "DECISION_CHAIN_REQUEST_INVALID", started);
        }
        AiDecisionChainPromptBuilder.PromptPayload prompt = backgroundPromptBuilder.build(request);
        if (prompt.truncated()) {
            return applicationWorkerFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, "PROMPT_INPUT_TOO_LARGE", started);
        }
        OffsetDateTime submittedAt = OffsetDateTime.now(ZoneOffset.UTC);
        try {
            AiHttpRequest httpRequest = buildDecisionChainHttpRequest(
                    prompt.dataJson(), request.getRole(), timeoutOverrideMs, reasoningModel());
            AiHttpResponse response = backgroundTransport.post(httpRequest);
            AiDecisionChainResult result = parseBackgroundResponse(request, response, started);
            result.setSubmittedAt(submittedAt);
            result.setStartedAt(submittedAt);
            result.setBackgroundMode("APPLICATION_PERSISTED_WORKER");
            if (result.getTaskState() != null && result.getTaskState().active()) {
                return applicationWorkerFailure(request, AiProviderCallStatus.INVALID_RESPONSE,
                        AiBackgroundTaskState.FAILED,
                        "APPLICATION_WORKER_NON_TERMINAL_RESPONSE", started);
            }
            return result;
        } catch (HttpTimeoutException exception) {
            return applicationWorkerFailure(request, AiProviderCallStatus.TIMEOUT,
                    AiBackgroundTaskState.TIMED_OUT, "GPT_JOB_DEADLINE_EXCEEDED", started);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return applicationWorkerFailure(request, AiProviderCallStatus.TIMEOUT,
                    AiBackgroundTaskState.CANCELLED, "APPLICATION_WORKER_INTERRUPTED", started);
        } catch (IOException exception) {
            return applicationWorkerFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, "PROVIDER_IO_FAILURE", started);
        } catch (Exception exception) {
            return applicationWorkerFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, "PROVIDER_FAILURE", started);
        }
    }

    @Override
    public AiDecisionChainResult pollDecisionChainBackground(AiDecisionChainRequest request,
                                                              String providerResponseId,
                                                              long timeoutOverrideMs) {
        long started = System.nanoTime();
        if (blank(providerResponseId)) {
            return backgroundFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, "PROVIDER_RESPONSE_ID_MISSING", false, started);
        }
        try {
            AiHttpRequest httpRequest = backgroundRequest(
                    "/v1/responses/" + providerResponseId, null, timeoutOverrideMs);
            AiHttpResponse response = backgroundTransport.get(httpRequest);
            return parseBackgroundResponse(request, response, started);
        } catch (HttpTimeoutException exception) {
            return backgroundFailure(request, AiProviderCallStatus.TIMEOUT,
                    AiBackgroundTaskState.RUNNING, "OPENAI_POLL_TIMEOUT", true, started);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return backgroundFailure(request, AiProviderCallStatus.TIMEOUT,
                    AiBackgroundTaskState.CANCELLED, "OPENAI_POLL_INTERRUPTED", false, started);
        } catch (IOException exception) {
            return backgroundFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.RUNNING, "PROVIDER_IO_FAILURE", true, started);
        } catch (Exception exception) {
            return backgroundFailure(request, AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, "PROVIDER_FAILURE", false, started);
        }
    }

    @Override
    public boolean cancelDecisionChainBackground(String providerResponseId, long timeoutOverrideMs) {
        if (blank(providerResponseId)) return false;
        try {
            AiHttpRequest request = backgroundRequest(
                    "/v1/responses/" + providerResponseId + "/cancel", "{}", timeoutOverrideMs);
            AiHttpResponse response = backgroundTransport.post(request);
            return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
        } catch (Exception ignored) {
            return false;
        }
    }

    private AiDecisionChainResult parseBackgroundResponse(AiDecisionChainRequest request,
                                                           AiHttpResponse response,
                                                           long started) throws Exception {
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            boolean retryable = response.getStatusCode() == 408 || response.getStatusCode() == 429
                    || response.getStatusCode() >= 500;
            String code;
            if (response.getStatusCode() == 401 || response.getStatusCode() == 403) {
                code = "PROVIDER_AUTH_FAILURE";
            } else if (response.getStatusCode() == 404) {
                code = "PROVIDER_MODEL_NOT_FOUND";
            } else if (response.getStatusCode() == 429) {
                code = "PROVIDER_RATE_LIMITED";
            } else if (response.getStatusCode() == 400
                    && safeBody(response).toLowerCase(java.util.Locale.ROOT).contains("background")) {
                code = "BACKGROUND_NOT_SUPPORTED";
            } else {
                code = "PROVIDER_HTTP_" + response.getStatusCode();
            }
            return backgroundFailure(request,
                    response.getStatusCode() == 429
                            ? AiProviderCallStatus.RATE_LIMITED : AiProviderCallStatus.FAILED,
                    AiBackgroundTaskState.FAILED, code, retryable, started);
        }

        JsonNode root = backgroundObjectMapper.readTree(response.getBody());
        String status = text(root, "status");
        String responseId = text(root, "id");
        if (blank(responseId)) responseId = response.firstHeader("x-request-id");
        AiBackgroundTaskState taskState = switch (status == null ? "" : status) {
            case "queued" -> AiBackgroundTaskState.QUEUED;
            case "in_progress" -> AiBackgroundTaskState.RUNNING;
            case "completed" -> AiBackgroundTaskState.SUCCEEDED;
            case "cancelled" -> AiBackgroundTaskState.CANCELLED;
            case "failed", "incomplete" -> AiBackgroundTaskState.FAILED;
            default -> AiBackgroundTaskState.SUBMITTED;
        };
        if (taskState == AiBackgroundTaskState.QUEUED
                || taskState == AiBackgroundTaskState.SUBMITTED
                || taskState == AiBackgroundTaskState.RUNNING) {
            AiDecisionChainResult active = new AiDecisionChainResult();
            active.setProvider(provider());
            active.setRole(request.getRole());
            active.setCallStatus(AiProviderCallStatus.STARTED);
            active.setTaskState(taskState);
            active.setProviderRequestId(responseId);
            active.setSelectedModel(text(root, "model") == null ? reasoningModel() : text(root, "model"));
            active.setLatencyMs(elapsedMs(started));
            active.setSubmittedAt(OffsetDateTime.now(ZoneOffset.UTC));
            active.setBackgroundMode("PROVIDER_NATIVE");
            return active;
        }
        if (taskState == AiBackgroundTaskState.CANCELLED) {
            AiDecisionChainResult cancelled = backgroundFailure(request, AiProviderCallStatus.FAILED,
                    taskState, "PROVIDER_TASK_CANCELLED", false, started);
            cancelled.setProviderRequestId(responseId);
            return cancelled;
        }
        if (taskState == AiBackgroundTaskState.FAILED) {
            String incompleteReason = root.path("incomplete_details").path("reason").asText(null);
            String code = "max_output_tokens".equals(incompleteReason)
                    ? "OUTPUT_TRUNCATED" : "PROVIDER_BACKGROUND_FAILED";
            AiDecisionChainResult failed = backgroundFailure(request,
                    "OUTPUT_TRUNCATED".equals(code)
                            ? AiProviderCallStatus.INVALID_RESPONSE : AiProviderCallStatus.FAILED,
                    taskState, code, false, started);
            failed.setProviderRequestId(responseId);
            return failed;
        }

        ProviderPayload payload = extractPayload(response);
        String raw = payload.rawContent() == null ? payload.content() : payload.rawContent();
        AiDecisionChainResult result = backgroundResponseParser.parse(provider(), request.getRole(), raw);
        if (!result.successful() && blank(result.getFailureClassification())) {
            result.setFailureClassification(result.getErrorCode());
        }
        result.setProviderRequestId(responseId);
        result.setLatencyMs(elapsedMs(started));
        result.setInputTokens(payload.inputTokens());
        result.setOutputTokens(payload.outputTokens());
        result.setTotalTokens(totalTokens(payload.inputTokens(), payload.outputTokens(), payload.totalTokens()));
        result.setCalculatedCostUsd(calculateCost(payload.inputTokens(), payload.outputTokens()));
        result.setReasoningTokens(longValue(root.path("usage").path("output_tokens_details"),
                "reasoning_tokens"));
        result.setSelectedModel(text(root, "model") == null ? reasoningModel() : text(root, "model"));
        result.setTaskState(result.successful()
                ? AiBackgroundTaskState.SUCCEEDED : AiBackgroundTaskState.FAILED);
        result.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        result.setBackgroundMode("PROVIDER_NATIVE");
        return result;
    }

    private AiHttpRequest backgroundRequest(String path, String body, long timeoutMs) {
        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(), path), body, timeoutMs);
        Map<String, String> headers = jsonHeaders();
        headers.put("Authorization", "Bearer " + providerProperties().getApiKey());
        request.setHeaders(headers);
        return request;
    }

    private AiDecisionChainResult backgroundFailure(AiDecisionChainRequest request,
                                                     AiProviderCallStatus status,
                                                     AiBackgroundTaskState taskState,
                                                     String code,
                                                     boolean retryable,
                                                     long started) {
        AiDecisionChainResult result = AiDecisionChainResult.failed(
                provider(), request == null ? null : request.getRole(), status, code);
        result.setTaskState(taskState);
        result.setFailureClassification(code);
        result.setRetryable(retryable);
        result.setLatencyMs(elapsedMs(started));
        result.setSelectedModel(reasoningModel());
        result.setBackgroundMode("PROVIDER_NATIVE");
        return result;
    }

    private AiDecisionChainResult applicationWorkerFailure(AiDecisionChainRequest request,
                                                            AiProviderCallStatus status,
                                                            AiBackgroundTaskState taskState,
                                                            String code,
                                                            long started) {
        AiDecisionChainResult result = backgroundFailure(
                request, status, taskState, code, false, started);
        result.setBackgroundMode("APPLICATION_PERSISTED_WORKER");
        return result;
    }

    private Map<String, Object> decisionChainBody(AiDecisionChainRole role,
                                                   String promptJson,
                                                   String selectedModel) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", selectedModel);
        body.put("instructions", AiDecisionChainPromptBuilder.systemInstruction(role));
        body.put("input", List.of(Map.of("role", "user", "content", promptJson)));
        body.put("max_output_tokens", properties.getBackgroundExecution().getStructuredMaxOutputTokens());
        body.put("reasoning", Map.of("effort",
                properties.getBackgroundExecution().getReasoningEffort()));
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("verbosity", properties.getBackgroundExecution().getTextVerbosity());
        text.put("format", Map.of(
                "type", "json_schema",
                "name", "fundamental_ai_v41_gpt_candidate",
                "strict", true,
                "schema", AiDecisionChainSchema.responseJsonSchema(role)));
        body.put("text", text);
        return body;
    }

    private static String safeBody(AiHttpResponse response) {
        return response == null || response.getBody() == null ? "" : response.getBody();
    }

    private String reasoningModel() {
        String model = providerProperties().getGptFinal().getReasoningModel();
        return model == null ? null : model.trim();
    }

    @Override
    public AiProviderName provider() {
        return AiProviderName.OPENAI;
    }

    @Override
    public AiProviderRole role() {
        return AiProviderRole.GPT_RULE_REVIEW;
    }

    @Override
    public AiProviderProperties providerProperties() {
        return properties.getOpenai();
    }

    @Override
    public AiProviderReadiness readiness() {
        AiProviderProperties provider = providerProperties();
        boolean enabled = provider.isEnabled();
        boolean configured = provider.hasKeyAndModel() && !blank(provider.getBaseUrl());
        OpenAiModelRouter.RoutePlan plan = modelRouter.plan(null, provider.getGptFinal());
        boolean validModels = modelRouter.valid(plan);
        OpenAiModelRoutingDecision last = lastRoutingDecision;
        String configuredModel = plan.models().isEmpty() ? null : plan.models().get(0);
        String effectiveModel = last == null ? configuredModel : last.selectedModel();
        boolean fallbackUsed = last != null && last.fallbackUsed();
        String fallbackReason = last == null ? null : last.fallbackReason();
        AiModelReadinessStatus status;
        if (!validModels || last != null && !last.available()) {
            status = AiModelReadinessStatus.MODEL_UNAVAILABLE;
        } else if (fallbackUsed) {
            status = AiModelReadinessStatus.MODEL_FALLBACK_ACTIVE;
        } else if (last != null && last.available()) {
            status = AiModelReadinessStatus.MODEL_ACTIVE;
        } else {
            status = AiModelReadinessStatus.MODEL_CONFIGURED;
        }
        List<String> reasons = switch (status) {
            case MODEL_UNAVAILABLE -> List.of("OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE");
            case MODEL_FALLBACK_ACTIVE -> List.of(fallbackReason);
            case MODEL_ACTIVE -> List.of("MODEL_CALL_VERIFIED");
            case MODEL_CONFIGURED -> List.of(enabled && configured
                    ? "MODEL_AVAILABILITY_UNVERIFIED" : "PROVIDER_DISABLED");
        };
        boolean ready = enabled && configured && validModels
                && status == AiModelReadinessStatus.MODEL_ACTIVE;
        return new AiProviderReadiness(provider(), role(), enabled, configured && validModels, ready,
                configuredModel, effectiveModel, fallbackUsed, fallbackReason,
                last == null ? GptFinalModelStrategy.FAST_DECISION_MODEL.name()
                        : last.modelStrategy().name(),
                status, reasons);
    }

    @Override
    public AiProviderReviewResult review(AiProviderRequest request, long timeoutOverrideMs) {
        OpenAiModelRouter.RoutePlan plan = modelRouter.plan(request, providerProperties().getGptFinal());
        if (!modelRouter.valid(plan)) {
            return unavailable(plan, request, 0, List.of("OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE"));
        }

        long started = System.nanoTime();
        List<String> routingReasons = new ArrayList<>();
        AiProviderReviewResult latest = null;
        int lastAttempt = 0;
        for (int index = 0; index < plan.models().size(); index++) {
            lastAttempt = index;
            if (index > 0) {
                routingReasons.add(fallbackReason(index));
            }
            long remainingMs = Math.max(1L, timeoutOverrideMs
                    - (System.nanoTime() - started) / 1_000_000L);
            latest = reviewWithModel(request, remainingMs, plan.models().get(index));
            if (latest.successful()) {
                String fallbackReason = fallbackReason(index);
                return decorate(latest, plan, request, index, fallbackReason, routingReasons, true);
            }
            if (!retryableModelFailure(latest) || index == plan.models().size() - 1) {
                break;
            }
            routingReasons.add(primaryFailureReason(plan.strategy(), latest));
        }

        if (plan.models().size() > 1 && latest != null && retryableModelFailure(latest)
                && lastAttempt == plan.models().size() - 1) {
            routingReasons.add("OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE");
            return unavailable(plan, request, lastAttempt, routingReasons);
        }
        return decorate(latest, plan, request, lastAttempt, fallbackReason(lastAttempt),
                routingReasons, !retryableModelFailure(latest));
    }

    @Override
    protected AiHttpRequest buildHttpRequest(String promptJson, long timeoutOverrideMs,
                                             String selectedModel) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", selectedModel);
        body.put("instructions", AiPromptBuilder.SYSTEM_INSTRUCTION);
        body.put("input", List.of(Map.of("role", "user", "content", promptJson)));
        body.put("max_output_tokens", maxOutputTokens());
        if (selectedModel.startsWith("gpt-5.")) {
            body.put("reasoning", Map.of("effort", "high"));
        } else {
            body.put("temperature", 0);
        }

        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(), "/v1/responses"),
                json(body), timeoutOverrideMs);
        Map<String, String> headers = jsonHeaders();
        headers.put("Authorization", "Bearer " + providerProperties().getApiKey());
        request.setHeaders(headers);
        return request;
    }

    @Override
    protected AiHttpRequest buildDecisionChainHttpRequest(String promptJson,
                                                          AiDecisionChainRole role,
                                                          long timeoutOverrideMs,
                                                          String selectedModel) throws Exception {
        Map<String, Object> body = decisionChainBody(role, promptJson, reasoningModel());
        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(), "/v1/responses"),
                json(body), timeoutOverrideMs);
        Map<String, String> headers = jsonHeaders();
        headers.put("Authorization", "Bearer " + providerProperties().getApiKey());
        request.setHeaders(headers);
        return request;
    }

    @Override
    protected ProviderPayload extractPayload(AiHttpResponse response) throws Exception {
        JsonNode root = readTree(response.getBody());
        String content = text(root, "output_text");
        if (blank(content)) {
            JsonNode output = root.path("output");
            if (output.isArray()) {
                for (JsonNode item : output) {
                    JsonNode contentNodes = item.path("content");
                    if (contentNodes.isArray()) {
                        for (JsonNode contentNode : contentNodes) {
                            content = text(contentNode, "text");
                            if (!blank(content)) {
                                break;
                            }
                        }
                    }
                    if (!blank(content)) {
                        break;
                    }
                }
            }
        }
        JsonNode usage = root.path("usage");
        String requestId = response.firstHeader("x-request-id");
        if (blank(requestId)) {
            requestId = text(root, "id");
        }
        return new ProviderPayload(content, requestId,
                longValue(usage, "input_tokens"),
                longValue(usage, "output_tokens"),
                longValue(usage, "total_tokens"),
                null,
                null);
    }

    private AiProviderReviewResult unavailable(OpenAiModelRouter.RoutePlan plan,
                                               AiProviderRequest request, int level,
                                               List<String> reasons) {
        AiProviderReviewResult result = AiProviderReviewResult.skipped(
                provider(), role(), AiProviderCallStatus.FAILED, "OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE");
        result.setErrorCode("MODEL_UNAVAILABLE");
        result.setReasonCodes(reasons);
        return decorate(result, plan, request, level, "OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE",
                reasons, false);
    }

    private AiProviderReviewResult decorate(AiProviderReviewResult result,
                                            OpenAiModelRouter.RoutePlan plan,
                                            AiProviderRequest request, int level,
                                            String fallbackReason, List<String> routingReasons,
                                            boolean available) {
        if (result == null) {
            return unavailable(plan, request, level, routingReasons);
        }
        OpenAiModelRoutingDecision decision = modelRouter.decision(
                plan, level, level > 0 || !available ? fallbackReason : null, request, available);
        if (result.successful() || !available) {
            lastRoutingDecision = decision;
        }
        result.setOriginalModel(decision.originalModel());
        result.setSelectedModel(decision.selectedModel());
        result.setFallbackLevel(decision.fallbackLevel());
        result.setModelStrategy(decision.modelStrategy().name());
        result.setModelRoutingTimestamp(decision.timestamp());
        result.setModelRoutingTraceId(decision.traceId());
        if (decision.fallbackUsed()) {
            result.setFallback(true);
            result.setFallbackReason(decision.fallbackReason());
        }
        List<String> reasons = new ArrayList<>(result.getReasonCodes());
        for (String routingReason : routingReasons) {
            if (routingReason != null && !routingReason.isBlank() && !reasons.contains(routingReason)) {
                reasons.add(routingReason);
            }
        }
        result.setReasonCodes(reasons);
        return result;
    }

    private static boolean retryableModelFailure(AiProviderReviewResult result) {
        if (result == null) {
            return true;
        }
        String code = result.getErrorCode() == null ? "" : result.getErrorCode();
        return result.getCallStatus() == AiProviderCallStatus.TIMEOUT
                || "PROVIDER_MODEL_NOT_FOUND".equals(code)
                || "PROVIDER_IO_FAILURE".equals(code)
                || code.startsWith("PROVIDER_HTTP_5");
    }

    private static String primaryFailureReason(GptFinalModelStrategy strategy,
                                               AiProviderReviewResult result) {
        if (result != null && result.getCallStatus() == AiProviderCallStatus.TIMEOUT
                && strategy == GptFinalModelStrategy.FAST_DECISION_MODEL) {
            return "OPENAI_FAST_MODEL_TIMEOUT";
        }
        if (strategy == GptFinalModelStrategy.DEEP_REASONING_MODEL) {
            return "OPENAI_REASONING_MODEL_UNAVAILABLE";
        }
        return "OPENAI_PRIMARY_UNAVAILABLE";
    }

    private static String fallbackReason(int level) {
        return switch (level) {
            case 1 -> "OPENAI_FALLBACK_GPT55";
            case 2 -> "OPENAI_FALLBACK_GPT54";
            default -> null;
        };
    }
}
