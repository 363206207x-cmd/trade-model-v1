package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceOwnershipResult;
import org.example.trademodel.service.SourceTraceLiquiditySourceOwnershipService;
import org.springframework.stereotype.Service;

@Service
public class FailClosedSourceTraceLiquiditySourceOwnershipService
        implements SourceTraceLiquiditySourceOwnershipService {

    @Override
    public SourceTraceLiquiditySourceOwnershipResult resolveLiquiditySourceOwnership(
            RuntimeKlineContextDTO runtimeKlineContext
    ) {
        if (runtimeKlineContext == null) {
            return SourceTraceLiquiditySourceOwnershipResult.missingSource(null, null);
        }
        return SourceTraceLiquiditySourceOwnershipResult.missingSource(
                runtimeKlineContext.getSymbol(),
                runtimeKlineContext.getTimeframe()
        );
    }
}
