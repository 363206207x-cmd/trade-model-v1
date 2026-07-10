package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderRateBudgetManager {
    private final ProviderCallProperties properties;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Map<String, Integer> advertised = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public ProviderRateBudgetManager(ProviderCallProperties properties) {
        this(properties, Clock.systemUTC());
        register("BINANCE_PUBLIC", properties.getProviderBudgets().getBinancePublicAdvertisedRpm());
        register("COINGLASS", properties.getProviderBudgets().getCoinglassAdvertisedRpm());
        register("AI", properties.getProviderBudgets().getAiAdvertisedRpm());
        register("EXTERNAL_CONTEXT", properties.getProviderBudgets().getExternalContextAdvertisedRpm());
    }

    public ProviderRateBudgetManager(ProviderCallProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void register(String provider, int advertisedRpm) {
        advertised.put(normalize(provider), Math.max(1, advertisedRpm));
    }

    public synchronized boolean reserve(String provider, AssetPriority priority) {
        String key = normalize(provider);
        int advertisedRpm = advertised.getOrDefault(key, 60);
        int effective = effectiveRpm(advertisedRpm);
        Instant now = clock.instant();
        Window window = currentWindow(key, now);
        if (window.retryAfter != null && now.isBefore(window.retryAfter)) {
            window.rejectedPriority = priority;
            return false;
        }
        int priorityLimit = switch (priority) {
            case P3_POOL -> Math.max(1, effective / 2);
            case P2_CANDIDATE -> Math.max(1, (effective * 7) / 10);
            case P1_CORE -> effective;
            case P0_POSITION -> advertisedRpm;
        };
        if (window.usage >= priorityLimit) {
            window.rejectedPriority = priority;
            return false;
        }
        window.usage++;
        window.rejectedPriority = null;
        return true;
    }

    public synchronized void applyRetryAfter(String provider, long seconds) {
        Window window = currentWindow(normalize(provider), clock.instant());
        window.retryAfter = clock.instant().plusSeconds(Math.max(1, seconds));
    }

    public synchronized ProviderBudgetState state(String provider, ProviderCircuitState circuitState) {
        String key = normalize(provider);
        int advertisedRpm = advertised.getOrDefault(key, 60);
        int effective = effectiveRpm(advertisedRpm);
        Window window = currentWindow(key, clock.instant());
        return new ProviderBudgetState(key, advertisedRpm, effective,
                properties.getInternalBudgetRatio(), properties.getEmergencyReserveRatio(), window.usage,
                Math.max(0, advertisedRpm - window.usage), window.retryAfter, circuitState, window.rejectedPriority);
    }

    private int effectiveRpm(int advertisedRpm) {
        return Math.max(1, (int) Math.floor(advertisedRpm * properties.getInternalBudgetRatio()));
    }

    private Window currentWindow(String provider, Instant now) {
        long minute = now.getEpochSecond() / 60;
        return windows.compute(provider, (ignored, previous) -> previous == null || previous.minute != minute
                ? new Window(minute) : previous);
    }

    private static String normalize(String provider) {
        return provider == null ? "UNKNOWN" : provider.trim().toUpperCase(Locale.ROOT);
    }

    private static final class Window {
        private final long minute;
        private int usage;
        private Instant retryAfter;
        private AssetPriority rejectedPriority;
        private Window(long minute) { this.minute = minute; }
    }
}
