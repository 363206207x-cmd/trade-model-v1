package org.example.trademodel.telegram;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.testsupport.FrozenFinalExecutionPlanTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real mapper SQL, existing H2 test schema, no application context or network client.
 * Each test owns a random in-memory database; all fixture writes roll back and the
 * last connection closes even on failure, destroying that database.
 */
@Tag("core-regression")
class TelegramDeliveryOrphanMapperIntegrationTest {
    private Connection databaseLifetime;
    private SqlSession session;
    private MessageMapper messages;
    private UserPositionMapper positions;
    private PositionMonitorLogMapper logs;
    private long user;
    private long otherUser;
    private LocalDateTime now;

    @BeforeEach
    void proveIsolationBeforeCreatingAnyFixture() throws Exception {
        String databaseName = "telegram_route_test_" + UUID.randomUUID().toString().replace("-", "");
        String expectedUrl = "jdbc:h2:mem:" + databaseName;
        // Constructed locally, never resolved from environment/application/Staging configuration.
        assertThat(expectedUrl).matches("jdbc:h2:mem:telegram_route_test_[0-9a-f]{32}");
        DriverManagerDataSource datasource = new DriverManagerDataSource(
                expectedUrl + ";DB_CLOSE_DELAY=0", "sa", "");
        databaseLifetime = datasource.getConnection();
        assertThat(databaseLifetime.getMetaData().getURL()).isEqualTo(expectedUrl);
        assertThat(databaseLifetime.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        ScriptUtils.executeSqlScript(databaseLifetime, new ClassPathResource("schema.sql"));

        Configuration configuration = new Configuration(
                new Environment("isolated-telegram-routing-test", new JdbcTransactionFactory(), datasource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(PersonalUserMapper.class);
        configuration.addMapper(UserPositionMapper.class);
        configuration.addMapper(PositionMonitorLogMapper.class);
        configuration.addMapper(MessageMapper.class);
        configuration.addMapper(ExecutionPlanMapper.class);
        session = new SqlSessionFactoryBuilder().build(configuration).openSession(false);
        assertThat(session.getConnection().getAutoCommit()).isFalse();
        assertThat(session.getConnection().getMetaData().getURL()).isEqualTo(expectedUrl);
        assertThat(countRows("tm_user")).isZero();
        assertThat(countRows("tm_user_position")).isZero();
        messages = session.getMapper(MessageMapper.class);
        positions = session.getMapper(UserPositionMapper.class);
        logs = session.getMapper(PositionMonitorLogMapper.class);
        now = LocalDateTime.now(Clock.systemUTC()).withNano(0);
        user = createRandomTestUser();
        otherUser = createRandomTestUser();
        assertThat(otherUser).isNotEqualTo(user);
    }

    @AfterEach
    void rollBackAllFixturesAndDestroyDatabaseEvenAfterFailure() throws Exception {
        try {
            if (session != null) {
                session.rollback(true);
                assertThat(countRows("tm_user")).isZero();
                assertThat(countRows("tm_user_position")).isZero();
                assertThat(countRows("tm_position_monitor_log")).isZero();
                assertThat(countRows("tm_message")).isZero();
            }
        } finally {
            try {
                if (session != null) session.close();
            } finally {
                if (databaseLifetime != null) databaseLifetime.close();
            }
        }
    }

    @Test
    void sameUserOpenManualPositionWithLatestVerifiedFreshLogMatches() {
        UserPositionDO position = position(user, "OPEN", "MANUAL_INDEPENDENT");
        PositionMonitorLogDO log = log(position, "VERIFIED", now.minusSeconds(5), now.plusMinutes(1));
        MessageDO message = insert(positionMessage(user, position, log));
        assertThat(sourceCount(message, user)).isOne();
    }

    @Test
    void differentMessageOwnerAndDifferentRequestingUserCannotMatch() {
        UserPositionDO foreign = position(otherUser, "OPEN", "MANUAL_INDEPENDENT");
        PositionMonitorLogDO log = log(foreign, "VERIFIED", now.minusSeconds(5), now.plusMinutes(1));
        MessageDO wrongOwner = insert(positionMessage(user, foreign, log));
        MessageDO correctOwner = insert(positionMessage(otherUser, foreign, log));
        assertThat(sourceCount(wrongOwner, user)).isZero();
        assertThat(sourceCount(correctOwner, user)).isZero();
        assertThat(sourceCount(correctOwner, otherUser)).isOne();
    }

    @Test
    void separatelyCreatedClosedPositionCannotMatch() {
        UserPositionDO closed = position(user, "CLOSED", "MANUAL_INDEPENDENT");
        PositionMonitorLogDO log = log(closed, "VERIFIED", now.minusSeconds(5), now.plusMinutes(1));
        assertThat(sourceCount(insert(positionMessage(user, closed, log)), user)).isZero();
        assertThat(positions.selectByIdAndUserId(closed.getId(), user).getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void nonManualSourceCannotMatchDespiteVerifiedFreshLog() {
        UserPositionDO nonManual = position(user, "OPEN", "SYSTEM_PLAN_POSITION");
        PositionMonitorLogDO log = log(nonManual, "VERIFIED", now.minusSeconds(5), now.plusMinutes(1));
        assertThat(sourceCount(insert(positionMessage(user, nonManual, log)), user)).isZero();
    }

    @Test
    void staleUnverifiedAndMissingLogsUseIndependentPositionsAndDoNotMatch() {
        UserPositionDO stalePosition = position(user, "OPEN", "MANUAL_INDEPENDENT");
        PositionMonitorLogDO stale = log(stalePosition, "VERIFIED", now.minusMinutes(5), now.minusSeconds(1));
        assertThat(sourceCount(insert(positionMessage(user, stalePosition, stale)), user)).isZero();

        UserPositionDO unverifiedPosition = position(user, "OPEN", "MANUAL_INDEPENDENT");
        PositionMonitorLogDO unverified = log(unverifiedPosition, "PENDING_VERIFICATION",
                now.minusSeconds(5), now.plusMinutes(1));
        assertThat(sourceCount(insert(positionMessage(user, unverifiedPosition, unverified)), user)).isZero();

        UserPositionDO missingPosition = position(user, "OPEN", "MANUAL_INDEPENDENT");
        assertThat(sourceCount(insert(positionMessage(user, missingPosition, null)), user)).isZero();
    }

    @Test
    void onlyNewestLogDeterminesQualificationIncludingEqualTimestampTieBreak() {
        UserPositionDO pendingLatest = position(user, "OPEN", "MANUAL_INDEPENDENT");
        PositionMonitorLogDO oldVerified = log(pendingLatest, "VERIFIED", now.minusSeconds(5), now.plusMinutes(1));
        PositionMonitorLogDO newPending = log(pendingLatest, "PENDING_VERIFICATION",
                now.minusSeconds(5), now.plusMinutes(1));
        assertThat(newPending.getLogId()).isGreaterThan(oldVerified.getLogId());
        assertThat(sourceCount(insert(positionMessage(user, pendingLatest, oldVerified)), user)).isZero();

        UserPositionDO verifiedLatest = position(user, "OPEN", "MANUAL_INDEPENDENT");
        PositionMonitorLogDO oldPending = log(verifiedLatest, "PENDING_VERIFICATION",
                now.minusSeconds(5), now.plusMinutes(1));
        PositionMonitorLogDO newVerified = log(verifiedLatest, "VERIFIED",
                now.minusSeconds(5), now.plusMinutes(1));
        assertThat(newVerified.getLogId()).isGreaterThan(oldPending.getLogId());
        assertThat(sourceCount(insert(positionMessage(user, verifiedLatest, newVerified)), user)).isOne();

        UserPositionDO staleLatest = position(user, "OPEN", "MANUAL_INDEPENDENT");
        PositionMonitorLogDO earlier = log(staleLatest, "VERIFIED", now.minusMinutes(5), now.plusMinutes(1));
        log(staleLatest, "VERIFIED", now.minusMinutes(2), now.minusSeconds(1));
        assertThat(sourceCount(insert(positionMessage(user, staleLatest, earlier)), user)).isZero();
    }

    @Test
    void orphanQueryKeepsOnlyTwoCanonicalCategoriesWithoutDeletingSafetyFacts() {
        MessageDO opportunity = insert(opportunityMessage());
        UserPositionDO position = position(user, "OPEN", "MANUAL_INDEPENDENT");
        PositionMonitorLogDO log = log(position, "VERIFIED", now.minusSeconds(5), now.plusMinutes(1));
        MessageDO risk = insert(positionMessage(user, position, log));

        MessageDO safety = safetyMessage();
        insert(safety);
        MessageDO expiredSafety = safetyMessage();
        expiredSafety.setExpiresAt(now.minusSeconds(1));
        insert(expiredSafety);
        MessageDO malformedSafety = safetyMessage();
        malformedSafety.setBody("资产：BTCUSDT\n变化：Hot Reset");
        insert(malformedSafety);

        MessageDO malformedPlan = opportunityMessage();
        malformedPlan.setBody("BTCUSDT  ·  强偏多  ·  确认型\n\n字段不完整");
        insert(malformedPlan);
        MessageDO legacyPlan = opportunityMessage();
        legacyPlan.setTitle("【机会达到人工复核条件】");
        insert(legacyPlan);

        UserPositionDO malformedPosition = position(user, "OPEN", "MANUAL_INDEPENDENT");
        MessageDO malformedRisk = positionMessage(user, malformedPosition,
                log(malformedPosition, "VERIFIED", now.minusSeconds(5), now.plusMinutes(1)));
        malformedRisk.setBody("BTCUSDT  ·  做多\n\n变化：风险高");
        insert(malformedRisk);
        UserPositionDO legacyPosition = position(user, "OPEN", "MANUAL_INDEPENDENT");
        MessageDO legacyRisk = positionMessage(user, legacyPosition,
                log(legacyPosition, "VERIFIED", now.minusSeconds(5), now.plusMinutes(1)));
        legacyRisk.setTitle("【持仓逻辑发生重要变化】");
        insert(legacyRisk);

        assertThat(messages.listTelegramDeliveryOrphans(now, 20)).extracting(MessageDO::getMessageId)
                .containsExactlyInAnyOrder(opportunity.getMessageId(), risk.getMessageId());
        assertThat(messages.selectByIdForUser(safety.getMessageId(), user)).isNotNull();
    }

    private long createRandomTestUser() {
        PersonalUserDO row = new PersonalUserDO();
        row.setUsername(id("telegram-test-user"));
        row.setPasswordHash("TEST_ONLY_NOT_A_LOGIN_CREDENTIAL");
        row.setCreatedAt(now);
        assertThat(session.getMapper(PersonalUserMapper.class).insert(row)).isOne();
        assertThat(row.getId()).isNotNull().isPositive();
        return row.getId();
    }

    private UserPositionDO position(long owner, String status, String source) {
        UserPositionDO row = new UserPositionDO();
        row.setUserId(owner);
        row.setSubmissionId(id("test-submission"));
        row.setAssetSymbol("BTCUSDT");
        row.setSide("LONG");
        row.setStatus(status);
        row.setSourceType(source);
        row.setEntryPrice(new BigDecimal("100"));
        row.setQuantity(new BigDecimal("0.25"));
        row.setLeverage(BigDecimal.ONE);
        row.setOpenedAt(now.minusHours(1));
        if ("CLOSED".equals(status)) {
            row.setClosedAt(now.minusMinutes(1));
            row.setClosePrice(new BigDecimal("101"));
            row.setCloseReason("ISOLATED_TEST_INITIAL_CLOSED_FIXTURE");
        }
        if ("SYSTEM_PLAN_POSITION".equals(source)) {
            // The existing schema requires an actual plan row for this non-MANUAL source.
            var plan = FrozenFinalExecutionPlanTestFixture.complete(id("test-plan"), id("test-analysis"), now);
            plan.setFinalPlan(false);
            plan.setChainStatus("RULE_VALIDATION_BLOCKED");
            plan.setRuleValidationStatus("BLOCKED");
            plan.setFinalPlanMode("BLOCKED");
            plan.setCandidateId(null);
            plan.setOpportunityId(null);
            plan.setResolverResultId(null);
            plan.setAssetId(null);
            plan.setAccountRiskSnapshotId(null);
            session.getMapper(ExecutionPlanMapper.class).insert(plan);
            row.setFinalPlanId(plan.getPlanId());
        }
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        assertThat(positions.insert(row)).isOne();
        assertThat(row.getId()).isNotNull().isPositive();
        return row;
    }

    private PositionMonitorLogDO log(UserPositionDO position, String status,
                                    LocalDateTime observed, LocalDateTime freshUntil) {
        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setPositionId(position.getId());
        row.setAnalysisId(id("test-analysis"));
        row.setTraceId(id("test-trace"));
        row.setCurrentPrice(new BigDecimal("99"));
        row.setMonitorSourceStatus(status);
        row.setObservedAt(observed);
        row.setFreshUntil(freshUntil);
        row.setCreatedAt(observed);
        if ("VERIFIED".equals(status)) {
            row.setMarkPriceSource("ISOLATED_TEST_QUOTE");
            row.setEntryLogicStatus("WEAKENED");
            row.setMonitorConclusion("HIGH_RISK_OBSERVATION");
            row.setReversalStatus("STRONG_REVERSAL");
            row.setRiskChangeReason("OPPOSING_EVIDENCE_INCREASED");
            row.setRiskLevel("HIGH");
            row.setRiskTrend("INCREASED");
            row.setSuggestedAction("WAIT_CONFIRMATION");
        }
        assertThat(logs.insert(row)).isOne();
        assertThat(row.getLogId()).isNotNull().isPositive();
        return row;
    }

    private MessageDO positionMessage(long owner, UserPositionDO position, PositionMonitorLogDO log) {
        MessageDO row = baseMessage(owner, "POSITION_LOGIC_RISK_CHANGE", HighValueAlertPolicy.POSITION_SHORT_TITLE);
        row.setSourceType("POSITION_MONITOR");
        row.setPositionId(position.getId());
        row.setSourceId(log == null ? id("missing-test-log") : log.getLogId().toString());
        row.setAnalysisId(log == null ? id("missing-test-analysis") : log.getAnalysisId());
        row.setTraceId(log == null ? id("missing-test-trace") : log.getTraceId());
        row.setBody("BTCUSDT  ·  做多\n\n变化：风险升至高风险\n\n入场：100\n现价：99\n"
                + "止损：98  目标：105\n\n操作：打开持仓详情");
        row.setDedupeKey(TelegramDedupeKey.createPlanLifetime("POSITION_RISK_CHANGE", "RISK_HIGH", 3,
                owner, "USER_POSITION", position.getId().toString()));
        return row;
    }

    private MessageDO opportunityMessage() {
        MessageDO row = baseMessage(user, "HIGH_PERMISSION_OPPORTUNITY", HighValueAlertPolicy.OPPORTUNITY_SHORT_TITLE);
        row.setSourceType("FINAL_PLAN");
        row.setSourceId(id("test-plan"));
        row.setPlanId(row.getSourceId());
        row.setAnalysisId(id("test-analysis"));
        row.setBody("BTCUSDT  ·  强偏多  ·  确认型\n\n"
                + "入场：100 - 101\n触发：收盘确认\n止损：98\n目标：105\n有效至：" + now.plusHours(1)
                + "\n\n操作：打开系统重新校验");
        row.setDedupeKey(TelegramDedupeKey.create("OPPORTUNITY_READY", "CONFIRMATION", 3, 15,
                user, "FINAL_PLAN", row.getPlanId(), now));
        return row;
    }

    private MessageDO safetyMessage() {
        MessageDO row = baseMessage(user, "OPPORTUNITY_PLAN_SAFETY_CHANGE", HighValueAlertPolicy.SAFETY_SHORT_TITLE);
        row.setSourceType("FINAL_PLAN");
        row.setSourceId(id("test-safety-plan"));
        row.setPlanId(row.getSourceId());
        row.setAnalysisId(id("test-analysis"));
        row.setBody("资产：BTCUSDT\n变化：Hot Reset\n原因：来源门禁失效\n"
                + "当前状态：暂不视为有效机会\n恢复条件：重新分析并通过规则校验");
        row.setDedupeKey(TelegramDedupeKey.create("PLAN_SAFETY_CHANGE", "HOT_RESET", 4, 15,
                user, "FINAL_PLAN", row.getPlanId(), now));
        return row;
    }

    private MessageDO baseMessage(long owner, String category, String title) {
        MessageDO row = new MessageDO();
        row.setMessageId(id("test-message"));
        row.setUserId(owner);
        row.setCategory(category);
        row.setTitle(title);
        row.setSymbol("BTCUSDT");
        row.setTraceId(id("test-trace"));
        row.setBusinessState("ACTIVE");
        row.setReadState("UNREAD");
        row.setExpiresAt(now.plusHours(1));
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }

    private MessageDO insert(MessageDO message) {
        assertThat(messages.insert(message)).isOne();
        return message;
    }

    private int sourceCount(MessageDO message, long requestingUser) {
        return messages.countCurrentTelegramSource(message.getMessageId(), requestingUser, now);
    }

    private int countRows(String table) throws Exception {
        // Only hard-coded local test table names are passed by setup/teardown.
        try (var statement = session.getConnection().createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static String id(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
