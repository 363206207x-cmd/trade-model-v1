package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("controlled-postgresql")
class ControlledGreenfieldFlywayV7ActionTest {

    private static final String JDBC_URL =
            "jdbc:postgresql://127.0.0.1:55435/trade_model_v1_p3g_primary";
    private static final String CONFIRMATION = "I_CONFIRM_LOCAL_GREENFIELD_FLYWAY_V1_TO_V7";

    @Test
    void migratesExactEmptyGreenfieldDatabaseFromV1ToV7AndRepeatsIdempotently() {
        String jdbcUrl = env("P3G_CONTROLLED_POSTGRESQL_JDBC_URL");
        String username = env("P3G_CONTROLLED_POSTGRESQL_USERNAME");
        String password = env("P3G_CONTROLLED_POSTGRESQL_PASSWORD");

        assumeTrue(hasText(jdbcUrl) && hasText(username) && hasText(password),
                "P3-G Flyway action is environment-gated");
        assertThat(env("P3G_CONTROLLED_FLYWAY_CONFIRM")).isEqualTo(CONFIRMATION);
        assertThat(jdbcUrl).isEqualTo(JDBC_URL);
        assertThat(username).startsWith("p3g_migrator_");

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .target("7")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .connectRetries(0)
                .load();

        MigrateResult first = flyway.migrate();
        assertThat(first.success).isTrue();
        assertThat(first.migrationsExecuted).isEqualTo(7);
        assertThat(first.targetSchemaVersion).isEqualTo("7");
        assertValid(flyway);

        MigrateResult second = flyway.migrate();
        assertThat(second.success).isTrue();
        assertThat(second.migrationsExecuted).isZero();
        assertThat(second.targetSchemaVersion).isNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("7");
        assertValid(flyway);

        System.out.println("GREENFIELD_FLYWAY_FRESH_V1_TO_V7: PASS");
        System.out.println("GREENFIELD_FLYWAY_REPEAT_MIGRATE: ZERO_MIGRATIONS");
        System.out.println("FLYWAY_SCHEMA_VERSION: 7");
    }

    private static void assertValid(Flyway flyway) {
        ValidateResult result = flyway.validateWithResult();
        assertThat(result.validationSuccessful).isTrue();
        assertThat(result.invalidMigrations).isEmpty();
        assertThat(result.validateCount).isEqualTo(7);
    }

    private static String env(String name) {
        return System.getenv().getOrDefault(name, "").trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
