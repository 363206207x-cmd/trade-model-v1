package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("controlled-postgresql")
class ControlledGeneratedReleaseLikeFixtureFlywayTest {

    private static final String DATABASE = "trade_model_v1_p3_generated_source";
    private static final String JDBC_URL = "jdbc:postgresql://127.0.0.1:55434/" + DATABASE;
    private static final String CONFIRMATION = "I_CONFIRM_LOCAL_GENERATED_P3_V6_SCHEMA";

    @Test
    void createsOnlyTheApprovedLocalGeneratedFixtureAtFlywayV6() {
        String jdbcUrl = env("P3_GENERATED_POSTGRESQL_JDBC_URL");
        String username = env("P3_GENERATED_POSTGRESQL_USERNAME");
        String password = env("P3_GENERATED_POSTGRESQL_PASSWORD");
        String database = env("P3_GENERATED_POSTGRESQL_DATABASE");

        assumeTrue(hasText(jdbcUrl) && hasText(username) && hasText(password) && hasText(database),
                "P3 generated fixture Flyway action is environment-gated");

        assertThat(env("P3_GENERATED_FLYWAY_CONFIRM")).isEqualTo(CONFIRMATION);
        assertThat(database).isEqualTo(DATABASE);
        assertThat(jdbcUrl).isEqualTo(JDBC_URL);
        assertThat(jdbcUrl.toLowerCase()).doesNotContain(
                "prod", "production", "live", "primary", "main");

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("6"))
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();

        MigrateResult migration = flyway.migrate();
        assertThat(migration.success).isTrue();
        assertThat(migration.targetSchemaVersion).isEqualTo("6");
        assertThat(migration.migrationsExecuted).isEqualTo(6);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    private static String env(String name) {
        return System.getenv().getOrDefault(name, "").trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
