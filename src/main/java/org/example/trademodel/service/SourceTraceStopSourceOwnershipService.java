package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;

public interface SourceTraceStopSourceOwnershipService {

    SourceTraceStopSourceOwnershipResult resolveStopSourceOwnership(RuntimeKlineContextDTO runtimeKlineContext);
}
