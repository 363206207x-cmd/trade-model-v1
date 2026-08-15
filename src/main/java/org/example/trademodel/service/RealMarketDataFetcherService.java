package org.example.trademodel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicKlineFetchResult;
import org.example.trademodel.dto.ohlcv.PublicMarketHttpResult;
import org.example.trademodel.dto.ohlcv.PublicProviderErrorCode;
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
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Instant;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import java.util.List;

@Service
public class RealMarketDataFetcherService {

    private static final Logger logger = LoggerFactory.getLogger(RealMarketDataFetcherService.class);

    private final RestOperations restOperations;
    private final ObjectMapper objectMapper;

    @Value("${market.api.base-url:https://api.binance.com}")
    private String baseUrl;

    private final AnalysisSchedulerService analysisSchedulerService;

    @Autowired
    public RealMarketDataFetcherService(@Lazy AnalysisSchedulerService analysisSchedulerService) {
        this(analysisSchedulerService, new RestTemplate(), new ObjectMapper());
    }

    RealMarketDataFetcherService(AnalysisSchedulerService analysisSchedulerService,
                                 RestOperations restOperations,
                                 ObjectMapper objectMapper) {
        this.analysisSchedulerService = analysisSchedulerService;
        this.restOperations = restOperations;
        this.objectMapper = objectMapper;
        logger.info("RealMarketDataFetcherService initialized successfully with baseUrl: {}", baseUrl);
    }

    @Value("${market.api.connect-timeout-ms:5000}")
    void configureConnectTimeout(int connectTimeoutMs) {
        SimpleClientHttpRequestFactory factory = requestFactory();
        if (factory == null) return;
        factory.setConnectTimeout(Math.max(1000, connectTimeoutMs));
    }

    @Value("${market.api.read-timeout-ms:10000}")
    void configureReadTimeout(int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = requestFactory();
        if (factory == null) return;
        factory.setReadTimeout(Math.max(1000, readTimeoutMs));
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        if (!(restOperations instanceof RestTemplate restTemplate)) {
            return null;
        }
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
        String url = baseUrl + "/api/v3/klines?symbol=" + symbol.toUpperCase()
                + "&interval=" + interval + "&limit=" + limit;
        PublicMarketHttpResult fetched = fetchPublicJson("BINANCE", url);
        if (!fetched.ready()) {
            String reason = fetched.httpStatus() == 451
                    ? PublicProviderErrorCode.REGION_RESTRICTED.name() : fetched.reasonCode();
            return new PublicKlineFetchResult(fetched.sourceState(), reason, fetched.fetchTime(),
                    List.of(), fetched.httpStatus());
        }
        try {
            List<List<Object>> data = objectMapper.convertValue(fetched.payload(), new TypeReference<>() {});
            List<String[]> rows = data.stream()
                    .map(row -> row.stream().map(Object::toString).toArray(String[]::new))
                    .toList();
            if (rows.isEmpty()) {
                return new PublicKlineFetchResult(OhlcvSourceState.EMPTY_CONFIRMED,
                        "PUBLIC_KLINE_RESULT_EMPTY", fetched.fetchTime(), rows, fetched.httpStatus());
            }
            return new PublicKlineFetchResult(OhlcvSourceState.READY, null,
                    fetched.fetchTime(), rows, fetched.httpStatus());
        } catch (Exception e) {
            return new PublicKlineFetchResult(OhlcvSourceState.ERROR,
                    PublicProviderErrorCode.INVALID_RESPONSE.name(), fetched.fetchTime(), List.of(), fetched.httpStatus());
        }
    }

    public PublicMarketHttpResult fetchPublicJson(String provider, String url) {
        Instant fetchTime = Instant.now();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            ResponseEntity<String> response = restOperations.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String raw = response.getBody();
            if (raw == null || raw.isBlank()) {
                return httpResult(OhlcvSourceState.ERROR,
                        PublicProviderErrorCode.INVALID_RESPONSE, response.getStatusCode().value(), fetchTime, null);
            }
            JsonNode payload = objectMapper.readTree(raw);
            return httpResult(OhlcvSourceState.READY, null,
                    response.getStatusCode().value(), fetchTime, payload);
        } catch (HttpStatusCodeException failure) {
            int status = failure.getStatusCode().value();
            return httpResult(OhlcvSourceState.ERROR, httpReason(status), status, fetchTime, null);
        } catch (ResourceAccessException failure) {
            PublicProviderErrorCode reason = hasCause(failure, UnknownHostException.class)
                    ? PublicProviderErrorCode.DNS_FAILURE
                    : hasCause(failure, SocketTimeoutException.class) || hasCause(failure, HttpTimeoutException.class)
                    ? PublicProviderErrorCode.TIMEOUT : PublicProviderErrorCode.PROVIDER_UNAVAILABLE;
            return httpResult(OhlcvSourceState.ERROR, reason, 0, fetchTime, null);
        } catch (Exception failure) {
            return httpResult(OhlcvSourceState.ERROR,
                    PublicProviderErrorCode.INVALID_RESPONSE, 0, fetchTime, null);
        }
    }

    private static PublicProviderErrorCode httpReason(int status) {
        if (status == 401) return PublicProviderErrorCode.HTTP_401;
        if (status == 403) return PublicProviderErrorCode.HTTP_403;
        if (status == 429) return PublicProviderErrorCode.HTTP_429;
        if (status == 451) return PublicProviderErrorCode.REGION_RESTRICTED;
        if (status >= 500) return PublicProviderErrorCode.HTTP_5XX;
        return PublicProviderErrorCode.INVALID_RESPONSE;
    }

    private static PublicMarketHttpResult httpResult(OhlcvSourceState state,
                                                     PublicProviderErrorCode reason,
                                                     int status,
                                                     Instant fetchTime,
                                                     JsonNode payload) {
        return new PublicMarketHttpResult(state, reason == null ? null : reason.name(), status, fetchTime, payload);
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
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
