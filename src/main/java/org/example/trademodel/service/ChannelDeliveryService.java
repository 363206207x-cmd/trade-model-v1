package org.example.trademodel.service;

import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.mapper.ChannelDeliveryMapper;
import org.example.trademodel.service.UserConfigService;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.telegram.TelegramDedupeKey;
import org.example.trademodel.telegram.TelegramDeliveryStatus;
import org.example.trademodel.telegram.TelegramProperties;
import org.example.trademodel.telegram.TelegramReadinessService;
import org.example.trademodel.telegram.TelegramSecretSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChannelDeliveryService {
    private final ChannelDeliveryMapper mapper;
    private final MessageFactService messageFactService;
    private final TelegramProperties telegramProperties;
    private final TelegramReadinessService readinessService;
    private final UserConfigService userConfigService;
    private final Clock clock = Clock.systemUTC();

    public ChannelDeliveryService(ChannelDeliveryMapper mapper, MessageFactService messageFactService) {
        this(mapper, messageFactService, new TelegramProperties(), null, null);
    }

    @Autowired
    public ChannelDeliveryService(ChannelDeliveryMapper mapper,
                                  MessageFactService messageFactService,
                                  TelegramProperties telegramProperties,
                                  TelegramReadinessService readinessService,
                                  UserConfigService userConfigService) {
        this.mapper = mapper;
        this.messageFactService = messageFactService;
        this.telegramProperties = telegramProperties;
        this.readinessService = readinessService;
        this.userConfigService = userConfigService;
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
        row.setNextAttemptAt(bound ? now : null);
        row.setErrorCode(bound ? null : "TELEGRAM_NOT_BOUND");
        row.setErrorMessage(bound ? null : "Telegram binding is not available");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        if (mapper.insert(row) != 1) throw new IllegalStateException("channel delivery persistence failed");
        return row;
    }

    public ChannelDeliveryDO queueTelegram(Long userId, String messageId) {
        MessageDO message = messageFactService.findForUser(userId, messageId);
        if (message == null) throw new IllegalArgumentException("message not found");
        ChannelDeliveryDO existing = mapper.selectByMessageAndChannel(messageId, "TELEGRAM");
        if (existing != null) return existing;

        LocalDateTime now = LocalDateTime.now(clock);
        int severity = TelegramDedupeKey.severity(message.getDedupeKey());
        String cooldownKey = message.getCategory() + "|" + message.getSourceType() + "|"
                + message.getSourceId() + "|" + TelegramDedupeKey.eventType(message.getDedupeKey());
        int cooldownMinutes = cooldownMinutes(userId);
        ChannelDeliveryDO recent = mapper.selectRecentActiveCooldown(
                userId, cooldownKey, now.minusMinutes(cooldownMinutes));

        ChannelDeliveryDO row = baseRow(message, now, severity, cooldownKey);
        if (recent != null && safeSeverity(recent) >= severity) {
            row.setStatus(TelegramDeliveryStatus.SUPPRESSED.name());
            row.setErrorCode("DUPLICATE_OR_COOLDOWN");
            row.setErrorMessage("Equivalent Telegram alert is within the configured cooldown window");
        } else if (readinessService == null || !readinessService.canAttemptDelivery()) {
            row.setStatus(TelegramDeliveryStatus.NOT_CONFIGURED.name());
            row.setErrorCode(readinessService == null || readinessService.state() == null
                    ? "NOT_CONFIGURED" : readinessService.state().name());
            row.setErrorMessage("Telegram delivery is not configured");
        } else {
            row.setStatus(TelegramDeliveryStatus.QUEUED.name());
            row.setNextAttemptAt(now);
        }
        try {
            if (mapper.insert(row) != 1) throw new IllegalStateException("channel delivery persistence failed");
            return row;
        } catch (DataIntegrityViolationException duplicate) {
            existing = mapper.selectByMessageAndChannel(messageId, "TELEGRAM");
            if (existing != null) return existing;
            throw duplicate;
        }
    }

    public List<ChannelDeliveryDO> listForMessage(Long userId, String messageId) {
        return mapper.listByMessageForUser(messageId, userId);
    }

    public ChannelDeliveryDO latestForMessage(String messageId) {
        return mapper.selectByMessageAndChannel(messageId, "TELEGRAM");
    }

    public ChannelDeliveryDO latestTelegramForUser(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId is required");
        return mapper.selectLatestTelegramForUser(userId);
    }

    public int retryingCountForUser(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId is required");
        return mapper.countRetryingForUser(userId);
    }

    public List<ChannelDeliveryDO> claimDue(int limit) {
        LocalDateTime now = LocalDateTime.now(clock);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return mapper.listDue(now, safeLimit).stream().map(candidate -> {
            String token = UUID.randomUUID().toString();
            int claimed = mapper.claim(candidate.getDeliveryId(), token, now,
                    now.plusSeconds(telegramProperties.getClaimLeaseSeconds()));
            return claimed == 1 ? mapper.selectById(candidate.getDeliveryId()) : null;
        }).filter(java.util.Objects::nonNull).toList();
    }

    public int recoverExpiredClaims() {
        return mapper.recoverExpiredClaims(LocalDateTime.now(clock));
    }

    public boolean completeClaim(ChannelDeliveryDO delivery) {
        delivery.setUpdatedAt(LocalDateTime.now(clock));
        return mapper.completeClaim(delivery) == 1;
    }

    public boolean requeue(Long userId, String deliveryId) {
        return mapper.requeue(deliveryId, userId, LocalDateTime.now(clock)) == 1;
    }

    public boolean requeueTelegramForMessage(Long userId, String messageId) {
        MessageDO message = messageFactService.findForUser(userId, messageId);
        if (message == null) throw new IllegalArgumentException("message not found");
        ChannelDeliveryDO delivery = mapper.selectByMessageAndChannel(messageId, "TELEGRAM");
        return delivery != null && requeue(userId, delivery.getDeliveryId());
    }

    private ChannelDeliveryDO baseRow(MessageDO message, LocalDateTime now, int severity, String cooldownKey) {
        ChannelDeliveryDO row = new ChannelDeliveryDO();
        row.setDeliveryId("delivery-" + UUID.randomUUID());
        row.setMessageId(message.getMessageId());
        row.setUserId(message.getUserId());
        row.setChannel("TELEGRAM");
        row.setAttemptCount(0);
        row.setRecipientFingerprint(TelegramSecretSanitizer.recipientFingerprint(telegramProperties.getChatId()));
        row.setCooldownKey(cooldownKey);
        row.setSeverityRank(severity);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }

    private int cooldownMinutes(Long userId) {
        if (userConfigService != null) {
            try {
                UserConfigDO config = userConfigService.getUserConfig(String.valueOf(userId));
                if (config != null && config.getCooldownMinutes() != null && config.getCooldownMinutes() > 0) {
                    return config.getCooldownMinutes();
                }
            } catch (RuntimeException ignored) {
                // Delivery policy remains available with the centrally configured cooldown.
            }
        }
        return telegramProperties.getCooldownMinutes();
    }

    private static int safeSeverity(ChannelDeliveryDO row) {
        return row.getSeverityRank() == null ? 0 : row.getSeverityRank();
    }
}
