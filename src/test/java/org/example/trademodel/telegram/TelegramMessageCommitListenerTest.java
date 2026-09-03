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
    void allThreeCanonicalCategoriesQueueAfterCommitAndMalformedEventsRemainInAppOnly() throws Exception {
        ChannelDeliveryService delivery = mock(ChannelDeliveryService.class);
        TelegramMessageCommitListener listener = new TelegramMessageCommitListener(delivery);

        listener.afterMessageCommit(new MessageRecordedEvent("message-1", 41L, "ordinary-message"));
        listener.afterMessageCommit(new MessageRecordedEvent(
                "message-malformed-safety", 41L, "TG1|HOT_RESET|CONFUSED|4|9|FINAL_PLAN|hash"));
        listener.afterMessageCommit(new MessageRecordedEvent(
                "message-safety", 41L, "TG1|PLAN_SAFETY_CHANGE|HOT_RESET|4|9|FINAL_PLAN|hash"));
        listener.afterMessageCommit(new MessageRecordedEvent(
                "message-legacy", 41L, "TG1|OPPORTUNITY_READY|TRIGGERED|3|9|FINAL_PLAN|hash"));
        listener.afterMessageCommit(new MessageRecordedEvent(
                "message-plan", 41L, "TG1|OPPORTUNITY_READY|CONFIRMATION|3|9|FINAL_PLAN|hash"));
        listener.afterMessageCommit(new MessageRecordedEvent(
                "message-position", 41L, "TG1|POSITION_RISK_CHANGE|RISK_HIGH|3|9|USER_POSITION|hash"));

        verify(delivery, never()).queueTelegram(41L, "message-1");
        verify(delivery, never()).queueTelegram(41L, "message-malformed-safety");
        verify(delivery, never()).queueTelegram(41L, "message-legacy");
        verify(delivery).queueTelegram(41L, "message-plan");
        verify(delivery).queueTelegram(41L, "message-position");
        verify(delivery).queueTelegram(41L, "message-safety");

        TransactionalEventListener annotation = TelegramMessageCommitListener.class
                .getMethod("afterMessageCommit", MessageRecordedEvent.class)
                .getAnnotation(TransactionalEventListener.class);
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isFalse();
    }
}
