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
    void rejectsExplicitlyEnabledAiProviderWithoutKeyModelOrBaseUrl() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.ai.openai.enabled", "true");
        environment.setProperty("trade-model.ai.openai.api-key", " ");
        environment.setProperty("trade-model.ai.openai.model", "");
        environment.setProperty("trade-model.ai.openai.base-url", " ");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OpenAI API key missing for explicitly enabled production AI provider")
                .hasMessageContaining("OpenAI model missing for explicitly enabled production AI provider")
                .hasMessageContaining("OpenAI base URL missing for explicitly enabled production AI provider");
    }

    @Test
    void allowsMissingAiKeysWhenAiProvidersAreNotExplicitlyEnabled() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.ai.openai.api-key", "");
        environment.setProperty("trade-model.ai.gemini.api-key", "");
        environment.setProperty("trade-model.ai.xai.api-key", "");

        assertThatCode(() -> ProductionProfileSafetyGuard.validate(environment))
                .doesNotThrowAnyException();
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
    void rejectsMissingAdminCredentials() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.auth.admin-username", "");
        environment.setProperty("trade-model.auth.admin-password", " ");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production admin username missing")
                .hasMessageContaining("production admin password missing");
    }

    @Test
    void rejectsUnsafeAdminPasswordDefaults() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.auth.admin-password", "change-me");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production admin password uses an unsafe default value");
    }

    @Test
    void rejectsSensitiveActuatorEndpointExposure() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("management.endpoints.web.exposure.include", "health,env,beans");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production actuator web exposure must be limited to health");
    }

    @Test
    void rejectsWildcardActuatorEndpointExposure() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("management.endpoints.web.exposure.include", "*");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production actuator web exposure must be limited to health");
    }


    @Test
    void rejectsMissingProductionSchedulerPolicy() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.production.scheduler-policy", " ");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production scheduler policy missing");
    }

    @Test
    void rejectsMissingProductionSchedulerClassification() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.production.scheduler-approval.push-recheck", "");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production scheduler classification missing for push-recheck");
    }

    @Test
    void rejectsLockedDownPolicyWithGlobalSchedulerEnabled() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.schedulers.enabled", "true");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production global scheduler switch must be disabled under LOCKED_DOWN policy");
    }

    @Test
    void rejectsSchedulerEnabledWithoutExplicitOptInClassification() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.production.scheduler-policy", "EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.schedulers.enabled", "true");
        environment.setProperty("trade-model.schedulers.push-recheck.enabled", "true");
        environment.setProperty("trade-model.production.scheduler-approval.push-recheck", "PROD_BLOCKED");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production scheduler enabled without explicit opt-in classification: push-recheck")
                .hasMessageContaining("production scheduler classification blocks enabled scheduler: push-recheck");
    }

    @Test
    void allowsExplicitOptInSchedulerWhenClassificationApproves() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.production.scheduler-policy", "EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.schedulers.enabled", "true");
        environment.setProperty("trade-model.schedulers.push-recheck.enabled", "true");
        environment.setProperty("trade-model.production.scheduler-approval.push-recheck", "PROD_ALLOWED_EXPLICIT_OPT_IN");

        assertThatCode(() -> ProductionProfileSafetyGuard.validate(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProductionPositionMonitorSchedulerEnabled() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.production.scheduler-policy", "EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.schedulers.enabled", "true");
        environment.setProperty("trade-model.schedulers.position-monitor.enabled", "true");
        environment.setProperty("trade-model.production.scheduler-approval.position-monitor", "PROD_ALLOWED_EXPLICIT_OPT_IN");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production position-monitor scheduler must remain default-off");
    }

    @Test
    void rejectsOhlcvSchedulerWithoutExplicitPublicProviderOptIn() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.production.scheduler-policy", "EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.schedulers.enabled", "true");
        environment.setProperty("trade-model.schedulers.ohlcv-ingestion.enabled", "true");
        environment.setProperty("trade-model.schedulers.ohlcv-ingestion.symbols", "BTCUSDT,ETHUSDT");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production OHLCV ingestion requires explicitly enabled public provider")
                .hasMessageContaining("production OHLCV ingestion requires explicit external-call opt-in");
    }

    @Test
    void allowsBoundedExplicitOhlcvSchedulerOptIn() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.production.scheduler-policy", "EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.schedulers.enabled", "true");
        environment.setProperty("trade-model.schedulers.ohlcv-ingestion.enabled", "true");
        environment.setProperty("trade-model.schedulers.ohlcv-ingestion.symbols", "BTCUSDT,ETHUSDT");
        environment.setProperty("trade-model.ohlcv.public-provider.enabled", "true");
        environment.setProperty("trade-model.ohlcv.public-provider.external-calls-enabled", "true");

        assertThatCode(() -> ProductionProfileSafetyGuard.validate(environment)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDisabledOrInvalidRateLimitInProduction() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.security.rate-limit.enabled", "false");
        environment.setProperty("trade-model.security.rate-limit.requests-per-minute", "0");
        environment.setProperty("trade-model.security.rate-limit.window-ms", " ");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production rate limit must be enabled")
                .hasMessageContaining("production rate limit requests-per-minute must be positive")
                .hasMessageContaining("production rate limit window-ms must be positive");
    }

    @Test
    void allowsSafeExternalDatasourceLookingConfig() {
        MockEnvironment environment = safeEnvironment();

        assertThatCode(() -> ProductionProfileSafetyGuard.validate(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProviderScanWithoutExplicitCoordinatorAndExternalOptIn() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.provider-call.scheduler-enabled", "true");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production provider-call features require explicitly enabled coordinator")
                .hasMessageContaining("production provider-call scheduler requires explicit external-call opt-in");
    }

    @Test
    void providerCallProductionSwitchesAreFailClosedByDefault() {
        MockEnvironment environment = safeEnvironment();
        assertThatCode(() -> ProductionProfileSafetyGuard.validate(environment)).doesNotThrowAnyException();
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
        environment.setProperty("trade-model.auth.admin-username", "operator");
        environment.setProperty("trade-model.auth.admin-password", "configured-admin-password");
        environment.setProperty("management.endpoints.web.exposure.include", "health");
        environment.setProperty("trade-model.security.rate-limit.enabled", "true");
        environment.setProperty("trade-model.security.rate-limit.requests-per-minute", "120");
        environment.setProperty("trade-model.security.rate-limit.window-ms", "60000");
        environment.setProperty("trade-model.production.scheduler-policy", "LOCKED_DOWN");
        environment.setProperty("trade-model.schedulers.enabled", "false");
        environment.setProperty("trade-model.schedulers.push-recheck.enabled", "false");
        environment.setProperty("trade-model.schedulers.position-sync.enabled", "false");
        environment.setProperty("trade-model.schedulers.market-data.enabled", "false");
        environment.setProperty("trade-model.schedulers.ohlcv-ingestion.enabled", "false");
        environment.setProperty("trade-model.schedulers.watchlist.enabled", "false");
        environment.setProperty("trade-model.schedulers.position-monitor.enabled", "false");
        environment.setProperty("trade-model.analysis.scheduler.enabled", "false");
        environment.setProperty("trade-model.production.scheduler-approval.push-recheck", "PROD_ALLOWED_EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.production.scheduler-approval.position-sync", "PROD_ALLOWED_EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.production.scheduler-approval.market-data", "PROD_ALLOWED_EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.production.scheduler-approval.ohlcv-ingestion", "PROD_ALLOWED_EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.production.scheduler-approval.watchlist", "LOCAL_ONLY");
        environment.setProperty("trade-model.production.scheduler-approval.position-monitor", "PROD_ALLOWED_DEFAULT_OFF");
        environment.setProperty("trade-model.production.scheduler-approval.analysis", "PROD_ALLOWED_EXPLICIT_OPT_IN");
        environment.setProperty("trade-model.production.scheduler-approval.provider-scan", "PROD_ALLOWED_DEFAULT_OFF");
        environment.setProperty("trade-model.provider-call.enabled", "false");
        environment.setProperty("trade-model.provider-call.scheduler-enabled", "false");
        environment.setProperty("trade-model.provider-call.profile-escalation-enabled", "false");
        environment.setProperty("trade-model.provider-call.auto-escalation-enabled", "false");
        environment.setProperty("trade-model.provider-call.external-calls-enabled", "false");
        environment.setProperty("trade-model.ohlcv.public-provider.enabled", "false");
        environment.setProperty("trade-model.ohlcv.public-provider.external-calls-enabled", "false");
        environment.setProperty("trade-model.schedulers.ohlcv-ingestion.symbols", "");
        environment.setProperty("trade-model.schedulers.ohlcv-ingestion.timeframes", "5m,15m,1h,4h");
        return environment;
    }
}
