package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.vo.DecisionResultVO;

/**
 * Read-only SourceTrace / derivatives-risk detail assembly boundary for dashboard detail.
 * Implementations must not fetch external data, fabricate boundary prices, or emit trading actions.
 */
public interface DashboardSourceTraceDetailAdapter {

    DashboardSourceTraceDetailContext build(String symbol, DecisionResultVO decision);

    class DashboardSourceTraceDetailContext {
        private final SourceTraceDTO sourceTrace;
        private final RuntimeKlineContextDTO runtimeKlineContext;
        private final DerivativesRiskContextDTO derivativesRiskContext;

        public DashboardSourceTraceDetailContext(
                SourceTraceDTO sourceTrace,
                DerivativesRiskContextDTO derivativesRiskContext
        ) {
            this(sourceTrace, null, derivativesRiskContext);
        }

        public DashboardSourceTraceDetailContext(
                SourceTraceDTO sourceTrace,
                RuntimeKlineContextDTO runtimeKlineContext,
                DerivativesRiskContextDTO derivativesRiskContext
        ) {
            this.sourceTrace = sourceTrace;
            this.runtimeKlineContext = runtimeKlineContext;
            this.derivativesRiskContext = derivativesRiskContext;
        }

        public SourceTraceDTO getSourceTrace() {
            return sourceTrace;
        }

        public RuntimeKlineContextDTO getRuntimeKlineContext() {
            return runtimeKlineContext;
        }

        public DerivativesRiskContextDTO getDerivativesRiskContext() {
            return derivativesRiskContext;
        }
    }
}
