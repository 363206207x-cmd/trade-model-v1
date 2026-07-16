package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ControlledP3hComposeOfflineSmokeTest {

    @Test
    void disposableComposeProvesBootstrapSecretsProxyAndReadOnlyRole() throws Exception {
        assumeTrue("true".equals(System.getenv("P3H_RUN_DOCKER_CONTRACT_TEST")),
                "explicit Docker contract opt-in is not enabled");

        ProcessBuilder builder = new ProcessBuilder(
                "bash", "scripts/controlled-p3h-compose-offline-smoke.sh");
        builder.redirectErrorStream(true);
        builder.environment().put("P3H_OFFLINE_COMPOSE_SMOKE_CONFIRM",
                "I_CONFIRM_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE");
        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofMinutes(10).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(process.exitValue()).as("sanitized output: %s", output).isZero();
        assertThat(output).contains(
                "GREENFIELD_BOOTSTRAP_ORDER: PASS",
                "ROLE_PROVISIONING_STATUS: PASS",
                "DATABASE_PROVISIONING_STATUS: PASS_PRIMARY_AND_RECOVERY",
                "FLYWAY_V1_TO_V7_STATUS: PASS",
                "APP_SECRET_READABILITY_STATUS: PASS_ACTUAL_CONTAINER",
                "UNRELATED_UID_SECRET_READABILITY: DENIED",
                "SECRET_VALUES_IN_DOCKER_INSPECT: ABSENT",
                "SECRET_VALUES_IN_PROCESS_ARGUMENTS: ABSENT",
                "APP_RUNTIME_USER: NON_ROOT_UID_10001",
                "HOST_HEADER_CONTRACT: PASS",
                "UNKNOWN_HTTPS_HOST_REJECTED: PASS",
                "TLS_1_3_LOCAL: PASS",
                "READ_ONLY_WRITE_PROBE: DENIED",
                "LOCAL_COMPOSE_TEMPLATE_SMOKE: PASS_LOCAL_DISPOSABLE_P3H_TEMPLATE_SMOKE",
                "LOCAL_DISPOSABLE_RESOURCE_CLEANUP: PASS",
                "REAL_STAGING_STATUS: BLOCKED_MISSING_AUTHORIZED_INPUT",
                "P4_ALLOWED: NO",
                "PRODUCTION_READINESS: BLOCKED");
    }
}
