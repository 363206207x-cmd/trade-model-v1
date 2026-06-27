-- Trade Model V1 PostgreSQL baseline schema indexes.
-- Generated from current V1 index semantics for PDR-2C1.
-- Local and test bootstrap remains src/main/resources/schema.sql.
-- Production readiness remains blocked until validation packages complete.

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_analysis_run_idempotency_key
    ON tm_analysis_run(idempotency_key);

CREATE INDEX IF NOT EXISTS idx_tm_analysis_run_trace_id ON tm_analysis_run(trace_id);

CREATE INDEX IF NOT EXISTS idx_tm_analysis_run_request_id ON tm_analysis_run(request_id);

CREATE INDEX IF NOT EXISTS idx_tm_analysis_run_status_lease ON tm_analysis_run(status, lease_expires_at);

CREATE INDEX IF NOT EXISTS idx_tm_analysis_run_trigger_ref ON tm_analysis_run(trigger_type, trigger_reference);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_macro_event_dedupe ON tm_macro_event(dedupe_key);

CREATE INDEX IF NOT EXISTS idx_tm_macro_event_window ON tm_macro_event(window_start, window_end);

CREATE INDEX IF NOT EXISTS idx_tm_macro_event_scope ON tm_macro_event(market_scope, status);

CREATE INDEX IF NOT EXISTS idx_tm_macro_event_source_trace ON tm_macro_event(source_trace_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_news_event_dedupe ON tm_news_event(dedupe_key);

CREATE INDEX IF NOT EXISTS idx_tm_news_event_window ON tm_news_event(window_start, window_end);

CREATE INDEX IF NOT EXISTS idx_tm_news_event_scope ON tm_news_event(market_scope, status);

CREATE INDEX IF NOT EXISTS idx_tm_news_event_source_trace ON tm_news_event(source_trace_id);

CREATE INDEX IF NOT EXISTS idx_tm_market_env_snapshot_symbol_create_time
    ON tm_market_environment_snapshot(symbol, create_time);

CREATE INDEX IF NOT EXISTS idx_tm_market_env_snapshot_source_type
    ON tm_market_environment_snapshot(source_type);

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

CREATE INDEX IF NOT EXISTS idx_tm_real_position_symbol_status ON tm_real_position(symbol, position_status);

CREATE INDEX IF NOT EXISTS idx_tm_user_position_status_opened_at
    ON tm_user_position(status, opened_at);

CREATE INDEX IF NOT EXISTS idx_tm_user_position_asset_status
    ON tm_user_position(asset_symbol, status);

CREATE INDEX IF NOT EXISTS idx_tm_position_monitor_log_position_created
    ON tm_position_monitor_log(position_id, created_at);

CREATE INDEX IF NOT EXISTS idx_tm_position_monitor_log_analysis_created
    ON tm_position_monitor_log(analysis_id, created_at);

CREATE INDEX IF NOT EXISTS idx_tm_push_snapshot_analysis_id ON tm_push_snapshot(analysis_id);

CREATE INDEX IF NOT EXISTS idx_tm_account_risk_snapshot_analysis_id ON tm_account_risk_snapshot(analysis_id);

CREATE INDEX IF NOT EXISTS idx_tm_push_recheck_log_push_id ON tm_push_recheck_log(push_id);

CREATE INDEX IF NOT EXISTS idx_tm_push_recheck_log_dispatch_batch_id ON tm_push_recheck_log(dispatch_batch_id);

CREATE INDEX IF NOT EXISTS idx_tm_push_recheck_log_dispatch_instruction_id ON tm_push_recheck_log(dispatch_instruction_id);

CREATE INDEX IF NOT EXISTS idx_tm_push_recheck_dispatch_config_audit_key_time
    ON tm_push_recheck_dispatch_config_audit(config_key, create_time);

CREATE INDEX IF NOT EXISTS idx_tm_monitor_alert_list ON tm_monitor_alert(is_deleted, created_at);

CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_analysis_id ON tm_opportunity_log(analysis_id);

CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_decision_id ON tm_opportunity_log(decision_id);

CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_execution_plan_id ON tm_opportunity_log(execution_plan_id);

CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_symbol_anchor ON tm_opportunity_log(symbol, anchor_time);

CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_status_resolved ON tm_opportunity_log(opportunity_status, resolved_at);

CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_user_position_id ON tm_opportunity_log(user_position_id);

CREATE INDEX IF NOT EXISTS idx_tm_opportunity_log_push_id ON tm_opportunity_log(push_id);

CREATE INDEX IF NOT EXISTS idx_tm_missed_opportunity_biz_date ON tm_missed_opportunity(biz_date);

CREATE INDEX IF NOT EXISTS idx_tm_missed_opportunity_decision_id ON tm_missed_opportunity(decision_id);

CREATE INDEX IF NOT EXISTS idx_tm_missed_opportunity_symbol ON tm_missed_opportunity(symbol);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_review_result_analysis_id ON tm_review_result(analysis_id);

CREATE INDEX IF NOT EXISTS idx_tm_rule_version_log_analysis_time ON tm_rule_version_log(analysis_id, created_at);

CREATE INDEX IF NOT EXISTS idx_tm_rule_version_log_rule_version_time ON tm_rule_version_log(rule_version, created_at);

CREATE INDEX IF NOT EXISTS idx_tm_rule_version_log_operator_time ON tm_rule_version_log(operator, created_at);

CREATE INDEX IF NOT EXISTS idx_tm_rule_version_log_rollback_time ON tm_rule_version_log(rollback_flag, created_at);

CREATE INDEX IF NOT EXISTS idx_tm_hot_reset_event_analysis_id ON tm_hot_reset_event(analysis_id);

CREATE INDEX IF NOT EXISTS idx_tm_hot_reset_event_trace_id ON tm_hot_reset_event(trace_id);

CREATE INDEX IF NOT EXISTS idx_tm_hot_reset_event_symbol_event_time ON tm_hot_reset_event(symbol, event_time);

CREATE INDEX IF NOT EXISTS idx_tm_ai_call_log_analysis_id ON tm_ai_call_log(analysis_id);

CREATE INDEX IF NOT EXISTS idx_tm_ai_call_log_trace_id ON tm_ai_call_log(trace_id);

CREATE INDEX IF NOT EXISTS idx_tm_ai_call_log_provider_time ON tm_ai_call_log(provider_name, started_at);

CREATE INDEX IF NOT EXISTS idx_tm_ai_call_log_status_time ON tm_ai_call_log(call_status, started_at);
