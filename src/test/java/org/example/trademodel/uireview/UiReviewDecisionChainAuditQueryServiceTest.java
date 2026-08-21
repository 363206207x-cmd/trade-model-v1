package org.example.trademodel.uireview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.vo.DecisionChainAuditVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UiReviewDecisionChainAuditQueryServiceTest {
    private final UiReviewDecisionChainAuditQueryService service =
            new UiReviewDecisionChainAuditQueryService(new ObjectMapper());

    @Test
    void routeFixturesCoverFormalModesReviewsFailurePathsAndBlockedRoles() {
        assertThat(audit("ui-review-analysis-preview").getAnalysis().analysisMode())
                .isEqualTo("ANALYSIS_PREVIEW");
        assertThat(audit("ui-review-analysis-unknown").getAnalysis().analysisMode())
                .isEqualTo("UNKNOWN");
        assertThat(role("ui-review-gemini-approve", "GEMINI_REVIEW").reviewResult())
                .isEqualTo("APPROVE");
        assertThat(role("ui-review-gemini-downgrade", "GEMINI_REVIEW").reviewResult())
                .isEqualTo("DOWNGRADE");
        assertThat(role("ui-review-gemini-reject", "GEMINI_REVIEW").reviewResult())
                .isEqualTo("REJECT_CANDIDATE");
        assertThat(role("ui-review-gemini-risk", "GEMINI_REVIEW").reviewResult())
                .isEqualTo("RISK_WARNING");
        assertThat(role("ui-review-grok-found", "GROK_CHALLENGE").failurePaths()).hasSize(1);
        assertThat(role("ui-review-grok-found-empty", "GROK_CHALLENGE").failurePaths()).isEmpty();
        assertThat(role("ui-review-grok-no-path", "GROK_CHALLENGE").failurePathState())
                .isEqualTo("NO_VERIFIABLE_FAILURE_PATH");
        for (String state : new String[]{"unavailable", "error", "fallback"}) {
            AiRoleResultsPayload.RolePayload role = role("ui-review-role-" + state, "GPT_FINAL");
            assertThat(role.resultAvailable()).isFalse();
            assertThat(role.roleState()).isEqualTo(state.toUpperCase());
            assertThat(role.coreJudgment()).isNotNull();
        }
    }

    private DecisionChainAuditVO audit(String analysisId) {
        return service.queryForUser(1L, analysisId, null, null).orElseThrow();
    }

    private AiRoleResultsPayload.RolePayload role(String analysisId, String role) {
        return audit(analysisId).getAiRoleResults().roles().get(role);
    }
}
