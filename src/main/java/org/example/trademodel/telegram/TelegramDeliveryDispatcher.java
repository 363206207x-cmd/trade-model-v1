package org.example.trademodel.telegram;

import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.service.ChannelDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

@Service
public class TelegramDeliveryDispatcher {
    private final TelegramProperties properties;
    private final TelegramClient client;
    private final TelegramReadinessService readinessService;
    private final TelegramMessageFormatter formatter;
    private final ChannelDeliveryService deliveryService;
    private final MessageMapper messageMapper;
    private final boolean schedulersEnabled;
    private final Clock clock = Clock.systemUTC();

    public TelegramDeliveryDispatcher(TelegramProperties properties,
                                      TelegramClient client,
                                      TelegramReadinessService readinessService,
                                      TelegramMessageFormatter formatter,
                                      ChannelDeliveryService deliveryService,
                                      MessageMapper messageMapper,
                                      @Value("${trade-model.schedulers.enabled:true}") boolean schedulersEnabled) {
        this.properties = properties;
        this.client = client;
        this.readinessService = readinessService;
        this.formatter = formatter;
        this.deliveryService = deliveryService;
        this.messageMapper = messageMapper;
        this.schedulersEnabled = schedulersEnabled;
    }

    @Scheduled(fixedDelayString = "${trade-model.telegram.dispatch-fixed-delay-ms:5000}")
    public void dispatchScheduled() {
        if (!schedulersEnabled || !properties.isDispatchEnabled()) return;
        dispatchDue();
    }

    public int dispatchDue() {
        if (!properties.isDispatchEnabled() || !readinessService.canAttemptDelivery()) return 0;
        deliveryService.recoverExpiredClaims();
        reconcileOrphanedMessages();
        List<ChannelDeliveryDO> claimed = deliveryService.claimDue(properties.getDeliveryBatchSize());
        claimed.forEach(this::dispatchOne);
        return claimed.size();
    }

    public int reconcileOrphanedMessages() {
        int queued = 0;
        for (MessageDO message : messageMapper.listTelegramDeliveryOrphans(properties.getDeliveryBatchSize())) {
            try {
                deliveryService.queueTelegram(message.getUserId(), message.getMessageId());
                queued++;
            } catch (RuntimeException ignored) {
                // A later run retries; Message remains intact and queryable.
            }
        }
        return queued;
    }

    void dispatchOne(ChannelDeliveryDO delivery) {
        MessageDO message = messageMapper.selectByIdForUser(delivery.getMessageId(), delivery.getUserId());
        if (message == null) {
            terminal(delivery, "MESSAGE_NOT_FOUND", "Committed message could not be loaded", 0);
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (message.getExpiresAt() != null && !now.isBefore(message.getExpiresAt())) {
            suppressExpired(delivery, now);
            return;
        }
        if (!deliveryService.extendClaimForProviderCall(delivery)) {
            deliveryService.failClosedOutcome(delivery.getDeliveryId(), "DELIVERY_CLAIM_LOST",
                    "Delivery claim was not valid before provider call; manual review required");
            return;
        }
        TelegramClientResult result;
        try {
            result = client.sendMessage(formatter.format(message));
        } catch (RuntimeException unexpected) {
            result = TelegramClientResult.failure(0, TelegramReadinessState.PROVIDER_UNAVAILABLE,
                    "PROVIDER_UNAVAILABLE", "Telegram delivery failed", null, true);
        }
        readinessService.observe(result);
        if (result.success()) {
            delivery.setStatus(TelegramDeliveryStatus.SENT.name());
            delivery.setProviderReference(result.providerReference());
            delivery.setDeliveredAt(LocalDateTime.now(clock));
            delivery.setNextAttemptAt(null);
            delivery.setLastResponseCode(result.httpStatus());
            delivery.setRetryAfterSeconds(null);
            delivery.setErrorCode(null);
            delivery.setErrorMessage(null);
            deliveryService.reconcileProviderSuccess(delivery);
            return;
        }
        if (result.retryable() && safeAttempts(delivery) < properties.getMaxAttempts()) {
            int delay = retryDelaySeconds(delivery, result.retryAfterSeconds());
            delivery.setStatus(TelegramDeliveryStatus.RETRYING.name());
            delivery.setNextAttemptAt(LocalDateTime.now(clock).plusSeconds(delay));
            delivery.setRetryAfterSeconds(result.retryAfterSeconds());
        } else {
            delivery.setStatus(TelegramDeliveryStatus.FAILED.name());
            delivery.setNextAttemptAt(null);
            delivery.setRetryAfterSeconds(result.retryAfterSeconds());
        }
        delivery.setLastResponseCode(result.httpStatus());
        delivery.setErrorCode(result.errorCode());
        delivery.setErrorMessage(TelegramSecretSanitizer.sanitize(result.errorMessage(), properties));
        completeKnownOutcome(delivery);
    }

    int retryDelaySeconds(ChannelDeliveryDO delivery, Integer providerRetryAfter) {
        if (providerRetryAfter != null && providerRetryAfter > 0) {
            return providerRetryAfter;
        }
        int exponent = Math.max(0, Math.min(20, safeAttempts(delivery) - 1));
        long exponential = (long) properties.getRetryBaseSeconds() << exponent;
        int bounded = (int) Math.min(exponential, properties.getRetryMaxSeconds());
        int jitterRange = Math.max(1, Math.min(10, bounded / 4 + 1));
        int jitter = Math.floorMod(delivery.getDeliveryId().hashCode(), jitterRange);
        return Math.min(properties.getRetryMaxSeconds(), bounded + jitter);
    }

    private void terminal(ChannelDeliveryDO delivery, String code, String message, int status) {
        delivery.setStatus(TelegramDeliveryStatus.FAILED.name());
        delivery.setNextAttemptAt(null);
        delivery.setLastResponseCode(status);
        delivery.setErrorCode(code);
        delivery.setErrorMessage(message);
        completeKnownOutcome(delivery);
    }

    private void suppressExpired(ChannelDeliveryDO delivery, LocalDateTime now) {
        delivery.setStatus(TelegramDeliveryStatus.SUPPRESSED.name());
        delivery.setNextAttemptAt(null);
        delivery.setRetryAfterSeconds(null);
        delivery.setErrorCode("MESSAGE_EXPIRED");
        delivery.setErrorMessage("Telegram alert expired before delivery");
        delivery.setUpdatedAt(now);
        completeKnownOutcome(delivery);
    }

    private void completeKnownOutcome(ChannelDeliveryDO delivery) {
        if (!deliveryService.completeClaim(delivery)) {
            deliveryService.failClosedOutcome(delivery.getDeliveryId(), "DELIVERY_OUTCOME_UNKNOWN",
                    "Delivery claim changed before provider outcome persistence; manual review required");
        }
    }

    private static int safeAttempts(ChannelDeliveryDO delivery) {
        return delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount();
    }
}
