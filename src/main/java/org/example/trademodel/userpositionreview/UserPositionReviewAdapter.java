package org.example.trademodel.userpositionreview;

public interface UserPositionReviewAdapter {

    UserPositionReviewSummaryDTO buildSummaryForUser(Long userId, Long positionId);

    UserPositionReviewFeedbackResultDTO recordFeedbackForUser(
            Long userId, Long positionId, UserPositionReviewFeedbackReq request);
}
