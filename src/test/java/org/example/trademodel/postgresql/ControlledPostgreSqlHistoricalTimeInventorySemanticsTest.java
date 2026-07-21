package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("controlled-postgresql")
class ControlledPostgreSqlHistoricalTimeInventorySemanticsTest {

    private static final String CONFIRM_VALUE = "I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL";
    private static final String RUN_VALUE = "I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB";
    private static final String EXPECTED_URL =
            "jdbc:postgresql://127.0.0.1:55432/trade_model_v1_test";
    private static final String INVENTORY_AS_OF = "2026-07-14 12:00:00";
    private static final LocalDateTime AS_OF = LocalDateTime.parse("2026-07-14T12:00:00");
    private static final List<String> SESSION_TIMEZONES =
            List.of("UTC", "Asia/Shanghai", "America/New_York");
    private static final String QUERY_START = "-- INVENTORY_QUERY_BEGIN";
    private static final String QUERY_END = "-- INVENTORY_QUERY_END";

    private static String jdbcUrl;
    private static String username;
    private static String password;
    private static String inventoryQuery;

    @BeforeAll
    static void prepareControlledPostgreSql() throws Exception {
        jdbcUrl = env("CONTROLLED_POSTGRESQL_JDBC_URL");
        username = env("CONTROLLED_POSTGRESQL_USERNAME");
        password = env("CONTROLLED_POSTGRESQL_PASSWORD");

        assumeTrue(hasText(jdbcUrl) && hasText(username) && hasText(password),
                "Controlled PostgreSQL env is missing; evidence test is environment-gated");
        assertThat(jdbcUrl).isEqualTo(EXPECTED_URL);
        assertThat(username).isEqualTo("trade_model_test");
        assertThat(env("CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM")).isEqualTo(CONFIRM_VALUE);
        assertThat(env("CONTROLLED_POSTGRESQL_FLYWAY_RUN")).isEqualTo(RUN_VALUE);
        assertThat(jdbcUrl.toLowerCase()).doesNotContain("prod", "production", "live", "primary", "main");

        Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .target("7")
                .load()
                .migrate();
        inventoryQuery = loadInventoryQuery();
    }

    @Test
    void fieldCatalogMakesApplicabilityAndCandidateOnlyClassificationExplicit() throws Exception {
        List<String> lines = runAcrossSessionTimezones(connection -> {
        });

        assertThat(lines.stream().filter(line -> line.startsWith("FIELD_POLICY|")).count())
                .isEqualTo(14);
        assertThat(lineStartingWith(lines, "FIELD_POLICY|tm_monitor_alert.cooldown_until|"))
                .contains("semantic_type=SCHEDULED_DEADLINE")
                .contains("future_check_mode=NOT_APPLICABLE")
                .contains("relation_check_mode=ORDERING_AND_DURATION")
                .contains("offset_pattern_applicable=false");
        assertThat(lineStartingWith(lines, "FIELD_POLICY|tm_decision_result.valid_from|"))
                .contains("semantic_type=VALIDITY_START")
                .contains("future_check_mode=NOT_APPLICABLE")
                .contains("tolerance_contract=FUTURE_ALLOWED_NO_ASSUMED_HORIZON");
        assertThat(lineStartingWith(lines, "FIELD_POLICY|tm_decision_result.expires_at|"))
                .contains("semantic_type=VALIDITY_END")
                .contains("future_check_mode=NOT_APPLICABLE");
        assertThat(lineStartingWith(lines, "FIELD_POLICY|tm_analysis_run.analysis_time|"))
                .contains("semantic_type=CANONICAL_ANALYSIS_TIME")
                .contains("relation_check_mode=DISTRIBUTION_ONLY")
                .contains("offset_pattern_applicable=false");
        assertThat(lineStartingWith(lines, "FIELD_POLICY|tm_analysis_run.created_at|"))
                .contains("reference_field=tm_analysis_run.started_at")
                .contains("expected_ordering=NEAR_SIMULTANEOUS")
                .contains("offset_pattern_applicable=true");
        assertThat(lines).noneMatch(line -> line.contains("VERIFIED_UTC")
                || line.contains("POST_CUTOVER_UTC")
                || line.contains("REFERENCE_MISMATCH")
                || line.startsWith("OFFSET_PATTERN|"));

        System.out.println("INVENTORY_FIELD_POLICY_STATUS: PASS_EXPLICIT_14_FIELDS");
    }

    @Test
    void normalScheduledAndValidityFixturesHaveNoFalsePositivesAcrossSessionTimezones() throws Exception {
        List<String> lines = runAcrossSessionTimezones(
                ControlledPostgreSqlHistoricalTimeInventorySemanticsTest::insertNormalFixtures);

        assertThat(lineStartingWith(lines, "FIELD_SUMMARY|tm_monitor_alert.cooldown_until|"))
                .contains("future_check=NOT_APPLICABLE")
                .contains("future_event_candidates=NOT_APPLICABLE")
                .contains("reference_check=ORDERING_AND_DURATION");
        assertThat(lineStartingWith(lines, "DURATION_SUMMARY|MONITOR_COOLDOWN|"))
                .contains("count=1")
                .contains("negative=0")
                .contains("min_seconds=900")
                .contains("max_seconds=900");
        assertThat(lineStartingWith(lines,
                "ORDERING_ANOMALY|SCHEDULE_ORDER_INVALID|relation=tm_monitor_alert.cooldown_until"))
                .endsWith("|count=0");

        assertThat(lineStartingWith(lines, "FIELD_SUMMARY|tm_decision_result.valid_from|"))
                .contains("future_check=NOT_APPLICABLE")
                .contains("future_event_candidates=NOT_APPLICABLE")
                .contains("reference_check=VALIDITY_INTERVAL");
        assertThat(lineStartingWith(lines, "FIELD_SUMMARY|tm_decision_result.expires_at|"))
                .contains("future_check=NOT_APPLICABLE")
                .contains("future_event_candidates=NOT_APPLICABLE")
                .contains("reference_check=VALIDITY_INTERVAL");
        assertThat(lineStartingWith(lines, "DURATION_BUCKET|DECISION_VALIDITY|bucket=GT_12H_LE_24H|"))
                .endsWith("|count=1");
        assertThat(lineStartingWith(lines, "VALIDITY_STATE|NOT_ACTIVE|"))
                .endsWith("|count=1");
        assertThat(lineStartingWith(lines, "VALIDITY_STATE|ACTIVE|"))
                .endsWith("|count=1");
        assertThat(lineStartingWith(lines, "VALIDITY_STATE|EXPIRED|"))
                .endsWith("|count=1");
        assertThat(lineStartingWith(lines, "ORDERING_ANOMALY|VALIDITY_ORDER_INVALID|"))
                .endsWith("|count=0");
        assertThat(lineStartingWith(lines, "ORDERING_ANOMALY|VALIDITY_PARTIAL_NULL|"))
                .endsWith("|count=0");
        assertThat(lines).noneMatch(line -> line.contains("REFERENCE_MISMATCH"));

        System.out.println("NORMAL_COOLDOWN_FALSE_POSITIVE_COUNT: 0");
        System.out.println("NORMAL_FUTURE_VALID_FROM_FALSE_POSITIVE_COUNT: 0");
        System.out.println("NORMAL_24H_EXPIRES_AT_MISMATCH_COUNT: 0");
        System.out.println("NORMAL_VALIDITY_STATES: NOT_ACTIVE=1,ACTIVE=1,EXPIRED=1");
        System.out.println("NORMAL_SESSION_TIMEZONE_CONSISTENCY: PASS");
    }

    @Test
    void trueAnomaliesRemainCandidatesAcrossSessionTimezones() throws Exception {
        List<String> lines = runAcrossSessionTimezones(
                ControlledPostgreSqlHistoricalTimeInventorySemanticsTest::insertAnomalyFixtures);

        assertThat(lineStartingWith(lines,
                "FUTURE_EVENT_CANDIDATE|EVENT_FUTURE_OUTLIER|field=tm_monitor_alert.created_at|"))
                .endsWith("|count=1");
        assertThat(lineStartingWith(lines,
                "ORDERING_ANOMALY|AUDIT_ORDER_INVALID|relation=tm_monitor_alert.updated_at"))
                .endsWith("|count=1");
        assertThat(lineStartingWith(lines,
                "ORDERING_ANOMALY|SCHEDULE_ORDER_INVALID|relation=tm_monitor_alert.cooldown_until"))
                .endsWith("|count=1");
        assertThat(lineStartingWith(lines, "ORDERING_ANOMALY|VALIDITY_ORDER_INVALID|"))
                .endsWith("|count=1");
        assertThat(lineStartingWith(lines, "ORDERING_ANOMALY|VALIDITY_PARTIAL_NULL|"))
                .endsWith("|count=1");
        assertThat(lineStartingWith(lines,
                "OFFSET_PATTERN_CANDIDATE|field=tm_analysis_run.created_at|"))
                .contains("reference=tm_analysis_run.started_at")
                .contains("plus_8h=1")
                .contains("minus_8h=0")
                .contains("plus_4h=0")
                .contains("minus_4h=0");
        assertThat(lines).noneMatch(line -> line.contains("VERIFIED_OFFSET_ERROR")
                || line.contains("REFERENCE_MISMATCH"));

        System.out.println("TRUE_FUTURE_EVENT_ANOMALY_COUNT: 1");
        System.out.println("AUDIT_ORDER_INVALID_COUNT: 1");
        System.out.println("SCHEDULE_ORDER_INVALID_COUNT: 1");
        System.out.println("VALIDITY_ORDER_INVALID_COUNT: 1");
        System.out.println("VALIDITY_PARTIAL_NULL_COUNT: 1");
        System.out.println("OFFSET_PATTERN_CANDIDATE_PLUS_8H_COUNT: 1");
        System.out.println("ANOMALY_SESSION_TIMEZONE_CONSISTENCY: PASS");
    }

    private static List<String> runAcrossSessionTimezones(Fixture fixture) throws Exception {
        List<String> reference = null;
        for (String sessionTimezone : SESSION_TIMEZONES) {
            List<String> current = runInventory(sessionTimezone, fixture);
            if (reference == null) {
                reference = current;
            } else {
                assertThat(current).as("inventory aggregate for %s", sessionTimezone)
                        .containsExactlyElementsOf(reference);
            }
        }
        return reference;
    }

    private static List<String> runInventory(String sessionTimezone, Fixture fixture) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL TIME ZONE '" + sessionTimezone + "'");
                    statement.execute("DELETE FROM tm_monitor_alert");
                    statement.execute("DELETE FROM tm_decision_result");
                    statement.execute("DELETE FROM tm_hot_reset_event");
                    statement.execute("DELETE FROM tm_analysis_run");
                }
                fixture.insert(connection);
                try (PreparedStatement setting = connection.prepareStatement(
                        "SELECT set_config('trade_model.inventory_as_of_utc', ?, true)")) {
                    setting.setString(1, INVENTORY_AS_OF);
                    setting.executeQuery();
                }
                List<String> lines = new ArrayList<>();
                try (Statement statement = connection.createStatement();
                     ResultSet rows = statement.executeQuery(inventoryQuery)) {
                    while (rows.next()) {
                        lines.add(rows.getString(1));
                    }
                }
                return lines;
            } finally {
                connection.rollback();
            }
        }
    }

    private static void insertNormalFixtures(Connection connection) throws Exception {
        insertMonitorAlert(connection, "normal-cooldown", AS_OF, AS_OF, AS_OF.plusMinutes(15));

        insertDecision(connection, "normal-not-active", AS_OF,
                OffsetDateTime.parse("2026-07-14T13:00:00Z"),
                OffsetDateTime.parse("2026-07-15T13:00:00Z"));
        insertDecision(connection, "normal-active", AS_OF.minusHours(2),
                OffsetDateTime.parse("2026-07-14T11:00:00Z"),
                OffsetDateTime.parse("2026-07-14T13:00:00Z"));
        insertDecision(connection, "normal-expired", AS_OF.minusDays(1),
                OffsetDateTime.parse("2026-07-13T11:00:00Z"),
                OffsetDateTime.parse("2026-07-14T12:00:00Z"));
    }

    private static void insertAnomalyFixtures(Connection connection) throws Exception {
        insertMonitorAlert(connection, "future-created", AS_OF.plusMinutes(6),
                AS_OF.plusMinutes(6), null);
        insertMonitorAlert(connection, "invalid-audit-order", AS_OF,
                AS_OF.minusMinutes(1), null);
        insertMonitorAlert(connection, "invalid-schedule-order", AS_OF,
                AS_OF, AS_OF.minusMinutes(1));

        insertDecision(connection, "invalid-validity-order", AS_OF,
                OffsetDateTime.parse("2026-07-14T13:00:00Z"),
                OffsetDateTime.parse("2026-07-14T12:00:00Z"));
        insertDecision(connection, "partial-validity", AS_OF,
                null, OffsetDateTime.parse("2026-07-14T13:00:00Z"));

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tm_analysis_run(
                    analysis_id, symbol, timeframe, analysis_time, status,
                    started_at, completed_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, "offset-candidate-analysis");
            statement.setString(2, "BTCUSDT");
            statement.setString(3, "4h");
            setTimestamp(statement, 4, LocalDateTime.parse("2026-07-14T03:00:00"));
            statement.setString(5, "SUCCESS");
            setTimestamp(statement, 6, LocalDateTime.parse("2026-07-14T03:00:00"));
            setTimestamp(statement, 7, LocalDateTime.parse("2026-07-14T03:01:00"));
            setTimestamp(statement, 8, LocalDateTime.parse("2026-07-14T11:00:00"));
            setTimestamp(statement, 9, LocalDateTime.parse("2026-07-14T11:00:00"));
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private static void insertMonitorAlert(
            Connection connection,
            String id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime cooldownUntil
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tm_monitor_alert(
                    id, asset_symbol, alert_type, alert_level, alert_message, status,
                    cooldown_until, created_at, updated_at, is_deleted, version_no
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 1)
                """)) {
            statement.setString(1, id);
            statement.setString(2, "BTCUSDT");
            statement.setString(3, "TIME_BASIS_FIXTURE");
            statement.setString(4, "INFO");
            statement.setString(5, "controlled aggregate-only fixture");
            statement.setString(6, "OPEN");
            setTimestamp(statement, 7, cooldownUntil);
            setTimestamp(statement, 8, createdAt);
            setTimestamp(statement, 9, updatedAt);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private static void insertDecision(
            Connection connection,
            String decisionId,
            LocalDateTime createTime,
            OffsetDateTime validFrom,
            OffsetDateTime expiresAt
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tm_decision_result(
                    decision_id, analysis_id, symbol, create_time, valid_from, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, decisionId);
            statement.setString(2, "fixture-analysis-" + decisionId);
            statement.setString(3, "BTCUSDT");
            setTimestamp(statement, 4, createTime);
            statement.setObject(5, validFrom);
            statement.setObject(6, expiresAt);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private static void setTimestamp(PreparedStatement statement, int index, LocalDateTime value)
            throws Exception {
        if (value == null) {
            statement.setTimestamp(index, null);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private static String lineStartingWith(List<String> lines, String prefix) {
        List<String> matches = lines.stream().filter(line -> line.startsWith(prefix)).toList();
        assertThat(matches).as("line starting with %s", prefix).hasSize(1);
        return matches.get(0);
    }

    private static String loadInventoryQuery() throws Exception {
        String script = Files.readString(Path.of("scripts/historical-time-basis-inventory.sql"));
        int start = script.indexOf(QUERY_START);
        int end = script.indexOf(QUERY_END);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        String query = script.substring(start + QUERY_START.length(), end).trim();
        if (query.endsWith(";")) {
            query = query.substring(0, query.length() - 1);
        }
        return query;
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @FunctionalInterface
    private interface Fixture {
        void insert(Connection connection) throws Exception;
    }
}
