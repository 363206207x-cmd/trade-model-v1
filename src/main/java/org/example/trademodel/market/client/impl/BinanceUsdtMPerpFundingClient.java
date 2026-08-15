package org.example.trademodel.market.client.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.example.trademodel.market.client.PerpFundingRateClient;
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
 * Binance USDⓈ-M {@code GET /fapi/v1/premiumIndex} → {@code lastFundingRate} only.
 */
@Service
public class BinanceUsdtMPerpFundingClient implements PerpFundingRateClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceUsdtMPerpFundingClient.class);
    private static final String PREMIUM_INDEX_URL = "https://fapi.binance.com/fapi/v1/premiumIndex";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public BinanceUsdtMPerpFundingClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    BinanceUsdtMPerpFundingClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<BigDecimal> fetchLastFundingRate(String assetSymbol) {
        return Optional.ofNullable(fetchLastFundingRateResult(assetSymbol).payload());
    }

    @Override
    public ProviderAdapterResponse<BigDecimal> fetchLastFundingRateResult(String assetSymbol) {
        String symbol = BinanceUsdtSymbol.toUsdtPair(assetSymbol);
        String url = PREMIUM_INDEX_URL + "?symbol=" + symbol;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.info("[perp-funding] Binance HTTP {} symbol={} snippet={}",
                        response.statusCode(), symbol, truncate(response.body(), 160));
                return ProviderFailureClassifier.httpFailure(response.statusCode(),
                        "BINANCE_FUNDING_HTTP_" + response.statusCode(), null);
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root == null || !root.hasNonNull("lastFundingRate")) {
                log.warn("[perp-funding] missing lastFundingRate symbol={}", symbol);
                return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 200,
                        "BINANCE_FUNDING_MALFORMED", null);
            }
            BigDecimal rate = new BigDecimal(root.get("lastFundingRate").asText());
            return ProviderAdapterResponse.ready(rate, java.time.Instant.now());
        } catch (Exception e) {
            log.info("[perp-funding] fetch failed symbol={} err={}", symbol, e.getMessage());
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                    "PROVIDER_TRANSPORT_FAILED", null);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
