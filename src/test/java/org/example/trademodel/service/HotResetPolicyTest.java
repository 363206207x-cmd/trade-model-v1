package org.example.trademodel.service;

import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.HotResetEventTypeEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HotResetPolicyTest {

    @Test
    void extremePriceMoveReachesThresholdTriggers() {
        HotResetCommand command = base(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        command.setPriceMoveRatio(new BigDecimal("0.08"));

        assertThat(HotResetPolicy.evaluate(command).isTriggered()).isTrue();
    }

    @Test
    void extremePriceMoveBelowThresholdDoesNotTrigger() {
        HotResetCommand command = base(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        command.setPriceMoveRatio(new BigDecimal("0.079"));

        assertThat(HotResetPolicy.evaluate(command).isTriggered()).isFalse();
    }

    @Test
    void oiCollapseRequiresSourceAndThreshold() {
        HotResetCommand command = base(HotResetEventTypeEnum.OI_COLLAPSE);
        command.setOpenInterestChangeRatio(new BigDecimal("-0.30"));
        command.setSourceType("OI_SOURCE");
        command.setSourceReference("openInterestDelta");

        assertThat(HotResetPolicy.evaluate(command).isTriggered()).isTrue();

        command.setSourceReference(null);
        assertThat(HotResetPolicy.evaluate(command).isTriggered()).isFalse();
    }

    @Test
    void liquidityDrainRequiresEvidenceAndThreshold() {
        HotResetCommand command = base(HotResetEventTypeEnum.LIQUIDITY_DRAIN);
        command.setLiquidityChangeRatio(new BigDecimal("-0.40"));
        command.setSourceType("LIQUIDITY_SOURCE");
        command.setSourceReference("liquidityChangeRatio");

        assertThat(HotResetPolicy.evaluate(command).isTriggered()).isTrue();

        command.setLiquidityChangeRatio(null);
        command.setCurrentLiquidity(null);
        command.setBaselineLiquidity(null);
        assertThat(HotResetPolicy.evaluate(command).isTriggered()).isFalse();
    }

    @Test
    void systemicShockRequiresSourceAndSeverity() {
        HotResetCommand command = base(HotResetEventTypeEnum.SYSTEMIC_SHOCK);
        command.setSystemicShock(true);
        command.setSeverityScore(85);
        command.setSourceType("SYSTEMIC_SOURCE");
        command.setSourceReference("shock-feed-1");

        assertThat(HotResetPolicy.evaluate(command).isTriggered()).isTrue();

        command.setSourceType(null);
        assertThat(HotResetPolicy.evaluate(command).isTriggered()).isFalse();
    }

    @Test
    void postStateNeverReturnsUnsafeStates() {
        HotResetCommand command = base(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        ConfusedResult confused = new ConfusedResult(80, "TRIGGERED", "TRIGGERED",
                false, false, 0, false, "test", "test");

        AssetStateEnum postState = HotResetPolicy.resolvePostState(command, confused, false);

        assertThat(postState).isEqualTo(AssetStateEnum.INVALIDATED);
        assertThat(postState).isNotIn(AssetStateEnum.CANDIDATE, AssetStateEnum.WAITING_TRIGGER, AssetStateEnum.TRIGGERED);
    }

    private static HotResetCommand base(HotResetEventTypeEnum eventType) {
        HotResetCommand command = new HotResetCommand();
        command.setEventKey("event-key-" + eventType.name());
        command.setSymbol("BTCUSDT");
        command.setAnalysisId("ana-test");
        command.setEventType(eventType);
        return command;
    }
}
