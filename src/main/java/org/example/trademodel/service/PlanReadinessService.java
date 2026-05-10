package org.example.trademodel.service;

import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.PlanReadinessVO;

public interface PlanReadinessService {

    PlanReadinessVO derive(DecisionResultVO decision);
}
