CREATE TABLE tm_telegram_channel_test_audit (
    test_id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL,
    provider_reference VARCHAR(256),
    bot_username VARCHAR(128),
    recipient_fingerprint VARCHAR(32),
    response_code INT,
    error_code VARCHAR(128),
    error_message VARCHAR(512),
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    attempted_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_tm_telegram_channel_test_owner_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_tm_telegram_channel_test_status CHECK (
        status IN ('PENDING', 'PASSED', 'FAILED', 'RATE_LIMITED', 'NOT_CONFIGURED', 'BLOCKED')
    ),
    CONSTRAINT ck_tm_telegram_channel_test_safety CHECK (
        not_trade_instruction = TRUE AND not_order_execution = TRUE
    )
);

CREATE INDEX idx_tm_telegram_channel_test_owner_time
    ON tm_telegram_channel_test_audit(user_id, requested_at DESC);

COMMENT ON TABLE tm_telegram_channel_test_audit IS
    'Owner-scoped durable audit for explicit Telegram channel tests; never a trade instruction.';
