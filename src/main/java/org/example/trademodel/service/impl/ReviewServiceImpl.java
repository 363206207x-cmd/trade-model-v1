package org.example.trademodel.service.impl;

import org.example.trademodel.constant.ReviewErrorType;
import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ReviewResultDO;
import org.example.trademodel.entity.RuleVersionLogDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.vo.AnalysisReviewSummaryVO;
import org.example.trademodel.vo.ReviewStateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String OPERATOR_SYSTEM = "SYSTEM";
    private static final String CHANGE_CATEGORY_REVIEW_FEEDBACK_SAVED = "REVIEW_FEEDBACK_SAVED";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String REVIEW_STATUS_EMPTY = "EMPTY";
    private static final String REVIEW_STATUS_FILLED = "FILLED";

    private final ReviewResultMapper reviewResultMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final RuleVersionLogMapper ruleVersionLogMapper;

    public ReviewServiceImpl(ReviewResultMapper reviewResultMapper,
                               AnalysisRunMapper analysisRunMapper,
                               RuleVersionLogMapper ruleVersionLogMapper) {
        this.reviewResultMapper = reviewResultMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.ruleVersionLogMapper = ruleVersionLogMapper;
    }

    @Override
    @Transactional
    public ReviewStateVO saveOrUpdate(WriteReviewResultReq req) {
        String analysisId = req.getAnalysisId().trim();
        LocalDateTime now = LocalDateTime.now();

        String errorType = trimToNull(req.getErrorType());
        ReviewErrorType.validateAllowedOrThrow(errorType);
        String actualOutcome = trimToNull(req.getActualOutcome());
        String adjustmentSuggestion = trimToNull(req.getAdjustmentSuggestion());

        ReviewResultDO existing = reviewResultMapper.selectByAnalysisId(analysisId);
        if (existing == null) {
            ReviewResultDO row = new ReviewResultDO();
            row.setId(UUID.randomUUID().toString());
            row.setAnalysisId(analysisId);
            row.setErrorType(errorType);
            row.setActualOutcome(actualOutcome);
            row.setAdjustmentSuggestion(adjustmentSuggestion);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            reviewResultMapper.insert(row);
        }
        if (existing != null) {
            existing.setErrorType(errorType);
            existing.setActualOutcome(actualOutcome);
            existing.setAdjustmentSuggestion(adjustmentSuggestion);
            existing.setUpdateTime(now);
            reviewResultMapper.updateContentByAnalysisId(existing);
        }

        // review 写入成功后，同事务追加最小审计链：tm_rule_version_log
        writeRuleVersionLog(analysisId, errorType, actualOutcome, adjustmentSuggestion, now);

        return toVo(reviewResultMapper.selectByAnalysisId(analysisId));
    }

    @Override
    public ReviewStateVO getStateByAnalysisId(String analysisId) {
        if (analysisId == null || analysisId.isBlank()) {
            return null;
        }
        ReviewResultDO row = reviewResultMapper.selectByAnalysisId(analysisId.trim());
        return row == null ? null : toVo(row);
    }

    @Override
    public AnalysisReviewSummaryVO getAnalysisReviewSummary(String analysisId) {
        if (analysisId == null || analysisId.isBlank()) {
            return null;
        }
        String id = analysisId.trim();
        ReviewResultDO row = reviewResultMapper.selectByAnalysisId(id);
        if (row == null) {
            return buildEmptySummary(id, null);
        }
        if (!hasReviewContent(row)) {
            return buildEmptySummary(id, row.getUpdateTime());
        }
        return buildFilledSummary(id, row.getUpdateTime());
    }

    private static AnalysisReviewSummaryVO buildEmptySummary(String analysisId, LocalDateTime updatedAt) {
        AnalysisReviewSummaryVO vo = new AnalysisReviewSummaryVO();
        vo.setReviewStatus(REVIEW_STATUS_EMPTY);
        vo.setReviewStatusText("未复盘");
        vo.setReviewCompleted(Boolean.FALSE);
        vo.setReviewHasContent(Boolean.FALSE);
        vo.setReviewUpdatedAt(updatedAt);
        vo.setReviewEntryUrl("/review/" + analysisId);
        return vo;
    }

    private static AnalysisReviewSummaryVO buildFilledSummary(String analysisId, LocalDateTime updatedAt) {
        AnalysisReviewSummaryVO vo = new AnalysisReviewSummaryVO();
        vo.setReviewStatus(REVIEW_STATUS_FILLED);
        vo.setReviewStatusText("已复盘");
        vo.setReviewCompleted(Boolean.TRUE);
        vo.setReviewHasContent(Boolean.TRUE);
        vo.setReviewUpdatedAt(updatedAt);
        vo.setReviewEntryUrl("/review/" + analysisId);
        return vo;
    }

    private static boolean hasReviewContent(ReviewResultDO row) {
        return hasText(row.getErrorType())
                || hasText(row.getActualOutcome())
                || hasText(row.getAdjustmentSuggestion());
    }

    private static boolean hasText(String s) {
        if (s == null) {
            return false;
        }
        return !s.trim().isEmpty();
    }

    private static ReviewStateVO toVo(ReviewResultDO row) {
        ReviewStateVO vo = new ReviewStateVO();
        vo.setReviewId(row.getId());
        vo.setAnalysisId(row.getAnalysisId());
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
}
