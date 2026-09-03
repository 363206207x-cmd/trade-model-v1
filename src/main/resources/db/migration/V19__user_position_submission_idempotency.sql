ALTER TABLE tm_user_position
    ADD COLUMN submission_id VARCHAR(128);

ALTER TABLE tm_user_position
    ADD COLUMN close_submission_id VARCHAR(128);

CREATE UNIQUE INDEX uk_tm_user_position_user_submission
    ON tm_user_position(user_id, submission_id);

CREATE UNIQUE INDEX uk_tm_user_position_user_close_submission
    ON tm_user_position(user_id, close_submission_id);

COMMENT ON COLUMN tm_user_position.submission_id IS
    'Owner-scoped stable idempotency identity for one manual position-open form; historical rows remain null.';

COMMENT ON COLUMN tm_user_position.close_submission_id IS
    'Owner-scoped stable idempotency identity for one manual position-close form; historical/open rows remain null.';
