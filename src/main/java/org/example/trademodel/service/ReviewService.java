package org.example.trademodel.service;

import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.vo.ReviewStateVO;

public interface ReviewService {

    ReviewStateVO saveOrUpdate(WriteReviewResultReq req);

    ReviewStateVO saveOrUpdateForUserAnalysis(Long userId, WriteReviewResultReq req);

    ReviewStateVO saveOrUpdateForUserPosition(Long userId, Long userPositionId, WriteReviewResultReq req);

    ReviewStateVO getStateByAnalysisId(String analysisId);

    ReviewStateVO getStateByAnalysisIdForUser(Long userId, String analysisId);
}
