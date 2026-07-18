package org.example.trademodel.providercall.adapter;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.springframework.stereotype.Service;

@Service
public class NoCallPriceProviderAdapter implements PriceProviderAdapter, NoCallProviderAdapter {
    @Override
    public ProviderAdapterResponse<MarketPriceSnapshot> fetch(ProviderSymbolMapping mapping, String traceId) {
        return ProviderAdapterResponse.failed(UnifiedSourceStatus.DISABLED, 0,
                "PRICE_ADAPTER_EXTERNAL_CALLS_DISABLED", null);
    }
}
