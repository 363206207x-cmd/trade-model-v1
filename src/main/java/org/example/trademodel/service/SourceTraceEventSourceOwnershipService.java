package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipResult;

public interface SourceTraceEventSourceOwnershipService {

    SourceTraceEventSourceOwnershipResult resolveEventSourceOwnership(RuntimeKlineContextDTO runtimeKlineContext);
}
