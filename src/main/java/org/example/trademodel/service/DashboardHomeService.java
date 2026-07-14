package org.example.trademodel.service;

import org.example.trademodel.vo.DashboardHomeVO;

public interface DashboardHomeService {
    DashboardHomeVO getHome(String selectedSymbol, Integer limit, Long selectedPositionId);

    default DashboardHomeVO getHome(String selectedSymbol, Integer limit) {
        return getHome(selectedSymbol, limit, null);
    }
}
