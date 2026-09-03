package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class V18PersistentSessionMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V18__persistent_http_session.sql");

    @Test
    void migrationDeclaresCanonicalSpringSessionSchemaWithoutTouchingEarlierVersions() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "CREATE TABLE SPRING_SESSION",
                "CREATE UNIQUE INDEX SPRING_SESSION_IX1",
                "CREATE INDEX SPRING_SESSION_IX2",
                "CREATE INDEX SPRING_SESSION_IX3",
                "CREATE TABLE SPRING_SESSION_ATTRIBUTES",
                "REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE");
        try (var migrations = Files.list(Path.of("src/main/resources/db/migration"))) {
            assertThat(migrations.filter(path -> path.getFileName().toString().matches("V(?:[1-9]|1[0-8])__.+\\.sql"))
                    .count()).isEqualTo(18);
        }
    }

    @Test
    void h2ExecutesV18AndEnforcesSessionIdentityAndAttributeLifecycle() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v18-session;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "")) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V18__persistent_http_session.sql"));

            insertSession(connection, "primary-a", "session-a");
            insertAttribute(connection, "primary-a", "security-context");

            assertThat(count(connection, "spring_session")).isEqualTo(1);
            assertThat(count(connection, "spring_session_attributes")).isEqualTo(1);
            assertThatThrownBy(() -> insertSession(connection, "primary-b", "session-a"))
                    .isInstanceOf(SQLException.class);

            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM spring_session WHERE primary_id = ?")) {
                delete.setString(1, "primary-a");
                assertThat(delete.executeUpdate()).isEqualTo(1);
            }
            assertThat(count(connection, "spring_session_attributes")).isZero();
        }
    }

    private static void insertSession(Connection connection, String primaryId, String sessionId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO spring_session(
                    primary_id, session_id, creation_time, last_access_time,
                    max_inactive_interval, expiry_time, principal_name)
                VALUES (?, ?, 1000, 1000, 1800, 1801000, 'operator')
                """)) {
            statement.setString(1, primaryId);
            statement.setString(2, sessionId);
            statement.executeUpdate();
        }
    }

    private static void insertAttribute(Connection connection, String primaryId, String attributeName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO spring_session_attributes(session_primary_id, attribute_name, attribute_bytes)
                VALUES (?, ?, ?)
                """)) {
            statement.setString(1, primaryId);
            statement.setString(2, attributeName);
            statement.setBytes(3, new byte[]{1, 2, 3});
            statement.executeUpdate();
        }
    }

    private static int count(Connection connection, String table) throws Exception {
        try (ResultSet result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertThat(result.next()).isTrue();
            return result.getInt(1);
        }
    }
}
