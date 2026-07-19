package org.example.trademodel.providercall.adapter;

import org.example.trademodel.providercall.ProviderAdapterResponse;

public interface AiReviewProviderAdapter {
    ProviderAdapterResponse<AiReviewSnapshot> review(AiReviewRequest request);
}
