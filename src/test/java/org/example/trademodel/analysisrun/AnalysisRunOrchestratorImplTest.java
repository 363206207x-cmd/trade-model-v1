package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.service.AnalysisAssemblerService;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
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
    void schedulerCycleMayBeSharedButSixAnalysisIdsAndSnapshotsAreAssetScoped() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
        AnalysisRunOrchestratorImpl orchestrator = orchestrator(
                guard, new CapturingAssembler(false), "rules-2026-06");
        List<String> symbols = List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");

        symbols.forEach(symbol -> orchestrator.run(AnalysisRunCommand.scheduled(
                symbol, "5m", "req-" + symbol, "scheduler-cycle-shared")));

        assertThat(guard.requests).hasSize(6);
        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getAnalysisId).doesNotHaveDuplicates();
        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getTraceId).doesNotHaveDuplicates();
        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getIdempotencyKey).doesNotHaveDuplicates();
        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getTriggerReference)
                .containsOnly("scheduler-cycle-shared");
        assertThat(guard.requests).allSatisfy(request ->
                assertThat(request.getInputSnapshotJson()).contains("\"symbol\":\"" + request.getSymbol() + "\""));
    }

    @Test
    void oneMinuteBucketKeepsSameKeyAcrossDifferentSeconds() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
        AnalysisRunOrchestratorImpl orchestrator = orchestrator(guard, new CapturingAssembler(false), "rules-2026-06");

        orchestrator.run(AnalysisRunCommand.manual("BTCUSDT", "1m", "req-1", "2026-06-23T10:04:01Z"));
        orchestrator.run(AnalysisRunCommand.manual("BTCUSDT", "1m", "req-2", "2026-06-23T10:04:59Z"));

        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getIdempotencyKey)
                .containsOnly(guard.requests.get(0).getIdempotencyKey());
    }

    @Test
    void fiveMinuteBucketKeepsSameKeyAcrossDifferentMinutesInBucket() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
        AnalysisRunOrchestratorImpl orchestrator = orchestrator(guard, new CapturingAssembler(false), "rules-2026-06");

        orchestrator.run(AnalysisRunCommand.manual("BTCUSDT", "5m", "req-1", "2026-06-23T10:01:00Z"));
        orchestrator.run(AnalysisRunCommand.manual("BTCUSDT", "5m", "req-2", "2026-06-23T10:04:59Z"));

        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getIdempotencyKey)
                .containsOnly(guard.requests.get(0).getIdempotencyKey());
    }

    @Test
    void idempotencyKeyChangesAcrossCanonicalTimeBuckets() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
        AnalysisRunOrchestratorImpl orchestrator = orchestrator(guard, new CapturingAssembler(false), "rules-2026-06");

        orchestrator.run(AnalysisRunCommand.manual("BTCUSDT", "1m", "req-1", "2026-06-23T10:04:59Z"));
        orchestrator.run(AnalysisRunCommand.manual("BTCUSDT", "1m", "req-2", "2026-06-23T10:05:00Z"));

        assertThat(guard.requests).extracting(AnalysisRunClaimRequest::getIdempotencyKey)
                .doesNotHaveDuplicates();
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
    void duplicateSuccessNeverReentersAssemblerOrCreatesDownstreamState() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.DUPLICATE_SUCCESS);
        CapturingAssembler assembler = new CapturingAssembler(false);

        AnalysisRunResult result = orchestrator(guard, assembler, "rules-2026-06")
                .run(AnalysisRunCommand.manual("BNBUSDT", "5m", "req-duplicate", "2026-06-23T10:04:59Z"));

        assertThat(result.getStatus()).isEqualTo("EXISTING_SUCCESS");
        assertThat(result.isDuplicateTriggerBlocked()).isTrue();
        assertThat(assembler.calls).isZero();
        assertThat(guard.failedCode).isNull();
    }

    @Test
    void submitReturnsQueuedImmediatelyWithoutRunningAssemblerOnRequestThread() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.CLAIMED_NEW);
        CapturingAssembler assembler = new CapturingAssembler(false);
        AnalysisRunBackgroundWorker worker = mock(AnalysisRunBackgroundWorker.class);
        AnalysisRunOrchestratorImpl orchestrator = new AnalysisRunOrchestratorImpl(
                guard, assembler, ruleConfig("rules-2026-06"), new AnalysisRunProperties(),
                FIXED, mock(AnalysisRunMapper.class), worker);

        AnalysisRunResult result = orchestrator.submit(
                AnalysisRunCommand.manual("ADAUSDT", "5m", "req-background", "2026-06-23T10:04:59Z"));

        assertThat(result.getStatus()).isEqualTo("QUEUED");
        assertThat(result.getReasonCode()).isEqualTo("ANALYSIS_BACKGROUND_QUEUED");
        assertThat(result.hasAnalysisId()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(assembler.calls).isZero();
        verify(worker).submit(any(AnalysisRunDO.class), any(Runnable.class));
    }

    @Test
    void duplicateInProgressSubmissionDoesNotQueueSecondBackgroundExecution() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
        AnalysisRunBackgroundWorker worker = mock(AnalysisRunBackgroundWorker.class);
        AnalysisRunOrchestratorImpl orchestrator = new AnalysisRunOrchestratorImpl(
                guard, new CapturingAssembler(false), ruleConfig("rules-2026-06"),
                new AnalysisRunProperties(), FIXED, mock(AnalysisRunMapper.class), worker);

        AnalysisRunResult result = orchestrator.submit(
                AnalysisRunCommand.manual("ADAUSDT", "5m", "req-duplicate", "2026-06-23T10:04:59Z"));

        assertThat(result.getStatus()).isEqualTo("CONCURRENT_TRIGGER_BLOCKED");
        assertThat(result.isConcurrentTriggerBlocked()).isTrue();
        verify(worker, never()).submit(any(), any());
    }

    @Test
    void fullBackgroundQueueFailsClosedAndMarksRunFailed() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.CLAIMED_NEW);
        AnalysisRunBackgroundWorker worker = mock(AnalysisRunBackgroundWorker.class);
        doThrow(new RejectedExecutionException("full"))
                .when(worker).submit(any(AnalysisRunDO.class), any(Runnable.class));
        AnalysisRunOrchestratorImpl orchestrator = new AnalysisRunOrchestratorImpl(
                guard, new CapturingAssembler(false), ruleConfig("rules-2026-06"),
                new AnalysisRunProperties(), FIXED, mock(AnalysisRunMapper.class), worker);

        AnalysisRunResult result = orchestrator.submit(
                AnalysisRunCommand.manual("ADAUSDT", "5m", "req-full", "2026-06-23T10:04:59Z"));

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(guard.failedCode).isEqualTo("ANALYSIS_BACKGROUND_QUEUE_FULL");
        assertThat(guard.failedMessage).isEqualTo("analysis background queue is full");
    }

    @Test
    void applicationReadySchedulesEveryDurableRecoverableRunWithoutImmediateResubmit() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.IN_PROGRESS);
        AnalysisRunMapper mapper = mock(AnalysisRunMapper.class);
        AnalysisRunBackgroundWorker worker = mock(AnalysisRunBackgroundWorker.class);
        AnalysisRunDO first = new AnalysisRunDO();
        first.setAnalysisId("analysis-recover-1");
        AnalysisRunDO second = new AnalysisRunDO();
        second.setAnalysisId("analysis-recover-2");
        second.setLeaseExpiresAt(LocalDateTime.of(2026, 6, 23, 10, 5, 9));
        when(mapper.selectRecoverableBackgroundRuns(100)).thenReturn(List.of(first, second));
        AnalysisRunOrchestratorImpl orchestrator = new AnalysisRunOrchestratorImpl(
                guard, new CapturingAssembler(false), ruleConfig("rules-2026-06"),
                new AnalysisRunProperties(), FIXED, mapper, worker);

        orchestrator.resumeDurableBackgroundRuns();

        verify(worker).schedule(any(Runnable.class), org.mockito.ArgumentMatchers.eq(0L));
        verify(worker).schedule(any(Runnable.class), org.mockito.ArgumentMatchers.eq(10_100L));
        verify(worker, never()).submit(any(), any());
    }

    @Test
    void saturatedRecoveryQueueReschedulesBeforeClaimingDatabaseLease() {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.RECOVERED_EXPIRED_LEASE);
        AnalysisRunBackgroundWorker worker = mock(AnalysisRunBackgroundWorker.class);
        doThrow(new RejectedExecutionException("full"))
                .when(worker).submit(any(AnalysisRunDO.class), any(Runnable.class));
        AnalysisRunOrchestratorImpl orchestrator = new AnalysisRunOrchestratorImpl(
                guard, new CapturingAssembler(false), ruleConfig("rules-2026-06"),
                new AnalysisRunProperties(), FIXED, mock(AnalysisRunMapper.class), worker);
        AnalysisRunDO existing = new AnalysisRunDO();
        existing.setAnalysisId("analysis-recover-queue-full");

        ReflectionTestUtils.invokeMethod(orchestrator, "resumeAfterLease", existing);

        assertThat(guard.requests).isEmpty();
        verify(worker).schedule(any(Runnable.class), org.mockito.ArgumentMatchers.eq(1_000L));
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

    @Test
    void evidencePrimaryKeyCollisionUsesPreciseFailureCodeAndSanitizedDiagnostics(CapturedOutput output) {
        CapturingGuard guard = new CapturingGuard(AnalysisIdempotencyClaimStatus.CLAIMED_NEW);
        DuplicateKeyException collision = new DuplicateKeyException(
                "Unique index or primary key violation: \"PUBLIC.PRIMARY_KEY_4 ON "
                        + "PUBLIC.TM_EVIDENCE_ITEM(EVIDENCE_ID) VALUES ('secret-value')\"");

        AnalysisRunResult result = orchestrator(guard, new CapturingAssembler(collision), "rules-2026-06")
                .run(AnalysisRunCommand.manual("ETHUSDT", "5m", "req-collision", "2026-06-23T10:04:59Z"));

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(guard.failedCode).isEqualTo("EVIDENCE_ID_COLLISION");
        assertThat(output).contains("symbol=ETHUSDT")
                .contains("entity=tm_evidence_item")
                .contains("constraint=PRIMARY_KEY_4")
                .contains("failureCode=EVIDENCE_ID_COLLISION")
                .doesNotContain("secret-value");
    }

    @Test
    void nonDuplicateFailureKeepsExistingFailureClassification() {
        assertThat(AnalysisRunOrchestratorImpl.classifyPersistenceFailure(
                new IllegalStateException("REAL_MARKET_ENVIRONMENT_REQUIRED"))).isNull();
    }

    @Test
    void decisionDirectionStateCheckIsNotReportedAsIdCollision() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "Check constraint violation: \"CK_TM_DECISION_DIRECTION_DATA_STATE\"; "
                        + "table TM_DECISION_RESULT");

        var failure = AnalysisRunOrchestratorImpl
                .classifyPersistenceFailure(violation);

        assertThat(failure).isNotNull();
        assertThat(failure.failureCode()).isEqualTo("DECISION_STATE_CONSTRAINT_VIOLATION");
        assertThat(failure.entity()).isEqualTo("tm_decision_result");
        assertThat(failure.constraintName()).isEqualTo("CK_TM_DECISION_DIRECTION_DATA_STATE");
    }

    @Test
    void otherCheckConstraintHasGenericConstraintClassification() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "Check constraint violation: \"CK_TM_DECISION_MACHINE_SCORES\"; "
                        + "table TM_DECISION_RESULT");

        var failure = AnalysisRunOrchestratorImpl
                .classifyPersistenceFailure(violation);

        assertThat(failure).isNotNull();
        assertThat(failure.failureCode()).isEqualTo("PERSISTENCE_CONSTRAINT_VIOLATION");
        assertThat(failure.constraintName()).isEqualTo("CK_TM_DECISION_MACHINE_SCORES");
    }

    @Test
    void ordinaryDataAccessFailureHasPersistenceClassification() {
        var failure = AnalysisRunOrchestratorImpl.classifyPersistenceFailure(
                new DataAccessResourceFailureException("database unavailable"));

        assertThat(failure).isNotNull();
        assertThat(failure.failureCode()).isEqualTo("PERSISTENCE_FAILURE");
        assertThat(failure.entity()).isEqualTo("unknown");
        assertThat(failure.constraintName()).isEqualTo("UNKNOWN");
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
        private String failedCode;
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
            failedCode = errorCode;
            failedMessage = errorMessage;
        }
    }

    private static final class CapturingAssembler implements AnalysisAssemblerService {
        private final RuntimeException failure;
        private AnalysisExecutionContext context;
        private int calls;

        private CapturingAssembler(boolean fail) {
            this.failure = fail
                    ? new IllegalStateException("Authorization: Bearer SECRET https://api.example.test/path?api_key=SECRET&x=1")
                    : null;
        }

        private CapturingAssembler(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public AssetAnalysisVO assemble(String symbol, String timeframe) {
            throw new IllegalStateException("DIRECT_ASSEMBLER_ENTRY_DISABLED");
        }

        @Override
        public AssetAnalysisVO assemble(AnalysisExecutionContext context) {
            calls++;
            this.context = context;
            if (failure != null) {
                throw failure;
            }
            AssetAnalysisVO analysis = new AssetAnalysisVO();
            analysis.setAnalysisId(context.getAnalysisId());
            analysis.setSymbol(context.getSymbol());
            analysis.setTimeframe(context.getTimeframe());
            return analysis;
        }
    }
}
