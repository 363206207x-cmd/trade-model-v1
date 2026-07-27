package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingComposeTopologyTest {

    @Test
    void composeExposesOnlyProxyAndKeepsBackendInternal() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");

        assertThat(compose).contains(
                "p3h_edge:", "p3h_backend:", "internal: true",
                "${P3H_HTTP_HOST_PORT:-80}:8080", "${P3H_HTTPS_HOST_PORT:-443}:8443",
                "SPRING_CONFIG_IMPORT: configtree:/run/secrets/config/",
                "SPRING_FLYWAY_ENABLED: \"false\"",
                "SPRING_SQL_INIT_MODE: never",
                "SPRING_DATASOURCE_USERNAME: p3h_app_readonly",
                "SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver",
                "SPRING_DATASOURCE_HIKARI_READ_ONLY: \"false\"",
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
                "p3h_materialized_secrets", "type: tmpfs", "target: /run/secrets");
        assertThat(compose).doesNotContain(
                "PROD_DATASOURCE_PASSWORD:", "APP_ADMIN_PASSWORD:",
                "BINANCE_API_KEY:", "BINANCE_API_SECRET:", ".env");
    }

    @Test
    void bootstrapFailurePreventsAppStartup() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");
        String bootstrap = P3hContractTestSupport.read(
                "deploy/p3h/postgres-role-bootstrap-entrypoint.sh");

        assertThat(serviceBlock(compose, "role-bootstrap", "migrate")).contains(
                "greenfield-preflight:", "condition: service_completed_successfully");
        assertThat(serviceBlock(compose, "migrate", "readonly-grants")).contains(
                "role-bootstrap:", "condition: service_completed_successfully");
        assertThat(bootstrap).contains(
                "P3H_ROLE_COUNT: ${role_count}",
                "P3H_DATABASE_COUNT: ${database_count}",
                "trade_model_v1_p3h_primary", "p3h_migration_owner",
                "trade_model_v1_p3h_recovery", "p3h_recovery_owner",
                "BLOCKED_DATABASE_VERIFICATION");
    }

    @Test
    void migrationFailurePreventsAppStartup() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");

        assertThat(serviceBlock(compose, "readonly-grants", "secret-materializer")).contains(
                "migrate:", "condition: service_completed_successfully");
        assertThat(serviceBlock(compose, "app", "proxy")).contains(
                "readonly-grants:", "condition: service_completed_successfully");
    }

    @Test
    void appCannotStartBeforeV9OwnershipAndTmUserContract() throws Exception {
        String grants = P3hContractTestSupport.read("deploy/p3h/postgres-readonly-grants.sql");
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");

        assertThat(grants).contains(
                "successful_migrations <> 9", "final_version <> '9'",
                "P3-H Flyway V9 verification failed",
                "public.tm_user", "public.tm_user_id_seq");
        assertThat(serviceBlock(compose, "app", "proxy")).contains(
                "secret-materializer:", "condition: service_completed_successfully");
    }

    @Test
    void migrationServiceUsesCanonicalV9WithoutDuplicatingTmUserDdl() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");
        String start = P3hContractTestSupport.read("deploy/p3h/p3h-compose-start.sh");

        assertThat(serviceBlock(compose, "migrate", "readonly-grants")).contains(
                "../../src/main/resources/db/migration:/flyway/sql:ro",
                "FLYWAY_BASELINE_ON_MIGRATE: \"false\"",
                "FLYWAY_CLEAN_DISABLED: \"true\"");
        assertThat(start).contains("P3H_CURRENT_STEP=FLYWAY_MIGRATE");
        assertThat(compose + start).doesNotContain("CREATE TABLE tm_user");
    }

    @Test
    void tmpfsSecretHolderCannotReadOrReceiveSourceSecrets() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");
        String holder = serviceBlock(compose, "secret-volume-holder", "secret-materializer");

        assertThat(holder).contains(
                "user: \"65534:65534\"", "network_mode: none", "read_only: true",
                "source: p3h_materialized_secrets");
        assertThat(holder).doesNotContain("secrets:", "app_database_password", "tls_private_key");
        assertThat(serviceBlock(compose, "secret-materializer", "app")).contains(
                "secret-volume-holder:", "condition: service_started");
    }

    @Test
    void proxyCannotStartBeforeAppHealthy() throws Exception {
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");
        String proxy = serviceBlock(compose, "proxy", "app-role-probe");

        assertThat(proxy).contains(
                "app:", "condition: service_healthy",
                "/etc/nginx/conf.d:rw,noexec,nosuid,size=1m",
                "nginx -t >/dev/null 2>&1 && kill -0 1");
    }

    private String serviceBlock(String compose, String start, String next) {
        int from = compose.indexOf("  " + start + ":");
        int to = compose.indexOf("  " + next + ":", from + 1);
        return compose.substring(from, to);
    }
}
