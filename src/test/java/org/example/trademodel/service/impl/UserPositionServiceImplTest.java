package org.example.trademodel.service.impl;

import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class UserPositionServiceImplTest {
    private static final Long USER_ID = 17L;

    @Mock
    private UserPositionMapper userPositionMapper;

    private UserPositionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserPositionServiceImpl(userPositionMapper);
        lenient().when(userPositionMapper.insert(any())).thenReturn(1);
    }

    @Test
    void manualOpenCreatesOpenManualUserPositionWithSafetyFlags() {
        CreateUserPositionReq request = validOpenRequest();

        UserPositionVO vo = service.manualOpenForUser(USER_ID, request);

        ArgumentCaptor<UserPositionDO> captor = ArgumentCaptor.forClass(UserPositionDO.class);
        verify(userPositionMapper).insert(captor.capture());
        UserPositionDO row = captor.getValue();
        assertThat(row.getAssetSymbol()).isEqualTo("BTCUSDT");
        assertThat(row.getUserId()).isEqualTo(USER_ID);
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
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, missingSymbol))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asset_symbol");

        CreateUserPositionReq badSide = validOpenRequest();
        badSide.setSide("FLAT");
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, badSide))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LONG or SHORT");

        CreateUserPositionReq badPrice = validOpenRequest();
        badPrice.setEntryPrice(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, badPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entry_price");

        CreateUserPositionReq badQuantity = validOpenRequest();
        badQuantity.setQuantity(new BigDecimal("-1"));
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, badQuantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");

        CreateUserPositionReq badLeverage = validOpenRequest();
        badLeverage.setLeverage(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, badLeverage))
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
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden UserPosition input field");

        CreateUserPositionReq executionRequest = validOpenRequest();
        executionRequest.putExtraField("execution_action", "OPEN");
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, executionRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden UserPosition input field");

        CreateUserPositionReq autoTradingRequest = validOpenRequest();
        autoTradingRequest.putExtraField("autoTradingAction", "OPEN");
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, autoTradingRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden UserPosition input field");

        CreateUserPositionReq positionSyncRequest = validOpenRequest();
        positionSyncRequest.putExtraField("position_sync", "true");
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, positionSyncRequest))
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
        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(open, closed);
        when(userPositionMapper.manualCloseByIdAndUserId(
                eq(7L), eq(USER_ID), any(), eq(new BigDecimal("105.25")), eq("manual exit"), any()))
                .thenReturn(1);

        UserPositionVO vo = service.manualCloseForUser(7L, USER_ID, closeRequest("105.25", "manual exit"));

        assertThat(vo.getStatus()).isEqualTo("CLOSED");
        assertThat(vo.getClosePrice()).isEqualByComparingTo("105.25");
        assertThat(vo.isNotTradeInstruction()).isTrue();
        assertThat(vo.isNotAutoTrading()).isTrue();
        verify(userPositionMapper).manualCloseByIdAndUserId(
                eq(7L), eq(USER_ID), any(), eq(new BigDecimal("105.25")), eq("manual exit"), any());
    }

    @Test
    void closingClosedPositionFailsClosed() {
        when(userPositionMapper.selectByIdAndUserId(9L, USER_ID)).thenReturn(row(9L, "CLOSED"));

        assertThatThrownBy(() -> service.manualCloseForUser(9L, USER_ID, closeRequest("105.25", "already closed")))
                .isInstanceOf(UserPositionConflictException.class)
                .hasMessageContaining("not OPEN");

        verify(userPositionMapper, never()).manualCloseByIdAndUserId(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void manualCloseRejectsInvalidClosePriceAndForbiddenFields() {
        CloseUserPositionReq badPrice = closeRequest("0", "bad");
        assertThatThrownBy(() -> service.manualCloseForUser(1L, USER_ID, badPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("close_price");

        CloseUserPositionReq forbidden = closeRequest("10", "bad");
        forbidden.putExtraField("executionAction", "CLOSE");
        assertThatThrownBy(() -> service.manualCloseForUser(1L, USER_ID, forbidden))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forbidden UserPosition input field");

        verify(userPositionMapper, never()).manualCloseByIdAndUserId(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void listOpenPositionsReturnsOnlyOpenVisibleStatusesWithSafetyFields() {
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(
                row(1L, "OPEN"),
                row(2L, "PARTIALLY_CLOSED")
        ));

        List<UserPositionVO> rows = service.listOpenPositionsForUser(USER_ID);

        assertThat(rows).hasSize(1);
        assertThat(rows).extracting(UserPositionVO::getStatus)
                .containsExactly("OPEN");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.isManualReviewRequired()).isTrue();
            assertThat(row.isNotTradeInstruction()).isTrue();
            assertThat(row.isNotAutoTrading()).isTrue();
            assertThat(row.isNotOrderExecution()).isTrue();
            assertThat(row.isNotPositionSync()).isTrue();
        });
    }

    @Test
    void everyUserScopedServiceEntryFailsClosedWithoutCanonicalOwner() {
        assertThatThrownBy(() -> service.manualOpenForUser(null, validOpenRequest()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("userId");
        assertThatThrownBy(() -> service.manualCloseForUser(1L, null, closeRequest("101", "manual")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("userId");
        assertThatThrownBy(() -> service.listOpenPositionsForUser(null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("userId");
        assertThatThrownBy(() -> service.findByIdForUser(1L, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("userId");
        verify(userPositionMapper, never()).insert(any());
    }

    @Test
    void nonOwnerAndUnclaimedRowsShareNotFoundSemantics() {
        when(userPositionMapper.selectByIdAndUserId(71L, USER_ID)).thenReturn(null);
        when(userPositionMapper.selectByIdAndUserId(72L, USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.findByIdForUser(71L, USER_ID))
                .isInstanceOf(UserPositionNotFoundException.class)
                .hasMessage("UserPosition not found");
        assertThatThrownBy(() -> service.manualCloseForUser(
                72L, USER_ID, closeRequest("101", "manual")))
                .isInstanceOf(UserPositionNotFoundException.class)
                .hasMessage("UserPosition not found");
    }

    @Test
    void twoConcurrentCloseRequestsProduceOneSuccessAndOneConflict() throws Exception {
        AtomicBoolean closedState = new AtomicBoolean(false);
        UserPositionDO open = row(81L, "OPEN");
        UserPositionDO closed = row(81L, "CLOSED");
        closed.setClosedAt(LocalDateTime.of(2026, 6, 22, 9, 0));
        closed.setClosePrice(new BigDecimal("105.25"));
        when(userPositionMapper.selectByIdAndUserId(81L, USER_ID))
                .thenAnswer(invocation -> closedState.get() ? closed : open);
        when(userPositionMapper.manualCloseByIdAndUserId(
                eq(81L), eq(USER_ID), any(), eq(new BigDecimal("105.25")), eq("manual exit"), any()))
                .thenAnswer(invocation -> closedState.compareAndSet(false, true) ? 1 : 0);

        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = executor.submit(() -> closeAfter(start));
            Future<Object> second = executor.submit(() -> closeAfter(start));
            start.countDown();
            List<Object> results = List.of(first.get(), second.get());

            assertThat(results.stream().filter(UserPositionVO.class::isInstance)).hasSize(1);
            assertThat(results.stream().filter(UserPositionConflictException.class::isInstance)).hasSize(1);
            assertThat(closedState).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void repeatedManualOpenIsNotSilentlyDeduplicatedOrRetried() {
        AtomicLong ids = new AtomicLong(100L);
        when(userPositionMapper.insert(any())).thenAnswer(invocation -> {
            invocation.<UserPositionDO>getArgument(0).setId(ids.incrementAndGet());
            return 1;
        });

        UserPositionVO first = service.manualOpenForUser(USER_ID, validOpenRequest());
        UserPositionVO second = service.manualOpenForUser(USER_ID, validOpenRequest());

        assertThat(first.getId()).isNotEqualTo(second.getId());
        verify(userPositionMapper, times(2)).insert(any());
    }

    private Object closeAfter(CountDownLatch start) {
        try {
            start.await();
            return service.manualCloseForUser(81L, USER_ID, closeRequest("105.25", "manual exit"));
        } catch (UserPositionConflictException conflict) {
            return conflict;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private void assertAutoSourceRejected(String sourceType) {
        CreateUserPositionReq request = validOpenRequest();
        request.setSourceType(sourceType);
        assertThatThrownBy(() -> service.manualOpenForUser(USER_ID, request))
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
        row.setUserId(USER_ID);
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
