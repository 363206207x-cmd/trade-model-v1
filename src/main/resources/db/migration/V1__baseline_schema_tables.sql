-- Trade Model V1 PostgreSQL baseline schema tables.
-- Generated from current V1 table semantics for PDR-2C1.
-- Local and test bootstrap remains src/main/resources/schema.sql.
-- Production readiness remains blocked until validation packages complete.

CREATE TABLE IF NOT EXISTS tm_analysis_run (
    analysis_id VARCHAR(64) PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10) NOT NULL,
    analysis_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    rule_version VARCHAR(32),
    data_quality_score INTEGER,
    trace_id VARCHAR(128),
    status VARCHAR(20),
    idempotency_key VARCHAR(128),
    request_id VARCHAR(128),
    trigger_type VARCHAR(64),
    trigger_reference VARCHAR(256),
    parent_analysis_id VARCHAR(64),
    parent_trace_id VARCHAR(128),
    input_snapshot_json TEXT,
    input_snapshot_hash VARCHAR(128),
    attempt_count INTEGER NOT NULL DEFAULT 1,
    lease_owner VARCHAR(128),
    lease_expires_at TIMESTAMP WITHOUT TIME ZONE,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    error_code VARCHAR(128),
    error_message VARCHAR(512),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT ck_tm_analysis_run_status CHECK (
        status IS NULL OR status IN ('STARTED', 'SUCCESS', 'FAILED')
    )
);

CREATE TABLE IF NOT EXISTS tm_evidence_item (
    evidence_id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    evidence_type VARCHAR(50),
    description TEXT,
    direction VARCHAR(20),
    strength double precision,
    confidence double precision,
    source VARCHAR(100),
    source_provider VARCHAR(100),
    source_reference VARCHAR(512),
    source_trace_id VARCHAR(128),
    external_event_id VARCHAR(64),
    external_event_type VARCHAR(32),
    event_window_start TIMESTAMP WITHOUT TIME ZONE,
    event_window_end TIMESTAMP WITHOUT TIME ZONE,
    impact_score INTEGER,
    severity VARCHAR(32),
    create_time TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS tm_score_item (
    score_id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    score_type VARCHAR(50),
    score_value double precision,
    weight double precision,
    direction VARCHAR(20),
    description TEXT
);

CREATE TABLE IF NOT EXISTS tm_macro_event (
    event_id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    title VARCHAR(256) NOT NULL,
    description TEXT,
    affected_symbols VARCHAR(512),
    market_scope VARCHAR(64),
    event_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    window_start TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    window_end TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    impact_score INTEGER NOT NULL,
    severity VARCHAR(32) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_reference VARCHAR(512) NOT NULL,
    source_trace_id VARCHAR(128) NOT NULL,
    source_event_id VARCHAR(128),
    source_hash VARCHAR(128),
    source_published_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    source_published_at_reason_code VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    execution_blocking BOOLEAN NOT NULL DEFAULT FALSE,
    dedupe_key VARCHAR(256) NOT NULL,
    review_only BOOLEAN NOT NULL DEFAULT TRUE,
    manual_review_only BOOLEAN NOT NULL DEFAULT TRUE,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_executable BOOLEAN NOT NULL DEFAULT TRUE,
    not_auto_trading BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    not_user_position_creation BOOLEAN NOT NULL DEFAULT TRUE,
    not_user_position_mutation BOOLEAN NOT NULL DEFAULT TRUE,
    not_push_send BOOLEAN NOT NULL DEFAULT TRUE,
    not_external_channel BOOLEAN NOT NULL DEFAULT TRUE,
    not_external_fetch BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_news_event (
    event_id VARCHAR(64) PRIMARY KEY,
    headline VARCHAR(256) NOT NULL,
    summary TEXT,
    affected_symbols VARCHAR(512),
    market_scope VARCHAR(64),
    event_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    window_start TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    window_end TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    impact_score INTEGER NOT NULL,
    severity VARCHAR(32) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_reference VARCHAR(512) NOT NULL,
    source_trace_id VARCHAR(128) NOT NULL,
    source_event_id VARCHAR(128),
    source_hash VARCHAR(128),
    source_published_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    execution_blocking BOOLEAN NOT NULL DEFAULT FALSE,
    dedupe_key VARCHAR(256) NOT NULL,
    review_only BOOLEAN NOT NULL DEFAULT TRUE,
    manual_review_only BOOLEAN NOT NULL DEFAULT TRUE,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_executable BOOLEAN NOT NULL DEFAULT TRUE,
    not_auto_trading BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    not_user_position_creation BOOLEAN NOT NULL DEFAULT TRUE,
    not_user_position_mutation BOOLEAN NOT NULL DEFAULT TRUE,
    not_push_send BOOLEAN NOT NULL DEFAULT TRUE,
    not_external_channel BOOLEAN NOT NULL DEFAULT TRUE,
    not_external_fetch BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_decision_result (
    decision_id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(20),
    market_bias_hierarchy VARCHAR(30),
    trade_type VARCHAR(30),
    confidence_level VARCHAR(20),
    risk_level VARCHAR(20),
    action_priority VARCHAR(20),
    conclusion_summary TEXT,
    is_worth_opening BOOLEAN,
    multi_tf_convergence VARCHAR(50),
    ai_role_results TEXT,
    is_adopted BOOLEAN,
    valid_period VARCHAR(200),
    invalid_condition TEXT,
    evidence_summary TEXT,
    explanation_json TEXT,
    review_reasons TEXT,
    ai_conflict_level VARCHAR(64),
    ai_conflict_score INTEGER,
    ai_plan_mode VARCHAR(50),
    confused_score INTEGER,
    asset_state_snapshot VARCHAR(512),
    hot_reset_invalidated BOOLEAN NOT NULL DEFAULT FALSE,
    hot_reset_event_id VARCHAR(64),
    hot_reset_invalidated_at TIMESTAMP WITHOUT TIME ZONE,
    hot_reset_reason_code VARCHAR(64),
    create_time TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS tm_execution_plan (
    plan_id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    plan_mode VARCHAR(32) NOT NULL DEFAULT 'ADVISORY',
    execution_plan_status VARCHAR(32) NOT NULL DEFAULT 'INCOMPLETE',
    source_gate_status VARCHAR(32) NOT NULL DEFAULT 'INCOMPLETE',
    source_gate_complete BOOLEAN NOT NULL DEFAULT FALSE,
    source_missing_reasons TEXT,
    source_blocker_reasons TEXT,
    source_completeness_summary TEXT,
    recommended_action VARCHAR(50),
    entry_zone VARCHAR(100),
    stop_loss VARCHAR(100),
    take_profit_rules TEXT,
    leverage_suggestion VARCHAR(50),
    position_suggestion VARCHAR(100),
    account_risk_json TEXT,
    invalid_condition TEXT,
    manual_review_required BOOLEAN NOT NULL DEFAULT TRUE,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_executable BOOLEAN NOT NULL DEFAULT TRUE,
    not_auto_trading BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    not_user_position_creation BOOLEAN NOT NULL DEFAULT TRUE,
    needs_revalidation BOOLEAN NOT NULL DEFAULT FALSE,
    revalidation_reason VARCHAR(512),
    hot_reset_event_id VARCHAR(64),
    revalidation_required_at TIMESTAMP WITHOUT TIME ZONE,
    create_time TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT ck_tm_execution_plan_status CHECK (
        execution_plan_status IN ('VALID', 'INCOMPLETE', 'BLOCKED', 'REVIEW_ONLY', 'INVALID')
    ),
    CONSTRAINT ck_tm_execution_plan_source_gate_status CHECK (
        source_gate_status IN ('VALID', 'INCOMPLETE', 'BLOCKED', 'REVIEW_ONLY', 'INVALID')
    ),
    CONSTRAINT ck_tm_execution_plan_safety_flags CHECK (
        manual_review_required = TRUE
        AND not_trade_instruction = TRUE
        AND not_executable = TRUE
        AND not_auto_trading = TRUE
        AND not_order_execution = TRUE
        AND not_user_position_creation = TRUE
    )
);

CREATE TABLE IF NOT EXISTS tm_market_environment_snapshot (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL UNIQUE,
    symbol VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10) NOT NULL,
    environment_type VARCHAR(50),
    risk_mode VARCHAR(50),
    trend_friendliness INTEGER,
    leverage_suggestion VARCHAR(50),
    range_pct_24h double precision,
    volatility_regime VARCHAR(50),
    last_funding_rate DECIMAL(20, 10),
    perp_funding_applied BOOLEAN,
    last_open_interest DECIMAL(28, 8),
    open_interest_delta DECIMAL(28, 8),
    oi_applied BOOLEAN,
    derivatives_crowding_state VARCHAR(32),
    summary TEXT,
    source_type VARCHAR(64) NOT NULL,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_persisted_ohlcv_bar (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10) NOT NULL,
    open_time_ms BIGINT NOT NULL,
    close_time_ms BIGINT NOT NULL,
    open_price DECIMAL(20, 8) NOT NULL,
    high_price DECIMAL(20, 8) NOT NULL,
    low_price DECIMAL(20, 8) NOT NULL,
    close_price DECIMAL(20, 8) NOT NULL,
    volume DECIMAL(28, 8) NOT NULL,
    quote_volume DECIMAL(28, 8),
    trade_count BIGINT,
    taker_buy_base_volume DECIMAL(28, 8),
    taker_buy_quote_volume DECIMAL(28, 8),
    is_closed BOOLEAN NOT NULL,
    provider VARCHAR(64) NOT NULL,
    provider_market_type VARCHAR(32) NOT NULL,
    source_endpoint VARCHAR(256) NOT NULL,
    source_batch_id VARCHAR(64) NOT NULL,
    source_trace_id VARCHAR(64) NOT NULL,
    source_version INTEGER NOT NULL,
    ingested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    quality_status VARCHAR(32) NOT NULL,
    quality_reason VARCHAR(512),
    raw_payload_hash VARCHAR(128),
    is_deleted INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tm_rule_config (
    rule_id VARCHAR(64) PRIMARY KEY,
    rule_type VARCHAR(50),
    rule_key VARCHAR(100) UNIQUE,
    rule_value TEXT,
    description TEXT,
    version VARCHAR(20),
    enabled BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS tm_user_config (
    user_id VARCHAR(64) PRIMARY KEY,
    risk_preference VARCHAR(50),
    ai_model_preference VARCHAR(50),
    notify_channels VARCHAR(100),
    cooldown_minutes INTEGER DEFAULT 15
);

CREATE TABLE IF NOT EXISTS tm_real_position (
    position_id VARCHAR(64) PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) DEFAULT 'UNKNOWN',
    source_name VARCHAR(50),
    position_side VARCHAR(10),
    avg_open_price DECIMAL(20, 8),
    position_open_time TIMESTAMP WITHOUT TIME ZONE,
    position_quantity DECIMAL(20, 8),
    unrealized_pnl_pct DECIMAL(10, 4),
    position_status VARCHAR(20) DEFAULT 'OPEN',
    mark_price DECIMAL(20, 8),
    break_even_price DECIMAL(20, 8),
    liquidation_price DECIMAL(20, 8),
    update_time TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS tm_user_position (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    asset_symbol VARCHAR(32) NOT NULL,
    side VARCHAR(10) NOT NULL,
    status VARCHAR(32) NOT NULL,
    entry_price DECIMAL(20, 8) NOT NULL,
    quantity DECIMAL(28, 8) NOT NULL,
    leverage DECIMAL(20, 8) NOT NULL,
    stop_loss DECIMAL(20, 8),
    take_profit DECIMAL(20, 8),
    opened_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITHOUT TIME ZONE,
    close_price DECIMAL(20, 8),
    close_reason VARCHAR(512),
    source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    source_ref_id VARCHAR(128),
    manual_review_required BOOLEAN NOT NULL DEFAULT TRUE,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_auto_trading BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    not_position_sync BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_user_position_side CHECK (side IN ('LONG', 'SHORT')),
    CONSTRAINT ck_tm_user_position_status CHECK (status IN ('OPEN', 'PARTIALLY_CLOSED', 'CLOSED')),
    CONSTRAINT ck_tm_user_position_source_type CHECK (source_type = 'MANUAL'),
    CONSTRAINT ck_tm_user_position_entry_price CHECK (entry_price > 0),
    CONSTRAINT ck_tm_user_position_quantity CHECK (quantity > 0),
    CONSTRAINT ck_tm_user_position_leverage CHECK (leverage > 0),
    CONSTRAINT ck_tm_user_position_safety_flags CHECK (
        manual_review_required = TRUE
        AND not_trade_instruction = TRUE
        AND not_auto_trading = TRUE
        AND not_order_execution = TRUE
        AND not_position_sync = TRUE
    )
);

CREATE TABLE IF NOT EXISTS tm_position_monitor_log (
    log_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    position_id BIGINT NOT NULL,
    analysis_id VARCHAR(64) NOT NULL,
    execution_plan_id VARCHAR(64),
    current_price DECIMAL(20, 8) NOT NULL,
    logic_status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    suggested_action VARCHAR(32) NOT NULL,
    reason VARCHAR(1024),
    evidence_snapshot TEXT,
    score_snapshot TEXT,
    decision_snapshot TEXT,
    risk_snapshot TEXT,
    trace_id VARCHAR(64),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_position_monitor_log_price CHECK (current_price > 0),
    CONSTRAINT ck_tm_position_monitor_log_logic_status CHECK (
        logic_status IN ('LOGIC_VALID', 'LOGIC_WEAKENED', 'PLAN_INVALIDATED', 'HIGH_RISK')
    ),
    CONSTRAINT ck_tm_position_monitor_log_suggested_action CHECK (
        suggested_action IN ('HOLD', 'MANUAL_REVIEW', 'RECHECK_PLAN', 'RISK_REVIEW')
    )
);

CREATE TABLE IF NOT EXISTS tm_push_snapshot (
    push_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(20),
    timeframe VARCHAR(10),
    push_type VARCHAR(50),
    push_status VARCHAR(30),
    push_create_time TIMESTAMP WITHOUT TIME ZONE,
    rule_version VARCHAR(20),
    trigger_price DECIMAL(20, 8),
    entry_zone_json TEXT,
    stop_zone_json TEXT,
    invalidation_condition_json TEXT,
    plan_mode_snapshot VARCHAR(100),
    cause_effect_alignment_snapshot VARCHAR(200),
    execution_feasibility_snapshot INTEGER,
    data_quality_score_snapshot INTEGER,
    confused_score_snapshot INTEGER,
    account_risk_snapshot_id BIGINT,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    trace_id VARCHAR(64),
    create_time TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS tm_account_risk_snapshot (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(20),
    risk_level_snapshot VARCHAR(32),
    risk_allowed BOOLEAN NOT NULL,
    risk_reason_code VARCHAR(64),
    risk_reason_text VARCHAR(512),
    position_exposure DECIMAL(10, 4),
    max_allowed_exposure DECIMAL(10, 4),
    snapshot_source VARCHAR(128),
    snapshot_version INTEGER,
    source_note VARCHAR(128),
    trace_id VARCHAR(64),
    create_time TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS tm_push_recheck_log (
    log_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    push_id BIGINT NOT NULL,
    dispatch_batch_id VARCHAR(64),
    dispatch_instruction_id VARCHAR(64),
    trigger_source VARCHAR(32),
    retry_attempt INTEGER,
    max_attempts INTEGER,
    retry_backoff_minutes INTEGER,
    replay_from_log_id BIGINT,
    execution_status VARCHAR(32),
    execution_error_code VARCHAR(64),
    execution_error_message VARCHAR(512),
    recheck_time TIMESTAMP WITHOUT TIME ZONE,
    recheck_status VARCHAR(30),
    current_price DECIMAL(20, 8),
    price_drift_ratio DECIMAL(20, 8),
    current_slippage_estimation DECIMAL(20, 8),
    current_data_quality_score INTEGER,
    current_confused_score INTEGER,
    current_account_risk_allowed BOOLEAN,
    fail_reason_json TEXT,
    trace_id VARCHAR(64),
    create_time TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS tm_push_recheck_dispatch_config (
    config_key VARCHAR(64) PRIMARY KEY,
    config_value INTEGER NOT NULL,
    updated_by VARCHAR(64),
    update_source VARCHAR(64),
    update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_push_recheck_dispatch_config_audit (
    audit_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL,
    old_value INTEGER,
    new_value INTEGER NOT NULL,
    changed_by VARCHAR(64),
    change_source VARCHAR(64),
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_monitor_alert (
    id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64),
    asset_symbol VARCHAR(20),
    alert_type VARCHAR(50),
    alert_level VARCHAR(20),
    alert_message TEXT,
    status VARCHAR(30),
    cooldown_until TIMESTAMP WITHOUT TIME ZONE,
    suppress_reason VARCHAR(512),
    trace_id VARCHAR(64),
    rule_version VARCHAR(20),
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    version_no INTEGER DEFAULT 1
);

CREATE TABLE IF NOT EXISTS tm_opportunity_log (
    opportunity_id VARCHAR(64) PRIMARY KEY,
    opportunity_key VARCHAR(160) NOT NULL UNIQUE,
    analysis_id VARCHAR(64) NOT NULL,
    decision_id VARCHAR(64),
    execution_plan_id VARCHAR(64),
    push_id BIGINT,
    user_position_id BIGINT,
    symbol VARCHAR(32) NOT NULL,
    timeframe VARCHAR(16) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    lifecycle_status VARCHAR(32) NOT NULL,
    opportunity_status VARCHAR(40),
    anchor_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    evaluation_as_of TIMESTAMP WITHOUT TIME ZONE,
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    entry_reference DECIMAL(20, 8),
    target_price DECIMAL(20, 8),
    invalidation_price DECIMAL(20, 8),
    target_hit BOOLEAN NOT NULL DEFAULT FALSE,
    invalidation_hit BOOLEAN NOT NULL DEFAULT FALSE,
    target_hit_at TIMESTAMP WITHOUT TIME ZONE,
    invalidation_hit_at TIMESTAMP WITHOUT TIME ZONE,
    hit_order VARCHAR(32),
    mfe_price DECIMAL(20, 8),
    mfe_ratio DECIMAL(20, 10),
    mae_price DECIMAL(20, 8),
    mae_ratio DECIMAL(20, 10),
    push_present BOOLEAN NOT NULL DEFAULT FALSE,
    risk_blocked_evidence BOOLEAN NOT NULL DEFAULT FALSE,
    risk_blocked_at TIMESTAMP WITHOUT TIME ZONE,
    user_position_present BOOLEAN NOT NULL DEFAULT FALSE,
    source_type VARCHAR(64),
    source_reference VARCHAR(256),
    market_data_source VARCHAR(128),
    market_data_trace_id VARCHAR(128),
    reason_codes TEXT,
    trace_id VARCHAR(64),
    review_only BOOLEAN NOT NULL DEFAULT TRUE,
    manual_review_only BOOLEAN NOT NULL DEFAULT TRUE,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_executable BOOLEAN NOT NULL DEFAULT TRUE,
    not_auto_trading BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    not_user_position_creation BOOLEAN NOT NULL DEFAULT TRUE,
    not_user_position_mutation BOOLEAN NOT NULL DEFAULT TRUE,
    not_push_send BOOLEAN NOT NULL DEFAULT TRUE,
    not_external_channel BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_opportunity_log_lifecycle CHECK (
        lifecycle_status IN ('PENDING_EVALUATION', 'RESOLVED', 'SOURCE_INCOMPLETE',
        'MARKET_PATH_UNAVAILABLE', 'AMBIGUOUS_MARKET_PATH', 'REVIEW_REQUIRED')
    ),
    CONSTRAINT ck_tm_opportunity_log_status CHECK (
        opportunity_status IS NULL OR opportunity_status IN ('EXECUTED_VALID', 'EXECUTED_INVALID',
        'MISSED_VALID', 'MISSED_INVALID', 'PUSHED_NOT_FILLED_VALID', 'BLOCKED_BY_RISK_VALID')
    ),
    CONSTRAINT ck_tm_opportunity_log_hit_order CHECK (
        hit_order IS NULL OR hit_order IN ('TARGET_FIRST', 'INVALIDATION_FIRST', 'AMBIGUOUS_SAME_BAR')
    ),
    CONSTRAINT ck_tm_opportunity_log_safety CHECK (
        review_only = TRUE
        AND manual_review_only = TRUE
        AND not_trade_instruction = TRUE
        AND not_executable = TRUE
        AND not_auto_trading = TRUE
        AND not_order_execution = TRUE
        AND not_user_position_creation = TRUE
        AND not_user_position_mutation = TRUE
        AND not_push_send = TRUE
        AND not_external_channel = TRUE
    )
);

CREATE TABLE IF NOT EXISTS tm_missed_opportunity (
    missed_id VARCHAR(64) PRIMARY KEY,
    decision_id VARCHAR(64) NOT NULL,
    analysis_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(20),
    biz_date DATE NOT NULL,
    reason_json TEXT,
    rule_version VARCHAR(20),
    trace_id VARCHAR(64),
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_review_result (
    id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    error_type VARCHAR(200),
    actual_outcome VARCHAR(2000),
    adjustment_suggestion VARCHAR(4000),
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_rule_version_log (
    id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64),
    rule_version VARCHAR(20),
    error_type VARCHAR(200),
    change_category VARCHAR(64),
    change_summary VARCHAR(1024),
    change_detail TEXT,
    operator VARCHAR(64),
    publish_time VARCHAR(64),
    rollback_flag VARCHAR(20),
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    version_no INTEGER DEFAULT 1
);

CREATE TABLE IF NOT EXISTS tm_asset_state (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL UNIQUE,
    state VARCHAR(32),
    confused_score INTEGER,
    confused_low_streak INTEGER NOT NULL DEFAULT 0,
    hot_reset_flag BOOLEAN DEFAULT FALSE,
    hot_reset_trigger_type VARCHAR(64),
    hot_reset_trigger_value VARCHAR(128),
    hot_reset_time TIMESTAMP WITHOUT TIME ZONE,
    pre_reset_state VARCHAR(32),
    post_reset_state VARCHAR(32),
    last_update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    trace_id VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS tm_hot_reset_event (
    event_id VARCHAR(64) PRIMARY KEY,
    event_key VARCHAR(128) NOT NULL UNIQUE,
    analysis_id VARCHAR(64) NOT NULL,
    rebuild_analysis_id VARCHAR(64),
    trace_id VARCHAR(64),
    symbol VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10),
    trigger_type VARCHAR(64) NOT NULL,
    trigger_value VARCHAR(128),
    source_type VARCHAR(64),
    source_reference VARCHAR(256),
    severity_score INTEGER,
    decision_id VARCHAR(64),
    decision_state VARCHAR(32),
    decision_invalidated_count INTEGER NOT NULL DEFAULT 0,
    plan_revalidation_count INTEGER NOT NULL DEFAULT 0,
    push_invalidated_count INTEGER NOT NULL DEFAULT 0,
    confused_score_snapshot INTEGER,
    confused_score_before INTEGER,
    confused_score_after INTEGER,
    multi_timeframe_aligned_snapshot BOOLEAN,
    account_risk_status VARCHAR(64),
    account_risk_level VARCHAR(32),
    account_risk_blocked BOOLEAN,
    account_risk_snapshot TEXT,
    rebuild_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    execution_status VARCHAR(32),
    execution_error_code VARCHAR(64),
    execution_error_message VARCHAR(512),
    trigger_reason_code VARCHAR(64),
    trigger_reason_text VARCHAR(512),
    event_version INTEGER,
    event_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    pre_state VARCHAR(32),
    post_state VARCHAR(32),
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_ai_call_log (
    call_id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64),
    trace_id VARCHAR(128),
    request_id VARCHAR(128),
    provider_name VARCHAR(32) NOT NULL,
    model_name VARCHAR(128),
    ai_role VARCHAR(64) NOT NULL,
    call_status VARCHAR(32) NOT NULL,
    provider_request_id VARCHAR(128),
    started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    latency_ms BIGINT,
    input_tokens BIGINT,
    output_tokens BIGINT,
    total_tokens BIGINT,
    reserved_cost_usd DECIMAL(20, 8) DEFAULT 0,
    calculated_cost_usd DECIMAL(20, 8) DEFAULT 0,
    cost_currency VARCHAR(8) NOT NULL DEFAULT 'USD',
    cost_calculation_method VARCHAR(64) NOT NULL DEFAULT 'TOKEN_RATE_ESTIMATE',
    fallback_flag BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_reason VARCHAR(512),
    rate_limited BOOLEAN NOT NULL DEFAULT FALSE,
    budget_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    timeout_flag BOOLEAN NOT NULL DEFAULT FALSE,
    error_code VARCHAR(128),
    error_message VARCHAR(512),
    request_hash VARCHAR(128),
    request_summary TEXT,
    response_summary TEXT,
    rule_version VARCHAR(32),
    review_only BOOLEAN NOT NULL DEFAULT TRUE,
    manual_review_only BOOLEAN NOT NULL DEFAULT TRUE,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_executable BOOLEAN NOT NULL DEFAULT TRUE,
    not_auto_trading BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    not_user_position_creation BOOLEAN NOT NULL DEFAULT TRUE,
    not_position_mutation BOOLEAN NOT NULL DEFAULT TRUE,
    not_state_machine_override BOOLEAN NOT NULL DEFAULT TRUE,
    not_execution_plan_creation BOOLEAN NOT NULL DEFAULT TRUE,
    rule_direction_preserved BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_ai_call_log_status CHECK (
        call_status IN ('STARTED', 'SUCCESS', 'DISABLED', 'NOT_CONFIGURED',
        'RATE_LIMITED', 'BUDGET_BLOCKED', 'TIMEOUT', 'FAILED', 'INVALID_RESPONSE')
    ),
    CONSTRAINT ck_tm_ai_call_log_safety CHECK (
        review_only = TRUE
        AND manual_review_only = TRUE
        AND not_trade_instruction = TRUE
        AND not_executable = TRUE
        AND not_auto_trading = TRUE
        AND not_order_execution = TRUE
        AND not_user_position_creation = TRUE
        AND not_position_mutation = TRUE
        AND not_state_machine_override = TRUE
        AND not_execution_plan_creation = TRUE
        AND rule_direction_preserved = TRUE
    )
);
