package org.example.trademodel.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControlledStagingConfigTreeSecretInjectionTest {

    private static final String DATABASE_SECRET = "p3h-database-secret-fixture";
    private static final String ADMIN_SECRET = "p3h-admin-secret-fixture";
    private static final String BINANCE_KEY = "p3h-nonfunctional-key-fixture";
    private static final String BINANCE_SECRET = "p3h-nonfunctional-secret-fixture";

    @TempDir
    Path tempDir;

    @Test
    void configTreeInjectsRequiredSecretsAndProductionGuardAcceptsThem() throws Exception {
        StandardEnvironment environment = loadConfigTree(Map.of(
                "spring.datasource.password", DATABASE_SECRET,
                "trade-model.auth.initial-password", ADMIN_SECRET,
                "binance.api.key", BINANCE_KEY,
                "binance.api.secret", BINANCE_SECRET));

        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo(DATABASE_SECRET);
        assertThat(environment.getProperty("trade-model.auth.initial-password")).isEqualTo(ADMIN_SECRET);
        assertThat(environment.getProperty("binance.api.key")).isEqualTo(BINANCE_KEY);
        assertThat(environment.getProperty("binance.api.secret")).isEqualTo(BINANCE_SECRET);
        assertThatCode(() -> ProductionProfileSafetyGuard.validate(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void missingConfigTreeSecretFailsClosedWithoutDisclosingOtherValues() throws Exception {
        StandardEnvironment environment = loadConfigTree(Map.of(
                "trade-model.auth.initial-password", ADMIN_SECRET,
                "binance.api.key", BINANCE_KEY,
                "binance.api.secret", BINANCE_SECRET));

        assertThatThrownBy(() -> ProductionProfileSafetyGuard.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("production datasource password missing")
                .hasMessageNotContaining(ADMIN_SECRET)
                .hasMessageNotContaining(BINANCE_KEY)
                .hasMessageNotContaining(BINANCE_SECRET);
    }

    @Test
    void deploymentUsesConfigTreeWithoutEnvFilesOrSensitiveActuatorExposure() throws Exception {
        String compose = Files.readString(Path.of("deploy/p3h/docker-compose.p3h.yml"),
                StandardCharsets.UTF_8);
        String baseConfig = Files.readString(Path.of("src/main/resources/application.yml"),
                StandardCharsets.UTF_8);

        assertThat(compose).contains(
                "SPRING_CONFIG_IMPORT: configtree:/run/secrets/config/",
                "p3h_materialized_secrets",
                "target: /run/secrets",
                "read_only: true",
                "SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver",
                "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health");
        String materializer = Files.readString(Path.of("deploy/p3h/p3h-secret-materializer.sh"),
                StandardCharsets.UTF_8);
        assertThat(materializer).contains(
                "spring.datasource.password", "trade-model.auth.initial-password",
                "binance.api.key", "binance.api.secret", "chmod 400");
        assertThat(compose).contains(
                "TRADE_MODEL_AUTH_ENABLED: \"true\"",
                "TRADE_MODEL_INITIAL_USERNAME: p3h_operator",
                "TRADE_MODEL_SESSION_COOKIE_SECURE: \"true\"");
        assertThat(compose).doesNotContain(
                "PROD_DATASOURCE_PASSWORD:", "APP_ADMIN_PASSWORD:",
                "BINANCE_API_KEY:", "BINANCE_API_SECRET:", ".env");
        assertThat(baseConfig).contains("include: health", "show-details: never", "show-components: never");
        assertThat(baseConfig).doesNotContain("include: env", "include: configprops");
    }

    private StandardEnvironment loadConfigTree(Map<String, String> secretFiles) throws IOException {
        Path configTree = Files.createDirectory(tempDir.resolve("config-" + secretFiles.size()));
        for (Map.Entry<String, String> entry : secretFiles.entrySet()) {
            Files.writeString(configTree.resolve(entry.getKey()), entry.getValue(),
                    StandardCharsets.UTF_8);
        }

        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> properties = safeNonSecretProperties();
        properties.put("spring.config.import",
                "configtree:" + configTree.toAbsolutePath().normalize() + "/");
        environment.getPropertySources().addFirst(new MapPropertySource("p3h-non-secret-test", properties));
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return environment;
    }

    private Map<String, Object> safeNonSecretProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", "jdbc:postgresql://db.internal/trade_model_v1_p3h_primary");
        properties.put("spring.datasource.username", "p3h_app_readonly");
        properties.put("spring.h2.console.enabled", "false");
        properties.put("server.address", "0.0.0.0");
        properties.put("trade-model.production.allow-public-bind", "true");
        properties.put("position.provider.type", "BINANCE");
        properties.put("trade-model.auth.enabled", "true");
        properties.put("trade-model.auth.initial-username", "p3h_operator");
        properties.put("server.servlet.session.cookie.http-only", "true");
        properties.put("server.servlet.session.cookie.same-site", "lax");
        properties.put("server.servlet.session.cookie.secure", "true");
        properties.put("spring.session.jdbc.initialize-schema", "never");
        properties.put("server.forward-headers-strategy", "framework");
        properties.put("management.endpoints.web.exposure.include", "health");
        properties.put("trade-model.security.rate-limit.enabled", "true");
        properties.put("trade-model.security.rate-limit.requests-per-minute", "120");
        properties.put("trade-model.security.rate-limit.window-ms", "60000");
        properties.put("trade-model.production.scheduler-policy", "LOCKED_DOWN");
        properties.put("trade-model.schedulers.enabled", "false");
        properties.put("trade-model.schedulers.push-recheck.enabled", "false");
        properties.put("trade-model.schedulers.position-sync.enabled", "false");
        properties.put("trade-model.schedulers.market-data.enabled", "false");
        properties.put("trade-model.schedulers.ohlcv-ingestion.enabled", "false");
        properties.put("trade-model.schedulers.watchlist.enabled", "false");
        properties.put("trade-model.schedulers.position-monitor.enabled", "false");
        properties.put("trade-model.analysis.scheduler.enabled", "false");
        properties.put("trade-model.provider-call.enabled", "false");
        properties.put("trade-model.provider-call.scheduler-enabled", "false");
        properties.put("trade-model.provider-call.profile-escalation-enabled", "false");
        properties.put("trade-model.provider-call.auto-escalation-enabled", "false");
        properties.put("trade-model.provider-call.external-calls-enabled", "false");
        properties.put("trade-model.ohlcv.public-provider.enabled", "false");
        properties.put("trade-model.ohlcv.public-provider.external-calls-enabled", "false");
        properties.put("trade-model.providers.coinglass.enabled", "false");
        properties.put("trade-model.providers.coinglass.external-calls-enabled", "false");
        properties.put("trade-model.schedulers.ohlcv-ingestion.symbols", "");
        properties.put("trade-model.schedulers.ohlcv-ingestion.timeframes", "5m,15m,1h,4h");
        properties.put("trade-model.production.scheduler-approval.push-recheck",
                "PROD_ALLOWED_EXPLICIT_OPT_IN");
        properties.put("trade-model.production.scheduler-approval.position-sync",
                "PROD_ALLOWED_EXPLICIT_OPT_IN");
        properties.put("trade-model.production.scheduler-approval.market-data",
                "PROD_ALLOWED_EXPLICIT_OPT_IN");
        properties.put("trade-model.production.scheduler-approval.ohlcv-ingestion",
                "PROD_ALLOWED_EXPLICIT_OPT_IN");
        properties.put("trade-model.production.scheduler-approval.watchlist", "LOCAL_ONLY");
        properties.put("trade-model.production.scheduler-approval.position-monitor",
                "PROD_ALLOWED_DEFAULT_OFF");
        properties.put("trade-model.production.scheduler-approval.analysis",
                "PROD_ALLOWED_EXPLICIT_OPT_IN");
        properties.put("trade-model.production.scheduler-approval.provider-scan",
                "PROD_ALLOWED_DEFAULT_OFF");
        return properties;
    }
}
