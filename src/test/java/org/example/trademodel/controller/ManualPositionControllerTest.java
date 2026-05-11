package org.example.trademodel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.service.PositionSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                // 不跑定时任务，避免测试期间触发外部链路
                "spring.task.scheduling.enabled=false"
        }
)
@AutoConfigureMockMvc
class ManualPositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PositionSyncService positionSyncService;

    @Autowired
    private RealPositionMapper realPositionMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private MarketQuoteClient marketQuoteClient;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM tm_position_trade_result");
        jdbcTemplate.update("DELETE FROM tm_review_result");
        jdbcTemplate.update("DELETE FROM tm_position_monitor_record");
        jdbcTemplate.update("DELETE FROM tm_decision_result");
        jdbcTemplate.update("DELETE FROM tm_real_position");
        // 手动创建接口依赖行情；测试用兜底路径（markPrice=avgOpenPrice）
        when(marketQuoteClient.fetch24hTicker(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.<MarketQuoteSnapshot>empty());
    }

    @Test
    void create_manual_success() throws Exception {
        String req = """
                {
                  "symbol":"BTCUSDT",
                  "positionSide":"LONG",
                  "avgOpenPrice":50000,
                  "positionQuantity":0.01
                }
                """;

        String res = mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.positionId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        String positionId = root.path("data").path("positionId").asText();

        // close 用 positionId
        mockMvc.perform(post("/api/positions/" + positionId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void create_manual_auto_generates_first_monitor_record() throws Exception {
        String req = """
                {
                  "symbol":"BTCUSDT",
                  "positionSide":"LONG",
                  "avgOpenPrice":50000,
                  "positionQuantity":0.01
                }
                """;
        String res = mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(res);
        String positionId = root.path("data").path("positionId").asText();
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tm_position_monitor_record WHERE position_id = ?",
                Integer.class,
                positionId
        );
        org.assertj.core.api.Assertions.assertThat(cnt).isEqualTo(1);
    }

    @Test
    void manual_position_monitor_run_open_and_close_smoke_keeps_action_advice_manual_only() throws Exception {
        String createReq = """
                {
                  "symbol":"BTCUSDT",
                  "positionSide":"LONG",
                  "avgOpenPrice":50000,
                  "positionQuantity":0.01
                }
                """;
        String createRes = mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String positionId = objectMapper.readTree(createRes).path("data").path("positionId").asText();

        String runRes = mockMvc.perform(post("/api/position-monitor/" + positionId + "/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.latestMonitorRecord.actionAdvice.manualOnly").value(true))
                .andExpect(jsonPath("$.data.latestMonitorRecord.actionAdvice.notTradeInstruction").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode advice = objectMapper.readTree(runRes)
                .path("data").path("latestMonitorRecord").path("actionAdvice");
        org.assertj.core.api.Assertions.assertThat(advice.path("disclaimerText").asText())
                .contains("不会自动下单、不会自动平仓、不会自动反手");
        org.assertj.core.api.Assertions.assertThat(advice.path("actionCode").asText())
                .isEqualTo("PLAN_INVALID_WAIT_CONFIRM");

        String openRes = mockMvc.perform(get("/api/position-monitor/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode openRows = objectMapper.readTree(openRes).path("data");
        JsonNode monitorRow = null;
        for (JsonNode row : openRows) {
            if (positionId.equals(row.path("positionId").asText())) {
                monitorRow = row;
                break;
            }
        }
        org.assertj.core.api.Assertions.assertThat(monitorRow).isNotNull();
        org.assertj.core.api.Assertions.assertThat(monitorRow.path("latestMonitorRecord").path("actionAdvice").path("manualOnly").asBoolean())
                .isTrue();

        String closeRes = mockMvc.perform(post("/api/positions/" + positionId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tradeResultId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String tradeResultId = objectMapper.readTree(closeRes).path("data").path("tradeResultId").asText();

        Integer monitorCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tm_position_monitor_record WHERE position_id = ?",
                Integer.class,
                positionId
        );
        org.assertj.core.api.Assertions.assertThat(monitorCount).isGreaterThanOrEqualTo(1);

        String latestMonitorRecordId = jdbcTemplate.queryForObject(
                "SELECT latest_monitor_record_id FROM tm_position_trade_result WHERE trade_result_id = ?",
                String.class,
                tradeResultId
        );
        String suggestedActionAtClose = jdbcTemplate.queryForObject(
                "SELECT system_suggested_action_at_close FROM tm_position_trade_result WHERE trade_result_id = ?",
                String.class,
                tradeResultId
        );
        org.assertj.core.api.Assertions.assertThat(latestMonitorRecordId).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(suggestedActionAtClose).isEqualTo("PLAN_INVALID_WAIT_CONFIRM");
    }

    @Test
    void create_manual_reject_same_symbol_second_time() throws Exception {
        String req = """
                {
                  "symbol":"BTCUSDT",
                  "positionSide":"LONG",
                  "avgOpenPrice":50000
                }
                """;

        mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("请先关闭原手动持仓，再录入新持仓。"));
    }

    @Test
    void open_only_returns_manual_open_positions() throws Exception {
        // 先手动 BTC
        String req = """
                {
                  "symbol":"BTCUSDT",
                  "positionSide":"LONG",
                  "avgOpenPrice":50000
                }
                """;
        mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk());

        // 再触发同步：会有 BTC/ETH 的非手动 open 出现（BTC 将被手动保护，不会插入/覆盖）
        positionSyncService.syncPositions();

        mockMvc.perform(get("/api/positions/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                // 仅返回手动 BTC
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.data[0].sourceType").value("MANUAL_INPUT"))
                .andExpect(jsonPath("$.data[0].sourceName").value("USER_MANUAL"));
    }

    @Test
    void close_manual_sets_closed_and_not_returned() throws Exception {
        String req = """
                {
                  "symbol":"BTCUSDT",
                  "positionSide":"LONG",
                  "avgOpenPrice":50000
                }
                """;

        String res = mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        String positionId = root.path("data").path("positionId").asText();

        mockMvc.perform(post("/api/positions/" + positionId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.positionStatus").value("CLOSED"));

        mockMvc.perform(get("/api/positions/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void close_returns_monitor_analysis_when_latest_monitor_has_analysis_id() throws Exception {
        String positionId = createManualPosition("BTCUSDT");
        jdbcTemplate.update(
                "INSERT INTO tm_position_monitor_record(position_monitor_record_id, position_id, symbol, analysis_id, plan_id, monitor_time, " +
                        "entry_logic_state, direction_support_state, reversal_state, position_risk_level, ai_support_state, system_suggested_action, " +
                        "monitor_summary, review_entry_status, create_time, update_time) " +
                        "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'VALID', 'SUPPORT_ORIGINAL', 'NONE', 'LOW', 'SUPPORT', 'CONTINUE_HOLD', ?, 'NOT_ENTERED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "pmr-test-1", positionId, "BTCUSDT", "ana-monitor-1", "plan-1", "summary"
        );

        mockMvc.perform(post("/api/positions/" + positionId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.positionId").value(positionId))
                .andExpect(jsonPath("$.data.positionStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data.tradeResultId").exists())
                .andExpect(jsonPath("$.data.tradeReviewUrl").exists())
                .andExpect(jsonPath("$.data.reviewAnalysisId").value("ana-monitor-1"))
                .andExpect(jsonPath("$.data.analysisReviewUrl").value("/review/ana-monitor-1"))
                .andExpect(jsonPath("$.data.reviewLevel").value("TRADE"));
    }

    @Test
    void close_fallbacks_to_latest_decision_analysis_when_no_monitor_analysis() throws Exception {
        String positionId = createManualPosition("ETHUSDT");
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, create_time) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                "dec-fallback-1", "ana-decision-1", "ETHUSDT"
        );

        mockMvc.perform(post("/api/positions/" + positionId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reviewAnalysisId").value("ana-decision-1"))
                .andExpect(jsonPath("$.data.analysisReviewUrl").value("/review/ana-decision-1"));
    }

    @Test
    void close_returns_null_review_link_when_no_monitor_and_no_decision() throws Exception {
        String positionId = createManualPosition("XRPUSDT");

        mockMvc.perform(post("/api/positions/" + positionId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.positionStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data.reviewAnalysisId").value(nullValue()))
                .andExpect(jsonPath("$.data.analysisReviewUrl").value(nullValue()));
    }

    @Test
    void close_does_not_create_review_result() throws Exception {
        String positionId = createManualPosition("SOLUSDT");
        Integer before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_review_result", Integer.class);
        mockMvc.perform(post("/api/positions/" + positionId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        Integer after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_review_result", Integer.class);
        org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before);
    }

    @Test
    void close_with_exit_price_creates_trade_result_and_sets_closed() throws Exception {
        String positionId = createManualPosition("ADAUSDT");
        String closeReq = """
                {
                  "exitPrice": 51000,
                  "closeReason": "MANUAL_CLOSE",
                  "userActionType": "CLOSE",
                  "userRemark": "manual close"
                }
                """;
        String res = mockMvc.perform(post("/api/positions/" + positionId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.positionStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data.tradeResultId").exists())
                .andExpect(jsonPath("$.data.tradeReviewUrl").exists())
                .andExpect(jsonPath("$.data.reviewLevel").value("TRADE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String tradeResultId = objectMapper.readTree(res).path("data").path("tradeResultId").asText();
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tm_position_trade_result WHERE trade_result_id = ? AND position_id = ?",
                Integer.class,
                tradeResultId,
                positionId
        );
        org.assertj.core.api.Assertions.assertThat(cnt).isEqualTo(1);
        String status = jdbcTemplate.queryForObject(
                "SELECT position_status FROM tm_real_position WHERE position_id = ?",
                String.class,
                positionId
        );
        org.assertj.core.api.Assertions.assertThat(status).isEqualTo("CLOSED");
    }

    @Test
    void close_without_body_uses_mark_price_fallback_and_flags_response() throws Exception {
        String positionId = createManualPosition("DOTUSDT");
        String res = mockMvc.perform(post("/api/positions/" + positionId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.exitPriceFallbackUsed").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String tradeResultId = objectMapper.readTree(res).path("data").path("tradeResultId").asText();
        BigDecimal exitPrice = jdbcTemplate.queryForObject(
                "SELECT exit_price FROM tm_position_trade_result WHERE trade_result_id = ?",
                BigDecimal.class,
                tradeResultId
        );
        org.assertj.core.api.Assertions.assertThat(exitPrice).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    void close_rejects_when_exit_price_non_positive_and_no_fallback_available() throws Exception {
        String positionId = createManualPosition("LTCUSDT");
        jdbcTemplate.update("UPDATE tm_real_position SET mark_price = 0, avg_open_price = 0 WHERE position_id = ?", positionId);
        String req = """
                {"exitPrice":0}
                """;
        mockMvc.perform(post("/api/positions/" + positionId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void close_non_manual_is_rejected() throws Exception {
        // 手动 BTC
        String req = """
                {
                  "symbol":"BTCUSDT",
                  "positionSide":"LONG",
                  "avgOpenPrice":50000
                }
                """;
        mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk());

        // 同步：ETH 会生成非手动持仓
        positionSyncService.syncPositions();

        // 取 ETH 的 OPEN 非手动 id
        String ethId = jdbcTemplate.queryForObject(
                "SELECT position_id FROM tm_real_position WHERE symbol='ETHUSDT' AND position_status='OPEN' AND source_type<>'MANUAL_INPUT' LIMIT 1",
                String.class);

        mockMvc.perform(post("/api/positions/" + ethId + "/close"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("不是手动持仓，不允许关闭"));
    }

    @Test
    void position_sync_does_not_close_manual_open_for_unknown_symbol() throws Exception {
        // XRPUSDT 不在 SimulatedPositionProvider 返回列表中；如果 closeMissingOpenPositions 未排除 manual，会被关闭
        String req = """
                {
                  "symbol":"XRPUSDT",
                  "positionSide":"LONG",
                  "avgOpenPrice":0.5
                }
                """;

        mockMvc.perform(post("/api/positions/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk());

        positionSyncService.syncPositions();

        mockMvc.perform(get("/api/positions/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].symbol").value("XRPUSDT"));
    }

    private String createManualPosition(String symbol) throws Exception {
        String req = """
                {
                  "symbol":"%s",
                  "positionSide":"LONG",
                  "avgOpenPrice":50000
                }
                """.formatted(symbol);
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
}
