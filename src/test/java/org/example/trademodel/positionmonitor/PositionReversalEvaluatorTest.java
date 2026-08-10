package org.example.trademodel.positionmonitor;

import org.example.trademodel.positionmonitorlog.PositionReversalStatusEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PositionReversalEvaluatorTest {
    private final PositionReversalEvaluator evaluator = new PositionReversalEvaluator();

    @Test
    void mapsAllThreeReversalStatesFromTheIndependentRuleDirection() {
        assertThat(evaluator.evaluate("LONG", "BULLISH").status())
                .isEqualTo(PositionReversalStatusEnum.NO_REVERSAL);
        assertThat(evaluator.evaluate("LONG", "WEAK_BEARISH").status())
                .isEqualTo(PositionReversalStatusEnum.WEAK_REVERSAL);
        assertThat(evaluator.evaluate("LONG", "STRONG_BEARISH").status())
                .isEqualTo(PositionReversalStatusEnum.STRONG_REVERSAL);

        assertThat(evaluator.evaluate("SHORT", "BEARISH").status())
                .isEqualTo(PositionReversalStatusEnum.NO_REVERSAL);
        assertThat(evaluator.evaluate("SHORT", "WEAK_BULLISH").status())
                .isEqualTo(PositionReversalStatusEnum.WEAK_REVERSAL);
        assertThat(evaluator.evaluate("SHORT", "BULLISH").status())
                .isEqualTo(PositionReversalStatusEnum.STRONG_REVERSAL);
    }

    @Test
    void missingOrUnknownRuleDirectionIsUnavailableInsteadOfInventingNoReversal() {
        assertThat(evaluator.evaluate("LONG", null).sourceAvailable()).isFalse();
        assertThat(evaluator.evaluate("SHORT", "UNKNOWN").sourceAvailable()).isFalse();
    }
}
