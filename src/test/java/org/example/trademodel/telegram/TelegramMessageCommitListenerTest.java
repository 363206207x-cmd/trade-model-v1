package org.example.trademodel.telegram;

import org.example.trademodel.message.MessageRecordedEvent;
import org.example.trademodel.service.ChannelDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TelegramMessageCommitListenerTest {

    @Test
    void onlyManagedHighValueMessagesQueueAfterCommit() throws Exception {
        ChannelDeliveryService delivery = mock(ChannelDeliveryService.class);
        TelegramMessageCommitListener listener = new TelegramMessageCommitListener(delivery);

        listener.afterMessageCommit(new MessageRecordedEvent("message-1", 41L, "ordinary-message"));
        listener.afterMessageCommit(new MessageRecordedEvent("message-2", 41L, "TG1|EVENT|STATE|2|9|hash"));

        verify(delivery, never()).queueTelegram(41L, "message-1");
        verify(delivery).queueTelegram(41L, "message-2");

        TransactionalEventListener annotation = TelegramMessageCommitListener.class
                .getMethod("afterMessageCommit", MessageRecordedEvent.class)
                .getAnnotation(TransactionalEventListener.class);
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isFalse();
    }
}
