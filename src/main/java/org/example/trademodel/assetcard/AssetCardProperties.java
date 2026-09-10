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
    public void setCanarySymbols(Set<String> value) { canarySymbols = value == null ? Set.of() : Set.copyOf(value); }
}
