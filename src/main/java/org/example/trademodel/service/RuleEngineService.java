package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.ExecutionPlanVO;

public interface RuleEngineService {
    RuleBaseOutput execute(DecisionContext ctx);

    default RuleBaseOutput execute(DecisionContext ctx, SourceTraceDTO sourceTrace) {
        if (sourceTrace == null || !sourceTrace.hasRequiredBoundarySources()) {
            RuleBaseOutput output = new RuleBaseOutput();
            output.setCanExecute(false);
            output.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
            if (sourceTrace != null && sourceTrace.getFallbackStatus() == SourceTraceFallbackStatusEnum.WATCH_ONLY) {
                output.setConfidenceLevel(ExecutionPlanVO.READINESS_WATCH_ONLY);
                output.setRiskLevel("SOURCE_TRACE_WATCH_ONLY");
            } else if (sourceTrace != null
                    && sourceTrace.getFallbackStatus() == SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY) {
                output.setConfidenceLevel(ExecutionPlanVO.READINESS_WATCH_ONLY);
                output.setRiskLevel("SOURCE_TRACE_SAFE_FAIL_CLOSED_ONLY");
            } else {
                output.setConfidenceLevel(ExecutionPlanVO.READINESS_INCOMPLETE);
                output.setRiskLevel("SOURCE_TRACE_INCOMPLETE");
            }
            return output;
        }

        RuleBaseOutput output = execute(ctx);
        if (output == null) {
            output = new RuleBaseOutput();
        }
        output.setCanExecute(false);
        output.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        return output;
    }
}
