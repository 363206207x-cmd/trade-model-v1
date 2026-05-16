package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineServiceSourceTraceTest {

    @Test
    void executeWithMissingSourceTraceFailsClosed() {
        RuleEngineService service = executableRuleEngine();

        RuleBaseOutput output = service.execute(new DecisionContext(), null);

        assertThat(output.isCanExecute()).isFalse();
        assertThat(output.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(output.getConfidenceLevel()).isEqualTo(ExecutionPlanVO.READINESS_INCOMPLETE);
        assertThat(output.getRiskLevel()).isEqualTo("SOURCE_TRACE_INCOMPLETE");
    }

    @Test
    void executeWithWatchOnlySourceTraceFailsClosed() {
        RuleEngineService service = executableRuleEngine();
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setEventSource(null);
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        sourceTrace.setMissingFields(List.of("eventSource"));

        RuleBaseOutput output = service.execute(new DecisionContext(), sourceTrace);

        assertThat(output.isCanExecute()).isFalse();
        assertThat(output.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(output.getConfidenceLevel()).isEqualTo(ExecutionPlanVO.READINESS_WATCH_ONLY);
        assertThat(output.getRiskLevel()).isEqualTo("SOURCE_TRACE_WATCH_ONLY");
    }

    @Test
    void executeWithCompleteSourceTraceStillReturnsReviewOnlyNoExecutionGate() {
        RuleEngineService service = executableRuleEngine();

        RuleBaseOutput output = service.execute(new DecisionContext(), validSourceTrace());

        assertThat(output.isCanExecute()).isFalse();
        assertThat(output.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(output.getMarketBias()).isEqualTo("BULLISH");
    }

    private RuleEngineService executableRuleEngine() {
        return ctx -> {
            RuleBaseOutput output = new RuleBaseOutput();
            output.setMarketBias("BULLISH");
            output.setConfidenceLevel("HIGH");
            output.setRiskLevel("MEDIUM");
            output.setPlanMode(ExecutionPlanVO.PLAN_MODE_SEMI_STRUCTURED);
            output.setCanExecute(true);
            return output;
        };
    }

    private SourceTraceDTO validSourceTrace() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setEntryPriceSource(BigDecimal.valueOf(68000));
        sourceTrace.setEntrySourceType("support");
        sourceTrace.setEntrySourceTimeframe("1h");
        sourceTrace.setEntrySourceReason("support retest");
        sourceTrace.setEntrySourceRef("entry-1");
        sourceTrace.setStopPriceSource(BigDecimal.valueOf(66800));
        sourceTrace.setStopSourceType("swing_low");
        sourceTrace.setStopSourceTimeframe("1h");
        sourceTrace.setStopSourceReason("recent swing low");
        sourceTrace.setStopSourceRef("stop-1");
        sourceTrace.setTpPriceSources(List.of(BigDecimal.valueOf(70400)));
        sourceTrace.setTpSourceType("rr_ladder");
        sourceTrace.setTpSourceTimeframe("1h");
        sourceTrace.setTpSourceReason("2R target");
        sourceTrace.setTpSourceRef("tp-1");
        sourceTrace.setRrSource(BigDecimal.valueOf(2));
        sourceTrace.setRrRuleRef("min_rr_2");
        sourceTrace.setLiquiditySource("liquidity-ok");
        sourceTrace.setMultiTimeframeSource("multi-timeframe-aligned");
        sourceTrace.setEventSource("no-event-window");
        sourceTrace.setWickSource("wick-confirmed");
        return sourceTrace;
    }
}
