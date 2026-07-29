package org.example.trademodel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.messagepush.MessageListDTO;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PushDetailDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
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
    void messageAndPushIdsRemainJsonStringsBeyondJavascriptSafeInteger() throws Exception {
        String largeId = "9007199254740993";
        MessageListDTO.MessageItem item = new MessageListDTO.MessageItem(
                largeId,
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
        assertThat(json.path("pushId").isTextual()).isTrue();
        assertThat(json.path("pushId").textValue()).isEqualTo(largeId);
    }

    @Test
    void emptyAndErrorAreDistinctAndErrorNeverCarriesAnEmptySuccessList() {
        when(opportunityLogMapper.listPushBackedShared(anyInt())).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of());

        MessageListDTO empty = service.listForUser(USER_ID, null);

        assertThat(empty.state()).isEqualTo(MessageReadState.EMPTY);
        assertThat(empty.items()).isEmpty();

        when(opportunityLogMapper.listPushBackedShared(anyInt()))
                .thenThrow(new IllegalStateException("database unavailable"));

        MessageListDTO error = service.listForUser(USER_ID, null);

        assertThat(error.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(error.items()).isNull();
        assertThat(error.reason()).isEqualTo("MESSAGE_READ_FAILED");
    }

    @Test
    void orphanPushSnapshotReturnsPartialInsteadOfInventingDetail() {
        OpportunityLogDO opportunity = new OpportunityLogDO();
        opportunity.setOpportunityId("opp-orphan-push");
        opportunity.setAnalysisId("ana-orphan-push");
        opportunity.setPushId(42L);
        when(opportunityLogMapper.selectPushBackedSharedByOpportunityId("opp-orphan-push"))
                .thenReturn(opportunity);
        when(pushSnapshotMapper.selectByPushId(42L)).thenReturn(null);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-orphan-push");

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.messageId()).isEqualTo("opp-orphan-push");
        assertThat(detail.pushId()).isEqualTo("42");
        assertThat(detail.originalSnapshot()).isNull();
        assertThat(detail.currentRecheck()).isNull();
        assertThat(detail.missingFields()).containsExactly("originalSnapshot", "currentRecheck");
    }

    @Test
    void mismatchedPersistedPushIdentityReturnsPartialWithoutComposingDetail() {
        OpportunityLogDO opportunity = new OpportunityLogDO();
        opportunity.setOpportunityId("opp-mismatched-push");
        opportunity.setAnalysisId("ana-authoritative");
        opportunity.setPushId(42L);
        TmPushSnapshotDO mismatched = new TmPushSnapshotDO();
        mismatched.setPushId(43L);
        mismatched.setAnalysisId("ana-other");
        when(opportunityLogMapper.selectPushBackedSharedByOpportunityId("opp-mismatched-push"))
                .thenReturn(opportunity);
        when(pushSnapshotMapper.selectByPushId(42L)).thenReturn(mismatched);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-mismatched-push");

        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.originalSnapshot()).isNull();
        assertThat(detail.currentRecheck()).isNull();
        assertThat(detail.reason()).isEqualTo("PUSH_SNAPSHOT_MISSING");
    }

    @Test
    void malformedIdentityIsMissingWithoutSymbolOrTimeFallback() {
        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "BTCUSDT");

        assertThat(detail.state()).isEqualTo(MessageReadState.MISSING);
        assertThat(detail.reason()).isEqualTo("MESSAGE_NOT_FOUND");
    }

    @Test
    void completeRecheckReturnsReady() {
        LocalDateTime recheckTime = LocalDateTime.of(2026, 7, 29, 10, 5);
        TmPushRecheckLogDO recheck = completeRecheck(101L, "REVIEW_WAITING", recheckTime);
        stubOpportunityDetail("opp-complete-recheck", 201L, recheck);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-complete-recheck");

        assertThat(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(detail.originalSnapshot().status()).isEqualTo("CAPTURED");
        assertThat(detail.currentRecheck()).isNotNull();
        assertThat(detail.currentRecheck().status()).isEqualTo("REVIEW_WAITING");
        assertThat(detail.currentRecheck().checkedAt()).isEqualTo(recheckTime);
        assertThat(detail.reason()).isNull();
    }

    @Test
    void nullPushStatusReturnsPartialWithoutComposingSnapshot() {
        TmPushRecheckLogDO recheck = completeRecheck(
                108L, "REVIEW_WAITING", LocalDateTime.of(2026, 7, 29, 10, 12));
        stubOpportunityDetail("opp-null-push-status", 208L, recheck, null);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-null-push-status");

        assertInvalidPushStatusPartial(detail);
    }

    @Test
    void unknownPushStatusReturnsPartialWithoutLeakingUnknownState() {
        TmPushRecheckLogDO recheck = completeRecheck(
                109L, "REVIEW_WAITING", LocalDateTime.of(2026, 7, 29, 10, 13));
        stubOpportunityDetail("opp-unknown-push-status", 209L, recheck, "UNKNOWN_PUSH_STATE");

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-unknown-push-status");

        assertInvalidPushStatusPartial(detail);
        assertThat(detail.toString()).doesNotContain("UNKNOWN_PUSH_STATE");
    }

    @Test
    void positionRiskListUsesAuthoritativeOwnedPositionSymbol() {
        PositionMonitorLogDO risk = positionRisk(301L, 401L);
        UserPositionDO position = ownedPosition(401L, "BTCUSDT");
        when(opportunityLogMapper.listPushBackedShared(anyInt())).thenReturn(List.of());
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
        when(opportunityLogMapper.listPushBackedShared(anyInt())).thenReturn(List.of());
        when(positionMonitorLogMapper.listRiskByUserId(USER_ID, MessagePushReadService.DEFAULT_LIMIT))
                .thenReturn(List.of(risk));
        when(userPositionMapper.selectByIdAndUserId(402L, USER_ID)).thenReturn(null);

        MessageListDTO result = service.listForUser(USER_ID, null);

        assertThat(result.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(result.items()).isEmpty();
        assertThat(result.reason()).isEqualTo("SOURCE_RECORD_INCOMPLETE");
    }

    @Test
    void missingEachRequiredRecheckFieldReturnsPartial() {
        TmPushRecheckLogDO missingStatus = completeRecheck(
                102L, null, LocalDateTime.of(2026, 7, 29, 10, 6));
        stubOpportunityDetail("opp-recheck-no-status", 202L, missingStatus);

        TmPushRecheckLogDO missingTime = completeRecheck(103L, "REVIEW_WAITING", null);
        missingTime.setCreateTime(LocalDateTime.of(2026, 7, 29, 10, 7));
        stubOpportunityDetail("opp-recheck-no-time", 203L, missingTime);

        TmPushRecheckLogDO missingExecution = completeRecheck(
                104L, "REVIEW_WAITING", LocalDateTime.of(2026, 7, 29, 10, 8));
        missingExecution.setExecutionStatus(null);
        stubOpportunityDetail("opp-recheck-no-execution", 204L, missingExecution);

        assertIncompleteRecheck(
                service.findPushDetailForUser(USER_ID, "opp-recheck-no-status"),
                "currentRecheck.status");
        assertIncompleteRecheck(
                service.findPushDetailForUser(USER_ID, "opp-recheck-no-time"),
                "currentRecheck.checkedAt");
        assertIncompleteRecheck(
                service.findPushDetailForUser(USER_ID, "opp-recheck-no-execution"),
                "currentRecheck.executionStatus");
    }

    @Test
    void invalidRecheckStatusReturnsErrorWithoutLeakingUnknownState() {
        TmPushRecheckLogDO recheck = completeRecheck(
                105L, "UNKNOWN_RECHECK_STATE", LocalDateTime.of(2026, 7, 29, 10, 9));
        stubOpportunityDetail("opp-invalid-recheck", 205L, recheck);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-invalid-recheck");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("CURRENT_RECHECK_INVALID");
        assertThat(detail.currentRecheck()).isNull();
        assertThat(detail.toString()).doesNotContain("UNKNOWN_RECHECK_STATE");
    }

    @Test
    void invalidExecutionStatusReturnsError() {
        TmPushRecheckLogDO recheck = completeRecheck(
                106L, "REVIEW_WAITING", LocalDateTime.of(2026, 7, 29, 10, 10));
        recheck.setExecutionStatus("FAILED");
        stubOpportunityDetail("opp-invalid-execution", 206L, recheck);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-invalid-execution");

        assertThat(detail.state()).isEqualTo(MessageReadState.ERROR);
        assertThat(detail.reason()).isEqualTo("CURRENT_RECHECK_INVALID");
        assertThat(detail.currentRecheck()).isNull();
    }

    @Test
    void existingLegacyValidRecheckRemainsReadyAndCanonical() {
        TmPushRecheckLogDO recheck = completeRecheck(
                107L, "VALID_EXECUTABLE", LocalDateTime.of(2026, 7, 29, 10, 11));
        recheck.setExecutionStatus(" completed ");
        stubOpportunityDetail("opp-legacy-recheck", 207L, recheck);

        PushDetailDTO detail = service.findPushDetailForUser(USER_ID, "opp-legacy-recheck");

        assertThat(detail.state()).isEqualTo(MessageReadState.READY);
        assertThat(detail.currentRecheck().status()).isEqualTo("REVIEW_PASSED");
    }

    private void stubOpportunityDetail(String opportunityId,
                                       Long pushId,
                                       TmPushRecheckLogDO recheck) {
        stubOpportunityDetail(opportunityId, pushId, recheck, "CAPTURED");
    }

    private void stubOpportunityDetail(String opportunityId,
                                       Long pushId,
                                       TmPushRecheckLogDO recheck,
                                       String pushStatus) {
        String analysisId = "ana-" + opportunityId;
        OpportunityLogDO opportunity = new OpportunityLogDO();
        opportunity.setOpportunityId(opportunityId);
        opportunity.setAnalysisId(analysisId);
        opportunity.setPushId(pushId);
        opportunity.setDirection("LONG");

        TmPushSnapshotDO push = new TmPushSnapshotDO();
        push.setPushId(pushId);
        push.setAnalysisId(analysisId);
        push.setSymbol("BTCUSDT");
        push.setPushStatus(pushStatus);

        when(opportunityLogMapper.selectPushBackedSharedByOpportunityId(opportunityId))
                .thenReturn(opportunity);
        when(pushSnapshotMapper.selectByPushId(pushId)).thenReturn(push);
        when(pushRecheckLogMapper.selectLatestByPushId(pushId)).thenReturn(recheck);
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

    private static TmPushRecheckLogDO completeRecheck(Long logId,
                                                       String recheckStatus,
                                                       LocalDateTime recheckTime) {
        TmPushRecheckLogDO recheck = new TmPushRecheckLogDO();
        recheck.setLogId(logId);
        recheck.setExecutionStatus("COMPLETED");
        recheck.setRecheckStatus(recheckStatus);
        recheck.setRecheckTime(recheckTime);
        return recheck;
    }

    private static void assertIncompleteRecheck(PushDetailDTO detail, String missingField) {
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.currentRecheck()).isNull();
        assertThat(detail.missingFields()).containsExactly(missingField);
        assertThat(detail.reason()).isEqualTo("CURRENT_RECHECK_INCOMPLETE");
    }

    private static void assertInvalidPushStatusPartial(PushDetailDTO detail) {
        assertThat(detail.state()).isEqualTo(MessageReadState.PARTIAL);
        assertThat(detail.originalSnapshot()).isNull();
        assertThat(detail.currentRecheck()).isNull();
        assertThat(detail.missingFields()).containsExactly("originalSnapshot.status");
        assertThat(detail.reason()).isEqualTo("PUSH_SNAPSHOT_INCOMPLETE");
    }
}
