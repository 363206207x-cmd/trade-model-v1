package org.example.trademodel.controller;

import java.util.List;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
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
    public ApiResponse<List<ScoreItemVO>> buildScore(@RequestBody AssetAnalysisVO request) {
        List<ScoreItemVO> scores = scoreService.buildScoreListFromEnvironment(request.getMarketEnvironment());
        return ApiResponse.success(scores);
    }

    @GetMapping("/list")
    public ApiResponse<List<ScoreItemVO>> getScoreList(@RequestParam String symbol, @RequestParam String timeframe) {
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary(symbol + "-" + timeframe);
        List<ScoreItemVO> scores = scoreService.buildScoreListFromEnvironment(env);
        return ApiResponse.success(scores);
    }
}
