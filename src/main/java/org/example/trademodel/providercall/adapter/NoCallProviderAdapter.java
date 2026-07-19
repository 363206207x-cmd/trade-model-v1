package org.example.trademodel.providercall.adapter;

public interface NoCallProviderAdapter {
    default int networkCallCount() {
        return 0;
    }
}
