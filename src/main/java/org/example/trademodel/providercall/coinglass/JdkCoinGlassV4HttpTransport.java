package org.example.trademodel.providercall.coinglass;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class JdkCoinGlassV4HttpTransport implements CoinGlassV4HttpTransport {
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public CoinGlassHttpResponse get(URI uri, String authHeaderName, String apiKey, Duration timeout)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .header(authHeaderName, apiKey)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new CoinGlassHttpResponse(response.statusCode(), response.body(), response.headers().map());
    }
}
