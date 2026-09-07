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
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.instrument.ProviderCapabilityState;
import org.example.trademodel.providercall.instrument.ProviderInstrumentCapability;

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
    private final ProviderCapabilityRegistry capabilityRegistry;
    private final Clock clock;
    private CoinGlassProviderHealthService runtimeHealth;

    @org.springframework.beans.factory.annotation.Autowired
    void setRuntimeHealth(CoinGlassProviderHealthService health) {
        this.runtimeHealth = health;
    }

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
        this(datasetType, timeframe, coordinator, properties, symbolMapper, null, clock);
    }

    AbstractCoinGlassDatasetSnapshotService(ProviderDatasetType datasetType,
                                             String timeframe,
                                             ProviderCallCoordinator coordinator,
                                             CoinGlassProperties properties,
                                             CoinGlassSymbolMapper symbolMapper,
                                             ProviderCapabilityRegistry capabilityRegistry) {
        this(datasetType, timeframe, coordinator, properties, symbolMapper,
                capabilityRegistry, Clock.systemUTC());
    }

    AbstractCoinGlassDatasetSnapshotService(ProviderDatasetType datasetType,
                                             String timeframe,
                                             ProviderCallCoordinator coordinator,
                                             CoinGlassProperties properties,
                                             CoinGlassSymbolMapper symbolMapper,
                                             ProviderCapabilityRegistry capabilityRegistry,
                                             Clock clock) {
        this.datasetType = datasetType;
        this.timeframe = timeframe;
        this.coordinator = coordinator;
        this.properties = properties;
        this.symbolMapper = symbolMapper;
        this.capabilityRegistry = capabilityRegistry;
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
        CoinGlassConfigurationState configuration = properties.configurationState();
        if (configuration != CoinGlassConfigurationState.CONFIGURED) {
            String reason = switch (configuration) {
                case NOT_CONFIGURED -> "COINGLASS_PROVIDER_NOT_CONFIGURED";
                case KEY_MISSING -> "COINGLASS_API_KEY_MISSING";
                case RPM_NOT_CONFIGURED -> "COINGLASS_RPM_NOT_CONFIGURED";
                case INVALID_RPM -> "COINGLASS_RPM_INVALID";
                case CONFIGURED -> throw new IllegalStateException("unreachable");
            };
            return unavailable(key, traceId, UnifiedSourceStatus.NOT_CONFIGURED, reason);
        }
        ProviderInstrumentCapability capability = authorize(symbol, true);
        if (capability != null && !capability.usableFor(timeframe)) {
            return unavailable(key, traceId, status(capability.capabilityState()), reason(capability));
        }
        Duration minimum = Duration.ofSeconds(Math.max(1, properties.getEmergencyMinRefreshGapSeconds()));
        Duration freshTtl = requestedFreshTtl == null || requestedFreshTtl.compareTo(minimum) < 0
                ? minimum : requestedFreshTtl;
        if (runtimeHealth != null) runtimeHealth.noteRefreshCadence(capabilityId(), symbol, freshTtl);
        Duration staleTtl = Duration.ofSeconds(Math.max(1, properties.getStaleTtlSeconds()));
        Duration readWindow = properties.readFreshnessWindow(freshTtl);
        if (staleTtl.compareTo(readWindow) < 0) staleTtl = readWindow;
        Duration timeout = Duration.ofMillis(Math.max(1, properties.getRequestTimeoutMs()));
        Supplier<ProviderAdapterResponse<T>> observedCall = () -> {
            ProviderAdapterResponse<T> response = call.get();
            if (capabilityRegistry != null) capabilityRegistry.record(key, response, traceId);
            return response;
        };
        return coordinator.execute(new ProviderCallRequest<>(key, priority, freshTtl, staleTtl, timeout,
                traceId, Math.max(0, properties.getMaxRetry5xx()),
                Math.max(0, properties.getMaxRetryTimeout()), observedCall));
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
        ProviderInstrumentCapability capability = authorize(symbol, false);
        if (capability != null && !capability.usableFor(timeframe)) {
            return unavailable(key, traceId, status(capability.capabilityState()), reason(capability));
        }
        Duration minimum = Duration.ofSeconds(Math.max(1, properties.getEmergencyMinRefreshGapSeconds()));
        Duration freshTtl = requestedFreshTtl == null || requestedFreshTtl.compareTo(minimum) < 0
                ? minimum : requestedFreshTtl;
        Duration cadence = runtimeHealth == null ? freshTtl : runtimeHealth.refreshCadence(capabilityId(), symbol);
        if (cadence.compareTo(freshTtl) < 0) cadence = freshTtl;
        // Read coverage tolerates scan jitter; execute above still uses the original cadence.
        return coordinator.peek(key, priority, properties.readFreshnessWindow(cadence), traceId);
    }

    private String capabilityId() {
        return switch (datasetType) {
            case COINGLASS_OPEN_INTEREST -> CoinGlassV4ResponseValidator.OI_CAPABILITY;
            case COINGLASS_FUNDING -> CoinGlassV4ResponseValidator.FUNDING_CAPABILITY;
            case COINGLASS_LIQUIDATION -> CoinGlassV4ResponseValidator.LIQUIDATION_CAPABILITY;
            case COINGLASS_LONG_SHORT_RATIO -> CoinGlassV4ResponseValidator.LONG_SHORT_CAPABILITY;
            default -> throw new IllegalStateException("COINGLASS_COMPONENT_CAPABILITY_REQUIRED");
        };
    }

    private ProviderInstrumentCapability authorize(String symbol, boolean externalRefresh) {
        if (capabilityRegistry == null) return null;
        return externalRefresh
                ? capabilityRegistry.authorize(PROVIDER, symbol, timeframe,
                MarketType.PERPETUAL, ContractType.LINEAR, datasetType)
                : capabilityRegistry.inspect(PROVIDER, symbol, timeframe,
                MarketType.PERPETUAL, ContractType.LINEAR, datasetType);
    }

    private static UnifiedSourceStatus status(ProviderCapabilityState state) {
        return switch (state) {
            case PROVIDER_DISABLED -> UnifiedSourceStatus.DISABLED;
            case STALE_CAPABILITY -> UnifiedSourceStatus.STALE;
            case SOURCE_UNAVAILABLE, REGION_RESTRICTED -> UnifiedSourceStatus.ERROR;
            case SUPPORTED -> UnifiedSourceStatus.ERROR;
            default -> UnifiedSourceStatus.NOT_CONFIGURED;
        };
    }

    private static String reason(ProviderInstrumentCapability capability) {
        return capability.failureReason() == null
                ? capability.capabilityState().name() : capability.failureReason();
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
