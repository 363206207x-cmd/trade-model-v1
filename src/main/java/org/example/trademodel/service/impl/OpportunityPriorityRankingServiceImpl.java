package org.example.trademodel.service.impl;

import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.service.OpportunityPriorityRankingService;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.HomeTopAssetProjection;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class OpportunityPriorityRankingServiceImpl implements OpportunityPriorityRankingService {
    static final int HOME_TOP_ASSET_LIMIT = 6;

    private final AssetPoolService assetPoolService;
    private final DecisionResultMapper decisionResultMapper;
    private final AssetStateMapper assetStateMapper;

    public OpportunityPriorityRankingServiceImpl(AssetPoolService assetPoolService,
                                                 DecisionResultMapper decisionResultMapper,
                                                 AssetStateMapper assetStateMapper) {
        this.assetPoolService = assetPoolService;
        this.decisionResultMapper = decisionResultMapper;
        this.assetStateMapper = assetStateMapper;
    }

    @Override
    public List<HomeTopAssetProjection> rankForHome(Long userId, int limit) {
        int effectiveLimit = Math.max(1, Math.min(HOME_TOP_ASSET_LIMIT, limit));
        List<AssetPoolAssetDTO> pool = userId == null
                ? assetPoolService.listSystemDefaults()
                : assetPoolService.listForUser(userId);
        Map<String, AssetPoolAssetDTO> poolBySymbol = effectivePool(pool);
        if (poolBySymbol.isEmpty()) {
            return List.of();
        }

        List<String> symbols = List.copyOf(poolBySymbol.keySet());
        List<DecisionResultVO> decisions = safe(decisionResultMapper
                .findLatestDecisionResultsForSymbolsJoined(symbols));
        List<AssetStateDO> states = safe(assetStateMapper.listBySymbols(symbols));

        return decisions.stream()
                .filter(Objects::nonNull)
                .map(decision -> projection(poolBySymbol, states, decision))
                .filter(Objects::nonNull)
                .sorted(priorityOrder())
                .limit(effectiveLimit)
                .toList();
    }

    private static Map<String, AssetPoolAssetDTO> effectivePool(List<AssetPoolAssetDTO> pool) {
        Map<String, AssetPoolAssetDTO> bySymbol = new LinkedHashMap<>();
        for (AssetPoolAssetDTO asset : safe(pool)) {
            String symbol = normalize(asset == null ? null : asset.symbol());
            if (asset != null && asset.assetId() != null && symbol != null) {
                bySymbol.putIfAbsent(symbol, asset);
            }
        }
        return bySymbol;
    }

    private static HomeTopAssetProjection projection(Map<String, AssetPoolAssetDTO> poolBySymbol,
                                                      List<AssetStateDO> states,
                                                      DecisionResultVO decision) {
        String symbol = normalize(decision.getSymbol());
        String analysisId = text(decision.getAnalysisId());
        String timeframe = normalizeTimeframe(decision.getTimeframe());
        AssetPoolAssetDTO asset = symbol == null ? null : poolBySymbol.get(symbol);
        if (asset == null || analysisId == null || timeframe == null) {
            return null;
        }
        AssetStateDO opportunity = states.stream()
                .filter(Objects::nonNull)
                .filter(state -> symbol.equals(normalize(state.getSymbol())))
                .filter(state -> timeframe.equals(normalizeTimeframe(state.getTimeframe())))
                .filter(state -> analysisId.equals(text(state.getLastAnalysisId())))
                .filter(state -> text(state.getOpportunityId()) != null)
                .findFirst()
                .orElse(null);
        if (opportunity == null) {
            return null;
        }

        Integer opportunityScore = decision.getOpportunityScore() == null
                ? null : (int) Math.round(decision.getOpportunityScore());
        String confidence = upper(decision.getConfidenceLevel());
        String riskLevel = upper(decision.getRiskLevel());
        String planMode = upper(decision.getPlanMode());
        String aiDecisionResult = upper(decision.getAiConflictLevel());
        Integer dataQuality = decision.getDataQualityScore();
        String opportunityState = opportunity.getState() == null ? null : opportunity.getState().name();
        String rankingReason = rankingReason(opportunityScore, confidence, riskLevel, planMode,
                aiDecisionResult, dataQuality);

        return new HomeTopAssetProjection(
                asset.assetId(),
                symbol,
                opportunityScore,
                confidence,
                riskLevel,
                planMode,
                aiDecisionResult,
                dataQuality,
                rankingReason,
                analysisId,
                opportunity.getOpportunityId(),
                opportunityState,
                decision);
    }

    private static Comparator<HomeTopAssetProjection> priorityOrder() {
        return Comparator
                .comparingInt((HomeTopAssetProjection value) -> planModeRank(value.planMode())).reversed()
                .thenComparing(Comparator.comparingInt(
                        (HomeTopAssetProjection value) -> value.opportunityScore() == null
                                ? Integer.MIN_VALUE : value.opportunityScore()).reversed())
                .thenComparing(Comparator.comparingInt(
                        (HomeTopAssetProjection value) -> confidenceRank(value.confidence())).reversed())
                .thenComparing(Comparator.comparingInt(
                        (HomeTopAssetProjection value) -> aiDecisionRank(value.aiDecisionResult())).reversed())
                .thenComparing(Comparator.comparingInt(
                        (HomeTopAssetProjection value) -> value.dataQuality() == null
                                ? Integer.MIN_VALUE : value.dataQuality()).reversed())
                .thenComparing(Comparator.comparingInt(
                        (HomeTopAssetProjection value) -> riskQualityRank(value.riskLevel())).reversed())
                .thenComparing(HomeTopAssetProjection::sourceDecision,
                        Comparator.comparing(DecisionResultVO::getCreateTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .thenComparing(HomeTopAssetProjection::symbol);
    }

    private static int planModeRank(String value) {
        return switch (upperOrEmpty(value)) {
            case "CONFIRM" -> 5;
            case "PREPARE" -> 4;
            case "REDUCE" -> 3;
            case "WATCH" -> 2;
            case "BLOCKED" -> 1;
            default -> 0;
        };
    }

    private static int confidenceRank(String value) {
        return switch (upperOrEmpty(value)) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private static int riskQualityRank(String value) {
        return switch (upperOrEmpty(value)) {
            case "LOW" -> 4;
            case "MEDIUM" -> 3;
            case "HIGH" -> 2;
            case "EXTREME" -> 1;
            default -> 0;
        };
    }

    private static int aiDecisionRank(String value) {
        return switch (upperOrEmpty(value)) {
            case "LEVEL_1_CONSISTENT" -> 4;
            case "LEVEL_2_MINOR_DISAGREEMENT" -> 3;
            case "LEVEL_3_SIGNIFICANT_DISAGREEMENT" -> 2;
            case "LEVEL_4_EXTREME_CONFLICT" -> 1;
            default -> 0;
        };
    }

    private static String rankingReason(Integer opportunityScore,
                                        String confidence,
                                        String riskLevel,
                                        String planMode,
                                        String aiDecisionResult,
                                        Integer dataQuality) {
        return "OPPORTUNITY_SCORE=" + value(opportunityScore)
                + "|CONFIDENCE=" + value(confidence)
                + "|RISK_LEVEL=" + value(riskLevel)
                + "|PLAN_MODE=" + value(planMode)
                + "|AI_DECISION=" + value(aiDecisionResult)
                + "|DATA_QUALITY=" + value(dataQuality);
    }

    private static String value(Object value) {
        return value == null || value.toString().isBlank() ? "MISSING" : value.toString();
    }

    private static String upper(String value) {
        String text = text(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }

    private static String upperOrEmpty(String value) {
        String normalized = upper(value);
        return normalized == null ? "" : normalized;
    }

    private static String normalize(String value) {
        String text = text(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT)
                .replace("/", "").replace("-", "").replace("_", "");
    }

    private static String normalizeTimeframe(String value) {
        String text = text(value);
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
