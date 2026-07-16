\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE p3h_contract_input(expected_version integer) ON COMMIT DROP;
INSERT INTO p3h_contract_input(expected_version) VALUES (:p3h_expected_version);

CREATE TEMP TABLE p3h_expected_rule_defaults (
    rule_id varchar(64) NOT NULL,
    rule_type varchar(64) NOT NULL,
    rule_key varchar(128) NOT NULL,
    rule_value varchar(256) NOT NULL,
    description varchar(512) NOT NULL,
    version varchar(32) NOT NULL,
    enabled boolean NOT NULL,
    min_flyway_version integer NOT NULL
) ON COMMIT DROP;

INSERT INTO p3h_expected_rule_defaults VALUES
('cfg-hot-reset-price-move', 'hot_reset_config', 'hot_reset_config.extreme_price_move_ratio_threshold', '0.08', 'Hot Reset extreme price move ratio threshold', 'v1.0', TRUE, 3),
('cfg-hot-reset-oi-collapse', 'hot_reset_config', 'hot_reset_config.oi_collapse_change_ratio_threshold', '-0.30', 'Hot Reset open-interest collapse threshold', 'v1.0', TRUE, 3),
('cfg-hot-reset-liquidity-drain', 'hot_reset_config', 'hot_reset_config.liquidity_drain_change_ratio_threshold', '-0.40', 'Hot Reset liquidity drain threshold', 'v1.0', TRUE, 3),
('cfg-hot-reset-systemic-severity', 'hot_reset_config', 'hot_reset_config.systemic_shock_severity_threshold', '85', 'Hot Reset systemic shock severity threshold', 'v1.0', TRUE, 3),
('cfg-confused-enter-threshold', 'confused_state_config', 'confused_state_config.enter_threshold', '70', 'Confused state enter threshold', 'v1.0', TRUE, 3),
('cfg-confused-push-block-threshold', 'confused_state_config', 'confused_state_config.directional_push_block_threshold', '85', 'Directional push block threshold for Confused state', 'v1.0', TRUE, 3),
('cfg-confused-exit-threshold', 'confused_state_config', 'confused_state_config.exit_threshold_exclusive', '55', 'Confused state exit threshold exclusive', 'v1.0', TRUE, 3),
('cfg-confused-exit-cycles', 'confused_state_config', 'confused_state_config.exit_required_consecutive_cycles', '2', 'Required consecutive low cycles before Confused exit', 'v1.0', TRUE, 3),
('cfg-ai-conflict-level1-max', 'ai_conflict_config', 'ai_conflict_config.level1_max_score', '20', 'AI conflict level 1 max score', 'v1.0', TRUE, 3),
('cfg-ai-conflict-level2-max', 'ai_conflict_config', 'ai_conflict_config.level2_max_score', '45', 'AI conflict level 2 max score', 'v1.0', TRUE, 3),
('cfg-ai-conflict-level3-max', 'ai_conflict_config', 'ai_conflict_config.level3_max_score', '70', 'AI conflict level 3 max score', 'v1.0', TRUE, 3),
('cfg-ai-conflict-single-objection-max', 'ai_conflict_config', 'ai_conflict_config.single_objection_max_score', '35', 'Single AI objection score cap', 'v1.0', TRUE, 3),
('cfg-push-recheck-drift-ratio', 'push_recheck_config', 'push_recheck_config.drift_ratio_threshold', '0.02', 'Push recheck drift ratio threshold', 'v1.0', TRUE, 3),
('cfg-push-recheck-confused-wait', 'push_recheck_config', 'push_recheck_config.confused_wait_threshold', '70', 'Push recheck Confused wait threshold', 'v1.0', TRUE, 3),
('cfg-push-recheck-confused-block', 'push_recheck_config', 'push_recheck_config.confused_block_threshold', '85', 'Push recheck Confused block threshold', 'v1.0', TRUE, 3),
('cfg-push-recheck-exec-feas-wait', 'push_recheck_config', 'push_recheck_config.execution_feasibility_wait_threshold', '60', 'Push recheck execution feasibility wait threshold', 'v1.0', TRUE, 3),
('cfg-missed-review-window-hours', 'missed_opportunity_config', 'missed_opportunity_config.review_window_hours', '24', 'Missed opportunity review window in hours', 'v1.0', TRUE, 3),
('cfg-missed-min-mfe-ratio', 'missed_opportunity_config', 'missed_opportunity_config.min_mfe_ratio_threshold', '0.01', 'Missed opportunity minimum MFE ratio', 'v1.0', TRUE, 3),
('cfg-missed-max-mae-ratio', 'missed_opportunity_config', 'missed_opportunity_config.max_mae_ratio_threshold', '0.02', 'Missed opportunity maximum MAE ratio', 'v1.0', TRUE, 3),
('cfg-provider-scan-emergency-price', 'provider_scan_profile_config', 'provider.scan.emergency_price_movement_1m', '0.05', 'Emergency 1m price movement threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-emergency-liquidation', 'provider_scan_profile_config', 'provider.scan.emergency_liquidation_spike', '90', 'Emergency liquidation spike score threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-emergency-confused', 'provider_scan_profile_config', 'provider.scan.emergency_confused_score', '85', 'Emergency confused score threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-high-price', 'provider_scan_profile_config', 'provider.scan.high_price_movement_1m', '0.02', 'High 1m price movement threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-high-atr', 'provider_scan_profile_config', 'provider.scan.high_atr_multiple_5m', '2.0', 'High 5m ATR multiple threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-high-volume', 'provider_scan_profile_config', 'provider.scan.high_volume_spike', '2.5', 'High volume spike threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-high-spread', 'provider_scan_profile_config', 'provider.scan.high_spread_spike', '2.0', 'High spread spike threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-high-oi', 'provider_scan_profile_config', 'provider.scan.high_open_interest_change', '0.10', 'High open interest change threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-high-funding', 'provider_scan_profile_config', 'provider.scan.high_funding_extremity', '80', 'High funding extremity threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-near-boundary', 'provider_scan_profile_config', 'provider.scan.near_boundary_distance', '0.01', 'Near stop or target distance threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-data-quality', 'provider_scan_profile_config', 'provider.scan.data_quality_deterioration_score', '60', 'Data quality deterioration threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-standard-confused', 'provider_scan_profile_config', 'provider.scan.standard_confused_score', '55', 'Standard profile confused score threshold', 'v1.0', TRUE, 5),
('cfg-provider-scan-high-hold', 'provider_scan_profile_config', 'provider.scan.high_min_hold_seconds', '300', 'High profile minimum hold seconds', 'v1.0', TRUE, 5),
('cfg-provider-scan-emergency-hold', 'provider_scan_profile_config', 'provider.scan.emergency_min_hold_seconds', '120', 'Emergency profile minimum hold seconds', 'v1.0', TRUE, 5),
('cfg-provider-scan-recovery-cycles', 'provider_scan_profile_config', 'provider.scan.recovery_confirm_cycles', '2', 'Recovery cycles before downgrade', 'v1.0', TRUE, 5),
('cfg-provider-scan-downgrade-cooldown', 'provider_scan_profile_config', 'provider.scan.downgrade_cooldown_seconds', '300', 'Profile downgrade cooldown seconds', 'v1.0', TRUE, 5),
('cfg-deriv-oi-5m-weak', 'derivatives_evidence_config', 'derivatives_evidence_config.oi_change_5m_weak', '0.02', '5m OI weak-change threshold', 'v1.0', TRUE, 6),
('cfg-deriv-oi-5m-strong', 'derivatives_evidence_config', 'derivatives_evidence_config.oi_change_5m_strong', '0.05', '5m OI strong-change threshold', 'v1.0', TRUE, 6),
('cfg-deriv-oi-15m-weak', 'derivatives_evidence_config', 'derivatives_evidence_config.oi_change_15m_weak', '0.04', '15m OI weak-change threshold', 'v1.0', TRUE, 6),
('cfg-deriv-oi-15m-strong', 'derivatives_evidence_config', 'derivatives_evidence_config.oi_change_15m_strong', '0.10', '15m OI strong-change threshold', 'v1.0', TRUE, 6),
('cfg-deriv-funding-positive', 'derivatives_evidence_config', 'derivatives_evidence_config.funding_positive_extreme', '0.0005', 'Positive funding extreme threshold', 'v1.0', TRUE, 6),
('cfg-deriv-funding-negative', 'derivatives_evidence_config', 'derivatives_evidence_config.funding_negative_extreme', '-0.0005', 'Negative funding extreme threshold', 'v1.0', TRUE, 6),
('cfg-deriv-long-crowding', 'derivatives_evidence_config', 'derivatives_evidence_config.long_short_long_crowding', '1.20', 'Long crowding ratio threshold', 'v1.0', TRUE, 6),
('cfg-deriv-short-crowding', 'derivatives_evidence_config', 'derivatives_evidence_config.long_short_short_crowding', '0.80', 'Short crowding ratio threshold', 'v1.0', TRUE, 6),
('cfg-deriv-liquidation-5m', 'derivatives_evidence_config', 'derivatives_evidence_config.liquidation_spike_5m', '1000000', '5m liquidation spike USD threshold', 'v1.0', TRUE, 6),
('cfg-deriv-liquidation-15m', 'derivatives_evidence_config', 'derivatives_evidence_config.liquidation_spike_15m', '3000000', '15m liquidation spike USD threshold', 'v1.0', TRUE, 6),
('cfg-deriv-liquidation-imbalance', 'derivatives_evidence_config', 'derivatives_evidence_config.liquidation_imbalance_ratio', '2.0', 'Liquidation imbalance ratio threshold', 'v1.0', TRUE, 6),
('cfg-deriv-exchange-concentration', 'derivatives_evidence_config', 'derivatives_evidence_config.exchange_concentration_high', '0.70', 'Exchange concentration high ratio threshold (0.70 = 70%)', 'v1.0', TRUE, 6),
('cfg-deriv-max-age', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_max_data_age_seconds', '120', 'Maximum derivatives data age in seconds', 'v1.0', TRUE, 6),
('cfg-deriv-required-confirm', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_required_for_confirm', 'true', 'Require OI and Funding for confirm paths', 'v1.0', TRUE, 6),
('cfg-deriv-min-datasets', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_minimum_dataset_count', '2', 'Minimum derivatives dataset count', 'v1.0', TRUE, 6),
('cfg-deriv-partial-penalty', 'derivatives_score_config', 'derivatives_score_config.derivatives_partial_confidence_penalty', '15', 'Partial derivatives confidence penalty', 'v1.0', TRUE, 6),
('cfg-deriv-stale-penalty', 'derivatives_score_config', 'derivatives_score_config.derivatives_stale_confidence_penalty', '30', 'Stale derivatives confidence penalty', 'v1.0', TRUE, 6),
('cfg-deriv-score-cap', 'derivatives_score_config', 'derivatives_score_config.derivatives_score_cap', '20', 'Maximum absolute derivatives contribution for non-trend scores', 'v1.0', TRUE, 6),
('cfg-deriv-trend-score-cap', 'derivatives_score_config', 'derivatives_score_config.derivatives_trend_score_cap', '5', 'Maximum absolute derivatives contribution for trend score', 'v1.0', TRUE, 6),
('cfg-deriv-min-data-quality', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_min_data_quality_score', '60', 'Minimum data quality for derivatives-confirmed opportunity', 'v1.0', TRUE, 6),
('cfg-deriv-eight-score-cap', 'derivatives_decision_config', 'derivatives_decision_config.eight_score_adjustment_cap', '10', 'Maximum absolute eight-score adjustment to rule decision score', 'v1.0', TRUE, 6),
('cfg-deriv-eight-score-factor', 'derivatives_decision_config', 'derivatives_decision_config.eight_score_adjustment_factor_percent', '20', 'Eight-score decision adjustment factor percent', 'v1.0', TRUE, 6),
('cfg-deriv-high-risk-downgrade', 'derivatives_decision_config', 'derivatives_decision_config.derivatives_high_risk_plan_downgrade', 'true', 'Downgrade plan mode under high derivatives risk', 'v1.0', TRUE, 6),
('cfg-deriv-monitor-refresh', 'derivatives_monitor_config', 'derivatives_monitor_config.refresh_seconds', '60', 'Cached derivatives monitor cadence', 'v1.0', TRUE, 6);

DO $$
DECLARE
    mismatch_count integer;
BEGIN
    WITH expected AS (
        SELECT rule_id, rule_type, rule_key, rule_value, description, version, enabled
        FROM p3h_expected_rule_defaults, p3h_contract_input
        WHERE min_flyway_version <= expected_version
    ), differences AS (
        (SELECT * FROM expected
         EXCEPT
         SELECT rule_id, rule_type, rule_key, rule_value, description, version, enabled
         FROM tm_rule_config)
        UNION ALL
        (SELECT rule_id, rule_type, rule_key, rule_value, description, version, enabled
         FROM tm_rule_config
         EXCEPT
         SELECT * FROM expected)
    )
    SELECT count(*) INTO mismatch_count FROM differences;

    IF mismatch_count <> 0 THEN
        RAISE EXCEPTION 'P3-H exact versioned rule-default contract mismatch';
    END IF;
END
$$;

COMMIT;
