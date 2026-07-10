package org.example.trademodel.providercall.coinglass;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface CoinGlassV4HttpTransport {
    CoinGlassHttpResponse get(URI uri, String authHeaderName, String apiKey, Duration timeout) throws Exception;

    record CoinGlassHttpResponse(int statusCode, String body, Map<String, List<String>> responseHeaders) {
        public CoinGlassHttpResponse {
            responseHeaders = responseHeaders == null ? Map.of() : Map.copyOf(responseHeaders);
        }
    }
}
