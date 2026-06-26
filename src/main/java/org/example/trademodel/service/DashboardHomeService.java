package org.example.trademodel.service;

import org.example.trademodel.vo.DashboardHomeVO;

public interface DashboardHomeService {
    DashboardHomeVO getHome(String selectedSymbol, Integer limit);
}
