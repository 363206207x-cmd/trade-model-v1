package org.example.trademodel.postgresql;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.positionmonitor.PositionPlanSourceResolver;
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
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.postgresql.ds.PGSimpleDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("controlled-postgresql")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ControlledPostgreSqlFlywayV7EvidenceTest {

    private static final String CONFIRM_VALUE = "I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL";
    private static final String RUN_VALUE = "I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB";
    private static final String BASE_URL = "jdbc:postgresql://127.0.0.1:55432/";
    private static final String CONTROL_DATABASE = "trade_model_v1_test";
    private static final String FRESH_DATABASE = "trade_model_v1_fresh_test";
    private static final String UPGRADE_DATABASE = "trade_model_v1_upgrade_test";
    private static final LocalDateTime WINDOW_START = LocalDateTime.parse("2026-07-14T11:30:00");
    private static final LocalDateTime AS_OF = LocalDateTime.parse("2026-07-14T12:00:00");
    private static final Instant AS_OF_INSTANT = Instant.parse("2026-07-14T12:00:00Z");

    private static ControlledTarget target;

    @BeforeAll
    static void prepareDisposableDatabases() throws Exception {
        String jdbcUrl = env("CONTROLLED_POSTGRESQL_JDBC_URL");
        String username = env("CONTROLLED_POSTGRESQL_USERNAME");
        String password = env("CONTROLLED_POSTGRESQL_PASSWORD");

        assumeTrue(hasText(jdbcUrl) && hasText(username) && hasText(password),
                "Controlled PostgreSQL env is missing; evidence test is environment-gated");
        assertThat(jdbcUrl).isEqualTo(BASE_URL + CONTROL_DATABASE);
        assertThat(username).isEqualTo("trade_model_test");
        assertThat(env("CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM")).isEqualTo(CONFIRM_VALUE);
        assertThat(env("CONTROLLED_POSTGRESQL_FLYWAY_RUN")).isEqualTo(RUN_VALUE);
        assertThat(jdbcUrl.toLowerCase()).doesNotContain("prod", "production", "live", "primary", "main");

        target = new ControlledTarget(username, password);
        recreateDatabase(FRESH_DATABASE);
        recreateDatabase(UPGRADE_DATABASE);
    }

    @AfterAll
    static void removeDisposableDatabases() throws Exception {
        if (target == null || Boolean.parseBoolean(env("CONTROLLED_POSTGRESQL_KEEP_EVIDENCE_DATABASES"))) {
            return;
        }
        dropDatabase(FRESH_DATABASE);
        dropDatabase(UPGRADE_DATABASE);
    }

    @Test
    @Order(1)
    void freshV1ToV7MigrationAndIdempotentRerunPass() throws Exception {
        Flyway flyway = flyway(FRESH_DATABASE, "7");

        MigrateResult first = flyway.migrate();
        assertThat(first.success).isTrue();
        assertThat(first.migrationsExecuted).isEqualTo(7);
        assertValidHistory(flyway);

        try (Connection connection = connect(FRESH_DATABASE)) {
            assertThat(count(connection, """
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_name LIKE 'tm_%'
                    """)).isEqualTo(27);
            assertV7Types(connection);
            assertExecutionPlanSafetyDefaults(connection);
            assertHistoryRows(connection);
        }

        MigrateResult second = flyway.migrate();
        assertThat(second.success).isTrue();
        assertThat(second.migrationsExecuted).isZero();
        assertValidHistory(flyway);
    }

    @Test
    @Order(2)
    void v6ToV7UpgradePreservesHistoricalRowsAndSupportsOffsetAwareWrites() throws Exception {
        Flyway throughV6 = flyway(UPGRADE_DATABASE, "6");
        MigrateResult v6Result = throughV6.migrate();
        assertThat(v6Result.success).isTrue();
        assertThat(v6Result.migrationsExecuted).isEqualTo(6);
        assertThat(v6Result.targetSchemaVersion).isEqualTo("6");

        insertHistoricalV6Rows();

        Flyway throughV7 = flyway(UPGRADE_DATABASE, "7");
        MigrateResult v7Result = throughV7.migrate();
        assertThat(v7Result.success).isTrue();
        assertThat(v7Result.migrationsExecuted).isEqualTo(1);
        assertValidHistory(throughV7);

        try (Connection connection = connect(UPGRADE_DATABASE)) {
            assertHistoryRows(connection);
            assertV7Types(connection);
            assertThat(count(connection,
                    "SELECT COUNT(*) FROM tm_analysis_run WHERE analysis_id = 'upgrade-analysis-a'"))
                    .isEqualTo(1);
            assertThat(count(connection,
                    "SELECT COUNT(*) FROM tm_decision_result WHERE decision_id = 'upgrade-decision-a'"))
                    .isEqualTo(1);
            assertThat(count(connection,
                    "SELECT COUNT(*) FROM tm_execution_plan WHERE plan_id = 'upgrade-plan-a'"))
                    .isEqualTo(1);
            assertThat(count(connection, """
                    SELECT COUNT(*) FROM tm_decision_result
                    WHERE decision_id = 'upgrade-decision-a'
                      AND valid_from IS NULL AND expires_at IS NULL
                    """)).isEqualTo(1);
        }

        SqlSessionFactory sessions = sqlSessions(UPGRADE_DATABASE);
        try (SqlSession session = sessions.openSession(false)) {
            AnalysisRunMapper runMapper = session.getMapper(AnalysisRunMapper.class);
            DecisionResultMapper decisionMapper = session.getMapper(DecisionResultMapper.class);
            ExecutionPlanMapper planMapper = session.getMapper(ExecutionPlanMapper.class);

            assertThat(runMapper.selectById("upgrade-analysis-a")).isNotNull();
            assertThat(decisionMapper.selectByDecisionId("upgrade-decision-a")).isNotNull();
            assertThat(decisionMapper.selectByDecisionId("upgrade-decision-a").getValidFrom()).isNull();
            assertThat(planMapper.selectByPlanId("upgrade-plan-a")).isNotNull();

            DecisionResult plusEight = offsetDecision(
                    "upgrade-decision-offset-plus8",
                    OffsetDateTime.parse("2026-07-14T20:00:00+08:00"),
                    OffsetDateTime.parse("2026-07-15T20:00:00+08:00"));
            DecisionResult minusFour = offsetDecision(
                    "upgrade-decision-offset-minus4",
                    OffsetDateTime.parse("2026-07-14T08:00:00-04:00"),
                    OffsetDateTime.parse("2026-07-15T08:00:00-04:00"));
            assertThat(decisionMapper.insert(plusEight)).isEqualTo(1);
            assertThat(decisionMapper.insert(minusFour)).isEqualTo(1);

            DecisionResult loadedPlusEight = decisionMapper.selectByDecisionId(plusEight.getDecisionId());
            DecisionResult loadedMinusFour = decisionMapper.selectByDecisionId(minusFour.getDecisionId());
            assertThat(loadedPlusEight.getValidFrom().toInstant()).isEqualTo(AS_OF_INSTANT);
            assertThat(loadedMinusFour.getValidFrom().toInstant()).isEqualTo(AS_OF_INSTANT);
            assertThat(loadedPlusEight.getExpiresAt().toInstant())
                    .isEqualTo(Instant.parse("2026-07-15T12:00:00Z"));
            assertThat(loadedMinusFour.getExpiresAt().toInstant())
                    .isEqualTo(Instant.parse("2026-07-15T12:00:00Z"));

            PositionPlanSourceResolver resolver = new PositionPlanSourceResolver(planMapper, runMapper);
            PositionPlanSourceResolver.Resolution resolution = resolver.resolveTypedReference(
                    42L,
                    "BTCUSDT",
                    PositionMonitorSourceContract.executionPlanReference("upgrade-plan-a"));
            assertThat(resolution.verified()).isTrue();
            assertThat(resolution.analysisId()).isEqualTo("upgrade-analysis-a");
            assertThat(resolution.executionPlanId()).isEqualTo("upgrade-plan-a");
            session.commit();
        }
    }

    @Test
    @Order(3)
    void sessionTimezonesPreserveUtcWritesAndOneBaselineWindow() throws Exception {
        SqlSessionFactory sessions = sqlSessions(FRESH_DATABASE);

        for (String sessionTimezone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
            try (SqlSession session = sessions.openSession(false)) {
                try (Statement statement = session.getConnection().createStatement()) {
                    statement.execute("SET TIME ZONE '" + sessionTimezone + "'");
                }

                String marker = sessionTimezone.replace('/', '-');
                MonitorAlertMapper monitorAlertMapper = session.getMapper(MonitorAlertMapper.class);
                AnalysisRunMapper analysisRunMapper = session.getMapper(AnalysisRunMapper.class);
                DecisionResultMapper decisionResultMapper = session.getMapper(DecisionResultMapper.class);
                PushRecheckLogMapper recheckLogMapper = session.getMapper(PushRecheckLogMapper.class);
                HotResetEventMapper hotResetEventMapper = session.getMapper(HotResetEventMapper.class);

                writeMonitorAlert(monitorAlertMapper, "timezone-writer-" + marker);
                assertMonitorAlertStoredAtUtc(session.getConnection(), "timezone-writer-" + marker);
                insertBoundaryRows(
                        monitorAlertMapper, analysisRunMapper, recheckLogMapper, hotResetEventMapper, marker);

                DecisionResult decision = offsetDecision(
                        "timezone-decision-" + marker,
                        OffsetDateTime.parse("2026-07-14T20:00:00+08:00"),
                        OffsetDateTime.parse("2026-07-15T08:00:00-04:00"));
                assertThat(decisionResultMapper.insert(decision)).isEqualTo(1);
                DecisionResult storedDecision = decisionResultMapper.selectByDecisionId(decision.getDecisionId());
                assertThat(storedDecision.getValidFrom().toInstant()).isEqualTo(AS_OF_INSTANT);
                assertThat(storedDecision.getExpiresAt().toInstant())
                        .isEqualTo(Instant.parse("2026-07-15T12:00:00Z"));
                assertThat(AS_OF_INSTANT.isBefore(storedDecision.getValidFrom().toInstant())).isFalse();
                assertThat(Instant.parse("2026-07-15T12:00:00Z")
                        .isBefore(storedDecision.getExpiresAt().toInstant())).isFalse();

                assertBoundaryCounts(
                        monitorAlertMapper, analysisRunMapper, recheckLogMapper, hotResetEventMapper);
                RunBaselineVO baseline = runBaseline(
                        monitorAlertMapper, analysisRunMapper, recheckLogMapper, hotResetEventMapper);
                assertThat(baseline.getAlertSummary().getOpenCountWindow()).isEqualTo(4);
                assertThat(baseline.getDataQualitySummary().getAnalysisRunCountWindow()).isEqualTo(3);
                assertThat(baseline.getDataQualitySummary().getLowQualityCountWindow()).isEqualTo(3);
                assertThat(baseline.getRecheckSummary().getStatusCountsWindow().get("REVIEW_WAITING")).isEqualTo(3);
                assertThat(baseline.getHotResetSummary().getEventCountWindow()).isEqualTo(3);
                assertThat(baseline.getHotResetSummary().getTriggerTypeCountsWindow().get("CONTROLLED_TIMEZONE"))
                        .isEqualTo(3);
                session.rollback();
            }
        }
    }

    @Test
    @Order(4)
    void keyPostgreSqlMappersAndExactPlanResolverAreCompatible() throws Exception {
        SqlSessionFactory sessions = sqlSessions(FRESH_DATABASE);
        try (SqlSession session = sessions.openSession(false)) {
            AnalysisRunMapper analysisRunMapper = session.getMapper(AnalysisRunMapper.class);
            DecisionResultMapper decisionResultMapper = session.getMapper(DecisionResultMapper.class);
            ExecutionPlanMapper executionPlanMapper = session.getMapper(ExecutionPlanMapper.class);
            PushSnapshotMapper pushSnapshotMapper = session.getMapper(PushSnapshotMapper.class);
            PushRecheckLogMapper pushRecheckLogMapper = session.getMapper(PushRecheckLogMapper.class);
            MonitorAlertMapper monitorAlertMapper = session.getMapper(MonitorAlertMapper.class);
            HotResetEventMapper hotResetEventMapper = session.getMapper(HotResetEventMapper.class);
            AssetStateMapper assetStateMapper = session.getMapper(AssetStateMapper.class);

            AnalysisRunDO run = analysisRun("mapper-analysis", AS_OF, 85);
            assertThat(analysisRunMapper.insert(run)).isEqualTo(1);

            DecisionResult decision = offsetDecision(
                    "mapper-decision",
                    OffsetDateTime.parse("2026-07-14T12:00:00Z"),
                    OffsetDateTime.parse("2026-07-15T12:00:00Z"));
            decision.setAnalysisId(run.getAnalysisId());
            assertThat(decisionResultMapper.insert(decision)).isEqualTo(1);

            ExecutionPlanDO plan = executionPlan("mapper-plan", run.getAnalysisId());
            assertThat(executionPlanMapper.insert(plan)).isEqualTo(1);

            TmPushSnapshotDO snapshot = new TmPushSnapshotDO();
            snapshot.setAnalysisId(run.getAnalysisId());
            snapshot.setSymbol("BTCUSDT");
            snapshot.setTimeframe("1h");
            snapshot.setPushType("CONTROLLED_EVIDENCE");
            snapshot.setPushStatus("CAPTURED");
            snapshot.setPushCreateTime(AS_OF);
            snapshot.setExpiresAt(AS_OF.plusHours(1));
            snapshot.setTraceId(run.getTraceId());
            snapshot.setCreateTime(AS_OF);
            assertThat(pushSnapshotMapper.insert(snapshot)).isEqualTo(1);
            assertThat(snapshot.getPushId()).isPositive();

            TmPushRecheckLogDO recheck = recheck(snapshot.getPushId(), "REVIEW_WAITING", AS_OF);
            assertThat(pushRecheckLogMapper.insert(recheck)).isEqualTo(1);
            assertThat(pushRecheckLogMapper.selectLatestByPushId(snapshot.getPushId())).isNotNull();

            writeMonitorAlert(monitorAlertMapper, run.getAnalysisId());
            assertThat(monitorAlertMapper.listByAnalysisId(run.getAnalysisId())).hasSize(1);

            HotResetEventDO hotReset = hotReset("mapper-hot-reset", run.getAnalysisId(), AS_OF);
            assertThat(hotResetEventMapper.insert(hotReset)).isEqualTo(1);
            assertThat(hotResetEventMapper.selectByEventKey(hotReset.getEventKey())).isNotNull();

            AssetStateDO assetState = new AssetStateDO();
            assetState.setSymbol("BTCUSDT");
            assetState.setState(AssetStateEnum.OBSERVING);
            assetState.setConfusedScore(0);
            assetState.setConfusedLowStreak(0);
            assetState.setLastUpdateTime(AS_OF);
            assetState.setTraceId(run.getTraceId());
            assertThat(assetStateMapper.mergeUpsertCore(assetState)).isEqualTo(1);
            assertThat(assetStateMapper.selectBySymbol("BTCUSDT").getTraceId()).isEqualTo(run.getTraceId());

            PositionPlanSourceResolver resolver = new PositionPlanSourceResolver(
                    executionPlanMapper, analysisRunMapper);
            PositionPlanSourceResolver.Resolution resolution = resolver.resolveTypedReference(
                    101L,
                    "BTCUSDT",
                    PositionMonitorSourceContract.executionPlanReference(plan.getPlanId()));
            assertThat(resolution.verified()).isTrue();
            assertThat(resolution.executionPlanId()).isEqualTo(plan.getPlanId());
            assertThat(resolution.sourceTraceId()).isEqualTo(run.getTraceId());
            assertThat(decisionResultMapper.findByAnalysisIdAndPlanIdJoined(
                    run.getAnalysisId(), plan.getPlanId())).isNotNull();
            assertThat(pushSnapshotMapper.selectByPushId(snapshot.getPushId())).isNotNull();
            session.rollback();
        }
    }

    private static void recreateDatabase(String database) throws Exception {
        assertThat(List.of(FRESH_DATABASE, UPGRADE_DATABASE)).contains(database);
        try (Connection connection = DriverManager.getConnection(
                BASE_URL + "postgres", target.username(), target.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS \"" + database + "\" WITH (FORCE)");
            statement.execute("CREATE DATABASE \"" + database + "\"");
        }
    }

    private static void dropDatabase(String database) throws Exception {
        assertThat(List.of(FRESH_DATABASE, UPGRADE_DATABASE)).contains(database);
        try (Connection connection = DriverManager.getConnection(
                BASE_URL + "postgres", target.username(), target.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS \"" + database + "\" WITH (FORCE)");
            assertThat(count(connection, "SELECT COUNT(*) FROM pg_database WHERE datname = '" + database + "'"))
                    .isZero();
        }
    }

    private static Flyway flyway(String database, String targetVersion) {
        var configuration = Flyway.configure()
                .dataSource(BASE_URL + database, target.username(), target.password())
                .locations("classpath:db/migration")
                .connectRetries(0);
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        return configuration.load();
    }

    private static void assertValidHistory(Flyway flyway) {
        ValidateResult validation = flyway.validateWithResult();
        assertThat(validation.validationSuccessful).isTrue();
        assertThat(validation.invalidMigrations).isEmpty();
        assertThat(validation.validateCount).isEqualTo(7);
    }

    private static Connection connect(String database) throws Exception {
        return DriverManager.getConnection(BASE_URL + database, target.username(), target.password());
    }

    private static void assertHistoryRows(Connection connection) throws Exception {
        List<String> versions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT version FROM flyway_schema_history
                WHERE version IS NOT NULL AND success = TRUE
                ORDER BY installed_rank
                """); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                versions.add(rows.getString(1));
            }
        }
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7");
        assertThat(count(connection,
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE")).isZero();
    }

    private static void assertV7Types(Connection connection) throws Exception {
        for (String column : List.of("valid_from", "expires_at")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT data_type FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'tm_decision_result'
                      AND column_name = ?
                    """)) {
                statement.setString(1, column);
                try (ResultSet rows = statement.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("timestamp with time zone");
                }
            }
        }
    }

    private static void assertExecutionPlanSafetyDefaults(Connection connection) throws Exception {
        for (String column : List.of(
                "manual_review_required",
                "not_trade_instruction",
                "not_executable",
                "not_auto_trading",
                "not_order_execution",
                "not_user_position_creation")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT is_nullable, column_default
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'tm_execution_plan'
                      AND column_name = ?
                    """)) {
                statement.setString(1, column);
                try (ResultSet rows = statement.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("is_nullable")).isEqualTo("NO");
                    assertThat(rows.getString("column_default")).contains("true");
                }
            }
        }
    }

    private static int count(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            assertThat(rows.next()).isTrue();
            return rows.getInt(1);
        }
    }

    private static void insertHistoricalV6Rows() throws Exception {
        try (Connection connection = connect(UPGRADE_DATABASE)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tm_analysis_run(
                      analysis_id, symbol, timeframe, analysis_time, rule_version,
                      data_quality_score, trace_id, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, "upgrade-analysis-a");
                statement.setString(2, "BTCUSDT");
                statement.setString(3, "1h");
                statement.setObject(4, AS_OF);
                statement.setString(5, "v1");
                statement.setInt(6, 85);
                statement.setString(7, "upgrade-trace-a");
                statement.setString(8, "SUCCESS");
                statement.setObject(9, AS_OF);
                statement.setObject(10, AS_OF);
                assertThat(statement.executeUpdate()).isEqualTo(1);
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tm_decision_result(
                      decision_id, analysis_id, symbol, market_bias_hierarchy,
                      valid_period, create_time)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, "upgrade-decision-a");
                statement.setString(2, "upgrade-analysis-a");
                statement.setString(3, "BTCUSDT");
                statement.setString(4, "NEUTRAL");
                statement.setString(5, "historical-v6-period");
                statement.setObject(6, AS_OF);
                assertThat(statement.executeUpdate()).isEqualTo(1);
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tm_execution_plan(
                      plan_id, analysis_id, recommended_action, entry_zone,
                      stop_loss, take_profit_rules, create_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, "upgrade-plan-a");
                statement.setString(2, "upgrade-analysis-a");
                statement.setString(3, "MANUAL_REVIEW_ONLY");
                statement.setString(4, "100-101");
                statement.setString(5, "99");
                statement.setString(6, "102/103");
                statement.setObject(7, AS_OF);
                assertThat(statement.executeUpdate()).isEqualTo(1);
            }
            connection.commit();
        }
    }

    private static SqlSessionFactory sqlSessions(String database) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(BASE_URL + database);
        dataSource.setUser(target.username());
        dataSource.setPassword(target.password());
        Environment environment = new Environment(
                "controlled-postgresql-v7", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setDatabaseId("postgresql");
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(AnalysisRunMapper.class);
        configuration.addMapper(DecisionResultMapper.class);
        configuration.addMapper(ExecutionPlanMapper.class);
        configuration.addMapper(PushSnapshotMapper.class);
        configuration.addMapper(PushRecheckLogMapper.class);
        configuration.addMapper(MonitorAlertMapper.class);
        configuration.addMapper(HotResetEventMapper.class);
        configuration.addMapper(AssetStateMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private static DecisionResult offsetDecision(
            String decisionId,
            OffsetDateTime validFrom,
            OffsetDateTime expiresAt) {
        DecisionResult row = new DecisionResult();
        row.setDecisionId(decisionId);
        row.setAnalysisId("upgrade-analysis-a");
        row.setSymbol("BTCUSDT");
        row.setMarketBiasHierarchy("NEUTRAL");
        row.setTradeType("WAIT");
        row.setIsWorthOpening(false);
        row.setValidPeriod(validFrom + " ~ " + expiresAt);
        row.setValidFrom(validFrom);
        row.setExpiresAt(expiresAt);
        row.setCreateTime(AS_OF);
        return row;
    }

    private static AnalysisRunDO analysisRun(String analysisId, LocalDateTime time, int dataQualityScore) {
        AnalysisRunDO row = new AnalysisRunDO();
        row.setAnalysisId(analysisId);
        row.setSymbol("BTCUSDT");
        row.setTimeframe("1h");
        row.setAnalysisTime(time);
        row.setRuleVersion("v1");
        row.setDataQualityScore(dataQualityScore);
        row.setTraceId("trace-" + analysisId);
        row.setStatus("SUCCESS");
        row.setAttemptCount(1);
        row.setCreatedAt(time);
        row.setUpdatedAt(time);
        row.setVersionNo(1);
        return row;
    }

    private static ExecutionPlanDO executionPlan(String planId, String analysisId) {
        ExecutionPlanDO row = new ExecutionPlanDO();
        row.setPlanId(planId);
        row.setAnalysisId(analysisId);
        row.setPlanMode("ADVISORY");
        row.setExecutionPlanStatus("INCOMPLETE");
        row.setSourceGateStatus("INCOMPLETE");
        row.setSourceGateComplete(false);
        row.setRecommendedAction("MANUAL_REVIEW_ONLY");
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotExecutable(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotUserPositionCreation(true);
        row.setNeedsRevalidation(false);
        row.setCreateTime(AS_OF);
        return row;
    }

    private static void writeMonitorAlert(MonitorAlertMapper mapper, String analysisId) {
        MonitorAlertWriteServiceImpl writer = new MonitorAlertWriteServiceImpl(mapper);
        writer.setClock(Clock.fixed(AS_OF_INSTANT, ZoneOffset.UTC));
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

    private static void assertMonitorAlertStoredAtUtc(Connection connection, String analysisId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT created_at, updated_at, cooldown_until
                FROM tm_monitor_alert WHERE analysis_id = ?
                """)) {
            statement.setString(1, analysisId);
            try (ResultSet rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getObject("created_at", LocalDateTime.class)).isEqualTo(AS_OF);
                assertThat(rows.getObject("updated_at", LocalDateTime.class)).isEqualTo(AS_OF);
                assertThat(rows.getObject("cooldown_until", LocalDateTime.class))
                        .isEqualTo(AS_OF.plusMinutes(15));
            }
        }
    }

    private static void insertBoundaryRows(
            MonitorAlertMapper monitorAlertMapper,
            AnalysisRunMapper analysisRunMapper,
            PushRecheckLogMapper recheckLogMapper,
            HotResetEventMapper hotResetEventMapper,
            String marker) {
        List<LocalDateTime> times = List.of(
                LocalDateTime.parse("2026-07-14T11:29:59"),
                WINDOW_START,
                LocalDateTime.parse("2026-07-14T11:59:59"),
                AS_OF,
                LocalDateTime.parse("2026-07-14T12:00:01"));
        for (int index = 0; index < times.size(); index++) {
            LocalDateTime time = times.get(index);
            String id = marker + "-" + index;

            MonitorAlertDO alert = new MonitorAlertDO();
            alert.setId("boundary-alert-" + id);
            alert.setAnalysisId("boundary-analysis-" + id);
            alert.setAssetSymbol("BTCUSDT");
            alert.setAlertType("CONTROLLED_TIMEZONE");
            alert.setAlertLevel("INFO");
            alert.setAlertMessage("controlled evidence");
            alert.setStatus("OPEN");
            alert.setCreatedAtUtc(time);
            alert.setUpdatedAtUtc(time);
            alert.setIsDeleted(0);
            alert.setVersionNo(1);
            assertThat(monitorAlertMapper.insert(alert)).isEqualTo(1);

            AnalysisRunDO run = analysisRun("boundary-run-" + id, time, 50);
            assertThat(analysisRunMapper.insert(run)).isEqualTo(1);

            TmPushRecheckLogDO recheck = recheck(9000L + index, "REVIEW_WAITING", time);
            assertThat(recheckLogMapper.insert(recheck)).isEqualTo(1);

            HotResetEventDO hotReset = hotReset("boundary-reset-" + id, run.getAnalysisId(), time);
            assertThat(hotResetEventMapper.insert(hotReset)).isEqualTo(1);
        }
    }

    private static TmPushRecheckLogDO recheck(Long pushId, String status, LocalDateTime time) {
        TmPushRecheckLogDO row = new TmPushRecheckLogDO();
        row.setPushId(pushId);
        row.setRecheckTime(time);
        row.setRecheckStatus(status);
        row.setCurrentPrice(new BigDecimal("100"));
        row.setCurrentDataQualityScore(85);
        row.setCurrentConfusedScore(0);
        row.setCurrentAccountRiskAllowed(false);
        row.setFailReasonJson("{\"code\":\"CONTROLLED_EVIDENCE\"}");
        row.setTraceId("trace-recheck-" + pushId + "-" + time);
        row.setCreateTime(time);
        return row;
    }

    private static HotResetEventDO hotReset(String eventId, String analysisId, LocalDateTime time) {
        HotResetEventDO row = new HotResetEventDO();
        row.setEventId(eventId);
        row.setEventKey("key-" + eventId);
        row.setAnalysisId(analysisId);
        row.setTraceId("trace-" + analysisId);
        row.setSymbol("BTCUSDT");
        row.setTimeframe("1h");
        row.setTriggerType("CONTROLLED_TIMEZONE");
        row.setDecisionInvalidatedCount(0);
        row.setPlanRevalidationCount(0);
        row.setPushInvalidatedCount(0);
        row.setRebuildTriggered(false);
        row.setEventTime(time);
        row.setCreateTime(time);
        return row;
    }

    private static void assertBoundaryCounts(
            MonitorAlertMapper monitorAlertMapper,
            AnalysisRunMapper analysisRunMapper,
            PushRecheckLogMapper recheckLogMapper,
            HotResetEventMapper hotResetEventMapper) {
        assertThat(monitorAlertMapper.countByStatusInWindow("OPEN", WINDOW_START, AS_OF)).isEqualTo(4);
        assertThat(monitorAlertMapper.countByStatusAndTypeInWindow(
                "OPEN", "CONTROLLED_TIMEZONE", WINDOW_START, AS_OF)).isEqualTo(3);
        assertThat(analysisRunMapper.countInWindow(WINDOW_START, AS_OF)).isEqualTo(3);
        assertThat(analysisRunMapper.countLowQualityInWindow(WINDOW_START, AS_OF, 60)).isEqualTo(3);
        assertThat(recheckLogMapper.countByStatusInWindow("REVIEW_WAITING", WINDOW_START, AS_OF)).isEqualTo(3);
        assertThat(hotResetEventMapper.countInWindow(WINDOW_START, AS_OF)).isEqualTo(3);
        assertThat(hotResetEventMapper.selectTriggerTypeCountsInWindow(WINDOW_START, AS_OF))
                .filteredOn(row -> "CONTROLLED_TIMEZONE".equals(row.getKey()))
                .extracting(row -> row.getCount())
                .containsExactly(3);
    }

    private static RunBaselineVO runBaseline(
            MonitorAlertMapper monitorAlertMapper,
            AnalysisRunMapper analysisRunMapper,
            PushRecheckLogMapper recheckLogMapper,
            HotResetEventMapper hotResetEventMapper) {
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
                monitorAlertMapper,
                analysisRunMapper,
                recheckLogMapper,
                hotResetEventMapper);
        baseline.setClock(Clock.fixed(AS_OF_INSTANT, ZoneOffset.UTC));
        return baseline.getRunBaseline(30);
    }

    private static String env(String name) {
        return System.getenv(name);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ControlledTarget(String username, String password) {
    }
}
