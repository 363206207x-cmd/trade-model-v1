package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceStopSourceOwnershipService;
import org.springframework.stereotype.Service;

@Service
public class FailClosedSourceTraceStopSourceOwnershipService implements SourceTraceStopSourceOwnershipService {

    @Override
    public SourceTraceStopSourceOwnershipResult resolveStopSourceOwnership(RuntimeKlineContextDTO runtimeKlineContext) {
        if (runtimeKlineContext == null) {
            return SourceTraceStopSourceOwnershipResult.missingSource(null, null);
        }
        return SourceTraceStopSourceOwnershipResult.missingSource(
                runtimeKlineContext.getSymbol(),
                runtimeKlineContext.getTimeframe()
        );
    }
}
