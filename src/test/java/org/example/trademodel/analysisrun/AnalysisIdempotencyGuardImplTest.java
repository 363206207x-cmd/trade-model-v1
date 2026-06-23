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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisIdempotencyGuardImplTest {
    @Mock private AnalysisRunMapper mapper;

    private AnalysisRunProperties properties;
    private AnalysisIdempotencyGuardImpl guard;

    @BeforeEach
    void setUp() {
        properties = new AnalysisRunProperties();
        properties.getIdempotency().setMaxRecoveryAttempts(3);
        guard = new AnalysisIdempotencyGuardImpl(mapper, properties, new NoopTransactionManager());
    }

    @Test
    void newClaimInsertsStartedRun() {
        when(mapper.insertStarted(any())).thenReturn(1);

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-new"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.CLAIMED_NEW);
        assertThat(claim.getRun().getStatus()).isEqualTo("STARTED");
        assertThat(claim.getRun().getIdempotencyKey()).isEqualTo("idem-new");
    }

    @Test
    void duplicateSuccessBlocksReExecution() {
        when(mapper.insertStarted(any())).thenThrow(new DuplicateKeyException("dup"));
        when(mapper.selectByIdempotencyKey("idem-success")).thenReturn(row("ana-success", "SUCCESS", 1, null));

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-success"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.DUPLICATE_SUCCESS);
        verify(mapper, never()).recoverFailed(anyString(), anyString(), anyString(), any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void activeStartedLeaseBlocksConcurrentTrigger() {
        when(mapper.insertStarted(any())).thenThrow(new DuplicateKeyException("dup"));
        when(mapper.selectByIdempotencyKey("idem-active"))
                .thenReturn(row("ana-active", "STARTED", 1, LocalDateTime.now().plusMinutes(2)));

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-active"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
    }

    @Test
    void failedRunWithPartialStateBlocksRecovery() {
        when(mapper.insertStarted(any())).thenThrow(new DuplicateKeyException("dup"));
        AnalysisRunDO failed = row("ana-failed", "FAILED", 1, null);
        when(mapper.selectByIdempotencyKey("idem-partial")).thenReturn(failed);
        when(mapper.countPartialStateRows("ana-failed")).thenReturn(2);

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-partial"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.RECOVERY_BLOCKED_PARTIAL_STATE);
        verify(mapper, never()).recoverFailed(anyString(), anyString(), anyString(), any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void expiredLeaseCanBeRecoveredAtomically() {
        when(mapper.insertStarted(any())).thenThrow(new DuplicateKeyException("dup"));
        AnalysisRunDO expired = row("ana-expired", "STARTED", 1, LocalDateTime.now().minusMinutes(1));
        AnalysisRunDO recovered = row("ana-expired", "STARTED", 2, LocalDateTime.now().plusMinutes(2));
        when(mapper.selectByIdempotencyKey("idem-expired")).thenReturn(expired);
        when(mapper.countPartialStateRows("ana-expired")).thenReturn(0);
        when(mapper.recoverExpiredLease(anyString(), anyString(), anyString(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(1);
        when(mapper.selectById("ana-expired")).thenReturn(recovered);

        AnalysisIdempotencyClaim claim = guard.claim(request("idem-expired"));

        assertThat(claim.getStatus()).isEqualTo(AnalysisIdempotencyClaimStatus.RECOVERED_EXPIRED_LEASE);
        assertThat(claim.getRun().getAttemptCount()).isEqualTo(2);
    }

    private static AnalysisRunClaimRequest request(String key) {
        return new AnalysisRunClaimRequest("ana-new", "trace-new", "req-test", key,
                "BTCUSDT", "1m", LocalDateTime.of(2026, 6, 23, 10, 0), "v1.0",
                AnalysisRunTriggerType.MANUAL_API, "req-test", null, null,
                "{}", "hash", "lease-test", LocalDateTime.now().plusMinutes(2));
    }

    private static AnalysisRunDO row(String analysisId, String status, int attempts, LocalDateTime leaseExpiresAt) {
        AnalysisRunDO row = new AnalysisRunDO();
        row.setAnalysisId(analysisId);
        row.setSymbol("BTCUSDT");
        row.setTimeframe("1m");
        row.setTraceId("trace-" + analysisId);
        row.setRequestId("req-test");
        row.setIdempotencyKey("idem-" + analysisId);
        row.setTriggerType(AnalysisRunTriggerType.MANUAL_API.name());
        row.setStatus(status);
        row.setAttemptCount(attempts);
        row.setLeaseExpiresAt(leaseExpiresAt);
        row.setVersionNo(1);
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
