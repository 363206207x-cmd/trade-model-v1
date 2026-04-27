package org.example.trademodel.position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BinancePositionProvider implements PositionProvider {

    private static final Logger log = LoggerFactory.getLogger(BinancePositionProvider.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Value("${binance.api.base-url:https://fapi.binance.com}")
    private String baseUrl;

    @Value("${binance.api.key:}")
    private String apiKey;

    @Value("${binance.api.secret:}")
    private String apiSecret;

    public BinancePositionProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean hasCredentials() {
        return isNotBlank(apiKey) && isNotBlank(apiSecret);
    }

    @Override
    public PositionProviderResult fetchOpenPositions() {
        if (!hasCredentials()) {
            throw new IllegalStateException("binance api credentials are missing");
        }
        long nowMillis = System.currentTimeMillis();
        String query = "timestamp=" + nowMillis + "&recvWindow=5000";
        String signature = sign(query, apiSecret);
        String requestUrl = normalizeBaseUrl(baseUrl) + "/fapi/v2/positionRisk?" + query + "&signature=" + signature;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("X-MBX-APIKEY", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String body = truncate(response.body(), 200);
                throw new IllegalStateException("binance position fetch http=" + response.statusCode() + " body=" + body);
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root == null || !root.isArray()) {
                throw new IllegalStateException("binance position payload is not array");
            }

            List<PositionSnapshot> openPositions = new ArrayList<>();
            int total = 0;
            for (JsonNode item : root) {
                total++;
                BigDecimal positionAmt = decimalOrNull(item.path("positionAmt").asText(null));
                if (positionAmt == null || positionAmt.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                PositionSnapshot snapshot = new PositionSnapshot();
                snapshot.setSymbol(item.path("symbol").asText(null));
                snapshot.setPositionSide(positionAmt.signum() >= 0 ? "LONG" : "SHORT");
                snapshot.setPositionQuantity(positionAmt.abs());

                BigDecimal entryPrice = decimalOrNull(item.path("entryPrice").asText(null));
                BigDecimal markPrice = decimalOrNull(item.path("markPrice").asText(null));
                snapshot.setAvgOpenPrice(entryPrice);
                snapshot.setMarkPrice(markPrice);
                snapshot.setBreakEvenPrice(decimalOrNull(item.path("breakEvenPrice").asText(null)));
                snapshot.setLiquidationPrice(decimalOrNull(item.path("liquidationPrice").asText(null)));
                snapshot.setUnrealizedPnlPct(calcPnlPct(snapshot.getPositionSide(), entryPrice, markPrice));
                snapshot.setPositionOpenTime(resolveOpenTime(item.path("updateTime").asText(null), nowMillis));
                openPositions.add(snapshot);
            }

            log.info("[position-sync] binance position request success total={} open={}", total, openPositions.size());
            return new PositionProviderResult("BINANCE", "binance-provider-v1", openPositions);
        } catch (Exception e) {
            throw new IllegalStateException("binance position fetch failed: " + e.getMessage(), e);
        }
    }

    private LocalDateTime resolveOpenTime(String updateTimeText, long fallbackMillis) {
        try {
            long updateMillis = Long.parseLong(updateTimeText);
            if (updateMillis > 0) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(updateMillis), ZoneId.systemDefault());
            }
        } catch (Exception ignored) {
            // fallback to current sync time when updateTime is unavailable
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(fallbackMillis), ZoneId.systemDefault());
    }

    private BigDecimal calcPnlPct(String side, BigDecimal entryPrice, BigDecimal markPrice) {
        if (entryPrice == null || markPrice == null || entryPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal pct;
        if ("SHORT".equalsIgnoreCase(side)) {
            pct = entryPrice.subtract(markPrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(entryPrice, 6, RoundingMode.HALF_UP);
        } else {
            pct = markPrice.subtract(entryPrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(entryPrice, 6, RoundingMode.HALF_UP);
        }
        return pct;
    }

    private static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] signed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(signed.length * 2);
            for (byte b : signed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("binance signature error", e);
        }
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        String normalized = safeText(rawBaseUrl, "https://fapi.binance.com");
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String safeText(String text, String fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        return text.trim();
    }

    private static BigDecimal decimalOrNull(String value) {
        if (!isNotBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = URLEncoder.encode(text, StandardCharsets.UTF_8);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }
}
