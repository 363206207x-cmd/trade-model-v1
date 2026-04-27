package org.example.trademodel.service.impl;

import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanServiceImplTest {

    private final PlanServiceImpl service = new PlanServiceImpl();

    @Test
    void generateExecutionPlan_setsPlanModeNonNullAndWithinAllowedValues() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(false);

        ExecutionPlanVO plan = service.generateExecutionPlan(decision, null, null, null);

        assertThat(plan.getPlanMode()).isNotBlank();
        assertThat(plan.getPlanMode()).isIn(
                ExecutionPlanVO.PLAN_MODE_ADVISORY,
                ExecutionPlanVO.PLAN_MODE_SEMI_STRUCTURED
        );
    }
}
