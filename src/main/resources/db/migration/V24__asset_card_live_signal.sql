CREATE TABLE IF NOT EXISTS tm_asset_card_snapshot (
    symbol VARCHAR(32) PRIMARY KEY,
    version_counter BIGINT NOT NULL DEFAULT 0,
    snapshot_version BIGINT NOT NULL DEFAULT 0,
    snapshot_json TEXT,
    card_as_of TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_asset_card_snapshot_versions CHECK (
        snapshot_version >= 0 AND version_counter >= snapshot_version
    )
);

CREATE TABLE IF NOT EXISTS tm_asset_card_spot_bar (
    symbol VARCHAR(32) NOT NULL,
    interval_code VARCHAR(4) NOT NULL,
    open_time TIMESTAMP WITH TIME ZONE NOT NULL,
    close_time TIMESTAMP WITH TIME ZONE NOT NULL,
    open_price DECIMAL(38,18) NOT NULL,
    high_price DECIMAL(38,18) NOT NULL,
    low_price DECIMAL(38,18) NOT NULL,
    close_price DECIMAL(38,18) NOT NULL,
    volume DECIMAL(38,18) NOT NULL,
    taker_buy_base_volume DECIMAL(38,18),
    trade_count BIGINT,
    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (symbol, interval_code, open_time),
    CONSTRAINT ck_asset_card_spot_interval CHECK (interval_code IN ('1m','5m','15m','1h','4h')),
    CONSTRAINT ck_asset_card_spot_times CHECK (close_time > open_time AND available_at >= close_time),
    CONSTRAINT ck_asset_card_spot_prices CHECK (
        open_price > 0 AND close_price > 0 AND low_price > 0
        AND high_price >= low_price AND high_price >= open_price AND high_price >= close_price
        AND low_price <= open_price AND low_price <= close_price AND volume >= 0
    )
);
CREATE INDEX IF NOT EXISTS idx_asset_card_spot_available
    ON tm_asset_card_spot_bar(symbol, interval_code, close_time, available_at);

CREATE TABLE IF NOT EXISTS tm_asset_card_feature_history (
    symbol VARCHAR(32) NOT NULL,
    record_kind VARCHAR(16) NOT NULL,
    record_key VARCHAR(128) NOT NULL,
    signal_as_of TIMESTAMP WITH TIME ZONE NOT NULL,
    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload_json TEXT NOT NULL,
    version_no BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (symbol, record_kind, record_key),
    CONSTRAINT ck_asset_card_history_kind CHECK (record_kind IN ('FEATURE','INFERENCE','TRADE','LABEL')),
    CONSTRAINT ck_asset_card_history_key CHECK (LENGTH(TRIM(record_key)) > 0),
    CONSTRAINT ck_asset_card_feature_times CHECK (available_at >= signal_as_of),
    CONSTRAINT ck_asset_card_feature_version CHECK (version_no = 1)
);
CREATE INDEX IF NOT EXISTS idx_asset_card_feature_available
    ON tm_asset_card_feature_history(symbol, record_kind, signal_as_of, available_at);
