package org.example.trademodel.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramBotApiClientTest {
    private static final String TOKEN = "TEST_TOKEN";
    private static final String CHAT_ID = "41001";

    private final ObjectMapper json = new ObjectMapper();
    private final AtomicReference<Response> response = new AtomicReference<>();
    private final AtomicReference<RequestCapture> request = new AtomicReference<>();
    private HttpServer server;
    private TelegramProperties properties;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setBotToken(TOKEN);
        properties.setChatId(CHAT_ID);
        properties.setApiBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setConnectTimeoutMs(250);
        properties.setReadTimeoutMs(250);
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void probesBotAndPrivateRecipientBeforeParsingVerifiedDeliveryReceipt() throws Exception {
        respond(200, "{\"ok\":true,\"result\":{\"id\":7,\"is_bot\":true,"
                + "\"username\":\"test_bot\"}}");
        TelegramBotApiClient client = client();

        TelegramClientResult probe = client.getMe();

        assertThat(probe.success()).isTrue();
        assertThat(probe.botUsername()).isEqualTo("test_bot");
        assertThat(request.get().method()).isEqualTo("POST");
        assertThat(request.get().path()).isEqualTo("/bot" + TOKEN + "/getMe");

        respond(200, "{\"ok\":true,\"result\":{\"id\":41001,\"type\":\"private\"}}");
        TelegramClientResult recipient = client.getChat();

        assertThat(recipient.success()).isTrue();
        assertThat(recipient.recipientFingerprint())
                .isEqualTo(TelegramSecretSanitizer.recipientFingerprint(CHAT_ID));
        assertThat(request.get().path()).isEqualTo("/bot" + TOKEN + "/getChat");
        assertThat(json.readTree(request.get().body()).path("chat_id").asText()).isEqualTo(CHAT_ID);

        respond(200, "{\"ok\":true,\"result\":{\"message_id\":41,"
                + "\"from\":{\"id\":7,\"is_bot\":true,\"username\":\"test_bot\"},"
                + "\"chat\":{\"id\":41001,\"type\":\"private\"}}}");
        TelegramClientResult sent = client.sendMessage(
                new TelegramOutboundMessage("人工复核", "打开并重新校验", "https://app.example.test/recheck/9"));

        assertThat(sent.success()).isTrue();
        assertThat(sent.providerReference()).isEqualTo("41");
        assertThat(sent.botUsername()).isEqualTo("test_bot");
        assertThat(sent.recipientFingerprint()).isEqualTo(recipient.recipientFingerprint());
        JsonNode payload = json.readTree(request.get().body());
        assertThat(payload.path("chat_id").asText()).isEqualTo(CHAT_ID);
        assertThat(payload.path("text").asText()).isEqualTo("人工复核");
        assertThat(payload.has("parse_mode")).isFalse();
        assertThat(payload.has("message_thread_id")).isFalse();
        assertThat(payload.path("reply_markup").path("inline_keyboard").isArray()).isTrue();
    }

    @Test
    void incompleteOrMismatchedProviderSuccessNeverBecomesVerifiedDelivery() {
        assertUnverifiedSend("{\"ok\":true,\"result\":{\"message_id\":41}}",
                "RECIPIENT_IDENTITY_MISMATCH");
        assertUnverifiedSend("{\"ok\":true,\"result\":{\"message_id\":41,"
                        + "\"from\":{\"is_bot\":true,\"username\":\"test_bot\"},"
                        + "\"chat\":{\"id\":99999,\"type\":\"private\"}}}",
                "RECIPIENT_IDENTITY_MISMATCH");
        assertUnverifiedSend("{\"ok\":true,\"result\":{"
                        + "\"from\":{\"is_bot\":true,\"username\":\"test_bot\"},"
                        + "\"chat\":{\"id\":41001,\"type\":\"private\"}}}",
                "DELIVERY_RECEIPT_UNVERIFIED");
        assertUnverifiedSend("{\"ok\":true,\"result\":{\"message_id\":41,"
                        + "\"from\":{\"is_bot\":false,\"username\":\"test_bot\"},"
                        + "\"chat\":{\"id\":41001,\"type\":\"private\"}}}",
                "DELIVERY_RECEIPT_UNVERIFIED");
        assertUnverifiedSend("{\"ok\":true,\"result\":{\"message_id\":41,"
                        + "\"from\":{\"is_bot\":true,\"username\":\"test_bot\"},"
                        + "\"chat\":{\"id\":41001,\"type\":\"group\"}}}",
                "RECIPIENT_IDENTITY_MISMATCH");
    }

    @Test
    void botAndRecipientProbesFailClosedOnIdentityMismatch() {
        respond(200, "{\"ok\":true,\"result\":{\"id\":7,\"is_bot\":false,"
                + "\"username\":\"test_bot\"}}");
        TelegramClientResult bot = client().getMe();
        assertThat(bot.success()).isFalse();
        assertThat(bot.errorCode()).isEqualTo("BOT_IDENTITY_UNVERIFIED");

        respond(200, "{\"ok\":true,\"result\":{\"id\":99999,\"type\":\"private\"}}");
        TelegramClientResult chat = client().getChat();
        assertThat(chat.success()).isFalse();
        assertThat(chat.errorCode()).isEqualTo("RECIPIENT_IDENTITY_MISMATCH");
        assertThat(chat.retryable()).isFalse();
    }

    @Test
    void classifiesNonRetryableClientFailures() {
        assertFailure(400, "{\"ok\":false,\"error_code\":400,\"description\":\"bad\"}",
                "BAD_REQUEST", TelegramReadinessState.DEGRADED, false);
        assertFailure(401, "{\"ok\":false,\"error_code\":401,\"description\":\"auth\"}",
                "AUTH_FAILED", TelegramReadinessState.AUTH_FAILED, false);
        assertFailure(403, "{\"ok\":false,\"error_code\":403,\"description\":\"chat\"}",
                "CHAT_UNAVAILABLE", TelegramReadinessState.CHAT_UNAVAILABLE, false);
    }

    @Test
    void honorsRateLimitAndRetriesProviderFailures() {
        respond(429, "{\"ok\":false,\"error_code\":429,\"description\":\"slow\","
                + "\"parameters\":{\"retry_after\":17}}");
        TelegramClientResult rateLimited = client().sendMessage(new TelegramOutboundMessage("test", null, null));
        assertThat(rateLimited.errorCode()).isEqualTo("RATE_LIMITED");
        assertThat(rateLimited.retryAfterSeconds()).isEqualTo(17);
        assertThat(rateLimited.retryable()).isTrue();

        assertFailure(500, "{\"ok\":false,\"error_code\":500,\"description\":\"down\"}",
                "PROVIDER_UNAVAILABLE", TelegramReadinessState.PROVIDER_UNAVAILABLE, true);
        assertFailure(200, "{\"ok\":false,\"error_code\":500,\"description\":\"down\"}",
                "PROVIDER_UNAVAILABLE", TelegramReadinessState.PROVIDER_UNAVAILABLE, true);
    }

    @Test
    void invalidJsonOkFalseAndTimeoutNeverBecomeSent() {
        assertFailure(200, "not-json", "INVALID_RESPONSE",
                TelegramReadinessState.PROVIDER_UNAVAILABLE, false);
        assertFailure(200, "{\"ok\":false,\"error_code\":418,\"description\":\"rejected\"}",
                "TELEGRAM_REJECTED", TelegramReadinessState.PROVIDER_UNAVAILABLE, false);

        response.set(new Response(200, "{\"ok\":true}", 500));
        properties.setReadTimeoutMs(25);
        TelegramClientResult timeout = client().sendMessage(new TelegramOutboundMessage("test", null, null));
        assertThat(timeout.success()).isFalse();
        assertThat(timeout.errorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(timeout.retryable()).isTrue();
    }

    @Test
    void providerErrorsAndUtilityEscapingNeverExposeSecrets() {
        respond(401, "{\"ok\":false,\"error_code\":401,\"description\":\"bot" + TOKEN
                + " recipient " + CHAT_ID + " rejected\"}");
        TelegramClientResult result = client().sendMessage(new TelegramOutboundMessage("test", null, null));

        assertThat(result.errorMessage()).doesNotContain(TOKEN, CHAT_ID);
        assertThat(result.errorMessage()).contains("[REDACTED");
        assertThat(TelegramSecretSanitizer.sanitize(
                "https://api.telegram.org/bot" + TOKEN + "/sendMessage?chat_id=" + CHAT_ID, properties))
                .doesNotContain(TOKEN, CHAT_ID);
        assertThat(TelegramSecretSanitizer.escapeHtml("<b>A&B</b>"))
                .isEqualTo("&lt;b&gt;A&amp;B&lt;/b&gt;");
    }

    private void assertFailure(int status, String body, String code,
                               TelegramReadinessState state, boolean retryable) {
        respond(status, body);
        TelegramClientResult result = client().sendMessage(new TelegramOutboundMessage("test", null, null));
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(code);
        assertThat(result.readinessState()).isEqualTo(state);
        assertThat(result.retryable()).isEqualTo(retryable);
    }

    private void assertUnverifiedSend(String body, String code) {
        respond(200, body);
        TelegramClientResult result = client().sendMessage(new TelegramOutboundMessage("test", null, null));
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(code);
        assertThat(result.retryable()).isFalse();
    }

    private TelegramBotApiClient client() {
        return new TelegramBotApiClient(properties, json);
    }

    private void respond(int status, String body) {
        response.set(new Response(status, body, 0));
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] input = exchange.getRequestBody().readAllBytes();
        request.set(new RequestCapture(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                new String(input, StandardCharsets.UTF_8)));
        Response value = response.get();
        if (value.delayMs() > 0) {
            try {
                Thread.sleep(value.delayMs());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] output = value.body().getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(value.status(), output.length);
            exchange.getResponseBody().write(output);
        } catch (IOException clientTimedOut) {
            // Expected for timeout coverage.
        } finally {
            exchange.close();
        }
    }

    private record Response(int status, String body, long delayMs) {
    }

    private record RequestCapture(String method, String path, String body) {
    }
}
