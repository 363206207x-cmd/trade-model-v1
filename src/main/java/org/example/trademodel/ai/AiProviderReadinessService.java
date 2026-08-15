package org.example.trademodel.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiProviderReadinessService {
    static final Map<AiProviderName, String> EXACT_MODELS = Map.of(
            AiProviderName.OPENAI, "gpt-5.6-sol",
            AiProviderName.GEMINI, "gemini-3.5-flash",
            AiProviderName.XAI, "grok-4.5");

    private final AiOrchestratorProperties properties;
    private final Map<AiProviderName, AiProviderClient> clients;
    private final long verificationTtlSeconds;
    private final Clock clock;
    private final Map<AiProviderName, AiProviderRuntimeReadiness> verificationCache =
            new ConcurrentHashMap<>();

    @Autowired
    public AiProviderReadinessService(
            AiOrchestratorProperties properties,
            List<AiProviderClient> clients,
            @Value("${trade-model.ai.readiness-verification-ttl-seconds:3600}") long verificationTtlSeconds) {
        this(properties, clients, verificationTtlSeconds, Clock.systemUTC());
    }

    AiProviderReadinessService(AiOrchestratorProperties properties,
                               List<AiProviderClient> clients,
                               long verificationTtlSeconds,
                               Clock clock) {
        this.properties = properties;
        Map<AiProviderName, AiProviderClient> indexed = new EnumMap<>(AiProviderName.class);
        if (clients != null) {
            clients.stream().filter(client -> client != null && client.provider() != null)
                    .forEach(client -> indexed.putIfAbsent(client.provider(), client));
        }
        this.clients = Map.copyOf(indexed);
        this.verificationTtlSeconds = Math.max(1L, verificationTtlSeconds);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public List<AiProviderRuntimeReadiness> readiness() {
        return List.of(AiProviderName.OPENAI, AiProviderName.GEMINI, AiProviderName.XAI).stream()
                .map(this::readiness)
                .toList();
    }

    public AiProviderRuntimeReadiness readiness(AiProviderName provider) {
        AiProviderName safeProvider = provider == null ? AiProviderName.OPENAI : provider;
        AiProviderProperties providerProperties = providerProperties(safeProvider);
        String model = exactModel(safeProvider, providerProperties);
        String configVersion = configVersion(safeProvider, providerProperties, model);
        AiProviderRuntimeReadiness prerequisite = prerequisite(
                safeProvider, providerProperties, model, configVersion);
        if (prerequisite != null) return prerequisite;

        AiProviderRuntimeReadiness cached = verificationCache.get(safeProvider);
        Instant now = clock.instant();
        if (cached == null || !configVersion.equals(cached.configVersion())) {
            return state(safeProvider, providerProperties, model,
                    AiProviderReadinessState.MODEL_NOT_VERIFIED, null, null,
                    "EXACT_MODEL_NOT_VERIFIED", null, configVersion);
        }
        if (cached.expiresAt() == null || !cached.expiresAt().isAfter(now)) {
            return state(safeProvider, providerProperties, model,
                    AiProviderReadinessState.MODEL_NOT_VERIFIED, cached.verifiedAt(), cached.expiresAt(),
                    "EXACT_MODEL_VERIFICATION_EXPIRED", cached.requestId(), configVersion);
        }
        return cached;
    }

    public AiProviderRuntimeReadiness reverify(AiProviderName provider) {
        AiProviderName safeProvider = provider == null ? AiProviderName.OPENAI : provider;
        AiProviderProperties providerProperties = providerProperties(safeProvider);
        String model = exactModel(safeProvider, providerProperties);
        String configVersion = configVersion(safeProvider, providerProperties, model);
        AiProviderRuntimeReadiness prerequisite = prerequisite(
                safeProvider, providerProperties, model, configVersion);
        if (prerequisite != null) return prerequisite;
        AiProviderClient client = clients.get(safeProvider);
        if (client == null) {
            return cache(state(safeProvider, providerProperties, model,
                    AiProviderReadinessState.PROVIDER_UNAVAILABLE, null,
                    clock.instant().plusSeconds(verificationTtlSeconds),
                    "APPLICATION_PROVIDER_CLIENT_MISSING", null, configVersion));
        }

        AiProviderReviewResult result;
        try {
            result = client.verifyExactModel(
                    model, properties.getProviderTimeouts().timeoutMs(safeProvider));
        } catch (Exception exception) {
            result = new AiProviderReviewResult();
            result.setProvider(safeProvider);
            result.setRole(client.role());
            result.setCallStatus(AiProviderCallStatus.FAILED);
            result.setErrorCode("PROVIDER_VERIFICATION_EXCEPTION");
        }
        Instant now = clock.instant();
        AiProviderReadinessState state = verificationState(result);
        String reason = result == null || result.getErrorCode() == null
                ? state == AiProviderReadinessState.AUTHORIZED
                    ? "EXACT_MODEL_AUTHORIZED" : "EXACT_MODEL_VERIFICATION_FAILED"
                : sanitizedReason(result.getErrorCode());
        AiProviderRuntimeReadiness readiness = state(
                safeProvider, providerProperties, model, state,
                state == AiProviderReadinessState.AUTHORIZED ? now : null,
                now.plusSeconds(verificationTtlSeconds), reason,
                result == null ? null : safeRequestId(result.getProviderRequestId()), configVersion);
        return cache(readiness);
    }

    private AiProviderRuntimeReadiness prerequisite(AiProviderName provider,
                                                    AiProviderProperties providerProperties,
                                                    String model,
                                                    String configVersion) {
        if (!properties.isEnabled() || !providerProperties.isEnabled()) {
            return state(provider, providerProperties, model, AiProviderReadinessState.DISABLED,
                    null, null, "AI_PROVIDER_DISABLED", null, configVersion);
        }
        if (providerProperties.getApiKey() == null || providerProperties.getApiKey().isBlank()) {
            return state(provider, providerProperties, model, AiProviderReadinessState.KEY_MISSING,
                    null, null, "AI_PROVIDER_KEY_MISSING", null, configVersion);
        }
        if (!EXACT_MODELS.get(provider).equals(model)) {
            return state(provider, providerProperties, model, AiProviderReadinessState.MODEL_UNAVAILABLE,
                    null, null, "EXACT_MODEL_MISMATCH", null, configVersion);
        }
        if (providerProperties.inputCostPresence() != AiConfigurationPresence.POSITIVE_VALUE
                || providerProperties.outputCostPresence() != AiConfigurationPresence.POSITIVE_VALUE) {
            return state(provider, providerProperties, model, AiProviderReadinessState.COST_NOT_CONFIGURED,
                    null, null, "AI_PROVIDER_COST_NOT_CONFIGURED", null, configVersion);
        }
        if (providerProperties.requestsPerMinutePresence() != AiConfigurationPresence.POSITIVE_VALUE) {
            return state(provider, providerProperties, model, AiProviderReadinessState.RPM_NOT_CONFIGURED,
                    null, null, "AI_PROVIDER_RPM_NOT_CONFIGURED", null, configVersion);
        }
        if (properties.dailyBudgetPresence() != AiConfigurationPresence.POSITIVE_VALUE
                || properties.perAnalysisBudgetPresence() != AiConfigurationPresence.POSITIVE_VALUE) {
            return state(provider, providerProperties, model, AiProviderReadinessState.BUDGET_NOT_CONFIGURED,
                    null, null, "AI_PROVIDER_BUDGET_NOT_CONFIGURED", null, configVersion);
        }
        return null;
    }

    private AiProviderRuntimeReadiness state(AiProviderName provider,
                                             AiProviderProperties providerProperties,
                                             String model,
                                             AiProviderReadinessState state,
                                             Instant verifiedAt,
                                             Instant expiresAt,
                                             String reason,
                                             String requestId,
                                             String configVersion) {
        return new AiProviderRuntimeReadiness(
                provider.name(), model, state, verifiedAt, expiresAt, reason, requestId, configVersion,
                providerProperties.requestsPerMinutePresence(),
                providerProperties.inputCostPresence(), providerProperties.outputCostPresence(),
                properties.dailyBudgetPresence(), properties.perAnalysisBudgetPresence());
    }

    private AiProviderRuntimeReadiness cache(AiProviderRuntimeReadiness readiness) {
        verificationCache.put(AiProviderName.valueOf(readiness.provider()), readiness);
        return readiness;
    }

    private AiProviderProperties providerProperties(AiProviderName provider) {
        return switch (provider) {
            case OPENAI -> properties.getOpenai();
            case GEMINI -> properties.getGemini();
            case XAI -> properties.getXai();
        };
    }

    private static String exactModel(AiProviderName provider, AiProviderProperties properties) {
        if (provider == AiProviderName.OPENAI) {
            return normalize(properties.getGptFinal().getReasoningModel());
        }
        return normalize(properties.getConfiguredModel());
    }

    private String configVersion(AiProviderName provider,
                                 AiProviderProperties providerProperties,
                                 String model) {
        String material = String.join("|",
                provider.name(), normalize(model), normalize(providerProperties.getBaseUrl()),
                providerProperties.requestsPerMinutePresence().name(),
                decimal(providerProperties.getConfiguredRequestsPerMinute()),
                providerProperties.inputCostPresence().name(),
                decimal(providerProperties.getConfiguredInputCostPerMillionUsd()),
                providerProperties.outputCostPresence().name(),
                decimal(providerProperties.getConfiguredOutputCostPerMillionUsd()),
                properties.dailyBudgetPresence().name(), decimal(properties.getConfiguredDailyBudgetUsd()),
                properties.perAnalysisBudgetPresence().name(),
                decimal(properties.getConfiguredPerAnalysisBudgetUsd()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return "AI-CONFIG-" + HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception impossible) {
            throw new IllegalStateException("AI_CONFIG_VERSION_UNAVAILABLE", impossible);
        }
    }

    private static AiProviderReadinessState verificationState(AiProviderReviewResult result) {
        if (result != null && result.successful() && !result.isFallback()) {
            return AiProviderReadinessState.AUTHORIZED;
        }
        if (result != null && result.isFallback()) {
            return AiProviderReadinessState.MODEL_NOT_VERIFIED;
        }
        String code = result == null || result.getErrorCode() == null
                ? "" : result.getErrorCode().toUpperCase(Locale.ROOT);
        if (result != null && result.getCallStatus() == AiProviderCallStatus.RATE_LIMITED) {
            return AiProviderReadinessState.RATE_LIMITED;
        }
        if ((result != null && result.getCallStatus() == AiProviderCallStatus.BUDGET_BLOCKED)
                || code.contains("BILLING") || code.contains("CREDIT")) {
            return AiProviderReadinessState.BUDGET_BLOCKED;
        }
        if (code.contains("AUTH")) return AiProviderReadinessState.AUTH_FAILED;
        if (code.contains("MODEL") || code.contains("CAPABILITY")) {
            return AiProviderReadinessState.MODEL_UNAVAILABLE;
        }
        return AiProviderReadinessState.PROVIDER_UNAVAILABLE;
    }

    private static String safeRequestId(String value) {
        if (value == null || value.isBlank()) return null;
        String sanitized = value.replaceAll("[^A-Za-z0-9._:-]", "");
        return sanitized.length() > 128 ? sanitized.substring(0, 128) : sanitized;
    }

    private static String sanitizedReason(String value) {
        if (value == null || value.isBlank()) return "EXACT_MODEL_VERIFICATION_FAILED";
        String sanitized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_:-]", "_");
        return sanitized.length() > 128 ? sanitized.substring(0, 128) : sanitized;
    }

    private static String decimal(Object value) {
        if (value == null) return "MISSING";
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
        return String.valueOf(value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
