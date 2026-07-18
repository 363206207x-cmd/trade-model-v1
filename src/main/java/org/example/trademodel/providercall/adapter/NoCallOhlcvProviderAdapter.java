package org.example.trademodel.providercall.adapter;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.springframework.stereotype.Service;

@Service
public class NoCallOhlcvProviderAdapter implements OhlcvProviderAdapter, NoCallProviderAdapter {
    @Override
    public ProviderAdapterResponse<OhlcvIngestionResult> refresh(ProviderSymbolMapping mapping,
                                                                 String timeframe,
                                                                 int limit,
                                                                 String traceId) {
        return ProviderAdapterResponse.failed(UnifiedSourceStatus.DISABLED, 0,
                "OHLCV_ADAPTER_EXTERNAL_CALLS_DISABLED", null);
    }
}
