package org.example.trademodel.providercall.coinglass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.providercall.ProviderFailureClassifier;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CoinGlassV4Client {
    private final CoinGlassProperties properties;
    private final CoinGlassV4HttpTransport transport;
    private final CoinGlassRateLimitMetadataParser rateLimitParser;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CoinGlassV4Client(CoinGlassProperties properties,
                             CoinGlassV4HttpTransport transport,
                             CoinGlassRateLimitMetadataParser rateLimitParser,
                             ObjectMapper objectMapper) {
        this(properties, transport, rateLimitParser, objectMapper, Clock.systemUTC());
    }

    CoinGlassV4Client(CoinGlassProperties properties,
                      CoinGlassV4HttpTransport transport,
                      CoinGlassRateLimitMetadataParser rateLimitParser,
                      ObjectMapper objectMapper,
                      Clock clock) {
        this.properties = properties;
        this.transport = transport;
        this.rateLimitParser = rateLimitParser;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public CoinGlassClientResponse get(String capabilityId, String path, Map<String, String> query) {
        Instant fetchTime = clock.instant();
        try {
            validateRuntimeConfiguration(path);
            CoinGlassV4HttpTransport.CoinGlassHttpResponse response = transport.get(
                    uri(path, query), properties.getAuthHeaderName(), properties.getApiKey(),
                    Duration.ofMillis(Math.max(1, properties.getRequestTimeoutMs())));
            CoinGlassRateLimitMetadata rateLimit = rateLimitParser.parse(response.responseHeaders());
            JsonNode root = parse(response.body());
            String providerCode = text(root, "code");
            JsonNode data = root == null ? null : root.get("data");
            String errorCode = classify(response.statusCode(), providerCode, data);
            return new CoinGlassClientResponse(response.statusCode(), providerCode, data, rateLimit,
                    fetchTime, capabilityId, errorCode);
        } catch (IllegalArgumentException invalid) {
            return new CoinGlassClientResponse(0, null, null, new CoinGlassRateLimitMetadata(null, null, null),
                    fetchTime, capabilityId, "INVALID_PROVIDER_CONFIGURATION");
        } catch (Exception failure) {
            return new CoinGlassClientResponse(0, null, null, new CoinGlassRateLimitMetadata(null, null, null),
                    fetchTime, capabilityId, "PROVIDER_TRANSPORT_FAILED");
        }
    }

    private void validateRuntimeConfiguration(String path) {
        if (properties.configurationState() != CoinGlassConfigurationState.CONFIGURED) {
            throw new IllegalArgumentException("CoinGlass external call is not configured: "
                    + properties.configurationState());
        }
        URI base = URI.create(properties.getBaseUrl());
        if (!"https".equalsIgnoreCase(base.getScheme())
                || !"open-api-v4.coinglass.com".equalsIgnoreCase(base.getHost())) {
            throw new IllegalArgumentException("CoinGlass base URL must use the official v4 HTTPS host");
        }
        if (!CoinGlassProperties.OFFICIAL_AUTH_HEADER.equals(properties.getAuthHeaderName())) {
            throw new IllegalArgumentException("CoinGlass v4 auth header mismatch");
        }
        if (path == null || !path.startsWith("/api/") || path.contains("..")) {
            throw new IllegalArgumentException("invalid CoinGlass endpoint path");
        }
        if (!java.util.Set.of(CoinGlassProperties.OPEN_INTEREST_PATH, CoinGlassProperties.FUNDING_PATH,
                CoinGlassProperties.LIQUIDATION_PATH, CoinGlassProperties.LONG_SHORT_PATH).contains(path)) {
            throw new IllegalArgumentException("CoinGlass endpoint is not in the verified capability allowlist");
        }
    }

    private URI uri(String path, Map<String, String> query) {
        String base = properties.getBaseUrl().endsWith("/")
                ? properties.getBaseUrl().substring(0, properties.getBaseUrl().length() - 1)
                : properties.getBaseUrl();
        StringBuilder value = new StringBuilder(base).append(path);
        Map<String, String> sorted = query == null ? Map.of() : new TreeMap<>(query);
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            value.append(first ? '?' : '&');
            first = false;
            value.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return URI.create(value.toString());
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return objectMapper.readTree(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String classify(int httpStatus, String providerCode, JsonNode data) {
        if (ProviderFailureClassifier.isRegionRestricted(httpStatus, "HTTP_" + httpStatus)) {
            return "REGION_RESTRICTED";
        }
        if (httpStatus == 401 || httpStatus == 403) return "AUTHENTICATION_FAILED";
        if (httpStatus == 429) return "RATE_LIMITED";
        if (httpStatus >= 500) return "UPSTREAM_UNAVAILABLE";
        if (httpStatus < 200 || httpStatus >= 300) return "HTTP_" + httpStatus;
        if (providerCode == null) return "MALFORMED_RESPONSE";
        if (!"0".equals(providerCode)) return "PROVIDER_STATUS_" + sanitizeCode(providerCode);
        if (data == null) return "MALFORMED_RESPONSE";
        return null;
    }

    private static String text(JsonNode root, String field) {
        JsonNode value = root == null ? null : root.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String sanitizeCode(String value) {
        return value == null ? "UNKNOWN" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
