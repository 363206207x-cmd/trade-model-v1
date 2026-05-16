package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;

public interface SourceAssembler {

    SourceTraceDTO assembleSourceTrace(
            RuntimeKlineContextDTO runtimeKlineContext,
            DerivativesRiskContextDTO derivativesRiskContext
    );
}
