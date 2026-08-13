package org.example.trademodel.position;

import java.util.List;

public class PositionProviderResult {

    private final String sourceType;
    private final String sourceName;
    private final List<PositionSnapshot> openPositions;
    private final String configuredProviderType;
    private final boolean fallbackOccurred;
    private final String fallbackReason;
    private final boolean authoritativeSnapshot;

    public PositionProviderResult(String sourceType, String sourceName, List<PositionSnapshot> openPositions) {
        this(sourceType, sourceName, openPositions, null, false, null, true);
    }

    public PositionProviderResult(String sourceType,
                                  String sourceName,
                                  List<PositionSnapshot> openPositions,
                                  String configuredProviderType,
                                  boolean fallbackOccurred,
                                  String fallbackReason) {
        this(
                sourceType,
                sourceName,
                openPositions,
                configuredProviderType,
                fallbackOccurred,
                fallbackReason,
                !fallbackOccurred
        );
    }

    public PositionProviderResult(String sourceType,
                                  String sourceName,
                                  List<PositionSnapshot> openPositions,
                                  String configuredProviderType,
                                  boolean fallbackOccurred,
                                  String fallbackReason,
                                  boolean authoritativeSnapshot) {
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.openPositions = openPositions;
        this.configuredProviderType = configuredProviderType;
        this.fallbackOccurred = fallbackOccurred;
        this.fallbackReason = fallbackReason;
        this.authoritativeSnapshot = authoritativeSnapshot;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public List<PositionSnapshot> getOpenPositions() {
        return openPositions;
    }

    public String getConfiguredProviderType() {
        return configuredProviderType;
    }

    public boolean isFallbackOccurred() {
        return fallbackOccurred;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public boolean isAuthoritativeSnapshot() {
        return authoritativeSnapshot;
    }
}
