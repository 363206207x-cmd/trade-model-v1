package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.service.SourceTraceEntrySourceOwnershipService;
import org.springframework.stereotype.Service;

@Service
public class FailClosedSourceTraceEntrySourceOwnershipService implements SourceTraceEntrySourceOwnershipService {

    @Override
    public SourceTraceEntrySourceOwnershipResult resolveEntrySourceOwnership(RuntimeKlineContextDTO runtimeKlineContext) {
        if (runtimeKlineContext == null) {
            return SourceTraceEntrySourceOwnershipResult.missingSource(null, null);
        }
        return SourceTraceEntrySourceOwnershipResult.missingSource(
                runtimeKlineContext.getSymbol(),
                runtimeKlineContext.getTimeframe()
        );
    }
}
