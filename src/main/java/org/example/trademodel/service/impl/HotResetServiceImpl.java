package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.HotResetEventTypeEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.ConfusedResult;
import org.example.trademodel.service.ConfusedStateService;
import org.example.trademodel.service.DecisionContext;
import org.example.trademodel.service.HotResetCommand;
import org.example.trademodel.service.HotResetPolicy;
import org.example.trademodel.service.HotResetResult;
import org.example.trademodel.service.HotResetService;
import org.example.trademodel.service.OpportunityStateIdentity;
import org.example.trademodel.service.OpportunityTriggerSource;
import org.example.trademodel.service.PlanRevalidationService;
import org.example.trademodel.enums.PlanRevalidationTriggerTypeEnum;
import org.example.trademodel.service.support.RuleConfigContractService;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.example.trademodel.telegram.HighValueAlertMessageService;
import org.example.trademodel.telegram.HighValueAlertPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class HotResetServiceImpl implements HotResetService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int LEGACY_MIN_CONFUSED_SCORE_THRESHOLD = 40;
    private static final int DEFAULT_EVENT_VERSION = 3;
    private static final int MAX_REBUILD_ERROR_CODE_LENGTH = 128;
    private static final int MAX_REBUILD_ERROR_MESSAGE_LENGTH = 512;
    private static final Pattern AUTHORIZATION = Pattern.compile("(?i)(authorization\\s*[:=]\\s*)([^,;]+)");
    private static final Pattern SECRET_PARAM = Pattern.compile("(?i)(api[_-]?key|token|access[_-]?token|secret)=([^&\\s]+)");
    private static final Pattern URL_QUERY = Pattern.compile("https?://([^\\s?]+)\\?[^\\s]+", Pattern.CASE_INSENSITIVE);

    private final AssetStateMapper assetStateMapper;
    private final HotResetEventMapper hotResetEventMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final ConfusedStateService confusedStateService;
    private final UserPositionRiskAdapter userPositionRiskAdapter;
    private final ObjectProvider<AnalysisSchedulerService> analysisSchedulerServiceProvider;
    private final RuleConfigContractService ruleConfigContractService;
    private final AssetStateService assetStateService;
    private PlanRevalidationService planRevalidationService;
    private HighValueAlertMessageService highValueAlertMessageService;
    private Clock clock = Clock.systemUTC();

    public HotResetServiceImpl(AssetStateMapper assetStateMapper,
                               HotResetEventMapper hotResetEventMapper,
                               DecisionResultMapper decisionResultMapper,
                               ExecutionPlanMapper executionPlanMapper,
                               PushSnapshotMapper pushSnapshotMapper,
                               ConfusedStateService confusedStateService,
                               UserPositionRiskAdapter userPositionRiskAdapter,
                               ObjectProvider<AnalysisSchedulerService> analysisSchedulerServiceProvider) {
        this(assetStateMapper, hotResetEventMapper, decisionResultMapper, executionPlanMapper, pushSnapshotMapper,
                confusedStateService, userPositionRiskAdapter, analysisSchedulerServiceProvider, null, null);
    }

    public HotResetServiceImpl(AssetStateMapper assetStateMapper,
                               HotResetEventMapper hotResetEventMapper,
                               DecisionResultMapper decisionResultMapper,
                               ExecutionPlanMapper executionPlanMapper,
                               PushSnapshotMapper pushSnapshotMapper,
                               ConfusedStateService confusedStateService,
                               UserPositionRiskAdapter userPositionRiskAdapter,
                               ObjectProvider<AnalysisSchedulerService> analysisSchedulerServiceProvider,
                               RuleConfigContractService ruleConfigContractService) {
        this(assetStateMapper, hotResetEventMapper, decisionResultMapper, executionPlanMapper, pushSnapshotMapper,
                confusedStateService, userPositionRiskAdapter, analysisSchedulerServiceProvider,
                ruleConfigContractService, null);
    }

    @Autowired
    public HotResetServiceImpl(AssetStateMapper assetStateMapper,
                               HotResetEventMapper hotResetEventMapper,
                               DecisionResultMapper decisionResultMapper,
                               ExecutionPlanMapper executionPlanMapper,
                               PushSnapshotMapper pushSnapshotMapper,
                               ConfusedStateService confusedStateService,
                               UserPositionRiskAdapter userPositionRiskAdapter,
                               ObjectProvider<AnalysisSchedulerService> analysisSchedulerServiceProvider,
                               RuleConfigContractService ruleConfigContractService,
                               AssetStateService assetStateService) {
        this.assetStateMapper = assetStateMapper;
        this.hotResetEventMapper = hotResetEventMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.confusedStateService = confusedStateService;
        this.userPositionRiskAdapter = userPositionRiskAdapter;
        this.analysisSchedulerServiceProvider = analysisSchedulerServiceProvider;
        this.ruleConfigContractService = ruleConfigContractService;
        this.assetStateService = assetStateService;
    }

    @Autowired(required = false)
    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Autowired(required = false)
    public void setPlanRevalidationService(PlanRevalidationService planRevalidationService) {
        this.planRevalidationService = planRevalidationService;
    }

    @Autowired(required = false)
    public void setHighValueAlertMessageService(HighValueAlertMessageService value) {
        this.highValueAlertMessageService = value;
    }

    @Override
    public boolean shouldTriggerHotReset(int confusedScore, boolean multiTimeframeAligned) {
        return confusedScore >= LEGACY_MIN_CONFUSED_SCORE_THRESHOLD && !multiTimeframeAligned;
    }

    @Override
    public boolean shouldTriggerHotReset(HotResetCommand command) {
        return evaluatePolicy(command).isTriggered();
    }

    @Override
    @Transactional
    public HotResetResult evaluateAndExecute(HotResetCommand command) {
        LocalDateTime nowUtc = UtcLocalTimePolicy.now(clock);
        HotResetPolicy.Evaluation evaluation = evaluatePolicy(command);
        HotResetResult result = baseResult(command, evaluation, nowUtc);
        if (!evaluation.isTriggered()) {
            result.setExecutionStatus("NOT_TRIGGERED");
            return result;
        }

        HotResetEventDO existing = hotResetEventMapper.selectByEventKey(command.getEventKey());
        if (existing != null) {
            return fromExistingEvent(existing);
        }

        String normalizedSymbol = normalizeSymbol(command.getSymbol());
        String normalizedTimeframe = normalizeTimeframe(command.getTimeframe());
        OpportunityStateIdentity identity = stateIdentity(command, normalizedSymbol, normalizedTimeframe);
        LocalDateTime occurredAt = command.getOccurredAt() != null ? command.getOccurredAt() : nowUtc;
        result.setOccurredAt(occurredAt);
        String eventId = "hre-" + UUID.randomUUID().toString().substring(0, 12);

        AssetStateDO currentState = "SYSTEM".equals(identity.ownerType())
                ? assetStateMapper.selectBySymbolAndTimeframe(normalizedSymbol, normalizedTimeframe)
                : assetStateMapper.selectByIdentity(
                        identity.ownerType(), identity.ownerId(), normalizedSymbol, normalizedTimeframe);
        AssetStateEnum preState = currentState != null && currentState.getState() != null
                ? currentState.getState()
                : AssetStateEnum.OBSERVING;
        int confusedScoreBefore = currentState != null && currentState.getConfusedScore() != null
                ? currentState.getConfusedScore()
                : 0;

        DecisionContext confusedContext = buildConfusedContext(command);
        ConfusedResult confusedResult = "SYSTEM".equals(identity.ownerType())
                ? confusedStateService.calculateConfused(normalizedSymbol, normalizedTimeframe, confusedContext)
                : confusedStateService.calculateConfused(identity, confusedContext);
        UserPositionRiskResult riskResult = currentRiskFailClosed();
        AssetStateEnum postState = HotResetPolicy.resolvePostState(command, confusedResult, riskResult.isRiskBlocked());
        if (HotResetPolicy.isUnsafePreState(postState)) {
            postState = AssetStateEnum.INVALIDATED;
        }

        int confusedLowStreak = confusedResult != null ? confusedResult.getConfusedLowStreak() : 0;
        int confusedScoreAfter = confusedResult != null ? confusedResult.getConfusedScore() : confusedScoreBefore;
        persistAssetState(identity, postState, confusedScoreAfter, confusedLowStreak,
                command, occurredAt, nowUtc, preState);

        String reasonCode = evaluation.getReasonCode();
        int decisionCount = decisionResultMapper.markHotResetInvalidatedBySymbol(
                normalizedSymbol, eventId, reasonCode, nowUtc);
        List<ExecutionPlanDO> affectedPlans = executionPlanMapper.listFinalPlansForHotReset(
                command.getAnalysisId(), normalizedSymbol);
        int planCount = executionPlanMapper.markNeedsRevalidationForHotReset(
                command.getAnalysisId(), normalizedSymbol, eventId,
                command.getEventType().name() + ":" + reasonCode, nowUtc);
        if (planRevalidationService != null) {
            for (ExecutionPlanDO plan : affectedPlans) {
                planRevalidationService.recordSystemTrigger(
                        plan, PlanRevalidationTriggerTypeEnum.HOT_RESET,
                        command.getEventType().name() + ":" + reasonCode, nowUtc);
            }
        }
        int pushCount = pushSnapshotMapper.invalidatePendingBySymbolForHotReset(normalizedSymbol);

        HotResetEventDO event = buildEvent(command, eventId, evaluation, occurredAt, nowUtc, preState, postState,
                confusedScoreBefore, confusedScoreAfter, confusedLowStreak, riskResult,
                decisionCount, planCount, pushCount);
        hotResetEventMapper.insert(event);

        result.setEventId(eventId);
        result.setTriggered(true);
        result.setPreState(preState.name());
        result.setPostState(postState.name());
        result.setDecisionInvalidatedCount(decisionCount);
        result.setPlanRevalidationCount(planCount);
        result.setPushInvalidatedCount(pushCount);
        result.setConfusedScoreBefore(confusedScoreBefore);
        result.setConfusedScoreAfter(confusedScoreAfter);
        result.setAccountRiskStatus(riskResult.getRiskStatus());
        result.setAccountRiskLevel(riskResult.getRiskLevel());
        result.setAccountRiskBlocked(riskResult.isRiskBlocked());
        result.setRebuildTriggered(true);
        result.setExecutionStatus("COMPLETED");
        result.setCompletedAt(nowUtc);

        if (highValueAlertMessageService != null && "USER".equals(event.getOwnerType())
                && event.getOwnerId() != null && event.getOwnerId() > 0) {
            String planId = affectedPlans.isEmpty() ? null : affectedPlans.get(0).getPlanId();
            highValueAlertMessageService.recordSafetyChange(
                    new HighValueAlertMessageService.SafetyChangeInput(
                            event.getOwnerId(), HighValueAlertPolicy.SafetyChangeType.HOT_RESET,
                            "HOT_RESET", eventId, command.getAnalysisId(), planId,
                            latestPushId(command.getAnalysisId()), normalizedSymbol, command.getTraceId(),
                            postState.name(), 4, event.getTriggerReasonText(),
                            "等待重建分析、规则校验和计划重新验证完成", occurredAt, null));
        }

        runRebuildAfterCommit(event, result);
        return result;
    }

    @Override
    public DecisionResult executeHotReset(DecisionContext context, DecisionResult currentResult) {
        HotResetCommand command = new HotResetCommand();
        command.setEventKey("legacy-hot-reset-" + (currentResult != null ? currentResult.getDecisionId() : "missing"));
        command.setAnalysisId(currentResult != null ? currentResult.getAnalysisId() : null);
        command.setOwnerType("SYSTEM");
        command.setOwnerId(0L);
        command.setRuleVersion("LEGACY_HOT_RESET");
        command.setSymbol(currentResult != null ? currentResult.getSymbol() : null);
        command.setEventType(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        command.setDecisionContext(context);
        evaluateAndExecute(command);
        return currentResult;
    }

    private HotResetPolicy.Evaluation evaluatePolicy(HotResetCommand command) {
        try {
            HotResetPolicy.Thresholds thresholds = ruleConfigContractService != null
                    ? ruleConfigContractService.requireHotResetThresholds()
                    : null;
            return HotResetPolicy.evaluate(command, thresholds);
        } catch (RuntimeException ex) {
            return HotResetPolicy.Evaluation.notTriggered("HOT_RESET_CONFIG_NOT_READY", List.of(redact(ex.getMessage())));
        }
    }

    private HotResetResult baseResult(HotResetCommand command, HotResetPolicy.Evaluation evaluation,
                                      LocalDateTime nowUtc) {
        HotResetResult result = new HotResetResult();
        if (command != null) {
            result.setEventKey(command.getEventKey());
            result.setEventType(command.getEventType());
            result.setAnalysisId(command.getAnalysisId());
            result.setSymbol(command.getSymbol());
            result.setTimeframe(command.getTimeframe());
            result.setOccurredAt(command.getOccurredAt());
        }
        result.setTriggered(false);
        result.setDeduplicated(false);
        result.setReasonCodes(evaluation.getReasonCodes());
        result.setCompletedAt(nowUtc);
        return result;
    }

    private HotResetResult fromExistingEvent(HotResetEventDO event) {
        HotResetResult result = new HotResetResult();
        result.setEventId(event.getEventId());
        result.setEventKey(event.getEventKey());
        result.setEventType(parseEventType(event.getTriggerType()));
        result.setTriggered(true);
        result.setDeduplicated(true);
        result.setAnalysisId(event.getAnalysisId());
        result.setRebuildAnalysisId(event.getRebuildAnalysisId());
        result.setSymbol(event.getSymbol());
        result.setTimeframe(event.getTimeframe());
        result.setPreState(event.getPreState());
        result.setPostState(event.getPostState());
        result.setDecisionInvalidatedCount(zero(event.getDecisionInvalidatedCount()));
        result.setPlanRevalidationCount(zero(event.getPlanRevalidationCount()));
        result.setPushInvalidatedCount(zero(event.getPushInvalidatedCount()));
        result.setConfusedScoreBefore(event.getConfusedScoreBefore());
        result.setConfusedScoreAfter(event.getConfusedScoreAfter());
        result.setAccountRiskStatus(event.getAccountRiskStatus());
        result.setAccountRiskLevel(event.getAccountRiskLevel());
        result.setAccountRiskBlocked(Boolean.TRUE.equals(event.getAccountRiskBlocked()));
        result.setRebuildTriggered(Boolean.TRUE.equals(event.getRebuildTriggered()));
        result.setExecutionStatus(event.getExecutionStatus());
        result.setReasonCodes(List.of("EVENT_KEY_DEDUPLICATED"));
        result.setOccurredAt(event.getEventTime());
        result.setCompletedAt(event.getCompletedAt());
        return result;
    }

    private String latestPushId(String analysisId) {
        if (analysisId == null || analysisId.isBlank()) return null;
        var rows = pushSnapshotMapper.listByAnalysisId(analysisId);
        return rows == null || rows.isEmpty() || rows.get(0).getPushId() == null
                ? null : String.valueOf(rows.get(0).getPushId());
    }

    private void persistAssetState(OpportunityStateIdentity identity, AssetStateEnum postState, int confusedScore,
                                   int confusedLowStreak, HotResetCommand command, LocalDateTime occurredAt,
                                   LocalDateTime nowUtc, AssetStateEnum preState) {
        if (assetStateService == null) {
            throw new IllegalStateException("Canonical opportunity transition service is required");
        }
        assetStateService.transition(identity, postState, confusedScore,
                Math.max(0, confusedLowStreak), command.getAnalysisId(), command.getTraceId(),
                requireRuleVersion(command),
                "HOT_RESET:" + command.getEventType().name(), OpportunityTriggerSource.HOT_RESET);

        AssetStateDO hot = new AssetStateDO();
        hot.setOwnerType(identity.ownerType());
        hot.setOwnerId(identity.ownerId());
        hot.setAssetId(identity.assetId());
        hot.setSymbol(identity.symbol());
        hot.setTimeframe(identity.timeframe());
        hot.setHotResetFlag(true);
        hot.setHotResetTriggerType(command.getEventType().name());
        hot.setHotResetTriggerValue(command.getEventKey());
        hot.setHotResetTime(occurredAt);
        hot.setPreResetState(preState.name());
        hot.setPostResetState(postState.name());
        hot.setLastUpdateTime(nowUtc);
        assetStateMapper.updateHotResetColumns(hot);
    }

    private HotResetEventDO buildEvent(HotResetCommand command, String eventId, HotResetPolicy.Evaluation evaluation,
                                       LocalDateTime occurredAt, LocalDateTime completedAt,
                                       AssetStateEnum preState, AssetStateEnum postState,
                                       int confusedScoreBefore, int confusedScoreAfter, int confusedLowStreak,
                                       UserPositionRiskResult riskResult, int decisionCount, int planCount, int pushCount) {
        HotResetEventDO event = new HotResetEventDO();
        event.setEventId(eventId);
        event.setEventKey(command.getEventKey());
        event.setAnalysisId(command.getAnalysisId());
        event.setTraceId(command.getTraceId());
        event.setOwnerType(normalizeOwnerType(command.getOwnerType()));
        event.setOwnerId(normalizeOwnerId(event.getOwnerType(), command.getOwnerId()));
        event.setAssetId(command.getAssetId());
        event.setRuleVersion(requireRuleVersion(command));
        event.setSymbol(normalizeSymbol(command.getSymbol()));
        event.setTimeframe(command.getTimeframe());
        event.setTriggerType(command.getEventType().name());
        event.setTriggerValue(String.join(";", evaluation.getReasonCodes()));
        event.setSourceType(command.getSourceType());
        event.setSourceReference(command.getSourceReference());
        event.setSeverityScore(command.getSeverityScore());
        event.setDecisionInvalidatedCount(decisionCount);
        event.setPlanRevalidationCount(planCount);
        event.setPushInvalidatedCount(pushCount);
        event.setConfusedScoreSnapshot(confusedScoreAfter);
        event.setConfusedScoreBefore(confusedScoreBefore);
        event.setConfusedScoreAfter(confusedScoreAfter);
        event.setMultiTimeframeAlignedSnapshot(command.getDecisionContext() != null
                ? command.getDecisionContext().isMultiTimeframeAligned()
                : null);
        event.setAccountRiskStatus(riskResult.getRiskStatus());
        event.setAccountRiskLevel(riskResult.getRiskLevel());
        event.setAccountRiskBlocked(riskResult.isRiskBlocked());
        event.setAccountRiskSnapshot(writeRiskSnapshot(riskResult));
        event.setRebuildTriggered(true);
        event.setExecutionStatus("COMPLETED");
        event.setTriggerReasonCode(evaluation.getReasonCode());
        event.setTriggerReasonText(String.join(" | ", evaluation.getReasonCodes()));
        event.setEventVersion(DEFAULT_EVENT_VERSION);
        event.setEventTime(occurredAt);
        event.setPreState(preState.name());
        event.setPostState(postState.name());
        event.setCompletedAt(completedAt);
        event.setCreateTime(completedAt);
        return event;
    }

    private void runRebuildAfterCommit(HotResetEventDO event, HotResetResult result) {
        Runnable rebuild = () -> runRebuild(event, result);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rebuild.run();
                }
            });
        } else {
            rebuild.run();
        }
    }

    private void runRebuild(HotResetEventDO event, HotResetResult result) {
        LocalDateTime completedAtUtc = UtcLocalTimePolicy.now(clock);
        HotResetEventDO update = new HotResetEventDO();
        update.setEventId(event.getEventId());
        update.setRebuildTriggered(true);
        update.setCompletedAt(completedAtUtc);
        result.setCompletedAt(completedAtUtc);
        try {
            AnalysisSchedulerService scheduler = analysisSchedulerServiceProvider.getIfAvailable();
            if (scheduler == null) {
                markRebuildFailed(update, result,
                        "ANALYSIS_SCHEDULER_UNAVAILABLE",
                        "AnalysisSchedulerService unavailable");
            } else {
                AnalysisRunResult rebuildResult = "SYSTEM".equals(event.getOwnerType())
                        ? scheduler.runHotResetRebuild(
                                event.getSymbol(), event.getTimeframe(), event.getEventId(),
                                event.getAnalysisId(), event.getTraceId())
                        : scheduler.runHotResetRebuild(
                                event.getOwnerType(), event.getOwnerId(), event.getAssetId(),
                                event.getSymbol(), event.getTimeframe(), event.getEventId(),
                                event.getAnalysisId(), event.getTraceId());
                applyRebuildOutcome(update, result, rebuildResult);
            }
        } catch (Exception e) {
            markRebuildFailed(update, result, "REBUILD_EXCEPTION", e.getMessage());
        }
        hotResetEventMapper.updateRebuildOutcome(update);
    }

    private void applyRebuildOutcome(HotResetEventDO update, HotResetResult result, AnalysisRunResult rebuildResult) {
        if (rebuildResult != null && rebuildResult.isSuccessfulAnalysisAvailable()) {
            update.setRebuildAnalysisId(rebuildResult.getAnalysisId());
            update.setExecutionStatus("COMPLETED");
            update.setExecutionErrorCode(null);
            update.setExecutionErrorMessage(null);
            result.setRebuildAnalysisId(rebuildResult.getAnalysisId());
            result.setExecutionStatus("COMPLETED");
            return;
        }
        markRebuildFailed(update, result, rebuildFailureCode(rebuildResult), rebuildFailureMessage(rebuildResult));
    }

    private void markRebuildFailed(HotResetEventDO update, HotResetResult result, String errorCode, String errorMessage) {
        update.setRebuildAnalysisId(null);
        update.setExecutionStatus("REBUILD_FAILED");
        update.setExecutionErrorCode(truncate(redact(errorCode), MAX_REBUILD_ERROR_CODE_LENGTH));
        update.setExecutionErrorMessage(truncate(redact(errorMessage), MAX_REBUILD_ERROR_MESSAGE_LENGTH));
        result.setRebuildAnalysisId(null);
        result.setExecutionStatus("REBUILD_FAILED");
    }

    private static String rebuildFailureCode(AnalysisRunResult rebuildResult) {
        if (rebuildResult == null) {
            return "ANALYSIS_REBUILD_RESULT_MISSING";
        }
        return hasText(rebuildResult.getReasonCode())
                ? rebuildResult.getReasonCode()
                : "ANALYSIS_REBUILD_RESULT_INVALID";
    }

    private static String rebuildFailureMessage(AnalysisRunResult rebuildResult) {
        if (rebuildResult == null) {
            return "Analysis rebuild returned no result";
        }
        String status = hasText(rebuildResult.getStatus()) ? rebuildResult.getStatus() : "UNKNOWN";
        String message = hasText(rebuildResult.getMessage())
                ? rebuildResult.getMessage()
                : "analysis rebuild did not execute successfully";
        if (!rebuildResult.hasAnalysisId()) {
            message = message + "; analysisId missing";
        }
        return status + ": " + message;
    }

    private static String redact(String raw) {
        if (raw == null) {
            return null;
        }
        String t = AUTHORIZATION.matcher(raw).replaceAll("$1<redacted>");
        t = SECRET_PARAM.matcher(t).replaceAll("$1=<redacted>");
        return URL_QUERY.matcher(t).replaceAll("https://$1?<redacted>");
    }

    private static String truncate(String raw, int max) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private DecisionContext buildConfusedContext(HotResetCommand command) {
        DecisionContext context = command.getDecisionContext() != null ? command.getDecisionContext() : new DecisionContext();
        context.setSymbol(normalizeSymbol(command.getSymbol()));
        int score = command.getSeverityScore() != null ? Math.max(0, Math.min(100, command.getSeverityScore()))
                : defaultSeverity(command.getEventType());
        if (context.getDriverConflictScore() == null) {
            context.setDriverConflictScore(score);
        }
        if (context.getExecutionInstabilityScore() == null) {
            context.setExecutionInstabilityScore(score);
        }
        if (context.getMicrostructureTrapScore() == null) {
            context.setMicrostructureTrapScore(score);
        }
        if (context.getCauseEffectDivergenceScore() == null) {
            context.setCauseEffectDivergenceScore(score);
        }
        if (context.getAiConflictScore() == null) {
            context.setAiConflictScore(score);
        }
        return context;
    }

    private int defaultSeverity(HotResetEventTypeEnum type) {
        if (type == HotResetEventTypeEnum.LIQUIDITY_DRAIN) {
            return 54;
        }
        if (type == HotResetEventTypeEnum.SYSTEMIC_SHOCK) {
            return 90;
        }
        if (type == HotResetEventTypeEnum.OI_COLLAPSE) {
            return 75;
        }
        return 80;
    }

    private UserPositionRiskResult currentRiskFailClosed() {
        try {
            UserPositionRiskResult result = userPositionRiskAdapter.currentRiskForSystem();
            return result != null ? result : UserPositionRiskResult.failClosed("HOT_RESET_RISK_CONTEXT_UNAVAILABLE");
        } catch (Exception e) {
            return UserPositionRiskResult.failClosed("HOT_RESET_RISK_CONTEXT_UNAVAILABLE");
        }
    }

    private String writeRiskSnapshot(UserPositionRiskResult riskResult) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("riskStatus", riskResult.getRiskStatus());
            snapshot.put("riskLevel", riskResult.getRiskLevel());
            snapshot.put("riskBlocked", riskResult.isRiskBlocked());
            snapshot.put("aggregateRiskScore", riskResult.getAggregateRiskScore());
            snapshot.put("reasonCodes", riskResult.getReasonCodes());
            snapshot.put("calculatedAt", riskResult.getCalculatedAt() != null
                    ? riskResult.getCalculatedAt().toString()
                    : null);
            return JSON.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "{\"snapshotStatus\":\"SERIALIZATION_FAILED\"}";
        }
    }

    private HotResetEventTypeEnum parseEventType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return HotResetEventTypeEnum.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTimeframe(String timeframe) {
        return timeframe == null || timeframe.isBlank()
                ? "global" : timeframe.trim().toLowerCase(Locale.ROOT);
    }

    private OpportunityStateIdentity stateIdentity(HotResetCommand command,
                                                   String symbol,
                                                   String timeframe) {
        String ownerType = normalizeOwnerType(command.getOwnerType());
        return new OpportunityStateIdentity(ownerType, normalizeOwnerId(ownerType, command.getOwnerId()),
                command.getAssetId(), symbol, timeframe);
    }

    private String normalizeOwnerType(String ownerType) {
        return hasText(ownerType) ? ownerType.trim().toUpperCase(Locale.ROOT) : "SYSTEM";
    }

    private Long normalizeOwnerId(String ownerType, Long ownerId) {
        return "SYSTEM".equals(ownerType) ? 0L : ownerId;
    }

    private String requireRuleVersion(HotResetCommand command) {
        if (!hasText(command.getRuleVersion())) {
            throw new IllegalArgumentException("Hot Reset ruleVersion is required");
        }
        return command.getRuleVersion().trim();
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
