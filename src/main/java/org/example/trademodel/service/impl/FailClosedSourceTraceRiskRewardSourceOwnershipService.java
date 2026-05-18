package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceRiskRewardSourceOwnershipService;
import org.springframework.stereotype.Service;

@Service
public class FailClosedSourceTraceRiskRewardSourceOwnershipService
        implements SourceTraceRiskRewardSourceOwnershipService {

    @Override
    public SourceTraceRiskRewardSourceOwnershipResult resolveRiskRewardSourceOwnership(
            RuntimeKlineContextDTO runtimeKlineContext
    ) {
        if (runtimeKlineContext == null) {
            return SourceTraceRiskRewardSourceOwnershipResult.missingSource(null, null);
        }
        return SourceTraceRiskRewardSourceOwnershipResult.missingSource(
                runtimeKlineContext.getSymbol(),
                runtimeKlineContext.getTimeframe()
        );
    }
}
