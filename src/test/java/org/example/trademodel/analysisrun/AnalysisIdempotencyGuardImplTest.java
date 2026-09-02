package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisIdempotencyGuardImplTest {
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-06-23T10:00:00Z"), ZoneOffset.UTC);

    @Mock private AnalysisRunMapper mapper;

    private AnalysisRunProperties properties;
    private AnalysisIdempotencyGuardImpl guard;

    @BeforeEach
    void setUp() {
        properties = new AnalysisRunProperties();
        properties.getIdempotency().setMaxRecoveryAttempts(3);
        guard = new AnalysisIdempotencyGuardImpl(mapper, properties, new NoopTransactionManager(), FIXED);
    }

    @Test
    void newClaimInsertsStartedRun() {
        when(mapper.insertStartedIfAbsent(any())).thenReturn(1);

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-new"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.CLAIMED_NEW);
        assertThat(claim.getRun().getStatus()).isEqualTo("STARTED");
        assertThat(claim.getRun().getIdempotencyKey()).isEqualTo("idem-new");
    }

    @Test
    void duplicateSuccessBlocksReExecution() {
        when(mapper.insertStartedIfAbsent(any())).thenReturn(0);
        when(mapper.selectByIdempotencyKey("idem-success")).thenReturn(row("ana-success", "SUCCESS", 1, null));

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-success"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.DUPLICATE_SUCCESS);
        verify(mapper, never()).recoverFailed(anyString(), anyString(), anyString(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void activeStartedLeaseBlocksConcurrentTrigger() {
        when(mapper.insertStartedIfAbsent(any())).thenReturn(0);
        when(mapper.selectByIdempotencyKey("idem-active"))
                .thenReturn(row("ana-active", "STARTED", 1, LocalDateTime.of(2026, 6, 23, 10, 2)));

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-active"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
    }

    @Test
    void failedRunWithPartialStateBlocksRecovery() {
        when(mapper.insertStartedIfAbsent(any())).thenReturn(0);
        AnalysisRunDO failed = row("ana-failed", "FAILED", 1, null);
        when(mapper.selectByIdempotencyKey("idem-partial")).thenReturn(failed);
        when(mapper.countPartialStateRows("ana-failed")).thenReturn(2);

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-partial"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.RECOVERY_BLOCKED_PARTIAL_STATE);
        verify(mapper, never()).recoverFailed(anyString(), anyString(), anyString(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void expiredLeaseCanBeRecoveredAtomically() {
        when(mapper.insertStartedIfAbsent(any())).thenReturn(0);
        AnalysisRunDO expired = row("ana-expired", "STARTED", 1, LocalDateTime.of(2026, 6, 23, 9, 59));
        AnalysisRunDO recovered = row("ana-expired", "STARTED", 2, LocalDateTime.of(2026, 6, 23, 10, 2));
        when(mapper.selectByIdempotencyKey("idem-expired")).thenReturn(expired);
        when(mapper.countPartialStateRows("ana-expired")).thenReturn(0);
        when(mapper.recoverExpiredLease(anyString(), anyString(), anyString(), any(), any(), anyInt(), anyInt()))
                .thenReturn(1);
        when(mapper.selectById("ana-expired")).thenReturn(recovered);

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-expired"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.RECOVERED_EXPIRED_LEASE);
        assertThat(claim.getRun().getAttemptCount()).isEqualTo(2);
    }

    @Test
    void fallbackDuplicateIsReadOnlyAfterTheFailedInsertTransactionEnds() {
        when(mapper.insertStartedIfAbsent(any())).thenThrow(new DuplicateKeyException("dup"));
        when(mapper.selectByIdempotencyKey("idem-success"))
                .thenReturn(row("ana-success", "SUCCESS", 1, null));

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-success"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.DUPLICATE_SUCCESS);
    }

    @Test
    void duplicateWithoutCanonicalIdempotencyRowRemainsARealPersistenceFailure() {
        DuplicateKeyException collision = new DuplicateKeyException("analysis id collision");
        when(mapper.insertStartedIfAbsent(any())).thenThrow(collision);
        when(mapper.selectByIdempotencyKey("idem-id-collision")).thenReturn(null);

        assertThatThrownBy(() -> guard.claim(request("idem-id-collision")))
                .isSameAs(collision);
    }

    @Test
    void sameKeyWithDifferentCanonicalInputFailsClosed() {
        when(mapper.insertStartedIfAbsent(any())).thenReturn(0);
        AnalysisRunDO existing = row("ana-mismatch", "STARTED", 1,
                LocalDateTime.of(2026, 6, 23, 10, 2));
        existing.setOwnerId(42L);
        when(mapper.selectByIdempotencyKey("idem-mismatch")).thenReturn(existing);

        assertThatThrownBy(() -> guard.claim(request("idem-mismatch")))
                .isInstanceOfSatisfying(AnalysisRunInputException.class,
                        error -> assertThat(error.getReasonCode())
                                .isEqualTo("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH"));
    }

    @Test
    void requestAndTraceMetadataDoNotChangeCanonicalInputFingerprint() {
        when(mapper.insertStartedIfAbsent(any())).thenReturn(0);
        AnalysisRunDO existing = row("ana-metadata", "SUCCESS", 1, null);
        existing.setInputSnapshotJson("{\"symbol\":\"BTCUSDT\",\"canonicalAnalysisTimeBucket\":\"2026-06-23T10:00\","
                + "\"requestId\":\"old\",\"traceId\":\"old\",\"triggerType\":\"SCHEDULED\"}");
        when(mapper.selectByIdempotencyKey("idem-metadata")).thenReturn(existing);
        AnalysisRunClaimRequest request = request("idem-metadata",
                "{\"traceId\":\"new\",\"requestId\":\"new\",\"triggerType\":\"MANUAL_API\","
                        + "\"canonicalAnalysisTimeBucket\":\"2026-06-23T10:00\",\"symbol\":\"BTCUSDT\"}");

        assertThat(guard.claim(request).getStatus())
                .isEqualTo(AnalysisIdempotencyClaimStatus.DUPLICATE_SUCCESS);
    }

    @Test
    void markFailedUsesLeaseOwnerVersionAndRedactsSensitiveMessage() {
        AnalysisExecutionContext context = new AnalysisExecutionContext(
                "ana-fail", "trace-fail", "req-fail", "idem-fail", "BTCUSDT", "1m",
                LocalDateTime.of(2026, 6, 23, 10, 0),
                LocalDateTime.of(2026, 6, 23, 10, 0),
                "v1.0", AnalysisRunTriggerType.MANUAL_API, "req-fail", null, null,
                "{}", "hash", "lease-owner-1", 7, 2, true);

        guard.markFailed(context, "RuntimeException",
                "Authorization: Bearer SECRET https://api.example.test/path?api_key=SECRET&x=1 token=SECRET");

        verify(mapper).markFailed(
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.argThat(msg -> msg.contains("<redacted>")
                        && !msg.contains("Bearer SECRET")
                        && !msg.contains("api_key=SECRET")
                        && !msg.contains("token=SECRET")),
                any(),
                org.mockito.ArgumentMatchers.eq("lease-owner-1"),
                org.mockito.ArgumentMatchers.eq(7));
    }

    private static AnalysisRunClaimRequest request(String key) {
        return request(key, "{}");
    }

    private static AnalysisRunClaimRequest request(String key, String snapshotJson) {
        return new AnalysisRunClaimRequest("ana-new", "trace-new", "req-test", key,
                "BTCUSDT", "1m", LocalDateTime.of(2026, 6, 23, 10, 0), "v1.0",
                AnalysisRunTriggerType.MANUAL_API, "req-test", null, null,
                snapshotJson, "hash", "lease-test", LocalDateTime.of(2026, 6, 23, 10, 2));
    }

    private static AnalysisRunDO row(String analysisId, String status, int attempts, LocalDateTime leaseExpiresAt) {
        AnalysisRunDO row = new AnalysisRunDO();
        row.setAnalysisId(analysisId);
        row.setSymbol("BTCUSDT");
        row.setTimeframe("1m");
        row.setAnalysisTime(LocalDateTime.of(2026, 6, 23, 10, 0));
        row.setRuleVersion("v1.0");
        row.setTraceId("trace-" + analysisId);
        row.setRequestId("req-test");
        row.setIdempotencyKey("idem-" + analysisId);
        row.setTriggerType(AnalysisRunTriggerType.MANUAL_API.name());
        row.setStatus(status);
        row.setAttemptCount(attempts);
        row.setLeaseExpiresAt(leaseExpiresAt);
        row.setVersionNo(1);
        row.setInputSnapshotJson("{}");
        row.setInputSnapshotHash("hash");
        row.setOwnerType("SYSTEM");
        row.setOwnerId(0L);
        row.setPreview(false);
        row.setAnalysisMode("OPPORTUNITY_DECISION");
        return row;
    }

    private static final class NoopTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
