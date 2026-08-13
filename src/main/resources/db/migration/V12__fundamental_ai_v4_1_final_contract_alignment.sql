-- Final frozen-contract alignment. Historical rows are never promoted to a new success state.

CREATE TABLE tm_asset (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    asset_name VARCHAR(64),
    source VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no INT NOT NULL DEFAULT 1,
    ext_json TEXT,
    CONSTRAINT uk_tm_asset_symbol UNIQUE (symbol),
    CONSTRAINT ck_tm_asset_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO tm_asset(symbol, asset_name, source, status, created_at, updated_at)
SELECT UPPER(BTRIM(symbol)), COALESCE(MIN(display_name), UPPER(BTRIM(symbol))),
       'V12_ASSET_POOL_BACKFILL', 'ACTIVE', MIN(created_at), MAX(updated_at)
FROM tm_asset_pool_item
GROUP BY UPPER(BTRIM(symbol));

ALTER TABLE tm_asset_pool_item
    ADD COLUMN asset_id BIGINT,
    ADD COLUMN watch_status VARCHAR(32) NOT NULL DEFAULT 'OBSERVING',
    ADD COLUMN version_no INT NOT NULL DEFAULT 1,
    ADD COLUMN ext_json TEXT;

UPDATE tm_asset_pool_item pool
SET asset_id = asset.id,
    watch_status = CASE WHEN pool.active THEN 'OBSERVING' ELSE 'REMOVED' END
FROM tm_asset asset
WHERE asset.symbol = UPPER(BTRIM(pool.symbol));

ALTER TABLE tm_asset_pool_item
    ALTER COLUMN asset_id SET NOT NULL,
    ADD CONSTRAINT ck_tm_asset_pool_watch_status CHECK (watch_status IN ('OBSERVING', 'REMOVED')),
    ADD CONSTRAINT fk_tm_asset_pool_asset FOREIGN KEY (asset_id) REFERENCES tm_asset(id);

ALTER TABLE tm_analysis_run
    ADD COLUMN owner_type VARCHAR(16),
    ADD COLUMN owner_id BIGINT,
    ADD COLUMN asset_id BIGINT,
    ADD COLUMN preview BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE tm_analysis_run
SET owner_type = 'SYSTEM', owner_id = 0
WHERE owner_type IS NULL OR owner_id IS NULL;

ALTER TABLE tm_analysis_run
    ALTER COLUMN owner_type SET NOT NULL,
    ALTER COLUMN owner_id SET NOT NULL,
    ADD CONSTRAINT ck_tm_analysis_run_owner CHECK (
        (owner_type = 'SYSTEM' AND owner_id = 0)
        OR (owner_type = 'USER' AND owner_id > 0)
    );

CREATE INDEX idx_tm_analysis_run_owner_asset
    ON tm_analysis_run(owner_type, owner_id, asset_id, analysis_time DESC);

ALTER TABLE tm_hot_reset_event
    ADD COLUMN owner_type VARCHAR(16),
    ADD COLUMN owner_id BIGINT,
    ADD COLUMN asset_id BIGINT,
    ADD COLUMN rule_version VARCHAR(32);

UPDATE tm_hot_reset_event hre
SET owner_type = COALESCE(ar.owner_type, 'SYSTEM'),
    owner_id = COALESCE(ar.owner_id, 0),
    asset_id = ar.asset_id,
    rule_version = COALESCE(ar.rule_version, 'LEGACY_UNAVAILABLE')
FROM tm_analysis_run ar
WHERE ar.analysis_id = hre.analysis_id;

UPDATE tm_hot_reset_event
SET owner_type = 'SYSTEM', owner_id = 0, rule_version = 'LEGACY_UNAVAILABLE'
WHERE owner_type IS NULL OR owner_id IS NULL OR rule_version IS NULL;

ALTER TABLE tm_hot_reset_event
    ALTER COLUMN owner_type SET NOT NULL,
    ALTER COLUMN owner_id SET NOT NULL,
    ALTER COLUMN rule_version SET NOT NULL,
    ADD CONSTRAINT ck_tm_hot_reset_event_owner CHECK (
        (owner_type = 'SYSTEM' AND owner_id = 0)
        OR (owner_type = 'USER' AND owner_id > 0)
    );

CREATE INDEX idx_tm_hot_reset_event_owner_asset_time
    ON tm_hot_reset_event(owner_type, owner_id, asset_id, event_time DESC);

ALTER TABLE tm_evidence_item
    ADD COLUMN current_value VARCHAR(512),
    ADD COLUMN change_from_baseline VARCHAR(512),
    ADD COLUMN observed_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN freshness VARCHAR(32);

ALTER TABLE tm_decision_result
    ADD COLUMN rule_market_bias VARCHAR(32),
    ADD COLUMN final_market_bias VARCHAR(32),
    ADD COLUMN rule_confidence VARCHAR(16),
    ADD COLUMN rule_risk VARCHAR(16),
    ADD COLUMN rule_plan_mode VARCHAR(32),
    ADD COLUMN rule_can_execute BOOLEAN,
    ADD COLUMN candidate_plan_mode VARCHAR(32),
    ADD COLUMN final_plan_mode VARCHAR(32),
    ADD COLUMN bias_adjustment_reason VARCHAR(512),
    ADD COLUMN plan_mode_adjustment_reason VARCHAR(512);

ALTER TABLE tm_asset_state
    ADD COLUMN owner_type VARCHAR(16),
    ADD COLUMN owner_id BIGINT,
    ADD COLUMN asset_id BIGINT,
    ADD COLUMN pool_item_id BIGINT,
    ADD COLUMN opportunity_score INT,
    ADD COLUMN confidence VARCHAR(16),
    ADD COLUMN risk VARCHAR(16),
    ADD COLUMN created_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN updated_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN ext_json TEXT,
    ADD COLUMN rule_version VARCHAR(32);

UPDATE tm_asset_state
SET owner_type = 'SYSTEM', owner_id = 0, rule_version = 'LEGACY_UNAVAILABLE'
WHERE owner_type IS NULL OR owner_id IS NULL OR rule_version IS NULL;

UPDATE tm_asset_state
SET created_at = COALESCE(state_entered_at, last_update_time, CURRENT_TIMESTAMP),
    updated_at = COALESCE(last_update_time, state_entered_at, CURRENT_TIMESTAMP);

UPDATE tm_asset_state state
SET asset_id = asset.id
FROM tm_asset asset
WHERE asset.symbol = UPPER(BTRIM(state.symbol));

UPDATE tm_asset_state state
SET pool_item_id = pool.id
FROM tm_asset_pool_item pool
WHERE pool.owner_type = 'SYSTEM'
  AND pool.owner_id = 0
  AND pool.symbol = state.symbol
  AND state.owner_type = 'SYSTEM'
  AND state.owner_id = 0;

UPDATE tm_asset_state state
SET pool_item_id = COALESCE(
    (SELECT user_pool.id FROM tm_asset_pool_item user_pool
     WHERE user_pool.owner_type = 'USER' AND user_pool.owner_id = state.owner_id
       AND user_pool.symbol = state.symbol AND user_pool.active = TRUE LIMIT 1),
    (SELECT system_pool.id FROM tm_asset_pool_item system_pool
     WHERE system_pool.owner_type = 'SYSTEM' AND system_pool.owner_id = 0
       AND system_pool.symbol = state.symbol AND system_pool.active = TRUE LIMIT 1)
)
WHERE state.owner_type = 'USER';

ALTER TABLE tm_asset_state
    ALTER COLUMN owner_type SET NOT NULL,
    ALTER COLUMN owner_id SET NOT NULL,
    ALTER COLUMN rule_version SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    DROP CONSTRAINT IF EXISTS uk_tm_asset_state_symbol_timeframe,
    ADD CONSTRAINT uk_tm_asset_state_owner_symbol_timeframe
        UNIQUE (owner_type, owner_id, symbol, timeframe),
    ADD CONSTRAINT ck_tm_asset_state_owner CHECK (
        (owner_type = 'SYSTEM' AND owner_id = 0)
        OR (owner_type = 'USER' AND owner_id > 0)
    ),
    ADD CONSTRAINT ck_tm_asset_state_score CHECK (
        opportunity_score IS NULL OR opportunity_score BETWEEN 0 AND 100
    ),
    ADD CONSTRAINT ck_tm_asset_state_confidence CHECK (
        confidence IS NULL OR confidence IN ('LOW', 'MEDIUM', 'HIGH')
    ),
    ADD CONSTRAINT ck_tm_asset_state_risk CHECK (
        risk IS NULL OR risk IN ('LOW', 'MEDIUM', 'HIGH', 'EXTREME')
    ),
    ADD CONSTRAINT fk_tm_asset_state_asset FOREIGN KEY (asset_id) REFERENCES tm_asset(id),
    ADD CONSTRAINT fk_tm_asset_state_pool_item FOREIGN KEY (pool_item_id) REFERENCES tm_asset_pool_item(id);

ALTER TABLE tm_opportunity_state_transition
    ADD COLUMN owner_type VARCHAR(16),
    ADD COLUMN owner_id BIGINT,
    ADD COLUMN asset_id BIGINT,
    ADD COLUMN rule_version VARCHAR(32);

UPDATE tm_opportunity_state_transition
SET owner_type = 'SYSTEM', owner_id = 0, rule_version = 'LEGACY_UNAVAILABLE'
WHERE owner_type IS NULL OR owner_id IS NULL OR rule_version IS NULL;

ALTER TABLE tm_opportunity_state_transition
    ALTER COLUMN owner_type SET NOT NULL,
    ALTER COLUMN owner_id SET NOT NULL,
    ALTER COLUMN rule_version SET NOT NULL,
    ADD CONSTRAINT ck_tm_opportunity_transition_owner CHECK (
        (owner_type = 'SYSTEM' AND owner_id = 0)
        OR (owner_type = 'USER' AND owner_id > 0)
    );

CREATE INDEX idx_tm_opportunity_transition_owner_asset
    ON tm_opportunity_state_transition(owner_type, owner_id, asset_id, occurred_at DESC);

ALTER TABLE tm_execution_plan_candidate
    DROP CONSTRAINT IF EXISTS ck_tm_plan_candidate_mode;

UPDATE tm_execution_plan_candidate
SET plan_mode = CASE plan_mode
    WHEN 'CONFIRM' THEN 'CONFIRMATION'
    WHEN 'PREPARE' THEN 'PREPARATION'
    WHEN 'REDUCE' THEN 'REDUCED'
    WHEN 'WATCH' THEN 'OBSERVATION'
    WHEN 'CONFIRMATION' THEN 'CONFIRMATION'
    WHEN 'PREPARATION' THEN 'PREPARATION'
    WHEN 'REDUCED' THEN 'REDUCED'
    WHEN 'OBSERVATION' THEN 'OBSERVATION'
    WHEN 'BLOCKED' THEN 'BLOCKED'
    ELSE 'BLOCKED'
END;

UPDATE tm_execution_plan_candidate
SET rule_direction = CASE
        WHEN rule_direction IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        ) THEN rule_direction
        ELSE 'WAIT'
    END,
    candidate_direction = CASE
        WHEN candidate_direction IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        ) THEN candidate_direction
        ELSE 'WAIT'
    END;

ALTER TABLE tm_execution_plan_candidate
    DROP CONSTRAINT IF EXISTS ck_tm_plan_candidate_mode,
    ADD COLUMN rule_plan_mode VARCHAR(32),
    ADD COLUMN rule_can_execute BOOLEAN,
    ADD COLUMN bias_adjustment_reason VARCHAR(512),
    ADD COLUMN asset_id BIGINT,
    ADD COLUMN rule_version VARCHAR(32),
    ADD COLUMN opportunity_type VARCHAR(64),
    ADD COLUMN entry_logic TEXT,
    ADD COLUMN entry_source VARCHAR(256),
    ADD COLUMN entry_reason TEXT,
    ADD COLUMN trigger_condition TEXT,
    ADD COLUMN stop_logic TEXT,
    ADD COLUMN stop_source VARCHAR(256),
    ADD COLUMN stop_reason TEXT,
    ADD COLUMN target_logic TEXT,
    ADD COLUMN target_source VARCHAR(256),
    ADD COLUMN target_reason TEXT,
    ADD COLUMN add_position_condition TEXT,
    ADD COLUMN reduce_position_condition TEXT,
    ADD COLUMN abandon_condition TEXT,
    ADD COLUMN risk_explanation TEXT,
    ADD COLUMN invalidation_source VARCHAR(256),
    ADD COLUMN invalidation_reason TEXT,
    ADD COLUMN expected_risk_reward NUMERIC(20, 8),
    ADD COLUMN expected_risk_reward_source VARCHAR(256),
    ADD COLUMN expected_risk_reward_reason TEXT,
    ADD COLUMN analysis_timeframes_json TEXT,
    ADD COLUMN trigger_timeframe VARCHAR(16),
    ADD COLUMN valid_from TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN valid_until TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN holding_horizon VARCHAR(64),
    ADD COLUMN revalidation_rule TEXT,
    ADD COLUMN source_refs_json TEXT,
    ADD COLUMN evidence_refs_json TEXT,
    ADD COLUMN score_refs_json TEXT,
    ADD COLUMN data_quality INT,
    ADD COLUMN confused_score INT,
    ADD COLUMN account_risk_snapshot_id BIGINT,
    ADD COLUMN version INT NOT NULL DEFAULT 1,
    ADD CONSTRAINT ck_tm_plan_candidate_mode CHECK (
        plan_mode IN ('CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED')
    ),
    ADD CONSTRAINT ck_tm_plan_candidate_market_bias CHECK (
        rule_direction IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        )
        AND candidate_direction IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        )
    ),
    ADD CONSTRAINT ck_tm_plan_candidate_rule_mode CHECK (
        rule_plan_mode IS NULL OR rule_plan_mode IN (
            'CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED'
        )
    ),
    ADD CONSTRAINT ck_tm_plan_candidate_data_quality CHECK (
        data_quality IS NULL OR data_quality BETWEEN 0 AND 100
    );

UPDATE tm_conflict_resolver_result
SET plan_mode_before = CASE plan_mode_before
        WHEN 'CONFIRM' THEN 'CONFIRMATION'
        WHEN 'PREPARE' THEN 'PREPARATION'
        WHEN 'REDUCE' THEN 'REDUCED'
        WHEN 'WATCH' THEN 'OBSERVATION'
        WHEN 'CONFIRMATION' THEN 'CONFIRMATION'
        WHEN 'PREPARATION' THEN 'PREPARATION'
        WHEN 'REDUCED' THEN 'REDUCED'
        WHEN 'OBSERVATION' THEN 'OBSERVATION'
        WHEN 'BLOCKED' THEN 'BLOCKED'
        ELSE 'BLOCKED'
    END,
    plan_mode_after = CASE plan_mode_after
        WHEN 'CONFIRM' THEN 'CONFIRMATION'
        WHEN 'PREPARE' THEN 'PREPARATION'
        WHEN 'REDUCE' THEN 'REDUCED'
        WHEN 'WATCH' THEN 'OBSERVATION'
        WHEN 'CONFIRMATION' THEN 'CONFIRMATION'
        WHEN 'PREPARATION' THEN 'PREPARATION'
        WHEN 'REDUCED' THEN 'REDUCED'
        WHEN 'OBSERVATION' THEN 'OBSERVATION'
        WHEN 'BLOCKED' THEN 'BLOCKED'
        ELSE 'BLOCKED'
    END,
    rule_direction = CASE
        WHEN rule_direction IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        ) THEN rule_direction
        ELSE 'WAIT'
    END;

ALTER TABLE tm_conflict_resolver_result
    ADD COLUMN bias_before VARCHAR(32),
    ADD COLUMN bias_after VARCHAR(32),
    ADD COLUMN adjustment_reason VARCHAR(512),
    ADD COLUMN recovery_condition TEXT,
    ADD COLUMN rule_plan_mode VARCHAR(32),
    ADD COLUMN rule_can_execute BOOLEAN,
    ADD COLUMN data_quality_score INT,
    ADD COLUMN confused_score INT,
    ADD COLUMN account_risk_state VARCHAR(64),
    ADD CONSTRAINT ck_tm_conflict_rule_mode CHECK (
        rule_plan_mode IS NULL OR rule_plan_mode IN (
            'CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED'
        )
    ),
    ADD CONSTRAINT ck_tm_conflict_plan_modes CHECK (
        plan_mode_before IN ('CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED')
        AND plan_mode_after IN ('CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED')
    ),
    ADD CONSTRAINT ck_tm_conflict_market_bias CHECK (
        rule_direction IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        )
        AND (bias_before IS NULL OR bias_before IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        ))
        AND (bias_after IS NULL OR bias_after IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        ))
    ),
    ADD CONSTRAINT ck_tm_conflict_data_quality CHECK (
        data_quality_score IS NULL OR data_quality_score BETWEEN 0 AND 100
    ),
    ADD CONSTRAINT ck_tm_conflict_confused_score CHECK (
        confused_score IS NULL OR confused_score BETWEEN 0 AND 100
    );

ALTER TABLE tm_execution_plan
    ADD COLUMN asset_id BIGINT,
    ADD COLUMN rule_version VARCHAR(32),
    ADD COLUMN rule_market_bias VARCHAR(32),
    ADD COLUMN final_market_bias VARCHAR(32),
    ADD COLUMN candidate_plan_mode VARCHAR(32),
    ADD COLUMN final_plan_mode VARCHAR(32),
    ADD COLUMN bias_adjustment_reason VARCHAR(512),
    ADD COLUMN plan_mode_adjustment_reason VARCHAR(512),
    ADD COLUMN adjustment_reason VARCHAR(512),
    ADD COLUMN downgrade_reason VARCHAR(512),
    ADD COLUMN opportunity_type VARCHAR(64),
    ADD COLUMN entry_logic TEXT,
    ADD COLUMN entry_source VARCHAR(256),
    ADD COLUMN entry_reason TEXT,
    ADD COLUMN trigger_condition TEXT,
    ADD COLUMN stop_logic TEXT,
    ADD COLUMN stop_source VARCHAR(256),
    ADD COLUMN stop_reason TEXT,
    ADD COLUMN target_logic TEXT,
    ADD COLUMN target_source VARCHAR(256),
    ADD COLUMN target_reason TEXT,
    ADD COLUMN add_position_condition TEXT,
    ADD COLUMN reduce_position_condition TEXT,
    ADD COLUMN abandon_condition TEXT,
    ADD COLUMN risk_explanation TEXT,
    ADD COLUMN invalidation_source VARCHAR(256),
    ADD COLUMN invalidation_reason TEXT,
    ADD COLUMN leverage_limit VARCHAR(50),
    ADD COLUMN position_limit VARCHAR(100),
    ADD COLUMN risk_limit NUMERIC(20, 8),
    ADD COLUMN expected_risk_reward NUMERIC(20, 8),
    ADD COLUMN expected_risk_reward_source VARCHAR(256),
    ADD COLUMN expected_risk_reward_reason TEXT,
    ADD COLUMN account_risk_snapshot_id BIGINT,
    ADD COLUMN execution_feasibility_status VARCHAR(32) NOT NULL DEFAULT 'UNAVAILABLE',
    ADD COLUMN slippage_status VARCHAR(32) NOT NULL DEFAULT 'UNAVAILABLE',
    ADD COLUMN depth_status VARCHAR(32) NOT NULL DEFAULT 'UNAVAILABLE',
    ADD COLUMN entry_drift_status VARCHAR(32) NOT NULL DEFAULT 'UNAVAILABLE',
    ADD COLUMN trigger_status VARCHAR(32) NOT NULL DEFAULT 'UNAVAILABLE',
    ADD COLUMN execution_feasibility_reason TEXT,
    ADD COLUMN execution_feasibility_observed_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN execution_feasibility_fresh_until TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN execution_feasibility_source_refs_json TEXT,
    ADD COLUMN analysis_timeframes_json TEXT,
    ADD COLUMN trigger_timeframe VARCHAR(16),
    ADD COLUMN valid_from TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN valid_until TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN holding_horizon VARCHAR(64),
    ADD COLUMN revalidation_rule TEXT,
    ADD COLUMN data_quality INT,
    ADD COLUMN source_refs_json TEXT,
    ADD COLUMN evidence_refs_json TEXT,
    ADD COLUMN score_refs_json TEXT,
    ADD COLUMN validation_result_id VARCHAR(64),
    ADD COLUMN validation_reasons TEXT;
ALTER TABLE tm_execution_plan
    ADD COLUMN source_status VARCHAR(32);

UPDATE tm_execution_plan
SET execution_feasibility_status = 'UNAVAILABLE',
    slippage_status = 'UNAVAILABLE',
    depth_status = 'UNAVAILABLE',
    entry_drift_status = 'UNAVAILABLE',
    trigger_status = 'UNAVAILABLE',
    execution_feasibility_reason = COALESCE(
        execution_feasibility_reason, 'V12_EXECUTION_FEASIBILITY_TRUST_UNAVAILABLE')
WHERE execution_feasibility_status <> 'VERIFIED';

ALTER TABLE tm_execution_plan
    ADD CONSTRAINT ck_tm_execution_plan_execution_feasibility CHECK (
        execution_feasibility_status IN ('VERIFIED', 'PENDING', 'UNAVAILABLE', 'INVALID', 'STALE')
        AND slippage_status IN ('VERIFIED', 'PENDING', 'UNAVAILABLE', 'INVALID', 'STALE')
        AND depth_status IN ('VERIFIED', 'PENDING', 'UNAVAILABLE', 'INVALID', 'STALE')
        AND entry_drift_status IN ('VERIFIED', 'PENDING', 'UNAVAILABLE', 'INVALID', 'STALE')
        AND trigger_status IN ('VERIFIED', 'PENDING', 'UNAVAILABLE', 'INVALID', 'STALE')
        AND (
            execution_feasibility_status <> 'VERIFIED'
            OR (
                slippage_status = 'VERIFIED'
                AND depth_status = 'VERIFIED'
                AND entry_drift_status = 'VERIFIED'
                AND trigger_status = 'VERIFIED'
                AND execution_feasibility_observed_at IS NOT NULL
                AND execution_feasibility_fresh_until > execution_feasibility_observed_at
                AND execution_feasibility_source_refs_json IS NOT NULL
            )
        )
    );

ALTER TABLE tm_account_risk_snapshot
    ADD COLUMN owner_type VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN owner_id BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN account_risk_status VARCHAR(64),
    ADD COLUMN candidate_leverage NUMERIC(10, 4),
    ADD COLUMN max_allowed_leverage NUMERIC(10, 4),
    ADD COLUMN gross_notional NUMERIC(30, 8),
    ADD COLUMN leverage_risk NUMERIC(10, 4),
    ADD COLUMN position_size_risk NUMERIC(10, 4),
    ADD COLUMN concentration_risk NUMERIC(10, 4),
    ADD COLUMN correlation_risk NUMERIC(10, 4),
    ADD COLUMN drawdown_or_var_risk NUMERIC(10, 4),
    ADD COLUMN aggregate_risk_score NUMERIC(10, 4),
    ADD COLUMN source_status VARCHAR(32) NOT NULL DEFAULT 'INVALID',
    ADD COLUMN observed_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN fresh_until TIMESTAMP WITHOUT TIME ZONE;

UPDATE tm_account_risk_snapshot
SET source_status = 'INVALID',
    risk_allowed = FALSE,
    risk_reason_code = COALESCE(risk_reason_code, 'V12_ACCOUNT_RISK_TRUST_UNAVAILABLE'),
    risk_reason_text = COALESCE(risk_reason_text, 'Legacy snapshot has no v4.1 owner/freshness trust evidence')
WHERE source_status <> 'VERIFIED';

ALTER TABLE tm_account_risk_snapshot
    ADD CONSTRAINT ck_tm_account_risk_source_status CHECK (
        source_status IN ('VERIFIED', 'INVALID')
    ),
    ADD CONSTRAINT ck_tm_account_risk_verified_freshness CHECK (
        source_status <> 'VERIFIED'
        OR (observed_at IS NOT NULL AND fresh_until IS NOT NULL AND fresh_until > observed_at)
    );

CREATE INDEX idx_tm_account_risk_owner_analysis
    ON tm_account_risk_snapshot(owner_type, owner_id, analysis_id, create_time DESC);

-- Rule validation may emit the complete ordered set of contract violations. Never
-- truncate that audit evidence to the legacy 512-character display allowance.
ALTER TABLE tm_execution_plan
    ALTER COLUMN rule_veto_reason TYPE TEXT;

ALTER TABLE tm_conflict_resolver_result
    ALTER COLUMN rule_veto_reason TYPE TEXT;

ALTER TABLE tm_decision_result
    ADD CONSTRAINT ck_tm_decision_v41_market_bias CHECK (
        (rule_market_bias IS NULL OR rule_market_bias IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        ))
        AND (final_market_bias IS NULL OR final_market_bias IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        ))
    ),
    ADD CONSTRAINT ck_tm_decision_v41_plan_mode CHECK (
        (rule_plan_mode IS NULL OR rule_plan_mode IN (
            'CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED'
        ))
        AND (candidate_plan_mode IS NULL OR candidate_plan_mode IN (
            'CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED'
        ))
        AND (final_plan_mode IS NULL OR final_plan_mode IN (
            'CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED'
        ))
    );

ALTER TABLE tm_execution_plan
    ADD CONSTRAINT ck_tm_execution_plan_v41_market_bias CHECK (
        (rule_market_bias IS NULL OR rule_market_bias IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        ))
        AND (final_market_bias IS NULL OR final_market_bias IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'RANGE',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH', 'WAIT'
        ))
    ),
    ADD CONSTRAINT ck_tm_execution_plan_v41_plan_mode CHECK (
        (candidate_plan_mode IS NULL OR candidate_plan_mode IN (
            'CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED'
        ))
        AND (final_plan_mode IS NULL OR final_plan_mode IN (
            'CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION', 'BLOCKED'
        ))
    );

UPDATE tm_execution_plan
SET final_plan = FALSE,
    rule_validation_status = 'BLOCKED',
    chain_status = 'RULE_VALIDATION_BLOCKED',
    rule_veto_reason = CASE
        WHEN rule_veto_reason IS NULL OR BTRIM(rule_veto_reason) = ''
            THEN 'V12_FINAL_CONTRACT_FIELDS_UNAVAILABLE'
        ELSE rule_veto_reason || ';V12_FINAL_CONTRACT_FIELDS_UNAVAILABLE'
    END,
    finalized_at = NULL
WHERE final_plan = TRUE;

ALTER TABLE tm_execution_plan
    DROP CONSTRAINT IF EXISTS ck_tm_execution_plan_final_boundary,
    ADD CONSTRAINT ck_tm_execution_plan_final_boundary CHECK (
        (
            final_plan = TRUE
            AND candidate_id IS NOT NULL
            AND opportunity_id IS NOT NULL
            AND resolver_result_id IS NOT NULL
            AND trace_id IS NOT NULL
            AND validation_result_id IS NOT NULL
            AND analysis_id IS NOT NULL
            AND asset_id IS NOT NULL
            AND rule_version IS NOT NULL
            AND rule_market_bias IS NOT NULL
            AND final_market_bias IS NOT NULL
            AND candidate_plan_mode IS NOT NULL
            AND final_plan_mode IN ('CONFIRMATION', 'PREPARATION', 'REDUCED', 'OBSERVATION')
            AND opportunity_type IS NOT NULL
            AND recommended_action IS NOT NULL
            AND entry_logic IS NOT NULL
            AND entry_zone IS NOT NULL AND entry_source IS NOT NULL AND entry_reason IS NOT NULL
            AND trigger_condition IS NOT NULL
            AND stop_logic IS NOT NULL
            AND stop_loss IS NOT NULL AND stop_source IS NOT NULL AND stop_reason IS NOT NULL
            AND target_logic IS NOT NULL
            AND take_profit_rules IS NOT NULL AND target_source IS NOT NULL AND target_reason IS NOT NULL
            AND add_position_condition IS NOT NULL
            AND reduce_position_condition IS NOT NULL
            AND abandon_condition IS NOT NULL
            AND invalid_condition IS NOT NULL
            AND invalidation_source IS NOT NULL
            AND invalidation_reason IS NOT NULL
            AND risk_explanation IS NOT NULL
            AND leverage_limit IS NOT NULL
            AND position_limit IS NOT NULL
            AND risk_limit IS NOT NULL AND risk_limit > 0
            AND account_risk_snapshot_id IS NOT NULL
            AND execution_feasibility_status = 'VERIFIED'
            AND slippage_status = 'VERIFIED'
            AND depth_status = 'VERIFIED'
            AND entry_drift_status = 'VERIFIED'
            AND trigger_status = 'VERIFIED'
            AND execution_feasibility_observed_at IS NOT NULL
            AND execution_feasibility_fresh_until > execution_feasibility_observed_at
            AND execution_feasibility_source_refs_json IS NOT NULL
            AND expected_risk_reward IS NOT NULL AND expected_risk_reward > 0
            AND expected_risk_reward_source IS NOT NULL
            AND expected_risk_reward_reason IS NOT NULL
            AND analysis_timeframes_json IS NOT NULL
            AND trigger_timeframe IS NOT NULL
            AND valid_from IS NOT NULL
            AND valid_until IS NOT NULL
            AND valid_until > valid_from
            AND holding_horizon IS NOT NULL
            AND revalidation_rule IS NOT NULL
            AND data_quality BETWEEN 0 AND 100
            AND source_refs_json IS NOT NULL
            AND evidence_refs_json IS NOT NULL
            AND score_refs_json IS NOT NULL
            AND adjustment_reason IS NOT NULL
            AND source_status = 'VALID'
            AND finalized_at IS NOT NULL
            AND rule_validation_status = 'PASS'
            AND chain_status = 'FINAL_VALIDATED'
        )
        OR (
            final_plan = FALSE
            AND rule_validation_status <> 'PASS'
        )
    );

ALTER TABLE tm_ai_call_log
    ADD COLUMN opportunity_id VARCHAR(64),
    ADD COLUMN cache_hit BOOLEAN,
    ADD COLUMN observed_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX idx_tm_ai_call_log_opportunity
    ON tm_ai_call_log(opportunity_id, started_at DESC);
CREATE INDEX idx_tm_ai_call_log_role
    ON tm_ai_call_log(ai_role, started_at DESC);

ALTER TABLE tm_review_result
    ADD COLUMN opportunity_id VARCHAR(64),
    ADD COLUMN resolver_result_id VARCHAR(64),
    ADD COLUMN validation_result_id VARCHAR(64),
    ADD COLUMN review_type VARCHAR(64),
    ADD COLUMN outcome VARCHAR(64),
    ADD COLUMN execution_deviation TEXT,
    ADD COLUMN ai_assessment TEXT,
    ADD COLUMN rule_assessment TEXT,
    ADD COLUMN rule_feedback TEXT,
    ADD COLUMN metrics_json TEXT,
    ADD COLUMN contract_version VARCHAR(32);

CREATE INDEX idx_tm_review_opportunity
    ON tm_review_result(opportunity_id, create_time DESC);

ALTER TABLE tm_user_position
    DROP CONSTRAINT IF EXISTS ck_tm_user_position_source_type;

UPDATE tm_user_position
SET source_type = 'MANUAL_INDEPENDENT'
WHERE source_type IN ('MANUAL', 'MANUAL_POSITION');

ALTER TABLE tm_user_position
    ALTER COLUMN source_type SET DEFAULT 'MANUAL_INDEPENDENT',
    ADD CONSTRAINT ck_tm_user_position_source_type CHECK (
        (source_type = 'MANUAL_INDEPENDENT' AND final_plan_id IS NULL)
        OR (source_type = 'SYSTEM_PLAN_POSITION' AND final_plan_id IS NOT NULL)
    );
