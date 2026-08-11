package org.example.trademodel.risk;

import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.UserPositionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class UserPositionRiskAdapterTest {
    private static final Long USER_ID = 17L;

    @Mock
    private UserPositionMapper userPositionMapper;

    private DefaultUserPositionRiskAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DefaultUserPositionRiskAdapter(userPositionMapper);
        when(userPositionMapper.countClosedByUserId(USER_ID)).thenReturn(0);
    }

    @Test
    void openAndPartiallyClosedPositionsAreIncludedAndClosedPositionsAreExcluded() {
        when(userPositionMapper.countClosedByUserId(USER_ID)).thenReturn(1);
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(
                row(1L, "BTCUSDT", "LONG", "OPEN", "100", "1", "2", "95"),
                row(2L, "ETHUSDT", "SHORT", "PARTIALLY_CLOSED", "100", "1", "2", "105"),
                row(3L, "SOLUSDT", "LONG", "CLOSED", "100", "1", "2", "95")
        ));

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.getIncludedPositionCount()).isEqualTo(2);
        assertThat(result.getOpenPositionCount()).isEqualTo(1);
        assertThat(result.getPartiallyClosedPositionCount()).isEqualTo(1);
        assertThat(result.getExcludedClosedPositionCount()).isEqualTo(2);
        assertThat(result.isRiskBlocked()).isFalse();
    }

    @Test
    void emptyUserPositionsReturnExplicitSafeNoOpenState() {
        when(userPositionMapper.countClosedByUserId(USER_ID)).thenReturn(2);
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of());

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.getRiskStatus()).isEqualTo("NO_OPEN_USER_POSITION");
        assertThat(result.getRiskLevel()).isEqualTo("LOW");
        assertThat(result.isRiskBlocked()).isFalse();
        assertThat(result.getExcludedClosedPositionCount()).isEqualTo(2);
        assertThat(result.getReasonCodes()).contains("NO_OPEN_USER_POSITION");
    }

    @Test
    void highLeverageReturnsRiskBlocked() {
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(
                row(1L, "BTCUSDT", "LONG", "OPEN", "100", "1", "12", "95")
        ));

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.isRiskBlocked()).isTrue();
        assertThat(result.getRiskStatus()).isEqualTo("RISK_BLOCKED");
        assertThat(result.getReasonCodes()).contains("HIGH_LEVERAGE_RISK");
    }

    @Test
    void highConcentrationReturnsRiskBlocked() {
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(
                row(1L, "BTCUSDT", "LONG", "OPEN", "100", "20", "2", "95"),
                row(2L, "BTCUSDT", "LONG", "OPEN", "100", "20", "2", "95"),
                row(3L, "ETHUSDT", "SHORT", "OPEN", "100", "2", "2", "105")
        ));

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.isRiskBlocked()).isTrue();
        assertThat(result.getReasonCodes()).contains("CONCENTRATION_RISK_BLOCKED");
    }

    @Test
    void balancedMultiAssetPositionsRemainAllowed() {
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(
                row(1L, "BTCUSDT", "LONG", "OPEN", "100", "1", "2", "95"),
                row(2L, "ETHUSDT", "SHORT", "OPEN", "100", "1", "2", "105"),
                row(3L, "SOLUSDT", "LONG", "OPEN", "100", "1", "2", "95")
        ));

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.getRiskStatus()).isEqualTo("RISK_ALLOWED");
        assertThat(result.isRiskBlocked()).isFalse();
        assertThat(result.getGrossNotional()).isEqualByComparingTo("600.00000000");
    }

    @Test
    void conservativeDirectionalProxyBlocksCorrelationRisk() {
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(
                row(1L, "BTCUSDT", "LONG", "OPEN", "100", "1", "2", "95"),
                row(2L, "ETHUSDT", "LONG", "OPEN", "100", "1", "2", "95")
        ));

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.isRiskBlocked()).isTrue();
        assertThat(result.getReasonCodes()).contains("CORRELATION_DIRECTIONAL_PROXY_BLOCKED");
        assertThat(result.getCalculationMethod()).contains("conservative directional proxy");
    }

    @Test
    void drawdownOrVarProxyBlocksLargeStopLossLoss() {
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(
                row(1L, "BTCUSDT", "LONG", "OPEN", "100", "200", "1", "50")
        ));

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.isRiskBlocked()).isTrue();
        assertThat(result.getReasonCodes()).contains("DRAWDOWN_OR_VAR_RISK_BLOCKED");
    }

    @Test
    void missingStopLossFailsClosed() {
        UserPositionDO row = row(1L, "BTCUSDT", "LONG", "OPEN", "100", "1", "2", "95");
        row.setStopLoss(null);
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(row));

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.isRiskBlocked()).isTrue();
        assertThat(result.getReasonCodes()).contains("STOP_LOSS_REQUIRED_FAIL_CLOSED");
    }

    @Test
    void invalidPriceQuantityOrLeverageFailsClosed() {
        UserPositionDO badPrice = row(1L, "BTCUSDT", "LONG", "OPEN", "0", "1", "2", "95");
        UserPositionDO badQuantity = row(2L, "ETHUSDT", "SHORT", "OPEN", "100", "0", "2", "105");
        UserPositionDO badLeverage = row(3L, "SOLUSDT", "LONG", "OPEN", "100", "1", "0", "95");
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(badPrice, badQuantity, badLeverage));

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.isRiskBlocked()).isTrue();
        assertThat(result.getReasonCodes()).contains(
                "ENTRY_PRICE_REQUIRED_FAIL_CLOSED",
                "QUANTITY_REQUIRED_FAIL_CLOSED",
                "LEVERAGE_REQUIRED_FAIL_CLOSED");
    }

    @Test
    void riskResultContainsSafetyFieldsAndNoExecutableActionFields() {
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(
                row(1L, "BTCUSDT", "LONG", "OPEN", "100", "1", "2", "95")
        ));

        UserPositionRiskResult result = adapter.currentRiskForUser(USER_ID);

        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isManualReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotAutoReduce()).isTrue();
        assertThat(result.isNotAutoClose()).isTrue();
        assertThat(result.isNotAutoReverse()).isTrue();
        assertThat(result.isNotUserPositionMutation()).isTrue();
        assertThat(Arrays.stream(UserPositionRiskResult.class.getDeclaredFields()).map(java.lang.reflect.Field::getName))
                .doesNotContain("reduceAction", "closeAction", "reverseAction", "orderAction",
                        "executionAction", "autoTradingAction", "executablePayload", "providerPayload");
    }

    @Test
    void adapterReadsOnlyUserPositionFactsAndDoesNotMutatePositions() {
        when(userPositionMapper.listOpenByUserId(USER_ID)).thenReturn(List.of(
                row(1L, "BTCUSDT", "LONG", "OPEN", "100", "1", "2", "95")
        ));

        UserPositionRiskAdapter monitorCompatibleCaller = adapter;
        UserPositionRiskResult result = monitorCompatibleCaller.currentRiskForUser(USER_ID);

        assertThat(result.getIncludedPositionCount()).isEqualTo(1);
        verify(userPositionMapper).countClosedByUserId(USER_ID);
        verify(userPositionMapper).listOpenByUserId(USER_ID);
        verify(userPositionMapper, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(userPositionMapper, never()).manualCloseByIdAndUserId(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(userPositionMapper, never()).selectByIdAndUserId(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static UserPositionDO row(Long id,
                                      String symbol,
                                      String side,
                                      String status,
                                      String entryPrice,
                                      String quantity,
                                      String leverage,
                                      String stopLoss) {
        UserPositionDO row = new UserPositionDO();
        row.setId(id);
        row.setUserId(USER_ID);
        row.setAssetSymbol(symbol);
        row.setSide(side);
        row.setStatus(status);
        row.setEntryPrice(new BigDecimal(entryPrice));
        row.setQuantity(new BigDecimal(quantity));
        row.setLeverage(new BigDecimal(leverage));
        row.setStopLoss(new BigDecimal(stopLoss));
        row.setSourceType("MANUAL_POSITION");
        return row;
    }
}
