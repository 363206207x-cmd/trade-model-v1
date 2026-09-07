package org.example.trademodel.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;

public class ProviderReadinessVO {
    private String marketDataProviderStatus = "WAITING_SYNC";
    private String aiProviderStatus = "WAITING_SYNC";
    private String externalContextProviderStatus = "WAITING_SYNC";
    private String dataSourceText = "WAITING_SYNC";
    private List<ProviderStatusVO> providers = new ArrayList<>();
    private Map<String, String> summary = new LinkedHashMap<>();

    public String getMarketDataProviderStatus() {
        return marketDataProviderStatus;
    }

    public void setMarketDataProviderStatus(String marketDataProviderStatus) {
        this.marketDataProviderStatus = marketDataProviderStatus;
    }

    public String getAiProviderStatus() {
        return aiProviderStatus;
    }

    public void setAiProviderStatus(String aiProviderStatus) {
        this.aiProviderStatus = aiProviderStatus;
    }

    public String getExternalContextProviderStatus() {
        return externalContextProviderStatus;
    }

    public void setExternalContextProviderStatus(String externalContextProviderStatus) {
        this.externalContextProviderStatus = externalContextProviderStatus;
    }

    public String getDataSourceText() {
        return dataSourceText;
    }

    public void setDataSourceText(String dataSourceText) {
        this.dataSourceText = dataSourceText;
    }

    public List<ProviderStatusVO> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderStatusVO> providers) {
        this.providers = providers == null ? new ArrayList<>() : new ArrayList<>(providers);
    }

    public Map<String, String> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, String> summary) {
        this.summary = summary == null ? new LinkedHashMap<>() : new LinkedHashMap<>(summary);
    }

    public static class ProviderStatusVO {
        private String category;
        private String name;
        private String status = "WAITING_SYNC";
        private Boolean enabled = false;
        private Boolean configured = false;
        private Boolean connected = false;
        private String reason;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant lastSuccessAt;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant lastAttemptAt;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant providerDataAt;
        private String runtimeState;
        private long stateVersion;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant nextCheckAt;
        private String nextCheckStatus = "UNSCHEDULED";
        private String freshness;
        private Long latencyMs;
        private String impact;
        private String retryStatus;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Boolean getConfigured() {
            return configured;
        }

        public void setConfigured(Boolean configured) {
            this.configured = configured;
        }

        public Boolean getConnected() {
            return connected;
        }

        public void setConnected(Boolean connected) {
            this.connected = connected;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public Instant getLastSuccessAt() { return lastSuccessAt; }
        public Instant getNextCheckAt() { return nextCheckAt; }
        public void setNextCheckAt(Instant value) { nextCheckAt = value; }
        public Instant getLastAttemptAt() { return lastAttemptAt; }
        public void setLastAttemptAt(Instant value) { lastAttemptAt = value; }
        public Instant getProviderDataAt() { return providerDataAt; }
        public void setProviderDataAt(Instant value) { providerDataAt = value; }
        public String getRuntimeState() { return runtimeState; }
        public void setRuntimeState(String value) { runtimeState = value; }
        public long getStateVersion() { return stateVersion; }
        public void setStateVersion(long value) { stateVersion = value; }
        public String getNextCheckStatus() { return nextCheckStatus; }
        public void setNextCheckStatus(String value) { nextCheckStatus = value; }
        public void setLastSuccessAt(Instant value) { this.lastSuccessAt = value; }
        public String getFreshness() { return freshness; }
        public void setFreshness(String value) { this.freshness = value; }
        public Long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(Long value) { this.latencyMs = value; }
        public String getImpact() { return impact; }
        public void setImpact(String value) { this.impact = value; }
        public String getRetryStatus() { return retryStatus; }
        public void setRetryStatus(String value) { this.retryStatus = value; }
    }
}
