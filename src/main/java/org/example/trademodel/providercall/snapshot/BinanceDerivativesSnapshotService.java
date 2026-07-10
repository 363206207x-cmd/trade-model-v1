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
import org.example.trademodel.providercall.UnifiedSourceStatus;
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
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public BinanceDerivativesSnapshotService(ProviderCallCoordinator coordinator,
                                              PerpFundingRateClient fundingClient,
                                              OpenInterestClient openInterestClient) {
        this(coordinator, fundingClient, openInterestClient, Clock.systemUTC());
    }

    public BinanceDerivativesSnapshotService(ProviderCallCoordinator coordinator,
                                              PerpFundingRateClient fundingClient,
                                              OpenInterestClient openInterestClient,
                                              Clock clock) {
        this.coordinator = coordinator;
        this.fundingClient = fundingClient;
        this.openInterestClient = openInterestClient;
        this.clock = clock;
    }

    public ProviderCallResult<MinimalDerivativesSnapshot> get(String symbol, AssetPriority priority,
                                                               Duration freshTtl, String traceId) {
        Instant now = clock.instant();
        long bucket = Math.max(1, freshTtl.toSeconds());
        ProviderRequestKey key = new ProviderRequestKey("BINANCE_USDM_MINIMAL", ProviderDatasetType.DERIVATIVES,
                symbol, "GLOBAL", String.valueOf(now.getEpochSecond() / bucket));
        ProviderCallResult<MinimalDerivativesSnapshot> result = coordinator.execute(new ProviderCallRequest<>(key,
                priority, freshTtl, freshTtl.multipliedBy(4), Duration.ofSeconds(3), traceId,
                () -> fetch(symbol)));
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
}
