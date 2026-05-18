package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceMultiTimeframeSourceOwnershipService;
import org.springframework.stereotype.Service;

@Service
public class FailClosedSourceTraceMultiTimeframeSourceOwnershipService
        implements SourceTraceMultiTimeframeSourceOwnershipService {

    @Override
    public SourceTraceMultiTimeframeSourceOwnershipResult resolveMultiTimeframeSourceOwnership(
            RuntimeKlineContextDTO runtimeKlineContext
    ) {
        if (runtimeKlineContext == null) {
            return SourceTraceMultiTimeframeSourceOwnershipResult.missingSource(null, null);
        }
        return SourceTraceMultiTimeframeSourceOwnershipResult.missingSource(
                runtimeKlineContext.getSymbol(),
                runtimeKlineContext.getTimeframe()
        );
    }
}
