package org.example.trademodel.userpositionreview;

public interface UserPositionReviewAdapter {

    UserPositionReviewSummaryDTO buildSummary(Long positionId);

    UserPositionReviewFeedbackResultDTO recordFeedback(Long positionId, UserPositionReviewFeedbackReq request);
}
