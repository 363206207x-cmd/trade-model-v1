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
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
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
import org.junit.jupiter.api.Tag;
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

@Tag("core-regression")
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
        long legacyMonitorLogId;
        try (Connection connection = DriverManager.getConnection(
                target.jdbcUrl(), target.username(), target.password())) {
            legacyPositionId = insertLegacyUserPositionBeforeOwnershipMigration(connection);
            legacyMonitorLogId = insertLegacyPositionMonitorBeforeContractMigration(connection, legacyPositionId);
            insertLegacyAssetStateBeforeDecisionChainMigration(connection);
        }

        Flyway.configure()
                .dataSource(target.jdbcUrl(), target.username(), target.password())
                .locations("classpath:db/migration")
                .target("11")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                target.jdbcUrl(), target.username(), target.password())) {
            insertLegacyDecisionChainV11Fixture(connection);
        }

        Flyway.configure()
                .dataSource(target.jdbcUrl(), target.username(), target.password())
                .locations("classpath:db/migration")
                .target("13")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                target.jdbcUrl(), target.username(), target.password())) {
            insertLegacyOwnerBeforeMultiUserMigration(connection);
            insertLegacyTelegramV13Fixture(connection);
        }

        Flyway.configure()
                .dataSource(target.jdbcUrl(), target.username(), target.password())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                target.jdbcUrl(), target.username(), target.password())) {
            assertThat(countTradeModelTables(connection)).isEqualTo(40);
            assertTablesExist(connection, List.of(
                    "tm_asset",
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
                    "tm_asset_state",
                    "tm_asset_pool_item",
                    "tm_opportunity_state_transition",
                    "tm_execution_plan_candidate",
                    "tm_conflict_resolver_result",
                    "tm_plan_revalidation_record",
                    "tm_message",
                    "tm_channel_delivery",
                    "tm_user_registration_guard",
                    "tm_owner_password_setup_token",
                    "tm_async_task",
                    "tm_event_asset_relation"));
            assertIndexesExist(connection, List.of(
                    "idx_tm_user_position_status_opened_at",
                    "idx_tm_user_position_user_status_opened_at",
                    "uk_tm_user_position_id_user",
                    "idx_tm_position_monitor_log_trust_freshness",
                    "idx_tm_push_snapshot_analysis_id",
                    "idx_tm_ai_call_log_trace_id",
                    "idx_tm_ai_call_log_candidate",
                    "idx_tm_asset_pool_active_order",
                    "idx_tm_opportunity_transition_opportunity_time",
                    "uk_tm_plan_candidate_analysis",
                    "uk_tm_conflict_resolver_candidate",
                    "idx_tm_execution_plan_lifecycle",
                    "idx_tm_plan_revalidation_plan",
                    "idx_tm_message_user_time",
                    "idx_tm_channel_delivery_message",
                    "idx_tm_async_task_owner_time",
                    "idx_tm_event_asset_symbol_time",
                    "uk_tm_channel_delivery_message_channel_active",
                    "idx_tm_channel_delivery_due",
                    "idx_tm_channel_delivery_cooldown",
                    "uk_tm_review_result_analysis_scope",
                    "idx_tm_review_result_user_update",
                    "uk_tm_persisted_ohlcv_bar_source",
                    "idx_tm_persisted_ohlcv_bar_ingestion_run"));
            assertOhlcvProvenanceColumnsExist(connection);
            assertProviderScanProfileV5ColumnsExist(connection);
            assertProviderScanRuleDefaultsExist(connection);
            assertDerivativesBusinessRuleDefaultsExist(connection);
            assertDecisionPlanOffsetTimeColumnsExist(connection);
            assertUserPositionOwnershipV9Contract(connection, legacyPositionId);
            assertPositionMonitorV10Contract(connection, legacyMonitorLogId);
            assertPositionMonitorV10NewRowDefaults(connection, legacyPositionId);
            assertDecisionChainV11Contract(connection);
            assertDecisionChainV12AuditTextRoundTrip(connection);
            assertFinalInteractionV13Contract(connection);
            assertTelegramDeliveryV14Contract(connection);
            assertPrivateMultiUserV15Contract(connection);
            String profileUserId = assertProviderScanProfileSaveLoadAndAudit(connection);
            assertProviderScanProfileRollbackIsAtomic(connection, profileUserId);
            assertFlywayHistorySucceeded(connection);
            assertUserPositionIdentityGeneratedKey(connection);
        }
        assertOpportunityRankingReadQueries(target);
        assertMonitorAlertUtcNaiveAcrossSessionTimezones(target);
    }

    private static void assertOpportunityRankingReadQueries(DatabaseTarget target) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(target.jdbcUrl());
        dataSource.setUser(target.username());
        dataSource.setPassword(target.password());
        Environment environment = new Environment(
                "controlled-postgresql-ranking", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setDatabaseId("postgresql");
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(DecisionResultMapper.class);
        configuration.addMapper(AssetStateMapper.class);
        SqlSessionFactory sessions = new SqlSessionFactoryBuilder().build(configuration);

        try (SqlSession session = sessions.openSession(false)) {
            assertThat(session.getMapper(DecisionResultMapper.class)
                    .findLatestDecisionResultsForSymbolsJoined(
                            List.of("BTCUSDT"), "SYSTEM", 0L)).isEmpty();
            assertThat(session.getMapper(AssetStateMapper.class)
                    .listBySymbols(List.of("BTCUSDT"))).isEmpty();
            session.rollback();
        }
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
        assertThat(jdbcUrl)
                .matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/trade_model_v1_test");
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
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(15);
            }
        }
    }

    private static void assertPrivateMultiUserV15Contract(Connection connection) throws Exception {
        for (String column : List.of(
                "role", "enabled", "session_version", "updated_at", "disabled_at", "owner_slot")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'tm_user' AND column_name = ?
                    """)) {
                statement.setString(1, column);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("V15 tm_user column %s", column).isEqualTo(1);
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, username, role, enabled, owner_slot
                FROM tm_user WHERE id = 1
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("username")).isEqualTo("xuchao");
                assertThat(rs.getString("role")).isEqualTo("OWNER");
                assertThat(rs.getBoolean("enabled")).isTrue();
                assertThat(rs.getInt("owner_slot")).isEqualTo(1);
                assertThat(rs.next()).isFalse();
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT max_active_accounts FROM tm_user_registration_guard WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(10);
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM tm_asset_pool_item
                WHERE owner_type = 'USER' AND owner_id = 1 AND source_type = 'USER_OVERRIDE'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(6);
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     INSERT INTO tm_user(username, password_hash, created_at, updated_at)
                     VALUES ('v15-sequence-user', '$2a$10$not-a-real-secret-hash',
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     RETURNING id
                     """)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(1)).isEqualTo(2L);
            assertThat(rs.next()).isFalse();
        }
        try (Statement statement = connection.createStatement()) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tm_user(
                      username, password_hash, role, enabled, session_version,
                      created_at, updated_at, owner_slot
                    ) VALUES ('another-owner', '$2a$10$not-a-real-secret-hash', 'OWNER', TRUE, 0,
                      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tm_user(username, password_hash, created_at, updated_at)
                    VALUES ('XUCHAO', '$2a$10$not-a-real-secret-hash', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    private static void insertLegacyOwnerBeforeMultiUserMigration(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            assertThat(statement.executeUpdate("""
                    INSERT INTO tm_user(id, username, password_hash, created_at)
                    VALUES (1, 'xuchao', '$2a$10$legacyOwnerHashForMigrationEvidenceOnly',
                      TIMESTAMP '2026-08-20 08:00:00')
                    """)).isEqualTo(1);
        }
    }

    private static void assertFinalInteractionV13Contract(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT analysis_mode FROM tm_analysis_run WHERE analysis_id = 'analysis-v11-chain'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("analysis_mode")).isEqualTo("OPPORTUNITY_DECISION");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT plan_lifecycle_state, plan_version
                FROM tm_execution_plan WHERE plan_id = 'final-plan-v11-chain'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("plan_lifecycle_state")).isEqualTo("INVALIDATED");
                assertThat(rs.getInt("plan_version")).isEqualTo(1);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT account_risk_coverage_state
                FROM tm_account_risk_snapshot
                ORDER BY create_time DESC, id DESC LIMIT 1
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    assertThat(rs.getString("account_risk_coverage_state")).isEqualTo("UNKNOWN");
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tm_async_task(
                      task_id, owner_type, owner_id, task_type, state, retry_count, max_retries,
                      created_at, updated_at
                    ) VALUES ('invalid-success-task', 'SYSTEM', 0, 'POOL_SCAN', 'SUCCESS', 0, 0,
                      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tm_message(
                      message_id, user_id, category, source_type, source_id, title,
                      business_state, read_state, dedupe_key, not_trade_instruction,
                      not_order_execution, created_at, updated_at
                    ) VALUES ('unsafe-message', 1, 'HIGH_PERMISSION_OPPORTUNITY', 'OPPORTUNITY', 'opp-1',
                      'unsafe', 'ACTIVE', 'UNREAD', 'unsafe-message', FALSE, FALSE,
                      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    private static void assertTelegramDeliveryV14Contract(Connection connection) throws Exception {
        for (String column : List.of(
                "next_attempt_at", "claimed_at", "lease_until", "claim_token",
                "last_response_code", "retry_after_seconds", "recipient_fingerprint",
                "cooldown_key", "severity_rank")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'tm_channel_delivery'
                      AND column_name = ?
                    """)) {
                statement.setString(1, column);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("V14 column %s", column).isEqualTo(1);
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'uk_tm_channel_delivery_message_channel_active'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).contains("message_id", "channel", "DUPLICATE_MIGRATED");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT delivery_id, status, error_code, next_attempt_at, severity_rank
                FROM tm_channel_delivery
                WHERE delivery_id IN (
                    'delivery-v13-sent', 'delivery-v13-duplicate', 'delivery-v13-queued'
                )
                ORDER BY delivery_id
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("delivery_id")).isEqualTo("delivery-v13-duplicate");
                assertThat(rs.getString("status")).isEqualTo("SUPPRESSED");
                assertThat(rs.getString("error_code")).isEqualTo("DUPLICATE_MIGRATED");
                assertThat(rs.getObject("next_attempt_at")).isNull();
                assertThat(rs.getInt("severity_rank")).isZero();

                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("delivery_id")).isEqualTo("delivery-v13-queued");
                assertThat(rs.getString("status")).isEqualTo("QUEUED");
                assertThat(rs.getString("error_code")).isNull();
                assertThat(rs.getObject("next_attempt_at", LocalDateTime.class))
                        .isEqualTo(LocalDateTime.of(2026, 8, 16, 8, 2));
                assertThat(rs.getInt("severity_rank")).isZero();

                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("delivery_id")).isEqualTo("delivery-v13-sent");
                assertThat(rs.getString("status")).isEqualTo("SENT");
                assertThat(rs.getString("error_code")).isNull();
                assertThat(rs.getObject("next_attempt_at")).isNull();
                assertThat(rs.getInt("severity_rank")).isZero();
                assertThat(rs.next()).isFalse();
            }
        }
        for (String messageId : List.of(
                "message-v13-newer-sent", "message-v13-older-sent",
                "message-v13-multiple-sent", "message-v13-sent-sending")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*) FILTER (WHERE status = 'SENT') AS sent_count,
                           COUNT(*) FILTER (WHERE status IN ('QUEUED','RETRYING','SENDING')) AS due_capable_count,
                           COUNT(*) FILTER (WHERE status = 'SUPPRESSED'
                             AND error_code = 'DUPLICATE_MIGRATED'
                             AND next_attempt_at IS NULL AND claim_token IS NULL
                             AND claimed_at IS NULL AND lease_until IS NULL) AS clean_duplicate_count,
                           COUNT(*) AS total_count
                    FROM tm_channel_delivery WHERE message_id = ?
                    """)) {
                statement.setString(1, messageId);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt("sent_count")).as(messageId).isEqualTo(1);
                    assertThat(rs.getInt("due_capable_count")).as(messageId).isZero();
                    assertThat(rs.getInt("clean_duplicate_count")).as(messageId).isEqualTo(1);
                    assertThat(rs.getInt("total_count")).as(messageId).isEqualTo(2);
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT delivery_id FROM tm_channel_delivery
                WHERE message_id = 'message-v13-multiple-sent' AND status = 'SENT'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("delivery-v13-sent-newer");
                assertThat(rs.next()).isFalse();
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT status, error_code, next_attempt_at FROM tm_channel_delivery
                WHERE message_id = 'message-v13-no-sent' AND error_code IS DISTINCT FROM 'DUPLICATE_MIGRATED'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("status")).isEqualTo("FAILED");
                assertThat(rs.getString("error_code")).isEqualTo("DELIVERY_OUTCOME_UNKNOWN");
                assertThat(rs.getObject("next_attempt_at")).isNull();
                assertThat(rs.next()).isFalse();
            }
        }
    }

    private static void insertLegacyTelegramV13Fixture(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            assertThat(statement.executeUpdate("""
                    INSERT INTO tm_message(
                      message_id, user_id, category, source_type, source_id, title,
                      business_state, read_state, dedupe_key, created_at, updated_at
                    ) VALUES
                      ('message-v13-duplicate', 1, 'HIGH_PERMISSION_OPPORTUNITY',
                       'OPPORTUNITY', 'opportunity-v13', 'legacy duplicate', 'ACTIVE',
                       'UNREAD', 'message-v13-duplicate',
                       TIMESTAMP '2026-08-16 08:00:00', TIMESTAMP '2026-08-16 08:00:00'),
                      ('message-v13-queued', 1, 'POSITION_LOGIC_RISK_CHANGE',
                       'POSITION', 'position-v13', 'legacy queued', 'ACTIVE',
                       'UNREAD', 'message-v13-queued',
                       TIMESTAMP '2026-08-16 08:02:00', TIMESTAMP '2026-08-16 08:02:00'),
                      ('message-v13-newer-sent', 1, 'HIGH_PERMISSION_OPPORTUNITY',
                       'OPPORTUNITY', 'opp-newer-sent', 'newer sent wins', 'ACTIVE',
                       'UNREAD', 'message-v13-newer-sent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                      ('message-v13-older-sent', 1, 'HIGH_PERMISSION_OPPORTUNITY',
                       'OPPORTUNITY', 'opp-older-sent', 'older sent wins', 'ACTIVE',
                       'UNREAD', 'message-v13-older-sent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                      ('message-v13-multiple-sent', 1, 'HIGH_PERMISSION_OPPORTUNITY',
                       'OPPORTUNITY', 'opp-multiple-sent', 'deterministic sent wins', 'ACTIVE',
                       'UNREAD', 'message-v13-multiple-sent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                      ('message-v13-sent-sending', 1, 'HIGH_PERMISSION_OPPORTUNITY',
                       'OPPORTUNITY', 'opp-sent-sending', 'sent beats sending', 'ACTIVE',
                       'UNREAD', 'message-v13-sent-sending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                      ('message-v13-no-sent', 1, 'HIGH_PERMISSION_OPPORTUNITY',
                       'OPPORTUNITY', 'opp-no-sent', 'no sent deterministic', 'ACTIVE',
                       'UNREAD', 'message-v13-no-sent', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """)).isEqualTo(7);
            statement.executeUpdate("ALTER TABLE tm_channel_delivery DROP CONSTRAINT ck_tm_channel_delivery_status");
            assertThat(statement.executeUpdate("""
                    INSERT INTO tm_channel_delivery(
                      delivery_id, message_id, user_id, channel, status,
                      attempt_count, attempted_at, delivered_at, created_at, updated_at
                    ) VALUES
                      ('delivery-v13-sent', 'message-v13-duplicate', 1, 'TELEGRAM',
                       'DELIVERED', 1, TIMESTAMP '2026-08-16 08:00:30',
                       TIMESTAMP '2026-08-16 08:00:31',
                       TIMESTAMP '2026-08-16 08:00:00', TIMESTAMP '2026-08-16 08:00:31'),
                      ('delivery-v13-duplicate', 'message-v13-duplicate', 1, 'TELEGRAM',
                       'QUEUED', 0, NULL, NULL,
                       TIMESTAMP '2026-08-16 08:01:00', TIMESTAMP '2026-08-16 08:01:00'),
                      ('delivery-v13-queued', 'message-v13-queued', 1, 'TELEGRAM',
                       'QUEUED', 0, NULL, NULL,
                       TIMESTAMP '2026-08-16 08:02:00', TIMESTAMP '2026-08-16 08:02:00'),
                      ('delivery-v13-newer-sent-queued', 'message-v13-newer-sent', 1, 'TELEGRAM',
                       'QUEUED', 0, NULL, NULL, TIMESTAMP '2026-08-16 08:00:00', TIMESTAMP '2026-08-16 08:00:00'),
                      ('delivery-v13-newer-sent', 'message-v13-newer-sent', 1, 'TELEGRAM',
                       'DELIVERED', 1, CURRENT_TIMESTAMP, TIMESTAMP '2026-08-16 08:05:00',
                       TIMESTAMP '2026-08-16 08:04:00', TIMESTAMP '2026-08-16 08:05:00'),
                      ('delivery-v13-older-sent', 'message-v13-older-sent', 1, 'TELEGRAM',
                       'DELIVERED', 1, CURRENT_TIMESTAMP, TIMESTAMP '2026-08-16 08:00:00',
                       TIMESTAMP '2026-08-16 08:00:00', TIMESTAMP '2026-08-16 08:00:00'),
                      ('delivery-v13-older-sent-queued', 'message-v13-older-sent', 1, 'TELEGRAM',
                       'QUEUED', 0, NULL, NULL, TIMESTAMP '2026-08-16 08:06:00', TIMESTAMP '2026-08-16 08:06:00'),
                      ('delivery-v13-sent-older', 'message-v13-multiple-sent', 1, 'TELEGRAM',
                       'DELIVERED', 1, CURRENT_TIMESTAMP, TIMESTAMP '2026-08-16 08:01:00',
                       TIMESTAMP '2026-08-16 08:01:00', TIMESTAMP '2026-08-16 08:01:00'),
                      ('delivery-v13-sent-newer', 'message-v13-multiple-sent', 1, 'TELEGRAM',
                       'DELIVERED', 1, CURRENT_TIMESTAMP, TIMESTAMP '2026-08-16 08:07:00',
                       TIMESTAMP '2026-08-16 08:02:00', TIMESTAMP '2026-08-16 08:07:00'),
                      ('delivery-v13-sent-active', 'message-v13-sent-sending', 1, 'TELEGRAM',
                       'DELIVERED', 1, CURRENT_TIMESTAMP, TIMESTAMP '2026-08-16 08:01:00',
                       TIMESTAMP '2026-08-16 08:01:00', TIMESTAMP '2026-08-16 08:01:00'),
                      ('delivery-v13-sending-duplicate', 'message-v13-sent-sending', 1, 'TELEGRAM',
                       'SENDING', 1, CURRENT_TIMESTAMP, NULL,
                       TIMESTAMP '2026-08-16 08:08:00', TIMESTAMP '2026-08-16 08:08:00'),
                      ('delivery-v13-sending-only', 'message-v13-no-sent', 1, 'TELEGRAM',
                       'SENDING', 1, CURRENT_TIMESTAMP, NULL,
                       TIMESTAMP '2026-08-16 08:08:00', TIMESTAMP '2026-08-16 08:08:00'),
                      ('delivery-v13-retrying-duplicate', 'message-v13-no-sent', 1, 'TELEGRAM',
                       'RETRYING', 1, CURRENT_TIMESTAMP, NULL,
                       TIMESTAMP '2026-08-16 08:07:00', TIMESTAMP '2026-08-16 08:07:00')
                    """)).isEqualTo(13);
        }
    }

    private static void insertLegacyAssetStateBeforeDecisionChainMigration(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tm_asset_state(
                  symbol, state, confused_score, confused_low_streak, last_update_time, trace_id
                ) VALUES ('LEGACYV11USDT', 'legacy_state', 5, 0, ?, 'legacy-v11-trace')
                """)) {
            statement.setObject(1, LocalDateTime.of(2026, 7, 27, 8, 10));
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private static void insertLegacyDecisionChainV11Fixture(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            assertThat(statement.executeUpdate("""
                    INSERT INTO tm_analysis_run(
                      analysis_id, symbol, timeframe, analysis_time, trace_id, status, created_at, updated_at
                    ) VALUES ('analysis-v11-chain', 'V11CHAINUSDT', '5m', CURRENT_TIMESTAMP,
                      'trace-v11-chain', 'SUCCESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """)).isEqualTo(1);
            assertThat(statement.executeUpdate("""
                    INSERT INTO tm_asset_state(
                      symbol, state, confused_score, opportunity_id, state_entered_at,
                      last_update_time, trace_id
                    ) VALUES ('V11CHAINUSDT', 'CANDIDATE', 10, 'opp-v11-chain', CURRENT_TIMESTAMP,
                      CURRENT_TIMESTAMP, 'trace-v11-chain')
                    """)).isEqualTo(1);
            assertThat(statement.executeUpdate("""
                    INSERT INTO tm_execution_plan_candidate(
                      candidate_id, opportunity_id, analysis_id, trace_id,
                      rule_direction, rule_confidence, rule_risk, candidate_direction,
                      plan_mode, confidence_level, risk_level, worth_opening,
                      recommended_action, entry_zone, stop_loss, take_profit_rules,
                      leverage_suggestion, position_suggestion, invalid_condition, validity,
                      summary, candidate_source, candidate_status, payload_json,
                      not_final_plan, not_state_machine_mutation, not_user_position_creation, created_at
                    ) VALUES ('candidate-v11-chain', 'opp-v11-chain', 'analysis-v11-chain', 'trace-v11-chain',
                      'BULLISH', 'HIGH', 'MEDIUM', 'BULLISH', 'CONFIRM', 'HIGH', 'MEDIUM', TRUE,
                      'MANUAL_REVIEW', '100-101', '95', '110 then 120', '1x', 'small',
                      'close below 95', '2026-08-12T00:00Z', 'candidate only', 'GPT_FINAL', 'VALIDATED',
                      '{}', TRUE, TRUE, TRUE, CURRENT_TIMESTAMP)
                    """)).isEqualTo(1);
            assertThat(statement.executeUpdate("""
                    INSERT INTO tm_conflict_resolver_result(
                      resolver_result_id, candidate_id, analysis_id, trace_id,
                      rule_direction, rule_confidence, rule_risk,
                      gemini_review_json, grok_challenge_json, conflict_level, conflict_score,
                      plan_mode_before, plan_mode_after, confidence_before, confidence_after,
                      risk_before, risk_after, confused_decision, rule_direction_preserved, created_at
                    ) VALUES ('resolver-v11-chain', 'candidate-v11-chain', 'analysis-v11-chain', 'trace-v11-chain',
                      'BULLISH', 'HIGH', 'MEDIUM', '{}', '{}', 'LEVEL_1_CONSISTENT', 0,
                      'CONFIRM', 'CONFIRM', 'HIGH', 'HIGH', 'MEDIUM', 'MEDIUM', FALSE, TRUE,
                      CURRENT_TIMESTAMP)
                    """)).isEqualTo(1);
            assertThat(statement.executeUpdate("""
                    INSERT INTO tm_execution_plan(
                      plan_id, analysis_id, plan_mode, execution_plan_status,
                      source_gate_status, source_gate_complete, candidate_id, opportunity_id,
                      resolver_result_id, trace_id, chain_status, rule_validation_status,
                      finalized_at, final_plan, create_time
                    ) VALUES ('final-plan-v11-chain', 'analysis-v11-chain', 'CONFIRM', 'VALID',
                      'VALID', TRUE, 'candidate-v11-chain', 'opp-v11-chain', 'resolver-v11-chain',
                      'trace-v11-chain', 'FINAL_VALIDATED', 'PASS', CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP)
                    """)).isEqualTo(1);
        }
    }

    private static void assertDecisionChainV11Contract(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM tm_asset_pool_item
                WHERE owner_type = 'SYSTEM' AND owner_id = 0 AND active = TRUE
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(6);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT state, timeframe, opportunity_id, state_entered_at,
                       last_transition_reason, last_trigger_source
                FROM tm_asset_state WHERE symbol = 'LEGACYV11USDT'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("state")).isEqualTo("OBSERVING");
                assertThat(rs.getString("timeframe")).isEqualTo("global");
                assertThat(rs.getString("opportunity_id")).isEqualTo("opp-legacyv11usdt-global");
                assertThat(rs.getObject("state_entered_at")).isNotNull();
                assertThat(rs.getString("last_transition_reason")).isEqualTo("LEGACY_STATE_ADOPTED");
                assertThat(rs.getString("last_trigger_source")).isEqualTo("LEGACY_ANALYSIS");
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT candidate_id, resolver_result_id, final_plan, rule_validation_status,
                       chain_status, rule_veto_reason
                FROM tm_execution_plan WHERE plan_id = 'final-plan-v11-chain'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("candidate_id")).isEqualTo("candidate-v11-chain");
                assertThat(rs.getString("resolver_result_id")).isEqualTo("resolver-v11-chain");
                assertThat(rs.getBoolean("final_plan")).isFalse();
                assertThat(rs.getString("rule_validation_status")).isEqualTo("BLOCKED");
                assertThat(rs.getString("chain_status")).isEqualTo("RULE_VALIDATION_BLOCKED");
                assertThat(rs.getString("rule_veto_reason"))
                        .contains("V12_FINAL_CONTRACT_FIELDS_UNAVAILABLE");
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT plan_mode, rule_direction, candidate_direction
                FROM tm_execution_plan_candidate WHERE candidate_id = 'candidate-v11-chain'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("plan_mode")).isEqualTo("CONFIRMATION");
                assertThat(rs.getString("rule_direction")).isEqualTo("BULLISH");
                assertThat(rs.getString("candidate_direction")).isEqualTo("BULLISH");
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT plan_mode_before, plan_mode_after, rule_direction
                FROM tm_conflict_resolver_result WHERE resolver_result_id = 'resolver-v11-chain'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("plan_mode_before")).isEqualTo("CONFIRMATION");
                assertThat(rs.getString("plan_mode_after")).isEqualTo("CONFIRMATION");
                assertThat(rs.getString("rule_direction")).isEqualTo("BULLISH");
            }
        }

        try (Statement statement = connection.createStatement()) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tm_execution_plan(
                      plan_id, analysis_id, chain_status, rule_validation_status, final_plan, create_time
                    ) VALUES ('invalid-final-v11-chain', 'analysis-v11-chain',
                      'FINAL_VALIDATED', 'PASS', TRUE, CURRENT_TIMESTAMP)
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    UPDATE tm_execution_plan_candidate SET plan_mode = 'ADVISORY'
                    WHERE candidate_id = 'candidate-v11-chain'
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    UPDATE tm_execution_plan_candidate SET candidate_direction = 'LONG'
                    WHERE candidate_id = 'candidate-v11-chain'
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    UPDATE tm_conflict_resolver_result SET plan_mode_after = 'ADVISORY'
                    WHERE resolver_result_id = 'resolver-v11-chain'
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThat(statement.executeUpdate("""
                    INSERT INTO tm_user_position(
                      user_id, asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                      source_type, final_plan_id
                    ) VALUES (1, 'V11CHAINUSDT', 'LONG', 'OPEN', 100, 1, 1,
                      CURRENT_TIMESTAMP, 'SYSTEM_PLAN_POSITION', 'final-plan-v11-chain')
                    """)).isEqualTo(1);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tm_user_position(
                      user_id, asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                      source_type, final_plan_id
                    ) VALUES (1, 'V11CHAINUSDT', 'LONG', 'OPEN', 100, 1, 1,
                      CURRENT_TIMESTAMP, 'SYSTEM_PLAN_POSITION', NULL)
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tm_user_position(
                      user_id, asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                      source_type, final_plan_id
                    ) VALUES (1, 'V11CHAINUSDT', 'LONG', 'OPEN', 100, 1, 1,
                      CURRENT_TIMESTAMP, 'MANUAL_POSITION', 'final-plan-v11-chain')
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO tm_user_position(
                      user_id, asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                      source_type, final_plan_id
                    ) VALUES (1, 'V11CHAINUSDT', 'LONG', 'OPEN', 100, 1, 1,
                      CURRENT_TIMESTAMP, 'AUTO', 'final-plan-v11-chain')
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    private static void assertDecisionChainV12AuditTextRoundTrip(Connection connection) throws Exception {
        for (String table : List.of("tm_execution_plan", "tm_conflict_resolver_result")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = ?
                      AND column_name = 'rule_veto_reason'
                    """)) {
                statement.setString(1, table);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).as("V12 %s.rule_veto_reason", table).isTrue();
                    assertThat(rs.getString("data_type")).isEqualTo("text");
                }
            }
        }

        String longAuditReason = "V12_ORDERED_RULE_VALIDATION_REASON;".repeat(24);
        assertThat(longAuditReason.length()).isGreaterThan(512);
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tm_execution_plan
                SET rule_veto_reason = ?
                WHERE plan_id = 'final-plan-v11-chain'
                """)) {
            statement.setString(1, longAuditReason);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tm_conflict_resolver_result
                SET rule_veto_reason = ?
                WHERE resolver_result_id = 'resolver-v11-chain'
                """)) {
            statement.setString(1, longAuditReason);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
        for (String query : List.of(
                "SELECT rule_veto_reason FROM tm_execution_plan WHERE plan_id = 'final-plan-v11-chain'",
                "SELECT rule_veto_reason FROM tm_conflict_resolver_result "
                        + "WHERE resolver_result_id = 'resolver-v11-chain'")) {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(query)) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo(longAuditReason);
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

    private static long insertLegacyPositionMonitorBeforeContractMigration(
            Connection connection, long positionId) throws Exception {
        String sql = """
                INSERT INTO tm_position_monitor_log(
                    position_id, analysis_id, execution_plan_id, current_price, logic_status,
                    risk_level, suggested_action, reason, trace_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 27, 8, 5);
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, positionId);
            statement.setString(2, "legacy-position-monitor");
            statement.setString(3, "legacy-plan");
            statement.setBigDecimal(4, new BigDecimal("100"));
            statement.setString(5, "LOGIC_WEAKENED");
            statement.setString(6, "LEGACY_UNKNOWN");
            statement.setString(7, "MANUAL_REVIEW");
            statement.setString(8, "legacy monitor evidence");
            statement.setString(9, "legacy-monitor-trace");
            statement.setObject(10, createdAt);
            assertThat(statement.executeUpdate()).isEqualTo(1);
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }

    private static void assertPositionMonitorV10Contract(Connection connection, long logId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT logic_status, entry_logic_status, monitor_conclusion, reversal_status,
                       risk_change_reason, risk_level, risk_trend, suggested_action, source_status,
                       observed_at, fresh_until, mark_price_source
                FROM tm_position_monitor_log
                WHERE log_id = ?
                """)) {
            statement.setLong(1, logId);
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("logic_status")).isEqualTo("LOGIC_WEAKENED");
                assertThat(rs.getString("entry_logic_status")).isNull();
                assertThat(rs.getString("monitor_conclusion")).isNull();
                assertThat(rs.getString("reversal_status")).isNull();
                assertThat(rs.getString("risk_change_reason")).isNull();
                assertThat(rs.getString("risk_level")).isNull();
                assertThat(rs.getString("risk_trend")).isNull();
                assertThat(rs.getString("suggested_action")).isNull();
                assertThat(rs.getString("source_status")).isEqualTo("PENDING_VERIFICATION");
                assertThat(rs.getObject("observed_at", LocalDateTime.class))
                        .isEqualTo(LocalDateTime.of(2026, 7, 27, 8, 5));
                assertThat(rs.getObject("fresh_until", LocalDateTime.class))
                        .isEqualTo(LocalDateTime.of(2026, 7, 27, 8, 5));
                assertThat(rs.getString("mark_price_source")).isNull();
            }
        }
    }

    private static void assertPositionMonitorV10NewRowDefaults(
            Connection connection, long positionId) throws Exception {
        LocalDateTime observedAt = LocalDateTime.of(2026, 7, 27, 9, 0);
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO tm_position_monitor_log(
                    position_id, analysis_id, current_price, observed_at, fresh_until, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                RETURNING source_status, entry_logic_status, monitor_conclusion, reversal_status,
                          risk_change_reason, risk_level, risk_trend, suggested_action
                """)) {
            insert.setLong(1, positionId);
            insert.setString(2, "post-v10-default-monitor");
            insert.setBigDecimal(3, new BigDecimal("101.25"));
            insert.setObject(4, observedAt);
            insert.setObject(5, observedAt);
            insert.setObject(6, observedAt);
            try (ResultSet rs = insert.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("source_status")).isEqualTo("PENDING_VERIFICATION");
                assertThat(rs.getString("entry_logic_status")).isNull();
                assertThat(rs.getString("monitor_conclusion")).isNull();
                assertThat(rs.getString("reversal_status")).isNull();
                assertThat(rs.getString("risk_change_reason")).isNull();
                assertThat(rs.getString("risk_level")).isNull();
                assertThat(rs.getString("risk_trend")).isNull();
                assertThat(rs.getString("suggested_action")).isNull();
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
                assertThat(rs.getString("is_nullable")).isEqualTo("NO");
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
                assertThat(rs.getLong("user_id")).isEqualTo(1L);
                assertThat(rs.getString("asset_symbol")).isEqualTo("LEGACYUSDT");
                assertThat(rs.getString("status")).isEqualTo("OPEN");
                assertThat(rs.getString("source_type")).isEqualTo("MANUAL_INDEPENDENT");
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
                    user_id, asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                    source_type, manual_review_required, not_trade_instruction, not_auto_trading,
                    not_order_execution, not_position_sync, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.of(2026, 6, 28, 9, 0);
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, 1L);
                statement.setString(2, "BTCUSDT");
                statement.setString(3, "LONG");
                statement.setString(4, "OPEN");
                statement.setBigDecimal(5, new BigDecimal("100.50"));
                statement.setBigDecimal(6, new BigDecimal("0.25"));
                statement.setBigDecimal(7, new BigDecimal("2"));
                statement.setTimestamp(8, Timestamp.valueOf(now));
                statement.setString(9, "MANUAL_INDEPENDENT");
                statement.setBoolean(10, true);
                statement.setBoolean(11, true);
                statement.setBoolean(12, true);
                statement.setBoolean(13, true);
                statement.setBoolean(14, true);
                statement.setTimestamp(15, Timestamp.valueOf(now));
                statement.setTimestamp(16, Timestamp.valueOf(now));

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
