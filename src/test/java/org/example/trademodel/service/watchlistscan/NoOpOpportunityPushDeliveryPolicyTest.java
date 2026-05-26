package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionStatusEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("core-regression")
class NoOpOpportunityPushDeliveryPolicyTest {

    private final NoOpOpportunityPushDeliveryPolicy policy = new NoOpOpportunityPushDeliveryPolicy();

    @Test
    void nullInputFailsClosed() {
        OpportunityPushDeliveryDecisionDTO result = policy.evaluate(null, null, null);

        assertThat(result.getDecisionStatus()).isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("OPPORTUNITY_PUSH_MISSING", "INCOMPLETE");
        assertSafeNoDeliveryDefaults(result);
    }

    @Test
    void blankSymbolFailsClosedIfSymbolIsSeparatelyProvided() {
        OpportunityPushDeliveryDecisionDTO result = policy.evaluate(" ", reviewOnlyPush(), List.of());

        assertThat(result.getDecisionStatus()).isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SYMBOL_MISSING", "INCOMPLETE");
        assertSafeNoDeliveryDefaults(result);
    }

    @Test
    void missingOpportunityPushFailsClosed() {
        OpportunityPushDeliveryDecisionDTO result = policy.evaluate("BTCUSDT", null, List.of());

        assertThat(result.getDecisionStatus()).isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("OPPORTUNITY_PUSH_MISSING", "INCOMPLETE");
        assertSafeNoDeliveryDefaults(result);
    }

    @Test
    void unsafeOpportunityPushFailsClosed() throws Exception {
        OpportunityPushDeliveryDecisionDTO result = policy.evaluate("BTCUSDT", unsafePush(), List.of());

        assertThat(result.getDecisionStatus()).isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("PUSH_BLOCKING_REASON", "OPPORTUNITY_PUSH_UNSAFE",
                "INCOMPLETE");
        assertSafeNoDeliveryDefaults(result);
    }

    @Test
    void nonReviewOnlyOpportunityPushRemainsDisabled() {
        OpportunityPushDeliveryDecisionDTO result = policy.evaluate(
                "BTCUSDT",
                OpportunityPushDTO.disabled("BTCUSDT", List.of("PUSH_DISABLED_BY_TEST")),
                List.of()
        );

        assertThat(result.getDecisionStatus()).isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.DISABLED);
        assertThat(result.getBlockingReasons())
                .contains("PUSH_DISABLED_BY_TEST", "DISABLED", "OPPORTUNITY_PUSH_NOT_REVIEW_ONLY");
        assertSafeNoDeliveryDefaults(result);
    }

    @Test
    void stampedeOrExtremeStressBlocksDeliveryEligibility() {
        OpportunityPushDeliveryDecisionDTO result = policy.evaluate(
                "BTCUSDT",
                reviewOnlyPush(),
                List.of("STAMPEDE_DETECTED")
        );

        assertThat(result.getDecisionStatus()).isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.BLOCKED);
        assertThat(result.getRiskGuardReasons()).contains("RISK_ACTION_GUARD_REVIEW_ONLY", "STAMPEDE_DETECTED");
        assertThat(result.getBlockingReasons()).contains(
                "PUSH_BLOCKING_REASON",
                "STAMPEDE_OR_EXTREME_STRESS_BLOCKS_DELIVERY",
                "BLOCKED"
        );
        assertSafeNoDeliveryDefaults(result);
    }

    @Test
    void liquidityDeteriorationBlocksDeliverySemantics() {
        OpportunityPushDeliveryDecisionDTO result = policy.evaluate(
                "BTCUSDT",
                reviewOnlyPush(),
                List.of("LIQUIDITY_DETERIORATION")
        );

        assertThat(result.getDecisionStatus()).isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.BLOCKED);
        assertThat(result.getRiskGuardReasons()).contains("LIQUIDITY_DETERIORATION");
        assertThat(result.getBlockingReasons()).contains(
                "LIQUIDITY_DETERIORATION_BLOCKS_DELIVERY",
                "BLOCKED"
        );
        assertSafeNoDeliveryDefaults(result);
    }

    @Test
    void wickOnlyOrPinBarDirectReversalReasonBlocksTrendReversalDeliverySemantics() {
        OpportunityPushDeliveryDecisionDTO result = policy.evaluate(
                "BTCUSDT",
                reviewOnlyPush(),
                List.of("PIN_BAR_DIRECT_REVERSAL")
        );

        assertThat(result.getDecisionStatus()).isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.BLOCKED);
        assertThat(result.getRiskGuardReasons()).contains("PIN_BAR_DIRECT_REVERSAL");
        assertThat(result.getBlockingReasons()).contains(
                "WICK_ONLY_REVERSAL_BLOCKS_DELIVERY",
                "BLOCKED"
        );
        assertSafeNoDeliveryDefaults(result);
    }

    @Test
    void safeReviewOnlyOpportunityPushCanProduceOnlyNoOpReviewOnlyDeliveryDecision() {
        OpportunityPushDeliveryDecisionDTO result = policy.evaluate(
                null,
                reviewOnlyPush(),
                List.of("DELIVERY_REVIEW_ONLY_CONTEXT")
        );

        assertThat(result.getDecisionStatus()).isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getSource()).isEqualTo("unit-test");
        assertThat(result.getDeliveryReasons()).contains(
                "OPPORTUNITY_PUSH_NOOP_REVIEW_ONLY",
                "PUSH_CHANNEL_DISABLED_BY_DEFAULT"
        );
        assertThat(result.getPushReasons()).contains("OPPORTUNITY_PUSH_REVIEW_ONLY");
        assertThat(result.getAttentionReasons()).contains("CANDIDATE_ATTENTION_REVIEW_ONLY");
        assertThat(result.getRiskGuardReasons())
                .contains("RISK_ACTION_GUARD_REVIEW_ONLY", "DELIVERY_REVIEW_ONLY_CONTEXT");
        assertThat(result.getBlockingReasons()).contains("PUSH_BLOCKING_REASON");
        assertSafeNoDeliveryDefaults(result);
    }

    @Test
    void everyOutputKeepsNoDeliveryDefaults() throws Exception {
        List<OpportunityPushDeliveryDecisionDTO> outputs = List.of(
                policy.evaluate(null, null, null),
                policy.evaluate(" ", reviewOnlyPush(), List.of()),
                policy.evaluate("BTCUSDT", null, List.of()),
                policy.evaluate("BTCUSDT", unsafePush(), List.of()),
                policy.evaluate("BTCUSDT", OpportunityPushDTO.disabled("BTCUSDT", List.of()), List.of()),
                policy.evaluate("BTCUSDT", reviewOnlyPush(), List.of("STAMPEDE_DETECTED")),
                policy.evaluate("BTCUSDT", reviewOnlyPush(), List.of("LIQUIDITY_DETERIORATION")),
                policy.evaluate("BTCUSDT", reviewOnlyPush(), List.of("WICK_ONLY_DIRECT_REVERSAL")),
                policy.evaluate("BTCUSDT", reviewOnlyPush(), List.of())
        );

        for (OpportunityPushDeliveryDecisionDTO output : outputs) {
            assertSafeNoDeliveryDefaults(output);
        }
    }

    @Test
    void dtoDefensiveCopy() {
        List<String> deliveryReasons = new ArrayList<>(List.of("DELIVERY_REASON"));
        List<String> pushReasons = new ArrayList<>(List.of("PUSH_REASON"));
        List<String> attentionReasons = new ArrayList<>(List.of("ATTENTION_REASON"));
        List<String> riskGuardReasons = new ArrayList<>(List.of("RISK_REASON"));
        List<String> blockingReasons = new ArrayList<>(List.of("BLOCK_REASON"));

        OpportunityPushDeliveryDecisionDTO result = OpportunityPushDeliveryDecisionDTO.reviewOnly(
                "BTCUSDT",
                "unit-test",
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                blockingReasons
        );

        deliveryReasons.add("MUTATED_DELIVERY");
        pushReasons.add("MUTATED_PUSH");
        attentionReasons.add("MUTATED_ATTENTION");
        riskGuardReasons.add("MUTATED_RISK");
        blockingReasons.add("MUTATED_BLOCK");
        result.getDeliveryReasons().add("GETTER_MUTATION");
        result.getPushReasons().add("GETTER_MUTATION");
        result.getAttentionReasons().add("GETTER_MUTATION");
        result.getRiskGuardReasons().add("GETTER_MUTATION");
        result.getBlockingReasons().add("GETTER_MUTATION");

        assertThat(result.getDeliveryReasons()).containsExactly("DELIVERY_REASON");
        assertThat(result.getPushReasons()).containsExactly("PUSH_REASON");
        assertThat(result.getAttentionReasons()).containsExactly("ATTENTION_REASON");
        assertThat(result.getRiskGuardReasons()).containsExactly("RISK_REASON");
        assertThat(result.getBlockingReasons()).containsExactly("BLOCK_REASON");
    }

    @Test
    void enumNamesExposeNoTradingOrExecutionSurface() {
        List<String> forbidden = List.of(
                "BUY",
                "SELL",
                "LONG",
                "SHORT",
                "READY",
                "EXECUTABLE",
                "SENT",
                "TRADE",
                "ORDER",
                "ENTRY",
                "STOP",
                "TAKE_PROFIT"
        );

        for (OpportunityPushDeliveryDecisionStatusEnum status : OpportunityPushDeliveryDecisionStatusEnum.values()) {
            assertThat(status.name()).isNotIn(forbidden);
        }
    }

    @Test
    void implementationHasNoForbiddenDependencies() {
        List<String> forbidden = List.of(
                "Controller",
                "Scheduler",
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "Webhook",
                "Telegram",
                "Email",
                "AppNotification",
                "LocalNotification",
                "DataSource",
                "JdbcTemplate",
                "Order",
                "Execution",
                "AutoTrading",
                "Scheduled"
        );

        for (Field field : NoOpOpportunityPushDeliveryPolicy.class.getDeclaredFields()) {
            for (String token : forbidden) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(OpportunityPushDeliveryPolicy.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = OpportunityPushDeliveryPolicy.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("evaluate");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(OpportunityPushDeliveryDecisionDTO.class);

        for (Method method : NoOpOpportunityPushDeliveryPolicy.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            assertThat(methodName).doesNotContain("send");
            assertThat(methodName).doesNotContain("notify");
            assertThat(methodName).doesNotContain("delivernow");
            assertThat(methodName).doesNotContain("execute");
            assertThat(methodName).doesNotContain("trade");
            assertThat(methodName).doesNotContain("order");
            for (String token : forbidden) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("Scheduled");
            }
        }
    }

    private static OpportunityPushDTO reviewOnlyPush() {
        return OpportunityPushDTO.reviewOnly(
                "BTCUSDT",
                "unit-test",
                List.of("OPPORTUNITY_PUSH_REVIEW_ONLY"),
                List.of("CANDIDATE_ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PUSH_BLOCKING_REASON")
        );
    }

    private static OpportunityPushDTO unsafePush() throws Exception {
        OpportunityPushDTO push = reviewOnlyPush();
        Field field = OpportunityPushDTO.class.getDeclaredField("externalPushSent");
        field.setAccessible(true);
        field.setBoolean(push, true);
        return push;
    }

    private static void assertSafeNoDeliveryDefaults(OpportunityPushDeliveryDecisionDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isExternalPushSent()).isFalse();
        assertThat(result.isDeliveryAttempted()).isFalse();
        assertThat(result.isDeliveryEnabled()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }
}
