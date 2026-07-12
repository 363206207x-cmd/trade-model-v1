package org.example.trademodel.market.client.impl;

public record KrakenPairMetadata(
        String internalSymbol,
        String requestPair,
        String displayPair,
        String resultKey,
        String status
) {
}
