package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceOwnershipResult;

public interface SourceTraceLiquiditySourceOwnershipService {

    SourceTraceLiquiditySourceOwnershipResult resolveLiquiditySourceOwnership(RuntimeKlineContextDTO runtimeKlineContext);
}
