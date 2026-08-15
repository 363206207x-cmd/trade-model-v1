package org.example.trademodel.market.client.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.example.trademodel.market.client.OpenInterestClient;
import org.example.trademodel.market.util.BinanceUsdtSymbol;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.ProviderFailureClassifier;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Binance USDⓈ-M {@code GET /fapi/v1/openInterest} → {@code openInterest} only.
 * <p>
 * 官方口径锚定：{@code openInterest} 语义以 Binance Futures 文档为准（
 * <a href="https://binance-docs.github.io/apidocs/futures/en/#open-interest">Open Interest</a>，
 * 摘录以接入日期为准）。
 */
@Service
public class BinanceUsdtMOpenInterestClient implements OpenInterestClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceUsdtMOpenInterestClient.class);
    private static final String OPEN_INTEREST_URL = "https://fapi.binance.com/fapi/v1/openInterest";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public BinanceUsdtMOpenInterestClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    BinanceUsdtMOpenInterestClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<BigDecimal> fetchOpenInterest(String assetSymbol) {
        return Optional.ofNullable(fetchOpenInterestResult(assetSymbol).payload());
    }

    @Override
    public ProviderAdapterResponse<BigDecimal> fetchOpenInterestResult(String assetSymbol) {
        String symbol = BinanceUsdtSymbol.toUsdtPair(assetSymbol);
        String url = OPEN_INTEREST_URL + "?symbol=" + symbol;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.info("[open-interest] Binance HTTP {} symbol={} snippet={}",
                        response.statusCode(), symbol, truncate(response.body(), 160));
                return ProviderFailureClassifier.httpFailure(response.statusCode(),
                        "BINANCE_OPEN_INTEREST_HTTP_" + response.statusCode(), null);
            }
            JsonNode root = objectMapper.readTree(response.body());
            Optional<BigDecimal> parsed = parseOpenInterestNode(root, symbol);
            return parsed.isPresent()
                    ? ProviderAdapterResponse.ready(parsed.get(), java.time.Instant.now())
                    : ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 200,
                    "BINANCE_OPEN_INTEREST_MALFORMED", null);
        } catch (Exception e) {
            log.info("[open-interest] fetch failed symbol={} err={}", symbol, e.getMessage());
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                    "PROVIDER_TRANSPORT_FAILED", null);
        }
    }

    /**
     * Package-private for tests.
     */
    static Optional<BigDecimal> parseOpenInterestNode(JsonNode root, String symbolForLog) {
        if (root == null || !root.hasNonNull("openInterest")) {
            log.warn("[open-interest] missing openInterest symbol={}", symbolForLog);
            return Optional.empty();
        }
        try {
            BigDecimal oi = new BigDecimal(root.get("openInterest").asText());
            return Optional.of(oi);
        } catch (Exception e) {
            log.warn("[open-interest] parse openInterest failed symbol={} err={}", symbolForLog, e.getMessage());
            return Optional.empty();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
