package org.example.trademodel.service;

import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.vo.AnalysisReviewSummaryVO;
import org.example.trademodel.vo.ReviewStateVO;

public interface ReviewService {

    ReviewStateVO saveOrUpdate(WriteReviewResultReq req);

    ReviewStateVO getStateByAnalysisId(String analysisId);

    /**
     * 当前 analysis 在 {@code tm_review_result} 上的窄摘要（EMPTY / FILLED）。
     * analysisId 为空或 blank 时返回 null。
     */
    AnalysisReviewSummaryVO getAnalysisReviewSummary(String analysisId);
}
