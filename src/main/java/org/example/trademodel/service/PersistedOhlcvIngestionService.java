package org.example.trademodel.service;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;

public interface PersistedOhlcvIngestionService {

    OhlcvIngestionResult ingest(OhlcvIngestionBatch batch);
}
