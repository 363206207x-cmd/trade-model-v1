package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.SourceAssembler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultSourceAssembler implements SourceAssembler {

    @Override
    public SourceTraceDTO assembleSourceTrace(
            RuntimeKlineContextDTO runtimeKlineContext,
            DerivativesRiskContextDTO derivativesRiskContext
    ) {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setManualReviewRequired(true);
        sourceTrace.setNotTradeInstruction(true);

        List<String> missingFields = new ArrayList<>();
        if (runtimeKlineContext == null) {
            missingFields.add("runtimeKlineContext");
            sourceTrace.setMissingFields(missingFields);
            sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
            return sourceTrace;
        }

        copyRuntimeSources(runtimeKlineContext, sourceTrace);
        collectRuntimeMissingFields(runtimeKlineContext, missingFields);
        collectContractMissingFields(runtimeKlineContext, "runtimeKlineContext", missingFields);
        collectContractMissingFields(derivativesRiskContext, "derivativesRiskContext", missingFields);

        SourceTraceFallbackStatusEnum fallbackStatus = resolveFallbackStatus(
                missingFields,
                runtimeKlineContext.getFallbackStatus(),
                derivativesRiskContext == null ? null : derivativesRiskContext.getFallbackStatus()
        );
        sourceTrace.setMissingFields(missingFields);
        sourceTrace.setFallbackStatus(fallbackStatus);
        return sourceTrace;
    }

    private void copyRuntimeSources(RuntimeKlineContextDTO runtimeKlineContext, SourceTraceDTO sourceTrace) {
        sourceTrace.setSymbol(runtimeKlineContext.getSymbol());
        sourceTrace.setTimeframe(runtimeKlineContext.getTimeframe());
        sourceTrace.setEntryPriceSource(runtimeKlineContext.getEntryPriceSource());
        sourceTrace.setEntrySourceType(runtimeKlineContext.getEntrySourceType());
        sourceTrace.setEntrySourceTimeframe(runtimeKlineContext.getEntrySourceTimeframe());
        sourceTrace.setEntrySourceReason(runtimeKlineContext.getEntrySourceReason());
        sourceTrace.setEntrySourceRef(runtimeKlineContext.getEntrySourceRef());
        sourceTrace.setStopPriceSource(runtimeKlineContext.getStopPriceSource());
        sourceTrace.setStopSourceType(runtimeKlineContext.getStopSourceType());
        sourceTrace.setStopSourceTimeframe(runtimeKlineContext.getStopSourceTimeframe());
        sourceTrace.setStopSourceReason(runtimeKlineContext.getStopSourceReason());
        sourceTrace.setStopSourceRef(runtimeKlineContext.getStopSourceRef());
        sourceTrace.setTpPriceSources(runtimeKlineContext.getTpPriceSources());
        sourceTrace.setTpSourceType(runtimeKlineContext.getTpSourceType());
        sourceTrace.setTpSourceTimeframe(runtimeKlineContext.getTpSourceTimeframe());
        sourceTrace.setTpSourceReason(runtimeKlineContext.getTpSourceReason());
        sourceTrace.setTpSourceRef(runtimeKlineContext.getTpSourceRef());
        sourceTrace.setRrSource(runtimeKlineContext.getRrSource());
        sourceTrace.setRrRuleRef(runtimeKlineContext.getRrRuleRef());
        sourceTrace.setLiquiditySource(runtimeKlineContext.getLiquiditySource());
        sourceTrace.setMultiTimeframeSource(runtimeKlineContext.getMultiTimeframeSource());
        sourceTrace.setEventSource(runtimeKlineContext.getEventSource());
        sourceTrace.setWickSource(runtimeKlineContext.getWickSource());
    }

    private void collectRuntimeMissingFields(RuntimeKlineContextDTO runtimeKlineContext, List<String> missingFields) {
        addWhenBlank(runtimeKlineContext.getSymbol(), "symbol", missingFields);
        addWhenBlank(runtimeKlineContext.getTimeframe(), "timeframe", missingFields);
        addWhenNull(runtimeKlineContext.getLatestPrice(), "latestPrice", missingFields);
        addWhenNull(runtimeKlineContext.getDataQualityScore(), "dataQualityScore", missingFields);
        addWhenNull(runtimeKlineContext.getEntryPriceSource(), "entryPriceSource", missingFields);
        addWhenBlank(runtimeKlineContext.getEntrySourceType(), "entrySourceType", missingFields);
        addWhenBlank(runtimeKlineContext.getEntrySourceTimeframe(), "entrySourceTimeframe", missingFields);
        addWhenBlank(runtimeKlineContext.getEntrySourceReason(), "entrySourceReason", missingFields);
        addWhenBlank(runtimeKlineContext.getEntrySourceRef(), "entrySourceRef", missingFields);
        addWhenNull(runtimeKlineContext.getStopPriceSource(), "stopPriceSource", missingFields);
        addWhenBlank(runtimeKlineContext.getStopSourceType(), "stopSourceType", missingFields);
        addWhenBlank(runtimeKlineContext.getStopSourceTimeframe(), "stopSourceTimeframe", missingFields);
        addWhenBlank(runtimeKlineContext.getStopSourceReason(), "stopSourceReason", missingFields);
        addWhenBlank(runtimeKlineContext.getStopSourceRef(), "stopSourceRef", missingFields);
        if (runtimeKlineContext.getTpPriceSources() == null || runtimeKlineContext.getTpPriceSources().isEmpty()) {
            missingFields.add("tpPriceSources");
        }
        addWhenBlank(runtimeKlineContext.getTpSourceType(), "tpSourceType", missingFields);
        addWhenBlank(runtimeKlineContext.getTpSourceTimeframe(), "tpSourceTimeframe", missingFields);
        addWhenBlank(runtimeKlineContext.getTpSourceReason(), "tpSourceReason", missingFields);
        addWhenBlank(runtimeKlineContext.getTpSourceRef(), "tpSourceRef", missingFields);
        addWhenNull(runtimeKlineContext.getRrSource(), "rrSource", missingFields);
        addWhenBlank(runtimeKlineContext.getRrRuleRef(), "rrRuleRef", missingFields);
        addWhenBlank(runtimeKlineContext.getLiquiditySource(), "liquiditySource", missingFields);
        addWhenBlank(runtimeKlineContext.getMultiTimeframeSource(), "multiTimeframeSource", missingFields);
        addWhenBlank(runtimeKlineContext.getEventSource(), "eventSource", missingFields);
        addWhenBlank(runtimeKlineContext.getWickSource(), "wickSource", missingFields);
    }

    private void collectContractMissingFields(
            org.example.trademodel.dto.planboundary.SourceCompletenessContract contract,
            String prefix,
            List<String> missingFields
    ) {
        if (contract == null || contract.getMissingFields() == null) {
            return;
        }
        for (String missingField : contract.getMissingFields()) {
            if (missingField != null && !missingField.trim().isEmpty()) {
                missingFields.add(prefix + "." + missingField);
            }
        }
    }

    private SourceTraceFallbackStatusEnum resolveFallbackStatus(
            List<String> missingFields,
            SourceTraceFallbackStatusEnum runtimeFallbackStatus,
            SourceTraceFallbackStatusEnum derivativesFallbackStatus
    ) {
        if (containsStructuralMissingField(missingFields)
                || runtimeFallbackStatus == SourceTraceFallbackStatusEnum.INCOMPLETE
                || derivativesFallbackStatus == SourceTraceFallbackStatusEnum.INCOMPLETE) {
            return SourceTraceFallbackStatusEnum.INCOMPLETE;
        }
        if (containsField(missingFields, "liquiditySource")
                || runtimeFallbackStatus == SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY
                || derivativesFallbackStatus == SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY) {
            return SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY;
        }
        if (!missingFields.isEmpty()
                || runtimeFallbackStatus == SourceTraceFallbackStatusEnum.WATCH_ONLY
                || derivativesFallbackStatus == SourceTraceFallbackStatusEnum.WATCH_ONLY) {
            return SourceTraceFallbackStatusEnum.WATCH_ONLY;
        }
        return null;
    }

    private boolean containsStructuralMissingField(List<String> missingFields) {
        return containsField(missingFields, "symbol")
                || containsField(missingFields, "timeframe")
                || containsField(missingFields, "latestPrice")
                || containsField(missingFields, "dataQualityScore")
                || containsField(missingFields, "entry")
                || containsField(missingFields, "stop")
                || containsField(missingFields, "tp")
                || containsField(missingFields, "rr");
    }

    private boolean containsField(List<String> missingFields, String fieldFragment) {
        for (String missingField : missingFields) {
            if (missingField != null && missingField.toLowerCase().contains(fieldFragment.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void addWhenNull(Object value, String fieldName, List<String> missingFields) {
        if (value == null) {
            missingFields.add(fieldName);
        }
    }

    private void addWhenBlank(String value, String fieldName, List<String> missingFields) {
        if (value == null || value.trim().isEmpty()) {
            missingFields.add(fieldName);
        }
    }
}
