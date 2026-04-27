package org.example.trademodel.service;

import org.example.trademodel.vo.RunBaselineVO;

public interface RunBaselineService {

    RunBaselineVO getRunBaseline(int windowMinutes);
}
