package org.example.trademodel.assetcard;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "trade-model.asset-card")
public class AssetCardProperties {
    public enum ModelMode { LEGACY, SHADOW, CANARY, ACTIVE }
    private boolean enabled;
    private boolean externalCallsEnabled;
    private boolean writerEnabled;
    private final Writer writer = new Writer();
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
    private Map<String,AssetCardModelBundle.Source> modelBundles = Map.of();
    private int labelMaturityBatchSize = 128;
    private Duration labelMaturityInterval = Duration.ofSeconds(60);
    private Path trainingExportDirectory;
    private boolean trainingExportEnabled;
    private Set<String> canarySymbols = Set.of();
    private final CollectionWindow collectionWindow = new CollectionWindow();
    private boolean retentionEnabled;
    private Path archiveDirectory;
    private long archiveMinimumFreeBytes = 10L * 1024 * 1024 * 1024;
    private int retentionBatchSize = 128;

    public CollectionWindow getCollectionWindow() { return collectionWindow; }
    public boolean isRetentionEnabled() { return retentionEnabled; }
    public void setRetentionEnabled(boolean value) { retentionEnabled = value; }
    public Path getArchiveDirectory() { return archiveDirectory; }
    public void setArchiveDirectory(Path value) { archiveDirectory = value; }
    public long getArchiveMinimumFreeBytes() { return archiveMinimumFreeBytes; }
    public void setArchiveMinimumFreeBytes(long value) {
        if (value < 1) throw new IllegalArgumentException("Positive archive free-space reserve required");
        archiveMinimumFreeBytes = value;
    }
    public int getRetentionBatchSize() { return retentionBatchSize; }
    public void setRetentionBatchSize(int value) {
        if (value < 1 || value > 500) throw new IllegalArgumentException("Retention batch must contain 1..500 records");
        retentionBatchSize = value;
    }

    /** An explicit, absolute engineering window; JVM startup never supplies or renews either clock. */
    public static final class CollectionWindow {
        private String id;
        private Instant startsAt, endsAt;
        private Path stateDirectory;
        private Set<String> symbols = Set.of();
        private int sharedIpWeightAllowancePerMinute;
        private int sharedIpWeightLimitPerMinute;
        private Instant sharedIpHeadroomConfirmedAt;
        private long maximumRestRequests = 288, maximumRestWeight = 72_000;
        private long maximumConnectionAttempts = 32, maximumControlMessages = 128;
        private long maximumNewRows = 1_100_000, maximumDatabaseGrowthBytes = 3L * 1024 * 1024 * 1024;
        private long maximumWalGrowthBytes = 8L * 1024 * 1024 * 1024, minimumFreeBytes = 20L * 1024 * 1024 * 1024;
        public String getId() { return id; }
        public void setId(String value) { id = value; }
        public Instant getStartsAt() { return startsAt; }
        public void setStartsAt(Instant value) { startsAt = value; }
        public Instant getEndsAt() { return endsAt; }
        public void setEndsAt(Instant value) { endsAt = value; }
        public Path getStateDirectory() { return stateDirectory; }
        public void setStateDirectory(Path value) { stateDirectory = value; }
        public Set<String> getSymbols() { return symbols; }
        public void setSymbols(Set<String> value) {
            if (value == null || value.isEmpty() || value.size() > 36
                    || value.stream().anyMatch(s -> s == null || !s.matches("[A-Z0-9]{2,28}USDT")))
                throw new IllegalArgumentException("Collection requires 1..36 exact registered Spot symbols");
            symbols = Set.copyOf(value);
        }
        public int getSharedIpWeightAllowancePerMinute() { return sharedIpWeightAllowancePerMinute; }
        public void setSharedIpWeightAllowancePerMinute(int value) { sharedIpWeightAllowancePerMinute = value; }
        public int getSharedIpWeightLimitPerMinute() { return sharedIpWeightLimitPerMinute; }
        public void setSharedIpWeightLimitPerMinute(int value) { sharedIpWeightLimitPerMinute = value; }
        public Instant getSharedIpHeadroomConfirmedAt() { return sharedIpHeadroomConfirmedAt; }
        public void setSharedIpHeadroomConfirmedAt(Instant value) { sharedIpHeadroomConfirmedAt = value; }
        public long getMaximumRestRequests() { return maximumRestRequests; }
        public void setMaximumRestRequests(long value) { maximumRestRequests = positive(value); }
        public long getMaximumRestWeight() { return maximumRestWeight; }
        public void setMaximumRestWeight(long value) { maximumRestWeight = positive(value); }
        public long getMaximumConnectionAttempts() { return maximumConnectionAttempts; }
        public void setMaximumConnectionAttempts(long value) { maximumConnectionAttempts = positive(value); }
        public long getMaximumControlMessages() { return maximumControlMessages; }
        public void setMaximumControlMessages(long value) { maximumControlMessages = positive(value); }
        public long getMaximumNewRows() { return maximumNewRows; }
        public void setMaximumNewRows(long value) { maximumNewRows = positive(value); }
        public long getMaximumDatabaseGrowthBytes() { return maximumDatabaseGrowthBytes; }
        public void setMaximumDatabaseGrowthBytes(long value) { maximumDatabaseGrowthBytes = positive(value); }
        public long getMaximumWalGrowthBytes() { return maximumWalGrowthBytes; }
        public void setMaximumWalGrowthBytes(long value) { maximumWalGrowthBytes = positive(value); }
        public long getMinimumFreeBytes() { return minimumFreeBytes; }
        public void setMinimumFreeBytes(long value) { minimumFreeBytes = positive(value); }
        private static long positive(long value) {
            if (value <= 0) throw new IllegalArgumentException("Positive collection budget required");
            return value;
        }
        public boolean valid() {
            return id != null && id.matches("[A-Za-z0-9_-]{8,80}") && startsAt != null && endsAt != null
                    && startsAt.isBefore(endsAt) && Duration.between(startsAt, endsAt).compareTo(Duration.ofHours(8)) <= 0
                    && stateDirectory != null && stateDirectory.isAbsolute() && !symbols.isEmpty()
                    && sharedIpWeightAllowancePerMinute >= 250 && sharedIpWeightAllowancePerMinute <= 1000
                    && sharedIpWeightLimitPerMinute >= sharedIpWeightAllowancePerMinute * 2
                    && sharedIpHeadroomConfirmedAt != null && !sharedIpHeadroomConfirmedAt.isAfter(startsAt)
                    && !sharedIpHeadroomConfirmedAt.isBefore(startsAt.minusSeconds(300));
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isExternalCallsEnabled() { return externalCallsEnabled; }
    public void setExternalCallsEnabled(boolean value) { externalCallsEnabled = value; }
    public boolean isWriterEnabled() { return writerEnabled; }
    public void setWriterEnabled(boolean value) { writerEnabled = value; }
    public Writer getWriter() { return writer; }

    /** Dedicated card connection metadata only; no password/environment-password property exists. */
    public static final class Writer {
        private String jdbcUrl;
        private String expectedDatabase;
        private Path credentialFile;
        private String credentialOwner;
        private int maximumPoolSize = 2;
        private Duration connectionTimeout = Duration.ofSeconds(5);
        public String getJdbcUrl() { return jdbcUrl; }
        public void setJdbcUrl(String value) { jdbcUrl = value; }
        public String getExpectedDatabase() { return expectedDatabase; }
        public void setExpectedDatabase(String value) { expectedDatabase = value; }
        public Path getCredentialFile() { return credentialFile; }
        public void setCredentialFile(Path value) { credentialFile = value; }
        public String getCredentialOwner() { return credentialOwner; }
        public void setCredentialOwner(String value) { credentialOwner = value; }
        public int getMaximumPoolSize() { return maximumPoolSize; }
        public void setMaximumPoolSize(int value) {
            if (value < 1 || value > 4) throw new IllegalArgumentException("Card writer pool must contain 1..4 connections");
            maximumPoolSize = value;
        }
        public Duration getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(Duration value) {
            if (value == null || value.compareTo(Duration.ofMillis(250)) < 0 || value.compareTo(Duration.ofSeconds(30)) > 0)
                throw new IllegalArgumentException("Card writer connection timeout must be 250ms..30s");
            connectionTimeout = value;
        }
    }
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
    public Map<String,AssetCardModelBundle.Source> getModelBundles() { return modelBundles; }
    public void setModelBundles(Map<String,AssetCardModelBundle.Source> value) { modelBundles = AssetCardModelBundle.normalizeSources(value); }
    public int getLabelMaturityBatchSize() { return labelMaturityBatchSize; }
    public void setLabelMaturityBatchSize(int value) {
        if(value<1 || value>10_000) throw new IllegalArgumentException("Invalid card label maturity batch size");
        labelMaturityBatchSize=value;
    }
    public Duration getLabelMaturityInterval() { return labelMaturityInterval; }
    public void setLabelMaturityInterval(Duration value) {
        if(value==null || value.isNegative() || value.isZero()) throw new IllegalArgumentException("Card label interval must be positive");
        labelMaturityInterval=value;
    }
    public Path getTrainingExportDirectory() { return trainingExportDirectory; }
    public void setTrainingExportDirectory(Path value) { trainingExportDirectory=value; }
    public boolean isTrainingExportEnabled() { return trainingExportEnabled; }
    public void setTrainingExportEnabled(boolean value) { trainingExportEnabled=value; }
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
