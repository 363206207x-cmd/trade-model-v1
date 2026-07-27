package org.example.trademodel.service;

import org.example.trademodel.vo.DashboardHomeVO;

public interface DashboardHomeService {
    DashboardHomeVO getHome(String selectedSymbol, Integer limit, Long selectedPositionId);

    DashboardHomeVO getHomeForUser(Long userId, String selectedSymbol, Integer limit, Long selectedPositionId);

    default DashboardHomeVO getHomeForUser(Long userId, String selectedSymbol, Integer limit) {
        return getHomeForUser(userId, selectedSymbol, limit, null);
    }

    default DashboardHomeVO getHome(String selectedSymbol, Integer limit) {
        return getHome(selectedSymbol, limit, null);
    }
}
