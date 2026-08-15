package org.example.trademodel.providercall.coinglass;

public enum CoinGlassConfigurationState {
    NOT_CONFIGURED,
    KEY_MISSING,
    RPM_NOT_CONFIGURED,
    INVALID_RPM,
    CONFIGURED;

    public static CoinGlassConfigurationState evaluate(boolean enabled,
                                                       boolean externalCallsEnabled,
                                                       boolean apiKeyPresent,
                                                       Integer advertisedRpm) {
        if (!enabled || !externalCallsEnabled) return NOT_CONFIGURED;
        if (!apiKeyPresent) return KEY_MISSING;
        if (advertisedRpm == null) return RPM_NOT_CONFIGURED;
        if (advertisedRpm <= 0) return INVALID_RPM;
        return CONFIGURED;
    }
}
