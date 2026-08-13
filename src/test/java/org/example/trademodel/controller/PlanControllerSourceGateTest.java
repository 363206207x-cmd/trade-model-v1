package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanControllerSourceGateTest {

    @Test
    void directPlanGenerationCannotBypassCandidateResolverAndRuleValidation() {
        PlanController controller = new PlanController();
        ApiResponse<ExecutionPlanVO> response = controller.generatePlan(new AssetAnalysisVO());

        assertThat(response.getCode()).isEqualTo(409);
        assertThat(response.getData()).isNull();
        assertThat(response.getMsg())
                .isEqualTo("DIRECT_PLAN_GENERATION_DISABLED_USE_ANALYSIS_RUN_DECISION_CHAIN");
        assertThat(response.getServerTime().getOffset()).isEqualTo(java.time.ZoneOffset.UTC);
    }
}
