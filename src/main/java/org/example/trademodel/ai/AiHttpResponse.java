package org.example.trademodel.ai;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AiHttpResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    public AiHttpResponse(int statusCode, String body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = body == null ? "" : body;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public String firstHeader(String name) {
        if (name == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                List<String> values = entry.getValue();
                return values == null || values.isEmpty() ? null : values.get(0);
            }
        }
        return null;
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public Map<String, List<String>> getHeaders() { return Collections.unmodifiableMap(headers); }
}
