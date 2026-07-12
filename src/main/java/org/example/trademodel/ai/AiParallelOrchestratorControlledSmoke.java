package org.example.trademodel.ai;

import org.example.trademodel.service.AiDecisionOrchestratorService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Explicitly gated, review-only entrypoint for controlled parallel provider evidence. */
public final class AiParallelOrchestratorControlledSmoke {
    public static final String ENABLE_EXTERNAL_CALLS = "AI_PARALLEL_SMOKE_ENABLE_EXTERNAL_CALLS";
    public static final String HARNESS_ENTRY = "AI_PARALLEL_SMOKE_HARNESS_ENTRY";
    public static final String HARNESS_CONFIRMATION = "I_CONFIRM_THREE_PROVIDER_PARALLEL_SMOKE";
    public static final String FINAL_RESULT_ORDER =
            "GPT_RULE_REVIEW,GEMINI_CONSISTENCY_REVIEW,GROK_ADVERSARIAL_CHALLENGE";

    private static final List<AiProviderName> PROVIDERS = List.of(
            AiProviderName.OPENAI, AiProviderName.GEMINI, AiProviderName.XAI);
    private static final List<AiProviderRole> ROLES = List.of(
            AiProviderRole.GPT_RULE_REVIEW,
            AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
            AiProviderRole.GROK_ADVERSARIAL_CHALLENGE);

    public SmokeResult run(Map<String, String> environment,
                           AiDecisionOrchestratorService orchestrator,
                           CallCountAudit callCountAudit) {
        Map<String, String> env = environment == null ? Map.of() : environment;
        String gateStatus = gateStatus(env);
        if (gateStatus != null) {
            return SmokeResult.skipped(gateStatus);
        }
        if (orchestrator == null) {
            return SmokeResult.skipped("FAIL_ORCHESTRATOR_UNAVAILABLE");
        }

        AiOrchestratorResult result;
        try {
            result = orchestrator.review(fixedReviewRequest());
        } catch (Exception exception) {
            return SmokeResult.failed("FAIL_ORCHESTRATOR_EXCEPTION", safeCounts(callCountAudit), 3);
        }
        Map<AiProviderName, String> counts = safeCounts(callCountAudit);
        if (!safeResult(result)) {
            return SmokeResult.from("FAIL_SAFETY_CONTRACT", result, counts, 3);
        }
        String status = switch (result.getOrchestrationMode()) {
            case AI_ASSISTED -> "PASS";
            case PARTIAL_FALLBACK -> "PASS_PARTIAL_FALLBACK";
            case RULE_ONLY_FALLBACK -> "PASS_RULE_ONLY_FALLBACK";
        };
        return SmokeResult.from(status, result, counts, 3);
    }

    public String gateStatus(Map<String, String> environment) {
        Map<String, String> env = environment == null ? Map.of() : environment;
        if (!"true".equals(env.get(ENABLE_EXTERNAL_CALLS))) {
            return "SKIPPED_EXTERNAL_CALLS_DISABLED";
        }
        if (!HARNESS_CONFIRMATION.equals(env.get(HARNESS_ENTRY))) {
            return "SKIPPED_HARNESS_ENTRY_MISSING";
        }
        if (!enabled(env, "TRADE_MODEL_AI_ENABLED")
                || !enabled(env, "TRADE_MODEL_AI_OPENAI_ENABLED")
                || !enabled(env, "TRADE_MODEL_AI_GEMINI_ENABLED")
                || !enabled(env, "TRADE_MODEL_AI_XAI_ENABLED")) {
            return "SKIPPED_PROVIDER_DISABLED";
        }
        if (!present(env, "OPENAI_API_KEY")
                || !present(env, "GEMINI_API_KEY")
                || !present(env, "XAI_API_KEY")) {
            return "SKIPPED_MISSING_API_KEY";
        }
        return null;
    }

    public static AiProviderRequest fixedReviewRequest() {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("ai-parallel-controlled-smoke");
        request.setTraceId("ai-parallel-controlled-smoke-trace");
        request.setSymbol("BTCUSDT");
        request.setTimeframe("15m");
        request.setRuleMarketBias("BULLISH");
        request.setRuleConfidence("MEDIUM");
        request.setRuleRiskLevel("MEDIUM");
        request.setRuleWorthOpening(false);
        request.setDataQualityScore(82);
        request.setTrendStructureScore(68);
        request.setMultiTimeframeState("15m structure supported; higher timeframe confirmation pending");
        request.setExternalContextState("NOT_REQUIRED_FOR_FIXED_REVIEW_FIXTURE");
        request.setEvidenceSummary(
                "Rule evidence supports a bullish 15m bias with medium confidence; review consistency and conflicts only.");
        request.setScoreSummary("dataQuality=82;trendStructure=68;risk=MEDIUM");
        request.setDecisionFacts(Map.of(
                "reviewOnly", true,
                "manualReviewOnly", true,
                "notTradeInstruction", true,
                "notExecutable", true,
                "ruleDirectionPreserved", true));
        request.setRequestTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        return request;
    }

    private static boolean safeResult(AiOrchestratorResult result) {
        if (result == null
                || !result.isReviewOnly()
                || !result.isManualReviewOnly()
                || !result.isNotTradeInstruction()
                || !result.isNotExecutable()
                || !result.isNotAutoTrading()
                || !result.isNotOrderExecution()
                || !result.isNotUserPositionCreation()
                || !result.isNotPositionMutation()
                || !result.isNotExecutionPlanCreation()
                || !result.isRuleDirectionPreserved()) {
            return false;
        }
        List<AiProviderRole> actualRoles = result.getProviderResults().stream()
                .map(AiProviderReviewResult::getRole)
                .toList();
        return actualRoles.equals(ROLES);
    }

    private static Map<AiProviderName, String> safeCounts(CallCountAudit audit) {
        if (audit == null) {
            return zeroCounts();
        }
        try {
            Map<AiProviderName, String> supplied = audit.snapshot();
            Map<AiProviderName, String> safe = new EnumMap<>(AiProviderName.class);
            for (AiProviderName provider : PROVIDERS) {
                String value = supplied == null ? null : supplied.get(provider);
                safe.put(provider, "0".equals(value) || "1".equals(value) ? value : "UNKNOWN_MAX_1");
            }
            return safe;
        } catch (Exception exception) {
            return unknownCounts();
        }
    }

    private static boolean enabled(Map<String, String> env, String name) {
        return "true".equals(env.get(name));
    }

    private static boolean present(Map<String, String> env, String name) {
        String value = env.get(name);
        return value != null && !value.isBlank();
    }

    private static Map<AiProviderName, String> zeroCounts() {
        Map<AiProviderName, String> counts = new EnumMap<>(AiProviderName.class);
        PROVIDERS.forEach(provider -> counts.put(provider, "0"));
        return counts;
    }

    private static Map<AiProviderName, String> unknownCounts() {
        Map<AiProviderName, String> counts = new EnumMap<>(AiProviderName.class);
        PROVIDERS.forEach(provider -> counts.put(provider, "UNKNOWN_MAX_1"));
        return counts;
    }

    public interface CallCountAudit {
        Map<AiProviderName, String> snapshot();
    }

    public record ProviderEvidence(String status, String httpStatusClass,
                                   String parseStatus, long latencyMs, String callCount) {
        private static ProviderEvidence notRun(String count) {
            return new ProviderEvidence("NOT_RUN", "NOT_RUN", "NOT_RUN", 0L, count);
        }
    }

    public record SmokeResult(String status,
                              String orchestrationMode,
                              long orchestrationLatencyMs,
                              boolean globalDeadlineExceeded,
                              int providerSubmittedCount,
                              int providerCompletedCount,
                              int providerSuccessCount,
                              int providerTimeoutCount,
                              int providerFailedCount,
                              boolean partialFallbackUsed,
                              Map<AiProviderName, ProviderEvidence> providers,
                              String liveProviderCalls,
                              int realKeysRead,
                              String finalResultOrder) {
        private static SmokeResult skipped(String status) {
            Map<AiProviderName, ProviderEvidence> providers = new EnumMap<>(AiProviderName.class);
            zeroCounts().forEach((provider, count) -> providers.put(provider, ProviderEvidence.notRun(count)));
            return new SmokeResult(status, "NOT_RUN", 0L, false,
                    0, 0, 0, 0, 0, false, providers, "0", 0, "NOT_RUN");
        }

        private static SmokeResult failed(String status, Map<AiProviderName, String> counts, int keysRead) {
            Map<AiProviderName, ProviderEvidence> providers = new EnumMap<>(AiProviderName.class);
            counts.forEach((provider, count) -> providers.put(provider, ProviderEvidence.notRun(count)));
            return new SmokeResult(status, "RULE_ONLY_FALLBACK", 0L, false,
                    0, 0, 0, 0, 3, false, providers, aggregateCount(counts), keysRead, "NOT_AVAILABLE");
        }

        private static SmokeResult from(String status, AiOrchestratorResult result,
                                        Map<AiProviderName, String> counts, int keysRead) {
            if (result == null) {
                return failed(status, counts, keysRead);
            }
            Map<AiProviderName, ProviderEvidence> evidence = new EnumMap<>(AiProviderName.class);
            for (AiProviderReviewResult providerResult : result.getProviderResults()) {
                String count = counts.getOrDefault(providerResult.getProvider(), "UNKNOWN_MAX_1");
                evidence.put(providerResult.getProvider(), providerEvidence(providerResult, count));
            }
            for (AiProviderName provider : PROVIDERS) {
                evidence.putIfAbsent(provider,
                        ProviderEvidence.notRun(counts.getOrDefault(provider, "UNKNOWN_MAX_1")));
            }
            return new SmokeResult(status, result.getOrchestrationMode().name(),
                    result.getOrchestrationLatencyMs(), result.isGlobalDeadlineExceeded(),
                    result.getProviderSubmittedCount(), result.getProviderCompletedCount(),
                    result.getProviderSuccessCount(), result.getProviderTimeoutCount(),
                    result.getProviderFailedCount(), result.isPartialFallbackUsed(),
                    evidence, aggregateCount(counts), keysRead, finalOrder(result));
        }

        public List<String> sanitizedOutputLines() {
            List<String> lines = new ArrayList<>();
            lines.add("AI_PARALLEL_LIVE_SMOKE: " + status);
            lines.add("AI_PARALLEL_SMOKE_STATUS: " + status);
            lines.add("ORCHESTRATION_MODE: " + orchestrationMode);
            lines.add("ORCHESTRATION_LATENCY_MS: " + orchestrationLatencyMs);
            lines.add("GLOBAL_DEADLINE_EXCEEDED: " + globalDeadlineExceeded);
            lines.add("PROVIDER_SUBMITTED_COUNT: " + providerSubmittedCount);
            lines.add("PROVIDER_COMPLETED_COUNT: " + providerCompletedCount);
            lines.add("PROVIDER_SUCCESS_COUNT: " + providerSuccessCount);
            lines.add("PROVIDER_TIMEOUT_COUNT: " + providerTimeoutCount);
            lines.add("PROVIDER_FAILED_COUNT: " + providerFailedCount);
            lines.add("PARTIAL_FALLBACK_USED: " + partialFallbackUsed);
            appendProvider(lines, "OPENAI", providers.get(AiProviderName.OPENAI));
            appendProvider(lines, "GEMINI", providers.get(AiProviderName.GEMINI));
            appendProvider(lines, "XAI", providers.get(AiProviderName.XAI));
            lines.add("FINAL_RESULT_ORDER: " + finalResultOrder);
            lines.add("LIVE_PROVIDER_CALLS: " + liveProviderCalls);
            lines.add("REAL_KEYS_READ: " + realKeysRead);
            lines.add("PRODUCTION_READINESS: BLOCKED");
            return List.copyOf(lines);
        }

        private static void appendProvider(List<String> lines, String prefix, ProviderEvidence evidence) {
            ProviderEvidence value = evidence == null ? ProviderEvidence.notRun("UNKNOWN_MAX_1") : evidence;
            lines.add(prefix + "_STATUS: " + value.status());
            lines.add(prefix + "_HTTP_STATUS_CLASS: " + value.httpStatusClass());
            lines.add(prefix + "_PARSE_STATUS: " + value.parseStatus());
            lines.add(prefix + "_LATENCY_MS: " + value.latencyMs());
            lines.add(prefix + "_CALL_COUNT: " + value.callCount());
        }

        private static ProviderEvidence providerEvidence(AiProviderReviewResult result, String count) {
            String status = result.getCallStatus() == null ? "FAILED" : result.getCallStatus().name();
            String parseStatus = result.successful() ? "PASS"
                    : result.getCallStatus() == AiProviderCallStatus.INVALID_RESPONSE
                    ? "FAIL_RESPONSE_SCHEMA" : "NOT_AVAILABLE";
            return new ProviderEvidence(status, httpStatusClass(result), parseStatus,
                    result.getLatencyMs() == null ? 0L : Math.max(0L, result.getLatencyMs()), count);
        }

        private static String httpStatusClass(AiProviderReviewResult result) {
            if (result.successful() || result.getCallStatus() == AiProviderCallStatus.INVALID_RESPONSE) {
                return "2XX";
            }
            if (result.getCallStatus() == AiProviderCallStatus.TIMEOUT) {
                return "TIMEOUT";
            }
            if (result.getCallStatus() == AiProviderCallStatus.RATE_LIMITED) {
                return "4XX";
            }
            String code = result.getErrorCode() == null ? "" : result.getErrorCode();
            if (code.contains("AUTH") || code.contains("MODEL_NOT_FOUND") || code.contains("BILLING")) {
                return "4XX";
            }
            int marker = code.indexOf("PROVIDER_HTTP_");
            if (marker >= 0 && code.length() >= marker + 17) {
                char first = code.charAt(marker + 14);
                return first >= '1' && first <= '5' ? first + "XX" : "NOT_AVAILABLE";
            }
            return "NOT_AVAILABLE";
        }

        private static String finalOrder(AiOrchestratorResult result) {
            String joined = String.join(",", result.getProviderResults().stream()
                    .map(providerResult -> providerResult.getRole().name())
                    .toList());
            return joined.isBlank() ? "NOT_AVAILABLE" : joined;
        }

        private static String aggregateCount(Map<AiProviderName, String> counts) {
            if (counts.values().stream().anyMatch(value -> !"0".equals(value) && !"1".equals(value))) {
                return "UNKNOWN_MAX_3";
            }
            int total = counts.values().stream().mapToInt(value -> "1".equals(value) ? 1 : 0).sum();
            return Integer.toString(total);
        }
    }
}
