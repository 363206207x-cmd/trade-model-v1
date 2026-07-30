package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.controller.DashboardHomeController;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.readiness.ProviderReadinessService;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.ProviderReadinessVO;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("controlled-postgresql")
class ControlledPostgreSqlDashboardPlanValidityEvidenceTest {

    private static final String JDBC_URL =
            "jdbc:postgresql://127.0.0.1:55432/trade_model_v1_test";
    private static final String USERNAME = "trade_model_test";
    private static final String CONFIRM_VALUE = "I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL";
    private static final String RUN_VALUE = "I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB";
    private static final Instant AS_OF = Instant.parse("2026-07-14T12:00:00Z");
    private static final LocalDateTime AS_OF_UTC = LocalDateTime.ofInstant(AS_OF, ZoneOffset.UTC);
    private static final List<String> SESSION_TIMEZONES =
            List.of("UTC", "Asia/Shanghai", "America/New_York");

    private static ControlledTarget target;
    private static SqlSessionFactory sessions;

    @BeforeAll
    static void prepareControlledPostgreSql() {
        String jdbcUrl = env("CONTROLLED_POSTGRESQL_JDBC_URL");
        String username = env("CONTROLLED_POSTGRESQL_USERNAME");
        String password = env("CONTROLLED_POSTGRESQL_PASSWORD");

        assumeTrue(hasText(jdbcUrl) && hasText(username) && hasText(password),
                "Controlled PostgreSQL env is missing; evidence test is environment-gated");
        assertThat(jdbcUrl).isEqualTo(JDBC_URL);
        assertThat(username).isEqualTo(USERNAME);
        assertThat(env("CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM")).isEqualTo(CONFIRM_VALUE);
        assertThat(env("CONTROLLED_POSTGRESQL_FLYWAY_RUN")).isEqualTo(RUN_VALUE);
        assertThat(jdbcUrl.toLowerCase(Locale.ROOT))
                .doesNotContain("prod", "production", "live", "primary", "main");

        target = new ControlledTarget(username, password);
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .connectRetries(0)
                .load();
        assertThat(flyway.migrate().success).isTrue();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        sessions = sqlSessions();
    }

    @Test
    void postgresqlPlanNotActiveIsFailClosedAcrossSessionTimezones() throws Exception {
        List<DashboardOutcome> outcomes = new ArrayList<>();
        for (String timezone : SESSION_TIMEZONES) {
            DashboardEvidence evidence = newOpportunityEvidence(
                    timezone,
                    "not-active",
                    AS_OF,
                    OffsetDateTime.parse("2026-07-14T13:00:00Z"),
                    OffsetDateTime.parse("2026-07-15T13:00:00Z"));

            DashboardHomeVO.ExecutionSuggestionVO suggestion = evidence.home().getExecutionSuggestion();
            assertThat(suggestion.getStatus()).isEqualTo("PLAN_NOT_ACTIVE");
            assertThat(suggestion.getStatusLabel()).isEqualTo("当前暂无完整执行计划");
            assertThat(suggestion.getBlockedReason()).isEqualTo("计划尚未进入有效期，等待重新分析");
            assertBlockedPlanFieldsAreEmpty(suggestion);
            assertThat(evidence.serialized()).doesNotContain("USABLE_REVIEW_PLAN");
            assertDashboardSafety(evidence.home());
            outcomes.add(evidence.outcome());
        }
        assertEquivalentAcrossSessionTimezones(outcomes);
    }

    @Test
    void postgresqlExactExpiryBoundaryIsExpiredAcrossSessionTimezones() throws Exception {
        List<DashboardOutcome> outcomes = new ArrayList<>();
        for (String timezone : SESSION_TIMEZONES) {
            DashboardEvidence evidence = newOpportunityEvidence(
                    timezone,
                    "exact-expiry",
                    AS_OF,
                    OffsetDateTime.parse("2026-07-13T12:00:00Z"),
                    OffsetDateTime.parse("2026-07-14T12:00:00Z"));

            DashboardHomeVO.ExecutionSuggestionVO suggestion = evidence.home().getExecutionSuggestion();
            assertThat(suggestion.getStatus()).isEqualTo("PLAN_EXPIRED");
            assertThat(suggestion.getBlockedReason()).isEqualTo("计划已失效，等待重新分析");
            assertBlockedPlanFieldsAreEmpty(suggestion);
            assertThat(evidence.serialized()).doesNotContain("USABLE_REVIEW_PLAN");
            assertDashboardSafety(evidence.home());
            outcomes.add(evidence.outcome());
        }
        assertEquivalentAcrossSessionTimezones(outcomes);
    }

    @Test
    void postgresqlOneSecondBeforeExpiryRemainsManualReviewOnlyAcrossSessionTimezones() throws Exception {
        List<DashboardOutcome> outcomes = new ArrayList<>();
        Instant oneSecondBeforeExpiry = Instant.parse("2026-07-14T11:59:59Z");
        for (String timezone : SESSION_TIMEZONES) {
            DashboardEvidence evidence = newOpportunityEvidence(
                    timezone,
                    "before-expiry",
                    oneSecondBeforeExpiry,
                    OffsetDateTime.parse("2026-07-13T12:00:00Z"),
                    OffsetDateTime.parse("2026-07-14T12:00:00Z"));

            DashboardHomeVO.ExecutionSuggestionVO suggestion = evidence.home().getExecutionSuggestion();
            assertThat(suggestion.getStatus()).isEqualTo("USABLE_REVIEW_PLAN");
            assertThat(suggestion.getStatusLabel()).isEqualTo("完整执行计划，仅供人工复核");
            assertThat(suggestion.getEntryZone()).isEqualTo("100-101");
            assertThat(suggestion.getStopLoss()).isEqualTo("99");
            assertThat(suggestion.getTakeProfitRules()).isEqualTo("102/103");
            assertThat(suggestion.getValidFrom().toInstant())
                    .isEqualTo(Instant.parse("2026-07-13T12:00:00Z"));
            assertThat(suggestion.getExpiresAt().toInstant())
                    .isEqualTo(Instant.parse("2026-07-14T12:00:00Z"));
            assertDashboardSafety(evidence.home());
            outcomes.add(evidence.outcome());
        }
        assertEquivalentAcrossSessionTimezones(outcomes);
    }

    @Test
    void postgresqlVerifiedPositionPlanExpiresAsHistoricalReviewOnlyAcrossSessionTimezones() throws Exception {
        List<DashboardOutcome> outcomes = new ArrayList<>();
        for (String timezone : SESSION_TIMEZONES) {
            DashboardEvidence evidence = expiredPositionEvidence(timezone);
            DashboardHomeVO.ExecutionSuggestionVO suggestion = evidence.home().getExecutionSuggestion();

            assertThat(suggestion.getStatus()).isEqualTo("POSITION_MONITORING");
            assertThat(suggestion.getPositionMode()).isTrue();
            assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("VERIFIED");
            assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo("EXPIRED");
            assertThat(suggestion.getOriginalPlanLabel()).isEqualTo("原计划已失效，仅用于历史复核");
            assertThat(suggestion.getEntryZone()).isEqualTo("PLAN-A-entry");
            assertThat(suggestion.getStopLoss()).isEqualTo("PLAN-A-stop");
            assertThat(suggestion.getTakeProfitRules()).isEqualTo("PLAN-A-tp");
            assertThat(suggestion.getSourceAnalysisId()).contains("position-a");
            assertThat(suggestion.getSourceExecutionPlanId()).contains("position-a");
            assertThat(evidence.serialized())
                    .doesNotContain("PLAN-B-entry", "PLAN-B-stop", "PLAN-B-tp", "USABLE_REVIEW_PLAN");
            assertDashboardSafety(evidence.home());
            outcomes.add(evidence.outcome());
        }
        assertEquivalentAcrossSessionTimezones(outcomes);
    }

    @Test
    void postgresqlEquivalentOffsetsProduceOneDashboardValidityResultAcrossSessionTimezones() throws Exception {
        List<OffsetDateTime> validFromValues = List.of(
                OffsetDateTime.parse("2026-07-14T12:00:00Z"),
                OffsetDateTime.parse("2026-07-14T20:00:00+08:00"),
                OffsetDateTime.parse("2026-07-14T08:00:00-04:00"));
        List<OffsetDateTime> expiresAtValues = List.of(
                OffsetDateTime.parse("2026-07-15T12:00:00Z"),
                OffsetDateTime.parse("2026-07-15T20:00:00+08:00"),
                OffsetDateTime.parse("2026-07-15T08:00:00-04:00"));

        List<DashboardOutcome> outcomes = new ArrayList<>();
        for (String timezone : SESSION_TIMEZONES) {
            for (int index = 0; index < validFromValues.size(); index++) {
                DashboardEvidence evidence = newOpportunityEvidence(
                        timezone,
                        "equivalent-offset-" + index,
                        AS_OF,
                        validFromValues.get(index),
                        expiresAtValues.get(index));

                DashboardHomeVO.ExecutionSuggestionVO suggestion = evidence.home().getExecutionSuggestion();
                assertThat(suggestion.getStatus()).isEqualTo("USABLE_REVIEW_PLAN");
                assertThat(suggestion.getValidFrom().toInstant()).isEqualTo(AS_OF);
                assertThat(suggestion.getExpiresAt().toInstant())
                        .isEqualTo(Instant.parse("2026-07-15T12:00:00Z"));
                assertDashboardSafety(evidence.home());
                outcomes.add(evidence.outcome());
            }
        }
        assertThat(outcomes).hasSize(9);
        assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome).isEqualTo(outcomes.get(0)));
    }

    private DashboardEvidence newOpportunityEvidence(
            String sessionTimezone,
            String scenario,
            Instant now,
            OffsetDateTime validFrom,
            OffsetDateTime expiresAt) throws Exception {
        try (SqlSession session = sessions.openSession(false)) {
            try {
                setSessionTimezone(session, sessionTimezone);
                isolateNewOpportunityPositionState(session);
                String marker = marker(scenario, sessionTimezone);
                AnalysisRunMapper runMapper = session.getMapper(AnalysisRunMapper.class);
                DecisionResultMapper decisionMapper = session.getMapper(DecisionResultMapper.class);
                ExecutionPlanMapper planMapper = session.getMapper(ExecutionPlanMapper.class);
                AssetStateMapper stateMapper = session.getMapper(AssetStateMapper.class);

                String analysisId = "dashboard-analysis-" + marker;
                String traceId = "dashboard-trace-" + marker;
                insertPlanGraph(runMapper, decisionMapper, planMapper, stateMapper,
                        analysisId, "dashboard-decision-" + marker, "dashboard-plan-" + marker,
                        traceId, validFrom, expiresAt, "100-101", "99", "102/103", AS_OF_UTC);

                DashboardHomeController controller = controller(session, now);
                ApiResponse<DashboardHomeVO> response = controller.home("BTCUSDT", 6, null);
                DashboardHomeVO home = response.getData();
                String serialized = objectMapper().writeValueAsString(response);
                DecisionResult stored = decisionMapper.selectByDecisionId("dashboard-decision-" + marker);
                return evidence(home, serialized, stored);
            } finally {
                session.rollback();
            }
        }
    }

    private DashboardEvidence expiredPositionEvidence(String sessionTimezone) throws Exception {
        try (SqlSession session = sessions.openSession(false)) {
            try {
                setSessionTimezone(session, sessionTimezone);
                String marker = marker("position", sessionTimezone);
                AnalysisRunMapper runMapper = session.getMapper(AnalysisRunMapper.class);
                DecisionResultMapper decisionMapper = session.getMapper(DecisionResultMapper.class);
                ExecutionPlanMapper planMapper = session.getMapper(ExecutionPlanMapper.class);
                AssetStateMapper stateMapper = session.getMapper(AssetStateMapper.class);
                UserPositionMapper positionMapper = session.getMapper(UserPositionMapper.class);
                PositionMonitorLogMapper monitorLogMapper = session.getMapper(PositionMonitorLogMapper.class);

                String analysisA = "dashboard-analysis-position-a-" + marker;
                String decisionA = "dashboard-decision-position-a-" + marker;
                String planA = "dashboard-plan-position-a-" + marker;
                String traceA = "dashboard-trace-position-a-" + marker;
                insertPlanGraph(runMapper, decisionMapper, planMapper, stateMapper,
                        analysisA, decisionA, planA, traceA,
                        OffsetDateTime.parse("2026-07-13T12:00:00Z"),
                        OffsetDateTime.parse("2026-07-14T12:00:00Z"),
                        "PLAN-A-entry", "PLAN-A-stop", "PLAN-A-tp", AS_OF_UTC);

                String analysisB = "dashboard-analysis-position-b-" + marker;
                insertPlanGraph(runMapper, decisionMapper, planMapper, null,
                        analysisB, "dashboard-decision-position-b-" + marker,
                        "dashboard-plan-position-b-" + marker,
                        "dashboard-trace-position-b-" + marker,
                        OffsetDateTime.parse("2026-07-14T11:00:00Z"),
                        OffsetDateTime.parse("2026-07-15T12:00:00Z"),
                        "PLAN-B-entry", "PLAN-B-stop", "PLAN-B-tp", AS_OF_UTC.plusMinutes(1));

                UserPositionDO position = userPosition(planA);
                assertThat(positionMapper.insert(position)).isEqualTo(1);
                assertThat(position.getId()).isPositive();

                PositionMonitorLogDO monitor = monitorLog(position.getId(), analysisA, planA, traceA);
                assertThat(monitorLogMapper.insert(monitor)).isEqualTo(1);

                DashboardHomeController controller = controller(session, AS_OF);
                ApiResponse<DashboardHomeVO> response = controller.home("BTCUSDT", 6, position.getId());
                DashboardHomeVO home = response.getData();
                String serialized = objectMapper().writeValueAsString(response);
                DecisionResult stored = decisionMapper.selectByDecisionId(decisionA);
                return evidence(home, serialized, stored);
            } finally {
                session.rollback();
            }
        }
    }

    private static void insertPlanGraph(
            AnalysisRunMapper runMapper,
            DecisionResultMapper decisionMapper,
            ExecutionPlanMapper planMapper,
            AssetStateMapper stateMapper,
            String analysisId,
            String decisionId,
            String planId,
            String traceId,
            OffsetDateTime validFrom,
            OffsetDateTime expiresAt,
            String entryZone,
            String stopLoss,
            String takeProfitRules,
            LocalDateTime createTime) {
        assertThat(runMapper.insert(analysisRun(analysisId, traceId, createTime))).isEqualTo(1);
        assertThat(decisionMapper.insert(decision(
                decisionId, analysisId, validFrom, expiresAt, createTime))).isEqualTo(1);
        assertThat(planMapper.insert(executionPlan(
                planId, analysisId, entryZone, stopLoss, takeProfitRules, createTime))).isEqualTo(1);
        if (stateMapper != null) {
            assertThat(stateMapper.mergeUpsertCore(assetState(traceId, createTime))).isEqualTo(1);
        }
    }

    private DashboardHomeController controller(SqlSession session, Instant now) {
        DecisionResultMapper decisionMapper = session.getMapper(DecisionResultMapper.class);
        AnalysisRunMapper runMapper = session.getMapper(AnalysisRunMapper.class);
        ExecutionPlanMapper planMapper = session.getMapper(ExecutionPlanMapper.class);
        AssetStateMapper stateMapper = session.getMapper(AssetStateMapper.class);
        UserPositionMapper positionMapper = session.getMapper(UserPositionMapper.class);
        PositionMonitorLogMapper monitorLogMapper = session.getMapper(PositionMonitorLogMapper.class);

        MonitorService monitorService = mock(MonitorService.class);
        when(monitorService.getRecentAlerts(anyInt())).thenReturn(List.of());
        PositionSyncService positionSyncService = mock(PositionSyncService.class);
        ProviderReadinessService readinessService = mock(ProviderReadinessService.class);
        when(readinessService.getReadiness()).thenReturn(new ProviderReadinessVO());

        DashboardHomeServiceImpl service = new DashboardHomeServiceImpl(
                new MapperDecisionService(decisionMapper),
                monitorService,
                new UserPositionServiceImpl(positionMapper),
                new PositionMonitorLogServiceImpl(monitorLogMapper, positionMapper),
                positionSyncService,
                mock(OpportunityLogService.class),
                mock(ExternalContextEvidenceBuilder.class),
                readinessService,
                objectMapper());
        service.setOriginalPlanSources(decisionMapper, planMapper, runMapper);
        service.setAssetStateMapper(stateMapper);
        service.setPlanValidityClock(Clock.fixed(now, ZoneOffset.UTC));
        AuthenticatedUserIdResolver resolver = mock(AuthenticatedUserIdResolver.class);
        when(resolver.requireCurrentUserId()).thenReturn(17L);
        return new DashboardHomeController(service, resolver);
    }

    private static AnalysisRunDO analysisRun(String analysisId, String traceId, LocalDateTime time) {
        AnalysisRunDO row = new AnalysisRunDO();
        row.setAnalysisId(analysisId);
        row.setSymbol("BTCUSDT");
        row.setTimeframe("4h");
        row.setAnalysisTime(time);
        row.setRuleVersion("v1");
        row.setDataQualityScore(85);
        row.setTraceId(traceId);
        row.setStatus("SUCCESS");
        row.setAttemptCount(1);
        row.setCreatedAt(time);
        row.setUpdatedAt(time);
        row.setVersionNo(1);
        return row;
    }

    private static DecisionResult decision(
            String decisionId,
            String analysisId,
            OffsetDateTime validFrom,
            OffsetDateTime expiresAt,
            LocalDateTime createTime) {
        DecisionResult row = new DecisionResult();
        row.setDecisionId(decisionId);
        row.setAnalysisId(analysisId);
        row.setSymbol("BTCUSDT");
        row.setMarketBiasHierarchy("BULLISH");
        row.setTradeType("MANUAL_REVIEW");
        row.setConfidenceLevel("HIGH");
        row.setRiskLevel("LOW");
        row.setActionPriority("NORMAL");
        row.setConclusionSummary("controlled PostgreSQL dashboard validity evidence");
        row.setIsWorthOpening(true);
        row.setIsAdopted(false);
        row.setValidPeriod(validFrom + " ~ " + expiresAt);
        row.setValidFrom(validFrom);
        row.setExpiresAt(expiresAt);
        row.setInvalidCondition("manual invalidation only");
        row.setConfusedScore(0);
        row.setAssetStateSnapshot("CANDIDATE");
        row.setCreateTime(createTime);
        return row;
    }

    private static ExecutionPlanDO executionPlan(
            String planId,
            String analysisId,
            String entryZone,
            String stopLoss,
            String takeProfitRules,
            LocalDateTime createTime) {
        ExecutionPlanDO row = new ExecutionPlanDO();
        row.setPlanId(planId);
        row.setAnalysisId(analysisId);
        row.setPlanMode("ADVISORY");
        row.setExecutionPlanStatus("VALID");
        row.setSourceGateStatus("VALID");
        row.setSourceGateComplete(true);
        row.setRecommendedAction("MANUAL_REVIEW_ONLY");
        row.setEntryZone(entryZone);
        row.setStopLoss(stopLoss);
        row.setTakeProfitRules(takeProfitRules);
        row.setLeverageSuggestion("low_leverage");
        row.setPositionSuggestion("small manual position");
        row.setInvalidCondition("manual invalidation only");
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotExecutable(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotUserPositionCreation(true);
        row.setNeedsRevalidation(false);
        row.setCreateTime(createTime);
        return row;
    }

    private static AssetStateDO assetState(String traceId, LocalDateTime time) {
        AssetStateDO row = new AssetStateDO();
        row.setSymbol("BTCUSDT");
        row.setState(AssetStateEnum.CANDIDATE);
        row.setConfusedScore(0);
        row.setConfusedLowStreak(0);
        row.setLastUpdateTime(time);
        row.setTraceId(traceId);
        return row;
    }

    private static UserPositionDO userPosition(String planId) {
        UserPositionDO row = new UserPositionDO();
        row.setAssetSymbol("BTCUSDT");
        row.setSide("LONG");
        row.setStatus("OPEN");
        row.setEntryPrice(new BigDecimal("100"));
        row.setQuantity(new BigDecimal("0.1"));
        row.setLeverage(BigDecimal.ONE);
        row.setStopLoss(new BigDecimal("99"));
        row.setTakeProfit(new BigDecimal("103"));
        row.setOpenedAt(AS_OF_UTC.minusHours(2));
        row.setSourceType("MANUAL");
        row.setSourceRefId(PositionMonitorSourceContract.executionPlanReference(planId));
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
        row.setCreatedAt(AS_OF_UTC.minusHours(2));
        row.setUpdatedAt(AS_OF_UTC);
        return row;
    }

    private static PositionMonitorLogDO monitorLog(
            Long positionId,
            String analysisId,
            String planId,
            String traceId) {
        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setPositionId(positionId);
        row.setAnalysisId(analysisId);
        row.setExecutionPlanId(planId);
        row.setCurrentPrice(new BigDecimal("100"));
        row.setLogicStatus("LOGIC_VALID");
        row.setRiskLevel("LOW");
        row.setSuggestedAction("HOLD");
        row.setReason("controlled historical review only");
        row.setTraceId(traceId);
        row.setCreatedAt(AS_OF_UTC);
        return row;
    }

    private static DashboardEvidence evidence(
            DashboardHomeVO home,
            String serialized,
            DecisionResult stored) {
        DashboardHomeVO.ExecutionSuggestionVO suggestion = home.getExecutionSuggestion();
        return new DashboardEvidence(home, serialized, new DashboardOutcome(
                suggestion.getStatus(),
                suggestion.getBlockedReason(),
                suggestion.getOriginalPlanCurrentValidity(),
                stored.getValidFrom().toInstant(),
                stored.getExpiresAt().toInstant()));
    }

    private static void assertBlockedPlanFieldsAreEmpty(DashboardHomeVO.ExecutionSuggestionVO suggestion) {
        assertThat(suggestion.getEntryZone()).isNull();
        assertThat(suggestion.getStopLoss()).isNull();
        assertThat(suggestion.getTakeProfitRules()).isNull();
        assertThat(suggestion.getLeverageSuggestion()).isNull();
        assertThat(suggestion.getPositionSuggestion()).isNull();
    }

    private static void assertDashboardSafety(DashboardHomeVO home) {
        assertThat(home.getSafety().getReviewOnly()).isTrue();
        assertThat(home.getSafety().getManualReviewOnly()).isTrue();
        assertThat(home.getSafety().getNotTradeInstruction()).isTrue();
        assertThat(home.getSafety().getNotExecutable()).isTrue();
        assertThat(home.getSafety().getNotAutoTrading()).isTrue();
        assertThat(home.getSafety().getNotOrderExecution()).isTrue();
        assertThat(home.getSafety().getNotUserPositionCreation()).isTrue();
        assertThat(home.getSafety().getNotUserPositionMutation()).isTrue();
    }

    private static void assertEquivalentAcrossSessionTimezones(List<DashboardOutcome> outcomes) {
        assertThat(outcomes).hasSize(SESSION_TIMEZONES.size());
        assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome).isEqualTo(outcomes.get(0)));
    }

    private static void setSessionTimezone(SqlSession session, String timezone) throws Exception {
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("SET TIME ZONE '" + timezone + "'");
        }
    }

    private static void isolateNewOpportunityPositionState(SqlSession session) throws Exception {
        try (Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate("DELETE FROM tm_position_monitor_log");
            statement.executeUpdate("DELETE FROM tm_user_position");
        }
        assertThat(session.getMapper(UserPositionMapper.class).listClaimedOpenForSystemMonitoring()).isEmpty();
    }

    private static SqlSessionFactory sqlSessions() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(JDBC_URL);
        dataSource.setUser(target.username());
        dataSource.setPassword(target.password());
        Environment environment = new Environment(
                "controlled-postgresql-dashboard-validity", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setDatabaseId("postgresql");
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(AnalysisRunMapper.class);
        configuration.addMapper(DecisionResultMapper.class);
        configuration.addMapper(ExecutionPlanMapper.class);
        configuration.addMapper(AssetStateMapper.class);
        configuration.addMapper(UserPositionMapper.class);
        configuration.addMapper(PositionMonitorLogMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private static String marker(String scenario, String timezone) {
        return scenario + "-" + timezone.toLowerCase(Locale.ROOT).replace('/', '-');
    }

    private static String env(String name) {
        return System.getenv(name);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class MapperDecisionService implements DecisionService {
        private final DecisionResultMapper mapper;

        private MapperDecisionService(DecisionResultMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public LightSystemStatusVO getLightSystemStatus() {
            return new LightSystemStatusVO();
        }

        @Override
        public List<DecisionResultVO> getLatestDecisionResults(int limit) {
            return mapper.findLatestDecisionResultsJoined(limit);
        }

        @Override
        public List<DecisionResultVO> getLatestDecisionResultsForUser(Long userId, int limit) {
            return getLatestDecisionResults(limit);
        }

        @Override
        public DecisionResultVO getLatestDecisionResultBySymbol(String symbol) {
            return mapper.findLatestDecisionResultBySymbolJoined(symbol.trim().toUpperCase(Locale.ROOT));
        }

        @Override
        public DecisionResultVO getLatestDecisionResultBySymbolForUser(Long userId, String symbol) {
            return getLatestDecisionResultBySymbol(symbol);
        }

        @Override
        public int countOpenPositions() {
            return 0;
        }

        @Override
        public int countOpenPositionsForUser(Long userId) {
            return 0;
        }
    }

    private record ControlledTarget(String username, String password) {
    }

    private record DashboardOutcome(
            String status,
            String blockedReason,
            String originalPlanCurrentValidity,
            Instant validFrom,
            Instant expiresAt) {
    }

    private record DashboardEvidence(
            DashboardHomeVO home,
            String serialized,
            DashboardOutcome outcome) {
    }
}
