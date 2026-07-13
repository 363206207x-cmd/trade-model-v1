ALTER TABLE tm_decision_result
    ADD COLUMN IF NOT EXISTS valid_from TIMESTAMP WITH TIME ZONE;

ALTER TABLE tm_decision_result
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN tm_decision_result.valid_from IS
    'Authoritative offset-aware plan validity start. Null on historical legacy rows.';

COMMENT ON COLUMN tm_decision_result.expires_at IS
    'Authoritative offset-aware plan expiry. Null on historical legacy rows.';
