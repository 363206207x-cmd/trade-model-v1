package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledGreenfieldFirstBootRehearsalP3GContractTest {

    private static final Path RUNNER = Path.of(
            "scripts/controlled-greenfield-first-boot-rehearsal-p3g.sh").toAbsolutePath();
    private static final String CONFIRMATION =
            "I_CONFIRM_LOCAL_GREENFIELD_EMPTY_DATABASE_REHEARSAL";
    private static final Set<String> P3G_ENVIRONMENT = Set.of(
            "P3G_CONFIRM", "P3G_HOST", "P3G_POSTGRES_PORT", "P3G_APP_PORT",
            "P3G_PRIMARY_DATABASE", "P3G_RECOVERY_DATABASE");

    @Test
    void missingConfirmationBlocksBeforeDockerOrDatabaseAccess() throws Exception {
        ScriptResult result = run(Map.of());

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "P3G_RESULT: BLOCKED_CONFIRMATION_REQUIRED",
                "DATABASE_ACCESS: NOT_ATTEMPTED",
                "DOCKER_ACTION: NOT_ATTEMPTED",
                "P4_ALLOWED: NO",
                "PRODUCTION_READINESS: BLOCKED");
    }

    @Test
    void nonLocalhostTargetFailsClosedBeforeDockerAccess() throws Exception {
        ScriptResult result = run(Map.of(
                "P3G_CONFIRM", CONFIRMATION,
                "P3G_HOST", "staging-db.internal"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("P3G_RESULT: BLOCKED_NON_LOCALHOST_TARGET");
    }

    @Test
    void nonFixedPortsFailClosedBeforeDockerAccess() throws Exception {
        ScriptResult result = run(Map.of(
                "P3G_CONFIRM", CONFIRMATION,
                "P3G_POSTGRES_PORT", "5432"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("P3G_RESULT: BLOCKED_UNAPPROVED_LOCAL_PORT");
    }

    @Test
    void nonFixedDatabaseNamesFailClosedBeforeDockerAccess() throws Exception {
        ScriptResult result = run(Map.of(
                "P3G_CONFIRM", CONFIRMATION,
                "P3G_PRIMARY_DATABASE", "trade_model_production"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("P3G_RESULT: BLOCKED_UNAPPROVED_DATABASE_NAME");
    }

    @Test
    void runnerPinsTopologyImageAndFreshDatabaseContract() throws Exception {
        String script = Files.readString(RUNNER, StandardCharsets.UTF_8);

        assertThat(script).contains(
                "P3G_POSTGRES_PORT=\"${P3G_POSTGRES_PORT:-55435}\"",
                "P3G_APP_PORT=\"${P3G_APP_PORT:-18085}\"",
                "P3G_HOST=\"${P3G_HOST:-127.0.0.1}\"",
                "trade_model_v1_p3g_primary",
                "trade_model_v1_p3g_recovery",
                "postgres@sha256:fd1e8d0274f13f5a03a2673a207b28e14823c2f2efc3ca4bb4197c8a9f841bdc",
                "docker network create --internal",
                "--publish \"127.0.0.1:${P3G_POSTGRES_PORT}:${P3G_POSTGRES_PORT}\"",
                "--env \"PGPORT=${P3G_POSTGRES_PORT}\"",
                "--network \"container:${PG_CONTAINER}\"",
                "/opt/maven/bin/mvn -o -q -Dtest=ControlledGreenfieldFlywayV7ActionTest test",
                "BLOCKED_NON_EMPTY_GREENFIELD_DATABASE",
                "GREENFIELD_PRE_MIGRATION_FLYWAY_HISTORY: ABSENT",
                "POST_MIGRATION_BUSINESS_ROWS: ${BUSINESS_ROWS}",
                "POST_MIGRATION_SEED_ALLOWLIST: tm_rule_config=${RULE_CONFIG_ROWS}",
                ".HostConfig.PortBindings",
                "APPLICATION_HOST_BIND_CONFIG: 127.0.0.1:18085",
                "APPLICATION_HOST_EXPOSURE: LOOPBACK_ONLY",
                "EMPTY_ASSET_CARDS_FAIL_CLOSED: PASS",
                "EMPTY_SYSTEM_STATE_FAIL_CLOSED: PASS",
                "FAKE_ASSET_CONCLUSIONS: NONE",
                "FAKE_POSITION_PLAN_RECORDS: NONE",
                "ASSET_ENUM_CONTRACT: PASS_EXACT_FORMAL_VALUES",
                "MARKET_BIAS_EMPTY_CONTRACT: WAIT_OR_EMPTY_ONLY",
                "ASSET_JSON_SHAPE: PASS_STRICT",
                "trade_model.inventory_as_of_utc=2026-07-15T00:00:00");
        assertThat(script).doesNotContain(
                "flyway clean", "flyway repair", "flyway baseline",
                "ignoreMigrationPatterns", "V8__");
        assertThat(script).doesNotContain("docker port \"${APP_CONTAINER}\"");
        assertThat(script).contains(
                "write_redacted_flyway_failure",
                "jdbc:postgresql://[REDACTED]",
                "[REDACTED_ROLE]",
                "[REDACTED_SECRET]",
                "[REDACTED_SENSITIVE_LOG_LINE]",
                "application-${label}-failure-redacted.txt",
                "application-${label}-smoke-failure-redacted.txt",
                "/Users/[REDACTED]");
    }

    @Test
    void runnerHasBoundedCleanupAndKeepsReleaseGatesClosed() throws Exception {
        String script = Files.readString(RUNNER, StandardCharsets.UTF_8);

        assertThat(script).contains(
                "run_bounded()",
                "trap on_exit EXIT",
                "trap 'exit 130' INT",
                "trap 'exit 143' TERM",
                "for disposable_container in",
                "docker rm -f",
                "docker network rm",
                "docker volume rm",
                "CONTAINER_CLEANUP: ${CONTAINER_CLEANUP}",
                "NETWORK_CLEANUP: ${NETWORK_CLEANUP}",
                "VOLUME_CLEANUP: ${VOLUME_CLEANUP}",
                "P4_ALLOWED: NO",
                "PRODUCTION_READINESS: BLOCKED");
    }

    private ScriptResult run(Map<String, String> overrides) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", RUNNER.toString());
        builder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
        builder.redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        P3G_ENVIRONMENT.forEach(environment::remove);
        environment.putAll(new HashMap<>(overrides));

        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
