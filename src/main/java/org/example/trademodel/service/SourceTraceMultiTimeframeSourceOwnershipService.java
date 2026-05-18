package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceOwnershipResult;

public interface SourceTraceMultiTimeframeSourceOwnershipService {

    SourceTraceMultiTimeframeSourceOwnershipResult resolveMultiTimeframeSourceOwnership(
            RuntimeKlineContextDTO runtimeKlineContext
    );
}
