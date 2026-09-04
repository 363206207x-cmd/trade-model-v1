package org.example.trademodel.service;

import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.vo.UserPositionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PositionMonitoringProjectionServiceTest {
    @Mock UserPositionService userPositionService;
    @Mock PositionMonitorLogService monitorLogService;
    @Mock ExecutionPlanMapper executionPlanMapper;
    @Mock AnalysisRunMapper analysisRunMapper;

    private PositionMonitoringProjectionService service;

    @BeforeEach
    void setUp() {
        service = new PositionMonitoringProjectionService(userPositionService, monitorLogService,
                executionPlanMapper, analysisRunMapper);
    }

    @Test
    void fourthPositionUsesItsOwnLatestMonitorInsteadOfHomeTopThree() {
        List<UserPositionVO> positions = List.of(
                position(1L, "BTCUSDT"), position(2L, "ETHUSDT"),
                position(3L, "SOLUSDT"), position(4L, "LINKUSDT"));
        when(userPositionService.listOpenPositionsForUser(7L)).thenReturn(positions);
        for (UserPositionVO position : positions) {
            when(monitorLogService.listByPositionIdForUser(7L, position.getId(), 1))
                    .thenReturn(List.of(trusted(position.getId(), new BigDecimal("104"))));
        }

        PositionMonitoringProjectionService.CollectionProjection result = service.listForUser(7L);

        assertThat(result.positions()).hasSize(4);
        assertThat(result.positions().get(3).position().getAssetSymbol()).isEqualTo("LINKUSDT");
        assertThat(result.positions().get(3).monitor().getPositionId()).isEqualTo(4L);
        assertThat(result.positions().get(3).monitor().getMarkPrice()).isEqualByComparingTo("104");
    }

    @Test
    void missingMonitorPreservesPositionFactsAndFailsClosedOnce() {
        UserPositionVO fourth = position(4L, "LINKUSDT");
        when(userPositionService.findByIdForUser(4L, 7L)).thenReturn(fourth);
        when(monitorLogService.listByPositionIdForUser(7L, 4L, 1)).thenReturn(List.of());

        PositionMonitoringProjectionService.ItemProjection result = service.findForUser(7L, 4L);

        assertThat(result.position().getEntryPrice()).isEqualByComparingTo("100");
        assertThat(result.monitorAvailable()).isFalse();
        assertThat(result.monitor().getMonitorTrustState()).isEqualTo("PENDING_FIRST_RUN");
        assertThat(result.monitor().getMarkPrice()).isNull();
        assertThat(result.monitor().getRiskLevel()).isNull();
        assertThat(result.monitor().getMonitorConclusion()).isNull();
        assertThat(result.monitor().getSuggestedAction()).isNull();
    }

    @Test
    void trustStatesRemainPositionLocalAndFailClosed() {
        UserPositionVO position = position(9L, "AAVEUSDT");
        position.setSourceType("SYSTEM_PLAN_POSITION");
        when(userPositionService.findByIdForUser(9L, 7L)).thenReturn(position);

        PositionMonitorLogDTO pending = trusted(9L, new BigDecimal("101"));
        pending.setMonitorSourceStatus("PENDING_VERIFICATION");
        when(monitorLogService.listByPositionIdForUser(7L, 9L, 1)).thenReturn(List.of(pending));
        assertThat(service.findForUser(7L, 9L).monitor().getMonitorTrustState()).isEqualTo("PENDING");

        PositionMonitorLogDTO stale = trusted(9L, new BigDecimal("101"));
        stale.setObservedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(2));
        stale.setFreshUntil(LocalDateTime.now(ZoneOffset.UTC).minusHours(1));
        when(monitorLogService.listByPositionIdForUser(7L, 9L, 1)).thenReturn(List.of(stale));
        assertThat(service.findForUser(7L, 9L).monitor().getMonitorTrustState()).isEqualTo("STALE");

        PositionMonitorLogDTO invalid = trusted(9L, new BigDecimal("101"));
        invalid.setMonitorSourceStatus("INVALID");
        when(monitorLogService.listByPositionIdForUser(7L, 9L, 1)).thenReturn(List.of(invalid));
        assertThat(service.findForUser(7L, 9L).monitor().getMonitorTrustState()).isEqualTo("INVALID");

        when(monitorLogService.listByPositionIdForUser(7L, 9L, 1))
                .thenReturn(List.of(trusted(9L, new BigDecimal("101"))));
        assertThat(service.findForUser(7L, 9L).monitorAvailable()).isTrue();
    }

    @Test
    void manualIndependentPendingContextStillProjectsFreshBasePriceAsPartial() {
        UserPositionVO position = position(13L, "ETHUSDT");
        when(userPositionService.findByIdForUser(13L, 7L)).thenReturn(position);
        PositionMonitorLogDTO partial = trusted(13L, new BigDecimal("2529.10"));
        partial.setAnalysisId(null);
        partial.setExecutionPlanId(null);
        partial.setEntryLogicStatus(null);
        partial.setMonitorConclusion(null);
        partial.setReversalStatus(null);
        partial.setRiskChangeReason(null);
        partial.setRiskLevel(null);
        partial.setRiskTrend(null);
        partial.setSuggestedAction(null);
        partial.setMonitorSourceStatus("PENDING_VERIFICATION");
        when(monitorLogService.listByPositionIdForUser(7L, 13L, 1)).thenReturn(List.of(partial));

        PositionMonitoringProjectionService.ItemProjection result = service.findForUser(7L, 13L);

        assertThat(result.monitorAvailable()).isTrue();
        assertThat(result.collectionState()).isEqualTo("PARTIAL");
        assertThat(result.monitor().getCurrentPrice()).isEqualByComparingTo("2529.10");
        assertThat(result.monitor().getMarkPriceFresh()).isTrue();
        assertThat(result.monitor().getEntryLogicStatus()).isEqualTo("NOT_APPLICABLE");
        assertThat(result.monitor().getDataState()).isEqualTo("PARTIAL");
        assertThat(result.monitor().getModuleState()).isEqualTo("PARTIAL");
    }

    @Test
    void partiallyClosedLifecycleRemainsInActiveProjection() {
        UserPositionVO partial = position(10L, "ETHUSDT");
        partial.setStatus("PARTIALLY_CLOSED");
        when(userPositionService.listOpenPositionsForUser(7L)).thenReturn(List.of(partial));
        when(monitorLogService.listByPositionIdForUser(7L, 10L, 1))
                .thenReturn(List.of(trusted(10L, new BigDecimal("101"))));

        PositionMonitoringProjectionService.CollectionProjection result = service.listForUser(7L);

        assertThat(result.positions()).singleElement().satisfies(item -> {
            assertThat(item.position().getStatus()).isEqualTo("PARTIALLY_CLOSED");
            assertThat(item.monitorAvailable()).isTrue();
        });
    }

    @Test
    void closedDetailPreservesFactsWithoutReadingOldMonitorAsCurrent() {
        UserPositionVO closed = position(11L, "SOLUSDT");
        closed.setStatus("CLOSED");
        closed.setClosePrice(new BigDecimal("112"));
        closed.setClosedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        closed.setCloseReason("用户记录平仓");
        when(userPositionService.findByIdForUser(11L, 7L)).thenReturn(closed);

        PositionMonitoringProjectionService.ItemProjection result = service.findForUser(7L, 11L);

        assertThat(result.position().getEntryPrice()).isEqualByComparingTo("100");
        assertThat(result.position().getClosePrice()).isEqualByComparingTo("112");
        assertThat(result.collectionState()).isEqualTo("CLOSED");
        assertThat(result.monitorAvailable()).isFalse();
        assertThat(result.monitor().getDataState()).isEqualTo("CLOSED");
        assertThat(result.monitor().getMonitorConclusion()).isNull();
        verify(monitorLogService, never()).listByPositionIdForUser(7L, 11L, 1);
    }

    @Test
    void historyRemainsOwnerScopedAndSeparateFromActiveMonitoring() {
        UserPositionVO closed = position(12L, "BTCUSDT");
        closed.setStatus("CLOSED");
        when(userPositionService.listClosedPositionsForUser(7L, 100)).thenReturn(List.of(closed));
        when(userPositionService.countClosedPositionsForUser(7L)).thenReturn(1);

        PositionMonitoringProjectionService.HistoryProjection result = service.historyForUser(7L, 100);

        assertThat(result.positions()).extracting(UserPositionVO::getId).containsExactly(12L);
        assertThat(result.totalCount()).isEqualTo(1);
        verify(userPositionService).listClosedPositionsForUser(7L, 100);
        verify(userPositionService).countClosedPositionsForUser(7L);
        verify(monitorLogService, never()).listByPositionIdForUser(7L, 12L, 1);
    }

    private static UserPositionVO position(Long id, String symbol) {
        UserPositionVO value = new UserPositionVO();
        value.setId(id);
        value.setAssetSymbol(symbol);
        value.setSide("LONG");
        value.setStatus("OPEN");
        value.setEntryPrice(new BigDecimal("100"));
        value.setQuantity(new BigDecimal("2"));
        value.setLeverage(new BigDecimal("2"));
        value.setOpenedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        value.setSourceType("MANUAL_INDEPENDENT");
        return value;
    }

    private static PositionMonitorLogDTO trusted(Long positionId, BigDecimal price) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PositionMonitorLogDTO value = new PositionMonitorLogDTO();
        value.setPositionId(positionId);
        value.setCurrentPrice(price);
        value.setMarkPriceSource("BINANCE_MARK_PRICE");
        value.setEntryLogicStatus("STILL_VALID");
        value.setMonitorConclusion("LOGIC_VALID");
        value.setReversalStatus("NO_REVERSAL");
        value.setRiskChangeReason("NO_CLEAR_RISK_FACTOR");
        value.setRiskLevel("LOW");
        value.setRiskTrend("STABLE");
        value.setSuggestedAction("CONTINUE_HOLD");
        value.setMonitorSourceStatus("VERIFIED");
        value.setObservedAt(now.minusMinutes(1));
        value.setFreshUntil(now.plusMinutes(5));
        value.setCreatedAt(now);
        return value;
    }
}
