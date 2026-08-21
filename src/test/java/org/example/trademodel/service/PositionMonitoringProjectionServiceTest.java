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
        assertThat(result.monitor().getMonitorTrustState()).isEqualTo("SOURCE_UNAVAILABLE");
        assertThat(result.monitor().getMarkPrice()).isNull();
        assertThat(result.monitor().getRiskLevel()).isNull();
        assertThat(result.monitor().getMonitorConclusion()).isNull();
        assertThat(result.monitor().getSuggestedAction()).isNull();
    }

    @Test
    void trustStatesRemainPositionLocalAndFailClosed() {
        UserPositionVO position = position(9L, "AAVEUSDT");
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
