package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.PlanModeEnum;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.service.OpportunityPriorityRankingService;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.HomeTopAssetProjection;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private final ConcurrentMap<Long, List<String>> stableTierOneSymbols = new ConcurrentHashMap<>();
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
        List<DecisionResultVO> queriedDecisions = safe(decisionResultMapper
                .findLatestDecisionResultsForSymbolsJoined(symbols, "USER", userId));
        List<AssetStateDO> states = safe(assetStateMapper.listByOwnerAndSymbols(symbols, "USER", userId));
        List<DecisionResultVO> decisions = sourceOwnedDecisions(
                poolBySymbol, states, queriedDecisions, userId);
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

        List<HomeTopAssetProjection> tierOne = applyTierOneHysteresis(userId,
                opportunities.stream().filter(this::tierOneEligible)
                        .sorted(tierOneOrder()).toList(), effectiveLimit);
        Set<String> tierOneSymbols = tierOne.stream().map(HomeTopAssetProjection::symbol)
                .map(OpportunityPriorityRankingServiceImpl::normalize)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, HomeTopAssetProjection> decisionObservations = opportunities.stream()
                .filter(row -> !tierOneSymbols.contains(normalize(row.symbol())))
                .map(this::asObservationProjection)
                .collect(Collectors.toMap(row -> normalize(row.symbol()), row -> row,
                        (left, right) -> tierTwoOrder().compare(left, right) <= 0 ? left : right,
                        LinkedHashMap::new));
        List<HomeTopAssetProjection> tierTwo = new ArrayList<>();
        for (AssetPoolAssetDTO asset : poolBySymbol.values()) {
            String symbol = normalize(asset.symbol());
            if (tierOneSymbols.contains(symbol)) continue;
            HomeTopAssetProjection observation = decisionObservations.get(symbol);
            if (observation == null) {
                observation = observationProjection(asset, states, decisions, userId, now);
            }
            if (observation != null) tierTwo.add(observation);
        }
        tierTwo.sort(tierTwoOrder());

        List<HomeTopAssetProjection> result = new ArrayList<>(tierOne);
        tierTwo.stream().limit(Math.max(0, effectiveLimit - result.size())).forEach(result::add);
        return List.copyOf(result);
    }

    private static Map<String, AssetPoolAssetDTO> effectivePool(List<AssetPoolAssetDTO> pool, Long userId) {
        Map<String, AssetPoolAssetDTO> bySymbol = new LinkedHashMap<>();
        for (AssetPoolAssetDTO asset : safe(pool)) {
            String symbol = normalize(asset == null ? null : asset.symbol());
            String watchStatus = upper(asset == null ? null : asset.watchStatus());
            if (asset != null
                    && effectiveForUser(asset, userId)
                    && asset.assetId() != null
                    && symbol != null
                    && (watchStatus == null || !INACTIVE_WATCH_STATES.contains(watchStatus))) {
                bySymbol.putIfAbsent(symbol, asset);
            }
        }
        return bySymbol;
    }

    private static boolean effectiveForUser(AssetPoolAssetDTO asset, Long userId) {
        if (asset == null || userId == null) return false;
        if (userId.equals(asset.userId())) return true;
        return Long.valueOf(0L).equals(asset.userId())
                && "DEFAULT".equals(upper(asset.sourceType()));
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
        if (opportunity == null || opportunity.getState() == null) {
            return null;
        }

        Integer opportunityScore = decision.getOpportunityScore() == null
                ? null : (int) Math.round(decision.getOpportunityScore());
        String finalMarketBias = upper(decision.getFinalMarketBias());
        Integer finalConfidence = decision.getFinalConfidence();
        String confidence = finalConfidence == null ? null : String.valueOf(finalConfidence);
        String riskLevel = upper(decision.getRiskLevel());
        String planMode = upper(decision.getPlanMode());
        String aiDecisionResult = upper(decision.getAiConflictLevel());
        Integer dataQuality = decision.getDataQualityScore();
        LocalDateTime completedAt = latestRunTime(run);
        String opportunityState = opportunity.getState().name();

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
        int priorityScore = tierOneEligible(finalMarketBias, finalConfidence, riskLevel, planMode,
                aiDecisionResult, dataQuality, opportunityScore, opportunityState, freshness,
                decision.getOneHourOpportunityQuality(), decision.getFourHourTrendAlignment())
                ? rankingScore(finalMarketBias, finalConfidence,
                decision.getOneHourOpportunityQuality(), decision.getFourHourTrendAlignment(),
                planMode, ageSeconds, riskLevel, aiDecisionResult)
                : 0;
        String rankingReason = "HOME_STATE=" + opportunityState
                + "|DATA_STATUS=" + freshness
                + "|TIER=" + (priorityScore > 0 ? "TIER_1" : "TIER_2")
                + "|RANKING_SCORE=" + priorityScore
                + "|LATEST_FORMAL_SCAN_TIME=" + latestScanAt
                + "|SOURCE=ASSET_POOL_SCAN";

        return new HomeTopAssetProjection(
                asset.assetId(), symbol, asset.displayName(), opportunityScore, finalMarketBias,
                confidence, riskLevel, planMode, aiDecisionResult, dataQuality, freshness,
                ageSeconds, stabilitySeconds, priorityScore, rankingReason, analysisId,
                opportunity.getOpportunityId(), opportunityState, opportunity.getOpportunityId(),
                timeframe, planMode, 0,
                "TIMEFRAME_CONFLICT".equals(freshness) ? "TIMEFRAME_CONFLICT" : "ALIGNED",
                latestScanAt, decision);
    }

    private HomeTopAssetProjection observationProjection(AssetPoolAssetDTO asset,
                                                          List<AssetStateDO> states,
                                                          List<DecisionResultVO> decisions,
                                                          Long userId,
                                                          LocalDateTime now) {
        List<ObservationState> observations = states.stream()
                .filter(state -> exactUserState(state, asset, userId))
                .map(state -> observationState(state, asset, userId, now))
                .filter(state -> state.formalRun() != null)
                .sorted(observationStateOrder())
                .toList();
        if (observations.isEmpty()) return null;
        ObservationState primary = observations.get(0);
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
        DecisionResultVO sourceDecision = exactDecision(
                decisions, analysisId, normalize(asset.symbol()), primary.timeframe());
        return new HomeTopAssetProjection(
                asset.assetId(), normalize(asset.symbol()), asset.displayName(), null, null, null,
                null, null, null,
                sourceDecision == null ? null : sourceDecision.getDataQualityScore(),
                primary.dataStatus(), primary.ageSeconds(), 0L, 0,
                rankingReason, analysisId, null, state, null, primary.timeframe(), null, 0,
                conflictState, primary.finishedAt(), sourceDecision);
    }

    private List<DecisionResultVO> sourceOwnedDecisions(
            Map<String, AssetPoolAssetDTO> poolBySymbol,
            List<AssetStateDO> states,
            List<DecisionResultVO> queriedDecisions,
            Long userId) {
        Map<String, DecisionResultVO> byAnalysisId = safe(queriedDecisions).stream()
                .filter(Objects::nonNull)
                .filter(row -> text(row.getAnalysisId()) != null)
                .collect(Collectors.toMap(row -> text(row.getAnalysisId()), row -> row,
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, DecisionResultVO> verified = new LinkedHashMap<>();
        for (AssetStateDO state : safe(states)) {
            String symbol = normalize(state == null ? null : state.getSymbol());
            AssetPoolAssetDTO asset = symbol == null ? null : poolBySymbol.get(symbol);
            if (!exactUserState(state, asset, userId)) continue;
            String analysisId = text(state.getLastAnalysisId());
            String timeframe = normalizeTimeframe(state.getTimeframe());
            AnalysisRunDO run = formalAssetPoolRun(analysisId, asset, userId, timeframe);
            if (run == null) continue;
            DecisionResultVO decision = byAnalysisId.get(analysisId);
            if (!sameDecisionRun(decision, run)) {
                decision = persistedDecision(run);
            }
            if (sameDecisionRun(decision, run)) {
                verified.putIfAbsent(analysisId, decision);
            }
        }
        return List.copyOf(verified.values());
    }

    private DecisionResultVO persistedDecision(AnalysisRunDO run) {
        if (run == null || text(run.getAnalysisId()) == null) return null;
        try {
            DecisionResult stored = decisionResultMapper.selectLatestByAnalysisId(run.getAnalysisId());
            if (stored == null) return null;
            DecisionResultVO decision = new DecisionResultVO();
            BeanUtils.copyProperties(stored, decision);
            decision.setTimeframe(run.getTimeframe());
            decision.setAnalysisTime(run.getAnalysisTime());
            return decision;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean sameDecisionRun(DecisionResultVO decision, AnalysisRunDO run) {
        return decision != null && run != null
                && Objects.equals(text(decision.getAnalysisId()), text(run.getAnalysisId()))
                && Objects.equals(normalize(decision.getSymbol()), normalize(run.getSymbol()))
                && Objects.equals(normalizeTimeframe(decision.getTimeframe()),
                normalizeTimeframe(run.getTimeframe()));
    }

    private static DecisionResultVO exactDecision(List<DecisionResultVO> decisions,
                                                   String analysisId,
                                                   String symbol,
                                                   String timeframe) {
        return safe(decisions).stream()
                .filter(Objects::nonNull)
                .filter(row -> Objects.equals(text(row.getAnalysisId()), text(analysisId)))
                .filter(row -> Objects.equals(normalize(row.getSymbol()), normalize(symbol)))
                .filter(row -> Objects.equals(normalizeTimeframe(row.getTimeframe()),
                        normalizeTimeframe(timeframe)))
                .findFirst()
                .orElse(null);
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
                .sorted(projectionPrimaryOrder())
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
                freshness, primary.freshnessAgeSeconds(), primary.stabilitySeconds(),
                primary.priorityScore(), reason,
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

    private static Comparator<HomeTopAssetProjection> projectionPrimaryOrder() {
        return Comparator.comparingInt((HomeTopAssetProjection row) -> timeframeRank(row.primaryTimeframe()))
                .thenComparing(HomeTopAssetProjection::priorityScore,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(HomeTopAssetProjection::analysisTime,
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static Comparator<HomeTopAssetProjection> tierOneOrder() {
        return Comparator.comparing(HomeTopAssetProjection::priorityScore,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(row -> riskRank(row.riskLevel()))
                .thenComparingInt(row -> -integer(row.confidence()))
                .thenComparingInt(row -> -oneHourQuality(row))
                .thenComparing(HomeTopAssetProjection::analysisTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(HomeTopAssetProjection::symbol);
    }

    private static Comparator<HomeTopAssetProjection> tierTwoOrder() {
        return Comparator.comparing(HomeTopAssetProjection::dataQuality,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(row -> dataStatusRank(row.freshness()))
                .thenComparing(HomeTopAssetProjection::analysisTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(row -> riskRank(row.riskLevel()))
                .thenComparing(HomeTopAssetProjection::symbol);
    }

    private static Comparator<ObservationState> observationStateOrder() {
        return Comparator.comparingInt((ObservationState row) -> dataStatusRank(row.dataStatus()))
                .thenComparing(ObservationState::finishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ObservationState::timeframe,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static int timeframeRank(String timeframe) {
        return switch (normalizeTimeframe(timeframe) == null ? "" : normalizeTimeframe(timeframe)) {
            case "1h" -> 0;
            case "4h" -> 1;
            case "15m" -> 2;
            case "5m" -> 3;
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

    private boolean tierOneEligible(HomeTopAssetProjection row) {
        if (row == null || row.sourceDecision() == null) return false;
        DecisionResultVO decision = row.sourceDecision();
        return tierOneEligible(row.finalMarketBias(), decision.getFinalConfidence(), row.riskLevel(),
                row.finalPlanMode(), row.aiDecisionResult(), row.dataQuality(), row.opportunityScore(),
                row.opportunityState(), row.freshness(), decision.getOneHourOpportunityQuality(),
                decision.getFourHourTrendAlignment());
    }

    private static boolean tierOneEligible(String finalBias,
                                           Integer finalConfidence,
                                           String risk,
                                           String mode,
                                           String conflict,
                                           Integer dataQuality,
                                           Integer opportunityScore,
                                           String state,
                                           String freshness,
                                           Integer oneHourQuality,
                                           Integer fourHourAlignment) {
        if (!directional(finalBias) || finalConfidence == null || opportunityScore == null
                || oneHourQuality == null || fourHourAlignment == null || dataQuality == null
                || dataQuality < 70 || !"FRESH".equals(upper(freshness))
                || Set.of("EXTREME", "UNKNOWN").contains(upperOrEmpty(risk))
                || conflictPenalty(conflict) == Integer.MAX_VALUE) {
            return false;
        }
        String normalizedState = upperOrEmpty(state);
        String normalizedMode = upperOrEmpty(mode);
        if ("WAITING_TRIGGER".equals(normalizedState)) return "PREPARATION".equals(normalizedMode);
        if ("TRIGGERED".equals(normalizedState)) {
            return Set.of("CONFIRMATION", "REDUCED", "PREPARATION").contains(normalizedMode);
        }
        return "HIGH_RISK".equals(normalizedState) && "REDUCED".equals(normalizedMode);
    }

    private int rankingScore(String finalBias,
                             Integer finalConfidence,
                             Integer oneHourQuality,
                             Integer fourHourAlignment,
                             String planMode,
                             long freshnessAgeSeconds,
                             String risk,
                             String conflict) {
        BigDecimal score = weighted(directionStrength(finalBias), properties.getRanking().getDirectionStrengthWeight())
                .add(weighted(finalConfidence, properties.getRanking().getFinalConfidenceWeight()))
                .add(weighted(oneHourQuality, properties.getRanking().getOneHourOpportunityWeight()))
                .add(weighted(fourHourAlignment, properties.getRanking().getFourHourAlignmentWeight()))
                .add(weighted(executionFeasibility(planMode), properties.getRanking().getExecutionFeasibilityWeight()))
                .add(weighted(freshnessScore(freshnessAgeSeconds), properties.getRanking().getFreshnessWeight()))
                .subtract(BigDecimal.valueOf(riskPenalty(risk)))
                .subtract(BigDecimal.valueOf(conflictPenalty(conflict)));
        return Math.max(0, score.setScale(0, RoundingMode.HALF_UP).intValue());
    }

    private static BigDecimal weighted(Integer value, BigDecimal weight) {
        if (value == null || weight == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(value).multiply(weight);
    }

    private int freshnessScore(long ageSeconds) {
        long window = properties.getRanking().getFreshnessWindowSeconds();
        if (window <= 0 || ageSeconds >= window) return 0;
        return BigDecimal.valueOf(window - Math.max(0, ageSeconds))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(window), 0, RoundingMode.HALF_UP).intValue();
    }

    private static int directionStrength(String bias) {
        return switch (upperOrEmpty(bias)) {
            case "STRONG_BULLISH", "STRONG_BEARISH" -> 100;
            case "BULLISH", "BEARISH" -> 70;
            case "WEAK_BULLISH", "WEAK_BEARISH" -> 40;
            default -> 0;
        };
    }

    private static boolean directional(String bias) {
        return directionStrength(bias) > 0;
    }

    private static int executionFeasibility(String mode) {
        return switch (upperOrEmpty(mode)) {
            case "CONFIRMATION" -> 100;
            case "REDUCED" -> 75;
            case "PREPARATION" -> 60;
            default -> 0;
        };
    }

    private static int riskPenalty(String risk) {
        return switch (upperOrEmpty(risk)) {
            case "LOW" -> 0;
            case "MEDIUM" -> 5;
            case "HIGH" -> 15;
            default -> 30;
        };
    }

    private static int riskRank(String risk) {
        return switch (upperOrEmpty(risk)) {
            case "LOW" -> 0;
            case "MEDIUM" -> 1;
            case "HIGH" -> 2;
            case "EXTREME" -> 3;
            default -> 4;
        };
    }

    private static int conflictPenalty(String conflict) {
        return switch (upperOrEmpty(conflict)) {
            case "LEVEL_1_CONSISTENT", "NONE" -> 0;
            case "LEVEL_2_MINOR_DISAGREEMENT", "MINOR" -> 5;
            case "LEVEL_3_SIGNIFICANT_DISAGREEMENT", "SIGNIFICANT" -> 15;
            case "LEVEL_4_EXTREME_CONFLICT", "CONFUSED" -> Integer.MAX_VALUE;
            default -> Integer.MAX_VALUE;
        };
    }

    private static int integer(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.replace("%", "").trim());
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static int oneHourQuality(HomeTopAssetProjection row) {
        return row == null || row.sourceDecision() == null
                || row.sourceDecision().getOneHourOpportunityQuality() == null
                ? 0 : row.sourceDecision().getOneHourOpportunityQuality();
    }

    private HomeTopAssetProjection asObservationProjection(HomeTopAssetProjection row) {
        String decisionBias = row.sourceDecision() == null ? null
                : upper(row.sourceDecision().getMarketBiasHierarchy());
        String state = decisionBias != null && Set.of("RANGE", "WAIT").contains(decisionBias)
                ? decisionBias : row.opportunityState() == null ? "OBSERVING" : row.opportunityState();
        return new HomeTopAssetProjection(row.assetId(), row.symbol(), row.name(), null, null, null,
                null, null, null, row.dataQuality(), row.freshness(), row.freshnessAgeSeconds(),
                row.stabilitySeconds(), 0, "SLOT_TYPE=OBSERVATION|" + row.rankingReason(),
                row.analysisId(), row.opportunityId(), state, row.primaryOpportunityId(),
                row.primaryTimeframe(), null, row.secondaryOpportunityCount(),
                row.timeframeConflictState(), row.analysisTime(), row.sourceDecision());
    }

    private List<HomeTopAssetProjection> applyTierOneHysteresis(Long userId,
                                                                 List<HomeTopAssetProjection> ranked,
                                                                 int capacity) {
        Map<String, HomeTopAssetProjection> current = safe(ranked).stream()
                .collect(Collectors.toMap(row -> normalize(row.symbol()), row -> row,
                        (left, right) -> tierOneOrder().compare(left, right) <= 0 ? left : right,
                        LinkedHashMap::new));
        synchronized (stableTierOneSymbols) {
            List<HomeTopAssetProjection> selected = new ArrayList<>();
            for (String symbol : stableTierOneSymbols.getOrDefault(userId, List.of())) {
                HomeTopAssetProjection row = current.remove(symbol);
                if (row != null && selected.size() < capacity) selected.add(row);
            }
            for (HomeTopAssetProjection challenger : current.values().stream().sorted(tierOneOrder()).toList()) {
                if (selected.size() < capacity) {
                    selected.add(challenger);
                    continue;
                }
                HomeTopAssetProjection incumbent = selected.stream().min(tierOneOrder().reversed()).orElse(null);
                if (incumbent != null && challenger.priorityScore() >= incumbent.priorityScore()
                        + properties.getRanking().getReplacementThreshold()) {
                    selected.remove(incumbent);
                    selected.add(challenger);
                }
            }
            selected.sort(tierOneOrder());
            List<HomeTopAssetProjection> result = selected.stream().limit(capacity).toList();
            stableTierOneSymbols.put(userId, result.stream().map(row -> normalize(row.symbol())).toList());
            return result;
        }
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
