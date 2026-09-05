package org.example.trademodel.positionmonitorlog;

import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.service.impl.PositionMonitorLogServiceImpl;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.beans.Introspector;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class PositionMonitorLogServiceImplTest {
    private static final Long USER_ID = 17L;

    @Mock
    private PositionMonitorLogMapper positionMonitorLogMapper;
    @Mock
    private UserPositionMapper userPositionMapper;

    private PositionMonitorLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PositionMonitorLogServiceImpl(positionMonitorLogMapper, userPositionMapper);
    }

    @Test
    void recordMonitorRunWritesExactlyOneNormalLogWithSafetyFields() throws Exception {
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "OPEN"));
        when(positionMonitorLogMapper.insert(any())).thenAnswer(invocation -> {
            PositionMonitorLogDO row = invocation.getArgument(0);
            row.setLogId(101L);
            return 1;
        });

        PositionMonitorLogDTO dto = service.recordMonitorRunForUser(
                USER_ID, command("LOGIC_VALID", "LOW", "CONTINUE_HOLD"));

        ArgumentCaptor<PositionMonitorLogDO> captor = ArgumentCaptor.forClass(PositionMonitorLogDO.class);
        verify(positionMonitorLogMapper).insert(captor.capture());
        PositionMonitorLogDO row = captor.getValue();
        assertThat(row.getPositionId()).isEqualTo(7L);
        assertThat(row.getAnalysisId()).isEqualTo("ana-p0-4");
        assertThat(row.getExecutionPlanId()).isEqualTo("plan-p0-4");
        assertThat(row.getCurrentPrice()).isEqualByComparingTo("111.25000000");
        assertThat(row.getLogicStatus()).isNull();
        assertThat(row.getEntryLogicStatus()).isEqualTo("STILL_VALID");
        assertThat(row.getMonitorConclusion()).isEqualTo("LOGIC_VALID");
        assertThat(row.getReversalStatus()).isEqualTo("NO_REVERSAL");
        assertThat(row.getRiskChangeReason()).isEqualTo("NO_CLEAR_RISK_FACTOR");
        assertThat(row.getRiskLevel()).isEqualTo("LOW");
        assertThat(row.getSuggestedAction()).isEqualTo("CONTINUE_HOLD");
        assertThat(row.getMonitorSourceStatus()).isEqualTo("VERIFIED");
        assertThat(row.getObservedAt()).isNotNull();
        assertThat(row.getFreshUntil()).isAfter(row.getObservedAt());
        assertThat(row.getEvidenceSnapshot()).contains("evidence");
        assertThat(row.getScoreSnapshot()).contains("score");
        assertThat(row.getDecisionSnapshot()).contains("decision");
        assertThat(row.getRiskSnapshot()).contains("risk");
        assertThat(row.getCreatedAt()).isNotNull();

        assertThat(dto.getLogId()).isEqualTo(101L);
        assertSafetyFields(dto);
        assertForbiddenActionFieldsAbsent();
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(positionMonitorLogMapper).deleteOlderThan(cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(LocalDateTime.now().minusDays(29));
    }

    @Test
    void repeatedMonitorRunKeyReturnsCanonicalLogWithoutDuplicateInsert() {
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "OPEN"));
        PositionMonitorLogDO canonical = logRow(301L, 7L, "ana-p0-4", "LOGIC_VALID", "CONTINUE_HOLD",
                LocalDateTime.now().minusMinutes(1));
        canonical.setMonitorRunKey("position-monitor:7:2026-09-04T08:00");
        when(positionMonitorLogMapper.insertIfAbsent(any())).thenReturn(0);
        when(positionMonitorLogMapper.selectByMonitorRunKey("position-monitor:7:2026-09-04T08:00"))
                .thenReturn(canonical);
        when(positionMonitorLogMapper.refreshByMonitorRunKey(any())).thenReturn(1);
        RecordPositionMonitorLogCommand command = command("LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        command.setMonitorRunKey("position-monitor:7:2026-09-04T08:00");
        command.setCurrentPrice(new BigDecimal("112.50"));
        command.setObservedAt(canonical.getObservedAt().plusSeconds(30));
        command.setFreshUntil(canonical.getFreshUntil().plusSeconds(30));

        PositionMonitorLogDTO result = service.recordMonitorRunForUser(USER_ID, command);

        assertThat(result.getLogId()).isEqualTo(301L);
        assertThat(result.getCurrentPrice()).isEqualByComparingTo("112.50");
        verify(positionMonitorLogMapper).refreshByMonitorRunKey(any());
        verify(positionMonitorLogMapper, never()).insert(any());
    }

    @Test
    void recordMonitorRunAllowsWeakenedInvalidatedAndHighRiskScenarios() {
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID))
                .thenReturn(position(7L, "PARTIALLY_CLOSED"));
        when(positionMonitorLogMapper.insert(any())).thenAnswer(invocation -> {
            PositionMonitorLogDO row = invocation.getArgument(0);
            row.setLogId(202L);
            return 1;
        });

        PositionMonitorLogDTO weakened = service.recordMonitorRunForUser(
                USER_ID, command("LOGIC_WEAKENED", "MEDIUM", "NO_ADD_POSITION"));
        PositionMonitorLogDTO invalidated = service.recordMonitorRunForUser(
                USER_ID, command("PLAN_INVALIDATED", "HIGH", "WAIT_CONFIRMATION"));
        PositionMonitorLogDTO highRisk = service.recordMonitorRunForUser(
                USER_ID, command("HIGH_RISK_OBSERVATION", "EXTREME", "REDUCE_POSITION"));

        assertThat(weakened.getMonitorConclusion()).isEqualTo("LOGIC_WEAKENED");
        assertThat(invalidated.getMonitorConclusion()).isEqualTo("PLAN_INVALIDATED");
        assertThat(highRisk.getMonitorConclusion()).isEqualTo("HIGH_RISK_OBSERVATION");
        assertThat(highRisk.getSuggestedAction()).isEqualTo("REDUCE_POSITION");
        assertSafetyFields(weakened);
        assertSafetyFields(invalidated);
        assertSafetyFields(highRisk);
    }

    @Test
    void unverifiedMonitorLogDtoHidesInternalSentinel() {
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "OPEN"));
        when(positionMonitorLogMapper.insert(any())).thenAnswer(invocation -> {
            PositionMonitorLogDO row = invocation.getArgument(0);
            row.setLogId(203L);
            return 1;
        });
        RecordPositionMonitorLogCommand command = command("LOGIC_WEAKENED", "LOW", "NO_ADD_POSITION");
        command.setAnalysisId(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
        command.setExecutionPlanId(null);
        command.setMonitorSourceStatus("PENDING_VERIFICATION");
        clearMonitorResult(command);

        PositionMonitorLogDTO result = service.recordMonitorRunForUser(USER_ID, command);

        ArgumentCaptor<PositionMonitorLogDO> captor = ArgumentCaptor.forClass(PositionMonitorLogDO.class);
        verify(positionMonitorLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getAnalysisId())
                .isEqualTo(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
        assertThat(captor.getValue().getExecutionPlanId()).isNull();
        assertThat(captor.getValue().getMonitorConclusion()).isNull();
        assertThat(captor.getValue().getReversalStatus()).isNull();
        assertThat(captor.getValue().getRiskLevel()).isNull();
        assertThat(captor.getValue().getSuggestedAction()).isNull();
        assertThat(result.getAnalysisId()).isNull();
        assertThat(result.getExecutionPlanId()).isNull();
        assertThat(result.isSourceVerified()).isFalse();
        assertThat(result.getSourceStatus()).isEqualTo("UNVERIFIED");
        assertThat(result.getSourceStatusLabel()).isEqualTo("来源不可验证");
        assertThat(result.toString()).doesNotContain(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
    }

    @Test
    void untrustedSourceRejectsInventedSemanticResults() {
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "OPEN"));
        RecordPositionMonitorLogCommand command = command("LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        command.setMonitorSourceStatus("INVALID");

        assertThatThrownBy(() -> service.recordMonitorRunForUser(USER_ID, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entry_logic_status must be absent");
        verify(positionMonitorLogMapper, never()).insert(any());
    }

    @Test
    void staleMonitorResultCannotBeRecordedAsVerified() {
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "OPEN"));
        RecordPositionMonitorLogCommand command = command("LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        command.setObservedAt(LocalDateTime.now().minusMinutes(10));
        command.setFreshUntil(LocalDateTime.now().minusMinutes(5));

        assertThatThrownBy(() -> service.recordMonitorRunForUser(USER_ID, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("verified monitor result must be fresh when recorded");
        verify(positionMonitorLogMapper, never()).insert(any());
    }

    @Test
    void invalidSourcePersistsOnlyRawObservationAndExplicitMissingSemantics() {
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "OPEN"));
        when(positionMonitorLogMapper.insert(any())).thenAnswer(invocation -> {
            PositionMonitorLogDO row = invocation.getArgument(0);
            row.setLogId(205L);
            return 1;
        });
        RecordPositionMonitorLogCommand command = command("LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        command.setMonitorSourceStatus("INVALID");
        command.setMarkPriceSource(null);
        clearMonitorResult(command);

        PositionMonitorLogDTO result = service.recordMonitorRunForUser(USER_ID, command);

        ArgumentCaptor<PositionMonitorLogDO> captor = ArgumentCaptor.forClass(PositionMonitorLogDO.class);
        verify(positionMonitorLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getCurrentPrice()).isEqualByComparingTo("111.25");
        assertThat(captor.getValue().getMonitorSourceStatus()).isEqualTo("INVALID");
        assertThat(captor.getValue().getMarkPriceSource()).isNull();
        assertThat(captor.getValue().getEntryLogicStatus()).isNull();
        assertThat(captor.getValue().getMonitorConclusion()).isNull();
        assertThat(captor.getValue().getRiskLevel()).isNull();
        assertThat(captor.getValue().getSuggestedAction()).isNull();
        assertThat(result.isTrustedAndFreshAt(LocalDateTime.now())).isFalse();
    }

    @Test
    void legacyNonSentinelMonitorRowIsNotAutomaticallyVerified() {
        PositionMonitorLogDO row = logRow(204L, 7L, "analysis-verified", "LOGIC_VALID", "CONTINUE_HOLD",
                LocalDateTime.of(2026, 6, 22, 9, 0));
        row.setExecutionPlanId("plan-verified");
        when(positionMonitorLogMapper.selectById(204L)).thenReturn(row);

        PositionMonitorLogDTO result = service.findByIdForSystem(204L);

        assertThat(result.getAnalysisId()).isEqualTo("analysis-verified");
        assertThat(result.getExecutionPlanId()).isEqualTo("plan-verified");
        assertThat(result.isSourceVerified()).isFalse();
        assertThat(result.getSourceStatus()).isEqualTo("PENDING_VERIFICATION");
        assertThat(result.getSourceStatusLabel()).isEqualTo("来源待验证");
    }

    @Test
    void closedOrMissingUserPositionRejectsNewMonitorRunLog() {
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "CLOSED"));
        assertThatThrownBy(() -> service.recordMonitorRunForUser(
                USER_ID, command("LOGIC_VALID", "LOW", "CONTINUE_HOLD")))
                .isInstanceOf(UserPositionConflictException.class)
                .hasMessageContaining("CLOSED UserPosition");

        when(userPositionMapper.selectByIdAndUserId(8L, USER_ID)).thenReturn(null);
        RecordPositionMonitorLogCommand missing = command("LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        missing.setPositionId(8L);
        assertThatThrownBy(() -> service.recordMonitorRunForUser(USER_ID, missing))
                .isInstanceOf(UserPositionNotFoundException.class)
                .hasMessageContaining("UserPosition not found");

        verify(positionMonitorLogMapper, never()).insert(any());
    }

    @Test
    void invalidPriceLogicStatusSuggestedActionAndExecutableWordsFailClosed() {
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "OPEN"));

        RecordPositionMonitorLogCommand badPrice = command("LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        badPrice.setCurrentPrice(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.recordMonitorRunForUser(USER_ID, badPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current_price");

        assertThatThrownBy(() -> service.recordMonitorRunForUser(USER_ID, command("BOGUS", "LOW", "CONTINUE_HOLD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monitor_conclusion");

        assertThatThrownBy(() -> service.recordMonitorRunForUser(
                USER_ID, command("LOGIC_VALID", "LOW", "AUTO_CLOSE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("suggested_action");

        assertThatThrownBy(() -> service.recordMonitorRunForUser(
                USER_ID, command("LOGIC_VALID", "LOW", "REDUCE_POSITION")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid for monitor_conclusion");

        RecordPositionMonitorLogCommand forbiddenReason = command("LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        forbiddenReason.setReason("please auto close this position");
        assertThatThrownBy(() -> service.recordMonitorRunForUser(USER_ID, forbiddenReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden executable monitor log content");

        verify(positionMonitorLogMapper, never()).insert(any());
    }

    @Test
    void listQueriesUseFailClosedLimitAndReturnSafetyDtos() {
        PositionMonitorLogDO row = logRow(11L, 7L, "ana-p0-4", "HIGH_RISK_OBSERVATION", "REDUCE_POSITION",
                LocalDateTime.of(2026, 6, 22, 9, 0));
        when(positionMonitorLogMapper.listByPositionId(7L, 20)).thenReturn(List.of(row));
        when(positionMonitorLogMapper.listByAnalysisId("ana-p0-4", 2)).thenReturn(List.of(row));
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "OPEN"));

        List<PositionMonitorLogDTO> byPosition = service.listByPositionIdForUser(USER_ID, 7L, null);
        List<PositionMonitorLogDTO> byAnalysis = service.listByAnalysisIdForSystem(" ana-p0-4 ", 2);

        assertThat(byPosition).hasSize(1);
        assertThat(byAnalysis).hasSize(1);
        assertSafetyFields(byPosition.get(0));
        assertThatThrownBy(() -> service.listByPositionIdForUser(USER_ID, 7L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
        assertThatThrownBy(() -> service.listByPositionIdForUser(USER_ID, 7L, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void reviewTimelineQueryReturnsAllLogsInMapperOrderWithSafetyDtos() {
        PositionMonitorLogDO first = logRow(1L, 7L, "ana-p0-6", "LOGIC_WEAKENED", "NO_ADD_POSITION",
                LocalDateTime.of(2026, 6, 22, 8, 30));
        PositionMonitorLogDO second = logRow(2L, 7L, "ana-p0-6", "PLAN_INVALIDATED", "WAIT_CONFIRMATION",
                LocalDateTime.of(2026, 6, 22, 9, 30));
        when(positionMonitorLogMapper.listAllByPositionIdForReview(7L)).thenReturn(List.of(first, second));
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(position(7L, "CLOSED"));

        List<PositionMonitorLogDTO> logs = service.listAllByPositionIdForUserReview(USER_ID, 7L);

        assertThat(logs).extracting(PositionMonitorLogDTO::getLogId).containsExactly(1L, 2L);
        assertSafetyFields(logs.get(0));
        verify(positionMonitorLogMapper).listAllByPositionIdForReview(7L);
    }

    private static RecordPositionMonitorLogCommand command(String logicStatus, String riskLevel, String suggestedAction) {
        RecordPositionMonitorLogCommand command = new RecordPositionMonitorLogCommand();
        command.setPositionId(7L);
        command.setAnalysisId(" ana-p0-4 ");
        command.setExecutionPlanId(" plan-p0-4 ");
        command.setCurrentPrice(new BigDecimal("111.25"));
        command.setMarkPriceSource("TEST");
        command.setEntryLogicStatus(switch (logicStatus) {
            case "LOGIC_VALID" -> "STILL_VALID";
            case "PLAN_INVALIDATED" -> "INVALIDATED";
            default -> "WEAKENED";
        });
        command.setMonitorConclusion(logicStatus);
        command.setReversalStatus("NO_REVERSAL");
        command.setRiskChangeReason("NO_CLEAR_RISK_FACTOR");
        command.setRiskLevel(riskLevel);
        command.setRiskTrend("STABLE");
        command.setSuggestedAction(suggestedAction);
        command.setMonitorSourceStatus("VERIFIED");
        command.setObservedAt(LocalDateTime.now().minusSeconds(1));
        command.setFreshUntil(LocalDateTime.now().plusMinutes(1));
        command.setReason("manual review note");
        command.setEvidenceSnapshot("{\"evidence\":\"stable\"}");
        command.setScoreSnapshot("{\"score\":70}");
        command.setDecisionSnapshot("{\"decision\":\"watch\"}");
        command.setRiskSnapshot("{\"risk\":\"guarded\"}");
        command.setTraceId("trace-p0-4");
        return command;
    }

    private static UserPositionDO position(Long id, String status) {
        UserPositionDO row = new UserPositionDO();
        row.setId(id);
        row.setUserId(USER_ID);
        row.setStatus(status);
        return row;
    }

    private static PositionMonitorLogDO logRow(Long logId, Long positionId, String analysisId,
                                               String logicStatus, String suggestedAction,
                                               LocalDateTime createdAt) {
        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setLogId(logId);
        row.setPositionId(positionId);
        row.setAnalysisId(analysisId);
        row.setCurrentPrice(new BigDecimal("111.25"));
        row.setMarkPriceSource("TEST");
        row.setEntryLogicStatus("PLAN_INVALIDATED".equals(logicStatus) ? "INVALIDATED" : "WEAKENED");
        row.setMonitorConclusion(logicStatus);
        row.setReversalStatus("NO_REVERSAL");
        row.setRiskChangeReason("NO_CLEAR_RISK_FACTOR");
        row.setRiskLevel("HIGH");
        row.setRiskTrend("STABLE");
        row.setSuggestedAction(suggestedAction);
        row.setMonitorSourceStatus("PENDING_VERIFICATION");
        row.setObservedAt(createdAt);
        row.setFreshUntil(createdAt.plusMinutes(1));
        row.setCreatedAt(createdAt);
        return row;
    }

    private static void clearMonitorResult(RecordPositionMonitorLogCommand command) {
        command.setEntryLogicStatus(null);
        command.setMonitorConclusion(null);
        command.setReversalStatus(null);
        command.setRiskChangeReason(null);
        command.setRiskLevel(null);
        command.setRiskTrend(null);
        command.setSuggestedAction(null);
        command.setRiskSnapshot(null);
    }

    private static void assertSafetyFields(PositionMonitorLogDTO dto) {
        assertThat(dto.isReviewOnly()).isTrue();
        assertThat(dto.isManualReviewOnly()).isTrue();
        assertThat(dto.isNotTradeInstruction()).isTrue();
        assertThat(dto.isNotExecutable()).isTrue();
        assertThat(dto.isNotAutoClose()).isTrue();
        assertThat(dto.isNotAutoReverse()).isTrue();
        assertThat(dto.isNotOrderExecution()).isTrue();
        assertThat(dto.isNotAutoTrading()).isTrue();
        assertThat(dto.isNotPositionMutation()).isTrue();
    }

    private static void assertForbiddenActionFieldsAbsent() throws Exception {
        Set<String> propertyNames = Arrays.stream(Introspector.getBeanInfo(PositionMonitorLogDTO.class).getPropertyDescriptors())
                .map(descriptor -> descriptor.getName())
                .collect(Collectors.toSet());
        assertThat(propertyNames).doesNotContain(
                "closeAction", "reduceAction", "reverseAction", "orderAction", "executionAction",
                "autoTradingAction", "executablePayload", "providerPayload");
    }
}
