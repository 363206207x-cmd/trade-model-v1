package org.example.trademodel.service.impl;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.service.RuntimeKlineContextAssemblyService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RuntimeKlineContextAssemblyServiceImpl implements RuntimeKlineContextAssemblyService {
    private static final String QUALITY_OK = "OK";
    private static final Set<String> REAL_EXTERNAL_PROVIDERS = Set.of("KRAKEN", "BINANCE", "BINANCE_PUBLIC");

    @Override
    public RuntimeKlineContextDTO assemble(PersistedOhlcvReadinessResult readinessResult) {
        if (readinessResult == null) {
            RuntimeKlineContextDTO context = baseContext(null);
            context.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
            context.setMissingFields(List.of("persistedOhlcvReadinessResult"));
            return context;
        }

        RuntimeKlineContextDTO context = baseContext(readinessResult);
        applyReadinessMetadata(context, readinessResult);

        if (readinessResult.getStatus() != PersistedOhlcvReadinessStatus.FRESH) {
            return failClosed(context, missingFieldsForNonFresh(readinessResult));
        }

        List<String> unsafeFields = unsafeFreshFields(readinessResult);
        if (!unsafeFields.isEmpty()) {
            return failClosed(context, unsafeFields);
        }

        List<PersistedOhlcvBarDO> bars = readinessResult.getBars();
        PersistedOhlcvBarDO latestClosedBar = latestClosedBar(bars);
        context.setLatestPrice(latestClosedBar.getClosePrice());
        context.setKlineItems(toRuntimeKlineItems(bars));
        applyRealMarketProvenance(context, bars, latestClosedBar);
        context.setMissingFields(List.of());
        context.setFallbackStatus(null);
        context.setManualReviewRequired(true);
        context.setNotTradeInstruction(true);
        return context;
    }

    private RuntimeKlineContextDTO baseContext(PersistedOhlcvReadinessResult readinessResult) {
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        if (readinessResult != null) {
            context.setSymbol(readinessResult.getSymbol());
            context.setTimeframe(readinessResult.getTimeframe());
        }
        context.setManualReviewRequired(true);
        context.setNotTradeInstruction(true);
        return context;
    }

    private void applyReadinessMetadata(
            RuntimeKlineContextDTO context,
            PersistedOhlcvReadinessResult readinessResult
    ) {
        if (readinessResult.getStatus() != null) {
            context.setPersistedOhlcvReadinessStatus(readinessResult.getStatus().name());
        }
        if (readinessResult.getStaleReasonCode() != null) {
            context.setPersistedOhlcvStaleReasonCode(readinessResult.getStaleReasonCode().name());
        }
        context.setPersistedOhlcvStaleReasonText(readinessResult.getStaleReasonText());
        context.setPersistedOhlcvMissingFields(readinessResult.getMissingFields());
    }

    private RuntimeKlineContextDTO failClosed(RuntimeKlineContextDTO context, List<String> missingFields) {
        context.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        context.setMissingFields(missingFields);
        context.setLatestPrice(null);
        context.setKlineItems(List.of());
        context.setSourceMode(null);
        context.setSourceProvider(null);
        context.setSourceMarketType(null);
        context.setRealMarketEnvironment(false);
        context.setClosedBarCount(0);
        context.setLatestClosedBarTimeMs(null);
        context.setSourceTraceRefs(List.of());
        context.setManualReviewRequired(true);
        context.setNotTradeInstruction(true);
        return context;
    }

    private void applyRealMarketProvenance(RuntimeKlineContextDTO context,
                                           List<PersistedOhlcvBarDO> bars,
                                           PersistedOhlcvBarDO latestClosedBar) {
        String provider = bars.get(0).getProvider();
        String marketType = bars.get(0).getProviderMarketType();
        boolean consistentProvider = bars.stream()
                .allMatch(bar -> bar != null && safeEquals(provider, bar.getProvider()));
        boolean consistentMarketType = bars.stream()
                .allMatch(bar -> bar != null && safeEquals(marketType, bar.getProviderMarketType()));
        boolean realProvider = consistentProvider && consistentMarketType
                && isRealExternalProvider(provider) && "SPOT".equalsIgnoreCase(marketType);
        context.setSourceProvider(provider);
        context.setSourceMarketType(marketType);
        context.setSourceMode(realProvider ? "REAL" : null);
        context.setRealMarketEnvironment(realProvider);
        context.setClosedBarCount(bars.size());
        context.setLatestClosedBarTimeMs(latestClosedBar.getCloseTimeMs());
        context.setSourceTraceRefs(bars.stream()
                .map(PersistedOhlcvBarDO::getSourceTraceId)
                .filter(this::hasText)
                .distinct()
                .toList());
    }

    private boolean isRealExternalProvider(String provider) {
        if (!hasText(provider)) return false;
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        return REAL_EXTERNAL_PROVIDERS.contains(normalized);
    }

    private List<String> missingFieldsForNonFresh(PersistedOhlcvReadinessResult readinessResult) {
        List<String> fields = new ArrayList<>(readinessResult.getMissingFields());
        if (fields.isEmpty()) {
            addUnique("persistedOhlcvReadinessStatus", fields);
        }
        if (readinessResult.getStatus() == null) {
            addUnique("persistedOhlcvReadinessStatus", fields);
        }
        if (readinessResult.getStaleReasonCode() == null) {
            addUnique("persistedOhlcvStaleReasonCode", fields);
        }
        return fields;
    }

    private List<String> unsafeFreshFields(PersistedOhlcvReadinessResult readinessResult) {
        List<String> fields = new ArrayList<>();
        if (!hasText(readinessResult.getSymbol())) {
            addUnique("symbol", fields);
        }
        if (!hasText(readinessResult.getTimeframe())) {
            addUnique("timeframe", fields);
        }
        if (readinessResult.getRequiredWindowSize() == null || readinessResult.getRequiredWindowSize() <= 0) {
            addUnique("requiredWindowSize", fields);
        }
        if (readinessResult.getStaleReasonCode() != PersistedOhlcvStaleReasonCode.NONE) {
            addUnique("persistedOhlcvStaleReasonCode", fields);
        }
        if (!readinessResult.getMissingFields().isEmpty()) {
            fields.addAll(readinessResult.getMissingFields());
        }
        if (!readinessResult.isManualReviewRequired()) {
            addUnique("manualReviewRequired", fields);
        }
        if (!readinessResult.isNotTradeInstruction()) {
            addUnique("notTradeInstruction", fields);
        }

        List<PersistedOhlcvBarDO> bars = readinessResult.getBars();
        if (bars.isEmpty()) {
            addUnique("klineItems", fields);
            return fields;
        }
        if (readinessResult.getRequiredWindowSize() != null
                && readinessResult.getRequiredWindowSize() > 0
                && bars.size() < readinessResult.getRequiredWindowSize()) {
            addUnique("requiredClosedBars", fields);
        }
        Long intervalMs = parseTimeframeMs(readinessResult.getTimeframe());
        if (intervalMs == null) {
            addUnique("timeframePolicy", fields);
        }
        for (PersistedOhlcvBarDO bar : bars) {
            collectUnsafeBarFields(bar, readinessResult, fields);
        }
        if (intervalMs != null && !isContiguous(bars, intervalMs)) {
            addUnique("klineWindow", fields);
        }
        PersistedOhlcvBarDO latest = latestClosedBar(bars);
        if (readinessResult.getLatestCloseTimeMs() == null) {
            addUnique("latestCloseTimeMs", fields);
        } else if (latest == null || !readinessResult.getLatestCloseTimeMs().equals(latest.getCloseTimeMs())) {
            addUnique("latestCloseTimeMs", fields);
        }
        if (readinessResult.getLatestIngestedAt() == null) {
            addUnique("latestIngestedAt", fields);
        }
        return dedupe(fields);
    }

    private void collectUnsafeBarFields(
            PersistedOhlcvBarDO bar,
            PersistedOhlcvReadinessResult readinessResult,
            List<String> fields
    ) {
        if (bar == null) {
            addUnique("klineItems", fields);
            return;
        }
        if (!safeEquals(readinessResult.getSymbol(), bar.getSymbol())) {
            addUnique("symbol", fields);
        }
        if (!safeEquals(readinessResult.getTimeframe(), bar.getTimeframe())) {
            addUnique("timeframe", fields);
        }
        addWhenNull(bar.getOpenTimeMs(), "openTimeMs", fields);
        addWhenNull(bar.getCloseTimeMs(), "closeTimeMs", fields);
        addWhenNonPositive(bar.getOpenPrice(), "openPrice", fields);
        addWhenNonPositive(bar.getHighPrice(), "highPrice", fields);
        addWhenNonPositive(bar.getLowPrice(), "lowPrice", fields);
        addWhenNonPositive(bar.getClosePrice(), "closePrice", fields);
        addWhenNegativeOrNull(bar.getVolume(), "volume", fields);
        if (bar.getHighPrice() != null && bar.getLowPrice() != null
                && bar.getHighPrice().compareTo(bar.getLowPrice()) < 0) {
            addUnique("ohlcRange", fields);
        }
        if (bar.getHighPrice() != null && bar.getOpenPrice() != null
                && bar.getHighPrice().compareTo(bar.getOpenPrice()) < 0) {
            addUnique("ohlcGeometry", fields);
        }
        if (bar.getHighPrice() != null && bar.getClosePrice() != null
                && bar.getHighPrice().compareTo(bar.getClosePrice()) < 0) {
            addUnique("ohlcGeometry", fields);
        }
        if (bar.getLowPrice() != null && bar.getOpenPrice() != null
                && bar.getLowPrice().compareTo(bar.getOpenPrice()) > 0) {
            addUnique("ohlcGeometry", fields);
        }
        if (bar.getLowPrice() != null && bar.getClosePrice() != null
                && bar.getLowPrice().compareTo(bar.getClosePrice()) > 0) {
            addUnique("ohlcGeometry", fields);
        }
        if (bar.getOpenTimeMs() != null && bar.getCloseTimeMs() != null
                && (bar.getOpenTimeMs() < 0 || bar.getCloseTimeMs() <= bar.getOpenTimeMs())) {
            addUnique("timestampOrder", fields);
        }
        if (bar.getClosed() == null || !bar.getClosed()) {
            addUnique("closed", fields);
        }
        if (bar.getIsDeleted() == null || bar.getIsDeleted() != 0) {
            addUnique("isDeleted", fields);
        }
        addWhenBlank(bar.getProvider(), "provider", fields);
        addWhenBlank(bar.getProviderMarketType(), "providerMarketType", fields);
        addWhenBlank(bar.getSourceEndpoint(), "sourceEndpoint", fields);
        addWhenBlank(bar.getSourceBatchId(), "sourceBatchId", fields);
        addWhenBlank(bar.getSourceTraceId(), "sourceTraceId", fields);
        addWhenNull(bar.getSourceVersion(), "sourceVersion", fields);
        addWhenNull(bar.getFetchTime(), "fetchTime", fields);
        addWhenBlank(bar.getSourceStatus(), "sourceStatus", fields);
        addWhenBlank(bar.getFreshnessStatus(), "freshnessStatus", fields);
        addWhenBlank(bar.getProvenanceVersion(), "provenanceVersion", fields);
        addWhenBlank(bar.getIngestionRunId(), "ingestionRunId", fields);
        addWhenNull(bar.getIngestedAt(), "ingestedAt", fields);
        if (!"READY".equals(bar.getSourceStatus())) {
            addUnique("sourceStatus", fields);
        }
        if (!"FRESH".equals(bar.getFreshnessStatus())) {
            addUnique("freshnessStatus", fields);
        }
        if (!QUALITY_OK.equals(bar.getQualityStatus())) {
            addUnique("qualityStatus", fields);
        }
    }

    private PersistedOhlcvBarDO latestClosedBar(List<PersistedOhlcvBarDO> bars) {
        return bars.stream()
                .filter(bar -> bar != null && Boolean.TRUE.equals(bar.getClosed()))
                .filter(bar -> bar.getCloseTimeMs() != null)
                .max(Comparator.comparing(PersistedOhlcvBarDO::getCloseTimeMs))
                .orElse(null);
    }

    private List<RuntimeKlineItemDTO> toRuntimeKlineItems(List<PersistedOhlcvBarDO> bars) {
        List<RuntimeKlineItemDTO> items = new ArrayList<>();
        for (PersistedOhlcvBarDO bar : bars) {
            RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
            item.setOpenTimeMs(bar.getOpenTimeMs());
            item.setCloseTimeMs(bar.getCloseTimeMs());
            item.setOpenPrice(bar.getOpenPrice());
            item.setHighPrice(bar.getHighPrice());
            item.setLowPrice(bar.getLowPrice());
            item.setClosePrice(bar.getClosePrice());
            item.setVolume(bar.getVolume());
            item.setProvider(bar.getProvider());
            item.setProviderMarketType(bar.getProviderMarketType());
            item.setSourceEndpoint(bar.getSourceEndpoint());
            item.setSourceBatchId(bar.getSourceBatchId());
            item.setSourceTraceId(bar.getSourceTraceId());
            item.setSourceVersion(bar.getSourceVersion());
            item.setFetchTime(bar.getFetchTime());
            item.setSourceStatus(bar.getSourceStatus());
            item.setFreshnessStatus(bar.getFreshnessStatus());
            item.setProvenanceVersion(bar.getProvenanceVersion());
            item.setIngestionRunId(bar.getIngestionRunId());
            item.setIngestedAt(bar.getIngestedAt());
            item.setQualityStatus(bar.getQualityStatus());
            items.add(item);
        }
        return items;
    }

    private boolean isContiguous(List<PersistedOhlcvBarDO> bars, long intervalMs) {
        List<PersistedOhlcvBarDO> sorted = bars.stream()
                .filter(bar -> bar != null && bar.getOpenTimeMs() != null)
                .sorted(Comparator.comparing(PersistedOhlcvBarDO::getOpenTimeMs))
                .toList();
        if (sorted.size() != bars.size()) {
            return false;
        }
        for (int i = 0; i < sorted.size() - 1; i++) {
            long current = sorted.get(i).getOpenTimeMs();
            long next = sorted.get(i + 1).getOpenTimeMs();
            if (next - current != intervalMs) {
                return false;
            }
        }
        return true;
    }

    private Long parseTimeframeMs(String timeframe) {
        if (!hasText(timeframe) || timeframe.length() < 2) {
            return null;
        }
        String unit = timeframe.substring(timeframe.length() - 1);
        String amountText = timeframe.substring(0, timeframe.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) {
            return null;
        }
        return switch (unit) {
            case "m" -> amount * 60_000L;
            case "h" -> amount * 60L * 60_000L;
            case "d" -> amount * 24L * 60L * 60_000L;
            default -> null;
        };
    }

    private List<String> dedupe(List<String> fields) {
        List<String> deduped = new ArrayList<>();
        for (String field : fields) {
            addUnique(field, deduped);
        }
        return deduped;
    }

    private void addWhenBlank(String value, String field, List<String> fields) {
        if (!hasText(value)) {
            addUnique(field, fields);
        }
    }

    private void addWhenNull(Object value, String field, List<String> fields) {
        if (value == null) {
            addUnique(field, fields);
        }
    }

    private void addWhenNonPositive(BigDecimal value, String field, List<String> fields) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            addUnique(field, fields);
        }
    }

    private void addWhenNegativeOrNull(BigDecimal value, String field, List<String> fields) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            addUnique(field, fields);
        }
    }

    private void addUnique(String field, List<String> fields) {
        if (!fields.contains(field)) {
            fields.add(field);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
