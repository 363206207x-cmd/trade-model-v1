ALTER TABLE tm_user
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER',
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN disabled_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN owner_slot SMALLINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM tm_user
        GROUP BY LOWER(username)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'V15_CASE_INSENSITIVE_USERNAME_CONFLICT';
    END IF;
    IF (SELECT COUNT(*) FROM tm_user WHERE LOWER(username) = 'xuchao') > 1 THEN
        RAISE EXCEPTION 'V15_OWNER_IDENTITY_AMBIGUOUS';
    END IF;
    IF EXISTS (SELECT 1 FROM tm_user)
       AND NOT EXISTS (SELECT 1 FROM tm_user WHERE id = 1 AND LOWER(username) = 'xuchao') THEN
        RAISE EXCEPTION 'V15_EXISTING_OWNER_IDENTITY_MISMATCH';
    END IF;
    IF (SELECT COUNT(*) FROM tm_user WHERE enabled = TRUE) > 10 THEN
        RAISE EXCEPTION 'V15_ACTIVE_ACCOUNT_LIMIT_EXCEEDED';
    END IF;
    IF EXISTS (SELECT 1 FROM tm_user_position WHERE user_id IS NULL)
       AND NOT EXISTS (SELECT 1 FROM tm_user WHERE id = 1 AND LOWER(username) = 'xuchao') THEN
        RAISE EXCEPTION 'V15_LEGACY_POSITION_OWNER_MISSING';
    END IF;
END $$;

UPDATE tm_user
SET role = 'USER', owner_slot = NULL
WHERE LOWER(username) <> 'xuchao';

UPDATE tm_user
SET role = 'OWNER', owner_slot = 1, enabled = TRUE, disabled_at = NULL
WHERE LOWER(username) = 'xuchao';

DO $$
DECLARE
    user_id_sequence TEXT;
BEGIN
    user_id_sequence := pg_get_serial_sequence('tm_user', 'id');
    IF user_id_sequence IS NULL THEN
        RAISE EXCEPTION 'V15_USER_ID_SEQUENCE_MISSING';
    END IF;
    PERFORM setval(
        user_id_sequence::regclass,
        COALESCE((SELECT MAX(id) FROM tm_user), 1),
        EXISTS(SELECT 1 FROM tm_user)
    );
END $$;

UPDATE tm_user_position
SET user_id = 1
WHERE user_id IS NULL;

ALTER TABLE tm_user_position
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE tm_user
    ADD CONSTRAINT ck_tm_user_role CHECK (role IN ('OWNER', 'USER')),
    ADD CONSTRAINT ck_tm_user_owner_identity CHECK (
        (role = 'OWNER' AND LOWER(username) = 'xuchao' AND owner_slot = 1 AND enabled = TRUE)
        OR (role = 'USER' AND owner_slot IS NULL)
    );

CREATE UNIQUE INDEX uk_tm_user_owner_slot ON tm_user(owner_slot);
CREATE UNIQUE INDEX uk_tm_user_username_lower ON tm_user(LOWER(username));

CREATE TABLE tm_user_registration_guard (
    id SMALLINT PRIMARY KEY,
    max_active_accounts INT NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_user_registration_guard_singleton CHECK (id = 1),
    CONSTRAINT ck_tm_user_registration_guard_limit CHECK (max_active_accounts = 10)
);

INSERT INTO tm_user_registration_guard(id, max_active_accounts)
VALUES (1, 10);

CREATE TABLE tm_owner_password_setup_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tm_owner_password_setup_hash UNIQUE (token_hash),
    CONSTRAINT fk_tm_owner_password_setup_user FOREIGN KEY (user_id) REFERENCES tm_user(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE INDEX idx_tm_owner_password_setup_user_time
    ON tm_owner_password_setup_token(user_id, created_at DESC);

INSERT INTO tm_asset_pool_item(
    owner_type, owner_id, asset_id, symbol, display_name, market_type, quote_asset,
    active, focus_enabled, sort_order, source_type, watch_status, version_no, ext_json,
    created_at, updated_at
)
SELECT 'USER', users.id, defaults.asset_id, defaults.symbol, defaults.display_name,
       defaults.market_type, defaults.quote_asset, defaults.active, defaults.focus_enabled,
       defaults.sort_order, 'USER_OVERRIDE', defaults.watch_status, 1, defaults.ext_json,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tm_user users
CROSS JOIN tm_asset_pool_item defaults
WHERE defaults.owner_type = 'SYSTEM' AND defaults.owner_id = 0
ON CONFLICT (owner_type, owner_id, symbol) DO NOTHING;

COMMENT ON TABLE tm_user_registration_guard IS
    'Singleton row locked by registration and re-enable operations to enforce the ten-active-account limit.';
COMMENT ON TABLE tm_owner_password_setup_token IS
    'One-time Owner password setup token hashes. Plaintext tokens are never persisted.';
COMMENT ON COLUMN tm_user.session_version IS
    'Server-side session generation. Incrementing invalidates every prior session for the account.';
COMMENT ON COLUMN tm_user_position.user_id IS
    'Canonical tm_user owner. V15 assigns deterministic legacy positions to the preserved xuchao Owner.';
