ALTER TABLE tm_decision_result
    ADD COLUMN validated_market_bias VARCHAR(32),
    ADD COLUMN direction_data_state VARCHAR(32),
    ADD COLUMN data_quality_score INT,
    ADD COLUMN evidence_reliability INT,
    ADD COLUMN opportunity_score INT,
    ADD COLUMN risk_score INT,
    ADD COLUMN final_confidence INT,
    ADD COLUMN one_hour_opportunity_quality INT,
    ADD COLUMN four_hour_trend_alignment INT,
    ADD COLUMN normalization_version VARCHAR(64),
    ADD COLUMN score_version VARCHAR(64),
    ADD COLUMN data_quality_version VARCHAR(64),
    ADD COLUMN provider_matrix_version VARCHAR(64);

ALTER TABLE tm_decision_result
    ADD CONSTRAINT ck_tm_decision_validated_bias CHECK (
        validated_market_bias IS NULL OR validated_market_bias IN (
            'STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH',
            'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH'
        )
    ),
    ADD CONSTRAINT ck_tm_decision_direction_data_state CHECK (
        direction_data_state IS NULL OR direction_data_state IN (
            'READY', 'INSUFFICIENT_DATA', 'STALE', 'SOURCE_UNAVAILABLE'
        )
    ),
    ADD CONSTRAINT ck_tm_decision_dq_range CHECK (
        data_quality_score IS NULL OR data_quality_score BETWEEN 0 AND 100
    ),
    ADD CONSTRAINT ck_tm_decision_evidence_reliability_range CHECK (
        evidence_reliability IS NULL OR evidence_reliability BETWEEN 0 AND 100
    ),
    ADD CONSTRAINT ck_tm_decision_opportunity_score_range CHECK (
        opportunity_score IS NULL OR opportunity_score BETWEEN 0 AND 100
    ),
    ADD CONSTRAINT ck_tm_decision_risk_score_range CHECK (
        risk_score IS NULL OR risk_score BETWEEN 0 AND 100
    ),
    ADD CONSTRAINT ck_tm_decision_final_confidence_range CHECK (
        final_confidence IS NULL OR final_confidence BETWEEN 0 AND 100
    ),
    ADD CONSTRAINT ck_tm_decision_one_hour_quality_range CHECK (
        one_hour_opportunity_quality IS NULL OR one_hour_opportunity_quality BETWEEN 0 AND 100
    ),
    ADD CONSTRAINT ck_tm_decision_four_hour_alignment_range CHECK (
        four_hour_trend_alignment IS NULL OR four_hour_trend_alignment BETWEEN 0 AND 100
    );

CREATE INDEX idx_tm_decision_home_contract
    ON tm_decision_result(symbol, final_market_bias, final_plan_mode, create_time DESC);

COMMENT ON COLUMN tm_decision_result.validated_market_bias IS
    'Trusted six-direction bias after DQ/source/structure gates, before the AI review chain';
COMMENT ON COLUMN tm_decision_result.final_confidence IS
    'Deterministic post-Rule-Validation system confidence; never a win probability';
COMMENT ON COLUMN tm_decision_result.opportunity_score IS
    'Deterministic pre-Final opportunity score based on evidence reliability, not final confidence';

ALTER TABLE tm_execution_plan
    DROP CONSTRAINT ck_tm_execution_plan_final_boundary,
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
            AND analysis_timeframes_json IS NOT NULL
            AND trigger_timeframe IS NOT NULL
            AND valid_from IS NOT NULL
            AND valid_until IS NOT NULL
            AND valid_until > valid_from
            AND data_quality BETWEEN 0 AND 100
            AND evidence_refs_json IS NOT NULL
            AND score_refs_json IS NOT NULL
            AND source_status = 'VALID'
            AND finalized_at IS NOT NULL
            AND rule_validation_status = 'PASS'
            AND chain_status = 'FINAL_VALIDATED'
            AND recommended_action IS NOT NULL
            AND risk_explanation IS NOT NULL
            AND (
                (
                    final_plan_mode IN ('CONFIRMATION', 'PREPARATION', 'REDUCED')
                    AND opportunity_type IS NOT NULL
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
                    AND holding_horizon IS NOT NULL
                    AND revalidation_rule IS NOT NULL
                    AND source_refs_json IS NOT NULL
                    AND adjustment_reason IS NOT NULL
                )
                OR
                (
                    final_plan_mode = 'OBSERVATION'
                    AND entry_logic IS NULL
                    AND entry_zone IS NULL AND entry_source IS NULL AND entry_reason IS NULL
                    AND trigger_condition IS NULL
                    AND stop_logic IS NULL
                    AND stop_loss IS NULL AND stop_source IS NULL AND stop_reason IS NULL
                    AND target_logic IS NULL
                    AND take_profit_rules IS NULL AND target_source IS NULL AND target_reason IS NULL
                    AND add_position_condition IS NULL
                    AND reduce_position_condition IS NULL
                    AND abandon_condition IS NULL
                    AND invalid_condition IS NULL
                    AND invalidation_source IS NULL
                    AND invalidation_reason IS NULL
                    AND leverage_suggestion IS NULL
                    AND position_suggestion IS NULL
                    AND leverage_limit IS NULL
                    AND position_limit IS NULL
                    AND expected_risk_reward IS NULL
                    AND expected_risk_reward_source IS NULL
                    AND expected_risk_reward_reason IS NULL
                )
            )
        )
        OR (
            final_plan = FALSE
            AND rule_validation_status <> 'PASS'
        )
    );

ALTER TABLE tm_position_monitor_log
    DROP CONSTRAINT ck_tm_position_monitor_log_entry_logic,
    DROP CONSTRAINT ck_tm_position_monitor_log_trusted_payload;

ALTER TABLE tm_position_monitor_log
    ADD CONSTRAINT ck_tm_position_monitor_log_entry_logic CHECK (
        entry_logic_status IN ('STILL_VALID', 'WEAKENED', 'INVALIDATED', 'NOT_APPLICABLE')
    ),
    ADD CONSTRAINT ck_tm_position_monitor_log_trusted_payload CHECK (
        (
            source_status = 'VERIFIED'
            AND mark_price_source IS NOT NULL
            AND TRIM(mark_price_source) <> ''
            AND entry_logic_status IS NOT NULL
            AND reversal_status IS NOT NULL
            AND risk_change_reason IS NOT NULL
            AND risk_level IS NOT NULL
            AND risk_trend IS NOT NULL
            AND fresh_until > observed_at
            AND (
                (
                    entry_logic_status = 'NOT_APPLICABLE'
                    AND (
                        (monitor_conclusion IS NULL AND suggested_action IS NULL)
                        OR (monitor_conclusion IS NOT NULL AND suggested_action IS NOT NULL)
                    )
                )
                OR (
                    entry_logic_status <> 'NOT_APPLICABLE'
                    AND monitor_conclusion IS NOT NULL
                    AND suggested_action IS NOT NULL
                )
            )
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

COMMENT ON COLUMN tm_position_monitor_log.entry_logic_status IS
    'Original entry-logic state; NOT_APPLICABLE means MANUAL_INDEPENDENT has no recorded thesis contract.';
