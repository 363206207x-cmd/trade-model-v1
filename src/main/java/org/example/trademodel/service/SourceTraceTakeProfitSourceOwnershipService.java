package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipResult;

public interface SourceTraceTakeProfitSourceOwnershipService {

    SourceTraceTakeProfitSourceOwnershipResult resolveTakeProfitSourceOwnership(RuntimeKlineContextDTO runtimeKlineContext);
}
