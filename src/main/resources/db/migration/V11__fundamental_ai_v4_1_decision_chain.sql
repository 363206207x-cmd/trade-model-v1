CREATE TABLE tm_asset_pool_item (
    id BIGSERIAL PRIMARY KEY,
    owner_type VARCHAR(16) NOT NULL,
    owner_id BIGINT NOT NULL DEFAULT 0,
    symbol VARCHAR(32) NOT NULL,
    display_name VARCHAR(64),
    market_type VARCHAR(16) NOT NULL DEFAULT 'SPOT',
    quote_asset VARCHAR(16) NOT NULL DEFAULT 'USDT',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    focus_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    source_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tm_asset_pool_owner_symbol UNIQUE (owner_type, owner_id, symbol),
    CONSTRAINT ck_tm_asset_pool_owner CHECK (
        (owner_type = 'SYSTEM' AND owner_id = 0)
        OR (owner_type = 'USER' AND owner_id > 0)
    ),
    CONSTRAINT ck_tm_asset_pool_source CHECK (source_type IN ('DEFAULT', 'USER_ADDED', 'USER_OVERRIDE'))
);

CREATE INDEX idx_tm_asset_pool_active_order
    ON tm_asset_pool_item(active, focus_enabled, sort_order, id);
CREATE INDEX idx_tm_asset_pool_symbol_active
    ON tm_asset_pool_item(symbol, active);

INSERT INTO tm_asset_pool_item(owner_type, owner_id, symbol, display_name, sort_order, source_type)
VALUES
    ('SYSTEM', 0, 'BTCUSDT', 'BTC', 10, 'DEFAULT'),
    ('SYSTEM', 0, 'ETHUSDT', 'ETH', 20, 'DEFAULT'),
    ('SYSTEM', 0, 'SOLUSDT', 'SOL', 30, 'DEFAULT'),
    ('SYSTEM', 0, 'BNBUSDT', 'BNB', 40, 'DEFAULT'),
    ('SYSTEM', 0, 'XRPUSDT', 'XRP', 50, 'DEFAULT'),
    ('SYSTEM', 0, 'ADAUSDT', 'ADA', 60, 'DEFAULT')
ON CONFLICT (owner_type, owner_id, symbol) DO NOTHING;

ALTER TABLE tm_asset_state
    ADD COLUMN timeframe VARCHAR(16) NOT NULL DEFAULT 'global',
    ADD COLUMN opportunity_id VARCHAR(64),
    ADD COLUMN state_entered_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN cooling_until TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN last_transition_reason VARCHAR(512),
    ADD COLUMN last_trigger_source VARCHAR(64),
    ADD COLUMN last_analysis_id VARCHAR(64);

ALTER TABLE tm_asset_state
    ALTER COLUMN symbol TYPE VARCHAR(32),
    ALTER COLUMN trace_id TYPE VARCHAR(128);

UPDATE tm_asset_state
SET state = CASE
    WHEN UPPER(COALESCE(state, 'OBSERVING')) IN (
        'OBSERVING', 'CANDIDATE', 'WAITING_TRIGGER', 'TRIGGERED',
        'HIGH_RISK', 'INVALIDATED', 'COOLING', 'CONFUSED'
    ) THEN UPPER(COALESCE(state, 'OBSERVING'))
    ELSE 'OBSERVING'
END;

UPDATE tm_asset_state
SET opportunity_id = CONCAT('opp-', LOWER(REGEXP_REPLACE(symbol, '[^A-Za-z0-9]', '', 'g')), '-',
                            LOWER(REGEXP_REPLACE(timeframe, '[^A-Za-z0-9]', '', 'g'))),
    state_entered_at = COALESCE(last_update_time, CURRENT_TIMESTAMP),
    last_transition_reason = COALESCE(last_transition_reason, 'LEGACY_STATE_ADOPTED'),
    last_trigger_source = COALESCE(last_trigger_source, 'LEGACY_ANALYSIS')
WHERE opportunity_id IS NULL;

ALTER TABLE tm_asset_state
    DROP CONSTRAINT IF EXISTS tm_asset_state_symbol_key,
    ALTER COLUMN opportunity_id SET NOT NULL,
    ALTER COLUMN state_entered_at SET NOT NULL,
    ALTER COLUMN state SET NOT NULL,
    ADD CONSTRAINT uk_tm_asset_state_symbol_timeframe UNIQUE (symbol, timeframe),
    ADD CONSTRAINT uk_tm_asset_state_opportunity UNIQUE (opportunity_id),
    ADD CONSTRAINT ck_tm_asset_state_state CHECK (
        state IN ('OBSERVING', 'CANDIDATE', 'WAITING_TRIGGER', 'TRIGGERED',
                  'HIGH_RISK', 'INVALIDATED', 'COOLING', 'CONFUSED')
    );

CREATE TABLE tm_opportunity_state_transition (
    transition_id VARCHAR(64) PRIMARY KEY,
    opportunity_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    timeframe VARCHAR(16) NOT NULL,
    analysis_id VARCHAR(64),
    trace_id VARCHAR(128) NOT NULL,
    from_state VARCHAR(32),
    to_state VARCHAR(32) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    trigger_source VARCHAR(64) NOT NULL,
    transition_priority INT NOT NULL,
    suppressed BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT ck_tm_opportunity_transition_from CHECK (
        from_state IS NULL OR from_state IN ('OBSERVING', 'CANDIDATE', 'WAITING_TRIGGER', 'TRIGGERED',
                                             'HIGH_RISK', 'INVALIDATED', 'COOLING', 'CONFUSED')
    ),
    CONSTRAINT ck_tm_opportunity_transition_to CHECK (
        to_state IN ('OBSERVING', 'CANDIDATE', 'WAITING_TRIGGER', 'TRIGGERED',
                     'HIGH_RISK', 'INVALIDATED', 'COOLING', 'CONFUSED')
    )
);

CREATE INDEX idx_tm_opportunity_transition_opportunity_time
    ON tm_opportunity_state_transition(opportunity_id, occurred_at DESC);
CREATE INDEX idx_tm_opportunity_transition_analysis
    ON tm_opportunity_state_transition(analysis_id, occurred_at DESC);

CREATE TABLE tm_execution_plan_candidate (
    candidate_id VARCHAR(64) PRIMARY KEY,
    opportunity_id VARCHAR(64) NOT NULL,
    analysis_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    rule_direction VARCHAR(32) NOT NULL,
    rule_confidence VARCHAR(16) NOT NULL,
    rule_risk VARCHAR(16) NOT NULL,
    candidate_direction VARCHAR(32) NOT NULL,
    plan_mode VARCHAR(32) NOT NULL,
    confidence_level VARCHAR(16) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    worth_opening BOOLEAN NOT NULL,
    recommended_action VARCHAR(128),
    entry_zone VARCHAR(256),
    stop_loss VARCHAR(256),
    take_profit_rules TEXT,
    leverage_suggestion VARCHAR(128),
    position_suggestion VARCHAR(256),
    invalid_condition TEXT,
    validity VARCHAR(128),
    summary TEXT,
    candidate_source VARCHAR(32) NOT NULL,
    candidate_status VARCHAR(32) NOT NULL,
    fallback_reason VARCHAR(512),
    payload_json TEXT NOT NULL,
    not_final_plan BOOLEAN NOT NULL DEFAULT TRUE,
    not_state_machine_mutation BOOLEAN NOT NULL DEFAULT TRUE,
    not_user_position_creation BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_plan_candidate_source CHECK (candidate_source IN ('GPT_FINAL', 'RULE_FALLBACK')),
    CONSTRAINT ck_tm_plan_candidate_status CHECK (
        candidate_status IN ('GENERATED', 'FALLBACK', 'REJECTED', 'VALIDATED')
    ),
    CONSTRAINT ck_tm_plan_candidate_mode CHECK (
        plan_mode IN ('CONFIRM', 'PREPARE', 'REDUCE', 'WATCH', 'BLOCKED')
    ),
    CONSTRAINT ck_tm_plan_candidate_confidence CHECK (
        rule_confidence IN ('LOW', 'MEDIUM', 'HIGH')
        AND confidence_level IN ('LOW', 'MEDIUM', 'HIGH')
    ),
    CONSTRAINT ck_tm_plan_candidate_risk CHECK (
        rule_risk IN ('LOW', 'MEDIUM', 'HIGH', 'EXTREME')
        AND risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'EXTREME')
    ),
    CONSTRAINT ck_tm_plan_candidate_safety CHECK (
        not_final_plan = TRUE
        AND not_state_machine_mutation = TRUE
        AND not_user_position_creation = TRUE
    )
);

CREATE UNIQUE INDEX uk_tm_plan_candidate_analysis
    ON tm_execution_plan_candidate(analysis_id);
CREATE INDEX idx_tm_plan_candidate_opportunity
    ON tm_execution_plan_candidate(opportunity_id, created_at DESC);

CREATE TABLE tm_conflict_resolver_result (
    resolver_result_id VARCHAR(64) PRIMARY KEY,
    candidate_id VARCHAR(64) NOT NULL,
    analysis_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    rule_direction VARCHAR(32) NOT NULL,
    rule_confidence VARCHAR(16) NOT NULL,
    rule_risk VARCHAR(16) NOT NULL,
    gemini_review_json TEXT NOT NULL,
    grok_challenge_json TEXT NOT NULL,
    conflict_level VARCHAR(32) NOT NULL,
    conflict_score INT NOT NULL,
    plan_mode_before VARCHAR(32) NOT NULL,
    plan_mode_after VARCHAR(32) NOT NULL,
    confidence_before VARCHAR(16) NOT NULL,
    confidence_after VARCHAR(16) NOT NULL,
    risk_before VARCHAR(16) NOT NULL,
    risk_after VARCHAR(16) NOT NULL,
    downgrade_reason VARCHAR(512),
    confused_decision BOOLEAN NOT NULL DEFAULT FALSE,
    rule_veto_reason VARCHAR(512),
    rule_direction_preserved BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_conflict_score CHECK (conflict_score BETWEEN 0 AND 100),
    CONSTRAINT ck_tm_conflict_level CHECK (conflict_level IN (
        'LEVEL_1_CONSISTENT',
        'LEVEL_2_MINOR_DISAGREEMENT',
        'LEVEL_3_SIGNIFICANT_DISAGREEMENT',
        'LEVEL_4_EXTREME_CONFLICT'
    )),
    CONSTRAINT ck_tm_conflict_direction CHECK (rule_direction_preserved = TRUE)
);

CREATE UNIQUE INDEX uk_tm_conflict_resolver_candidate
    ON tm_conflict_resolver_result(candidate_id);
CREATE INDEX idx_tm_conflict_resolver_analysis
    ON tm_conflict_resolver_result(analysis_id, created_at DESC);

ALTER TABLE tm_opportunity_state_transition
    ADD CONSTRAINT fk_tm_opportunity_transition_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES tm_asset_state(opportunity_id);

ALTER TABLE tm_execution_plan_candidate
    ADD CONSTRAINT fk_tm_plan_candidate_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES tm_asset_state(opportunity_id),
    ADD CONSTRAINT fk_tm_plan_candidate_analysis
        FOREIGN KEY (analysis_id) REFERENCES tm_analysis_run(analysis_id);

ALTER TABLE tm_conflict_resolver_result
    ADD CONSTRAINT fk_tm_conflict_candidate
        FOREIGN KEY (candidate_id) REFERENCES tm_execution_plan_candidate(candidate_id),
    ADD CONSTRAINT fk_tm_conflict_analysis
        FOREIGN KEY (analysis_id) REFERENCES tm_analysis_run(analysis_id);

ALTER TABLE tm_execution_plan
    ADD COLUMN candidate_id VARCHAR(64),
    ADD COLUMN opportunity_id VARCHAR(64),
    ADD COLUMN resolver_result_id VARCHAR(64),
    ADD COLUMN trace_id VARCHAR(128),
    ADD COLUMN chain_status VARCHAR(32) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN rule_validation_status VARCHAR(32) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN rule_veto_reason VARCHAR(512),
    ADD COLUMN finalized_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN final_plan BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_tm_execution_plan_chain_status CHECK (
        chain_status IN ('LEGACY', 'FINAL_VALIDATED', 'RULE_FALLBACK_VALIDATED', 'RULE_VALIDATION_BLOCKED')
    ),
    ADD CONSTRAINT ck_tm_execution_plan_rule_validation CHECK (
        rule_validation_status IN ('LEGACY', 'PASS', 'BLOCKED')
    ),
    ADD CONSTRAINT ck_tm_execution_plan_final_boundary CHECK (
        (
            final_plan = TRUE
            AND candidate_id IS NOT NULL
            AND opportunity_id IS NOT NULL
            AND resolver_result_id IS NOT NULL
            AND trace_id IS NOT NULL
            AND finalized_at IS NOT NULL
            AND rule_validation_status = 'PASS'
            AND chain_status IN ('FINAL_VALIDATED', 'RULE_FALLBACK_VALIDATED')
        )
        OR (
            final_plan = FALSE
            AND rule_validation_status <> 'PASS'
        )
    );

CREATE INDEX idx_tm_execution_plan_candidate
    ON tm_execution_plan(candidate_id);
CREATE INDEX idx_tm_execution_plan_opportunity
    ON tm_execution_plan(opportunity_id, create_time DESC);

ALTER TABLE tm_execution_plan
    ADD CONSTRAINT fk_tm_execution_plan_candidate
        FOREIGN KEY (candidate_id) REFERENCES tm_execution_plan_candidate(candidate_id),
    ADD CONSTRAINT fk_tm_execution_plan_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES tm_asset_state(opportunity_id),
    ADD CONSTRAINT fk_tm_execution_plan_resolver
        FOREIGN KEY (resolver_result_id) REFERENCES tm_conflict_resolver_result(resolver_result_id);

ALTER TABLE tm_user_position
    ADD COLUMN final_plan_id VARCHAR(64),
    ADD CONSTRAINT fk_tm_user_position_final_plan
        FOREIGN KEY (final_plan_id) REFERENCES tm_execution_plan(plan_id);

ALTER TABLE tm_user_position
    DROP CONSTRAINT IF EXISTS ck_tm_user_position_source_type;

UPDATE tm_user_position
SET source_type = 'MANUAL_POSITION'
WHERE source_type = 'MANUAL';

ALTER TABLE tm_user_position
    ALTER COLUMN source_type SET DEFAULT 'MANUAL_POSITION',
    ADD CONSTRAINT ck_tm_user_position_source_type CHECK (
        (source_type = 'MANUAL_POSITION' AND final_plan_id IS NULL)
        OR (source_type = 'SYSTEM_PLAN_POSITION' AND final_plan_id IS NOT NULL)
    );

CREATE INDEX idx_tm_user_position_final_plan
    ON tm_user_position(final_plan_id);

ALTER TABLE tm_review_result
    ADD COLUMN final_plan_id VARCHAR(64),
    ADD COLUMN candidate_id VARCHAR(64),
    ADD COLUMN trace_id VARCHAR(128),
    ADD CONSTRAINT fk_tm_review_final_plan
        FOREIGN KEY (final_plan_id) REFERENCES tm_execution_plan(plan_id),
    ADD CONSTRAINT fk_tm_review_candidate
        FOREIGN KEY (candidate_id) REFERENCES tm_execution_plan_candidate(candidate_id);

ALTER TABLE tm_ai_call_log
    DROP CONSTRAINT IF EXISTS ck_tm_ai_call_log_safety;

ALTER TABLE tm_ai_call_log
    ADD COLUMN contract_type VARCHAR(64) NOT NULL DEFAULT 'AI_ROLE_RESULTS_SCHEMA_V1',
    ADD COLUMN candidate_id VARCHAR(64),
    ADD COLUMN output_payload TEXT,
    ADD COLUMN not_final_execution_plan_creation BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE tm_ai_call_log
    ADD CONSTRAINT ck_tm_ai_call_log_safety CHECK (
        manual_review_only = TRUE
        AND not_trade_instruction = TRUE
        AND not_executable = TRUE
        AND not_auto_trading = TRUE
        AND not_order_execution = TRUE
        AND not_user_position_creation = TRUE
        AND not_position_mutation = TRUE
        AND not_state_machine_override = TRUE
        AND not_final_execution_plan_creation = TRUE
        AND rule_direction_preserved = TRUE
        AND (
            (contract_type = 'AI_ROLE_RESULTS_SCHEMA_V1'
                AND review_only = TRUE
                AND not_execution_plan_creation = TRUE)
            OR
            (contract_type = 'DECISION_CHAIN_V4_1'
                AND (
                    (ai_role = 'GPT_FINAL'
                        AND review_only = FALSE
                        AND not_execution_plan_creation = FALSE)
                    OR
                    (ai_role IN ('GEMINI_REVIEW', 'GROK_CHALLENGE')
                        AND review_only = TRUE
                        AND not_execution_plan_creation = TRUE)
                ))
        )
    );

CREATE INDEX idx_tm_ai_call_log_candidate
    ON tm_ai_call_log(candidate_id, started_at DESC);
