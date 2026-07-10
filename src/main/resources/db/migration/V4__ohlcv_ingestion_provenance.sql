ALTER TABLE tm_persisted_ohlcv_bar
    ADD COLUMN IF NOT EXISTS fetch_time TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE tm_persisted_ohlcv_bar
    ADD COLUMN IF NOT EXISTS source_status VARCHAR(32);

ALTER TABLE tm_persisted_ohlcv_bar
    ADD COLUMN IF NOT EXISTS freshness_status VARCHAR(32);

ALTER TABLE tm_persisted_ohlcv_bar
    ADD COLUMN IF NOT EXISTS provenance_version VARCHAR(32);

ALTER TABLE tm_persisted_ohlcv_bar
    ADD COLUMN IF NOT EXISTS ingestion_run_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_tm_persisted_ohlcv_bar_ingestion_run
    ON tm_persisted_ohlcv_bar(ingestion_run_id);
