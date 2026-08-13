package org.example.trademodel.controller;

import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    @PostMapping("/generate")
    public ApiResponse<ExecutionPlanVO> generatePlan(@RequestBody AssetAnalysisVO request) {
        return ApiResponse.conflict(
                "DIRECT_PLAN_GENERATION_DISABLED_USE_ANALYSIS_RUN_DECISION_CHAIN");
    }
}
