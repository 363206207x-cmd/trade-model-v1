package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallRequest;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRequestKey;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

abstract class AbstractCoinGlassDatasetSnapshotService<T> {
    static final String PROVIDER = "COINGLASS";
    private final ProviderDatasetType datasetType;
    private final String timeframe;
    private final ProviderCallCoordinator coordinator;
    private final CoinGlassProperties properties;
    private final CoinGlassSymbolMapper symbolMapper;
    private final Clock clock;

    AbstractCoinGlassDatasetSnapshotService(ProviderDatasetType datasetType,
                                             String timeframe,
                                             ProviderCallCoordinator coordinator,
                                             CoinGlassProperties properties,
                                             CoinGlassSymbolMapper symbolMapper) {
        this(datasetType, timeframe, coordinator, properties, symbolMapper, Clock.systemUTC());
    }

    AbstractCoinGlassDatasetSnapshotService(ProviderDatasetType datasetType,
                                             String timeframe,
                                             ProviderCallCoordinator coordinator,
                                             CoinGlassProperties properties,
                                             CoinGlassSymbolMapper symbolMapper,
                                             Clock clock) {
        this.datasetType = datasetType;
        this.timeframe = timeframe;
        this.coordinator = coordinator;
        this.properties = properties;
        this.symbolMapper = symbolMapper;
        this.clock = clock;
    }

    ProviderCallResult<T> get(String symbol, AssetPriority priority, Duration requestedFreshTtl,
                              String traceId, Supplier<ProviderAdapterResponse<T>> call) {
        ProviderRequestKey key;
        try {
            CoinGlassSymbolMapper.CoinGlassSymbol mapped = symbolMapper.map(symbol);
            long bucket = Math.max(1L, requestedFreshTtl == null ? properties.getFreshTtlSeconds()
                    : requestedFreshTtl.toSeconds());
            key = new ProviderRequestKey(PROVIDER, datasetType, mapped.canonicalInstrumentId(),
                    mapped.pairSymbol(), timeframe, String.valueOf(clock.instant().getEpochSecond() / bucket),
                    mapped.sourceVersion());
        } catch (IllegalArgumentException invalid) {
            return unavailable(symbol, traceId, UnifiedSourceStatus.ERROR, "COINGLASS_SYMBOL_UNSUPPORTED");
        }
        if (!properties.isEnabled()) {
            return unavailable(key, traceId, UnifiedSourceStatus.DISABLED, "COINGLASS_PROVIDER_DISABLED");
        }
        if (!properties.hasApiKey()) {
            return unavailable(key, traceId, UnifiedSourceStatus.NOT_CONFIGURED, "COINGLASS_API_KEY_MISSING");
        }
        if (!properties.isExternalCallsEnabled()) {
            return unavailable(key, traceId, UnifiedSourceStatus.DISABLED, "COINGLASS_EXTERNAL_CALLS_DISABLED");
        }
        Duration minimum = Duration.ofSeconds(Math.max(1, properties.getEmergencyMinRefreshGapSeconds()));
        Duration freshTtl = requestedFreshTtl == null || requestedFreshTtl.compareTo(minimum) < 0
                ? minimum : requestedFreshTtl;
        Duration staleTtl = Duration.ofSeconds(Math.max(1, properties.getStaleTtlSeconds()));
        Duration timeout = Duration.ofMillis(Math.max(1, properties.getRequestTimeoutMs()));
        return coordinator.execute(new ProviderCallRequest<>(key, priority, freshTtl, staleTtl, timeout,
                traceId, Math.max(0, properties.getMaxRetry5xx()),
                Math.max(0, properties.getMaxRetryTimeout()), call));
    }

    ProviderCallResult<T> peek(String symbol, AssetPriority priority, Duration requestedFreshTtl,
                              String traceId) {
        ProviderRequestKey key;
        try {
            CoinGlassSymbolMapper.CoinGlassSymbol mapped = symbolMapper.map(symbol);
            long bucket = Math.max(1L, requestedFreshTtl == null ? properties.getFreshTtlSeconds()
                    : requestedFreshTtl.toSeconds());
            key = new ProviderRequestKey(PROVIDER, datasetType, mapped.canonicalInstrumentId(),
                    mapped.pairSymbol(), timeframe, String.valueOf(clock.instant().getEpochSecond() / bucket),
                    mapped.sourceVersion());
        } catch (IllegalArgumentException invalid) {
            return unavailable(symbol, traceId, UnifiedSourceStatus.ERROR, "COINGLASS_SYMBOL_UNSUPPORTED");
        }
        Duration minimum = Duration.ofSeconds(Math.max(1, properties.getEmergencyMinRefreshGapSeconds()));
        Duration freshTtl = requestedFreshTtl == null || requestedFreshTtl.compareTo(minimum) < 0
                ? minimum : requestedFreshTtl;
        return coordinator.peek(key, priority, freshTtl, traceId);
    }

    private ProviderCallResult<T> unavailable(String symbol, String traceId,
                                               UnifiedSourceStatus status, String reason) {
        Instant now = clock.instant();
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata(PROVIDER, datasetType,
                symbol == null || symbol.isBlank() ? "INVALID" : symbol.trim().toUpperCase(), timeframe,
                null, now, now, status, SnapshotFreshnessStatus.UNAVAILABLE, traceId,
                "UNMAPPED", false, false, reason, List.of(reason));
        return new ProviderCallResult<>(null, metadata, null);
    }

    private ProviderCallResult<T> unavailable(ProviderRequestKey key, String traceId,
                                               UnifiedSourceStatus status, String reason) {
        Instant now = clock.instant();
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata(PROVIDER, datasetType,
                key.canonicalInstrumentId(), key.providerSymbol(), timeframe, null, now, now, 0L,
                status, SnapshotFreshnessStatus.UNAVAILABLE, traceId, key.canonical(), key.sourceVersion(),
                false, false, reason, List.of(reason));
        return new ProviderCallResult<>(null, metadata, null);
    }
}
