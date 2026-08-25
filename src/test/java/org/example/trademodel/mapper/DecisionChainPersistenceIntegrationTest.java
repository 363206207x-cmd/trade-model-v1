package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.testsupport.FrozenFinalExecutionPlanTestFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class DecisionChainPersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExecutionPlanMapper executionPlanMapper;

    @Test
    void schemaSeedsSixDefaultAssetsAndEnforcesCandidateResolverFinalRelationships() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tm_asset_pool_item WHERE owner_type='SYSTEM' AND active=TRUE",
                Integer.class)).isEqualTo(6);
        insertAnalysis("analysis-chain-db");
        long accountRiskSnapshotId = insertVerifiedAccountRiskSnapshot(
                "analysis-chain-db", "CHAINDBUSDT", "trace-chain-db");
        jdbcTemplate.update("""
                INSERT INTO tm_asset_state(
                  symbol, state, confused_score, opportunity_id, state_entered_at,
                  last_update_time, trace_id
                ) VALUES (?, 'CANDIDATE', 10, ?, ?, ?, ?)
                """,
                "CHAINDBUSDT", "opp-chain-db", timestamp(), timestamp(), "trace-chain-db");
        jdbcTemplate.update("""
                INSERT INTO tm_execution_plan_candidate(
                  candidate_id, opportunity_id, analysis_id, trace_id,
                  rule_direction, rule_confidence, rule_risk, candidate_direction,
                  plan_mode, confidence_level, risk_level, worth_opening,
                  recommended_action, entry_zone, stop_loss, take_profit_rules,
                  leverage_suggestion, position_suggestion, invalid_condition, validity,
                  account_risk_snapshot_id,
                  summary, candidate_source, candidate_status, payload_json,
                  not_final_plan, not_state_machine_mutation, not_user_position_creation, created_at
                ) VALUES (?, ?, ?, ?, 'BULLISH', 'HIGH', 'MEDIUM', 'BULLISH',
                  'CONFIRMATION', 'HIGH', 'MEDIUM', TRUE,
                  'MANUAL_REVIEW', '100-101', '95', '110 then 120',
                  '1x', 'small', 'close below 95', '2026-08-12T00:00Z', ?,
                  'candidate only', 'GPT_FINAL', 'VALIDATED', '{}', TRUE, TRUE, TRUE, ?)
                """,
                "candidate-chain-db", "opp-chain-db", "analysis-chain-db", "trace-chain-db",
                accountRiskSnapshotId, timestamp());
        jdbcTemplate.update("""
                INSERT INTO tm_conflict_resolver_result(
                  resolver_result_id, candidate_id, analysis_id, trace_id,
                  rule_direction, rule_confidence, rule_risk,
                  gemini_review_json, grok_challenge_json, conflict_level, conflict_score,
                  plan_mode_before, plan_mode_after, confidence_before, confidence_after,
                  risk_before, risk_after, confused_decision, rule_direction_preserved, created_at
                ) VALUES (?, ?, ?, ?, 'BULLISH', 'HIGH', 'MEDIUM',
                  '{}', '{}', 'LEVEL_1_CONSISTENT', 0, 'CONFIRMATION', 'CONFIRMATION', 'HIGH', 'HIGH',
                  'MEDIUM', 'MEDIUM', FALSE, TRUE, ?)
                """,
                "resolver-chain-db", "candidate-chain-db", "analysis-chain-db", "trace-chain-db", timestamp());
        ExecutionPlanDO finalPlan = FrozenFinalExecutionPlanTestFixture.complete(
                "final-plan-chain-db", "analysis-chain-db", timestamp().toLocalDateTime());
        finalPlan.setCandidateId("candidate-chain-db");
        finalPlan.setOpportunityId("opp-chain-db");
        finalPlan.setResolverResultId("resolver-chain-db");
        finalPlan.setTraceId("trace-chain-db");
        finalPlan.setValidationResultId("validation-chain-db");
        finalPlan.setRuleVersion("v4.1");
        finalPlan.setAccountRiskSnapshotId(accountRiskSnapshotId);
        executionPlanMapper.insert(finalPlan);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT candidate_id FROM tm_execution_plan WHERE plan_id='final-plan-chain-db'",
                String.class)).isEqualTo("candidate-chain-db");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT final_plan FROM tm_execution_plan WHERE plan_id='final-plan-chain-db'",
                Boolean.class)).isTrue();
    }

    @Test
    void databaseRejectsCandidateAuthorityEscalationAndUnvalidatedFinalPlan() {
        insertAnalysis("analysis-chain-invalid");
        jdbcTemplate.update("""
                INSERT INTO tm_asset_state(
                  symbol, state, opportunity_id, state_entered_at, last_update_time, trace_id
                ) VALUES ('INVALIDDBUSDT', 'CANDIDATE', 'opp-invalid-db', ?, ?, 'trace-invalid-db')
                """, timestamp(), timestamp());

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tm_execution_plan_candidate(
                  candidate_id, opportunity_id, analysis_id, trace_id,
                  rule_direction, rule_confidence, rule_risk, candidate_direction,
                  plan_mode, confidence_level, risk_level, worth_opening,
                  candidate_source, candidate_status, payload_json,
                  not_final_plan, not_state_machine_mutation, not_user_position_creation
                ) VALUES ('candidate-invalid-db', 'opp-invalid-db', 'analysis-chain-invalid', 'trace-invalid-db',
                  'BULLISH', 'HIGH', 'MEDIUM', 'BULLISH', 'CONFIRMATION', 'HIGH', 'MEDIUM', TRUE,
                  'GPT_FINAL', 'GENERATED', '{}', FALSE, TRUE, TRUE)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tm_execution_plan(
                  plan_id, analysis_id, chain_status, rule_validation_status, final_plan, create_time
                ) VALUES ('unvalidated-final-db', 'analysis-chain-invalid',
                  'FINAL_VALIDATED', 'PASS', TRUE, ?)
                """, timestamp()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void finalPlanCanOnlyBeLinkedByManuallyCreatedUserPosition() {
        schemaSeedsSixDefaultAssetsAndEnforcesCandidateResolverFinalRelationships();
        Long userId = insertUser("decision-chain-position-owner");

        jdbcTemplate.update("""
                INSERT INTO tm_user_position(
                  user_id, asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                  source_type, final_plan_id
                ) VALUES (?, 'CHAINDBUSDT', 'LONG', 'OPEN', 100, 1, 1, ?, 'SYSTEM_PLAN_POSITION', ?)
                """, userId, timestamp(), "final-plan-chain-db");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tm_user_position WHERE final_plan_id='final-plan-chain-db'",
                Integer.class)).isEqualTo(1);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tm_user_position(
                  user_id, asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                  source_type, final_plan_id
                ) VALUES (?, 'CHAINDBUSDT', 'LONG', 'OPEN', 100, 1, 1, ?, 'MANUAL_INDEPENDENT', ?)
                """, userId, timestamp(), "final-plan-chain-db"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long insertUser(String username) {
        jdbcTemplate.update("""
                INSERT INTO tm_user(username, password_hash, created_at, updated_at)
                VALUES (?, '{noop}not-a-real-password', ?, ?)
                """, username, timestamp(), timestamp());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tm_user WHERE username = ?", Long.class, username);
    }

    @Test
    void aiTraceConstraintSeparatesCandidateGenerationFromReviewOnlyRoles() {
        insertAnalysis("analysis-ai-trace-db");
        jdbcTemplate.update("""
                INSERT INTO tm_ai_call_log(
                  call_id, analysis_id, trace_id, provider_name, model_name, ai_role,
                  call_status, started_at, contract_type, candidate_id,
                  review_only, not_execution_plan_creation, not_final_execution_plan_creation
                ) VALUES ('ai-gpt-db', 'analysis-ai-trace-db', 'trace-ai-db', 'OPENAI', 'gpt-test',
                  'GPT_FINAL', 'SUCCESS', ?, 'DECISION_CHAIN_V4_1', 'candidate-ai-db',
                  FALSE, FALSE, TRUE)
                """, timestamp());

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tm_ai_call_log(
                  call_id, analysis_id, trace_id, provider_name, model_name, ai_role,
                  call_status, started_at, contract_type, candidate_id,
                  review_only, not_execution_plan_creation, not_final_execution_plan_creation
                ) VALUES ('ai-gemini-invalid-db', 'analysis-ai-trace-db', 'trace-ai-db', 'GEMINI', 'gemini-test',
                  'GEMINI_REVIEW', 'SUCCESS', ?, 'DECISION_CHAIN_V4_1', 'candidate-ai-db',
                  FALSE, FALSE, TRUE)
                """, timestamp()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertAnalysis(String analysisId) {
        jdbcTemplate.update("""
                INSERT INTO tm_analysis_run(
                  analysis_id, symbol, timeframe, analysis_time, trace_id, status, created_at, updated_at
                ) VALUES (?, 'CHAINDBUSDT', '5m', ?, ?, 'SUCCESS', ?, ?)
                """, analysisId, timestamp(), "trace-" + analysisId, timestamp(), timestamp());
    }

    private long insertVerifiedAccountRiskSnapshot(String analysisId, String symbol, String traceId) {
        jdbcTemplate.update("""
                INSERT INTO tm_account_risk_snapshot(
                  analysis_id, symbol, owner_type, owner_id, account_risk_status,
                  risk_level_snapshot, risk_allowed, risk_reason_code, risk_reason_text,
                  position_exposure, max_allowed_exposure, candidate_leverage, max_allowed_leverage,
                  source_status, observed_at, fresh_until, snapshot_source, snapshot_version,
                  source_note, trace_id, create_time
                ) VALUES (?, ?, 'SYSTEM', 0, 'ALLOWED', 'LOW', TRUE,
                  'CONTROLLED_VERIFIED_ACCOUNT_RISK', 'Controlled integration account-risk fixture',
                  0.10, 0.20, 1.0, 3.0, 'VERIFIED', ?, ?,
                  'CONTROLLED_INTEGRATION_FIXTURE', 1, 'TEST_ONLY_VERIFIED_SOURCE', ?, ?)
                """, analysisId, symbol, timestamp(), freshUntil(), traceId, timestamp());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tm_account_risk_snapshot WHERE analysis_id=? ORDER BY id DESC LIMIT 1",
                Long.class, analysisId);
    }

    private static Timestamp timestamp() {
        return Timestamp.valueOf(LocalDateTime.of(2026, 8, 11, 12, 0));
    }

    private static Timestamp freshUntil() {
        return Timestamp.valueOf(LocalDateTime.of(2026, 8, 11, 13, 0));
    }
}
