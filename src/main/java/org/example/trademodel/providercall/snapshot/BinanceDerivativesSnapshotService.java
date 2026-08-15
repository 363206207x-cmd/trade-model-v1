package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.market.client.OpenInterestClient;
import org.example.trademodel.market.client.PerpFundingRateClient;
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
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.instrument.ProviderInstrumentCapability;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class BinanceDerivativesSnapshotService {
    private final ProviderCallCoordinator coordinator;
    private final PerpFundingRateClient fundingClient;
    private final OpenInterestClient openInterestClient;
    private final ProviderSymbolMappingRegistry mappingRegistry;
    private final ProviderRequestKeyFactory keyFactory;
    private final ProviderCapabilityRegistry capabilityRegistry;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public BinanceDerivativesSnapshotService(ProviderCallCoordinator coordinator,
                                              PerpFundingRateClient fundingClient,
                                              OpenInterestClient openInterestClient,
                                              ProviderSymbolMappingRegistry mappingRegistry,
                                              ProviderRequestKeyFactory keyFactory,
                                              ProviderCapabilityRegistry capabilityRegistry) {
        this(coordinator, fundingClient, openInterestClient, mappingRegistry, keyFactory,
                capabilityRegistry, Clock.systemUTC());
    }

    public BinanceDerivativesSnapshotService(ProviderCallCoordinator coordinator,
                                              PerpFundingRateClient fundingClient,
                                              OpenInterestClient openInterestClient,
                                              ProviderSymbolMappingRegistry mappingRegistry,
                                              ProviderRequestKeyFactory keyFactory,
                                              Clock clock) {
        this(coordinator, fundingClient, openInterestClient, mappingRegistry, keyFactory, null, clock);
    }

    public BinanceDerivativesSnapshotService(ProviderCallCoordinator coordinator,
                                              PerpFundingRateClient fundingClient,
                                              OpenInterestClient openInterestClient,
                                              ProviderSymbolMappingRegistry mappingRegistry,
                                              ProviderRequestKeyFactory keyFactory,
                                              ProviderCapabilityRegistry capabilityRegistry,
                                              Clock clock) {
        this.coordinator = coordinator;
        this.fundingClient = fundingClient;
        this.openInterestClient = openInterestClient;
        this.mappingRegistry = mappingRegistry;
        this.keyFactory = keyFactory;
        this.capabilityRegistry = capabilityRegistry;
        this.clock = clock;
    }

    public ProviderCallResult<MinimalDerivativesSnapshot> get(String symbol, AssetPriority priority,
                                                               Duration freshTtl, String traceId) {
        ProviderInstrumentCapability capability = capabilityRegistry == null ? null
                : capabilityRegistry.authorize("BINANCE", symbol, "GLOBAL",
                MarketType.PERPETUAL, ContractType.LINEAR);
        if (capability != null && !capability.usableFor("GLOBAL")) {
            return unavailable(capability, traceId);
        }
        ProviderSymbolMapping mapping = capability == null
                ? mappingRegistry.resolve("BINANCE", symbol, MarketType.PERPETUAL)
                : capability.mapping();
        ProviderRequestKey key = keyFactory.create("BINANCE", ProviderDatasetType.DERIVATIVES,
                mapping, "GLOBAL", freshTtl, clock.instant());
        ProviderCallResult<MinimalDerivativesSnapshot> result = coordinator.execute(new ProviderCallRequest<>(key,
                priority, freshTtl, freshTtl.multipliedBy(4), Duration.ofSeconds(3), traceId,
                () -> fetch(mapping.providerSymbol())));
        if (result.payload() == null) return result;
        MinimalDerivativesSnapshot payload = result.payload();
        return new ProviderCallResult<>(new MinimalDerivativesSnapshot(payload.symbol(), payload.lastFundingRate(),
                payload.openInterest(), payload.evidenceAvailability(), result.metadata()),
                result.metadata(), result.budgetState());
    }

    private ProviderAdapterResponse<MinimalDerivativesSnapshot> fetch(String symbol) {
        Optional<BigDecimal> funding = fundingClient == null ? Optional.empty()
                : fundingClient.fetchLastFundingRate(symbol);
        Optional<BigDecimal> oi = openInterestClient == null ? Optional.empty()
                : openInterestClient.fetchOpenInterest(symbol);
        if (funding.isEmpty() && oi.isEmpty()) {
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.NOT_CONFIGURED, 0,
                    "BINANCE_DERIVATIVES_MINIMAL_UNAVAILABLE", null);
        }
        String availability = funding.isPresent() && oi.isPresent() ? "PARTIAL_BINANCE_MINIMAL"
                : funding.isPresent() ? "FUNDING_ONLY" : "OPEN_INTEREST_ONLY";
        return ProviderAdapterResponse.ready(new MinimalDerivativesSnapshot(symbol, funding.orElse(null),
                oi.orElse(null), availability, null), clock.instant());
    }

    private ProviderCallResult<MinimalDerivativesSnapshot> unavailable(ProviderInstrumentCapability capability,
                                                                       String traceId) {
        Instant now = clock.instant();
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
                ProviderDatasetType.DERIVATIVES, capability.canonicalInstrumentId(),
                capability.providerSymbol() == null ? "UNMAPPED" : capability.providerSymbol(), "GLOBAL",
                null, now, now, 0L, status, SnapshotFreshnessStatus.UNAVAILABLE, traceId,
                capability.provider() + "|DERIVATIVES|" + capability.canonicalAssetId() + "|GLOBAL",
                capability.sourceVersion(), false, false, reason, java.util.List.of(reason));
        return new ProviderCallResult<>(null, metadata, null);
    }
}
