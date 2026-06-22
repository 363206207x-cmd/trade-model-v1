package org.example.trademodel.service.impl;

import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.vo.UserPositionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class UserPositionServiceImplTest {
    @Mock
    private UserPositionMapper userPositionMapper;

    private UserPositionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserPositionServiceImpl(userPositionMapper);
    }

    @Test
    void manualOpenCreatesOpenManualUserPositionWithSafetyFlags() {
        CreateUserPositionReq request = validOpenRequest();

        UserPositionVO vo = service.manualOpen(request);

        ArgumentCaptor<UserPositionDO> captor = ArgumentCaptor.forClass(UserPositionDO.class);
        verify(userPositionMapper).insert(captor.capture());
        UserPositionDO row = captor.getValue();
        assertThat(row.getAssetSymbol()).isEqualTo("BTCUSDT");
        assertThat(row.getSide()).isEqualTo("LONG");
        assertThat(row.getStatus()).isEqualTo("OPEN");
        assertThat(row.getEntryPrice()).isEqualByComparingTo("100.50");
        assertThat(row.getQuantity()).isEqualByComparingTo("0.25");
        assertThat(row.getLeverage()).isEqualByComparingTo("2");
        assertThat(row.getSourceType()).isEqualTo("MANUAL");
        assertThat(row.getManualReviewRequired()).isTrue();
        assertThat(row.getNotTradeInstruction()).isTrue();
        assertThat(row.getNotAutoTrading()).isTrue();
        assertThat(row.getNotOrderExecution()).isTrue();
        assertThat(row.getNotPositionSync()).isTrue();

        assertThat(vo.getStatus()).isEqualTo("OPEN");
        assertThat(vo.getSourceType()).isEqualTo("MANUAL");
        assertThat(vo.isManualReviewRequired()).isTrue();
        assertThat(vo.isNotTradeInstruction()).isTrue();
        assertThat(vo.isNotAutoTrading()).isTrue();
        assertThat(vo.isNotOrderExecution()).isTrue();
        assertThat(vo.isNotPositionSync()).isTrue();
    }

    @Test
    void manualOpenRejectsInvalidPriceQuantityLeverageAndSide() {
        CreateUserPositionReq missingSymbol = validOpenRequest();
        missingSymbol.setAssetSymbol(" ");
        assertThatThrownBy(() -> service.manualOpen(missingSymbol))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asset_symbol");

        CreateUserPositionReq badSide = validOpenRequest();
        badSide.setSide("FLAT");
        assertThatThrownBy(() -> service.manualOpen(badSide))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LONG or SHORT");

        CreateUserPositionReq badPrice = validOpenRequest();
        badPrice.setEntryPrice(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.manualOpen(badPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entry_price");

        CreateUserPositionReq badQuantity = validOpenRequest();
        badQuantity.setQuantity(new BigDecimal("-1"));
        assertThatThrownBy(() -> service.manualOpen(badQuantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");

        CreateUserPositionReq badLeverage = validOpenRequest();
        badLeverage.setLeverage(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.manualOpen(badLeverage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leverage");

        verify(userPositionMapper, never()).insert(any());
    }

    @Test
    void executionPlanTriggeredAndRealPositionSourcesCannotAutoCreateUserPosition() {
        assertAutoSourceRejected("PLAN_AUTO");
        assertAutoSourceRejected("TRIGGERED_AUTO");
        assertAutoSourceRejected("REAL_POSITION_SYNC_AUTO");

        verify(userPositionMapper, never()).insert(any());
    }

    @Test
    void manualOpenRejectsOrderExecutionAutoTradingAndPositionSyncInputFields() {
        CreateUserPositionReq request = validOpenRequest();
        request.putExtraField("orderAction", "BUY");
        assertThatThrownBy(() -> service.manualOpen(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden UserPosition input field");

        CreateUserPositionReq executionRequest = validOpenRequest();
        executionRequest.putExtraField("execution_action", "OPEN");
        assertThatThrownBy(() -> service.manualOpen(executionRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden UserPosition input field");

        CreateUserPositionReq autoTradingRequest = validOpenRequest();
        autoTradingRequest.putExtraField("autoTradingAction", "OPEN");
        assertThatThrownBy(() -> service.manualOpen(autoTradingRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden UserPosition input field");

        CreateUserPositionReq positionSyncRequest = validOpenRequest();
        positionSyncRequest.putExtraField("position_sync", "true");
        assertThatThrownBy(() -> service.manualOpen(positionSyncRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden UserPosition input field");

        verify(userPositionMapper, never()).insert(any());
    }

    @Test
    void manualCloseChangesOpenPositionToClosed() {
        UserPositionDO open = row(7L, "OPEN");
        UserPositionDO closed = row(7L, "CLOSED");
        closed.setClosedAt(LocalDateTime.of(2026, 6, 22, 9, 0));
        closed.setClosePrice(new BigDecimal("105.25"));
        when(userPositionMapper.selectById(7L)).thenReturn(open, closed);
        when(userPositionMapper.manualClose(eq(7L), any(), eq(new BigDecimal("105.25")), eq("manual exit"), any()))
                .thenReturn(1);

        UserPositionVO vo = service.manualClose(7L, closeRequest("105.25", "manual exit"));

        assertThat(vo.getStatus()).isEqualTo("CLOSED");
        assertThat(vo.getClosePrice()).isEqualByComparingTo("105.25");
        assertThat(vo.isNotTradeInstruction()).isTrue();
        assertThat(vo.isNotAutoTrading()).isTrue();
        verify(userPositionMapper).manualClose(eq(7L), any(), eq(new BigDecimal("105.25")), eq("manual exit"), any());
    }

    @Test
    void closingClosedPositionFailsClosed() {
        when(userPositionMapper.selectById(9L)).thenReturn(row(9L, "CLOSED"));

        assertThatThrownBy(() -> service.manualClose(9L, closeRequest("105.25", "already closed")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only OPEN or PARTIALLY_CLOSED");

        verify(userPositionMapper, never()).manualClose(any(), any(), any(), any(), any());
    }

    @Test
    void manualCloseRejectsInvalidClosePriceAndForbiddenFields() {
        CloseUserPositionReq badPrice = closeRequest("0", "bad");
        assertThatThrownBy(() -> service.manualClose(1L, badPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("close_price");

        CloseUserPositionReq forbidden = closeRequest("10", "bad");
        forbidden.putExtraField("executionAction", "CLOSE");
        assertThatThrownBy(() -> service.manualClose(1L, forbidden))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden UserPosition input field");

        verify(userPositionMapper, never()).manualClose(any(), any(), any(), any(), any());
    }

    @Test
    void listOpenPositionsReturnsOnlyOpenVisibleStatusesWithSafetyFields() {
        when(userPositionMapper.listOpenPositions()).thenReturn(List.of(
                row(1L, "OPEN"),
                row(2L, "PARTIALLY_CLOSED")
        ));

        List<UserPositionVO> rows = service.listOpenPositions();

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(UserPositionVO::getStatus)
                .containsExactly("OPEN", "PARTIALLY_CLOSED");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.isManualReviewRequired()).isTrue();
            assertThat(row.isNotTradeInstruction()).isTrue();
            assertThat(row.isNotAutoTrading()).isTrue();
            assertThat(row.isNotOrderExecution()).isTrue();
            assertThat(row.isNotPositionSync()).isTrue();
        });
    }

    private void assertAutoSourceRejected(String sourceType) {
        CreateUserPositionReq request = validOpenRequest();
        request.setSourceType(sourceType);
        assertThatThrownBy(() -> service.manualOpen(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source_type must be MANUAL");
    }

    private static CreateUserPositionReq validOpenRequest() {
        CreateUserPositionReq request = new CreateUserPositionReq();
        request.setAssetSymbol(" btcusdt ");
        request.setSide("LONG");
        request.setEntryPrice(new BigDecimal("100.50"));
        request.setQuantity(new BigDecimal("0.25"));
        request.setLeverage(new BigDecimal("2"));
        request.setStopLoss(new BigDecimal("95.00"));
        request.setTakeProfit(new BigDecimal("120.00"));
        request.setSourceRefId("manual-note-1");
        return request;
    }

    private static CloseUserPositionReq closeRequest(String price, String reason) {
        CloseUserPositionReq request = new CloseUserPositionReq();
        request.setClosePrice(new BigDecimal(price));
        request.setCloseReason(reason);
        return request;
    }

    private static UserPositionDO row(Long id, String status) {
        UserPositionDO row = new UserPositionDO();
        row.setId(id);
        row.setAssetSymbol("BTCUSDT");
        row.setSide("LONG");
        row.setStatus(status);
        row.setEntryPrice(new BigDecimal("100.50"));
        row.setQuantity(new BigDecimal("0.25"));
        row.setLeverage(new BigDecimal("2"));
        row.setOpenedAt(LocalDateTime.of(2026, 6, 22, 8, 30));
        row.setSourceType("MANUAL");
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
        row.setCreatedAt(LocalDateTime.of(2026, 6, 22, 8, 30));
        row.setUpdatedAt(LocalDateTime.of(2026, 6, 22, 8, 30));
        return row;
    }
}
