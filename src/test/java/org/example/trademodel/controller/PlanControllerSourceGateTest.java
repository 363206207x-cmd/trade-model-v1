package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.service.PlanService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanControllerSourceGateTest {

    @Test
    void generatePlanExposesSourceGateResultInResponse() {
        ExecutionPlanVO plan = new ExecutionPlanVO();
        plan.setExecutionPlanStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        plan.setSourceGateStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        plan.setSourceGateComplete(true);
        plan.setSourceCompletenessSummary("source gate VALID");
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);

        PlanController controller = new PlanController(new FixedPlanService(plan));
        ApiResponse<ExecutionPlanVO> response = controller.generatePlan(new AssetAnalysisVO());

        assertThat(response.getData().getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        assertThat(response.getData().getSourceGateComplete()).isTrue();
        assertThat(response.getData().getNotTradeInstruction()).isTrue();
        assertThat(response.getData().getNotExecutable()).isTrue();
        assertThat(response.getData().getNotOrderExecution()).isTrue();
        assertThat(response.getData().getNotUserPositionCreation()).isTrue();
    }

    private static class FixedPlanService implements PlanService {
        private final ExecutionPlanVO plan;

        private FixedPlanService(ExecutionPlanVO plan) {
            this.plan = plan;
        }

        @Override
        public ExecutionPlanVO buildExecutionPlanFromEnvironment(MarketEnvironmentVO env) {
            return plan;
        }

        @Override
        public ExecutionPlanVO generateExecutionPlan(
                DecisionBundleVO decision,
                List<ScoreItemVO> scoreList,
                MarketEnvironmentVO marketEnvironment,
                AssetAnalysisVO analysisContext
        ) {
            return plan;
        }

        @Override
        public ExecutionPlanVO generateExecutionPlan(
                DecisionBundleVO decision,
                List<ScoreItemVO> scoreList,
                MarketEnvironmentVO marketEnvironment,
                AssetAnalysisVO analysisContext,
                SourceTraceDTO sourceTrace
        ) {
            return plan;
        }

        @Override
        public ExecutionPlanVO generateExecutionPlan(
                DecisionBundleVO decision,
                List<ScoreItemVO> scoreList,
                MarketEnvironmentVO marketEnvironment,
                AssetAnalysisVO analysisContext,
                SourceTraceDTO sourceTrace,
                DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
        ) {
            return plan;
        }
    }
}
