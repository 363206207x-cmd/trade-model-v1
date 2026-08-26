package org.example.trademodel.service;

import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.mapper.ChannelDeliveryMapper;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.telegram.TelegramDedupeKey;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class MessageFactServiceTest {
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private ChannelDeliveryMapper deliveryMapper;

    @Test
    void canonicalMessageOwnerAcceptsOnlyTheThreeFrozenBusinessCategories() {
        MessageFactService service = new MessageFactService(messageMapper);
        when(messageMapper.countByDedupeKey(41L, "position-risk:7:trace-1")).thenReturn(0);
        when(messageMapper.insert(any())).thenReturn(1);
        MessageDO input = message("POSITION_LOGIC_RISK_CHANGE", "position-risk:7:trace-1");

        MessageDO saved = service.record(input);

        assertThat(saved.getBusinessState()).isEqualTo("ACTIVE");
        assertThat(saved.getReadState()).isEqualTo("UNREAD");
        assertThat(saved.getNotTradeInstruction()).isTrue();
        assertThat(saved.getNotOrderExecution()).isTrue();
        verify(messageMapper).insert(saved);

        assertThatThrownBy(() -> service.record(message("MARKETING", "marketing-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported message category");
    }

    @Test
    void duplicateBusinessFactsDoNotCreateASecondMessageOwner() {
        MessageFactService service = new MessageFactService(messageMapper);
        when(messageMapper.countByDedupeKey(41L, "plan-change:plan-1:v2")).thenReturn(1);

        assertThatThrownBy(() -> service.record(
                message("OPPORTUNITY_PLAN_SAFETY_CHANGE", "plan-change:plan-1:v2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void telegramIsOnlyADeliveryChannelAndNeverChangesMessageFact() {
        MessageFactService messageService = new MessageFactService(messageMapper);
        ChannelDeliveryService deliveryService = new ChannelDeliveryService(deliveryMapper, messageService);
        MessageDO fact = eligibleOpportunity();
        fact.setMessageId("message-1");
        when(messageMapper.selectByIdForUser("message-1", 41L)).thenReturn(fact);
        when(deliveryMapper.insert(any())).thenReturn(1);

        ChannelDeliveryDO suppressed = deliveryService.queueTelegram(41L, "message-1", false);
        ChannelDeliveryDO queued = deliveryService.queueTelegram(41L, "message-1", true);

        assertThat(suppressed.getStatus()).isEqualTo("SUPPRESSED");
        assertThat(suppressed.getErrorCode()).isEqualTo("TELEGRAM_NOT_BOUND");
        assertThat(queued.getStatus()).isEqualTo("QUEUED");
        assertThat(fact.getBusinessState()).isEqualTo("ACTIVE");
    }

    private static MessageDO message(String category, String dedupeKey) {
        MessageDO message = new MessageDO();
        message.setUserId(41L);
        message.setCategory(category);
        message.setSourceType("POSITION_MONITOR");
        message.setSourceId("7");
        message.setPositionId(7L);
        message.setSymbol("BTCUSDT");
        message.setTitle("持仓风险发生变化");
        message.setBody("请人工复核当前持仓。");
        message.setDedupeKey(dedupeKey);
        message.setBusinessState("ACTIVE");
        message.setReadState("UNREAD");
        return message;
    }

    private static MessageDO eligibleOpportunity() {
        MessageDO message = message("HIGH_PERMISSION_OPPORTUNITY", TelegramDedupeKey.create(
                "OPPORTUNITY_READY", "CONFIRMATION", 3, 15,
                41L, "FINAL_PLAN", "plan-1", LocalDateTime.of(2026, 8, 16, 12, 0)));
        message.setSourceType("FINAL_PLAN");
        message.setSourceId("plan-1");
        message.setPlanId("plan-1");
        message.setAnalysisId("analysis-1");
        message.setTraceId("trace-1");
        message.setTitle("【可复核执行计划】");
        message.setBody("BTCUSDT  ·  偏多  ·  确认型\n\n入场：100\n触发：收盘确认\n止损：98\n"
                + "目标：105\n有效至：2026-08-16 13:00 UTC\n\n操作：打开系统重新校验");
        message.setExpiresAt(LocalDateTime.of(2026, 8, 16, 13, 0));
        message.setNotTradeInstruction(true);
        message.setNotOrderExecution(true);
        return message;
    }
}
