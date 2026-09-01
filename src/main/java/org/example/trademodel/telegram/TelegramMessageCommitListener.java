package org.example.trademodel.telegram;

import org.example.trademodel.message.MessageRecordedEvent;
import org.example.trademodel.service.ChannelDeliveryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TelegramMessageCommitListener {
    private final ChannelDeliveryService channelDeliveryService;

    public TelegramMessageCommitListener(ChannelDeliveryService channelDeliveryService) {
        this.channelDeliveryService = channelDeliveryService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterMessageCommit(MessageRecordedEvent event) {
        if (event == null || !HighValueAlertPolicy.telegramDeliveryEventEligible(event.dedupeKey())) return;
        try {
            channelDeliveryService.queueTelegram(event.userId(), event.messageId());
        } catch (RuntimeException ignored) {
            // The committed Message remains authoritative; orphan reconciliation retries queue creation.
        }
    }
}
