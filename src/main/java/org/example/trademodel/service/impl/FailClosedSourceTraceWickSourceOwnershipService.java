package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceWickSourceOwnershipService;
import org.springframework.stereotype.Service;

@Service
public class FailClosedSourceTraceWickSourceOwnershipService
        implements SourceTraceWickSourceOwnershipService {

    @Override
    public SourceTraceWickSourceOwnershipResult resolveWickSourceOwnership(
            RuntimeKlineContextDTO runtimeKlineContext
    ) {
        if (runtimeKlineContext == null) {
            return SourceTraceWickSourceOwnershipResult.missingSource(null, null);
        }
        return SourceTraceWickSourceOwnershipResult.missingSource(
                runtimeKlineContext.getSymbol(),
                runtimeKlineContext.getTimeframe()
        );
    }
}
