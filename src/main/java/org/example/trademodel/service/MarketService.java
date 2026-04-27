package org.example.trademodel.service;

import org.example.trademodel.vo.MarketEnvironmentVO;

public interface MarketService {

    MarketEnvironmentVO getMarketEnvironment(String symbol, String timeframe);
}
