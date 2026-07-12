package org.example.trademodel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicKlineFetchResult;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Instant;
import java.util.List;

@Service
public class RealMarketDataFetcherService {

    private static final Logger logger = LoggerFactory.getLogger(RealMarketDataFetcherService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${market.api.base-url:https://api.binance.com}")
    private String baseUrl;

    private final AnalysisSchedulerService analysisSchedulerService;

    @Autowired
    public RealMarketDataFetcherService(@Lazy AnalysisSchedulerService analysisSchedulerService) {
        this.analysisSchedulerService = analysisSchedulerService;
        logger.info("RealMarketDataFetcherService initialized successfully with baseUrl: {}", baseUrl);
    }

    @Value("${market.api.connect-timeout-ms:5000}")
    void configureConnectTimeout(int connectTimeoutMs) {
        SimpleClientHttpRequestFactory factory = requestFactory();
        factory.setConnectTimeout(Math.max(1000, connectTimeoutMs));
        restTemplate.setRequestFactory(factory);
    }

    @Value("${market.api.read-timeout-ms:10000}")
    void configureReadTimeout(int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = requestFactory();
        factory.setReadTimeout(Math.max(1000, readTimeoutMs));
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        if (restTemplate.getRequestFactory() instanceof SimpleClientHttpRequestFactory factory) {
            return factory;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        restTemplate.setRequestFactory(factory);
        return factory;
    }

    /**
     * 新增：返回真实 K 线数据（供 DecisionEngineService 使用）
     */
    public List<String[]> fetchKlines(String symbol, String interval, int limit) {
        return fetchKlinesDetailed(symbol, interval, limit).rows();
    }

    /**
     * Public Binance spot K-line fetch with an explicit source state. This method uses no API key and
     * never calls account, order, or position endpoints.
     */
    public PublicKlineFetchResult fetchKlinesDetailed(String symbol, String interval, int limit) {
        String fetchId = "FETCH-" + Instant.now().toEpochMilli();
        Instant fetchTime = Instant.now();
        try {
            String url = baseUrl + "/api/v3/klines?symbol=" + symbol.toUpperCase()
                        + "&interval=" + interval + "&limit=" + limit;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String raw = response.getBody();

            if (raw == null || raw.trim().isEmpty()) {
                return new PublicKlineFetchResult(OhlcvSourceState.EMPTY_CONFIRMED,
                        "PUBLIC_KLINE_RESPONSE_EMPTY", fetchTime, List.of());
            }

            List<List<Object>> data = objectMapper.readValue(raw, new TypeReference<>() {});
            List<String[]> rows = data.stream()
                    .map(row -> row.stream().map(Object::toString).toArray(String[]::new))
                    .toList();
            if (rows.isEmpty()) {
                return new PublicKlineFetchResult(OhlcvSourceState.EMPTY_CONFIRMED,
                        "PUBLIC_KLINE_RESULT_EMPTY", fetchTime, rows);
            }
            return new PublicKlineFetchResult(OhlcvSourceState.READY, null, fetchTime, rows);

        } catch (Exception e) {
            logger.error("[{}] fetchKlines failed for {} {}: {}", fetchId, symbol, interval, e.getMessage());
            return new PublicKlineFetchResult(OhlcvSourceState.ERROR,
                    "PUBLIC_KLINE_FETCH_ERROR", fetchTime, List.of());
        }
    }

    public void fetchRealMarketData(String symbol, String interval) {
        String fetchId = "FETCH-" + Instant.now().toEpochMilli();
        logger.info("[{}] === REAL MARKET DATA FETCH START === Symbol: {} | Interval: {}", fetchId, symbol, interval);
        try {
            AnalysisRunResult result = analysisSchedulerService.runMarketDataCompatibility(
                    symbol, interval, RequestIdSupport.generate());
            logger.info("[{}] === FETCH ROUTED THROUGH ANALYSIS ORCHESTRATOR === status={} analysisId={}",
                    fetchId, result.getStatus(), result.getAnalysisId());
        } catch (Exception e) {
            logger.error("[{}] FETCH FAILED: {}", fetchId, e.getMessage());
        }
    }
}
