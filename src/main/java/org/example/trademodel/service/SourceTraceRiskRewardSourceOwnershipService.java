package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipResult;

public interface SourceTraceRiskRewardSourceOwnershipService {

    SourceTraceRiskRewardSourceOwnershipResult resolveRiskRewardSourceOwnership(RuntimeKlineContextDTO runtimeKlineContext);
}
