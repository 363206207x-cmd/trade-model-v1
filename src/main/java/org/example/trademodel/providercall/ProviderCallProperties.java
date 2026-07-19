package org.example.trademodel.providercall;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "trade-model.provider-call")
public class ProviderCallProperties {
    private boolean enabled;
    private boolean schedulerEnabled;
    private boolean profileEscalationEnabled;
    private boolean externalCallsEnabled;
    private UserScanProfile baseProfile = UserScanProfile.AUTO;
    private boolean autoEscalationEnabled = true;
    private double internalBudgetRatio = 0.80d;
    private double emergencyReserveRatio = 0.20d;
    private int eventRefreshMinGapSeconds = 40;
    private int perSymbolMinimumGapSeconds = 1;
    private int maxWatchlistAssets = 20;
    private int maxCandidateAssets = 20;
    private int maxDiscoveryAssets = 20;
    private int maxConcurrentProviderCalls = 8;
    private int maxConcurrentAiCalls = 3;
    private int maxQueuedCalls = 32;
    private int reservedPrioritySlots = 2;
    private int globalAdvertisedRequestsPerMinute = 1500;
    private int failureThreshold = 3;
    private int circuitOpenSeconds = 60;
    private String scanUserId = "admin";
    private final ProviderBudgets providerBudgets = new ProviderBudgets();
    private final Profiles profiles = new Profiles();
    private final ProfileTransition profileTransition = new ProfileTransition();

    @PostConstruct
    public void validate() {
        if (baseProfile == null) fail("base-profile is required");
        ratio(internalBudgetRatio, "internal-budget-ratio");
        ratio(emergencyReserveRatio, "emergency-reserve-ratio");
        if (internalBudgetRatio + emergencyReserveRatio > 1.000001d) {
            fail("internal-budget-ratio plus emergency-reserve-ratio must not exceed 1");
        }
        positive(eventRefreshMinGapSeconds, "event-refresh-min-gap-seconds");
        positive(perSymbolMinimumGapSeconds, "per-symbol-minimum-gap-seconds");
        positive(maxWatchlistAssets, "max-watchlist-assets");
        positive(maxCandidateAssets, "max-candidate-assets");
        positive(maxDiscoveryAssets, "max-discovery-assets");
        positive(maxConcurrentProviderCalls, "max-concurrent-provider-calls");
        positive(maxConcurrentAiCalls, "max-concurrent-ai-calls");
        positive(maxQueuedCalls, "max-queued-calls");
        positive(globalAdvertisedRequestsPerMinute, "global-advertised-requests-per-minute");
        if (reservedPrioritySlots < 0 || reservedPrioritySlots >= maxConcurrentProviderCalls) {
            fail("reserved-priority-slots must be between 0 and max-concurrent-provider-calls - 1");
        }
        positive(failureThreshold, "failure-threshold");
        positive(circuitOpenSeconds, "circuit-open-seconds");
        profiles.validate(eventRefreshMinGapSeconds);
        profileTransition.validate();
        providerBudgets.validate();
    }

    public int intervalSeconds(RuntimeScanProfile profile, AssetPriority priority,
                               ProviderDatasetType datasetType) {
        ProfileCadence cadence = profile(profile);
        if (profile == RuntimeScanProfile.EMERGENCY && priority == AssetPriority.P3_DISCOVERY) {
            cadence = profiles.low;
        }
        return switch (datasetType) {
            case PRICE -> cadence.price.forPriority(priority);
            case OHLCV -> cadence.ohlcv.forPriority(priority);
            case DERIVATIVES, COINGLASS_OPEN_INTEREST, COINGLASS_FUNDING,
                    COINGLASS_LIQUIDATION, COINGLASS_LONG_SHORT_RATIO -> cadence.derivatives.forPriority(priority);
            case EXTERNAL_CONTEXT -> cadence.externalContextSeconds;
            case AI_REVIEW -> cadence.aiCheckpointDebounceSeconds;
        };
    }

    public int fullAnalysisDebounceSeconds(RuntimeScanProfile profile) {
        return profile(profile).fullAnalysisDebounceSeconds;
    }

    private ProfileCadence profile(RuntimeScanProfile profile) {
        if (profile == null) fail("runtime profile is required");
        return switch (profile) {
            case LOW -> profiles.low;
            case STANDARD -> profiles.standard;
            case HIGH -> profiles.high;
            case EMERGENCY -> profiles.emergency;
        };
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean schedulerEnabled) { this.schedulerEnabled = schedulerEnabled; }
    public boolean isProfileEscalationEnabled() { return profileEscalationEnabled; }
    public void setProfileEscalationEnabled(boolean profileEscalationEnabled) { this.profileEscalationEnabled = profileEscalationEnabled; }
    public boolean isExternalCallsEnabled() { return externalCallsEnabled; }
    public void setExternalCallsEnabled(boolean externalCallsEnabled) { this.externalCallsEnabled = externalCallsEnabled; }
    public UserScanProfile getBaseProfile() { return baseProfile; }
    public void setBaseProfile(UserScanProfile baseProfile) { this.baseProfile = baseProfile; }
    public boolean isAutoEscalationEnabled() { return autoEscalationEnabled; }
    public void setAutoEscalationEnabled(boolean autoEscalationEnabled) { this.autoEscalationEnabled = autoEscalationEnabled; }
    public double getInternalBudgetRatio() { return internalBudgetRatio; }
    public void setInternalBudgetRatio(double internalBudgetRatio) { this.internalBudgetRatio = internalBudgetRatio; }
    public double getEmergencyReserveRatio() { return emergencyReserveRatio; }
    public void setEmergencyReserveRatio(double emergencyReserveRatio) { this.emergencyReserveRatio = emergencyReserveRatio; }
    public int getEventRefreshMinGapSeconds() { return eventRefreshMinGapSeconds; }
    public void setEventRefreshMinGapSeconds(int value) { this.eventRefreshMinGapSeconds = value; }
    public int getPerSymbolMinimumGapSeconds() { return perSymbolMinimumGapSeconds; }
    public void setPerSymbolMinimumGapSeconds(int value) { this.perSymbolMinimumGapSeconds = value; }
    public int getMaxWatchlistAssets() { return maxWatchlistAssets; }
    public void setMaxWatchlistAssets(int value) { this.maxWatchlistAssets = value; }
    public int getMaxCandidateAssets() { return maxCandidateAssets; }
    public void setMaxCandidateAssets(int value) { this.maxCandidateAssets = value; }
    public int getMaxDiscoveryAssets() { return maxDiscoveryAssets; }
    public void setMaxDiscoveryAssets(int value) { this.maxDiscoveryAssets = value; }
    public int getMaxConcurrentProviderCalls() { return maxConcurrentProviderCalls; }
    public void setMaxConcurrentProviderCalls(int value) { this.maxConcurrentProviderCalls = value; }
    public int getMaxConcurrentAiCalls() { return maxConcurrentAiCalls; }
    public void setMaxConcurrentAiCalls(int value) { this.maxConcurrentAiCalls = value; }
    public int getMaxQueuedCalls() { return maxQueuedCalls; }
    public void setMaxQueuedCalls(int value) { this.maxQueuedCalls = value; }
    public int getReservedPrioritySlots() { return reservedPrioritySlots; }
    public void setReservedPrioritySlots(int value) { this.reservedPrioritySlots = value; }
    public int getGlobalAdvertisedRequestsPerMinute() { return globalAdvertisedRequestsPerMinute; }
    public void setGlobalAdvertisedRequestsPerMinute(int value) { this.globalAdvertisedRequestsPerMinute = value; }
    public int getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
    public int getCircuitOpenSeconds() { return circuitOpenSeconds; }
    public void setCircuitOpenSeconds(int circuitOpenSeconds) { this.circuitOpenSeconds = circuitOpenSeconds; }
    public String getScanUserId() { return scanUserId; }
    public void setScanUserId(String scanUserId) { this.scanUserId = scanUserId; }
    public ProviderBudgets getProviderBudgets() { return providerBudgets; }
    public Profiles getProfiles() { return profiles; }
    public ProfileTransition getProfileTransition() { return profileTransition; }

    public static class ProviderBudgets {
        private int binancePublicAdvertisedRpm = 1200;
        private int coinglassAdvertisedRpm = 300;
        private int aiAdvertisedRpm = 60;
        private int externalContextAdvertisedRpm = 60;
        void validate() {
            positive(binancePublicAdvertisedRpm, "provider-budgets.binance-public-advertised-rpm");
            positive(coinglassAdvertisedRpm, "provider-budgets.coinglass-advertised-rpm");
            positive(aiAdvertisedRpm, "provider-budgets.ai-advertised-rpm");
            positive(externalContextAdvertisedRpm, "provider-budgets.external-context-advertised-rpm");
        }
        public int getBinancePublicAdvertisedRpm() { return binancePublicAdvertisedRpm; }
        public void setBinancePublicAdvertisedRpm(int value) { this.binancePublicAdvertisedRpm = value; }
        public int getCoinglassAdvertisedRpm() { return coinglassAdvertisedRpm; }
        public void setCoinglassAdvertisedRpm(int value) { this.coinglassAdvertisedRpm = value; }
        public int getAiAdvertisedRpm() { return aiAdvertisedRpm; }
        public void setAiAdvertisedRpm(int value) { this.aiAdvertisedRpm = value; }
        public int getExternalContextAdvertisedRpm() { return externalContextAdvertisedRpm; }
        public void setExternalContextAdvertisedRpm(int value) { this.externalContextAdvertisedRpm = value; }
    }

    public static class Profiles {
        private final ProfileCadence low = ProfileCadence.low();
        private final ProfileCadence standard = ProfileCadence.standard();
        private final ProfileCadence high = ProfileCadence.high();
        private final ProfileCadence emergency = ProfileCadence.emergency();
        public ProfileCadence getLow() { return low; }
        public ProfileCadence getStandard() { return standard; }
        public ProfileCadence getHigh() { return high; }
        public ProfileCadence getEmergency() { return emergency; }
        void validate(int derivativesMinGap) {
            low.validate("low", derivativesMinGap);
            standard.validate("standard", derivativesMinGap);
            high.validate("high", derivativesMinGap);
            emergency.validate("emergency", derivativesMinGap);
            if (low.price.positionSeconds > 15) fail("LOW position price interval must not exceed 15 seconds");
            emergency.notSlowerThan(high);
        }
    }

    public static class ProfileCadence {
        private final CadenceByPriority price = new CadenceByPriority();
        private final CadenceByPriority ohlcv = new CadenceByPriority();
        private final CadenceByPriority derivatives = new CadenceByPriority();
        private int externalContextSeconds;
        private int fullAnalysisDebounceSeconds;
        private int aiCheckpointDebounceSeconds;

        static ProfileCadence low() {
            return of(new int[]{15, 60, 120, 900}, new int[]{60, 300, 300, 900},
                    new int[]{120, 120, 300, 900}, 900, 300, 300);
        }
        static ProfileCadence standard() {
            return of(new int[]{10, 30, 60, 600}, new int[]{60, 180, 180, 600},
                    new int[]{60, 60, 120, 600}, 600, 60, 120);
        }
        static ProfileCadence high() {
            return of(new int[]{5, 15, 30, 300}, new int[]{60, 120, 120, 300},
                    new int[]{60, 60, 60, 300}, 300, 20, 60);
        }
        static ProfileCadence emergency() {
            return of(new int[]{3, 5, 10, 900}, new int[]{60, 60, 60, 900},
                    new int[]{40, 40, 40, 900}, 120, 20, 60);
        }
        private static ProfileCadence of(int[] priceValues, int[] ohlcvValues, int[] derivativesValues,
                                         int external, int analysis, int ai) {
            ProfileCadence cadence = new ProfileCadence();
            cadence.price.set(priceValues);
            cadence.ohlcv.set(ohlcvValues);
            cadence.derivatives.set(derivativesValues);
            cadence.externalContextSeconds = external;
            cadence.fullAnalysisDebounceSeconds = analysis;
            cadence.aiCheckpointDebounceSeconds = ai;
            return cadence;
        }
        void validate(String name, int derivativesMinGap) {
            price.validate(name + ".price", 1);
            ohlcv.validate(name + ".ohlcv", 1);
            derivatives.validate(name + ".derivatives", derivativesMinGap);
            positive(externalContextSeconds, name + ".external-context-seconds");
            positive(fullAnalysisDebounceSeconds, name + ".full-analysis-debounce-seconds");
            positive(aiCheckpointDebounceSeconds, name + ".ai-checkpoint-debounce-seconds");
        }
        void notSlowerThan(ProfileCadence high) {
            price.notSlowerThan(high.price, "emergency.price");
            ohlcv.notSlowerThan(high.ohlcv, "emergency.ohlcv");
            derivatives.notSlowerThan(high.derivatives, "emergency.derivatives");
            if (externalContextSeconds > high.externalContextSeconds
                    || fullAnalysisDebounceSeconds > high.fullAnalysisDebounceSeconds
                    || aiCheckpointDebounceSeconds > high.aiCheckpointDebounceSeconds) {
                fail("EMERGENCY cadence must not be slower than HIGH for affected datasets");
            }
        }
        public CadenceByPriority getPrice() { return price; }
        public CadenceByPriority getOhlcv() { return ohlcv; }
        public CadenceByPriority getDerivatives() { return derivatives; }
        public int getExternalContextSeconds() { return externalContextSeconds; }
        public void setExternalContextSeconds(int value) { this.externalContextSeconds = value; }
        public int getFullAnalysisDebounceSeconds() { return fullAnalysisDebounceSeconds; }
        public void setFullAnalysisDebounceSeconds(int value) { this.fullAnalysisDebounceSeconds = value; }
        public int getAiCheckpointDebounceSeconds() { return aiCheckpointDebounceSeconds; }
        public void setAiCheckpointDebounceSeconds(int value) { this.aiCheckpointDebounceSeconds = value; }
    }

    public static class CadenceByPriority {
        private int positionSeconds;
        private int watchlistSeconds;
        private int candidateSeconds;
        private int discoverySeconds;
        void set(int[] values) {
            this.positionSeconds = values[0];
            this.watchlistSeconds = values[1];
            this.candidateSeconds = values[2];
            this.discoverySeconds = values[3];
        }
        int forPriority(AssetPriority priority) {
            return switch (priority) {
                case P0_POSITION -> positionSeconds;
                case P1_WATCHLIST -> watchlistSeconds;
                case P2_CANDIDATE -> candidateSeconds;
                case P3_DISCOVERY -> discoverySeconds;
            };
        }
        void validate(String name, int minimum) {
            if (positionSeconds < minimum || watchlistSeconds < minimum
                    || candidateSeconds < minimum || discoverySeconds < minimum) {
                fail(name + " values must be at least " + minimum + " seconds");
            }
        }
        void notSlowerThan(CadenceByPriority high, String name) {
            if (positionSeconds > high.positionSeconds || watchlistSeconds > high.watchlistSeconds
                    || candidateSeconds > high.candidateSeconds) {
                fail(name + " must not be slower than HIGH for affected assets");
            }
        }
        public int getPositionSeconds() { return positionSeconds; }
        public void setPositionSeconds(int value) { this.positionSeconds = value; }
        public int getWatchlistSeconds() { return watchlistSeconds; }
        public void setWatchlistSeconds(int value) { this.watchlistSeconds = value; }
        public int getCandidateSeconds() { return candidateSeconds; }
        public void setCandidateSeconds(int value) { this.candidateSeconds = value; }
        public int getDiscoverySeconds() { return discoverySeconds; }
        public void setDiscoverySeconds(int value) { this.discoverySeconds = value; }
    }

    public static class ProfileTransition {
        private int highMinHoldSeconds = 300;
        private int emergencyMinHoldSeconds = 120;
        private int recoveryConfirmCycles = 2;
        private int downgradeCooldownSeconds = 300;
        void validate() {
            positive(highMinHoldSeconds, "profile-transition.high-min-hold-seconds");
            positive(emergencyMinHoldSeconds, "profile-transition.emergency-min-hold-seconds");
            positive(recoveryConfirmCycles, "profile-transition.recovery-confirm-cycles");
            positive(downgradeCooldownSeconds, "profile-transition.downgrade-cooldown-seconds");
        }
        public int getHighMinHoldSeconds() { return highMinHoldSeconds; }
        public void setHighMinHoldSeconds(int value) { this.highMinHoldSeconds = value; }
        public int getEmergencyMinHoldSeconds() { return emergencyMinHoldSeconds; }
        public void setEmergencyMinHoldSeconds(int value) { this.emergencyMinHoldSeconds = value; }
        public int getRecoveryConfirmCycles() { return recoveryConfirmCycles; }
        public void setRecoveryConfirmCycles(int value) { this.recoveryConfirmCycles = value; }
        public int getDowngradeCooldownSeconds() { return downgradeCooldownSeconds; }
        public void setDowngradeCooldownSeconds(int value) { this.downgradeCooldownSeconds = value; }
    }

    private static void ratio(double value, String field) {
        if (!Double.isFinite(value) || value <= 0d || value > 1d) fail(field + " must be in (0, 1]");
    }

    private static void positive(int value, String field) {
        if (value <= 0) fail(field + " must be greater than 0");
    }

    private static void fail(String message) {
        throw new IllegalStateException("invalid provider-call configuration: " + message);
    }
}
