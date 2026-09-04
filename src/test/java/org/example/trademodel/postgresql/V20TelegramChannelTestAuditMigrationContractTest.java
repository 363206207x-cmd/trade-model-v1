package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class V20TelegramChannelTestAuditMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V20__telegram_channel_test_audit.sql");

    @Test
    void migrationIsOwnerScopedIdempotentAndCarriesHardSafetyFlags() throws Exception {
        String sql = Files.readString(MIGRATION);
        assertThat(sql).contains(
                "UNIQUE (user_id, idempotency_key)",
                "not_trade_instruction = TRUE",
                "not_order_execution = TRUE",
                "'RATE_LIMITED'", "'BLOCKED'")
                .doesNotContainIgnoringCase("DELETE FROM")
                .doesNotContainIgnoringCase("UPDATE tm_user_position");
    }

    @Test
    void h2ExecutesMigrationAndEnforcesOwnerIsolationAndSafety() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v20-telegram-channel-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "")) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V20__telegram_channel_test_audit.sql"));
            insert(connection, "test-1", 41L, "telegram-test:key-1", true, true);
            insert(connection, "test-2", 42L, "telegram-test:key-1", true, true);
            assertThatThrownBy(() -> insert(connection, "test-3", 41L,
                    "telegram-test:key-1", true, true)).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> insert(connection, "test-4", 41L,
                    "telegram-test:key-2", false, true)).isInstanceOf(SQLException.class);
        }
    }

    private static void insert(Connection connection, String testId, long userId, String key,
                               boolean notTrade, boolean notOrder) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO tm_telegram_channel_test_audit(
                    test_id, user_id, idempotency_key, status, requested_at,
                    not_trade_instruction, not_order_execution)
                VALUES (?, ?, ?, 'BLOCKED', CURRENT_TIMESTAMP, ?, ?)
                """)) {
            statement.setString(1, testId);
            statement.setLong(2, userId);
            statement.setString(3, key);
            statement.setBoolean(4, notTrade);
            statement.setBoolean(5, notOrder);
            statement.executeUpdate();
        }
    }
}
