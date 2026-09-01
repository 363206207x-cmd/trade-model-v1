package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.testsupport.FrozenFinalExecutionPlanTestFixture;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class DecisionResultMapperLatestPlanIntegrationTest {

    @Autowired
    private DecisionResultMapper decisionResultMapper;

    @Autowired
    private ExecutionPlanMapper executionPlanMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findLatestDecisionResultsJoined_doesNotProjectLegacyPlansAsFinal() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-mapper-it-1", "BTCUSDT", "1h", 87);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, valid_period, invalid_condition, data_quality_score, create_time) VALUES (?,?,?,?,?,?, CURRENT_TIMESTAMP)",
                "dec-mapper-it-1", "ana-mapper-it-1", "BTCUSDT", "2h", "decision-invalid", 87);
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, invalid_condition, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?, TIMESTAMP '2020-01-01 00:00:00')",
                "plan-old", "ana-mapper-it-1", "ADVISORY", "OLD_ACTION", "zone-old", "sl-old", "tp-old", "1x", "pos-old", "invalid-old");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, invalid_condition, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-06-01 00:00:00')",
                "plan-new", "ana-mapper-it-1", "SEMI_STRUCTURED", "NEW_ACTION", "zone-new", "sl-new", "tp-new", "5x", "pos-new", "invalid-new");

        List<DecisionResultVO> list = decisionResultMapper.findLatestDecisionResultsJoined(10);
        assertThat(list).hasSize(1);
        DecisionResultVO row = list.get(0);
        assertThat(row.getRecommendedAction()).isNull();
        assertThat(row.getPlanMode()).isNull();
        assertThat(row.getEntryZone()).isNull();
        assertThat(row.getStopLoss()).isNull();
        assertThat(row.getTakeProfitRules()).isNull();
        assertThat(row.getLeverageSuggestion()).isNull();
        assertThat(row.getPositionSuggestion()).isNull();
        assertThat(row.getInvalidCondition()).isNull();
        assertThat(row.getExecutionPlanSummary()).isNull();
        assertThat(row.getDataQualityScore()).isEqualTo(87);
        assertThat(row.getTimeframe()).isEqualTo("1h");
    }

    @Test
    void findLatestDecisionResultBySymbolJoined_ignoresLegacyPlanTieBreaks() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-mapper-it-2", "ETHUSDT", "1h", 73);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, invalid_condition, data_quality_score, create_time) VALUES (?,?,?,?,?, CURRENT_TIMESTAMP)",
                "dec-mapper-it-2", "ana-mapper-it-2", "ETHUSDT", "decision-eth-invalid", 73);
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, invalid_condition, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 12:00:00')",
                "plan-aa", "ana-mapper-it-2", "ADVISORY", "FIRST", "z-a", "sl-a", "tp-a", "lev-a", "pos-a", "invalid-a");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, invalid_condition, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 12:00:00')",
                "plan-zz", "ana-mapper-it-2", "SEMI_STRUCTURED", "SECOND", "z-z", "sl-z", "tp-z", "lev-z", "pos-z", "invalid-z");

        DecisionResultVO row = decisionResultMapper.findLatestDecisionResultBySymbolJoined("ETHUSDT");
        assertThat(row).isNotNull();
        assertThat(row.getRecommendedAction()).isNull();
        assertThat(row.getPlanMode()).isNull();
        assertThat(row.getEntryZone()).isNull();
        assertThat(row.getStopLoss()).isNull();
        assertThat(row.getTakeProfitRules()).isNull();
        assertThat(row.getLeverageSuggestion()).isNull();
        assertThat(row.getPositionSuggestion()).isNull();
        assertThat(row.getInvalidCondition()).isNull();
        assertThat(row.getExecutionPlanSummary()).isNull();
        assertThat(row.getDataQualityScore()).isEqualTo(73);
        assertThat(row.getTimeframe()).isEqualTo("1h");
    }

    @Test
    void findLatestDecisionResultBySymbolJoined_neverFallsBackAcrossDecisionAndPlanSemantics() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-mapper-it-3", "SOLUSDT", "15m", 66);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, valid_period, invalid_condition, create_time) VALUES (?,?,?,?,?, CURRENT_TIMESTAMP)",
                "dec-mapper-it-3", "ana-mapper-it-3", "SOLUSDT", "2h", "decision-fallback-invalid");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, invalid_condition, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-06-01 00:00:00')",
                "plan-blank-invalid", "ana-mapper-it-3", "SEMI_STRUCTURED", "SOL_ACTION", "sol-zone", "sol-stop", "sol-tp", "sol-lev", "sol-pos", "   ");

        DecisionResultVO row = decisionResultMapper.findLatestDecisionResultBySymbolJoined("SOLUSDT");

        assertThat(row).isNotNull();
        assertThat(row.getEntryZone()).isNull();
        assertThat(row.getInvalidCondition()).isNull();
        assertThat(row.getExecutionPlanSummary()).isNull();
    }

    @Test
    void opportunityRankingReadReturnsLatestDecisionRealScoreAndOnlyValidatedFinalPlan() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) "
                        + "VALUES (?,?,?, TIMESTAMP '2025-01-01 00:00:00', ?)",
                "analysis-rank-btc-old", "BTCUSDT", "5m", 70);
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) "
                        + "VALUES (?,?,?, TIMESTAMP '2026-01-01 00:00:00', ?)",
                "analysis-rank-btc", "BTCUSDT", "5m", 94);
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) "
                        + "VALUES (?,?,?, TIMESTAMP '2026-01-01 00:00:00', ?)",
                "analysis-rank-link", "LINKUSDT", "1h", 88);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, confidence_level, risk_level, "
                        + "ai_conflict_level, data_quality_score, opportunity_score, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 00:00:00')",
                "decision-rank-btc-old", "analysis-rank-btc-old", "BTCUSDT", "LOW", "HIGH",
                "LEVEL_3_SIGNIFICANT_DISAGREEMENT", 70, 60);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, confidence_level, risk_level, "
                        + "ai_conflict_level, data_quality_score, opportunity_score, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?, TIMESTAMP '2026-01-01 00:00:00')",
                "decision-rank-btc", "analysis-rank-btc", "BTCUSDT", "HIGH", "LOW",
                "LEVEL_1_CONSISTENT", 94, 90);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, confidence_level, risk_level, "
                        + "ai_conflict_level, data_quality_score, opportunity_score, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?, TIMESTAMP '2026-01-01 00:00:00')",
                "decision-rank-link", "analysis-rank-link", "LINKUSDT", "MEDIUM", "MEDIUM",
                "LEVEL_2_MINOR_DISAGREEMENT", 88, 75);
        insertValidatedFinalPlan("analysis-rank-btc", "BTCUSDT", "5m",
                "plan-rank-btc", "CONFIRMATION", "FINAL_VALIDATED");
        insertValidatedFinalPlan("analysis-rank-link", "LINKUSDT", "1h",
                "plan-rank-link", "PREPARATION", "FINAL_VALIDATED");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, chain_status, "
                        + "rule_validation_status, final_plan, create_time) "
                        + "VALUES (?,?,?,?,?,?, TIMESTAMP '2026-02-01 00:00:00')",
                "plan-rank-btc-newer-blocked", "analysis-rank-btc", "BLOCKED",
                "RULE_VALIDATION_BLOCKED", "BLOCKED", false);
        jdbcTemplate.update(
                "INSERT INTO tm_score_item(score_id, analysis_id, score_type, score_value) VALUES (?,?,?,?)",
                "score-rank-btc-1", "analysis-rank-btc", "TREND", 80D);
        jdbcTemplate.update(
                "INSERT INTO tm_score_item(score_id, analysis_id, score_type, score_value) VALUES (?,?,?,?)",
                "score-rank-btc-2", "analysis-rank-btc", "STRUCTURE", 100D);
        jdbcTemplate.update(
                "INSERT INTO tm_score_item(score_id, analysis_id, score_type, score_value) VALUES (?,?,?,?)",
                "score-rank-link", "analysis-rank-link", "TREND", 75D);

        List<DecisionResultVO> rows = decisionResultMapper
                .findLatestDecisionResultsForSymbolsJoined(
                        List.of("BTCUSDT", "LINKUSDT"), "SYSTEM", 0L);

        assertThat(rows).hasSize(2);
        DecisionResultVO btc = rows.stream()
                .filter(row -> "BTCUSDT".equals(row.getSymbol()))
                .findFirst().orElseThrow();
        assertThat(btc.getAnalysisId()).isEqualTo("analysis-rank-btc");
        assertThat(btc.getPlanMode()).isEqualTo("CONFIRMATION");
        assertThat(btc.getDataQualityScore()).isEqualTo(94);
        assertThat(btc.getOpportunityScore()).isEqualTo(90D);
        assertThat(btc.getAnalysisTime()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(rows).extracting(DecisionResultVO::getSymbol)
                .containsExactlyInAnyOrder("BTCUSDT", "LINKUSDT");
    }

    @Test
    void opportunityRankingReadExcludesOtherUsersAndUsesLatestOwnedOrSystemAnalysis() {
        insertOwnedAnalysis("analysis-rank-system", "SYSTEM", 0L, "2026-01-01 00:00:00", 80);
        insertOwnedAnalysis("analysis-rank-user-41", "USER", 41L, "2026-01-02 00:00:00", 90);
        insertOwnedAnalysis("analysis-rank-user-99", "USER", 99L, "2026-01-03 00:00:00", 99);
        insertRankingDecision("analysis-rank-system", "decision-rank-system");
        insertRankingDecision("analysis-rank-user-41", "decision-rank-user-41");
        insertRankingDecision("analysis-rank-user-99", "decision-rank-user-99");
        insertValidatedFinalPlan("analysis-rank-system", "UNIUSDT", "5m",
                "plan-rank-system", "OBSERVATION", "FINAL_VALIDATED");
        insertValidatedFinalPlan("analysis-rank-user-41", "UNIUSDT", "15m",
                "plan-rank-user-41", "PREPARATION", "FINAL_VALIDATED");
        insertValidatedFinalPlan("analysis-rank-user-99", "UNIUSDT", "1h",
                "plan-rank-user-99", "CONFIRMATION", "FINAL_VALIDATED");

        List<DecisionResultVO> userRows = decisionResultMapper
                .findLatestDecisionResultsForSymbolsJoined(List.of("UNIUSDT"), "USER", 41L);
        List<DecisionResultVO> systemRows = decisionResultMapper
                .findLatestDecisionResultsForSymbolsJoined(List.of("UNIUSDT"), "SYSTEM", 0L);

        assertThat(userRows).singleElement()
                .extracting(DecisionResultVO::getAnalysisId).isEqualTo("analysis-rank-user-41");
        assertThat(systemRows).singleElement()
                .extracting(DecisionResultVO::getAnalysisId).isEqualTo("analysis-rank-system");
    }

    @Test
    void observationFinalPersistsWithoutDirectionalParametersAndRejectsExecutionLeakage() {
        insertOwnedAnalysis("analysis-observation-boundary", "SYSTEM", 0L,
                "2026-01-01 00:00:00", 86);
        insertValidatedFinalPlan("analysis-observation-boundary", "UNIUSDT", "5m",
                "plan-observation-boundary", "OBSERVATION", "FINAL_VALIDATED");

        ExecutionPlanDO persisted = executionPlanMapper.selectByPlanId("plan-observation-boundary");
        assertThat(persisted).isNotNull();
        assertThat(persisted.getFinalPlan()).isTrue();
        assertThat(persisted.getFinalPlanMode()).isEqualTo("OBSERVATION");
        assertThat(persisted.getEntryZone()).isNull();
        assertThat(persisted.getStopLoss()).isNull();
        assertThat(persisted.getTakeProfitRules()).isNull();
        assertThat(persisted.getExpectedRiskReward()).isNull();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE tm_execution_plan SET entry_zone=? WHERE plan_id=?",
                "100-101", "plan-observation-boundary"))
                .hasStackTraceContaining("CK_TM_EXECUTION_PLAN_FINAL_BOUNDARY");
    }

    @Test
    void directionalFinalCannotLoseItsOwnedEntrySource() {
        insertOwnedAnalysis("analysis-directional-boundary", "SYSTEM", 0L,
                "2026-01-01 00:00:00", 92);
        insertValidatedFinalPlan("analysis-directional-boundary", "UNIUSDT", "5m",
                "plan-directional-boundary", "PREPARATION", "FINAL_VALIDATED");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE tm_execution_plan SET entry_source=NULL WHERE plan_id=?",
                "plan-directional-boundary"))
                .hasStackTraceContaining("CK_TM_EXECUTION_PLAN_FINAL_BOUNDARY");
    }

    private void insertOwnedAnalysis(String analysisId, String ownerType, Long ownerId,
                                     String analysisTime, int quality) {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score, "
                        + "owner_type, owner_id) VALUES (?,?,?,CAST(? AS TIMESTAMP),?,?,?)",
                analysisId, "UNIUSDT", "5m", analysisTime, quality, ownerType, ownerId);
    }

    private void insertRankingDecision(String analysisId, String decisionId) {
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, confidence_level, risk_level, "
                        + "ai_conflict_level, create_time) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                decisionId, analysisId, "UNIUSDT", "HIGH", "LOW", "LEVEL_1_CONSISTENT");
    }

    private void insertValidatedFinalPlan(String analysisId,
                                          String symbol,
                                          String timeframe,
                                          String planId,
                                          String planMode,
                                          String chainStatus) {
        String opportunityId = "opportunity-" + planId;
        String candidateId = "candidate-" + planId;
        String resolverId = "resolver-" + planId;
        String traceId = "trace-" + planId;
        Timestamp createdAt = Timestamp.valueOf("2026-01-01 00:00:00");
        Timestamp freshUntil = Timestamp.valueOf("2026-01-01 01:00:00");
        long accountRiskSnapshotId = insertVerifiedAccountRiskSnapshot(
                analysisId, symbol, traceId, createdAt, freshUntil);
        jdbcTemplate.update("""
                INSERT INTO tm_asset_state(
                  symbol, timeframe, state, opportunity_id, state_entered_at,
                  last_analysis_id, last_update_time, trace_id
                ) VALUES (?, ?, 'CANDIDATE', ?, ?, ?, ?, ?)
                """, symbol, timeframe, opportunityId, createdAt, analysisId, createdAt, traceId);
        jdbcTemplate.update("""
                INSERT INTO tm_execution_plan_candidate(
                  candidate_id, opportunity_id, analysis_id, trace_id,
                  rule_direction, rule_confidence, rule_risk, candidate_direction,
                  plan_mode, confidence_level, risk_level, worth_opening,
                  account_risk_snapshot_id, candidate_source, candidate_status, payload_json, created_at
                ) VALUES (?, ?, ?, ?, 'BULLISH', 'HIGH', 'LOW', 'BULLISH',
                  ?, 'HIGH', 'LOW', TRUE, ?, 'GPT_FINAL', 'VALIDATED', '{}', ?)
                """, candidateId, opportunityId, analysisId, traceId, planMode,
                accountRiskSnapshotId, createdAt);
        jdbcTemplate.update("""
                INSERT INTO tm_conflict_resolver_result(
                  resolver_result_id, candidate_id, analysis_id, trace_id,
                  rule_direction, rule_confidence, rule_risk,
                  gemini_review_json, grok_challenge_json, conflict_level, conflict_score,
                  plan_mode_before, plan_mode_after, confidence_before, confidence_after,
                  risk_before, risk_after, confused_decision, rule_direction_preserved, created_at
                ) VALUES (?, ?, ?, ?, 'BULLISH', 'HIGH', 'LOW', '{}', '{}',
                  'LEVEL_1_CONSISTENT', 0, ?, ?, 'HIGH', 'HIGH', 'LOW', 'LOW', FALSE, TRUE, ?)
                """, resolverId, candidateId, analysisId, traceId, planMode, planMode, createdAt);
        ExecutionPlanDO finalPlan = FrozenFinalExecutionPlanTestFixture.complete(
                planId, analysisId, createdAt.toLocalDateTime());
        finalPlan.setOpportunityId(opportunityId);
        finalPlan.setCandidateId(candidateId);
        finalPlan.setResolverResultId(resolverId);
        finalPlan.setTraceId(traceId);
        finalPlan.setValidationResultId("validation-" + planId);
        finalPlan.setPlanMode(planMode);
        finalPlan.setCandidatePlanMode(planMode);
        finalPlan.setFinalPlanMode(planMode);
        if ("OBSERVATION".equals(planMode)) {
            clearDirectionalParameters(finalPlan);
        }
        finalPlan.setChainStatus(chainStatus);
        finalPlan.setAccountRiskSnapshotId(accountRiskSnapshotId);
        finalPlan.setCreateTime(createdAt.toLocalDateTime());
        executionPlanMapper.insert(finalPlan);
    }

    private void clearDirectionalParameters(ExecutionPlanDO plan) {
        plan.setEntryLogic(null);
        plan.setEntryZone(null);
        plan.setEntrySource(null);
        plan.setEntryReason(null);
        plan.setTriggerCondition(null);
        plan.setStopLogic(null);
        plan.setStopLoss(null);
        plan.setStopSource(null);
        plan.setStopReason(null);
        plan.setTargetLogic(null);
        plan.setTakeProfitRules(null);
        plan.setTargetSource(null);
        plan.setTargetReason(null);
        plan.setAddPositionCondition(null);
        plan.setReducePositionCondition(null);
        plan.setAbandonCondition(null);
        plan.setInvalidCondition(null);
        plan.setInvalidationSource(null);
        plan.setInvalidationReason(null);
        plan.setLeverageSuggestion(null);
        plan.setLeverageLimit(null);
        plan.setPositionSuggestion(null);
        plan.setPositionLimit(null);
        plan.setExpectedRiskReward(null);
        plan.setExpectedRiskRewardSource(null);
        plan.setExpectedRiskRewardReason(null);
    }

    private long insertVerifiedAccountRiskSnapshot(String analysisId,
                                                   String symbol,
                                                   String traceId,
                                                   Timestamp observedAt,
                                                   Timestamp freshUntil) {
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
                """, analysisId, symbol, observedAt, freshUntil, traceId, observedAt);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tm_account_risk_snapshot WHERE analysis_id=? ORDER BY id DESC LIMIT 1",
                Long.class, analysisId);
    }

    @Test
    void analysisOnlyMultiplePlansDoNotResolveAndExactPlanIdStillReturnsA() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score, trace_id) "
                        + "VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?, ?)",
                "ana-source-plan-it", "BTCUSDT", "1h", 90, "trace-source-plan-it");
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, valid_period, invalid_condition, create_time) "
                        + "VALUES (?,?,?,?,?, CURRENT_TIMESTAMP)",
                "dec-source-plan-it", "ana-source-plan-it", "BTCUSDT", "source-validity", "decision-invalid");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, entry_zone, stop_loss, take_profit_rules, "
                        + "leverage_suggestion, position_suggestion, invalid_condition, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 00:00:00')",
                "plan-source-A", "ana-source-plan-it", "ADVISORY", "A-entry", "A-stop", "A-tp",
                "A-leverage", "A-position", "A-invalid");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, entry_zone, stop_loss, take_profit_rules, "
                        + "leverage_suggestion, position_suggestion, invalid_condition, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2026-01-01 00:00:00')",
                "plan-latest-B", "ana-source-plan-it", "SEMI_STRUCTURED", "B-entry", "B-stop", "B-tp",
                "B-leverage", "B-position", "B-invalid");

        assertThat(executionPlanMapper.selectOnlyByAnalysisId("ana-source-plan-it")).isNull();

        ExecutionPlanDO exactPlan = executionPlanMapper.selectByPlanId("plan-source-A");
        assertThat(exactPlan).isNotNull();
        assertThat(exactPlan.getPlanId()).isEqualTo("plan-source-A");
        assertThat(exactPlan.getEntryZone()).isEqualTo("A-entry");

        DecisionResultVO row = decisionResultMapper.findByAnalysisIdAndPlanIdJoined(
                "ana-source-plan-it", "plan-source-A");

        assertThat(row).isNotNull();
        assertThat(row.getAnalysisId()).isEqualTo("ana-source-plan-it");
        assertThat(row.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(row.getEntryZone()).isEqualTo("A-entry");
        assertThat(row.getStopLoss()).isEqualTo("A-stop");
        assertThat(row.getTakeProfitRules()).isEqualTo("A-tp");
        assertThat(row.getEntryZone()).isNotEqualTo("B-entry");
        assertThat(row.getInvalidCondition()).isEqualTo("A-invalid");
    }

    @Test
    void countOpenSymbolsWithReverseSignal_recognizesStrongAndWeakDirectionFamilies() {
        jdbcTemplate.update(
                "INSERT INTO tm_real_position(position_id, symbol, position_side, position_status) VALUES (?,?,?,?)",
                "position-strong-bear", "BTCUSDT", "LONG", "OPEN");
        jdbcTemplate.update(
                "INSERT INTO tm_real_position(position_id, symbol, position_side, position_status) VALUES (?,?,?,?)",
                "position-weak-bull", "ETHUSDT", "SHORT", "OPEN");
        jdbcTemplate.update(
                "INSERT INTO tm_real_position(position_id, symbol, position_side, position_status) VALUES (?,?,?,?)",
                "position-range", "SOLUSDT", "LONG", "OPEN");
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, market_bias_hierarchy, create_time) VALUES (?,?,?,?, CURRENT_TIMESTAMP)",
                "decision-strong-bear", "analysis-strong-bear", "BTCUSDT", "STRONG_BEARISH");
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, market_bias_hierarchy, create_time) VALUES (?,?,?,?, CURRENT_TIMESTAMP)",
                "decision-weak-bull", "analysis-weak-bull", "ETHUSDT", "WEAK_BULLISH");
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, market_bias_hierarchy, create_time) VALUES (?,?,?,?, CURRENT_TIMESTAMP)",
                "decision-range", "analysis-range", "SOLUSDT", "RANGE");

        assertThat(decisionResultMapper.countOpenSymbolsWithReverseSignal()).isEqualTo(2);
    }
}
