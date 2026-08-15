package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderRateBudgetManager implements ProviderRateBudget {
    private static final String GLOBAL = "__GLOBAL__";

    private final ProviderCallProperties properties;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Map<String, Integer> advertised = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastSymbolReservations = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public ProviderRateBudgetManager(ProviderCallProperties properties) {
        this(properties, Clock.systemUTC());
        register("BINANCE", properties.getProviderBudgets().getBinancePublicAdvertisedRpm());
        register("BINANCE_PUBLIC", properties.getProviderBudgets().getBinancePublicAdvertisedRpm());
        register("KRAKEN", properties.getProviderBudgets().getBinancePublicAdvertisedRpm());
        registerIfPositive("COINGLASS", properties.getProviderBudgets().getCoinglassAdvertisedRpm());
        register("AI", properties.getProviderBudgets().getAiAdvertisedRpm());
        register("EXTERNAL_CONTEXT", properties.getProviderBudgets().getExternalContextAdvertisedRpm());
    }

    public ProviderRateBudgetManager(ProviderCallProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public void register(String provider, int advertisedRpm) {
        if (advertisedRpm <= 0) throw new IllegalArgumentException("advertised RPM must be positive");
        advertised.put(normalize(provider), advertisedRpm);
    }

    private void registerIfPositive(String provider, Integer advertisedRpm) {
        if (advertisedRpm != null && advertisedRpm > 0) register(provider, advertisedRpm);
    }

    @Override
    public synchronized boolean reserve(ProviderRequestKey key,
                                        AssetPriority priority,
                                        RuntimeScanProfile profile) {
        return reserveAttempt(key, priority, profile, false);
    }

    @Override
    public synchronized boolean reserveAttempt(ProviderRequestKey key,
                                               AssetPriority priority,
                                               RuntimeScanProfile profile,
                                               boolean retryAttempt) {
        if (key == null) throw new IllegalArgumentException("provider request key is required");
        if (priority == null) throw new IllegalArgumentException("asset priority is required");
        if (profile == null) throw new IllegalArgumentException("runtime profile is required");
        return reserveInternal(key.provider(), retryAttempt ? null : symbolGapKey(key), priority, profile);
    }

    /** Compatibility entry point for existing callers that do not yet carry a canonical request key. */
    public synchronized boolean reserve(String provider, AssetPriority priority) {
        return reserveInternal(provider, null, priority, RuntimeScanProfile.STANDARD);
    }

    private boolean reserveInternal(String provider,
                                    String symbolGapKey,
                                    AssetPriority priority,
                                    RuntimeScanProfile profile) {
        String providerKey = normalize(provider);
        Instant now = clock.instant();
        Window providerWindow = currentWindow(providerKey, now);
        Window globalWindow = currentWindow(GLOBAL, now);
        Integer configuredRpm = advertised.get(providerKey);
        if (configuredRpm == null || configuredRpm <= 0) {
            reject(providerWindow, priority, "PROVIDER_RPM_NOT_CONFIGURED");
            return false;
        }
        if (providerWindow.retryAfter != null && now.isBefore(providerWindow.retryAfter)) {
            reject(providerWindow, priority, "PROVIDER_SUSPENDED_RETRY_AFTER");
            return false;
        }

        int minimumGapSeconds = profile == RuntimeScanProfile.EMERGENCY
                ? Math.max(properties.getPerSymbolMinimumGapSeconds(), emergencyMinimumGap(providerKey))
                : properties.getPerSymbolMinimumGapSeconds();
        if (symbolGapKey != null) {
            Instant previous = lastSymbolReservations.get(symbolGapKey);
            if (previous != null && now.isBefore(previous.plusSeconds(minimumGapSeconds))) {
                reject(providerWindow, priority, "PER_SYMBOL_MINIMUM_GAP");
                return false;
            }
        }

        int providerAdvertised = configuredRpm;
        int globalAdvertised = properties.getGlobalAdvertisedRequestsPerMinute();
        boolean emergency = profile == RuntimeScanProfile.EMERGENCY;
        int providerLimit = emergency
                ? emergencyLimit(providerAdvertised)
                : priorityLimit(regularLimit(providerAdvertised), priority);
        int globalLimit = emergency
                ? emergencyLimit(globalAdvertised)
                : priorityLimit(regularLimit(globalAdvertised), priority);
        int providerUsage = emergency ? providerWindow.emergencyUsage : providerWindow.regularUsage;
        int globalUsage = emergency ? globalWindow.emergencyUsage : globalWindow.regularUsage;
        if (providerUsage >= providerLimit) {
            reject(providerWindow, priority, emergency
                    ? "PROVIDER_EMERGENCY_RESERVE_EXHAUSTED" : "PROVIDER_REGULAR_BUDGET_EXHAUSTED");
            return false;
        }
        if (globalUsage >= globalLimit) {
            reject(providerWindow, priority, emergency
                    ? "GLOBAL_EMERGENCY_RESERVE_EXHAUSTED" : "GLOBAL_REGULAR_BUDGET_EXHAUSTED");
            return false;
        }

        if (emergency) {
            providerWindow.emergencyUsage++;
            globalWindow.emergencyUsage++;
        } else {
            providerWindow.regularUsage++;
            globalWindow.regularUsage++;
        }
        providerWindow.rejectedPriority = null;
        providerWindow.lastRejectionReason = null;
        if (symbolGapKey != null) lastSymbolReservations.put(symbolGapKey, now);
        return true;
    }

    @Override
    public synchronized void applyRetryAfter(String provider, long seconds) {
        Instant now = clock.instant();
        Window window = currentWindow(normalize(provider), now);
        window.retryAfter = now.plusSeconds(Math.max(1, seconds));
    }

    @Override
    public synchronized ProviderBudgetState state(String provider, ProviderCircuitState circuitState) {
        String key = normalize(provider);
        int advertisedRpm = advertised.getOrDefault(key, 0);
        int regularLimit = regularLimit(advertisedRpm);
        int emergencyLimit = emergencyLimit(advertisedRpm);
        Window window = currentWindow(key, clock.instant());
        Window globalWindow = currentWindow(GLOBAL, clock.instant());
        int usage = window.regularUsage + window.emergencyUsage;
        int totalAvailable = regularLimit + emergencyLimit;
        return new ProviderBudgetState(key, advertisedRpm, regularLimit,
                properties.getInternalBudgetRatio(), properties.getEmergencyReserveRatio(), usage,
                Math.max(0, totalAvailable - usage), window.retryAfter, circuitState, window.rejectedPriority,
                window.regularUsage, window.emergencyUsage,
                globalWindow.regularUsage + globalWindow.emergencyUsage, window.lastRejectionReason);
    }

    private int regularLimit(int advertisedRpm) {
        return advertisedRpm <= 0 ? 0
                : Math.max(1, (int) Math.floor(advertisedRpm * properties.getInternalBudgetRatio()));
    }

    private int emergencyLimit(int advertisedRpm) {
        return advertisedRpm <= 0 ? 0
                : Math.max(1, (int) Math.floor(advertisedRpm * properties.getEmergencyReserveRatio()));
    }

    private static int priorityLimit(int regularLimit, AssetPriority priority) {
        return switch (priority) {
            case P3_DISCOVERY -> Math.max(1, regularLimit / 2);
            case P1_WATCHLIST -> Math.max(1, (regularLimit * 7) / 10);
            case P2_CANDIDATE -> Math.max(1, (regularLimit * 9) / 10);
            case P0_POSITION -> regularLimit;
        };
    }

    private int emergencyMinimumGap(String provider) {
        return "COINGLASS".equals(provider)
                ? properties.getEventRefreshMinGapSeconds()
                : properties.getPerSymbolMinimumGapSeconds();
    }

    private Window currentWindow(String provider, Instant now) {
        long minute = now.getEpochSecond() / 60;
        return windows.compute(provider, (ignored, previous) -> previous == null || previous.minute != minute
                ? new Window(minute) : previous);
    }

    private static void reject(Window window, AssetPriority priority, String reason) {
        window.rejectedPriority = priority;
        window.lastRejectionReason = reason;
    }

    private static String symbolGapKey(ProviderRequestKey key) {
        return normalize(key.provider()) + "|" + key.datasetType() + "|"
                + key.canonicalInstrumentId().canonical() + "|" + key.timeframe()
                + "|" + key.sourceVersion();
    }

    private static String normalize(String provider) {
        return provider == null ? "UNKNOWN" : provider.trim().toUpperCase(Locale.ROOT);
    }

    private static final class Window {
        private final long minute;
        private int regularUsage;
        private int emergencyUsage;
        private Instant retryAfter;
        private AssetPriority rejectedPriority;
        private String lastRejectionReason;

        private Window(long minute) {
            this.minute = minute;
        }
    }
}
