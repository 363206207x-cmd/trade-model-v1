package org.example.trademodel.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trade-model.telegram")
public class TelegramProperties {
    private boolean enabled;
    private boolean externalCallsEnabled;
    private String botToken;
    private String chatId;
    private String apiBaseUrl = "https://api.telegram.org";
    private String publicBaseUrl;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private int maxAttempts = 5;
    private int retryBaseSeconds = 5;
    private int retryMaxSeconds = 300;
    private int deliveryBatchSize = 20;
    private boolean dispatchEnabled;
    private long dispatchFixedDelayMs = 5000;
    private int claimLeaseSeconds = 60;
    private int cooldownMinutes = 15;
    private boolean allowHighQualityReduced;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { this.enabled = value; }
    public boolean isExternalCallsEnabled() { return externalCallsEnabled; }
    public void setExternalCallsEnabled(boolean value) { this.externalCallsEnabled = value; }
    public String getBotToken() { return botToken; }
    public void setBotToken(String value) { this.botToken = value; }
    public String getChatId() { return chatId; }
    public void setChatId(String value) { this.chatId = value; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String value) { this.apiBaseUrl = value; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String value) { this.publicBaseUrl = value; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int value) { this.connectTimeoutMs = positive(value, 3000); }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int value) { this.readTimeoutMs = positive(value, 5000); }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int value) { this.maxAttempts = positive(value, 5); }
    public int getRetryBaseSeconds() { return retryBaseSeconds; }
    public void setRetryBaseSeconds(int value) { this.retryBaseSeconds = positive(value, 5); }
    public int getRetryMaxSeconds() { return retryMaxSeconds; }
    public void setRetryMaxSeconds(int value) { this.retryMaxSeconds = positive(value, 300); }
    public int getDeliveryBatchSize() { return deliveryBatchSize; }
    public void setDeliveryBatchSize(int value) { this.deliveryBatchSize = positive(value, 20); }
    public boolean isDispatchEnabled() { return dispatchEnabled; }
    public void setDispatchEnabled(boolean value) { this.dispatchEnabled = value; }
    public long getDispatchFixedDelayMs() { return dispatchFixedDelayMs; }
    public void setDispatchFixedDelayMs(long value) { this.dispatchFixedDelayMs = value > 0 ? value : 5000; }
    public int getClaimLeaseSeconds() { return claimLeaseSeconds; }
    public void setClaimLeaseSeconds(int value) { this.claimLeaseSeconds = positive(value, 60); }
    public int getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(int value) { this.cooldownMinutes = positive(value, 15); }
    public boolean isAllowHighQualityReduced() { return allowHighQualityReduced; }
    public void setAllowHighQualityReduced(boolean value) { this.allowHighQualityReduced = value; }

    public boolean hasToken() { return hasText(botToken); }
    public boolean hasChatId() { return hasText(chatId); }
    public boolean hasApiBaseUrl() { return hasText(apiBaseUrl); }
    public boolean configuredForExternalDelivery() {
        return enabled && externalCallsEnabled && hasToken() && hasChatId() && hasApiBaseUrl();
    }

    private static int positive(int value, int fallback) { return value > 0 ? value : fallback; }
    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
