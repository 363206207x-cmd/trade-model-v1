package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicMarketHttpResult;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class KrakenPairResolver {
    static final String ASSET_PAIRS_ENDPOINT = "/0/public/AssetPairs";
    private static final Logger log = LoggerFactory.getLogger(KrakenPairResolver.class);
    private static final Map<String, Set<String>> DISPLAY_ALIASES = Map.of(
            "BTCUSDT", Set.of("BTC/USD", "XBT/USD"),
            "ETHUSDT", Set.of("ETH/USD"),
            "SOLUSDT", Set.of("SOL/USD"),
            "BNBUSDT", Set.of("BNB/USD"),
            "XRPUSDT", Set.of("XRP/USD"),
            "DOGEUSDT", Set.of("DOGE/USD", "XDG/USD"));
    private static final Set<String> RETRYABLE = Set.of("HTTP_5XX", "TIMEOUT");
    private static final Set<String> CORE_REQUEST_PAIRS = Set.of(
            "XBTUSD", "BTCUSD", "ETHUSD", "SOLUSD", "BNBUSD", "XRPUSD", "XDGUSD", "DOGEUSD");

    private final RealMarketDataFetcherService fetcher;
    private final String assetPairsUrl;
    private volatile KrakenPairCacheState state = KrakenPairCacheState.NOT_LOADED;
    private volatile String failureReason;
    private volatile Map<String, KrakenPairMetadata> cache = Map.of();
    private final Set<String> missingPairLogs = ConcurrentHashMap.newKeySet();

    KrakenPairResolver(RealMarketDataFetcherService fetcher, String baseUrl) {
        this.fetcher = fetcher;
        this.assetPairsUrl = stripTrailingSlash(baseUrl) + ASSET_PAIRS_ENDPOINT + "?assetVersion=1";
    }

    KrakenPairResolution resolve(String internalSymbol) {
        String normalized = normalize(internalSymbol);
        if (!DISPLAY_ALIASES.containsKey(normalized)) {
            return KrakenPairResolution.failed(OhlcvSourceState.ERROR, "INVALID_SYMBOL_MAPPING");
        }
        ensureLoaded();
        if (state != KrakenPairCacheState.READY) {
            return KrakenPairResolution.failed(OhlcvSourceState.ERROR,
                    failureReason == null ? "KRAKEN_ASSET_PAIRS_FETCH_FAILED" : failureReason);
        }
        KrakenPairMetadata metadata = cache.get(normalized);
        if (metadata == null) {
            if (missingPairLogs.add(normalized)) {
                log.warn("KRAKEN_PAIR_NOT_FOUND internalSymbol={}", normalized);
            }
            return KrakenPairResolution.failed(OhlcvSourceState.ERROR, "PAIR_NOT_SUPPORTED");
        }
        return KrakenPairResolution.ready(metadata);
    }

    KrakenPairMetadata cached(String internalSymbol) {
        return cache.get(normalize(internalSymbol));
    }

    KrakenPairCacheState state() {
        return state;
    }

    String failureReason() {
        return failureReason;
    }

    private void ensureLoaded() {
        if (state == KrakenPairCacheState.READY || state == KrakenPairCacheState.FAILED) return;
        synchronized (this) {
            if (state == KrakenPairCacheState.READY || state == KrakenPairCacheState.FAILED) return;
            state = KrakenPairCacheState.LOADING;
            PublicMarketHttpResult fetched = fetcher.fetchPublicJson("KRAKEN", assetPairsUrl);
            if (!ready(fetched) && retryable(fetched)) {
                fetched = fetcher.fetchPublicJson("KRAKEN", assetPairsUrl);
            }
            if (!ready(fetched)) {
                fail(fetched == null || fetched.reasonCode() == null
                        ? "KRAKEN_ASSET_PAIRS_FETCH_FAILED" : normalizeReason(fetched.reasonCode()));
                log.warn("KRAKEN_ASSET_PAIRS_FETCH_FAILED reasonCode={}", failureReason);
                return;
            }
            Map<String, KrakenPairMetadata> parsed = parse(fetched.payload());
            if (parsed == null) {
                fail("KRAKEN_PAIR_RESOLUTION_ERROR");
                log.warn("KRAKEN_PAIR_RESOLUTION_ERROR reasonCode={}", failureReason);
                return;
            }
            cache = Map.copyOf(parsed);
            failureReason = null;
            state = KrakenPairCacheState.READY;
            for (KrakenPairMetadata metadata : cache.values()) {
                log.info("KRAKEN_PAIR_RESOLVED internalSymbol={} requestPair={} displayPair={} resultKey={}",
                        metadata.internalSymbol(), metadata.requestPair(), metadata.displayPair(), metadata.resultKey());
            }
        }
    }

    private Map<String, KrakenPairMetadata> parse(JsonNode payload) {
        if (payload == null || !payload.isObject() || !payload.path("error").isArray()
                || !payload.path("error").isEmpty() || !payload.path("result").isObject()) {
            return null;
        }
        Map<String, KrakenPairMetadata> resolved = new LinkedHashMap<>();
        boolean knownPairMalformed = false;
        var fields = payload.path("result").fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode pair = entry.getValue();
            if (!pair.isObject()) continue;
            String wsname = text(pair, "wsname");
            String internal = internalForDisplay(wsname);
            if (internal == null && isMalformedCorePair(pair)) {
                knownPairMalformed = true;
                continue;
            }
            if (internal == null) continue;
            String altname = text(pair, "altname");
            String requestPair = altname == null ? blankToNull(entry.getKey()) : altname;
            String status = text(pair, "status");
            if (requestPair == null || wsname == null) {
                knownPairMalformed = true;
                continue;
            }
            if (status != null && !"online".equalsIgnoreCase(status)) continue;
            resolved.putIfAbsent(internal, new KrakenPairMetadata(
                    internal, requestPair, wsname, entry.getKey(), status));
        }
        if (knownPairMalformed) return null;
        return resolved;
    }

    private void fail(String reason) {
        cache = Map.of();
        failureReason = reason;
        state = KrakenPairCacheState.FAILED;
    }

    private static String internalForDisplay(String wsname) {
        if (wsname == null) return null;
        String normalized = wsname.trim().toUpperCase(Locale.ROOT);
        for (Map.Entry<String, Set<String>> aliases : DISPLAY_ALIASES.entrySet()) {
            if (aliases.getValue().contains(normalized)) return aliases.getKey();
        }
        return null;
    }

    private static boolean isMalformedCorePair(JsonNode pair) {
        String quote = canonicalAsset(text(pair, "quote"));
        String base = canonicalAsset(text(pair, "base"));
        if (!"USD".equals(quote) || base == null) return false;
        String altname = text(pair, "altname");
        return Set.of("BTC", "ETH", "SOL", "BNB", "XRP", "DOGE").contains(base)
                && altname != null && CORE_REQUEST_PAIRS.contains(altname.toUpperCase(Locale.ROOT));
    }

    private static String canonicalAsset(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "XXBT", "XBT", "BTC" -> "BTC";
            case "XXDG", "XDG", "DOGE" -> "DOGE";
            case "XETH", "ETH" -> "ETH";
            case "XXRP", "XRP" -> "XRP";
            case "ZUSD", "USD" -> "USD";
            default -> value;
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? blankToNull(value.asText()) : null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean ready(PublicMarketHttpResult result) {
        return result != null && result.ready();
    }

    private static boolean retryable(PublicMarketHttpResult result) {
        return result != null && RETRYABLE.contains(result.reasonCode());
    }

    private static String normalizeReason(String reason) {
        return "HTTP_429".equals(reason) ? "RATE_LIMITED" : reason;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://api.kraken.com";
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
