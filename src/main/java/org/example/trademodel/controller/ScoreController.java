package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/score")
public class ScoreController {

    private final ScoreService scoreService;

    @Autowired
    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @PostMapping("/build")
    public ApiResponse<java.util.List<ScoreItemVO>> buildScore(@RequestBody AssetAnalysisVO request) {
        return ApiResponse.conflict(
                "DIRECT_SCORE_BUILD_DISABLED_USE_ANALYSIS_RUN_DECISION_CHAIN");
    }

    @GetMapping("/list")
    public ApiResponse<java.util.List<ScoreItemVO>> getScoreList(
            @RequestParam String symbol, @RequestParam String timeframe) {
        return ApiResponse.conflict(
                "DIRECT_SCORE_BUILD_DISABLED_USE_ANALYSIS_RUN_DECISION_CHAIN");
    }
}
