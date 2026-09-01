package org.example.trademodel.service.support;

import org.example.trademodel.vo.ScoreItemVO;
import org.example.trademodel.vo.EvidenceItemVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Single deterministic owner for the v4.1 score DAG. Every result carries its
 * missing inputs so callers can fail closed instead of substituting neutral data.
 */
public final class V41DecisionContractPolicy {
    public static final String SCORE_VERSION = "V41-SCORE-1";
    public static final String DATA_QUALITY_VERSION = "V41-DQ-1";
    public static final String PLAN_SOURCE_VERSION = "V41-PLAN-SOURCE-1";

    public static final String TREND_STRUCTURE = "趋势结构分";
    public static final String CAPITAL_FLOW = "资金推动分";
    public static final String LEVERAGE_RISK = "杠杆风险分";
    public static final String LIQUIDITY_QUALITY = "流动性质量分";
    public static final String SENTIMENT = "情绪温度分";
    public static final String EVENT_IMPACT = "事件冲击分";
    public static final String MACRO = "宏观环境分";
    public static final String EVIDENCE_RELIABILITY = "证据可信度分";
    private static final Set<String> EXPLICIT_NO_VALUE_EVIDENCE_STATES = Set.of(
            "UNAVAILABLE", "SOURCE_UNAVAILABLE", "INSUFFICIENT_DATA",
            "INVALID", "PENDING_VERIFICATION");

    private V41DecisionContractPolicy() {
    }

    public static Metric dataQuality(Double completeness,
                                     Double freshness,
                                     Double providerHealth,
                                     Double crossSourceConsistency,
                                     Double sampleAdequacy) {
        return weighted("DATA_QUALITY", Map.of(
                "Completeness", input(completeness, 0.30),
                "Freshness", input(freshness, 0.25),
                "ProviderHealth", input(providerHealth, 0.20),
                "CrossSourceConsistency", input(crossSourceConsistency, 0.15),
                "SampleAdequacy", input(sampleAdequacy, 0.10)));
    }

    public static Metric evidenceReliability(Double evidenceCoverage,
                                             Double sourceQuality,
                                             Double freshness,
                                             Double crossSourceConsistency,
                                             Double sampleAdequacy) {
        return weighted("EVIDENCE_RELIABILITY", Map.of(
                "EvidenceCoverage", input(evidenceCoverage, 0.25),
                "SourceQuality", input(sourceQuality, 0.20),
                "Freshness", input(freshness, 0.20),
                "CrossSourceConsistency", input(crossSourceConsistency, 0.20),
                "SampleAdequacy", input(sampleAdequacy, 0.15)));
    }

    public static Metric opportunityScore(List<ScoreItemVO> scores,
                                          Integer evidenceReliability,
                                          Integer conflictPenalty,
                                          Integer stalePenalty) {
        Map<String, Double> byType = scoreMap(scores);
        Map<String, WeightedInput> inputs = new LinkedHashMap<>();
        inputs.put(TREND_STRUCTURE, input(byType.get(TREND_STRUCTURE), 0.30));
        inputs.put(CAPITAL_FLOW, input(byType.get(CAPITAL_FLOW), 0.20));
        inputs.put(LIQUIDITY_QUALITY, input(byType.get(LIQUIDITY_QUALITY), 0.15));
        inputs.put(SENTIMENT, input(byType.get(SENTIMENT), 0.10));
        inputs.put(EVENT_IMPACT, input(byType.get(EVENT_IMPACT), 0.10));
        inputs.put(MACRO, input(byType.get(MACRO), 0.05));
        inputs.put(EVIDENCE_RELIABILITY, input(number(evidenceReliability), 0.10));
        Metric base = weighted("OPPORTUNITY_SCORE", inputs);
        if (base.value() == null) return base;
        Double leverageRisk = byType.get(LEVERAGE_RISK);
        List<String> missing = new ArrayList<>(base.missingInputs());
        if (leverageRisk == null) missing.add(LEVERAGE_RISK);
        if (!missing.isEmpty()) return new Metric("OPPORTUNITY_SCORE", null, base.coverage(), missing);
        double penalties = leverageRisk * 0.15
                + safePenalty(conflictPenalty)
                + safePenalty(stalePenalty);
        return new Metric("OPPORTUNITY_SCORE", integer(base.value() - penalties), 1.0, List.of());
    }

    public static Metric riskScore(List<ScoreItemVO> scores,
                                   Integer eventRisk,
                                   Integer conflictAndExecutionRisk) {
        Map<String, Double> byType = scoreMap(scores);
        Double leverage = byType.get(LEVERAGE_RISK);
        Double liquidityQuality = byType.get(LIQUIDITY_QUALITY);
        return weighted("RISK_SCORE", Map.of(
                LEVERAGE_RISK, input(leverage, 0.35),
                "LiquidityRisk", input(liquidityQuality == null ? null : 100.0 - liquidityQuality, 0.25),
                "EventRisk", input(number(eventRisk), 0.20),
                "ConflictAndExecutionRisk", input(number(conflictAndExecutionRisk), 0.20)));
    }

    public static Metric finalConfidence(Integer dataQuality,
                                         Integer multiTimeframeConsistency,
                                         Integer evidenceCoverage,
                                         Integer crossSourceConsistency,
                                         Integer conflictPenalty) {
        Metric base = weighted("FINAL_CONFIDENCE", Map.of(
                "DataQuality", input(number(dataQuality), 0.35),
                "MultiTimeframeConsistency", input(number(multiTimeframeConsistency), 0.30),
                "EvidenceCoverage", input(number(evidenceCoverage), 0.20),
                "CrossSourceConsistency", input(number(crossSourceConsistency), 0.15)));
        if (base.value() == null) return base;
        return new Metric("FINAL_CONFIDENCE",
                integer(base.value() - safePenalty(conflictPenalty)), 1.0, List.of());
    }

    public static Map<String, Double> scoreMap(List<ScoreItemVO> scores) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (scores == null) return result;
        for (ScoreItemVO score : scores) {
            if (score == null || score.getScoreType() == null || score.getScoreValue() == null) continue;
            result.put(canonicalScoreType(score.getScoreType()), score.getScoreValue());
        }
        return result;
    }

    public static String canonicalScoreType(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return "综合可信度分".equals(normalized) ? EVIDENCE_RELIABILITY : normalized;
    }

    public static int evidenceCoverage(List<?> evidence) {
        if (evidence == null || evidence.isEmpty()) return 0;
        return Math.min(100, evidence.size() * 20);
    }

    public static int scoreCoverage(List<ScoreItemVO> scores) {
        int present = scoreMap(scores).size();
        return (int) Math.round(present * 100.0 / 8.0);
    }

    /**
     * A null score is valid only when the score row explicitly records why the
     * value is unavailable. This keeps all eight frozen score identities in the
     * audit chain without substituting a neutral value.
     */
    public static boolean scoreItemContractComplete(ScoreItemVO score) {
        if (score == null || !hasText(score.getScoreId()) || !hasText(score.getScoreType())
                || score.getWeight() == null || !Double.isFinite(score.getWeight())
                || score.getWeight() <= 0.0) {
            return false;
        }
        if (score.getScoreValue() != null) {
            return Double.isFinite(score.getScoreValue())
                    && score.getScoreValue() >= 0.0 && score.getScoreValue() <= 100.0;
        }
        String description = score.getDescription();
        if (!hasText(description) || !description.contains("coverage=")
                || !description.contains("permission=INSUFFICIENT_DATA")) {
            return false;
        }
        int start = description.indexOf("missingInputs=[");
        if (start < 0) return false;
        int valueStart = start + "missingInputs=[".length();
        int end = description.indexOf(']', valueStart);
        return end > valueStart && !description.substring(valueStart, end).isBlank();
    }

    /**
     * Fresh or stale evidence must carry the observed value contract. An
     * explicitly unavailable optional source may keep value/time fields null,
     * but it must retain identity, lineage, data state and an explanatory reason.
     */
    public static boolean evidenceItemContractComplete(EvidenceItemVO evidence, String analysisId) {
        if (evidence == null || !hasText(evidence.getEvidenceId())
                || !hasText(evidence.getEvidenceType()) || !hasText(evidence.getSource())
                || !hasText(evidence.getAnalysisId()) || !same(evidence.getAnalysisId(), analysisId)
                || (!hasText(evidence.getSourceReference()) && !hasText(evidence.getSourceTraceId()))
                || !hasText(evidence.getFreshness())) {
            return false;
        }
        String freshness = evidence.getFreshness().trim().toUpperCase(Locale.ROOT);
        if ("FRESH".equals(freshness) || "STALE".equals(freshness)) {
            return bounded(evidence.getStrength()) && bounded(evidence.getConfidence())
                    && hasText(evidence.getCurrentValue())
                    && hasText(evidence.getChangeFromBaseline())
                    && evidence.getObservedAt() != null;
        }
        return EXPLICIT_NO_VALUE_EVIDENCE_STATES.contains(freshness)
                && hasText(evidence.getDescription());
    }

    private static Metric weighted(String name, Map<String, WeightedInput> inputs) {
        List<String> missing = new ArrayList<>();
        double value = 0.0;
        double coveredWeight = 0.0;
        for (Map.Entry<String, WeightedInput> entry : inputs.entrySet()) {
            WeightedInput item = entry.getValue();
            if (item.value() == null) {
                missing.add(entry.getKey());
                continue;
            }
            value += clamp(item.value()) * item.weight();
            coveredWeight += item.weight();
        }
        double totalWeight = inputs.values().stream().mapToDouble(WeightedInput::weight).sum();
        double coverage = totalWeight == 0.0 ? 0.0 : coveredWeight / totalWeight;
        return missing.isEmpty()
                ? new Metric(name, integer(value), coverage, List.of())
                : new Metric(name, null, coverage, List.copyOf(missing));
    }

    private static WeightedInput input(Double value, double weight) {
        return new WeightedInput(value, weight);
    }

    private static Double number(Integer value) {
        return value == null ? null : value.doubleValue();
    }

    private static int integer(double value) {
        return (int) Math.round(clamp(value));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private static boolean bounded(Double value) {
        return value != null && Double.isFinite(value) && value >= 0.0 && value <= 100.0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean same(String left, String right) {
        return hasText(left) && hasText(right) && left.trim().equals(right.trim());
    }

    private static int safePenalty(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private record WeightedInput(Double value, double weight) {
    }

    public record Metric(String name, Integer value, double coverage, List<String> missingInputs) {
        public boolean complete() {
            return value != null && missingInputs.isEmpty();
        }

        public String permissionState() {
            return complete() ? "AVAILABLE" : "INSUFFICIENT_DATA";
        }
    }
}
