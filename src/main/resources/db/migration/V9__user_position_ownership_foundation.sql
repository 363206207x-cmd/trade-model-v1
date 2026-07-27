ALTER TABLE tm_user_position
    ADD COLUMN user_id BIGINT;

CREATE INDEX idx_tm_user_position_user_status_opened_at
    ON tm_user_position(user_id, status, opened_at);

ALTER TABLE tm_user_position
    ADD CONSTRAINT fk_tm_user_position_user
    FOREIGN KEY (user_id) REFERENCES tm_user(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

COMMENT ON COLUMN tm_user_position.user_id IS
    'Canonical tm_user owner. NULL denotes an unclaimed legacy position quarantined from user and scheduler scope.';
