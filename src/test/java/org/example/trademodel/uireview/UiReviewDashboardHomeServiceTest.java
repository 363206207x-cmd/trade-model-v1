package org.example.trademodel.uireview;

import org.example.trademodel.vo.DashboardHomeVO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class UiReviewDashboardHomeServiceTest {
    private final UiReviewDashboardHomeService service = new UiReviewDashboardHomeService();

    @Test
    void fullStateFixtureIsCompleteAndUsesLegalSelectedOpportunitySemantics() {
        DashboardHomeVO home = service.getHomeForUser(1L, "BTCUSDT", 6, null);

        assertThat(home.getAssets()).hasSize(6)
                .allSatisfy(asset -> {
                    assertThat(asset.getOpportunityId()).isNotBlank();
                    assertThat(asset.getAnalysisId()).isNotBlank();
                    assertThat(asset.getOpportunityScore()).isNotNull();
                });
        assertThat(home.getPositions()).hasSize(3)
                .allSatisfy(position -> {
                    assertThat(position.getEntryPrice()).isNotNull();
                    assertThat(position.getOpenedAt()).isNotNull();
                    assertThat(position.getMarkPriceFresh()).isTrue();
                    assertThat(position.getMonitorConclusion()).isNotBlank();
                    assertThat(position.getSuggestedAction()).isNotBlank();
                });
        assertThat(home.getExecutionSuggestion().getFinalPlan()).isTrue();
        assertThat(home.getExecutionSuggestion().getValidationStatus()).isEqualTo("PASS");
        assertThat(home.getExecutionSuggestion().getFinalPlanMode()).isEqualTo("PREPARATION");
        assertThat(home.getExecutionSuggestion().getStopZone()).isEqualTo("61,500 下方失效");
        assertThat(home.getSelectedAssetContext().getOpportunityState()).isEqualTo("WAITING_TRIGGER");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getTabs()).allSatisfy(role -> {
            assertThat(role.getResultAvailable()).isTrue();
            assertThat(role.getRoleState()).isEqualTo("READY");
            assertThat(role.getAnalysisId()).isNotBlank();
            assertThat(role.getTraceId()).isNotBlank();
        });
        assertThat(home.getAiDecision().getConsistency().getConflictLevel())
                .isEqualTo("LEVEL_2_MINOR_DISAGREEMENT");
        DashboardHomeVO.AiTabVO gpt = home.getAiDecision().getTabs().get(0);
        DashboardHomeVO.AiTabVO gemini = home.getAiDecision().getTabs().get(1);
        DashboardHomeVO.AiTabVO grok = home.getAiDecision().getTabs().get(2);
        assertThat(gpt.getCandidateSummary().confidence()).isEqualTo("MEDIUM");
        assertThat(gpt.getDecisionSummary()).contains("先不追涨", "重新校验");
        assertThat(gemini.getPlanModeAdjustment()).isEqualTo("DOWNGRADE_ONE");
        assertThat(gemini.getFinalDirectionImpact()).isEqualTo("SAME_FAMILY_DOWNGRADE");
        assertThat(gemini.getConfidenceAdjustment()).isEqualTo("DOWNGRADE_ONE");
        assertThat(gemini.getRiskAdjustment()).isEqualTo("RAISE_ONE");
        assertThat(grok.getPlanModeImpact()).isEqualTo("DOWNGRADE_ONE");
        assertThat(grok.getChallengeSummary()).contains("不追涨");
        assertThat(home.getDerivatives().getSource()).isEqualTo("CoinGlass v4");
        assertThat(home.getDerivatives().getOpenInterestStructure()).contains("未平仓量");
        assertThat(home.getDerivatives().getDecisionImpact()).isEqualTo("限制追涨，等待确认");
    }

    @Test
    void nonSelectedOpportunityDoesNotBorrowTheSelectedFinalOrAiResult() {
        DashboardHomeVO home = service.getHomeForUser(1L, "ETHUSDT", 6, null);

        assertThat(home.getSelectedSymbol()).isEqualTo("ETHUSDT");
        assertThat(home.getExecutionSuggestion().getFinalPlan()).isFalse();
        assertThat(home.getAiDecision().getTabs()).allSatisfy(role ->
                assertThat(role.getResultAvailable()).isFalse());
    }

    @Test
    void fixtureBeanDoesNotExistWithoutUiReviewProfile() {
        try (AnnotationConfigApplicationContext normal = new AnnotationConfigApplicationContext()) {
            normal.register(UiReviewDashboardHomeService.class);
            normal.refresh();
            assertThat(normal.getBeansOfType(UiReviewDashboardHomeService.class)).isEmpty();
        }
        try (AnnotationConfigApplicationContext review = new AnnotationConfigApplicationContext()) {
            review.getEnvironment().setActiveProfiles("ui-review");
            review.register(UiReviewDashboardHomeService.class);
            review.refresh();
            assertThat(review.getBeansOfType(UiReviewDashboardHomeService.class)).hasSize(1);
        }
    }
}
