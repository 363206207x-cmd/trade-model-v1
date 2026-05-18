package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceTakeProfitSourceOwnershipService;
import org.springframework.stereotype.Service;

@Service
public class FailClosedSourceTraceTakeProfitSourceOwnershipService
        implements SourceTraceTakeProfitSourceOwnershipService {

    @Override
    public SourceTraceTakeProfitSourceOwnershipResult resolveTakeProfitSourceOwnership(
            RuntimeKlineContextDTO runtimeKlineContext
    ) {
        if (runtimeKlineContext == null) {
            return SourceTraceTakeProfitSourceOwnershipResult.missingSource(null, null);
        }
        return SourceTraceTakeProfitSourceOwnershipResult.missingSource(
                runtimeKlineContext.getSymbol(),
                runtimeKlineContext.getTimeframe()
        );
    }
}
