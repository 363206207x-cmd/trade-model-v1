package org.example.trademodel.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionProfileSafetyGuardTest {

    @Test
    void rejectsH2MemoryDatasource() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:h2:mem:trade_model_v1;DB_CLOSE_DELAY=-1;MODE=MySQL");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("H2 memory database is not allowed in prod");
    }

    @Test
    void rejectsBlankDatasourcePassword() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("spring.datasource.password", " ");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production datasource password missing");
    }

    @Test
    void rejectsH2ConsoleEnabled() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("spring.h2.console.enabled", "true");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("H2 console must be disabled in prod");
    }

    @Test
    void rejectsSimulatedPositionProvider() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("position.provider.type", "SIMULATED");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("simulated provider is not allowed in prod");
    }

    @Test
    void rejectsBinanceProviderWithoutCredentials() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("binance.api.key", "");
        environment.setProperty("binance.api.secret", " ");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Binance API key missing for production position provider")
                .hasMessageContaining("Binance API secret missing for production position provider");
    }

    @Test
    void rejectsPublicBindUnlessExplicitlyAllowed() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("server.address", "0.0.0.0");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production public server bind requires trade-model.production.allow-public-bind=true");
    }

    @Test
    void allowsSafeExternalDatasourceLookingConfig() {
        MockEnvironment environment = safeEnvironment();

        assertThatCode(() -> ProductionProfileSafetyGuard.validate(environment))
                .doesNotThrowAnyException();
    }

    private MockEnvironment safeEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:postgresql://db.internal/trade_model_v1");
        environment.setProperty("spring.datasource.username", "trade_model_v1");
        environment.setProperty("spring.datasource.password", "configured-password");
        environment.setProperty("spring.h2.console.enabled", "false");
        environment.setProperty("server.address", "127.0.0.1");
        environment.setProperty("trade-model.production.allow-public-bind", "false");
        environment.setProperty("position.provider.type", "BINANCE");
        environment.setProperty("binance.api.key", "configured-key");
        environment.setProperty("binance.api.secret", "configured-secret");
        return environment;
    }
}
