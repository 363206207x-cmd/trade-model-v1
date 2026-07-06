INSERT INTO tm_rule_config(rule_id, rule_type, rule_key, rule_value, description, version, enabled) VALUES
('cfg-hot-reset-price-move', 'hot_reset_config', 'hot_reset_config.extreme_price_move_ratio_threshold', '0.08', 'Hot Reset extreme price move ratio threshold', 'v1.0', TRUE),
('cfg-hot-reset-oi-collapse', 'hot_reset_config', 'hot_reset_config.oi_collapse_change_ratio_threshold', '-0.30', 'Hot Reset open-interest collapse threshold', 'v1.0', TRUE),
('cfg-hot-reset-liquidity-drain', 'hot_reset_config', 'hot_reset_config.liquidity_drain_change_ratio_threshold', '-0.40', 'Hot Reset liquidity drain threshold', 'v1.0', TRUE),
('cfg-hot-reset-systemic-severity', 'hot_reset_config', 'hot_reset_config.systemic_shock_severity_threshold', '85', 'Hot Reset systemic shock severity threshold', 'v1.0', TRUE)
ON CONFLICT (rule_key) DO UPDATE SET rule_type = EXCLUDED.rule_type, rule_value = EXCLUDED.rule_value, description = EXCLUDED.description, version = EXCLUDED.version, enabled = EXCLUDED.enabled;

INSERT INTO tm_rule_config(rule_id, rule_type, rule_key, rule_value, description, version, enabled) VALUES
('cfg-confused-enter-threshold', 'confused_state_config', 'confused_state_config.enter_threshold', '70', 'Confused state enter threshold', 'v1.0', TRUE),
('cfg-confused-push-block-threshold', 'confused_state_config', 'confused_state_config.directional_push_block_threshold', '85', 'Directional push block threshold for Confused state', 'v1.0', TRUE),
('cfg-confused-exit-threshold', 'confused_state_config', 'confused_state_config.exit_threshold_exclusive', '55', 'Confused state exit threshold exclusive', 'v1.0', TRUE),
('cfg-confused-exit-cycles', 'confused_state_config', 'confused_state_config.exit_required_consecutive_cycles', '2', 'Required consecutive low cycles before Confused exit', 'v1.0', TRUE)
ON CONFLICT (rule_key) DO UPDATE SET rule_type = EXCLUDED.rule_type, rule_value = EXCLUDED.rule_value, description = EXCLUDED.description, version = EXCLUDED.version, enabled = EXCLUDED.enabled;

INSERT INTO tm_rule_config(rule_id, rule_type, rule_key, rule_value, description, version, enabled) VALUES
('cfg-ai-conflict-level1-max', 'ai_conflict_config', 'ai_conflict_config.level1_max_score', '20', 'AI conflict level 1 max score', 'v1.0', TRUE),
('cfg-ai-conflict-level2-max', 'ai_conflict_config', 'ai_conflict_config.level2_max_score', '45', 'AI conflict level 2 max score', 'v1.0', TRUE),
('cfg-ai-conflict-level3-max', 'ai_conflict_config', 'ai_conflict_config.level3_max_score', '70', 'AI conflict level 3 max score', 'v1.0', TRUE),
('cfg-ai-conflict-single-objection-max', 'ai_conflict_config', 'ai_conflict_config.single_objection_max_score', '35', 'Single AI objection score cap', 'v1.0', TRUE)
ON CONFLICT (rule_key) DO UPDATE SET rule_type = EXCLUDED.rule_type, rule_value = EXCLUDED.rule_value, description = EXCLUDED.description, version = EXCLUDED.version, enabled = EXCLUDED.enabled;

INSERT INTO tm_rule_config(rule_id, rule_type, rule_key, rule_value, description, version, enabled) VALUES
('cfg-push-recheck-drift-ratio', 'push_recheck_config', 'push_recheck_config.drift_ratio_threshold', '0.02', 'Push recheck drift ratio threshold', 'v1.0', TRUE),
('cfg-push-recheck-confused-wait', 'push_recheck_config', 'push_recheck_config.confused_wait_threshold', '70', 'Push recheck Confused wait threshold', 'v1.0', TRUE),
('cfg-push-recheck-confused-block', 'push_recheck_config', 'push_recheck_config.confused_block_threshold', '85', 'Push recheck Confused block threshold', 'v1.0', TRUE),
('cfg-push-recheck-exec-feas-wait', 'push_recheck_config', 'push_recheck_config.execution_feasibility_wait_threshold', '60', 'Push recheck execution feasibility wait threshold', 'v1.0', TRUE)
ON CONFLICT (rule_key) DO UPDATE SET rule_type = EXCLUDED.rule_type, rule_value = EXCLUDED.rule_value, description = EXCLUDED.description, version = EXCLUDED.version, enabled = EXCLUDED.enabled;

INSERT INTO tm_rule_config(rule_id, rule_type, rule_key, rule_value, description, version, enabled) VALUES
('cfg-missed-review-window-hours', 'missed_opportunity_config', 'missed_opportunity_config.review_window_hours', '24', 'Missed opportunity review window in hours', 'v1.0', TRUE),
('cfg-missed-min-mfe-ratio', 'missed_opportunity_config', 'missed_opportunity_config.min_mfe_ratio_threshold', '0.01', 'Missed opportunity minimum MFE ratio', 'v1.0', TRUE),
('cfg-missed-max-mae-ratio', 'missed_opportunity_config', 'missed_opportunity_config.max_mae_ratio_threshold', '0.02', 'Missed opportunity maximum MAE ratio', 'v1.0', TRUE)
ON CONFLICT (rule_key) DO UPDATE SET rule_type = EXCLUDED.rule_type, rule_value = EXCLUDED.rule_value, description = EXCLUDED.description, version = EXCLUDED.version, enabled = EXCLUDED.enabled;
