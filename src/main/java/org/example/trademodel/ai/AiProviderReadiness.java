package org.example.trademodel.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AiProviderReadiness {
    private final AiProviderName provider;
    private final AiProviderRole role;
    private final boolean enabled;
    private final boolean configured;
    private final boolean ready;
    private final String model;
    private final List<String> reasonCodes;

    public AiProviderReadiness(AiProviderName provider, AiProviderRole role, boolean enabled,
                               boolean configured, boolean ready, String model, List<String> reasonCodes) {
        this.provider = provider;
        this.role = role;
        this.enabled = enabled;
        this.configured = configured;
        this.ready = ready;
        this.model = model;
        this.reasonCodes = reasonCodes == null ? List.of() : new ArrayList<>(reasonCodes);
    }

    public static AiProviderReadiness disabled(AiProviderName provider, AiProviderRole role, String model) {
        return new AiProviderReadiness(provider, role, false, false, false, model, List.of("PROVIDER_DISABLED"));
    }

    public AiProviderName getProvider() { return provider; }
    public AiProviderRole getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public boolean isConfigured() { return configured; }
    public boolean isReady() { return ready; }
    public String getModel() { return model; }
    public List<String> getReasonCodes() { return Collections.unmodifiableList(reasonCodes); }
}
