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
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.ProviderFailureClassifier;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.MarketType;
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
    private static final String SPOT_TICKER_URL = "https://api.binance.com/api/v3/ticker/24hr";
    private static final String FUTURES_TICKER_URL = "https://fapi.binance.com/fapi/v1/ticker/24hr";
    private static final String FUTURES_BOOK_URL = "https://fapi.binance.com/fapi/v1/ticker/bookTicker";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public BinanceMarketQuoteClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    BinanceMarketQuoteClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<MarketQuoteSnapshot> fetch24hTicker(String assetSymbol) {
        return Optional.ofNullable(fetch24hTickerResult(assetSymbol, MarketType.SPOT).payload());
    }

    @Override
    public Optional<MarketQuoteSnapshot> fetch24hTicker(String assetSymbol, MarketType marketType) {
        return Optional.ofNullable(fetch24hTickerResult(assetSymbol, marketType).payload());
    }

    @Override
    public ProviderAdapterResponse<MarketQuoteSnapshot> fetch24hTickerResult(
            String assetSymbol, MarketType marketType) {
        String symbol = BinanceUsdtSymbol.toUsdtPair(assetSymbol);
        MarketType effectiveMarket = marketType == null ? MarketType.SPOT : marketType;
        String tickerUrl = (effectiveMarket == MarketType.PERPETUAL
                ? FUTURES_TICKER_URL : SPOT_TICKER_URL) + "?symbol=" + symbol;
        try {
            HttpResponse<String> response = get(tickerUrl);
            if (response.statusCode() != 200) {
                log.warn("[market-quote] Binance HTTP {} for symbol={} bodySnippet={}",
                        response.statusCode(), symbol, truncate(response.body(), 200));
                return ProviderFailureClassifier.httpFailure(response.statusCode(),
                        "BINANCE_QUOTE_HTTP_" + response.statusCode(), null);
            }
            JsonNode root = objectMapper.readTree(response.body());
            Optional<MarketQuoteSnapshot> parsed = parseBinance24hrJson(root, symbol);
            if (parsed.isEmpty()) {
                log.warn("[market-quote] Binance parse missing fields symbol={}", symbol);
                return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 200,
                        "BINANCE_QUOTE_MALFORMED", null);
            }
            MarketQuoteSnapshot snap = parsed.get();
            if (effectiveMarket == MarketType.PERPETUAL) {
                HttpResponse<String> book = get(FUTURES_BOOK_URL + "?symbol=" + symbol);
                if (ProviderFailureClassifier.isRegionRestricted(book.statusCode(),
                        "BINANCE_BOOK_TICKER_HTTP_" + book.statusCode())) {
                    return ProviderFailureClassifier.httpFailure(book.statusCode(),
                            "BINANCE_BOOK_TICKER_HTTP_" + book.statusCode(), null);
                }
                if (book.statusCode() != 200 || !applyBookTicker(snap, objectMapper.readTree(book.body()))) {
                    log.warn("[market-quote] Binance futures book ticker unavailable symbol={}", symbol);
                }
            }
            snap.setFetchedAtEpochMillis(System.currentTimeMillis());
            return ProviderAdapterResponse.ready(snap,
                    java.time.Instant.ofEpochMilli(snap.getFetchedAtEpochMillis()));
        } catch (Exception e) {
            log.warn("[market-quote] Binance fetch failed symbol={} err={}", symbol, e.getMessage());
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                    "PROVIDER_TRANSPORT_FAILED", null);
        }
    }

    private HttpResponse<String> get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
        applyBookTicker(snap, root);
        return Optional.of(snap);
    }

    static boolean applyBookTicker(MarketQuoteSnapshot snapshot, JsonNode root) {
        if (snapshot == null || root == null) return false;
        BigDecimal bid = decimal(root, "bidPrice");
        BigDecimal bidQty = decimal(root, "bidQty");
        BigDecimal ask = decimal(root, "askPrice");
        BigDecimal askQty = decimal(root, "askQty");
        if (!positive(bid) || !positive(bidQty) || !positive(ask) || !positive(askQty)
                || ask.compareTo(bid) < 0) {
            return false;
        }
        snapshot.setBidPrice(bid);
        snapshot.setBidQuantity(bidQty);
        snapshot.setAskPrice(ask);
        snapshot.setAskQuantity(askQty);
        return true;
    }

    private static BigDecimal decimal(JsonNode root, String field) {
        if (!root.hasNonNull(field)) return null;
        try {
            return new BigDecimal(root.get(field).asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
