package org.example.trademodel.ai;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class JdkAiHttpTransport implements AiHttpTransport {
    private static final int MAX_RESPONSE_CHARS = 120_000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public AiHttpResponse post(AiHttpRequest request) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.getUrl()))
                .timeout(request.getTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(request.getBody() == null ? "" : request.getBody()));
        for (var entry : request.getHeaders().entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new AiHttpResponse(response.statusCode(), truncate(response.body()), response.headers().map());
    }

    @Override
    public AiHttpResponse get(AiHttpRequest request) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.getUrl()))
                .timeout(request.getTimeout())
                .GET();
        for (var entry : request.getHeaders().entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new AiHttpResponse(response.statusCode(), truncate(response.body()), response.headers().map());
    }

    private static String truncate(String body) {
        if (body == null || body.length() <= MAX_RESPONSE_CHARS) {
            return body;
        }
        return body.substring(0, MAX_RESPONSE_CHARS);
    }
}
