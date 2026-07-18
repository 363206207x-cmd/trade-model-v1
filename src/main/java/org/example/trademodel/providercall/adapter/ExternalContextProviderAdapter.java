package org.example.trademodel.providercall.adapter;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.snapshot.ExternalContextSnapshot;

public interface ExternalContextProviderAdapter {
    ProviderAdapterResponse<ExternalContextSnapshot> fetch(ProviderSymbolMapping mapping, String traceId);
}
