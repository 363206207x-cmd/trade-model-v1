package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ReviewResultDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogSourceViewPolicy;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.service.ReviewCenterService;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.example.trademodel.userpositionreview.UserPositionReviewAdapter;
import org.example.trademodel.userpositionreview.UserPositionReviewSummaryDTO;
import org.example.trademodel.vo.ReviewAggregateVO;
import org.example.trademodel.vo.ReviewCenterDashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ReviewCenterServiceImpl implements ReviewCenterService {
    static final int DEFAULT_LIMIT = 50;
    static final String TELEGRAM_WAITING_SYNC = "WAITING_SYNC";
    private static final String REVIEW_TYPE_RULE_FEEDBACK = "RULE_FEEDBACK";
    private static final Set<String> REVIEWABLE_OPPORTUNITY_STATUSES = Set.of(
            OpportunityLogStatus.EXECUTED_VALID,
            OpportunityLogStatus.EXECUTED_INVALID,
            OpportunityLogStatus.MISSED_VALID,
            OpportunityLogStatus.MISSED_INVALID,
            OpportunityLogStatus.PUSHED_NOT_FILLED_VALID,
            OpportunityLogStatus.BLOCKED_BY_RISK_VALID);
    private static final Set<String> RULE_ISSUE_TYPES = Set.of("RULE_TOO_LOOSE", "RULE_TOO_STRICT");

    private final UserPositionMapper userPositionMapper;
    private final UserPositionReviewAdapter userPositionReviewAdapter;
    private final OpportunityLogService opportunityLogService;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;
    private final ReviewResultMapper reviewResultMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private Clock clock = Clock.systemUTC();

    public ReviewCenterServiceImpl(UserPositionMapper userPositionMapper,
                                   UserPositionReviewAdapter userPositionReviewAdapter,
                                   OpportunityLogService opportunityLogService,
                                   PushSnapshotMapper pushSnapshotMapper,
                                   PushRecheckLogMapper pushRecheckLogMapper,
                                   ReviewResultMapper reviewResultMapper,
                                   AnalysisRunMapper analysisRunMapper) {
        this.userPositionMapper = userPositionMapper;
        this.userPositionReviewAdapter = userPositionReviewAdapter;
        this.opportunityLogService = opportunityLogService;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.reviewResultMapper = reviewResultMapper;
        this.analysisRunMapper = analysisRunMapper;
    }

    @Autowired(required = false)
    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public ReviewCenterDashboardVO getDashboardForUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        ReviewCenterDashboardVO vo = new ReviewCenterDashboardVO();
        vo.setPositionReviews(positionReviews(userId));
        vo.setOpportunityReviews(opportunityReviews(userId));
        vo.setPushReviews(pushReviews());
        vo.setRuleFeedback(ruleFeedback());

        ReviewCenterDashboardVO.Summary summary = new ReviewCenterDashboardVO.Summary();
        summary.setPositionReviewCount(vo.getPositionReviews().size());
        summary.setOpportunityReviewCount(vo.getOpportunityReviews().size());
        summary.setPushReviewCount(vo.getPushReviews().size());
        summary.setRuleFeedbackCount(vo.getRuleFeedback().size());
        vo.setSummary(summary);
        vo.setDiagnostics(buildDiagnostics(vo));
        return vo;
    }

    private ReviewCenterDashboardVO.Diagnostics buildDiagnostics(ReviewCenterDashboardVO vo) {
        ReviewCenterDashboardVO.Diagnostics diagnostics = new ReviewCenterDashboardVO.Diagnostics();
        diagnostics.setPositionReviewStatus(sourceStatus(vo.getPositionReviews().size()));
        diagnostics.setOpportunityLogStatus(sourceStatus(vo.getOpportunityReviews().size()));
        diagnostics.setPushRecheckStatus(sourceStatus(vo.getPushReviews().size()));
        diagnostics.setRuleFeedbackStatus(sourceStatus(vo.getRuleFeedback().size()));
        diagnostics.setReviewCenterStatus("READY_READONLY");
        return diagnostics;
    }

    private static String sourceStatus(int count) {
        return count > 0 ? "READY" : "EMPTY";
    }

    private List<ReviewCenterDashboardVO.PositionReviewItem> positionReviews(Long userId) {
        List<UserPositionDO> rows = userPositionMapper.listClosedManualByUserId(userId, DEFAULT_LIMIT);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ReviewCenterDashboardVO.PositionReviewItem> out = new ArrayList<>(rows.size());
        for (UserPositionDO row : rows) {
            out.add(toPositionReview(userId, row));
        }
        return out;
    }

    private ReviewCenterDashboardVO.PositionReviewItem toPositionReview(Long userId, UserPositionDO row) {
        ReviewCenterDashboardVO.PositionReviewItem item = new ReviewCenterDashboardVO.PositionReviewItem();
        item.setTime(firstNonNull(row.getClosedAt(), row.getUpdatedAt(), row.getOpenedAt()));
        item.setSymbol(row.getAssetSymbol());
        item.setDirection(row.getSide());
        item.setEntryPrice(row.getEntryPrice());
        item.setClosePrice(row.getClosePrice());
        item.setActualExecution(compactActualExecution(row));

        try {
            UserPositionReviewSummaryDTO summary = userPositionReviewAdapter.buildSummaryForUser(userId, row.getId());
            item.setPnl(summary.getGrossPnl());
            item.setFinalPnl(summary.getGrossPnl());
            item.setExecutionDeviation(summary.getExecutionDeviationStatus());
            item.setExecutionDeviationDetail(summary.getExecutionDeviationReasons());
            List<PositionMonitorLogDTO> safeMonitorLogs = summary.getMonitorLogs() == null
                    ? List.of()
                    : summary.getMonitorLogs().stream()
                            .map(PositionMonitorLogSourceViewPolicy::sanitizeResolvedBusinessView)
                            .toList();
            item.setMonitorTimeline(safeMonitorLogs);
            item.setMonitorConclusion(latestMonitorConclusion(safeMonitorLogs));
            item.setReviewStatus(summary.getReviewStatus());
            item.setOriginalExecutionPlan(toPlanSummary(summary));
        } catch (IllegalArgumentException ex) {
            item.setReviewStatus(null);
        }
        return item;
    }

    private List<ReviewCenterDashboardVO.OpportunityReviewItem> opportunityReviews(Long userId) {
        List<OpportunityLogDTO> rows = opportunityLogService.queryForUser(
                userId, null, null, null, null, null, null, null, null, DEFAULT_LIMIT);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ReviewCenterDashboardVO.OpportunityReviewItem> out = new ArrayList<>();
        for (OpportunityLogDTO row : rows) {
            if (!REVIEWABLE_OPPORTUNITY_STATUSES.contains(row.getOpportunityStatus())) {
                continue;
            }
            ReviewCenterDashboardVO.OpportunityReviewItem item = new ReviewCenterDashboardVO.OpportunityReviewItem();
            item.setTime(row.getAnchorTime());
            item.setSymbol(row.getSymbol());
            item.setOpportunityType(row.getOpportunityStatus());
            item.setWasPushed(row.getPushPresent());
            item.setWasClicked(null);
            item.setWasExecuted(null);
            item.setOutcome(row.getOpportunityStatus());
            item.setMaxFavorableExcursion(row.getMfeRatio());
            item.setMaxAdverseExcursion(row.getMaeRatio());
            out.add(item);
        }
        return out;
    }

    private List<ReviewCenterDashboardVO.PushReviewItem> pushReviews() {
        List<TmPushSnapshotDO> rows = pushSnapshotMapper.listRecent(DEFAULT_LIMIT);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ReviewCenterDashboardVO.PushReviewItem> out = new ArrayList<>(rows.size());
        for (TmPushSnapshotDO row : rows) {
            TmPushRecheckLogDO latest = row.getPushId() == null ? null : pushRecheckLogMapper.selectLatestByPushId(row.getPushId());
            ReviewCenterDashboardVO.PushReviewItem item = new ReviewCenterDashboardVO.PushReviewItem();
            item.setPushTime(firstNonNull(row.getPushCreateTime(), row.getCreateTime()));
            item.setSymbol(row.getSymbol());
            item.setPushType(row.getPushType());
            item.setTelegramStatus(TELEGRAM_WAITING_SYNC);
            item.setClicked(null);
            item.setRecheckStatus(latest == null ? null : latest.getRecheckStatus());
            item.setExpired(isExpired(row));
            item.setFailReason(latest == null ? null : firstNonBlank(
                    latest.getFailReasonJson(), latest.getExecutionErrorMessage(), latest.getExecutionErrorCode()));
            item.setOutcome(latest == null ? row.getPushStatus() : firstNonBlank(latest.getExecutionStatus(), row.getPushStatus()));
            out.add(item);
        }
        return out;
    }

    private List<ReviewCenterDashboardVO.RuleFeedbackItem> ruleFeedback() {
        List<ReviewResultDO> rows = reviewResultMapper.listRecent(DEFAULT_LIMIT);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ReviewCenterDashboardVO.RuleFeedbackItem> out = new ArrayList<>(rows.size());
        for (ReviewResultDO row : rows) {
            AnalysisRunDO run = row.getAnalysisId() == null ? null : analysisRunMapper.selectById(row.getAnalysisId());
            ReviewCenterDashboardVO.RuleFeedbackItem item = new ReviewCenterDashboardVO.RuleFeedbackItem();
            item.setTime(firstNonNull(row.getUpdateTime(), row.getCreateTime()));
            item.setSymbol(run == null ? null : run.getSymbol());
            item.setReviewType(REVIEW_TYPE_RULE_FEEDBACK);
            item.setErrorType(row.getErrorType());
            item.setRuleIssue(ruleIssue(row.getErrorType()));
            item.setExecutionDeviation(executionDeviation(row.getErrorType()));
            item.setSuggestion(row.getAdjustmentSuggestion());
            item.setRuleVersion(run == null ? null : run.getRuleVersion());
            item.setStatus(null);
            out.add(item);
        }
        return out;
    }

    private static ReviewAggregateVO.ReviewPlanSummary toPlanSummary(UserPositionReviewSummaryDTO summary) {
        if (summary.getExecutionPlanId() == null && summary.getEntryZone() == null
                && summary.getPlanStopLoss() == null && summary.getTakeProfitRules() == null) {
            return null;
        }
        ReviewAggregateVO.ReviewPlanSummary plan = new ReviewAggregateVO.ReviewPlanSummary();
        plan.setPlanId(summary.getExecutionPlanId());
        plan.setRecommendedAction(summary.getRecommendedAction());
        plan.setEntryZone(summary.getEntryZone());
        plan.setStopLoss(summary.getPlanStopLoss());
        plan.setTakeProfitRules(summary.getTakeProfitRules());
        return plan;
    }

    private static String latestMonitorConclusion(List<PositionMonitorLogDTO> logs) {
        if (logs == null || logs.isEmpty()) {
            return null;
        }
        PositionMonitorLogDTO latest = logs.get(logs.size() - 1);
        return firstNonBlank(latest.getLogicStatus(), latest.getSuggestedAction(), latest.getReason());
    }

    private static String compactActualExecution(UserPositionDO row) {
        String side = trimToNull(row.getSide());
        String quantity = row.getQuantity() == null ? null : row.getQuantity().stripTrailingZeros().toPlainString();
        return firstNonBlank(joinNonBlank(side, quantity), side, quantity);
    }

    private Boolean isExpired(TmPushSnapshotDO row) {
        String status = trimToNull(row.getPushStatus());
        if (status != null && status.toUpperCase(Locale.ROOT).contains("EXPIRED")) {
            return true;
        }
        if (row.getExpiresAt() == null) {
            return null;
        }
        return !UtcLocalTimePolicy.now(clock).isBefore(row.getExpiresAt());
    }

    private static Boolean ruleIssue(String errorType) {
        String normalized = trimToNull(errorType);
        if (normalized == null || "UNKNOWN".equals(normalized)) {
            return null;
        }
        if (RULE_ISSUE_TYPES.contains(normalized)) {
            return true;
        }
        if ("PLAN_EXECUTION_MISMATCH".equals(normalized)) {
            return false;
        }
        return null;
    }

    private static Boolean executionDeviation(String errorType) {
        String normalized = trimToNull(errorType);
        return "PLAN_EXECUTION_MISMATCH".equals(normalized) ? Boolean.TRUE : null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String joinNonBlank(String left, String right) {
        String a = trimToNull(left);
        String b = trimToNull(right);
        if (a == null || b == null) {
            return null;
        }
        return a + " " + b;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
