package org.example.trademodel.providercall.scan;

import org.springframework.beans.factory.annotation.Autowired;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.coinglass.CoinGlassDerivativesSnapshotService;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.snapshot.CoordinatedOhlcvSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultProviderDatasetRefreshPort implements ProviderDatasetRefreshPort {
    private static final List<String> PRIMARY_TIMEFRAMES = List.of("5m", "15m", "1h", "4h");
    private final MarketPriceSnapshotService priceService;
    private final CoordinatedOhlcvSnapshotService ohlcvService;
    private final PersistedOhlcvBarMapper ohlcvBarMapper;
    private final ProviderSymbolMappingRegistry mappingRegistry;
    private final CoinGlassDerivativesSnapshotService derivativesService;
    private final ProviderCallProperties properties;
    private final ProviderRefreshStateRegistry registry;
    private final ProviderCapabilityRegistry capabilityRegistry;
    private final Clock clock;

    @Autowired
    public DefaultProviderDatasetRefreshPort(MarketPriceSnapshotService priceService,
                                             CoordinatedOhlcvSnapshotService ohlcvService,
                                             PersistedOhlcvBarMapper ohlcvBarMapper,
                                             ProviderSymbolMappingRegistry mappingRegistry,
                                             CoinGlassDerivativesSnapshotService derivativesService,
                                             ProviderCallProperties properties,
                                             ProviderRefreshStateRegistry registry,
                                             ProviderCapabilityRegistry capabilityRegistry) {
        this(priceService, ohlcvService, ohlcvBarMapper, mappingRegistry, derivativesService, properties, registry,
                capabilityRegistry, Clock.systemUTC());
    }

    /** Compatibility constructor for focused unit tests that do not build the Spring capability gate. */
    public DefaultProviderDatasetRefreshPort(MarketPriceSnapshotService priceService,
                                             CoordinatedOhlcvSnapshotService ohlcvService,
                                             PersistedOhlcvBarMapper ohlcvBarMapper,
                                             ProviderSymbolMappingRegistry mappingRegistry,
                                             CoinGlassDerivativesSnapshotService derivativesService,
                                             ProviderCallProperties properties,
                                             ProviderRefreshStateRegistry registry) {
        this(priceService, ohlcvService, ohlcvBarMapper, mappingRegistry, derivativesService, properties, registry,
                null, Clock.systemUTC());
    }

    public DefaultProviderDatasetRefreshPort(MarketPriceSnapshotService priceService,
                                             CoordinatedOhlcvSnapshotService ohlcvService,
                                             PersistedOhlcvBarMapper ohlcvBarMapper,
                                             ProviderSymbolMappingRegistry mappingRegistry,
                                             CoinGlassDerivativesSnapshotService derivativesService,
                                             ProviderCallProperties properties,
                                             ProviderRefreshStateRegistry registry,
                                             Clock clock) {
        this(priceService, ohlcvService, ohlcvBarMapper, mappingRegistry, derivativesService, properties, registry,
                null, clock);
    }

    public DefaultProviderDatasetRefreshPort(MarketPriceSnapshotService priceService,
                                             CoordinatedOhlcvSnapshotService ohlcvService,
                                             PersistedOhlcvBarMapper ohlcvBarMapper,
                                             ProviderSymbolMappingRegistry mappingRegistry,
                                             CoinGlassDerivativesSnapshotService derivativesService,
                                             ProviderCallProperties properties,
                                             ProviderRefreshStateRegistry registry,
                                             ProviderCapabilityRegistry capabilityRegistry,
                                             Clock clock) {
        this.priceService = priceService;
        this.ohlcvService = ohlcvService;
        this.ohlcvBarMapper = ohlcvBarMapper;
        this.mappingRegistry = mappingRegistry;
        this.derivativesService = derivativesService;
        this.properties = properties;
        this.registry = registry;
        this.capabilityRegistry = capabilityRegistry;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public void refresh(ScanPlanItem item, ProviderDatasetType datasetType) {
        String traceId = "provider-scan-" + UUID.randomUUID();
        Instant attemptedAt = clock.instant();
        switch (datasetType) {
            case PRICE -> refreshPrice(item, traceId, attemptedAt);
            case OHLCV -> refreshOhlcv(item, traceId, attemptedAt);
            case DERIVATIVES -> refreshDerivatives(item, traceId, attemptedAt);
            case FUNDING, OPEN_INTEREST -> unavailable(item, datasetType,
                    UnifiedSourceStatus.NOT_CONFIGURED, "DIRECT_SUBDATASET_REFRESH_NOT_SUPPORTED",
                    traceId, attemptedAt);
            case COINGLASS_OPEN_INTEREST, COINGLASS_FUNDING, COINGLASS_LIQUIDATION,
                    COINGLASS_LONG_SHORT_RATIO -> unavailable(item, datasetType, UnifiedSourceStatus.DISABLED,
                    "COINGLASS_COMPONENT_DATASET_INTERNAL_ONLY", traceId, attemptedAt);
            case EXTERNAL_CONTEXT -> unavailable(item, datasetType, UnifiedSourceStatus.NOT_CONFIGURED,
                    "EXTERNAL_CONTEXT_PROVIDER_NOT_CONFIGURED", traceId, attemptedAt);
            case AI_REVIEW -> unavailable(item, datasetType, UnifiedSourceStatus.DISABLED,
                    "AI_ROUTINE_SCAN_DISABLED", traceId, attemptedAt);
        }
    }

    private void refreshDerivatives(ScanPlanItem item, String traceId, Instant attemptedAt) {
        int seconds = properties.intervalSeconds(item.effectiveProfile(), item.effectivePriority(),
                ProviderDatasetType.DERIVATIVES);
        ProviderCallResult<DerivativesRiskSnapshot> result = derivativesService.get(item.canonicalInstrumentId(),
                item.effectivePriority(), Duration.ofSeconds(Math.max(1, seconds)), traceId);
        record(item, ProviderDatasetType.DERIVATIVES, result, attemptedAt, traceId);
    }

    private void refreshPrice(ScanPlanItem item, String traceId, Instant attemptedAt) {
        int seconds = properties.intervalSeconds(item.effectiveProfile(), item.effectivePriority(), ProviderDatasetType.PRICE);
        ProviderCallResult<MarketPriceSnapshot> result = priceService.get(item.canonicalInstrumentId(), item.effectivePriority(),
                Duration.ofSeconds(Math.max(1, seconds)), traceId);
        record(item, ProviderDatasetType.PRICE, result, attemptedAt, traceId);
    }

    private void refreshOhlcv(ScanPlanItem item, String traceId, Instant attemptedAt) {
        if (capabilityRegistry != null) {
            for (String timeframe : PRIMARY_TIMEFRAMES) {
                ProviderCallResult<OhlcvIngestionResult> result = ohlcvService.refresh(
                        item.canonicalInstrumentId(), timeframe, 100, item.effectivePriority(), traceId);
                recordAuthorizedOhlcv(item, result, attemptedAt, traceId, timeframe);
            }
            return;
        }
        ProviderSymbolMapping mapping;
        try {
            mapping = mappingRegistry.resolve("BINANCE", item.canonicalInstrumentId());
        } catch (RuntimeException missingMapping) {
            for (String timeframe : PRIMARY_TIMEFRAMES) {
                registry.record(new ProviderRefreshObservation(item.canonicalInstrumentId(), item.providerSymbol(),
                        ProviderDatasetType.OHLCV, UnifiedSourceStatus.NOT_CONFIGURED,
                        SnapshotFreshnessStatus.UNAVAILABLE, "PROVIDER_SYMBOL_MAPPING_NOT_FOUND",
                        attemptedAt, null, traceId, timeframe, "BINANCE",
                        providerMarketType(item.canonicalInstrumentId().marketType()), "UNVERIFIED"));
            }
            return;
        }
        OhlcvPersistedSourceIdentity sourceIdentity = sourceIdentity(mapping);
        for (String timeframe : PRIMARY_TIMEFRAMES) {
            OhlcvDueState dueState = dueState(mapping, sourceIdentity, timeframe, attemptedAt);
            if (!dueState.due()) {
                registry.record(new ProviderRefreshObservation(item.canonicalInstrumentId(), item.providerSymbol(),
                        ProviderDatasetType.OHLCV, UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH,
                        "NO_NEW_CLOSED_BAR_DUE", attemptedAt, dueState.latestCloseTime(), traceId, timeframe,
                        mapping.provider(), sourceIdentity.providerMarketType(), mapping.sourceVersion()));
                continue;
            }
            ProviderCallResult<OhlcvIngestionResult> result = ohlcvService.refresh(
                    item.canonicalInstrumentId(), timeframe, 100, item.effectivePriority(), traceId);
            recordOhlcv(item, mapping, sourceIdentity, result, attemptedAt, traceId, timeframe);
        }
    }

    private OhlcvDueState dueState(ProviderSymbolMapping mapping,
                                   OhlcvPersistedSourceIdentity sourceIdentity,
                                   String timeframe,
                                   Instant now) {
        try {
            List<PersistedOhlcvBarDO> rows = ohlcvBarMapper.selectLatestClosedWindowBySource(
                    mapping.providerSymbol(), timeframe, sourceIdentity.persistedProvider(),
                    sourceIdentity.providerMarketType(), 1);
            if (rows == null || rows.isEmpty() || !validAuthoritativeBar(rows.get(0), sourceIdentity)) {
                return new OhlcvDueState(true, null);
            }
            Instant latestClose = Instant.ofEpochMilli(rows.get(0).getCloseTimeMs());
            return new OhlcvDueState(!now.isBefore(latestClose.plusSeconds(timeframeSeconds(timeframe))), latestClose);
        } catch (RuntimeException ignored) {
            // A failed authoritative-read check must not suppress refresh recovery.
            return new OhlcvDueState(true, null);
        }
    }

    private static boolean validAuthoritativeBar(PersistedOhlcvBarDO row,
                                                  OhlcvPersistedSourceIdentity sourceIdentity) {
        return row != null
                && row.getCloseTimeMs() != null
                && Boolean.TRUE.equals(row.getClosed())
                && sourceIdentity.persistedProvider().equalsIgnoreCase(row.getProvider())
                && sourceIdentity.providerMarketType().equalsIgnoreCase(row.getProviderMarketType())
                && "READY".equalsIgnoreCase(row.getSourceStatus())
                && "FRESH".equalsIgnoreCase(row.getFreshnessStatus())
                && "OK".equalsIgnoreCase(row.getQualityStatus())
                && row.getSourceVersion() != null
                && row.getSourceVersion() > 0
                && (row.getIsDeleted() == null || row.getIsDeleted() == 0);
    }

    private static OhlcvPersistedSourceIdentity sourceIdentity(ProviderSymbolMapping mapping) {
        String persistedProvider = "BINANCE".equalsIgnoreCase(mapping.provider())
                ? "BINANCE_PUBLIC" : mapping.provider();
        return new OhlcvPersistedSourceIdentity(persistedProvider,
                providerMarketType(mapping.canonicalInstrumentId().marketType()));
    }

    private static String providerMarketType(MarketType marketType) {
        return marketType == MarketType.PERPETUAL ? "USDT_PERP" : "SPOT";
    }

    private static long timeframeSeconds(String timeframe) {
        return switch (timeframe) {
            case "5m" -> 300L;
            case "15m" -> 900L;
            case "1h" -> 3600L;
            case "4h" -> 14400L;
            default -> throw new IllegalArgumentException("unsupported timeframe: " + timeframe);
        };
    }

    private void unavailable(ScanPlanItem item, ProviderDatasetType datasetType, UnifiedSourceStatus status,
                             String reason, String traceId, Instant attemptedAt) {
        registry.record(new ProviderRefreshObservation(item.canonicalInstrumentId(), item.providerSymbol(),
                datasetType, status,
                SnapshotFreshnessStatus.UNAVAILABLE, reason, attemptedAt, null, traceId));
    }

    private void record(ScanPlanItem item, ProviderDatasetType datasetType, ProviderCallResult<?> result,
                        Instant attemptedAt, String traceId) {
        record(item, datasetType, result, attemptedAt, traceId, "GLOBAL");
    }

    private void record(ScanPlanItem item, ProviderDatasetType datasetType, ProviderCallResult<?> result,
                        Instant attemptedAt, String traceId, String timeframe) {
        if (result == null || result.metadata() == null) {
            registry.record(new ProviderRefreshObservation(item.canonicalInstrumentId(), item.providerSymbol(),
                    datasetType, UnifiedSourceStatus.ERROR,
                    SnapshotFreshnessStatus.UNAVAILABLE, "PROVIDER_RESULT_MISSING", attemptedAt, null, traceId,
                    timeframe));
            return;
        }
        registry.record(new ProviderRefreshObservation(item.canonicalInstrumentId(), item.providerSymbol(),
                datasetType, result.metadata().sourceStatus(),
                result.metadata().freshnessStatus(), result.metadata().errorCode(), attemptedAt,
                result.metadata().providerDataTime(), result.metadata().traceId(), timeframe));
    }

    private void recordOhlcv(ScanPlanItem item,
                             ProviderSymbolMapping mapping,
                             OhlcvPersistedSourceIdentity sourceIdentity,
                             ProviderCallResult<?> result,
                             Instant attemptedAt,
                             String traceId,
                             String timeframe) {
        if (result == null || result.metadata() == null) {
            registry.record(new ProviderRefreshObservation(item.canonicalInstrumentId(), item.providerSymbol(),
                    ProviderDatasetType.OHLCV, UnifiedSourceStatus.ERROR,
                    SnapshotFreshnessStatus.UNAVAILABLE, "PROVIDER_RESULT_MISSING", attemptedAt, null, traceId,
                    timeframe, mapping.provider(), sourceIdentity.providerMarketType(), mapping.sourceVersion()));
            return;
        }
        registry.record(new ProviderRefreshObservation(item.canonicalInstrumentId(), item.providerSymbol(),
                ProviderDatasetType.OHLCV, result.metadata().sourceStatus(), result.metadata().freshnessStatus(),
                result.metadata().errorCode(), attemptedAt, result.metadata().providerDataTime(),
                result.metadata().traceId(), timeframe, mapping.provider(), sourceIdentity.providerMarketType(),
                usableSourceVersion(result.metadata().sourceVersion(), mapping.sourceVersion())));
    }

    private void recordAuthorizedOhlcv(ScanPlanItem item,
                                       ProviderCallResult<?> result,
                                       Instant attemptedAt,
                                       String traceId,
                                       String timeframe) {
        if (result == null || result.metadata() == null) {
            registry.record(new ProviderRefreshObservation(item.canonicalInstrumentId(), item.providerSymbol(),
                    ProviderDatasetType.OHLCV, UnifiedSourceStatus.ERROR,
                    SnapshotFreshnessStatus.UNAVAILABLE, "PROVIDER_RESULT_MISSING", attemptedAt, null, traceId,
                    timeframe));
            return;
        }
        registry.record(new ProviderRefreshObservation(result.metadata().canonicalInstrumentId(),
                result.metadata().providerSymbol(), ProviderDatasetType.OHLCV,
                result.metadata().sourceStatus(), result.metadata().freshnessStatus(),
                result.metadata().errorCode(), attemptedAt, result.metadata().providerDataTime(),
                result.metadata().traceId(), timeframe, result.metadata().provider(),
                providerMarketType(result.metadata().canonicalInstrumentId().marketType()),
                result.metadata().sourceVersion()));
    }

    private static String usableSourceVersion(String resultVersion, String mappingVersion) {
        return resultVersion == null || resultVersion.isBlank() || "UNVERIFIED".equalsIgnoreCase(resultVersion)
                ? mappingVersion : resultVersion;
    }

    private record OhlcvDueState(boolean due, Instant latestCloseTime) {
    }

    private record OhlcvPersistedSourceIdentity(String persistedProvider, String providerMarketType) {
    }
}
