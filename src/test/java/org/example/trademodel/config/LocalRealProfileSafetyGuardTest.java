package org.example.trademodel.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class LocalRealProfileSafetyGuardTest {
    @Test
    void localRealProfileBindsLoopbackOnly() {
        MockEnvironment safe = environment("127.0.0.1");
        assertThatCode(() -> LocalRealProfileSafetyGuard.validate(safe)).doesNotThrowAnyException();

        MockEnvironment unsafe = environment("0.0.0.0");
        assertThatThrownBy(() -> LocalRealProfileSafetyGuard.validate(unsafe))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("loopback");
    }

    @Test
    void localRealRejectsAiOrCoinGlassEnablement() {
        MockEnvironment ai = environment("127.0.0.1").withProperty("trade-model.ai.enabled", "true");
        assertThatThrownBy(() -> LocalRealProfileSafetyGuard.validate(ai)).hasMessageContaining("AI");

        MockEnvironment derivatives = environment("127.0.0.1")
                .withProperty("trade-model.providers.coinglass.enabled", "true");
        assertThatThrownBy(() -> LocalRealProfileSafetyGuard.validate(derivatives)).hasMessageContaining("CoinGlass");
    }

    private static MockEnvironment environment(String address) {
        return new MockEnvironment()
                .withProperty("server.address", address)
                .withProperty("trade-model.auth.enabled", "false")
                .withProperty("trade-model.ai.enabled", "false")
                .withProperty("trade-model.providers.coinglass.enabled", "false");
    }
}
