package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.market.client.MarketKlineClient;
import org.example.trademodel.market.util.BinanceUsdtSymbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Public Binance kline REST client. Rows are returned as the existing Binance-style String[] contract.
 */
@Service
public class BinanceMarketKlineClient implements MarketKlineClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceMarketKlineClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${market.api.base-url:https://api.binance.com}")
    private String baseUrl;

    public BinanceMarketKlineClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String[]> fetchKlines(String symbol, String interval, int limit) {
        String symbolNormalized = BinanceUsdtSymbol.toUsdtPair(symbol);
        try {
            String url = baseUrl + "/api/v3/klines?symbol=" + symbolNormalized
                    + "&interval=" + interval + "&limit=" + limit;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String raw = response.getBody();
            if (raw == null || raw.trim().isEmpty()) {
                return List.of();
            }
            return parseBinanceKlinesJson(objectMapper, raw);
        } catch (Exception e) {
            log.warn("[market-kline] Binance fetch failed symbol={} interval={} err={}",
                    symbolNormalized, interval, e.getMessage());
            return List.of();
        }
    }

    static List<String[]> parseBinanceKlinesJson(ObjectMapper objectMapper, String raw) throws Exception {
        List<List<Object>> data = objectMapper.readValue(raw, new TypeReference<>() {});
        return data.stream()
                .map(row -> row.stream().map(Object::toString).toArray(String[]::new))
                .toList();
    }
}
