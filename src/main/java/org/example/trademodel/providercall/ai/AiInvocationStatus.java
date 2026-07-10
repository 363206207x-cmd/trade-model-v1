package org.example.trademodel.providercall.ai;

public enum AiInvocationStatus {
    RUN_GPT,
    RUN_GEMINI,
    RUN_GROK,
    SKIP_NOT_TRIGGERED,
    SKIP_SAME_EVIDENCE,
    SKIP_DATA_QUALITY,
    SKIP_BUDGET,
    SKIP_RATE_LIMIT,
    SKIP_PROVIDER_UNAVAILABLE
}
