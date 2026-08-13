package org.example.trademodel.service.impl;

import org.example.trademodel.constant.ReviewErrorType;
import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.ReviewResultDO;
import org.example.trademodel.entity.RuleVersionLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.enums.ReviewTypeEnum;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.support.ReviewMetricsContract;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.example.trademodel.vo.ReviewStateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String OPERATOR_SYSTEM = "SYSTEM";
    private static final String CHANGE_CATEGORY_REVIEW_FEEDBACK_SAVED = "REVIEW_FEEDBACK_SAVED";
    private static final String SHARED_SCOPE = "SHARED";
    private static final String V41_CONTRACT_VERSION = "FUNDAMENTAL_AI_V4_1";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReviewResultMapper reviewResultMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final RuleVersionLogMapper ruleVersionLogMapper;
    private final UserPositionMapper userPositionMapper;
    private final ExecutionPlanMapper executionPlanMapper;

    public ReviewServiceImpl(ReviewResultMapper reviewResultMapper,
                               AnalysisRunMapper analysisRunMapper,
                               RuleVersionLogMapper ruleVersionLogMapper,
                               UserPositionMapper userPositionMapper) {
        this(reviewResultMapper, analysisRunMapper, ruleVersionLogMapper, userPositionMapper, null);
    }

    @Autowired
    public ReviewServiceImpl(ReviewResultMapper reviewResultMapper,
                             AnalysisRunMapper analysisRunMapper,
                             RuleVersionLogMapper ruleVersionLogMapper,
                             UserPositionMapper userPositionMapper,
                             ExecutionPlanMapper executionPlanMapper) {
        this.reviewResultMapper = reviewResultMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.ruleVersionLogMapper = ruleVersionLogMapper;
        this.userPositionMapper = userPositionMapper;
        this.executionPlanMapper = executionPlanMapper;
    }

    @Override
    @Transactional
    public ReviewStateVO saveOrUpdate(WriteReviewResultReq req) {
        String analysisId = requireAnalysisId(req);
        LocalDateTime now = LocalDateTime.now();
        ReviewContent content = reviewContent(req);

        ReviewResultDO existing = reviewResultMapper.selectByAnalysisId(analysisId);
        if (existing == null) {
            ReviewResultDO row = newRow(analysisId, null, null, SHARED_SCOPE, content, now);
            applyDecisionChainTrace(row, analysisId, null);
            reviewResultMapper.insert(row);
        }
        if (existing != null) {
            applyContent(existing, content, now);
            applyDecisionChainTrace(existing, analysisId, null);
            reviewResultMapper.updateContentByAnalysisId(existing);
        }

        writeRuleVersionLog(analysisId, content.errorType(), content.actualOutcome(),
                content.adjustmentSuggestion(), now);

        return toVo(reviewResultMapper.selectByAnalysisId(analysisId));
    }

    @Override
    @Transactional
    public ReviewStateVO saveOrUpdateForUserPosition(
            Long userId, Long userPositionId, WriteReviewResultReq req) {
        requirePositive(userId, "userId");
        requirePositive(userPositionId, "userPositionId");
        UserPositionDO ownedPosition = userPositionMapper.selectByIdAndUserId(userPositionId, userId);
        if (ownedPosition == null) {
            throw new UserPositionNotFoundException();
        }
        if (!"CLOSED".equals(ownedPosition.getStatus())) {
            throw new UserPositionConflictException("POSITION_NOT_CLOSED");
        }

        String analysisId = requireAnalysisId(req);
        String reviewScopeKey = userPositionScopeKey(userId, userPositionId);
        LocalDateTime now = LocalDateTime.now();
        ReviewContent content = reviewContent(req);
        ReviewResultDO existing = reviewResultMapper.selectByUserPositionScope(
                analysisId, userId, userPositionId, reviewScopeKey);
        if (existing == null) {
            ReviewResultDO row = newRow(
                    analysisId, userId, userPositionId, reviewScopeKey, content, now);
            applyDecisionChainTrace(row, analysisId, ownedPosition);
            reviewResultMapper.insert(row);
        }
        if (existing != null) {
            applyContent(existing, content, now);
            applyDecisionChainTrace(existing, analysisId, ownedPosition);
            reviewResultMapper.updateContentByUserPositionScope(existing);
        }

        ReviewResultDO saved = reviewResultMapper.selectByUserPositionScope(
                analysisId, userId, userPositionId, reviewScopeKey);
        return saved == null ? null : toVo(saved);
    }

    @Override
    public ReviewStateVO getStateByAnalysisId(String analysisId) {
        if (analysisId == null || analysisId.isBlank()) {
            return null;
        }
        ReviewResultDO row = reviewResultMapper.selectByAnalysisId(analysisId.trim());
        return row == null ? null : toVo(row);
    }

    private static ReviewStateVO toVo(ReviewResultDO row) {
        ReviewStateVO vo = new ReviewStateVO();
        vo.setReviewId(row.getId());
        vo.setAnalysisId(row.getAnalysisId());
        vo.setFinalPlanId(row.getFinalPlanId());
        vo.setCandidateId(row.getCandidateId());
        vo.setTraceId(row.getTraceId());
        vo.setOpportunityId(row.getOpportunityId());
        vo.setResolverResultId(row.getResolverResultId());
        vo.setValidationResultId(row.getValidationResultId());
        vo.setReviewType(row.getReviewType());
        vo.setOutcome(row.getOutcome());
        vo.setExecutionDeviation(row.getExecutionDeviation());
        vo.setAiAssessment(row.getAiAssessment());
        vo.setRuleAssessment(row.getRuleAssessment());
        vo.setRuleFeedback(row.getRuleFeedback());
        vo.setMetricsJson(row.getMetricsJson());
        vo.setContractVersion(row.getContractVersion());
        vo.setErrorType(row.getErrorType());
        vo.setActualOutcome(row.getActualOutcome());
        vo.setAdjustmentSuggestion(row.getAdjustmentSuggestion());
        vo.setCreateTime(row.getCreateTime());
        vo.setUpdateTime(row.getUpdateTime());
        return vo;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireAnalysisId(WriteReviewResultReq req) {
        if (req == null || req.getAnalysisId() == null || req.getAnalysisId().isBlank()) {
            throw new IllegalArgumentException("analysisId is required");
        }
        return req.getAnalysisId().trim();
    }

    private static ReviewContent reviewContent(WriteReviewResultReq req) {
        String errorType = trimToNull(req.getErrorType());
        ReviewErrorType.validateAllowedOrThrow(errorType);
        return new ReviewContent(
                errorType,
                trimToNull(req.getActualOutcome()),
                trimToNull(req.getAdjustmentSuggestion()),
                ReviewTypeEnum.normalizeNullable(req.getReviewType()),
                trimToNull(req.getOutcome()),
                trimToNull(req.getExecutionDeviation()),
                trimToNull(req.getAiAssessment()),
                trimToNull(req.getRuleAssessment()),
                trimToNull(req.getRuleFeedback()),
                ReviewMetricsContract.normalizeOrThrow(req.getMetricsJson()));
    }

    private static ReviewResultDO newRow(String analysisId,
                                         Long userId,
                                         Long userPositionId,
                                         String reviewScopeKey,
                                         ReviewContent content,
                                         LocalDateTime now) {
        ReviewResultDO row = new ReviewResultDO();
        row.setId(UUID.randomUUID().toString());
        row.setAnalysisId(analysisId);
        row.setUserId(userId);
        row.setUserPositionId(userPositionId);
        row.setReviewScopeKey(reviewScopeKey);
        applyContent(row, content, now);
        row.setCreateTime(now);
        return row;
    }

    private static void applyContent(ReviewResultDO row, ReviewContent content, LocalDateTime now) {
        row.setErrorType(content.errorType());
        row.setActualOutcome(content.actualOutcome());
        row.setAdjustmentSuggestion(content.adjustmentSuggestion());
        row.setReviewType(content.reviewType());
        row.setOutcome(content.outcome());
        row.setExecutionDeviation(content.executionDeviation());
        row.setAiAssessment(content.aiAssessment());
        row.setRuleAssessment(content.ruleAssessment());
        row.setRuleFeedback(content.ruleFeedback());
        row.setMetricsJson(content.metricsJson());
        row.setContractVersion(V41_CONTRACT_VERSION);
        row.setUpdateTime(now);
    }

    private static String userPositionScopeKey(Long userId, Long userPositionId) {
        return "USER:" + userId + ":POSITION:" + userPositionId;
    }

    private void applyDecisionChainTrace(ReviewResultDO row,
                                         String analysisId,
                                         UserPositionDO position) {
        if (executionPlanMapper == null) {
            return;
        }
        ExecutionPlanDO plan = position != null && position.getFinalPlanId() != null
                ? executionPlanMapper.selectByPlanId(position.getFinalPlanId())
                : executionPlanMapper.selectLatestByAnalysisId(analysisId);
        if (plan == null || !Boolean.TRUE.equals(plan.getFinalPlan())
                || !"PASS".equals(plan.getRuleValidationStatus())) {
            return;
        }
        if (analysisId != null && plan.getAnalysisId() != null
                && !analysisId.equals(plan.getAnalysisId())) {
            throw new UserPositionConflictException("POSITION_FINAL_PLAN_ANALYSIS_MISMATCH");
        }
        row.setFinalPlanId(plan.getPlanId());
        row.setCandidateId(plan.getCandidateId());
        row.setTraceId(plan.getTraceId());
        row.setOpportunityId(plan.getOpportunityId());
        row.setResolverResultId(plan.getResolverResultId());
        row.setValidationResultId(plan.getValidationResultId());
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private void writeRuleVersionLog(String analysisId,
                                       String errorType,
                                       String actualOutcome,
                                       String adjustmentSuggestion,
                                       LocalDateTime now) {
        AnalysisRunDO run = analysisRunMapper.selectById(analysisId);
        String ruleVersion = run == null ? null : run.getRuleVersion();
        String ruleVersionForSummary = ruleVersion == null ? "MISSING" : ruleVersion;

        String timeStr = now.format(TIME_FMT);
        String errorTypeForSummary = errorType == null ? "NULL" : errorType;

        String changeSummary = CHANGE_CATEGORY_REVIEW_FEEDBACK_SAVED + ";analysisId=" + analysisId
                + ";ruleVersion=" + ruleVersionForSummary
                + ";errorType=" + errorTypeForSummary
                + ";operator=" + OPERATOR_SYSTEM
                + ";time=" + timeStr;

        String changeDetail = "errorType=" + toNullSafe(errorType)
                + ";actualOutcome=" + toNullSafe(actualOutcome)
                + ";adjustmentSuggestion=" + toNullSafe(adjustmentSuggestion);

        RuleVersionLogDO log = new RuleVersionLogDO();
        log.setId(UUID.randomUUID().toString());
        log.setAnalysisId(analysisId);
        log.setRuleVersion(ruleVersion); // 允许为 null（用于空值边界）
        log.setErrorType(errorType);
        log.setChangeCategory(CHANGE_CATEGORY_REVIEW_FEEDBACK_SAVED);
        log.setOperator(OPERATOR_SYSTEM);
        log.setPublishTime(timeStr);
        log.setChangeSummary(changeSummary);
        log.setChangeDetail(changeDetail);

        ruleVersionLogMapper.insert(log);
    }

    private static String toNullSafe(String s) {
        return s == null ? "NULL" : s;
    }

    private record ReviewContent(
            String errorType,
            String actualOutcome,
            String adjustmentSuggestion,
            String reviewType,
            String outcome,
            String executionDeviation,
            String aiAssessment,
            String ruleAssessment,
            String ruleFeedback,
            String metricsJson) {
    }
}
