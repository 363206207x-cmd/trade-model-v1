package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingComposeTopologyTest {

    @Test
    void composeExposesOnlyProxyAndKeepsBackendInternal() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");

        assertThat(compose).contains(
                "p3h_edge:", "p3h_backend:", "internal: true",
                "- \"80:8080\"", "- \"443:8443\"",
                "SPRING_CONFIG_IMPORT: configtree:/run/secrets/config/",
                "SPRING_FLYWAY_ENABLED: \"false\"",
                "SPRING_SQL_INIT_MODE: never",
                "SPRING_DATASOURCE_USERNAME: p3h_app_readonly",
                "SPRING_DATASOURCE_HIKARI_READ_ONLY: \"true\"",
                "TRADE_MODEL_PRODUCTION_SCHEDULER_POLICY: LOCKED_DOWN",
                "TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED: \"false\"",
                "TRADE_MODEL_AI_ENABLED: \"false\"",
                "TRADE_MODEL_SCHEDULERS_ENABLED: \"false\"",
                "TRADE_MODEL_POSITION_MONITOR_SCHEDULER_ENABLED: \"false\"",
                "TRADE_MODEL_PROVIDER_CALL_ENABLED: \"false\"");
        assertThat(serviceBlock(compose, "postgres", "migrate")).doesNotContain("ports:");
        assertThat(serviceBlock(compose, "app", "proxy")).doesNotContain("ports:");
        assertThat(compose).doesNotContain("/var/run/docker.sock", "network_mode: host");
    }

    @Test
    void composeUsesPinnedImagesAndNeverPlacesSecretValuesInEnvironment() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");

        assertThat(compose).contains(
                "postgres:16-alpine@sha256:", "flyway/flyway:10-alpine@sha256:",
                "nginx:1.27.4-alpine@sha256:", "POSTGRES_PASSWORD_FILE:",
                "target: /run/secrets/config/spring.datasource.password",
                "target: /run/secrets/config/trade-model.auth.admin-password");
        assertThat(compose).doesNotContain(
                "PROD_DATASOURCE_PASSWORD:", "APP_ADMIN_PASSWORD:",
                "BINANCE_API_KEY:", "BINANCE_API_SECRET:", ".env");
    }

    private String serviceBlock(String compose, String start, String next) {
        int from = compose.indexOf("  " + start + ":");
        int to = compose.indexOf("  " + next + ":", from + 1);
        return compose.substring(from, to);
    }
}
