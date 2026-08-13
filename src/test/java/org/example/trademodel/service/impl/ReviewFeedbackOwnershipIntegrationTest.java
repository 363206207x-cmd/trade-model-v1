package org.example.trademodel.service.impl;

import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.entity.ReviewResultDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.enums.ReviewTypeEnum;
import org.example.trademodel.service.ReviewCenterService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.example.trademodel.vo.ReviewCenterDashboardVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Tag("core-regression")
class ReviewFeedbackOwnershipIntegrationTest {
    private static final String SHARED_ANALYSIS_ID = "review-owner-shared-analysis";

    @Autowired
    private PersonalUserMapper personalUserMapper;
    @Autowired
    private UserPositionMapper userPositionMapper;
    @Autowired
    private ReviewResultMapper reviewResultMapper;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewCenterService reviewCenterService;

    @Test
    void sharedAnalysisFeedbackRemainsIsolatedByOwnerAndExactPosition() {
        Long userA = insertUser("review.owner.a");
        Long userB = insertUser("review.owner.b");
        UserPositionDO positionA1 = insertPosition(userA, "BTCUSDT", "CLOSED", 1);
        UserPositionDO positionA2 = insertPosition(userA, "BTCUSDT", "CLOSED", 2);
        UserPositionDO positionB = insertPosition(userB, "BTCUSDT", "CLOSED", 3);
        UserPositionDO openB = insertPosition(userB, "BTCUSDT", "OPEN", 4);

        reviewService.saveOrUpdateForUserPosition(
                userA, positionA1.getId(), feedback("OWNER_A_1", "suggestion-a-1"));
        reviewService.saveOrUpdateForUserPosition(
                userA, positionA2.getId(), feedback("OWNER_A_2", "suggestion-a-2"));
        reviewService.saveOrUpdateForUserPosition(
                userB, positionB.getId(), feedback("OWNER_B", "suggestion-b"));

        ReviewResultDO ownerA1 = ownerRow(userA, positionA1.getId());
        ReviewResultDO ownerA2 = ownerRow(userA, positionA2.getId());
        ReviewResultDO ownerB = ownerRow(userB, positionB.getId());
        assertThat(List.of(ownerA1.getId(), ownerA2.getId(), ownerB.getId())).doesNotHaveDuplicates();
        assertThat(ownerA1.getActualOutcome()).isEqualTo("OWNER_A_1");
        assertThat(ownerA2.getActualOutcome()).isEqualTo("OWNER_A_2");
        assertThat(ownerB.getActualOutcome()).isEqualTo("OWNER_B");
        assertThat(reviewResultMapper.selectByAnalysisId(SHARED_ANALYSIS_ID)).isNull();

        reviewService.saveOrUpdateForUserPosition(
                userA, positionA1.getId(), feedback("OWNER_A_UPDATED", "suggestion-a-updated"));
        assertThat(ownerRow(userA, positionA1.getId()).getActualOutcome()).isEqualTo("OWNER_A_UPDATED");
        assertThat(ownerRow(userA, positionA2.getId()).getActualOutcome()).isEqualTo("OWNER_A_2");
        assertThat(ownerRow(userB, positionB.getId()).getActualOutcome()).isEqualTo("OWNER_B");

        assertThatThrownBy(() -> reviewService.saveOrUpdateForUserPosition(
                userA, positionB.getId(), feedback("ATTACK", "must-not-write")))
                .isInstanceOf(UserPositionNotFoundException.class);
        assertThat(ownerRow(userB, positionB.getId()).getActualOutcome()).isEqualTo("OWNER_B");

        assertThatThrownBy(() -> reviewService.saveOrUpdateForUserPosition(
                userB, openB.getId(), feedback("OPEN", "must-not-write")))
                .isInstanceOf(UserPositionConflictException.class)
                .hasMessageContaining("POSITION_NOT_CLOSED");

        reviewService.saveOrUpdate(feedback("SHARED_STATE", "shared-only"));
        assertThat(reviewResultMapper.selectByAnalysisId(SHARED_ANALYSIS_ID)).satisfies(shared -> {
            assertThat(shared.getUserId()).isNull();
            assertThat(shared.getUserPositionId()).isNull();
            assertThat(shared.getReviewScopeKey()).isEqualTo("SHARED");
            assertThat(shared.getActualOutcome()).isEqualTo("SHARED_STATE");
        });
        assertThat(ownerRow(userA, positionA1.getId()).getActualOutcome()).isEqualTo("OWNER_A_UPDATED");
        assertThat(ownerRow(userB, positionB.getId()).getActualOutcome()).isEqualTo("OWNER_B");

        assertThat(reviewResultMapper.listRecentByUserId(userA, 50))
                .extracting(ReviewResultDO::getAdjustmentSuggestion)
                .containsExactlyInAnyOrder("suggestion-a-updated", "suggestion-a-2");
        assertThat(reviewResultMapper.listRecentByUserId(userB, 50))
                .extracting(ReviewResultDO::getAdjustmentSuggestion)
                .containsExactly("suggestion-b");

        ReviewCenterDashboardVO centerA = reviewCenterService.getDashboardForUser(userA);
        ReviewCenterDashboardVO centerB = reviewCenterService.getDashboardForUser(userB);
        assertThat(centerA.getRuleFeedback())
                .extracting(ReviewCenterDashboardVO.RuleFeedbackItem::getSuggestion)
                .containsExactlyInAnyOrder("suggestion-a-updated", "suggestion-a-2")
                .doesNotContain("suggestion-b", "shared-only");
        assertThat(centerB.getRuleFeedback())
                .extracting(ReviewCenterDashboardVO.RuleFeedbackItem::getSuggestion)
                .containsExactly("suggestion-b")
                .doesNotContain("suggestion-a-updated", "suggestion-a-2", "shared-only");
    }

    @Test
    void everyFrozenReviewOutcomePersistsWithoutCollapsingAiRuleOrUserResponsibility() {
        int index = 0;
        for (ReviewTypeEnum reviewType : ReviewTypeEnum.values()) {
            String analysisId = "review-contract-" + index++;
            WriteReviewResultReq req = new WriteReviewResultReq();
            req.setAnalysisId(analysisId);
            req.setReviewType(reviewType.name());
            req.setOutcome("OUTCOME_" + reviewType.name());
            req.setExecutionDeviation("USER_EXECUTION_DEVIATION_" + reviewType.name());
            req.setAiAssessment("AI_RESPONSIBILITY_" + reviewType.name());
            req.setRuleAssessment("RULE_RESPONSIBILITY_" + reviewType.name());
            req.setRuleFeedback("RULE_FEEDBACK_" + reviewType.name());
            req.setMetricsJson(readyMetricsJson());

            reviewService.saveOrUpdate(req);

            assertThat(reviewResultMapper.selectByAnalysisId(analysisId)).satisfies(saved -> {
                assertThat(saved.getReviewType()).isEqualTo(reviewType.name());
                assertThat(saved.getOutcome()).isEqualTo("OUTCOME_" + reviewType.name());
                assertThat(saved.getExecutionDeviation()).contains(reviewType.name());
                assertThat(saved.getAiAssessment()).contains(reviewType.name());
                assertThat(saved.getRuleAssessment()).contains(reviewType.name());
                assertThat(saved.getRuleFeedback()).contains(reviewType.name());
                assertThat(saved.getMetricsJson())
                        .contains("structuredOutputCompletenessRate", "fabricatedFillRate");
            });
        }
    }

    private ReviewResultDO ownerRow(Long userId, Long positionId) {
        return reviewResultMapper.selectByUserPositionScope(
                SHARED_ANALYSIS_ID,
                userId,
                positionId,
                "USER:" + userId + ":POSITION:" + positionId);
    }

    private static String readyMetricsJson() {
        return """
                {
                  "schemaVersion":"FUNDAMENTAL_AI_V4_1_REVIEW_METRICS",
                  "dataState":"READY",
                  "evidenceTraceabilityRate":1.0,
                  "structuredOutputCompletenessRate":{
                    "byRole":{"GPT_FINAL":1.0,"GEMINI_REVIEW":1.0,"GROK_CHALLENGE":1.0},
                    "byPlanMode":{"CONFIRMATION":1.0,"REDUCED":1.0,"PREPARATION":1.0,"OBSERVATION":1.0}
                  },
                  "unsupportedConclusionRate":0.0,
                  "fabricatedFillRate":0.0,
                  "confidenceCalibration":0.95,
                  "falsePositiveCount":0,
                  "falseNegativeCount":0,
                  "missedValidOpportunityCount":0,
                  "planModeEffectiveness":{"CONFIRMATION":0.9,"REDUCED":0.8,"PREPARATION":0.7,"OBSERVATION":0.6},
                  "effectiveDowngradeRate":0.8,
                  "failurePathHitRate":0.5,
                  "opportunityOmissionQuality":{"MISSED_VALID":0,"PUSHED_NOT_FILLED_VALID":0,"BLOCKED_BY_RISK_VALID":0}
                }
                """;
    }

    private static WriteReviewResultReq feedback(String outcome, String suggestion) {
        WriteReviewResultReq req = new WriteReviewResultReq();
        req.setAnalysisId(SHARED_ANALYSIS_ID);
        req.setErrorType("RULE_TOO_STRICT");
        req.setActualOutcome(outcome);
        req.setAdjustmentSuggestion(suggestion);
        return req;
    }

    private Long insertUser(String username) {
        PersonalUserDO row = new PersonalUserDO();
        row.setUsername(username);
        row.setPasswordHash("{noop}test-only");
        row.setCreatedAt(LocalDateTime.now());
        personalUserMapper.insert(row);
        return row.getId();
    }

    private UserPositionDO insertPosition(Long userId, String symbol, String status, int minute) {
        LocalDateTime openedAt = LocalDateTime.of(2026, 7, 1, 8, minute);
        UserPositionDO row = new UserPositionDO();
        row.setUserId(userId);
        row.setAssetSymbol(symbol);
        row.setSide("LONG");
        row.setStatus(status);
        row.setEntryPrice(new BigDecimal("100"));
        row.setQuantity(new BigDecimal("0.25"));
        row.setLeverage(new BigDecimal("2"));
        row.setStopLoss(new BigDecimal("90"));
        row.setTakeProfit(new BigDecimal("120"));
        row.setOpenedAt(openedAt);
        if ("CLOSED".equals(status)) {
            row.setClosedAt(openedAt.plusHours(1));
            row.setClosePrice(new BigDecimal("110"));
            row.setCloseReason("manual fixture close");
        }
        row.setSourceType("MANUAL_INDEPENDENT");
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
        row.setCreatedAt(openedAt);
        row.setUpdatedAt(openedAt);
        userPositionMapper.insert(row);
        return row;
    }
}
