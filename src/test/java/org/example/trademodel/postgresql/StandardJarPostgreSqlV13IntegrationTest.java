package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StandardJarPostgreSqlV13IntegrationTest {

    @Test
    void controlledHarnessUsesStandardPackageJarAndPostgresql16ForFifteenMigrations() throws Exception {
        String script = Files.readString(Path.of("scripts/standard-release-postgresql-smoke.sh"));
        assertThat(script).contains(
                "./mvnw clean package",
                "java -jar \"${jar_path}\"",
                "postgres:16-alpine",
                "SELECT COUNT(*) FROM flyway_schema_history",
                "POSTGRESQL_V1_V16=16/16_PASS");
        assertThat(script).doesNotContain("-Pflyway-migration", "spring-boot:run");
    }
}
