package org.example.trademodel.providercall.adapter;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.snapshot.ExternalContextSnapshot;
import org.springframework.stereotype.Service;

@Service
public class NoCallExternalContextProviderAdapter implements ExternalContextProviderAdapter, NoCallProviderAdapter {
    @Override
    public ProviderAdapterResponse<ExternalContextSnapshot> fetch(ProviderSymbolMapping mapping, String traceId) {
        return ProviderAdapterResponse.failed(UnifiedSourceStatus.DISABLED, 0,
                "EXTERNAL_CONTEXT_ADAPTER_EXTERNAL_CALLS_DISABLED", null);
    }
}
