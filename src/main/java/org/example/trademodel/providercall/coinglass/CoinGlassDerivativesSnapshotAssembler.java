package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.ProviderBudgetState;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CoinGlassDerivativesSnapshotAssembler {
    private final CoinGlassProperties properties;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CoinGlassDerivativesSnapshotAssembler(CoinGlassProperties properties) {
        this(properties, Clock.systemUTC());
    }

    CoinGlassDerivativesSnapshotAssembler(CoinGlassProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public ProviderCallResult<DerivativesRiskSnapshot> assemble(
            String symbol,
            String traceId,
            ProviderCallResult<CoinGlassOpenInterestSnapshot> oiResult,
            ProviderCallResult<CoinGlassFundingSnapshot> fundingResult,
            ProviderCallResult<CoinGlassLiquidationSnapshot> liquidationResult,
            ProviderCallResult<CoinGlassLongShortSnapshot> longShortResult) {
        List<DatasetResult> datasets = List.of(
                dataset(ProviderDatasetType.COINGLASS_OPEN_INTEREST, oiResult),
                dataset(ProviderDatasetType.COINGLASS_FUNDING, fundingResult),
                dataset(ProviderDatasetType.COINGLASS_LIQUIDATION, liquidationResult),
                dataset(ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, longShortResult));

        List<String> available = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> degraded = new ArrayList<>();
        Set<String> reasons = new LinkedHashSet<>();
        Instant providerDataTime = null;
        Instant fetchTime = null;
        Instant expiresAt = null;
        boolean stale = false;
        for (DatasetResult dataset : datasets) {
            ProviderSnapshotMetadata metadata = dataset.metadata();
            if (dataset.payload() != null) {
                available.add(dataset.type().name());
            } else {
                missing.add(dataset.type().name());
            }
            if (metadata != null) {
                if (metadata.errorCode() != null) reasons.add(dataset.type().name() + ":" + metadata.errorCode());
                if (metadata.sourceStatus() != UnifiedSourceStatus.READY) {
                    degraded.add(dataset.type().name());
                }
                providerDataTime = later(providerDataTime, metadata.providerDataTime());
                fetchTime = later(fetchTime, metadata.fetchTime());
                expiresAt = earlier(expiresAt, metadata.expiresAt());
                stale |= isStale(metadata);
            }
        }
        UnifiedSourceStatus sourceStatus = aggregateStatus(datasets, available.size(), stale);
        SnapshotFreshnessStatus freshness = aggregateFreshness(sourceStatus, stale, available.size());
        Instant now = clock.instant();
        if (fetchTime == null) fetchTime = now;
        if (expiresAt == null) expiresAt = fetchTime;
        String evidence = available.size() == datasets.size() && sourceStatus == UnifiedSourceStatus.READY
                ? "COMPLETE" : available.isEmpty() ? "UNAVAILABLE" : "PARTIAL";
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("COINGLASS", ProviderDatasetType.DERIVATIVES,
                symbol, "GLOBAL", providerDataTime, fetchTime, expiresAt, sourceStatus, freshness, traceId,
                "COINGLASS|DERIVATIVES|" + symbol + "|GLOBAL|LATEST", false,
                datasets.stream().anyMatch(value -> value.metadata() != null && value.metadata().fallbackUsed()),
                reasons.isEmpty() ? null : reasons.iterator().next(), List.copyOf(reasons));
        ProviderBudgetState budgetState = datasets.stream()
                .map(DatasetResult::budgetState)
                .filter(java.util.Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);

        if (available.isEmpty()) {
            return new ProviderCallResult<>(null, metadata, budgetState);
        }

        CoinGlassOpenInterestSnapshot oi = payload(oiResult);
        CoinGlassFundingSnapshot funding = payload(fundingResult);
        CoinGlassLiquidationSnapshot liquidation = payload(liquidationResult);
        CoinGlassLongShortSnapshot longShort = payload(longShortResult);
        Map<String, String> fieldSources = new LinkedHashMap<>();
        if (oi != null) fieldSources.putAll(oi.fieldSources());
        if (funding != null) fieldSources.putAll(funding.fieldSources());
        if (liquidation != null) fieldSources.putAll(liquidation.fieldSources());
        if (longShort != null) fieldSources.putAll(longShort.fieldSources());

        DerivativesRiskSnapshot snapshot = new DerivativesRiskSnapshot(symbol, "COINGLASS_V4",
                providerDataTime, fetchTime, expiresAt,
                oi == null ? null : oi.openInterestUsd(),
                oi == null ? null : oi.openInterestChange1m(),
                oi == null ? null : oi.openInterestChange5m(),
                oi == null ? null : oi.openInterestChange15m(),
                oi == null ? null : oi.openInterestChange1h(),
                funding == null ? null : funding.weightedFundingRate(),
                null,
                longShort == null ? null : longShort.longShortRatio(),
                longShort == null ? null : longShort.longShortRatioSource(),
                liquidation == null ? null : liquidation.longLiquidationUsd1m(),
                liquidation == null ? null : liquidation.longLiquidationUsd5m(),
                liquidation == null ? null : liquidation.longLiquidationUsd15m(),
                liquidation == null ? null : liquidation.longLiquidationUsd1h(),
                liquidation == null ? null : liquidation.shortLiquidationUsd1m(),
                liquidation == null ? null : liquidation.shortLiquidationUsd5m(),
                liquidation == null ? null : liquidation.shortLiquidationUsd15m(),
                liquidation == null ? null : liquidation.shortLiquidationUsd1h(),
                null,
                oi == null ? null : oi.exchangeConcentrationScore(),
                available, missing, degraded, sourceStatus, freshness, evidence, List.copyOf(reasons), traceId,
                fieldSources, metadata);
        return new ProviderCallResult<>(snapshot, metadata, budgetState);
    }

    private boolean isStale(ProviderSnapshotMetadata metadata) {
        if (metadata.sourceStatus() == UnifiedSourceStatus.STALE
                || metadata.freshnessStatus() == SnapshotFreshnessStatus.STALE) return true;
        if (metadata.providerDataTime() == null) return false;
        return metadata.providerDataTime().plus(Duration.ofSeconds(Math.max(1, properties.getFreshTtlSeconds())))
                .isBefore(clock.instant());
    }

    private static UnifiedSourceStatus aggregateStatus(List<DatasetResult> datasets, int availableCount, boolean stale) {
        if (availableCount == datasets.size()) return stale ? UnifiedSourceStatus.STALE : UnifiedSourceStatus.READY;
        if (availableCount > 0) return UnifiedSourceStatus.DEGRADED;
        Set<UnifiedSourceStatus> statuses = new LinkedHashSet<>();
        datasets.stream().map(DatasetResult::metadata).filter(java.util.Objects::nonNull)
                .map(ProviderSnapshotMetadata::sourceStatus).forEach(statuses::add);
        if (statuses.size() == 1) return statuses.iterator().next();
        if (statuses.contains(UnifiedSourceStatus.ERROR)) return UnifiedSourceStatus.ERROR;
        if (statuses.contains(UnifiedSourceStatus.DEGRADED)) return UnifiedSourceStatus.DEGRADED;
        if (statuses.contains(UnifiedSourceStatus.NOT_CONFIGURED)) return UnifiedSourceStatus.NOT_CONFIGURED;
        if (statuses.contains(UnifiedSourceStatus.DISABLED)) return UnifiedSourceStatus.DISABLED;
        if (statuses.contains(UnifiedSourceStatus.EMPTY_CONFIRMED)) return UnifiedSourceStatus.EMPTY_CONFIRMED;
        return UnifiedSourceStatus.WAITING_SYNC;
    }

    private static SnapshotFreshnessStatus aggregateFreshness(
            UnifiedSourceStatus status, boolean stale, int availableCount) {
        if (stale) return SnapshotFreshnessStatus.STALE;
        if (status == UnifiedSourceStatus.ERROR) return SnapshotFreshnessStatus.ERROR;
        if (availableCount > 0) return SnapshotFreshnessStatus.FRESH;
        return SnapshotFreshnessStatus.UNAVAILABLE;
    }

    private static DatasetResult dataset(ProviderDatasetType type, ProviderCallResult<?> result) {
        return new DatasetResult(type, result == null ? null : result.payload(),
                result == null ? null : result.metadata(), result == null ? null : result.budgetState());
    }

    private static <T> T payload(ProviderCallResult<T> result) {
        return result == null ? null : result.payload();
    }

    private static Instant later(Instant first, Instant second) {
        return first == null ? second : second == null || first.isAfter(second) ? first : second;
    }

    private static Instant earlier(Instant first, Instant second) {
        return first == null ? second : second == null || first.isBefore(second) ? first : second;
    }

    private record DatasetResult(ProviderDatasetType type, Object payload,
                                 ProviderSnapshotMetadata metadata, ProviderBudgetState budgetState) {
    }
}
