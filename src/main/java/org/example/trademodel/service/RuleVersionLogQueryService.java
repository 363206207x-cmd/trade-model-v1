package org.example.trademodel.service;

import org.example.trademodel.vo.ReviewAggregateVO;

import java.util.List;

public interface RuleVersionLogQueryService {

    List<ReviewAggregateVO.RuleVersionLogSummary> listByAnalysisId(String analysisId, int limit);

    List<ReviewAggregateVO.RuleVersionLogSummary> query(
            String analysisId,
            String ruleVersion,
            String operator,
            String rollbackFlag,
            String errorType,
            String changeCategory,
            String keyword,
            String createdAtFrom,
            String createdAtTo,
            int limit);
}
