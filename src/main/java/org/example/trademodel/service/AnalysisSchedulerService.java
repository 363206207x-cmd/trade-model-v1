package org.example.trademodel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunInputException;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunProperties;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.analysisrun.AnalysisTimePolicy;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.providercall.coinglass.CoinGlassConfigurationState;
import org.example.trademodel.providercall.coinglass.CoinGlassProperties;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.example.trademodel.service.watchlistsource.AssetPoolScanTarget;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AnalysisSchedulerService {
    private static final Logger log = LoggerFactory.getLogger(AnalysisSchedulerService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> PRODUCT_TIMEFRAMES = Set.of("5m", "15m", "1h", "4h");
    private static final String BINANCE_PROVIDER = "BINANCE_PUBLIC";
    private static final String SPOT_MARKET = "SPOT";

    private final AnalysisRunOrchestrator analysisRunOrchestrator;
    private final AnalysisRunProperties properties;
    private final Clock clock;
    private final AssetPoolService assetPoolService;
    private final AssetStateMapper assetStateMapper;
    private PersistedOhlcvQueryService persistedOhlcvQueryService;
    private AssetStateService assetStateService;
    private RuleConfigService ruleConfigService;
    private ExecutionPlanMapper executionPlanMapper;
    private CoinGlassProperties coinGlassProperties;
    private FundamentalAiV41Properties v41Properties;
    private final AtomicLong lightweightScanCount = new AtomicLong();
    private final AtomicLong fullAnalysisRequestCount = new AtomicLong();
    private final AtomicLong triggeredLightweightCount = new AtomicLong();
    private final AtomicLong triggeredFullAnalysisRequestCount = new AtomicLong();

    public AnalysisSchedulerService(AnalysisRunOrchestrator analysisRunOrchestrator,
                                    AnalysisRunProperties properties) {
        this(analysisRunOrchestrator, properties, Clock.systemUTC(), null, null);
    }

    public AnalysisSchedulerService(AnalysisRunOrchestrator analysisRunOrchestrator,
                                    AnalysisRunProperties properties,
                                    Clock analysisRunClock) {
        this(analysisRunOrchestrator, properties, analysisRunClock, null, null);
    }

    public AnalysisSchedulerService(AnalysisRunOrchestrator analysisRunOrchestrator,
                                    AnalysisRunProperties properties,
                                    Clock analysisRunClock,
                                    AssetPoolService assetPoolService) {
        this(analysisRunOrchestrator, properties, analysisRunClock, assetPoolService, null);
    }

    @Autowired
    public AnalysisSchedulerService(AnalysisRunOrchestrator analysisRunOrchestrator,
                                    AnalysisRunProperties properties,
                                    Clock analysisRunClock,
                                    AssetPoolService assetPoolService,
                                    AssetStateMapper assetStateMapper) {
        this.analysisRunOrchestrator = analysisRunOrchestrator;
        this.properties = properties;
        this.clock = analysisRunClock != null ? analysisRunClock : Clock.systemUTC();
        this.assetPoolService = assetPoolService;
        this.assetStateMapper = assetStateMapper;
    }

    @Autowired(required = false)
    public void setPersistedOhlcvQueryService(PersistedOhlcvQueryService value) {
        this.persistedOhlcvQueryService = value;
    }

    @Autowired(required = false)
    public void setScheduledScanDependencies(AssetStateService stateService,
                                             RuleConfigService rules,
                                             CoinGlassProperties coinGlass,
                                             FundamentalAiV41Properties contract) {
        this.assetStateService = stateService;
        this.ruleConfigService = rules;
        this.coinGlassProperties = coinGlass;
        this.v41Properties = contract;
    }

    @Autowired(required = false)
    public void setExecutionPlanMapper(ExecutionPlanMapper value) {
        this.executionPlanMapper = value;
    }

    public ApiResponse<AssetAnalysisVO> executeAnalysis(String symbol, String timeframe, String triggerType) {
        AnalysisRunResult result;
        if (triggerType != null && triggerType.startsWith("HOT_RESET:")) {
            String eventId = triggerType.substring("HOT_RESET:".length());
            result = runHotResetRebuild(symbol, timeframe, eventId, null, null);
        } else if ("SCHEDULED".equalsIgnoreCase(triggerType)) {
            validateInput(symbol, timeframe);
            String reference = "SCHEDULED:" + LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
            result = analysisRunOrchestrator.run(AnalysisRunCommand.scheduled(
                    symbol, timeframe, RequestIdSupport.generate(), reference));
        } else {
            validateInput(symbol, timeframe);
            result = analysisRunOrchestrator.run(AnalysisRunCommand.manual(
                    symbol, timeframe, RequestIdSupport.generate(), null));
        }
        if (result != null && result.isSuccessfulAnalysisAvailable()) {
            return ApiResponse.success(result.getStatus(), analysisOrMinimal(result));
        }
        return ApiResponse.fail(failureMessage(result));
    }

    public AnalysisRunResult runManual(String symbol, String timeframe, String requestId, String analysisTime) {
        return analysisRunOrchestrator.run(AnalysisRunCommand.manual(symbol, timeframe, requestId, analysisTime));
    }

    public AnalysisRunResult runMarketDataCompatibility(String symbol, String timeframe, String requestId) {
        return analysisRunOrchestrator.run(AnalysisRunCommand.marketDataCompatibility(symbol, timeframe, requestId));
    }

    public AnalysisRunResult runHotResetRebuild(String symbol, String timeframe, String eventId,
                                                String parentAnalysisId, String parentTraceId) {
        return runHotResetRebuild("SYSTEM", 0L, null, symbol, timeframe, eventId,
                parentAnalysisId, parentTraceId);
    }

    public AnalysisRunResult runHotResetRebuild(String ownerType, Long ownerId, Long assetId,
                                                String symbol, String timeframe, String eventId,
                                                String parentAnalysisId, String parentTraceId) {
        return analysisRunOrchestrator.run(AnalysisRunCommand.hotResetRebuild(
                ownerType, ownerId, assetId, symbol, timeframe, eventId,
                RequestIdSupport.generate(), parentAnalysisId, parentTraceId));
    }

    /**
     * One production tick. Every due Asset Pool target first passes a persisted,
     * source-owned lightweight scan. Only legal material changes reach the full
     * Analysis/Three-AI/Final chain.
     */
    public List<AnalysisRunResult> runScheduledCycle() {
        if (!properties.getScheduler().isEnabled() || !schedulerConfigValid()) return List.of();
        List<AnalysisRunResult> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);
        for (AssetPoolScanTarget target : scanTargets()) {
            try {
                AnalysisRunResult result = scanTarget(target, now);
                if (result != null) results.add(result);
            } catch (RuntimeException failure) {
                log.warn("asset-pool lightweight scan failed symbol={} reason={}",
                        target.symbol(), safeReason(failure));
            }
        }
        return results;
    }

    private AnalysisRunResult scanTarget(AssetPoolScanTarget target, LocalDateTime now) {
        if (assetStateService == null || assetStateMapper == null || persistedOhlcvQueryService == null) return null;
        String timeframe = properties.getScheduler().getDecisionTimeframe();
        OpportunityStateIdentity identity = new OpportunityStateIdentity(
                target.ownerType(), target.ownerId(), target.assetId(), target.symbol(), timeframe);
        AssetStateDO before = assetStateMapper.selectByIdentity(
                identity.ownerType(), identity.ownerId(), identity.symbol(), identity.timeframe());
        AssetStateEnum state = before == null || before.getState() == null
                ? AssetStateEnum.OBSERVING : before.getState();
        long intervalSeconds = properties.getScheduler().intervalSeconds(state.name());
        String traceId = "pool-scan-" + UUID.randomUUID();
        String ruleVersion = ruleConfigService == null ? "v1.0" : ruleConfigService.resolveActiveRuleVersion();
        Long poolItemId = assetPoolService == null ? null : assetPoolService.resolvePoolItemId(
                target.ownerType(), target.ownerId(), target.assetId(), target.symbol());
        if (poolItemId == null || poolItemId <= 0) return null;
        AssetStateService.ScheduledScanClaim claim = assetStateService.claimScheduledScan(
                identity, poolItemId, now, intervalSeconds, traceId, ruleVersion);
        if (claim == null) return null;
        lightweightScanCount.incrementAndGet();
        if (claim.state() == AssetStateEnum.TRIGGERED) triggeredLightweightCount.incrementAndGet();

        LightweightAssessment assessment = lightweightAssessment(claim);
        boolean requestFull = assessment.fullAnalysisCondition();
        String result = !assessment.fresh()
                ? "DATA_NOT_READY"
                : requestFull ? assessment.fullAnalysisReason() : "NO_MATERIAL_CHANGE";
        String failureReason = !assessment.fresh() ? assessment.failureReason() : null;
        if (!requestFull) {
            assetStateService.completeScheduledScan(claim, LocalDateTime.now(clock), result, failureReason,
                    assessment.dataFreshness(), assessment.structureSignature(), assessment.latestCloseTimeMs(),
                    null, false, false);
            return null;
        }

        boolean requestRecorded = assetStateService.completeScheduledScan(
                claim, LocalDateTime.now(clock), assessment.fullAnalysisReason() + ":REQUESTED", null,
                assessment.dataFreshness(), assessment.structureSignature(), assessment.latestCloseTimeMs(),
                null, true, false);
        if (!requestRecorded) return null;

        fullAnalysisRequestCount.incrementAndGet();
        if (claim.state() == AssetStateEnum.TRIGGERED) triggeredFullAnalysisRequestCount.incrementAndGet();
        String reference = "ASSET_POOL_SCAN:" + assessment.latestCloseTimeMs()
                + ":" + assessment.fullAnalysisReason();
        try {
            AnalysisRunResult fullResult = analysisRunOrchestrator.run(AnalysisRunCommand.assetPoolScan(
                    target.ownerType(), target.ownerId(), target.assetId(), target.symbol(), timeframe,
                    RequestIdSupport.generate(), reference));
            boolean succeeded = fullResult != null && fullResult.isSuccessfulAnalysisAvailable();
            String completion = assessment.fullAnalysisReason() + ":"
                    + (fullResult == null ? "RESULT_MISSING" : fullResult.getStatus());
            String completionFailure = succeeded ? null : failureMessage(fullResult);
            assetStateService.completeScheduledScan(
                    claim, LocalDateTime.now(clock), completion, completionFailure,
                    assessment.dataFreshness(), assessment.structureSignature(), assessment.latestCloseTimeMs(),
                    fullResult == null ? null : fullResult.getTraceId(), true, succeeded);
            return fullResult;
        } catch (RuntimeException failure) {
            assetStateService.completeScheduledScan(
                    claim, LocalDateTime.now(clock), assessment.fullAnalysisReason() + ":FAILED",
                    safeReason(failure), assessment.dataFreshness(), assessment.structureSignature(),
                    assessment.latestCloseTimeMs(), null, true, false);
            throw failure;
        }
    }

    private LightweightAssessment lightweightAssessment(AssetStateService.ScheduledScanClaim claim) {
        Map<String, String> trends = new LinkedHashMap<>();
        Long latestDecisionClose = null;
        BigDecimal latestDecisionPrice = null;
        Long latestOneHourClose = null;
        Long latestFourHourClose = null;
        for (String timeframe : properties.getScheduler().getRequiredMarketTimeframes()) {
            PersistedOhlcvReadinessResult readiness = persistedOhlcvQueryService.evaluateReadinessForSource(
                    claim.identity().symbol(), timeframe, properties.getScheduler().getRequiredClosedBars(),
                    maxReadLagMs(timeframe), BINANCE_PROVIDER, SPOT_MARKET);
            if (readiness == null || !readiness.isFresh()) {
                String reason = readiness == null || readiness.getStaleReasonCode() == null
                        ? "SOURCE_READINESS_MISSING" : readiness.getStaleReasonCode().name();
                return LightweightAssessment.notReady(reason);
            }
            trends.put(timeframe, trend(readiness.getBars()));
            if ("1h".equals(timeframe)) latestOneHourClose = readiness.getLatestCloseTimeMs();
            if ("4h".equals(timeframe)) latestFourHourClose = readiness.getLatestCloseTimeMs();
            if (timeframe.equals(properties.getScheduler().getDecisionTimeframe())) {
                latestDecisionClose = readiness.getLatestCloseTimeMs();
                List<PersistedOhlcvBarDO> bars = readiness.getBars();
                latestDecisionPrice = bars.isEmpty() ? null : bars.get(0).getClosePrice();
            }
        }
        if (latestDecisionPrice == null || latestDecisionPrice.signum() <= 0) {
            return LightweightAssessment.notReady("TRUSTED_DECISION_PRICE_MISSING");
        }
        String riskPrecheck = riskPrecheck(claim.risk());
        PlanPrecheck planPrecheck = planPrecheck(claim, LocalDateTime.now(clock));
        String signature = structureSignature(trends)
                + ";CORE_CLOSES=1h:" + latestOneHourClose + ",4h:" + latestFourHourClose
                + ";RISK=" + riskPrecheck + ";PLAN=" + planPrecheck.status();
        String previousFullSignature = scanText(claim.previousExtJson(), "latestFullStructureSignature");
        Long previousFullClose = scanLong(claim.previousExtJson(), "latestFullAnalysisCloseTimeMs");
        Long previousOneHourClose = firstNonNull(
                scanLong(claim.previousExtJson(), "latestFull1hCloseTimeMs"),
                scanSignatureClose(previousFullSignature, "1h"));
        Long previousFourHourClose = firstNonNull(
                scanLong(claim.previousExtJson(), "latestFull4hCloseTimeMs"),
                scanSignatureClose(previousFullSignature, "4h"));
        String previousFullHotReset = scanText(claim.previousExtJson(), "latestFullHotResetAt");
        boolean materialSinceFull = !comparableStructureSignature(signature)
                .equals(comparableStructureSignature(previousFullSignature));
        boolean hotResetPending = claim.hotResetTime() != null
                && !claim.hotResetTime().toString().equals(previousFullHotReset);
        boolean newClosedCandle = latestDecisionClose != null
                && (previousFullClose == null || latestDecisionClose > previousFullClose);
        boolean newCoreClosedCandle = previousFullClose != null
                && (newer(latestOneHourClose, previousOneHourClose)
                || newer(latestFourHourClose, previousFourHourClose));
        boolean promotion = claim.state() == AssetStateEnum.OBSERVING && alignedForPromotion(trends);
        boolean stateRecalculation = newClosedCandle
                && (claim.state() == AssetStateEnum.CANDIDATE
                || claim.state() == AssetStateEnum.WAITING_TRIGGER);
        boolean triggeredMaterialRecheck = claim.state() == AssetStateEnum.TRIGGERED
                && materialSinceFull;
        boolean otherMaterialRecheck = materialSinceFull
                && (claim.state() == AssetStateEnum.HIGH_RISK || claim.state() == AssetStateEnum.CONFUSED);
        boolean full = hotResetPending || newCoreClosedCandle || promotion || stateRecalculation
                || triggeredMaterialRecheck || otherMaterialRecheck;
        String reason = hotResetPending ? "HOT_RESET_RECALCULATION"
                : newCoreClosedCandle ? "NEW_CORE_CLOSED_CANDLE_RECALCULATION"
                : promotion ? "PROMOTION_SIGNAL"
                : stateRecalculation ? "NEW_CLOSED_CANDLE_RECALCULATION"
                : triggeredMaterialRecheck ? "TRIGGERED_MATERIAL_EVIDENCE_CHANGE"
                : otherMaterialRecheck ? "MATERIAL_EVIDENCE_CHANGE" : "NO_MATERIAL_CHANGE";
        return new LightweightAssessment(true, "FRESH:BINANCE_PUBLIC:SPOT", null,
                signature, latestDecisionClose, full, reason);
    }

    private static boolean newer(Long current, Long previous) {
        return current != null && (previous == null || current > previous);
    }

    private static Long firstNonNull(Long primary, Long fallback) {
        return primary == null ? fallback : primary;
    }

    private static Long scanSignatureClose(String signature, String timeframe) {
        if (signature == null || timeframe == null) return null;
        String marker = timeframe + ":";
        int start = signature.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        int end = start;
        while (end < signature.length() && Character.isDigit(signature.charAt(end))) end++;
        if (end == start) return null;
        try {
            return Long.valueOf(signature.substring(start, end));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String comparableStructureSignature(String signature) {
        if (signature == null) return "";
        return signature.replaceFirst(";CORE_CLOSES=1h:[0-9]+,4h:[0-9]+", "");
    }

    public boolean marketDataReady(String symbol) {
        if (persistedOhlcvQueryService == null) return false;
        for (String timeframe : properties.getScheduler().getRequiredMarketTimeframes()) {
            PersistedOhlcvReadinessResult readiness = persistedOhlcvQueryService.evaluateReadinessForSource(
                    symbol, timeframe, properties.getScheduler().getRequiredClosedBars(),
                    maxReadLagMs(timeframe), BINANCE_PROVIDER, SPOT_MARKET);
            if (readiness == null || !readiness.isFresh()) return false;
        }
        return true;
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", properties.getScheduler().isEnabled());
        status.put("symbols", scanSymbols());
        status.put("targetCount", scanTargets().size());
        status.put("assetPoolOnly", true);
        status.put("timeframes", properties.getScheduler().getTimeframes());
        status.put("decisionTimeframe", properties.getScheduler().getDecisionTimeframe());
        status.put("leaseSeconds", properties.getIdempotency().getLeaseSeconds());
        status.put("maxRecoveryAttempts", properties.getIdempotency().getMaxRecoveryAttempts());
        status.put("entryUnified", true);
        status.put("persistentScanClaim", true);
        status.put("lightweightScanCount", lightweightScanCount.get());
        status.put("fullAnalysisRequestCount", fullAnalysisRequestCount.get());
        status.put("triggeredLightweightCount", triggeredLightweightCount.get());
        status.put("triggeredFullAnalysisRequestCount", triggeredFullAnalysisRequestCount.get());
        status.put("notAutoTrading", true);
        status.put("notOrderExecution", true);
        status.put("notUserPositionCreation", true);
        status.put("notUserPositionMutation", true);
        status.put("requiredMarketTimeframes", properties.getScheduler().getRequiredMarketTimeframes());
        status.put("requiredClosedBars", properties.getScheduler().getRequiredClosedBars());
        status.put("stateCadenceConfigured", properties.getScheduler().productionCadenceConfigured());
        status.put("coinGlassConfigured", coinGlassReady());
        status.put("configValid", schedulerConfigValid());
        return status;
    }

    private boolean alignedForPromotion(Map<String, String> trends) {
        FundamentalAiV41Properties contract = v41Properties == null
                ? FundamentalAiV41Properties.contractFixture() : v41Properties;
        String winner = trends.values().stream()
                .filter(value -> !"FLAT".equals(value))
                .max(Comparator.comparingLong(value -> trends.values().stream().filter(value::equals).count()))
                .orElse("FLAT");
        if ("FLAT".equals(winner)) return false;
        int count = (int) trends.values().stream().filter(winner::equals).count();
        BigDecimal weight = BigDecimal.ZERO;
        for (Map.Entry<String, String> entry : trends.entrySet()) {
            if (winner.equals(entry.getValue())) weight = weight.add(weight(entry.getKey(), contract));
        }
        return count >= contract.getMultiTimeframe().getMinimumAlignedCount()
                && weight.compareTo(contract.getMultiTimeframe().getMinimumAlignedWeight()) >= 0;
    }

    private static BigDecimal weight(String timeframe, FundamentalAiV41Properties contract) {
        return switch (timeframe) {
            case "4h" -> contract.getMultiTimeframe().getFourHourWeight();
            case "1h" -> contract.getMultiTimeframe().getOneHourWeight();
            case "15m" -> contract.getMultiTimeframe().getFifteenMinuteWeight();
            case "5m" -> contract.getMultiTimeframe().getFiveMinuteWeight();
            default -> BigDecimal.ZERO;
        };
    }

    private static String trend(List<PersistedOhlcvBarDO> bars) {
        if (bars == null || bars.size() < 2) return "FLAT";
        BigDecimal newest = bars.get(0).getClosePrice();
        BigDecimal oldest = bars.get(bars.size() - 1).getClosePrice();
        if (newest == null || oldest == null) return "FLAT";
        int comparison = newest.compareTo(oldest);
        return comparison > 0 ? "UP" : comparison < 0 ? "DOWN" : "FLAT";
    }

    private static String structureSignature(Map<String, String> trends) {
        return List.of("5m", "15m", "1h", "4h").stream()
                .map(timeframe -> timeframe + "=" + trends.getOrDefault(timeframe, "MISSING"))
                .reduce((left, right) -> left + ";" + right).orElse("");
    }

    private PlanPrecheck planPrecheck(AssetStateService.ScheduledScanClaim claim, LocalDateTime now) {
        if (claim.state() != AssetStateEnum.TRIGGERED) return new PlanPrecheck("NOT_REQUIRED");
        if (executionPlanMapper == null) return new PlanPrecheck("SOURCE_UNAVAILABLE");
        if (claim.opportunityId() == null || claim.opportunityId().isBlank()) {
            return new PlanPrecheck("OPPORTUNITY_ID_MISSING");
        }
        ExecutionPlanDO plan = executionPlanMapper.selectLatestFinalByOpportunityId(claim.opportunityId());
        if (plan == null) return new PlanPrecheck("FINAL_MISSING");
        if (!"CURRENT".equals(normalized(plan.getPlanLifecycleState()))) {
            return new PlanPrecheck("LIFECYCLE_" + normalizedOrUnknown(plan.getPlanLifecycleState()));
        }
        if (plan.getValidFrom() == null || plan.getValidUntil() == null) {
            return new PlanPrecheck("VALIDITY_MISSING");
        }
        if (now.isBefore(plan.getValidFrom())) return new PlanPrecheck("NOT_YET_VALID");
        if (!now.isBefore(plan.getValidUntil())) return new PlanPrecheck("EXPIRED");
        Integer threshold = v41Properties == null ? null
                : v41Properties.getAiGate().getMinimumDataQuality();
        if (threshold == null || plan.getDataQuality() == null || plan.getDataQuality() < threshold) {
            return new PlanPrecheck("DATA_QUALITY_BLOCKED");
        }
        ExecutionPlanReviewPolicy.PersistedPlanState state =
                ExecutionPlanReviewPolicy.currentProjectionPlanState(plan, now);
        return new PlanPrecheck(state == ExecutionPlanReviewPolicy.PersistedPlanState.ACTIVE
                ? "READY" : state.name());
    }

    private static String riskPrecheck(String risk) {
        String normalized = normalized(risk);
        return Set.of("LOW", "MEDIUM", "HIGH", "EXTREME").contains(normalized)
                ? normalized : "UNAVAILABLE";
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizedOrUnknown(String value) {
        String normalized = normalized(value);
        return normalized.isEmpty() ? "UNKNOWN" : normalized;
    }

    private boolean coinGlassReady() {
        return coinGlassProperties != null
                && coinGlassProperties.configurationState() == CoinGlassConfigurationState.CONFIGURED;
    }

    private boolean schedulerConfigValid() {
        try {
            List<AssetPoolScanTarget> targets = scanTargets();
            if (targets.isEmpty() || persistedOhlcvQueryService == null || assetStateService == null) return false;
            for (AssetPoolScanTarget target : targets) validateSymbol(target.symbol());
            Set<String> timeframes = Set.copyOf(properties.getScheduler().getTimeframes());
            Set<String> required = Set.copyOf(properties.getScheduler().getRequiredMarketTimeframes());
            for (String timeframe : timeframes) AnalysisTimePolicy.requireSupportedTimeframe(timeframe);
            return timeframes.equals(PRODUCT_TIMEFRAMES)
                    && required.equals(PRODUCT_TIMEFRAMES)
                    && "5m".equals(properties.getScheduler().getDecisionTimeframe())
                    && properties.getScheduler().getRequiredClosedBars() >= 100
                    && properties.getScheduler().cadenceConfigured()
                    && properties.getScheduler().productionCadenceConfigured();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private List<String> scanSymbols() {
        if (assetPoolService == null) return List.of();
        List<String> symbols = assetPoolService.listScanSymbols();
        return symbols == null ? List.of() : symbols;
    }

    private List<AssetPoolScanTarget> scanTargets() {
        if (assetPoolService == null) return List.of();
        List<AssetPoolScanTarget> targets = assetPoolService.listScanTargets();
        if (targets != null && !targets.isEmpty()) return targets;
        return scanSymbols().stream().map(AssetPoolScanTarget::system).toList();
    }

    private static long maxReadLagMs(String timeframe) {
        return switch (timeframe) {
            case "5m" -> 11L * 60_000L;
            case "15m" -> 31L * 60_000L;
            case "1h" -> 121L * 60_000L;
            case "4h" -> 481L * 60_000L;
            default -> 0L;
        };
    }

    private static String scanText(String json, String field) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = JSON.readTree(json).path("schedulerScan").path(field);
            return node.isTextual() && !node.textValue().isBlank() ? node.textValue() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long scanLong(String json, String field) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = JSON.readTree(json).path("schedulerScan").path(field);
            return node.isIntegralNumber() ? node.longValue() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void validateInput(String symbol, String timeframe) {
        validateSymbol(symbol);
        AnalysisTimePolicy.requireSupportedTimeframe(timeframe);
    }

    private static String validateSymbol(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AnalysisRunInputException("SYMBOL_REQUIRED", "symbol is required");
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static AssetAnalysisVO analysisOrMinimal(AnalysisRunResult result) {
        if (result.getAnalysis() != null) return result.getAnalysis();
        AssetAnalysisVO vo = new AssetAnalysisVO();
        vo.setAnalysisId(result.getAnalysisId());
        vo.setSymbol(result.getSymbol());
        vo.setTimeframe(result.getTimeframe());
        vo.setAnalysisTime(LocalDateTime.now().toString());
        return vo;
    }

    private static String failureMessage(AnalysisRunResult result) {
        if (result == null) return "ANALYSIS_RESULT_MISSING";
        if (result.getReasonCode() != null && !result.getReasonCode().isBlank()) return result.getReasonCode();
        return result.getMessage() == null ? "ANALYSIS_NOT_AVAILABLE" : result.getMessage();
    }

    private static String safeReason(RuntimeException failure) {
        return failure == null ? "RuntimeException" : failure.getClass().getSimpleName();
    }

    private record LightweightAssessment(boolean fresh,
                                         String dataFreshness,
                                         String failureReason,
                                         String structureSignature,
                                         Long latestCloseTimeMs,
                                         boolean fullAnalysisCondition,
                                         String fullAnalysisReason) {
        static LightweightAssessment notReady(String reason) {
            return new LightweightAssessment(false, "NOT_FRESH", reason, null, null,
                    false, "DATA_NOT_READY");
        }
    }

    private record PlanPrecheck(String status) {
    }
}
