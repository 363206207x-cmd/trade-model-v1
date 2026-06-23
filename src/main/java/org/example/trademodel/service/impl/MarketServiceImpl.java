package org.example.trademodel.service.impl;

import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.service.MarketService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.springframework.stereotype.Service;

@Service
public class MarketServiceImpl implements MarketService {

    private final AnalysisSchedulerService analysisSchedulerService;

    public MarketServiceImpl(AnalysisSchedulerService analysisSchedulerService) {
        this.analysisSchedulerService = analysisSchedulerService;
    }

    @Override
    public MarketEnvironmentVO getMarketEnvironment(String symbol, String timeframe) {
        AnalysisRunResult result = analysisSchedulerService.runManual(
                symbol, timeframe, RequestIdSupport.currentOrNew(), null);
        AssetAnalysisVO analysis = result.getAnalysis();
        if (analysis == null || analysis.getMarketEnvironment() == null) {
            return new MarketEnvironmentVO();
        }
        return analysis.getMarketEnvironment();
    }
}
