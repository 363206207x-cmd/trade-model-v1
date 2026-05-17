package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.vo.DecisionResultVO;

/**
 * Dashboard-only RuntimeKline boundary.
 * It must stay fail-closed until persisted OHLCV, freshness, and stale-status sources exist.
 */
public interface DashboardRuntimeKlineContextAdapter {

    RuntimeKlineContextDTO buildUnavailableContext(String symbol, DecisionResultVO decision);
}
