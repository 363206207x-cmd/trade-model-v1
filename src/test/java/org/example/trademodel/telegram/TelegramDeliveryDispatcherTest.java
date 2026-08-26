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

    @BeforeEach
    void setUp() {
        properties = new TelegramProperties();
        properties.setDispatchEnabled(true);
        properties.setMaxAttempts(3);
        properties.setRetryBaseSeconds(5);
        properties.setRetryMaxSeconds(300);
        dispatcher = new TelegramDeliveryDispatcher(
                properties, client, readiness, formatter, deliveryService, messageMapper, true);
        org.mockito.Mockito.lenient().when(deliveryService.extendClaimForProviderCall(any())).thenReturn(true);
        org.mockito.Mockito.lenient().when(deliveryService.completeClaim(any())).thenReturn(true);
    }

    @Test
    void successfulClaimStoresProviderReferenceAndCanNeverRemainQueued() {
        ChannelDeliveryDO delivery = delivery(1);
        MessageDO message = message();
        TelegramOutboundMessage outbound = new TelegramOutboundMessage("人工复核", null, null);
        when(messageMapper.selectByIdForUser("message-1", 41L)).thenReturn(message);
        when(formatter.format(message)).thenReturn(outbound);
        when(client.sendMessage(outbound)).thenReturn(TelegramClientResult.success(200, "provider-9", null));

        dispatcher.dispatchOne(delivery);

        assertThat(delivery.getStatus()).isEqualTo("SENT");
        assertThat(delivery.getProviderReference()).isEqualTo("provider-9");
        assertThat(delivery.getDeliveredAt()).isNotNull();
        assertThat(delivery.getNextAttemptAt()).isNull();
        verify(deliveryService).extendClaimForProviderCall(delivery);
        verify(deliveryService).reconcileProviderSuccess(delivery);
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
        stubMessage(delivery, TelegramClientResult.success(200, "provider-9", null));
        when(deliveryService.reconcileProviderSuccess(delivery)).thenReturn(false);

        dispatcher.dispatchOne(delivery);

        assertThat(delivery.getStatus()).isEqualTo("SENT");
        assertThat(delivery.getNextAttemptAt()).isNull();
        verify(client, times(1)).sendMessage(any());
        verify(deliveryService).reconcileProviderSuccess(delivery);
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
        when(messageMapper.listTelegramDeliveryOrphans(20)).thenReturn(List.of());
        when(deliveryService.claimDue(20)).thenReturn(List.of());

        assertThat(dispatcher.dispatchDue()).isZero();

        var inOrder = org.mockito.Mockito.inOrder(deliveryService);
        inOrder.verify(deliveryService).recoverExpiredClaims();
        inOrder.verify(deliveryService).claimDue(20);
        verify(client, never()).sendMessage(any());
    }

    @Test
    void concurrentDispatchCyclesProduceOneExternalCallForOneAtomicClaim() {
        ChannelDeliveryDO delivery = delivery(1);
        when(readiness.canAttemptDelivery()).thenReturn(true);
        when(messageMapper.listTelegramDeliveryOrphans(20)).thenReturn(List.of());
        when(deliveryService.claimDue(20)).thenReturn(List.of(delivery), List.of());
        stubMessage(delivery, TelegramClientResult.success(200, "provider-9", null));

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

    private static ChannelDeliveryDO delivery(int attempts) {
        ChannelDeliveryDO delivery = new ChannelDeliveryDO();
        delivery.setDeliveryId("delivery-" + attempts);
        delivery.setMessageId("message-1");
        delivery.setUserId(41L);
        delivery.setStatus("SENDING");
        delivery.setAttemptCount(attempts);
        delivery.setClaimToken("claim-" + attempts);
        return delivery;
    }

    private static MessageDO message() {
        MessageDO message = HighValueAlertPolicyTest.eligiblePositionMessage("RISK_HIGH");
        message.setMessageId("message-1");
        return message;
    }
}
