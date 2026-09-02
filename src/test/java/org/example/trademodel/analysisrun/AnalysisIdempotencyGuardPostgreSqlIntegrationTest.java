package org.example.trademodel.analysisrun;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@Tag("core-regression")
class AnalysisIdempotencyGuardPostgreSqlIntegrationTest {
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    private static AnalysisRunMapper mapper;
    private static AnalysisIdempotencyGuard guard;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void initializePostgreSql() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Environment environment = new Environment(
                "analysis-idempotency-postgresql",
                new SpringManagedTransactionFactory(),
                dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setDatabaseId("postgresql");
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(AnalysisRunMapper.class);
        SqlSessionFactory sessions = new SqlSessionFactoryBuilder().build(configuration);
        mapper = new SqlSessionTemplate(sessions).getMapper(AnalysisRunMapper.class);

        AnalysisRunProperties properties = new AnalysisRunProperties();
        properties.getIdempotency().setMaxRecoveryAttempts(3);
        guard = new AnalysisIdempotencyGuardImpl(
                mapper, properties, new DataSourceTransactionManager(dataSource), Clock.systemUTC());
        jdbc = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void setUp() {
        clean();
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void tenSequentialRetriesReturnOneCanonicalAnalysisRun() {
        List<AnalysisIdempotencyClaim> claims = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            claims.add(guard.claim(request("pg-sequential", index)));
        }

        assertCanonicalClaims(claims, 10);
        assertThat(rowCount("pg-sequential")).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0} concurrent PostgreSQL retries return one canonical run")
    @ValueSource(ints = {2, 10, 50})
    void concurrentRetriesAreAtomicAndDoNotAbortTransactions(int workers) throws Exception {
        String key = "pg-concurrent-" + workers;

        List<AnalysisIdempotencyClaim> claims = concurrentClaims(workers, key);

        assertCanonicalClaims(claims, workers);
        assertThat(rowCount(key)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
    }

    @Test
    void sameKeyWithDifferentNormalizedPayloadFailsClosed() {
        guard.claim(request("pg-payload", 1, "CONFUSED"));

        assertThatThrownBy(() -> guard.claim(request("pg-payload", 2, "NORMAL")))
                .isInstanceOfSatisfying(AnalysisRunInputException.class,
                        error -> assertThat(error.getReasonCode())
                                .isEqualTo("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH"));
        assertThat(rowCount("pg-payload")).isEqualTo(1);
    }

    @Test
    void differentKeysCreateDifferentRowsAndPostConflictTransactionsStayHealthy() {
        AnalysisIdempotencyClaim first = guard.claim(request("pg-health-a", 1));
        AnalysisIdempotencyClaim duplicate = guard.claim(request("pg-health-a", 2));
        AnalysisIdempotencyClaim second = guard.claim(request("pg-health-b", 3));

        assertThat(duplicate.getRun().getAnalysisId()).isEqualTo(first.getRun().getAnalysisId());
        assertThat(second.getRun().getAnalysisId()).isNotEqualTo(first.getRun().getAnalysisId());
        assertThat(mapper.selectById(second.getRun().getAnalysisId())).isNotNull();
        assertThat(rowCount("pg-health-a")).isEqualTo(1);
        assertThat(rowCount("pg-health-b")).isEqualTo(1);
    }

    private static List<AnalysisIdempotencyClaim> concurrentClaims(int workers, String key) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(workers);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            List<Future<AnalysisIdempotencyClaim>> futures = new ArrayList<>();
            for (int index = 0; index < workers; index++) {
                int requestIndex = index;
                futures.add(pool.submit(() -> {
                    barrier.await(20, TimeUnit.SECONDS);
                    return guard.claim(request(key, requestIndex));
                }));
            }
            List<AnalysisIdempotencyClaim> claims = new ArrayList<>();
            for (Future<AnalysisIdempotencyClaim> future : futures) {
                claims.add(future.get(45, TimeUnit.SECONDS));
            }
            return claims;
        } finally {
            pool.shutdownNow();
        }
    }

    private static AnalysisRunClaimRequest request(String key, int index) {
        return request(key, index, "CONFUSED");
    }

    private static AnalysisRunClaimRequest request(String key, int index, String inputMode) {
        String analysisId = "ana-" + key + "-" + index;
        String requestId = "req-" + key + "-" + index;
        String traceId = "trace-" + key + "-" + index;
        String snapshot = "{\"symbol\":\"BNBUSDT\",\"inputMode\":\"" + inputMode
                + "\",\"canonicalAnalysisTimeBucket\":\"2026-09-02T01:00\",\"requestId\":\""
                + requestId + "\",\"traceId\":\"" + traceId + "\"}";
        return new AnalysisRunClaimRequest(
                analysisId,
                traceId,
                requestId,
                key,
                "BNBUSDT",
                "5m",
                LocalDateTime.of(2026, 9, 2, 1, 4, 59),
                "rules-postgresql-idempotency",
                AnalysisRunTriggerType.MANUAL_API,
                requestId,
                null,
                null,
                snapshot,
                "hash-" + analysisId,
                "lease-" + key + "-" + index,
                LocalDateTime.now(Clock.systemUTC()).plusMinutes(5),
                "USER",
                1L,
                6L,
                false);
    }

    private static void assertCanonicalClaims(List<AnalysisIdempotencyClaim> claims, int expectedCount) {
        assertThat(claims).hasSize(expectedCount);
        assertThat(claims.stream().filter(claim ->
                claim.getStatus() == AnalysisIdempotencyClaimStatus.CLAIMED_NEW)).hasSize(1);
        assertThat(claims).extracting(claim -> claim.getRun().getAnalysisId())
                .containsOnly(claims.get(0).getRun().getAnalysisId());
    }

    private static int rowCount(String key) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tm_analysis_run WHERE idempotency_key = ?", Integer.class, key);
        return count != null ? count : 0;
    }

    private static void clean() {
        jdbc.update("DELETE FROM tm_analysis_run WHERE analysis_id LIKE 'ana-pg-%' OR idempotency_key LIKE 'pg-%'");
    }
}
