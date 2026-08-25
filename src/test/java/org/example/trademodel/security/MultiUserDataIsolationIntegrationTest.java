package org.example.trademodel.security;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetPoolItemMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.UserConfigMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.MultiUserAccountService;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.testsupport.FrozenFinalExecutionPlanTestFixture;
import org.example.trademodel.vo.DashboardHomeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TradeModelApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:multi-user-isolation;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=xuchao",
        "trade-model.auth.initial-password=owner-isolation-secret"
})
@AutoConfigureMockMvc
@Transactional
class MultiUserDataIsolationIntegrationTest {
    @Autowired
    private MultiUserAccountService accountService;
    @Autowired
    private DashboardHomeService dashboardHomeService;
    @Autowired
    private UserPositionService userPositionService;
    @Autowired
    private AnalysisRunMapper analysisRunMapper;
    @Autowired
    private DecisionResultMapper decisionResultMapper;
    @Autowired
    private ExecutionPlanMapper executionPlanMapper;
    @Autowired
    private UserPositionMapper userPositionMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private ReviewResultMapper reviewResultMapper;
    @Autowired
    private AssetPoolItemMapper assetPoolItemMapper;
    @Autowired
    private UserConfigMapper userConfigMapper;
    @Autowired
    private PersistedOhlcvBarMapper ohlcvBarMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void userOwnedFactsArePrivateWhilePublicMarketFactsRemainShared() throws Exception {
        PersonalUserDO alice = accountService.register("isolation_alice", "alice-pass-123");
        PersonalUserDO bob = accountService.register("isolation_bob", "bob-pass-12345");
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 12, 0);

        userConfigMapper.findByUserId(String.valueOf(alice.getId())).setRiskPreference("CONSERVATIVE");
        userConfigMapper.saveOrUpdate(userConfigMapper.findByUserId(String.valueOf(alice.getId())));
        assertThat(userConfigMapper.findByUserId(String.valueOf(alice.getId())).getRiskPreference())
                .isEqualTo("CONSERVATIVE");
        assertThat(userConfigMapper.findByUserId(String.valueOf(bob.getId())).getRiskPreference())
                .isNotEqualTo("CONSERVATIVE");

        assertThat(assetPoolItemMapper.listUserOverrides(alice.getId())).hasSize(6);
        assertThat(assetPoolItemMapper.listUserOverrides(bob.getId())).hasSize(6);
        assetPoolItemMapper.deleteUserOverride(alice.getId(), "BTCUSDT");
        assertThat(assetPoolItemMapper.listUserOverrides(alice.getId())).hasSize(5);
        assertThat(assetPoolItemMapper.listUserOverrides(bob.getId())).hasSize(6);

        insertAnalysis("analysis-alice", "BTCUSDT", alice.getId(), now);
        insertSystemAnalysis("analysis-system", now.plusMinutes(2));
        insertDecisionAndFinalPlan("analysis-alice", "decision-alice", "plan-alice",
                "BTCUSDT", alice.getId(), now);

        DashboardHomeVO bobEmptyHome = dashboardHomeService.getHomeForUser(bob.getId(), null, 6, null);
        assertThat(bobEmptyHome.getAssets()).isEmpty();
        assertThat(bobEmptyHome.getPositions()).isEmpty();
        assertThat(bobEmptyHome.getPositionMonitoringState()).isEqualTo("NO_POSITION");
        assertThat(bobEmptyHome.getExecutionSuggestion().getDirection()).isNull();
        assertThat(bobEmptyHome.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(bobEmptyHome.getAiDecision().getTabs())
                .allSatisfy(tab -> assertThat(tab.getResultAvailable()).isFalse());

        insertAnalysis("analysis-bob", "ETHUSDT", bob.getId(), now.plusMinutes(1));
        insertDecisionAndFinalPlan("analysis-bob", "decision-bob", "plan-bob",
                "ETHUSDT", bob.getId(), now.plusMinutes(1));

        assertThat(analysisRunMapper.selectReadableByUser("analysis-alice", alice.getId())).isNotNull();
        assertThat(analysisRunMapper.selectReadableByUser("analysis-alice", bob.getId())).isNull();
        assertThat(analysisRunMapper.selectReadableByUser("analysis-bob", alice.getId())).isNull();
        assertThat(analysisRunMapper.selectReadableByUser("analysis-system", alice.getId())).isNull();
        assertThat(decisionResultMapper.findLatestDecisionResultsJoinedForUser(alice.getId(), 20))
                .extracting("analysisId").containsExactly("analysis-alice");
        assertThat(decisionResultMapper.findLatestDecisionResultsJoinedForUser(bob.getId(), 20))
                .extracting("analysisId").containsExactly("analysis-bob");
        assertThat(executionPlanMapper.selectByPlanIdForUser("plan-alice", alice.getId())).isNotNull();
        assertThat(executionPlanMapper.selectByPlanIdForUser("plan-alice", bob.getId())).isNull();
        assertThatThrownBy(() -> userPositionService.manualOpenForUser(
                bob.getId(), planPositionRequest("BTCUSDT", "plan-alice", now.minusMinutes(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("final_plan_id");

        Long alicePosition = insertPosition(alice.getId(), "BTCUSDT", now);
        Long bobPosition = insertPosition(bob.getId(), "ETHUSDT", now.plusMinutes(1));
        assertThat(userPositionMapper.selectByIdAndUserId(alicePosition, alice.getId())).isNotNull();
        assertThat(userPositionMapper.selectByIdAndUserId(alicePosition, bob.getId())).isNull();
        assertThat(userPositionMapper.listOpenByUserId(alice.getId()))
                .extracting("id").containsExactly(alicePosition);
        assertThat(userPositionMapper.listOpenByUserId(bob.getId()))
                .extracting("id").containsExactly(bobPosition);

        insertMessage("message-alice", alice.getId(), "analysis-alice", now);
        insertMessage("message-bob", bob.getId(), "analysis-bob", now.plusMinutes(1));
        assertThat(messageMapper.selectByIdForUser("message-alice", alice.getId())).isNotNull();
        assertThat(messageMapper.selectByIdForUser("message-alice", bob.getId())).isNull();
        assertThat(messageMapper.listActiveForUser(alice.getId(), now, 20))
                .extracting("messageId").containsExactly("message-alice");
        assertThat(messageMapper.listActiveForUser(bob.getId(), now, 20))
                .extracting("messageId").containsExactly("message-bob");

        insertReview("review-alice", "analysis-alice", alice.getId(), now);
        insertReview("review-bob", "analysis-bob", bob.getId(), now.plusMinutes(1));
        assertThat(reviewResultMapper.listByAnalysisIdForUser("analysis-alice", alice.getId()))
                .extracting("id").containsExactly("review-alice");
        assertThat(reviewResultMapper.listByAnalysisIdForUser("analysis-alice", bob.getId())).isEmpty();

        insertAnalysis("analysis-alice-legacy-review", "BTCUSDT", alice.getId(), now.plusMinutes(2));
        insertLegacySharedReview("review-alice-legacy", "analysis-alice-legacy-review", now.plusMinutes(2));
        assertThat(reviewResultMapper.listByAnalysisIdForUser(
                "analysis-alice-legacy-review", alice.getId()))
                .extracting("id").containsExactly("review-alice-legacy");
        assertThat(reviewResultMapper.listByAnalysisIdForUser(
                "analysis-alice-legacy-review", bob.getId())).isEmpty();

        insertMissedOpportunity("missed-alice", "analysis-alice", "BTCUSDT", now.plusMinutes(3));
        insertMissedOpportunity("missed-bob", "analysis-bob", "ETHUSDT", now.plusMinutes(4));

        insertAlert("alert-alice", "analysis-alice", "BTCUSDT", now.plusMinutes(3));
        insertAlert("alert-bob", "analysis-bob", "ETHUSDT", now.plusMinutes(4));
        assertThat(dashboardHomeService.getHomeForUser(alice.getId(), null, 6, null).getAlerts())
                .extracting("symbol").containsExactly("BTC/USDT");
        assertThat(dashboardHomeService.getHomeForUser(bob.getId(), null, 6, null).getAlerts())
                .extracting("symbol").containsExactly("ETH/USDT");

        insertPublicClosedBar(now);
        assertThat(ohlcvBarMapper.selectLatestClosedWindowBySource(
                "BTCUSDT", "5m", "BINANCE", "SPOT", 1))
                .singleElement().extracting("closePrice").isEqualTo(new BigDecimal("101.00000000"));

        MockHttpSession aliceSession = login("isolation_alice", "alice-pass-123");
        MockHttpSession bobSession = login("isolation_bob", "bob-pass-12345");
        mockMvc.perform(get("/api/analysis/runs/analysis-alice").session(aliceSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analysis/runs/analysis-alice").session(bobSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/opportunities/opportunity-analysis-alice").session(aliceSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/opportunities/opportunity-analysis-alice").session(bobSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/opportunity-log/opportunity-analysis-alice").session(aliceSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/opportunity-log/opportunity-analysis-alice").session(bobSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/opportunity-log/query")
                        .param("analysisId", "analysis-alice")
                        .session(bobSession))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data")
                        .isEmpty());
        mockMvc.perform(get("/api/workspace/plans/plan-alice").session(aliceSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/workspace/plans/plan-alice").session(bobSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/workspace/messages").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("message-alice"),
                                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("message-bob")))));
        mockMvc.perform(get("/api/workspace/messages").session(bobSession))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("message-bob"),
                                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("message-alice")))));
        mockMvc.perform(get("/api/missed-opportunity/query")
                        .param("analysisId", "analysis-alice")
                        .session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("missed-alice"),
                                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("missed-bob")))));
        mockMvc.perform(get("/api/missed-opportunity/query")
                        .param("missedId", "missed-alice")
                        .session(bobSession))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data")
                        .isEmpty());
    }

    private void insertAnalysis(String analysisId, String symbol, Long ownerId, LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, status, "
                        + "owner_type, owner_id, preview, created_at, updated_at) VALUES (?, ?, '5m', ?, 'SUCCESS', "
                        + "'USER', ?, FALSE, ?, ?)",
                analysisId, symbol, time, ownerId, time, time);
    }

    private void insertSystemAnalysis(String analysisId, LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, status, "
                        + "owner_type, owner_id, preview, created_at, updated_at) VALUES (?, 'SOLUSDT', '5m', ?, "
                        + "'SUCCESS', 'SYSTEM', 0, FALSE, ?, ?)", analysisId, time, time, time);
    }

    private void insertDecisionAndFinalPlan(String analysisId, String decisionId, String planId,
                                            String symbol, Long ownerId, LocalDateTime time) {
        String opportunityId = "opportunity-" + analysisId;
        String candidateId = "candidate-" + analysisId;
        String resolverId = "resolver-" + analysisId;
        String traceId = "trace-" + analysisId;
        jdbcTemplate.update("INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, create_time) "
                + "VALUES (?, ?, ?, ?)", decisionId, analysisId, symbol, time);
        jdbcTemplate.update("INSERT INTO tm_asset_state(owner_type, owner_id, asset_id, symbol, timeframe, state, "
                        + "opportunity_id, state_entered_at, last_update_time, trace_id) "
                        + "VALUES ('USER', ?, 1, ?, '5m', 'CANDIDATE', ?, ?, ?, ?)",
                ownerId, symbol, opportunityId, time, time, traceId);
        jdbcTemplate.update("INSERT INTO tm_execution_plan_candidate(candidate_id, opportunity_id, analysis_id, trace_id, "
                        + "rule_direction, rule_confidence, rule_risk, candidate_direction, plan_mode, confidence_level, "
                        + "risk_level, worth_opening, recommended_action, entry_zone, stop_loss, take_profit_rules, "
                        + "leverage_suggestion, position_suggestion, invalid_condition, validity, summary, "
                        + "candidate_source, candidate_status, payload_json, not_final_plan, not_state_machine_mutation, "
                        + "not_user_position_creation, created_at) VALUES (?, ?, ?, ?, 'BULLISH', 'HIGH', 'MEDIUM', "
                        + "'BULLISH', 'CONFIRMATION', 'HIGH', 'MEDIUM', TRUE, 'MANUAL_REVIEW', '100-101', '95', "
                        + "'110 then 120', '1x', 'small', 'close below 95', 'controlled', 'candidate only', "
                        + "'GPT_FINAL', 'VALIDATED', '{}', TRUE, TRUE, TRUE, ?)",
                candidateId, opportunityId, analysisId, traceId, time);
        jdbcTemplate.update("INSERT INTO tm_conflict_resolver_result(resolver_result_id, candidate_id, analysis_id, "
                        + "trace_id, rule_direction, rule_confidence, rule_risk, gemini_review_json, "
                        + "grok_challenge_json, conflict_level, conflict_score, plan_mode_before, plan_mode_after, "
                        + "confidence_before, confidence_after, risk_before, risk_after, confused_decision, "
                        + "rule_direction_preserved, created_at) VALUES (?, ?, ?, ?, 'BULLISH', 'HIGH', 'MEDIUM', "
                        + "'{}', '{}', 'LEVEL_1_CONSISTENT', 0, 'CONFIRMATION', 'CONFIRMATION', 'HIGH', 'HIGH', "
                        + "'MEDIUM', 'MEDIUM', FALSE, TRUE, ?)",
                resolverId, candidateId, analysisId, traceId, time);
        jdbcTemplate.update("INSERT INTO tm_account_risk_snapshot(analysis_id, symbol, owner_type, owner_id, "
                        + "account_risk_status, risk_level_snapshot, risk_allowed, risk_reason_code, risk_reason_text, "
                        + "position_exposure, max_allowed_exposure, candidate_leverage, max_allowed_leverage, "
                        + "source_status, observed_at, fresh_until, snapshot_source, snapshot_version, source_note, "
                        + "trace_id, create_time) VALUES (?, ?, 'USER', ?, 'ALLOWED', 'LOW', TRUE, "
                        + "'CONTROLLED_VERIFIED_ACCOUNT_RISK', 'Controlled multi-user isolation fixture', "
                        + "0.10, 0.20, 1.0, 3.0, 'VERIFIED', ?, ?, 'CONTROLLED_INTEGRATION_FIXTURE', 1, "
                        + "'TEST_ONLY_VERIFIED_SOURCE', ?, ?)",
                analysisId, symbol, ownerId, time, time.plusHours(1), traceId, time);
        Long accountRiskSnapshotId = jdbcTemplate.queryForObject(
                "SELECT id FROM tm_account_risk_snapshot WHERE analysis_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, analysisId);
        var plan = FrozenFinalExecutionPlanTestFixture.complete(planId, analysisId, time);
        plan.setCandidateId(candidateId);
        plan.setOpportunityId(opportunityId);
        plan.setResolverResultId(resolverId);
        plan.setTraceId(traceId);
        plan.setValidationResultId("validation-" + analysisId);
        plan.setRuleVersion("v4.1-multi-user-isolation");
        plan.setAccountRiskSnapshotId(accountRiskSnapshotId);
        executionPlanMapper.insert(plan);
        jdbcTemplate.update("INSERT INTO tm_opportunity_log(opportunity_id, opportunity_key, analysis_id, "
                        + "decision_id, execution_plan_id, symbol, timeframe, direction, lifecycle_status, "
                        + "anchor_time, source_type, source_reference, market_data_source, trace_id, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, '5m', 'LONG', "
                        + "'PENDING_EVALUATION', ?, 'ANALYSIS', ?, 'BINANCE', ?, ?, ?)",
                opportunityId, "opportunity-key-" + analysisId, analysisId, decisionId, planId,
                symbol, time, analysisId, traceId, time, time);
    }

    private Long insertPosition(Long userId, String symbol, LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO tm_user_position(user_id, asset_symbol, side, status, entry_price, quantity, "
                        + "leverage, opened_at, source_type, created_at, updated_at) "
                        + "VALUES (?, ?, 'LONG', 'OPEN', 100, 1, 1, ?, 'MANUAL_INDEPENDENT', ?, ?)",
                userId, symbol, time, time, time);
        return jdbcTemplate.queryForObject("SELECT id FROM tm_user_position WHERE user_id = ? AND asset_symbol = ?",
                Long.class, userId, symbol);
    }

    private void insertMessage(String messageId, Long userId, String analysisId, LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO tm_message(message_id, user_id, category, source_type, source_id, "
                        + "analysis_id, symbol, title, body, business_state, read_state, dedupe_key, created_at, updated_at) "
                        + "VALUES (?, ?, 'OPPORTUNITY_PLAN_SAFETY_CHANGE', 'ANALYSIS', ?, ?, 'BTCUSDT', "
                        + "'Private message', 'Private body', 'ACTIVE', 'UNREAD', ?, ?, ?)",
                messageId, userId, analysisId, analysisId, "dedupe-" + messageId, time, time);
    }

    private void insertReview(String reviewId, String analysisId, Long userId, LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO tm_review_result(id, analysis_id, user_id, review_scope_key, "
                        + "review_type, outcome, create_time, update_time) VALUES (?, ?, ?, ?, 'ANALYSIS_FEEDBACK', "
                        + "'PRIVATE', ?, ?)",
                reviewId, analysisId, userId, "USER:" + userId + ":ANALYSIS:" + analysisId, time, time);
    }

    private void insertLegacySharedReview(String reviewId, String analysisId, LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO tm_review_result(id, analysis_id, review_scope_key, "
                        + "review_type, outcome, create_time, update_time) VALUES (?, ?, 'SHARED', "
                        + "'ANALYSIS_FEEDBACK', 'LEGACY_PRIVATE', ?, ?)",
                reviewId, analysisId, time, time);
    }

    private void insertMissedOpportunity(String missedId, String analysisId,
                                         String symbol, LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO tm_missed_opportunity(missed_id, decision_id, analysis_id, symbol, "
                        + "biz_date, reason_json, rule_version, trace_id, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, '{}', 'missed-v1', ?, ?)",
                missedId, "decision-" + missedId, analysisId, symbol,
                time.toLocalDate(), "trace-" + missedId, time);
    }

    private void insertPublicClosedBar(LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO tm_persisted_ohlcv_bar(symbol, timeframe, open_time_ms, close_time_ms, "
                        + "open_price, high_price, low_price, close_price, volume, quote_volume, trade_count, "
                        + "taker_buy_base_volume, taker_buy_quote_volume, is_closed, provider, provider_market_type, "
                        + "source_endpoint, source_batch_id, source_trace_id, source_version, source_status, "
                        + "freshness_status, ingested_at, updated_at, quality_status, raw_payload_hash, is_deleted) "
                        + "VALUES ('BTCUSDT', '5m', 1000, 2000, 100, 102, 99, 101, 10, 1010, 5, 6, 606, TRUE, "
                        + "'BINANCE', 'SPOT', 'public-market-fixture', 'batch-public', 'trace-public', 1, "
                        + "'VERIFIED', 'FRESH', ?, ?, 'OK', 'hash-public', 0)", time, time);
    }

    private void insertAlert(String id, String analysisId, String symbol, LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO tm_monitor_alert(id, analysis_id, asset_symbol, alert_type, alert_level, "
                        + "alert_message, status, trace_id, created_at, updated_at, is_deleted, version_no) "
                        + "VALUES (?, ?, ?, 'RISK_CHANGE', 'HIGH', 'Private alert', 'OPEN', ?, ?, ?, 0, 1)",
                id, analysisId, symbol, "trace-" + id, time, time);
    }

    private CreateUserPositionReq planPositionRequest(String symbol, String planId, LocalDateTime openedAt) {
        CreateUserPositionReq request = new CreateUserPositionReq();
        request.setAssetSymbol(symbol);
        request.setSide("LONG");
        request.setEntryPrice(new BigDecimal("100"));
        request.setQuantity(BigDecimal.ONE);
        request.setLeverage(BigDecimal.ONE);
        request.setOpenedAt(openedAt);
        request.setSourceType("SYSTEM_PLAN_POSITION");
        request.setFinalPlanId(planId);
        return request;
    }

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin().user(username).password(password))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession(false);
    }
}
