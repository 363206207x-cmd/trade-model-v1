package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionStatusEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("core-regression")
class NoOpOpportunityPushAuditEnvelopePersistencePortTest {

    private final NoOpOpportunityPushAuditEnvelopePersistencePort port =
            new NoOpOpportunityPushAuditEnvelopePersistencePort();

    @Test
    void nullInputFailsClosed() {
        OpportunityPushAuditPersistenceResultDTO result = port.evaluate(null, null);

        assertThat(result.getPersistenceStatus()).isEqualTo(OpportunityPushAuditPersistenceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("AUDIT_ENVELOPE_MISSING", "INCOMPLETE");
        assertReviewOnlyNoPersistenceDefaults(result);
    }

    @Test
    void blankSymbolFailsClosedIfSymbolIsSeparatelyProvided() {
        OpportunityPushAuditPersistenceResultDTO result = port.evaluate(" ", auditOnlyEnvelope());

        assertThat(result.getPersistenceStatus()).isEqualTo(OpportunityPushAuditPersistenceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("AUDIT_BLOCKING_REASON", "SYMBOL_MISSING", "INCOMPLETE");
        assertReviewOnlyNoPersistenceDefaults(result);
    }

    @Test
    void missingAuditEnvelopeFailsClosed() {
        OpportunityPushAuditPersistenceResultDTO result = port.evaluate("BTCUSDT", null);

        assertThat(result.getPersistenceStatus()).isEqualTo(OpportunityPushAuditPersistenceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("AUDIT_ENVELOPE_MISSING", "INCOMPLETE");
        assertReviewOnlyNoPersistenceDefaults(result);
    }

    @Test
    void unsafeAuditEnvelopeFailsClosed() throws Exception {
        OpportunityPushAuditPersistenceResultDTO result = port.evaluate("BTCUSDT", unsafeEnvelope());

        assertThat(result.getPersistenceStatus()).isEqualTo(OpportunityPushAuditPersistenceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("AUDIT_BLOCKING_REASON", "AUDIT_ENVELOPE_UNSAFE",
                "INCOMPLETE");
        assertThat(result.getEnvelopeReasons()).contains("AUDIT_ONLY_ENVELOPE");
        assertReviewOnlyNoPersistenceDefaults(result);
    }

    @Test
    void nonAuditOnlyAuditEnvelopeRemainsBlockedDisabledOrIncomplete() {
        List<OpportunityPushAuditPersistenceResultDTO> results = List.of(
                port.evaluate("BTCUSDT", blockedEnvelope()),
                port.evaluate("BTCUSDT", disabledEnvelope()),
                port.evaluate("BTCUSDT", incompleteEnvelope())
        );

        assertThat(results)
                .extracting(OpportunityPushAuditPersistenceResultDTO::getPersistenceStatus)
                .containsExactly(
                        OpportunityPushAuditPersistenceStatusEnum.BLOCKED,
                        OpportunityPushAuditPersistenceStatusEnum.DISABLED,
                        OpportunityPushAuditPersistenceStatusEnum.INCOMPLETE
                );
        for (OpportunityPushAuditPersistenceResultDTO result : results) {
            assertThat(result.getBlockingReasons()).contains("AUDIT_ENVELOPE_NOT_AUDIT_ONLY");
            assertThat(result.getPersistenceStatus())
                    .isNotEqualTo(OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY);
            assertReviewOnlyNoPersistenceDefaults(result);
        }
    }

    @Test
    void safeAuditOnlyEnvelopeCanProduceOnlyNoOpPersistenceResult() {
        OpportunityPushAuditPersistenceResultDTO result = port.evaluate(null, auditOnlyEnvelope());

        assertThat(result.getPersistenceStatus()).isEqualTo(OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY);
        assertThat(result.getEnvelopeStatus()).isEqualTo(OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY);
        assertThat(result.getDeliveryDecisionStatus())
                .isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getSource()).isEqualTo("unit-test");
        assertThat(result.getPersistenceReasons()).contains(
                "AUDIT_ENVELOPE_NOOP_REVIEW_ONLY",
                "AUDIT_PERSISTENCE_DISABLED_BY_DEFAULT"
        );
        assertThat(result.getEnvelopeReasons()).contains("AUDIT_ONLY_ENVELOPE");
        assertThat(result.getDeliveryReasons()).contains("DELIVERY_REVIEW_ONLY");
        assertThat(result.getPushReasons()).contains("PUSH_REVIEW_ONLY");
        assertThat(result.getAttentionReasons()).contains("ATTENTION_REVIEW_ONLY");
        assertThat(result.getRiskGuardReasons()).contains("RISK_ACTION_GUARD_REVIEW_ONLY");
        assertThat(result.getBlockingReasons()).contains("AUDIT_BLOCKING_REASON");
        assertReviewOnlyNoPersistenceDefaults(result);
    }

    @Test
    void everyOutputKeepsReviewOnlyNoPersistenceDefaults() throws Exception {
        List<OpportunityPushAuditPersistenceResultDTO> outputs = List.of(
                port.evaluate(null, null),
                port.evaluate(" ", auditOnlyEnvelope()),
                port.evaluate("BTCUSDT", null),
                port.evaluate("BTCUSDT", unsafeEnvelope()),
                port.evaluate("BTCUSDT", blockedEnvelope()),
                port.evaluate("BTCUSDT", disabledEnvelope()),
                port.evaluate("BTCUSDT", incompleteEnvelope()),
                port.evaluate("BTCUSDT", auditOnlyEnvelope())
        );

        for (OpportunityPushAuditPersistenceResultDTO output : outputs) {
            assertReviewOnlyNoPersistenceDefaults(output);
        }
    }

    @Test
    void dtoDefensiveCopy() {
        List<String> persistenceReasons = new ArrayList<>(List.of("PERSISTENCE_REASON"));
        List<String> envelopeReasons = new ArrayList<>(List.of("ENVELOPE_REASON"));
        List<String> deliveryReasons = new ArrayList<>(List.of("DELIVERY_REASON"));
        List<String> pushReasons = new ArrayList<>(List.of("PUSH_REASON"));
        List<String> attentionReasons = new ArrayList<>(List.of("ATTENTION_REASON"));
        List<String> riskGuardReasons = new ArrayList<>(List.of("RISK_REASON"));
        List<String> blockingReasons = new ArrayList<>(List.of("BLOCK_REASON"));

        OpportunityPushAuditPersistenceResultDTO result =
                OpportunityPushAuditPersistenceResultDTO.noopReviewOnly(
                        "BTCUSDT",
                        OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY,
                        OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                        "unit-test",
                        persistenceReasons,
                        envelopeReasons,
                        deliveryReasons,
                        pushReasons,
                        attentionReasons,
                        riskGuardReasons,
                        blockingReasons
                );

        persistenceReasons.add("MUTATED_PERSISTENCE");
        envelopeReasons.add("MUTATED_ENVELOPE");
        deliveryReasons.add("MUTATED_DELIVERY");
        pushReasons.add("MUTATED_PUSH");
        attentionReasons.add("MUTATED_ATTENTION");
        riskGuardReasons.add("MUTATED_RISK");
        blockingReasons.add("MUTATED_BLOCK");
        result.getPersistenceReasons().add("GETTER_MUTATION");
        result.getEnvelopeReasons().add("GETTER_MUTATION");
        result.getDeliveryReasons().add("GETTER_MUTATION");
        result.getPushReasons().add("GETTER_MUTATION");
        result.getAttentionReasons().add("GETTER_MUTATION");
        result.getRiskGuardReasons().add("GETTER_MUTATION");
        result.getBlockingReasons().add("GETTER_MUTATION");

        assertThat(result.getPersistenceReasons()).containsExactly("PERSISTENCE_REASON");
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

        for (OpportunityPushAuditPersistenceStatusEnum status : OpportunityPushAuditPersistenceStatusEnum.values()) {
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
                "Mapper",
                "Repository",
                "DataSource",
                "JdbcTemplate",
                "Order",
                "Execution",
                "AutoTrading",
                "Scheduled"
        );

        for (Field field : NoOpOpportunityPushAuditEnvelopePersistencePort.class.getDeclaredFields()) {
            for (String token : forbidden) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(OpportunityPushAuditEnvelopePersistencePort.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = OpportunityPushAuditEnvelopePersistencePort.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("evaluate");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(OpportunityPushAuditPersistenceResultDTO.class);

        for (Method method : NoOpOpportunityPushAuditEnvelopePersistencePort.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            assertThat(methodName).doesNotContain("send");
            assertThat(methodName).doesNotContain("notify");
            assertThat(methodName).doesNotContain("delivernow");
            assertThat(methodName).doesNotContain("enqueue");
            assertThat(methodName).doesNotContain("queue");
            assertThat(methodName).doesNotContain("persistnow");
            assertThat(methodName).doesNotContain("save");
            assertThat(methodName).doesNotContain("insert");
            assertThat(methodName).doesNotContain("update");
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

        assertThat(NoOpOpportunityPushAuditEnvelopePersistencePort.class.getDeclaredAnnotations()).isEmpty();
        assertThat(OpportunityPushAuditEnvelopePersistencePort.class.getDeclaredAnnotations()).isEmpty();
    }

    private static OpportunityPushAuditEnvelopeDTO auditOnlyEnvelope() {
        return OpportunityPushAuditEnvelopeDTO.auditOnly(
                "BTCUSDT",
                OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                "unit-test",
                List.of("AUDIT_ONLY_ENVELOPE"),
                List.of("DELIVERY_REVIEW_ONLY"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("AUDIT_BLOCKING_REASON")
        );
    }

    private static OpportunityPushAuditEnvelopeDTO blockedEnvelope() {
        return OpportunityPushAuditEnvelopeDTO.blocked(
                "BTCUSDT",
                OpportunityPushDeliveryDecisionStatusEnum.BLOCKED,
                "unit-test",
                List.of("DELIVERY_BLOCKED"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_BLOCKED"),
                List.of("AUDIT_BLOCKED_REASON")
        );
    }

    private static OpportunityPushAuditEnvelopeDTO disabledEnvelope() {
        return OpportunityPushAuditEnvelopeDTO.disabled(
                "BTCUSDT",
                OpportunityPushDeliveryDecisionStatusEnum.DISABLED,
                "unit-test",
                List.of("DELIVERY_DISABLED"),
                List.of("PUSH_DISABLED"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("AUDIT_DISABLED_REASON")
        );
    }

    private static OpportunityPushAuditEnvelopeDTO incompleteEnvelope() {
        return OpportunityPushAuditEnvelopeDTO.incomplete(
                "BTCUSDT",
                OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE,
                "unit-test",
                List.of("DELIVERY_INCOMPLETE"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("AUDIT_INCOMPLETE_REASON")
        );
    }

    private static OpportunityPushAuditEnvelopeDTO unsafeEnvelope() throws Exception {
        OpportunityPushAuditEnvelopeDTO envelope = auditOnlyEnvelope();
        Field field = OpportunityPushAuditEnvelopeDTO.class.getDeclaredField("persisted");
        field.setAccessible(true);
        field.setBoolean(envelope, true);
        return envelope;
    }

    private static void assertReviewOnlyNoPersistenceDefaults(OpportunityPushAuditPersistenceResultDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isAuditOnly()).isTrue();
        assertThat(result.isPersisted()).isFalse();
        assertThat(result.isPersistenceAttempted()).isFalse();
        assertThat(result.isQueueCreated()).isFalse();
        assertThat(result.isQueued()).isFalse();
        assertThat(result.isExternalPushSent()).isFalse();
        assertThat(result.isDeliveryAttempted()).isFalse();
        assertThat(result.isDeliveryEnabled()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }
}
