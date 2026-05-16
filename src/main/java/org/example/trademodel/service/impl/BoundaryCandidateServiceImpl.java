package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.BoundaryCandidateDTO;
import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.BoundarySourceFieldsDTO;
import org.example.trademodel.dto.planboundary.BoundaryStatusEnum;
import org.example.trademodel.dto.planboundary.BoundaryStopDTO;
import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.BoundaryCandidateService;
import org.example.trademodel.service.SourceAssembler;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BoundaryCandidateServiceImpl implements BoundaryCandidateService {

    private final SourceAssembler sourceAssembler;

    public BoundaryCandidateServiceImpl() {
        this(new DefaultSourceAssembler());
    }

    public BoundaryCandidateServiceImpl(SourceAssembler sourceAssembler) {
        this.sourceAssembler = sourceAssembler;
    }

    @Override
    public BoundaryCandidateDTO evaluateBoundaryCandidate(
            String symbol,
            String timeframe,
            SourceTraceDTO sourceTrace,
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal dataQualityScore
    ) {
        return evaluateBoundaryCandidate(
                symbol,
                timeframe,
                sourceTrace,
                entry,
                stop,
                takeProfitLevels,
                sourceFields,
                dataQualityScore,
                null
        );
    }

    @Override
    public BoundaryCandidateDTO evaluateBoundaryCandidate(
            String symbol,
            String timeframe,
            SourceTraceDTO sourceTrace,
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal dataQualityScore,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        List<String> blockingReasons = new ArrayList<>();

        addCandidateShapeBlockingReasons(
                symbol,
                timeframe,
                entry,
                stop,
                takeProfitLevels,
                sourceFields,
                dataQualityScore,
                blockingReasons
        );
        addSourceTraceBlockingReasons(sourceTrace, blockingReasons);
        addBoundarySourceBlockingReasons(entry, stop, takeProfitLevels, sourceFields, blockingReasons);
        addRiskActionGuardBlockingReasons(riskActionGuardDisplay, blockingReasons);

        if (!blockingReasons.isEmpty()) {
            BoundaryStatusEnum fallbackStatus = resolveFallbackStatus(sourceTrace, riskActionGuardDisplay);
            return fallbackCandidate(symbol, timeframe, fallbackStatus, blockingReasons, sourceFields, dataQualityScore);
        }

        BoundaryCandidateDTO candidate = BoundaryCandidateDTO.valid(
                symbol,
                timeframe,
                entry,
                stop,
                takeProfitLevels,
                sourceFields,
                dataQualityScore
        );
        candidate.setManualReviewRequired(true);
        candidate.setNotTradeInstruction(true);
        return candidate;
    }

    @Override
    public BoundaryCandidateDTO evaluateBoundaryCandidate(
            String symbol,
            String timeframe,
            RuntimeKlineContextDTO runtimeKlineContext,
            DerivativesRiskContextDTO derivativesRiskContext,
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal dataQualityScore
    ) {
        SourceTraceDTO sourceTrace = sourceAssembler.assembleSourceTrace(runtimeKlineContext, derivativesRiskContext);
        return evaluateBoundaryCandidate(
                symbol,
                timeframe,
                sourceTrace,
                entry,
                stop,
                takeProfitLevels,
                sourceFields,
                dataQualityScore
        );
    }

    private void addCandidateShapeBlockingReasons(
            String symbol,
            String timeframe,
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal dataQualityScore,
            List<String> blockingReasons
    ) {
        addWhenBlank(symbol, "symbol missing", blockingReasons);
        addWhenBlank(timeframe, "timeframe missing", blockingReasons);
        addWhenNull(entry, "entry missing", blockingReasons);
        addWhenNull(stop, "stop missing", blockingReasons);
        if (takeProfitLevels == null || takeProfitLevels.isEmpty()) {
            blockingReasons.add("takeProfitLevels missing");
        }
        addWhenNull(sourceFields, "sourceFields missing", blockingReasons);
        addWhenNull(dataQualityScore, "dataQualityScore missing", blockingReasons);
    }

    private void addSourceTraceBlockingReasons(SourceTraceDTO sourceTrace, List<String> blockingReasons) {
        if (sourceTrace == null) {
            blockingReasons.add("sourceTrace missing");
            return;
        }
        if (sourceTrace.getFallbackStatus() != null) {
            blockingReasons.add("sourceTrace fallbackStatus=" + sourceTrace.getFallbackStatus().name());
        }
        if (sourceTrace.getMissingFields() != null && !sourceTrace.getMissingFields().isEmpty()) {
            blockingReasons.add("sourceTrace missingFields present");
        }
        addWhenNull(sourceTrace.getEntryPriceSource(), "entry source missing", blockingReasons);
        addWhenBlank(sourceTrace.getEntrySourceType(), "entry source type missing", blockingReasons);
        addWhenBlank(sourceTrace.getEntrySourceTimeframe(), "entry source timeframe missing", blockingReasons);
        addWhenBlank(sourceTrace.getEntrySourceReason(), "entry source reason missing", blockingReasons);
        addWhenBlank(sourceTrace.getEntrySourceRef(), "entry source ref missing", blockingReasons);
        addWhenNull(sourceTrace.getStopPriceSource(), "stop source missing", blockingReasons);
        addWhenBlank(sourceTrace.getStopSourceType(), "stop source type missing", blockingReasons);
        addWhenBlank(sourceTrace.getStopSourceTimeframe(), "stop source timeframe missing", blockingReasons);
        addWhenBlank(sourceTrace.getStopSourceReason(), "stop source reason missing", blockingReasons);
        addWhenBlank(sourceTrace.getStopSourceRef(), "stop source ref missing", blockingReasons);
        if (sourceTrace.getTpPriceSources() == null || sourceTrace.getTpPriceSources().isEmpty()) {
            blockingReasons.add("TP source missing");
        }
        addWhenBlank(sourceTrace.getTpSourceType(), "TP source type missing", blockingReasons);
        addWhenBlank(sourceTrace.getTpSourceTimeframe(), "TP source timeframe missing", blockingReasons);
        addWhenBlank(sourceTrace.getTpSourceReason(), "TP source reason missing", blockingReasons);
        addWhenBlank(sourceTrace.getTpSourceRef(), "TP source ref missing", blockingReasons);
        addWhenNull(sourceTrace.getRrSource(), "RR source missing", blockingReasons);
        addWhenBlank(sourceTrace.getRrRuleRef(), "RR rule source missing", blockingReasons);
        addWhenBlank(sourceTrace.getLiquiditySource(), "liquidity source missing", blockingReasons);
        addWhenBlank(sourceTrace.getMultiTimeframeSource(), "multi-timeframe source missing", blockingReasons);
        addWhenBlank(sourceTrace.getEventSource(), "event window source missing", blockingReasons);
        addWhenBlank(sourceTrace.getWickSource(), "wick confirmation source missing", blockingReasons);
    }

    private void addBoundarySourceBlockingReasons(
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            List<String> blockingReasons
    ) {
        if (entry != null) {
            addWhenNull(entry.getEntryPrice(), "entry price missing", blockingReasons);
            addWhenBlank(entry.getNumericSourceType(), "entry numeric source type missing", blockingReasons);
            addWhenNull(entry.getNumericSourceValue(), "entry numeric source value missing", blockingReasons);
            addWhenBlank(entry.getSourceTimeframe(), "entry source timeframe missing", blockingReasons);
            addWhenBlank(entry.getReason(), "entry source reason missing", blockingReasons);
        }
        if (stop != null) {
            addWhenNull(stop.getStopPrice(), "stop price missing", blockingReasons);
            addWhenBlank(stop.getNumericSourceType(), "stop numeric source type missing", blockingReasons);
            addWhenNull(stop.getNumericSourceValue(), "stop numeric source value missing", blockingReasons);
            addWhenBlank(stop.getSourceTimeframe(), "stop source timeframe missing", blockingReasons);
            addWhenBlank(stop.getReason(), "stop source reason missing", blockingReasons);
        }
        if (takeProfitLevels != null) {
            for (int i = 0; i < takeProfitLevels.size(); i++) {
                BoundaryTakeProfitLevelDTO takeProfit = takeProfitLevels.get(i);
                if (takeProfit == null) {
                    blockingReasons.add("TP level " + i + " missing");
                    continue;
                }
                addWhenNull(takeProfit.getPrice(), "TP price missing", blockingReasons);
                addWhenNull(takeProfit.getRr(), "TP RR missing", blockingReasons);
                addWhenBlank(takeProfit.getNumericSourceType(), "TP numeric source type missing", blockingReasons);
                addWhenNull(takeProfit.getNumericSourceValue(), "TP numeric source value missing", blockingReasons);
                addWhenBlank(takeProfit.getSourceTimeframe(), "TP source timeframe missing", blockingReasons);
                addWhenBlank(takeProfit.getSourceRef(), "TP source ref missing", blockingReasons);
            }
        }
        if (sourceFields != null) {
            addWhenBlank(sourceFields.getEntrySourceField(), "entry source field missing", blockingReasons);
            addWhenBlank(sourceFields.getStopSourceField(), "stop source field missing", blockingReasons);
            addWhenBlank(sourceFields.getTakeProfitSourceField(), "TP source field missing", blockingReasons);
            addWhenBlank(sourceFields.getRrRule(), "RR rule field missing", blockingReasons);
            addWhenBlank(sourceFields.getDataSource(), "data source missing", blockingReasons);
        }
    }

    private void addRiskActionGuardBlockingReasons(
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay,
            List<String> blockingReasons
    ) {
        if (riskActionGuardDisplay == null) {
            return;
        }
        if (isBlank(riskActionGuardDisplay.getRiskActionGuardStatus())
                || "BACKEND_PENDING".equalsIgnoreCase(riskActionGuardDisplay.getRiskActionGuardStatus())) {
            blockingReasons.add("riskActionGuard backend pending");
        }
        if (isBlank(riskActionGuardDisplay.getLiquidityState())
                || "BACKEND_PENDING".equalsIgnoreCase(riskActionGuardDisplay.getLiquidityState())) {
            blockingReasons.add("liquidity source missing");
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getStampedeDetected())) {
            blockingReasons.add("stampede risk detected");
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getWickOnlyRisk())) {
            blockingReasons.add("wick-only risk detected");
        }
        String blockingReason = riskActionGuardDisplay.getRiskActionBlockingReason();
        if (!isBlank(blockingReason) && !"MANUAL_REVIEW_REQUIRED".equalsIgnoreCase(blockingReason)) {
            blockingReasons.add("riskActionGuard blocked:" + blockingReason);
        }
    }

    private BoundaryStatusEnum resolveFallbackStatus(
            SourceTraceDTO sourceTrace,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        if (riskActionGuardDisplay != null
                && (Boolean.TRUE.equals(riskActionGuardDisplay.getStampedeDetected())
                || Boolean.TRUE.equals(riskActionGuardDisplay.getWickOnlyRisk())
                || (!isBlank(riskActionGuardDisplay.getRiskActionBlockingReason())
                && !"MANUAL_REVIEW_REQUIRED".equalsIgnoreCase(riskActionGuardDisplay.getRiskActionBlockingReason())))) {
            return BoundaryStatusEnum.WATCH_ONLY;
        }
        if (sourceTrace == null) {
            return BoundaryStatusEnum.INCOMPLETE;
        }
        SourceTraceFallbackStatusEnum fallbackStatus = sourceTrace.getFallbackStatus();
        if (fallbackStatus == SourceTraceFallbackStatusEnum.WATCH_ONLY
                || fallbackStatus == SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY) {
            return BoundaryStatusEnum.WATCH_ONLY;
        }
        return BoundaryStatusEnum.INCOMPLETE;
    }

    private BoundaryCandidateDTO fallbackCandidate(
            String symbol,
            String timeframe,
            BoundaryStatusEnum boundaryStatus,
            List<String> blockingReasons,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal dataQualityScore
    ) {
        BoundaryCandidateDTO candidate = new BoundaryCandidateDTO();
        candidate.setSymbol(symbol);
        candidate.setTimeframe(timeframe);
        candidate.setBoundaryStatus(boundaryStatus);
        candidate.setSourceFields(sourceFields);
        candidate.setDataQualityScore(dataQualityScore);
        candidate.setManualReviewRequired(true);
        candidate.setNotTradeInstruction(true);
        candidate.setBlockingReasons(blockingReasons);
        return candidate;
    }

    private void addWhenNull(Object value, String reason, List<String> blockingReasons) {
        if (value == null) {
            blockingReasons.add(reason);
        }
    }

    private void addWhenBlank(String value, String reason, List<String> blockingReasons) {
        if (value == null || value.trim().isEmpty()) {
            blockingReasons.add(reason);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
