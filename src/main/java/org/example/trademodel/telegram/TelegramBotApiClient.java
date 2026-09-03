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
        return post("sendMessage", payload, ResponseKind.SEND_MESSAGE);
    }

    @Override
    public TelegramClientResult getMe() {
        if (!properties.configuredForExternalDelivery()) {
            return TelegramClientResult.failure(0, configurationState(), "NOT_CONFIGURED",
                    "Telegram provider probe is not configured", null, false);
        }
        return post("getMe", Map.of(), ResponseKind.GET_ME);
    }

    @Override
    public TelegramClientResult getChat() {
        if (!properties.configuredForExternalDelivery()) {
            return TelegramClientResult.failure(0, configurationState(), "NOT_CONFIGURED",
                    "Telegram recipient probe is not configured", null, false);
        }
        return post("getChat", Map.of("chat_id", properties.getChatId()), ResponseKind.GET_CHAT);
    }

    private TelegramClientResult post(String method, Map<String, Object> payload, ResponseKind responseKind) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(endpoint(method))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parse(response.statusCode(), response.body(), responseKind);
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

    private TelegramClientResult parse(int status, String responseBody, ResponseKind responseKind) {
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
            return verifiedSuccess(status, result, responseKind);
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

    private TelegramClientResult verifiedSuccess(int status, JsonNode result, ResponseKind responseKind) {
        if (responseKind == ResponseKind.GET_ME) {
            String username = text(result, "username");
            if (!result.path("is_bot").asBoolean(false) || !positiveNumber(text(result, "id"))
                    || !hasText(username)) {
                return unverified(status, "BOT_IDENTITY_UNVERIFIED",
                        "Telegram bot identity was missing from the provider response");
            }
            return TelegramClientResult.success(status, null, username.trim(), null);
        }

        JsonNode chat = responseKind == ResponseKind.GET_CHAT ? result : result.path("chat");
        String chatId = text(chat, "id");
        String chatType = text(chat, "type");
        String actualRecipient = TelegramSecretSanitizer.recipientFingerprint(chatId);
        String configuredRecipient = TelegramSecretSanitizer.recipientFingerprint(properties.getChatId());
        if (!"private".equals(chatType) || configuredRecipient == null
                || !configuredRecipient.equals(actualRecipient)) {
            return unverified(status, "RECIPIENT_IDENTITY_MISMATCH",
                    "Telegram recipient identity did not match the configured private chat");
        }
        if (responseKind == ResponseKind.GET_CHAT) {
            return TelegramClientResult.success(status, null, null, actualRecipient);
        }

        String reference = text(result, "message_id");
        JsonNode sender = result.path("from");
        String username = text(sender, "username");
        if (!positiveNumber(reference) || !sender.path("is_bot").asBoolean(false) || !hasText(username)) {
            return unverified(status, "DELIVERY_RECEIPT_UNVERIFIED",
                    "Telegram delivery receipt did not contain a verifiable message and bot identity");
        }
        return TelegramClientResult.success(status, reference, username.trim(), actualRecipient);
    }

    private static boolean positiveNumber(String value) {
        return value != null && value.matches("[1-9][0-9]*");
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static TelegramClientResult unverified(int status, String code, String message) {
        return TelegramClientResult.failure(status, TelegramReadinessState.DEGRADED,
                code, message, null, false);
    }

    private enum ResponseKind {
        GET_ME,
        GET_CHAT,
        SEND_MESSAGE
    }
}
