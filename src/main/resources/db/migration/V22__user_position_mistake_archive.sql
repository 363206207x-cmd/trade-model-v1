ALTER TABLE tm_user_position
    ADD COLUMN IF NOT EXISTS archive_submission_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS archive_reason VARCHAR(512);

ALTER TABLE tm_user_position
    DROP CONSTRAINT IF EXISTS ck_tm_user_position_status;

ALTER TABLE tm_user_position
    ADD CONSTRAINT ck_tm_user_position_status
        CHECK (status IN ('OPEN', 'PARTIALLY_CLOSED', 'CLOSED', 'ARCHIVED_MISTAKE'));

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_user_position_user_archive_submission
    ON tm_user_position(user_id, archive_submission_id);

CREATE INDEX IF NOT EXISTS idx_tm_user_position_user_archive_time
    ON tm_user_position(user_id, archived_at DESC)
    WHERE status = 'ARCHIVED_MISTAKE';

COMMENT ON COLUMN tm_user_position.archive_submission_id IS
    'Owner-scoped idempotency identity for an explicit mistake archive; never a close or trade action.';

COMMENT ON COLUMN tm_user_position.archived_at IS
    'Audit timestamp for explicit owner mistake archive; historical rows remain null.';

COMMENT ON COLUMN tm_user_position.archive_reason IS
    'Optional owner note for a mistake archive; historical rows remain null.';
