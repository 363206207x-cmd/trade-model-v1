package org.example.trademodel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.messagepush.MessageListDTO;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.OpportunityPushReadinessProjection;
import org.example.trademodel.messagepush.PushDetailDTO;
import org.example.trademodel.messagepush.PushRecheckReadinessProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagePushReadServiceTest {
    private static final Long USER_ID = 11L;
    private final OpportunityLogMapper opportunityLogMapper = mock(OpportunityLogMapper.class);
    private final PositionMonitorLogMapper positionMonitorLogMapper = mock(PositionMonitorLogMapper.class);
    private final PushSnapshotMapper pushSnapshotMapper = mock(PushSnapshotMapper.class);
    private final PushRecheckLogMapper pushRecheckLogMapper = mock(PushRecheckLogMapper.class);
    private final UserPositionMapper userPositionMapper = mock(UserPositionMapper.class);
    private final MessagePushReadService service = new MessagePushReadService(
            opportunityLogMapper,
            positionMonitorLogMapper,
            pushSnapshotMapper,
            pushRecheckLogMapper,
            userPositionMapper);

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
    void publicMapperProjectionsDoNotSelectPrivateRiskColumns() throws Exception {
        String opportunitySql = OpportunityLogMapper.PUBLIC_MESSAGE_SELECT.toLowerCase(Locale.ROOT);
        String opportunityApiSql = OpportunityLogMapper.PUBLIC_API_SELECT.toLowerCase(Locale.ROOT);
        String opportunityPredicate =
                OpportunityLogMapper.PUBLIC_PROJECTION_PREDICATE.toLowerCase(Locale.ROOT);
        Select pushSelect = PushSnapshotMapper.class
                .getMethod("selectPublicProjectionByPushId", Long.class)
                .getAnnotation(Select.class);
        String pushSql = String.join(" ", pushSelect.value()).toLowerCase(Locale.ROOT);
        Select recheckSelect = PushRecheckLogMapper.class
                .getMethod("selectReadinessByPushId", Long.class)
                .getAnnotation(Select.class);
        String recheckSql = String.join(" ", recheckSelect.value()).toLowerCase(Locale.ROOT);

        assertThat(opportunitySql)
                .contains("opportunity_id", "analysis_id", "push_id", "symbol")
                .doesNotContain(
                        "user_position_id",
                        "risk_blocked_evidence",
                        "risk_blocked_at",
                        "reason_codes",
                        "entry_reference",
                        "target_price",
                        "invalidation_price");
        assertThat(opportunityPredicate)
                .contains(
                        "user_position_id is null",
                        "risk_blocked_evidence",
                        "risk_blocked_at is null",
                        "blocked_by_risk_valid");
        assertThat(opportunityApiSql)
                .contains("opportunity_id", "analysis_id", "symbol", "opportunity_status")
                .doesNotContain(
                        "push_id",
                        "user_position_id",
                        "risk_blocked_evidence",
                        "risk_blocked_at",
                        "reason_codes",
                        "source_reference",
                        "trace_id");
        assertThat(pushSql)
                .contains("push_id", "analysis_id", "push_status", "push_create_time")
                .doesNotContain(
                        "account_risk_snapshot_id",
                        "entry_zone_json",
                        "stop_zone_json",
                        "invalidation_condition_json");
        assertThat(PushSnapshotMapper.class
                .getMethod("selectPublicProjectionByPushId", Long.class)
                .getReturnType()).isEqualTo(OpportunityPushReadinessProjection.class);
        assertThat(recheckSql)
                .contains("recheck_status", "recheck_time", "execution_status")
                .doesNotContain(
                        "current_account_risk_allowed",
                        "current_price",
                        "current_data_quality_score",
                        "current_confused_score");
    }

    @Test
    void emptyAndErrorAreDistinctAndErrorNeverCarriesAnEmptySuccessList() {
        when(opportunityLogMapper.listPushBackedPublic(anyInt())).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of());

        MessageListDTO empty = service.listForUser(USER_ID, null);

        assertThat(empty.state()).isEqualTo(MessageReadState.EMPTY);
        assertThat(empty.items()).isEmpty();

        when(opportunityLogMapper.listPushBackedPublic(anyInt()))
                .thenThrow(new IllegalStateException("database unavailable"));

        MessageListDTO error = service.listForUser(USER_ID, null);

        assertThat(error.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(error.items()).isNull();
        assertThat(error.reason()).isEqualTo("MESSAGE_READ_FAILED");
    }

    @Test
    void orphanPushSnapshotReturnsPartialInsteadOfInventingDetail() {
        OpportunityLogDO opportunity = publicOpportunity("opp-orphan-push", 42L);
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId("opp-orphan-push"))
                .thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(42L)).thenReturn(null);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-orphan-push");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.MISSING);
        assertThat(detail.messageId()).isEqualTo("opp-orphan-push");
        assertThat(projection.opportunityIdentity().opportunityId()).isEqualTo("opp-orphan-push");
        assertThat(projection.opportunityIdentity().analysisId()).isEqualTo("ana-opp-orphan-push");
        assertThat(projection.publicStatus()).isEqualTo("PENDING_EVALUATION");
        assertThat(projection.missingFields()).containsExactly("publicPushProjection");
    }

    @Test
    void mismatchedPersistedPushIdentityReturnsMissingWithoutComposingDetail() {
        OpportunityLogDO opportunity = publicOpportunity("opp-mismatched-push", 42L);
        opportunity.setAnalysisId("ana-authoritative");
        OpportunityPushReadinessProjection mismatched =
                new OpportunityPushReadinessProjection();
        mismatched.setPushId(43L);
        mismatched.setAnalysisId("ana-other");
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId("opp-mismatched-push"))
                .thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(42L)).thenReturn(mismatched);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-mismatched-push");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.MISSING);
        assertThat(projection.publicDescription()).isEqualTo("BTCUSDT LONG 1H");
        assertThat(detail.reason()).isEqualTo("PUSH_SNAPSHOT_MISSING");
    }

    @Test
    void malformedIdentityIsMissingWithoutSymbolOrTimeFallback() {
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "BTCUSDT");

        assertThat(detail.state()).isEqualTo(MessageReadState.MISSING);
        assertThat(detail.reason()).isEqualTo("MESSAGE_NOT_FOUND");
    }

    @Test
    void completePublicOpportunityReturnsReadyWithoutSerializingPrivateRecheckData() {
        LocalDateTime pushTime = LocalDateTime.of(2026, 7, 29, 10, 5);
        stubOpportunityDetail("opp-public-ready", 201L, pushTime);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-public-ready");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(projection.publicStatus()).isEqualTo("PENDING_EVALUATION");
        assertThat(projection.publicTimestamp()).isEqualTo(pushTime);
        assertThat(projection.publicDescription()).isEqualTo("BTCUSDT LONG 1H");
        assertThat(detail.reason()).isNull();
        verify(opportunityLogMapper, never())
                .selectPushBackedSharedByOpportunityId("opp-public-ready");
        verify(pushSnapshotMapper, never()).selectByPushId(201L);
    }

    @Test
    void missingPushStatusIsPartialAndCannotBecomeReady() {
        OpportunityLogDO opportunity = publicOpportunity("opp-push-status-missing", 202L);
        OpportunityPushReadinessProjection push = publicPush(
                202L, opportunity.getAnalysisId(), opportunity.getAnchorTime());
        push.setPushStatus(null);
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(
                opportunity.getOpportunityId())).thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(202L)).thenReturn(push);

        PushDetailDTO detail = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.reason()).isEqualTo("PUSH_STATUS_INCOMPLETE");
        verify(pushRecheckLogMapper, never()).selectReadinessByPushId(202L);
    }

    @Test
    void unknownPushStatusIsErrorAndCannotPassThrough() {
        OpportunityLogDO opportunity = publicOpportunity("opp-push-status-invalid", 203L);
        OpportunityPushReadinessProjection push = publicPush(
                203L, opportunity.getAnalysisId(), opportunity.getAnchorTime());
        push.setPushStatus("NOT_A_REAL_PUSH_STATUS");
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(
                opportunity.getOpportunityId())).thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(203L)).thenReturn(push);

        PushDetailDTO detail = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("PUSH_STATUS_INVALID");
    }

    @Test
    void missingRecheckIsPartialInsteadOfReady() {
        OpportunityLogDO opportunity = publicOpportunity("opp-recheck-missing", 204L);
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(
                opportunity.getOpportunityId())).thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(204L)).thenReturn(
                publicPush(204L, opportunity.getAnalysisId(), opportunity.getAnchorTime()));
        when(pushRecheckLogMapper.selectReadinessByPushId(204L)).thenReturn(null);

        PushDetailDTO detail = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.reason()).isEqualTo("CURRENT_RECHECK_MISSING");
    }

    @Test
    void incompleteRecheckIsPartialInsteadOfReady() {
        OpportunityLogDO opportunity = publicOpportunity("opp-recheck-incomplete", 205L);
        PushRecheckReadinessProjection recheck = readiness("REVIEW_PASSED", "COMPLETED");
        recheck.setRecheckTime(null);
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(
                opportunity.getOpportunityId())).thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(205L)).thenReturn(
                publicPush(205L, opportunity.getAnalysisId(), opportunity.getAnchorTime()));
        when(pushRecheckLogMapper.selectReadinessByPushId(205L)).thenReturn(recheck);

        PushDetailDTO detail = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.reason()).isEqualTo("CURRENT_RECHECK_INCOMPLETE");
        assertThat(((PushDetailDTO.OpportunityPublicProjection) detail).missingFields())
                .containsExactly("currentRecheck.checkedAt");
    }

    @Test
    void invalidRecheckStatusIsError() {
        OpportunityLogDO opportunity = publicOpportunity("opp-recheck-invalid", 206L);
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(
                opportunity.getOpportunityId())).thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(206L)).thenReturn(
                publicPush(206L, opportunity.getAnalysisId(), opportunity.getAnchorTime()));
        when(pushRecheckLogMapper.selectReadinessByPushId(206L)).thenReturn(
                readiness("NOT_A_REAL_RECHECK_STATUS", "COMPLETED"));

        PushDetailDTO detail = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("CURRENT_RECHECK_INVALID");
    }

    @Test
    void inProgressExecutionIsPartialAndMalformedReasonJsonIsError() {
        OpportunityLogDO opportunity = publicOpportunity("opp-recheck-running", 207L);
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(
                opportunity.getOpportunityId())).thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(207L)).thenReturn(
                publicPush(207L, opportunity.getAnalysisId(), opportunity.getAnchorTime()));
        when(pushRecheckLogMapper.selectReadinessByPushId(207L)).thenReturn(
                readiness("REVIEW_PASSED", "RUNNING"));

        PushDetailDTO running = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(running.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(running.reason()).isEqualTo("CURRENT_RECHECK_INCOMPLETE");

        PushRecheckReadinessProjection malformed = readiness("REVIEW_PASSED", "COMPLETED");
        malformed.setFailReasonJson("PRIVATE_ACCOUNT_RISK_REASON");
        when(pushRecheckLogMapper.selectReadinessByPushId(207L)).thenReturn(malformed);

        PushDetailDTO invalid = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(invalid.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(invalid.reason()).isEqualTo("CURRENT_RECHECK_REASON_INVALID");
    }

    @Test
    void missingExecutionStatusIsPartialAndUnknownExecutionStatusIsError() {
        OpportunityLogDO opportunity = publicOpportunity("opp-execution-status", 214L);
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(
                opportunity.getOpportunityId())).thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(214L)).thenReturn(
                publicPush(214L, opportunity.getAnalysisId(), opportunity.getAnchorTime()));

        PushRecheckReadinessProjection missing = readiness("REVIEW_PASSED", null);
        when(pushRecheckLogMapper.selectReadinessByPushId(214L)).thenReturn(missing);

        PushDetailDTO partial = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(partial.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(partial.reason()).isEqualTo("CURRENT_RECHECK_INCOMPLETE");
        assertThat(((PushDetailDTO.OpportunityPublicProjection) partial).missingFields())
                .containsExactly("currentRecheck.executionStatus");

        PushRecheckReadinessProjection invalid =
                readiness("REVIEW_PASSED", "NOT_A_REAL_EXECUTION_STATUS");
        when(pushRecheckLogMapper.selectReadinessByPushId(214L)).thenReturn(invalid);

        PushDetailDTO error = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(error.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(error.reason()).isEqualTo("CURRENT_RECHECK_EXECUTION_INVALID");
    }

    @Test
    void pushAndRecheckStatusConflictIsError() {
        OpportunityLogDO opportunity = publicOpportunity("opp-state-conflict", 209L);
        OpportunityPushReadinessProjection push = publicPush(
                209L, opportunity.getAnalysisId(), opportunity.getAnchorTime());
        push.setPushStatus("RECHECK_INVALIDATED");
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(
                opportunity.getOpportunityId())).thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(209L)).thenReturn(push);
        when(pushRecheckLogMapper.selectReadinessByPushId(209L)).thenReturn(
                readiness("REVIEW_PASSED", "COMPLETED"));

        PushDetailDTO detail = service.findPushDetailForUser(
                USER_ID, opportunity.getOpportunityId());

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("PUSH_RECHECK_STATE_CONFLICT");
    }

    @Test
    void opportunityPublicProjectionNeverSerializesRiskDerivedStatusOrPrivateFields() throws Exception {
        OpportunityLogDO opportunity = publicOpportunity("opp-public-projection", 210L);
        opportunity.setOpportunityStatus("BLOCKED_BY_RISK_VALID");
        OpportunityPushReadinessProjection push = publicPush(
                210L,
                opportunity.getAnalysisId(),
                LocalDateTime.of(2026, 7, 29, 10, 14));
        push.setPushStatus("RECHECK_RISK_BLOCKED");
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId("opp-public-projection"))
                .thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(210L)).thenReturn(push);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-public-projection");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(detail));

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        assertThat(json.path("publicStatus").asText()).isEqualTo("PENDING_EVALUATION");
        assertThat(json.path("sourceIdentity").has("positionId")).isFalse();
        assertThat(json.toString()).doesNotContain("pushId");
        assertThat(json.has("originalSnapshot")).isFalse();
        assertThat(json.has("currentRecheck")).isFalse();
        assertThat(json.has("changeReason")).isFalse();
        assertThat(json.toString()).doesNotContain(
                "currentAccountRiskAllowed",
                "failReasonJson",
                "PRIVATE_ACCOUNT_RISK_REASON",
                "riskLevel",
                "RISK_BLOCKED",
                "positionId",
                "currentPrice",
                "dataQualityScore",
                "confusedScore");
    }

    @Test
    void missingPublicStatusReturnsPartialWithoutReadingPrivateStatus() {
        OpportunityLogDO opportunity = publicOpportunity("opp-missing-public-status", 208L);
        opportunity.setLifecycleStatus(null);
        opportunity.setOpportunityStatus("BLOCKED_BY_RISK_VALID");
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId("opp-missing-public-status"))
                .thenReturn(opportunity);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-missing-public-status");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(projection.publicStatus()).isNull();
        assertThat(projection.missingFields()).containsExactly("publicStatus");
        assertThat(detail.reason()).isEqualTo("PUBLIC_OPPORTUNITY_INCOMPLETE");
        verify(pushSnapshotMapper, never()).selectPublicProjectionByPushId(208L);
    }

    @Test
    void missingPublicTimestampReturnsPartialBeforePushRead() {
        OpportunityLogDO opportunity = publicOpportunity("opp-missing-public-time", 211L);
        opportunity.setAnchorTime(null);
        opportunity.setCreatedAt(null);
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId("opp-missing-public-time"))
                .thenReturn(opportunity);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-missing-public-time");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(projection.missingFields()).containsExactly("publicTimestamp");
        assertThat(detail.reason()).isEqualTo("PUBLIC_OPPORTUNITY_INCOMPLETE");
        verify(pushSnapshotMapper, never()).selectPublicProjectionByPushId(211L);
    }

    @Test
    void missingPublicDescriptionReturnsPartialBeforePushRead() {
        OpportunityLogDO opportunity = publicOpportunity("opp-missing-public-description", 212L);
        opportunity.setSymbol(null);
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(
                "opp-missing-public-description")).thenReturn(opportunity);

        PushDetailDTO detail = service.findPushDetailForUser(
                USER_ID, "opp-missing-public-description");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(projection.missingFields()).containsExactly("publicDescription");
        assertThat(detail.reason()).isEqualTo("PUBLIC_OPPORTUNITY_INCOMPLETE");
        verify(pushSnapshotMapper, never()).selectPublicProjectionByPushId(212L);
    }

    @Test
    void safeTerminalOpportunityStatusTakesPrecedenceOverLifecycleStatus() {
        OpportunityLogDO opportunity = publicOpportunity("opp-public-terminal", 213L);
        opportunity.setLifecycleStatus("RESOLVED");
        opportunity.setOpportunityStatus("MISSED_VALID");
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId("opp-public-terminal"))
                .thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(213L)).thenReturn(
                publicPush(213L, opportunity.getAnalysisId(),
                        LocalDateTime.of(2026, 7, 29, 10, 20)));
        when(pushRecheckLogMapper.selectReadinessByPushId(213L)).thenReturn(
                readiness("REVIEW_PASSED", "COMPLETED"));

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-public-terminal");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(projection.publicStatus()).isEqualTo("MISSED_VALID");
    }

    @Test
    void positionRiskListUsesAuthoritativeOwnedPositionSymbol() {
        PositionMonitorLogDO risk = positionRisk(301L, 401L);
        UserPositionDO position = ownedPosition(401L, "BTCUSDT");
        when(opportunityLogMapper.listPushBackedPublic(anyInt())).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of(risk));
        when(userPositionMapper.selectByIdAndUserId(401L, USER_ID)).thenReturn(position);

        MessageListDTO result = service.listForUser(USER_ID, null);

        assertThat(result.state()).isEqualTo(MessageReadState.READY);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.symbol()).isEqualTo("BTCUSDT");
            assertThat(item.sourceIdentity().positionId()).isEqualTo("401");
        });
    }

    @Test
    void missingOwnedPositionMakesRiskMessageListPartialWithoutInventingSymbol() {
        PositionMonitorLogDO risk = positionRisk(302L, 402L);
        when(opportunityLogMapper.listPushBackedPublic(anyInt())).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of(risk));
        when(userPositionMapper.selectByIdAndUserId(402L, USER_ID)).thenReturn(null);

        MessageListDTO result = service.listForUser(USER_ID, null);

        assertThat(result.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(result.items()).isEmpty();
        assertThat(result.reason()).isEqualTo("SOURCE_RECORD_INCOMPLETE");
    }

    private void stubOpportunityDetail(String opportunityId,
                                       Long pushId,
                                       LocalDateTime pushTime) {
        String analysisId = "ana-" + opportunityId;
        OpportunityLogDO opportunity = publicOpportunity(opportunityId, pushId);
        opportunity.setAnalysisId(analysisId);

        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId(opportunityId))
                .thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(pushId))
                .thenReturn(publicPush(pushId, analysisId, pushTime));
        when(pushRecheckLogMapper.selectReadinessByPushId(pushId))
                .thenReturn(readiness("REVIEW_PASSED", "COMPLETED"));
    }

    private static OpportunityLogDO publicOpportunity(String opportunityId, Long pushId) {
        OpportunityLogDO opportunity = new OpportunityLogDO();
        opportunity.setOpportunityId(opportunityId);
        opportunity.setAnalysisId("ana-" + opportunityId);
        opportunity.setPushId(pushId);
        opportunity.setSymbol("BTCUSDT");
        opportunity.setDirection("LONG");
        opportunity.setTimeframe("1h");
        opportunity.setLifecycleStatus("PENDING_EVALUATION");
        opportunity.setAnchorTime(LocalDateTime.of(2026, 7, 29, 10, 0));
        opportunity.setCreatedAt(opportunity.getAnchorTime());
        return opportunity;
    }

    private static PositionMonitorLogDO positionRisk(Long logId, Long positionId) {
        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setLogId(logId);
        row.setPositionId(positionId);
        row.setAnalysisId("ana-position-" + positionId);
        row.setLogicStatus("HIGH_RISK");
        row.setCreatedAt(LocalDateTime.of(2026, 7, 29, 11, 0));
        return row;
    }

    private static UserPositionDO ownedPosition(Long positionId, String symbol) {
        UserPositionDO row = new UserPositionDO();
        row.setId(positionId);
        row.setUserId(USER_ID);
        row.setAssetSymbol(symbol);
        return row;
    }

    private static OpportunityPushReadinessProjection publicPush(
            Long pushId,
            String analysisId,
            LocalDateTime pushTime) {
        OpportunityPushReadinessProjection push =
                new OpportunityPushReadinessProjection();
        push.setPushId(pushId);
        push.setAnalysisId(analysisId);
        push.setPushStatus("RECHECK_REVIEW_PASSED");
        push.setPushCreateTime(pushTime);
        return push;
    }

    private static PushRecheckReadinessProjection readiness(
            String recheckStatus,
            String executionStatus) {
        PushRecheckReadinessProjection projection = new PushRecheckReadinessProjection();
        projection.setLogId(901L);
        projection.setRecheckStatus(recheckStatus);
        projection.setRecheckTime(LocalDateTime.of(2026, 7, 29, 10, 5));
        projection.setExecutionStatus(executionStatus);
        projection.setFailReasonJson("{\"code\":\"REVIEW_ONLY\"}");
        return projection;
    }
}
