package org.example.trademodel.controller;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.service.PlanRevalidationService;
import org.example.trademodel.v41.DashboardLiveEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real MVC/SQL projection with isolated fixtures. JDBC login-session renewal is not a business mutation. */
@SpringBootTest(properties = {"trade-model.auth.enabled=true", "trade-model.schedulers.enabled=false",
        "trade-model.home-live.enabled=false", "spring.flyway.enabled=false"})
@AutoConfigureMockMvc
@Transactional
@Tag("core-regression")
@Import(DashboardHomeReadOnlyIntegrationTest.SqlEvidenceConfiguration.class)
class DashboardHomeReadOnlyIntegrationTest {
    private static final String DATABASE = "jdbc:h2:mem:home_read_only_" + UUID.randomUUID().toString().replace("-", "")
            + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final List<Write> WRITES = new CopyOnWriteArrayList<>();
    private static final String SESSION_RENEWAL_SQL = "UPDATE SPRING_SESSION SET SESSION_ID = ?, "
            + "LAST_ACCESS_TIME = ?, MAX_INACTIVE_INTERVAL = ?, EXPIRY_TIME = ?, PRINCIPAL_NAME = ? "
            + "WHERE PRIMARY_ID = ?";
    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE);
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PersonalUserMapper users;
    @Autowired UserPositionMapper positions;
    @Autowired DashboardLiveEventService live;
    @MockBean PositionMonitorService monitor;
    @MockBean PlanRevalidationService revalidation;
    private PersonalUserDO owner;

    @BeforeEach
    void proveIsolationThenCreateRandomUser() throws Exception {
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:home_read_only_");
        }
        owner = new PersonalUserDO();
        owner.setUsername("home-read-" + UUID.randomUUID());
        owner.setPasswordHash("ISOLATED_TEST_NOT_A_LOGIN_CREDENTIAL");
        owner.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        assertThat(users.insert(owner)).isEqualTo(1);
        assertThat(owner.getId()).isPositive();
        clearInvocations(monitor, revalidation);
    }

    @Test
    void repeatedHomeReadsAndSixSelectionsDoNotWriteOrCreateMonitorContexts() throws Exception {
        UserPositionDO position = new UserPositionDO();
        position.setUserId(owner.getId());
        position.setSubmissionId(UUID.randomUUID().toString());
        position.setAssetSymbol("ETHUSDT");
        position.setStatus("OPEN");
        position.setSide("LONG");
        position.setSourceType("MANUAL_INDEPENDENT");
        position.setEntryPrice(new BigDecimal("2000"));
        position.setQuantity(BigDecimal.ONE);
        position.setLeverage(BigDecimal.ONE);
        position.setOpenedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(1));
        position.setCreatedAt(position.getOpenedAt());
        position.setUpdatedAt(position.getOpenedAt());
        position.setManualReviewRequired(true);
        position.setNotTradeInstruction(true);
        position.setNotAutoTrading(true);
        position.setNotOrderExecution(true);
        position.setNotPositionSync(true);
        positions.insert(position); // generated ID; this fixture and user are rolled back
        assertThat(position.getId()).isPositive();
        var before = jdbc.queryForMap("SELECT * FROM tm_user_position WHERE id = ?", position.getId());
        assertReadsHaveNoCausalWrites();
        assertThat(jdbc.queryForMap("SELECT * FROM tm_user_position WHERE id = ?", position.getId())).isEqualTo(before);
    }

    @Test
    void noActivePositionsMeansHomeCannotCreateAnyMonitoringLog() throws Exception {
        assertThat(positions.listOpenByUserId(owner.getId())).isEmpty();
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM tm_position_monitor_log", Long.class);
        assertReadsHaveNoCausalWrites();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tm_position_monitor_log", Long.class)).isEqualTo(before);
    }

    @Test
    void sqlProbeDetectsActualJdbcWritesUnderACorrelationId() {
        String correlation = "probe-" + UUID.randomUUID();
        org.slf4j.MDC.put("requestId", correlation);
        try {
            jdbc.update("UPDATE tm_user SET last_login_at = ? WHERE id = ?", owner.getCreatedAt(), owner.getId());
        } finally {
            org.slf4j.MDC.remove("requestId");
        }
        assertThat(WRITES.stream().filter(write -> write.correlation().equals(correlation)).toList())
                .singleElement().satisfies(write -> assertThat(write.sql()).startsWith("UPDATE tm_user"));
    }

    private void assertReadsHaveNoCausalWrites() throws Exception {
        // Establish once, then reuse the same authenticated JDBC session, as the real browser does.
        var established = mvc.perform(get("/api/dashboard/home").param("limit", "6")
                        .with(user(owner.getUsername()).roles("OPERATOR")))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("JSESSIONID");
        assertThat(established).isNotNull();
        var originalSessions = sessionRows();
        var ownedSessions = originalSessions.values().stream()
                .filter(row -> owner.getUsername().equals(row.get("PRINCIPAL_NAME"))).toList();
        assertThat(ownedSessions).hasSize(1);
        String primaryId = (String) ownedSessions.get(0).get("PRIMARY_ID");
        var originalAttributes = canonicalRows("SPRING_SESSION_ATTRIBUTES");
        var originalBusiness = businessRows();
        var originalContexts = contextSnapshot();
        int renewals = 0;
        for (String symbol : List.of("BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "DOGEUSDT", "UNIUSDT")) {
            for (int repeat = 0; repeat < 2; repeat++) {
                String correlation = "home-read-" + UUID.randomUUID();
                var before = sessionRows();
                long requestStarted = System.currentTimeMillis();
                var response = mvc.perform(get("/api/dashboard/home").param("selectedSymbol", symbol).param("limit", "6")
                                .header("X-Request-Id", correlation).cookie(established))
                        .andExpect(status().isOk()).andReturn().getResponse();
                long requestFinished = System.currentTimeMillis();
                var writes = WRITES.stream().filter(write -> write.correlation().equals(correlation)).toList();
                // Every causal write must be the framework's exact renewal, with unchanged identity/TTL/principal.
                // Any business DML, session INSERT/DELETE, attribute write or other session UPDATE fails here.
                assertThat(writes).as("GET writes: only one existing-session renewal is permitted").hasSizeLessThanOrEqualTo(1);
                for (Write write : writes) {
                    assertAllowedRenewal(write, before.get(primaryId), requestStarted, requestFinished);
                    renewals++;
                }
                var after = sessionRows();
                assertThat(after.keySet()).as("no session insertion, deletion or PRIMARY_ID change")
                        .isEqualTo(originalSessions.keySet());
                for (String id : after.keySet()) {
                    var expected = new LinkedHashMap<>(before.get(id));
                    if (primaryId.equals(id)) {
                        expected.put("LAST_ACCESS_TIME", after.get(id).get("LAST_ACCESS_TIME"));
                        expected.put("EXPIRY_TIME", after.get(id).get("EXPIRY_TIME"));
                        long lastAccess = ((Number) after.get(id).get("LAST_ACCESS_TIME")).longValue();
                        assertThat(lastAccess).isBetween(requestStarted, requestFinished);
                        assertThat(lastAccess).isGreaterThanOrEqualTo(((Number) before.get(id).get("LAST_ACCESS_TIME")).longValue());
                        assertThat(((Number) after.get(id).get("EXPIRY_TIME")).longValue())
                                .isEqualTo(lastAccess + ((Number) before.get(id).get("MAX_INACTIVE_INTERVAL")).longValue() * 1000L);
                    }
                    assertThat(after.get(id)).as("all non-renewal session fields stay unchanged").isEqualTo(expected);
                }
                var returnedCookie = response.getCookie("JSESSIONID");
                if (returnedCookie != null) assertThat(returnedCookie.getValue()).isEqualTo(established.getValue());
                assertThat(canonicalRows("SPRING_SESSION_ATTRIBUTES")).isEqualTo(originalAttributes);
                assertThat(businessRows()).as("all business-table rows, including logs/tasks/deliveries/trades").isEqualTo(originalBusiness);
                assertThat(contextSnapshot()).isEqualTo(originalContexts);
            }
        }
        assertThat(renewals).as("normal framework renewal was exercised, not disabled").isPositive();
        verifyNoInteractions(monitor, revalidation);
        System.out.printf("HOME_READ_ONLY: GETS=12 BUSINESS_TABLE_WRITES=0 SPRING_SESSION_RENEWALS=%d "
                + "SESSION_IDENTITIES_UNCHANGED=true SESSION_ATTRIBUTES_UNCHANGED=true MONITOR_CONTEXTS_UNCHANGED=true%n", renewals);
    }

    private Map<?, ?> contextSnapshot() {
        return new LinkedHashMap<>((Map<?, ?>) ReflectionTestUtils.getField(live, "structuralContexts"));
    }

    private Map<String, Map<String, Object>> sessionRows() throws Exception {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (var row : canonicalRows("SPRING_SESSION")) result.put((String) row.get("PRIMARY_ID"), row);
        return result;
    }

    private Map<String, List<Map<String, Object>>> businessRows() throws Exception {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : jdbc.queryForList("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME", String.class)) {
            if (!table.equals("SPRING_SESSION") && !table.equals("SPRING_SESSION_ATTRIBUTES")) {
                result.put(table, canonicalRows(table));
            }
        }
        assertThat(result).containsKeys("TM_USER_POSITION", "TM_POSITION_MONITOR_LOG", "TM_ANALYSIS_RUN",
                "TM_ASYNC_TASK", "TM_AI_CALL_LOG", "TM_CHANNEL_DELIVERY");
        return result;
    }

    private List<Map<String, Object>> canonicalRows(String table) throws Exception {
        assertThat(table).matches("[A-Z0-9_]+"); // names come only from this private H2 database's metadata
        var rows = jdbc.queryForList("SELECT * FROM \"" + table + "\"");
        for (var row : rows) {
            for (var entry : row.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof java.sql.Blob blob) value = blob.getBytes(1, Math.toIntExact(blob.length()));
                if (value instanceof byte[] bytes) value = Base64.getEncoder().encodeToString(bytes);
                if (value instanceof java.sql.Clob clob) value = clob.getSubString(1, Math.toIntExact(clob.length()));
                entry.setValue(value);
            }
        }
        return rows;
    }

    private static void assertAllowedRenewal(Write write, Map<String, Object> before, long started, long finished) {
        assertThat(write.sql().trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT)).isEqualTo(SESSION_RENEWAL_SQL);
        assertThat(write.bindings()).containsOnlyKeys(1, 2, 3, 4, 5, 6)
                .containsEntry(1, before.get("SESSION_ID"))
                .containsEntry(3, before.get("MAX_INACTIVE_INTERVAL"))
                .containsEntry(5, before.get("PRINCIPAL_NAME"))
                .containsEntry(6, before.get("PRIMARY_ID"));
        long lastAccess = ((Number) write.bindings().get(2)).longValue();
        assertThat(lastAccess).isBetween(started, finished)
                .isGreaterThanOrEqualTo(((Number) before.get("LAST_ACCESS_TIME")).longValue());
        assertThat(((Number) write.bindings().get(4)).longValue())
                .isEqualTo(lastAccess + ((Number) before.get("MAX_INACTIVE_INTERVAL")).longValue() * 1000L);
    }

    @Test
    void renewalExceptionRejectsBusinessWritesSessionCreationAttributesAndIdentityChanges() {
        var before = Map.<String, Object>of("PRIMARY_ID", UUID.randomUUID().toString(),
                "SESSION_ID", UUID.randomUUID().toString(), "PRINCIPAL_NAME", "test-" + UUID.randomUUID(),
                "LAST_ACCESS_TIME", 1000L, "MAX_INACTIVE_INTERVAL", 1800);
        var bindings = Map.<Integer, Object>of(1, before.get("SESSION_ID"), 2, 1001L, 3, 1800,
                4, 1801001L, 5, before.get("PRINCIPAL_NAME"), 6, before.get("PRIMARY_ID"));
        assertAllowedRenewal(new Write("test", SESSION_RENEWAL_SQL, bindings), before, 1001, 1002);
        for (String sql : List.of("INSERT INTO SPRING_SESSION VALUES (?)", "DELETE FROM SPRING_SESSION WHERE PRIMARY_ID=?",
                "UPDATE SPRING_SESSION_ATTRIBUTES SET ATTRIBUTE_BYTES=?", "UPDATE tm_user SET last_login_at=?")) {
            assertThatThrownBy(() -> assertAllowedRenewal(new Write("test", sql, bindings), before, 1001, 1002))
                    .isInstanceOf(AssertionError.class);
        }
        for (int key : List.of(1, 3, 5, 6)) {
            var changed = new LinkedHashMap<>(bindings);
            changed.put(key, key == 3 ? 3600 : UUID.randomUUID().toString());
            assertThatThrownBy(() -> assertAllowedRenewal(new Write("test", SESSION_RENEWAL_SQL, changed), before, 1001, 1002))
                    .isInstanceOf(AssertionError.class);
        }
    }

    record Write(String correlation, String sql, Map<Integer, Object> bindings) {}

    @TestConfiguration
    static class SqlEvidenceConfiguration {
        @Bean static BeanPostProcessor captureActualSqlExecution() {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean, String name) {
                    if (!(bean instanceof DataSource source)) return bean;
                    return new DelegatingDataSource(source) {
                        @Override public Connection getConnection() throws java.sql.SQLException {
                            return observedConnection(super.getConnection());
                        }
                        @Override public Connection getConnection(String username, String password) throws java.sql.SQLException {
                            return observedConnection(super.getConnection(username, password));
                        }
                    };
                }
            };
        }
        static Connection observedConnection(Connection connection) {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        try {
                            Object result = method.invoke(connection, args);
                            if (!(result instanceof Statement statement)) return result;
                            String preparedSql = args != null && args.length > 0 && args[0] instanceof String sql ? sql : null;
                            Class<?> type = result instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
                            Map<Integer, Object> bindings = new LinkedHashMap<>();
                            return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, operation, values) -> {
                                if (operation.getName().startsWith("set") && values != null && values.length >= 2
                                        && values[0] instanceof Integer index) {
                                    bindings.put(index, operation.getName().equals("setNull") ? null : values[1]);
                                } else if (operation.getName().equals("clearParameters")) bindings.clear();
                                String sql = preparedSql != null ? preparedSql : values != null && values.length > 0
                                        && values[0] instanceof String query ? query : "";
                                String correlation = org.slf4j.MDC.get("requestId");
                                if (correlation != null && operation.getName().startsWith("execute")
                                        && sql.stripLeading().matches("(?is)^(INSERT|UPDATE|DELETE|MERGE|CREATE|ALTER|DROP|TRUNCATE|CALL)\\b.*")) {
                                    WRITES.add(new Write(correlation, sql, new LinkedHashMap<>(bindings)));
                                }
                                try { return operation.invoke(statement, values); }
                                catch (InvocationTargetException failure) { throw failure.getCause(); }
                            });
                        } catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
        }
    }
}
