package org.example.trademodel.service;

import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.mapper.ChannelDeliveryMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChannelDeliveryService {
    private final ChannelDeliveryMapper mapper;
    private final MessageFactService messageFactService;
    private final Clock clock = Clock.systemUTC();

    public ChannelDeliveryService(ChannelDeliveryMapper mapper, MessageFactService messageFactService) {
        this.mapper = mapper;
        this.messageFactService = messageFactService;
    }

    public ChannelDeliveryDO queueTelegram(Long userId, String messageId, boolean bound) {
        MessageDO message = messageFactService.findForUser(userId, messageId);
        if (message == null) {
            throw new IllegalArgumentException("message not found");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        ChannelDeliveryDO row = new ChannelDeliveryDO();
        row.setDeliveryId("delivery-" + UUID.randomUUID());
        row.setMessageId(messageId);
        row.setUserId(userId);
        row.setChannel("TELEGRAM");
        row.setStatus(bound ? "QUEUED" : "SUPPRESSED");
        row.setAttemptCount(0);
        row.setErrorCode(bound ? null : "TELEGRAM_NOT_BOUND");
        row.setErrorMessage(bound ? null : "Telegram binding is not available");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        mapper.insert(row);
        return row;
    }

    public List<ChannelDeliveryDO> listForMessage(Long userId, String messageId) {
        return mapper.listByMessageForUser(messageId, userId);
    }
}
