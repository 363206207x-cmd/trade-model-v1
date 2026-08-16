package org.example.trademodel.service;

import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.message.MessageRecordedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MessageFactService {
    private static final Set<String> CATEGORIES = Set.of(
            "HIGH_PERMISSION_OPPORTUNITY",
            "OPPORTUNITY_PLAN_SAFETY_CHANGE",
            "POSITION_LOGIC_RISK_CHANGE");

    private final MessageMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock = Clock.systemUTC();

    public MessageFactService(MessageMapper mapper) {
        this(mapper, null);
    }

    @Autowired
    public MessageFactService(MessageMapper mapper, ApplicationEventPublisher eventPublisher) {
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MessageDO record(MessageDO input) {
        if (input == null || input.getUserId() == null || input.getUserId() <= 0) {
            throw new IllegalArgumentException("message userId is required");
        }
        require(input.getSourceType(), "sourceType");
        require(input.getSourceId(), "sourceId");
        require(input.getTitle(), "title");
        require(input.getDedupeKey(), "dedupeKey");
        if (!CATEGORIES.contains(require(input.getCategory(), "category"))) {
            throw new IllegalArgumentException("unsupported message category");
        }
        if (mapper.countByDedupeKey(input.getUserId(), input.getDedupeKey()) > 0) {
            throw new IllegalStateException("message already exists for dedupeKey");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        input.setMessageId(hasText(input.getMessageId()) ? input.getMessageId() : "message-" + UUID.randomUUID());
        input.setBusinessState(hasText(input.getBusinessState()) ? input.getBusinessState() : "ACTIVE");
        input.setReadState(hasText(input.getReadState()) ? input.getReadState() : "UNREAD");
        input.setCreatedAt(input.getCreatedAt() == null ? now : input.getCreatedAt());
        input.setUpdatedAt(now);
        input.setNotTradeInstruction(true);
        input.setNotOrderExecution(true);
        if (mapper.insert(input) != 1) {
            throw new IllegalStateException("message persistence failed");
        }
        publish(input);
        return input;
    }

    @Transactional
    public MessageDO recordIfAbsent(MessageDO input) {
        validateIdentity(input);
        MessageDO existing = mapper.selectByDedupeKey(input.getUserId(), input.getDedupeKey());
        if (existing != null) return existing;
        try {
            return record(input);
        } catch (DataIntegrityViolationException | IllegalStateException duplicate) {
            existing = mapper.selectByDedupeKey(input.getUserId(), input.getDedupeKey());
            if (existing != null) return existing;
            throw duplicate;
        }
    }

    public List<MessageDO> listForUser(Long userId, int limit) {
        requireUser(userId);
        return mapper.listActiveForUser(userId, LocalDateTime.now(clock), Math.max(1, Math.min(limit, 100)));
    }

    public MessageDO findForUser(Long userId, String messageId) {
        requireUser(userId);
        return mapper.selectByIdForUser(require(messageId, "messageId"), userId);
    }

    public boolean markRead(Long userId, String messageId) {
        requireUser(userId);
        return mapper.markRead(require(messageId, "messageId"), userId, LocalDateTime.now(clock)) == 1;
    }

    private void publish(MessageDO message) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new MessageRecordedEvent(
                    message.getMessageId(), message.getUserId(), message.getDedupeKey()));
        }
    }

    private static void validateIdentity(MessageDO input) {
        if (input == null || input.getUserId() == null || input.getUserId() <= 0) {
            throw new IllegalArgumentException("message userId is required");
        }
        require(input.getDedupeKey(), "dedupeKey");
    }

    private static void requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private static String require(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
