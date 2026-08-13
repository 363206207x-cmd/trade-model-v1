package org.example.trademodel.service.impl;

import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.PlanModeEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.service.OpportunityPriorityRankingService;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.HomeTopAssetProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class OpportunityPriorityRankingServiceImpl implements OpportunityPriorityRankingService {
    private final AssetPoolService assetPoolService;
    private final DecisionResultMapper decisionResultMapper;
    private final AssetStateMapper assetStateMapper;
    private final FundamentalAiV41Properties properties;
    private final Clock clock;

    public OpportunityPriorityRankingServiceImpl(AssetPoolService assetPoolService,
                                                 DecisionResultMapper decisionResultMapper,
                                                 AssetStateMapper assetStateMapper) {
        this(assetPoolService, decisionResultMapper, assetStateMapper,
                FundamentalAiV41Properties.contractFixture(), Clock.systemUTC());
    }

    @Autowired
    public OpportunityPriorityRankingServiceImpl(AssetPoolService assetPoolService,
                                                 DecisionResultMapper decisionResultMapper,
                                                 AssetStateMapper assetStateMapper,
                                                 FundamentalAiV41Properties properties,
                                                 Clock analysisRunClock) {
        this.assetPoolService = assetPoolService;
        this.decisionResultMapper = decisionResultMapper;
        this.assetStateMapper = assetStateMapper;
        this.properties = Objects.requireNonNull(properties, "v4.1 properties");
        this.clock = analysisRunClock == null ? Clock.systemUTC() : analysisRunClock;
    }

    @Override
    public List<HomeTopAssetProjection> rankForHome(Long userId, int limit) {
        int effectiveLimit = Math.max(1, Math.min(properties.getRanking().getHomeCapacity(), limit));
        List<AssetPoolAssetDTO> pool = userId == null
                ? assetPoolService.listSystemDefaults()
                : assetPoolService.listForUser(userId);
        Map<String, AssetPoolAssetDTO> poolBySymbol = effectivePool(pool);
        if (poolBySymbol.isEmpty()) {
            return List.of();
        }

        List<String> symbols = List.copyOf(poolBySymbol.keySet());
        String ownerType = userId == null ? "SYSTEM" : "USER";
        Long ownerId = userId == null ? 0L : userId;
        List<DecisionResultVO> decisions = safe(decisionResultMapper
                .findLatestDecisionResultsForSymbolsJoined(symbols, ownerType, ownerId));
        List<AssetStateDO> states = safe(assetStateMapper.listByOwnerAndSymbols(symbols, ownerType, ownerId));

        return decisions.stream()
                .filter(Objects::nonNull)
                .map(decision -> projection(poolBySymbol, states, decision, LocalDateTime.now(clock)))
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

    private HomeTopAssetProjection projection(Map<String, AssetPoolAssetDTO> poolBySymbol,
                                              List<AssetStateDO> states,
                                              DecisionResultVO decision,
                                              LocalDateTime now) {
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
        if (opportunity == null || !eligibleState(opportunity.getState())) {
            return null;
        }

        Integer opportunityScore = opportunity.getOpportunityScore();
        String finalMarketBias = upper(decision.getFinalMarketBias());
        String confidence = upper(opportunity.getConfidence());
        String riskLevel = upper(opportunity.getRisk());
        String planMode = upper(decision.getPlanMode());
        String aiDecisionResult = upper(decision.getAiConflictLevel());
        Integer dataQuality = decision.getDataQualityScore();
        LocalDateTime analysisTime = decision.getAnalysisTime();
        String opportunityState = opportunity.getState() == null ? null : opportunity.getState().name();
        if (!completeRankingInput(opportunityScore, finalMarketBias, confidence, riskLevel, planMode,
                aiDecisionResult, dataQuality, analysisTime)) {
            return null;
        }
        if (PlanModeEnum.BLOCKED.name().equals(planMode)
                || dataQuality < properties.getRanking().getMinimumDataQuality()) {
            return null;
        }
        long ageSeconds = Math.max(0, Duration.between(analysisTime, now).getSeconds());
        if (ageSeconds > properties.getRanking().getFreshnessWindowSeconds()) {
            return null;
        }
        long stabilitySeconds = opportunity.getStateEnteredAt() == null
                ? 0L : Math.max(0, Duration.between(opportunity.getStateEnteredAt(), now).getSeconds());
        int priorityScore = priorityScore(opportunityScore, confidence, riskLevel, planMode,
                aiDecisionResult, dataQuality, ageSeconds, stabilitySeconds);
        String rankingReason = rankingReason(opportunityScore, confidence, riskLevel, planMode,
                aiDecisionResult, dataQuality, finalMarketBias, ageSeconds, stabilitySeconds, priorityScore);

        return new HomeTopAssetProjection(
                asset.assetId(),
                symbol,
                asset.displayName(),
                opportunityScore,
                finalMarketBias,
                confidence,
                riskLevel,
                planMode,
                aiDecisionResult,
                dataQuality,
                "FRESH",
                ageSeconds,
                stabilitySeconds,
                priorityScore,
                rankingReason,
                analysisId,
                opportunity.getOpportunityId(),
                opportunityState,
                analysisTime,
                decision);
    }

    private static Comparator<HomeTopAssetProjection> priorityOrder() {
        return Comparator
                .comparingInt(HomeTopAssetProjection::priorityScore).reversed()
                .thenComparing(HomeTopAssetProjection::freshnessAgeSeconds,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HomeTopAssetProjection::analysisTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Comparator.comparingLong(HomeTopAssetProjection::stabilitySeconds).reversed())
                .thenComparing(HomeTopAssetProjection::symbol);
    }

    private static int planModeRank(String value) {
        return switch (upperOrEmpty(value)) {
            case "CONFIRMATION" -> 5;
            case "PREPARATION" -> 4;
            case "REDUCED" -> 3;
            case "OBSERVATION" -> 2;
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

    private int priorityScore(Integer opportunityScore,
                              String confidence,
                              String riskLevel,
                              String planMode,
                              String conflict,
                              Integer dataQuality,
                              long freshnessAgeSeconds,
                              long stabilitySeconds) {
        FundamentalAiV41Properties.Ranking cfg = properties.getRanking();
        BigDecimal freshness = BigDecimal.valueOf(Math.max(0D,
                100D * (1D - (double) freshnessAgeSeconds / cfg.getFreshnessWindowSeconds())));
        BigDecimal stability = BigDecimal.valueOf(Math.min(100D,
                100D * stabilitySeconds / Math.max(1D, cfg.getFreshnessWindowSeconds())));
        BigDecimal total = weighted(opportunityScore, cfg.getOpportunityScoreWeight())
                .add(weighted(confidenceRank(confidence) * 100D / 3D, cfg.getConfidenceWeight()))
                .add(weighted(riskQualityRank(riskLevel) * 25D, cfg.getRiskWeight()))
                .add(weighted(planModeRank(planMode) * 20D, cfg.getPlanModeWeight()))
                .add(weighted(dataQuality, cfg.getDataQualityWeight()))
                .add(weighted(freshness, cfg.getFreshnessWeight()))
                .add(weighted(aiDecisionRank(conflict) * 25D, cfg.getConflictWeight()))
                .add(weighted(stability, cfg.getStabilityWeight()));
        return total.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static BigDecimal weighted(Number value, BigDecimal weight) {
        return BigDecimal.valueOf(value == null ? 0D : value.doubleValue()).multiply(weight);
    }

    private static String rankingReason(Integer opportunityScore,
                                        String confidence,
                                        String riskLevel,
                                        String planMode,
                                        String aiDecisionResult,
                                        Integer dataQuality,
                                        String finalMarketBias,
                                        long freshnessAgeSeconds,
                                        long stabilitySeconds,
                                        int priorityScore) {
        return "OPPORTUNITY_SCORE=" + value(opportunityScore)
                + "|FINAL_MARKET_BIAS=" + value(finalMarketBias)
                + "|CONFIDENCE=" + value(confidence)
                + "|RISK_LEVEL=" + value(riskLevel)
                + "|PLAN_MODE=" + value(planMode)
                + "|AI_DECISION=" + value(aiDecisionResult)
                + "|DATA_QUALITY=" + value(dataQuality)
                + "|FRESHNESS_AGE_SECONDS=" + freshnessAgeSeconds
                + "|STABILITY_SECONDS=" + stabilitySeconds
                + "|PRIORITY_SCORE=" + priorityScore;
    }

    private static boolean completeRankingInput(Integer opportunityScore,
                                                String finalMarketBias,
                                                String confidence,
                                                String risk,
                                                String planMode,
                                                String conflict,
                                                Integer dataQuality,
                                                LocalDateTime analysisTime) {
        try {
            org.example.trademodel.enums.MarketBiasEnum.valueOf(finalMarketBias);
            PlanModeEnum.require(planMode);
        } catch (RuntimeException invalid) {
            return false;
        }
        return opportunityScore != null && confidence != null && risk != null
                && conflict != null && dataQuality != null && analysisTime != null;
    }

    private static boolean eligibleState(AssetStateEnum state) {
        return state == AssetStateEnum.CANDIDATE
                || state == AssetStateEnum.WAITING_TRIGGER
                || state == AssetStateEnum.TRIGGERED;
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
