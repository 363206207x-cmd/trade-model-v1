package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.trademodel.dto.ohlcv.OhlcvBarInput;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicMarketHttpResult;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.dto.ohlcv.PublicProviderErrorCode;
import org.example.trademodel.service.PublicOhlcvProvider;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KrakenPublicOhlcvProvider implements PublicOhlcvProvider {
    static final String SOURCE_ENDPOINT = "/0/public/OHLC";
    private static final String ASSET_PAIRS_ENDPOINT = "/0/public/AssetPairs";
    private static final Map<String, String> DISPLAY_PAIRS = Map.of(
            "BTCUSDT", "BTC/USD",
            "ETHUSDT", "ETH/USD",
            "SOLUSDT", "SOL/USD",
            "XRPUSDT", "XRP/USD",
            "DOGEUSDT", "DOGE/USD");
    private static final Map<String, Integer> INTERVALS = Map.of(
            "5m", 5, "15m", 15, "1h", 60, "4h", 240);

    private final RealMarketDataFetcherService fetcher;
    private final boolean enabled;
    private final boolean externalCallsEnabled;
    private final String baseUrl;
    private volatile boolean bnbPairChecked;
    private volatile String bnbDisplayPair;
    private final AtomicLong rateLimitedUntilMs = new AtomicLong(0L);

    public KrakenPublicOhlcvProvider(
            RealMarketDataFetcherService fetcher,
            @Value("${trade-model.ohlcv.kraken.enabled:false}") boolean enabled,
            @Value("${trade-model.ohlcv.kraken.external-calls-enabled:false}") boolean externalCallsEnabled,
            @Value("${trade-model.ohlcv.kraken.base-url:https://api.kraken.com}") String baseUrl) {
        this.fetcher = fetcher;
        this.enabled = enabled;
        this.externalCallsEnabled = externalCallsEnabled;
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    @Override
    public PublicOhlcvProviderResult fetchClosedBars(String symbol, String timeframe, int limit,
                                                     String ingestionRunId) {
        if (!enabled) return result(OhlcvSourceState.DISABLED, "KRAKEN_PROVIDER_DISABLED", null);
        if (!externalCallsEnabled) {
            return result(OhlcvSourceState.NOT_CONFIGURED, "KRAKEN_EXTERNAL_CALL_NOT_ENABLED", null);
        }
        if (System.currentTimeMillis() < rateLimitedUntilMs.get()) {
            return result(OhlcvSourceState.ERROR, PublicProviderErrorCode.RATE_LIMITED.name(), null);
        }
        String normalized = normalizeSymbol(symbol);
        Integer interval = INTERVALS.get(timeframe);
        if (normalized == null || interval == null || limit <= 0 || limit > 500) {
            return result(OhlcvSourceState.DEGRADED, "PUBLIC_OHLCV_REQUEST_INVALID", null);
        }

        PairResolution pair = resolvePair(normalized);
        if (!pair.ready()) return result(pair.state(), pair.reasonCode(), null);
        String url = buildOhlcUrl(baseUrl, pair.displayPair(), timeframe, limit, Instant.now());
        PublicMarketHttpResult fetched = fetchWithBoundedRetry(url);
        if (!fetched.ready()) return result(fetched.sourceState(), normalizeReason(fetched.reasonCode()), null);
        return mapResponse(normalized, timeframe, limit, ingestionRunId, fetched);
    }

    PublicOhlcvProviderResult mapResponse(String symbol, String timeframe, int limit, String ingestionRunId,
                                          PublicMarketHttpResult fetched) {
        JsonNode root = fetched.payload();
        if (root == null || !root.isObject() || !validErrorArray(root.path("error"))) {
            return result(OhlcvSourceState.ERROR, PublicProviderErrorCode.INVALID_RESPONSE.name(), null);
        }
        if (containsUnknownPair(root.path("error"))) {
            return result(OhlcvSourceState.ERROR, PublicProviderErrorCode.PAIR_NOT_SUPPORTED.name(), null);
        }
        if (!root.path("error").isEmpty()) {
            return result(OhlcvSourceState.ERROR, PublicProviderErrorCode.INVALID_RESPONSE.name(), null);
        }
        JsonNode resultNode = root.path("result");
        JsonNode candles = firstCandleArray(resultNode);
        if (candles == null || !candles.isArray() || candles.size() < 2) {
            return result(OhlcvSourceState.ERROR, PublicProviderErrorCode.INVALID_RESPONSE.name(), null);
        }

        int intervalMinutes = intervalMinutes(timeframe);
        List<OhlcvBarInput> parsed = new ArrayList<>();
        long previousOpen = -1L;
        for (int i = 0; i < candles.size() - 1; i++) {
            JsonNode row = candles.get(i);
            OhlcvBarInput bar = parseRow(symbol, timeframe, intervalMinutes, row);
            if (bar == null || bar.openTimeMs() <= previousOpen || !validGeometry(bar)) {
                return result(OhlcvSourceState.ERROR, PublicProviderErrorCode.INVALID_RESPONSE.name(), null);
            }
            previousOpen = bar.openTimeMs();
            parsed.add(bar);
        }
        if (parsed.isEmpty()) {
            return result(OhlcvSourceState.WAITING_SYNC, "PUBLIC_OHLCV_NO_CLOSED_BAR", null);
        }
        List<OhlcvBarInput> bars = parsed.size() <= limit
                ? parsed : new ArrayList<>(parsed.subList(parsed.size() - limit, parsed.size()));
        OhlcvIngestionBatch batch = new OhlcvIngestionBatch(
                "KRAKEN", "SPOT", SOURCE_ENDPOINT, OhlcvSourceState.READY,
                fetched.fetchTime(), "kraken-public-ohlc-v1", 1,
                ingestionRunId, ingestionRunId, bars);
        return result(OhlcvSourceState.READY, null, batch);
    }

    private PairResolution resolvePair(String symbol) {
        String direct = DISPLAY_PAIRS.get(symbol);
        if (direct != null) return PairResolution.ready(direct);
        if (!"BNBUSDT".equals(symbol)) {
            return PairResolution.failed(PublicProviderErrorCode.PAIR_NOT_SUPPORTED.name());
        }
        if (bnbPairChecked) {
            return bnbDisplayPair == null
                    ? PairResolution.failed(PublicProviderErrorCode.PAIR_NOT_SUPPORTED.name())
                    : PairResolution.ready(bnbDisplayPair);
        }
        synchronized (this) {
            if (bnbPairChecked) {
                return bnbDisplayPair == null
                        ? PairResolution.failed(PublicProviderErrorCode.PAIR_NOT_SUPPORTED.name())
                        : PairResolution.ready(bnbDisplayPair);
            }
            String url = baseUrl + ASSET_PAIRS_ENDPOINT + "?pair=BNBUSD&assetVersion=1";
            PublicMarketHttpResult result = fetcher.fetchPublicJson("KRAKEN", url);
            if (!result.ready()) {
                return new PairResolution(false, null, result.sourceState(), normalizeReason(result.reasonCode()));
            }
            bnbDisplayPair = parseAssetVersionDisplayPair(result.payload(), "BNB/USD");
            bnbPairChecked = true;
            return bnbDisplayPair == null
                    ? PairResolution.failed(PublicProviderErrorCode.PAIR_NOT_SUPPORTED.name())
                    : PairResolution.ready(bnbDisplayPair);
        }
    }

    private PublicMarketHttpResult fetchWithBoundedRetry(String url) {
        PublicMarketHttpResult result = fetcher.fetchPublicJson("KRAKEN", url);
        if (result != null && result.httpStatus() == 429) {
            rateLimitedUntilMs.set(System.currentTimeMillis() + 60_000L);
            return result;
        }
        if (result.ready() || !("HTTP_5XX".equals(result.reasonCode()) || "TIMEOUT".equals(result.reasonCode()))) {
            return result;
        }
        return fetcher.fetchPublicJson("KRAKEN", url);
    }

    static String buildOhlcUrl(String baseUrl, String displayPair, String timeframe, int limit, Instant now) {
        int interval = intervalMinutes(timeframe);
        long since = Math.max(0L, now.getEpochSecond() - (long) interval * 60L * (limit + 2L));
        return stripTrailingSlash(baseUrl) + SOURCE_ENDPOINT
                + "?pair=" + URLEncoder.encode(displayPair, StandardCharsets.UTF_8)
                + "&interval=" + interval + "&since=" + since + "&assetVersion=1";
    }

    static int intervalMinutes(String timeframe) {
        Integer interval = INTERVALS.get(timeframe);
        if (interval == null) throw new IllegalArgumentException("unsupported timeframe");
        return interval;
    }

    static String parseAssetVersionDisplayPair(JsonNode payload, String expectedDisplayPair) {
        if (payload == null || !payload.path("error").isArray() || payload.path("error").size() > 0) return null;
        JsonNode result = payload.path("result");
        if (!result.isObject()) return null;
        var fields = result.fields();
        while (fields.hasNext()) {
            JsonNode pair = fields.next().getValue();
            String wsname = pair.path("wsname").asText(null);
            if (expectedDisplayPair.equalsIgnoreCase(wsname)) return wsname;
        }
        return null;
    }

    private static JsonNode firstCandleArray(JsonNode result) {
        if (result == null || !result.isObject()) return null;
        var fields = result.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!"last".equals(field.getKey()) && field.getValue().isArray()) return field.getValue();
        }
        return null;
    }

    private static OhlcvBarInput parseRow(String symbol, String timeframe, int intervalMinutes, JsonNode row) {
        if (row == null || !row.isArray() || row.size() < 8) return null;
        try {
            long openTimeMs = row.get(0).asLong() * 1000L;
            long closeTimeMs = openTimeMs + intervalMinutes * 60_000L - 1L;
            return new OhlcvBarInput(symbol, timeframe, openTimeMs, closeTimeMs,
                    decimal(row.get(1)), decimal(row.get(2)), decimal(row.get(3)), decimal(row.get(4)),
                    decimal(row.get(6)), null, row.get(7).asLong(), null, null, true);
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private static boolean validGeometry(OhlcvBarInput bar) {
        return positive(bar.open()) && positive(bar.high()) && positive(bar.low()) && positive(bar.close())
                && bar.volume() != null && bar.volume().compareTo(BigDecimal.ZERO) >= 0
                && bar.high().compareTo(bar.low()) >= 0
                && bar.high().compareTo(bar.open()) >= 0
                && bar.high().compareTo(bar.close()) >= 0
                && bar.low().compareTo(bar.open()) <= 0
                && bar.low().compareTo(bar.close()) <= 0;
    }

    private static boolean validErrorArray(JsonNode errors) {
        return errors != null && errors.isArray();
    }

    private static boolean containsUnknownPair(JsonNode errors) {
        if (errors == null || !errors.isArray()) return false;
        for (JsonNode error : errors) {
            if (error.asText("").toLowerCase(Locale.ROOT).contains("unknown asset pair")) return true;
        }
        return false;
    }

    private static BigDecimal decimal(JsonNode node) {
        return new BigDecimal(node.asText());
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) return null;
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeReason(String reason) {
        if ("HTTP_429".equals(reason)) return PublicProviderErrorCode.RATE_LIMITED.name();
        return reason == null ? PublicProviderErrorCode.PROVIDER_UNAVAILABLE.name() : reason;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://api.kraken.com";
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static PublicOhlcvProviderResult result(OhlcvSourceState state, String reason,
                                                    OhlcvIngestionBatch batch) {
        return new PublicOhlcvProviderResult(state, reason, batch);
    }

    private record PairResolution(boolean ready, String displayPair, OhlcvSourceState state, String reasonCode) {
        static PairResolution ready(String pair) {
            return new PairResolution(true, pair, OhlcvSourceState.READY, null);
        }

        static PairResolution failed(String reason) {
            return new PairResolution(false, null, OhlcvSourceState.ERROR, reason);
        }
    }
}
