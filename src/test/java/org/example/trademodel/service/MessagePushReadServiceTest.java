package org.example.trademodel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.messagepush.MessageListDTO;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PushDetailDTO;
import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessagePushReadServiceTest {
    private static final Long USER_ID = 11L;
    private final OpportunityLogMapper opportunityLogMapper = mock(OpportunityLogMapper.class);
    private final PositionMonitorLogMapper positionMonitorLogMapper = mock(PositionMonitorLogMapper.class);
    private final UserPositionMapper userPositionMapper = mock(UserPositionMapper.class);
    private final MessagePushReadService service = new MessagePushReadService(
            opportunityLogMapper,
            positionMonitorLogMapper,
            userPositionMapper);

    {
        service.setClock(Clock.fixed(Instant.parse("2026-07-29T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void publicMessageIdsRemainJsonStringsWithoutExposingPrivatePushIdentity() throws Exception {
        String largeId = "9007199254740993";
        MessageListDTO.MessageItem item = new MessageListDTO.MessageItem(
                largeId,
                new MessageListDTO.SourceIdentity("OPPORTUNITY", "opp-precision", "ana-precision", null),
                "BTCUSDT",
                "PENDING_EVALUATION",
                LocalDateTime.of(2026, 7, 29, 9, 0),
                true,
                true,
                true,
                true);

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(item));

        assertThat(json.path("messageId").isTextual()).isTrue();
        assertThat(json.path("messageId").textValue()).isEqualTo(largeId);
        assertThat(json.has("pushId")).isFalse();
    }

    @Test
    void publicMapperProjectionsDoNotSelectPrivateRiskColumns() {
        String opportunitySql = OpportunityLogMapper.PUBLIC_MESSAGE_SELECT.toLowerCase(Locale.ROOT);
        String opportunityApiSql = OpportunityLogMapper.PUBLIC_API_SELECT.toLowerCase(Locale.ROOT);
        String opportunityEvaluationSql =
                OpportunityLogMapper.PUBLIC_EVALUATION_SELECT.toLowerCase(Locale.ROOT);
        String opportunityEvaluationUpdateSql =
                OpportunityLogMapper.PUBLIC_EVALUATION_UPDATE.toLowerCase(Locale.ROOT);
        String opportunityPredicate =
                OpportunityLogMapper.PUBLIC_PROJECTION_PREDICATE.toLowerCase(Locale.ROOT);
        String ownerSql = PositionMonitorLogMapper.OWNER_SCOPED_SELECT.toLowerCase(Locale.ROOT);

        assertThat(opportunitySql)
                .contains("opportunity_id", "analysis_id", "symbol")
                .doesNotContain(
                        "push_id",
                        "user_position_id",
                        "risk_blocked_evidence",
                        "risk_blocked_at",
                        "reason_codes",
                        "fail_reason_json",
                        "current_account_risk_allowed");
        assertThat(opportunityPredicate)
                .contains("opportunity_id is not null")
                .doesNotContain("user_position", "push_id", "risk");
        assertThat(opportunityApiSql)
                .contains("opportunity_id", "analysis_id", "symbol", "opportunity_status")
                .doesNotContain(
                        "push_id",
                        "user_position_id",
                        "risk_blocked_evidence",
                        "risk_blocked_at",
                        "reason_codes",
                        "source_reference",
                        "trace_id",
                        "fail_reason_json",
                        "current_account_risk_allowed");
        assertThat(opportunityEvaluationSql)
                .contains("opportunity_id", "analysis_id", "symbol", "opportunity_status")
                .doesNotContain(
                        "push_id",
                        "user_position_id",
                        "user_position_present",
                        "push_present",
                        "risk_blocked_evidence",
                        "risk_blocked_at",
                        "fail_reason_json",
                        "current_account_risk_allowed");
        assertThat(opportunityEvaluationUpdateSql)
                .contains("opportunity_id", "lifecycle_status", "opportunity_status", "market_data_source")
                .doesNotContain(
                        "push_id",
                        "user_position_id",
                        "user_position_present",
                        "push_present",
                        "risk_blocked_evidence",
                        "risk_blocked_at",
                        "fail_reason_json",
                        "current_account_risk_allowed");
        assertThat(ownerSql)
                .contains("inner join tm_user_position", "p.user_id = #{userid}")
                .doesNotContain("latestbysymbol", "order by");
    }

    @Test
    void emptyAndErrorAreDistinctAndErrorNeverCarriesAnEmptySuccessList() {
        when(opportunityLogMapper.queryPublicApi(
                null, null, null, null, null, null, null, null,
                MessagePushReadService.DEFAULT_LIMIT)).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of());

        MessageListDTO empty = service.listForUser(USER_ID, null);

        assertThat(empty.state()).isEqualTo(MessageReadState.EMPTY);
        assertThat(empty.items()).isEmpty();

        when(opportunityLogMapper.queryPublicApi(
                null, null, null, null, null, null, null, null,
                MessagePushReadService.DEFAULT_LIMIT))
                .thenThrow(new IllegalStateException("database unavailable"));

        MessageListDTO error = service.listForUser(USER_ID, null);

        assertThat(error.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(error.items()).isNull();
        assertThat(error.reason()).isEqualTo("MESSAGE_READ_FAILED");
    }

    @Test
    void absentPublicOpportunityIsMissingWithoutConsultingPrivateSources() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-missing")).thenReturn(null);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-missing");

        assertThat(detail.state()).isEqualTo(MessageReadState.MISSING);
        assertThat(detail.reason()).isEqualTo("MESSAGE_NOT_FOUND");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void mismatchedPublicOpportunityIdentityIsError() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-expected"))
                .thenReturn(publicOpportunity(
                        "opp-other", OpportunityLogStatus.RESOLVED, OpportunityLogStatus.MISSED_VALID));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-expected");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("PUBLIC_OPPORTUNITY_IDENTITY_INVALID");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void malformedIdentityIsMissingWithoutSymbolOrTimeFallback() {
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "BTCUSDT");

        assertThat(detail.state()).isEqualTo(MessageReadState.MISSING);
        assertThat(detail.reason()).isEqualTo("MESSAGE_NOT_FOUND");
        verifyNoInteractions(opportunityLogMapper, positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void completePublicOpportunityReturnsReadyWithoutReadingPrivateState() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-public-ready"))
                .thenReturn(publicOpportunity(
                        "opp-public-ready", OpportunityLogStatus.RESOLVED, OpportunityLogStatus.MISSED_VALID));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-public-ready");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(projection.publicLifecycle()).isEqualTo(OpportunityLogStatus.RESOLVED);
        assertThat(projection.publicStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(projection.publicTimestamp()).isEqualTo(LocalDateTime.of(2026, 7, 29, 10, 0));
        assertThat(projection.publicDescription()).isEqualTo("BTCUSDT LONG 1H");
        assertThat(detail.reason()).isNull();
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void completePublicOpportunityUsesNormalizedPublicState() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-public-normalized"))
                .thenReturn(publicOpportunity(
                        "opp-public-normalized", "resolved", "missed_valid"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-public-normalized");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(projection.publicLifecycle()).isEqualTo(OpportunityLogStatus.RESOLVED);
        assertThat(projection.publicStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
    }

    @Test
    void resolvedOpportunityMissingPublicMarketEvidenceIsPartial() {
        OpportunityLogPublicDTO complete = publicOpportunity(
                "opp-public-evidence", OpportunityLogStatus.RESOLVED, OpportunityLogStatus.MISSED_VALID);
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-public-evidence"))
                .thenReturn(withPublicEvidence(complete, complete.targetPrice(), null));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-public-evidence");

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.missingFields()).containsExactly("publicEvidence.marketDataSource");
        assertThat(detail.reason()).isEqualTo("PUBLIC_OPPORTUNITY_INCOMPLETE");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void contradictoryPublicMarketEvidenceIsError() {
        OpportunityLogPublicDTO complete = publicOpportunity(
                "opp-public-invalid-evidence",
                OpportunityLogStatus.RESOLVED,
                OpportunityLogStatus.MISSED_VALID);
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-public-invalid-evidence"))
                .thenReturn(withPublicEvidence(
                        complete,
                        new BigDecimal("90"),
                        complete.marketDataSource()));

        PushDetailDTO detail = service.findPushDetailForUser(
                USER_ID, "opp-public-invalid-evidence");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("PUBLIC_MARKET_EVIDENCE_INVALID");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void incompletePublicOpportunityIsPartialAndCannotBecomeReady() {
        OpportunityLogPublicDTO opportunity = publicOpportunity(
                "opp-public-partial", null, null);
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-public-partial"))
                .thenReturn(opportunity);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-public-partial");

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.missingFields()).contains("publicLifecycle");
        assertThat(detail.reason()).isEqualTo("PUBLIC_OPPORTUNITY_INCOMPLETE");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void unknownPublicLifecycleIsErrorAndCannotPassThrough() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-lifecycle-invalid"))
                .thenReturn(publicOpportunity("opp-lifecycle-invalid", "PRIVATE_RECHECK_READY", null));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-lifecycle-invalid");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("PUBLIC_LIFECYCLE_INVALID");
    }

    @Test
    void pendingPublicEvaluationIsPartialWithoutPrivateRecheckOracle() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-pending"))
                .thenReturn(publicOpportunity(
                        "opp-pending", OpportunityLogStatus.PENDING_EVALUATION, null));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-pending");

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.missingFields()).containsExactly("publicEvaluation");
        assertThat(detail.reason()).isEqualTo("PUBLIC_EVALUATION_PENDING");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void invalidPublicStatusIsError() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-status-invalid"))
                .thenReturn(publicOpportunity(
                        "opp-status-invalid", OpportunityLogStatus.RESOLVED, "BLOCKED_BY_RISK_VALID"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-status-invalid");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("PUBLIC_STATUS_INVALID");
    }

    @Test
    void publicStateConflictIsError() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-state-conflict"))
                .thenReturn(publicOpportunity(
                        "opp-state-conflict",
                        OpportunityLogStatus.PENDING_EVALUATION,
                        OpportunityLogStatus.MISSED_VALID));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-state-conflict");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("PUBLIC_STATE_CONFLICT");
    }

    @Test
    void samePublicOpportunityIsIdenticalAcrossUsersWithDifferentPrivateState() {
        OpportunityLogPublicDTO opportunity = publicOpportunity(
                "opp-cross-user", OpportunityLogStatus.RESOLVED, OpportunityLogStatus.MISSED_VALID);
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-cross-user"))
                .thenReturn(opportunity);

        PushDetailDTO userA = service.findPushDetailForUser(11L, "opp-cross-user");
        PushDetailDTO userB = service.findPushDetailForUser(22L, "opp-cross-user");

        assertThat(userA).isEqualTo(userB);
        verify(opportunityLogMapper, org.mockito.Mockito.times(2))
                .selectPublicApiByOpportunityId("opp-cross-user");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void opportunityPublicProjectionNeverSerializesRiskDerivedStatusOrPrivateFields() throws Exception {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-public-projection"))
                .thenReturn(publicOpportunity(
                        "opp-public-projection",
                        OpportunityLogStatus.RESOLVED,
                        OpportunityLogStatus.MISSED_VALID));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-public-projection");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(detail));

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        assertThat(json.path("publicStatus").asText()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(json.path("sourceIdentity").has("positionId")).isFalse();
        assertThat(json.has("pushId")).isFalse();
        assertThat(json.has("originalSnapshot")).isFalse();
        assertThat(json.has("currentRecheck")).isFalse();
        assertThat(json.has("changeReason")).isFalse();
        assertThat(json.toString()).doesNotContain(
                "recheckStatus",
                "currentAccountRiskAllowed",
                "failReasonJson",
                "PRIVATE_ACCOUNT_RISK_REASON",
                "riskLevel",
                "RISK_BLOCKED",
                "positionId",
                "currentPrice",
                "dataQualityScore",
                "confusedScore");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void missingPublicStatusReturnsPartialWithoutReadingPrivateStatus() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-missing-public-status"))
                .thenReturn(publicOpportunity("opp-missing-public-status", null, null));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-missing-public-status");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(projection.publicStatus()).isNull();
        assertThat(projection.missingFields()).containsExactly("publicLifecycle");
        assertThat(detail.reason()).isEqualTo("PUBLIC_OPPORTUNITY_INCOMPLETE");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void missingPublicTimestampReturnsPartialBeforeAnyPrivateRead() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-missing-public-time"))
                .thenReturn(publicOpportunity(
                        "opp-missing-public-time",
                        OpportunityLogStatus.PENDING_EVALUATION,
                        null,
                        "BTCUSDT",
                        null,
                        "LONG",
                        "1h"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-missing-public-time");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(projection.missingFields()).containsExactly("publicTimestamp");
        assertThat(detail.reason()).isEqualTo("PUBLIC_OPPORTUNITY_INCOMPLETE");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void missingPublicDescriptionReturnsPartialBeforeAnyPrivateRead() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-missing-public-description"))
                .thenReturn(publicOpportunity(
                        "opp-missing-public-description",
                        OpportunityLogStatus.PENDING_EVALUATION,
                        null,
                        null,
                        LocalDateTime.of(2026, 7, 29, 10, 0),
                        "LONG",
                        "1h"));

        PushDetailDTO detail = service.findPushDetailForUser(
                USER_ID, "opp-missing-public-description");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(projection.missingFields()).containsExactly("symbol");
        assertThat(detail.reason()).isEqualTo("PUBLIC_OPPORTUNITY_INCOMPLETE");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void safeTerminalOpportunityStatusTakesPrecedenceOverLifecycleStatus() {
        when(opportunityLogMapper.selectPublicApiByOpportunityId("opp-public-terminal"))
                .thenReturn(publicOpportunity(
                        "opp-public-terminal",
                        OpportunityLogStatus.RESOLVED,
                        OpportunityLogStatus.MISSED_VALID));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-public-terminal");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(projection.publicStatus()).isEqualTo("MISSED_VALID");
        verifyNoInteractions(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void positionRiskListUsesAuthoritativeOwnedPositionSymbol() {
        PositionMonitorLogDO risk = validMonitor(301L, 401L);
        UserPositionDO position = ownedPosition(401L, "BTCUSDT");
        when(opportunityLogMapper.queryPublicApi(
                null, null, null, null, null, null, null, null,
                MessagePushReadService.DEFAULT_LIMIT)).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of(risk));
        when(userPositionMapper.selectByIdAndUserId(401L, USER_ID)).thenReturn(position);
        when(positionMonitorLogMapper.selectLatestByPositionIdAndUserId(401L, USER_ID))
                .thenReturn(risk);

        MessageListDTO result = service.listForUser(USER_ID, null);

        assertThat(result.state()).isEqualTo(MessageReadState.READY);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.symbol()).isEqualTo("BTCUSDT");
            assertThat(item.sourceIdentity().positionId()).isEqualTo("401");
        });
    }

    @Test
    void incompletePositionRiskListCannotBecomeReady() {
        PositionMonitorLogDO risk = positionRisk(302L, 402L);
        UserPositionDO position = ownedPosition(402L, "BTCUSDT");
        when(opportunityLogMapper.queryPublicApi(
                null, null, null, null, null, null, null, null,
                MessagePushReadService.DEFAULT_LIMIT)).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of(risk));
        when(userPositionMapper.selectByIdAndUserId(402L, USER_ID)).thenReturn(position);
        when(positionMonitorLogMapper.selectLatestByPositionIdAndUserId(402L, USER_ID))
                .thenReturn(risk);

        MessageListDTO result = service.listForUser(USER_ID, null);

        assertThat(result.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(result.items()).isEmpty();
        assertThat(result.reason()).isEqualTo("SOURCE_RECORD_INCOMPLETE");
    }

    @Test
    void invalidPositionRiskListReturnsError() {
        PositionMonitorLogDO risk = validMonitor(303L, 403L);
        risk.setRiskLevel("CRITICAL");
        when(opportunityLogMapper.queryPublicApi(
                null, null, null, null, null, null, null, null,
                MessagePushReadService.DEFAULT_LIMIT)).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of(risk));
        when(userPositionMapper.selectByIdAndUserId(403L, USER_ID))
                .thenReturn(ownedPosition(403L, "BTCUSDT"));
        when(positionMonitorLogMapper.selectLatestByPositionIdAndUserId(403L, USER_ID))
                .thenReturn(risk);

        MessageListDTO result = service.listForUser(USER_ID, null);

        assertThat(result.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(result.items()).isNull();
        assertThat(result.reason()).isEqualTo("POSITION_MONITOR_RISK_LEVEL_INVALID");
    }

    @Test
    void independentMonitorSemanticsAreValidatedWithoutLegacyFallback() {
        PositionMonitorLogDO risk = validMonitor(333L, 433L);
        stubPositionRiskListAndDetail(risk, risk, ownedPosition(433L, "BTCUSDT"));

        risk.setEntryLogicStatus("UNKNOWN");
        assertPositionRiskError("POSITION_ENTRY_LOGIC_STATUS_INVALID");

        risk.setEntryLogicStatus("WEAKENED");
        risk.setReversalStatus("UNKNOWN");
        assertPositionRiskError("POSITION_REVERSAL_STATUS_INVALID");

        risk.setReversalStatus("NO_REVERSAL");
        risk.setRiskChangeReason("UNKNOWN");
        assertPositionRiskError("POSITION_RISK_CHANGE_REASON_INVALID");
    }

    @Test
    void monitorConclusionAndSuggestedActionMustFormALegalPair() {
        PositionMonitorLogDO risk = validMonitor(335L, 435L);
        risk.setSuggestedAction("CONTINUE_HOLD");
        stubPositionRiskListAndDetail(risk, risk, ownedPosition(435L, "BTCUSDT"));

        MessageListDTO list = service.listForUser(USER_ID, null);
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "335");

        assertThat(list.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(list.reason()).isEqualTo("POSITION_MONITOR_STATE_CONFLICT");
        assertThat(detail.reason()).isEqualTo("POSITION_MONITOR_STATE_CONFLICT");
    }

    @Test
    void missingIndependentMonitorSemanticsCannotBecomeReady() {
        PositionMonitorLogDO risk = validMonitor(334L, 434L);
        risk.setEntryLogicStatus(null);
        risk.setReversalStatus(null);
        risk.setRiskChangeReason(null);
        risk.setMarkPriceSource(null);
        stubPositionRiskListAndDetail(risk, risk, ownedPosition(434L, "ETHUSDT"));

        MessageListDTO list = service.listForUser(USER_ID, null);
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "334");

        assertThat(list.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.missingFields()).contains(
                "originalSnapshot.markPriceSource",
                "originalSnapshot.entryLogicStatus",
                "originalSnapshot.reversalStatus",
                "originalSnapshot.riskChangeReason");
    }

    @Test
    void positionRiskListIdentityMismatchIsError() {
        PositionMonitorLogDO risk = validMonitor(304L, 404L);
        when(opportunityLogMapper.queryPublicApi(
                null, null, null, null, null, null, null, null,
                MessagePushReadService.DEFAULT_LIMIT)).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of(risk));
        when(userPositionMapper.selectByIdAndUserId(404L, USER_ID))
                .thenReturn(ownedPosition(999L, "BTCUSDT"));

        MessageListDTO result = service.listForUser(USER_ID, null);

        assertThat(result.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(result.items()).isNull();
        assertThat(result.reason()).isEqualTo("POSITION_RISK_IDENTITY_MISMATCH");
    }

    @Test
    void historicalReadyAndLatestInvalidAreConsistentlyError() {
        PositionMonitorLogDO original = validMonitor(325L, 425L);
        PositionMonitorLogDO latest = validMonitor(326L, 425L);
        latest.setCreatedAt(LocalDateTime.of(2026, 7, 29, 11, 30));
        latest.setMonitorConclusion("UNKNOWN_LOGIC");
        stubPositionRiskListAndDetail(original, latest, ownedPosition(425L, "BTCUSDT"));

        MessageListDTO list = service.listForUser(USER_ID, null);
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "325");

        assertThat(list.state()).isEqualTo(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(list.reason()).isEqualTo("POSITION_MONITOR_CONCLUSION_INVALID");
        assertThat(detail.reason()).isEqualTo("POSITION_MONITOR_CONCLUSION_INVALID");
    }

    @Test
    void historicalReadyAndLatestIncompleteAreConsistentlyPartial() {
        PositionMonitorLogDO original = validMonitor(327L, 427L);
        PositionMonitorLogDO latest = validMonitor(328L, 427L);
        latest.setCreatedAt(LocalDateTime.of(2026, 7, 29, 11, 30));
        latest.setReason(null);
        stubPositionRiskListAndDetail(original, latest, ownedPosition(427L, "ETHUSDT"));

        MessageListDTO list = service.listForUser(USER_ID, null);
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "327");

        assertThat(list.state()).isEqualTo(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(list.items()).isEmpty();
        assertThat(detail.missingFields()).contains("currentRecheck.reason");
    }

    @Test
    void historicalAndLatestValidAreConsistentlyReady() {
        PositionMonitorLogDO original = validMonitor(329L, 429L);
        PositionMonitorLogDO latest = validMonitor(330L, 429L);
        latest.setCreatedAt(LocalDateTime.of(2026, 7, 29, 11, 30));
        stubPositionRiskListAndDetail(original, latest, ownedPosition(429L, "SOLUSDT"));

        MessageListDTO list = service.listForUser(USER_ID, null);
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "329");

        assertThat(list.state()).isEqualTo(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(list.items()).singleElement()
                .extracting(MessageListDTO.MessageItem::messageId)
                .isEqualTo("329");
    }

    @Test
    void historicalAndLatestIdentityMismatchAreConsistentlyError() {
        PositionMonitorLogDO original = validMonitor(331L, 431L);
        PositionMonitorLogDO latest = validMonitor(332L, 999L);
        latest.setCreatedAt(LocalDateTime.of(2026, 7, 29, 11, 30));
        stubPositionRiskListAndDetail(original, latest, ownedPosition(431L, "BTCUSDT"));

        MessageListDTO list = service.listForUser(USER_ID, null);
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "331");

        assertThat(list.state()).isEqualTo(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(list.reason()).isEqualTo("POSITION_RISK_IDENTITY_MISMATCH");
        assertThat(detail.reason()).isEqualTo("POSITION_RISK_IDENTITY_MISMATCH");
    }

    @Test
    void completeOwnedPositionRiskReturnsReadyWithStringSafeIdentity() throws Exception {
        long largeLogId = 9_007_199_254_740_993L;
        PositionMonitorLogDO monitor = validMonitor(largeLogId, 401L);
        stubPositionRisk(monitor, monitor, ownedPosition(401L, "BTCUSDT"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, Long.toString(largeLogId));
        JsonNode json = new ObjectMapper().findAndRegisterModules()
                .readTree(new ObjectMapper().findAndRegisterModules().writeValueAsString(detail));

        assertThat(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(detail).isInstanceOf(PushDetailDTO.PositionRiskPrivateProjection.class);
        assertThat(json.path("messageId").isTextual()).isTrue();
        assertThat(json.path("messageId").asText()).isEqualTo(Long.toString(largeLogId));
        assertThat(json.path("sourceIdentity").path("positionId").asText()).isEqualTo("401");
    }

    @Test
    void missingCurrentMonitorIsMissingRatherThanReady() {
        PositionMonitorLogDO original = validMonitor(310L, 410L);
        stubPositionRiskListAndDetail(original, null, ownedPosition(410L, "BTCUSDT"));

        MessageListDTO list = service.listForUser(USER_ID, null);
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "310");

        assertThat(list.state()).isEqualTo(detail.state()).isEqualTo(MessageReadState.MISSING);
        assertThat(list.reason()).isEqualTo("CURRENT_MONITOR_STATE_MISSING");
        assertThat(detail.reason()).isEqualTo("CURRENT_MONITOR_STATE_MISSING");
    }

    @Test
    void nullLogicStatusIsPartialAndRowExistenceCannotBecomeReady() {
        PositionMonitorLogDO monitor = validMonitor(311L, 411L);
        monitor.setMonitorConclusion(null);
        stubPositionRisk(monitor, monitor, ownedPosition(411L, "BTCUSDT"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "311");

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.missingFields()).contains("originalSnapshot.status");
    }

    @Test
    void unknownLogicStatusIsError() {
        PositionMonitorLogDO monitor = validMonitor(312L, 412L);
        monitor.setMonitorConclusion("UNKNOWN_LOGIC");
        stubPositionRisk(monitor, monitor, ownedPosition(412L, "BTCUSDT"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "312");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("POSITION_RISK_SOURCE_MISMATCH");
    }

    @Test
    void nullTimestampAndMissingRiskStatusArePartial() {
        PositionMonitorLogDO monitor = validMonitor(313L, 413L);
        monitor.setCreatedAt(null);
        monitor.setRiskLevel(null);
        monitor.setRiskSnapshot("{\"riskBlocked\":true}");
        stubPositionRisk(monitor, monitor, ownedPosition(413L, "BTCUSDT"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "313");

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.missingFields())
                .contains("originalSnapshot.checkedAt", "originalSnapshot.riskLevel",
                        "originalSnapshot.riskSnapshot.riskLevel");
    }

    @Test
    void unknownRiskStatusAndMalformedRiskJsonAreErrors() {
        PositionMonitorLogDO invalidRisk = validMonitor(314L, 414L);
        invalidRisk.setRiskLevel("CRITICAL");
        stubPositionRisk(invalidRisk, invalidRisk, ownedPosition(414L, "BTCUSDT"));

        PushDetailDTO unknown = service.findPushDetailForUser(USER_ID, "314");

        assertThat(unknown.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(unknown.reason()).isEqualTo("POSITION_MONITOR_RISK_LEVEL_INVALID");

        PositionMonitorLogDO malformed = validMonitor(315L, 415L);
        malformed.setRiskSnapshot("{not-json");
        stubPositionRisk(malformed, malformed, ownedPosition(415L, "ETHUSDT"));

        PushDetailDTO malformedResult = service.findPushDetailForUser(USER_ID, "315");

        assertThat(malformedResult.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(malformedResult.reason()).isEqualTo("POSITION_MONITOR_RISK_DATA_MALFORMED");
    }

    @Test
    void accountRiskSnapshotMayDifferFromCompositeMonitorRisk() {
        PositionMonitorLogDO monitor = validMonitor(323L, 423L);
        monitor.setRiskLevel("HIGH");
        monitor.setRiskSnapshot("{\"riskLevel\":\"MEDIUM\",\"riskBlocked\":false}");
        stubPositionRisk(monitor, monitor, ownedPosition(423L, "BTCUSDT"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "323");

        assertThat(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(detail.reason()).isNull();
    }

    @Test
    void unknownAccountRiskSnapshotLevelIsError() {
        PositionMonitorLogDO monitor = validMonitor(324L, 424L);
        monitor.setRiskSnapshot("{\"riskLevel\":\"CRITICAL\",\"riskBlocked\":true}");
        stubPositionRisk(monitor, monitor, ownedPosition(424L, "BTCUSDT"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "324");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("POSITION_MONITOR_RISK_DATA_INVALID");
    }

    @Test
    void missingConclusionIsPartialAndFutureTimestampIsError() {
        PositionMonitorLogDO incomplete = validMonitor(316L, 416L);
        incomplete.setReason(null);
        stubPositionRisk(incomplete, incomplete, ownedPosition(416L, "BTCUSDT"));

        PushDetailDTO partial = service.findPushDetailForUser(USER_ID, "316");

        assertThat(partial.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(partial.missingFields()).contains("originalSnapshot.reason");

        PositionMonitorLogDO future = validMonitor(317L, 417L);
        future.setCreatedAt(LocalDateTime.of(2026, 7, 29, 13, 0));
        stubPositionRisk(future, future, ownedPosition(417L, "BTCUSDT"));

        PushDetailDTO error = service.findPushDetailForUser(USER_ID, "317");

        assertThat(error.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(error.reason()).isEqualTo("POSITION_MONITOR_TIMESTAMP_INVALID");
    }

    @Test
    void monitorPositionIdentityMismatchIsError() {
        PositionMonitorLogDO original = validMonitor(318L, 418L);
        PositionMonitorLogDO latest = validMonitor(319L, 999L);
        stubPositionRisk(original, latest, ownedPosition(418L, "BTCUSDT"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "318");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("POSITION_RISK_IDENTITY_MISMATCH");
    }

    @Test
    void ownerIsolationFailureIsMissingAndNeverFallsBackBySymbol() {
        PositionMonitorLogDO original = validMonitor(320L, 420L);
        when(positionMonitorLogMapper.selectRiskByIdAndUserId(320L, USER_ID)).thenReturn(original);
        when(userPositionMapper.selectByIdAndUserId(420L, USER_ID)).thenReturn(null);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "320");

        assertThat(detail.state()).isEqualTo(MessageReadState.MISSING);
        assertThat(detail.reason()).isEqualTo("MESSAGE_NOT_FOUND");
        verify(positionMonitorLogMapper, never())
                .selectLatestByPositionIdAndUserId(420L, USER_ID);
    }

    @Test
    void lifecycleConflictIsErrorWhileValidClosedPositionRemainsReadable() {
        PositionMonitorLogDO conflictMonitor = validMonitor(321L, 421L);
        UserPositionDO conflictPosition = ownedPosition(421L, "BTCUSDT");
        conflictPosition.setClosedAt(LocalDateTime.of(2026, 7, 29, 11, 30));
        stubPositionRisk(conflictMonitor, conflictMonitor, conflictPosition);

        PushDetailDTO conflict = service.findPushDetailForUser(USER_ID, "321");

        assertThat(conflict.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(conflict.reason()).isEqualTo("POSITION_LIFECYCLE_CONFLICT");

        PositionMonitorLogDO closedMonitor = validMonitor(322L, 422L);
        UserPositionDO closedPosition = ownedPosition(422L, "ETHUSDT");
        closedPosition.setStatus("CLOSED");
        closedPosition.setClosedAt(LocalDateTime.of(2026, 7, 29, 11, 30));
        stubPositionRisk(closedMonitor, closedMonitor, closedPosition);

        PushDetailDTO closed = service.findPushDetailForUser(USER_ID, "322");

        assertThat(closed.state()).isEqualTo(MessageReadState.READY);
    }

    private void stubPositionRisk(
            PositionMonitorLogDO original,
            PositionMonitorLogDO latest,
            UserPositionDO position) {
        when(positionMonitorLogMapper.selectRiskByIdAndUserId(original.getLogId(), USER_ID))
                .thenReturn(original);
        when(userPositionMapper.selectByIdAndUserId(original.getPositionId(), USER_ID))
                .thenReturn(position);
        if (position != null) {
            when(positionMonitorLogMapper.selectLatestByPositionIdAndUserId(position.getId(), USER_ID))
                    .thenReturn(latest);
        }
    }

    private void assertPositionRiskError(String reason) {
        MessageListDTO list = service.listForUser(USER_ID, null);
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "333");

        assertThat(list.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(list.reason()).isEqualTo(reason);
        assertThat(detail.reason()).isEqualTo(reason);
    }

    private void stubPositionRiskListAndDetail(
            PositionMonitorLogDO original,
            PositionMonitorLogDO latest,
            UserPositionDO position) {
        when(opportunityLogMapper.queryPublicApi(
                null, null, null, null, null, null, null, null,
                MessagePushReadService.DEFAULT_LIMIT)).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of(original));
        stubPositionRisk(original, latest, position);
    }

    private static OpportunityLogPublicDTO publicOpportunity(
            String opportunityId,
            String lifecycle,
            String status) {
        return publicOpportunity(
                opportunityId,
                lifecycle,
                status,
                "BTCUSDT",
                LocalDateTime.of(2026, 7, 29, 10, 0),
                "LONG",
                "1h");
    }

    private static OpportunityLogPublicDTO publicOpportunity(
            String opportunityId,
            String lifecycle,
            String status,
            String symbol,
            LocalDateTime timestamp,
            String direction,
            String timeframe) {
        return new OpportunityLogPublicDTO(
                opportunityId,
                "ana-" + opportunityId,
                symbol,
                timeframe,
                direction,
                lifecycle,
                status,
                timestamp,
                OpportunityLogStatus.RESOLVED.equalsIgnoreCase(lifecycle) ? timestamp : null,
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("95"),
                OpportunityLogStatus.MISSED_VALID.equalsIgnoreCase(status),
                OpportunityLogStatus.MISSED_INVALID.equalsIgnoreCase(status),
                OpportunityLogStatus.MISSED_VALID.equalsIgnoreCase(status) ? timestamp : null,
                OpportunityLogStatus.MISSED_INVALID.equalsIgnoreCase(status) ? timestamp : null,
                OpportunityLogStatus.MISSED_VALID.equalsIgnoreCase(status)
                        ? OpportunityLogStatus.TARGET_FIRST
                        : OpportunityLogStatus.MISSED_INVALID.equalsIgnoreCase(status)
                        ? OpportunityLogStatus.INVALIDATION_FIRST
                        : null,
                null,
                null,
                null,
                null,
                "MARKET_DATA",
                timestamp,
                timestamp,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true);
    }

    private static OpportunityLogPublicDTO withPublicEvidence(
            OpportunityLogPublicDTO source,
            BigDecimal targetPrice,
            String marketDataSource) {
        return new OpportunityLogPublicDTO(
                source.opportunityId(),
                source.analysisId(),
                source.symbol(),
                source.timeframe(),
                source.direction(),
                source.lifecycleStatus(),
                source.opportunityStatus(),
                source.anchorTime(),
                source.resolvedAt(),
                source.entryReference(),
                targetPrice,
                source.invalidationPrice(),
                source.targetHit(),
                source.invalidationHit(),
                source.targetHitAt(),
                source.invalidationHitAt(),
                source.hitOrder(),
                source.mfePrice(),
                source.mfeRatio(),
                source.maePrice(),
                source.maeRatio(),
                marketDataSource,
                source.createdAt(),
                source.updatedAt(),
                source.reviewOnly(),
                source.manualReviewOnly(),
                source.notTradeInstruction(),
                source.notExecutable(),
                source.notAutoTrading(),
                source.notOrderExecution(),
                source.notUserPositionCreation(),
                source.notUserPositionMutation(),
                source.notPushSend(),
                source.notExternalChannel());
    }

    private static PositionMonitorLogDO positionRisk(Long logId, Long positionId) {
        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setLogId(logId);
        row.setPositionId(positionId);
        row.setAnalysisId("ana-position-" + positionId);
        row.setEntryLogicStatus("WEAKENED");
        row.setMonitorConclusion("HIGH_RISK_OBSERVATION");
        row.setReversalStatus("NO_REVERSAL");
        row.setRiskChangeReason("OPPOSING_EVIDENCE_INCREASED");
        row.setMonitorSourceStatus("VERIFIED");
        row.setObservedAt(LocalDateTime.of(2026, 7, 29, 11, 0));
        row.setFreshUntil(LocalDateTime.of(2026, 7, 29, 13, 0));
        row.setCreatedAt(LocalDateTime.of(2026, 7, 29, 11, 0));
        return row;
    }

    private static PositionMonitorLogDO validMonitor(Long logId, Long positionId) {
        PositionMonitorLogDO row = positionRisk(logId, positionId);
        row.setExecutionPlanId("plan-position-" + positionId);
        row.setCurrentPrice(new BigDecimal("101.25"));
        row.setMarkPriceSource("TEST");
        row.setRiskLevel("HIGH");
        row.setSuggestedAction("REDUCE_POSITION");
        row.setReason("risk increased");
        row.setRiskSnapshot("{\"riskLevel\":\"HIGH\",\"riskBlocked\":true}");
        return row;
    }

    private static UserPositionDO ownedPosition(Long positionId, String symbol) {
        UserPositionDO row = new UserPositionDO();
        row.setId(positionId);
        row.setUserId(USER_ID);
        row.setAssetSymbol(symbol);
        row.setSide("LONG");
        row.setStatus("OPEN");
        return row;
    }
}
