package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.AnalysisAssemblerService;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisRunOrchestratorImplTest {
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-06-23T10:04:59Z"), ZoneOffset.UTC);

    @Test
    void canonicalIdempotencyKeyIgnoresTriggerRequestAndParentMetadata() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
        AnalysisRunOrchestratorImpl orchestrator = orchestrator(guard, new CapturingAssembler(false), "rules-2026-06");

        orchestrator.run(AnalysisRunCommand.manual("btcusdt", "5m", "req-manual", "2026-06-23T10:04:59Z"));
        orchestrator.run(AnalysisRunCommand.scheduled("BTCUSDT", "5m", "req-scheduled", "scheduler:other"));
        orchestrator.run(AnalysisRunCommand.hotResetRebuild("BTCUSDT", "5m", "hot-event-1", "req-hot", "parent-ana", "parent-trace"));

        assertThat(guard.requests).hasSize(3);
        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getIdempotencyKey).containsOnly(guard.requests.get(0).getIdempotencyKey());
        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getRequestId)
                .containsExactly("req-manual", "req-scheduled", "req-hot");
        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getTriggerType)
                .containsExactly(AnalysisRunTriggerType.MANUAL_API, AnalysisRunTriggerType.SCHEDULED, AnalysisRunTriggerType.HOT_RESET_REBUILD);
    }

    @Test
    void canonicalIdempotencyKeyChangesForTupleMembersOnly() {
        LocalDateTime bucket = LocalDateTime.of(2026, 6, 23, 10, 0);
        String base = AnalysisRunOrchestratorImpl.idempotencyKeyForTest("BTCUSDT", "5m", bucket, "rules-a");

        assertThat(AnalysisRunOrchestratorImpl.idempotencyKeyForTest("ETHUSDT", "5m", bucket, "rules-a"))
                .isNotEqualTo(base);
        assertThat(AnalysisRunOrchestratorImpl.idempotencyKeyForTest("BTCUSDT", "15m", bucket, "rules-a"))
                .isNotEqualTo(base);
        assertThat(AnalysisRunOrchestratorImpl.idempotencyKeyForTest("BTCUSDT", "5m", bucket.plusMinutes(5), "rules-a"))
                .isNotEqualTo(base);
        assertThat(AnalysisRunOrchestratorImpl.idempotencyKeyForTest("BTCUSDT", "5m", bucket, "rules-b"))
                .isNotEqualTo(base);
    }

    @Test
    void assemblerContextCarriesLeaseFenceFromClaimedRun() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.CLAIMED_NEW);
        CapturingAssembler assembler = new CapturingAssembler(false);
        AnalysisRunResult result = orchestrator(guard, assembler, "rules-2026-06")
                .run(AnalysisRunCommand.manual("BTCUSDT", "5m", "req-context", "2026-06-23T10:04:59Z"));

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(assembler.context.getLeaseOwner()).isEqualTo(guard.requests.get(0).getLeaseOwner());
        assertThat(assembler.context.getClaimVersion()).isEqualTo(1);
        assertThat(assembler.context.getAttemptCount()).isEqualTo(1);
        assertThat(assembler.context.getCanonicalAnalysisTimeBucket()).isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 0));
    }

    @Test
    void executionFailureRedactsSensitiveMessageBeforeAuditAndResponse() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.CLAIMED_NEW);
        AnalysisRunResult result = orchestrator(guard, new CapturingAssembler(true), "rules-2026-06")
                .run(AnalysisRunCommand.manual("BTCUSDT", "5m", "req-fail", "2026-06-23T10:04:59Z"));

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).contains("<redacted>");
        assertThat(result.getMessage()).doesNotContain("Bearer SECRET");
        assertThat(result.getMessage()).doesNotContain("api_key=SECRET");
        assertThat(guard.failedMessage).isEqualTo(result.getMessage());
    }

    private static AnalysisRunOrchestratorImpl orchestrator(CapturingGuard guard, CapturingAssembler assembler, String ruleVersion) {
        AnalysisRunProperties properties = new AnalysisRunProperties();
        return new AnalysisRunOrchestratorImpl(guard, assembler, ruleConfig(ruleVersion), properties, FIXED);
    }

    private static RuleConfigService ruleConfig(String version) {
        return new RuleConfigService() {
            @Override
            public Map<String, RuleConfigDO> getRuleConfigMap() {
                RuleConfigDO cfg = new RuleConfigDO();
                cfg.setRuleValue(version);
                return Map.of("rule.active_version_fallback", cfg);
            }

            @Override
            public void reloadRules() {
            }
        };
    }

    private static AnalysisRunDO runFrom(AnalysisRunClaimRequest request) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(request.getAnalysisId());
        run.setTraceId(request.getTraceId());
        run.setRequestId(request.getRequestId());
        run.setIdempotencyKey(request.getIdempotencyKey());
        run.setSymbol(request.getSymbol());
        run.setTimeframe(request.getTimeframe());
        run.setAnalysisTime(request.getAnalysisTime());
        run.setRuleVersion(request.getRuleVersion());
        run.setTriggerType(request.getTriggerType().name());
        run.setTriggerReference(request.getTriggerReference());
        run.setParentAnalysisId(request.getParentAnalysisId());
        run.setParentTraceId(request.getParentTraceId());
        run.setInputSnapshotJson(request.getInputSnapshotJson());
        run.setInputSnapshotHash(request.getInputSnapshotHash());
        run.setStatus("STARTED");
        run.setLeaseOwner(request.getLeaseOwner());
        run.setAttemptCount(1);
        run.setVersionNo(1);
        return run;
    }

    private static final class CapturingGuard implements AnalysisIdempotencyGuard {
        private final AnalysisIdempotencyClaimStatus status;
        private final List<AnalysisRunClaimRequest> requests = new ArrayList<>();
        private String failedMessage;

        private CapturingGuard(AnalysisIdempotencyClaimStatus status) {
            this.status = status;
        }

        @Override
        public AnalysisIdempotencyClaim claim(AnalysisRunClaimRequest request) {
            requests.add(request);
            return new AnalysisIdempotencyClaim(status, runFrom(request), status.name(), status.name());
        }

        @Override
        public void markFailed(AnalysisExecutionContext context, String errorCode, String errorMessage) {
            failedMessage = errorMessage;
        }
    }

    private static final class CapturingAssembler implements AnalysisAssemblerService {
        private final boolean fail;
        private AnalysisExecutionContext context;

        private CapturingAssembler(boolean fail) {
            this.fail = fail;
        }

        @Override
        public AssetAnalysisVO assemble(String symbol, String timeframe) {
            throw new IllegalStateException("DIRECT_ASSEMBLER_ENTRY_DISABLED");
        }

        @Override
        public AssetAnalysisVO assemble(AnalysisExecutionContext context) {
            this.context = context;
            if (fail) {
                throw new IllegalStateException("Authorization: Bearer SECRET https://api.example.test/path?api_key=SECRET&x=1");
            }
            AssetAnalysisVO analysis = new AssetAnalysisVO();
            analysis.setAnalysisId(context.getAnalysisId());
            analysis.setSymbol(context.getSymbol());
            analysis.setTimeframe(context.getTimeframe());
            return analysis;
        }
    }
}
