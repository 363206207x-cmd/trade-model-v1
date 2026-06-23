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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AnalysisRunOrchestratorImpl implements AnalysisRunOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(AnalysisRunOrchestratorImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AnalysisIdempotencyGuard idempotencyGuard;
    private final AnalysisAssemblerService assemblerService;
    private final RuleConfigService ruleConfigService;
    private final AnalysisRunProperties properties;

    public AnalysisRunOrchestratorImpl(AnalysisIdempotencyGuard idempotencyGuard,
                                       AnalysisAssemblerService assemblerService,
                                       RuleConfigService ruleConfigService,
                                       AnalysisRunProperties properties) {
        this.idempotencyGuard = idempotencyGuard;
        this.assemblerService = assemblerService;
        this.ruleConfigService = ruleConfigService;
        this.properties = properties;
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
                LocalDateTime.now().plusSeconds(properties.getIdempotency().getLeaseSeconds()));

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
            log.warn("analysis run failed analysisId={} traceId={} reason={}",
                    run != null ? run.getAnalysisId() : null,
                    run != null ? run.getTraceId() : null,
                    e.getMessage());
            if (run != null) {
                idempotencyGuard.markFailed(run.getAnalysisId(), e.getClass().getSimpleName(), e.getMessage());
            }
            return AnalysisRunResult.failed(run, e.getMessage());
        }
    }

    private NormalizedCommand normalize(AnalysisRunCommand command) {
        if (command == null) {
            command = AnalysisRunCommand.manual("BTCUSDT", "1m", RequestIdSupport.generate(), null);
        }
        String symbol = normalizeSymbol(command.getSymbol());
        String timeframe = normalizeTimeframe(command.getTimeframe());
        AnalysisRunTriggerType triggerType = command.getTriggerType() != null
                ? command.getTriggerType() : AnalysisRunTriggerType.MANUAL_API;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime analysisTime = AnalysisTimePolicy.normalize(command.getAnalysisTime(), now);
        String requestId = RequestIdSupport.normalizeOrGenerate(command.getRequestId());
        String triggerReference = normalizeTriggerReference(triggerType, command.getTriggerReference(), requestId, analysisTime);
        String ruleVersion = ruleConfigService != null ? ruleConfigService.resolveActiveRuleVersion() : "v1.0";
        return new NormalizedCommand(symbol, timeframe, triggerType, triggerReference, requestId, analysisTime,
                ruleVersion, safe(command.getParentAnalysisId()), safe(command.getParentTraceId()));
    }

    private static String normalizeTriggerReference(AnalysisRunTriggerType triggerType, String raw,
                                                    String requestId, LocalDateTime analysisTime) {
        String explicit = safe(raw);
        if (explicit != null) {
            return explicit;
        }
        if (triggerType == AnalysisRunTriggerType.SCHEDULED) {
            return "SCHEDULED:" + AnalysisTimePolicy.idempotencyBucket(analysisTime);
        }
        return requestId;
    }

    private static String normalizeSymbol(String raw) {
        String t = raw == null ? "BTCUSDT" : raw.trim().toUpperCase(Locale.ROOT);
        return t.isEmpty() ? "BTCUSDT" : t;
    }

    private static String normalizeTimeframe(String raw) {
        String t = raw == null ? "1m" : raw.trim();
        return t.isEmpty() ? "1m" : t;
    }

    private static String idempotencyKey(NormalizedCommand command) {
        LocalDateTime bucket = command.triggerType() == AnalysisRunTriggerType.MANUAL_API
                || command.triggerType() == AnalysisRunTriggerType.MARKET_DATA_COMPATIBILITY
                || command.triggerType() == AnalysisRunTriggerType.HOT_RESET_REBUILD
                ? command.analysisTime().truncatedTo(ChronoUnit.SECONDS)
                : AnalysisTimePolicy.idempotencyBucket(command.analysisTime());
        return sha256("P2-3|" + command.symbol() + "|" + command.timeframe() + "|"
                + command.triggerType().name() + "|" + command.triggerReference() + "|"
                + command.ruleVersion() + "|" + bucket);
    }

    private static String inputSnapshotJson(NormalizedCommand command, String traceId, String idempotencyKey) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("version", "P2-3");
            snapshot.put("symbol", command.symbol());
            snapshot.put("timeframe", command.timeframe());
            snapshot.put("triggerType", command.triggerType().name());
            snapshot.put("triggerReference", command.triggerReference());
            snapshot.put("requestId", command.requestId());
            snapshot.put("traceId", traceId);
            snapshot.put("ruleVersion", command.ruleVersion());
            snapshot.put("analysisTime", command.analysisTime().toString());
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
                run.getRuleVersion(),
                triggerType,
                run.getTriggerReference(),
                run.getParentAnalysisId(),
                run.getParentTraceId(),
                run.getInputSnapshotJson(),
                run.getInputSnapshotHash(),
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

    private static String safe(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    private record NormalizedCommand(String symbol, String timeframe, AnalysisRunTriggerType triggerType,
                                     String triggerReference, String requestId, LocalDateTime analysisTime,
                                     String ruleVersion, String parentAnalysisId, String parentTraceId) {
    }
}
