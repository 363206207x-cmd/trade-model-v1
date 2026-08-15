package org.example.trademodel.providercall.instrument;

public enum ProviderCapabilityState {
    SUPPORTED,
    UNSUPPORTED_SYMBOL,
    UNSUPPORTED_TIMEFRAME,
    REGION_RESTRICTED,
    PROVIDER_DISABLED,
    SOURCE_UNAVAILABLE,
    STALE_CAPABILITY,
    NOT_CONFIGURED
}
