package org.example.trademodel.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TelegramReadinessService {
    private final TelegramProperties properties;
    private final Clock clock;
    private final AtomicReference<ProviderState> providerState = new AtomicReference<>();

    @Autowired
    public TelegramReadinessService(TelegramProperties properties) {
        this(properties, Clock.systemUTC());
    }

    TelegramReadinessService(TelegramProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public TelegramReadinessState state() {
        TelegramReadinessState configuration = configurationState();
        if (configuration != TelegramReadinessState.DEGRADED) return configuration;
        ProviderState observed = providerState.get();
        return observed == null ? TelegramReadinessState.DEGRADED : observed.state();
    }

    public boolean configured() { return properties.configuredForExternalDelivery(); }
    public boolean recipientConfigured() { return properties.hasChatId(); }
    public boolean canAttemptDelivery() { return properties.configuredForExternalDelivery(); }
    public String botUsername() {
        ProviderState observed = providerState.get();
        return observed != null && observed.state() == TelegramReadinessState.READY
                ? observed.botUsername() : null;
    }
    public String recipientFingerprint() {
        ProviderState observed = providerState.get();
        return observed != null && observed.state() == TelegramReadinessState.READY
                ? observed.recipientFingerprint() : null;
    }
    public LocalDateTime observedAt() {
        ProviderState observed = providerState.get();
        return observed == null ? null : observed.observedAt();
    }
    public String reasonCode() {
        TelegramReadinessState configuration = configurationState();
        if (configuration != TelegramReadinessState.DEGRADED) return configuration.name();
        ProviderState observed = providerState.get();
        if (observed == null) return TelegramReadinessState.DEGRADED.name();
        return observed.reasonCode() == null ? observed.state().name() : observed.reasonCode();
    }

    public void observe(TelegramClientResult result) {
        if (result == null) return;
        ProviderState previous = providerState.get();
        providerState.set(new ProviderState(result.readinessState(),
                hasText(result.botUsername()) ? result.botUsername() : previous == null ? null : previous.botUsername(),
                hasText(result.recipientFingerprint()) ? result.recipientFingerprint() : previous == null ? null : previous.recipientFingerprint(),
                result.errorCode(), LocalDateTime.now(clock)));
    }

    private TelegramReadinessState configurationState() {
        if (!properties.isEnabled() || !properties.isExternalCallsEnabled()) {
            return TelegramReadinessState.NOT_CONFIGURED;
        }
        if (!properties.hasToken()) return TelegramReadinessState.TOKEN_MISSING;
        if (!properties.hasChatId()) return TelegramReadinessState.CHAT_ID_MISSING;
        if (!properties.hasApiBaseUrl()) return TelegramReadinessState.NOT_CONFIGURED;
        return TelegramReadinessState.DEGRADED;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ProviderState(TelegramReadinessState state, String botUsername,
                                 String recipientFingerprint, String reasonCode, LocalDateTime observedAt) {
    }
}
