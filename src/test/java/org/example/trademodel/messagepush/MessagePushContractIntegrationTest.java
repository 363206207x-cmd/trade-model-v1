package org.example.trademodel.messagepush;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "trade-model.auth.enabled=true")
@AutoConfigureMockMvc
@Transactional
@Tag("core-regression")
class MessagePushContractIntegrationTest {
    private static final String USER_A = "message.owner.a";
    private static final String USER_B = "message.owner.b";
    private static final String LARGE_MESSAGE_ID = "9007199254740993";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PersonalUserMapper personalUserMapper;
    @Autowired
    private UserPositionMapper userPositionMapper;
    @Autowired
    private PositionMonitorLogMapper positionMonitorLogMapper;
    @Autowired
    private PushSnapshotMapper pushSnapshotMapper;
    @Autowired
    private PushRecheckLogMapper pushRecheckLogMapper;
    @Autowired
    private OpportunityLogMapper opportunityLogMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userAId;
    private Long userBId;
    private UserPositionDO positionA;
    private UserPositionDO positionB;

    @BeforeEach
    void setUp() {
        userAId = insertUser(USER_A);
        userBId = insertUser(USER_B);
        positionA = insertPosition(userAId, "BTCUSDT", 1);
        positionB = insertPosition(userBId, "BTCUSDT", 2);
    }

    @Test
    void positionRiskMessagesUseExactStringIdAndRemainOwnerScopedForTheSameSymbol() throws Exception {
        insertMonitorLogWithId(Long.valueOf(LARGE_MESSAGE_ID), positionA, "HIGH_RISK",
                "RISK_CONTEXT_UNAVAILABLE");
        PositionMonitorLogDO userBLog = insertMonitorLog(positionB, "PLAN_INVALIDATED",
                "STOP_LOSS_BREACHED");
        PositionMonitorLogDO nonRisk = insertMonitorLog(positionA, "LOGIC_VALID", "LOGIC_VALID");

        String listBody = mockMvc.perform(get("/api/messages")
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("READY"))
                .andReturn().getResponse().getContentAsString();

        JsonNode items = objectMapper.readTree(listBody).path("data").path("items");
        assertThat(items.toString()).contains("\"messageId\":\"" + LARGE_MESSAGE_ID + "\"");
        assertThat(items.toString()).contains("\"sourceType\":\"POSITION_RISK\"");
        assertThat(items.toString()).contains("\"symbol\":\"BTCUSDT\"");
        assertThat(items.toString()).doesNotContain("\"messageId\":\"" + userBLog.getLogId() + "\"");
        assertThat(items.toString()).doesNotContain("\"messageId\":\"" + nonRisk.getLogId() + "\"");
        assertThat(items.toString()).doesNotContain("userId", "ownerId");

        mockMvc.perform(get("/api/messages/{messageId}/push-detail", LARGE_MESSAGE_ID)
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("READY"))
                .andExpect(jsonPath("$.data.messageId").value(LARGE_MESSAGE_ID))
                .andExpect(jsonPath("$.data.messageId").isString())
                .andExpect(jsonPath("$.data.pushId").doesNotExist())
                .andExpect(jsonPath("$.data.sourceIdentity.sourceType").value("POSITION_RISK"))
                .andExpect(jsonPath("$.data.sourceIdentity.sourceId").value(LARGE_MESSAGE_ID))
                .andExpect(jsonPath("$.data.sourceIdentity.positionId")
                        .value(String.valueOf(positionA.getId())))
                .andExpect(jsonPath("$.data.originalSnapshot.status").value("HIGH_RISK"))
                .andExpect(jsonPath("$.data.currentRecheck.status").value("LOGIC_VALID"));

        mockMvc.perform(get("/api/messages/{messageId}/push-detail", userBLog.getLogId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.state").value("MISSING"));

        mockMvc.perform(get("/api/messages/{messageId}/push-detail", userBLog.getLogId())
                        .with(user(USER_B).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceIdentity.positionId")
                        .value(String.valueOf(positionB.getId())));
    }

    @Test
    void opportunityPushDetailReturnsOnlyTheServerSidePublicProjection() throws Exception {
        TmPushSnapshotDO push = insertPushSnapshot();
        OpportunityLogDO opportunity = insertOpportunity(push);
        insertPushRecheck(push);

        String opportunityBody = mockMvc.perform(
                        get("/api/opportunity-log/{opportunityId}", opportunity.getOpportunityId())
                                .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opportunityId").value(opportunity.getOpportunityId()))
                .andExpect(jsonPath("$.data.pushId").doesNotExist())
                .andExpect(jsonPath("$.data.riskBlockedEvidence").doesNotExist())
                .andExpect(jsonPath("$.data.riskBlockedAt").doesNotExist())
                .andExpect(jsonPath("$.data.userPositionId").doesNotExist())
                .andExpect(jsonPath("$.data.userPositionPresent").doesNotExist())
                .andExpect(jsonPath("$.data.reasonCodes").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(opportunityBody).doesNotContain(
                "currentAccountRiskAllowed",
                "failReasonJson",
                "PRIVATE_ACCOUNT_RISK_REASON");

        mockMvc.perform(get("/api/opportunity-log/query")
                        .param("symbol", "SOLUSDT")
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].opportunityId")
                        .value(opportunity.getOpportunityId()))
                .andExpect(jsonPath("$.data[0].pushId").doesNotExist())
                .andExpect(jsonPath("$.data[0].riskBlockedEvidence").doesNotExist())
                .andExpect(jsonPath("$.data[0].riskBlockedAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].userPositionId").doesNotExist());

        String listBody = mockMvc.perform(get("/api/messages")
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("READY"))
                .andExpect(jsonPath("$.data.items[0].messageId")
                        .value(opportunity.getOpportunityId()))
                .andExpect(jsonPath("$.data.items[0].pushId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(listBody).doesNotContain(
                "\"pushId\"",
                "currentAccountRiskAllowed",
                "failReasonJson",
                "PRIVATE_ACCOUNT_RISK_REASON");

        String responseBody = mockMvc.perform(
                        get("/api/messages/{messageId}/push-detail", opportunity.getOpportunityId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PARTIAL"))
                .andExpect(jsonPath("$.data.messageId").value(opportunity.getOpportunityId()))
                .andExpect(jsonPath("$.data.sourceIdentity.sourceType").value("OPPORTUNITY"))
                .andExpect(jsonPath("$.data.sourceIdentity.sourceId").value(opportunity.getOpportunityId()))
                .andExpect(jsonPath("$.data.sourceIdentity.positionId").doesNotExist())
                .andExpect(jsonPath("$.data.opportunityIdentity.opportunityId")
                        .value(opportunity.getOpportunityId()))
                .andExpect(jsonPath("$.data.opportunityIdentity.pushId").doesNotExist())
                .andExpect(jsonPath("$.data.publicLifecycle").value("PENDING_EVALUATION"))
                .andExpect(jsonPath("$.data.publicStatus").isEmpty())
                .andExpect(jsonPath("$.data.missingFields[0]").value("publicEvaluation"))
                .andExpect(jsonPath("$.data.publicTimestamp").value("2026-07-29T10:00:00"))
                .andExpect(jsonPath("$.data.publicDescription").value("SOLUSDT LONG 1H"))
                .andExpect(jsonPath("$.data.originalSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.currentRecheck").doesNotExist())
                .andExpect(jsonPath("$.data.changeReason").doesNotExist())
                .andExpect(jsonPath("$.data.notExecutable").value(true))
                .andExpect(jsonPath("$.data.notPushSend").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(responseBody).doesNotContain(
                "\"pushId\"",
                "currentAccountRiskAllowed",
                "failReasonJson",
                "PRIVATE_ACCOUNT_RISK_REASON",
                "riskLevel",
                "RISK_BLOCKED",
                "BLOCKED_BY_RISK_VALID",
                "riskBlockedEvidence",
                "riskBlockedAt",
                "positionId",
                "currentPrice",
                "entryZone",
                "invalidationCondition");

        mockMvc.perform(get("/api/messages/{messageId}/push-detail", opportunity.getOpportunityId())
                        .with(user(USER_B).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceIdentity.sourceType").value("OPPORTUNITY"))
                .andExpect(jsonPath("$.data.publicLifecycle").value("PENDING_EVALUATION"))
                .andExpect(jsonPath("$.data.publicStatus").isEmpty())
                .andExpect(jsonPath("$.data.sourceIdentity.positionId").doesNotExist());

        String previewBody = mockMvc.perform(
                        get("/api/dashboard/recheck-preview-status")
                                .param("pushId", String.valueOf(push.getPushId()))
                .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECHECK_PREVIEW_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reason").value("RAW_PUSH_RECHECK_READ_DISABLED"))
                .andExpect(jsonPath("$.pushId").doesNotExist())
                .andExpect(jsonPath("$.latestLogAvailable").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String latestBody = mockMvc.perform(
                        get("/api/push/recheck/{pushId}/latest", push.getPushId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andReturn().getResponse().getContentAsString();
        String logsBody = mockMvc.perform(
                        get("/api/push/recheck/{pushId}/logs", push.getPushId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andReturn().getResponse().getContentAsString();

        assertThat(previewBody + latestBody + logsBody).doesNotContain(
                "currentAccountRiskAllowed",
                "failReasonJson",
                "PRIVATE_ACCOUNT_RISK_REASON",
                "RISK_BLOCKED");
    }

    @Test
    void emptyListIsSuccessfulAndDistinctFromAnError() throws Exception {
        mockMvc.perform(get("/api/messages")
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.state").value("EMPTY"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void unauthenticatedReadsAreRejected() throws Exception {
        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/messages/1/push-detail"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/push/recheck/1/latest"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/push/recheck/1/logs"))
                .andExpect(status().isUnauthorized());
    }

    private Long insertUser(String username) {
        PersonalUserDO user = new PersonalUserDO();
        user.setUsername(username);
        user.setPasswordHash("{noop}test-only-password");
        user.setCreatedAt(LocalDateTime.now());
        personalUserMapper.insert(user);
        return user.getId();
    }

    private UserPositionDO insertPosition(Long userId, String symbol, int minute) {
        LocalDateTime openedAt = LocalDateTime.of(2026, 7, 29, 8, minute);
        UserPositionDO row = new UserPositionDO();
        row.setUserId(userId);
        row.setAssetSymbol(symbol);
        row.setSide("LONG");
        row.setStatus("OPEN");
        row.setEntryPrice(new BigDecimal("100"));
        row.setQuantity(new BigDecimal("0.25"));
        row.setLeverage(new BigDecimal("2"));
        row.setStopLoss(new BigDecimal("90"));
        row.setTakeProfit(new BigDecimal("120"));
        row.setOpenedAt(openedAt);
        row.setSourceType("MANUAL");
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
        row.setCreatedAt(openedAt);
        row.setUpdatedAt(openedAt);
        userPositionMapper.insert(row);
        return row;
    }

    private PositionMonitorLogDO insertMonitorLog(UserPositionDO position,
                                                   String logicStatus,
                                                   String reason) {
        PositionMonitorLogDO row = monitorLog(position, logicStatus, reason);
        positionMonitorLogMapper.insert(row);
        return row;
    }

    private void insertMonitorLogWithId(Long logId,
                                        UserPositionDO position,
                                        String logicStatus,
                                        String reason) {
        PositionMonitorLogDO row = monitorLog(position, logicStatus, reason);
        jdbcTemplate.update(
                "INSERT INTO tm_position_monitor_log("
                        + "log_id, position_id, analysis_id, execution_plan_id, current_price, logic_status, "
                        + "risk_level, suggested_action, reason, risk_snapshot, trace_id, created_at"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                logId,
                row.getPositionId(),
                row.getAnalysisId(),
                row.getExecutionPlanId(),
                row.getCurrentPrice(),
                row.getLogicStatus(),
                row.getRiskLevel(),
                row.getSuggestedAction(),
                row.getReason(),
                row.getRiskSnapshot(),
                row.getTraceId(),
                row.getCreatedAt());
    }

    private PositionMonitorLogDO monitorLog(UserPositionDO position,
                                            String logicStatus,
                                            String reason) {
        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setPositionId(position.getId());
        row.setAnalysisId("ana-position-" + position.getId());
        row.setExecutionPlanId("plan-position-" + position.getId());
        row.setCurrentPrice(new BigDecimal("95"));
        row.setLogicStatus(logicStatus);
        row.setRiskLevel("LOGIC_VALID".equals(logicStatus) ? "LOW" : "HIGH");
        row.setSuggestedAction(switch (logicStatus) {
            case "LOGIC_VALID" -> "HOLD";
            case "LOGIC_WEAKENED" -> "MANUAL_REVIEW";
            case "PLAN_INVALIDATED" -> "RECHECK_PLAN";
            default -> "RISK_REVIEW";
        });
        row.setReason(reason);
        row.setRiskSnapshot("LOGIC_VALID".equals(logicStatus)
                ? "{\"riskLevel\":\"LOW\",\"riskBlocked\":false}"
                : "{\"riskLevel\":\"HIGH\",\"riskBlocked\":true}");
        row.setTraceId("trace-position-" + position.getId());
        row.setCreatedAt(LocalDateTime.of(2026, 7, 29, 9,
                "LOGIC_VALID".equals(logicStatus) ? 5 : 0));
        return row;
    }

    private TmPushSnapshotDO insertPushSnapshot() {
        TmPushSnapshotDO row = new TmPushSnapshotDO();
        row.setAnalysisId("ana-message-opportunity");
        row.setSymbol("SOLUSDT");
        row.setTimeframe("1h");
        row.setPushType("ANALYSIS_RUN");
        row.setPushStatus("RECHECK_REVIEW_PASSED");
        row.setPushCreateTime(LocalDateTime.of(2026, 7, 29, 10, 0));
        row.setRuleVersion("v1");
        row.setTriggerPrice(new BigDecimal("100"));
        row.setEntryZoneJson("{\"text\":\"100-101\"}");
        row.setInvalidationConditionJson("{\"text\":\"below 95\"}");
        row.setTraceId("trace-message-opportunity");
        row.setCreateTime(row.getPushCreateTime());
        pushSnapshotMapper.insert(row);
        return row;
    }

    private OpportunityLogDO insertOpportunity(TmPushSnapshotDO push) {
        OpportunityLogDO row = new OpportunityLogDO();
        row.setOpportunityId("opp-message-contract");
        row.setOpportunityKey("ana-message-opportunity:decision-message-opportunity");
        row.setAnalysisId(push.getAnalysisId());
        row.setDecisionId("decision-message-opportunity");
        row.setExecutionPlanId("plan-message-opportunity");
        row.setPushId(push.getPushId());
        row.setSymbol(push.getSymbol());
        row.setTimeframe(push.getTimeframe());
        row.setDirection("LONG");
        row.setLifecycleStatus(OpportunityLogStatus.PENDING_EVALUATION);
        row.setOpportunityStatus(null);
        row.setAnchorTime(push.getPushCreateTime());
        row.setTargetHit(false);
        row.setInvalidationHit(false);
        row.setPushPresent(true);
        row.setRiskBlockedEvidence(false);
        row.setRiskBlockedAt(null);
        row.setUserPositionPresent(false);
        row.setSourceType("AUTHORITATIVE_ANALYSIS");
        row.setSourceReference("analysisId=" + push.getAnalysisId());
        row.setReasonCodes("PUSH_CAPTURED");
        row.setTraceId(push.getTraceId());
        row.setCreatedAt(push.getPushCreateTime());
        row.setUpdatedAt(push.getPushCreateTime());
        opportunityLogMapper.insert(row);
        return row;
    }

    private TmPushRecheckLogDO insertPushRecheck(TmPushSnapshotDO push) {
        TmPushRecheckLogDO row = new TmPushRecheckLogDO();
        row.setPushId(push.getPushId());
        row.setExecutionStatus("COMPLETED");
        row.setRecheckTime(LocalDateTime.of(2026, 7, 29, 10, 5));
        row.setRecheckStatus("REVIEW_PASSED");
        row.setCurrentPrice(new BigDecimal("102"));
        row.setCurrentDataQualityScore(88);
        row.setCurrentConfusedScore(12);
        row.setCurrentAccountRiskAllowed(true);
        row.setFailReasonJson("{\"code\":\"PRIVATE_ACCOUNT_RISK_REASON\"}");
        row.setTraceId("trace-message-recheck");
        row.setCreateTime(row.getRecheckTime());
        pushRecheckLogMapper.insert(row);
        return row;
    }
}
