package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.dto.ohlcv.PublicProviderHealthSnapshot;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityState;
import org.example.trademodel.providercall.instrument.ProviderInstrumentCapability;
import org.example.trademodel.service.PublicOhlcvProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
public class RoutedPublicOhlcvProvider implements PublicOhlcvProvider {
    private static final Set<String> KRAKEN_FALLBACK_REASONS = Set.of(
            "TIMEOUT", "DNS_FAILURE", "HTTP_5XX", "PROVIDER_UNAVAILABLE", "RATE_LIMITED",
            "PAIR_NOT_SUPPORTED", "REGION_RESTRICTED");
    private final KrakenPublicOhlcvProvider kraken;
    private final BinancePublicOhlcvProvider binance;
    private final String primary;
    private final String fallback;
    private final boolean fallbackEnabled;
    private final ProviderCapabilityRegistry capabilityRegistry;
    private final Map<String, MutableHealth> health = new ConcurrentHashMap<>();

    @Autowired
    public RoutedPublicOhlcvProvider(KrakenPublicOhlcvProvider kraken,
                                     BinancePublicOhlcvProvider binance,
                                     @Value("${trade-model.ohlcv.provider.primary:kraken}") String primary,
                                     @Value("${trade-model.ohlcv.provider.fallback:binance}") String fallback,
                                     @Value("${trade-model.ohlcv.provider.fallback-enabled:true}") boolean fallbackEnabled,
                                     ProviderCapabilityRegistry capabilityRegistry) {
        this.kraken = kraken;
        this.binance = binance;
        this.primary = normalizeProvider(primary);
        this.fallback = normalizeProvider(fallback);
        this.fallbackEnabled = fallbackEnabled;
        this.capabilityRegistry = capabilityRegistry;
        health.put("KRAKEN", new MutableHealth("KRAKEN"));
        health.put("BINANCE", new MutableHealth("BINANCE"));
    }

    RoutedPublicOhlcvProvider(KrakenPublicOhlcvProvider kraken,
                              BinancePublicOhlcvProvider binance,
                              String primary,
                              String fallback,
                              boolean fallbackEnabled) {
        this(kraken, binance, primary, fallback, fallbackEnabled, null);
    }

    @Override
    public PublicOhlcvProviderResult fetchClosedBars(String symbol, String timeframe, int limit,
                                                     String ingestionRunId) {
        if (capabilityRegistry == null) {
            return legacyFetch(symbol, timeframe, limit, ingestionRunId);
        }
        ProviderInstrumentCapability primaryCapability = authorize(primary, symbol, timeframe);
        PublicOhlcvProviderResult primaryResult = primaryCapability.usableFor(timeframe)
                ? call(primaryCapability, symbol, timeframe, limit, ingestionRunId)
                : blocked(primaryCapability);
        if (primaryCapability.usableFor(timeframe)) {
            record(primaryCapability, symbol, timeframe, primaryResult);
        }
        if (ready(primaryResult) || !fallbackEnabled || primary.equals(fallback)
                || (primaryCapability.usableFor(timeframe) && !fallbackAllowed(primary, primaryResult))) {
            return primaryResult;
        }

        ProviderInstrumentCapability fallbackCapability = authorize(fallback, symbol, timeframe);
        if (!fallbackCapability.usableFor(timeframe)) {
            return primaryCapability.usableFor(timeframe) ? primaryResult : blocked(fallbackCapability);
        }
        PublicOhlcvProviderResult fallbackResult = call(
                fallbackCapability, symbol, timeframe, limit, ingestionRunId);
        record(fallbackCapability, symbol, timeframe, fallbackResult);
        return fallbackResult;
    }

    public ProviderInstrumentCapability authorize(String provider, String symbol, String timeframe) {
        if (capabilityRegistry == null) return null;
        return capabilityRegistry.authorize(provider, symbol, timeframe, MarketType.SPOT, ContractType.NONE);
    }

    public ProviderInstrumentCapability preferredCapability(String symbol, String timeframe) {
        ProviderInstrumentCapability first = authorize(primary, symbol, timeframe);
        if (first != null && first.usableFor(timeframe)) return first;
        if (!fallbackEnabled || primary.equals(fallback)) return first;
        ProviderInstrumentCapability second = authorize(fallback, symbol, timeframe);
        return second != null && second.usableFor(timeframe) ? second : first;
    }

    public String primaryProvider() {
        return primary;
    }

    public Map<String, PublicProviderHealthSnapshot> health() {
        Map<String, PublicProviderHealthSnapshot> snapshots = new LinkedHashMap<>();
        snapshots.put("kraken", health.get("KRAKEN").snapshot(false));
        snapshots.put("binance", health.get("BINANCE").snapshot(binance.isGeoRestrictedCircuitOpen()));
        return snapshots;
    }

    public String requestPair(String symbol) {
        return kraken.cachedRequestPair(symbol);
    }

    public KrakenPairCacheState krakenPairCacheState() {
        return kraken.pairCacheState();
    }

    private PublicOhlcvProviderResult call(ProviderInstrumentCapability capability,
                                          String canonicalSymbol,
                                          String timeframe,
                                          int limit,
                                          String runId) {
        String provider = normalizeProvider(capability.provider());
        String exactProviderSymbol = capability.providerSymbol();
        return switch (provider) {
            case "KRAKEN" -> kraken.fetchClosedBars(normalizeCanonicalSymbol(canonicalSymbol), timeframe, limit, runId);
            case "BINANCE" -> binance.fetchClosedBars(exactProviderSymbol, timeframe, limit, runId);
            default -> new PublicOhlcvProviderResult(OhlcvSourceState.ERROR, "PROVIDER_UNAVAILABLE", null);
        };
    }

    private void record(ProviderInstrumentCapability capability,
                        String symbol,
                        String timeframe,
                        PublicOhlcvProviderResult result) {
        String provider = normalizeProvider(capability.provider());
        MutableHealth item = health.computeIfAbsent(provider, MutableHealth::new);
        if (ready(result)) item.success();
        else item.failure(result == null ? "PROVIDER_UNAVAILABLE" : result.reasonCode());
        if (capabilityRegistry != null) {
            String sourceVersion = result != null && result.batch() != null
                    ? result.batch().provenanceVersion() : capability.sourceVersion();
            capabilityRegistry.recordOhlcv(provider, symbol, timeframe, capability.providerSymbol(), sourceVersion, result);
        }
    }

    private PublicOhlcvProviderResult legacyFetch(String symbol, String timeframe, int limit, String ingestionRunId) {
        PublicOhlcvProviderResult primaryResult = legacyCall(primary, symbol, timeframe, limit, ingestionRunId);
        if (ready(primaryResult) || !fallbackEnabled || primary.equals(fallback)
                || !fallbackAllowed(primary, primaryResult)) return primaryResult;
        return legacyCall(fallback, symbol, timeframe, limit, ingestionRunId);
    }

    private PublicOhlcvProviderResult legacyCall(String provider, String symbol,
                                                 String timeframe, int limit, String runId) {
        return switch (provider) {
            case "KRAKEN" -> kraken.fetchClosedBars(symbol, timeframe, limit, runId);
            case "BINANCE" -> binance.fetchClosedBars(symbol, timeframe, limit, runId);
            default -> new PublicOhlcvProviderResult(OhlcvSourceState.ERROR, "PROVIDER_UNAVAILABLE", null);
        };
    }

    private static PublicOhlcvProviderResult blocked(ProviderInstrumentCapability capability) {
        if (capability == null) {
            return new PublicOhlcvProviderResult(OhlcvSourceState.ERROR, "PROVIDER_UNAVAILABLE", null);
        }
        OhlcvSourceState state = switch (capability.capabilityState()) {
            case PROVIDER_DISABLED -> OhlcvSourceState.DISABLED;
            case NOT_CONFIGURED -> OhlcvSourceState.NOT_CONFIGURED;
            case STALE_CAPABILITY -> OhlcvSourceState.STALE;
            case UNSUPPORTED_SYMBOL, UNSUPPORTED_TIMEFRAME -> OhlcvSourceState.DEGRADED;
            case REGION_RESTRICTED, SOURCE_UNAVAILABLE -> OhlcvSourceState.ERROR;
            case SUPPORTED -> OhlcvSourceState.ERROR;
        };
        String reason = capability.failureReason() == null
                ? capability.capabilityState().name() : capability.failureReason();
        return new PublicOhlcvProviderResult(state, reason, null);
    }

    private static boolean ready(PublicOhlcvProviderResult result) {
        return result != null && result.sourceState() == OhlcvSourceState.READY && result.batch() != null;
    }

    private static boolean fallbackAllowed(String provider, PublicOhlcvProviderResult result) {
        if (!"KRAKEN".equals(provider)) return true;
        String reason = result == null ? "PROVIDER_UNAVAILABLE" : result.reasonCode();
        return KRAKEN_FALLBACK_REASONS.contains(reason);
    }

    private static String normalizeProvider(String value) {
        return value == null ? "KRAKEN" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeCanonicalSymbol(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT)
                .replace("/", "").replace("-", "").replace("_", "");
    }

    private static final class MutableHealth {
        private final String provider;
        private volatile Instant lastSuccessAt;
        private volatile Instant lastFailureAt;
        private volatile String lastFailureCode;

        private MutableHealth(String provider) {
            this.provider = provider;
        }

        void success() {
            lastSuccessAt = Instant.now();
            lastFailureCode = null;
        }

        void failure(String reason) {
            lastFailureAt = Instant.now();
            lastFailureCode = reason;
        }

        PublicProviderHealthSnapshot snapshot(boolean circuitOpen) {
            String status = circuitOpen ? "REGION_RESTRICTED"
                    : lastSuccessAt != null && lastFailureCode == null ? "UP"
                    : lastFailureAt != null ? "DEGRADED" : "NOT_USED";
            return new PublicProviderHealthSnapshot(provider, status, lastSuccessAt,
                    lastFailureAt, circuitOpen, circuitOpen ? "HTTP_451" : lastFailureCode);
        }
    }
}
