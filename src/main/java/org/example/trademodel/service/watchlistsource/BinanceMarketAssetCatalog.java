package org.example.trademodel.service.watchlistsource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityDirectory;
import org.example.trademodel.providercall.instrument.ProviderCapabilityState;
import org.example.trademodel.providercall.instrument.ProviderInstrumentCapability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BinanceMarketAssetCatalog implements MarketAssetCatalog, ProviderCapabilityDirectory {
    private static final URI EXCHANGE_INFO = URI.create("https://api.binance.com/api/v3/exchangeInfo");
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final Map<String, List<String>> IDENTITY_ALIASES = Map.ofEntries(
            Map.entry("BTC", List.of("BITCOIN", "XBT", "比特币")),
            Map.entry("ETH", List.of("ETHEREUM", "ETHER", "以太坊")),
            Map.entry("SOL", List.of("SOLANA")),
            Map.entry("BNB", List.of("BINANCECOIN")),
            Map.entry("XRP", List.of("RIPPLE")),
            Map.entry("ADA", List.of("CARDANO")),
            Map.entry("DOGE", List.of("DOGECOIN")),
            Map.entry("LINK", List.of("CHAINLINK")),
            Map.entry("AAVE", List.of("AAVE")),
            Map.entry("TAO", List.of("BITTENSOR")),
            Map.entry("SUI", List.of("SUI")),
            Map.entry("ARB", List.of("ARBITRUM"))
    );

    private final ObjectMapper objectMapper;
    private final ProviderSymbolMappingRegistry mappingRegistry;
    private final HttpClient httpClient;
    private final boolean providerEnabled;
    private final boolean externalCallsEnabled;
    private volatile CatalogSnapshot cache;

    @Autowired
    public BinanceMarketAssetCatalog(ObjectMapper objectMapper,
                                     ProviderSymbolMappingRegistry mappingRegistry,
                                     @Value("${trade-model.ohlcv.binance.enabled:${trade-model.ohlcv.public-provider.enabled:false}}")
                                     boolean providerEnabled,
                                     @Value("${trade-model.ohlcv.binance.external-calls-enabled:${trade-model.ohlcv.public-provider.external-calls-enabled:false}}")
                                     boolean ohlcvExternalCallsEnabled,
                                     @Value("${trade-model.provider-call.enabled:false}")
                                     boolean providerCallEnabled,
                                     @Value("${trade-model.provider-call.external-calls-enabled:false}")
                                     boolean providerCallExternalCallsEnabled) {
        this(objectMapper, mappingRegistry, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build(),
                providerEnabled || providerCallEnabled,
                ohlcvExternalCallsEnabled || providerCallExternalCallsEnabled);
    }

    BinanceMarketAssetCatalog(ObjectMapper objectMapper,
                              ProviderSymbolMappingRegistry mappingRegistry,
                              HttpClient httpClient) {
        this(objectMapper, mappingRegistry, httpClient, true, true);
    }

    BinanceMarketAssetCatalog(ObjectMapper objectMapper,
                              ProviderSymbolMappingRegistry mappingRegistry,
                              HttpClient httpClient,
                              boolean providerEnabled,
                              boolean externalCallsEnabled) {
        this.objectMapper = objectMapper;
        this.mappingRegistry = mappingRegistry;
        this.httpClient = httpClient;
        this.providerEnabled = providerEnabled;
        this.externalCallsEnabled = externalCallsEnabled;
    }

    @Override
    public List<MarketAssetDTO> search(String query, int limit) {
        String normalized = normalizeLoose(query);
        return currentCatalog().stream()
                .filter(asset -> matchesIdentity(asset, normalized))
                .sorted(Comparator.comparing(MarketAssetDTO::symbol))
                .limit(Math.max(1, Math.min(100, limit)))
                .toList();
    }

    @Override
    public MarketAssetDTO requireTradable(String symbol) {
        String normalized = normalizeSymbol(symbol);
        return currentCatalog().stream()
                .filter(asset -> normalized.equals(normalizeSymbol(asset.symbol())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ASSET_NOT_FOUND_IN_MARKET_CATALOG"));
    }

    private List<MarketAssetDTO> currentCatalog() {
        CatalogSnapshot existing = cache;
        Instant now = Instant.now();
        if (existing != null && now.isBefore(existing.loadedAt().plus(CACHE_TTL))) {
            return existing.assets();
        }
        synchronized (this) {
            existing = cache;
            if (existing != null && now.isBefore(existing.loadedAt().plus(CACHE_TTL))) {
                return existing.assets();
            }
            List<MarketAssetDTO> loaded = providerEnabled && externalCallsEnabled
                    ? fetchExchangeInfo() : List.of();
            boolean directoryVerified = !loaded.isEmpty();
            if (!directoryVerified) loaded = configuredFallback();
            cache = new CatalogSnapshot(List.copyOf(loaded), now, directoryVerified);
            return cache.assets();
        }
    }

    @Override
    public String provider() {
        return "BINANCE";
    }

    @Override
    public ProviderInstrumentCapability verify(CanonicalInstrumentId requested,
                                               String timeframe,
                                               Instant observedAt) {
        Instant now = observedAt == null ? Instant.now() : observedAt;
        CanonicalInstrumentId exact = requested == null ? null : requested.withVenue("BINANCE");
        if (exact == null || exact.marketType() != MarketType.SPOT
                || exact.contractType() != ContractType.NONE || !"USDT".equals(exact.quoteAsset())) {
            return capability(exact, timeframe, null, ProviderCapabilityState.UNSUPPORTED_SYMBOL,
                    "BINANCE_EXACT_SPOT_USDT_REQUIRED", now, now);
        }
        List<MarketAssetDTO> assets = currentCatalog();
        CatalogSnapshot snapshot = cache;
        if (snapshot == null || !snapshot.directoryVerified()) {
            return capability(exact, timeframe, null, ProviderCapabilityState.SOURCE_UNAVAILABLE,
                    "BINANCE_EXCHANGE_INFO_UNAVAILABLE", now, null);
        }
        String symbol = exact.baseAsset() + exact.quoteAsset();
        MarketAssetDTO match = assets.stream().filter(value -> symbol.equals(normalizeSymbol(value.symbol())))
                .findFirst().orElse(null);
        if (match == null) {
            return capability(exact, timeframe, null, ProviderCapabilityState.UNSUPPORTED_SYMBOL,
                    "BINANCE_SYMBOL_NOT_TRADABLE", now, now);
        }
        if (!"GLOBAL".equals(timeframe) && !List.of("5m", "15m", "1h", "4h").contains(timeframe)) {
            return capability(exact, timeframe, null, ProviderCapabilityState.UNSUPPORTED_TIMEFRAME,
                    "UNSUPPORTED_TIMEFRAME", now, now);
        }
        return capability(exact, timeframe, symbol, ProviderCapabilityState.SUPPORTED, null, now, now);
    }

    private static ProviderInstrumentCapability capability(CanonicalInstrumentId instrument,
                                                           String timeframe,
                                                           String providerSymbol,
                                                           ProviderCapabilityState state,
                                                           String reason,
                                                           Instant observedAt,
                                                           Instant verifiedAt) {
        CanonicalInstrumentId exact = instrument == null
                ? new CanonicalInstrumentId("UNKNOWN", "USDT", MarketType.SPOT, "BINANCE", ContractType.NONE)
                : instrument;
        return new ProviderInstrumentCapability("BINANCE", exact.canonical(), exact.baseAsset(), exact.quoteAsset(),
                exact.marketType(), exact.contractType(), providerSymbol, List.of("5m", "15m", "1h", "4h"),
                state, "BINANCE_SPOT_EXCHANGE_INFO_RUNTIME_V1", verifiedAt, reason, observedAt);
    }

    private List<MarketAssetDTO> fetchExchangeInfo() {
        try {
            HttpRequest request = HttpRequest.newBuilder(EXCHANGE_INFO)
                    .timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return List.of();
            }
            return parseExchangeInfo(objectMapper.readTree(response.body()));
        } catch (Exception ignored) {
            return List.of();
        }
    }

    static List<MarketAssetDTO> parseExchangeInfo(JsonNode root) {
        if (root == null || !root.path("symbols").isArray()) {
            return List.of();
        }
        Map<String, MarketAssetDTO> assets = new LinkedHashMap<>();
        for (JsonNode item : root.path("symbols")) {
            if (!"TRADING".equalsIgnoreCase(item.path("status").asText())) {
                continue;
            }
            String quote = item.path("quoteAsset").asText("").toUpperCase(Locale.ROOT);
            if (!"USDT".equals(quote)) {
                continue;
            }
            String symbol = normalizeSymbol(item.path("symbol").asText());
            String base = item.path("baseAsset").asText("").toUpperCase(Locale.ROOT);
            if (!symbol.isBlank() && !base.isBlank()) {
                assets.put(symbol, new MarketAssetDTO(symbol, base, quote, "SPOT"));
            }
        }
        return new ArrayList<>(assets.values());
    }

    private List<MarketAssetDTO> configuredFallback() {
        Map<String, MarketAssetDTO> assets = new LinkedHashMap<>();
        for (ProviderSymbolMapping mapping : mappingRegistry.snapshot()) {
            String base = mapping.canonicalInstrumentId().baseAsset();
            String quote = mapping.canonicalInstrumentId().quoteAsset();
            if (!"USDT".equalsIgnoreCase(quote)) {
                continue;
            }
            String symbol = normalizeSymbol(base + quote);
            assets.putIfAbsent(symbol, new MarketAssetDTO(symbol, base, quote,
                    mapping.canonicalInstrumentId().marketType().name()));
        }
        return new ArrayList<>(assets.values());
    }

    private static String normalizeSymbol(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase(Locale.ROOT)
                .replace("/", "").replace("-", "").replace("_", "");
    }

    private static String normalizeLoose(String value) {
        return normalizeSymbol(value);
    }

    private static boolean matchesIdentity(MarketAssetDTO asset, String normalizedQuery) {
        if (normalizedQuery.isEmpty()
                || normalizeLoose(asset.symbol()).contains(normalizedQuery)
                || normalizeLoose(asset.baseAsset()).contains(normalizedQuery)) {
            return true;
        }
        return IDENTITY_ALIASES.getOrDefault(asset.baseAsset().toUpperCase(Locale.ROOT), List.of()).stream()
                .map(BinanceMarketAssetCatalog::normalizeLoose)
                .anyMatch(alias -> alias.contains(normalizedQuery));
    }

    private record CatalogSnapshot(List<MarketAssetDTO> assets, Instant loadedAt, boolean directoryVerified) {
    }
}
