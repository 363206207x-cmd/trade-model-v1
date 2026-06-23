package org.example.trademodel.ai;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class AiHttpRequest {
    private String url;
    private Map<String, String> headers = new LinkedHashMap<>();
    private String body;
    private Duration timeout = Duration.ofSeconds(5);

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) {
        this.timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
    }
}
