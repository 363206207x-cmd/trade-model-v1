package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("controlled-postgresql")
class ControlledCurrentStateContentFingerprintTest {

    private static final String DATABASE = "trade_model_v1_p3_generated_source";
    private static final String JDBC_URL = "jdbc:postgresql://127.0.0.1:55434/" + DATABASE;
    private static final String CONFIRMATION = "I_CONFIRM_LOCAL_P3_CONTENT_FINGERPRINT";
    private static final long HASH_SEED_A = 20260715L;
    private static final long HASH_SEED_B = 11270031L;
    private static final String RAW_MUTATION_MARKER = "P3_SENSITIVE_MUTATION_MARKER";

    @Test
    void sameDataProducesMatchingFingerprint() throws Exception {
        try (Connection connection = connection()) {
            Map<String, TableFingerprint> first = fingerprint(connection);
            Map<String, TableFingerprint> second = fingerprint(connection);

            assertThat(second).isEqualTo(first);
            connection.rollback();
        }
    }

    @Test
    void sameRowCountStatusMutationIsDetected() throws Exception {
        assertMutationDetected("""
                UPDATE tm_analysis_run
                SET status = CASE WHEN status = 'SUCCESS' THEN 'FAILED' ELSE 'SUCCESS' END
                WHERE analysis_id = (SELECT MIN(analysis_id) FROM tm_analysis_run)
                """);
    }

    @Test
    void sameRowCountTimeMutationIsDetected() throws Exception {
        assertMutationDetected("""
                UPDATE tm_analysis_run
                SET analysis_time = analysis_time + INTERVAL '1 second'
                WHERE analysis_id = (SELECT MIN(analysis_id) FROM tm_analysis_run)
                """);
    }

    @Test
    void sameRowCountPlanBoundaryMutationIsDetected() throws Exception {
        assertMutationDetected("""
                UPDATE tm_execution_plan
                SET entry_zone = 'P3_SENSITIVE_MUTATION_MARKER'
                WHERE plan_id = (SELECT MIN(plan_id) FROM tm_execution_plan)
                """);
    }

    @Test
    void rollbackRestoresFingerprint() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            Map<String, TableFingerprint> before = fingerprint(connection);
            assertThat(statement.executeUpdate("""
                    UPDATE tm_execution_plan
                    SET stop_loss = 'P3_SENSITIVE_MUTATION_MARKER'
                    WHERE plan_id = (SELECT MIN(plan_id) FROM tm_execution_plan)
                    """)).isEqualTo(1);
            assertThat(fingerprint(connection)).isNotEqualTo(before);

            connection.rollback();

            assertThat(fingerprint(connection)).isEqualTo(before);
            connection.rollback();
        }
    }

    @Test
    void sessionTimezoneDoesNotChangeFingerprint() throws Exception {
        Map<String, TableFingerprint> expected = fingerprintInSessionTimezone("UTC");

        assertThat(fingerprintInSessionTimezone("Asia/Shanghai")).isEqualTo(expected);
        assertThat(fingerprintInSessionTimezone("America/New_York")).isEqualTo(expected);
    }

    @Test
    void fingerprintOutputDoesNotContainRawModifiedValues() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertThat(statement.executeUpdate("""
                    UPDATE tm_execution_plan
                    SET take_profit_rules = 'P3_SENSITIVE_MUTATION_MARKER'
                    WHERE plan_id = (SELECT MIN(plan_id) FROM tm_execution_plan)
                    """)).isEqualTo(1);

            String output = render(fingerprint(connection));

            assertThat(output).doesNotContain(RAW_MUTATION_MARKER);
            assertThat(output).contains("CONTENT_TABLE|tm_execution_plan|");
            connection.rollback();
        }
    }

    private void assertMutationDetected(String updateSql) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            Map<String, TableFingerprint> before = fingerprint(connection);
            assertThat(statement.executeUpdate(updateSql)).isEqualTo(1);
            Map<String, TableFingerprint> after = fingerprint(connection);

            assertThat(rowCounts(after)).isEqualTo(rowCounts(before));
            assertThat(after).isNotEqualTo(before);

            connection.rollback();
            assertThat(fingerprint(connection)).isEqualTo(before);
            connection.rollback();
        }
    }

    private Map<String, TableFingerprint> fingerprintInSessionTimezone(String timezone)
            throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("SET TIME ZONE '" + timezone + "'");
            connection.commit();
            Map<String, TableFingerprint> result = fingerprint(connection);
            connection.rollback();
            return result;
        }
    }

    private Map<String, TableFingerprint> fingerprint(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL TIME ZONE 'UTC'");
        }

        Map<String, TableFingerprint> result = new TreeMap<>();
        for (String table : tableNames(connection)) {
            String rowJson = "to_jsonb(row_data)::text";
            String sql = "SELECT COUNT(*)::text, "
                    + "COALESCE(SUM(hashtextextended(" + rowJson + ", " + HASH_SEED_A
                    + ")), 0)::text, "
                    + "COALESCE(bit_xor(hashtextextended(" + rowJson + ", " + HASH_SEED_A
                    + ")), 0)::text, "
                    + "COALESCE(SUM(hashtextextended(" + rowJson + ", " + HASH_SEED_B
                    + ")), 0)::text, "
                    + "COALESCE(bit_xor(hashtextextended(" + rowJson + ", " + HASH_SEED_B
                    + ")), 0)::text "
                    + "FROM public." + quoteIdentifier(table) + " AS row_data";
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(sql)) {
                assertThat(rows.next()).isTrue();
                result.put(table, new TableFingerprint(
                        rows.getString(1), rows.getString(2), rows.getString(3),
                        rows.getString(4), rows.getString(5)));
            }
        }
        return result;
    }

    private List<String> tableNames(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' "
                + "AND table_name LIKE 'tm\\_%' ESCAPE '\\' ORDER BY table_name";
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                tables.add(rows.getString(1));
            }
        }
        assertThat(tables).isNotEmpty();
        return tables;
    }

    private Map<String, String> rowCounts(Map<String, TableFingerprint> fingerprints) {
        Map<String, String> counts = new TreeMap<>();
        fingerprints.forEach((table, value) -> counts.put(table, value.rowCount()));
        return counts;
    }

    private String render(Map<String, TableFingerprint> fingerprints) {
        StringBuilder output = new StringBuilder();
        fingerprints.forEach((table, value) -> output.append("CONTENT_TABLE|")
                .append(table).append('|').append(value.rowCount()).append('|')
                .append(value.seedASum()).append('|').append(value.seedAXor()).append('|')
                .append(value.seedBSum()).append('|').append(value.seedBXor()).append('\n'));
        return output.toString();
    }

    private Connection connection() throws SQLException {
        String jdbcUrl = env("P3_CONTENT_FINGERPRINT_JDBC_URL");
        String username = env("P3_CONTENT_FINGERPRINT_USERNAME");
        String password = env("P3_CONTENT_FINGERPRINT_PASSWORD");
        String database = env("P3_CONTENT_FINGERPRINT_DATABASE");
        assumeTrue(hasText(jdbcUrl) && hasText(username) && hasText(password) && hasText(database),
                "P3 content fingerprint test is environment-gated");

        assertThat(env("P3_CONTENT_FINGERPRINT_CONFIRM")).isEqualTo(CONFIRMATION);
        assertThat(database).isEqualTo(DATABASE);
        assertThat(jdbcUrl).isEqualTo(JDBC_URL);
        assertThat(jdbcUrl.toLowerCase()).doesNotContain(
                "prod", "production", "live", "primary", "main");

        Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
        connection.setAutoCommit(false);
        return connection;
    }

    private static String quoteIdentifier(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String env(String name) {
        return System.getenv().getOrDefault(name, "").trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record TableFingerprint(
            String rowCount,
            String seedASum,
            String seedAXor,
            String seedBSum,
            String seedBXor) {
    }
}
