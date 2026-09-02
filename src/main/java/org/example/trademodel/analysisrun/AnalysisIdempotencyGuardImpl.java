package org.example.trademodel.analysisrun;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

@Service
public class AnalysisIdempotencyGuardImpl implements AnalysisIdempotencyGuard {
    private static final String STATUS_STARTED = "STARTED";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String PAYLOAD_MISMATCH = "IDEMPOTENCY_KEY_PAYLOAD_MISMATCH";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> NON_CANONICAL_SNAPSHOT_FIELDS = Set.of(
            "analysisTime",
            "requestId",
            "traceId",
            "triggerType",
            "triggerReference",
            "parentAnalysisId",
            "parentTraceId");
    private static final Pattern AUTHORIZATION = Pattern.compile("(?i)(authorization\\s*[:=]\\s*)([^,;]+)");
    private static final Pattern SECRET_PARAM = Pattern.compile("(?i)(api[_-]?key|token|access[_-]?token|secret)=([^&\\s]+)");
    private static final Pattern URL_QUERY = Pattern.compile("https?://([^\\s?]+)\\?[^\\s]+", Pattern.CASE_INSENSITIVE);

    private final AnalysisRunMapper analysisRunMapper;
    private final AnalysisRunProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public AnalysisIdempotencyGuardImpl(AnalysisRunMapper analysisRunMapper,
                                        AnalysisRunProperties properties,
                                        PlatformTransactionManager transactionManager,
                                        Clock analysisRunClock) {
        this.analysisRunMapper = analysisRunMapper;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = analysisRunClock != null ? analysisRunClock : Clock.systemUTC();
    }

    @Override
    public AnalysisIdempotencyClaim claim(AnalysisRunClaimRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        AnalysisRunDO run = newStartedRun(request, now);
        DuplicateKeyException fallbackCollision = null;
        Integer inserted;
        try {
            inserted = transactionTemplate.execute(status -> analysisRunMapper.insertStartedIfAbsent(run));
        } catch (DuplicateKeyException duplicate) {
            // Non-PostgreSQL fallback variants may still signal a collision. The
            // failed transaction has ended before the canonical row is read.
            fallbackCollision = duplicate;
            inserted = 0;
        }
        if (Integer.valueOf(1).equals(inserted)) {
            return new AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus.CLAIMED_NEW, run,
                    "CLAIMED_NEW", "new analysis run claimed");
        }
        DuplicateKeyException collision = fallbackCollision;
        return transactionTemplate.execute(status -> handleExisting(request, now, collision));
    }

    private static AnalysisRunDO newStartedRun(AnalysisRunClaimRequest request, LocalDateTime now) {
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
        run.setOwnerType(request.getOwnerType());
        run.setOwnerId(request.getOwnerId());
        run.setAssetId(request.getAssetId());
        run.setPreview(request.isPreview());
        run.setAnalysisMode(request.isPreview() ? "ANALYSIS_PREVIEW" : "OPPORTUNITY_DECISION");
        return run;
    }

    private AnalysisIdempotencyClaim handleExisting(AnalysisRunClaimRequest request, LocalDateTime now,
                                                     DuplicateKeyException fallbackCollision) {
        AnalysisRunDO existing = analysisRunMapper.selectByIdempotencyKey(request.getIdempotencyKey());
        if (existing == null) {
            if (fallbackCollision != null) {
                throw fallbackCollision;
            }
            throw new IllegalStateException("IDEMPOTENCY_ROW_MISSING");
        }
        requireCanonicalPayloadMatch(request, existing);
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

    private static void requireCanonicalPayloadMatch(AnalysisRunClaimRequest request, AnalysisRunDO existing) {
        boolean matches = equalsIgnoreCase(request.getSymbol(), existing.getSymbol())
                && Objects.equals(request.getTimeframe(), existing.getTimeframe())
                && Objects.equals(canonicalBucket(request.getAnalysisTime(), request.getTimeframe()),
                canonicalBucket(existing.getAnalysisTime(), existing.getTimeframe()))
                && Objects.equals(trimToNull(request.getRuleVersion()), trimToNull(existing.getRuleVersion()))
                && equalsIgnoreCase(request.getOwnerType(), existing.getOwnerType())
                && Objects.equals(request.getOwnerId(), existing.getOwnerId())
                && Objects.equals(request.getAssetId(), existing.getAssetId())
                && Objects.equals(request.isPreview(), Boolean.TRUE.equals(existing.getPreview()))
                && Objects.equals(request.isPreview() ? "ANALYSIS_PREVIEW" : "OPPORTUNITY_DECISION",
                existing.getAnalysisMode())
                && Objects.equals(canonicalInputFingerprint(request.getInputSnapshotJson()),
                canonicalInputFingerprint(existing.getInputSnapshotJson()));
        if (!matches) {
            throw new AnalysisRunInputException(PAYLOAD_MISMATCH,
                    "idempotency key does not match the canonical analysis input");
        }
    }

    private static LocalDateTime canonicalBucket(LocalDateTime analysisTime, String timeframe) {
        if (analysisTime == null || timeframe == null) {
            return null;
        }
        return AnalysisTimePolicy.canonicalBucket(analysisTime, timeframe);
    }

    private static String canonicalInputFingerprint(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return "";
        }
        try {
            Map<String, Object> fields = JSON.readValue(snapshotJson, new TypeReference<>() { });
            NON_CANONICAL_SNAPSHOT_FIELDS.forEach(fields::remove);
            return JSON.writeValueAsString(new TreeMap<>(fields));
        } catch (Exception ignored) {
            return snapshotJson.trim();
        }
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return Objects.equals(normalizeCase(left), normalizeCase(right));
    }

    private static String normalizeCase(String value) {
        String normalized = trimToNull(value);
        return normalized != null ? normalized.toUpperCase(Locale.ROOT) : null;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
    public void markFailed(AnalysisExecutionContext context, String errorCode, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> analysisRunMapper.markFailed(
                context.getAnalysisId(),
                truncate(redact(errorCode), 128),
                truncate(redact(errorMessage), 512),
                LocalDateTime.now(clock),
                context.getLeaseOwner(),
                safeVersion(context.getClaimVersion())));
    }

    private static int safeVersion(AnalysisRunDO row) {
        return row.getVersionNo() != null ? row.getVersionNo() : 1;
    }

    private static int safeVersion(Integer version) {
        return version != null ? version : 1;
    }

    static String redact(String raw) {
        if (raw == null) {
            return null;
        }
        String t = AUTHORIZATION.matcher(raw).replaceAll("$1<redacted>");
        t = SECRET_PARAM.matcher(t).replaceAll("$1=<redacted>");
        t = URL_QUERY.matcher(t).replaceAll("https://$1?<redacted>");
        return t;
    }

    private static String truncate(String raw, int max) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
