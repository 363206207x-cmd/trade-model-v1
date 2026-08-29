package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.PlanModeEnum;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.service.OpportunityPriorityRankingService;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.HomeTopAssetProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OpportunityPriorityRankingServiceImpl implements OpportunityPriorityRankingService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> INACTIVE_WATCH_STATES = Set.of("REMOVED", "DISABLED", "INACTIVE");

    private final AssetPoolService assetPoolService;
    private final DecisionResultMapper decisionResultMapper;
    private final AssetStateMapper assetStateMapper;
    private final FundamentalAiV41Properties properties;
    private final Clock clock;
    private AnalysisRunMapper analysisRunMapper;

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

    @Autowired(required = false)
    void setAnalysisRunMapper(AnalysisRunMapper analysisRunMapper) {
        this.analysisRunMapper = analysisRunMapper;
    }

    @Override
    public List<HomeTopAssetProjection> rankForHome(Long userId, int limit) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        int effectiveLimit = Math.max(1, Math.min(properties.getRanking().getHomeCapacity(), limit));
        Map<String, AssetPoolAssetDTO> poolBySymbol = effectivePool(assetPoolService.listForUser(userId), userId);
        if (poolBySymbol.isEmpty()) {
            return List.of();
        }

        List<String> symbols = List.copyOf(poolBySymbol.keySet());
        List<DecisionResultVO> decisions = safe(decisionResultMapper
                .findLatestDecisionResultsForSymbolsJoined(symbols, "USER", userId));
        List<AssetStateDO> states = safe(assetStateMapper.listByOwnerAndSymbols(symbols, "USER", userId));
        LocalDateTime now = LocalDateTime.now(clock);

        List<HomeTopAssetProjection> opportunities = decisions.stream()
                .filter(Objects::nonNull)
                .map(decision -> opportunityProjection(poolBySymbol, states, decision, userId, now))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(HomeTopAssetProjection::symbol,
                        LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::aggregateTimeframes)
                .toList();

        List<HomeTopAssetProjection> combined = new ArrayList<>(opportunities);
        Set<Long> opportunityAssetIds = opportunities.stream()
                .map(HomeTopAssetProjection::assetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> opportunitySymbols = opportunities.stream()
                .map(HomeTopAssetProjection::symbol)
                .map(OpportunityPriorityRankingServiceImpl::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (AssetPoolAssetDTO asset : poolBySymbol.values()) {
            String symbol = normalize(asset.symbol());
            if (opportunityAssetIds.contains(asset.assetId()) || opportunitySymbols.contains(symbol)) {
                continue;
            }
            combined.add(observationProjection(asset, states, userId, now));
        }

        Set<Long> usedAssetIds = new LinkedHashSet<>();
        Set<String> usedSymbols = new LinkedHashSet<>();
        return combined.stream()
                .filter(Objects::nonNull)
                .sorted(homeOrder())
                .filter(row -> row.assetId() != null && usedAssetIds.add(row.assetId()))
                .filter(row -> normalize(row.symbol()) != null && usedSymbols.add(normalize(row.symbol())))
                .limit(effectiveLimit)
                .toList();
    }

    private static Map<String, AssetPoolAssetDTO> effectivePool(List<AssetPoolAssetDTO> pool, Long userId) {
        Map<String, AssetPoolAssetDTO> bySymbol = new LinkedHashMap<>();
        for (AssetPoolAssetDTO asset : safe(pool)) {
            String symbol = normalize(asset == null ? null : asset.symbol());
            String watchStatus = upper(asset == null ? null : asset.watchStatus());
            if (asset != null
                    && userId.equals(asset.userId())
                    && asset.assetId() != null
                    && symbol != null
                    && (watchStatus == null || !INACTIVE_WATCH_STATES.contains(watchStatus))) {
                bySymbol.putIfAbsent(symbol, asset);
            }
        }
        return bySymbol;
    }

    private HomeTopAssetProjection opportunityProjection(Map<String, AssetPoolAssetDTO> poolBySymbol,
                                                          List<AssetStateDO> states,
                                                          DecisionResultVO decision,
                                                          Long userId,
                                                          LocalDateTime now) {
        String symbol = normalize(decision.getSymbol());
        String analysisId = text(decision.getAnalysisId());
        String timeframe = normalizeTimeframe(decision.getTimeframe());
        AssetPoolAssetDTO asset = symbol == null ? null : poolBySymbol.get(symbol);
        if (asset == null || analysisId == null || timeframe == null) {
            return null;
        }
        AnalysisRunDO run = formalAssetPoolRun(analysisId, asset, userId, timeframe);
        if (run == null) {
            return null;
        }
        AssetStateDO opportunity = states.stream()
                .filter(state -> exactUserState(state, asset, userId))
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
        LocalDateTime completedAt = latestRunTime(run);
        String opportunityState = opportunity.getState().name();
        if (!completeOpportunityInput(opportunityScore, finalMarketBias, confidence, riskLevel,
                planMode, aiDecisionResult, dataQuality, completedAt)) {
            return null;
        }

        SchedulerScanAudit audit = schedulerScanAudit(opportunity.getExtJson());
        LocalDateTime latestScanAt = audit.present() ? audit.finishedAt() : completedAt;
        long ageSeconds = ageSeconds(latestScanAt, now);
        String auditFreshness = upper(audit.dataFreshness());
        String freshness;
        if (audit.present() && auditFreshness != null && auditFreshness.contains("CONFLICT")) {
            freshness = "TIMEFRAME_CONFLICT";
        } else if (audit.present() && (auditFreshness == null || !auditFreshness.startsWith("FRESH"))) {
            freshness = "STALE";
        } else {
            freshness = ageSeconds > properties.getRanking().getFreshnessWindowSeconds()
                    ? "STALE" : "FRESH";
        }
        long stabilitySeconds = opportunity.getStateEnteredAt() == null
                ? 0L : ageSeconds(opportunity.getStateEnteredAt(), now);
        String rankingReason = "HOME_STATE=" + opportunityState
                + "|DATA_STATUS=" + freshness
                + "|LATEST_FORMAL_SCAN_TIME=" + latestScanAt
                + "|SOURCE=ASSET_POOL_SCAN";

        return new HomeTopAssetProjection(
                asset.assetId(), symbol, asset.displayName(), opportunityScore, finalMarketBias,
                confidence, riskLevel, planMode, aiDecisionResult, dataQuality, freshness,
                ageSeconds, stabilitySeconds, 0, rankingReason, analysisId,
                opportunity.getOpportunityId(), opportunityState, opportunity.getOpportunityId(),
                timeframe, planMode, 0,
                "TIMEFRAME_CONFLICT".equals(freshness) ? "TIMEFRAME_CONFLICT" : "ALIGNED",
                latestScanAt, decision);
    }

    private HomeTopAssetProjection observationProjection(AssetPoolAssetDTO asset,
                                                          List<AssetStateDO> states,
                                                          Long userId,
                                                          LocalDateTime now) {
        List<ObservationState> observations = states.stream()
                .filter(state -> exactUserState(state, asset, userId))
                .map(state -> observationState(state, asset, userId, now))
                .sorted(observationStateOrder())
                .toList();
        ObservationState primary = observations.stream().findFirst().orElse(ObservationState.neverScanned());
        String analysisId = primary.formalRun() == null ? null : primary.formalRun().getAnalysisId();
        String state = observationDisplayState(primary);
        String assetState = primary.state() == null || primary.state().getState() == null
                ? "MISSING" : primary.state().getState().name();
        String rankingReason = "SLOT_TYPE=OBSERVATION"
                + "|OBSERVATION_STATE=" + state
                + "|DATA_STATUS=" + primary.dataStatus()
                + "|LATEST_FORMAL_SCAN_TIME=" + value(primary.finishedAt())
                + "|ASSET_STATE=" + assetState;
        String conflictState = "TIMEFRAME_CONFLICT".equals(primary.dataStatus())
                ? "TIMEFRAME_CONFLICT" : "ALIGNED";
        return new HomeTopAssetProjection(
                asset.assetId(), normalize(asset.symbol()), asset.displayName(), null, null, null,
                null, null, null, null, primary.dataStatus(), primary.ageSeconds(), 0L, 0,
                rankingReason, analysisId, null, state, null, primary.timeframe(), null, 0,
                conflictState, primary.finishedAt(), null);
    }

    private ObservationState observationState(AssetStateDO state,
                                              AssetPoolAssetDTO asset,
                                              Long userId,
                                              LocalDateTime now) {
        String timeframe = normalizeTimeframe(state.getTimeframe());
        AnalysisRunDO run = formalAssetPoolRun(text(state.getLastAnalysisId()), asset, userId, timeframe);
        if (run == null) {
            return new ObservationState(state, null, "NEVER_SCANNED", null, Long.MAX_VALUE,
                    timeframe, false, null);
        }
        SchedulerScanAudit audit = schedulerScanAudit(state.getExtJson());
        if (!audit.present()) {
            LocalDateTime completedAt = latestRunTime(run);
            if (completedAt != null) {
                return new ObservationState(state, run, "STALE", completedAt,
                        ageSeconds(completedAt, now), timeframe, false, null);
            }
            return new ObservationState(state, null, "NEVER_SCANNED", null, Long.MAX_VALUE,
                    timeframe, false, null);
        }
        long age = ageSeconds(audit.finishedAt(), now);
        String dataStatus;
        String rawFreshness = upper(audit.dataFreshness());
        if (rawFreshness != null && rawFreshness.contains("CONFLICT")) {
            dataStatus = "TIMEFRAME_CONFLICT";
        } else if (rawFreshness == null || !rawFreshness.startsWith("FRESH")
                || age > properties.getRanking().getFreshnessWindowSeconds()) {
            dataStatus = "STALE";
        } else {
            dataStatus = "FRESH";
        }
        return new ObservationState(state, run, dataStatus, audit.finishedAt(), age,
                timeframe, audit.fullAnalysisSucceeded(), audit.result());
    }

    private HomeTopAssetProjection aggregateTimeframes(List<HomeTopAssetProjection> rows) {
        List<HomeTopAssetProjection> ordered = safe(rows).stream()
                .filter(Objects::nonNull)
                .sorted(homeOrder())
                .toList();
        HomeTopAssetProjection primary = ordered.get(0);
        String conflictState = ordered.stream().anyMatch(
                row -> "TIMEFRAME_CONFLICT".equals(upper(row.freshness())))
                ? "TIMEFRAME_CONFLICT" : timeframeConflictState(ordered);
        String freshness = "OPPOSING".equals(conflictState)
                || "TIMEFRAME_CONFLICT".equals(conflictState)
                ? "TIMEFRAME_CONFLICT" : primary.freshness();
        String reason = primary.rankingReason()
                + "|PRIMARY_TIMEFRAME=" + value(primary.primaryTimeframe())
                + "|SECONDARY_OPPORTUNITY_COUNT=" + Math.max(0, ordered.size() - 1)
                + "|TIMEFRAME_CONFLICT_STATE=" + conflictState;
        return new HomeTopAssetProjection(
                primary.assetId(), primary.symbol(), primary.name(), primary.opportunityScore(),
                primary.finalMarketBias(), primary.confidence(), primary.riskLevel(),
                primary.finalPlanMode(), primary.aiDecisionResult(), primary.dataQuality(),
                freshness, primary.freshnessAgeSeconds(), primary.stabilitySeconds(), 0, reason,
                primary.analysisId(), primary.opportunityId(), primary.opportunityState(),
                primary.opportunityId(), primary.primaryTimeframe(), primary.finalPlanMode(),
                Math.max(0, ordered.size() - 1), conflictState, primary.analysisTime(),
                primary.sourceDecision());
    }

    private String timeframeConflictState(List<HomeTopAssetProjection> rows) {
        boolean bullish = rows.stream().map(HomeTopAssetProjection::finalMarketBias)
                .map(this::directionFamily).anyMatch("BULLISH"::equals);
        boolean bearish = rows.stream().map(HomeTopAssetProjection::finalMarketBias)
                .map(this::directionFamily).anyMatch("BEARISH"::equals);
        if (bullish && bearish) return "OPPOSING";
        long directionalFamilies = rows.stream().map(HomeTopAssetProjection::finalMarketBias)
                .map(this::directionFamily).filter(value -> !"NEUTRAL".equals(value)).distinct().count();
        return directionalFamilies == 1 ? "ALIGNED" : "MIXED_NEUTRAL";
    }

    private String directionFamily(String value) {
        String normalized = upperOrEmpty(value);
        if (normalized.endsWith("BULLISH")) return "BULLISH";
        if (normalized.endsWith("BEARISH")) return "BEARISH";
        return "NEUTRAL";
    }

    private AnalysisRunDO formalAssetPoolRun(String analysisId,
                                             AssetPoolAssetDTO asset,
                                             Long userId,
                                             String expectedTimeframe) {
        if (analysisRunMapper == null || analysisId == null || asset == null) {
            return null;
        }
        try {
            AnalysisRunDO run = analysisRunMapper.selectById(analysisId);
            if (run == null
                    || Boolean.TRUE.equals(run.getPreview())
                    || !"SUCCESS".equals(upper(run.getStatus()))
                    || AnalysisRunTriggerType.normalize(run.getTriggerType()) != AnalysisRunTriggerType.ASSET_POOL_SCAN
                    || "ANALYSIS_PREVIEW".equals(upper(run.getAnalysisMode()))
                    || !"USER".equals(upper(run.getOwnerType()))
                    || !userId.equals(run.getOwnerId())
                    || !asset.assetId().equals(run.getAssetId())
                    || !normalize(asset.symbol()).equals(normalize(run.getSymbol()))
                    || expectedTimeframe == null
                    || !expectedTimeframe.equals(normalizeTimeframe(run.getTimeframe()))) {
                return null;
            }
            return run;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean exactUserState(AssetStateDO state, AssetPoolAssetDTO asset, Long userId) {
        return state != null
                && asset != null
                && "USER".equals(upper(state.getOwnerType()))
                && userId.equals(state.getOwnerId())
                && asset.assetId().equals(state.getAssetId())
                && normalize(asset.symbol()).equals(normalize(state.getSymbol()));
    }

    private static Comparator<HomeTopAssetProjection> homeOrder() {
        return Comparator.comparingInt((HomeTopAssetProjection row) -> stateRank(row.opportunityState()))
                .thenComparingInt(row -> dataStatusRank(row.freshness()))
                .thenComparing(HomeTopAssetProjection::analysisTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(HomeTopAssetProjection::symbol);
    }

    private static Comparator<ObservationState> observationStateOrder() {
        return Comparator.comparingInt((ObservationState row) -> dataStatusRank(row.dataStatus()))
                .thenComparing(ObservationState::finishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ObservationState::timeframe,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static int stateRank(String state) {
        return switch (upperOrEmpty(state)) {
            case "TRIGGERED" -> 0;
            case "WAITING_TRIGGER" -> 1;
            case "CANDIDATE" -> 2;
            case "HIGH_RISK" -> 3;
            default -> 4;
        };
    }

    private static int dataStatusRank(String value) {
        return switch (upperOrEmpty(value)) {
            case "FRESH", "READY" -> 0;
            case "TIMEFRAME_CONFLICT" -> 1;
            case "STALE" -> 2;
            default -> 3;
        };
    }

    private String observationDisplayState(ObservationState observation) {
        if ("NEVER_SCANNED".equals(observation.dataStatus())) return "NEVER_SCANNED";
        if ("STALE".equals(observation.dataStatus())) return "STALE";
        if (observation.formalRun() != null
                && (observation.fullAnalysisSucceeded()
                || "WAIT".equals(upper(observation.result())))) {
            return "NO_QUALIFIED_OPPORTUNITY";
        }
        return "OBSERVING";
    }

    private static SchedulerScanAudit schedulerScanAudit(String json) {
        if (text(json) == null) return SchedulerScanAudit.missing();
        try {
            JsonNode scan = JSON.readTree(json).path("schedulerScan");
            if (!scan.isObject()) return SchedulerScanAudit.missing();
            LocalDateTime finishedAt = parseTime(scan.path("finishedAt"));
            if (finishedAt == null) return SchedulerScanAudit.missing();
            return new SchedulerScanAudit(true, nodeText(scan.path("result")),
                    nodeText(scan.path("dataFreshness")), finishedAt,
                    scan.path("fullAnalysisSucceeded").asBoolean(false));
        } catch (Exception ignored) {
            return SchedulerScanAudit.missing();
        }
    }

    private static LocalDateTime parseTime(JsonNode value) {
        String raw = value != null && value.isTextual() ? text(value.textValue()) : null;
        if (raw == null) return null;
        try {
            return LocalDateTime.parse(raw);
        } catch (RuntimeException ignored) {
            try {
                return LocalDateTime.ofInstant(Instant.parse(raw), ZoneOffset.UTC);
            } catch (RuntimeException invalid) {
                return null;
            }
        }
    }

    private static String nodeText(JsonNode value) {
        return value != null && value.isTextual() ? text(value.textValue()) : null;
    }

    private static LocalDateTime latestRunTime(AnalysisRunDO run) {
        if (run == null) return null;
        if (run.getCompletedAt() != null) return run.getCompletedAt();
        return run.getAnalysisTime();
    }

    private static long ageSeconds(LocalDateTime value, LocalDateTime now) {
        if (value == null || now == null) return Long.MAX_VALUE;
        return Math.max(0L, Duration.between(value, now).getSeconds());
    }

    private static boolean completeOpportunityInput(Integer opportunityScore,
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
        return state == AssetStateEnum.TRIGGERED
                || state == AssetStateEnum.WAITING_TRIGGER
                || state == AssetStateEnum.CANDIDATE
                || state == AssetStateEnum.HIGH_RISK;
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

    private record SchedulerScanAudit(boolean present,
                                      String result,
                                      String dataFreshness,
                                      LocalDateTime finishedAt,
                                      boolean fullAnalysisSucceeded) {
        private static SchedulerScanAudit missing() {
            return new SchedulerScanAudit(false, null, null, null, false);
        }
    }

    private record ObservationState(AssetStateDO state,
                                    AnalysisRunDO formalRun,
                                    String dataStatus,
                                    LocalDateTime finishedAt,
                                    long ageSeconds,
                                    String timeframe,
                                    boolean fullAnalysisSucceeded,
                                    String result) {
        private static ObservationState neverScanned() {
            return new ObservationState(null, null, "NEVER_SCANNED", null,
                    Long.MAX_VALUE, null, false, null);
        }
    }
}
