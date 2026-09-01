package org.example.trademodel.analysisrun;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.example.trademodel.service.AnalysisAssemblerService;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnalysisRunOrchestratorImpl implements AnalysisRunOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(AnalysisRunOrchestratorImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern AUTHORIZATION = Pattern.compile("(?i)(authorization\\s*[:=]\\s*)([^,;]+)");
    private static final Pattern SECRET_PARAM = Pattern.compile("(?i)(api[_-]?key|token|access[_-]?token|secret)=([^&\\s]+)");
    private static final Pattern URL_QUERY = Pattern.compile("https?://([^\\s?]+)\\?[^\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_VALUES = Pattern.compile("(?i)(VALUES\\s*\\()([^)]*)(\\))");
    private static final Pattern H2_UNIQUE_CONSTRAINT = Pattern.compile(
            "(?i)\"(?:[A-Z0-9_]+\\.)?([A-Z0-9_]+)\\s+ON\\s+(?:[A-Z0-9_]+\\.)?([A-Z0-9_]+)\\(");
    private static final Pattern NAMED_UNIQUE_CONSTRAINT = Pattern.compile(
            "(?i)unique constraint \\\"([A-Z0-9_]+)\\\"");
    private static final Pattern NAMED_CHECK_CONSTRAINT = Pattern.compile(
            "(?i)(?:check constraint violation:\\s*\\\"?|violates check constraint\\s+\\\"?)([A-Z0-9_]+)");

    private final AnalysisIdempotencyGuard idempotencyGuard;
    private final AnalysisAssemblerService assemblerService;
    private final RuleConfigService ruleConfigService;
    private final AnalysisRunProperties properties;
    private final Clock clock;

    public AnalysisRunOrchestratorImpl(AnalysisIdempotencyGuard idempotencyGuard,
                                       AnalysisAssemblerService assemblerService,
                                       RuleConfigService ruleConfigService,
                                       AnalysisRunProperties properties,
                                       Clock analysisRunClock) {
        this.idempotencyGuard = idempotencyGuard;
        this.assemblerService = assemblerService;
        this.ruleConfigService = ruleConfigService;
        this.properties = properties;
        this.clock = analysisRunClock != null ? analysisRunClock : Clock.systemUTC();
    }

    @Override
    public AnalysisRunResult run(AnalysisRunCommand command) {
        NormalizedCommand normalized = normalize(command);
        String analysisId = AnalysisRunIds.analysisId();
        String traceId = AnalysisRunIds.traceId();
        String idempotencyKey = idempotencyKey(normalized);
        String inputSnapshotJson = inputSnapshotJson(normalized, traceId, idempotencyKey);
        String inputSnapshotHash = sha256(inputSnapshotJson);
        AnalysisRunClaimRequest request = new AnalysisRunClaimRequest(
                analysisId,
                traceId,
                normalized.requestId(),
                idempotencyKey,
                normalized.symbol(),
                normalized.timeframe(),
                normalized.analysisTime(),
                normalized.ruleVersion(),
                normalized.triggerType(),
                normalized.triggerReference(),
                normalized.parentAnalysisId(),
                normalized.parentTraceId(),
                inputSnapshotJson,
                inputSnapshotHash,
                AnalysisRunIds.leaseOwner(),
                LocalDateTime.now(clock).plusSeconds(properties.getIdempotency().getLeaseSeconds()),
                normalized.ownerType(), normalized.ownerId(), normalized.assetId(), normalized.preview());

        AnalysisIdempotencyClaim claim = idempotencyGuard.claim(request);
        if (claim.getStatus() == AnalysisIdempotencyClaimStatus.DUPLICATE_SUCCESS) {
            return AnalysisRunResult.duplicateSuccess(claim.getRun());
        }
        if (claim.getStatus() == AnalysisIdempotencyClaimStatus.IN_PROGRESS) {
            return AnalysisRunResult.inProgress(claim.getRun());
        }
        if (claim.getStatus() == AnalysisIdempotencyClaimStatus.RECOVERY_BLOCKED_PARTIAL_STATE) {
            return AnalysisRunResult.recoveryBlocked(claim.getRun(), claim.getReasonCode(), claim.getMessage());
        }
        if (claim.getStatus() == AnalysisIdempotencyClaimStatus.MAX_RECOVERY_ATTEMPTS_EXCEEDED) {
            return AnalysisRunResult.maxAttempts(claim.getRun());
        }

        AnalysisRunDO run = claim.getRun();
        AnalysisExecutionContext context = contextFromRun(run);
        try {
            AssetAnalysisVO analysis = assemblerService.assemble(context);
            boolean failedRecovery = claim.getStatus() == AnalysisIdempotencyClaimStatus.RECOVERED_FAILED;
            boolean expiredLeaseRecovery = claim.getStatus() == AnalysisIdempotencyClaimStatus.RECOVERED_EXPIRED_LEASE;
            return AnalysisRunResult.executed(run, analysis, failedRecovery, expiredLeaseRecovery);
        } catch (Exception e) {
            String redactedMessage = redact(e.getMessage());
            AnalysisPersistenceFailure persistenceFailure = classifyPersistenceFailure(e);
            String failureCode = persistenceFailure != null
                    ? persistenceFailure.failureCode() : e.getClass().getSimpleName();
            log.warn("analysis run failed symbol={} analysisId={} traceId={} entity={} constraint={} failureCode={} reason={}",
                    run != null ? run.getSymbol() : null,
                    run != null ? run.getAnalysisId() : null,
                    run != null ? run.getTraceId() : null,
                    persistenceFailure != null ? persistenceFailure.entity() : "NOT_APPLICABLE",
                    persistenceFailure != null ? persistenceFailure.constraintName() : "NOT_APPLICABLE",
                    failureCode,
                    redactedMessage);
            if (run != null) {
                idempotencyGuard.markFailed(context, failureCode, redactedMessage);
            }
            return AnalysisRunResult.failed(run, redactedMessage);
        }
    }

    private NormalizedCommand normalize(AnalysisRunCommand command) {
        if (command == null) {
            throw new AnalysisRunInputException("ANALYSIS_COMMAND_REQUIRED", "analysis command is required");
        }
        String symbol = normalizeSymbol(command.getSymbol());
        String timeframe = AnalysisTimePolicy.requireSupportedTimeframe(command.getTimeframe());
        AnalysisRunTriggerType triggerType = command.getTriggerType() != null
                ? command.getTriggerType() : AnalysisRunTriggerType.MANUAL_API;
        LocalDateTime analysisTime = AnalysisTimePolicy.normalize(command.getAnalysisTime(), timeframe, clock);
        LocalDateTime canonicalBucket = AnalysisTimePolicy.canonicalBucket(analysisTime, timeframe);
        String requestId = RequestIdSupport.normalizeOrGenerate(command.getRequestId());
        String triggerReference = normalizeTriggerReference(triggerType, command.getTriggerReference(), requestId, canonicalBucket);
        String ruleVersion = ruleConfigService != null ? ruleConfigService.resolveActiveRuleVersion() : "v1.0";
        String ownerType = normalizeOwnerType(command.getOwnerType());
        Long ownerId = normalizeOwnerId(ownerType, command.getOwnerId());
        if (command.isPreview() && triggerType != AnalysisRunTriggerType.ANALYSIS_PREVIEW) {
            throw new AnalysisRunInputException("PREVIEW_TRIGGER_REQUIRED", "preview requires ANALYSIS_PREVIEW trigger");
        }
        return new NormalizedCommand(symbol, timeframe, triggerType, triggerReference, requestId, analysisTime,
                canonicalBucket, ruleVersion, safe(command.getParentAnalysisId()), safe(command.getParentTraceId()),
                ownerType, ownerId, command.getAssetId(), command.isPreview());
    }

    private static String normalizeTriggerReference(AnalysisRunTriggerType triggerType, String raw,
                                                    String requestId, LocalDateTime canonicalBucket) {
        String explicit = safe(raw);
        if (explicit != null) {
            return explicit;
        }
        if (triggerType == AnalysisRunTriggerType.SCHEDULED) {
            return "SCHEDULED:" + canonicalBucket;
        }
        return requestId;
    }

    static String normalizeSymbol(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AnalysisRunInputException("SYMBOL_REQUIRED", "symbol is required");
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    static String idempotencyKeyForTest(String symbol, String timeframe, LocalDateTime canonicalBucket, String ruleVersion) {
        return canonicalIdempotencyKey(normalizeSymbol(symbol), AnalysisTimePolicy.requireSupportedTimeframe(timeframe),
                canonicalBucket, ruleVersion);
    }

    private static String idempotencyKey(NormalizedCommand command) {
        return canonicalIdempotencyKey(command.symbol(), command.timeframe(), command.canonicalAnalysisTimeBucket(),
                command.ruleVersion(), command.ownerType(), command.ownerId(), command.preview());
    }

    private static String canonicalIdempotencyKey(String symbol, String timeframe,
                                                  LocalDateTime canonicalBucket, String ruleVersion) {
        if (canonicalBucket == null) {
            throw new AnalysisRunInputException("ANALYSIS_TIME_BUCKET_REQUIRED", "canonical analysis time bucket is required");
        }
        String version = ruleVersion == null || ruleVersion.isBlank() ? "v1.0" : ruleVersion.trim();
        return canonicalIdempotencyKey(symbol, timeframe, canonicalBucket, version, "SYSTEM", 0L, false);
    }

    private static String canonicalIdempotencyKey(String symbol, String timeframe,
                                                  LocalDateTime canonicalBucket, String ruleVersion,
                                                  String ownerType, Long ownerId, boolean preview) {
        if (canonicalBucket == null) {
            throw new AnalysisRunInputException("ANALYSIS_TIME_BUCKET_REQUIRED", "canonical analysis time bucket is required");
        }
        String version = ruleVersion == null || ruleVersion.isBlank() ? "v1.0" : ruleVersion.trim();
        return sha256(symbol + "|" + timeframe + "|" + canonicalBucket + "|" + version + "|"
                + ownerType + "|" + ownerId + "|preview=" + preview);
    }

    private static String inputSnapshotJson(NormalizedCommand command, String traceId, String idempotencyKey) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("version", "P2-3");
            snapshot.put("symbol", command.symbol());
            snapshot.put("timeframe", command.timeframe());
            snapshot.put("analysisTime", command.analysisTime().toString());
            snapshot.put("canonicalAnalysisTimeBucket", command.canonicalAnalysisTimeBucket().toString());
            snapshot.put("ruleVersion", command.ruleVersion());
            snapshot.put("triggerType", command.triggerType().name());
            snapshot.put("triggerReference", command.triggerReference());
            snapshot.put("requestId", command.requestId());
            snapshot.put("traceId", traceId);
            snapshot.put("parentAnalysisId", command.parentAnalysisId());
            snapshot.put("parentTraceId", command.parentTraceId());
            snapshot.put("idempotencyKey", idempotencyKey);
            snapshot.put("ownerType", command.ownerType());
            snapshot.put("ownerId", command.ownerId());
            snapshot.put("assetId", command.assetId());
            snapshot.put("preview", command.preview());
            snapshot.put("reviewOnly", true);
            snapshot.put("notAutoTrading", true);
            snapshot.put("notOrderExecution", true);
            snapshot.put("notUserPositionCreation", true);
            snapshot.put("notUserPositionMutation", true);
            snapshot.put("notPushSend", true);
            snapshot.put("notExternalChannel", true);
            return JSON.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "{\"version\":\"P2-3\",\"snapshot\":\"serialization_failed\"}";
        }
    }

    private static AnalysisExecutionContext contextFromRun(AnalysisRunDO run) {
        AnalysisRunTriggerType triggerType = AnalysisRunTriggerType.normalize(run.getTriggerType());
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
                triggerType,
                run.getTriggerReference(),
                run.getParentAnalysisId(),
                run.getParentTraceId(),
                run.getInputSnapshotJson(),
                run.getInputSnapshotHash(),
                run.getLeaseOwner(),
                run.getVersionNo(),
                run.getAttemptCount(),
                true,
                run.getOwnerType(), run.getOwnerId(), run.getAssetId(), Boolean.TRUE.equals(run.getPreview()));
    }

    static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((raw != null ? raw : "").getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String redact(String raw) {
        if (raw == null) {
            return null;
        }
        String t = AUTHORIZATION.matcher(raw).replaceAll("$1<redacted>");
        t = SECRET_PARAM.matcher(t).replaceAll("$1=<redacted>");
        t = URL_QUERY.matcher(t).replaceAll("https://$1?<redacted>");
        return SQL_VALUES.matcher(t).replaceAll("$1<redacted>$3");
    }

    static AnalysisPersistenceFailure classifyPersistenceFailure(Throwable error) {
        if (error == null) {
            return null;
        }
        String message = throwableMessages(error);
        ConstraintContext context = constraintContext(message);
        if (isDuplicateKey(error, message)) {
            String failureCode = switch (context.table() != null ? context.table() : "") {
                case "TM_ANALYSIS_RUN" -> "ANALYSIS_ID_COLLISION";
                case "TM_ANALYSIS_INPUT_SNAPSHOT" -> "SNAPSHOT_ID_COLLISION";
                case "TM_EVIDENCE_ITEM" -> "EVIDENCE_ID_COLLISION";
                case "TM_SCORE_ITEM" -> "SCORE_ID_COLLISION";
                case "TM_DECISION_RESULT" -> "DECISION_ID_COLLISION";
                default -> "PERSISTENCE_ID_COLLISION";
            };
            return persistenceFailure(failureCode, context);
        }

        if (isIntegrityConstraint(error)) {
            boolean decisionStateConstraint = "CK_TM_DECISION_DIRECTION_DATA_STATE".equals(context.constraint())
                    || ("TM_DECISION_RESULT".equals(context.table())
                    && message.toUpperCase(Locale.ROOT).contains("DIRECTION_DATA_STATE"));
            return persistenceFailure(decisionStateConstraint
                    ? "DECISION_STATE_CONSTRAINT_VIOLATION"
                    : "PERSISTENCE_CONSTRAINT_VIOLATION", context);
        }

        if (isPersistenceError(error)) {
            return persistenceFailure("PERSISTENCE_FAILURE", context);
        }
        return null;
    }

    private static boolean isDuplicateKey(Throwable error, String message) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof DuplicateKeyException) {
                return true;
            }
            if (current instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
        }
        String upper = message.toUpperCase(Locale.ROOT);
        return upper.contains("DUPLICATE KEY")
                || upper.contains("UNIQUE INDEX OR PRIMARY KEY VIOLATION")
                || upper.contains("VIOLATES UNIQUE CONSTRAINT");
    }

    private static boolean isIntegrityConstraint(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof DataIntegrityViolationException
                    || current.getClass().getSimpleName().contains("IntegrityConstraintViolation")) {
                return true;
            }
            if (current instanceof SQLException sql
                    && sql.getSQLState() != null && sql.getSQLState().startsWith("23")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPersistenceError(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof DataAccessException || current instanceof SQLException) {
                return true;
            }
        }
        return false;
    }

    private static ConstraintContext constraintContext(String message) {
        Matcher h2Unique = H2_UNIQUE_CONSTRAINT.matcher(message);
        if (h2Unique.find()) {
            return new ConstraintContext(
                    h2Unique.group(2).toUpperCase(Locale.ROOT),
                    h2Unique.group(1).toUpperCase(Locale.ROOT));
        }
        Matcher namedUnique = NAMED_UNIQUE_CONSTRAINT.matcher(message);
        String constraint = namedUnique.find() ? namedUnique.group(1).toUpperCase(Locale.ROOT) : null;
        Matcher namedCheck = NAMED_CHECK_CONSTRAINT.matcher(message);
        if (constraint == null && namedCheck.find()) {
            constraint = namedCheck.group(1).toUpperCase(Locale.ROOT);
        }
        String table = knownTable(message);
        if (table == null && constraint != null && constraint.startsWith("CK_TM_DECISION_")) {
            table = "TM_DECISION_RESULT";
        }
        return new ConstraintContext(table, constraint);
    }

    private static AnalysisPersistenceFailure persistenceFailure(String failureCode, ConstraintContext context) {
        return new AnalysisPersistenceFailure(failureCode,
                context.table() != null ? context.table().toLowerCase(Locale.ROOT) : "unknown",
                context.constraint() != null ? context.constraint() : "UNKNOWN");
    }

    private static String throwableMessages(Throwable error) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current.getMessage() != null) {
                messages.append(' ').append(current.getMessage());
            }
        }
        return messages.toString();
    }

    private static String knownTable(String message) {
        String upper = message.toUpperCase(Locale.ROOT);
        for (String table : new String[]{"TM_ANALYSIS_RUN", "TM_ANALYSIS_INPUT_SNAPSHOT",
                "TM_EVIDENCE_ITEM", "TM_SCORE_ITEM", "TM_DECISION_RESULT"}) {
            if (upper.contains(table)) {
                return table;
            }
        }
        return null;
    }

    private record ConstraintContext(String table, String constraint) {
    }

    private static String safe(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeOwnerType(String raw) {
        String value = safe(raw);
        if (value == null) return "SYSTEM";
        String normalized = value.toUpperCase(Locale.ROOT);
        if (!"SYSTEM".equals(normalized) && !"USER".equals(normalized)) {
            throw new AnalysisRunInputException("ANALYSIS_OWNER_TYPE_INVALID", "ownerType must be SYSTEM or USER");
        }
        return normalized;
    }

    private static Long normalizeOwnerId(String ownerType, Long ownerId) {
        if ("SYSTEM".equals(ownerType)) return 0L;
        if (ownerId == null || ownerId <= 0) {
            throw new AnalysisRunInputException("ANALYSIS_OWNER_REQUIRED", "user-owned analysis requires ownerId");
        }
        return ownerId;
    }

    private record NormalizedCommand(String symbol, String timeframe, AnalysisRunTriggerType triggerType,
                                     String triggerReference, String requestId, LocalDateTime analysisTime,
                                     LocalDateTime canonicalAnalysisTimeBucket, String ruleVersion,
                                     String parentAnalysisId, String parentTraceId,
                                     String ownerType, Long ownerId, Long assetId, boolean preview) {
    }

    record AnalysisPersistenceFailure(String failureCode, String entity, String constraintName) {
    }
}
