package org.example.trademodel.providercall.adapter;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;

public interface DerivativesProviderAdapter {
    ProviderAdapterResponse<DerivativesRiskSnapshot> fetch(ProviderSymbolMapping mapping, String traceId);
}
