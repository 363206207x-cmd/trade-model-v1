package org.example.trademodel.positionmonitor;

import org.example.trademodel.entity.UserPositionDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SinglePositionRiskCalculatorTest {
    private final SinglePositionRiskCalculator calculator = new SinglePositionRiskCalculator();

    @Test
    void calculatesRiskFromEachPositionInsteadOfSharingOwnerAggregateRisk() {
        UserPositionDO lowRisk = position("LONG", "100", "1", "1", "90", "120");
        UserPositionDO highRisk = position("LONG", "100", "1", "10", "90", "120");

        SinglePositionRiskCalculator.Assessment low = calculator.calculate(
                lowRisk, new BigDecimal("101"), false, false);
        SinglePositionRiskCalculator.Assessment high = calculator.calculate(
                highRisk, new BigDecimal("101"), false, false);

        assertThat(low.level()).isEqualTo(PositionRiskLevelEnum.LOW);
        assertThat(high.level()).isEqualTo(PositionRiskLevelEnum.HIGH);
        assertThat(low.level()).isNotEqualTo(high.level());
    }

    @Test
    void supportsExtremeRiskForAnIndividualPosition() {
        UserPositionDO position = position("LONG", "100", "1", "20", "90", "120");

        SinglePositionRiskCalculator.Assessment result = calculator.calculate(
                position, new BigDecimal("100"), false, false);

        assertThat(result.level()).isEqualTo(PositionRiskLevelEnum.EXTREME);
        assertThat(result.riskBlocked()).isTrue();
    }

    @Test
    void evaluatesShortAdverseMovementUsingShortDirection() {
        UserPositionDO position = position("SHORT", "100", "1", "2", "115", "80");

        SinglePositionRiskCalculator.Assessment result = calculator.calculate(
                position, new BigDecimal("110"), false, false);

        assertThat(result.leveragedAdverseMovePercent()).isEqualByComparingTo("20");
        assertThat(result.level()).isEqualTo(PositionRiskLevelEnum.EXTREME);
    }

    @Test
    void missingOrInvalidRiskBoundariesFailClosedPerPosition() {
        UserPositionDO missingBoundaries = position("LONG", "100", "1", "1", null, null);

        SinglePositionRiskCalculator.Assessment missing = calculator.calculate(
                missingBoundaries, new BigDecimal("100"), false, false);

        assertThat(missing.level()).isEqualTo(PositionRiskLevelEnum.HIGH);
        assertThat(missing.reasonCodes()).contains("STOP_LOSS_MISSING", "TAKE_PROFIT_MISSING");

        UserPositionDO invalidBoundaries = position("SHORT", "100", "1", "1", "90", "120");

        SinglePositionRiskCalculator.Assessment invalid = calculator.calculate(
                invalidBoundaries, new BigDecimal("100"), false, false);

        assertThat(invalid.level()).isEqualTo(PositionRiskLevelEnum.EXTREME);
        assertThat(invalid.reasonCodes())
                .contains("STOP_LOSS_DIRECTION_INVALID", "TAKE_PROFIT_DIRECTION_INVALID");
    }

    private static UserPositionDO position(String side,
                                           String entryPrice,
                                           String quantity,
                                           String leverage,
                                           String stopLoss,
                                           String takeProfit) {
        UserPositionDO position = new UserPositionDO();
        position.setSide(side);
        position.setEntryPrice(new BigDecimal(entryPrice));
        position.setQuantity(new BigDecimal(quantity));
        position.setLeverage(new BigDecimal(leverage));
        position.setStopLoss(stopLoss == null ? null : new BigDecimal(stopLoss));
        position.setTakeProfit(takeProfit == null ? null : new BigDecimal(takeProfit));
        return position;
    }
}
