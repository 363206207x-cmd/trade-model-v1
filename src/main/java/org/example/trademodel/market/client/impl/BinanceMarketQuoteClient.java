package org.example.trademodel.market.client.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.util.BinanceUsdtSymbol;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Public Binance REST (no API key). V1: 24h ticker only.
 */
@Service
public class BinanceMarketQuoteClient implements MarketQuoteClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceMarketQuoteClient.class);
    private static final String BASE_URL = "https://api.binance.com/api/v3/ticker/24hr";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public BinanceMarketQuoteClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<MarketQuoteSnapshot> fetch24hTicker(String assetSymbol) {
        String symbol = BinanceUsdtSymbol.toUsdtPair(assetSymbol);
        String url = BASE_URL + "?symbol=" + symbol;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("[market-quote] Binance HTTP {} for symbol={} bodySnippet={}",
                        response.statusCode(), symbol, truncate(response.body(), 200));
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            Optional<MarketQuoteSnapshot> parsed = parseBinance24hrJson(root, symbol);
            if (parsed.isEmpty()) {
                log.warn("[market-quote] Binance parse missing fields symbol={}", symbol);
                return Optional.empty();
            }
            MarketQuoteSnapshot snap = parsed.get();
            snap.setFetchedAtEpochMillis(System.currentTimeMillis());
            return Optional.of(snap);
        } catch (Exception e) {
            log.warn("[market-quote] Binance fetch failed symbol={} err={}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parses Binance {@code /api/v3/ticker/24hr} JSON. {@code highPrice}/{@code lowPrice} are optional;
     * if absent, snapshot still succeeds when {@code lastPrice} and {@code priceChangePercent} are present.
     */
    static Optional<MarketQuoteSnapshot> parseBinance24hrJson(JsonNode root, String symbolNormalized) {
        if (root == null || !root.hasNonNull("lastPrice") || !root.hasNonNull("priceChangePercent")) {
            return Optional.empty();
        }
        MarketQuoteSnapshot snap = new MarketQuoteSnapshot();
        snap.setProvider("binance");
        snap.setSymbolNormalized(symbolNormalized);
        snap.setLastPrice(new BigDecimal(root.get("lastPrice").asText()));
        snap.setPriceChangePercent24h(new BigDecimal(root.get("priceChangePercent").asText()));
        if (root.hasNonNull("highPrice")) {
            try {
                snap.setHighPrice(new BigDecimal(root.get("highPrice").asText()));
            } catch (NumberFormatException e) {
                log.warn("[market-quote] invalid highPrice symbol={}", symbolNormalized);
            }
        }
        if (root.hasNonNull("lowPrice")) {
            try {
                snap.setLowPrice(new BigDecimal(root.get("lowPrice").asText()));
            } catch (NumberFormatException e) {
                log.warn("[market-quote] invalid lowPrice symbol={}", symbolNormalized);
            }
        }
        return Optional.of(snap);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
