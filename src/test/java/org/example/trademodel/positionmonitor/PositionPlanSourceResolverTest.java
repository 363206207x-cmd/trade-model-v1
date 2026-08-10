package org.example.trademodel.positionmonitor;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogSourceViewPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class PositionPlanSourceResolverTest {
    @Mock
    private ExecutionPlanMapper executionPlanMapper;
    @Mock
    private AnalysisRunMapper analysisRunMapper;

    private PositionPlanSourceResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PositionPlanSourceResolver(executionPlanMapper, analysisRunMapper);
    }

    @Test
    void trustedExactPlanMonitorSourceCanBeMarkedVerified() {
        when(executionPlanMapper.selectByPlanId("plan-A")).thenReturn(plan("plan-A", "analysis-X"));
        when(analysisRunMapper.selectById("analysis-X")).thenReturn(run("analysis-X", "BTCUSDT"));

        PositionPlanSourceResolver.Resolution source = resolver.resolveTrustedMonitorSource(
                7L,
                "BTC/USDT",
                PositionMonitorSourceContract.executionPlanReference("plan-A"),
                "analysis-X",
                "plan-A");
        PositionMonitorLogDTO log = new PositionMonitorLogDTO();
        PositionMonitorLogSourceViewPolicy.markVerified(log, source.monitorAnalysisId(), source.executionPlanId());

        assertThat(source.verified()).isTrue();
        assertThat(log.isSourceVerified()).isTrue();
        assertThat(log.getSourceStatus()).isEqualTo("VERIFIED");
        assertThat(log.getAnalysisId()).isEqualTo("analysis-X");
        assertThat(log.getExecutionPlanId()).isEqualTo("plan-A");
    }

    @Test
    void monitorSourceMustMatchCurrentTypedPositionSource() {
        PositionPlanSourceResolver.Resolution wrongPlan = resolver.resolveTrustedMonitorSource(
                7L,
                "BTCUSDT",
                PositionMonitorSourceContract.executionPlanReference("plan-A"),
                "analysis-X",
                "plan-B");
        when(executionPlanMapper.selectByPlanId("plan-A")).thenReturn(plan("plan-A", "analysis-X"));
        when(analysisRunMapper.selectById("analysis-X")).thenReturn(run("analysis-X", "BTCUSDT"));
        PositionPlanSourceResolver.Resolution wrongAnalysis = resolver.resolveTrustedMonitorSource(
                7L,
                "BTCUSDT",
                PositionMonitorSourceContract.analysisReference("analysis-Z"),
                "analysis-Y",
                "plan-A");

        assertThat(wrongPlan.verified()).isFalse();
        assertThat(wrongPlan.failureReason()).isEqualTo("POSITION_MONITOR_PLAN_MISMATCH");
        assertThat(wrongAnalysis.verified()).isFalse();
        assertThat(wrongAnalysis.failureReason()).isEqualTo("POSITION_MONITOR_ANALYSIS_MISMATCH");
    }

    @Test
    void currentMonitorAnalysisCanDifferFromOriginalPlanAnalysis() {
        when(executionPlanMapper.selectByPlanId("plan-A")).thenReturn(plan("plan-A", "analysis-original"));
        when(analysisRunMapper.selectById("analysis-original"))
                .thenReturn(run("analysis-original", "BTCUSDT"));
        when(analysisRunMapper.selectById("analysis-current"))
                .thenReturn(run("analysis-current", "BTCUSDT"));

        PositionPlanSourceResolver.Resolution source = resolver.resolveTrustedMonitorSource(
                7L,
                "BTCUSDT",
                PositionMonitorSourceContract.executionPlanReference("plan-A"),
                "analysis-current",
                "plan-A");

        assertThat(source.verified()).isTrue();
        assertThat(source.analysisId()).isEqualTo("analysis-original");
        assertThat(source.monitorAnalysisId()).isEqualTo("analysis-current");
    }

    @Test
    void untypedLegacyPositionCannotPromoteOldMonitorIds() {
        PositionPlanSourceResolver.Resolution source = resolver.resolveTrustedMonitorSource(
                7L, "BTCUSDT", "legacy-untyped", "analysis-X", "plan-A");

        assertThat(source.verified()).isFalse();
        assertThat(source.failureReason()).isEqualTo("TYPED_SOURCE_REFERENCE_REQUIRED");
        verify(executionPlanMapper, never()).selectByPlanId(anyString());
        verify(analysisRunMapper, never()).selectById(anyString());
    }

    @Test
    void typedAnalysisRequiresMonitorExactPlanAndValidatesItAgainstAnalysis() {
        PositionPlanSourceResolver.Resolution missingPlan = resolver.resolveTrustedMonitorSource(
                7L,
                "BTCUSDT",
                PositionMonitorSourceContract.analysisReference("analysis-X"),
                "analysis-X",
                null);
        assertThat(missingPlan.verified()).isFalse();
        assertThat(missingPlan.failureReason()).isEqualTo("MONITOR_EXACT_SOURCE_REQUIRED");

        when(executionPlanMapper.selectByPlanId("plan-A")).thenReturn(plan("plan-A", "analysis-X"));
        when(analysisRunMapper.selectById("analysis-X")).thenReturn(run("analysis-X", "BTCUSDT"));
        PositionPlanSourceResolver.Resolution exact = resolver.resolveTrustedMonitorSource(
                7L,
                "BTCUSDT",
                PositionMonitorSourceContract.analysisReference("analysis-X"),
                "analysis-X",
                "plan-A");

        assertThat(exact.verified()).isTrue();
        assertThat(exact.executionPlanId()).isEqualTo("plan-A");
    }

    private static ExecutionPlanDO plan(String planId, String analysisId) {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId(planId);
        plan.setAnalysisId(analysisId);
        return plan;
    }

    private static AnalysisRunDO run(String analysisId, String symbol) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol(symbol);
        run.setTraceId("trace-" + analysisId);
        return run;
    }
}
