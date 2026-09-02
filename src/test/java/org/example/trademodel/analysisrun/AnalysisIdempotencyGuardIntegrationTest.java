package org.example.trademodel.analysisrun;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TradeModelApplication.class)
@Tag("core-regression")
class AnalysisIdempotencyGuardIntegrationTest {
    @Autowired
    private AnalysisIdempotencyGuard guard;
    @Autowired
    private AnalysisRunMapper mapper;
    @Autowired
    private AnalysisRunProperties properties;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        properties.getIdempotency().setMaxRecoveryAttempts(3);
        clean();
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void uniqueIndexBlocksDuplicateRowsForSameCanonicalKey() {
        mapper.insertStarted(row("ana-it-unique-1", "trace-it-unique-1", "req-it-unique-1", "it-unique", "STARTED", 1, 1, future(), "lease-1"));

        assertThatThrownBy(() -> mapper.insertStarted(row("ana-it-unique-2", "trace-it-unique-2", "req-it-unique-2", "it-unique", "STARTED", 1, 1, future(), "lease-2")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void duplicateSameTupleAndCrossTriggerKeyReuseReturnExistingStartedRun() {
        AnalysisIdempotencyClaim first = guard.claim(request("ana-it-cross-1", "trace-it-cross-1", "req-manual", "it-cross-trigger", AnalysisRunTriggerType.MANUAL_API, "req-manual", "lease-cross-1"));
        AnalysisIdempotencyClaim second = guard.claim(request("ana-it-cross-2", "trace-it-cross-2", "req-scheduled", "it-cross-trigger", AnalysisRunTriggerType.SCHEDULED, "scheduled:10:00", "lease-cross-2"));

        assertThat(first.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.CLAIMED_NEW);
        assertThat(second.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
        assertThat(second.getRun().getAnalysisId()).isEqualTo(first.getRun().getAnalysisId());
        assertThat(rowCount("it-cross-trigger")).isEqualTo(1);
    }

    @Test
    void tenSequentialSameRequestsReuseOneCanonicalRun() {
        List<AnalysisIdempotencyClaim> claims = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            claims.add(guard.claim(request("ana-it-sequential-" + i, "trace-it-sequential-" + i,
                    "req-it-sequential-" + i, "it-sequential", AnalysisRunTriggerType.MANUAL_API,
                    "req-it-sequential-" + i, "lease-it-sequential-" + i)));
        }

        assertCanonicalClaims(claims, 10);
        assertThat(rowCount("it-sequential")).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0} concurrent H2 claims reuse one canonical analysis run")
    @ValueSource(ints = {2, 10, 50})
    void concurrentSameRequestsReuseOneCanonicalRun(int workers) throws Exception {
        String key = "it-concurrent-" + workers;

        List<AnalysisIdempotencyClaim> claims = concurrentClaims(workers, key);

        assertCanonicalClaims(claims, workers);
        assertThat(rowCount(key)).isEqualTo(1);
    }

    @Test
    void differentKeysCreateDifferentAnalysisRuns() {
        AnalysisIdempotencyClaim first = guard.claim(request("ana-it-key-a", "trace-it-key-a", "req-it-key-a",
                "it-key-a", AnalysisRunTriggerType.MANUAL_API, "req-it-key-a", "lease-it-key-a"));
        AnalysisIdempotencyClaim second = guard.claim(request("ana-it-key-b", "trace-it-key-b", "req-it-key-b",
                "it-key-b", AnalysisRunTriggerType.MANUAL_API, "req-it-key-b", "lease-it-key-b"));

        assertThat(first.getRun().getAnalysisId()).isNotEqualTo(second.getRun().getAnalysisId());
        assertThat(rowCount("it-key-a")).isEqualTo(1);
        assertThat(rowCount("it-key-b")).isEqualTo(1);
    }

    @Test
    void sameKeyWithDifferentCanonicalSnapshotFailsClosed() {
        guard.claim(request("ana-it-payload-a", "trace-it-payload-a", "req-it-payload-a", "it-payload",
                AnalysisRunTriggerType.MANUAL_API, "req-it-payload-a", "lease-it-payload-a",
                "{\"symbol\":\"BTCUSDT\",\"strategy\":\"A\"}"));

        assertThatThrownBy(() -> guard.claim(request("ana-it-payload-b", "trace-it-payload-b",
                "req-it-payload-b", "it-payload", AnalysisRunTriggerType.MANUAL_API,
                "req-it-payload-b", "lease-it-payload-b",
                "{\"symbol\":\"BTCUSDT\",\"strategy\":\"B\"}")))
                .isInstanceOfSatisfying(AnalysisRunInputException.class,
                        error -> assertThat(error.getReasonCode())
                                .isEqualTo("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH"));
        assertThat(rowCount("it-payload")).isEqualTo(1);
    }

    @Test
    void duplicateClaimLeavesFollowingTransactionHealthyForReadAndWrite() {
        AnalysisIdempotencyClaim first = guard.claim(request("ana-it-health-a", "trace-it-health-a",
                "req-it-health-a", "it-health", AnalysisRunTriggerType.MANUAL_API,
                "req-it-health-a", "lease-it-health-a"));
        AnalysisIdempotencyClaim duplicate = guard.claim(request("ana-it-health-b", "trace-it-health-b",
                "req-it-health-b", "it-health", AnalysisRunTriggerType.MANUAL_API,
                "req-it-health-b", "lease-it-health-b"));
        AnalysisIdempotencyClaim next = guard.claim(request("ana-it-health-next", "trace-it-health-next",
                "req-it-health-next", "it-health-next", AnalysisRunTriggerType.MANUAL_API,
                "req-it-health-next", "lease-it-health-next"));

        assertThat(duplicate.getRun().getAnalysisId()).isEqualTo(first.getRun().getAnalysisId());
        assertThat(next.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.CLAIMED_NEW);
        assertThat(mapper.selectById(next.getRun().getAnalysisId())).isNotNull();
    }

    @Test
    void failedRunRecoveryKeepsAnalysisAndTraceIdAndAdvancesAttempt() {
        AnalysisIdempotencyClaim first = guard.claim(request("ana-it-recover", "trace-it-recover", "req-it-recover-1", "it-recover", AnalysisRunTriggerType.MANUAL_API, "req-it-recover-1", "lease-recover-1"));
        guard.markFailed(context(first.getRun()), "RuntimeException", "temporary failure");

        AnalysisIdempotencyClaim recovered = guard.claim(request("ana-it-recover-new", "trace-it-recover-new", "req-it-recover-2", "it-recover", AnalysisRunTriggerType.MANUAL_API, "req-it-recover-2", "lease-recover-2"));

        assertThat(recovered.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.RECOVERED_FAILED);
        assertThat(recovered.getRun().getAnalysisId()).isEqualTo(first.getRun().getAnalysisId());
        assertThat(recovered.getRun().getTraceId()).isEqualTo(first.getRun().getTraceId());
        assertThat(recovered.getRun().getAttemptCount()).isEqualTo(2);
        assertThat(recovered.getRun().getRequestId()).isEqualTo("req-it-recover-2");
    }

    @Test
    void partialStateBlocksFailedRecoveryBeforeNewExecutorCanOverwrite() {
        AnalysisIdempotencyClaim first = guard.claim(request("ana-it-partial", "trace-it-partial", "req-it-partial-1", "it-partial", AnalysisRunTriggerType.MANUAL_API, "req-it-partial-1", "lease-partial-1"));
        guard.markFailed(context(first.getRun()), "RuntimeException", "partial failure");
        jdbc.update("INSERT INTO tm_evidence_item(evidence_id, analysis_id, evidence_type, description) VALUES (?,?,?,?)",
                "ev-it-partial", first.getRun().getAnalysisId(), "TEST", "partial downstream row");

        AnalysisIdempotencyClaim blocked = guard.claim(request("ana-it-partial-new", "trace-it-partial-new", "req-it-partial-2", "it-partial", AnalysisRunTriggerType.MANUAL_API, "req-it-partial-2", "lease-partial-2"));

        assertThat(blocked.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.RECOVERY_BLOCKED_PARTIAL_STATE);
        assertThat(mapper.selectById(first.getRun().getAnalysisId()).getStatus()).isEqualTo("FAILED");
    }

    @Test
    void maxRecoveryAttemptsBlocksRecovery() {
        mapper.insertStarted(row("ana-it-max", "trace-it-max", "req-it-max", "it-max", "FAILED", 3, 1, null, null));

        AnalysisIdempotencyClaim claim = guard.claim(request("ana-it-max-new", "trace-it-max-new", "req-it-max-2", "it-max", AnalysisRunTriggerType.MANUAL_API, "req-it-max-2", "lease-max-2"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.MAX_RECOVERY_ATTEMPTS_EXCEEDED);
        assertThat(claim.getRun().getAnalysisId()).isEqualTo("ana-it-max");
    }

    @Test
    void expiredLeaseConcurrentRecoveryHasSingleWinner() throws Exception {
        mapper.insertStarted(row("ana-it-expired", "trace-it-expired", "req-it-expired-old", "it-expired", "STARTED", 1, 1, past(), "lease-old"));
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<AnalysisIdempotencyClaim>> futures = List.of(
                pool.submit(recoveryTask(barrier, "ana-it-expired-a", "trace-it-expired-a", "req-it-expired-a", "lease-expired-a")),
                pool.submit(recoveryTask(barrier, "ana-it-expired-b", "trace-it-expired-b", "req-it-expired-b", "lease-expired-b")));

        List<AnalysisIdempotencyClaimStatus> statuses = collectStatuses(futures);
        pool.shutdownNow();

        assertThat(statuses).contains(AnalysisIdempotencyClaimStatus.RECOVERED_EXPIRED_LEASE);
        assertThat(statuses.stream().filter(AnalysisIdempotencyClaimStatus.RECOVERED_EXPIRED_LEASE::equals).count()).isEqualTo(1);
        assertThat(rowCount("it-expired")).isEqualTo(1);
        assertThat(mapper.selectById("ana-it-expired").getAttemptCount()).isEqualTo(2);
    }

    private Callable<AnalysisIdempotencyClaim> recoveryTask(CyclicBarrier barrier, String analysisId, String traceId, String requestId, String leaseOwner) {
        return () -> {
            barrier.await(3, TimeUnit.SECONDS);
            return guard.claim(request(analysisId, traceId, requestId, "it-expired", AnalysisRunTriggerType.MANUAL_API, requestId, leaseOwner));
        };
    }

    private List<AnalysisIdempotencyClaimStatus> collectStatuses(List<Future<AnalysisIdempotencyClaim>> futures) throws Exception {
        List<AnalysisIdempotencyClaimStatus> statuses = new ArrayList<>();
        for (Future<AnalysisIdempotencyClaim> future : futures) {
            statuses.add(future.get(3, TimeUnit.SECONDS).getStatus());
        }
        return statuses;
    }

    private List<AnalysisIdempotencyClaim> concurrentClaims(int workers, String key) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(workers);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            List<Future<AnalysisIdempotencyClaim>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                int index = i;
                futures.add(pool.submit(() -> {
                    barrier.await(15, TimeUnit.SECONDS);
                    return guard.claim(request("ana-" + key + "-" + index, "trace-" + key + "-" + index,
                            "req-" + key + "-" + index, key, AnalysisRunTriggerType.MANUAL_API,
                            "req-" + key + "-" + index, "lease-" + key + "-" + index));
                }));
            }
            List<AnalysisIdempotencyClaim> claims = new ArrayList<>();
            for (Future<AnalysisIdempotencyClaim> future : futures) {
                claims.add(future.get(30, TimeUnit.SECONDS));
            }
            return claims;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void assertCanonicalClaims(List<AnalysisIdempotencyClaim> claims, int expectedCount) {
        assertThat(claims).hasSize(expectedCount);
        assertThat(claims.stream().filter(claim ->
                claim.getStatus() == AnalysisIdempotencyClaimStatus.CLAIMED_NEW)).hasSize(1);
        assertThat(claims).extracting(claim -> claim.getRun().getAnalysisId()).containsOnly(
                claims.get(0).getRun().getAnalysisId());
    }

    private AnalysisExecutionContext context(AnalysisRunDO run) {
        return new AnalysisExecutionContext(
                run.getAnalysisId(),
                run.getTraceId(),
                run.getRequestId(),
                run.getIdempotencyKey(),
                run.getSymbol(),
                run.getTimeframe(),
                run.getAnalysisTime(),
                AnalysisTimePolicy.canonicalBucket(run.getAnalysisTime(), run.getTimeframe()),
                run.getRuleVersion(),
                AnalysisRunTriggerType.normalize(run.getTriggerType()),
                run.getTriggerReference(),
                run.getParentAnalysisId(),
                run.getParentTraceId(),
                run.getInputSnapshotJson(),
                run.getInputSnapshotHash(),
                run.getLeaseOwner(),
                run.getVersionNo(),
                run.getAttemptCount(),
                true);
    }

    private AnalysisRunClaimRequest request(String analysisId, String traceId, String requestId, String key,
                                            AnalysisRunTriggerType triggerType, String triggerReference, String leaseOwner) {
        return request(analysisId, traceId, requestId, key, triggerType, triggerReference, leaseOwner, "{}");
    }

    private AnalysisRunClaimRequest request(String analysisId, String traceId, String requestId, String key,
                                            AnalysisRunTriggerType triggerType, String triggerReference,
                                            String leaseOwner, String snapshotJson) {
        return new AnalysisRunClaimRequest(
                analysisId,
                traceId,
                requestId,
                key,
                "BTCUSDT",
                "5m",
                LocalDateTime.of(2026, 6, 23, 10, 4, 59),
                "rules-it",
                triggerType,
                triggerReference,
                null,
                null,
                snapshotJson,
                "hash-" + analysisId,
                leaseOwner,
                future());
    }

    private AnalysisRunDO row(String analysisId, String traceId, String requestId, String key, String status,
                              int attempts, int version, LocalDateTime leaseExpiresAt, String leaseOwner) {
        LocalDateTime now = LocalDateTime.now();
        AnalysisRunDO row = new AnalysisRunDO();
        row.setAnalysisId(analysisId);
        row.setTraceId(traceId);
        row.setRequestId(requestId);
        row.setIdempotencyKey(key);
        row.setSymbol("BTCUSDT");
        row.setTimeframe("5m");
        row.setAnalysisTime(LocalDateTime.of(2026, 6, 23, 10, 4, 59));
        row.setRuleVersion("rules-it");
        row.setTriggerType(AnalysisRunTriggerType.MANUAL_API.name());
        row.setTriggerReference(requestId);
        row.setInputSnapshotJson("{}");
        row.setInputSnapshotHash("hash-" + analysisId);
        row.setStatus(status);
        row.setAttemptCount(attempts);
        row.setLeaseOwner(leaseOwner);
        row.setLeaseExpiresAt(leaseExpiresAt);
        row.setStartedAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        row.setVersionNo(version);
        row.setOwnerType("SYSTEM");
        row.setOwnerId(0L);
        row.setPreview(false);
        return row;
    }

    private int rowCount(String key) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM tm_analysis_run WHERE idempotency_key = ?", Integer.class, key);
        return count != null ? count : 0;
    }

    private static LocalDateTime future() {
        return LocalDateTime.now().plusMinutes(5);
    }

    private static LocalDateTime past() {
        return LocalDateTime.now(java.time.Clock.systemUTC()).minusMinutes(5);
    }

    private void clean() {
        jdbc.update("DELETE FROM tm_evidence_item WHERE analysis_id LIKE 'ana-it-%'");
        jdbc.update("DELETE FROM tm_analysis_run WHERE analysis_id LIKE 'ana-it-%' OR idempotency_key LIKE 'it-%'");
    }
}
