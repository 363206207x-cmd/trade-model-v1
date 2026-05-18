package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceOwnershipResult;

public interface SourceTraceWickSourceOwnershipService {

    SourceTraceWickSourceOwnershipResult resolveWickSourceOwnership(RuntimeKlineContextDTO runtimeKlineContext);
}
