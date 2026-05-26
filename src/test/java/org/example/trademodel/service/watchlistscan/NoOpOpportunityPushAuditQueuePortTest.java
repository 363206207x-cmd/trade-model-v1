package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionStatusEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("core-regression")
class NoOpOpportunityPushAuditQueuePortTest {

    private final NoOpOpportunityPushAuditQueuePort port = new NoOpOpportunityPushAuditQueuePort();

    @Test
    void nullInputFailsClosed() {
        OpportunityPushAuditQueueResultDTO result = port.evaluate(null, null);

        assertThat(result.getQueueStatus()).isEqualTo(OpportunityPushAuditQueueStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("PERSISTENCE_RESULT_MISSING", "INCOMPLETE");
        assertReviewOnlyNoQueueDefaults(result);
    }

    @Test
    void blankSymbolFailsClosedIfSymbolIsSeparatelyProvided() {
        OpportunityPushAuditQueueResultDTO result = port.evaluate(" ", noopPersistenceResult());

        assertThat(result.getQueueStatus()).isEqualTo(OpportunityPushAuditQueueStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("PERSISTENCE_BLOCKING_REASON", "SYMBOL_MISSING",
                "INCOMPLETE");
        assertReviewOnlyNoQueueDefaults(result);
    }

    @Test
    void missingPersistenceResultFailsClosed() {
        OpportunityPushAuditQueueResultDTO result = port.evaluate("BTCUSDT", null);

        assertThat(result.getQueueStatus()).isEqualTo(OpportunityPushAuditQueueStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("PERSISTENCE_RESULT_MISSING", "INCOMPLETE");
        assertReviewOnlyNoQueueDefaults(result);
    }

    @Test
    void unsafePersistenceResultFailsClosed() throws Exception {
        OpportunityPushAuditQueueResultDTO result = port.evaluate("BTCUSDT", unsafePersistenceResult());

        assertThat(result.getQueueStatus()).isEqualTo(OpportunityPushAuditQueueStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("PERSISTENCE_BLOCKING_REASON",
                "PERSISTENCE_RESULT_UNSAFE", "INCOMPLETE");
        assertThat(result.getPersistenceReasons()).contains("PERSISTENCE_NOOP_REVIEW_ONLY");
        assertReviewOnlyNoQueueDefaults(result);
    }

    @Test
    void nonNoopReviewOnlyPersistenceResultRemainsBlockedDisabledOrIncomplete() {
        List<OpportunityPushAuditQueueResultDTO> results = List.of(
                port.evaluate("BTCUSDT", blockedPersistenceResult()),
                port.evaluate("BTCUSDT", disabledPersistenceResult()),
                port.evaluate("BTCUSDT", incompletePersistenceResult())
        );

        assertThat(results)
                .extracting(OpportunityPushAuditQueueResultDTO::getQueueStatus)
                .containsExactly(
                        OpportunityPushAuditQueueStatusEnum.BLOCKED,
                        OpportunityPushAuditQueueStatusEnum.DISABLED,
                        OpportunityPushAuditQueueStatusEnum.INCOMPLETE
                );
        for (OpportunityPushAuditQueueResultDTO result : results) {
            assertThat(result.getBlockingReasons()).contains("PERSISTENCE_RESULT_NOT_NOOP_REVIEW_ONLY");
            assertThat(result.getQueueStatus()).isNotEqualTo(OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY);
            assertReviewOnlyNoQueueDefaults(result);
        }
    }

    @Test
    void safeNoopPersistenceResultCanProduceOnlyNoOpQueueResult() {
        OpportunityPushAuditQueueResultDTO result = port.evaluate(null, noopPersistenceResult());

        assertThat(result.getQueueStatus()).isEqualTo(OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY);
        assertThat(result.getPersistenceStatus())
                .isEqualTo(OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY);
        assertThat(result.getEnvelopeStatus()).isEqualTo(OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY);
        assertThat(result.getDeliveryDecisionStatus())
                .isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getSource()).isEqualTo("unit-test");
        assertThat(result.getQueueReasons()).contains(
                "AUDIT_QUEUE_NOOP_REVIEW_ONLY",
                "AUDIT_QUEUE_DISABLED_BY_DEFAULT"
        );
        assertThat(result.getPersistenceReasons()).contains("PERSISTENCE_NOOP_REVIEW_ONLY");
        assertThat(result.getEnvelopeReasons()).contains("AUDIT_ONLY_ENVELOPE");
        assertThat(result.getDeliveryReasons()).contains("DELIVERY_REVIEW_ONLY");
        assertThat(result.getPushReasons()).contains("PUSH_REVIEW_ONLY");
        assertThat(result.getAttentionReasons()).contains("ATTENTION_REVIEW_ONLY");
        assertThat(result.getRiskGuardReasons()).contains("RISK_ACTION_GUARD_REVIEW_ONLY");
        assertThat(result.getBlockingReasons()).contains("PERSISTENCE_BLOCKING_REASON");
        assertReviewOnlyNoQueueDefaults(result);
    }

    @Test
    void everyOutputKeepsReviewOnlyNoQueueDefaults() throws Exception {
        List<OpportunityPushAuditQueueResultDTO> outputs = List.of(
                port.evaluate(null, null),
                port.evaluate(" ", noopPersistenceResult()),
                port.evaluate("BTCUSDT", null),
                port.evaluate("BTCUSDT", unsafePersistenceResult()),
                port.evaluate("BTCUSDT", blockedPersistenceResult()),
                port.evaluate("BTCUSDT", disabledPersistenceResult()),
                port.evaluate("BTCUSDT", incompletePersistenceResult()),
                port.evaluate("BTCUSDT", noopPersistenceResult())
        );

        for (OpportunityPushAuditQueueResultDTO output : outputs) {
            assertReviewOnlyNoQueueDefaults(output);
        }
    }

    @Test
    void dtoDefensiveCopy() {
        List<String> queueReasons = new ArrayList<>(List.of("QUEUE_REASON"));
        List<String> persistenceReasons = new ArrayList<>(List.of("PERSISTENCE_REASON"));
        List<String> envelopeReasons = new ArrayList<>(List.of("ENVELOPE_REASON"));
        List<String> deliveryReasons = new ArrayList<>(List.of("DELIVERY_REASON"));
        List<String> pushReasons = new ArrayList<>(List.of("PUSH_REASON"));
        List<String> attentionReasons = new ArrayList<>(List.of("ATTENTION_REASON"));
        List<String> riskGuardReasons = new ArrayList<>(List.of("RISK_REASON"));
        List<String> blockingReasons = new ArrayList<>(List.of("BLOCK_REASON"));

        OpportunityPushAuditQueueResultDTO result = OpportunityPushAuditQueueResultDTO.noopReviewOnly(
                "BTCUSDT",
                OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY,
                OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY,
                OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                "unit-test",
                queueReasons,
                persistenceReasons,
                envelopeReasons,
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                blockingReasons
        );

        queueReasons.add("MUTATED_QUEUE");
        persistenceReasons.add("MUTATED_PERSISTENCE");
        envelopeReasons.add("MUTATED_ENVELOPE");
        deliveryReasons.add("MUTATED_DELIVERY");
        pushReasons.add("MUTATED_PUSH");
        attentionReasons.add("MUTATED_ATTENTION");
        riskGuardReasons.add("MUTATED_RISK");
        blockingReasons.add("MUTATED_BLOCK");
        result.getQueueReasons().add("GETTER_MUTATION");
        result.getPersistenceReasons().add("GETTER_MUTATION");
        result.getEnvelopeReasons().add("GETTER_MUTATION");
        result.getDeliveryReasons().add("GETTER_MUTATION");
        result.getPushReasons().add("GETTER_MUTATION");
        result.getAttentionReasons().add("GETTER_MUTATION");
        result.getRiskGuardReasons().add("GETTER_MUTATION");
        result.getBlockingReasons().add("GETTER_MUTATION");

        assertThat(result.getQueueReasons()).containsExactly("QUEUE_REASON");
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

        for (OpportunityPushAuditQueueStatusEnum status : OpportunityPushAuditQueueStatusEnum.values()) {
            assertThat(status.name()).isNotIn(forbidden);
        }
    }

    @Test
    void implementationHasNoForbiddenDependenciesOrMethodSurface() {
        List<String> forbiddenDependencies = List.of(
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

        for (Field field : NoOpOpportunityPushAuditQueuePort.class.getDeclaredFields()) {
            for (String token : forbiddenDependencies) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(OpportunityPushAuditQueuePort.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = OpportunityPushAuditQueuePort.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("evaluate");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(OpportunityPushAuditQueueResultDTO.class);

        for (Method method : NoOpOpportunityPushAuditQueuePort.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            assertThat(methodName).doesNotContain("send");
            assertThat(methodName).doesNotContain("notify");
            assertThat(methodName).doesNotContain("delivernow");
            assertThat(methodName).doesNotContain("enqueuenow");
            assertThat(methodName).doesNotContain("dequeue");
            assertThat(methodName).doesNotContain("worker");
            assertThat(methodName).doesNotContain("schedule");
            assertThat(methodName).doesNotContain("persistnow");
            assertThat(methodName).doesNotContain("save");
            assertThat(methodName).doesNotContain("insert");
            assertThat(methodName).doesNotContain("update");
            assertThat(methodName).doesNotContain("execute");
            assertThat(methodName).doesNotContain("trade");
            assertThat(methodName).doesNotContain("order");
            for (String token : forbiddenDependencies) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("Scheduled");
            }
        }

        assertThat(NoOpOpportunityPushAuditQueuePort.class.getDeclaredAnnotations()).isEmpty();
        assertThat(OpportunityPushAuditQueuePort.class.getDeclaredAnnotations()).isEmpty();
    }

    private static OpportunityPushAuditPersistenceResultDTO noopPersistenceResult() {
        return OpportunityPushAuditPersistenceResultDTO.noopReviewOnly(
                "BTCUSDT",
                OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY,
                OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                "unit-test",
                List.of("PERSISTENCE_NOOP_REVIEW_ONLY"),
                List.of("AUDIT_ONLY_ENVELOPE"),
                List.of("DELIVERY_REVIEW_ONLY"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PERSISTENCE_BLOCKING_REASON")
        );
    }

    private static OpportunityPushAuditPersistenceResultDTO blockedPersistenceResult() {
        return OpportunityPushAuditPersistenceResultDTO.blocked(
                "BTCUSDT",
                OpportunityPushAuditEnvelopeStatusEnum.BLOCKED,
                OpportunityPushDeliveryDecisionStatusEnum.BLOCKED,
                "unit-test",
                List.of("AUDIT_BLOCKED"),
                List.of("DELIVERY_BLOCKED"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_BLOCKED"),
                List.of("PERSISTENCE_BLOCKED_REASON")
        );
    }

    private static OpportunityPushAuditPersistenceResultDTO disabledPersistenceResult() {
        return OpportunityPushAuditPersistenceResultDTO.disabled(
                "BTCUSDT",
                OpportunityPushAuditEnvelopeStatusEnum.DISABLED,
                OpportunityPushDeliveryDecisionStatusEnum.DISABLED,
                "unit-test",
                List.of("AUDIT_DISABLED"),
                List.of("DELIVERY_DISABLED"),
                List.of("PUSH_DISABLED"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PERSISTENCE_DISABLED_REASON")
        );
    }

    private static OpportunityPushAuditPersistenceResultDTO incompletePersistenceResult() {
        return OpportunityPushAuditPersistenceResultDTO.incomplete(
                "BTCUSDT",
                OpportunityPushAuditEnvelopeStatusEnum.INCOMPLETE,
                OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE,
                "unit-test",
                List.of("AUDIT_INCOMPLETE"),
                List.of("DELIVERY_INCOMPLETE"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PERSISTENCE_INCOMPLETE_REASON")
        );
    }

    private static OpportunityPushAuditPersistenceResultDTO unsafePersistenceResult() throws Exception {
        OpportunityPushAuditPersistenceResultDTO result = noopPersistenceResult();
        Field field = OpportunityPushAuditPersistenceResultDTO.class.getDeclaredField("queued");
        field.setAccessible(true);
        field.setBoolean(result, true);
        return result;
    }

    private static void assertReviewOnlyNoQueueDefaults(OpportunityPushAuditQueueResultDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isAuditOnly()).isTrue();
        assertThat(result.isQueueCreated()).isFalse();
        assertThat(result.isQueued()).isFalse();
        assertThat(result.isEnqueueAttempted()).isFalse();
        assertThat(result.isDequeueAttempted()).isFalse();
        assertThat(result.isWorkerStarted()).isFalse();
        assertThat(result.isPersisted()).isFalse();
        assertThat(result.isPersistenceAttempted()).isFalse();
        assertThat(result.isExternalPushSent()).isFalse();
        assertThat(result.isDeliveryAttempted()).isFalse();
        assertThat(result.isDeliveryEnabled()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }
}
