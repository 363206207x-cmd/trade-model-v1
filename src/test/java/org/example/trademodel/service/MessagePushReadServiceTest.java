package org.example.trademodel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.messagepush.MessageListDTO;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PushDetailDTO;
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
    private final UserPositionMapper userPositionMapper = mock(UserPositionMapper.class);
    private final MessagePushReadService service = new MessagePushReadService(
            opportunityLogMapper,
            positionMonitorLogMapper,
            pushSnapshotMapper,
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
        Select pushSelect = PushSnapshotMapper.class
                .getMethod("selectPublicProjectionByPushId", Long.class)
                .getAnnotation(Select.class);
        String pushSql = String.join(" ", pushSelect.value()).toLowerCase(Locale.ROOT);

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
        assertThat(pushSql)
                .contains("push_id", "analysis_id", "push_create_time")
                .doesNotContain(
                        "push_status",
                        "account_risk_snapshot_id",
                        "entry_zone_json",
                        "stop_zone_json",
                        "invalidation_condition_json");
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
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.messageId()).isEqualTo("opp-orphan-push");
        assertThat(projection.opportunityIdentity().opportunityId()).isEqualTo("opp-orphan-push");
        assertThat(projection.opportunityIdentity().analysisId()).isEqualTo("ana-opp-orphan-push");
        assertThat(projection.publicStatus()).isEqualTo("PENDING_EVALUATION");
        assertThat(projection.missingFields()).containsExactly("publicPushProjection");
    }

    @Test
    void mismatchedPersistedPushIdentityReturnsPartialWithoutComposingDetail() {
        OpportunityLogDO opportunity = publicOpportunity("opp-mismatched-push", 42L);
        opportunity.setAnalysisId("ana-authoritative");
        TmPushSnapshotDO mismatched = new TmPushSnapshotDO();
        mismatched.setPushId(43L);
        mismatched.setAnalysisId("ana-other");
        when(opportunityLogMapper.selectPushBackedPublicByOpportunityId("opp-mismatched-push"))
                .thenReturn(opportunity);
        when(pushSnapshotMapper.selectPublicProjectionByPushId(42L)).thenReturn(mismatched);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-mismatched-push");

        assertThat(detail).isInstanceOf(PushDetailDTO.OpportunityPublicProjection.class);
        PushDetailDTO.OpportunityPublicProjection projection =
                (PushDetailDTO.OpportunityPublicProjection) detail;
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
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
    void completePublicOpportunityReturnsReadyWithoutPrivateRecheckData() {
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
    void opportunityPublicProjectionNeverSerializesRiskDerivedStatusOrPrivateFields() throws Exception {
        OpportunityLogDO opportunity = publicOpportunity("opp-public-projection", 210L);
        opportunity.setOpportunityStatus("BLOCKED_BY_RISK_VALID");
        TmPushSnapshotDO push = publicPush(
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

    private static TmPushSnapshotDO publicPush(
            Long pushId,
            String analysisId,
            LocalDateTime pushTime) {
        TmPushSnapshotDO push = new TmPushSnapshotDO();
        push.setPushId(pushId);
        push.setAnalysisId(analysisId);
        push.setPushCreateTime(pushTime);
        return push;
    }
}
