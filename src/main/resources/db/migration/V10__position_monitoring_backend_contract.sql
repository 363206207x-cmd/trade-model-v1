ALTER TABLE tm_position_monitor_log
    DROP CONSTRAINT IF EXISTS ck_tm_position_monitor_log_logic_status;

ALTER TABLE tm_position_monitor_log
    DROP CONSTRAINT IF EXISTS ck_tm_position_monitor_log_suggested_action;

ALTER TABLE tm_position_monitor_log
    ALTER COLUMN logic_status DROP NOT NULL,
    ALTER COLUMN risk_level DROP NOT NULL,
    ALTER COLUMN suggested_action TYPE VARCHAR(64),
    ALTER COLUMN suggested_action DROP NOT NULL,
    ADD COLUMN mark_price_source VARCHAR(64),
    ADD COLUMN entry_logic_status VARCHAR(32),
    ADD COLUMN monitor_conclusion VARCHAR(40),
    ADD COLUMN reversal_status VARCHAR(32),
    ADD COLUMN risk_change_reason VARCHAR(64),
    ADD COLUMN risk_trend VARCHAR(32),
    ADD COLUMN source_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    ADD COLUMN observed_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN fresh_until TIMESTAMP WITHOUT TIME ZONE;

UPDATE tm_position_monitor_log
SET entry_logic_status = NULL,
    monitor_conclusion = NULL,
    reversal_status = NULL,
    risk_change_reason = NULL,
    risk_trend = NULL,
    risk_level = NULL,
    suggested_action = NULL,
    source_status = 'PENDING_VERIFICATION',
    observed_at = created_at,
    fresh_until = created_at;

ALTER TABLE tm_position_monitor_log
    ALTER COLUMN observed_at SET NOT NULL,
    ALTER COLUMN fresh_until SET NOT NULL;

ALTER TABLE tm_position_monitor_log
    ADD CONSTRAINT ck_tm_position_monitor_log_entry_logic CHECK (
        entry_logic_status IN ('STILL_VALID', 'WEAKENED', 'INVALIDATED')
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_conclusion CHECK (
        monitor_conclusion IN (
            'LOGIC_VALID', 'LOGIC_WEAKENED', 'PLAN_INVALIDATED', 'NEAR_STOP_LOSS',
            'NEAR_TAKE_PROFIT', 'HIGH_RISK_OBSERVATION', 'WAIT_USER_CONFIRM_CLOSE'
        )
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_reversal CHECK (
        reversal_status IN ('NO_REVERSAL', 'WEAK_REVERSAL', 'STRONG_REVERSAL')
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_risk_reason CHECK (
        risk_change_reason IN (
            'NO_CLEAR_RISK_FACTOR', 'OPPOSING_EVIDENCE_INCREASED', 'STRUCTURE_CHANGED',
            'EVENT_IMPACT', 'DATA_QUALITY_DEGRADED'
        )
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_risk_level CHECK (
        risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'EXTREME')
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_risk_trend CHECK (
        risk_trend IN ('STABLE', 'INCREASED', 'SHARPLY_INCREASED')
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_suggested_action CHECK (
        suggested_action IN (
            'CONTINUE_HOLD', 'NO_ADD_POSITION', 'REDUCE_POSITION', 'TIGHTEN_STOP',
            'MOVE_STOP', 'PARTIAL_TAKE_PROFIT', 'WAIT_CONFIRMATION', 'RECORD_CLOSE_REVIEW'
        )
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_source_status CHECK (
        source_status IN ('VERIFIED', 'PENDING_VERIFICATION', 'INVALID')
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_freshness CHECK (
        fresh_until >= observed_at
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_trusted_payload CHECK (
        (
            source_status = 'VERIFIED'
            AND mark_price_source IS NOT NULL
            AND TRIM(mark_price_source) <> ''
            AND entry_logic_status IS NOT NULL
            AND monitor_conclusion IS NOT NULL
            AND reversal_status IS NOT NULL
            AND risk_change_reason IS NOT NULL
            AND risk_level IS NOT NULL
            AND risk_trend IS NOT NULL
            AND suggested_action IS NOT NULL
            AND fresh_until > observed_at
        )
        OR
        (
            source_status IN ('PENDING_VERIFICATION', 'INVALID')
            AND entry_logic_status IS NULL
            AND monitor_conclusion IS NULL
            AND reversal_status IS NULL
            AND risk_change_reason IS NULL
            AND risk_level IS NULL
            AND risk_trend IS NULL
            AND suggested_action IS NULL
        )
    );

CREATE INDEX idx_tm_position_monitor_log_trust_freshness
    ON tm_position_monitor_log(position_id, source_status, fresh_until DESC);

COMMENT ON COLUMN tm_position_monitor_log.logic_status IS
    'Legacy compatibility column. New monitor writes use independent entry_logic_status and monitor_conclusion.';

COMMENT ON COLUMN tm_position_monitor_log.source_status IS
    'Monitor-result trust status. Only VERIFIED and unexpired rows may feed Home position results.';

COMMENT ON COLUMN tm_position_monitor_log.risk_trend IS
    'Risk movement relative to the prior historically trusted position monitor result; independent from risk_level.';
