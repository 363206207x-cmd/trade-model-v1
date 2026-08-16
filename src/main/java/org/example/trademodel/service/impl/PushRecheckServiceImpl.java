package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.derivatives.DerivativesSnapshotReadPort;
import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotPolicy;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.PushRecheckAccessBoundary;
import org.example.trademodel.service.PushRecheckDispatchConfigService;
import org.example.trademodel.service.PushRecheckService;
import org.example.trademodel.service.PushRecheckStatusContract;
import org.example.trademodel.service.RecheckExecutionCommand;
import org.example.trademodel.service.RecheckResult;
import org.example.trademodel.service.support.RuleConfigContractService;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.example.trademodel.telegram.HighValueAlertMessageService;
import org.example.trademodel.telegram.HighValueAlertPolicy;
import org.example.trademodel.vo.PushRecheckLogItemVO;
import org.example.trademodel.vo.PushRecheckOpsOverviewVO;
import org.example.trademodel.vo.PushRecheckReplaySummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PushRecheckServiceImpl implements PushRecheckService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final PushSnapshotMapper pushSnapshotMapper;
    private final AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;
    private final PushRecheckDispatchConfigService dispatchConfigService;
    private final UserPositionRiskAdapter userPositionRiskAdapter;
    private final MarketPriceSnapshotService marketPriceSnapshotService;
    private final RuleConfigContractService ruleConfigContractService;
    private final PushRecheckAccessBoundary accessBoundary;
    private DerivativesSnapshotReadPort derivativesSnapshotReadPort;
    private HighValueAlertMessageService highValueAlertMessageService;
    private AnalysisRunMapper analysisRunMapper;
    private ExecutionPlanMapper executionPlanMapper;
    private Clock clock = Clock.systemUTC();

    public PushRecheckServiceImpl(PushSnapshotMapper pushSnapshotMapper,
                                  AccountRiskSnapshotMapper accountRiskSnapshotMapper,
                                  PushRecheckLogMapper pushRecheckLogMapper,
                                  PushRecheckDispatchConfigService dispatchConfigService,
                                  UserPositionRiskAdapter userPositionRiskAdapter) {
        this(pushSnapshotMapper, accountRiskSnapshotMapper, pushRecheckLogMapper, dispatchConfigService,
                userPositionRiskAdapter, null, null, new PushRecheckAccessBoundary());
    }

    public PushRecheckServiceImpl(PushSnapshotMapper pushSnapshotMapper,
                                  AccountRiskSnapshotMapper accountRiskSnapshotMapper,
                                  PushRecheckLogMapper pushRecheckLogMapper,
                                  PushRecheckDispatchConfigService dispatchConfigService,
                                  UserPositionRiskAdapter userPositionRiskAdapter,
                                  MarketPriceSnapshotService marketPriceSnapshotService,
                                  RuleConfigContractService ruleConfigContractService) {
        this(pushSnapshotMapper, accountRiskSnapshotMapper, pushRecheckLogMapper, dispatchConfigService,
                userPositionRiskAdapter, marketPriceSnapshotService, ruleConfigContractService,
                new PushRecheckAccessBoundary());
    }

    @Autowired
    public PushRecheckServiceImpl(PushSnapshotMapper pushSnapshotMapper,
                                  AccountRiskSnapshotMapper accountRiskSnapshotMapper,
                                  PushRecheckLogMapper pushRecheckLogMapper,
                                  PushRecheckDispatchConfigService dispatchConfigService,
                                  UserPositionRiskAdapter userPositionRiskAdapter,
                                  MarketPriceSnapshotService marketPriceSnapshotService,
                                  RuleConfigContractService ruleConfigContractService,
                                  PushRecheckAccessBoundary accessBoundary) {
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.accountRiskSnapshotMapper = accountRiskSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.dispatchConfigService = dispatchConfigService;
        this.userPositionRiskAdapter = userPositionRiskAdapter;
        this.marketPriceSnapshotService = marketPriceSnapshotService;
        this.ruleConfigContractService = ruleConfigContractService;
        this.accessBoundary = accessBoundary;
    }

    @Autowired(required = false)
    void setDerivativesSnapshotReadPort(DerivativesSnapshotReadPort derivativesSnapshotReadPort) {
        this.derivativesSnapshotReadPort = derivativesSnapshotReadPort;
    }

    @Autowired(required = false)
    void setHighValueAlertDependencies(HighValueAlertMessageService service,
                                       AnalysisRunMapper analysisRunMapper,
                                       ExecutionPlanMapper executionPlanMapper) {
        this.highValueAlertMessageService = service;
        this.analysisRunMapper = analysisRunMapper;
        this.executionPlanMapper = executionPlanMapper;
    }

    @Autowired(required = false)
    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public RecheckResult recheck(Long pushId, BigDecimal currentPrice) {
        return recheck(pushId, currentPrice, RecheckExecutionCommand.manual());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecheckResult recheck(Long pushId, BigDecimal currentPrice, RecheckExecutionCommand command) {
        RecheckExecutionCommand executionCommand = command != null ? command : RecheckExecutionCommand.manual();
        accessBoundary.requireInternalScheduledExecution(executionCommand);
        if (pushId == null) {
            RecheckResult early = new RecheckResult();
            early.setPushId(null);
            early.setRecheckStatus(RecheckStatusEnum.INVALIDATED);
            early.setCurrentPrice(currentPrice);
            early.setValid(false);
            early.setReviewPassed(false);
            early.setMessage("复查上下文无效，不得作为当前交易依据");
            return early;
        }

        LocalDateTime now = UtcLocalTimePolicy.now(clock);
        TmPushSnapshotDO snap = pushSnapshotMapper.selectByPushId(pushId);

        RecheckStatusEnum status;
        String message;
        BigDecimal priceDriftRatio = null;
        String failReasonJson = null;
        Boolean currentAccountRiskAllowed = null;
        TmAccountRiskSnapshotDO accountRiskSnapshot = null;
        UserPositionRiskResult userPositionRiskResult = null;
        RuleConfigContractService.PushRecheckThresholds thresholds = null;
        DerivativesGuard derivativesGuard = null;

        if (snap == null) {
            status = RecheckStatusEnum.INVALIDATED;
            message = "复查上下文无效，不得作为当前交易依据";
            failReasonJson = failJson("SNAPSHOT_NOT_FOUND", "push_id=" + pushId);
        } else if ((thresholds = resolvePushRecheckThresholds()) == null) {
            status = RecheckStatusEnum.INVALIDATED;
            message = "复查配置不可用，仅供人工复核";
            failReasonJson = failJson("PUSH_RECHECK_CONFIG_NOT_READY", "push_recheck_config missing or invalid");
        } else if (snap.getExpiresAt() != null && !now.isBefore(snap.getExpiresAt())) {
            status = RecheckStatusEnum.EXPIRED;
            message = "推送已过期，不得作为当前交易依据";
            failReasonJson = failJson("EXPIRED", "expires_at=" + snap.getExpiresAt());
        } else if ((derivativesGuard = validateDerivativesForRecheck(snap)) != null
                && derivativesGuard.status() != null) {
            status = derivativesGuard.status();
            message = derivativesGuard.message();
            failReasonJson = failJson(derivativesGuard.reasonCode(), derivativesGuard.detail());
        } else {
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                PriceResolution resolved = resolveCurrentPrice(snap);
                currentPrice = resolved.currentPrice();
                failReasonJson = resolved.failReasonJson();
            }
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                status = RecheckStatusEnum.INVALIDATED;
                message = "当前行情不可用，仅供人工复核";
                if (failReasonJson == null) {
                    failReasonJson = failJson("PRICE_REQUIRED", "current_price null or non-positive");
                }
            } else {
            accountRiskSnapshot = resolveAccountRiskSnapshot(snap);
            currentAccountRiskAllowed = accountRiskSnapshot != null ? accountRiskSnapshot.getRiskAllowed() : null;
            userPositionRiskResult = resolveUserPositionRiskResult();
            currentAccountRiskAllowed = combineRiskAllowance(currentAccountRiskAllowed, userPositionRiskResult);
            String invHit = evaluateStructuredInvalidation(snap.getInvalidationConditionJson(), currentPrice);
            if (invHit != null) {
                status = RecheckStatusEnum.INVALIDATED;
                message = "命中快照中的失效条件";
                failReasonJson = failJson("INVALIDATED", invHit);
            } else if (snap.getTriggerPrice() != null
                    && snap.getTriggerPrice().compareTo(BigDecimal.ZERO) > 0) {
                priceDriftRatio = currentPrice.subtract(snap.getTriggerPrice()).abs()
                        .divide(snap.getTriggerPrice(), 8, RoundingMode.HALF_UP);
                if (priceDriftRatio.compareTo(thresholds.getDriftRatioThreshold()) > 0) {
                    status = RecheckStatusEnum.DRIFTED_FROM_ENTRY_ZONE;
                    message = "当前价格已偏离原入场区域，需要人工复核";
                    failReasonJson = failJson("DRIFTED",
                            "threshold=" + thresholds.getDriftRatioThreshold() + ",ratio=" + priceDriftRatio);
                } else {
                    status = classifyPostPriceChecks(snap, currentAccountRiskAllowed, thresholds);
                    message = statusMessage(status, currentAccountRiskAllowed);
                    failReasonJson = failReasonForStatus(
                            status, currentAccountRiskAllowed, snap, accountRiskSnapshot, userPositionRiskResult,
                            thresholds);
                }
            } else {
                priceDriftRatio = null;
                status = classifyPostPriceChecks(snap, currentAccountRiskAllowed, thresholds);
                message = status == RecheckStatusEnum.REVIEW_PASSED
                        ? "复查条件通过，仅供人工复核，不是交易指令"
                        : statusMessage(status, currentAccountRiskAllowed);
                failReasonJson = failReasonForStatus(
                        status, currentAccountRiskAllowed, snap, accountRiskSnapshot, userPositionRiskResult,
                        thresholds);
            }
            }
        }

        RecheckResult result = new RecheckResult();
        result.setPushId(pushId);
        result.setRecheckStatus(status);
        result.setCurrentPrice(currentPrice);
        result.setValid(false);
        result.setReviewPassed(PushRecheckStatusContract.isReviewPassed(status));
        result.setMessage(message);

        TmPushRecheckLogDO row = new TmPushRecheckLogDO();
        row.setPushId(pushId);
        row.setDispatchBatchId(executionCommand.getDispatchBatchId());
        row.setDispatchInstructionId(executionCommand.getDispatchInstructionId());
        row.setTriggerSource(executionCommand.getTriggerSource());
        row.setRetryAttempt(executionCommand.getRetryAttempt());
        row.setMaxAttempts(executionCommand.getMaxAttempts());
        row.setRetryBackoffMinutes(executionCommand.getRetryBackoffMinutes());
        row.setReplayFromLogId(executionCommand.getReplayFromLogId());
        row.setExecutionStatus("COMPLETED");
        row.setExecutionErrorCode(extractFailCode(failReasonJson));
        row.setExecutionErrorMessage(message);
        row.setRecheckTime(now);
        row.setRecheckStatus(status.name());
        row.setCurrentPrice(currentPrice);
        row.setPriceDriftRatio(priceDriftRatio);
        // Recheck 仅有实时价与快照 trigger_price，可稳定复用为“当前滑点估算”。
        row.setCurrentSlippageEstimation(priceDriftRatio);
        row.setCurrentAccountRiskAllowed(currentAccountRiskAllowed);
        row.setFailReasonJson(failReasonJson);
        row.setTraceId(snap != null ? snap.getTraceId() : null);
        fillSnapshotSideMetrics(row, snap);
        row.setCreateTime(now);
        pushRecheckLogMapper.insert(row);

        // 先落 recheck 日志，再在同一事务里回写 push_snapshot.push_status，
        // 让手动触发与自动调度共用同一“状态跟随链”。
        String nextPushStatus = PushRecheckStatusContract.toPushStatus(status);
        pushSnapshotMapper.updatePushStatus(pushId, nextPushStatus);
        recordHighValueSafetyChange(snap, row, status, message, failReasonJson, now);

        return result;
    }

    private void recordHighValueSafetyChange(TmPushSnapshotDO snapshot,
                                             TmPushRecheckLogDO log,
                                             RecheckStatusEnum status,
                                             String message,
                                             String failReasonJson,
                                             LocalDateTime occurredAt) {
        HighValueAlertPolicy.SafetyChangeType changeType = safetyChangeType(status, failReasonJson);
        if (changeType == null || highValueAlertMessageService == null || analysisRunMapper == null
                || snapshot == null || snapshot.getAnalysisId() == null) return;
        AnalysisRunDO analysis = analysisRunMapper.selectById(snapshot.getAnalysisId());
        if (analysis == null || !"USER".equalsIgnoreCase(analysis.getOwnerType())
                || analysis.getOwnerId() == null || analysis.getOwnerId() <= 0) return;
        ExecutionPlanDO plan = executionPlanMapper == null
                ? null : executionPlanMapper.selectLatestByAnalysisId(snapshot.getAnalysisId());
        int severity = status == RecheckStatusEnum.CONFUSED_BLOCKED
                || status == RecheckStatusEnum.RISK_BLOCKED
                || status == RecheckStatusEnum.INVALIDATED ? 4 : 3;
        highValueAlertMessageService.recordSafetyChange(new HighValueAlertMessageService.SafetyChangeInput(
                analysis.getOwnerId(), changeType, "PUSH_RECHECK",
                log.getLogId() == null ? String.valueOf(snapshot.getPushId()) : String.valueOf(log.getLogId()),
                snapshot.getAnalysisId(), plan == null ? null : plan.getPlanId(),
                String.valueOf(snapshot.getPushId()), snapshot.getSymbol(), snapshot.getTraceId(),
                status.name(), severity, message,
                recoveryCondition(status), occurredAt, snapshot.getExpiresAt()));
    }

    static HighValueAlertPolicy.SafetyChangeType safetyChangeType(
            RecheckStatusEnum status, String failReasonJson) {
        if (status == null) return null;
        return switch (status) {
            case DRIFTED_FROM_ENTRY_ZONE, DRIFTED -> HighValueAlertPolicy.SafetyChangeType.EXECUTION_DRIFT;
            case RISK_BLOCKED -> HighValueAlertPolicy.SafetyChangeType.RISK_BLOCKED;
            case CONFUSED_BLOCKED -> HighValueAlertPolicy.SafetyChangeType.HIGH_CONFUSED;
            case EXPIRED -> HighValueAlertPolicy.SafetyChangeType.PLAN_EXPIRED;
            case INVALIDATED -> {
                String code = extractFailCode(failReasonJson);
                if (code != null && (code.contains("DATA_QUALITY") || code.contains("QUALITY"))) {
                    yield HighValueAlertPolicy.SafetyChangeType.DATA_QUALITY_BLOCKED;
                }
                if (code != null && (code.contains("SOURCE") || code.contains("QUOTE")
                        || code.contains("DERIVATIVES"))) {
                    yield HighValueAlertPolicy.SafetyChangeType.SOURCE_INVALID;
                }
                yield HighValueAlertPolicy.SafetyChangeType.FINAL_INVALIDATED;
            }
            case REVIEW_PASSED, REVIEW_WAITING -> null;
        };
    }

    static String recoveryCondition(RecheckStatusEnum status) {
        return switch (status) {
            case DRIFTED_FROM_ENTRY_ZONE, DRIFTED -> "价格与执行环境重新进入有效范围并再次通过复核";
            case RISK_BLOCKED -> "风险门禁恢复并由用户重新发起复核";
            case CONFUSED_BLOCKED -> "冲突降至允许范围并重新通过规则校验";
            case EXPIRED -> "生成新的可信分析和通过校验的最终计划";
            case INVALIDATED -> "重新分析并通过来源、风险和规则校验";
            case REVIEW_PASSED, REVIEW_WAITING -> "等待下一次有效复核";
        };
    }

    @Override
    public PushRecheckLogItemVO getLatestLog(Long pushId) {
        throw accessBoundary.disabledGlobalOperation(PushRecheckAccessBoundary.Operation.READ_LATEST);
    }

    @Override
    public List<PushRecheckLogItemVO> listLogs(Long pushId) {
        throw accessBoundary.disabledGlobalOperation(PushRecheckAccessBoundary.Operation.READ_LOGS);
    }

    @Override
    public List<RecheckResult> replayByDispatch(String dispatchBatchId, String dispatchInstructionId) {
        throw accessBoundary.disabledGlobalOperation(PushRecheckAccessBoundary.Operation.MUTATE_REPLAY);
    }

    @Override
    public PushRecheckReplaySummaryVO summarizeReplayByDispatch(String dispatchBatchId, String dispatchInstructionId) {
        throw accessBoundary.disabledGlobalOperation(PushRecheckAccessBoundary.Operation.READ_REPLAY_SUMMARY);
    }

    @Override
    public PushRecheckOpsOverviewVO getOpsOverview(String dispatchBatchId,
                                                   String dispatchInstructionId,
                                                   Integer auditLimit,
                                                   Integer logLimit) {
        throw accessBoundary.disabledGlobalOperation(PushRecheckAccessBoundary.Operation.READ_OPS);
    }

    private PushRecheckLogItemVO toLogVo(TmPushRecheckLogDO row) {
        PushRecheckLogItemVO vo = new PushRecheckLogItemVO();
        vo.setLogId(row.getLogId());
        vo.setPushId(row.getPushId());
        vo.setDispatchBatchId(row.getDispatchBatchId());
        vo.setDispatchInstructionId(row.getDispatchInstructionId());
        vo.setTriggerSource(row.getTriggerSource());
        vo.setRetryAttempt(row.getRetryAttempt());
        vo.setMaxAttempts(row.getMaxAttempts());
        vo.setRetryBackoffMinutes(row.getRetryBackoffMinutes());
        vo.setReplayFromLogId(row.getReplayFromLogId());
        vo.setExecutionStatus(row.getExecutionStatus());
        vo.setExecutionErrorCode(row.getExecutionErrorCode());
        vo.setExecutionErrorMessage(row.getExecutionErrorMessage());
        vo.setRecheckTime(row.getRecheckTime());
        vo.setRecheckStatus(PushRecheckStatusContract.canonicalizeRecheckStatusName(row.getRecheckStatus()));
        vo.setCurrentPrice(row.getCurrentPrice());
        vo.setPriceDriftRatio(row.getPriceDriftRatio());
        vo.setCurrentSlippageEstimation(row.getCurrentSlippageEstimation());
        vo.setCurrentDataQualityScore(row.getCurrentDataQualityScore());
        vo.setCurrentConfusedScore(row.getCurrentConfusedScore());
        vo.setCurrentAccountRiskAllowed(row.getCurrentAccountRiskAllowed());
        vo.setFailReasonJson(row.getFailReasonJson());
        vo.setTraceId(row.getTraceId());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    /** 从推送快照复用困惑分、数据质量分（Recheck 不重跑分析）。 */
    private static void fillSnapshotSideMetrics(TmPushRecheckLogDO row, TmPushSnapshotDO snap) {
        if (snap == null) {
            return;
        }
        row.setCurrentConfusedScore(snap.getConfusedScoreSnapshot());
        row.setCurrentDataQualityScore(snap.getDataQualityScoreSnapshot());
    }

    private PriceResolution resolveCurrentPrice(TmPushSnapshotDO snap) {
        String symbol = trimToNull(snap != null ? snap.getSymbol() : null);
        if (symbol == null) {
            return PriceResolution.failed(failJson("PRICE_REQUIRED", "snapshot symbol missing"));
        }
        if (marketPriceSnapshotService == null) {
            return PriceResolution.failed(failJson("QUOTE_UNAVAILABLE", "MarketPriceSnapshotService unavailable"));
        }
        try {
            ProviderCallResult<MarketPriceSnapshot> result = marketPriceSnapshotService.get(symbol,
                    AssetPriority.P1_WATCHLIST, Duration.ofSeconds(15), "push-recheck-" + UUID.randomUUID());
            if (!MarketPriceSnapshotPolicy.isFresh(result)) {
                String code = MarketPriceSnapshotPolicy.failureCode(result);
                return PriceResolution.failed(failJson(code, "snapshot unavailable for symbol=" + symbol));
            }
            return PriceResolution.success(result.payload().lastPrice());
        } catch (RuntimeException ex) {
            return PriceResolution.failed(failJson("QUOTE_UNAVAILABLE", ex.getMessage()));
        }
    }

    private RuleConfigContractService.PushRecheckThresholds resolvePushRecheckThresholds() {
        try {
            return ruleConfigContractService != null ? ruleConfigContractService.requirePushRecheckThresholds() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private DerivativesGuard validateDerivativesForRecheck(TmPushSnapshotDO snap) {
        if (snap == null || !requiresDerivatives(snap.getInvalidationConditionJson())) return null;
        if (derivativesSnapshotReadPort == null) {
            return DerivativesGuard.blocked("DERIVATIVES_UNAVAILABLE", "cached derivatives reader unavailable");
        }
        try {
            ProviderCallResult<DerivativesRiskSnapshot> result = derivativesSnapshotReadPort.readCached(
                    snap.getSymbol(), AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60),
                    snap.getTraceId() == null ? "push-recheck-derivatives" : snap.getTraceId());
            DerivativesRiskSnapshot payload = result == null ? null : result.payload();
            if (payload == null) {
                return DerivativesGuard.blocked("DERIVATIVES_UNAVAILABLE", "latest cached snapshot missing");
            }
            if (payload.sourceStatus() == UnifiedSourceStatus.STALE
                    || payload.freshnessStatus() == SnapshotFreshnessStatus.STALE_READABLE) {
                return DerivativesGuard.blocked("DERIVATIVES_STALE", "latest cached snapshot stale");
            }
            boolean oiReady = payload.openInterestUsd() != null
                    && (payload.openInterestChange5m() != null || payload.openInterestChange15m() != null);
            boolean fundingReady = payload.weightedFundingRate() != null;
            if (!oiReady || !fundingReady) {
                return DerivativesGuard.blocked("DERIVATIVES_REQUIRED",
                        "openInterestReady=" + oiReady + ",fundingReady=" + fundingReady);
            }
            if (payload.sourceStatus() != UnifiedSourceStatus.READY
                    || !"COMPLETE".equalsIgnoreCase(payload.evidenceAvailability())) {
                return DerivativesGuard.waiting("DERIVATIVES_PARTIAL", "latest cached snapshot partial");
            }
            return null;
        } catch (RuntimeException failure) {
            return DerivativesGuard.blocked("DERIVATIVES_UNAVAILABLE", "cached derivatives read failed");
        }
    }

    private static boolean requiresDerivatives(String invalidationJson) {
        if (invalidationJson == null || invalidationJson.isBlank()) return false;
        try {
            JsonNode root = JSON.readTree(invalidationJson);
            return root != null && root.path("derivativesRequired").asBoolean(false);
        } catch (Exception ignored) {
            return false;
        }
    }

    private record DerivativesGuard(RecheckStatusEnum status, String reasonCode, String message, String detail) {
        private static DerivativesGuard blocked(String reasonCode, String detail) {
            return new DerivativesGuard(RecheckStatusEnum.INVALIDATED, reasonCode,
                    "衍生品确认数据不可用，仅供人工复核", detail);
        }

        private static DerivativesGuard waiting(String reasonCode, String detail) {
            return new DerivativesGuard(RecheckStatusEnum.REVIEW_WAITING, reasonCode,
                    "衍生品确认数据不完整，等待人工复核", detail);
        }
    }

    private static RecheckStatusEnum classifyPostPriceChecks(TmPushSnapshotDO snap,
                                                             Boolean currentAccountRiskAllowed,
                                                             RuleConfigContractService.PushRecheckThresholds thresholds) {
        if (Boolean.FALSE.equals(currentAccountRiskAllowed)) {
            return RecheckStatusEnum.RISK_BLOCKED;
        }
        Integer confused = snap.getConfusedScoreSnapshot();
        if (confused != null && confused >= thresholds.getConfusedBlockThreshold()) {
            return RecheckStatusEnum.CONFUSED_BLOCKED;
        }
        if (confused != null && confused >= thresholds.getConfusedWaitThreshold()) {
            return RecheckStatusEnum.REVIEW_WAITING;
        }
        Integer execFeas = snap.getExecutionFeasibilitySnapshot();
        if (execFeas != null && execFeas < thresholds.getExecutionFeasibilityWaitThreshold()) {
            return RecheckStatusEnum.REVIEW_WAITING;
        }
        return RecheckStatusEnum.REVIEW_PASSED;
    }

    private static String statusMessage(RecheckStatusEnum status, Boolean currentAccountRiskAllowed) {
        if (status == RecheckStatusEnum.RISK_BLOCKED) {
            return "账户或持仓风险阻断，仅供人工复核";
        }
        if (status == RecheckStatusEnum.CONFUSED_BLOCKED) {
            return "困惑度过高，方向性结论已阻断";
        }
        if (status == RecheckStatusEnum.REVIEW_WAITING) {
            if (currentAccountRiskAllowed == null) {
                return "复查结果建议等待，仅供人工复核";
            }
            return "复查结果建议等待，仅供人工复核";
        }
        return "复查条件通过，仅供人工复核，不是交易指令";
    }

    private static String failReasonForStatus(RecheckStatusEnum status,
                                              Boolean currentAccountRiskAllowed,
                                              TmPushSnapshotDO snap,
                                              TmAccountRiskSnapshotDO accountRiskSnapshot,
                                              UserPositionRiskResult userPositionRiskResult,
                                              RuleConfigContractService.PushRecheckThresholds thresholds) {
        if (status == RecheckStatusEnum.RISK_BLOCKED) {
            if (userPositionRiskResult != null && userPositionRiskResult.isRiskBlocked()) {
                String detail = "riskStatus=" + userPositionRiskResult.getRiskStatus()
                        + ",riskLevel=" + userPositionRiskResult.getRiskLevel()
                        + ",aggregateRiskScore=" + userPositionRiskResult.getAggregateRiskScore()
                        + ",reasonCodes=" + userPositionRiskResult.getReasonCodes()
                        + ",calculationMethod=" + userPositionRiskResult.getCalculationMethod();
                return failJson("RISK_BLOCKED", detail);
            }
            if (accountRiskSnapshot != null) {
                String detail = "riskReasonCode=" + accountRiskSnapshot.getRiskReasonCode()
                        + ",riskReasonText=" + accountRiskSnapshot.getRiskReasonText()
                        + ",positionExposure=" + accountRiskSnapshot.getPositionExposure()
                        + ",maxAllowedExposure=" + accountRiskSnapshot.getMaxAllowedExposure()
                        + ",snapshotSource=" + accountRiskSnapshot.getSnapshotSource()
                        + ",snapshotVersion=" + accountRiskSnapshot.getSnapshotVersion();
                return failJson("RISK_BLOCKED", detail);
            }
            return failJson("RISK_BLOCKED", "currentAccountRiskAllowed=false");
        }
        if (status == RecheckStatusEnum.CONFUSED_BLOCKED) {
            return failJson("CONFUSED_BLOCKED", "confusedScoreSnapshot=" + snap.getConfusedScoreSnapshot()
                    + ",threshold=" + thresholds.getConfusedBlockThreshold());
        }
        if (status == RecheckStatusEnum.REVIEW_WAITING && currentAccountRiskAllowed == null) {
            return failJson("RISK_UNKNOWN_WAIT", "currentAccountRiskAllowed=null");
        }
        return null;
    }

    private UserPositionRiskResult resolveUserPositionRiskResult() {
        try {
            UserPositionRiskResult result = userPositionRiskAdapter.currentRiskForSystem();
            return result != null ? result : UserPositionRiskResult.failClosed("USER_POSITION_RISK_UNAVAILABLE");
        } catch (RuntimeException ex) {
            return UserPositionRiskResult.failClosed("USER_POSITION_RISK_UNAVAILABLE");
        }
    }

    private static Boolean combineRiskAllowance(Boolean accountRiskAllowed,
                                                UserPositionRiskResult userPositionRiskResult) {
        if (userPositionRiskResult != null && userPositionRiskResult.isRiskBlocked()) {
            return Boolean.FALSE;
        }
        if (accountRiskAllowed != null) {
            return accountRiskAllowed;
        }
        return userPositionRiskResult == null ? null : Boolean.TRUE;
    }

    private TmAccountRiskSnapshotDO resolveAccountRiskSnapshot(TmPushSnapshotDO snap) {
        if (snap == null || snap.getAccountRiskSnapshotId() == null) {
            return null;
        }
        return accountRiskSnapshotMapper.selectById(snap.getAccountRiskSnapshotId());
    }

    /**
     * 解析 {@code invalidation_condition_json}：支持与本机写入格式兼容的 {@code {"text":"..."}}（不自动判失效），
     * 以及可选数值字段 {@code invalidPriceBelow} / {@code invalidPriceAbove}（命中则失效）。
     *
     * @return 非 null 表示命中失效条件，字符串为简要说明
     */
    private static String evaluateStructuredInvalidation(String invalidationJson, BigDecimal price) {
        if (invalidationJson == null || invalidationJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = JSON.readTree(invalidationJson);
            if (root == null || !root.isObject()) {
                return null;
            }
            JsonNode below = root.get("invalidPriceBelow");
            if (below != null && below.isNumber()) {
                BigDecimal v = below.decimalValue();
                if (price.compareTo(v) < 0) {
                    return "price " + price + " < invalidPriceBelow " + v;
                }
            }
            JsonNode above = root.get("invalidPriceAbove");
            if (above != null && above.isNumber()) {
                BigDecimal v = above.decimalValue();
                if (price.compareTo(v) > 0) {
                    return "price " + price + " > invalidPriceAbove " + v;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String failJson(String code, String detail) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", code);
            m.put("detail", detail);
            return JSON.writeValueAsString(m);
        } catch (Exception e) {
            return "{\"code\":\"" + code + "\"}";
        }
    }

    private record PriceResolution(BigDecimal currentPrice, String failReasonJson) {
        private static PriceResolution success(BigDecimal currentPrice) {
            return new PriceResolution(currentPrice, null);
        }

        private static PriceResolution failed(String failReasonJson) {
            return new PriceResolution(null, failReasonJson);
        }
    }

    private static String extractFailCode(String failReasonJson) {
        if (failReasonJson == null || failReasonJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = JSON.readTree(failReasonJson);
            JsonNode code = root.get("code");
            return code != null && !code.isNull() ? code.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private PushRecheckOpsOverviewVO.ConfigSummary buildConfigSummary(int auditLimit) {
        Map<String, Integer> current = dispatchConfigService.getCurrentConfig();
        PushRecheckOpsOverviewVO.ConfigSummary config = new PushRecheckOpsOverviewVO.ConfigSummary();
        config.setLimit(current.get("limit"));
        config.setMaxAttempts(current.get("maxAttempts"));
        config.setMinRetryMinutes(current.get("minRetryMinutes"));

        Optional.ofNullable(dispatchConfigService.listRecentAudit(auditLimit)).flatMap(rows -> rows.stream().findFirst())
                .ifPresent(latest -> {
                    config.setUpdatedBy(trimToNull(latest.getChangedBy()));
                    config.setUpdatedTime(latest.getCreateTime());
                });
        return config;
    }

    private PushRecheckOpsOverviewVO.AuditSummary buildAuditSummary(int auditLimit) {
        List<org.example.trademodel.entity.PushRecheckDispatchConfigAuditDO> audits =
                dispatchConfigService.listRecentAudit(auditLimit);
        PushRecheckOpsOverviewVO.AuditSummary summary = new PushRecheckOpsOverviewVO.AuditSummary();
        int count = audits == null ? 0 : audits.size();
        summary.setAuditCount(count);
        if (count > 0) {
            org.example.trademodel.entity.PushRecheckDispatchConfigAuditDO latest = audits.get(0);
            summary.setLatestAuditTime(latest.getCreateTime());
            summary.setLatestAuditOperator(trimToNull(latest.getChangedBy()));
            summary.setLatestAuditSummary(buildAuditSummaryText(latest));
        }
        return summary;
    }

    private static String buildAuditSummaryText(org.example.trademodel.entity.PushRecheckDispatchConfigAuditDO latest) {
        String operator = trimToNull(latest.getChangedBy());
        if (operator == null) {
            operator = "unknown";
        }
        return operator + " changed " + latest.getConfigKey() + " from "
                + latest.getOldValue() + " to " + latest.getNewValue();
    }

    private PushRecheckOpsOverviewVO.RecentLogSummary toRecentLogSummary(TmPushRecheckLogDO row) {
        PushRecheckOpsOverviewVO.RecentLogSummary summary = new PushRecheckOpsOverviewVO.RecentLogSummary();
        summary.setLogId(row.getLogId());
        summary.setDispatchBatchId(row.getDispatchBatchId());
        summary.setDispatchInstructionId(row.getDispatchInstructionId());
        summary.setTriggerSource(row.getTriggerSource());
        summary.setExecutionStatus(row.getExecutionStatus());
        summary.setExecutionErrorCode(row.getExecutionErrorCode());
        summary.setCreateTime(row.getCreateTime());
        return summary;
    }

    private static int safeLimit(Integer candidate, int fallback, int max) {
        if (candidate == null || candidate <= 0) {
            return fallback;
        }
        return Math.min(candidate, max);
    }

    private List<TmPushRecheckLogDO> selectReplaySource(String dispatchBatchId, String dispatchInstructionId) {
        if (dispatchInstructionId != null && !dispatchInstructionId.isBlank()) {
            return pushRecheckLogMapper.selectByInstructionId(dispatchInstructionId.trim());
        }
        if (dispatchBatchId != null && !dispatchBatchId.isBlank()) {
            return pushRecheckLogMapper.selectByBatchId(dispatchBatchId.trim());
        }
        return Collections.emptyList();
    }

    private static String replayInstructionId(TmPushRecheckLogDO row, String fallbackInstructionId) {
        String original = row.getDispatchInstructionId();
        if (original != null && !original.isBlank()) {
            return "REPLAY-" + original;
        }
        if (fallbackInstructionId != null && !fallbackInstructionId.isBlank()) {
            return "REPLAY-" + fallbackInstructionId.trim();
        }
        return "REPLAY-LOG-" + row.getLogId();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }

    private static String blankToNull(String value) {
        return trimToNull(value);
    }

}
