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
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushExternalChannelDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushExternalChannelStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushMessageEnvelopeStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushProviderChannelDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushProviderChannelStatusEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("core-regression")
class NoOpOpportunityPushExternalChannelPolicyTest {

    private final NoOpOpportunityPushExternalChannelPolicy policy =
            new NoOpOpportunityPushExternalChannelPolicy();

    @Test
    void nullInputFailsClosed() {
        OpportunityPushExternalChannelDTO result = policy.evaluate(null, null);

        assertThat(result.getExternalChannelStatus())
                .isEqualTo(OpportunityPushExternalChannelStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons())
                .contains("PROVIDER_CHANNEL_MISSING", "INCOMPLETE");
        assertReviewOnlyNoExternalChannelDefaults(result);
    }

    @Test
    void blankSymbolFailsClosedIfSymbolIsSeparatelyProvided() {
        OpportunityPushExternalChannelDTO result = policy.evaluate(" ", disabledNoopProviderChannel());

        assertThat(result.getExternalChannelStatus())
                .isEqualTo(OpportunityPushExternalChannelStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains(
                "PROVIDER_CHANNEL_BLOCKING_REASON",
                "SYMBOL_MISSING",
                "INCOMPLETE"
        );
        assertReviewOnlyNoExternalChannelDefaults(result);
    }

    @Test
    void missingProviderChannelFailsClosed() {
        OpportunityPushExternalChannelDTO result = policy.evaluate("BTCUSDT", null);

        assertThat(result.getExternalChannelStatus())
                .isEqualTo(OpportunityPushExternalChannelStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons())
                .contains("PROVIDER_CHANNEL_MISSING", "INCOMPLETE");
        assertReviewOnlyNoExternalChannelDefaults(result);
    }

    @Test
    void unsafeProviderChannelFailsClosed() throws Exception {
        OpportunityPushExternalChannelDTO result = policy.evaluate("BTCUSDT", unsafeProviderChannel());

        assertThat(result.getExternalChannelStatus())
                .isEqualTo(OpportunityPushExternalChannelStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains(
                "PROVIDER_CHANNEL_BLOCKING_REASON",
                "PROVIDER_CHANNEL_UNSAFE",
                "INCOMPLETE"
        );
        assertThat(result.getProviderChannelReasons()).contains("PROVIDER_CHANNEL_DISABLED_NOOP");
        assertReviewOnlyNoExternalChannelDefaults(result);
    }

    @Test
    void nonDisabledNoopProviderChannelRemainsBlockedDisabledOrIncomplete() {
        List<OpportunityPushExternalChannelDTO> results = List.of(
                policy.evaluate("BTCUSDT", blockedProviderChannel()),
                policy.evaluate("BTCUSDT", disabledProviderChannel()),
                policy.evaluate("BTCUSDT", incompleteProviderChannel())
        );

        assertThat(results)
                .extracting(OpportunityPushExternalChannelDTO::getExternalChannelStatus)
                .containsExactly(
                        OpportunityPushExternalChannelStatusEnum.BLOCKED,
                        OpportunityPushExternalChannelStatusEnum.DISABLED,
                        OpportunityPushExternalChannelStatusEnum.INCOMPLETE
                );
        for (OpportunityPushExternalChannelDTO result : results) {
            assertThat(result.getBlockingReasons())
                    .contains("PROVIDER_CHANNEL_NOT_DISABLED_NOOP");
            assertThat(result.getExternalChannelStatus())
                    .isNotEqualTo(OpportunityPushExternalChannelStatusEnum.DISABLED_NOOP);
            assertReviewOnlyNoExternalChannelDefaults(result);
        }
    }

    @Test
    void safeDisabledNoopProviderChannelCanProduceOnlyDisabledNoOpExternalChannelResult() {
        OpportunityPushExternalChannelDTO result = policy.evaluate(null, disabledNoopProviderChannel());

        assertThat(result.getExternalChannelStatus())
                .isEqualTo(OpportunityPushExternalChannelStatusEnum.DISABLED_NOOP);
        assertThat(result.getProviderChannelStatus())
                .isEqualTo(OpportunityPushProviderChannelStatusEnum.DISABLED_NOOP);
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
        assertThat(result.getExternalChannelReasons()).contains(
                "EXTERNAL_CHANNEL_DISABLED_NOOP",
                "EXTERNAL_CHANNEL_DISABLED_BY_DEFAULT",
                "EXTERNAL_CHANNEL_NO_INTEGRATION",
                "EXTERNAL_CHANNEL_NO_CREDENTIAL",
                "EXTERNAL_CHANNEL_NO_LIVE_CALL",
                "EXTERNAL_CHANNEL_NO_MESSAGE_OUTPUT"
        );
        assertThat(result.getProviderChannelReasons()).contains("PROVIDER_CHANNEL_DISABLED_NOOP");
        assertThat(result.getMessageEnvelopeReasons()).contains("MESSAGE_ENVELOPE_DISABLED_NOOP");
        assertThat(result.getPipelineReasons()).contains("PIPELINE_DISABLED_NOOP");
        assertThat(result.getQueueReasons()).contains("QUEUE_NOOP_REVIEW_ONLY");
        assertThat(result.getPersistenceReasons()).contains("PERSISTENCE_NOOP_REVIEW_ONLY");
        assertThat(result.getEnvelopeReasons()).contains("AUDIT_ONLY_ENVELOPE");
        assertThat(result.getDeliveryReasons()).contains("DELIVERY_REVIEW_ONLY");
        assertThat(result.getPushReasons()).contains("PUSH_REVIEW_ONLY");
        assertThat(result.getAttentionReasons()).contains("ATTENTION_REVIEW_ONLY");
        assertThat(result.getRiskGuardReasons()).contains("RISK_ACTION_GUARD_REVIEW_ONLY");
        assertThat(result.getBlockingReasons()).contains("PROVIDER_CHANNEL_BLOCKING_REASON");
        assertReviewOnlyNoExternalChannelDefaults(result);
    }

    @Test
    void everyOutputKeepsReviewOnlyNoExternalChannelDefaults() throws Exception {
        List<OpportunityPushExternalChannelDTO> outputs = List.of(
                policy.evaluate(null, null),
                policy.evaluate(" ", disabledNoopProviderChannel()),
                policy.evaluate("BTCUSDT", null),
                policy.evaluate("BTCUSDT", unsafeProviderChannel()),
                policy.evaluate("BTCUSDT", blockedProviderChannel()),
                policy.evaluate("BTCUSDT", disabledProviderChannel()),
                policy.evaluate("BTCUSDT", incompleteProviderChannel()),
                policy.evaluate("BTCUSDT", disabledNoopProviderChannel())
        );

        for (OpportunityPushExternalChannelDTO output : outputs) {
            assertReviewOnlyNoExternalChannelDefaults(output);
        }
    }

    @Test
    void dtoDefensiveCopy() {
        List<String> externalChannelReasons = new ArrayList<>(List.of("EXTERNAL_REASON"));
        List<String> providerChannelReasons = new ArrayList<>(List.of("CHANNEL_REASON"));
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

        OpportunityPushExternalChannelDTO result =
                OpportunityPushExternalChannelDTO.disabledNoop(
                        "BTCUSDT",
                        OpportunityPushProviderChannelStatusEnum.DISABLED_NOOP,
                        OpportunityPushMessageEnvelopeStatusEnum.DISABLED_NOOP,
                        OpportunityPushDeliveryPipelineStatusEnum.DISABLED_NOOP,
                        OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY,
                        OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY,
                        OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY,
                        OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                        "unit-test",
                        externalChannelReasons,
                        providerChannelReasons,
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

        externalChannelReasons.add("MUTATED_EXTERNAL");
        providerChannelReasons.add("MUTATED_CHANNEL");
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
        result.getExternalChannelReasons().add("GETTER_MUTATION");
        result.getProviderChannelReasons().add("GETTER_MUTATION");
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

        assertThat(result.getExternalChannelReasons()).containsExactly("EXTERNAL_REASON");
        assertThat(result.getProviderChannelReasons()).containsExactly("CHANNEL_REASON");
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

        for (OpportunityPushExternalChannelStatusEnum status
                : OpportunityPushExternalChannelStatusEnum.values()) {
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
                "Credential",
                "Secret",
                "Order",
                "Execution",
                "AutoTrading",
                "Scheduled"
        );

        for (Field field : NoOpOpportunityPushExternalChannelPolicy.class.getDeclaredFields()) {
            for (String token : forbiddenDependencies) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(OpportunityPushExternalChannelPolicy.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = OpportunityPushExternalChannelPolicy.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("evaluate");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(OpportunityPushExternalChannelDTO.class);

        for (Method method : NoOpOpportunityPushExternalChannelPolicy.class.getDeclaredMethods()) {
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
            assertThat(methodName).doesNotContain("credential");
            assertThat(methodName).doesNotContain("secret");
            assertThat(methodName).doesNotContain("telegram");
            assertThat(methodName).doesNotContain("webhook");
            assertThat(methodName).doesNotContain("email");
            for (String token : forbiddenDependencies) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("Scheduled");
            }
        }

        assertThat(NoOpOpportunityPushExternalChannelPolicy.class.getDeclaredAnnotations()).isEmpty();
        assertThat(OpportunityPushExternalChannelPolicy.class.getDeclaredAnnotations()).isEmpty();
    }

    private static OpportunityPushProviderChannelDTO disabledNoopProviderChannel() {
        return OpportunityPushProviderChannelDTO.disabledNoop(
                "BTCUSDT",
                OpportunityPushMessageEnvelopeStatusEnum.DISABLED_NOOP,
                OpportunityPushDeliveryPipelineStatusEnum.DISABLED_NOOP,
                OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY,
                OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY,
                OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY,
                OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                "unit-test",
                List.of("PROVIDER_CHANNEL_DISABLED_NOOP"),
                List.of("MESSAGE_ENVELOPE_DISABLED_NOOP"),
                List.of("PIPELINE_DISABLED_NOOP"),
                List.of("QUEUE_NOOP_REVIEW_ONLY"),
                List.of("PERSISTENCE_NOOP_REVIEW_ONLY"),
                List.of("AUDIT_ONLY_ENVELOPE"),
                List.of("DELIVERY_REVIEW_ONLY"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PROVIDER_CHANNEL_BLOCKING_REASON")
        );
    }

    private static OpportunityPushProviderChannelDTO blockedProviderChannel() {
        return OpportunityPushProviderChannelDTO.blocked(
                "BTCUSDT",
                OpportunityPushMessageEnvelopeStatusEnum.BLOCKED,
                OpportunityPushDeliveryPipelineStatusEnum.BLOCKED,
                OpportunityPushAuditQueueStatusEnum.BLOCKED,
                OpportunityPushAuditPersistenceStatusEnum.BLOCKED,
                OpportunityPushAuditEnvelopeStatusEnum.BLOCKED,
                OpportunityPushDeliveryDecisionStatusEnum.BLOCKED,
                "unit-test",
                List.of("MESSAGE_ENVELOPE_BLOCKED"),
                List.of("PIPELINE_BLOCKED"),
                List.of("QUEUE_BLOCKED"),
                List.of("PERSISTENCE_BLOCKED"),
                List.of("AUDIT_BLOCKED"),
                List.of("DELIVERY_BLOCKED"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_BLOCKED"),
                List.of("PROVIDER_CHANNEL_BLOCKED_REASON")
        );
    }

    private static OpportunityPushProviderChannelDTO disabledProviderChannel() {
        return OpportunityPushProviderChannelDTO.disabled(
                "BTCUSDT",
                OpportunityPushMessageEnvelopeStatusEnum.DISABLED,
                OpportunityPushDeliveryPipelineStatusEnum.DISABLED,
                OpportunityPushAuditQueueStatusEnum.DISABLED,
                OpportunityPushAuditPersistenceStatusEnum.DISABLED,
                OpportunityPushAuditEnvelopeStatusEnum.DISABLED,
                OpportunityPushDeliveryDecisionStatusEnum.DISABLED,
                "unit-test",
                List.of("MESSAGE_ENVELOPE_DISABLED"),
                List.of("PIPELINE_DISABLED"),
                List.of("QUEUE_DISABLED"),
                List.of("PERSISTENCE_DISABLED"),
                List.of("AUDIT_DISABLED"),
                List.of("DELIVERY_DISABLED"),
                List.of("PUSH_DISABLED"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PROVIDER_CHANNEL_DISABLED_REASON")
        );
    }

    private static OpportunityPushProviderChannelDTO incompleteProviderChannel() {
        return OpportunityPushProviderChannelDTO.incomplete(
                "BTCUSDT",
                OpportunityPushMessageEnvelopeStatusEnum.INCOMPLETE,
                OpportunityPushDeliveryPipelineStatusEnum.INCOMPLETE,
                OpportunityPushAuditQueueStatusEnum.INCOMPLETE,
                OpportunityPushAuditPersistenceStatusEnum.INCOMPLETE,
                OpportunityPushAuditEnvelopeStatusEnum.INCOMPLETE,
                OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE,
                "unit-test",
                List.of("MESSAGE_ENVELOPE_INCOMPLETE"),
                List.of("PIPELINE_INCOMPLETE"),
                List.of("QUEUE_INCOMPLETE"),
                List.of("PERSISTENCE_INCOMPLETE"),
                List.of("AUDIT_INCOMPLETE"),
                List.of("DELIVERY_INCOMPLETE"),
                List.of("PUSH_REVIEW_ONLY"),
                List.of("ATTENTION_REVIEW_ONLY"),
                List.of("RISK_ACTION_GUARD_REVIEW_ONLY"),
                List.of("PROVIDER_CHANNEL_INCOMPLETE_REASON")
        );
    }

    private static OpportunityPushProviderChannelDTO unsafeProviderChannel() throws Exception {
        OpportunityPushProviderChannelDTO result = disabledNoopProviderChannel();
        Field field = OpportunityPushProviderChannelDTO.class.getDeclaredField("messageSent");
        field.setAccessible(true);
        field.setBoolean(result, true);
        return result;
    }

    private static void assertReviewOnlyNoExternalChannelDefaults(
            OpportunityPushExternalChannelDTO result
    ) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isAuditOnly()).isTrue();
        assertThat(result.isExternalChannelEnabled()).isFalse();
        assertThat(result.isExternalChannelSelected()).isFalse();
        assertThat(result.isExternalChannelConfigured()).isFalse();
        assertThat(result.isExternalChannelCredentialRequired()).isFalse();
        assertThat(result.isExternalChannelCredentialUsed()).isFalse();
        assertThat(result.isLiveExternalCallAttempted()).isFalse();
        assertThat(result.isMessageRendered()).isFalse();
        assertThat(result.isMessageSent()).isFalse();
        assertThat(result.isExternalPushSent()).isFalse();
        assertThat(result.isDeliveryAttempted()).isFalse();
        assertThat(result.isDeliveryEnabled()).isFalse();
        assertThat(result.isProviderChannelEnabled()).isFalse();
        assertThat(result.isProviderSelected()).isFalse();
        assertThat(result.isProviderCredentialRequired()).isFalse();
        assertThat(result.isProviderCredentialUsed()).isFalse();
        assertThat(result.isLiveProviderCallAttempted()).isFalse();
        assertThat(result.isMessageEnvelopeCreated()).isFalse();
        assertThat(result.isMessageRenderable()).isFalse();
        assertThat(result.isMessageSendable()).isFalse();
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
