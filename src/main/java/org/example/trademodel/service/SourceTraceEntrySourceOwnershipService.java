package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;

public interface SourceTraceEntrySourceOwnershipService {

    SourceTraceEntrySourceOwnershipResult resolveEntrySourceOwnership(RuntimeKlineContextDTO runtimeKlineContext);
}
