package org.example.trademodel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.service.PositionMonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Tag;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
@Tag("core-regression")
class PositionMonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RealPositionMapper realPositionMapper;

    @MockBean
    private MarketQuoteClient marketQuoteClient;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM tm_position_monitor_record");
        jdbcTemplate.update("DELETE FROM tm_execution_plan");
        jdbcTemplate.update("DELETE FROM tm_decision_result");
        jdbcTemplate.update("DELETE FROM tm_analysis_run");
        jdbcTemplate.update("DELETE FROM tm_real_position");

        when(marketQuoteClient.fetch24hTicker(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.<MarketQuoteSnapshot>empty());
    }

    @Test
    void run_manual_open_generates_monitor_record() throws Exception {
        String positionId = createManualOpenPosition("BTCUSDT", "LONG", 50000, 0.01);
        jdbcTemplate.update("DELETE FROM tm_position_monitor_record WHERE position_id = ?", positionId);

        String analysisId = "ana-it-1";
        insertAnalysisRun(analysisId, "BTCUSDT");
        insertDecisionResult(
                "dec-it-1", analysisId, "BTCUSDT",
                "BULLISH", "HIGH",
                true, "STRONG",
                "LEVEL_1_CONSISTENT", 10,
                null,
                LocalDateTime.of(2025, 1, 2, 0, 0)
        );
        insertExecutionPlan(
                "plan-it-1", analysisId,
                "ADVISORY", "观望",
                null,
                LocalDateTime.of(2025, 1, 2, 0, 0)
        );

        mockMvc.perform(post("/api/position-monitor/" + positionId + "/run")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.monitorRecordAvailable").value(true))
                // NO_ADD means conservative "do not add now" advisory, not an execution instruction.
                .andExpect(jsonPath("$.data.latestMonitorRecord.systemSuggestedAction").value("NO_ADD"))
                .andExpect(jsonPath("$.data.latestMonitorRecord.monitorSummary").exists());

        String summary = jdbcTemplate.queryForObject(
                "SELECT monitor_summary FROM tm_position_monitor_record WHERE position_id = ? ORDER BY monitor_time DESC LIMIT 1",
                String.class,
                positionId
        );
        org.assertj.core.api.Assertions.assertThat(summary).doesNotContain("entry_logic=");
        org.assertj.core.api.Assertions.assertThat(summary).doesNotContain("direction_support=");

        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tm_position_monitor_record WHERE position_id = ?",
                Integer.class,
                positionId
        );
        org.assertj.core.api.Assertions.assertThat(cnt).isEqualTo(1);
        String action = jdbcTemplate.queryForObject(
                "SELECT system_suggested_action FROM tm_position_monitor_record WHERE position_id = ? ORDER BY monitor_time DESC LIMIT 1",
                String.class,
                positionId
        );
        org.assertj.core.api.Assertions.assertThat(action).isEqualTo("NO_ADD");
        org.assertj.core.api.Assertions.assertThat(action)
                .as("NO_ADD is advisory-only: no auto place/close/reverse actions")
                .isNotIn("AUTO_PLACE", "AUTO_CLOSE", "AUTO_REVERSE");
    }

    @Test
    void run_non_manual_open_is_rejected() throws Exception {
        String positionId = "pos-nonmanual-1";
        insertOpenPositionRaw(positionId, "BTCUSDT", "SIMULATED", "BOT", "LONG", 50000, 0.01, 50000);

        mockMvc.perform(post("/api/position-monitor/" + positionId + "/run")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void run_closed_manual_is_rejected() throws Exception {
        String positionId = createManualOpenPosition("BTCUSDT", "LONG", 50000, 0.01);

        mockMvc.perform(post("/api/positions/" + positionId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/position-monitor/" + positionId + "/run")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void get_open_returns_facts_and_latest_monitor_record() throws Exception {
        String pos1 = createManualOpenPosition("BTCUSDT", "LONG", 50000, 0.01);
        String pos2 = createManualOpenPosition("ETHUSDT", "SHORT", 2000, 0.5);
        jdbcTemplate.update("DELETE FROM tm_position_monitor_record");

        // pos1: 写两条监控，确保最新一条被返回
        insertMonitorRecord(
                "pmr-1", pos1, "BTCUSDT",
                "ana-a", "plan-a",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                "VALID", "SUPPORT_ORIGINAL", "NONE",
                "MEDIUM", "SUPPORT",
                "NO_ADD",
                null,
                "NOT_ENTERED",
                LocalDateTime.of(2025, 1, 1, 0, 1),
                LocalDateTime.of(2025, 1, 1, 0, 1)
        );
        insertMonitorRecord(
                "pmr-2", pos1, "BTCUSDT",
                "ana-b", "plan-b",
                LocalDateTime.of(2025, 1, 2, 0, 0),
                "VALID", "SUPPORT_ORIGINAL", "NONE",
                "HIGH", "SUPPORT",
                "CONTINUE_HOLD",
                null,
                "NOT_ENTERED",
                LocalDateTime.of(2025, 1, 2, 0, 1),
                LocalDateTime.of(2025, 1, 2, 0, 1)
        );

        // pos2: 只有一条
        insertMonitorRecord(
                "pmr-3", pos2, "ETHUSDT",
                null, null,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                "WEAKENED", "RANGE", "WEAK",
                "MEDIUM", "UNKNOWN",
                "PLAN_INVALID_WAIT_CONFIRM",
                null,
                "NOT_ENTERED",
                LocalDateTime.of(2025, 1, 1, 0, 2),
                LocalDateTime.of(2025, 1, 1, 0, 2)
        );

        String res = mockMvc.perform(get("/api/position-monitor/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        JsonNode data = root.path("data");
        // 反查 pos1
        JsonNode pos1Row = findRowByPositionId(data, pos1);
        org.assertj.core.api.Assertions.assertThat(pos1Row).isNotNull();
        org.assertj.core.api.Assertions.assertThat(pos1Row.path("monitorRecordAvailable").asBoolean())
                .isEqualTo(true);
        org.assertj.core.api.Assertions.assertThat(pos1Row.path("latestMonitorRecord").path("systemSuggestedAction").asText())
                .isEqualTo("CONTINUE_HOLD");

        JsonNode pos2Row = findRowByPositionId(data, pos2);
        org.assertj.core.api.Assertions.assertThat(pos2Row).isNotNull();
        org.assertj.core.api.Assertions.assertThat(pos2Row.path("monitorRecordAvailable").asBoolean())
                .isEqualTo(true);
        org.assertj.core.api.Assertions.assertThat(pos2Row.path("latestMonitorRecord").path("systemSuggestedAction").asText())
                .isEqualTo("PLAN_INVALID_WAIT_CONFIRM");
    }

    @Test
    void get_open_without_monitor_record_returns_monitorRecordAvailable_false() throws Exception {
        String pos1 = createManualOpenPosition("BTCUSDT", "LONG", 50000, 0.01);
        jdbcTemplate.update("DELETE FROM tm_position_monitor_record WHERE position_id = ?", pos1);

        String res = mockMvc.perform(get("/api/position-monitor/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        JsonNode row = findRowByPositionId(root.path("data"), pos1);
        org.assertj.core.api.Assertions.assertThat(row).isNotNull();
        org.assertj.core.api.Assertions.assertThat(row.path("monitorRecordAvailable").asBoolean()).isFalse();
        JsonNode latest = row.get("latestMonitorRecord");
        boolean latestIsNull = latest == null || latest.isNull();
        org.assertj.core.api.Assertions.assertThat(latestIsNull).isTrue();
    }

    @Test
    void systemSuggestedAction_never_contains_auto_actions() throws Exception {
        String positionId = createManualOpenPosition("BTCUSDT", "LONG", 50000, 0.01);

        String analysisId = "ana-it-2";
        insertAnalysisRun(analysisId, "BTCUSDT");
        insertDecisionResult(
                "dec-it-2", analysisId, "BTCUSDT",
                "BULLISH", "HIGH",
                true, "STRONG",
                "LEVEL_1_CONSISTENT", 10,
                null,
                LocalDateTime.of(2025, 1, 3, 0, 0)
        );
        insertExecutionPlan(
                "plan-it-2", analysisId,
                "ADVISORY", "AUTO_PLACEHOLDER_SHOULD_NOT_APPEAR",
                null,
                LocalDateTime.of(2025, 1, 3, 0, 0)
        );

        String res = mockMvc.perform(post("/api/position-monitor/" + positionId + "/run")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        String action = root.path("data").path("latestMonitorRecord").path("systemSuggestedAction").asText();
        org.assertj.core.api.Assertions.assertThat(action).doesNotStartWith("AUTO_");
    }

    private String createManualOpenPosition(String symbol,
                                              String side,
                                              int avgOpenPrice,
                                              double qty) throws Exception {
        String req = """
                {
                  "symbol":"%s",
                  "positionSide":"%s",
                  "avgOpenPrice":%d,
                  "positionQuantity":%s
                }
                """.formatted(symbol, side, avgOpenPrice, qty);

        String res = mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        return root.path("data").path("positionId").asText();
    }

    private void insertAnalysisRun(String analysisId, String symbol) {
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, status, trace_id, data_quality_score) VALUES (?,?,?,?,?,?,?)",
                analysisId,
                symbol,
                "1h",
                Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 0, 0)),
                "SUCCESS",
                "trace-" + analysisId,
                80
        );
    }

    private void insertDecisionResult(String decisionId,
                                        String analysisId,
                                        String symbol,
                                        String marketBiasHierarchy,
                                        String riskLevel,
                                        Boolean isWorthOpening,
                                        String multiTfConvergence,
                                        String aiConflictLevel,
                                        Integer aiConflictScore,
                                        String invalidCondition,
                                        LocalDateTime createTime) {
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, market_bias_hierarchy, risk_level, is_worth_opening, multi_tf_convergence, ai_conflict_level, ai_conflict_score, invalid_condition, create_time) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                decisionId,
                analysisId,
                symbol,
                marketBiasHierarchy,
                riskLevel,
                isWorthOpening,
                multiTfConvergence,
                aiConflictLevel,
                aiConflictScore,
                invalidCondition,
                Timestamp.valueOf(createTime)
        );
    }

    private void insertExecutionPlan(String planId,
                                      String analysisId,
                                      String planMode,
                                      String recommendedAction,
                                      String invalidCondition,
                                      LocalDateTime createTime) {
        jdbcTemplate.update(
                "INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, invalid_condition, create_time) " +
                        "VALUES (?,?,?,?,?,?)",
                planId,
                analysisId,
                planMode,
                recommendedAction,
                invalidCondition,
                Timestamp.valueOf(createTime)
        );
    }

    private void insertOpenPositionRaw(String positionId,
                                         String symbol,
                                         String sourceType,
                                         String sourceName,
                                         String side,
                                         int avgOpenPrice,
                                         double qty,
                                         int markPrice) {
        jdbcTemplate.update(
                "INSERT INTO tm_real_position(position_id, symbol, source_type, source_name, position_side, avg_open_price, position_open_time, " +
                        "position_quantity, unrealized_pnl_pct, position_status, mark_price, break_even_price, liquidation_price, update_time) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,'OPEN',?,?,?,?)",
                positionId,
                symbol,
                sourceType,
                sourceName,
                side,
                avgOpenPrice,
                Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 0, 0)),
                qty,
                0,
                markPrice,
                null,
                null,
                Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 0, 0))
        );
    }

    private void insertMonitorRecord(String recordId,
                                       String positionId,
                                       String symbol,
                                       String analysisId,
                                       String planId,
                                       LocalDateTime monitorTime,
                                       String entryLogicState,
                                       String directionSupportState,
                                       String reversalState,
                                       String positionRiskLevel,
                                       String aiSupportState,
                                       String systemSuggestedAction,
                                       String monitorSummary,
                                       String reviewEntryStatus,
                                       LocalDateTime createTime,
                                       LocalDateTime updateTime) {
        jdbcTemplate.update(
                "INSERT INTO tm_position_monitor_record(position_monitor_record_id, position_id, symbol, analysis_id, plan_id, monitor_time, " +
                        "entry_logic_state, direction_support_state, reversal_state, position_risk_level, ai_support_state, system_suggested_action, " +
                        "monitor_summary, review_entry_status, create_time, update_time) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                recordId,
                positionId,
                symbol,
                analysisId,
                planId,
                Timestamp.valueOf(monitorTime),
                entryLogicState,
                directionSupportState,
                reversalState,
                positionRiskLevel,
                aiSupportState,
                systemSuggestedAction,
                monitorSummary,
                reviewEntryStatus,
                Timestamp.valueOf(createTime),
                Timestamp.valueOf(updateTime)
        );
    }

    private JsonNode findRowByPositionId(JsonNode dataArray, String positionId) {
        if (dataArray == null || !dataArray.isArray()) {
            return null;
        }
        for (JsonNode item : dataArray) {
            if (item != null && positionId.equals(item.path("positionId").asText())) {
                return item;
            }
        }
        return null;
    }
}

