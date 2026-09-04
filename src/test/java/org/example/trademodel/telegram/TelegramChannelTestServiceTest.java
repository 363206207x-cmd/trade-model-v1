package org.example.trademodel.telegram;

import org.example.trademodel.entity.TelegramChannelTestAuditDO;
import org.example.trademodel.mapper.TelegramChannelTestAuditMapper;
import org.example.trademodel.service.TelegramChannelTestService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramChannelTestServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-04T04:00:00Z"), ZoneOffset.UTC);

    @Test
    void runtimeServiceFailsClosedWithoutInstallingARealSender() {
        TelegramChannelTestAuditMapper mapper = inMemoryMapper();
        TelegramProperties properties = new TelegramProperties();
        properties.setChannelTestSendEnabled(true);
        TelegramChannelTestService service = new TelegramChannelTestService(mapper, properties);

        TelegramChannelTestService.TestResult result = service.test(41L, "telegram-test:blocked-1");

        assertThat(service.realSenderAvailable()).isFalse();
        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.success()).isFalse();
        assertThat(result.reason()).contains("未调用 Telegram");
        verify(mapper).insert(any(TelegramChannelTestAuditDO.class));
    }

    @Test
    void fakeSenderProvesFixedCopyAndIdempotentReplayWithoutExternalCalls() {
        TelegramChannelTestAuditMapper mapper = inMemoryMapper();
        TelegramProperties properties = new TelegramProperties();
        properties.setChannelTestSendEnabled(true);
        AtomicInteger calls = new AtomicInteger();
        TelegramChannelTestService.TestSender fake = text -> {
            calls.incrementAndGet();
            assertThat(text).isEqualTo(TelegramChannelTestService.TEST_TEXT)
                    .contains("通道测试、非交易指令");
            return new TelegramChannelTestService.TestDeliveryOutcome(
                    true, "501", "trine_test_bot", "owner-fingerprint", 200, null, null);
        };
        TelegramChannelTestService service = new TelegramChannelTestService(mapper, properties, fake, CLOCK);

        TelegramChannelTestService.TestResult first = service.test(41L, "telegram-test:idempotent-1");
        TelegramChannelTestService.TestResult replay = service.test(41L, "telegram-test:idempotent-1");

        assertThat(first.status()).isEqualTo("PASSED");
        assertThat(first.success()).isTrue();
        assertThat(replay.idempotentReplay()).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void recentAttemptRateLimitsBeforeCallingFakeSender() {
        TelegramChannelTestAuditMapper mapper = inMemoryMapper();
        TelegramChannelTestAuditDO recent = new TelegramChannelTestAuditDO();
        recent.setAttemptedAt(java.time.LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).minusSeconds(20));
        when(mapper.selectRecentAttempt(eq(41L), any())).thenReturn(recent);
        TelegramProperties properties = new TelegramProperties();
        properties.setChannelTestSendEnabled(true);
        TelegramChannelTestService.TestSender fake = mock(TelegramChannelTestService.TestSender.class);
        TelegramChannelTestService service = new TelegramChannelTestService(mapper, properties, fake, CLOCK);

        TelegramChannelTestService.TestResult result = service.test(41L, "telegram-test:rate-limit-1");

        assertThat(result.status()).isEqualTo("RATE_LIMITED");
        assertThat(result.reason()).contains("未调用 Telegram");
        verify(fake, never()).send(any());
    }

    private TelegramChannelTestAuditMapper inMemoryMapper() {
        TelegramChannelTestAuditMapper mapper = mock(TelegramChannelTestAuditMapper.class);
        AtomicReference<TelegramChannelTestAuditDO> stored = new AtomicReference<>();
        when(mapper.selectByIdempotencyKey(eq(41L), any())).thenAnswer(invocation -> {
            TelegramChannelTestAuditDO row = stored.get();
            return row != null && row.getIdempotencyKey().equals(invocation.getArgument(1)) ? row : null;
        });
        when(mapper.insert(any())).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return 1;
        });
        when(mapper.updateOutcome(any())).thenReturn(1);
        return mapper;
    }
}
