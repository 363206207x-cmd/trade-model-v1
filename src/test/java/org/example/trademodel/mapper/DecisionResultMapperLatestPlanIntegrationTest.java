package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
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
        assertThat(row.getTimeframe()).isEqualTo("1h");
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
        assertThat(row.getTimeframe()).isEqualTo("1h");
    }
}
