package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PackagedJarMigrationFailureReadinessTest {

    @Test
    void checksumMismatchMustTerminateApplicationAndLeaveReadinessUnavailable() throws Exception {
        String script = Files.readString(Path.of("scripts/standard-release-postgresql-smoke.sh"));
        assertThat(script).contains(
                "UPDATE flyway_schema_history SET checksum = checksum + 1",
                "PACKAGED_JAR_CHECKSUM_FAILURE=FAILED_APP_STILL_RUNNING",
                "/actuator/health/readiness",
                "PACKAGED_JAR_MIGRATION_FAILURE_READINESS=PASS");
    }
}
