ALTER TABLE tm_user_position
    ADD COLUMN user_id BIGINT;

CREATE INDEX idx_tm_user_position_user_status_opened_at
    ON tm_user_position(user_id, status, opened_at);

CREATE UNIQUE INDEX uk_tm_user_position_id_user
    ON tm_user_position(id, user_id);

ALTER TABLE tm_user_position
    ADD CONSTRAINT fk_tm_user_position_user
    FOREIGN KEY (user_id) REFERENCES tm_user(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

COMMENT ON COLUMN tm_user_position.user_id IS
    'Canonical tm_user owner. NULL denotes an unclaimed legacy position quarantined from user and scheduler scope.';

ALTER TABLE tm_review_result
    ADD COLUMN user_id BIGINT,
    ADD COLUMN user_position_id BIGINT,
    ADD COLUMN review_scope_key VARCHAR(128) NOT NULL DEFAULT 'SHARED';

DROP INDEX IF EXISTS uk_tm_review_result_analysis_id;

CREATE UNIQUE INDEX uk_tm_review_result_analysis_scope
    ON tm_review_result(analysis_id, review_scope_key);

CREATE INDEX idx_tm_review_result_user_update
    ON tm_review_result(user_id, update_time, id);

ALTER TABLE tm_review_result
    ADD CONSTRAINT ck_tm_review_result_owner_scope CHECK (
        (review_scope_key = 'SHARED' AND user_id IS NULL AND user_position_id IS NULL)
        OR (review_scope_key <> 'SHARED' AND user_id IS NOT NULL)
    );

ALTER TABLE tm_review_result
    ADD CONSTRAINT fk_tm_review_result_user
    FOREIGN KEY (user_id) REFERENCES tm_user(id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

ALTER TABLE tm_review_result
    ADD CONSTRAINT fk_tm_review_result_user_position_owner
    FOREIGN KEY (user_position_id, user_id) REFERENCES tm_user_position(id, user_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

COMMENT ON COLUMN tm_review_result.user_id IS
    'Canonical tm_user owner for user-scoped review feedback; NULL for shared analysis feedback.';

COMMENT ON COLUMN tm_review_result.user_position_id IS
    'Optional exact UserPosition identity for user-scoped review feedback.';

COMMENT ON COLUMN tm_review_result.review_scope_key IS
    'Server-generated uniqueness scope. SHARED is reserved for shared analysis feedback.';
