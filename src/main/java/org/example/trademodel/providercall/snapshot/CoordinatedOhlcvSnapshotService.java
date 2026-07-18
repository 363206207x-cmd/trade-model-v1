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
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
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
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CoordinatedOhlcvSnapshotService(ProviderCallCoordinator coordinator,
                                           PublicOhlcvProvider provider,
                                           PersistedOhlcvIngestionService authoritativeWriter,
                                           ProviderSymbolMappingRegistry mappingRegistry,
                                           ProviderRequestKeyFactory keyFactory) {
        this(coordinator, provider, authoritativeWriter, mappingRegistry, keyFactory, Clock.systemUTC());
    }

    public CoordinatedOhlcvSnapshotService(ProviderCallCoordinator coordinator,
                                           PublicOhlcvProvider provider,
                                           PersistedOhlcvIngestionService authoritativeWriter,
                                           ProviderSymbolMappingRegistry mappingRegistry,
                                           ProviderRequestKeyFactory keyFactory,
                                           Clock clock) {
        this.coordinator = coordinator;
        this.provider = provider;
        this.authoritativeWriter = authoritativeWriter;
        this.mappingRegistry = mappingRegistry;
        this.keyFactory = keyFactory;
        this.clock = clock;
    }

    public ProviderCallResult<OhlcvIngestionResult> refresh(
            String symbol,
            String timeframe,
            int limit,
            AssetPriority priority,
            String traceId) {
        Instant now = clock.instant();
        ProviderSymbolMapping mapping = mappingRegistry.resolve("BINANCE", symbol, MarketType.SPOT);
        ProviderRequestKey key = keyFactory.create("BINANCE", ProviderDatasetType.OHLCV,
                mapping, timeframe, Duration.ofSeconds(60), now);
        return coordinator.execute(new ProviderCallRequest<>(key, priority, Duration.ofSeconds(60),
                Duration.ofMinutes(10), Duration.ofSeconds(5), traceId,
                () -> fetchAndPersist(mapping.providerSymbol(), timeframe, limit, traceId)));
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
