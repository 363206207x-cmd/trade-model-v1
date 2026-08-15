package org.example.trademodel.providercall.coinglass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trade-model.providers.coinglass")
public class CoinGlassProperties {
    public static final String OFFICIAL_BASE_URL = "https://open-api-v4.coinglass.com";
    public static final String OFFICIAL_AUTH_HEADER = "CG-API-KEY";
    public static final String OPEN_INTEREST_PATH = "/api/futures/open-interest/exchange-list";
    public static final String FUNDING_PATH = "/api/futures/funding-rate/oi-weight-history";
    public static final String LIQUIDATION_PATH = "/api/futures/liquidation/aggregated-history";
    public static final String LONG_SHORT_PATH = "/api/futures/global-long-short-account-ratio/history";

    private boolean enabled;
    private boolean externalCallsEnabled;
    private String apiKey = "";
    private String baseUrl = OFFICIAL_BASE_URL;
    private String authHeaderName = OFFICIAL_AUTH_HEADER;
    private Integer advertisedRpm;
    private double internalBudgetRatio = 0.80d;
    private int requestTimeoutMs = 5000;
    private int freshTtlSeconds = 60;
    private int staleTtlSeconds = 180;
    private int emergencyMinRefreshGapSeconds = 40;
    private int maxRetry5xx = 2;
    private int maxRetryTimeout = 1;
    private String liquidationExchangeList = "Binance,OKX,Bybit";
    private String longShortExchange = "Binance";
    private final Endpoints endpoints = new Endpoints();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isExternalCallsEnabled() { return externalCallsEnabled; }
    public void setExternalCallsEnabled(boolean externalCallsEnabled) { this.externalCallsEnabled = externalCallsEnabled; }
    @JsonIgnore
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey == null ? "" : apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getAuthHeaderName() { return authHeaderName; }
    public void setAuthHeaderName(String authHeaderName) { this.authHeaderName = authHeaderName; }
    public Integer getAdvertisedRpm() { return advertisedRpm; }
    public void setAdvertisedRpm(Integer advertisedRpm) { this.advertisedRpm = advertisedRpm; }
    public double getInternalBudgetRatio() { return internalBudgetRatio; }
    public void setInternalBudgetRatio(double internalBudgetRatio) { this.internalBudgetRatio = internalBudgetRatio; }
    public int getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(int requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }
    public int getFreshTtlSeconds() { return freshTtlSeconds; }
    public void setFreshTtlSeconds(int freshTtlSeconds) { this.freshTtlSeconds = freshTtlSeconds; }
    public int getStaleTtlSeconds() { return staleTtlSeconds; }
    public void setStaleTtlSeconds(int staleTtlSeconds) { this.staleTtlSeconds = staleTtlSeconds; }
    public int getEmergencyMinRefreshGapSeconds() { return emergencyMinRefreshGapSeconds; }
    public void setEmergencyMinRefreshGapSeconds(int value) { this.emergencyMinRefreshGapSeconds = value; }
    public int getMaxRetry5xx() { return maxRetry5xx; }
    public void setMaxRetry5xx(int maxRetry5xx) { this.maxRetry5xx = maxRetry5xx; }
    public int getMaxRetryTimeout() { return maxRetryTimeout; }
    public void setMaxRetryTimeout(int maxRetryTimeout) { this.maxRetryTimeout = maxRetryTimeout; }
    public String getLiquidationExchangeList() { return liquidationExchangeList; }
    public void setLiquidationExchangeList(String value) { this.liquidationExchangeList = value; }
    public String getLongShortExchange() { return longShortExchange; }
    public void setLongShortExchange(String value) { this.longShortExchange = value; }
    public Endpoints getEndpoints() { return endpoints; }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public CoinGlassConfigurationState configurationState() {
        return CoinGlassConfigurationState.evaluate(enabled, externalCallsEnabled, hasApiKey(), advertisedRpm);
    }

    public static class Endpoints {
        public String getOpenInterest() { return OPEN_INTEREST_PATH; }
        public String getFunding() { return FUNDING_PATH; }
        public String getLiquidation() { return LIQUIDATION_PATH; }
        public String getLongShortRatio() { return LONG_SHORT_PATH; }
    }
}
