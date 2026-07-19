package org.example.trademodel.providercall.adapter;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;

public interface OhlcvProviderAdapter {
    ProviderAdapterResponse<OhlcvIngestionResult> refresh(ProviderSymbolMapping mapping,
                                                          String timeframe,
                                                          int limit,
                                                          String traceId);
}
