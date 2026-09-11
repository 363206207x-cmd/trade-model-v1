package org.example.trademodel.assetcard;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "trade-model.asset-card")
public class AssetCardProperties {
    public enum ModelMode { LEGACY, SHADOW, CANARY, ACTIVE }
    private boolean enabled;
    private boolean externalCallsEnabled;
    private boolean writerEnabled;
    private Duration barRetention = Duration.ZERO;
    private Duration featureRetention = Duration.ZERO;
    private Duration tradeRetention = Duration.ZERO;
    private Duration labelRetention = Duration.ZERO;
    private int depthWeightBudgetPerMinute = 2000;
    private ModelMode modelMode = ModelMode.SHADOW;
    private URI spotStreamBaseUri = URI.create("wss://stream.binance.com:9443/stream");
    private URI spotDepthSnapshotUri = URI.create("https://api.binance.com/api/v3/depth");
    private Duration priceTtl = Duration.ofSeconds(10);
    private int retainedBarsPerInterval = 1024;
    private String modelBundlePath;
    private String modelBundleSha256 = "";
    private Set<String> canarySymbols = Set.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isExternalCallsEnabled() { return externalCallsEnabled; }
    public void setExternalCallsEnabled(boolean value) { externalCallsEnabled = value; }
    public boolean isWriterEnabled() { return writerEnabled; }
    public void setWriterEnabled(boolean value) { writerEnabled = value; }
    public Duration getBarRetention() { return barRetention; }
    public void setBarRetention(Duration value) { barRetention = retention(value); }
    public Duration getFeatureRetention() { return featureRetention; }
    public void setFeatureRetention(Duration value) { featureRetention = retention(value); }
    public Duration getTradeRetention() { return tradeRetention; }
    public void setTradeRetention(Duration value) { tradeRetention = retention(value); }
    public Duration getLabelRetention() { return labelRetention; }
    public void setLabelRetention(Duration value) { labelRetention = retention(value); }
    private static Duration retention(Duration value) {
        if (value == null || value.isNegative() || !value.isZero() && value.compareTo(Duration.ofHours(5)) < 0)
            throw new IllegalArgumentException("Card retention must be unconfigured (zero) or at least five hours");
        return value;
    }
    public int getDepthWeightBudgetPerMinute() { return depthWeightBudgetPerMinute; }
    public void setDepthWeightBudgetPerMinute(int value) {
        if (value < 250 || value > 2000)
            throw new IllegalArgumentException("Card depth budget must reserve 250..2000 IP weight per minute");
        depthWeightBudgetPerMinute = value;
    }
    public ModelMode getModelMode() { return modelMode; }
    public void setModelMode(ModelMode value) { modelMode = value == null ? ModelMode.SHADOW : value; }
    public URI getSpotStreamBaseUri() { return spotStreamBaseUri; }
    public void setSpotStreamBaseUri(URI value) {
        if (value == null || !"wss".equals(value.getScheme())
                || !Set.of("stream.binance.com", "data-stream.binance.vision").contains(value.getHost())
                || value.getPort() != -1 && value.getPort() != 443 && value.getPort() != 9443
                || value.getUserInfo() != null || value.getQuery() != null || value.getFragment() != null
                || !"/stream".equals(value.getPath())) {
            throw new IllegalArgumentException("Asset card requires the public Binance Spot stream endpoint");
        }
        spotStreamBaseUri = value;
    }
    public URI getSpotDepthSnapshotUri() { return spotDepthSnapshotUri; }
    public void setSpotDepthSnapshotUri(URI value) {
        if (value == null || !"https".equals(value.getScheme())
                || !Set.of("api.binance.com", "data-api.binance.vision").contains(value.getHost())
                || value.getPort() != -1 && value.getPort() != 443 || value.getUserInfo() != null
                || value.getQuery() != null || value.getFragment() != null || !"/api/v3/depth".equals(value.getPath()))
            throw new IllegalArgumentException("Asset card depth bootstrap requires the public Binance Spot endpoint");
        spotDepthSnapshotUri = value;
    }
    public Duration getPriceTtl() { return priceTtl; }
    public void setPriceTtl(Duration value) {
        if (value == null || value.isNegative() || value.isZero() || value.compareTo(Duration.ofSeconds(30)) > 0)
            throw new IllegalArgumentException("Spot price TTL must be positive and at most 30 seconds");
        priceTtl = value;
    }
    public int getRetainedBarsPerInterval() { return retainedBarsPerInterval; }
    public void setRetainedBarsPerInterval(int value) {
        if (value < 100 || value > 10_000) throw new IllegalArgumentException("Invalid card bar cache size");
        retainedBarsPerInterval = value;
    }
    public String getModelBundlePath() { return modelBundlePath; }
    public void setModelBundlePath(String value) { modelBundlePath = value; }
    public String getModelBundleSha256() { return modelBundleSha256; }
    public void setModelBundleSha256(String value) {
        String checksum = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        if (!checksum.isEmpty() && !checksum.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Model bundle requires an exact SHA-256 checksum");
        modelBundleSha256 = checksum;
    }
    public Set<String> getCanarySymbols() { return canarySymbols; }
    public void setCanarySymbols(Set<String> value) {
        if (value == null) { canarySymbols = Set.of(); return; }
        Set<String> normalized = new java.util.HashSet<>();
        for (String symbol : value) {
            String canonical = symbol == null ? "" : symbol.trim().toUpperCase(java.util.Locale.ROOT);
            if (!canonical.matches("[A-Z0-9]{2,32}"))
                throw new IllegalArgumentException("Canary requires exact Spot symbols; patterns are not supported");
            normalized.add(canonical);
        }
        canarySymbols = Set.copyOf(normalized);
    }
}
