package org.example.trademodel.telegram;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramReadinessServiceTest {

    @Test
    void readinessIsFailClosedUntilConfigurationAndProviderValidationSucceed() {
        TelegramProperties properties = new TelegramProperties();
        TelegramReadinessService service = new TelegramReadinessService(properties);
        assertThat(service.state()).isEqualTo(TelegramReadinessState.NOT_CONFIGURED);

        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        assertThat(service.state()).isEqualTo(TelegramReadinessState.TOKEN_MISSING);

        properties.setBotToken("TEST_TOKEN");
        assertThat(service.state()).isEqualTo(TelegramReadinessState.CHAT_ID_MISSING);

        properties.setChatId("TEST_CHAT_ID");
        assertThat(service.state()).isEqualTo(TelegramReadinessState.DEGRADED);
        assertThat(service.canAttemptDelivery()).isTrue();

        service.observe(TelegramClientResult.success(200, null, "test_bot"));
        assertThat(service.state()).isEqualTo(TelegramReadinessState.READY);
        assertThat(service.botUsername()).isEqualTo("test_bot");

        service.observe(TelegramClientResult.failure(
                401, TelegramReadinessState.AUTH_FAILED, "AUTH_FAILED", "rejected", null, false));
        assertThat(service.state()).isEqualTo(TelegramReadinessState.AUTH_FAILED);
        assertThat(service.botUsername()).isNull();
    }
}
