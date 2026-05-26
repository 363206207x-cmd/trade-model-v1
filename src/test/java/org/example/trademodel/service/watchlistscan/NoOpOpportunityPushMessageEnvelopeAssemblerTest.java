package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushMessageEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushMessageEnvelopeStatusEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("core-regression")
class NoOpOpportunityPushMessageEnvelopeAssemblerTest {

    private final NoOpOpportunityPushMessageEnvelopeAssembler assembler =
            new NoOpOpportunityPushMessageEnvelopeAssembler();

    @Test
    void nullInputFailsClosed() {
        OpportunityPushMessageEnvelopeDTO result = assembler.evaluate(null, null);

        assertThat(result.getMessageEnvelopeStatus())
                .isEqualTo(OpportunityPushMessageEnvelopeStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons())
                .contains("DELIVERY_PIPELINE_RESULT_MISSING", "INCOMPLETE");
        assertReviewOnlyNoMessageEnvelopeDefaults(result);
    }

    @Test
    void blankSymbolFailsClosedIfSymbolIsSeparatelyProvided() {
        OpportunityPushMessageEnvelopeDTO result = assembler.evaluate(" ", disabledNoopPipelineResult());

        assertThat(result.getMessageEnvelopeStatus())
                .isEqualTo(OpportunityPushMessageEnvelopeStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains(
                "PIPELINE_BLOCKING_REASON",
                "SYMBOL_MISSING",
                "INCOMPLETE"
        );
        assertReviewOnlyNoMessageEnvelopeDefaults(result);
    }

    @Test
    void missingPipelineResultFailsClosed() {
        OpportunityPushMessageEnvelopeDTO result = assembler.evaluate("BTCUSDT", null);

        assertThat(result.getMessageEnvelopeStatus())
                .isEqualTo(OpportunityPushMessageEnvelopeStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons())
                .contains("DELIVERY_PIPELINE_RESULT_MISSING", "INCOMPLETE");
        assertReviewOnlyNoMessageEnvelopeDefaults(result);
    }

    @Test
    void unsafePipelineResultFailsClosed() throws Exception {
        OpportunityPushMessageEnvelopeDTO result = assembler.evaluate("BTCUSDT", unsafePipelineResult());

        assertThat(result.getMessageEnvelopeStatus())
                .isEqualTo(OpportunityPushMessageEnvelopeStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains(
                "PIPELINE_BLOCKING_REASON",
                "DELIVERY_PIPELINE_RESULT_UNSAFE",
                "INCOMPLETE"
        );
        assertThat(result.getPipelineReasons()).contains("PIPELINE_DISABLED_NOOP");
        assertReviewOnlyNoMessageEnvelopeDefaults(result);
    }

    @Test
    void nonDisabledNoopPipelineResultRemainsBlockedDisabledOrIncomplete() {
        List<OpportunityPushMessageEnvelopeDTO> results = List.of(
                assembler.evaluate("BTCUSDT", blockedPipelineResult()),
                assembler.evaluate("BTCUSDT", disabledPipelineResult()),
                assembler.evaluate("BTCUSDT", incompletePipelineResult())
        );

        assertThat(results)
                .extracting(OpportunityPushMessageEnvelopeDTO::getMessageEnvelopeStatus)
                .containsExactly(
                        OpportunityPushMessageEnvelopeStatusEnum.BLOCKED,
                        OpportunityPushMessageEnvelopeStatusEnum.DISABLED,
                        OpportunityPushMessageEnvelopeStatusEnum.INCOMPLETE
                );
        for (OpportunityPushMessageEnvelopeDTO result : results) {
            assertThat(result.getBlockingReasons())
                    .contains("DELIVERY_PIPELINE_RESULT_NOT_DISABLED_NOOP");
            assertThat(result.getMessageEnvelopeStatus())
                    .isNotEqualTo(OpportunityPushMessageEnvelopeStatusEnum.DISABLED_NOOP);
            assertReviewOnlyNoMessageEnvelopeDefaults(result);
        }
    }

    @Test
    void safeDisabledNoopPipelineResultCanProduceOnlyDisabledNoOpMessageEnvelopeResult() {
        OpportunityPushMessageEnvelopeDTO result = assembler.evaluate(null, disabledNoopPipelineResult());

        assertThat(result.getMessageEnvelopeStatus())
                .isEqualTo(OpportunityPushMessageEnvelopeStatusEnum.DISABLED_NOOP);
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
        assertThat(result.getMessageEnvelopeReasons()).contains(
                "MESSAGE_ENVELOPE_DISABLED_NOOP",
                "MESSAGE_ENVELOPE_DISABLED_BY_DEFAULT",
                "MESSAGE_ENVELOPE_NO_FINAL_TEXT",
                "MESSAGE_ENVELOPE_NO_SEND_OUTPUT",
                "MESSAGE_ENVELOPE_NO_PROVIDER"
        );
        assertThat(result.getPipelineReasons()).contains("PIPELINE_DISABLED_NOOP");
        assertThat(result.getQueueReasons()).contains("QUEUE_NOOP_REVIEW_ONLY");
        assertThat(result.getPersistenceReasons()).contains("PERSISTENCE_NOOP_REVIEW_ONLY");
        assertThat(result.getEnvelopeReasons()).contains("AUDIT_ONLY_ENVELOPE");
        assertThat(result.getDeliveryReasons()).contains("DELIVERY_REVIEW_ONLY");
        assertThat(result.getPushReasons()).contains("PUSH_REVIEW_ONLY");
        assertThat(result.getAttentionReasons()).contains("ATTENTION_REVIEW_ONLY");
        assertThat(result.getRiskGuardReasons()).contains("RISK_ACTION_GUARD_REVIEW_ONLY");
        assertThat(result.getBlockingReasons()).contains("PIPELINE_BLOCKING_REASON");
        assertReviewOnlyNoMessageEnvelopeDefaults(result);
    }

    @Test
    void everyOutputKeepsReviewOnlyNoMessageEnvelopeDefaults() throws Exception {
        List<OpportunityPushMessageEnvelopeDTO> outputs = List.of(
                assembler.evaluate(null, null),
                assembler.evaluate(" ", disabledNoopPipelineResult()),
                assembler.evaluate("BTCUSDT", null),
                assembler.evaluate("BTCUSDT", unsafePipelineResult()),
                assembler.evaluate("BTCUSDT", blockedPipelineResult()),
                assembler.evaluate("BTCUSDT", disabledPipelineResult()),
                assembler.evaluate("BTCUSDT", incompletePipelineResult()),
                assembler.evaluate("BTCUSDT", disabledNoopPipelineResult())
        );

        for (OpportunityPushMessageEnvelopeDTO output : outputs) {
            assertReviewOnlyNoMessageEnvelopeDefaults(output);
        }
    }

    @Test
    void dtoDefensiveCopy() {
        List<String> messageEnvelopeReasons = new ArrayList<>(List.of("MESSAGE_ENVELOPE_REASON"));
        List<String> pipelineReasons = new ArrayList<>(List.of("PIPELINE_REASON"));
        List<String> queueReasons = new ArrayList<>(List.of("QUEUE_REASON"));
        List<String> persistenceReasons = new ArrayList<>(List.of("PERSISTENCE_REASON"));
        List<String> envelopeReasons = new ArrayList<>(List.of("ENVELOPE_REASON"));
        List<String> deliveryReasons = new ArrayList<>(List.of("DELIVERY_REASON"));
        List<String> pushReasons = new ArrayList<>(List.of("PUSH_REASON"));
        List<String> attentionReasons = new ArrayList<>(List.of("ATTENTION_REASON"));
        List<String> riskGuardReasons = new ArrayList<>(List.of("RISK_REASON"));
        List<String> blockingReasons = new ArrayList<>(List.of("BLOCK_REASON"));

        OpportunityPushMessageEnvelopeDTO result =
                OpportunityPushMessageEnvelopeDTO.disabledNoop(
                        "BTCUSDT",
                        OpportunityPushDeliveryPipelineStatusEnum.DISABLED_NOOP,
                        OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY,
                        OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY,
                        OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY,
                        OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                        "unit-test",
                        messageEnvelopeReasons,
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

        messageEnvelopeReasons.add("MUTATED_MESSAGE_ENVELOPE");
        pipelineReasons.add("MUTATED_PIPELINE");
        queueReasons.add("MUTATED_QUEUE");
        persistenceReasons.add("MUTATED_PERSISTENCE");
        envelopeReasons.add("MUTATED_ENVELOPE");
        deliveryReasons.add("MUTATED_DELIVERY");
        pushReasons.add("MUTATED_PUSH");
        attentionReasons.add("MUTATED_ATTENTION");
        riskGuardReasons.add("MUTATED_RISK");
        blockingReasons.add("MUTATED_BLOCK");
        result.getMessageEnvelopeReasons().add("GETTER_MUTATION");
        result.getPipelineReasons().add("GETTER_MUTATION");
        result.getQueueReasons().add("GETTER_MUTATION");
        result.getPersistenceReasons().add("GETTER_MUTATION");
        result.getEnvelopeReasons().add("GETTER_MUTATION");
        result.getDeliveryReasons().add("GETTER_MUTATION");
        result.getPushReasons().add("GETTER_MUTATION");
        result.getAttentionReasons().add("GETTER_MUTATION");
        result.getRiskGuardReasons().add("GETTER_MUTATION");
        result.getBlockingReasons().add("GETTER_MUTATION");

        assertThat(result.getMessageEnvelopeReasons()).containsExactly("MESSAGE_ENVELOPE_REASON");
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

        for (OpportunityPushMessageEnvelopeStatusEnum status
                : OpportunityPushMessageEnvelopeStatusEnum.values()) {
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

        for (Field field : NoOpOpportunityPushMessageEnvelopeAssembler.class.getDeclaredFields()) {
            for (String token : forbiddenDependencies) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(OpportunityPushMessageEnvelopeAssembler.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = OpportunityPushMessageEnvelopeAssembler.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("evaluate");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(OpportunityPushMessageEnvelopeDTO.class);

        for (Method method : NoOpOpportunityPushMessageEnvelopeAssembler.class.getDeclaredMethods()) {
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

        assertThat(NoOpOpportunityPushMessageEnvelopeAssembler.class.getDeclaredAnnotations()).isEmpty();
        assertThat(OpportunityPushMessageEnvelopeAssembler.class.getDeclaredAnnotations()).isEmpty();
    }

    private static OpportunityPushDeliveryPipelineResultDTO disabledNoopPipelineResult() {
        return OpportunityPushDeliveryPipelineResultDTO.disabledNoop(
                "BTCUSDT",
                OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY,
                OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY,
                OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY,
                OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                "unit-test",
                List.of("PIPELINE_DISABLED_NOOP"),
                List.of("QUEUE_NOOP_REVIEW_ONLY"),
                List.of("PERSISTENCE_NOOP_REVIEW_ONLY"),
                List.of("AUDIT_ONLY_ENVELOPE"),
                List.of("DELIVERY_REVIEW_ONLY"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PIPELINE_BLOCKING_REASON")
        );
    }

    private static OpportunityPushDeliveryPipelineResultDTO blockedPipelineResult() {
        return OpportunityPushDeliveryPipelineResultDTO.blocked(
                "BTCUSDT",
                OpportunityPushAuditQueueStatusEnum.BLOCKED,
                OpportunityPushAuditPersistenceStatusEnum.BLOCKED,
                OpportunityPushAuditEnvelopeStatusEnum.BLOCKED,
                OpportunityPushDeliveryDecisionStatusEnum.BLOCKED,
                "unit-test",
                List.of("QUEUE_BLOCKED"),
                List.of("PERSISTENCE_BLOCKED"),
                List.of("AUDIT_BLOCKED"),
                List.of("DELIVERY_BLOCKED"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_BLOCKED"),
                List.of("PIPELINE_BLOCKED_REASON")
        );
    }

    private static OpportunityPushDeliveryPipelineResultDTO disabledPipelineResult() {
        return OpportunityPushDeliveryPipelineResultDTO.disabled(
                "BTCUSDT",
                OpportunityPushAuditQueueStatusEnum.DISABLED,
                OpportunityPushAuditPersistenceStatusEnum.DISABLED,
                OpportunityPushAuditEnvelopeStatusEnum.DISABLED,
                OpportunityPushDeliveryDecisionStatusEnum.DISABLED,
                "unit-test",
                List.of("QUEUE_DISABLED"),
                List.of("PERSISTENCE_DISABLED"),
                List.of("AUDIT_DISABLED"),
                List.of("DELIVERY_DISABLED"),
                List.of("PUSH_DISABLED"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PIPELINE_DISABLED_REASON")
        );
    }

    private static OpportunityPushDeliveryPipelineResultDTO incompletePipelineResult() {
        return OpportunityPushDeliveryPipelineResultDTO.incomplete(
                "BTCUSDT",
                OpportunityPushAuditQueueStatusEnum.INCOMPLETE,
                OpportunityPushAuditPersistenceStatusEnum.INCOMPLETE,
                OpportunityPushAuditEnvelopeStatusEnum.INCOMPLETE,
                OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE,
                "unit-test",
                List.of("QUEUE_INCOMPLETE"),
                List.of("PERSISTENCE_INCOMPLETE"),
                List.of("AUDIT_INCOMPLETE"),
                List.of("DELIVERY_INCOMPLETE"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PIPELINE_INCOMPLETE_REASON")
        );
    }

    private static OpportunityPushDeliveryPipelineResultDTO unsafePipelineResult() throws Exception {
        OpportunityPushDeliveryPipelineResultDTO result = disabledNoopPipelineResult();
        Field field = OpportunityPushDeliveryPipelineResultDTO.class.getDeclaredField("messageSent");
        field.setAccessible(true);
        field.setBoolean(result, true);
        return result;
    }

    private static void assertReviewOnlyNoMessageEnvelopeDefaults(
            OpportunityPushMessageEnvelopeDTO result
    ) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isAuditOnly()).isTrue();
        assertThat(result.isMessageEnvelopeCreated()).isFalse();
        assertThat(result.isMessageRenderable()).isFalse();
        assertThat(result.isMessageRendered()).isFalse();
        assertThat(result.isMessageSendable()).isFalse();
        assertThat(result.isMessageSent()).isFalse();
        assertThat(result.isProviderSelected()).isFalse();
        assertThat(result.isExternalPushSent()).isFalse();
        assertThat(result.isDeliveryAttempted()).isFalse();
        assertThat(result.isDeliveryEnabled()).isFalse();
        assertThat(result.isDeliveryPipelineEnabled()).isFalse();
        assertThat(result.isPipelineStarted()).isFalse();
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
