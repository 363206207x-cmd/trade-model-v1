package org.example.trademodel.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.security.PersonalUserBootstrap;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
        environment.setProperty("trade-model.ai.openai.gpt-final.fast-model", "");
        environment.setProperty("trade-model.ai.openai.gpt-final.reasoning-model", "");
        environment.setProperty("trade-model.ai.openai.gpt-final.fallback-models[0]", "");
        environment.setProperty("trade-model.ai.openai.gpt-final.fallback-models[1]", "");
        environment.setProperty("trade-model.ai.openai.base-url", " ");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OpenAI API key missing for explicitly enabled production AI provider")
                .hasMessageContaining("OpenAI GPT_FINAL model routing must stay within approved GPT-5.6/5.5/5.4 models")
                .hasMessageContaining("OpenAI base URL missing for explicitly enabled production AI provider");
    }

    @Test
    void rejectsOpenAiRoutingBelowGpt54() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.ai.openai.enabled", "true");
        environment.setProperty("trade-model.ai.openai.api-key", "configured-test-key");
        environment.setProperty("trade-model.ai.openai.base-url", "https://api.openai.test");
        environment.setProperty("trade-model.ai.openai.gpt-final.fast-model", "gpt-5.6-luna");
        environment.setProperty("trade-model.ai.openai.gpt-final.reasoning-model", "gpt-5.6-sol");
        environment.setProperty("trade-model.ai.openai.gpt-final.fallback-models[0]", "gpt-5.5");
        environment.setProperty("trade-model.ai.openai.gpt-final.fallback-models[1]", "gpt-4.1-mini");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OpenAI GPT_FINAL model routing must stay within approved GPT-5.6/5.5/5.4 models");
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
        environment.setProperty("trade-model.auth.initial-username", "");
        environment.setProperty("trade-model.auth.initial-password", " ");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production initial username missing")
                .hasMessageContaining("production initial password missing");
    }

    @Test
    void rejectsUnsafeAdminPasswordDefaults() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.auth.initial-password", "change-me");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production initial password uses an unsafe default value");
    }

    @Test
    void copiedEnvExampleFailsProductionSafetyGuard() throws Exception {
        String templateValue = Files.readAllLines(Path.of(".env.example")).stream()
                .filter(line -> line.startsWith("TRADE_MODEL_INITIAL_PASSWORD="))
                .map(line -> line.substring(line.indexOf('=') + 1))
                .findFirst()
                .orElseThrow();
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.auth.initial-password", templateValue);

        assertThat(templateValue).isEmpty();
        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production initial password missing");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "replace-with-long-local-secret",
            "replace-with-your-secret",
            "REPLACE-WITH-YOUR-SECRET"
    })
    void replaceWithPrefixIsRejected(String password) {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.auth.initial-password", password);

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production initial password uses an unsafe default value");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "placeholder-password",
            "placeholder",
            "example-password",
            "sample-password",
            "default-password",
            "secret-password",
            "long-local-secret",
            "change-me",
            "change-this-password",
            "dev-local-password",
            "<long-local-secret>"
    })
    void placeholderValuesAreRejected(String password) {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.auth.initial-password", password);

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production initial password uses an unsafe default value");
    }

    @Test
    void validStrongPasswordStillPasses() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.auth.initial-password", "A9!real-operator-secret-2026");

        assertThatCode(() -> ProductionProfileSafetyGuard.validate(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void bootstrapAndProductionGuardUseConsistentPolicy() {
        String unsafePassword = "replace-with-long-local-secret";
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.auth.initial-password", unsafePassword);
        PersonalUserBootstrap bootstrap = new PersonalUserBootstrap(
                true, "operator", unsafePassword, mock(PersonalUserMapper.class),
                new BCryptPasswordEncoder());

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsafe default value");
        assertThatThrownBy(() -> bootstrap.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsafe");
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

    @Test
    void rejectsCoinGlassExternalCallsWithoutExplicitProviderEnablement() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.providers.coinglass.external-calls-enabled", "true");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production CoinGlass external calls require explicitly enabled provider");
    }

    @Test
    void rejectsEnabledCoinGlassWithoutSafeCredentialsAndCoordinatorOptIn() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.providers.coinglass.enabled", "true");
        environment.setProperty("trade-model.providers.coinglass.external-calls-enabled", "true");
        environment.setProperty("trade-model.providers.coinglass.api-key", "");
        environment.setProperty("trade-model.providers.coinglass.base-url", "http://invalid.example");
        environment.setProperty("trade-model.providers.coinglass.auth-header-name", "Authorization");
        environment.setProperty("trade-model.providers.coinglass.advertised-rpm", "0");
        environment.setProperty("trade-model.providers.coinglass.internal-budget-ratio", "1");

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production CoinGlass requires explicitly enabled provider coordinator")
                .hasMessageContaining("production CoinGlass API key missing")
                .hasMessageContaining("production CoinGlass base URL must be valid HTTPS")
                .hasMessageContaining("production CoinGlass v4 auth header must be CG-API-KEY")
                .hasMessageContaining("production CoinGlass advertised-rpm must be positive")
                .hasMessageContaining("production CoinGlass internal-budget-ratio must be between 0 and 1");
    }

    @Test
    void allowsExplicitFailClosedCoinGlassConfiguration() {
        MockEnvironment environment = safeEnvironment();
        environment.setProperty("trade-model.providers.coinglass.enabled", "true");
        environment.setProperty("trade-model.providers.coinglass.external-calls-enabled", "true");
        environment.setProperty("trade-model.providers.coinglass.api-key", "configured-key");
        environment.setProperty("trade-model.providers.coinglass.base-url", "https://open-api-v4.coinglass.com");
        environment.setProperty("trade-model.providers.coinglass.auth-header-name", "CG-API-KEY");
        environment.setProperty("trade-model.providers.coinglass.advertised-rpm", "300");
        environment.setProperty("trade-model.providers.coinglass.internal-budget-ratio", "0.8");
        environment.setProperty("trade-model.provider-call.enabled", "true");
        environment.setProperty("trade-model.provider-call.external-calls-enabled", "true");

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
        environment.setProperty("trade-model.auth.enabled", "true");
        environment.setProperty("trade-model.auth.initial-username", "operator");
        environment.setProperty("trade-model.auth.initial-password", "configured-initial-password");
        environment.setProperty("server.servlet.session.cookie.http-only", "true");
        environment.setProperty("server.servlet.session.cookie.same-site", "lax");
        environment.setProperty("server.servlet.session.cookie.secure", "true");
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
        environment.setProperty("trade-model.providers.coinglass.enabled", "false");
        environment.setProperty("trade-model.providers.coinglass.external-calls-enabled", "false");
        environment.setProperty("trade-model.schedulers.ohlcv-ingestion.symbols", "");
        environment.setProperty("trade-model.schedulers.ohlcv-ingestion.timeframes", "5m,15m,1h,4h");
        return environment;
    }
}
