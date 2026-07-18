package org.example.trademodel.providercall.adapter;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.springframework.stereotype.Service;

@Service
public class NoCallDerivativesProviderAdapter implements DerivativesProviderAdapter, NoCallProviderAdapter {
    @Override
    public ProviderAdapterResponse<DerivativesRiskSnapshot> fetch(ProviderSymbolMapping mapping, String traceId) {
        return ProviderAdapterResponse.failed(UnifiedSourceStatus.DISABLED, 0,
                "DERIVATIVES_ADAPTER_EXTERNAL_CALLS_DISABLED", null);
    }
}
