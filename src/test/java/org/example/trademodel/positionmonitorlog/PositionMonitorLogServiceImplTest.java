package org.example.trademodel.positionmonitorlog;

import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.service.impl.PositionMonitorLogServiceImpl;
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
        when(userPositionMapper.selectById(7L)).thenReturn(position(7L, "OPEN"));
        when(positionMonitorLogMapper.insert(any())).thenAnswer(invocation -> {
            PositionMonitorLogDO row = invocation.getArgument(0);
            row.setLogId(101L);
            return 1;
        });

        PositionMonitorLogDTO dto = service.recordMonitorRun(command("LOGIC_VALID", "LOW", "HOLD"));

        ArgumentCaptor<PositionMonitorLogDO> captor = ArgumentCaptor.forClass(PositionMonitorLogDO.class);
        verify(positionMonitorLogMapper).insert(captor.capture());
        PositionMonitorLogDO row = captor.getValue();
        assertThat(row.getPositionId()).isEqualTo(7L);
        assertThat(row.getAnalysisId()).isEqualTo("ana-p0-4");
        assertThat(row.getExecutionPlanId()).isEqualTo("plan-p0-4");
        assertThat(row.getCurrentPrice()).isEqualByComparingTo("111.25000000");
        assertThat(row.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(row.getRiskLevel()).isEqualTo("LOW");
        assertThat(row.getSuggestedAction()).isEqualTo("HOLD");
        assertThat(row.getEvidenceSnapshot()).contains("evidence");
        assertThat(row.getScoreSnapshot()).contains("score");
        assertThat(row.getDecisionSnapshot()).contains("decision");
        assertThat(row.getRiskSnapshot()).contains("risk");
        assertThat(row.getCreatedAt()).isNotNull();

        assertThat(dto.getLogId()).isEqualTo(101L);
        assertSafetyFields(dto);
        assertForbiddenActionFieldsAbsent();
    }

    @Test
    void recordMonitorRunAllowsWeakenedInvalidatedAndHighRiskScenarios() {
        when(userPositionMapper.selectById(7L)).thenReturn(position(7L, "PARTIALLY_CLOSED"));
        when(positionMonitorLogMapper.insert(any())).thenAnswer(invocation -> {
            PositionMonitorLogDO row = invocation.getArgument(0);
            row.setLogId(202L);
            return 1;
        });

        PositionMonitorLogDTO weakened = service.recordMonitorRun(command("LOGIC_WEAKENED", "MEDIUM", "MANUAL_REVIEW"));
        PositionMonitorLogDTO invalidated = service.recordMonitorRun(command("PLAN_INVALIDATED", "HIGH", "RECHECK_PLAN"));
        PositionMonitorLogDTO highRisk = service.recordMonitorRun(command("HIGH_RISK", "HIGH", "RISK_REVIEW"));

        assertThat(weakened.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(invalidated.getLogicStatus()).isEqualTo("PLAN_INVALIDATED");
        assertThat(highRisk.getLogicStatus()).isEqualTo("HIGH_RISK");
        assertThat(highRisk.getSuggestedAction()).isEqualTo("RISK_REVIEW");
        assertSafetyFields(weakened);
        assertSafetyFields(invalidated);
        assertSafetyFields(highRisk);
    }

    @Test
    void unverifiedMonitorSourceUsesStructuralStatusNotFakePlanIdentity() {
        when(userPositionMapper.selectById(7L)).thenReturn(position(7L, "OPEN"));
        when(positionMonitorLogMapper.insert(any())).thenAnswer(invocation -> {
            PositionMonitorLogDO row = invocation.getArgument(0);
            row.setLogId(203L);
            return 1;
        });
        RecordPositionMonitorLogCommand command = command("LOGIC_WEAKENED", "LOW", "MANUAL_REVIEW");
        command.setAnalysisId(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
        command.setExecutionPlanId(null);

        PositionMonitorLogDTO result = service.recordMonitorRun(command);

        ArgumentCaptor<PositionMonitorLogDO> captor = ArgumentCaptor.forClass(PositionMonitorLogDO.class);
        verify(positionMonitorLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getAnalysisId())
                .isEqualTo(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
        assertThat(captor.getValue().getExecutionPlanId()).isNull();
        assertThat(result.getAnalysisId()).isEqualTo(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
        assertThat(PositionMonitorSourceContract.isUnverifiedAnalysisId(result.getAnalysisId())).isTrue();
    }

    @Test
    void closedOrMissingUserPositionRejectsNewMonitorRunLog() {
        when(userPositionMapper.selectById(7L)).thenReturn(position(7L, "CLOSED"));
        assertThatThrownBy(() -> service.recordMonitorRun(command("LOGIC_VALID", "LOW", "HOLD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CLOSED UserPosition");

        when(userPositionMapper.selectById(8L)).thenReturn(null);
        RecordPositionMonitorLogCommand missing = command("LOGIC_VALID", "LOW", "HOLD");
        missing.setPositionId(8L);
        assertThatThrownBy(() -> service.recordMonitorRun(missing))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UserPosition not found");

        verify(positionMonitorLogMapper, never()).insert(any());
    }

    @Test
    void invalidPriceLogicStatusSuggestedActionAndExecutableWordsFailClosed() {
        when(userPositionMapper.selectById(7L)).thenReturn(position(7L, "OPEN"));

        RecordPositionMonitorLogCommand badPrice = command("LOGIC_VALID", "LOW", "HOLD");
        badPrice.setCurrentPrice(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.recordMonitorRun(badPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current_price");

        assertThatThrownBy(() -> service.recordMonitorRun(command("BOGUS", "LOW", "HOLD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("logic_status");

        assertThatThrownBy(() -> service.recordMonitorRun(command("LOGIC_VALID", "LOW", "CLOSE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("suggested_action");

        RecordPositionMonitorLogCommand forbiddenReason = command("LOGIC_VALID", "LOW", "HOLD");
        forbiddenReason.setReason("please close this position");
        assertThatThrownBy(() -> service.recordMonitorRun(forbiddenReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden executable monitor log content");

        verify(positionMonitorLogMapper, never()).insert(any());
    }

    @Test
    void listQueriesUseFailClosedLimitAndReturnSafetyDtos() {
        PositionMonitorLogDO row = logRow(11L, 7L, "ana-p0-4", "HIGH_RISK", "RISK_REVIEW",
                LocalDateTime.of(2026, 6, 22, 9, 0));
        when(positionMonitorLogMapper.listByPositionId(7L, 20)).thenReturn(List.of(row));
        when(positionMonitorLogMapper.listByAnalysisId("ana-p0-4", 2)).thenReturn(List.of(row));

        List<PositionMonitorLogDTO> byPosition = service.listByPositionId(7L, null);
        List<PositionMonitorLogDTO> byAnalysis = service.listByAnalysisId(" ana-p0-4 ", 2);

        assertThat(byPosition).hasSize(1);
        assertThat(byAnalysis).hasSize(1);
        assertSafetyFields(byPosition.get(0));
        assertThatThrownBy(() -> service.listByPositionId(7L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
        assertThatThrownBy(() -> service.listByPositionId(7L, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void reviewTimelineQueryReturnsAllLogsInMapperOrderWithSafetyDtos() {
        PositionMonitorLogDO first = logRow(1L, 7L, "ana-p0-6", "LOGIC_WEAKENED", "MANUAL_REVIEW",
                LocalDateTime.of(2026, 6, 22, 8, 30));
        PositionMonitorLogDO second = logRow(2L, 7L, "ana-p0-6", "PLAN_INVALIDATED", "RECHECK_PLAN",
                LocalDateTime.of(2026, 6, 22, 9, 30));
        when(positionMonitorLogMapper.listAllByPositionIdForReview(7L)).thenReturn(List.of(first, second));

        List<PositionMonitorLogDTO> logs = service.listAllByPositionIdForReview(7L);

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
        command.setLogicStatus(logicStatus);
        command.setRiskLevel(riskLevel);
        command.setSuggestedAction(suggestedAction);
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
        row.setLogicStatus(logicStatus);
        row.setRiskLevel("HIGH");
        row.setSuggestedAction(suggestedAction);
        row.setCreatedAt(createdAt);
        return row;
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
