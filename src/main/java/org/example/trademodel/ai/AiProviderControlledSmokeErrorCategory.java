package org.example.trademodel.ai;

public enum AiProviderControlledSmokeErrorCategory {
    TIMEOUT,
    AUTH,
    MODEL_NOT_FOUND,
    RATE_LIMIT,
    PROVIDER_ERROR,
    RESPONSE_SCHEMA,
    INVALID_REQUEST,
    SCHEMA_UNSUPPORTED,
    MODEL_CAPABILITY_ERROR,
    PROVIDER_INTERNAL_ERROR,
    UNKNOWN_PROVIDER_ERROR
}
