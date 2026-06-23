package org.example.trademodel.analysisrun;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.example.trademodel.service.AnalysisAssemblerService;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AnalysisRunOrchestratorImpl implements AnalysisRunOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(AnalysisRunOrchestratorImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern AUTHORIZATION = Pattern.compile("(?i)(authorization\\s*[:=]\\s*)([^,;]+)");
    private static final Pattern SECRET_PARAM = Pattern.compile("(?i)(api[_-]?key|token|access[_-]?token|secret)=([^&\\s]+)");
    private static final Pattern URL_QUERY = Pattern.compile("https?://([^\\s?]+)\\?[^\\s]+", Pattern.CASE_INSENSITIVE);

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
                LocalDateTime.now(clock).plusSeconds(properties.getIdempotency().getLeaseSeconds()));

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
            log.warn("analysis run failed analysisId={} traceId={} reason={}",
                    run != null ? run.getAnalysisId() : null,
                    run != null ? run.getTraceId() : null,
                    redactedMessage);
            if (run != null) {
                idempotencyGuard.markFailed(context, e.getClass().getSimpleName(), redactedMessage);
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
        return new NormalizedCommand(symbol, timeframe, triggerType, triggerReference, requestId, analysisTime,
                canonicalBucket, ruleVersion, safe(command.getParentAnalysisId()), safe(command.getParentTraceId()));
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
                command.ruleVersion());
    }

    private static String canonicalIdempotencyKey(String symbol, String timeframe,
                                                  LocalDateTime canonicalBucket, String ruleVersion) {
        if (canonicalBucket == null) {
            throw new AnalysisRunInputException("ANALYSIS_TIME_BUCKET_REQUIRED", "canonical analysis time bucket is required");
        }
        String version = ruleVersion == null || ruleVersion.isBlank() ? "v1.0" : ruleVersion.trim();
        return sha256(symbol + "|" + timeframe + "|" + canonicalBucket + "|" + version);
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
                true);
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
        return URL_QUERY.matcher(t).replaceAll("https://$1?<redacted>");
    }

    private static String safe(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    private record NormalizedCommand(String symbol, String timeframe, AnalysisRunTriggerType triggerType,
                                     String triggerReference, String requestId, LocalDateTime analysisTime,
                                     LocalDateTime canonicalAnalysisTimeBucket, String ruleVersion,
                                     String parentAnalysisId, String parentTraceId) {
    }
}
