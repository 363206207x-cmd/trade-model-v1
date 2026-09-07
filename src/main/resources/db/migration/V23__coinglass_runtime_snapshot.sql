CREATE TABLE IF NOT EXISTS tm_coinglass_runtime_snapshot (
    capability_id VARCHAR(128) NOT NULL,
    provider_symbol VARCHAR(64) NOT NULL,
    runtime_state VARCHAR(24) NOT NULL,
    source_status VARCHAR(32) NOT NULL,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    last_success_at TIMESTAMP WITH TIME ZONE,
    provider_data_at TIMESTAMP WITH TIME ZONE,
    next_check_at TIMESTAMP WITH TIME ZONE,
    fresh_ttl_seconds INTEGER NOT NULL DEFAULT 120,
    http_status INTEGER,
    provider_status_code VARCHAR(64),
    reason_code VARCHAR(160),
    state_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (capability_id, provider_symbol),
    CONSTRAINT ck_coinglass_runtime_state CHECK (
        runtime_state IN ('NOT_STARTED', 'RUNNING', 'FRESH', 'STALE', 'RATE_LIMITED', 'ERROR', 'DISABLED')
    ),
    CONSTRAINT ck_coinglass_runtime_ttl CHECK (fresh_ttl_seconds > 0),
    CONSTRAINT ck_coinglass_runtime_version CHECK (state_version > 0)
);

CREATE INDEX IF NOT EXISTS idx_coinglass_runtime_next_check
    ON tm_coinglass_runtime_snapshot(next_check_at);

-- New-table-only privileges; no existing object or row is changed.
DO $runtime_grant$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rine_app') THEN
        GRANT SELECT, INSERT, UPDATE ON TABLE public.tm_coinglass_runtime_snapshot TO rine_app;
    END IF;
END
$runtime_grant$;
