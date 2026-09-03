package org.example.trademodel.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.entity.ReviewResultDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "trade-model.auth.enabled=true")
@AutoConfigureMockMvc
@Transactional
@Tag("core-regression")
class UserPositionOwnershipSecurityIntegrationTest {
    private static final String USER_A = "ownership.a";
    private static final String USER_B = "ownership.b";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PersonalUserMapper personalUserMapper;
    @Autowired
    private UserPositionMapper userPositionMapper;
    @Autowired
    private OpportunityLogMapper opportunityLogMapper;
    @Autowired
    private ReviewResultMapper reviewResultMapper;

    private Long userAId;
    private Long userBId;
    private UserPositionDO openA;
    private UserPositionDO openB;
    private UserPositionDO unclaimed;
    private UserPositionDO closedA;
    private UserPositionDO closedB;

    @BeforeEach
    void setUpOwnershipFixture() {
        userAId = insertUser(USER_A);
        userBId = insertUser(USER_B);
        openA = insertPosition(userAId, "BTCUSDT", "OPEN", 1);
        openB = insertPosition(userBId, "BTCUSDT", "OPEN", 2);
        unclaimed = insertPosition(null, "LEGACYUSDT", "OPEN", 3);
        closedA = insertPosition(userAId, "AONLYUSDT", "CLOSED", 4);
        closedB = insertPosition(userBId, "BONLYUSDT", "CLOSED", 5);
    }

    @Test
    void unauthenticatedUserPositionReadsAreRejected() throws Exception {
        mockMvc.perform(get("/api/user-positions/open"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/user-positions/{id}", openA.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openListContainsOnlyCurrentOwnerOpenRowsAndNeverExposesOwnerKey() throws Exception {
        UserPositionDO partialA = insertPosition(userAId, "BTCUSDT", "PARTIALLY_CLOSED", 6);
        UserPositionDO partialB = insertPosition(userBId, "BTCUSDT", "PARTIALLY_CLOSED", 7);

        String body = mockMvc.perform(get("/api/user-positions/open").with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(String.valueOf(partialA.getId())))
                .andExpect(jsonPath("$.data[0].status").value("PARTIALLY_CLOSED"))
                .andExpect(jsonPath("$.data[1].id").value(String.valueOf(openA.getId())))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("userId", "user_id", "ownerId", "owner_id");
        assertThat(body).doesNotContain("\"id\":" + partialB.getId());
    }

    @Test
    void detailUsesExactOwnerAndAllowsOwnedClosedPosition() throws Exception {
        mockMvc.perform(get("/api/user-positions/{id}", openB.getId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.msg").value("UserPosition not found"));
        mockMvc.perform(get("/api/user-positions/{id}", unclaimed.getId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/user-positions/{id}", Long.MAX_VALUE)
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/user-positions/{id}", closedA.getId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "userId", "user_id", "ownerId", "owner_id", "accountId", "account_id",
            "principalId", "principal_id", "tenantId", "tenant_id"
    })
    void manualOpenRejectsEveryClientSuppliedOwnerAlias(String forgedField) throws Exception {
        int before = userPositionMapper.listOpenByUserId(userAId).size();
        String body = validManualOpenJson().replace("\"quantity\": 0.25,",
                "\"quantity\": 0.25, \"" + forgedField + "\": 999,");

        mockMvc.perform(post("/api/user-positions/manual-open")
                        .with(user(USER_A).roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("invalid request"));

        assertThat(userPositionMapper.listOpenByUserId(userAId)).hasSize(before);
    }

    @Test
    void manualOpenDerivesOwnerAndForcesManualOpenSafetyContract() throws Exception {
        String response = mockMvc.perform(post("/api/user-positions/manual-open")
                        .with(user(USER_A).roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validManualOpenJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.sourceType").value("MANUAL_INDEPENDENT"))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true))
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.path("id").isTextual()).isTrue();
        Long id = Long.valueOf(data.path("id").textValue());
        UserPositionDO persisted = userPositionMapper.selectByIdAndUserId(id, userAId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getUserId()).isEqualTo(userAId);
        assertThat(persisted.getSourceType()).isEqualTo("MANUAL_INDEPENDENT");
        assertThat(persisted.getStatus()).isEqualTo("OPEN");
        assertThat(data.has("userId")).isFalse();
        assertThat(data.has("ownerId")).isFalse();
    }

    @Test
    void allPositionWritesRemainCsrfProtected() throws Exception {
        mockMvc.perform(post("/api/user-positions/manual-open")
                        .with(user(USER_A).roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validManualOpenJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/user-positions/{id}/manual-close", openA.getId())
                        .with(user(USER_A).roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/position-monitor/user-positions/{id}/run", openA.getId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotCloseAndClosedOwnerGetsConflict() throws Exception {
        UserPositionDO partialA = insertPosition(userAId, "BTCUSDT", "PARTIALLY_CLOSED", 6);
        UserPositionDO partialB = insertPosition(userBId, "BTCUSDT", "PARTIALLY_CLOSED", 7);

        mockMvc.perform(post("/api/user-positions/{id}/manual-close", openB.getId())
                        .with(user(USER_A).roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeJson()))
                .andExpect(status().isNotFound());
        assertThat(userPositionMapper.selectByIdAndUserId(openB.getId(), userBId).getStatus())
                .isEqualTo("OPEN");

        mockMvc.perform(post("/api/user-positions/{id}/manual-close", partialB.getId())
                        .with(user(USER_A).roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeJson()))
                .andExpect(status().isNotFound());
        assertThat(userPositionMapper.selectByIdAndUserId(partialB.getId(), userBId).getStatus())
                .isEqualTo("PARTIALLY_CLOSED");

        mockMvc.perform(post("/api/user-positions/{id}/manual-close", partialA.getId())
                        .with(user(USER_A).roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(String.valueOf(partialA.getId())))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(post("/api/user-positions/{id}/manual-close", closedA.getId())
                        .with(user(USER_A).roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closeJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void sameOwnerSameSymbolPositionsRemainExactPositionIdIdentities() throws Exception {
        UserPositionDO second = insertPosition(userAId, "BTCUSDT", "OPEN", 6);

        mockMvc.perform(post("/api/user-positions/{id}/manual-close", openA.getId())
                        .with(user(USER_A).roles("OPERATOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(closeJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(String.valueOf(openA.getId())))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        assertThat(userPositionMapper.selectByIdAndUserId(second.getId(), userAId).getStatus())
                .isEqualTo("OPEN");
    }

    @Test
    void nonOwnerCannotMonitorReadLogsReviewOrSubmitFeedback() throws Exception {
        mockMvc.perform(post("/api/position-monitor/user-positions/{id}/run", openB.getId())
                        .with(user(USER_A).roles("OPERATOR")).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/review/positions/{id}/monitor-logs", openB.getId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/review/user-positions/{id}/summary", closedB.getId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/review/user-positions/{id}/feedback", closedB.getId())
                        .with(user(USER_A).roles("OPERATOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonOwnerFeedbackEndpointCannotOverwriteOwnerReviewOrExposeItInReviewCenter() throws Exception {
        String ownerResponse = mockMvc.perform(post("/api/review/user-positions/{id}/feedback", closedB.getId())
                        .with(user(USER_B).roles("OPERATOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewFeedbackJson("OWNER_B_OUTCOME", "owner-b-suggestion")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionId").value(closedB.getId()))
                .andReturn().getResponse().getContentAsString();
        String analysisId = objectMapper.readTree(ownerResponse).path("data").path("analysisId").asText();
        String scopeKey = "USER:" + userBId + ":POSITION:" + closedB.getId();
        ReviewResultDO beforeAttack = reviewResultMapper.selectByUserPositionScope(
                analysisId, userBId, closedB.getId(), scopeKey);
        assertThat(beforeAttack).isNotNull();
        assertThat(beforeAttack.getActualOutcome()).isEqualTo("OWNER_B_OUTCOME");

        mockMvc.perform(post("/api/review/user-positions/{id}/feedback", closedB.getId())
                        .with(user(USER_A).roles("OPERATOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewFeedbackJson("ATTACK", "must-not-write")))
                .andExpect(status().isNotFound());

        ReviewResultDO afterAttack = reviewResultMapper.selectByUserPositionScope(
                analysisId, userBId, closedB.getId(), scopeKey);
        assertThat(afterAttack.getId()).isEqualTo(beforeAttack.getId());
        assertThat(afterAttack.getActualOutcome()).isEqualTo("OWNER_B_OUTCOME");
        assertThat(afterAttack.getAdjustmentSuggestion()).isEqualTo("owner-b-suggestion");

        String centerA = mockMvc.perform(get("/api/review/center")
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String centerB = mockMvc.perform(get("/api/review/center")
                        .with(user(USER_B).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(centerA).path("data").path("ruleFeedback").toString())
                .doesNotContain("owner-b-suggestion");
        assertThat(objectMapper.readTree(centerB).path("data").path("ruleFeedback").toString())
                .contains("owner-b-suggestion");
    }

    @Test
    void unclaimedLegacyPositionIsQuarantinedFromEveryUserOperation() throws Exception {
        mockMvc.perform(post("/api/user-positions/{id}/manual-close", unclaimed.getId())
                        .with(user(USER_A).roles("OPERATOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(closeJson()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/position-monitor/user-positions/{id}/run", unclaimed.getId())
                        .with(user(USER_A).roles("OPERATOR")).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/review/positions/{id}/monitor-logs", unclaimed.getId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/review/user-positions/{id}/summary", unclaimed.getId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "LEGACYUSDT")
                        .param("positionId", String.valueOf(unclaimed.getId()))
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionSelectionStatus").value("POSITION_NOT_FOUND"));
    }

    @Test
    void ownedClosedMonitorAndOwnedOpenReviewAreConflicts() throws Exception {
        mockMvc.perform(post("/api/position-monitor/user-positions/{id}/run", closedA.getId())
                        .with(user(USER_A).roles("OPERATOR")).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/review/user-positions/{id}/summary", openA.getId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isConflict());
    }

    @Test
    void authenticatedUserCannotInvokeGlobalOpenRun() throws Exception {
        mockMvc.perform(post("/api/position-monitor/user-positions/open/run")
                        .with(user(USER_A).roles("OPERATOR")).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("system-only operation"));
    }

    @Test
    void userPositionDerivedOpportunityLogIsSanitizedIntoTheSameSharedProjectionForEveryUser() throws Exception {
        UserPositionDO linkedB = insertPosition(
                userBId, "SOLUSDT", "OPEN", 6, "plan-owned-by-b");
        OpportunityLogDO opportunity = resolvedOpportunity(linkedB);
        opportunityLogMapper.insert(opportunity);

        String userAResponse = mockMvc.perform(get("/api/opportunity-log/{id}", opportunity.getOpportunityId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lifecycleStatus").value(OpportunityLogStatus.RESOLVED))
                .andExpect(jsonPath("$.data.opportunityStatus").value(OpportunityLogStatus.MISSED_VALID))
                .andExpect(jsonPath("$.data.userPositionId").doesNotExist())
                .andExpect(jsonPath("$.data.pushId").doesNotExist())
                .andExpect(jsonPath("$.data.riskBlockedEvidence").doesNotExist())
                .andExpect(jsonPath("$.data.reasonCodes").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String userBResponse = mockMvc.perform(get("/api/opportunity-log/{id}", opportunity.getOpportunityId())
                        .with(user(USER_B).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lifecycleStatus").value(OpportunityLogStatus.RESOLVED))
                .andExpect(jsonPath("$.data.opportunityStatus").value(OpportunityLogStatus.MISSED_VALID))
                .andExpect(jsonPath("$.data.userPositionId").doesNotExist())
                .andExpect(jsonPath("$.data.pushId").doesNotExist())
                .andExpect(jsonPath("$.data.riskBlockedEvidence").doesNotExist())
                .andExpect(jsonPath("$.data.reasonCodes").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(userAResponse).path("data"))
                .isEqualTo(objectMapper.readTree(userBResponse).path("data"));
    }

    @Test
    void privateReviewLifecycleAndReasonDoNotAlterThePublicMarketProjection() throws Exception {
        OpportunityLogDO opportunity = resolvedOpportunity(openB);
        opportunity.setOpportunityId("opp-owner-review-isolation");
        opportunity.setOpportunityKey("ana-owner-review:dec-owner-review");
        opportunity.setUserPositionId(null);
        opportunity.setUserPositionPresent(false);
        opportunity.setLifecycleStatus(OpportunityLogStatus.REVIEW_REQUIRED);
        opportunity.setOpportunityStatus(null);
        opportunity.setResolvedAt(null);
        opportunity.setReasonCodes("MULTIPLE_LINKED_USER_POSITIONS");
        opportunityLogMapper.insert(opportunity);

        String userAResponse = mockMvc.perform(get("/api/opportunity-log/{id}", opportunity.getOpportunityId())
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lifecycleStatus").value(OpportunityLogStatus.RESOLVED))
                .andExpect(jsonPath("$.data.opportunityStatus").value(OpportunityLogStatus.MISSED_VALID))
                .andExpect(jsonPath("$.data.userPositionId").doesNotExist())
                .andExpect(jsonPath("$.data.pushId").doesNotExist())
                .andExpect(jsonPath("$.data.reasonCodes").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String userBResponse = mockMvc.perform(get("/api/opportunity-log/{id}", opportunity.getOpportunityId())
                        .with(user(USER_B).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lifecycleStatus").value(OpportunityLogStatus.RESOLVED))
                .andExpect(jsonPath("$.data.opportunityStatus").value(OpportunityLogStatus.MISSED_VALID))
                .andExpect(jsonPath("$.data.userPositionId").doesNotExist())
                .andExpect(jsonPath("$.data.pushId").doesNotExist())
                .andExpect(jsonPath("$.data.reasonCodes").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(userAResponse).path("data"))
                .isEqualTo(objectMapper.readTree(userBResponse).path("data"));
    }

    @Test
    void dashboardRiskAndReviewCenterRemainOwnerScoped() throws Exception {
        insertPosition(userAId, "ETHUSDT", "PARTIALLY_CLOSED", 6);

        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "BTCUSDT")
                        .param("positionId", String.valueOf(openB.getId()))
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionSelectionStatus").value("POSITION_NOT_FOUND"));

        mockMvc.perform(get("/api/account-risk/user-positions/current")
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openPositionCount").value(1))
                .andExpect(jsonPath("$.data.partiallyClosedPositionCount").value(1))
                .andExpect(jsonPath("$.data.includedPositionCount").value(2));

        String reviewBody = mockMvc.perform(get("/api/review/center")
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode positionReviews = objectMapper.readTree(reviewBody).path("data").path("positionReviews");
        assertThat(positionReviews.toString()).contains("AONLYUSDT").doesNotContain("BONLYUSDT");
    }

    @Test
    void missingCanonicalUserAndMalformedPositionIdFailClosed() throws Exception {
        mockMvc.perform(get("/api/user-positions/open")
                        .with(user("not.registered").roles("OPERATOR")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/user-positions/not-a-number")
                        .with(user(USER_A).roles("OPERATOR")))
                .andExpect(status().isBadRequest());
    }

    private Long insertUser(String username) {
        PersonalUserDO user = new PersonalUserDO();
        user.setUsername(username);
        user.setPasswordHash("{noop}test-only-password");
        user.setCreatedAt(LocalDateTime.now());
        personalUserMapper.insert(user);
        return user.getId();
    }

    private UserPositionDO insertPosition(Long userId, String symbol, String status, int minute) {
        return insertPosition(userId, symbol, status, minute, null);
    }

    private UserPositionDO insertPosition(Long userId,
                                          String symbol,
                                          String status,
                                          int minute,
                                          String sourceRefId) {
        LocalDateTime openedAt = LocalDateTime.of(2026, 7, 1, 8, minute);
        UserPositionDO row = new UserPositionDO();
        row.setUserId(userId);
        row.setAssetSymbol(symbol);
        row.setSide("LONG");
        row.setStatus(status);
        row.setEntryPrice(new BigDecimal("100"));
        row.setQuantity(new BigDecimal("0.25"));
        row.setLeverage(new BigDecimal("2"));
        row.setStopLoss(new BigDecimal("90"));
        row.setTakeProfit(new BigDecimal("120"));
        row.setOpenedAt(openedAt);
        if ("CLOSED".equals(status)) {
            row.setClosedAt(openedAt.plusHours(1));
            row.setClosePrice(new BigDecimal("110"));
            row.setCloseReason("manual fixture close");
        }
        row.setSourceType("MANUAL_INDEPENDENT");
        row.setSourceRefId(sourceRefId);
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

    private static OpportunityLogDO resolvedOpportunity(UserPositionDO linkedPosition) {
        LocalDateTime anchor = LocalDateTime.of(2026, 7, 1, 8, 0);
        OpportunityLogDO row = new OpportunityLogDO();
        row.setOpportunityId("opp-owner-isolation");
        row.setOpportunityKey("ana-owner-isolation:dec-owner-isolation");
        row.setAnalysisId("ana-owner-isolation");
        row.setDecisionId("dec-owner-isolation");
        row.setExecutionPlanId(linkedPosition.getSourceRefId());
        row.setUserPositionId(linkedPosition.getId());
        row.setSymbol(linkedPosition.getAssetSymbol());
        row.setTimeframe("1h");
        row.setDirection("LONG");
        row.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
        row.setOpportunityStatus(OpportunityLogStatus.EXECUTED_VALID);
        row.setAnchorTime(anchor);
        row.setEvaluationAsOf(anchor.plusHours(2));
        row.setResolvedAt(anchor.plusHours(2));
        row.setTargetHit(true);
        row.setInvalidationHit(false);
        row.setTargetHitAt(anchor.plusHours(1));
        row.setHitOrder(OpportunityLogStatus.TARGET_FIRST);
        row.setPushPresent(false);
        row.setRiskBlockedEvidence(false);
        row.setUserPositionPresent(true);
        row.setSourceType("AUTHORITATIVE_ANALYSIS");
        row.setSourceReference("analysisId=ana-owner-isolation");
        row.setReasonCodes("TARGET_FIRST");
        row.setCreatedAt(anchor);
        row.setUpdatedAt(anchor.plusHours(2));
        return row;
    }

    private static String validManualOpenJson() {
        return """
                {
                  "submission_id": "ownership-open:manual-position",
                  "asset_symbol": "ETHUSDT",
                  "side": "LONG",
                  "entry_price": 2000,
                  "quantity": 0.25,
                  "leverage": 2,
                  "opened_at": "2024-01-01T10:00:00",
                  "source_type": "MANUAL_POSITION",
                  "stop_loss": 1900,
                  "take_profit": 2300
                }
                """;
    }

    private static String closeJson() {
        return "{\"submission_id\": \"ownership-close:manual-position\", \"close_price\": 105, \"closed_at\": \"2026-07-01T09:00:00\", "
                + "\"close_reason\": \"manual close\"}";
    }

    private static String reviewFeedbackJson(String outcome, String suggestion) {
        return """
                {
                  "errorType": "RULE_TOO_STRICT",
                  "actualOutcome": "%s",
                  "adjustmentSuggestion": "%s"
                }
                """.formatted(outcome, suggestion);
    }
}
