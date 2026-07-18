package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.CoordinatedProviderSnapshotQueryService;
import org.example.trademodel.providercall.CoordinatedProviderSnapshotRefreshService;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallRequest;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRequestKey;
import org.example.trademodel.providercall.ProviderRequestKeyFactory;
import org.example.trademodel.providercall.ProviderSnapshotQueryService;
import org.example.trademodel.providercall.ProviderSnapshotRefreshService;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class MarketPriceSnapshotService {
    private final ProviderSnapshotQueryService queryService;
    private final ProviderSnapshotRefreshService refreshService;
    private final MarketQuoteClient marketQuoteClient;
    private final ProviderSymbolMappingRegistry mappingRegistry;
    private final ProviderRequestKeyFactory keyFactory;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public MarketPriceSnapshotService(ProviderSnapshotQueryService queryService,
                                      ProviderSnapshotRefreshService refreshService,
                                      MarketQuoteClient marketQuoteClient,
                                      ProviderSymbolMappingRegistry mappingRegistry,
                                      ProviderRequestKeyFactory keyFactory) {
        this(queryService, refreshService, marketQuoteClient, mappingRegistry, keyFactory, Clock.systemUTC());
    }

    /** Compatibility constructor for focused unit tests. */
    public MarketPriceSnapshotService(
            ProviderCallCoordinator coordinator,
            MarketQuoteClient marketQuoteClient,
            ProviderSymbolMappingRegistry mappingRegistry,
            ProviderRequestKeyFactory keyFactory,
            Clock clock) {
        this(new CoordinatedProviderSnapshotQueryService(coordinator),
                new CoordinatedProviderSnapshotRefreshService(coordinator), marketQuoteClient,
                mappingRegistry, keyFactory, clock);
    }

    public MarketPriceSnapshotService(
            ProviderSnapshotQueryService queryService,
            ProviderSnapshotRefreshService refreshService,
            MarketQuoteClient marketQuoteClient,
            ProviderSymbolMappingRegistry mappingRegistry,
            ProviderRequestKeyFactory keyFactory,
            Clock clock) {
        this.queryService = queryService;
        this.refreshService = refreshService;
        this.marketQuoteClient = marketQuoteClient;
        this.mappingRegistry = mappingRegistry;
        this.keyFactory = keyFactory;
        this.clock = clock;
    }

    public ProviderCallResult<MarketPriceSnapshot> get(
            String symbol,
            AssetPriority priority,
            Duration freshTtl,
            String traceId) {
        ProviderSymbolMapping mapping = mappingRegistry.resolve("BINANCE", symbol, MarketType.SPOT);
        ProviderRequestKey key = keyFactory.create("BINANCE", ProviderDatasetType.PRICE,
                mapping, "GLOBAL", freshTtl, clock.instant());
        ProviderCallResult<MarketPriceSnapshot> result = refreshService.refresh(new ProviderCallRequest<>(key, priority, freshTtl,
                Duration.ofMinutes(2), Duration.ofSeconds(3), traceId,
                () -> fetch(mapping.providerSymbol())));
        if (result.payload() == null) return result;
        MarketPriceSnapshot enriched = new MarketPriceSnapshot(result.payload().symbol(), result.payload().lastPrice(),
                result.payload().bidPrice(), result.payload().askPrice(), result.payload().spread(),
                result.payload().highPrice24h(), result.payload().lowPrice24h(),
                result.payload().priceChangePercent24h(), result.payload().sourceProvider(),
                result.payload().sourceFetchedAt(), result.metadata());
        return new ProviderCallResult<>(enriched, result.metadata(), result.budgetState());
    }

    public ProviderCallResult<MarketPriceSnapshot> peek(
            String symbol,
            AssetPriority priority,
            Duration freshTtl,
            String traceId) {
        ProviderSymbolMapping mapping = mappingRegistry.resolve("BINANCE", symbol, MarketType.SPOT);
        ProviderRequestKey key = keyFactory.create("BINANCE", ProviderDatasetType.PRICE,
                mapping, "GLOBAL", freshTtl, clock.instant());
        ProviderCallResult<MarketPriceSnapshot> result = queryService.query(key, priority, freshTtl, traceId);
        if (result.payload() == null) return result;
        MarketPriceSnapshot payload = result.payload();
        return new ProviderCallResult<>(new MarketPriceSnapshot(payload.symbol(), payload.lastPrice(),
                payload.bidPrice(), payload.askPrice(), payload.spread(), payload.highPrice24h(),
                payload.lowPrice24h(), payload.priceChangePercent24h(), payload.sourceProvider(),
                payload.sourceFetchedAt(), result.metadata()), result.metadata(), result.budgetState());
    }

    private ProviderAdapterResponse<MarketPriceSnapshot> fetch(String symbol) {
        Optional<MarketQuoteSnapshot> quote = marketQuoteClient.fetch24hTicker(symbol);
        if (quote.isEmpty()) {
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0, "QUOTE_UNAVAILABLE", null);
        }
        if (quote.get().getLastPrice() == null) {
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0, "QUOTE_UNAVAILABLE", null);
        }
        if (!positive(quote.get().getLastPrice())) {
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0, "INVALID_MARKET_PRICE", null);
        }
        MarketQuoteSnapshot raw = quote.get();
        Instant fetchedAt = raw.getFetchedAtEpochMillis() > 0
                ? Instant.ofEpochMilli(raw.getFetchedAtEpochMillis()) : clock.instant();
        MarketPriceSnapshot snapshot = new MarketPriceSnapshot(raw.getSymbolNormalized(), raw.getLastPrice(),
                null, null, null, raw.getHighPrice(), raw.getLowPrice(), raw.getPriceChangePercent24h(),
                raw.getProvider(), fetchedAt, null);
        return ProviderAdapterResponse.ready(snapshot, fetchedAt);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

}
