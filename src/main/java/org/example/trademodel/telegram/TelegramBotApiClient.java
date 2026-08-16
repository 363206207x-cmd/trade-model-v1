package org.example.trademodel.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBotApiClient implements TelegramClient {
    private final TelegramProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public TelegramBotApiClient(TelegramProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build());
    }

    TelegramBotApiClient(TelegramProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public TelegramClientResult sendMessage(TelegramOutboundMessage message) {
        if (!properties.configuredForExternalDelivery()) {
            return TelegramClientResult.failure(0, configurationState(), "NOT_CONFIGURED",
                    "Telegram delivery is not configured", null, false);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_id", properties.getChatId());
        payload.put("text", message.text());
        payload.put("disable_web_page_preview", true);
        if (message.buttonUrl() != null) {
            payload.put("reply_markup", Map.of("inline_keyboard", List.of(List.of(Map.of(
                    "text", message.buttonLabel(), "url", message.buttonUrl())))));
        }
        return post("sendMessage", payload, false);
    }

    @Override
    public TelegramClientResult getMe() {
        if (!properties.configuredForExternalDelivery()) {
            return TelegramClientResult.failure(0, configurationState(), "NOT_CONFIGURED",
                    "Telegram provider probe is not configured", null, false);
        }
        return post("getMe", Map.of(), true);
    }

    private TelegramClientResult post(String method, Map<String, Object> payload, boolean botProbe) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(endpoint(method))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parse(response.statusCode(), response.body(), botProbe);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return providerFailure("INTERRUPTED");
        } catch (Exception exception) {
            return providerFailure(exception.getClass().getSimpleName());
        }
    }

    private URI endpoint(String method) {
        String root = properties.getApiBaseUrl().trim().replaceAll("/+$", "");
        return URI.create(root + "/bot" + properties.getBotToken().trim() + "/" + method);
    }

    private TelegramClientResult parse(int status, String responseBody, boolean botProbe) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody == null ? "" : responseBody);
        } catch (Exception invalidJson) {
            return TelegramClientResult.failure(status, TelegramReadinessState.PROVIDER_UNAVAILABLE,
                    "INVALID_RESPONSE", "Telegram returned an invalid response", null, status >= 500 || status == 0);
        }
        boolean ok = root.path("ok").asBoolean(false);
        if (status >= 200 && status < 300 && ok) {
            JsonNode result = root.path("result");
            String reference = botProbe ? null : text(result, "message_id");
            String username = botProbe ? text(result, "username") : null;
            return TelegramClientResult.success(status, reference, username);
        }
        int providerCode = root.path("error_code").asInt(status);
        String description = TelegramSecretSanitizer.sanitize(root.path("description").asText(null), properties);
        Integer retryAfter = root.path("parameters").path("retry_after").isNumber()
                ? root.path("parameters").path("retry_after").asInt() : null;
        return switch (providerCode) {
            case 400 -> TelegramClientResult.failure(status, TelegramReadinessState.DEGRADED,
                    "BAD_REQUEST", description, null, false);
            case 401 -> TelegramClientResult.failure(status, TelegramReadinessState.AUTH_FAILED,
                    "AUTH_FAILED", description, null, false);
            case 403 -> TelegramClientResult.failure(status, TelegramReadinessState.CHAT_UNAVAILABLE,
                    "CHAT_UNAVAILABLE", description, null, false);
            case 429 -> TelegramClientResult.failure(status, TelegramReadinessState.RATE_LIMITED,
                    "RATE_LIMITED", description, retryAfter, true);
            default -> {
                boolean unavailable = status >= 500 || providerCode >= 500 || status == 0;
                yield TelegramClientResult.failure(status, TelegramReadinessState.PROVIDER_UNAVAILABLE,
                        unavailable ? "PROVIDER_UNAVAILABLE" : "TELEGRAM_REJECTED",
                        description, null, unavailable);
            }
        };
    }

    private TelegramClientResult providerFailure(String reasonCode) {
        return TelegramClientResult.failure(0, TelegramReadinessState.PROVIDER_UNAVAILABLE,
                "PROVIDER_UNAVAILABLE", TelegramSecretSanitizer.sanitize(reasonCode, properties), null, true);
    }

    private TelegramReadinessState configurationState() {
        if (!properties.isEnabled() || !properties.isExternalCallsEnabled()) {
            return TelegramReadinessState.NOT_CONFIGURED;
        }
        if (!properties.hasToken()) return TelegramReadinessState.TOKEN_MISSING;
        if (!properties.hasChatId()) return TelegramReadinessState.CHAT_ID_MISSING;
        return TelegramReadinessState.NOT_CONFIGURED;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
