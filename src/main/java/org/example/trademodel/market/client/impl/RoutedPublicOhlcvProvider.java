package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.dto.ohlcv.PublicProviderHealthSnapshot;
import org.example.trademodel.service.PublicOhlcvProvider;
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
            "PAIR_NOT_SUPPORTED");
    private final KrakenPublicOhlcvProvider kraken;
    private final BinancePublicOhlcvProvider binance;
    private final String primary;
    private final String fallback;
    private final boolean fallbackEnabled;
    private final Map<String, MutableHealth> health = new ConcurrentHashMap<>();

    public RoutedPublicOhlcvProvider(KrakenPublicOhlcvProvider kraken,
                                     BinancePublicOhlcvProvider binance,
                                     @Value("${trade-model.ohlcv.provider.primary:kraken}") String primary,
                                     @Value("${trade-model.ohlcv.provider.fallback:binance}") String fallback,
                                     @Value("${trade-model.ohlcv.provider.fallback-enabled:true}") boolean fallbackEnabled) {
        this.kraken = kraken;
        this.binance = binance;
        this.primary = normalizeProvider(primary);
        this.fallback = normalizeProvider(fallback);
        this.fallbackEnabled = fallbackEnabled;
        health.put("KRAKEN", new MutableHealth("KRAKEN"));
        health.put("BINANCE", new MutableHealth("BINANCE"));
    }

    @Override
    public PublicOhlcvProviderResult fetchClosedBars(String symbol, String timeframe, int limit,
                                                     String ingestionRunId) {
        PublicOhlcvProviderResult primaryResult = call(primary, symbol, timeframe, limit, ingestionRunId);
        record(primary, primaryResult);
        if (ready(primaryResult) || !fallbackEnabled || primary.equals(fallback)
                || !fallbackAllowed(primary, primaryResult)) return primaryResult;

        PublicOhlcvProviderResult fallbackResult = call(fallback, symbol, timeframe, limit, ingestionRunId);
        record(fallback, fallbackResult);
        if ("PAIR_NOT_SUPPORTED".equals(primaryResult.reasonCode())
                && ("GEO_RESTRICTED".equals(fallbackResult.reasonCode())
                || "PROVIDER_UNAVAILABLE_FOR_LOCATION".equals(fallbackResult.reasonCode()))) {
            return new PublicOhlcvProviderResult(OhlcvSourceState.ERROR,
                    "PAIR_NOT_SUPPORTED_OR_GEO_RESTRICTED", null);
        }
        return fallbackResult;
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

    private PublicOhlcvProviderResult call(String provider, String symbol, String timeframe, int limit, String runId) {
        return switch (provider) {
            case "KRAKEN" -> kraken.fetchClosedBars(symbol, timeframe, limit, runId);
            case "BINANCE" -> binance.fetchClosedBars(symbol, timeframe, limit, runId);
            default -> new PublicOhlcvProviderResult(OhlcvSourceState.ERROR, "PROVIDER_UNAVAILABLE", null);
        };
    }

    private void record(String provider, PublicOhlcvProviderResult result) {
        MutableHealth item = health.computeIfAbsent(provider, MutableHealth::new);
        if (ready(result)) item.success();
        else item.failure(result == null ? "PROVIDER_UNAVAILABLE" : result.reasonCode());
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
            String status = circuitOpen ? "GEO_RESTRICTED"
                    : lastSuccessAt != null && lastFailureCode == null ? "UP"
                    : lastFailureAt != null ? "DEGRADED" : "NOT_USED";
            return new PublicProviderHealthSnapshot(provider, status, lastSuccessAt,
                    lastFailureAt, circuitOpen, circuitOpen ? "HTTP_451" : lastFailureCode);
        }
    }
}
