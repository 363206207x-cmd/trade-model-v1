package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineStatusEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("core-regression")
class NoOpOpportunityPushDeliveryPipelinePolicyTest {

    private final NoOpOpportunityPushDeliveryPipelinePolicy policy =
            new NoOpOpportunityPushDeliveryPipelinePolicy();

    @Test
    void nullInputFailsClosed() {
        OpportunityPushDeliveryPipelineResultDTO result = policy.evaluate(null, null);

        assertThat(result.getPipelineStatus())
                .isEqualTo(OpportunityPushDeliveryPipelineStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("AUDIT_QUEUE_RESULT_MISSING", "INCOMPLETE");
        assertReviewOnlyNoDeliveryPipelineDefaults(result);
    }

    @Test
    void blankSymbolFailsClosedIfSymbolIsSeparatelyProvided() {
        OpportunityPushDeliveryPipelineResultDTO result = policy.evaluate(" ", noopQueueResult());

        assertThat(result.getPipelineStatus())
                .isEqualTo(OpportunityPushDeliveryPipelineStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("QUEUE_BLOCKING_REASON", "SYMBOL_MISSING",
                "INCOMPLETE");
        assertReviewOnlyNoDeliveryPipelineDefaults(result);
    }

    @Test
    void missingQueueResultFailsClosed() {
        OpportunityPushDeliveryPipelineResultDTO result = policy.evaluate("BTCUSDT", null);

        assertThat(result.getPipelineStatus())
                .isEqualTo(OpportunityPushDeliveryPipelineStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("AUDIT_QUEUE_RESULT_MISSING", "INCOMPLETE");
        assertReviewOnlyNoDeliveryPipelineDefaults(result);
    }

    @Test
    void unsafeQueueResultFailsClosed() throws Exception {
        OpportunityPushDeliveryPipelineResultDTO result = policy.evaluate("BTCUSDT", unsafeQueueResult());

        assertThat(result.getPipelineStatus())
                .isEqualTo(OpportunityPushDeliveryPipelineStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("QUEUE_BLOCKING_REASON",
                "AUDIT_QUEUE_RESULT_UNSAFE", "INCOMPLETE");
        assertThat(result.getQueueReasons()).contains("QUEUE_NOOP_REVIEW_ONLY");
        assertReviewOnlyNoDeliveryPipelineDefaults(result);
    }

    @Test
    void nonNoopReviewOnlyQueueResultRemainsBlockedDisabledOrIncomplete() {
        List<OpportunityPushDeliveryPipelineResultDTO> results = List.of(
                policy.evaluate("BTCUSDT", blockedQueueResult()),
                policy.evaluate("BTCUSDT", disabledQueueResult()),
                policy.evaluate("BTCUSDT", incompleteQueueResult())
        );

        assertThat(results)
                .extracting(OpportunityPushDeliveryPipelineResultDTO::getPipelineStatus)
                .containsExactly(
                        OpportunityPushDeliveryPipelineStatusEnum.BLOCKED,
                        OpportunityPushDeliveryPipelineStatusEnum.DISABLED,
                        OpportunityPushDeliveryPipelineStatusEnum.INCOMPLETE
                );
        for (OpportunityPushDeliveryPipelineResultDTO result : results) {
            assertThat(result.getBlockingReasons()).contains("AUDIT_QUEUE_RESULT_NOT_NOOP_REVIEW_ONLY");
            assertThat(result.getPipelineStatus())
                    .isNotEqualTo(OpportunityPushDeliveryPipelineStatusEnum.DISABLED_NOOP);
            assertReviewOnlyNoDeliveryPipelineDefaults(result);
        }
    }

    @Test
    void safeNoopQueueResultCanProduceOnlyDisabledNoOpDeliveryPipelineResult() {
        OpportunityPushDeliveryPipelineResultDTO result = policy.evaluate(null, noopQueueResult());

        assertThat(result.getPipelineStatus())
                .isEqualTo(OpportunityPushDeliveryPipelineStatusEnum.DISABLED_NOOP);
        assertThat(result.getQueueStatus()).isEqualTo(OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY);
        assertThat(result.getPersistenceStatus())
                .isEqualTo(OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY);
        assertThat(result.getEnvelopeStatus()).isEqualTo(OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY);
        assertThat(result.getDeliveryDecisionStatus())
                .isEqualTo(OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getSource()).isEqualTo("unit-test");
        assertThat(result.getPipelineReasons()).contains(
                "DELIVERY_PIPELINE_DISABLED_NOOP",
                "DELIVERY_PIPELINE_DISABLED_BY_DEFAULT",
                "DELIVERY_PIPELINE_NO_EXTERNAL_PROVIDER",
                "DELIVERY_PIPELINE_NO_MESSAGE_OUTPUT"
        );
        assertThat(result.getQueueReasons()).contains("QUEUE_NOOP_REVIEW_ONLY");
        assertThat(result.getPersistenceReasons()).contains("PERSISTENCE_NOOP_REVIEW_ONLY");
        assertThat(result.getEnvelopeReasons()).contains("AUDIT_ONLY_ENVELOPE");
        assertThat(result.getDeliveryReasons()).contains("DELIVERY_REVIEW_ONLY");
        assertThat(result.getPushReasons()).contains("PUSH_REVIEW_ONLY");
        assertThat(result.getAttentionReasons()).contains("ATTENTION_REVIEW_ONLY");
        assertThat(result.getRiskGuardReasons()).contains("RISK_ACTION_GUARD_REVIEW_ONLY");
        assertThat(result.getBlockingReasons()).contains("QUEUE_BLOCKING_REASON");
        assertReviewOnlyNoDeliveryPipelineDefaults(result);
    }

    @Test
    void everyOutputKeepsReviewOnlyNoDeliveryPipelineDefaults() throws Exception {
        List<OpportunityPushDeliveryPipelineResultDTO> outputs = List.of(
                policy.evaluate(null, null),
                policy.evaluate(" ", noopQueueResult()),
                policy.evaluate("BTCUSDT", null),
                policy.evaluate("BTCUSDT", unsafeQueueResult()),
                policy.evaluate("BTCUSDT", blockedQueueResult()),
                policy.evaluate("BTCUSDT", disabledQueueResult()),
                policy.evaluate("BTCUSDT", incompleteQueueResult()),
                policy.evaluate("BTCUSDT", noopQueueResult())
        );

        for (OpportunityPushDeliveryPipelineResultDTO output : outputs) {
            assertReviewOnlyNoDeliveryPipelineDefaults(output);
        }
    }

    @Test
    void dtoDefensiveCopy() {
        List<String> pipelineReasons = new ArrayList<>(List.of("PIPELINE_REASON"));
        List<String> queueReasons = new ArrayList<>(List.of("QUEUE_REASON"));
        List<String> persistenceReasons = new ArrayList<>(List.of("PERSISTENCE_REASON"));
        List<String> envelopeReasons = new ArrayList<>(List.of("ENVELOPE_REASON"));
        List<String> deliveryReasons = new ArrayList<>(List.of("DELIVERY_REASON"));
        List<String> pushReasons = new ArrayList<>(List.of("PUSH_REASON"));
        List<String> attentionReasons = new ArrayList<>(List.of("ATTENTION_REASON"));
        List<String> riskGuardReasons = new ArrayList<>(List.of("RISK_REASON"));
        List<String> blockingReasons = new ArrayList<>(List.of("BLOCK_REASON"));

        OpportunityPushDeliveryPipelineResultDTO result =
                OpportunityPushDeliveryPipelineResultDTO.disabledNoop(
                        "BTCUSDT",
                        OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY,
                        OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY,
                        OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY,
                        OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                        "unit-test",
                        pipelineReasons,
                        queueReasons,
                        persistenceReasons,
                        envelopeReasons,
                        deliveryReasons,
                        pushReasons,
                        attentionReasons,
                        riskGuardReasons,
                        blockingReasons
                );

        pipelineReasons.add("MUTATED_PIPELINE");
        queueReasons.add("MUTATED_QUEUE");
        persistenceReasons.add("MUTATED_PERSISTENCE");
        envelopeReasons.add("MUTATED_ENVELOPE");
        deliveryReasons.add("MUTATED_DELIVERY");
        pushReasons.add("MUTATED_PUSH");
        attentionReasons.add("MUTATED_ATTENTION");
        riskGuardReasons.add("MUTATED_RISK");
        blockingReasons.add("MUTATED_BLOCK");
        result.getPipelineReasons().add("GETTER_MUTATION");
        result.getQueueReasons().add("GETTER_MUTATION");
        result.getPersistenceReasons().add("GETTER_MUTATION");
        result.getEnvelopeReasons().add("GETTER_MUTATION");
        result.getDeliveryReasons().add("GETTER_MUTATION");
        result.getPushReasons().add("GETTER_MUTATION");
        result.getAttentionReasons().add("GETTER_MUTATION");
        result.getRiskGuardReasons().add("GETTER_MUTATION");
        result.getBlockingReasons().add("GETTER_MUTATION");

        assertThat(result.getPipelineReasons()).containsExactly("PIPELINE_REASON");
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

        for (OpportunityPushDeliveryPipelineStatusEnum status
                : OpportunityPushDeliveryPipelineStatusEnum.values()) {
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

        for (Field field : NoOpOpportunityPushDeliveryPipelinePolicy.class.getDeclaredFields()) {
            for (String token : forbiddenDependencies) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(OpportunityPushDeliveryPipelinePolicy.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = OpportunityPushDeliveryPipelinePolicy.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("evaluate");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(OpportunityPushDeliveryPipelineResultDTO.class);

        for (Method method : NoOpOpportunityPushDeliveryPipelinePolicy.class.getDeclaredMethods()) {
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
            assertThat(methodName).doesNotContain("render");
            assertThat(methodName).doesNotContain("provider");
            for (String token : forbiddenDependencies) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("Scheduled");
            }
        }

        assertThat(NoOpOpportunityPushDeliveryPipelinePolicy.class.getDeclaredAnnotations()).isEmpty();
        assertThat(OpportunityPushDeliveryPipelinePolicy.class.getDeclaredAnnotations()).isEmpty();
    }

    private static OpportunityPushAuditQueueResultDTO noopQueueResult() {
        return OpportunityPushAuditQueueResultDTO.noopReviewOnly(
                "BTCUSDT",
                OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY,
                OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY,
                OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                "unit-test",
                List.of("QUEUE_NOOP_REVIEW_ONLY"),
                List.of("PERSISTENCE_NOOP_REVIEW_ONLY"),
                List.of("AUDIT_ONLY_ENVELOPE"),
                List.of("DELIVERY_REVIEW_ONLY"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("QUEUE_BLOCKING_REASON")
        );
    }

    private static OpportunityPushAuditQueueResultDTO blockedQueueResult() {
        return OpportunityPushAuditQueueResultDTO.blocked(
                "BTCUSDT",
                OpportunityPushAuditPersistenceStatusEnum.BLOCKED,
                OpportunityPushAuditEnvelopeStatusEnum.BLOCKED,
                OpportunityPushDeliveryDecisionStatusEnum.BLOCKED,
                "unit-test",
                List.of("PERSISTENCE_BLOCKED"),
                List.of("AUDIT_BLOCKED"),
                List.of("DELIVERY_BLOCKED"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_BLOCKED"),
                List.of("QUEUE_BLOCKED_REASON")
        );
    }

    private static OpportunityPushAuditQueueResultDTO disabledQueueResult() {
        return OpportunityPushAuditQueueResultDTO.disabled(
                "BTCUSDT",
                OpportunityPushAuditPersistenceStatusEnum.DISABLED,
                OpportunityPushAuditEnvelopeStatusEnum.DISABLED,
                OpportunityPushDeliveryDecisionStatusEnum.DISABLED,
                "unit-test",
                List.of("PERSISTENCE_DISABLED"),
                List.of("AUDIT_DISABLED"),
                List.of("DELIVERY_DISABLED"),
                List.of("PUSH_DISABLED"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("QUEUE_DISABLED_REASON")
        );
    }

    private static OpportunityPushAuditQueueResultDTO incompleteQueueResult() {
        return OpportunityPushAuditQueueResultDTO.incomplete(
                "BTCUSDT",
                OpportunityPushAuditPersistenceStatusEnum.INCOMPLETE,
                OpportunityPushAuditEnvelopeStatusEnum.INCOMPLETE,
                OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE,
                "unit-test",
                List.of("PERSISTENCE_INCOMPLETE"),
                List.of("AUDIT_INCOMPLETE"),
                List.of("DELIVERY_INCOMPLETE"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("QUEUE_INCOMPLETE_REASON")
        );
    }

    private static OpportunityPushAuditQueueResultDTO unsafeQueueResult() throws Exception {
        OpportunityPushAuditQueueResultDTO result = noopQueueResult();
        Field field = OpportunityPushAuditQueueResultDTO.class.getDeclaredField("queued");
        field.setAccessible(true);
        field.setBoolean(result, true);
        return result;
    }

    private static void assertReviewOnlyNoDeliveryPipelineDefaults(
            OpportunityPushDeliveryPipelineResultDTO result
    ) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isAuditOnly()).isTrue();
        assertThat(result.isDeliveryPipelineEnabled()).isFalse();
        assertThat(result.isPipelineStarted()).isFalse();
        assertThat(result.isProviderSelected()).isFalse();
        assertThat(result.isMessageRendered()).isFalse();
        assertThat(result.isMessageSent()).isFalse();
        assertThat(result.isExternalPushSent()).isFalse();
        assertThat(result.isDeliveryAttempted()).isFalse();
        assertThat(result.isDeliveryEnabled()).isFalse();
        assertThat(result.isQueueCreated()).isFalse();
        assertThat(result.isQueued()).isFalse();
        assertThat(result.isEnqueueAttempted()).isFalse();
        assertThat(result.isDequeueAttempted()).isFalse();
        assertThat(result.isWorkerStarted()).isFalse();
        assertThat(result.isPersisted()).isFalse();
        assertThat(result.isPersistenceAttempted()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }
}
