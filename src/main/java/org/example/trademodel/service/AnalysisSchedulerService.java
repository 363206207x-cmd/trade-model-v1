package org.example.trademodel.service;

import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.common.ApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisSchedulerService {

    private final AnalysisAssemblerService analysisAssemblerService;

    private final ConcurrentHashMap<String, AssetAnalysisVO> analysisCache = new ConcurrentHashMap<>();

    @Autowired
    public AnalysisSchedulerService(AnalysisAssemblerService analysisAssemblerService) {
        this.analysisAssemblerService = analysisAssemblerService;
    }

    public ApiResponse<AssetAnalysisVO> executeAnalysis(String symbol, String timeframe, String triggerType) {
        String cacheKey = symbol + ":" + timeframe + ":" + triggerType;

        if (analysisCache.containsKey(cacheKey)) {
            return ApiResponse.success(analysisCache.get(cacheKey));
        }

        AssetAnalysisVO result = analysisAssemblerService.assemble(symbol, timeframe);

        analysisCache.put(cacheKey, result);

        // 5秒后自动清理缓存
        new Thread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException e) {}
            analysisCache.remove(cacheKey);
        }).start();

        return ApiResponse.success(result);
    }
}
