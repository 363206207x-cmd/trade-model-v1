-- Durable Telegram ChannelDelivery queue. Message remains the business fact owner.

ALTER TABLE tm_channel_delivery
    DROP CONSTRAINT IF EXISTS ck_tm_channel_delivery_status;

UPDATE tm_channel_delivery
SET status = 'SENT'
WHERE status = 'DELIVERED';

ALTER TABLE tm_channel_delivery
    ADD COLUMN next_attempt_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN claimed_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN lease_until TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN claim_token VARCHAR(64),
    ADD COLUMN last_response_code INT,
    ADD COLUMN retry_after_seconds INT,
    ADD COLUMN recipient_fingerprint VARCHAR(128),
    ADD COLUMN cooldown_key VARCHAR(256),
    ADD COLUMN severity_rank INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_tm_channel_delivery_status CHECK (
        status IN ('QUEUED', 'SENDING', 'SENT', 'RETRYING', 'FAILED', 'SUPPRESSED', 'NOT_CONFIGURED')
    ),
    ADD CONSTRAINT ck_tm_channel_delivery_retry_after CHECK (
        retry_after_seconds IS NULL OR retry_after_seconds >= 0
    ),
    ADD CONSTRAINT ck_tm_channel_delivery_severity CHECK (severity_rank >= 0);

UPDATE tm_channel_delivery
SET next_attempt_at = COALESCE(attempted_at, created_at)
WHERE status IN ('QUEUED', 'RETRYING')
  AND next_attempt_at IS NULL;

-- Historical duplicate rows remain immutable evidence. Only one non-suppressed
-- delivery for a Message/channel pair may participate in dispatch.
WITH ranked AS (
    SELECT delivery_id,
           ROW_NUMBER() OVER (
               PARTITION BY message_id, channel
               ORDER BY CASE WHEN status <> 'SUPPRESSED' THEN 0 ELSE 1 END,
                        created_at ASC, delivery_id ASC
           ) AS row_number
    FROM tm_channel_delivery
)
UPDATE tm_channel_delivery delivery
SET status = 'SUPPRESSED',
    error_code = 'DUPLICATE_MIGRATED',
    error_message = 'Historical duplicate delivery retained as suppressed evidence',
    next_attempt_at = NULL,
    updated_at = CURRENT_TIMESTAMP
FROM ranked
WHERE delivery.delivery_id = ranked.delivery_id
  AND ranked.row_number > 1;

CREATE UNIQUE INDEX uk_tm_channel_delivery_message_channel_active
    ON tm_channel_delivery(message_id, channel)
    WHERE error_code IS DISTINCT FROM 'DUPLICATE_MIGRATED';

CREATE INDEX idx_tm_channel_delivery_due
    ON tm_channel_delivery(status, next_attempt_at, lease_until);

CREATE INDEX idx_tm_channel_delivery_cooldown
    ON tm_channel_delivery(user_id, cooldown_key, created_at DESC);
