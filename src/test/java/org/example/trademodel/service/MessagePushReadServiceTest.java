package org.example.trademodel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
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
}
