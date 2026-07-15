package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("controlled-postgresql")
class ControlledCurrentStateCloneFlywayActionTest {

    private static final String CONFIRMATION = "I_CONFIRM_LOCAL_P3_FLYWAY_ACTION";
    private static final Set<String> APPROVED_DATABASES = Set.of(
            "trade_model_v1_p3_source",
            "trade_model_v1_p3_rehearsal",
            "trade_model_v1_p3_recovery");

    @Test
    void validatesOrMigratesOnlyAnApprovedLocalP3Database() {
        String jdbcUrl = env("P3_CONTROLLED_POSTGRESQL_JDBC_URL");
        String username = env("P3_CONTROLLED_POSTGRESQL_USERNAME");
        String password = env("P3_CONTROLLED_POSTGRESQL_PASSWORD");
        String database = env("P3_CONTROLLED_POSTGRESQL_DATABASE");
        String action = env("P3_CONTROLLED_FLYWAY_ACTION");
        String sourceVersion = env("P3_CONTROLLED_SOURCE_FLYWAY_VERSION");

        assumeTrue(hasText(jdbcUrl) && hasText(username) && hasText(password)
                        && hasText(database) && hasText(action),
                "P3 controlled PostgreSQL action is environment-gated");

        assertThat(env("P3_CONTROLLED_FLYWAY_CONFIRM")).isEqualTo(CONFIRMATION);
        assertThat(APPROVED_DATABASES).contains(database);
        assertThat(jdbcUrl).isEqualTo("jdbc:postgresql://127.0.0.1:55433/" + database);
        assertThat(jdbcUrl.toLowerCase()).doesNotContain(
                "prod", "production", "live", "primary", "main");
        assertThat(action).isIn("VALIDATE", "MIGRATE");
        assertThat(sourceVersion).isIn("6", "7");

        MigrationVersion targetVersion = MigrationVersion.fromVersion(
                "VALIDATE".equals(action) ? sourceVersion : "7");
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .target(targetVersion)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();

        if ("VALIDATE".equals(action)) {
            ValidateResult validation = flyway.validateWithResult();
            assertThat(validation.validationSuccessful).isTrue();
            return;
        }

        assertThat(database).isEqualTo("trade_model_v1_p3_rehearsal");
        MigrateResult migration = flyway.migrate();
        assertThat(migration.success).isTrue();
        assertThat(migration.targetSchemaVersion).isEqualTo("7");
        assertThat(migration.migrationsExecuted).isEqualTo("6".equals(sourceVersion) ? 1 : 0);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    private static String env(String name) {
        return System.getenv().getOrDefault(name, "").trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
