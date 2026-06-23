package org.example.trademodel.analysistrace;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        when(mapper.selectById("ana-running")).thenReturn(run("ana-running", "trace-running", "req-running", "STARTED"));
        when(mapper.selectById("ana-failed")).thenReturn(run("ana-failed", "trace-failed", "req-failed", "FAILED"));

        AnalysisTraceServiceImpl service = new AnalysisTraceServiceImpl(mapper);

        assertThat(service.byAnalysisId("ana-running").getTraceStatus()).isEqualTo("RUNNING");
        assertThat(service.byAnalysisId("ana-failed").getTraceStatus()).isEqualTo("FAILED");
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
