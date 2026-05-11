package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.market.util.BinanceUsdtSymbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Public OKX REST fallback (no API key). V1: spot ticker only.
 */
@Service
public class OkxMarketQuoteClient implements MarketQuoteClient {

    static final String PROVIDER = "okx-fallback";

    private static final Logger log = LoggerFactory.getLogger(OkxMarketQuoteClient.class);
    private static final String BASE_URL = "https://www.okx.com/api/v5/market/ticker";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public OkxMarketQuoteClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<MarketQuoteSnapshot> fetch24hTicker(String assetSymbol) {
        String symbol = BinanceUsdtSymbol.toUsdtPair(assetSymbol);
        String instId = toOkxInstId(symbol);
        String url = BASE_URL + "?instId=" + instId;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("[market-quote] OKX HTTP {} for symbol={} instId={} bodySnippet={}",
                        response.statusCode(), symbol, instId, truncate(response.body(), 200));
                return Optional.empty();
            }
            Optional<MarketQuoteSnapshot> parsed = parseOkxTickerJson(objectMapper.readTree(response.body()), symbol);
            parsed.ifPresent(snap -> snap.setFetchedAtEpochMillis(System.currentTimeMillis()));
            if (parsed.isEmpty()) {
                log.warn("[market-quote] OKX parse missing fields symbol={} instId={}", symbol, instId);
            }
            return parsed;
        } catch (Exception e) {
            log.warn("[market-quote] OKX fetch failed symbol={} instId={} err={}", symbol, instId, e.getMessage());
            return Optional.empty();
        }
    }

    static Optional<MarketQuoteSnapshot> parseOkxTickerJson(JsonNode root, String symbolNormalized) {
        if (root == null || !"0".equals(root.path("code").asText())) {
            return Optional.empty();
        }
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            return Optional.empty();
        }
        JsonNode row = data.get(0);
        if (!row.hasNonNull("last") || !row.hasNonNull("open24h")) {
            return Optional.empty();
        }

        BigDecimal last = new BigDecimal(row.get("last").asText());
        BigDecimal open24h = new BigDecimal(row.get("open24h").asText());
        if (last.signum() <= 0 || open24h.signum() <= 0) {
            return Optional.empty();
        }

        MarketQuoteSnapshot snap = new MarketQuoteSnapshot();
        snap.setProvider(PROVIDER);
        snap.setSymbolNormalized(symbolNormalized);
        snap.setLastPrice(last);
        snap.setPriceChangePercent24h(last.subtract(open24h)
                .divide(open24h, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
        if (row.hasNonNull("high24h")) {
            snap.setHighPrice(new BigDecimal(row.get("high24h").asText()));
        }
        if (row.hasNonNull("low24h")) {
            snap.setLowPrice(new BigDecimal(row.get("low24h").asText()));
        }
        return Optional.of(snap);
    }

    static String toOkxInstId(String symbolNormalized) {
        String symbol = BinanceUsdtSymbol.toUsdtPair(symbolNormalized);
        if (symbol.endsWith("USDT") && symbol.length() > 4) {
            return symbol.substring(0, symbol.length() - 4) + "-USDT";
        }
        return symbol;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
