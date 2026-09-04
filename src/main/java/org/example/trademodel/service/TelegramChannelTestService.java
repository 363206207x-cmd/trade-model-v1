package org.example.trademodel.service;

import org.example.trademodel.entity.TelegramChannelTestAuditDO;
import org.example.trademodel.mapper.TelegramChannelTestAuditMapper;
import org.example.trademodel.telegram.TelegramProperties;
import org.example.trademodel.telegram.TelegramSecretSanitizer;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Owner-scoped idempotency, rate-limit and audit boundary for channel tests.
 * The runtime bean is deliberately fail-closed: this task does not install a real sender.
 */
@Service
public class TelegramChannelTestService {
    public static final String TEST_TEXT = "TRINE LOGIC 通道测试、非交易指令。仅用于验证 Owner Telegram 通知通道。";
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9:._-]{8,128}");
    private static final int RATE_LIMIT_SECONDS = 60;

    private final TelegramChannelTestAuditMapper mapper;
    private final TelegramProperties properties;
    private final TestSender sender;
    private Clock clock;

    @Autowired
    public TelegramChannelTestService(TelegramChannelTestAuditMapper mapper,
                                      TelegramProperties properties) {
        this(mapper, properties, null, Clock.systemUTC());
    }

    public TelegramChannelTestService(TelegramChannelTestAuditMapper mapper,
                                      TelegramProperties properties,
                                      TestSender sender,
                                      Clock clock) {
        this.mapper = mapper;
        this.properties = properties;
        this.sender = sender;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public boolean realSenderAvailable() {
        return false;
    }

    public TelegramChannelTestAuditDO latest(Long userId) {
        return mapper.selectLatestForUser(requireUser(userId));
    }

    public synchronized TestResult test(Long userId, String idempotencyKey) {
        Long ownerId = requireUser(userId);
        String key = normalizeKey(idempotencyKey);
        TelegramChannelTestAuditDO existing = mapper.selectByIdempotencyKey(ownerId, key);
        if (existing != null) return view(existing, true);

        LocalDateTime now = LocalDateTime.now(clock);
        TelegramChannelTestAuditDO row = base(ownerId, key, now);
        TelegramChannelTestAuditDO recent = mapper.selectRecentAttempt(ownerId, now.minusSeconds(RATE_LIMIT_SECONDS));
        if (recent != null) {
            complete(row, "RATE_LIMITED", "CHANNEL_TEST_RATE_LIMITED",
                    "请等待 60 秒后再试；本次未调用 Telegram。", now, null);
            insert(row);
            return view(mapper.selectByIdempotencyKey(ownerId, key), false);
        }
        if (!properties.isChannelTestSendEnabled() || sender == null) {
            complete(row, "BLOCKED", "CHANNEL_TEST_SEND_DISABLED",
                    "真实测试发送门禁未开启；本次未调用 Telegram。", now, null);
            insert(row);
            return view(mapper.selectByIdempotencyKey(ownerId, key), false);
        }

        row.setAttemptedAt(now);
        row.setStatus("PENDING");
        if (!insert(row)) return view(mapper.selectByIdempotencyKey(ownerId, key), true);
        TestDeliveryOutcome outcome;
        try {
            outcome = sender.send(TEST_TEXT);
        } catch (RuntimeException unexpected) {
            outcome = new TestDeliveryOutcome(false, null, null, null, 0,
                    "PROVIDER_UNAVAILABLE", "Telegram provider call failed");
        }
        if (outcome == null) {
            outcome = new TestDeliveryOutcome(false, null, null, null, 0,
                    "PROVIDER_UNAVAILABLE", "Telegram provider returned no result");
        }
        String status = outcome.success() ? "PASSED" : "FAILED";
        complete(row, status, outcome.success() ? null : outcome.errorCode(),
                outcome.success() ? "通道测试已送达；非交易指令。" : outcome.errorMessage(),
                LocalDateTime.now(clock), outcome);
        mapper.updateOutcome(row);
        return view(row, false);
    }

    private TelegramChannelTestAuditDO base(Long userId, String key, LocalDateTime now) {
        TelegramChannelTestAuditDO row = new TelegramChannelTestAuditDO();
        row.setTestId("telegram-test-" + UUID.randomUUID());
        row.setUserId(userId);
        row.setIdempotencyKey(key);
        row.setStatus("PENDING");
        row.setRequestedAt(now);
        row.setNotTradeInstruction(true);
        row.setNotOrderExecution(true);
        return row;
    }

    private boolean insert(TelegramChannelTestAuditDO row) {
        try {
            return mapper.insert(row) == 1;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    private void complete(TelegramChannelTestAuditDO row, String status, String code,
                          String message, LocalDateTime completedAt, TestDeliveryOutcome outcome) {
        row.setStatus(status);
        row.setCompletedAt(completedAt);
        row.setErrorCode(code);
        row.setErrorMessage(TelegramSecretSanitizer.sanitize(message, properties));
        if (outcome != null) {
            row.setProviderReference(outcome.providerReference());
            row.setBotUsername(outcome.botUsername());
            row.setRecipientFingerprint(outcome.recipientFingerprint());
            row.setResponseCode(outcome.responseCode());
        }
    }

    private TestResult view(TelegramChannelTestAuditDO row, boolean replay) {
        if (row == null) throw new IllegalStateException("Telegram channel test audit was not persisted");
        return new TestResult(row.getTestId(), row.getStatus(), "PASSED".equals(row.getStatus()),
                replay, row.getErrorMessage(), row.getRequestedAt(), row.getAttemptedAt(), row.getCompletedAt());
    }

    private static Long requireUser(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("Authenticated owner is required");
        return userId;
    }

    private static String normalizeKey(String value) {
        String key = value == null ? "" : value.trim();
        if (!IDEMPOTENCY_KEY.matcher(key).matches()) throw new IllegalArgumentException("Valid idempotencyKey is required");
        return key;
    }

    public interface TestSender {
        TestDeliveryOutcome send(String fixedText);
    }

    public record TestDeliveryOutcome(boolean success, String providerReference, String botUsername,
                                      String recipientFingerprint, Integer responseCode,
                                      String errorCode, String errorMessage) { }

    public record TestResult(String testId, String status, boolean success, boolean idempotentReplay,
                             String reason, LocalDateTime requestedAt, LocalDateTime attemptedAt,
                             LocalDateTime completedAt) { }
}
