package org.example.trademodel.providercall;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
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
    private int maxCoreAssets = 6;
    private int maxCandidateAssets = 20;
    private int maxPoolAssets = 20;
    private int failureThreshold = 3;
    private int circuitOpenSeconds = 60;
    private List<String> coreAssets = new ArrayList<>(List.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT"));
    private String scanUserId = "admin";
    private final ProviderBudgets providerBudgets = new ProviderBudgets();
    private final Profiles profiles = new Profiles();
    private final ProfileTransition profileTransition = new ProfileTransition();

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
    public void setEventRefreshMinGapSeconds(int eventRefreshMinGapSeconds) { this.eventRefreshMinGapSeconds = eventRefreshMinGapSeconds; }
    public int getMaxCoreAssets() { return maxCoreAssets; }
    public void setMaxCoreAssets(int maxCoreAssets) { this.maxCoreAssets = maxCoreAssets; }
    public int getMaxCandidateAssets() { return maxCandidateAssets; }
    public void setMaxCandidateAssets(int maxCandidateAssets) { this.maxCandidateAssets = maxCandidateAssets; }
    public int getMaxPoolAssets() { return maxPoolAssets; }
    public void setMaxPoolAssets(int maxPoolAssets) { this.maxPoolAssets = maxPoolAssets; }
    public int getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
    public int getCircuitOpenSeconds() { return circuitOpenSeconds; }
    public void setCircuitOpenSeconds(int circuitOpenSeconds) { this.circuitOpenSeconds = circuitOpenSeconds; }
    public List<String> getCoreAssets() { return List.copyOf(coreAssets); }
    public void setCoreAssets(List<String> coreAssets) {
        this.coreAssets = coreAssets == null ? new ArrayList<>() : new ArrayList<>(coreAssets);
    }
    public String getScanUserId() { return scanUserId; }
    public void setScanUserId(String scanUserId) { this.scanUserId = scanUserId; }
    public ProviderBudgets getProviderBudgets() { return providerBudgets; }
    public Profiles getProfiles() { return profiles; }
    public ProfileTransition getProfileTransition() { return profileTransition; }

    public int intervalSeconds(RuntimeScanProfile profile, AssetPriority priority, ProviderDatasetType datasetType) {
        ProfileCadence cadence = switch (profile) {
            case LOW -> profiles.low;
            case STANDARD -> profiles.standard;
            case HIGH -> profiles.high;
            case EMERGENCY -> profiles.emergency;
        };
        if (profile == RuntimeScanProfile.EMERGENCY && priority == AssetPriority.P3_POOL) {
            cadence = profiles.low;
        }
        boolean derivativesDataset = switch (datasetType) {
            case DERIVATIVES, COINGLASS_OPEN_INTEREST, COINGLASS_FUNDING,
                    COINGLASS_LIQUIDATION, COINGLASS_LONG_SHORT_RATIO -> true;
            default -> false;
        };
        CadenceByPriority values = derivativesDataset ? cadence.derivatives : cadence.price;
        return switch (priority) {
            case P0_POSITION -> values.positionSeconds;
            case P1_CORE -> values.coreSeconds;
            case P2_CANDIDATE -> values.candidateSeconds;
            case P3_POOL -> values.poolSeconds;
        };
    }

    public static class ProviderBudgets {
        private int binancePublicAdvertisedRpm = 1200;
        private int coinglassAdvertisedRpm = 300;
        private int aiAdvertisedRpm = 60;
        private int externalContextAdvertisedRpm = 60;
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
    }

    public static class ProfileCadence {
        private final CadenceByPriority price = new CadenceByPriority();
        private final CadenceByPriority derivatives = new CadenceByPriority();
        private int fullAnalysisDebounceSeconds = 20;
        static ProfileCadence low() { return of(15, 60, 120, 900, 120, 120, 300, 900); }
        static ProfileCadence standard() { return of(10, 30, 60, 600, 60, 60, 120, 600); }
        static ProfileCadence high() { return of(5, 15, 30, 300, 60, 60, 60, 300); }
        static ProfileCadence emergency() { return of(3, 5, 10, 900, 40, 40, 40, 900); }
        private static ProfileCadence of(int pp, int cp, int xp, int poolp, int pd, int cd, int xd, int poold) {
            ProfileCadence c = new ProfileCadence();
            c.price.set(pp, cp, xp, poolp);
            c.derivatives.set(pd, cd, xd, poold);
            return c;
        }
        public CadenceByPriority getPrice() { return price; }
        public CadenceByPriority getDerivatives() { return derivatives; }
        public int getFullAnalysisDebounceSeconds() { return fullAnalysisDebounceSeconds; }
        public void setFullAnalysisDebounceSeconds(int value) { this.fullAnalysisDebounceSeconds = value; }
    }

    public static class CadenceByPriority {
        private int positionSeconds;
        private int coreSeconds;
        private int candidateSeconds;
        private int poolSeconds;
        void set(int position, int core, int candidate, int pool) {
            this.positionSeconds = position; this.coreSeconds = core;
            this.candidateSeconds = candidate; this.poolSeconds = pool;
        }
        public int getPositionSeconds() { return positionSeconds; }
        public void setPositionSeconds(int value) { this.positionSeconds = value; }
        public int getCoreSeconds() { return coreSeconds; }
        public void setCoreSeconds(int value) { this.coreSeconds = value; }
        public int getCandidateSeconds() { return candidateSeconds; }
        public void setCandidateSeconds(int value) { this.candidateSeconds = value; }
        public int getPoolSeconds() { return poolSeconds; }
        public void setPoolSeconds(int value) { this.poolSeconds = value; }
    }

    public static class ProfileTransition {
        private int highMinHoldSeconds = 300;
        private int emergencyMinHoldSeconds = 120;
        private int recoveryConfirmCycles = 2;
        private int downgradeCooldownSeconds = 300;
        public int getHighMinHoldSeconds() { return highMinHoldSeconds; }
        public void setHighMinHoldSeconds(int value) { this.highMinHoldSeconds = value; }
        public int getEmergencyMinHoldSeconds() { return emergencyMinHoldSeconds; }
        public void setEmergencyMinHoldSeconds(int value) { this.emergencyMinHoldSeconds = value; }
        public int getRecoveryConfirmCycles() { return recoveryConfirmCycles; }
        public void setRecoveryConfirmCycles(int value) { this.recoveryConfirmCycles = value; }
        public int getDowngradeCooldownSeconds() { return downgradeCooldownSeconds; }
        public void setDowngradeCooldownSeconds(int value) { this.downgradeCooldownSeconds = value; }
    }
}
