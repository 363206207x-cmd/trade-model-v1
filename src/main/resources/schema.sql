-- V1 核心表结构

CREATE TABLE IF NOT EXISTS tm_analysis_run (
    analysis_id VARCHAR(64) PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10) NOT NULL,
    analysis_time TIMESTAMP NOT NULL,
    rule_version VARCHAR(32),
    data_quality_score INT,
    trace_id VARCHAR(128),
    status VARCHAR(20),
    idempotency_key VARCHAR(128),
    request_id VARCHAR(128),
    trigger_type VARCHAR(64),
    trigger_reference VARCHAR(256),
    parent_analysis_id VARCHAR(64),
    parent_trace_id VARCHAR(128),
    input_snapshot_json CLOB,
    input_snapshot_hash VARCHAR(128),
    attempt_count INT NOT NULL DEFAULT 1,
    lease_owner VARCHAR(128),
    lease_expires_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_code VARCHAR(128),
    error_message VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no INT NOT NULL DEFAULT 1,
    CONSTRAINT ck_tm_analysis_run_status CHECK (
        status IS NULL OR status IN ('STARTED', 'SUCCESS', 'FAILED')
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_analysis_run_idempotency_key
    ON tm_analysis_run(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_tm_analysis_run_trace_id ON tm_analysis_run(trace_id);
CREATE INDEX IF NOT EXISTS idx_tm_analysis_run_request_id ON tm_analysis_run(request_id);
CREATE INDEX IF NOT EXISTS idx_tm_analysis_run_status_lease ON tm_analysis_run(status, lease_expires_at);
CREATE INDEX IF NOT EXISTS idx_tm_analysis_run_trigger_ref ON tm_analysis_run(trigger_type, trigger_reference);

CREATE TABLE IF NOT EXISTS tm_evidence_item (
    evidence_id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    evidence_type VARCHAR(50),
    description TEXT,
    direction VARCHAR(20),
    strength DOUBLE,
    confidence DOUBLE,
    source VARCHAR(100),
    source_provider VARCHAR(100),
    source_reference VARCHAR(512),
    source_trace_id VARCHAR(128),
    external_event_id VARCHAR(64),
    external_event_type VARCHAR(32),
    event_window_start TIMESTAMP,
    event_window_end TIMESTAMP,
    impact_score INT,
    severity VARCHAR(32),
    create_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_score_item (
    score_id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    score_type VARCHAR(50),
    score_value DOUBLE,
    weight DOUBLE,
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
    event_time TIMESTAMP NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    impact_score INT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_reference VARCHAR(512) NOT NULL,
    source_trace_id VARCHAR(128) NOT NULL,
    source_event_id VARCHAR(128),
    source_hash VARCHAR(128),
    source_published_at TIMESTAMP NOT NULL,
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
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_macro_event_dedupe ON tm_macro_event(dedupe_key);
CREATE INDEX IF NOT EXISTS idx_tm_macro_event_window ON tm_macro_event(window_start, window_end);
CREATE INDEX IF NOT EXISTS idx_tm_macro_event_scope ON tm_macro_event(market_scope, status);
CREATE INDEX IF NOT EXISTS idx_tm_macro_event_source_trace ON tm_macro_event(source_trace_id);

CREATE TABLE IF NOT EXISTS tm_news_event (
    event_id VARCHAR(64) PRIMARY KEY,
    headline VARCHAR(256) NOT NULL,
    summary TEXT,
    affected_symbols VARCHAR(512),
    market_scope VARCHAR(64),
    event_time TIMESTAMP NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    impact_score INT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_reference VARCHAR(512) NOT NULL,
    source_trace_id VARCHAR(128) NOT NULL,
    source_event_id VARCHAR(128),
    source_hash VARCHAR(128),
    source_published_at TIMESTAMP NOT NULL,
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
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_news_event_dedupe ON tm_news_event(dedupe_key);
CREATE INDEX IF NOT EXISTS idx_tm_news_event_window ON tm_news_event(window_start, window_end);
CREATE INDEX IF NOT EXISTS idx_tm_news_event_scope ON tm_news_event(market_scope, status);
CREATE INDEX IF NOT EXISTS idx_tm_news_event_source_trace ON tm_news_event(source_trace_id);

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
    valid_from TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    invalid_condition TEXT,
    evidence_summary TEXT,
    explanation_json TEXT,
    review_reasons TEXT,
    ai_conflict_level VARCHAR(64),
    ai_conflict_score INT,
    ai_plan_mode VARCHAR(50),
    confused_score INT,
    asset_state_snapshot VARCHAR(512),
    hot_reset_invalidated BOOLEAN NOT NULL DEFAULT FALSE,
    hot_reset_event_id VARCHAR(64),
    hot_reset_invalidated_at TIMESTAMP,
    hot_reset_reason_code VARCHAR(64),
    create_time TIMESTAMP
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
    revalidation_required_at TIMESTAMP,
    create_time TIMESTAMP,
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
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL UNIQUE,
    symbol VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10) NOT NULL,
    environment_type VARCHAR(50),
    risk_mode VARCHAR(50),
    trend_friendliness INT,
    leverage_suggestion VARCHAR(50),
    range_pct_24h DOUBLE,
    volatility_regime VARCHAR(50),
    last_funding_rate DECIMAL(20, 10),
    perp_funding_applied BOOLEAN,
    last_open_interest DECIMAL(28, 8),
    open_interest_delta DECIMAL(28, 8),
    oi_applied BOOLEAN,
    derivatives_crowding_state VARCHAR(32),
    summary TEXT,
    source_type VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tm_market_env_snapshot_symbol_create_time
    ON tm_market_environment_snapshot(symbol, create_time);
CREATE INDEX IF NOT EXISTS idx_tm_market_env_snapshot_source_type
    ON tm_market_environment_snapshot(source_type);

-- Persisted OHLCV skeleton (BACKEND-P11): local read-model source contract only.
-- This table does not complete RuntimeKlineContext or SourceTrace by itself.
CREATE TABLE IF NOT EXISTS tm_persisted_ohlcv_bar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    source_version INT NOT NULL,
    fetch_time TIMESTAMP,
    source_status VARCHAR(32),
    freshness_status VARCHAR(32),
    provenance_version VARCHAR(32),
    ingestion_run_id VARCHAR(64),
    ingested_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    quality_status VARCHAR(32) NOT NULL,
    quality_reason VARCHAR(512),
    raw_payload_hash VARCHAR(128),
    is_deleted INT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_persisted_ohlcv_bar_source
    ON tm_persisted_ohlcv_bar(symbol, timeframe, open_time_ms, provider, provider_market_type);
CREATE INDEX IF NOT EXISTS idx_tm_persisted_ohlcv_bar_window
    ON tm_persisted_ohlcv_bar(symbol, timeframe, close_time_ms);
CREATE INDEX IF NOT EXISTS idx_tm_persisted_ohlcv_bar_ingested
    ON tm_persisted_ohlcv_bar(symbol, timeframe, ingested_at);
CREATE INDEX IF NOT EXISTS idx_tm_persisted_ohlcv_bar_source_batch
    ON tm_persisted_ohlcv_bar(source_batch_id);
CREATE INDEX IF NOT EXISTS idx_tm_persisted_ohlcv_bar_source_trace
    ON tm_persisted_ohlcv_bar(source_trace_id);
CREATE INDEX IF NOT EXISTS idx_tm_persisted_ohlcv_bar_ingestion_run
    ON tm_persisted_ohlcv_bar(ingestion_run_id);

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
    cooldown_minutes INT DEFAULT 15,
    scan_base_profile VARCHAR(16),
    scan_position_profile VARCHAR(16),
    scan_pool_profile VARCHAR(16),
    scan_auto_escalation_enabled BOOLEAN,
    scan_manual_override_until TIMESTAMP,
    scan_update_reason VARCHAR(512),
    scan_updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tm_real_position (
    position_id VARCHAR(64) PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) DEFAULT 'UNKNOWN',
    source_name VARCHAR(50),
    position_side VARCHAR(10),
    avg_open_price DECIMAL(20, 8),
    position_open_time TIMESTAMP,
    position_quantity DECIMAL(20, 8),
    unrealized_pnl_pct DECIMAL(10, 4),
    position_status VARCHAR(20) DEFAULT 'OPEN',
    mark_price DECIMAL(20, 8),
    break_even_price DECIMAL(20, 8),
    liquidation_price DECIMAL(20, 8),
    update_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tm_real_position_symbol_status ON tm_real_position(symbol, position_status);

CREATE TABLE IF NOT EXISTS tm_user_position (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_symbol VARCHAR(32) NOT NULL,
    side VARCHAR(10) NOT NULL,
    status VARCHAR(32) NOT NULL,
    entry_price DECIMAL(20, 8) NOT NULL,
    quantity DECIMAL(28, 8) NOT NULL,
    leverage DECIMAL(20, 8) NOT NULL,
    stop_loss DECIMAL(20, 8),
    take_profit DECIMAL(20, 8),
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    close_price DECIMAL(20, 8),
    close_reason VARCHAR(512),
    source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    source_ref_id VARCHAR(128),
    manual_review_required BOOLEAN NOT NULL DEFAULT TRUE,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_auto_trading BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    not_position_sync BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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

CREATE INDEX IF NOT EXISTS idx_tm_user_position_status_opened_at
    ON tm_user_position(status, opened_at);
CREATE INDEX IF NOT EXISTS idx_tm_user_position_asset_status
    ON tm_user_position(asset_symbol, status);

CREATE TABLE IF NOT EXISTS tm_position_monitor_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_position_monitor_log_price CHECK (current_price > 0),
    CONSTRAINT ck_tm_position_monitor_log_logic_status CHECK (
        logic_status IN ('LOGIC_VALID', 'LOGIC_WEAKENED', 'PLAN_INVALIDATED', 'HIGH_RISK')
    ),
    CONSTRAINT ck_tm_position_monitor_log_suggested_action CHECK (
        suggested_action IN ('HOLD', 'MANUAL_REVIEW', 'RECHECK_PLAN', 'RISK_REVIEW')
    )
);

CREATE INDEX IF NOT EXISTS idx_tm_position_monitor_log_position_created
    ON tm_position_monitor_log(position_id, created_at);
CREATE INDEX IF NOT EXISTS idx_tm_position_monitor_log_analysis_created
    ON tm_position_monitor_log(analysis_id, created_at);

-- Push 快照 / 二次校验（与 tm_analysis_run.analysis_id 类型一致：VARCHAR(64)）
-- 下列 CLOB JSON 列由 /api/review/aggregate 原样透出至复盘页文本展示，应用层不对其做解析或折叠 UI。
CREATE TABLE IF NOT EXISTS tm_push_snapshot (
    push_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(20),
    timeframe VARCHAR(10),
    push_type VARCHAR(50),
    push_status VARCHAR(30),
    push_create_time TIMESTAMP,
    rule_version VARCHAR(20),
    trigger_price DECIMAL(20, 8),
    entry_zone_json CLOB,
    stop_zone_json CLOB,
    invalidation_condition_json CLOB,
    plan_mode_snapshot VARCHAR(100),
    cause_effect_alignment_snapshot VARCHAR(200),
    execution_feasibility_snapshot INT,
    data_quality_score_snapshot INT,
    confused_score_snapshot INT,
    account_risk_snapshot_id BIGINT,
    expires_at TIMESTAMP,
    trace_id VARCHAR(64),
    create_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tm_push_snapshot_analysis_id ON tm_push_snapshot(analysis_id);

-- 账户风险快照（第一轮最小真值链）：由 PushSnapshotService 在推送快照时同步写入；
-- risk_allowed 为 current_account_risk_allowed 的直接真值来源，source_note 记录判定来源口径。
CREATE TABLE IF NOT EXISTS tm_account_risk_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(20),
    risk_level_snapshot VARCHAR(32),
    risk_allowed BOOLEAN NOT NULL,
    risk_reason_code VARCHAR(64),
    risk_reason_text VARCHAR(512),
    position_exposure DECIMAL(10, 4),
    max_allowed_exposure DECIMAL(10, 4),
    snapshot_source VARCHAR(128),
    snapshot_version INT,
    source_note VARCHAR(128),
    trace_id VARCHAR(64),
    create_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tm_account_risk_snapshot_analysis_id ON tm_account_risk_snapshot(analysis_id);

-- fail_reason_json：复盘聚合原样透传（与上表 JSON 列展示策略一致）。
-- current_slippage_estimation：Recheck 时基于 current_price 与 trigger_price 的当次估算。
-- current_account_risk_allowed：第一轮来源为 tm_account_risk_snapshot.risk_allowed（按 push.account_risk_snapshot_id 回查）。
CREATE TABLE IF NOT EXISTS tm_push_recheck_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    push_id BIGINT NOT NULL,
    dispatch_batch_id VARCHAR(64),
    dispatch_instruction_id VARCHAR(64),
    trigger_source VARCHAR(32),
    retry_attempt INT,
    max_attempts INT,
    retry_backoff_minutes INT,
    replay_from_log_id BIGINT,
    execution_status VARCHAR(32),
    execution_error_code VARCHAR(64),
    execution_error_message VARCHAR(512),
    recheck_time TIMESTAMP,
    recheck_status VARCHAR(30),
    current_price DECIMAL(20, 8),
    price_drift_ratio DECIMAL(20, 8),
    current_slippage_estimation DECIMAL(20, 8),
    current_data_quality_score INT,
    current_confused_score INT,
    current_account_risk_allowed BOOLEAN,
    fail_reason_json CLOB,
    trace_id VARCHAR(64),
    create_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tm_push_recheck_log_push_id ON tm_push_recheck_log(push_id);
CREATE INDEX IF NOT EXISTS idx_tm_push_recheck_log_dispatch_batch_id ON tm_push_recheck_log(dispatch_batch_id);
CREATE INDEX IF NOT EXISTS idx_tm_push_recheck_log_dispatch_instruction_id ON tm_push_recheck_log(dispatch_instruction_id);

-- 调度配置持久化：替换内存态参数源，支持实例重启后恢复。
CREATE TABLE IF NOT EXISTS tm_push_recheck_dispatch_config (
    config_key VARCHAR(64) PRIMARY KEY,
    config_value INT NOT NULL,
    updated_by VARCHAR(64),
    update_source VARCHAR(64),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 调度配置变更审计：记录每次配置修改的前后值。
CREATE TABLE IF NOT EXISTS tm_push_recheck_dispatch_config_audit (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL,
    old_value INT,
    new_value INT NOT NULL,
    changed_by VARCHAR(64),
    change_source VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tm_push_recheck_dispatch_config_audit_key_time
    ON tm_push_recheck_dispatch_config_audit(config_key, create_time);

-- 监控告警（与 MonitorAlertDO 对齐；冷却/抑制字段按 PROJECT_SPEC 一并落表，避免短期二次改表）
CREATE TABLE IF NOT EXISTS tm_monitor_alert (
    id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64),
    asset_symbol VARCHAR(20),
    alert_type VARCHAR(50),
    alert_level VARCHAR(20),
    alert_message TEXT,
    status VARCHAR(30),
    cooldown_until TIMESTAMP,
    suppress_reason VARCHAR(512),
    trace_id VARCHAR(64),
    rule_version VARCHAR(20),
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT NOT NULL DEFAULT 0,
    version_no INT DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_tm_monitor_alert_list ON tm_monitor_alert(is_deleted, created_at);

-- Opportunity Log（机会日志）：P1-4 权威机会结果所有者。
-- 仅记录分析后机会候选和只读结果分类，不代表交易指令，不创建或修改用户持仓。
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
    anchor_time TIMESTAMP NOT NULL,
    evaluation_as_of TIMESTAMP,
    resolved_at TIMESTAMP,
    entry_reference DECIMAL(20, 8),
    target_price DECIMAL(20, 8),
    invalidation_price DECIMAL(20, 8),
    target_hit BOOLEAN NOT NULL DEFAULT FALSE,
    invalidation_hit BOOLEAN NOT NULL DEFAULT FALSE,
    target_hit_at TIMESTAMP,
    invalidation_hit_at TIMESTAMP,
    hit_order VARCHAR(32),
    mfe_price DECIMAL(20, 8),
    mfe_ratio DECIMAL(20, 10),
    mae_price DECIMAL(20, 8),
    mae_ratio DECIMAL(20, 10),
    push_present BOOLEAN NOT NULL DEFAULT FALSE,
    risk_blocked_evidence BOOLEAN NOT NULL DEFAULT FALSE,
    risk_blocked_at TIMESTAMP,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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

CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_analysis_id ON tm_opportunity_log(analysis_id);
CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_decision_id ON tm_opportunity_log(decision_id);
CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_execution_plan_id ON tm_opportunity_log(execution_plan_id);
CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_symbol_anchor ON tm_opportunity_log(symbol, anchor_time);
CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_status_resolved ON tm_opportunity_log(opportunity_status, resolved_at);
CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_user_position_id ON tm_opportunity_log(user_position_id);
CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_push_id ON tm_opportunity_log(push_id);

-- Missed Opportunity（窄表；主链仅在 hotResetWouldFire=false 时落库；dashboard 仅消费当日计数）
CREATE TABLE IF NOT EXISTS tm_missed_opportunity (
    missed_id VARCHAR(64) PRIMARY KEY,
    decision_id VARCHAR(64) NOT NULL,
    analysis_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(20),
    biz_date DATE NOT NULL,
    reason_json CLOB,
    rule_version VARCHAR(20),
    trace_id VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tm_missed_opportunity_biz_date ON tm_missed_opportunity(biz_date);
CREATE INDEX IF NOT EXISTS idx_tm_missed_opportunity_decision_id ON tm_missed_opportunity(decision_id);
CREATE INDEX IF NOT EXISTS idx_tm_missed_opportunity_symbol ON tm_missed_opportunity(symbol);

-- 复盘结果（按 analysis_id 单行 upsert；用户经 /api/review/save 写入，与聚合摘要分离）
CREATE TABLE IF NOT EXISTS tm_review_result (
    id VARCHAR(64) PRIMARY KEY,
    analysis_id VARCHAR(64) NOT NULL,
    error_type VARCHAR(200),
    actual_outcome VARCHAR(2000),
    adjustment_suggestion VARCHAR(4000),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_review_result_analysis_id ON tm_review_result(analysis_id);

-- 规则版本演进审计链（最小链路）：用于挂载人工复盘反馈的版本追踪信息
-- 目标：确保 tm_review_result 保存后，审计链不停摆（即使 rule_version / error_type 为空也可落库）
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT NOT NULL DEFAULT 0,
    version_no INT DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_tm_rule_version_log_analysis_time ON tm_rule_version_log(analysis_id, created_at);
CREATE INDEX IF NOT EXISTS idx_tm_rule_version_log_rule_version_time ON tm_rule_version_log(rule_version, created_at);
CREATE INDEX IF NOT EXISTS idx_tm_rule_version_log_operator_time ON tm_rule_version_log(operator, created_at);
CREATE INDEX IF NOT EXISTS idx_tm_rule_version_log_rollback_time ON tm_rule_version_log(rollback_flag, created_at);

CREATE TABLE IF NOT EXISTS tm_asset_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL UNIQUE,
    state VARCHAR(32),
    confused_score INT,
    confused_low_streak INT NOT NULL DEFAULT 0,
    hot_reset_flag BOOLEAN DEFAULT FALSE,
    hot_reset_trigger_type VARCHAR(64),
    hot_reset_trigger_value VARCHAR(128),
    hot_reset_time TIMESTAMP,
    pre_reset_state VARCHAR(32),
    post_reset_state VARCHAR(32),
    last_update_time TIMESTAMP NOT NULL,
    trace_id VARCHAR(64)
);

-- Hot Reset 事件流水（第二轮最小语义）：
-- trigger_type 固定表示事件类别（如 HOT_RESET），trigger_reason_code 表示触发原因码（如 CONFUSED_HIGH_MTF_MISALIGNED）。
-- 两者语义不同，避免混用；本表仍仅服务 Hot Reset，不扩展为通用事件平台。
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
    severity_score INT,
    decision_id VARCHAR(64),
    decision_state VARCHAR(32),
    decision_invalidated_count INT NOT NULL DEFAULT 0,
    plan_revalidation_count INT NOT NULL DEFAULT 0,
    push_invalidated_count INT NOT NULL DEFAULT 0,
    confused_score_snapshot INT,
    confused_score_before INT,
    confused_score_after INT,
    multi_timeframe_aligned_snapshot BOOLEAN,
    account_risk_status VARCHAR(64),
    account_risk_level VARCHAR(32),
    account_risk_blocked BOOLEAN,
    account_risk_snapshot CLOB,
    rebuild_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    execution_status VARCHAR(32),
    execution_error_code VARCHAR(64),
    execution_error_message VARCHAR(512),
    trigger_reason_code VARCHAR(64),
    trigger_reason_text VARCHAR(512),
    event_version INT,
    event_time TIMESTAMP NOT NULL,
    pre_state VARCHAR(32),
    post_state VARCHAR(32),
    completed_at TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tm_hot_reset_event_analysis_id ON tm_hot_reset_event(analysis_id);
CREATE INDEX IF NOT EXISTS idx_tm_hot_reset_event_trace_id ON tm_hot_reset_event(trace_id);
CREATE INDEX IF NOT EXISTS idx_tm_hot_reset_event_symbol_event_time ON tm_hot_reset_event(symbol, event_time);

-- AI Call Log（P2-2）：只读 AI 复核调用审计链。
-- 本表仅记录 provider review-only 调用、fallback、token/cost/latency 与安全边界；不保存原始密钥、原始 prompt 或可执行交易 payload。
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
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
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
    request_summary CLOB,
    response_summary CLOB,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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

CREATE INDEX IF NOT EXISTS idx_tm_ai_call_log_analysis_id ON tm_ai_call_log(analysis_id);
CREATE INDEX IF NOT EXISTS idx_tm_ai_call_log_trace_id ON tm_ai_call_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_tm_ai_call_log_provider_time ON tm_ai_call_log(provider_name, started_at);
CREATE INDEX IF NOT EXISTS idx_tm_ai_call_log_status_time ON tm_ai_call_log(call_status, started_at);

CREATE TABLE IF NOT EXISTS tm_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    CONSTRAINT uq_tm_user_username UNIQUE (username)
);

-- tm_asset_state 语义：每个 symbol 当前仅一行（会被后续分析覆盖更新）。
MERGE INTO tm_rule_config KEY(rule_key) VALUES
('cfg-confused-enter-threshold', 'confused_state_config', 'confused_state_config.enter_threshold', '70', 'Confused state enter threshold', 'v1.0', TRUE),
('cfg-confused-push-block-threshold', 'confused_state_config', 'confused_state_config.directional_push_block_threshold', '85', 'Directional push block threshold for Confused state', 'v1.0', TRUE),
('cfg-confused-exit-threshold', 'confused_state_config', 'confused_state_config.exit_threshold_exclusive', '55', 'Confused state exit threshold exclusive', 'v1.0', TRUE),
('cfg-confused-exit-cycles', 'confused_state_config', 'confused_state_config.exit_required_consecutive_cycles', '2', 'Required consecutive low cycles before Confused exit', 'v1.0', TRUE),
('cfg-ai-conflict-level1-max', 'ai_conflict_config', 'ai_conflict_config.level1_max_score', '20', 'AI conflict level 1 max score', 'v1.0', TRUE),
('cfg-ai-conflict-level2-max', 'ai_conflict_config', 'ai_conflict_config.level2_max_score', '45', 'AI conflict level 2 max score', 'v1.0', TRUE),
('cfg-ai-conflict-level3-max', 'ai_conflict_config', 'ai_conflict_config.level3_max_score', '70', 'AI conflict level 3 max score', 'v1.0', TRUE),
('cfg-ai-conflict-single-objection-max', 'ai_conflict_config', 'ai_conflict_config.single_objection_max_score', '35', 'Single AI objection score cap', 'v1.0', TRUE),
('cfg-push-recheck-drift-ratio', 'push_recheck_config', 'push_recheck_config.drift_ratio_threshold', '0.02', 'Push recheck drift ratio threshold', 'v1.0', TRUE),
('cfg-push-recheck-confused-wait', 'push_recheck_config', 'push_recheck_config.confused_wait_threshold', '70', 'Push recheck Confused wait threshold', 'v1.0', TRUE),
('cfg-push-recheck-confused-block', 'push_recheck_config', 'push_recheck_config.confused_block_threshold', '85', 'Push recheck Confused block threshold', 'v1.0', TRUE),
('cfg-push-recheck-exec-feas-wait', 'push_recheck_config', 'push_recheck_config.execution_feasibility_wait_threshold', '60', 'Push recheck execution feasibility wait threshold', 'v1.0', TRUE),
('cfg-missed-review-window-hours', 'missed_opportunity_config', 'missed_opportunity_config.review_window_hours', '24', 'Missed opportunity review window in hours', 'v1.0', TRUE),
('cfg-missed-min-mfe-ratio', 'missed_opportunity_config', 'missed_opportunity_config.min_mfe_ratio_threshold', '0.01', 'Missed opportunity minimum MFE ratio', 'v1.0', TRUE),
('cfg-missed-max-mae-ratio', 'missed_opportunity_config', 'missed_opportunity_config.max_mae_ratio_threshold', '0.02', 'Missed opportunity maximum MAE ratio', 'v1.0', TRUE),
('cfg-hot-reset-price-move', 'hot_reset_config', 'hot_reset_config.extreme_price_move_ratio_threshold', '0.08', 'Hot Reset extreme price move ratio threshold', 'v1.0', TRUE),
('cfg-hot-reset-oi-collapse', 'hot_reset_config', 'hot_reset_config.oi_collapse_change_ratio_threshold', '-0.30', 'Hot Reset open-interest collapse threshold', 'v1.0', TRUE),
('cfg-hot-reset-liquidity-drain', 'hot_reset_config', 'hot_reset_config.liquidity_drain_change_ratio_threshold', '-0.40', 'Hot Reset liquidity drain threshold', 'v1.0', TRUE),
('cfg-hot-reset-systemic-severity', 'hot_reset_config', 'hot_reset_config.systemic_shock_severity_threshold', '85', 'Hot Reset systemic shock severity threshold', 'v1.0', TRUE);

MERGE INTO tm_rule_config KEY(rule_key) VALUES
('cfg-provider-scan-emergency-price', 'provider_scan_profile_config', 'provider.scan.emergency_price_movement_1m', '0.05', 'Emergency 1m price movement threshold', 'v1.0', TRUE),
('cfg-provider-scan-emergency-liquidation', 'provider_scan_profile_config', 'provider.scan.emergency_liquidation_spike', '90', 'Emergency liquidation spike score threshold', 'v1.0', TRUE),
('cfg-provider-scan-emergency-confused', 'provider_scan_profile_config', 'provider.scan.emergency_confused_score', '85', 'Emergency confused score threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-price', 'provider_scan_profile_config', 'provider.scan.high_price_movement_1m', '0.02', 'High 1m price movement threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-atr', 'provider_scan_profile_config', 'provider.scan.high_atr_multiple_5m', '2.0', 'High 5m ATR multiple threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-volume', 'provider_scan_profile_config', 'provider.scan.high_volume_spike', '2.5', 'High volume spike threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-spread', 'provider_scan_profile_config', 'provider.scan.high_spread_spike', '2.0', 'High spread spike threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-oi', 'provider_scan_profile_config', 'provider.scan.high_open_interest_change', '0.10', 'High open interest change threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-funding', 'provider_scan_profile_config', 'provider.scan.high_funding_extremity', '80', 'High funding extremity threshold', 'v1.0', TRUE),
('cfg-provider-scan-near-boundary', 'provider_scan_profile_config', 'provider.scan.near_boundary_distance', '0.01', 'Near stop or target distance threshold', 'v1.0', TRUE),
('cfg-provider-scan-data-quality', 'provider_scan_profile_config', 'provider.scan.data_quality_deterioration_score', '60', 'Data quality deterioration threshold', 'v1.0', TRUE),
('cfg-provider-scan-standard-confused', 'provider_scan_profile_config', 'provider.scan.standard_confused_score', '55', 'Standard profile confused score threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-hold', 'provider_scan_profile_config', 'provider.scan.high_min_hold_seconds', '300', 'High profile minimum hold seconds', 'v1.0', TRUE),
('cfg-provider-scan-emergency-hold', 'provider_scan_profile_config', 'provider.scan.emergency_min_hold_seconds', '120', 'Emergency profile minimum hold seconds', 'v1.0', TRUE),
('cfg-provider-scan-recovery-cycles', 'provider_scan_profile_config', 'provider.scan.recovery_confirm_cycles', '2', 'Recovery cycles before downgrade', 'v1.0', TRUE),
('cfg-provider-scan-downgrade-cooldown', 'provider_scan_profile_config', 'provider.scan.downgrade_cooldown_seconds', '300', 'Profile downgrade cooldown seconds', 'v1.0', TRUE);

MERGE INTO tm_rule_config KEY(rule_key) VALUES
('cfg-deriv-oi-5m-weak', 'derivatives_evidence_config', 'derivatives_evidence_config.oi_change_5m_weak', '0.02', '5m OI weak-change threshold', 'v1.0', TRUE),
('cfg-deriv-oi-5m-strong', 'derivatives_evidence_config', 'derivatives_evidence_config.oi_change_5m_strong', '0.05', '5m OI strong-change threshold', 'v1.0', TRUE),
('cfg-deriv-oi-15m-weak', 'derivatives_evidence_config', 'derivatives_evidence_config.oi_change_15m_weak', '0.04', '15m OI weak-change threshold', 'v1.0', TRUE),
('cfg-deriv-oi-15m-strong', 'derivatives_evidence_config', 'derivatives_evidence_config.oi_change_15m_strong', '0.10', '15m OI strong-change threshold', 'v1.0', TRUE),
('cfg-deriv-funding-positive', 'derivatives_evidence_config', 'derivatives_evidence_config.funding_positive_extreme', '0.0005', 'Positive funding extreme threshold', 'v1.0', TRUE),
('cfg-deriv-funding-negative', 'derivatives_evidence_config', 'derivatives_evidence_config.funding_negative_extreme', '-0.0005', 'Negative funding extreme threshold', 'v1.0', TRUE),
('cfg-deriv-long-crowding', 'derivatives_evidence_config', 'derivatives_evidence_config.long_short_long_crowding', '1.20', 'Long crowding ratio threshold', 'v1.0', TRUE),
('cfg-deriv-short-crowding', 'derivatives_evidence_config', 'derivatives_evidence_config.long_short_short_crowding', '0.80', 'Short crowding ratio threshold', 'v1.0', TRUE),
('cfg-deriv-liquidation-5m', 'derivatives_evidence_config', 'derivatives_evidence_config.liquidation_spike_5m', '1000000', '5m liquidation spike USD threshold', 'v1.0', TRUE),
('cfg-deriv-liquidation-15m', 'derivatives_evidence_config', 'derivatives_evidence_config.liquidation_spike_15m', '3000000', '15m liquidation spike USD threshold', 'v1.0', TRUE),
('cfg-deriv-liquidation-imbalance', 'derivatives_evidence_config', 'derivatives_evidence_config.liquidation_imbalance_ratio', '2.0', 'Liquidation imbalance ratio threshold', 'v1.0', TRUE),
('cfg-deriv-exchange-concentration', 'derivatives_evidence_config', 'derivatives_evidence_config.exchange_concentration_high', '0.70', 'Exchange concentration high ratio threshold (0.70 = 70%)', 'v1.0', TRUE),
('cfg-deriv-max-age', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_max_data_age_seconds', '120', 'Maximum derivatives data age in seconds', 'v1.0', TRUE),
('cfg-deriv-required-confirm', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_required_for_confirm', 'true', 'Require OI and Funding for confirm paths', 'v1.0', TRUE),
('cfg-deriv-min-datasets', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_minimum_dataset_count', '2', 'Minimum derivatives dataset count', 'v1.0', TRUE),
('cfg-deriv-partial-penalty', 'derivatives_score_config', 'derivatives_score_config.derivatives_partial_confidence_penalty', '15', 'Partial derivatives confidence penalty', 'v1.0', TRUE),
('cfg-deriv-stale-penalty', 'derivatives_score_config', 'derivatives_score_config.derivatives_stale_confidence_penalty', '30', 'Stale derivatives confidence penalty', 'v1.0', TRUE),
('cfg-deriv-score-cap', 'derivatives_score_config', 'derivatives_score_config.derivatives_score_cap', '20', 'Maximum absolute derivatives contribution for non-trend scores', 'v1.0', TRUE),
('cfg-deriv-trend-score-cap', 'derivatives_score_config', 'derivatives_score_config.derivatives_trend_score_cap', '5', 'Maximum absolute derivatives contribution for trend score', 'v1.0', TRUE),
('cfg-deriv-min-data-quality', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_min_data_quality_score', '60', 'Minimum data quality for derivatives-confirmed opportunity', 'v1.0', TRUE),
('cfg-deriv-eight-score-cap', 'derivatives_decision_config', 'derivatives_decision_config.eight_score_adjustment_cap', '10', 'Maximum absolute eight-score adjustment to rule decision score', 'v1.0', TRUE),
('cfg-deriv-eight-score-factor', 'derivatives_decision_config', 'derivatives_decision_config.eight_score_adjustment_factor_percent', '20', 'Eight-score decision adjustment factor percent', 'v1.0', TRUE),
('cfg-deriv-high-risk-downgrade', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_high_risk_plan_downgrade', 'true', 'Downgrade plan mode under high derivatives risk', 'v1.0', TRUE),
('cfg-deriv-monitor-refresh', 'derivatives_monitor_config', 'derivatives_monitor_config.refresh_seconds', '60', 'Cached derivatives monitor cadence', 'v1.0', TRUE);

-- hot_reset_* / pre_reset_state / post_reset_state 记录的是该行最近一次 Hot Reset 元数据，
-- 不是按 analysis_id 归档的事件流水；review 仅做当前行解释展示。
