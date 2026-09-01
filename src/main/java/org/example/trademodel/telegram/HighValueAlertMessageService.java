package org.example.trademodel.telegram;

import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.service.MessageFactService;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class HighValueAlertMessageService {
    private static final Set<String> MANUAL_POSITION_SOURCES = Set.of(
            "MANUAL_INDEPENDENT", "MANUAL_POSITION", "SYSTEM_PLAN_POSITION", "MANUAL");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private final MessageFactService messageFactService;
    private final AssetPoolService assetPoolService;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final FundamentalAiV41Properties v41Properties;
    private final TelegramProperties telegramProperties;
    private final HighValueAlertPolicy policy;
    private final Clock clock;

    @Autowired
    public HighValueAlertMessageService(MessageFactService messageFactService,
                                        AssetPoolService assetPoolService,
                                        PushSnapshotMapper pushSnapshotMapper,
                                        ExecutionPlanMapper executionPlanMapper,
                                        FundamentalAiV41Properties v41Properties,
                                        TelegramProperties telegramProperties,
                                        HighValueAlertPolicy policy) {
        this(messageFactService, assetPoolService, pushSnapshotMapper, executionPlanMapper,
                v41Properties, telegramProperties, policy, Clock.systemUTC());
    }

    HighValueAlertMessageService(MessageFactService messageFactService,
                                 AssetPoolService assetPoolService,
                                 PushSnapshotMapper pushSnapshotMapper,
                                 ExecutionPlanMapper executionPlanMapper,
                                 FundamentalAiV41Properties v41Properties,
                                 TelegramProperties telegramProperties,
                                 HighValueAlertPolicy policy,
                                 Clock clock) {
        this.messageFactService = messageFactService;
        this.assetPoolService = assetPoolService;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.v41Properties = v41Properties;
        this.telegramProperties = telegramProperties;
        this.policy = policy;
        this.clock = clock;
    }

    public MessageDO recordOpportunity(AnalysisRunDO run,
                                       DecisionResult decision,
                                       ExecutionPlanDO plan,
                                       OpportunityTransitionResult opportunity,
                                       OpportunityLogDTO persistedLog) {
        if (!isUserOwned(run)) return null;
        LocalDateTime now = LocalDateTime.now(clock);
        TmPushSnapshotDO snapshot = latestSnapshot(run.getAnalysisId());
        if (snapshot != null && (snapshot.getPushId() == null || snapshot.getExpiresAt() == null
                || !now.isBefore(snapshot.getExpiresAt()))) snapshot = null;
        LocalDateTime effectiveExpiry = plan != null && plan.getValidUntil() != null
                ? plan.getValidUntil() : snapshot == null ? null : snapshot.getExpiresAt();
        String target = plan == null ? null : firstText(plan.getTakeProfitRules(), plan.getTargetLogic());
        boolean traceable = hasText(run.getAnalysisId()) && plan != null && hasText(plan.getPlanId())
                && opportunity != null && hasText(opportunity.opportunityId())
                && persistedLog != null && hasText(persistedLog.getOpportunityId())
                && Objects.equals(run.getAnalysisId(), plan.getAnalysisId())
                && Objects.equals(opportunity.opportunityId(), persistedLog.getOpportunityId())
                && Objects.equals(opportunity.opportunityId(), plan.getOpportunityId())
                && hasText(run.getTraceId()) && Objects.equals(run.getTraceId(), plan.getTraceId())
                && hasText(run.getSymbol());
        boolean sourceGate = plan != null && Boolean.TRUE.equals(plan.getSourceGateComplete())
                && accepted(plan.getSourceGateStatus(), "PASS", "VALID", "COMPLETE", "ALLOWED", "READY")
                && accepted(plan.getSourceStatus(), "PASS", "VALID", "VERIFIED", "COMPLETE", "READY");
        boolean feasibility = plan != null && accepted(plan.getExecutionFeasibilityStatus(),
                "PASS", "VALID", "VERIFIED", "ALLOWED", "READY");
        boolean fresh = plan != null && plan.getExecutionFeasibilityFreshUntil() != null
                && now.isBefore(plan.getExecutionFeasibilityFreshUntil());
        boolean expired = effectiveExpiry == null || !now.isBefore(effectiveExpiry);
        HighValueAlertPolicy.OpportunityQualification qualification =
                new HighValueAlertPolicy.OpportunityQualification(
                        run.getOwnerId(),
                        assetPoolService.isOpportunitySource(run.getOwnerType(), run.getOwnerId(),
                                run.getAssetId(), run.getSymbol()),
                        persistedLog != null && hasText(persistedLog.getOpportunityId()),
                        plan != null && Boolean.TRUE.equals(plan.getFinalPlan()),
                        plan != null && "PASS".equals(normalize(plan.getRuleValidationStatus()))
                                && "FINAL_VALIDATED".equals(normalize(plan.getChainStatus())),
                        plan == null ? null : firstText(plan.getFinalPlanMode(), plan.getPlanMode()),
                        plan == null ? null : plan.getFinalMarketBias(),
                        decision == null ? null : decision.getRiskLevel(),
                        plan == null ? null : plan.getPlanLifecycleState(),
                        opportunity == null || opportunity.state() == null ? null : opportunity.state().name(),
                        expired,
                        plan != null && plan.getDataQuality() != null
                                && plan.getDataQuality() >= v41Properties.getAiGate().getMinimumDataQuality(),
                        fresh,
                        sourceGate,
                        feasibility,
                        traceable,
                        plan != null && hasText(plan.getEntryZone()),
                        plan != null && hasText(plan.getTriggerCondition()),
                        plan != null && hasText(plan.getStopLoss()),
                        hasText(target),
                        effectiveExpiry != null,
                        plan != null && hasText(plan.getFinalMarketBias()),
                        Boolean.TRUE.equals(run.getPreview()),
                        plan == null || !Boolean.TRUE.equals(plan.getFinalPlan()),
                        plan != null && Boolean.TRUE.equals(plan.getNotTradeInstruction()),
                        plan != null && Boolean.TRUE.equals(plan.getNotOrderExecution()));
        if (!policy.allowsOpportunity(qualification)) return null;

        MessageDO existingPlanMessage = messageFactService.findOpportunityForPlan(
                run.getOwnerId(), plan.getPlanId());
        if (existingPlanMessage != null) return existingPlanMessage;

        String sourceType = snapshot == null ? "FINAL_PLAN" : "PUSH_SNAPSHOT";
        String sourceId = snapshot == null ? plan.getPlanId() : String.valueOf(snapshot.getPushId());
        MessageDO message = base(run.getOwnerId(), "HIGH_PERMISSION_OPPORTUNITY", sourceType,
                sourceId, run.getAnalysisId(), null, plan.getPlanId(),
                run.getSymbol(), run.getTraceId(), effectiveExpiry);
        message.setCurrentRecheckId(null);
        message.setTitle(HighValueAlertPolicy.OPPORTUNITY_SHORT_TITLE);
        String finalMode = firstText(plan.getFinalPlanMode(), plan.getPlanMode());
        message.setBody(run.getSymbol().trim() + "  ·  " + readableBias(plan.getFinalMarketBias())
                + "  ·  " + readablePlanMode(finalMode)
                + "\n\n入场：" + plan.getEntryZone().trim()
                + "\n触发：" + plan.getTriggerCondition().trim()
                + "\n止损：" + plan.getStopLoss().trim()
                + "\n目标：" + target.trim()
                + "\n有效至：" + format(effectiveExpiry)
                + "\n\n操作：打开系统重新校验");
        message.setDedupeKey(TelegramDedupeKey.createPlanLifetime(HighValueAlertPolicy.OPPORTUNITY_EVENT,
                normalize(finalMode), 3,
                run.getOwnerId(), "FINAL_PLAN", plan.getPlanId()));
        return messageFactService.recordIfAbsent(message);
    }

    public MessageDO recordSafetyChange(SafetyChangeInput input) {
        if (input == null || !policy.allowsSafetyChange(new HighValueAlertPolicy.SafetyQualification(
                input.userId(), input.changeType(), input.traceable(), true, true))) return null;
        LocalDateTime now = input.occurredAt() == null ? LocalDateTime.now(clock) : input.occurredAt();
        String subjectType = hasText(input.planId()) ? "FINAL_PLAN" : "OPPORTUNITY";
        String subjectId = hasText(input.planId()) ? input.planId() : input.opportunityId();
        if (!hasText(subjectId)) return null;
        MessageDO message = base(input.userId(), "OPPORTUNITY_PLAN_SAFETY_CHANGE",
                subjectType, subjectId, input.analysisId(), null,
                input.planId(), input.symbol(), input.traceId(), input.expiresAt());
        message.setCurrentRecheckId(null);
        message.setTitle(HighValueAlertPolicy.SAFETY_SHORT_TITLE);
        message.setBody("资产：" + safe(input.symbol())
                + "\n变化：" + readableSafetyChange(input.changeType())
                + "\n原因：" + concise(input.reason())
                + "\n当前状态：暂不视为有效机会"
                + "\n恢复条件：" + concise(input.recoveryCondition()));
        message.setDedupeKey(TelegramDedupeKey.create(HighValueAlertPolicy.SAFETY_EVENT,
                input.changeType().name(), Math.max(2, input.severity()), telegramProperties.getCooldownMinutes(),
                input.userId(), subjectType, subjectId, now));
        return messageFactService.recordIfAbsent(message);
    }

    public MessageDO recordPosition(UserPositionDO position,
                                    PositionMonitorLogDTO log,
                                    PositionMonitorResultDTO result) {
        if (position == null || log == null || result == null || log.getLogId() == null) return null;
        LocalDateTime now = LocalDateTime.now(clock);
        boolean active = Set.of("OPEN", "PARTIALLY_CLOSED").contains(normalize(position.getStatus()))
                && MANUAL_POSITION_SOURCES.contains(normalize(position.getSourceType()))
                && Set.of("LONG", "SHORT").contains(normalize(position.getSide()))
                && hasText(position.getAssetSymbol());
        boolean verified = "VERIFIED".equals(normalize(log.getMonitorSourceStatus()));
        boolean fresh = log.isTrustedAndFreshAt(now);
        HighValueAlertPolicy.PositionQualification messageQualification =
                new HighValueAlertPolicy.PositionQualification(
                        position.getUserId(), active, verified, fresh,
                        log.getEntryLogicStatus(), log.getReversalStatus(), log.getRiskLevel(),
                        log.getRiskTrend(), log.getMonitorConclusion());
        if (!policy.allowsPositionMessage(messageQualification)) return null;

        BigDecimal currentPrice = trustedCurrentPrice(result, log);
        boolean sameMonitorResult = sameMonitorResult(position, log, result);
        HighValueAlertPolicy.PositionTelegramQualification telegramQualification =
                new HighValueAlertPolicy.PositionTelegramQualification(
                        position.getUserId(), active, verified, fresh, sameMonitorResult,
                        result.isMarkPriceFresh() && result.getMarkPriceObservedAt() != null
                                && hasText(result.getMarkPriceSource())
                                && positive(currentPrice),
                        positive(position.getEntryPrice()), positive(position.getStopLoss()),
                        positive(position.getTakeProfit()), result.isNearStopLoss(),
                        result.isStopLossBreached(), result.isNearTakeProfit(),
                        result.isTakeProfitReached(), result.getRiskLevel(), result.getRiskTrend(),
                        result.getReversalStatus(), result.isNotTradeInstruction(),
                        result.isNotOrderExecution());
        Optional<HighValueAlertPolicy.PositionTelegramChange> telegramChange =
                policy.resolveTelegramPositionChange(telegramQualification);

        MessageDO message = base(position.getUserId(), "POSITION_LOGIC_RISK_CHANGE",
                "POSITION_MONITOR", String.valueOf(log.getLogId()), log.getAnalysisId(),
                position.getId(), position.getFinalPlanId(), position.getAssetSymbol(),
                log.getTraceId(), log.getFreshUntil());
        if (telegramChange.isPresent()) {
            HighValueAlertPolicy.PositionTelegramChange change = telegramChange.get();
            message.setTitle(HighValueAlertPolicy.POSITION_SHORT_TITLE);
            message.setBody(position.getAssetSymbol().trim() + "  ·  " + readableDirection(position.getSide())
                    + "\n\n变化：" + change.displayText()
                    + "\n\n入场：" + decimal(position.getEntryPrice())
                    + "\n现价：" + decimal(currentPrice)
                    + "\n止损：" + decimalOrUnset(position.getStopLoss())
                    + "  目标：" + decimalOrUnset(position.getTakeProfit())
                    + "\n\n操作：打开持仓详情");
            message.setDedupeKey(TelegramDedupeKey.create(HighValueAlertPolicy.POSITION_EVENT,
                    change.name(), change.severityRank(), telegramProperties.getCooldownMinutes(),
                    position.getUserId(), "USER_POSITION", String.valueOf(position.getId()), now));
        } else {
            ExecutionPlanDO sourcePlan = hasText(position.getFinalPlanId())
                    ? executionPlanMapper.selectByPlanId(position.getFinalPlanId()) : null;
            message.setTitle("【持仓逻辑发生重要变化】");
            message.setBody("资产：" + safe(position.getAssetSymbol())
                    + "\n当前变化：" + readableEntryLogic(log.getEntryLogicStatus())
                    + "\n原入场逻辑：" + concise(sourcePlan == null
                            ? "手动持仓未关联系统计划，请核对原始录入依据" : sourcePlan.getEntryLogic())
                    + "\n反转状态：" + readableReversal(log.getReversalStatus())
                    + "\n风险：" + readableRisk(log.getRiskLevel(), log.getRiskTrend())
                    + "\n主要原因：" + readableRiskReason(log.getRiskChangeReason())
                    + "\n最近监控：" + format(log.getCreatedAt())
                    + "\n建议：打开持仓详情人工处理");
            message.setDedupeKey(TelegramDedupeKey.create(HighValueAlertPolicy.POSITION_EVENT,
                    strongestPositionState(log), positionSeverity(log), telegramProperties.getCooldownMinutes(),
                    position.getUserId(), "USER_POSITION", String.valueOf(position.getId()), now));
        }
        return messageFactService.recordIfAbsent(message);
    }

    private MessageDO base(Long userId, String category, String sourceType, String sourceId,
                           String analysisId, Long positionId, String planId, String symbol,
                           String traceId, LocalDateTime expiresAt) {
        MessageDO message = new MessageDO();
        message.setUserId(userId);
        message.setCategory(category);
        message.setSourceType(sourceType);
        message.setSourceId(sourceId);
        message.setAnalysisId(analysisId);
        message.setPositionId(positionId);
        message.setPlanId(planId);
        message.setSymbol(symbol);
        message.setTraceId(traceId);
        message.setExpiresAt(expiresAt);
        message.setNotTradeInstruction(true);
        message.setNotOrderExecution(true);
        return message;
    }

    private TmPushSnapshotDO latestSnapshot(String analysisId) {
        if (!hasText(analysisId)) return null;
        List<TmPushSnapshotDO> values = pushSnapshotMapper.listByAnalysisId(analysisId);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static BigDecimal trustedCurrentPrice(PositionMonitorResultDTO result, PositionMonitorLogDTO log) {
        if (result == null || !result.isMarkPriceFresh()) return null;
        if (positive(result.getMarkPrice())) return result.getMarkPrice();
        if (positive(result.getCurrentPrice())) return result.getCurrentPrice();
        return positive(log == null ? null : log.getCurrentPrice()) ? log.getCurrentPrice() : null;
    }

    private static boolean sameMonitorResult(UserPositionDO position,
                                             PositionMonitorLogDTO log,
                                             PositionMonitorResultDTO result) {
        if (position.getId() == null || log.getLogId() == null
                || !Objects.equals(position.getId(), log.getPositionId())
                || !Objects.equals(position.getId(), result.getPositionId())
                || !Objects.equals(log.getLogId(), result.getMonitorLogId())) return false;
        if (!sameSemantic(log.getRiskLevel(), result.getRiskLevel())
                || !sameSemantic(log.getRiskTrend(), result.getRiskTrend())
                || !sameSemantic(log.getReversalStatus(), result.getReversalStatus())) return false;
        BigDecimal logPrice = log.getCurrentPrice();
        BigDecimal resultPrice = result.getMarkPrice() != null ? result.getMarkPrice() : result.getCurrentPrice();
        return logPrice == null || resultPrice == null || logPrice.compareTo(resultPrice) == 0;
    }

    private static boolean sameSemantic(String left, String right) {
        return hasText(left) && normalize(left).equals(normalize(right));
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String decimalOrUnset(BigDecimal value) {
        return positive(value) ? decimal(value) : "未设置";
    }

    private static int positionSeverity(PositionMonitorLogDTO log) {
        if ("EXTREME".equals(normalize(log.getRiskLevel()))
                || "INVALIDATED".equals(normalize(log.getEntryLogicStatus()))
                || "STRONG_REVERSAL".equals(normalize(log.getReversalStatus()))) return 4;
        if ("HIGH".equals(normalize(log.getRiskLevel()))
                || "SHARPLY_INCREASED".equals(normalize(log.getRiskTrend()))) return 3;
        return 2;
    }

    private static String strongestPositionState(PositionMonitorLogDTO log) {
        if ("INVALIDATED".equals(normalize(log.getEntryLogicStatus()))) return "INVALIDATED";
        if ("STRONG_REVERSAL".equals(normalize(log.getReversalStatus()))) return "STRONG_REVERSAL";
        if ("EXTREME".equals(normalize(log.getRiskLevel()))) return "EXTREME";
        if ("HIGH".equals(normalize(log.getRiskLevel()))) return "HIGH";
        if (hasText(log.getMonitorConclusion())) return normalize(log.getMonitorConclusion());
        return normalize(log.getEntryLogicStatus());
    }

    private static boolean isUserOwned(AnalysisRunDO run) {
        return run != null && "USER".equals(normalize(run.getOwnerType()))
                && run.getOwnerId() != null && run.getOwnerId() > 0;
    }

    private static boolean accepted(String value, String... accepted) {
        String normalized = normalize(value);
        for (String item : accepted) if (item.equals(normalized)) return true;
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private static String required(String value) {
        if (!hasText(value)) throw new IllegalArgumentException("alert source identity is required");
        return value.trim();
    }
    private static String firstText(String first, String second) { return hasText(first) ? first : second; }
    private static String safe(String value) { return hasText(value) ? value.trim() : "当前不可查看"; }
    private static String concise(String value) {
        if (!hasText(value)) return "暂无可验证摘要，请回到系统查看";
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }
    private static String format(LocalDateTime value) { return value == null ? "当前不可查看" : DATE_TIME.format(value); }
    private static String readableBias(String value) {
        return switch (normalize(value)) {
            case "STRONG_BULLISH", "STRONG_LONG" -> "强偏多";
            case "BULLISH", "LONG" -> "偏多";
            case "WEAK_BULLISH", "WEAK_LONG" -> "弱偏多";
            case "RANGE", "NEUTRAL" -> "震荡";
            case "WEAK_BEARISH", "WEAK_SHORT" -> "弱偏空";
            case "BEARISH", "SHORT" -> "偏空";
            case "STRONG_BEARISH", "STRONG_SHORT" -> "强偏空";
            case "WAIT", "CONFUSED" -> "观望";
            default -> "当前不可查看";
        };
    }
    private static String readableDirection(String value) {
        return switch (normalize(value)) {
            case "LONG" -> "做多";
            case "SHORT" -> "做空";
            default -> throw new IllegalArgumentException("position direction is required");
        };
    }
    private static String readablePlanMode(String value) {
        return switch (normalize(value)) {
            case "CONFIRMATION" -> "确认型"; case "REDUCED" -> "缩减型";
            case "PREPARATION" -> "预备型"; case "OBSERVATION" -> "观察";
            case "BLOCKED" -> "禁止"; default -> "当前不可查看";
        };
    }
    private static String readableOpportunityState(String value) {
        return switch (normalize(value)) {
            case "TRIGGERED" -> "已触发"; case "CANDIDATE" -> "候选";
            case "WAITING_TRIGGER" -> "等待触发"; default -> "当前状态已记录";
        };
    }
    private static String readableSafetyChange(HighValueAlertPolicy.SafetyChangeType value) {
        return switch (value) {
            case CONFUSED, HIGH_CONFUSED -> "证据冲突，方向性建议暂停";
            case LIQUIDITY_TRAP -> "检测到流动性陷阱";
            case HOT_RESET -> "触发紧急重置";
            case FINAL_INVALIDATED -> "最终计划已失效";
            case RISK_BLOCKED -> "最终计划被风险门禁阻断";
            case EXECUTION_DRIFT -> "价格或执行环境显著漂移";
            case PLAN_EXPIRED -> "最终计划已过期";
            case DATA_QUALITY_BLOCKED -> "数据质量门禁阻断";
            case SOURCE_INVALID -> "可信来源门禁失效";
            case NEEDS_REVALIDATION -> "最终计划需要重新验证";
        };
    }
    private static String readableEntryLogic(String value) {
        return switch (normalize(value)) {
            case "WEAKENED" -> "入场逻辑弱化"; case "INVALIDATED" -> "入场逻辑失效";
            case "STILL_VALID" -> "入场逻辑仍成立";
            case "NOT_APPLICABLE" -> "原入场逻辑不适用";
            default -> "当前变化待人工核对";
        };
    }
    private static String readableReversal(String value) {
        return switch (normalize(value)) {
            case "NO_REVERSAL" -> "无明显反转"; case "WEAK_REVERSAL" -> "弱反转";
            case "STRONG_REVERSAL" -> "强反转"; default -> "当前不可查看";
        };
    }
    private static String readableRisk(String level, String trend) {
        String levelText = switch (normalize(level)) {
            case "LOW" -> "低"; case "MEDIUM" -> "中"; case "HIGH" -> "高";
            case "EXTREME" -> "极高"; default -> "当前不可查看";
        };
        String trendText = switch (normalize(trend)) {
            case "INCREASED" -> "，正在升级"; case "SHARPLY_INCREASED" -> "，快速升级";
            case "STABLE" -> "，稳定"; default -> "";
        };
        return levelText + trendText;
    }
    private static String readableRiskReason(String value) {
        return switch (normalize(value)) {
            case "NO_CLEAR_RISK_FACTOR" -> "暂无明显风险因素";
            case "OPPOSING_EVIDENCE_INCREASED" -> "反向证据增加";
            case "STRUCTURE_CHANGED" -> "市场结构发生变化";
            case "EVENT_IMPACT" -> "外部事件冲击";
            case "DATA_QUALITY_DEGRADED" -> "数据质量下降";
            default -> "暂无可验证风险原因";
        };
    }

    public record SafetyChangeInput(Long userId,
                                    HighValueAlertPolicy.SafetyChangeType changeType,
                                    String sourceType,
                                    String sourceId,
                                    String analysisId,
                                    String planId,
                                    String opportunityId,
                                    String pushSnapshotId,
                                    String symbol,
                                    String traceId,
                                    String state,
                                    int severity,
                                    String reason,
                                    String recoveryCondition,
                                    LocalDateTime occurredAt,
                                    LocalDateTime expiresAt) {
        boolean traceable() {
            return userId != null && userId > 0 && hasText(sourceType) && hasText(sourceId)
                    && hasText(symbol) && hasText(traceId);
        }
    }
}
