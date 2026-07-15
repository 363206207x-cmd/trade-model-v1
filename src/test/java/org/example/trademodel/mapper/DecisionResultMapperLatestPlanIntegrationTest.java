package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
    void findLatestDecisionResultsJoined_usesLatestExecutionPlanByCreateTime() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-mapper-it-1", "BTCUSDT", "1h", 87);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, valid_period, invalid_condition, create_time) VALUES (?,?,?,?,?, CURRENT_TIMESTAMP)",
                "dec-mapper-it-1", "ana-mapper-it-1", "BTCUSDT", "2h", "decision-invalid");
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
        assertThat(row.getRecommendedAction()).isEqualTo("NEW_ACTION");
        assertThat(row.getPlanMode()).isEqualTo("SEMI_STRUCTURED");
        assertThat(row.getEntryZone()).isEqualTo("zone-new");
        assertThat(row.getStopLoss()).isEqualTo("sl-new");
        assertThat(row.getTakeProfitRules()).isEqualTo("tp-new");
        assertThat(row.getLeverageSuggestion()).isEqualTo("5x");
        assertThat(row.getPositionSuggestion()).isEqualTo("pos-new");
        assertThat(row.getInvalidCondition()).isEqualTo("invalid-new");
        assertThat(row.getExecutionPlanSummary()).isEqualTo("2h | invalid-new");
        assertThat(row.getExecutionPlanSummary()).doesNotContain("invalid-old", "decision-invalid");
        assertThat(row.getDataQualityScore()).isEqualTo(87);
        assertThat(row.getTimeframe()).isEqualTo("1h");
    }

    @Test
    void findLatestDecisionResultBySymbolJoined_usesLatestPlanAndTieBreaksByPlanIdDesc() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-mapper-it-2", "ETHUSDT", "1h", 73);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, invalid_condition, create_time) VALUES (?,?,?,?, CURRENT_TIMESTAMP)",
                "dec-mapper-it-2", "ana-mapper-it-2", "ETHUSDT", "decision-eth-invalid");
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
        assertThat(row.getRecommendedAction()).isEqualTo("SECOND");
        assertThat(row.getPlanMode()).isEqualTo("SEMI_STRUCTURED");
        assertThat(row.getEntryZone()).isEqualTo("z-z");
        assertThat(row.getStopLoss()).isEqualTo("sl-z");
        assertThat(row.getTakeProfitRules()).isEqualTo("tp-z");
        assertThat(row.getLeverageSuggestion()).isEqualTo("lev-z");
        assertThat(row.getPositionSuggestion()).isEqualTo("pos-z");
        assertThat(row.getInvalidCondition()).isEqualTo("invalid-z");
        assertThat(row.getExecutionPlanSummary()).isEqualTo("invalid-z");
        assertThat(row.getDataQualityScore()).isEqualTo(73);
        assertThat(row.getTimeframe()).isEqualTo("1h");
    }

    @Test
    void findLatestDecisionResultBySymbolJoined_fallsBackToDecisionInvalidConditionWhenPlanValueIsBlank() {
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
        assertThat(row.getEntryZone()).isEqualTo("sol-zone");
        assertThat(row.getInvalidCondition()).isEqualTo("decision-fallback-invalid");
        assertThat(row.getExecutionPlanSummary()).isEqualTo("2h | decision-fallback-invalid");
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
