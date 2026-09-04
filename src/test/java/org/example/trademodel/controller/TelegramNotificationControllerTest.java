package org.example.trademodel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.ChannelDeliveryService;
import org.example.trademodel.service.TelegramChannelTestService;
import org.example.trademodel.telegram.TelegramProperties;
import org.example.trademodel.telegram.TelegramReadinessService;
import org.example.trademodel.telegram.TelegramReadinessState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramNotificationControllerTest {
    @Test
    void authenticatedStatusIsOperationalOnlyAndNeverReturnsRecipientOrToken() throws Exception {
        AuthenticatedUserIdResolver resolver = mock(AuthenticatedUserIdResolver.class);
        ChannelDeliveryService deliveryService = mock(ChannelDeliveryService.class);
        TelegramReadinessService readiness = mock(TelegramReadinessService.class);
        TelegramChannelTestService channelTestService = mock(TelegramChannelTestService.class);
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setBotToken("TEST_TOKEN");
        properties.setChatId("TEST_CHAT_ID");
        ChannelDeliveryDO latest = new ChannelDeliveryDO();
        latest.setStatus("RETRYING");
        latest.setDeliveredAt(LocalDateTime.of(2026, 8, 16, 12, 0));
        latest.setErrorCode("RATE_LIMITED");
        when(resolver.requireCurrentUserId()).thenReturn(41L);
        when(readiness.state()).thenReturn(TelegramReadinessState.RATE_LIMITED);
        when(readiness.botUsername()).thenReturn(null);
        when(readiness.recipientConfigured()).thenReturn(true);
        when(readiness.reasonCode()).thenReturn("RATE_LIMITED");
        when(deliveryService.latestTelegramForUser(41L)).thenReturn(latest);
        when(deliveryService.retryingCountForUser(41L)).thenReturn(1);

        TelegramNotificationController controller =
                new TelegramNotificationController(resolver, properties, readiness, deliveryService, channelTestService);
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(controller.status());

        assertThat(json).contains(
                "\"enabled\":true", "\"configured\":true", "\"externalCallsEnabled\":true",
                "\"state\":\"RATE_LIMITED\"", "\"recipientConfigured\":true",
                "\"lastDeliveryState\":\"RETRYING\"", "\"lastErrorCode\":\"RATE_LIMITED\"",
                "\"retryingCount\":1", "\"maskedChatIdentity\":\"sha256:",
                "\"testSendEnabled\":false");
        assertThat(json).doesNotContain("TEST_TOKEN", "TEST_CHAT_ID", "botToken", "chatId",
                "apiBaseUrl", "publicBaseUrl", "telegram.env");
    }

    @Test
    void manualRetryIsOwnerScopedAndOnlyRequeuesExistingDeliveryFact() throws Exception {
        AuthenticatedUserIdResolver resolver = mock(AuthenticatedUserIdResolver.class);
        ChannelDeliveryService deliveryService = mock(ChannelDeliveryService.class);
        TelegramReadinessService readiness = mock(TelegramReadinessService.class);
        TelegramChannelTestService channelTestService = mock(TelegramChannelTestService.class);
        when(resolver.requireCurrentUserId()).thenReturn(41L);
        when(deliveryService.requeueTelegramForMessage(41L, "message-9")).thenReturn(true);
        TelegramNotificationController controller = new TelegramNotificationController(
                resolver, new TelegramProperties(), readiness, deliveryService, channelTestService);

        String json = new ObjectMapper().findAndRegisterModules()
                .writeValueAsString(controller.retry("message-9"));

        assertThat(json).contains("\"messageId\":\"message-9\"", "\"requeued\":true",
                "\"state\":\"QUEUED\"");
        verify(deliveryService).requeueTelegramForMessage(41L, "message-9");
    }

    @Test
    void currentProductUiExposesOwnerOnlyChannelStatusWithoutSecretsOrAutomaticSend() throws Exception {
        String currentUi = Files.readString(Path.of("src/main/resources/templates/home.html"))
                + Files.readString(Path.of("src/main/resources/templates/workspace.html"))
                + Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"))
                + Files.readString(Path.of("src/main/resources/static/js/workspace.js"));

        assertThat(currentUi).contains(
                "Telegram 通知", "telegramChannelStatus", "overlay-telegram-channel-test", "O10",
                "真实测试发送门禁未开启", "通道测试、非交易指令")
                .doesNotContain("botToken", "chatId", "TELEGRAM_BOT_TOKEN", "TELEGRAM_CHAT_ID",
                        "data-retry-telegram");
    }
}
