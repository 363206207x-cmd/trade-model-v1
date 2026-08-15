package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallRequest;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRequestKey;
import org.example.trademodel.providercall.ProviderRequestKeyFactory;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.instrument.ProviderInstrumentCapability;
import org.example.trademodel.market.client.impl.RoutedPublicOhlcvProvider;
import org.example.trademodel.service.PersistedOhlcvIngestionService;
import org.example.trademodel.service.PublicOhlcvProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class CoordinatedOhlcvSnapshotService {
    private final ProviderCallCoordinator coordinator;
    private final PublicOhlcvProvider provider;
    private final PersistedOhlcvIngestionService authoritativeWriter;
    private final ProviderSymbolMappingRegistry mappingRegistry;
    private final ProviderRequestKeyFactory keyFactory;
    private final ProviderCapabilityRegistry capabilityRegistry;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CoordinatedOhlcvSnapshotService(ProviderCallCoordinator coordinator,
                                           PublicOhlcvProvider provider,
                                           PersistedOhlcvIngestionService authoritativeWriter,
                                           ProviderSymbolMappingRegistry mappingRegistry,
                                           ProviderRequestKeyFactory keyFactory,
                                           ProviderCapabilityRegistry capabilityRegistry) {
        this(coordinator, provider, authoritativeWriter, mappingRegistry, keyFactory,
                capabilityRegistry, Clock.systemUTC());
    }

    public CoordinatedOhlcvSnapshotService(ProviderCallCoordinator coordinator,
                                           PublicOhlcvProvider provider,
                                           PersistedOhlcvIngestionService authoritativeWriter,
                                           ProviderSymbolMappingRegistry mappingRegistry,
                                           ProviderRequestKeyFactory keyFactory,
                                           Clock clock) {
        this(coordinator, provider, authoritativeWriter, mappingRegistry, keyFactory, null, clock);
    }

    public CoordinatedOhlcvSnapshotService(ProviderCallCoordinator coordinator,
                                           PublicOhlcvProvider provider,
                                           PersistedOhlcvIngestionService authoritativeWriter,
                                           ProviderSymbolMappingRegistry mappingRegistry,
                                           ProviderRequestKeyFactory keyFactory,
                                           ProviderCapabilityRegistry capabilityRegistry,
                                           Clock clock) {
        this.coordinator = coordinator;
        this.provider = provider;
        this.authoritativeWriter = authoritativeWriter;
        this.mappingRegistry = mappingRegistry;
        this.keyFactory = keyFactory;
        this.capabilityRegistry = capabilityRegistry;
        this.clock = clock;
    }

    public ProviderCallResult<OhlcvIngestionResult> refresh(
            String symbol,
            String timeframe,
            int limit,
            AssetPriority priority,
            String traceId) {
        ProviderInstrumentCapability capability = authorize(symbol, timeframe);
        if (capability != null && !capability.usableFor(timeframe)) {
            return unavailable(capability, timeframe, traceId, clock.instant());
        }
        ProviderSymbolMapping mapping = capability == null
                ? mappingRegistry.resolve("BINANCE", symbol, MarketType.SPOT) : capability.mapping();
        return refresh(mapping, timeframe, limit, priority, traceId);
    }

    public ProviderCallResult<OhlcvIngestionResult> refresh(
            CanonicalInstrumentId canonicalInstrumentId,
            String timeframe,
            int limit,
            AssetPriority priority,
            String traceId) {
        ProviderInstrumentCapability capability = authorize(canonicalInstrumentId.displaySymbol(), timeframe);
        if (capability != null && !capability.usableFor(timeframe)) {
            return unavailable(capability, timeframe, traceId, clock.instant());
        }
        ProviderSymbolMapping mapping = capability == null
                ? mappingRegistry.resolve("BINANCE", canonicalInstrumentId) : capability.mapping();
        return refresh(mapping, timeframe, limit, priority, traceId);
    }

    private ProviderCallResult<OhlcvIngestionResult> refresh(
            ProviderSymbolMapping mapping,
            String timeframe,
            int limit,
            AssetPriority priority,
            String traceId) {
        Instant now = clock.instant();
        if (mapping.canonicalInstrumentId().marketType() != MarketType.SPOT) {
            return unavailable(mapping, timeframe, traceId, now,
                    "PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED");
        }
        ProviderRequestKey key = keyFactory.create(mapping.provider(), ProviderDatasetType.OHLCV,
                mapping, timeframe, Duration.ofSeconds(60), now);
        return coordinator.execute(new ProviderCallRequest<>(key, priority, Duration.ofSeconds(60),
                Duration.ofMinutes(10), Duration.ofSeconds(5), traceId,
                () -> fetchAndPersist(capabilityRegistry == null ? mapping.providerSymbol()
                        : mapping.canonicalInstrumentId().displaySymbol(), timeframe, limit, traceId)));
    }

    private ProviderCallResult<OhlcvIngestionResult> unavailable(
            ProviderSymbolMapping mapping,
            String timeframe,
            String traceId,
            Instant now,
            String reason) {
        ProviderRequestKey key = keyFactory.create(mapping.provider(), ProviderDatasetType.OHLCV,
                mapping, timeframe, Duration.ofSeconds(60), now);
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata(mapping.provider(), ProviderDatasetType.OHLCV,
                mapping.canonicalInstrumentId(), mapping.providerSymbol(), timeframe, null, now, now, 0L,
                UnifiedSourceStatus.NOT_CONFIGURED, SnapshotFreshnessStatus.UNAVAILABLE, traceId,
                key.canonical(), mapping.sourceVersion(), false, false, reason, java.util.List.of(reason));
        return new ProviderCallResult<>(null, metadata, null);
    }

    private ProviderCallResult<OhlcvIngestionResult> unavailable(
            ProviderInstrumentCapability capability,
            String timeframe,
            String traceId,
            Instant now) {
        String reason = capability.failureReason() == null
                ? capability.capabilityState().name() : capability.failureReason();
        UnifiedSourceStatus status = switch (capability.capabilityState()) {
            case PROVIDER_DISABLED -> UnifiedSourceStatus.DISABLED;
            case STALE_CAPABILITY -> UnifiedSourceStatus.STALE;
            case SOURCE_UNAVAILABLE, REGION_RESTRICTED -> UnifiedSourceStatus.ERROR;
            case SUPPORTED -> UnifiedSourceStatus.ERROR;
            default -> UnifiedSourceStatus.NOT_CONFIGURED;
        };
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata(capability.provider(),
                ProviderDatasetType.OHLCV, capability.canonicalInstrumentId(),
                capability.providerSymbol() == null ? "UNMAPPED" : capability.providerSymbol(), timeframe,
                null, now, now, 0L, status, SnapshotFreshnessStatus.UNAVAILABLE, traceId,
                capability.provider() + "|OHLCV|" + capability.canonicalAssetId() + "|" + timeframe,
                capability.sourceVersion(), false, false, reason, java.util.List.of(reason));
        return new ProviderCallResult<>(null, metadata, null);
    }

    private ProviderInstrumentCapability authorize(String symbol, String timeframe) {
        if (capabilityRegistry == null) return null;
        if (provider instanceof RoutedPublicOhlcvProvider routed) {
            return routed.preferredCapability(symbol, timeframe);
        }
        return capabilityRegistry.authorize("BINANCE", symbol, timeframe,
                MarketType.SPOT, ContractType.NONE, ProviderDatasetType.OHLCV);
    }

    private ProviderAdapterResponse<OhlcvIngestionResult> fetchAndPersist(
            String symbol, String timeframe, int limit, String traceId) {
        PublicOhlcvProviderResult fetched = provider.fetchClosedBars(symbol, timeframe, limit, traceId);
        if (fetched == null || fetched.sourceState() != OhlcvSourceState.READY || fetched.batch() == null) {
            OhlcvSourceState state = fetched == null ? OhlcvSourceState.ERROR : fetched.sourceState();
            String reason = fetched == null ? "OHLCV_PROVIDER_RESULT_MISSING" : fetched.reasonCode();
            return ProviderAdapterResponse.failed(map(state), 0, reason, null);
        }
        OhlcvIngestionResult persisted = authoritativeWriter.ingest(fetched.batch());
        if (persisted == null || !persisted.ready()) {
            String reason = persisted == null || persisted.reasonCodes().isEmpty()
                    ? "OHLCV_AUTHORITATIVE_WRITE_FAILED" : persisted.reasonCodes().get(0);
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 0, reason, null);
        }
        return ProviderAdapterResponse.ready(persisted, fetched.batch().fetchTime());
    }

    private static UnifiedSourceStatus map(OhlcvSourceState state) {
        if (state == null) return UnifiedSourceStatus.ERROR;
        return UnifiedSourceStatus.valueOf(state.name());
    }
}
