package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceEventSourceOwnershipService;
import org.springframework.stereotype.Service;

@Service
public class FailClosedSourceTraceEventSourceOwnershipService
        implements SourceTraceEventSourceOwnershipService {

    @Override
    public SourceTraceEventSourceOwnershipResult resolveEventSourceOwnership(
            RuntimeKlineContextDTO runtimeKlineContext
    ) {
        if (runtimeKlineContext == null) {
            return SourceTraceEventSourceOwnershipResult.missingSource(null, null);
        }
        return SourceTraceEventSourceOwnershipResult.missingSource(
                runtimeKlineContext.getSymbol(),
                runtimeKlineContext.getTimeframe()
        );
    }
}
