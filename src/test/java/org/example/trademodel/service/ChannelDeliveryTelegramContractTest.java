package org.example.trademodel.service;

import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.mapper.ChannelDeliveryMapper;
import org.example.trademodel.telegram.TelegramDedupeKey;
import org.example.trademodel.telegram.TelegramProperties;
import org.example.trademodel.telegram.TelegramReadinessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelDeliveryTelegramContractTest {
    @Mock private ChannelDeliveryMapper mapper;
    @Mock private MessageFactService messageFactService;
    @Mock private TelegramReadinessService readinessService;
    @Mock private UserConfigService userConfigService;

    private TelegramProperties properties;
    private ChannelDeliveryService service;

    @BeforeEach
    void setUp() {
        properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setBotToken("TEST_TOKEN");
        properties.setChatId("TEST_CHAT_ID");
        properties.setCooldownMinutes(15);
        org.mockito.Mockito.lenient().when(mapper.insert(any())).thenReturn(1);
        service = new ChannelDeliveryService(
                mapper, messageFactService, properties, readinessService, userConfigService);
    }

    @Test
    void sameMessageAndChannelAlwaysReuseExistingDeliveryFact() {
        ChannelDeliveryDO existing = new ChannelDeliveryDO();
        existing.setDeliveryId("delivery-existing");
        existing.setMessageId("message-1");
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message(2));
        when(mapper.selectByMessageAndChannel("message-1", "TELEGRAM")).thenReturn(existing);

        assertThat(service.queueTelegram(41L, "message-1")).isSameAs(existing);
        verify(mapper, never()).insert(any());
    }

    @Test
    void cooldownSuppressesSameOrLowerSeverityWithoutDeletingEvidence() {
        MessageDO message = message(2);
        ChannelDeliveryDO recent = new ChannelDeliveryDO();
        recent.setSeverityRank(2);
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message);
        when(mapper.selectRecentActiveCooldown(anyLong(), anyString(), any(LocalDateTime.class)))
                .thenReturn(recent);
        when(userConfigService.getUserConfig("41")).thenReturn(config(30));

        ChannelDeliveryDO result = service.queueTelegram(41L, "message-1");

        assertThat(result.getStatus()).isEqualTo("SUPPRESSED");
        assertThat(result.getErrorCode()).isEqualTo("DUPLICATE_OR_COOLDOWN");
        assertThat(result.getNextAttemptAt()).isNull();
        verify(mapper).insert(result);
    }

    @Test
    void severityEscalationCreatesANewQueuedAlertInsideCooldown() {
        MessageDO message = message(4);
        ChannelDeliveryDO recent = new ChannelDeliveryDO();
        recent.setSeverityRank(2);
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message);
        when(mapper.selectRecentActiveCooldown(anyLong(), anyString(), any(LocalDateTime.class)))
                .thenReturn(recent);
        when(readinessService.canAttemptDelivery()).thenReturn(true);

        ChannelDeliveryDO result = service.queueTelegram(41L, "message-1");

        assertThat(result.getStatus()).isEqualTo("QUEUED");
        assertThat(result.getNextAttemptAt()).isNotNull();
        assertThat(result.getSeverityRank()).isEqualTo(4);
        assertThat(result.getRecipientFingerprint()).isNotBlank().doesNotContain("TEST_CHAT_ID");
    }

    @Test
    void missingConfigurationCreatesDurableNotConfiguredStateRatherThanFakeSent() {
        when(readinessService.canAttemptDelivery()).thenReturn(false);
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message(2));

        ChannelDeliveryDO result = service.queueTelegram(41L, "message-1");

        assertThat(result.getStatus()).isEqualTo("NOT_CONFIGURED");
        assertThat(result.getProviderReference()).isNull();
        assertThat(result.getDeliveredAt()).isNull();
    }

    @Test
    void expiredSendingClaimsAreRecoveredAndClaimIsAtomic() {
        ChannelDeliveryDO due = new ChannelDeliveryDO();
        due.setDeliveryId("delivery-due");
        ChannelDeliveryDO claimed = new ChannelDeliveryDO();
        claimed.setDeliveryId("delivery-due");
        claimed.setStatus("SENDING");
        when(mapper.recoverExpiredClaims(any(LocalDateTime.class))).thenReturn(2);
        when(mapper.listDue(any(LocalDateTime.class), anyInt())).thenReturn(java.util.List.of(due));
        when(mapper.claim(anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(mapper.selectById("delivery-due")).thenReturn(claimed);

        assertThat(service.recoverExpiredClaims()).isEqualTo(2);
        assertThat(service.claimDue(20)).containsExactly(claimed);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).claim(anyString(), token.capture(), any(LocalDateTime.class), any(LocalDateTime.class));
        assertThat(token.getValue()).isNotBlank();
    }

    @Test
    void failedOrNotConfiguredDeliveryCanBeManuallyRequeuedForItsOwner() {
        ChannelDeliveryDO delivery = new ChannelDeliveryDO();
        delivery.setDeliveryId("delivery-1");
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message(2));
        when(mapper.selectByMessageAndChannel("message-1", "TELEGRAM")).thenReturn(delivery);
        when(mapper.requeue(anyString(), anyLong(), any(LocalDateTime.class))).thenReturn(1);

        assertThat(service.requeueTelegramForMessage(41L, "message-1")).isTrue();

        verify(mapper).requeue(org.mockito.ArgumentMatchers.eq("delivery-1"),
                org.mockito.ArgumentMatchers.eq(41L), any(LocalDateTime.class));
    }

    private static MessageDO message(int severity) {
        MessageDO message = new MessageDO();
        message.setMessageId("message-1");
        message.setUserId(41L);
        message.setCategory("POSITION_LOGIC_RISK_CHANGE");
        message.setSourceType("USER_POSITION");
        message.setSourceId("91");
        message.setDedupeKey(TelegramDedupeKey.create(
                "POSITION_RISK_CHANGE", severity >= 4 ? "EXTREME" : "HIGH", severity, 15,
                41L, "USER_POSITION", "91", LocalDateTime.of(2026, 8, 16, 12, 0)));
        return message;
    }

    private static UserConfigDO config(int cooldown) {
        UserConfigDO config = new UserConfigDO();
        config.setCooldownMinutes(cooldown);
        return config;
    }
}
