package org.example.trademodel.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    }
}
