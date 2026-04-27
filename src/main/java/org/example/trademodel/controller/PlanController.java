package org.example.trademodel.controller;

import org.example.trademodel.service.PlanService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanService planService;

    @Autowired
    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/generate")
    public ApiResponse<ExecutionPlanVO> generatePlan(@RequestBody AssetAnalysisVO request) {
        ExecutionPlanVO plan = planService.generateExecutionPlan(
            request.getDecisionBundle(), 
            request.getScoreList(), 
            request.getMarketEnvironment(), 
            request);
        return ApiResponse.success(plan);
    }
}
