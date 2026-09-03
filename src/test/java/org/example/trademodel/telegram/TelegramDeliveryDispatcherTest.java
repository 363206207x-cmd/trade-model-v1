package org.example.trademodel.telegram;

import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.service.ChannelDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramDeliveryDispatcherTest {
    @Mock private TelegramClient client;
    @Mock private TelegramReadinessService readiness;
    @Mock private TelegramMessageFormatter formatter;
    @Mock private ChannelDeliveryService deliveryService;
    @Mock private MessageMapper messageMapper;

    private TelegramProperties properties;
    private TelegramDeliveryDispatcher dispatcher;
    private String recipientFingerprint;

    @BeforeEach
    void setUp() {
        properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setBotToken("TEST_TOKEN");
        properties.setChatId("41001");
        properties.setDispatchEnabled(true);
        properties.setMaxAttempts(3);
        properties.setRetryBaseSeconds(5);
        properties.setRetryMaxSeconds(300);
        dispatcher = new TelegramDeliveryDispatcher(
                properties, client, readiness, formatter, deliveryService, messageMapper, true);
        recipientFingerprint = TelegramSecretSanitizer.recipientFingerprint(properties.getChatId());
        org.mockito.Mockito.lenient().when(deliveryService.extendClaimForProviderCall(any())).thenReturn(true);
        org.mockito.Mockito.lenient().when(deliveryService.completeClaim(any())).thenReturn(true);
        org.mockito.Mockito.lenient().when(client.getMe())
                .thenReturn(TelegramClientResult.success(200, null, "test_bot", null));
        org.mockito.Mockito.lenient().when(client.getChat())
                .thenReturn(TelegramClientResult.success(200, null, null, recipientFingerprint));
    }

    @Test
    void successfulClaimStoresProviderReferenceAndCanNeverRemainQueued() {
        ChannelDeliveryDO delivery = delivery(1);
        MessageDO message = message();
        TelegramOutboundMessage outbound = new TelegramOutboundMessage("人工复核", null, null);
        when(messageMapper.selectByIdForUser("message-1", 41L)).thenReturn(message);
        when(formatter.format(message)).thenReturn(outbound);
        when(client.sendMessage(outbound)).thenReturn(verifiedSuccess("9"));

        dispatcher.dispatchOne(delivery);

        assertThat(delivery.getStatus()).isEqualTo("SENT");
        assertThat(delivery.getProviderReference()).isEqualTo("9");
        assertThat(delivery.getDeliveredAt()).isNotNull();
        assertThat(delivery.getNextAttemptAt()).isNull();
        verify(deliveryService).extendClaimForProviderCall(delivery);
        verify(deliveryService).reconcileProviderSuccess(delivery);
        verify(client).getMe();
        verify(client).getChat();
        verify(client, times(1)).sendMessage(outbound);
    }

    @Test
    void lostClaimBeforeProviderCallFailsClosedWithoutExternalSend() {
        ChannelDeliveryDO delivery = delivery(1);
        when(messageMapper.selectByIdForUser("message-1", 41L)).thenReturn(message());
        when(deliveryService.extendClaimForProviderCall(delivery)).thenReturn(false);

        dispatcher.dispatchOne(delivery);

        verify(client, never()).sendMessage(any());
        verify(deliveryService).failClosedOutcome("delivery-1", "DELIVERY_CLAIM_LOST",
                "Delivery claim was not valid before provider call; manual review required");
    }

    @Test
    void providerSuccessNeverSchedulesAutomaticRetryWhenSentReconciliationIsUnconfirmed() {
        ChannelDeliveryDO delivery = delivery(1);
        stubMessage(delivery, verifiedSuccess("9"));
        when(deliveryService.reconcileProviderSuccess(delivery)).thenReturn(false);

        dispatcher.dispatchOne(delivery);

        assertThat(delivery.getStatus()).isEqualTo("SENT");
        assertThat(delivery.getNextAttemptAt()).isNull();
        verify(client, times(1)).sendMessage(any());
        verify(deliveryService).reconcileProviderSuccess(delivery);
    }

    @Test
    void changedRecipientConfigurationFailsClosedBeforeAnyProviderCall() {
        ChannelDeliveryDO delivery = delivery(1);
        delivery.setRecipientFingerprint(TelegramSecretSanitizer.recipientFingerprint("99999"));
        when(messageMapper.selectByIdForUser("message-1", 41L)).thenReturn(message());

        dispatcher.dispatchOne(delivery);

        assertThat(delivery.getStatus()).isEqualTo("FAILED");
        assertThat(delivery.getErrorCode()).isEqualTo("RECIPIENT_CONFIGURATION_CHANGED");
        assertThat(delivery.getNextAttemptAt()).isNull();
        verify(client, never()).getMe();
        verify(client, never()).getChat();
        verify(client, never()).sendMessage(any());
    }

    @Test
    void failedBotOrRecipientPreflightNeverCallsSendMessage() {
        ChannelDeliveryDO botMismatch = delivery(1);
        when(messageMapper.selectByIdForUser("message-1", 41L)).thenReturn(message());
        when(client.getMe()).thenReturn(TelegramClientResult.failure(
                200, TelegramReadinessState.DEGRADED, "BOT_IDENTITY_UNVERIFIED", "bad bot", null, false));

        dispatcher.dispatchOne(botMismatch);

        assertThat(botMismatch.getStatus()).isEqualTo("FAILED");
        assertThat(botMismatch.getErrorCode()).isEqualTo("BOT_IDENTITY_UNVERIFIED");
        verify(client, never()).sendMessage(any());

        org.mockito.Mockito.reset(client);
        when(client.getMe()).thenReturn(TelegramClientResult.success(200, null, "test_bot", null));
        when(client.getChat()).thenReturn(TelegramClientResult.failure(
                200, TelegramReadinessState.DEGRADED,
                "RECIPIENT_IDENTITY_MISMATCH", "bad recipient", null, false));
        ChannelDeliveryDO recipientMismatch = delivery(2);

        dispatcher.dispatchOne(recipientMismatch);

        assertThat(recipientMismatch.getStatus()).isEqualTo("FAILED");
        assertThat(recipientMismatch.getErrorCode()).isEqualTo("RECIPIENT_IDENTITY_MISMATCH");
        verify(client, never()).sendMessage(any());
    }

    @Test
    void providerOkWithoutMatchingBotRecipientAndMessageIdFailsClosedWithoutRetry() {
        ChannelDeliveryDO delivery = delivery(1);
        stubMessage(delivery, TelegramClientResult.success(200, null, "other_bot", recipientFingerprint));

        dispatcher.dispatchOne(delivery);

        assertThat(delivery.getStatus()).isEqualTo("FAILED");
        assertThat(delivery.getErrorCode()).isEqualTo("DELIVERY_RECEIPT_UNVERIFIED");
        assertThat(delivery.getProviderReference()).isNull();
        assertThat(delivery.getDeliveredAt()).isNull();
        assertThat(delivery.getNextAttemptAt()).isNull();
    }

    @Test
    void rateLimitUsesRetryAfterAndServerFailureUsesBoundedBackoff() {
        ChannelDeliveryDO rateLimited = delivery(1);
        stubMessage(rateLimited, TelegramClientResult.failure(
                429, TelegramReadinessState.RATE_LIMITED, "RATE_LIMITED", "slow", 17, true));
        dispatcher.dispatchOne(rateLimited);
        assertThat(rateLimited.getStatus()).isEqualTo("RETRYING");
        assertThat(rateLimited.getRetryAfterSeconds()).isEqualTo(17);
        assertThat(rateLimited.getNextAttemptAt()).isAfter(LocalDateTime.now(Clock.systemUTC()).plusSeconds(10));
        assertThat(dispatcher.retryDelaySeconds(rateLimited, 600)).isEqualTo(600);

        ChannelDeliveryDO serverError = delivery(2);
        stubMessage(serverError, TelegramClientResult.failure(
                500, TelegramReadinessState.PROVIDER_UNAVAILABLE,
                "PROVIDER_UNAVAILABLE", "down", null, true));
        dispatcher.dispatchOne(serverError);
        assertThat(serverError.getStatus()).isEqualTo("RETRYING");
        assertThat(dispatcher.retryDelaySeconds(serverError, null)).isBetween(10, 13);
    }

    @Test
    void nonRetryableAndMaxAttemptFailuresBecomeTerminalWithoutFakeSent() {
        ChannelDeliveryDO badRequest = delivery(1);
        stubMessage(badRequest, TelegramClientResult.failure(
                400, TelegramReadinessState.DEGRADED, "BAD_REQUEST", "bad", null, false));
        dispatcher.dispatchOne(badRequest);
        assertThat(badRequest.getStatus()).isEqualTo("FAILED");
        assertThat(badRequest.getDeliveredAt()).isNull();

        ChannelDeliveryDO exhausted = delivery(3);
        stubMessage(exhausted, TelegramClientResult.failure(
                0, TelegramReadinessState.PROVIDER_UNAVAILABLE,
                "PROVIDER_UNAVAILABLE", "network", null, true));
        dispatcher.dispatchOne(exhausted);
        assertThat(exhausted.getStatus()).isEqualTo("FAILED");
        assertThat(exhausted.getNextAttemptAt()).isNull();
    }

    @Test
    void expiredMessageIsSuppressedBeforeProviderCall() {
        ChannelDeliveryDO delivery = delivery(1);
        MessageDO message = message();
        message.setExpiresAt(LocalDateTime.now(Clock.systemUTC()).minusSeconds(1));
        when(messageMapper.selectByIdForUser("message-1", 41L)).thenReturn(message);

        dispatcher.dispatchOne(delivery);

        assertThat(delivery.getStatus()).isEqualTo("SUPPRESSED");
        assertThat(delivery.getErrorCode()).isEqualTo("MESSAGE_EXPIRED");
        assertThat(delivery.getDeliveredAt()).isNull();
        verify(client, never()).sendMessage(any());
        verify(deliveryService).completeClaim(delivery);
    }

    @Test
    void legacyIneligibleDeliveryIsSuppressedBeforeBotCall() {
        ChannelDeliveryDO delivery = delivery(1);
        MessageDO legacy = message();
        legacy.setTitle("【持仓逻辑发生重要变化】");
        when(messageMapper.selectByIdForUser("message-1", 41L)).thenReturn(legacy);

        dispatcher.dispatchOne(delivery);

        assertThat(delivery.getStatus()).isEqualTo("SUPPRESSED");
        assertThat(delivery.getErrorCode()).isEqualTo("TELEGRAM_CATEGORY_NOT_ELIGIBLE");
        assertThat(delivery.getNextAttemptAt()).isNull();
        verify(client, never()).sendMessage(any());
        verify(deliveryService).completeClaim(delivery);
    }

    @Test
    void dispatchCycleRecoversCrashClaimsBeforeClaimingDueRows() {
        when(readiness.canAttemptDelivery()).thenReturn(true);
        when(messageMapper.listTelegramDeliveryOrphans(any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(20)))
                .thenReturn(List.of());
        when(deliveryService.claimDue(20)).thenReturn(List.of());

        assertThat(dispatcher.dispatchDue()).isZero();

        var inOrder = org.mockito.Mockito.inOrder(deliveryService);
        inOrder.verify(deliveryService).recoverExpiredClaims();
        inOrder.verify(deliveryService).claimDue(20);
        verify(client, never()).sendMessage(any());
    }

    @Test
    void laterOrphanRecoveryQueuesCanonicalSafetyMessageAfterCommitListenerFailure() {
        MessageDO safety = HighValueAlertPolicyTest.eligibleSafetyMessage();
        safety.setMessageId("message-safety");
        safety.setExpiresAt(LocalDateTime.now(Clock.systemUTC()).plusMinutes(5));
        when(messageMapper.listTelegramDeliveryOrphans(any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(20)))
                .thenReturn(List.of(safety));
        when(deliveryService.queueTelegram(41L, "message-safety"))
                .thenThrow(new IllegalStateException("after-commit queue unavailable"))
                .thenReturn(new ChannelDeliveryDO());

        assertThat(dispatcher.reconcileOrphanedMessages()).isZero();
        assertThat(dispatcher.reconcileOrphanedMessages()).isEqualTo(1);

        verify(deliveryService, times(2)).queueTelegram(41L, "message-safety");
    }

    @Test
    void concurrentDispatchCyclesProduceOneExternalCallForOneAtomicClaim() {
        ChannelDeliveryDO delivery = delivery(1);
        when(readiness.canAttemptDelivery()).thenReturn(true);
        when(messageMapper.listTelegramDeliveryOrphans(any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(20)))
                .thenReturn(List.of());
        when(deliveryService.claimDue(20)).thenReturn(List.of(delivery), List.of());
        stubMessage(delivery, verifiedSuccess("9"));

        assertThat(dispatcher.dispatchDue()).isEqualTo(1);
        assertThat(dispatcher.dispatchDue()).isZero();

        verify(client, times(1)).sendMessage(any());
    }

    private void stubMessage(ChannelDeliveryDO delivery, TelegramClientResult result) {
        MessageDO message = message();
        TelegramOutboundMessage outbound = new TelegramOutboundMessage("人工复核", null, null);
        when(messageMapper.selectByIdForUser(delivery.getMessageId(), delivery.getUserId())).thenReturn(message);
        when(formatter.format(message)).thenReturn(outbound);
        when(client.sendMessage(outbound)).thenReturn(result);
    }

    private ChannelDeliveryDO delivery(int attempts) {
        ChannelDeliveryDO delivery = new ChannelDeliveryDO();
        delivery.setDeliveryId("delivery-" + attempts);
        delivery.setMessageId("message-1");
        delivery.setUserId(41L);
        delivery.setStatus("SENDING");
        delivery.setAttemptCount(attempts);
        delivery.setClaimToken("claim-" + attempts);
        delivery.setRecipientFingerprint(recipientFingerprint);
        return delivery;
    }

    private TelegramClientResult verifiedSuccess(String providerReference) {
        return TelegramClientResult.success(
                200, providerReference, "test_bot", recipientFingerprint);
    }

    private static MessageDO message() {
        MessageDO message = HighValueAlertPolicyTest.eligiblePositionMessage("RISK_HIGH");
        message.setMessageId("message-1");
        message.setExpiresAt(LocalDateTime.now(Clock.systemUTC()).plusMinutes(5));
        return message;
    }
}
