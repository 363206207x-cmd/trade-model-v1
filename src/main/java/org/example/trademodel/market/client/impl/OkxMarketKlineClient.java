package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.market.client.MarketKlineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Public OKX kline fallback (no API key). Converts OKX candles to Binance-style rows.
 */
@Service
public class OkxMarketKlineClient implements MarketKlineClient {

    static final String PROVIDER = "okx-fallback";

    private static final Logger log = LoggerFactory.getLogger(OkxMarketKlineClient.class);
    private static final String BASE_URL = "https://www.okx.com/api/v5/market/candles";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public OkxMarketKlineClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String[]> fetchKlines(String symbol, String interval, int limit) {
        String instId = OkxMarketQuoteClient.toOkxInstId(symbol);
        String url = BASE_URL + "?instId=" + instId + "&bar=" + interval + "&limit=" + limit;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("[market-kline] OKX HTTP {} for instId={} interval={} bodySnippet={}",
                        response.statusCode(), instId, interval, truncate(response.body(), 200));
                return List.of();
            }
            List<String[]> rows = parseOkxCandlesJson(objectMapper.readTree(response.body()), interval, limit);
            if (rows.isEmpty()) {
                log.warn("[market-kline] OKX parse empty instId={} interval={}", instId, interval);
            }
            return rows;
        } catch (Exception e) {
            log.warn("[market-kline] OKX fetch failed instId={} interval={} err={}", instId, interval, e.getMessage());
            return List.of();
        }
    }

    static List<String[]> parseOkxCandlesJson(JsonNode root, String interval, int limit) {
        if (root == null || !"0".equals(root.path("code").asText())) {
            return List.of();
        }
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            return List.of();
        }
        List<String[]> rows = new ArrayList<>();
        for (JsonNode candle : data) {
            if (!candle.isArray() || candle.size() < 6) {
                continue;
            }
            try {
                long openTime = Long.parseLong(candle.get(0).asText());
                long closeTime = openTime + intervalMillis(interval) - 1;
                rows.add(new String[]{
                        Long.toString(openTime),
                        candle.get(1).asText(),
                        candle.get(2).asText(),
                        candle.get(3).asText(),
                        candle.get(4).asText(),
                        candle.get(5).asText(),
                        Long.toString(closeTime),
                        candle.size() > 7 ? candle.get(7).asText() : "0",
                        "0",
                        "0",
                        "0",
                        "0"
                });
            } catch (RuntimeException ignored) {
                // Skip malformed single candles; callers fall back to empty if nothing valid remains.
            }
        }
        Collections.reverse(rows);
        if (limit > 0 && rows.size() > limit) {
            return List.copyOf(rows.subList(rows.size() - limit, rows.size()));
        }
        return List.copyOf(rows);
    }

    static long intervalMillis(String interval) {
        if (interval == null || interval.isBlank()) {
            return 60_000L;
        }
        String t = interval.trim();
        try {
            if (t.endsWith("m")) {
                return Long.parseLong(t.substring(0, t.length() - 1)) * 60_000L;
            }
            if (t.endsWith("H") || t.endsWith("h")) {
                return Long.parseLong(t.substring(0, t.length() - 1)) * 3_600_000L;
            }
            if (t.endsWith("D") || t.endsWith("d")) {
                return Long.parseLong(t.substring(0, t.length() - 1)) * 86_400_000L;
            }
        } catch (RuntimeException ignored) {
            return 60_000L;
        }
        return 60_000L;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
