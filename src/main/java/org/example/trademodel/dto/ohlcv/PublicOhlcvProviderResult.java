package org.example.trademodel.dto.ohlcv;

public record PublicOhlcvProviderResult(
        OhlcvSourceState sourceState,
        String reasonCode,
        OhlcvIngestionBatch batch
) {
}
