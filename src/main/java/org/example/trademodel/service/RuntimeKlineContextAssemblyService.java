package org.example.trademodel.service;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;

public interface RuntimeKlineContextAssemblyService {

    RuntimeKlineContextDTO assemble(PersistedOhlcvReadinessResult readinessResult);
}
