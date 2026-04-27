package org.example.trademodel.service;

import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.vo.ReviewStateVO;

public interface ReviewService {

    ReviewStateVO saveOrUpdate(WriteReviewResultReq req);

    ReviewStateVO getStateByAnalysisId(String analysisId);
}
