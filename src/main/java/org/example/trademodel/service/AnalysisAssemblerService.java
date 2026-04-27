package org.example.trademodel.service;

import org.example.trademodel.vo.AssetAnalysisVO;

public interface AnalysisAssemblerService {
    AssetAnalysisVO assemble(String symbol, String timeframe);
}
