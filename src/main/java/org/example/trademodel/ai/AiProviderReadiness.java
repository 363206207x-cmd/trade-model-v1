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
    private final String configuredModel;
    private final String effectiveModel;
    private final boolean fallbackUsed;
    private final String fallbackReason;
    private final String modelStrategy;
    private final AiModelReadinessStatus modelReadinessStatus;
    private final List<String> reasonCodes;

    public AiProviderReadiness(AiProviderName provider, AiProviderRole role, boolean enabled,
                               boolean configured, boolean ready, String model, List<String> reasonCodes) {
        this(provider, role, enabled, configured, ready, model, model, false, null,
                null,
                model == null || model.isBlank()
                        ? AiModelReadinessStatus.MODEL_UNAVAILABLE
                        : AiModelReadinessStatus.MODEL_CONFIGURED,
                reasonCodes);
    }

    public AiProviderReadiness(AiProviderName provider, AiProviderRole role, boolean enabled,
                               boolean configured, boolean ready, String configuredModel,
                               String effectiveModel, boolean fallbackUsed, String fallbackReason,
                               AiModelReadinessStatus modelReadinessStatus, List<String> reasonCodes) {
        this(provider, role, enabled, configured, ready, configuredModel, effectiveModel,
                fallbackUsed, fallbackReason, null, modelReadinessStatus, reasonCodes);
    }

    public AiProviderReadiness(AiProviderName provider, AiProviderRole role, boolean enabled,
                               boolean configured, boolean ready, String configuredModel,
                               String effectiveModel, boolean fallbackUsed, String fallbackReason,
                               String modelStrategy, AiModelReadinessStatus modelReadinessStatus,
                               List<String> reasonCodes) {
        this.provider = provider;
        this.role = role;
        this.enabled = enabled;
        this.configured = configured;
        this.ready = ready;
        this.configuredModel = configuredModel;
        this.effectiveModel = effectiveModel;
        this.fallbackUsed = fallbackUsed;
        this.fallbackReason = fallbackReason;
        this.modelStrategy = modelStrategy;
        this.modelReadinessStatus = modelReadinessStatus;
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
    public String getModel() { return effectiveModel; }
    public String getConfiguredModel() { return configuredModel; }
    public String getEffectiveModel() { return effectiveModel; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public String getFallbackReason() { return fallbackReason; }
    public String getModelStrategy() { return modelStrategy; }
    public AiModelReadinessStatus getModelReadinessStatus() { return modelReadinessStatus; }
    public List<String> getReasonCodes() { return Collections.unmodifiableList(reasonCodes); }
}
