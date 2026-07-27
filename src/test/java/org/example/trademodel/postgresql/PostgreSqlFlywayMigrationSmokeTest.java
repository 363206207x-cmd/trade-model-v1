package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.service.impl.MonitorAlertWriteServiceImpl;
import org.example.trademodel.service.impl.RunBaselineServiceImpl;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.RunBaselineVO;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgreSqlFlywayMigrationSmokeTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
    private static final String CONTROLLED_CONFIRM = "I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL";
    private static final String CONTROLLED_RUN = "I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB";

    @Test
    void postgreSqlCurrentMigrationRuntimeTest() throws Exception {
        DatabaseTarget controlledTarget = controlledDatabaseTarget();
        if (controlledTarget != null) {
            assertPostgreSqlRuntime(controlledTarget);
            return;
        }

        assumeTrue(dockerAvailable(), "Docker/Testcontainers is unavailable; PostgreSQL smoke skipped");

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)) {
            postgres.start();
            assertPostgreSqlRuntime(new DatabaseTarget(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        }
    }

    private static void assertPostgreSqlRuntime(DatabaseTarget target) throws Exception {
        Flyway.configure()
                .dataSource(target.jdbcUrl(), target.username(), target.password())
                .locations("classpath:db/migration")
                .target("8")
                .load()
                .migrate();

        long legacyPositionId;
        try (Connection connection = DriverManager.getConnection(
                target.jdbcUrl(), target.username(), target.password())) {
            legacyPositionId = insertLegacyUserPositionBeforeOwnershipMigration(connection);
        }

        Flyway.configure()
                .dataSource(target.jdbcUrl(), target.username(), target.password())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                target.jdbcUrl(), target.username(), target.password())) {
            assertThat(countTradeModelTables(connection)).isEqualTo(28);
            assertTablesExist(connection, List.of(
                    "tm_analysis_run",
                    "tm_decision_result",
                    "tm_execution_plan",
                    "tm_user_position",
                    "tm_position_monitor_log",
                    "tm_review_result",
                    "tm_opportunity_log",
                    "tm_push_snapshot",
                    "tm_push_recheck_log",
                    "tm_ai_call_log",
                    "tm_user",
                    "tm_asset_state"));
            assertIndexesExist(connection, List.of(
                    "idx_tm_user_position_status_opened_at",
                    "idx_tm_user_position_user_status_opened_at",
                    "idx_tm_push_snapshot_analysis_id",
                    "idx_tm_ai_call_log_trace_id",
                    "uk_tm_review_result_analysis_id",
                    "uk_tm_persisted_ohlcv_bar_source",
                    "idx_tm_persisted_ohlcv_bar_ingestion_run"));
            assertOhlcvProvenanceColumnsExist(connection);
            assertProviderScanProfileV5ColumnsExist(connection);
            assertProviderScanRuleDefaultsExist(connection);
            assertDerivativesBusinessRuleDefaultsExist(connection);
            assertDecisionPlanOffsetTimeColumnsExist(connection);
            assertUserPositionOwnershipV9Contract(connection, legacyPositionId);
            String profileUserId = assertProviderScanProfileSaveLoadAndAudit(connection);
            assertProviderScanProfileRollbackIsAtomic(connection, profileUserId);
            assertFlywayHistorySucceeded(connection);
            assertUserPositionIdentityGeneratedKey(connection);
        }
        assertMonitorAlertUtcNaiveAcrossSessionTimezones(target);
    }

    private static void assertMonitorAlertUtcNaiveAcrossSessionTimezones(
            DatabaseTarget target) throws Exception {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(target.jdbcUrl());
        dataSource.setUser(target.username());
        dataSource.setPassword(target.password());
        Environment environment = new Environment(
                "controlled-postgresql", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setDatabaseId("postgresql");
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(MonitorAlertMapper.class);
        SqlSessionFactory sessions = new SqlSessionFactoryBuilder().build(configuration);
        LocalDateTime expectedUtc = LocalDateTime.parse("2026-07-14T12:00:00");

        for (String sessionTimezone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
            try (SqlSession session = sessions.openSession(false)) {
                try (Statement statement = session.getConnection().createStatement()) {
                    statement.execute("SET TIME ZONE '" + sessionTimezone + "'");
                }
                MonitorAlertMapper mapper = session.getMapper(MonitorAlertMapper.class);
                String analysisId = "pg-time-basis-" + sessionTimezone.replace('/', '-');
                writeMonitorAlert(mapper, analysisId);

                try (PreparedStatement statement = session.getConnection().prepareStatement(
                        "SELECT created_at, updated_at, cooldown_until FROM tm_monitor_alert WHERE analysis_id = ?")) {
                    statement.setString(1, analysisId);
                    try (ResultSet result = statement.executeQuery()) {
                        assertThat(result.next()).isTrue();
                        assertThat(result.getObject(1, LocalDateTime.class)).isEqualTo(expectedUtc);
                        assertThat(result.getObject(2, LocalDateTime.class)).isEqualTo(expectedUtc);
                        assertThat(result.getObject(3, LocalDateTime.class))
                                .isEqualTo(LocalDateTime.parse("2026-07-14T12:15:00"));
                    }
                }

                RunBaselineVO baseline = runBaseline(mapper);
                assertThat(baseline.getAlertSummary().getOpenCountWindow()).isEqualTo(1);
                session.rollback();
            }
        }
    }

    private static void writeMonitorAlert(MonitorAlertMapper mapper, String analysisId) {
        MonitorAlertWriteServiceImpl writer = new MonitorAlertWriteServiceImpl(mapper);
        writer.setClock(Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC));
        AnalysisRunDO run = new AnalysisRunDO();
        run.setTraceId("trace-" + analysisId);
        run.setRuleVersion("v1");
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId(analysisId);
        analysis.setSymbol("BTCUSDT");
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setRiskLevel("HIGH");
        writer.emitAfterAnalysisPersist(run, analysis, decision);
    }

    private static RunBaselineVO runBaseline(MonitorAlertMapper mapper) {
        SystemHealthService systemHealthService = mock(SystemHealthService.class);
        PositionSyncService positionSyncService = mock(PositionSyncService.class);
        DecisionService decisionService = mock(DecisionService.class);
        RuntimeMetricService runtimeMetricService = mock(RuntimeMetricService.class);
        when(systemHealthService.getSystemHealth()).thenReturn(Map.of());
        when(runtimeMetricService.snapshot()).thenReturn(Map.of());
        when(decisionService.getLightSystemStatus()).thenReturn(new LightSystemStatusVO());
        RunBaselineServiceImpl baseline = new RunBaselineServiceImpl(
                systemHealthService,
                positionSyncService,
                decisionService,
                runtimeMetricService,
                mapper,
                mock(AnalysisRunMapper.class),
                mock(PushRecheckLogMapper.class),
                mock(HotResetEventMapper.class));
        baseline.setClock(Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC));
        return baseline.getRunBaseline(30);
    }

    private static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static DatabaseTarget controlledDatabaseTarget() {
        String jdbcUrl = System.getenv("CONTROLLED_POSTGRESQL_JDBC_URL");
        String username = System.getenv("CONTROLLED_POSTGRESQL_USERNAME");
        String password = System.getenv("CONTROLLED_POSTGRESQL_PASSWORD");
        if (!hasText(jdbcUrl) && !hasText(username) && !hasText(password)) {
            return null;
        }
        assertThat(jdbcUrl).as("controlled PostgreSQL JDBC URL").isNotBlank();
        assertThat(username).as("controlled PostgreSQL username").isNotBlank();
        assertThat(password).as("controlled PostgreSQL password presence").isNotBlank();
        assertThat(System.getenv("CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM"))
                .isEqualTo(CONTROLLED_CONFIRM);
        assertThat(System.getenv("CONTROLLED_POSTGRESQL_FLYWAY_RUN"))
                .isEqualTo(CONTROLLED_RUN);
        assertThat(jdbcUrl).isEqualTo(
                "jdbc:postgresql://127.0.0.1:55432/trade_model_v1_test");
        assertThat(username).isEqualTo("trade_model_test");
        assertThat(jdbcUrl.toLowerCase()).doesNotContain("prod", "production", "live", "primary", "main");
        return new DatabaseTarget(jdbcUrl, username, password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static int countTradeModelTables(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name LIKE 'tm_%'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getInt(1);
            }
        }
    }

    private static void assertTablesExist(Connection connection, List<String> tableNames) throws Exception {
        for (String tableName : tableNames) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_type = 'BASE TABLE'
                      AND table_name = ?
                    """)) {
                statement.setString(1, tableName);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("table %s", tableName).isEqualTo(1);
                }
            }
        }
    }

    private static void assertIndexesExist(Connection connection, List<String> indexNames) throws Exception {
        for (String indexName : indexNames) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*)
                    FROM pg_indexes
                    WHERE schemaname = 'public'
                      AND indexname = ?
                    """)) {
                statement.setString(1, indexName);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("index %s", indexName).isEqualTo(1);
                }
            }
        }
    }

    private static void assertFlywayHistorySucceeded(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(9);
            }
        }
    }

    private static long insertLegacyUserPositionBeforeOwnershipMigration(Connection connection) throws Exception {
        String sql = """
                INSERT INTO tm_user_position(
                    asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                    source_type, manual_review_required, not_trade_instruction, not_auto_trading,
                    not_order_execution, not_position_sync, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 8, 0);
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "LEGACYUSDT");
            statement.setString(2, "LONG");
            statement.setString(3, "OPEN");
            statement.setBigDecimal(4, new BigDecimal("25.50"));
            statement.setBigDecimal(5, new BigDecimal("1.25"));
            statement.setBigDecimal(6, new BigDecimal("1"));
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setString(8, "MANUAL");
            statement.setBoolean(9, true);
            statement.setBoolean(10, true);
            statement.setBoolean(11, true);
            statement.setBoolean(12, true);
            statement.setBoolean(13, true);
            statement.setTimestamp(14, Timestamp.valueOf(now));
            statement.setTimestamp(15, Timestamp.valueOf(now));
            assertThat(statement.executeUpdate()).isEqualTo(1);
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }

    private static void assertUserPositionOwnershipV9Contract(
            Connection connection,
            long legacyPositionId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT data_type, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'tm_user_position'
                  AND column_name = 'user_id'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("data_type")).isEqualTo("bigint");
                assertThat(rs.getString("is_nullable")).isEqualTo("YES");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'idx_tm_user_position_user_status_opened_at'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("indexdef"))
                        .contains("(user_id, status, opened_at)");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT pg_get_constraintdef(oid, true) AS definition, confdeltype, confupdtype
                FROM pg_constraint
                WHERE conname = 'fk_tm_user_position_user'
                  AND conrelid = 'public.tm_user_position'::regclass
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("definition"))
                        .contains("FOREIGN KEY (user_id) REFERENCES tm_user(id)")
                        .contains("ON UPDATE RESTRICT")
                        .contains("ON DELETE RESTRICT");
                assertThat(rs.getString("confdeltype")).isEqualTo("r");
                assertThat(rs.getString("confupdtype")).isEqualTo("r");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT user_id, asset_symbol, status, source_type
                FROM tm_user_position
                WHERE id = ?
                """)) {
            statement.setLong(1, legacyPositionId);
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getObject("user_id")).isNull();
                assertThat(rs.getString("asset_symbol")).isEqualTo("LEGACYUSDT");
                assertThat(rs.getString("status")).isEqualTo("OPEN");
                assertThat(rs.getString("source_type")).isEqualTo("MANUAL");
            }
        }
    }

    private static void assertDecisionPlanOffsetTimeColumnsExist(Connection connection) throws Exception {
        for (String column : List.of("valid_from", "expires_at")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'tm_decision_result'
                      AND column_name = ?
                    """)) {
                statement.setString(1, column);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).as("V7 column %s", column).isTrue();
                    assertThat(rs.getString(1)).as("V7 column %s type", column)
                            .isEqualTo("timestamp with time zone");
                }
            }
        }
    }

    private static void assertProviderScanProfileV5ColumnsExist(Connection connection) throws Exception {
        for (String column : List.of("scan_base_profile", "scan_position_profile", "scan_pool_profile",
                "scan_auto_escalation_enabled", "scan_manual_override_until", "scan_update_reason",
                "scan_updated_at")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'tm_user_config' AND column_name = ?
                    """)) {
                statement.setString(1, column);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("V5 column %s", column).isEqualTo(1);
                }
            }
        }
    }

    private static void assertProviderScanRuleDefaultsExist(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM tm_rule_config WHERE rule_key LIKE 'provider.scan.%'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(16);
            }
        }
    }

    private static void assertDerivativesBusinessRuleDefaultsExist(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM tm_rule_config WHERE rule_type LIKE 'derivatives_%_config'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(24);
            }
        }
    }

    private static String assertProviderScanProfileSaveLoadAndAudit(Connection connection) throws Exception {
        String suffix = UUID.randomUUID().toString();
        String profileUserId = "pg-v5-smoke-" + suffix;
        String auditId = "pg-v5-audit-" + suffix;
        Timestamp now = Timestamp.valueOf(LocalDateTime.of(2026, 7, 10, 12, 0));
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO tm_user_config(user_id, scan_base_profile, scan_position_profile, scan_pool_profile,
                  scan_auto_escalation_enabled, scan_manual_override_until, scan_update_reason, scan_updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            insert.setString(1, profileUserId); insert.setString(2, "AUTO"); insert.setString(3, "HIGH");
            insert.setString(4, "LOW"); insert.setBoolean(5, true); insert.setTimestamp(6, now);
            insert.setString(7, "V5_POSTGRESQL_SMOKE"); insert.setTimestamp(8, now);
            assertThat(insert.executeUpdate()).isEqualTo(1);
        }
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT scan_base_profile, scan_position_profile, scan_pool_profile,
                       scan_auto_escalation_enabled, scan_manual_override_until, scan_update_reason, scan_updated_at
                FROM tm_user_config WHERE user_id = ?
                """)) {
            query.setString(1, profileUserId);
            try (ResultSet rs = query.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("AUTO");
                assertThat(rs.getString(2)).isEqualTo("HIGH");
                assertThat(rs.getBoolean(4)).isTrue();
                assertThat(rs.getTimestamp(5)).isEqualTo(now);
            }
        }
        try (PreparedStatement audit = connection.prepareStatement("""
                INSERT INTO tm_rule_version_log(id, rule_version, change_category, change_summary, change_detail,
                  operator, publish_time, rollback_flag, created_by, updated_by, is_deleted, version_no)
                VALUES (?, 'v5', 'SCAN_PROFILE_CONFIG', 'profile saved', 'traceId=pg-v5',
                  'pg-smoke', '2026-07-10T12:00:00Z', 'N', 'pg-smoke', 'pg-smoke', 0, 1)
                """)) {
            audit.setString(1, auditId);
            assertThat(audit.executeUpdate()).isEqualTo(1);
        }
        return profileUserId;
    }

    private static void assertProviderScanProfileRollbackIsAtomic(
            Connection connection,
            String profileUserId) throws Exception {
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE tm_user_config SET scan_base_profile = 'EMERGENCY' WHERE user_id = ?
                """)) {
            update.setString(1, profileUserId);
            assertThat(update.executeUpdate()).isEqualTo(1);
            connection.rollback();
        } finally {
            connection.setAutoCommit(previous);
        }
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT scan_base_profile FROM tm_user_config WHERE user_id = ?
                """)) {
            query.setString(1, profileUserId);
            try (ResultSet rs = query.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("AUTO");
            }
        }
    }

    private static void assertOhlcvProvenanceColumnsExist(Connection connection) throws Exception {
        for (String column : List.of("fetch_time", "source_status", "freshness_status",
                "provenance_version", "ingestion_run_id")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'tm_persisted_ohlcv_bar'
                      AND column_name = ?
                    """)) {
                statement.setString(1, column);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("OHLCV column %s", column).isEqualTo(1);
                }
            }
        }
    }

    private static void assertUserPositionIdentityGeneratedKey(Connection connection) throws Exception {
        String sql = """
                INSERT INTO tm_user_position(
                    asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                    source_type, manual_review_required, not_trade_instruction, not_auto_trading,
                    not_order_execution, not_position_sync, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.of(2026, 6, 28, 9, 0);
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, "BTCUSDT");
                statement.setString(2, "LONG");
                statement.setString(3, "OPEN");
                statement.setBigDecimal(4, new BigDecimal("100.50"));
                statement.setBigDecimal(5, new BigDecimal("0.25"));
                statement.setBigDecimal(6, new BigDecimal("2"));
                statement.setTimestamp(7, Timestamp.valueOf(now));
                statement.setString(8, "MANUAL");
                statement.setBoolean(9, true);
                statement.setBoolean(10, true);
                statement.setBoolean(11, true);
                statement.setBoolean(12, true);
                statement.setBoolean(13, true);
                statement.setTimestamp(14, Timestamp.valueOf(now));
                statement.setTimestamp(15, Timestamp.valueOf(now));

                assertThat(statement.executeUpdate()).isEqualTo(1);
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    assertThat(keys.next()).isTrue();
                    assertThat(keys.getLong(1)).isPositive();
                }
            }
        } finally {
            connection.rollback();
            connection.setAutoCommit(previous);
        }
    }

    private record DatabaseTarget(String jdbcUrl, String username, String password) {
    }
}
