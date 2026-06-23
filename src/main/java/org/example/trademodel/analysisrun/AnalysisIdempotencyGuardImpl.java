package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
public class AnalysisIdempotencyGuardImpl implements AnalysisIdempotencyGuard {
    private static final String STATUS_STARTED = "STARTED";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final AnalysisRunMapper analysisRunMapper;
    private final AnalysisRunProperties properties;
    private final TransactionTemplate transactionTemplate;

    public AnalysisIdempotencyGuardImpl(AnalysisRunMapper analysisRunMapper,
                                        AnalysisRunProperties properties,
                                        PlatformTransactionManager transactionManager) {
        this.analysisRunMapper = analysisRunMapper;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public AnalysisIdempotencyClaim claim(AnalysisRunClaimRequest request) {
        return transactionTemplate.execute(status -> claimInTransaction(request));
    }

    private AnalysisIdempotencyClaim claimInTransaction(AnalysisRunClaimRequest request) {
        LocalDateTime now = LocalDateTime.now();
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(request.getAnalysisId());
        run.setSymbol(request.getSymbol());
        run.setTimeframe(request.getTimeframe());
        run.setAnalysisTime(request.getAnalysisTime());
        run.setRuleVersion(request.getRuleVersion());
        run.setDataQualityScore(null);
        run.setTraceId(request.getTraceId());
        run.setStatus(STATUS_STARTED);
        run.setIdempotencyKey(request.getIdempotencyKey());
        run.setRequestId(request.getRequestId());
        run.setTriggerType(request.getTriggerType().name());
        run.setTriggerReference(request.getTriggerReference());
        run.setParentAnalysisId(request.getParentAnalysisId());
        run.setParentTraceId(request.getParentTraceId());
        run.setInputSnapshotJson(request.getInputSnapshotJson());
        run.setInputSnapshotHash(request.getInputSnapshotHash());
        run.setAttemptCount(1);
        run.setLeaseOwner(request.getLeaseOwner());
        run.setLeaseExpiresAt(request.getLeaseExpiresAt());
        run.setStartedAt(now);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        run.setVersionNo(1);
        try {
            analysisRunMapper.insertStarted(run);
            return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.CLAIMED_NEW, run,
                    "CLAIMED_NEW", "new analysis run claimed");
        } catch (DuplicateKeyException duplicate) {
            return handleExisting(request, now);
        }
    }

    private AnalysisIdempotencyClaim handleExisting(AnalysisRunClaimRequest request, LocalDateTime now) {
        AnalysisRunDO existing = analysisRunMapper.selectByIdempotencyKey(request.getIdempotencyKey());
        if (existing == null) {
            return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.IN_PROGRESS, null,
                    "IDEMPOTENCY_ROW_MISSING", "duplicate key was observed but the row could not be loaded");
        }
        String status = existing.getStatus();
        if (STATUS_SUCCESS.equals(status)) {
            return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.DUPLICATE_SUCCESS, existing,
                    "IDEMPOTENCY_DUPLICATE_SUCCESS", "successful run already exists for idempotency key");
        }
        if (STATUS_FAILED.equals(status)) {
            return recoverFailed(request, existing, now);
        }
        if (STATUS_STARTED.equals(status)) {
            LocalDateTime leaseExpiresAt = existing.getLeaseExpiresAt();
            if (leaseExpiresAt != null && leaseExpiresAt.isBefore(now)) {
                return recoverExpiredLease(request, existing, now);
            }
            return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.IN_PROGRESS, existing,
                    "IDEMPOTENCY_IN_PROGRESS", "analysis run lease is still active");
        }
        return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.IN_PROGRESS, existing,
                "IDEMPOTENCY_UNKNOWN_STATUS", "analysis run has an unknown status");
    }

    private AnalysisIdempotencyClaim recoverFailed(AnalysisRunClaimRequest request, AnalysisRunDO existing, LocalDateTime now) {
        AnalysisIdempotencyClaim blocked = blockIfPartialOrMaxAttempts(existing);
        if (blocked != null) {
            return blocked;
        }
        int updated = analysisRunMapper.recoverFailed(
                existing.getAnalysisId(), request.getRequestId(), request.getLeaseOwner(), request.getLeaseExpiresAt(),
                now, safeVersion(existing), properties.getIdempotency().getMaxRecoveryAttempts());
        AnalysisRunDO recovered = analysisRunMapper.selectById(existing.getAnalysisId());
        if (updated == 1 && recovered != null) {
            return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.RECOVERED_FAILED, recovered,
                    "FAILED_RUN_RECOVERED", "failed run recovered for retry");
        }
        return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.IN_PROGRESS,
                recovered != null ? recovered : existing,
                "FAILED_RECOVERY_RACE_LOST", "another request claimed failed run recovery first");
    }

    private AnalysisIdempotencyClaim recoverExpiredLease(AnalysisRunClaimRequest request, AnalysisRunDO existing, LocalDateTime now) {
        AnalysisIdempotencyClaim blocked = blockIfPartialOrMaxAttempts(existing);
        if (blocked != null) {
            return blocked;
        }
        int updated = analysisRunMapper.recoverExpiredLease(
                existing.getAnalysisId(), request.getRequestId(), request.getLeaseOwner(), request.getLeaseExpiresAt(),
                now, safeVersion(existing), properties.getIdempotency().getMaxRecoveryAttempts());
        AnalysisRunDO recovered = analysisRunMapper.selectById(existing.getAnalysisId());
        if (updated == 1 && recovered != null) {
            return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.RECOVERED_EXPIRED_LEASE, recovered,
                    "EXPIRED_LEASE_RECOVERED", "expired analysis lease recovered for retry");
        }
        return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.IN_PROGRESS,
                recovered != null ? recovered : existing,
                "LEASE_RECOVERY_RACE_LOST", "another request claimed expired lease recovery first");
    }

    private AnalysisIdempotencyClaim blockIfPartialOrMaxAttempts(AnalysisRunDO existing) {
        Integer partialRows = analysisRunMapper.countPartialStateRows(existing.getAnalysisId());
        if (partialRows != null && partialRows > 0) {
            return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.RECOVERY_BLOCKED_PARTIAL_STATE, existing,
                    "PARTIAL_STATE_RECOVERY_BLOCKED", "downstream analysis rows exist; recovery is blocked for review");
        }
        int attempts = existing.getAttemptCount() != null ? existing.getAttemptCount() : 1;
        if (attempts >= properties.getIdempotency().getMaxRecoveryAttempts()) {
            return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.MAX_RECOVERY_ATTEMPTS_EXCEEDED, existing,
                    "MAX_RECOVERY_ATTEMPTS_EXCEEDED", "analysis run recovery attempt limit reached");
        }
        return null;
    }

    @Override
    public void markFailed(String analysisId, String errorCode, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> analysisRunMapper.markFailed(
                analysisId,
                truncate(errorCode, 128),
                truncate(errorMessage, 512),
                LocalDateTime.now()));
    }

    private static int safeVersion(AnalysisRunDO row) {
        return row.getVersionNo() != null ? row.getVersionNo() : 1;
    }

    private static String truncate(String raw, int max) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
