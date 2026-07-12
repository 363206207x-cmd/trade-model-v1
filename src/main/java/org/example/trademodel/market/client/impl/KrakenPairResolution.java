package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;

public record KrakenPairResolution(
        boolean ready,
        KrakenPairMetadata metadata,
        OhlcvSourceState sourceState,
        String reasonCode
) {
    static KrakenPairResolution ready(KrakenPairMetadata metadata) {
        return new KrakenPairResolution(true, metadata, OhlcvSourceState.READY, null);
    }

    static KrakenPairResolution failed(OhlcvSourceState state, String reasonCode) {
        return new KrakenPairResolution(false, null,
                state == null ? OhlcvSourceState.ERROR : state, reasonCode);
    }
}
