package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.CandidateAttentionDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushStatusEnum;
import org.junit.jupiter.api.Test;

class DefaultOpportunityPushRuleTest {

    private final DefaultOpportunityPushRule rule = new DefaultOpportunityPushRule();

    @Test
    void nullInputFailsClosed() {
        OpportunityPushDTO result = rule.evaluate(null, null, null);

        assertThat(result.getPushStatus()).isEqualTo(OpportunityPushStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("CANDIDATE_ATTENTION_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void blankSymbolFailsClosed() {
        OpportunityPushDTO result = rule.evaluate(" ", reviewOnlyCandidate(), List.of());

        assertThat(result.getPushStatus()).isEqualTo(OpportunityPushStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SYMBOL_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void missingCandidateAttentionFailsClosed() {
        OpportunityPushDTO result = rule.evaluate("BTCUSDT", null, List.of());

        assertThat(result.getPushStatus()).isEqualTo(OpportunityPushStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("CANDIDATE_ATTENTION_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void unsafeOrBlockedCandidateAttentionFailsClosed() throws Exception {
        OpportunityPushDTO unsafe = rule.evaluate("BTCUSDT", unsafeCandidate(), List.of());
        OpportunityPushDTO blocked = rule.evaluate(
                "BTCUSDT",
                CandidateAttentionDTO.disabled("BTCUSDT", List.of("CANDIDATE_ATTENTION_DISABLED_BY_TEST")),
                List.of()
        );

        assertThat(unsafe.getPushStatus()).isEqualTo(OpportunityPushStatusEnum.INCOMPLETE);
        assertThat(unsafe.getBlockingReasons()).contains("CANDIDATE_ATTENTION_UNSAFE", "INCOMPLETE");
        assertSafeNoExecutionDefaults(unsafe);

        assertThat(blocked.getPushStatus()).isEqualTo(OpportunityPushStatusEnum.INCOMPLETE);
        assertThat(blocked.getBlockingReasons())
                .contains("CANDIDATE_ATTENTION_DISABLED_BY_TEST", "DISABLED",
                        "CANDIDATE_ATTENTION_NOT_REVIEW_ONLY", "INCOMPLETE");
        assertSafeNoExecutionDefaults(blocked);
    }

    @Test
    void stampedeOrExtremeStressBlocksPushEligibility() {
        OpportunityPushDTO result = rule.evaluate(
                "BTCUSDT",
                reviewOnlyCandidate(),
                List.of("STAMPEDE_DETECTED")
        );

        assertThat(result.getPushStatus()).isEqualTo(OpportunityPushStatusEnum.BLOCKED);
        assertThat(result.getBlockingReasons()).contains(
                "STAMPEDE_DETECTED",
                "STAMPEDE_OR_EXTREME_STRESS_BLOCKS_PUSH",
                "BLOCKED"
        );
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void liquidityDeteriorationBlocksExecutionLikePushSemantics() {
        OpportunityPushDTO result = rule.evaluate(
                "BTCUSDT",
                reviewOnlyCandidate(),
                List.of("LIQUIDITY_DETERIORATION")
        );

        assertThat(result.getPushStatus()).isEqualTo(OpportunityPushStatusEnum.BLOCKED);
        assertThat(result.getBlockingReasons()).contains(
                "LIQUIDITY_DETERIORATION",
                "LIQUIDITY_DETERIORATION_BLOCKS_EXECUTION_LIKE_PUSH",
                "BLOCKED"
        );
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void wickOnlyOrPinBarDirectReversalReasonBlocksTrendReversalPushSemantics() {
        OpportunityPushDTO result = rule.evaluate(
                "BTCUSDT",
                reviewOnlyCandidate(),
                List.of("WICK_ONLY_DIRECT_REVERSAL")
        );

        assertThat(result.getPushStatus()).isEqualTo(OpportunityPushStatusEnum.BLOCKED);
        assertThat(result.getBlockingReasons()).contains(
                "WICK_ONLY_DIRECT_REVERSAL",
                "WICK_ONLY_REVERSAL_BLOCKED",
                "BLOCKED"
        );
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void safeReviewOnlyCandidateAttentionCanProduceReviewOnlyOpportunityPushCandidate() {
        OpportunityPushDTO result = rule.evaluate(
                "btcusdt",
                reviewOnlyCandidate(),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY")
        );

        assertThat(result.getPushStatus()).isEqualTo(OpportunityPushStatusEnum.REVIEW_ONLY);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getSource()).isEqualTo("unit-test");
        assertThat(result.getPushReasons()).contains("OPPORTUNITY_PUSH_REVIEW_ONLY");
        assertThat(result.getAttentionReasons()).contains("CANDIDATE_ATTENTION_REVIEW_ONLY");
        assertThat(result.getRiskGuardReasons()).contains("RISK_ACTION_GUARD_REVIEW_ONLY");
        assertThat(result.getBlockingReasons()).contains("CANDIDATE_BLOCKING_REASON");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void everyOutputKeepsNoExecutionDefaults() throws Exception {
        List<OpportunityPushDTO> outputs = List.of(
                rule.evaluate(null, null, null),
                rule.evaluate(" ", reviewOnlyCandidate(), List.of()),
                rule.evaluate("BTCUSDT", null, List.of()),
                rule.evaluate("BTCUSDT", unsafeCandidate(), List.of()),
                rule.evaluate("BTCUSDT", reviewOnlyCandidate(), List.of("STAMPEDE_DETECTED")),
                rule.evaluate("BTCUSDT", reviewOnlyCandidate(), List.of("LIQUIDITY_DETERIORATION")),
                rule.evaluate("BTCUSDT", reviewOnlyCandidate(), List.of("WICK_ONLY_DIRECT_REVERSAL")),
                rule.evaluate("BTCUSDT", reviewOnlyCandidate(), List.of())
        );

        for (OpportunityPushDTO output : outputs) {
            assertSafeNoExecutionDefaults(output);
        }
    }

    @Test
    void dtoDefensiveCopy() {
        List<String> pushReasons = new ArrayList<>(List.of("PUSH_REASON"));
        List<String> attentionReasons = new ArrayList<>(List.of("ATTENTION_REASON"));
        List<String> riskGuardReasons = new ArrayList<>(List.of("RISK_REASON"));
        List<String> blockingReasons = new ArrayList<>(List.of("BLOCK_REASON"));

        OpportunityPushDTO result = OpportunityPushDTO.reviewOnly(
                "BTCUSDT",
                "unit-test",
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                blockingReasons
        );

        pushReasons.add("MUTATED_PUSH");
        attentionReasons.add("MUTATED_ATTENTION");
        riskGuardReasons.add("MUTATED_RISK");
        blockingReasons.add("MUTATED_BLOCK");
        result.getPushReasons().add("GETTER_MUTATION");
        result.getAttentionReasons().add("GETTER_MUTATION");
        result.getRiskGuardReasons().add("GETTER_MUTATION");
        result.getBlockingReasons().add("GETTER_MUTATION");

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

        for (OpportunityPushStatusEnum status : OpportunityPushStatusEnum.values()) {
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
                "DataSource",
                "JdbcTemplate",
                "Order",
                "Execution",
                "AutoTrading",
                "Scheduled"
        );

        for (Field field : DefaultOpportunityPushRule.class.getDeclaredFields()) {
            for (String token : forbidden) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(OpportunityPushRule.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = OpportunityPushRule.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("evaluate");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(OpportunityPushDTO.class);

        for (Method method : DefaultOpportunityPushRule.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            assertThat(methodName).doesNotContain("send");
            assertThat(methodName).doesNotContain("notify");
            assertThat(methodName).doesNotContain("readiness");
            assertThat(methodName).doesNotContain("order");
            assertThat(methodName).doesNotContain("execute");
            assertThat(methodName).doesNotContain("trade");
            for (String token : forbidden) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("Scheduled");
            }
        }
    }

    private static CandidateAttentionDTO reviewOnlyCandidate() {
        return CandidateAttentionDTO.reviewOnly(
                "BTCUSDT",
                "unit-test",
                List.of("CANDIDATE_ATTENTION_REVIEW_ONLY"),
                List.of("SCANSCORE_REVIEW_ONLY_SKELETON"),
                List.of("CANDIDATE_BLOCKING_REASON")
        );
    }

    private static CandidateAttentionDTO unsafeCandidate() throws Exception {
        CandidateAttentionDTO candidate = reviewOnlyCandidate();
        Field field = CandidateAttentionDTO.class.getDeclaredField("opportunityPushAllowed");
        field.setAccessible(true);
        field.setBoolean(candidate, true);
        return candidate;
    }

    private static void assertSafeNoExecutionDefaults(OpportunityPushDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isExternalPushSent()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }
}
