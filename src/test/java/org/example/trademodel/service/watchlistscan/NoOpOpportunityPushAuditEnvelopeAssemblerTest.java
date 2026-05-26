package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionStatusEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("core-regression")
class NoOpOpportunityPushAuditEnvelopeAssemblerTest {

    private final NoOpOpportunityPushAuditEnvelopeAssembler assembler =
            new NoOpOpportunityPushAuditEnvelopeAssembler();

    @Test
    void nullInputFailsClosed() {
        OpportunityPushAuditEnvelopeDTO result = assembler.assemble(null, null);

        assertThat(result.getEnvelopeStatus()).isEqualTo(OpportunityPushAuditEnvelopeStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("DELIVERY_DECISION_MISSING", "INCOMPLETE");
        assertAuditOnlyNoDeliveryDefaults(result);
    }

    @Test
    void blankSymbolFailsClosedIfSymbolIsSeparatelyProvided() {
        OpportunityPushAuditEnvelopeDTO result = assembler.assemble(" ", reviewOnlyDecision());

        assertThat(result.getEnvelopeStatus()).isEqualTo(OpportunityPushAuditEnvelopeStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("DECISION_BLOCKING_REASON", "SYMBOL_MISSING",
                "INCOMPLETE");
        assertAuditOnlyNoDeliveryDefaults(result);
    }

    @Test
    void missingDeliveryDecisionFailsClosed() {
        OpportunityPushAuditEnvelopeDTO result = assembler.assemble("BTCUSDT", null);

        assertThat(result.getEnvelopeStatus()).isEqualTo(OpportunityPushAuditEnvelopeStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("DELIVERY_DECISION_MISSING", "INCOMPLETE");
        assertAuditOnlyNoDeliveryDefaults(result);
    }

    @Test
    void unsafeDeliveryDecisionFailsClosed() throws Exception {
        OpportunityPushAuditEnvelopeDTO result = assembler.assemble("BTCUSDT", unsafeDecision());

        assertThat(result.getEnvelopeStatus()).isEqualTo(OpportunityPushAuditEnvelopeStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("DECISION_BLOCKING_REASON", "DELIVERY_DECISION_UNSAFE",
                "INCOMPLETE");
        assertThat(result.getDeliveryReasons()).contains("DELIVERY_REVIEW_ONLY");
        assertAuditOnlyNoDeliveryDefaults(result);
    }

    @Test
    void nonReviewOnlyDeliveryDecisionRemainsBlockedDisabledOrIncomplete() {
        List<OpportunityPushAuditEnvelopeDTO> results = List.of(
                assembler.assemble("BTCUSDT", blockedDecision()),
                assembler.assemble("BTCUSDT", disabledDecision()),
                assembler.assemble("BTCUSDT", incompleteDecision())
        );

        assertThat(results)
                .extracting(OpportunityPushAuditEnvelopeDTO::getEnvelopeStatus)
                .containsExactly(
                        OpportunityPushAuditEnvelopeStatusEnum.BLOCKED,
                        OpportunityPushAuditEnvelopeStatusEnum.DISABLED,
                        OpportunityPushAuditEnvelopeStatusEnum.INCOMPLETE
                );
        for (OpportunityPushAuditEnvelopeDTO result : results) {
            assertThat(result.getBlockingReasons()).contains("DELIVERY_DECISION_NOT_REVIEW_ONLY");
            assertThat(result.getEnvelopeStatus()).isNotEqualTo(OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY);
            assertAuditOnlyNoDeliveryDefaults(result);
        }
    }

    @Test
    void safeReviewOnlyDeliveryDecisionCanProduceOnlyAuditOnlyEnvelope() {
        OpportunityPushAuditEnvelopeDTO result = assembler.assemble(null, reviewOnlyDecision());

        assertThat(result.getEnvelopeStatus()).isEqualTo(OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY);
        assertThat(result.getDeliveryDecisionStatus())
                .isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getSource()).isEqualTo("unit-test");
        assertThat(result.getEnvelopeReasons()).contains(
                "OPPORTUNITY_PUSH_AUDIT_ONLY_ENVELOPE",
                "AUDIT_ONLY_INTERNAL_ENVELOPE"
        );
        assertThat(result.getDeliveryReasons()).contains("DELIVERY_REVIEW_ONLY");
        assertThat(result.getPushReasons()).contains("PUSH_REVIEW_ONLY");
        assertThat(result.getAttentionReasons()).contains("ATTENTION_REVIEW_ONLY");
        assertThat(result.getRiskGuardReasons()).contains("RISK_ACTION_GUARD_REVIEW_ONLY");
        assertThat(result.getBlockingReasons()).contains("DECISION_BLOCKING_REASON");
        assertAuditOnlyNoDeliveryDefaults(result);
    }

    @Test
    void everyOutputKeepsAuditOnlyNoDeliveryDefaults() throws Exception {
        List<OpportunityPushAuditEnvelopeDTO> outputs = List.of(
                assembler.assemble(null, null),
                assembler.assemble(" ", reviewOnlyDecision()),
                assembler.assemble("BTCUSDT", null),
                assembler.assemble("BTCUSDT", unsafeDecision()),
                assembler.assemble("BTCUSDT", blockedDecision()),
                assembler.assemble("BTCUSDT", disabledDecision()),
                assembler.assemble("BTCUSDT", incompleteDecision()),
                assembler.assemble("BTCUSDT", reviewOnlyDecision())
        );

        for (OpportunityPushAuditEnvelopeDTO output : outputs) {
            assertAuditOnlyNoDeliveryDefaults(output);
        }
    }

    @Test
    void dtoDefensiveCopy() {
        List<String> envelopeReasons = new ArrayList<>(List.of("ENVELOPE_REASON"));
        List<String> deliveryReasons = new ArrayList<>(List.of("DELIVERY_REASON"));
        List<String> pushReasons = new ArrayList<>(List.of("PUSH_REASON"));
        List<String> attentionReasons = new ArrayList<>(List.of("ATTENTION_REASON"));
        List<String> riskGuardReasons = new ArrayList<>(List.of("RISK_REASON"));
        List<String> blockingReasons = new ArrayList<>(List.of("BLOCK_REASON"));

        OpportunityPushAuditEnvelopeDTO result = OpportunityPushAuditEnvelopeDTO.auditOnly(
                "BTCUSDT",
                OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                "unit-test",
                envelopeReasons,
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                blockingReasons
        );

        envelopeReasons.add("MUTATED_ENVELOPE");
        deliveryReasons.add("MUTATED_DELIVERY");
        pushReasons.add("MUTATED_PUSH");
        attentionReasons.add("MUTATED_ATTENTION");
        riskGuardReasons.add("MUTATED_RISK");
        blockingReasons.add("MUTATED_BLOCK");
        result.getEnvelopeReasons().add("GETTER_MUTATION");
        result.getDeliveryReasons().add("GETTER_MUTATION");
        result.getPushReasons().add("GETTER_MUTATION");
        result.getAttentionReasons().add("GETTER_MUTATION");
        result.getRiskGuardReasons().add("GETTER_MUTATION");
        result.getBlockingReasons().add("GETTER_MUTATION");

        assertThat(result.getEnvelopeReasons()).containsExactly("ENVELOPE_REASON");
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

        for (OpportunityPushAuditEnvelopeStatusEnum status : OpportunityPushAuditEnvelopeStatusEnum.values()) {
            assertThat(status.name()).isNotIn(forbidden);
        }
    }

    @Test
    void implementationHasNoForbiddenDependenciesOrMethodSurface() {
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

        for (Field field : NoOpOpportunityPushAuditEnvelopeAssembler.class.getDeclaredFields()) {
            for (String token : forbidden) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(OpportunityPushAuditEnvelopeAssembler.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = OpportunityPushAuditEnvelopeAssembler.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("assemble");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(OpportunityPushAuditEnvelopeDTO.class);

        for (Method method : NoOpOpportunityPushAuditEnvelopeAssembler.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            assertThat(methodName).doesNotContain("send");
            assertThat(methodName).doesNotContain("notify");
            assertThat(methodName).doesNotContain("delivernow");
            assertThat(methodName).doesNotContain("enqueue");
            assertThat(methodName).doesNotContain("persist");
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

    private static OpportunityPushDeliveryDecisionDTO reviewOnlyDecision() {
        return OpportunityPushDeliveryDecisionDTO.reviewOnly(
                "BTCUSDT",
                "unit-test",
                List.of("DELIVERY_REVIEW_ONLY"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("DECISION_BLOCKING_REASON")
        );
    }

    private static OpportunityPushDeliveryDecisionDTO blockedDecision() {
        return OpportunityPushDeliveryDecisionDTO.blocked(
                "BTCUSDT",
                "unit-test",
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_BLOCKED"),
                List.of("DECISION_BLOCKING_REASON")
        );
    }

    private static OpportunityPushDeliveryDecisionDTO disabledDecision() {
        return OpportunityPushDeliveryDecisionDTO.disabled(
                "BTCUSDT",
                "unit-test",
                List.of("PUSH_DISABLED"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("DECISION_DISABLED_REASON")
        );
    }

    private static OpportunityPushDeliveryDecisionDTO incompleteDecision() {
        return OpportunityPushDeliveryDecisionDTO.incomplete(
                "BTCUSDT",
                List.of("DECISION_INCOMPLETE_REASON")
        );
    }

    private static OpportunityPushDeliveryDecisionDTO unsafeDecision() throws Exception {
        OpportunityPushDeliveryDecisionDTO decision = reviewOnlyDecision();
        Field field = OpportunityPushDeliveryDecisionDTO.class.getDeclaredField("externalPushSent");
        field.setAccessible(true);
        field.setBoolean(decision, true);
        return decision;
    }

    private static void assertAuditOnlyNoDeliveryDefaults(OpportunityPushAuditEnvelopeDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isAuditOnly()).isTrue();
        assertThat(result.isExternalPushSent()).isFalse();
        assertThat(result.isDeliveryAttempted()).isFalse();
        assertThat(result.isDeliveryEnabled()).isFalse();
        assertThat(result.isPersisted()).isFalse();
        assertThat(result.isQueued()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }
}
