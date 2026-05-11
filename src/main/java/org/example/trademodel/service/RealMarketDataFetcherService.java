package org.example.trademodel.service;

import org.example.trademodel.market.client.MarketKlineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RealMarketDataFetcherService {

    private static final Logger logger = LoggerFactory.getLogger(RealMarketDataFetcherService.class);

    private final AnalysisAssemblerService analysisAssemblerService;
    private final MarketKlineClient marketKlineClient;

    @Autowired
    public RealMarketDataFetcherService(@Lazy AnalysisAssemblerService analysisAssemblerService,
                                        MarketKlineClient marketKlineClient) {
        this.analysisAssemblerService = analysisAssemblerService;
        this.marketKlineClient = marketKlineClient;
        logger.info("RealMarketDataFetcherService initialized successfully with marketKlineClient={}",
                marketKlineClient.getClass().getSimpleName());
    }

    /**
     * 新增：返回真实 K 线数据（供 DecisionEngineService 使用）
     */
    public List<String[]> fetchKlines(String symbol, String interval, int limit) {
        String fetchId = "FETCH-" + Instant.now().toEpochMilli();
        try {
            return marketKlineClient.fetchKlines(symbol, interval, limit);
        } catch (Exception e) {
            logger.error("[{}] fetchKlines failed for {} {}: {}", fetchId, symbol, interval, e.getMessage());
            return List.of();
        }
    }

    // 原有方法保持不变（兼容旧代码）
    public void fetchRealMarketData(String symbol, String interval) {
        String fetchId = "FETCH-" + Instant.now().toEpochMilli();
        logger.info("[{}] === REAL MARKET DATA FETCH START === Symbol: {} | Interval: {}", fetchId, symbol, interval);
        try {
            // 继续调用 assemble 进行完整落库
            analysisAssemblerService.assemble(symbol, interval);
            logger.info("[{}] === FETCH COMPLETED SUCCESSFULLY ===", fetchId);
        } catch (Exception e) {
            logger.error("[{}] FETCH FAILED: {}", fetchId, e.getMessage());
        }
    }
}
