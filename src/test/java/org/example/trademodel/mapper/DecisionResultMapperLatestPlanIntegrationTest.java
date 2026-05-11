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

import java.util.Arrays;
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
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, create_time) VALUES (?,?,?, CURRENT_TIMESTAMP)",
                "dec-mapper-it-1", "ana-mapper-it-1", "BTCUSDT");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2020-01-01 00:00:00')",
                "plan-old", "ana-mapper-it-1", "ADVISORY", "OLD_ACTION", "zone-old", "sl-old", "tp-old", "1x", "pos-old");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-06-01 00:00:00')",
                "plan-new", "ana-mapper-it-1", "SEMI_STRUCTURED", "NEW_ACTION", "zone-new", "sl-new", "tp-new", "5x", "pos-new");

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
        assertThat(row.getDataQualityScore()).isEqualTo(87);
    }

    @Test
    void findLatestDecisionResultBySymbolJoined_usesLatestPlanAndTieBreaksByPlanIdDesc() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-mapper-it-2", "ETHUSDT", "1h", 73);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, create_time) VALUES (?,?,?, CURRENT_TIMESTAMP)",
                "dec-mapper-it-2", "ana-mapper-it-2", "ETHUSDT");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 12:00:00')",
                "plan-aa", "ana-mapper-it-2", "ADVISORY", "FIRST", "z-a", "sl-a", "tp-a", "lev-a", "pos-a");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 12:00:00')",
                "plan-zz", "ana-mapper-it-2", "SEMI_STRUCTURED", "SECOND", "z-z", "sl-z", "tp-z", "lev-z", "pos-z");

        DecisionResultVO row = decisionResultMapper.findLatestDecisionResultBySymbolJoined("ETHUSDT");
        assertThat(row).isNotNull();
        assertThat(row.getRecommendedAction()).isEqualTo("SECOND");
        assertThat(row.getPlanMode()).isEqualTo("SEMI_STRUCTURED");
        assertThat(row.getEntryZone()).isEqualTo("z-z");
        assertThat(row.getStopLoss()).isEqualTo("sl-z");
        assertThat(row.getTakeProfitRules()).isEqualTo("tp-z");
        assertThat(row.getLeverageSuggestion()).isEqualTo("lev-z");
        assertThat(row.getPositionSuggestion()).isEqualTo("pos-z");
        assertThat(row.getDataQualityScore()).isEqualTo(73);
    }

    @Test
    void selectLatestByAnalysisIdTieBreak_matchesWindowSemanticsWhenCreateTimeTied() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-mapper-it-tie", "ETHUSDT", "1h", 73);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, create_time) VALUES (?,?,?, CURRENT_TIMESTAMP)",
                "dec-mapper-it-tie", "ana-mapper-it-tie", "ETHUSDT");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 12:00:00')",
                "plan-aa", "ana-mapper-it-tie", "ADVISORY", "FIRST", "z-a", "sl-a", "tp-a", "lev-a", "pos-a");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 12:00:00')",
                "plan-zz", "ana-mapper-it-tie", "SEMI_STRUCTURED", "SECOND", "z-z", "sl-z", "tp-z", "lev-z", "pos-z");

        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-mapper-it-tie");
        assertThat(plan).isNotNull();
        assertThat(plan.getPlanId()).isEqualTo("plan-zz");
        assertThat(plan.getRecommendedAction()).isEqualTo("SECOND");
    }

    @Test
    void findLatestDecisionResultBaseBySymbol_doesNotFetchPlanOrRunColumns() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-mapper-base-only", "BTCUSDT", "1h", 99);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, create_time) VALUES (?,?,?, CURRENT_TIMESTAMP)",
                "dec-mapper-base-only", "ana-mapper-base-only", "BTCUSDT");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, CURRENT_TIMESTAMP)",
                "plan-only", "ana-mapper-base-only", "ADVISORY", "SHOULD_NOT_APPEAR", "z", "sl", "tp", "1x", "pos");

        DecisionResultVO base = decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT");
        assertThat(base).isNotNull();
        assertThat(base.getAnalysisId()).isEqualTo("ana-mapper-base-only");
        assertThat(base.getRecommendedAction()).isNull();
        assertThat(base.getDataQualityScore()).isNull();
    }

    @Test
    void findLatestDecisionResultsBase_doesNotIncludeJoinedPlanOrRunColumns() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-base-list-1", "BTCUSDT", "1h", 87);
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, create_time) VALUES (?,?,?, CURRENT_TIMESTAMP)",
                "dec-base-list-1", "ana-base-list-1", "BTCUSDT");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, CURRENT_TIMESTAMP)",
                "plan-base-list", "ana-base-list-1", "ADVISORY", "NOT_FROM_JOIN", "z", "sl", "tp", "1x", "pos");

        List<DecisionResultVO> list = decisionResultMapper.findLatestDecisionResultsBase(10);
        assertThat(list.stream().filter(r -> "ana-base-list-1".equals(r.getAnalysisId())).findFirst()).isPresent();
        DecisionResultVO row = list.stream().filter(r -> "ana-base-list-1".equals(r.getAnalysisId())).findFirst().orElseThrow();
        assertThat(row.getRecommendedAction()).isNull();
        assertThat(row.getDataQualityScore()).isNull();
    }

    @Test
    void selectLatestByAnalysisIdsTieBreak_prefersHigherPlanIdWhenCreateTimeTied() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-batch-tie", "ETHUSDT", "1h", 73);
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 12:00:00')",
                "plan-batch-aa", "ana-batch-tie", "ADVISORY", "FIRST", "z-a", "sl-a", "tp-a", "lev-a", "pos-a");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-01-01 12:00:00')",
                "plan-batch-zz", "ana-batch-tie", "SEMI_STRUCTURED", "SECOND", "z-z", "sl-z", "tp-z", "lev-z", "pos-z");

        List<ExecutionPlanDO> plans =
                executionPlanMapper.selectLatestByAnalysisIdsTieBreak(List.of("ana-batch-tie"));
        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getPlanId()).isEqualTo("plan-batch-zz");
        assertThat(plans.get(0).getRecommendedAction()).isEqualTo("SECOND");
    }

    @Test
    void selectLatestByAnalysisIdsTieBreak_returnsLatestPerAnalysisId() {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-batch-a", "BTCUSDT", "1h", 10);
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) VALUES (?,?,?, TIMESTAMP '2025-01-02 00:00:00', ?)",
                "ana-batch-b", "ETHUSDT", "1h", 20);
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2020-01-01 00:00:00')",
                "p-a-old", "ana-batch-a", "A", "OLD_A", "z", "sl", "tp", "1x", "pos");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-06-01 00:00:00')",
                "p-a-new", "ana-batch-a", "A", "NEW_A", "z", "sl", "tp", "1x", "pos");
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?, TIMESTAMP '2025-06-01 00:00:00')",
                "p-b-only", "ana-batch-b", "B", "ONLY_B", "z", "sl", "tp", "1x", "pos");

        List<ExecutionPlanDO> plans =
                executionPlanMapper.selectLatestByAnalysisIdsTieBreak(Arrays.asList("ana-batch-a", "ana-batch-b"));
        assertThat(plans).hasSize(2);
        assertThat(plans.stream().filter(p -> "ana-batch-a".equals(p.getAnalysisId())).findFirst().orElseThrow().getRecommendedAction())
                .isEqualTo("NEW_A");
        assertThat(plans.stream().filter(p -> "ana-batch-b".equals(p.getAnalysisId())).findFirst().orElseThrow().getRecommendedAction())
                .isEqualTo("ONLY_B");
    }
}
