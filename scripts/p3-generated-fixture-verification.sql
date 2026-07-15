\set ON_ERROR_STOP on
\pset tuples_only on
\pset format unaligned
\pset fieldsep '|'

BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '120s';
SET LOCAL lock_timeout = '5s';

SELECT 'FIXTURE_SEED', '20260715';
SELECT 'FLYWAY_VERSION', COALESCE(MAX(version::integer), 0)
FROM flyway_schema_history WHERE success;
SELECT 'FLYWAY_SUCCESS_COUNT', COUNT(*) FROM flyway_schema_history WHERE success;
SELECT 'V7_VALIDITY_COLUMN_COUNT', COUNT(*)
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'tm_decision_result'
  AND column_name IN ('valid_from', 'expires_at');

SELECT 'ANALYSIS_TOTAL', COUNT(*) FROM tm_analysis_run;
SELECT 'ANALYSIS_SUCCESS', COUNT(*) FROM tm_analysis_run WHERE status = 'SUCCESS';
SELECT 'ANALYSIS_FAILED', COUNT(*) FROM tm_analysis_run WHERE status = 'FAILED';
SELECT 'ANALYSIS_STARTED', COUNT(*) FROM tm_analysis_run WHERE status = 'STARTED';
SELECT 'PRIMARY_ASSET_COUNT', COUNT(DISTINCT symbol)
FROM tm_analysis_run
WHERE symbol IN ('BTCUSDT','ETHUSDT','SOLUSDT','BNBUSDT','XRPUSDT','DOGEUSDT');
SELECT 'DECISION_TOTAL', COUNT(*) FROM tm_decision_result;
SELECT 'EXECUTION_PLAN_TOTAL', COUNT(*) FROM tm_execution_plan;
SELECT 'EXECUTION_PLAN_UNSAFE', COUNT(*) FROM tm_execution_plan
WHERE NOT manual_review_required OR NOT not_trade_instruction OR NOT not_executable
   OR NOT not_auto_trading OR NOT not_order_execution OR NOT not_user_position_creation;
SELECT 'EXECUTION_PLAN_SIBLING_COUNT', COUNT(*)
FROM tm_execution_plan WHERE analysis_id = 'P3A-BTCUSDT-001';
SELECT 'ASSET_STATE_TOTAL', COUNT(*) FROM tm_asset_state;
SELECT 'ASSET_STATE_DISTINCT_STATUS', COUNT(DISTINCT state) FROM tm_asset_state;
SELECT 'USER_POSITION_TOTAL', COUNT(*) FROM tm_user_position;
SELECT 'USER_POSITION_UNSAFE', COUNT(*) FROM tm_user_position
WHERE source_type <> 'MANUAL' OR NOT manual_review_required OR NOT not_trade_instruction
   OR NOT not_auto_trading OR NOT not_order_execution OR NOT not_position_sync;
SELECT 'OPEN_BTC_POSITION_COUNT', COUNT(*) FROM tm_user_position
WHERE asset_symbol = 'BTCUSDT' AND status IN ('OPEN', 'PARTIALLY_CLOSED');
SELECT 'POSITION_MONITOR_TOTAL', COUNT(*) FROM tm_position_monitor_log;
SELECT 'POSITION_MONITOR_REVALIDATION_REASON', COUNT(*)
FROM tm_position_monitor_log
WHERE evidence_snapshot::jsonb -> 'reasonCodes' ? 'PLAN_REVALIDATION_REQUIRED';
SELECT 'POSITION_MONITOR_BOUNDARY_REASON', COUNT(*)
FROM tm_position_monitor_log
WHERE evidence_snapshot::jsonb -> 'reasonCodes' ? 'PLAN_BOUNDARY_INCOMPLETE';
SELECT 'POSITION_MONITOR_SIBLING_B_REFERENCE', COUNT(*)
FROM tm_position_monitor_log
WHERE execution_plan_id = 'P3P-BTCUSDT-001-B';
SELECT 'POSITION_MONITOR_UNVERIFIED_SOURCE', COUNT(*)
FROM tm_position_monitor_log
WHERE analysis_id = 'POSITION_SOURCE_UNVERIFIED' AND execution_plan_id IS NULL;
SELECT 'MONITOR_ALERT_TOTAL', COUNT(*) FROM tm_monitor_alert;
SELECT 'PUSH_SNAPSHOT_TOTAL', COUNT(*) FROM tm_push_snapshot;
SELECT 'PUSH_RECHECK_TOTAL', COUNT(*) FROM tm_push_recheck_log;
SELECT 'HOT_RESET_TOTAL', COUNT(*) FROM tm_hot_reset_event;
SELECT 'HOT_RESET_TIME_ORDER_INVALID', COUNT(*) FROM tm_hot_reset_event
WHERE completed_at IS NOT NULL AND (completed_at < event_time OR create_time > completed_at);
SELECT 'AI_CALL_LOG_TOTAL', COUNT(*) FROM tm_ai_call_log;
SELECT 'AI_REQUIRED_STATE_MARKER_COUNT', COUNT(DISTINCT marker)
FROM (
    SELECT CASE
        WHEN error_code = 'NOT_CALLED' THEN 'NOT_CALLED'
        WHEN error_code = 'DISABLED' THEN 'DISABLED'
        WHEN error_code = 'ABSTAIN' THEN 'ABSTAIN'
        WHEN call_status = 'TIMEOUT' THEN 'TIMEOUT'
        WHEN call_status = 'FAILED' THEN 'FAILED'
    END AS marker
    FROM tm_ai_call_log
) required_markers
WHERE marker IS NOT NULL;
SELECT 'AI_CALL_LOG_UNSAFE', COUNT(*) FROM tm_ai_call_log
WHERE NOT review_only OR NOT manual_review_only OR NOT not_trade_instruction
   OR NOT not_executable OR NOT not_auto_trading OR NOT not_order_execution
   OR NOT not_user_position_creation OR NOT not_position_mutation
   OR NOT not_state_machine_override OR NOT not_execution_plan_creation
   OR NOT rule_direction_preserved;
SELECT 'OHLCV_TOTAL', COUNT(*) FROM tm_persisted_ohlcv_bar;
SELECT 'OHLCV_COMBINATION_COUNT', COUNT(*)
FROM (
    SELECT symbol, timeframe
    FROM tm_persisted_ohlcv_bar
    GROUP BY symbol, timeframe
    HAVING COUNT(*) >= 50
) combinations;
SELECT 'OHLCV_NON_SYNTHETIC', COUNT(*) FROM tm_persisted_ohlcv_bar
WHERE provider <> 'SYNTHETIC_FIXTURE' OR provider_market_type <> 'SYNTHETIC'
   OR source_endpoint <> 'generated-evidence';
SELECT 'DISPATCH_CONFIG_TOTAL', COUNT(*) FROM tm_push_recheck_dispatch_config;

COMMIT;
