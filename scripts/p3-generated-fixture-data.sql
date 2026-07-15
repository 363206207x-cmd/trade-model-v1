\set ON_ERROR_STOP on

BEGIN;
SET LOCAL statement_timeout = '180s';
SET LOCAL lock_timeout = '5s';
SET LOCAL TIME ZONE 'UTC';

-- Fixed seed contract: 20260715. Every row is synthetic and deterministic.
WITH assets(symbol, asset_no) AS (
    VALUES ('BTCUSDT', 1), ('ETHUSDT', 2), ('SOLUSDT', 3),
           ('BNBUSDT', 4), ('XRPUSDT', 5), ('DOGEUSDT', 6)
), samples(sample_no) AS (
    SELECT generate_series(1, 23)
)
INSERT INTO tm_analysis_run(
    analysis_id, symbol, timeframe, analysis_time, rule_version,
    data_quality_score, trace_id, status, idempotency_key, request_id,
    trigger_type, trigger_reference, input_snapshot_json, input_snapshot_hash,
    attempt_count, started_at, completed_at, error_code, error_message,
    created_at, updated_at, version_no
)
SELECT
    'P3A-' || symbol || '-' || lpad(sample_no::text, 3, '0'),
    symbol,
    (ARRAY['1m', '15m', '1h', '4h'])[1 + ((sample_no - 1) % 4)],
    timestamp '2026-07-14 00:00:00' + ((asset_no * 30 + sample_no) * interval '5 minutes'),
    'p3-generated-v1',
    40 + ((asset_no * 11 + sample_no * 7) % 61),
    'P3T-' || symbol || '-' || lpad(sample_no::text, 3, '0'),
    CASE WHEN sample_no <= 20 THEN 'SUCCESS'
         WHEN sample_no <= 22 THEN 'FAILED' ELSE 'STARTED' END,
    'P3I-' || symbol || '-' || lpad(sample_no::text, 3, '0'),
    'P3R-' || symbol || '-' || lpad(sample_no::text, 3, '0'),
    'MANUAL_REVIEW',
    'generated evidence',
    '{"fixture":"generated evidence"}',
    md5('20260715-' || symbol || '-' || sample_no),
    1,
    timestamp '2026-07-14 00:00:00' + ((asset_no * 30 + sample_no) * interval '5 minutes'),
    CASE WHEN sample_no <= 22
         THEN timestamp '2026-07-14 00:02:00' + ((asset_no * 30 + sample_no) * interval '5 minutes')
         ELSE NULL END,
    CASE WHEN sample_no IN (21, 22) THEN 'GENERATED_FAILURE' ELSE NULL END,
    CASE WHEN sample_no IN (21, 22) THEN 'generated evidence' ELSE NULL END,
    timestamp '2026-07-14 00:00:00' + ((asset_no * 30 + sample_no) * interval '5 minutes'),
    timestamp '2026-07-14 00:03:00' + ((asset_no * 30 + sample_no) * interval '5 minutes'),
    1
FROM assets CROSS JOIN samples;

WITH assets(symbol, asset_no) AS (
    VALUES ('BTCUSDT', 1), ('ETHUSDT', 2), ('SOLUSDT', 3),
           ('BNBUSDT', 4), ('XRPUSDT', 5), ('DOGEUSDT', 6)
), samples(sample_no) AS (
    SELECT generate_series(1, 20)
)
INSERT INTO tm_decision_result(
    decision_id, analysis_id, symbol, market_bias_hierarchy, trade_type,
    confidence_level, risk_level, action_priority, conclusion_summary,
    is_worth_opening, multi_tf_convergence, ai_role_results, is_adopted,
    valid_period, invalid_condition, evidence_summary, explanation_json,
    review_reasons, ai_conflict_level, ai_conflict_score, ai_plan_mode,
    confused_score, asset_state_snapshot, create_time
)
SELECT
    'P3D-' || symbol || '-' || lpad(sample_no::text, 3, '0'),
    'P3A-' || symbol || '-' || lpad(sample_no::text, 3, '0'),
    symbol,
    (ARRAY['BULLISH', 'BEARISH', 'NEUTRAL', 'WAIT', 'BLOCKED'])[1 + ((sample_no - 1) % 5)],
    (ARRAY['LONG', 'SHORT', 'NO_TRADE', 'WATCH', 'BLOCKED'])[1 + ((sample_no - 1) % 5)],
    (ARRAY['HIGH', 'MEDIUM', 'LOW'])[1 + ((sample_no - 1) % 3)],
    (ARRAY['LOW', 'MEDIUM', 'HIGH'])[1 + ((sample_no - 1) % 3)],
    (ARRAY['NORMAL', 'REVIEW', 'BLOCKED'])[1 + ((sample_no - 1) % 3)],
    'synthetic fixture',
    sample_no % 5 IN (1, 2),
    CASE WHEN sample_no % 4 = 0 THEN 'source unavailable' ELSE 'generated evidence' END,
    NULL,
    FALSE,
    CASE WHEN sample_no = 1
         THEN '2026-07-13T00:00:00Z ~ 2026-07-13T01:00:00Z'
         ELSE '2099-07-14T00:00:00Z ~ 2099-07-14T01:00:00Z' END,
    CASE WHEN sample_no % 5 = 0 THEN 'source unavailable' ELSE 'manual review only' END,
    'generated evidence',
    '{"fixture":"generated evidence"}',
    CASE WHEN sample_no % 4 = 0 THEN 'source unavailable' ELSE 'manual review only' END,
    'NOT_APPLICABLE',
    0,
    CASE WHEN sample_no % 4 = 0 THEN 'ABSTAIN' ELSE 'DISABLED' END,
    (asset_no * 9 + sample_no * 3) % 101,
    'generated evidence',
    timestamp '2026-07-14 00:04:00' + ((asset_no * 30 + sample_no) * interval '5 minutes')
FROM assets CROSS JOIN samples;

WITH assets(symbol) AS (
    VALUES ('BTCUSDT'), ('ETHUSDT'), ('SOLUSDT'),
           ('BNBUSDT'), ('XRPUSDT'), ('DOGEUSDT')
), samples(sample_no) AS (
    SELECT generate_series(1, 20)
), plan_state AS (
    SELECT symbol, sample_no,
           CASE sample_no % 5
               WHEN 1 THEN 'VALID'
               WHEN 2 THEN 'INCOMPLETE'
               WHEN 3 THEN 'REVIEW_ONLY'
               WHEN 4 THEN 'INVALID'
               ELSE 'BLOCKED'
           END AS status
    FROM assets CROSS JOIN samples
)
INSERT INTO tm_execution_plan(
    plan_id, analysis_id, plan_mode, execution_plan_status,
    source_gate_status, source_gate_complete, source_missing_reasons,
    source_blocker_reasons, source_completeness_summary, recommended_action,
    entry_zone, stop_loss, take_profit_rules, leverage_suggestion,
    position_suggestion, account_risk_json, invalid_condition,
    manual_review_required, not_trade_instruction, not_executable,
    not_auto_trading, not_order_execution, not_user_position_creation,
    needs_revalidation, revalidation_reason, revalidation_required_at, create_time
)
SELECT
    'P3P-' || symbol || '-' || lpad(sample_no::text, 3, '0') || '-A',
    'P3A-' || symbol || '-' || lpad(sample_no::text, 3, '0'),
    CASE WHEN status = 'VALID' THEN 'ADVISORY' ELSE 'REVIEW_ONLY' END,
    status,
    status,
    status = 'VALID',
    CASE WHEN status = 'INCOMPLETE' THEN 'source unavailable' ELSE NULL END,
    CASE WHEN status IN ('BLOCKED', 'INVALID') THEN 'manual review only' ELSE NULL END,
    CASE WHEN status = 'VALID' THEN 'generated evidence' ELSE 'source unavailable' END,
    'MANUAL_REVIEW',
    CASE WHEN status = 'VALID' THEN 'synthetic fixture' ELSE NULL END,
    CASE WHEN status = 'VALID' THEN 'synthetic fixture' ELSE NULL END,
    CASE WHEN status = 'VALID' THEN 'synthetic fixture' ELSE NULL END,
    'manual review only',
    'manual review only',
    '{"fixture":"generated evidence"}',
    CASE WHEN status = 'VALID' THEN 'manual review only' ELSE 'source unavailable' END,
    TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
    sample_no % 7 = 0,
    CASE WHEN sample_no % 7 = 0 THEN 'manual review only' ELSE NULL END,
    CASE WHEN sample_no % 7 = 0
         THEN timestamp '2026-07-14 12:00:00' + (sample_no * interval '1 minute')
         ELSE NULL END,
    timestamp '2026-07-14 00:05:00' + (sample_no * interval '5 minutes')
FROM plan_state;

INSERT INTO tm_execution_plan(
    plan_id, analysis_id, plan_mode, execution_plan_status,
    source_gate_status, source_gate_complete, source_blocker_reasons,
    source_completeness_summary, recommended_action, invalid_condition,
    manual_review_required, not_trade_instruction, not_executable,
    not_auto_trading, not_order_execution, not_user_position_creation,
    needs_revalidation, create_time
) VALUES (
    'P3P-BTCUSDT-001-B', 'P3A-BTCUSDT-001', 'REVIEW_ONLY', 'BLOCKED',
    'BLOCKED', FALSE, 'manual review only', 'source unavailable',
    'MANUAL_REVIEW', 'source unavailable', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
    FALSE, timestamp '2026-07-14 03:00:00'
);

INSERT INTO tm_asset_state(
    symbol, state, confused_score, confused_low_streak, hot_reset_flag,
    last_update_time, trace_id
) VALUES
('BTCUSDT',   'DATA_INSUFFICIENT', 10, 0, FALSE, timestamp '2026-07-14 10:00:00', 'P3T-BTCUSDT-020'),
('ETHUSDT',   'ANALYZING',         20, 0, FALSE, timestamp '2026-07-14 10:01:00', 'P3T-ETHUSDT-002'),
('SOLUSDT',   'WAITING_TRIGGER',   30, 0, FALSE, timestamp '2026-07-14 10:02:00', 'P3T-SOLUSDT-007'),
('BNBUSDT',   'TRIGGERED',         40, 0, FALSE, timestamp '2026-07-14 10:03:00', 'P3T-BNBUSDT-020'),
('XRPUSDT',   'WAITING_TRIGGER',   50, 0, FALSE, timestamp '2026-07-14 10:04:00', 'P3T-XRPUSDT-001'),
('DOGEUSDT',  'POSITION_MONITORING', 60, 0, FALSE, timestamp '2026-07-14 10:05:00', 'P3T-DOGEUSDT-020'),
('SYNTHAUSDT','BLOCKED',           85, 0, FALSE, timestamp '2026-07-14 10:06:00', 'P3T-SYNTHA-001'),
('SYNTHBUSDT','INVALIDATED',       90, 0, TRUE,  timestamp '2026-07-14 10:07:00', 'P3T-SYNTHB-001'),
('SYNTHCUSDT','PLAN_READY',        50, 0, FALSE, timestamp '2026-07-14 10:08:00', 'P3T-SYNTHC-001');

INSERT INTO tm_user_position(
    id, asset_symbol, side, status, entry_price, quantity, leverage,
    stop_loss, take_profit, opened_at, closed_at, close_price, close_reason,
    source_type, source_ref_id, manual_review_required, not_trade_instruction,
    not_auto_trading, not_order_execution, not_position_sync, created_at, updated_at
) VALUES
(1001, 'BTCUSDT',  'LONG',  'OPEN',             101, 1, 1,  90, 120, timestamp '2026-07-14 01:00:00', NULL, NULL, NULL, 'MANUAL', 'EXECUTION_PLAN:P3P-BTCUSDT-001-A', TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 01:00:00', timestamp '2026-07-14 08:00:00'),
(1002, 'BTCUSDT',  'SHORT', 'OPEN',             102, 2, 1, 110,  80, timestamp '2026-07-14 02:00:00', NULL, NULL, NULL, 'MANUAL', 'EXECUTION_PLAN:P3P-BTCUSDT-002-A', TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 02:00:00', timestamp '2026-07-14 08:01:00'),
(1003, 'ETHUSDT',  'LONG',  'OPEN',             103, 3, 1,  90, 120, timestamp '2026-07-14 03:00:00', NULL, NULL, NULL, 'MANUAL', 'ANALYSIS:P3A-ETHUSDT-002', TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 03:00:00', timestamp '2026-07-14 08:02:00'),
(1004, 'SOLUSDT',  'LONG',  'PARTIALLY_CLOSED', 104, 4, 1,  90, 120, timestamp '2026-07-14 04:00:00', NULL, NULL, NULL, 'MANUAL', 'EXECUTION_PLAN:P3P-SOLUSDT-007-A', TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 04:00:00', timestamp '2026-07-14 08:03:00'),
(1005, 'BNBUSDT',  'SHORT', 'CLOSED',           105, 5, 1, 115,  85, timestamp '2026-07-13 05:00:00', timestamp '2026-07-14 05:00:00', 100, 'manual review only', 'MANUAL', 'EXECUTION_PLAN:P3P-BNBUSDT-001-A', TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-13 05:00:00', timestamp '2026-07-14 05:00:00'),
(1006, 'XRPUSDT',  'LONG',  'OPEN',             106, 6, 1,  90, 120, timestamp '2026-07-14 06:00:00', NULL, NULL, NULL, 'MANUAL', 'EXECUTION_PLAN:P3P-XRPUSDT-001-A', TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 06:00:00', timestamp '2026-07-14 08:04:00'),
(1007, 'DOGEUSDT', 'LONG',  'CLOSED',           107, 7, 1,  90, 120, timestamp '2026-07-13 07:00:00', timestamp '2026-07-14 07:00:00', 108, 'manual review only', 'MANUAL', NULL, TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-13 07:00:00', timestamp '2026-07-14 07:00:00');

SELECT setval(pg_get_serial_sequence('tm_user_position', 'id'), 1007, TRUE);

INSERT INTO tm_position_monitor_log(
    log_id, position_id, analysis_id, execution_plan_id, current_price,
    logic_status, risk_level, suggested_action, reason, evidence_snapshot,
    score_snapshot, decision_snapshot, risk_snapshot, trace_id, created_at
) VALUES
(3001, 1001, 'P3A-BTCUSDT-001', 'P3P-BTCUSDT-001-A', 101, 'LOGIC_VALID',       'LOW',    'HOLD',          'generated evidence', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'P3M-BTC-A-001', timestamp '2026-07-14 09:00:00'),
(3002, 1001, 'P3A-BTCUSDT-001', 'P3P-BTCUSDT-001-A', 102, 'LOGIC_WEAKENED',    'MEDIUM', 'MANUAL_REVIEW', 'manual review only',  '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'P3M-BTC-A-002', timestamp '2026-07-14 09:05:00'),
(3003, 1002, 'P3A-BTCUSDT-002', 'P3P-BTCUSDT-002-A', 103, 'PLAN_INVALIDATED',   'HIGH',   'RECHECK_PLAN',  'source unavailable',  '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'P3M-BTC-B-001', timestamp '2026-07-14 09:10:00'),
(3004, 1003, 'P3A-ETHUSDT-002', 'P3P-ETHUSDT-002-A', 104, 'LOGIC_VALID',        'LOW',    'HOLD',          'generated evidence', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'P3M-ETH-A-001', timestamp '2026-07-14 09:15:00'),
(3005, 1004, 'P3A-SOLUSDT-007', 'P3P-SOLUSDT-007-A', 105, 'PLAN_INVALIDATED',   'HIGH',   'RECHECK_PLAN',  'manual review only',  '{"fixture":"generated evidence","reasonCodes":["PLAN_REVALIDATION_REQUIRED"]}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'P3M-SOL-A-001', timestamp '2026-07-14 09:20:00'),
(3006, 1004, 'P3A-SOLUSDT-007', 'P3P-SOLUSDT-007-A', 106, 'HIGH_RISK',          'HIGH',   'RISK_REVIEW',   'source unavailable',  '{"fixture":"generated evidence","reasonCodes":["PLAN_BOUNDARY_INCOMPLETE"]}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'P3M-SOL-A-002', timestamp '2026-07-14 09:25:00'),
(3007, 1006, 'P3A-XRPUSDT-001', 'P3P-XRPUSDT-001-A', 107, 'LOGIC_VALID',        'LOW',    'HOLD',          'generated evidence', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'P3M-XRP-A-001', timestamp '2026-07-14 09:30:00'),
(3008, 1007, 'POSITION_SOURCE_UNVERIFIED', NULL,                 108, 'LOGIC_WEAKENED',     'MEDIUM', 'MANUAL_REVIEW', 'source unavailable',  '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'P3M-DOGE-U-001', timestamp '2026-07-14 09:35:00');

SELECT setval(pg_get_serial_sequence('tm_position_monitor_log', 'log_id'), 3008, TRUE);

INSERT INTO tm_monitor_alert(
    id, analysis_id, asset_symbol, alert_type, alert_level, alert_message,
    status, cooldown_until, suppress_reason, trace_id, rule_version,
    created_by, updated_by, created_at, updated_at, is_deleted, version_no
) VALUES
('P3ALERT-001', 'P3A-BTCUSDT-001',  'BTCUSDT',  'LOGIC_REVIEW',  'LOW',    'generated evidence', 'OPEN',         timestamp '2026-07-14 10:15:00', NULL, 'P3T-BTCUSDT-001',  'p3-generated-v1', 'fixture', 'fixture', timestamp '2026-07-14 10:00:00', timestamp '2026-07-14 10:00:00', 0, 1),
('P3ALERT-002', 'P3A-ETHUSDT-001',  'ETHUSDT',  'RISK_REVIEW',   'MEDIUM', 'manual review only',  'ACKNOWLEDGED', timestamp '2026-07-14 10:16:00', NULL, 'P3T-ETHUSDT-001',  'p3-generated-v1', 'fixture', 'fixture', timestamp '2026-07-14 10:01:00', timestamp '2026-07-14 10:02:00', 0, 1),
('P3ALERT-003', 'P3A-SOLUSDT-001',  'SOLUSDT',  'PLAN_REVIEW',   'HIGH',   'source unavailable',  'RESOLVED',     timestamp '2026-07-14 10:17:00', NULL, 'P3T-SOLUSDT-001',  'p3-generated-v1', 'fixture', 'fixture', timestamp '2026-07-14 10:02:00', timestamp '2026-07-14 10:03:00', 0, 1),
('P3ALERT-004', 'P3A-BNBUSDT-001',  'BNBUSDT',  'DATA_REVIEW',   'LOW',    'generated evidence', 'OPEN',         timestamp '2026-07-14 10:18:00', NULL, 'P3T-BNBUSDT-001',  'p3-generated-v1', 'fixture', 'fixture', timestamp '2026-07-14 10:03:00', timestamp '2026-07-14 10:03:00', 0, 1),
('P3ALERT-005', 'P3A-XRPUSDT-001',  'XRPUSDT',  'SOURCE_REVIEW', 'MEDIUM', 'source unavailable',  'ACKNOWLEDGED', timestamp '2026-07-14 10:19:00', NULL, 'P3T-XRPUSDT-001',  'p3-generated-v1', 'fixture', 'fixture', timestamp '2026-07-14 10:04:00', timestamp '2026-07-14 10:05:00', 0, 1),
('P3ALERT-006', 'P3A-DOGEUSDT-001', 'DOGEUSDT', 'STATE_REVIEW',  'HIGH',   'manual review only',  'RESOLVED',     timestamp '2026-07-14 10:20:00', NULL, 'P3T-DOGEUSDT-001', 'p3-generated-v1', 'fixture', 'fixture', timestamp '2026-07-14 10:05:00', timestamp '2026-07-14 10:06:00', 0, 1);

INSERT INTO tm_push_snapshot(
    push_id, analysis_id, symbol, timeframe, push_type, push_status,
    push_create_time, rule_version, trigger_price, entry_zone_json,
    stop_zone_json, invalidation_condition_json, plan_mode_snapshot,
    cause_effect_alignment_snapshot, execution_feasibility_snapshot,
    data_quality_score_snapshot, confused_score_snapshot, expires_at,
    trace_id, create_time
) VALUES
(4001, 'P3A-BTCUSDT-001',  'BTCUSDT',  '15m', 'REVIEW_ONLY', 'CAPTURED',               timestamp '2026-07-14 11:00:00', 'p3-generated-v1', 101, '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'REVIEW_ONLY', 'generated evidence', 0, 90, 10, timestamp '2099-07-14 11:00:00', 'P3PS-BTC-001',  timestamp '2026-07-14 11:00:00'),
(4002, 'P3A-ETHUSDT-001',  'ETHUSDT',  '1h',  'REVIEW_ONLY', 'RECHECK_VALID_WAITING',  timestamp '2026-07-14 11:01:00', 'p3-generated-v1', 102, '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'REVIEW_ONLY', 'generated evidence', 0, 80, 20, timestamp '2099-07-14 11:01:00', 'P3PS-ETH-001',  timestamp '2026-07-14 11:01:00'),
(4003, 'P3A-SOLUSDT-001',  'SOLUSDT',  '4h',  'REVIEW_ONLY', 'EXPIRED',                timestamp '2026-07-14 11:02:00', 'p3-generated-v1', 103, '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'REVIEW_ONLY', 'source unavailable', 0, 60, 30, timestamp '2026-07-14 11:03:00', 'P3PS-SOL-001',  timestamp '2026-07-14 11:02:00'),
(4004, 'P3A-BNBUSDT-001',  'BNBUSDT',  '1m',  'REVIEW_ONLY', 'RECHECK_INVALIDATED',    timestamp '2026-07-14 11:03:00', 'p3-generated-v1', 104, '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'REVIEW_ONLY', 'source unavailable', 0, 50, 40, timestamp '2099-07-14 11:03:00', 'P3PS-BNB-001',  timestamp '2026-07-14 11:03:00'),
(4005, 'P3A-XRPUSDT-001',  'XRPUSDT',  '15m', 'REVIEW_ONLY', 'NOT_APPLICABLE',         timestamp '2026-07-14 11:04:00', 'p3-generated-v1', 105, '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', '{"fixture":"generated evidence"}', 'REVIEW_ONLY', 'source unavailable', 0, 40, 50, timestamp '2099-07-14 11:04:00', 'P3PS-XRP-001',  timestamp '2026-07-14 11:04:00');

SELECT setval(pg_get_serial_sequence('tm_push_snapshot', 'push_id'), 4005, TRUE);

INSERT INTO tm_push_recheck_log(
    log_id, push_id, trigger_source, retry_attempt, max_attempts,
    retry_backoff_minutes, execution_status, execution_error_code,
    execution_error_message, recheck_time, recheck_status, current_price,
    current_data_quality_score, current_confused_score,
    current_account_risk_allowed, fail_reason_json, trace_id, create_time
) VALUES
(5001, 4001, 'MANUAL', 0, 1, 5, 'SUCCESS', NULL, NULL, timestamp '2026-07-14 11:10:00', 'VALID',       101, 90, 10, TRUE,  NULL, 'P3PR-BTC-001', timestamp '2026-07-14 11:10:00'),
(5002, 4002, 'MANUAL', 0, 1, 5, 'FAILED',  'QUOTE_UNAVAILABLE', 'source unavailable', timestamp '2026-07-14 11:11:00', 'INVALIDATED', 102, 80, 20, FALSE, '{"code":"QUOTE_UNAVAILABLE"}', 'P3PR-ETH-001', timestamp '2026-07-14 11:11:00'),
(5003, 4003, 'MANUAL', 0, 1, 5, 'SKIPPED', 'EXPIRED', NULL, timestamp '2026-07-14 11:12:00', 'EXPIRED',     103, 60, 30, FALSE, '{"code":"EXPIRED"}', 'P3PR-SOL-001', timestamp '2026-07-14 11:12:00'),
(5004, 4004, 'MANUAL', 0, 1, 5, 'FAILED',  'SOURCE_UNAVAILABLE', 'source unavailable', timestamp '2026-07-14 11:13:00', 'INVALIDATED', 104, 50, 40, FALSE, '{"code":"SOURCE_UNAVAILABLE"}', 'P3PR-BNB-001', timestamp '2026-07-14 11:13:00'),
(5005, 4005, 'MANUAL', 0, 1, 5, 'SKIPPED', 'NOT_APPLICABLE', NULL, timestamp '2026-07-14 11:14:00', 'NOT_APPLICABLE', 105, 40, 50, FALSE, '{"code":"NOT_APPLICABLE"}', 'P3PR-XRP-001', timestamp '2026-07-14 11:14:00');

SELECT setval(pg_get_serial_sequence('tm_push_recheck_log', 'log_id'), 5005, TRUE);

INSERT INTO tm_hot_reset_event(
    event_id, event_key, analysis_id, trace_id, symbol, timeframe,
    trigger_type, trigger_value, source_type, source_reference, severity_score,
    decision_id, decision_state, decision_invalidated_count,
    plan_revalidation_count, push_invalidated_count, confused_score_snapshot,
    confused_score_before, confused_score_after,
    multi_timeframe_aligned_snapshot, account_risk_status, account_risk_level,
    account_risk_blocked, account_risk_snapshot, rebuild_triggered,
    execution_status, trigger_reason_code, trigger_reason_text, event_version,
    event_time, pre_state, post_state, completed_at, create_time
) VALUES
('P3HR-001', 'P3HRK-001', 'P3A-BTCUSDT-001',  'P3T-BTCUSDT-001',  'BTCUSDT',  '15m', 'PRICE_MOVE',      'generated evidence', 'GENERATED', 'generated evidence', 80, 'P3D-BTCUSDT-001',  'INVALIDATED', 1, 1, 1, 80, 70, 40, FALSE, 'BLOCKED', 'HIGH', TRUE, '{"fixture":"generated evidence"}', FALSE, 'COMPLETED', 'GENERATED_EVENT', 'generated evidence', 1, timestamp '2026-07-14 12:00:00', 'PLAN_READY', 'ANALYZING', timestamp '2026-07-14 12:01:00', timestamp '2026-07-14 12:00:00'),
('P3HR-002', 'P3HRK-002', 'P3A-ETHUSDT-001',  'P3T-ETHUSDT-001',  'ETHUSDT',  '1h',  'OI_COLLAPSE',     'generated evidence', 'GENERATED', 'generated evidence', 81, 'P3D-ETHUSDT-001',  'INVALIDATED', 1, 1, 0, 81, 71, 41, FALSE, 'BLOCKED', 'HIGH', TRUE, '{"fixture":"generated evidence"}', FALSE, 'COMPLETED', 'GENERATED_EVENT', 'generated evidence', 1, timestamp '2026-07-14 12:02:00', 'TRIGGERED', 'ANALYZING', timestamp '2026-07-14 12:03:00', timestamp '2026-07-14 12:02:00'),
('P3HR-003', 'P3HRK-003', 'P3A-SOLUSDT-001',  'P3T-SOLUSDT-001',  'SOLUSDT',  '4h',  'LIQUIDITY_DRAIN', 'generated evidence', 'GENERATED', 'generated evidence', 82, 'P3D-SOLUSDT-001',  'INVALIDATED', 1, 1, 0, 82, 72, 42, FALSE, 'BLOCKED', 'HIGH', TRUE, '{"fixture":"generated evidence"}', FALSE, 'COMPLETED', 'GENERATED_EVENT', 'generated evidence', 1, timestamp '2026-07-14 12:04:00', 'WAITING_TRIGGER', 'ANALYZING', timestamp '2026-07-14 12:05:00', timestamp '2026-07-14 12:04:00'),
('P3HR-004', 'P3HRK-004', 'P3A-BNBUSDT-001',  'P3T-BNBUSDT-001',  'BNBUSDT',  '1m',  'SYSTEMIC_SHOCK',  'generated evidence', 'GENERATED', 'generated evidence', 83, 'P3D-BNBUSDT-001',  'INVALIDATED', 1, 1, 0, 83, 73, 43, FALSE, 'BLOCKED', 'HIGH', TRUE, '{"fixture":"generated evidence"}', FALSE, 'COMPLETED', 'GENERATED_EVENT', 'generated evidence', 1, timestamp '2026-07-14 12:06:00', 'BLOCKED', 'ANALYZING', timestamp '2026-07-14 12:07:00', timestamp '2026-07-14 12:06:00'),
('P3HR-005', 'P3HRK-005', 'P3A-XRPUSDT-001',  'P3T-XRPUSDT-001',  'XRPUSDT',  '15m', 'AI_CONFLICT',     'generated evidence', 'GENERATED', 'generated evidence', 84, 'P3D-XRPUSDT-001',  'INVALIDATED', 1, 1, 0, 84, 74, 44, FALSE, 'BLOCKED', 'HIGH', TRUE, '{"fixture":"generated evidence"}', FALSE, 'COMPLETED', 'GENERATED_EVENT', 'generated evidence', 1, timestamp '2026-07-14 12:08:00', 'PLAN_READY', 'ANALYZING', timestamp '2026-07-14 12:09:00', timestamp '2026-07-14 12:08:00'),
('P3HR-006', 'P3HRK-006', 'P3A-DOGEUSDT-001', 'P3T-DOGEUSDT-001', 'DOGEUSDT', '1h',  'DATA_QUALITY',    'generated evidence', 'GENERATED', 'generated evidence', 85, 'P3D-DOGEUSDT-001', 'INVALIDATED', 1, 1, 0, 85, 75, 45, FALSE, 'BLOCKED', 'HIGH', TRUE, '{"fixture":"generated evidence"}', FALSE, 'COMPLETED', 'GENERATED_EVENT', 'generated evidence', 1, timestamp '2026-07-14 12:10:00', 'DATA_INSUFFICIENT', 'ANALYZING', timestamp '2026-07-14 12:11:00', timestamp '2026-07-14 12:10:00');

INSERT INTO tm_ai_call_log(
    call_id, analysis_id, trace_id, request_id, provider_name, model_name,
    ai_role, call_status, started_at, completed_at, latency_ms,
    fallback_flag, fallback_reason, rate_limited, budget_blocked, timeout_flag,
    error_code, error_message, request_summary, response_summary, rule_version,
    review_only, manual_review_only, not_trade_instruction, not_executable,
    not_auto_trading, not_order_execution, not_user_position_creation,
    not_position_mutation, not_state_machine_override,
    not_execution_plan_creation, rule_direction_preserved, created_at, updated_at
) VALUES
('P3AI-001', 'P3A-BTCUSDT-001', 'P3T-BTCUSDT-001', 'P3AIR-001', 'SYNTHETIC', NULL, 'GPT_FINAL',       'DISABLED', timestamp '2026-07-14 13:00:00', timestamp '2026-07-14 13:00:00', 0, FALSE, NULL, FALSE, FALSE, FALSE, 'NOT_CALLED', NULL, NULL, NULL, 'p3-generated-v1', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 13:00:00', timestamp '2026-07-14 13:00:00'),
('P3AI-002', 'P3A-ETHUSDT-001', 'P3T-ETHUSDT-001', 'P3AIR-002', 'SYNTHETIC', NULL, 'GEMINI_REVIEW',   'DISABLED', timestamp '2026-07-14 13:01:00', timestamp '2026-07-14 13:01:00', 0, FALSE, NULL, FALSE, FALSE, FALSE, 'DISABLED', NULL, NULL, NULL, 'p3-generated-v1', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 13:01:00', timestamp '2026-07-14 13:01:00'),
('P3AI-003', 'P3A-SOLUSDT-001', 'P3T-SOLUSDT-001', 'P3AIR-003', 'SYNTHETIC', NULL, 'GROK_CHALLENGE',  'DISABLED', timestamp '2026-07-14 13:02:00', timestamp '2026-07-14 13:02:00', 0, FALSE, NULL, FALSE, FALSE, FALSE, 'ABSTAIN', NULL, NULL, NULL, 'p3-generated-v1', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 13:02:00', timestamp '2026-07-14 13:02:00'),
('P3AI-004', 'P3A-BNBUSDT-001', 'P3T-BNBUSDT-001', 'P3AIR-004', 'SYNTHETIC', NULL, 'GEMINI_REVIEW',   'TIMEOUT',  timestamp '2026-07-14 13:03:00', timestamp '2026-07-14 13:03:05', 5000, FALSE, NULL, FALSE, FALSE, TRUE, 'TIMEOUT', 'source unavailable', NULL, NULL, 'p3-generated-v1', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 13:03:00', timestamp '2026-07-14 13:03:05'),
('P3AI-005', 'P3A-XRPUSDT-001', 'P3T-XRPUSDT-001', 'P3AIR-005', 'SYNTHETIC', NULL, 'GPT_FINAL',       'FAILED',   timestamp '2026-07-14 13:04:00', timestamp '2026-07-14 13:04:01', 1000, FALSE, NULL, FALSE, FALSE, FALSE, 'GENERATED_FAILURE', 'source unavailable', NULL, NULL, 'p3-generated-v1', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, timestamp '2026-07-14 13:04:00', timestamp '2026-07-14 13:04:01');

WITH assets(symbol, asset_no) AS (
    VALUES ('BTCUSDT', 1), ('ETHUSDT', 2), ('SOLUSDT', 3),
           ('BNBUSDT', 4), ('XRPUSDT', 5), ('DOGEUSDT', 6)
), frames(timeframe, frame_no, duration_ms) AS (
    VALUES ('1m', 1, 60000::bigint), ('15m', 2, 900000::bigint),
           ('1h', 3, 3600000::bigint), ('4h', 4, 14400000::bigint)
), bars(bar_no) AS (
    SELECT generate_series(1, 50)
)
INSERT INTO tm_persisted_ohlcv_bar(
    symbol, timeframe, open_time_ms, close_time_ms, open_price, high_price,
    low_price, close_price, volume, quote_volume, trade_count,
    taker_buy_base_volume, taker_buy_quote_volume, is_closed, provider,
    provider_market_type, source_endpoint, source_batch_id, source_trace_id,
    source_version, ingested_at, updated_at, quality_status, quality_reason,
    raw_payload_hash, is_deleted, fetch_time, source_status,
    freshness_status, provenance_version, ingestion_run_id
)
SELECT
    symbol,
    timeframe,
    1783900800000::bigint + (bar_no - 1) * duration_ms,
    1783900800000::bigint + bar_no * duration_ms - 1,
    100 + asset_no * 10 + bar_no * 0.10,
    101 + asset_no * 10 + bar_no * 0.10,
     99 + asset_no * 10 + bar_no * 0.10,
    100.5 + asset_no * 10 + bar_no * 0.10,
    1000 + asset_no * 100 + frame_no * 10 + bar_no,
    2000 + asset_no * 100 + frame_no * 10 + bar_no,
    100 + bar_no,
    500 + bar_no,
    1000 + bar_no,
    TRUE,
    'SYNTHETIC_FIXTURE',
    'SYNTHETIC',
    'generated-evidence',
    'P3B-' || symbol || '-' || timeframe,
    'P3K-' || symbol || '-' || timeframe || '-' || lpad(bar_no::text, 3, '0'),
    1,
    timestamp '2026-07-15 00:00:00' + (asset_no * interval '1 minute'),
    timestamp '2026-07-15 00:00:00' + (asset_no * interval '1 minute'),
    'VALID',
    'synthetic fixture',
    md5('20260715-' || symbol || '-' || timeframe || '-' || bar_no),
    0,
    timestamp '2026-07-15 00:00:00' + (asset_no * interval '1 minute'),
    'AVAILABLE',
    'FRESH',
    'p3-generated-v1',
    'P3INGEST-20260715'
FROM assets CROSS JOIN frames CROSS JOIN bars;

INSERT INTO tm_push_recheck_dispatch_config(
    config_key, config_value, updated_by, update_source, update_time
) VALUES
('limit', 50, 'fixture', 'GENERATED_FIXTURE', timestamp '2026-07-14 14:00:00'),
('maxAttempts', 3, 'fixture', 'GENERATED_FIXTURE', timestamp '2026-07-14 14:00:00'),
('minRetryMinutes', 5, 'fixture', 'GENERATED_FIXTURE', timestamp '2026-07-14 14:00:00');

COMMIT;
