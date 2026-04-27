package org.example.trademodel.service;

import org.example.trademodel.vo.ReviewAggregateVO;
import org.example.trademodel.vo.ReviewAggregateDetailVO;
import org.example.trademodel.vo.ReviewAggregateSummaryVO;

import java.util.Optional;

/**
 * 复盘聚合：按 analysisId 拉取运行/决策/计划/Push·Recheck/Missed/资产状态/告警等摘要（不含 {@code tm_review_result} 用户录入）。
 */
public interface ReviewAggregateService {

    Optional<ReviewAggregateVO> getAggregateByAnalysisId(String analysisId);

    Optional<ReviewAggregateSummaryVO> getAggregateSummaryByAnalysisId(String analysisId);

    Optional<ReviewAggregateDetailVO> getAggregateDetailByAnalysisId(String analysisId, String section, int limit);
}
