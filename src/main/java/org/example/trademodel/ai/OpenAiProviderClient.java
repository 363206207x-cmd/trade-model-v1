package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiProviderClient extends AbstractSafeAiProviderClient {
    private final AiOrchestratorProperties properties;
    private final OpenAiModelRouter modelRouter = new OpenAiModelRouter();
    private volatile OpenAiModelRoutingDecision lastRoutingDecision;

    public OpenAiProviderClient(AiOrchestratorProperties properties,
                                AiHttpTransport transport,
                                ObjectMapper objectMapper) {
        super(properties, transport, objectMapper);
        this.properties = properties;
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
        return new AiProviderReadiness(provider(), role(), enabled, configured && validModels, false,
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
                longValue(usage, "total_tokens"));
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
