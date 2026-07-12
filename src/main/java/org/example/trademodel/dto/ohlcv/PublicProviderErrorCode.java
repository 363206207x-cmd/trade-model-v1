package org.example.trademodel.dto.ohlcv;

public enum PublicProviderErrorCode {
    HTTP_401,
    HTTP_403,
    HTTP_429,
    HTTP_451,
    HTTP_5XX,
    TIMEOUT,
    DNS_FAILURE,
    INVALID_RESPONSE,
    PAIR_NOT_SUPPORTED,
    GEO_RESTRICTED,
    RATE_LIMITED,
    PROVIDER_UNAVAILABLE,
    PROVIDER_UNAVAILABLE_FOR_LOCATION
}
