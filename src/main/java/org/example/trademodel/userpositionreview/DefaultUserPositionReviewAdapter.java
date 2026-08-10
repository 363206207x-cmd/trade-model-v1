package org.example.trademodel.userpositionreview;

import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.positionmonitor.PositionPlanSourceResolver;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogSourceViewPolicy;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.vo.ReviewStateVO;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class DefaultUserPositionReviewAdapter implements UserPositionReviewAdapter {
    private static final String PLAN_CONTEXT_FOUND = "PLAN_CONTEXT_FOUND";
    private static final String PLAN_CONTEXT_MISSING = "PLAN_CONTEXT_MISSING";
    private static final String NOT_COMPUTABLE = "NOT_COMPUTABLE";
    private static final String ALIGNED = "ALIGNED";
    private static final String DEVIATED = "DEVIATED";

    private final UserPositionMapper userPositionMapper;
    private final PositionPlanSourceResolver positionPlanSourceResolver;
    private final PositionMonitorLogService positionMonitorLogService;
    private final ReviewService reviewService;

    public DefaultUserPositionReviewAdapter(UserPositionMapper userPositionMapper,
                                            ExecutionPlanMapper executionPlanMapper,
                                            AnalysisRunMapper analysisRunMapper,
                                            PositionMonitorLogService positionMonitorLogService,
                                            ReviewService reviewService) {
        this.userPositionMapper = userPositionMapper;
        this.positionPlanSourceResolver = new PositionPlanSourceResolver(executionPlanMapper, analysisRunMapper);
        this.positionMonitorLogService = positionMonitorLogService;
        this.reviewService = reviewService;
    }

    @Override
    public UserPositionReviewSummaryDTO buildSummaryForUser(Long userId, Long positionId) {
        UserPositionDO position = requireClosedPosition(userId, positionId);
        ResolvedPositionReviewContext context = resolveReviewContext(userId, position);
        PositionPlanSourceResolver.Resolution planSource = context.planSource();
        ExecutionPlanDO plan = context.executionPlan();

        UserPositionReviewSummaryDTO summary = new UserPositionReviewSummaryDTO();
        fillPosition(summary, position);
        fillPlan(summary, position, planSource);
        fillPnl(summary, position);
        fillExecutionDeviation(summary, position, plan);
        fillMonitorFacts(summary, position, context.monitorLogs());
        summary.setReviewStatus("REVIEW_SUMMARY_READY");
        if (PLAN_CONTEXT_MISSING.equals(summary.getPlanContextStatus())) {
            summary.getReviewReasons().add(PLAN_CONTEXT_MISSING);
            summary.getReviewReasons().add("PLAN_SOURCE_UNVERIFIED");
        }
        if (NOT_COMPUTABLE.equals(summary.getExecutionDeviationStatus())) {
            summary.getReviewReasons().add("EXECUTION_DEVIATION_NOT_COMPUTABLE");
        }
        summary.setGeneratedAt(LocalDateTime.now());
        forceSummarySafety(summary);
        return summary;
    }

    @Override
    public UserPositionReviewFeedbackResultDTO recordFeedbackForUser(
            Long userId, Long positionId, UserPositionReviewFeedbackReq request) {
        if (request == null) {
            throw new IllegalArgumentException("feedback request is required");
        }
        UserPositionDO position = requireClosedPosition(userId, positionId);
        ResolvedPositionReviewContext context = resolveReviewContext(userId, position);
        String analysisId = feedbackAnalysisId(position, context.planSource());

        WriteReviewResultReq req = new WriteReviewResultReq();
        req.setAnalysisId(analysisId);
        req.setErrorType(trimToNull(request.getErrorType()));
        req.setActualOutcome(trimToNull(request.getActualOutcome()));
        req.setAdjustmentSuggestion(trimToNull(request.getAdjustmentSuggestion()));
        ReviewStateVO state = reviewService.saveOrUpdateForUserPosition(userId, position.getId(), req);

        UserPositionReviewFeedbackResultDTO result = new UserPositionReviewFeedbackResultDTO();
        result.setPositionId(position.getId());
        result.setAnalysisId(analysisId);
        result.setReviewId(state == null ? null : state.getReviewId());
        result.setErrorType(state == null ? req.getErrorType() : state.getErrorType());
        result.setActualOutcome(state == null ? req.getActualOutcome() : state.getActualOutcome());
        result.setAdjustmentSuggestion(state == null ? req.getAdjustmentSuggestion() : state.getAdjustmentSuggestion());
        result.setRuleFeedbackRecorded(true);
        result.setRuleChangeApplied(false);
        result.setRecordedAt(state == null || state.getUpdateTime() == null ? LocalDateTime.now() : state.getUpdateTime());
        result.setManualInputOnly(true);
        result.setNotRuleAutoApply(true);
        result.setNotTradeInstruction(true);
        result.setNotExecutable(true);
        result.setNotAutoTrading(true);
        return result;
    }

    private UserPositionDO requireClosedPosition(Long userId, Long positionId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        if (positionId == null || positionId <= 0) {
            throw new IllegalArgumentException("position_id is required");
        }
        UserPositionDO position = userPositionMapper.selectByIdAndUserId(positionId, userId);
        if (position == null) {
            throw new UserPositionNotFoundException();
        }
        String status = trimToNull(position.getStatus());
        if ("OPEN".equals(status)) {
            throw new UserPositionConflictException("POSITION_NOT_CLOSED");
        }
        if ("PARTIALLY_CLOSED".equals(status)) {
            throw new UserPositionConflictException("POSITION_NOT_FULLY_CLOSED");
        }
        if (!"CLOSED".equals(status)) {
            throw new IllegalArgumentException("UserPosition status must be CLOSED");
        }
        requirePositive(position.getEntryPrice(), "entryPrice");
        requirePositive(position.getClosePrice(), "closePrice");
        requirePositive(position.getQuantity(), "quantity");
        requirePositive(position.getLeverage(), "leverage");
        if (position.getOpenedAt() == null) {
            throw new IllegalArgumentException("openedAt is required");
        }
        if (position.getClosedAt() == null) {
            throw new IllegalArgumentException("closedAt is required");
        }
        if (position.getClosedAt().isBefore(position.getOpenedAt())) {
            throw new IllegalArgumentException("closedAt must not be before openedAt");
        }
        String side = trimToNull(position.getSide());
        if (!"LONG".equals(side) && !"SHORT".equals(side)) {
            throw new IllegalArgumentException("side must be LONG or SHORT");
        }
        return position;
    }

    private ResolvedPositionReviewContext resolveReviewContext(Long userId, UserPositionDO position) {
        List<PositionMonitorLogDTO> rawLogs;
        try {
            rawLogs = positionMonitorLogService.listAllByPositionIdForUserReview(userId, position.getId());
        } catch (RuntimeException ignored) {
            rawLogs = List.of();
        }

        List<PositionMonitorLogDTO> safeLogs = new ArrayList<>();
        PositionPlanSourceResolver.Resolution latestTrustedSource = null;
        PositionMonitorLogDTO latestTrustedLog = null;
        String failureReason = "TRUSTED_MONITOR_SOURCE_MISSING";
        for (PositionMonitorLogDTO log : rawLogs == null ? List.<PositionMonitorLogDTO>of() : rawLogs) {
            if (log == null) {
                continue;
            }
            if (!Objects.equals(position.getId(), log.getPositionId())) {
                PositionMonitorLogSourceViewPolicy.markUnverified(log, true);
                failureReason = "MONITOR_POSITION_MISMATCH";
                safeLogs.add(log);
                continue;
            }
            PositionPlanSourceResolver.Resolution monitorSource = positionPlanSourceResolver
                    .resolveTrustedMonitorSource(position.getId(), position.getAssetSymbol(),
                            position.getSourceRefId(), log.getAnalysisId(), log.getExecutionPlanId());
            if (monitorSource.verified()) {
                PositionMonitorLogSourceViewPolicy.markVerified(
                        log, monitorSource.analysisId(), monitorSource.executionPlanId());
                if (latestTrustedLog == null || isNewerMonitorLog(log, latestTrustedLog)) {
                    latestTrustedLog = log;
                    latestTrustedSource = monitorSource;
                }
            } else {
                failureReason = monitorSource.failureReason();
                PositionMonitorLogSourceViewPolicy.markUnverified(log, true);
            }
            safeLogs.add(log);
        }

        PositionPlanSourceResolver.Resolution source = latestTrustedSource;
        if (source == null) {
            source = positionPlanSourceResolver.resolveTypedReference(
                    position.getId(), position.getAssetSymbol(), position.getSourceRefId());
            if (!source.verified()) {
                failureReason = source.failureReason();
            }
        }
        return new ResolvedPositionReviewContext(
                source,
                source.verified() ? source.executionPlan() : null,
                source.verified() ? source.analysisRun() : null,
                source.verified() ? source.analysisId() : null,
                source.verified() ? source.executionPlanId() : null,
                List.copyOf(safeLogs),
                failureReason);
    }

    private static boolean isNewerMonitorLog(PositionMonitorLogDTO candidate,
                                             PositionMonitorLogDTO current) {
        if (candidate.getCreatedAt() != null && current.getCreatedAt() != null) {
            int timeComparison = candidate.getCreatedAt().compareTo(current.getCreatedAt());
            if (timeComparison != 0) {
                return timeComparison > 0;
            }
        } else if (candidate.getCreatedAt() != null) {
            return true;
        } else if (current.getCreatedAt() != null) {
            return false;
        }
        if (candidate.getLogId() != null && current.getLogId() != null) {
            return candidate.getLogId() > current.getLogId();
        }
        return true;
    }

    private static void fillPosition(UserPositionReviewSummaryDTO summary, UserPositionDO position) {
        summary.setPositionId(position.getId());
        summary.setAssetSymbol(position.getAssetSymbol());
        summary.setSide(position.getSide());
        summary.setPositionStatus(position.getStatus());
        summary.setSourceRefId(position.getSourceRefId());
        summary.setEntryPrice(position.getEntryPrice());
        summary.setClosePrice(position.getClosePrice());
        summary.setStopLoss(position.getStopLoss());
        summary.setTakeProfit(position.getTakeProfit());
        summary.setQuantity(position.getQuantity());
        summary.setLeverage(position.getLeverage());
        summary.setOpenedAt(position.getOpenedAt());
        summary.setClosedAt(position.getClosedAt());
        summary.setHoldingDurationSeconds(Duration.between(position.getOpenedAt(), position.getClosedAt()).getSeconds());
    }

    private static void fillPlan(UserPositionReviewSummaryDTO summary,
                                 UserPositionDO position,
                                 PositionPlanSourceResolver.Resolution planSource) {
        if (planSource == null || !planSource.verified()) {
            summary.setPlanContextStatus(PLAN_CONTEXT_MISSING);
            summary.setSourceRefId(null);
            summary.setAnalysisId("USER_POSITION_" + position.getId());
            summary.setExecutionDeviationStatus(NOT_COMPUTABLE);
            return;
        }
        ExecutionPlanDO plan = planSource.executionPlan();
        summary.setPlanContextStatus(PLAN_CONTEXT_FOUND);
        summary.setAnalysisId(planSource.analysisId());
        summary.setExecutionPlanId(planSource.executionPlanId());
        summary.setExecutionPlanStatus(plan.getExecutionPlanStatus());
        summary.setSourceGateStatus(plan.getSourceGateStatus());
        summary.setSourceGateComplete(plan.getSourceGateComplete());
        summary.setEntryZone(plan.getEntryZone());
        summary.setPlanStopLoss(plan.getStopLoss());
        summary.setTakeProfitRules(plan.getTakeProfitRules());
        summary.setInvalidCondition(plan.getInvalidCondition());
        summary.setRecommendedAction(plan.getRecommendedAction());
    }

    private static void fillPnl(UserPositionReviewSummaryDTO summary, UserPositionDO position) {
        BigDecimal priceDelta;
        if ("LONG".equals(position.getSide())) {
            priceDelta = position.getClosePrice().subtract(position.getEntryPrice());
        } else {
            priceDelta = position.getEntryPrice().subtract(position.getClosePrice());
        }
        BigDecimal grossPnl = priceDelta.multiply(position.getQuantity()).setScale(8, RoundingMode.HALF_UP);
        BigDecimal basis = position.getEntryPrice().multiply(position.getQuantity());
        BigDecimal grossReturnPct = grossPnl.divide(basis, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(8, RoundingMode.HALF_UP);
        summary.setGrossPnl(grossPnl);
        summary.setGrossReturnPct(grossReturnPct);
        summary.setLeveragedReturnPctProxy(grossReturnPct.multiply(position.getLeverage()).setScale(8, RoundingMode.HALF_UP));
        if (grossPnl.compareTo(BigDecimal.ZERO) > 0) {
            summary.setOutcome("WIN");
        } else if (grossPnl.compareTo(BigDecimal.ZERO) < 0) {
            summary.setOutcome("LOSS");
        } else {
            summary.setOutcome("BREAKEVEN");
        }
        summary.setPnlCalculationMethod("GROSS_ESTIMATE_EXCLUDES_FEES_FUNDING_SLIPPAGE");
    }

    private static void fillExecutionDeviation(UserPositionReviewSummaryDTO summary,
                                               UserPositionDO position,
                                               ExecutionPlanDO plan) {
        if (plan == null) {
            summary.setExecutionDeviationStatus(NOT_COMPUTABLE);
            summary.getExecutionDeviationReasons().add(PLAN_CONTEXT_MISSING);
            return;
        }
        List<BigDecimal> ratios = new ArrayList<>();
        addDeviation(summary, ratios, "ENTRY", position.getEntryPrice(),
                UserPositionReviewPolicy.parseSingleNumberOrRangeMidpoint(plan.getEntryZone()));
        addDeviation(summary, ratios, "STOP_LOSS", position.getStopLoss(),
                UserPositionReviewPolicy.parseSingleNumberOrRangeMidpoint(plan.getStopLoss()));
        addDeviation(summary, ratios, "TAKE_PROFIT", position.getTakeProfit(),
                UserPositionReviewPolicy.parseSingleNumberOrRangeMidpoint(plan.getTakeProfitRules()));

        if (ratios.isEmpty()) {
            summary.setExecutionDeviationStatus(NOT_COMPUTABLE);
            summary.getExecutionDeviationReasons().add("NO_SAFE_NUMERIC_PLAN_BOUNDARY");
            return;
        }
        boolean deviated = ratios.stream().anyMatch(UserPositionReviewPolicy::exceedsTolerance);
        summary.setExecutionDeviationStatus(deviated ? DEVIATED : ALIGNED);
        if (!deviated) {
            summary.getExecutionDeviationReasons().add("COMPARABLE_BOUNDARIES_WITHIN_TOLERANCE");
        }
    }

    private static void addDeviation(UserPositionReviewSummaryDTO summary,
                                     List<BigDecimal> ratios,
                                     String label,
                                     BigDecimal actual,
                                     BigDecimal reference) {
        BigDecimal ratio = UserPositionReviewPolicy.deviationRatio(actual, reference);
        if (ratio == null) {
            summary.getExecutionDeviationReasons().add(label + "_NOT_COMPUTABLE");
            return;
        }
        ratios.add(ratio);
        if ("ENTRY".equals(label)) {
            summary.setEntryDeviationRatio(ratio);
        } else if ("STOP_LOSS".equals(label)) {
            summary.setStopLossDeviationRatio(ratio);
        } else if ("TAKE_PROFIT".equals(label)) {
            summary.setTakeProfitDeviationRatio(ratio);
        }
        if (UserPositionReviewPolicy.exceedsTolerance(ratio)) {
            summary.getExecutionDeviationReasons().add(label + "_DEVIATED");
        }
    }

    private static void fillMonitorFacts(UserPositionReviewSummaryDTO summary,
                                         UserPositionDO position,
                                         List<PositionMonitorLogDTO> logs) {
        List<PositionMonitorLogDTO> safeLogs = logs == null ? List.of() : logs.stream()
                .map(PositionMonitorLogSourceViewPolicy::sanitizeResolvedBusinessView)
                .toList();
        summary.setMonitorLogs(safeLogs);
        summary.setMonitorLogCount(safeLogs.size());

        List<PositionMonitorLogDTO> beforeClose = safeLogs.stream()
                .filter(log -> log.getCreatedAt() != null && !log.getCreatedAt().isAfter(position.getClosedAt()))
                .toList();
        List<PositionMonitorLogDTO> invalidations = beforeClose.stream()
                .filter(log -> "PLAN_INVALIDATED".equals(log.getMonitorConclusion()))
                .toList();
        summary.setPlanInvalidatedBeforeClose(!invalidations.isEmpty());
        summary.setPlanInvalidationWarningCount(invalidations.size());
        if (!invalidations.isEmpty()) {
            summary.setFirstPlanInvalidatedAt(invalidations.get(0).getCreatedAt());
        }

        List<PositionMonitorLogDTO> warnings = beforeClose.stream()
                .filter(DefaultUserPositionReviewAdapter::isWarningLog)
                .toList();
        summary.setWarnedBeforeClose(!warnings.isEmpty());
        summary.setWarningCount(warnings.size());
        if (warnings.isEmpty()) {
            summary.setWarningTimelinessStatus("NO_WARNING_BEFORE_CLOSE");
        } else {
            LocalDateTime firstWarning = warnings.get(0).getCreatedAt();
            LocalDateTime lastWarning = warnings.get(warnings.size() - 1).getCreatedAt();
            summary.setFirstWarningAt(firstWarning);
            summary.setLastWarningAt(lastWarning);
            summary.setWarningLeadSeconds(Duration.between(firstWarning, position.getClosedAt()).getSeconds());
            summary.setWarningTimelinessStatus("TIMELY_WARNING");
        }

        boolean highRiskBeforeClose = beforeClose.stream()
                .anyMatch(log -> "HIGH".equals(log.getRiskLevel()) || "EXTREME".equals(log.getRiskLevel()));
        if (summary.isWarnedBeforeClose()
                && position.getClosedAt().isAfter(summary.getFirstWarningAt())
                && ("LOSS".equals(summary.getOutcome()) || summary.isPlanInvalidatedBeforeClose() || highRiskBeforeClose)) {
            summary.setIgnoredWarning(true);
            if ("LOSS".equals(summary.getOutcome())) {
                summary.getIgnoredWarningReasons().add("LOSS_AFTER_WARNING");
            }
            if (summary.isPlanInvalidatedBeforeClose()) {
                summary.getIgnoredWarningReasons().add("PLAN_INVALIDATED_BEFORE_CLOSE");
            }
            if (highRiskBeforeClose) {
                summary.getIgnoredWarningReasons().add("HIGH_RISK_BEFORE_CLOSE");
            }
        }
    }

    private static boolean isWarningLog(PositionMonitorLogDTO log) {
        String monitorConclusion = log.getMonitorConclusion();
        String suggestedAction = log.getSuggestedAction();
        return "LOGIC_WEAKENED".equals(monitorConclusion)
                || "PLAN_INVALIDATED".equals(monitorConclusion)
                || "NEAR_STOP_LOSS".equals(monitorConclusion)
                || "HIGH_RISK_OBSERVATION".equals(monitorConclusion)
                || "WAIT_USER_CONFIRM_CLOSE".equals(monitorConclusion)
                || "REDUCE_POSITION".equals(suggestedAction)
                || "TIGHTEN_STOP".equals(suggestedAction)
                || "WAIT_CONFIRMATION".equals(suggestedAction)
                || "RECORD_CLOSE_REVIEW".equals(suggestedAction);
    }

    private static String feedbackAnalysisId(UserPositionDO position,
                                             PositionPlanSourceResolver.Resolution planSource) {
        if (planSource != null && planSource.verified() && trimToNull(planSource.analysisId()) != null) {
            return planSource.analysisId().trim();
        }
        return "USER_POSITION_" + position.getId();
    }

    private static void requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void forceSummarySafety(UserPositionReviewSummaryDTO summary) {
        summary.setReviewOnly(true);
        summary.setManualReviewOnly(true);
        summary.setNotTradeInstruction(true);
        summary.setNotExecutable(true);
        summary.setNotAutoTrading(true);
        summary.setNotOrderExecution(true);
        summary.setNotAutoOpen(true);
        summary.setNotAutoClose(true);
        summary.setNotAutoReverse(true);
        summary.setNotUserPositionMutation(true);
        summary.setNotRuleAutoApply(true);
    }

    private record ResolvedPositionReviewContext(
            PositionPlanSourceResolver.Resolution planSource,
            ExecutionPlanDO executionPlan,
            AnalysisRunDO analysisRun,
            String analysisId,
            String executionPlanId,
            List<PositionMonitorLogDTO> monitorLogs,
            String failureReason) {
    }
}
