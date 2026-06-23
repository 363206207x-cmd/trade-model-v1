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
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("ana-1");
        run.setTraceId("trace-1");
        run.setRequestId("req-1");
        run.setIdempotencyKey("idem-1");
        run.setSymbol("BTCUSDT");
        run.setTimeframe("1m");
        run.setStatus("SUCCESS");
        run.setTriggerType("MANUAL_API");
        run.setInputSnapshotHash("hash-1");
        when(mapper.selectByTraceId("trace-1")).thenReturn(run);
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
        assertThat(snapshot.isNotUserPositionMutation()).isTrue();
        assertThat(snapshot.isNotAutoTrading()).isTrue();
    }
}
