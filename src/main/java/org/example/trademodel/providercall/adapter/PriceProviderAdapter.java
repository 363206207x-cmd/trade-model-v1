package org.example.trademodel.providercall.adapter;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;

public interface PriceProviderAdapter {
    ProviderAdapterResponse<MarketPriceSnapshot> fetch(ProviderSymbolMapping mapping, String traceId);
}
