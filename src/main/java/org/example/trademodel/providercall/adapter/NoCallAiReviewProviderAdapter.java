package org.example.trademodel.providercall.adapter;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.springframework.stereotype.Service;

@Service
public class NoCallAiReviewProviderAdapter implements AiReviewProviderAdapter, NoCallProviderAdapter {
    @Override
    public ProviderAdapterResponse<AiReviewSnapshot> review(AiReviewRequest request) {
        return ProviderAdapterResponse.failed(UnifiedSourceStatus.NOT_CONFIGURED, 0,
                "AI_REVIEW_ADAPTER_NOT_CONFIGURED", null);
    }
}
