package org.example.trademodel.ai;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class OpenAiModelRouter {
    private static final Set<String> GPT56_MODELS = Set.of(
            "gpt-5.6", "gpt-5.6-sol", "gpt-5.6-terra", "gpt-5.6-luna");
    private static final Set<String> GPT55_MODELS = Set.of(
            "gpt-5.5", "gpt-5.5-2026-04-23");
    private static final Set<String> GPT54_MODELS = Set.of(
            "gpt-5.4", "gpt-5.4-2026-03-05");
    private static final Set<String> DEEP_SIGNAL_KEYS = Set.of(
            "aiconflict", "confused", "hotreset", "extrememarketevent",
            "highriskposition", "multitimeframecontradiction", "ruleevidenceconflict");

    public RoutePlan plan(AiProviderRequest request, GptFinalModelRoutingProperties properties) {
        GptFinalModelStrategy strategy = requiresDeepReasoning(request)
                ? GptFinalModelStrategy.DEEP_REASONING_MODEL
                : GptFinalModelStrategy.FAST_DECISION_MODEL;
        String primary = strategy == GptFinalModelStrategy.DEEP_REASONING_MODEL
                ? normalized(properties.getReasoningModel())
                : normalized(properties.getFastModel());
        List<String> models = new ArrayList<>();
        models.add(primary);
        if (properties.isFallbackEnabled()) {
            models.addAll(properties.getFallbackModels().stream().map(OpenAiModelRouter::normalized).toList());
        }
        return new RoutePlan(strategy, List.copyOf(models));
    }

    public OpenAiModelRoutingDecision decision(RoutePlan plan, int fallbackLevel,
                                               String fallbackReason, AiProviderRequest request,
                                               boolean available) {
        String selected = fallbackLevel >= 0 && fallbackLevel < plan.models().size()
                ? plan.models().get(fallbackLevel) : null;
        return new OpenAiModelRoutingDecision(plan.strategy(), plan.models().get(0), selected,
                Math.max(0, fallbackLevel), fallbackReason, LocalDateTime.now(),
                request == null ? null : request.getTraceId(), available);
    }

    public boolean valid(RoutePlan plan) {
        if (plan == null || plan.models().isEmpty() || !isApprovedPrimary(plan.models().get(0))) {
            return false;
        }
        if (plan.models().size() == 1) {
            return true;
        }
        return plan.models().size() == 3
                && isApprovedGpt55(plan.models().get(1))
                && isApprovedGpt54(plan.models().get(2));
    }

    public static boolean isApprovedPrimary(String model) {
        return GPT56_MODELS.contains(normalized(model));
    }

    public static boolean isApprovedGpt55(String model) {
        return GPT55_MODELS.contains(normalized(model));
    }

    public static boolean isApprovedGpt54(String model) {
        return GPT54_MODELS.contains(normalized(model));
    }

    public static boolean isApprovedModel(String model) {
        return isApprovedPrimary(model) || isApprovedGpt55(model) || isApprovedGpt54(model);
    }

    private static boolean requiresDeepReasoning(AiProviderRequest request) {
        if (request == null) {
            return false;
        }
        if (containsAny(request.getRuleRiskLevel(), "HIGH", "EXTREME", "CRITICAL")
                || containsAny(request.getMultiTimeframeState(), "MISALIGNED", "CONTRADICTION", "DIVERGENCE")
                || containsAny(request.getExternalContextState(), "BLOCKED", "EXTREME", "CRITICAL")) {
            return true;
        }
        for (Map.Entry<String, Object> entry : request.getDecisionFacts().entrySet()) {
            String key = normalizedKey(entry.getKey());
            if (DEEP_SIGNAL_KEYS.contains(key) && truthy(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.doubleValue() > 0;
        }
        String text = normalized(value == null ? null : value.toString());
        return !text.isBlank() && !"false".equals(text) && !"none".equals(text) && !"0".equals(text);
    }

    private static boolean containsAny(String value, String... tokens) {
        String normalized = normalized(value).toUpperCase(Locale.ROOT);
        for (String token : tokens) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizedKey(String value) {
        return normalized(value).replaceAll("[^a-z0-9]", "");
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record RoutePlan(GptFinalModelStrategy strategy, List<String> models) {
    }
}
