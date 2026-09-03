package org.example.trademodel.analysistrace;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisTraceServiceImplTest {
    @Test
    void traceSnapshotAggregatesRunEvidenceScoreDecisionPlanAiAndOpportunityIds() {
        AnalysisRunMapper mapper = mock(AnalysisRunMapper.class);
        AnalysisRunDO run = run("ana-1", "trace-1", "req-1", "SUCCESS");
        run.setIdempotencyKey("idem-1");
        run.setSymbol("BTCUSDT");
        run.setTimeframe("1m");
        run.setTriggerType("MANUAL_API");
        run.setInputSnapshotHash("hash-1");
        when(mapper.selectByTraceId("trace-1")).thenReturn(run);
        when(mapper.selectByRequestId("req-1")).thenReturn(run);
        when(mapper.selectEvidenceIdsByAnalysisId("ana-1")).thenReturn(List.of("ev-1"));
        when(mapper.selectScoreIdsByAnalysisId("ana-1")).thenReturn(List.of("sc-1"));
        when(mapper.selectDecisionIdsByAnalysisId("ana-1")).thenReturn(List.of("dec-1"));
        when(mapper.selectExecutionPlanIdsByAnalysisId("ana-1")).thenReturn(List.of("plan-1"));
        when(mapper.selectPositionMonitorLogIdsByAnalysisId("ana-1")).thenReturn(List.of("1"));
        when(mapper.selectReviewResultIdsByAnalysisId("ana-1")).thenReturn(List.of("rev-1"));
        when(mapper.selectAiCallIdsByTraceOrAnalysisId("trace-1", "ana-1")).thenReturn(List.of("ai-1"));
        when(mapper.selectOpportunityIdsByAnalysisId("ana-1")).thenReturn(List.of("opp-1"));
        when(mapper.countPushSnapshotsByAnalysisId("ana-1")).thenReturn(1);

        AnalysisTraceSnapshot snapshot = new AnalysisTraceServiceImpl(mapper).byTraceId("trace-1");

        assertThat(snapshot.getAnalysisId()).isEqualTo("ana-1");
        assertThat(snapshot.getEvidenceIds()).containsExactly("ev-1");
        assertThat(snapshot.getScoreIds()).containsExactly("sc-1");
        assertThat(snapshot.getDecisionIds()).containsExactly("dec-1");
        assertThat(snapshot.getExecutionPlanIds()).containsExactly("plan-1");
        assertThat(snapshot.getAiCallIds()).containsExactly("ai-1");
        assertThat(snapshot.getOpportunityIds()).containsExactly("opp-1");
        assertThat(snapshot.getPushSnapshotCount()).isEqualTo(1);
        assertThat(snapshot.getTraceStatus()).isEqualTo("COMPLETE");
        assertThat(snapshot.getMissingSegments()).isEmpty();
        assertThat(snapshot.getGeneratedAt()).isNotNull();
        assertThat(snapshot.isManualReviewOnly()).isTrue();
        assertThat(new AnalysisTraceServiceImpl(mapper).byRequestId("req-1").getTraceId()).isEqualTo("trace-1");
        assertThat(snapshot.isNotUserPositionMutation()).isTrue();
        assertThat(snapshot.isNotAutoTrading()).isTrue();
    }

    @Test
    void successfulTraceWithMissingRequiredSegmentsIsPartialTrace() {
        AnalysisRunMapper mapper = mock(AnalysisRunMapper.class);
        AnalysisRunDO run = run("ana-partial", "trace-partial", "req-partial", "SUCCESS");
        when(mapper.selectById("ana-partial")).thenReturn(run);

        AnalysisTraceSnapshot snapshot = new AnalysisTraceServiceImpl(mapper).byAnalysisId("ana-partial");

        assertThat(snapshot.getTraceStatus()).isEqualTo("PARTIAL_TRACE");
        assertThat(snapshot.getMissingSegments()).containsExactly("evidence", "score", "decision", "executionPlan");
    }

    @Test
    void startedAndFailedRunsExposeRunningAndFailedTraceStatus() {
        AnalysisRunMapper mapper = mock(AnalysisRunMapper.class);
        AnalysisRunDO running = run("ana-running", "trace-running", "req-running", "STARTED");
        running.setStartedAt(LocalDateTime.of(2026, 9, 3, 12, 0));
        running.setUpdatedAt(LocalDateTime.of(2026, 9, 3, 12, 1));
        AnalysisRunDO failed = run("ana-failed", "trace-failed", "req-failed", "FAILED");
        failed.setErrorCode("AUTHORITATIVE_OHLCV_UNAVAILABLE");
        failed.setCompletedAt(LocalDateTime.of(2026, 9, 3, 12, 2));
        when(mapper.selectById("ana-running")).thenReturn(running);
        when(mapper.selectById("ana-failed")).thenReturn(failed);

        AnalysisTraceServiceImpl service = new AnalysisTraceServiceImpl(mapper);

        assertThat(service.byAnalysisId("ana-running").getTraceStatus()).isEqualTo("RUNNING");
        assertThat(service.byAnalysisId("ana-running").getStartedAt()).isEqualTo(running.getStartedAt());
        assertThat(service.byAnalysisId("ana-running").getUpdatedAt()).isEqualTo(running.getUpdatedAt());
        assertThat(service.byAnalysisId("ana-failed").getTraceStatus()).isEqualTo("FAILED");
        assertThat(service.byAnalysisId("ana-failed").getErrorCode())
                .isEqualTo("AUTHORITATIVE_OHLCV_UNAVAILABLE");
        assertThat(service.byAnalysisId("ana-failed").getCompletedAt()).isEqualTo(failed.getCompletedAt());
    }

    @Test
    void userScopedTraceQueriesUseOwnershipFilteredMapperPaths() {
        AnalysisRunMapper mapper = mock(AnalysisRunMapper.class);
        AnalysisRunDO owned = run("ana-owned", "trace-owned", "req-owned", "SUCCESS");
        when(mapper.selectReadableByUser("ana-owned", 12L)).thenReturn(owned);
        when(mapper.selectReadableByTraceId("trace-owned", 12L)).thenReturn(owned);
        when(mapper.selectReadableByRequestId("req-owned", 12L)).thenReturn(owned);
        AnalysisTraceServiceImpl service = new AnalysisTraceServiceImpl(mapper);

        assertThat(service.byAnalysisIdForUser("ana-owned", 12L).getAnalysisId()).isEqualTo("ana-owned");
        assertThat(service.byTraceIdForUser("trace-owned", 12L).getTraceId()).isEqualTo("trace-owned");
        assertThat(service.byRequestIdForUser("req-owned", 12L).getRequestId()).isEqualTo("req-owned");
        assertThat(service.byAnalysisIdForUser("ana-owned", null)).isNull();
    }

    private static AnalysisRunDO run(String analysisId, String traceId, String requestId, String status) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setTraceId(traceId);
        run.setRequestId(requestId);
        run.setStatus(status);
        return run;
    }
}
