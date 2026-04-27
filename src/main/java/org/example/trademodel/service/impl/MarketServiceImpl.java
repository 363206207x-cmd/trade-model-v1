package org.example.trademodel.service.impl;

import org.example.trademodel.service.AnalysisAssemblerService;
import org.example.trademodel.service.MarketService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.springframework.stereotype.Service;

@Service
public class MarketServiceImpl implements MarketService {

    private final AnalysisAssemblerService analysisAssemblerService;

    public MarketServiceImpl(AnalysisAssemblerService analysisAssemblerService) {
        this.analysisAssemblerService = analysisAssemblerService;
    }

    @Override
    public MarketEnvironmentVO getMarketEnvironment(String symbol, String timeframe) {
        // 调用完整分析链路（包含落库）
        AssetAnalysisVO analysis = analysisAssemblerService.assemble(symbol, timeframe);
        return analysis.getMarketEnvironment();
    }
}
