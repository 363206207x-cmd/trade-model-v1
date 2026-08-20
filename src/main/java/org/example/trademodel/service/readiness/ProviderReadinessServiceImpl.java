package org.example.trademodel.service.readiness;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.example.trademodel.ai.AiProviderReadinessService;
import org.example.trademodel.ai.AiProviderRuntimeReadiness;
import org.example.trademodel.dto.ohlcv.PublicProviderHealthSnapshot;
import org.example.trademodel.localreal.LocalRealDataStatusService;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.coinglass.CoinGlassProperties;
import org.example.trademodel.providercall.coinglass.CoinGlassProviderHealthService;
import org.example.trademodel.vo.ProviderReadinessVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ProviderReadinessServiceImpl implements ProviderReadinessService {
    public static final String STATUS_CONNECTED = "CONNECTED";
    public static final String STATUS_CONFIGURED = "CONFIGURED";
    public static final String STATUS_NOT_CONFIGURED = "NOT_CONFIGURED";
    public static final String STATUS_WAITING_SYNC = "WAITING_SYNC";
    public static final String STATUS_FAIL_CLOSED = "FAIL_CLOSED";
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    private final Environment environment;
    private LocalRealDataStatusService localRealDataStatusService;
    private AiProviderReadinessService aiProviderReadinessService;
    private CoinGlassProperties coinGlassProperties;
    private CoinGlassProviderHealthService coinGlassProviderHealthService;

    public ProviderReadinessServiceImpl(Environment environment) {
        this.environment = environment;
    }

    @Autowired(required = false)
    void setLocalRealDataStatusService(LocalRealDataStatusService localRealDataStatusService) {
        this.localRealDataStatusService = localRealDataStatusService;
    }

    @Autowired(required = false)
    void setAiProviderReadinessService(AiProviderReadinessService aiProviderReadinessService) {
        this.aiProviderReadinessService = aiProviderReadinessService;
    }

    @Autowired(required = false)
    void setCoinGlassReadiness(CoinGlassProperties properties, CoinGlassProviderHealthService healthService) {
        this.coinGlassProperties = properties;
        this.coinGlassProviderHealthService = healthService;
    }

    @Override
    public ProviderReadinessVO getReadiness() {
        ProviderReadinessVO.ProviderStatusVO market = marketDataStatus();
        List<ProviderReadinessVO.ProviderStatusVO> aiProviders = aiProviderStatuses();
        ProviderReadinessVO.ProviderStatusVO externalContext = externalContextStatus();
        ProviderReadinessVO.ProviderStatusVO coinGlass = coinGlassStatus();

        List<ProviderReadinessVO.ProviderStatusVO> providers = new ArrayList<>();
        providers.add(market);
        providers.addAll(aiProviders);
        providers.add(externalContext);
        providers.add(coinGlass);

        ProviderReadinessVO readiness = new ProviderReadinessVO();
        readiness.setMarketDataProviderStatus(market.getStatus());
        readiness.setAiProviderStatus(aggregateAiStatus(aiProviders));
        readiness.setExternalContextProviderStatus(externalContext.getStatus());
        readiness.setDataSourceText(dataSourceText(market));
        readiness.setProviders(providers);

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("marketDataProvider", readiness.getMarketDataProviderStatus());
        summary.put("aiProvider", readiness.getAiProviderStatus());
        summary.put("externalContextProvider", readiness.getExternalContextProviderStatus());
        summary.put("coinglassProvider", coinGlass.getStatus());
        readiness.setSummary(summary);
        return readiness;
    }

    private ProviderReadinessVO.ProviderStatusVO marketDataStatus() {
        if (localRealDataStatusService != null) {
            return localRealMarketDataStatus();
        }
        String providerType = upper(firstNonBlank(property("position.provider.type"), "DISABLED"));
        if ("BINANCE".equals(providerType)) {
            boolean baseUrlConfigured = hasText(firstNonBlank(
                    property("binance.api.base-url"),
                    property("market.api.base-url"),
                    "https://api.binance.com"
            ));
            return item(
                    "MARKET_DATA",
                    "BINANCE_PUBLIC_MARKET_DATA",
                    baseUrlConfigured ? STATUS_CONFIGURED : STATUS_NOT_CONFIGURED,
                    true,
                    baseUrlConfigured,
                    false,
                    baseUrlConfigured
                            ? "BINANCE_PUBLIC_MARKET_CONFIG_ONLY_NOT_CONNECTED"
                            : "BINANCE_PUBLIC_MARKET_BASE_URL_MISSING"
            );
        }
        if ("SIMULATED".equals(providerType)) {
            return item(
                    "MARKET_DATA",
                    "SIMULATED_FALLBACK",
                    STATUS_WAITING_SYNC,
                    true,
                    false,
                    false,
                    "LOCAL_DEV_SIMULATED_FALLBACK_NOT_PRODUCTION_READY"
            );
        }
        if ("DISABLED".equals(providerType)) {
            return item(
                    "MARKET_DATA",
                    "MARKET_PROVIDER_UNAVAILABLE",
                    STATUS_WAITING_SYNC,
                    false,
                    false,
                    false,
                    "MARKET_PROVIDER_NOT_CONFIGURED"
            );
        }
        return item(
                "MARKET_DATA",
                providerType,
                STATUS_UNKNOWN,
                false,
                false,
                false,
                "UNKNOWN_MARKET_PROVIDER_TYPE"
        );
    }

    private ProviderReadinessVO.ProviderStatusVO localRealMarketDataStatus() {
        try {
            LocalRealDataStatusService.ProviderReadinessSnapshot snapshot =
                    localRealDataStatusService.providerReadinessSnapshot();
            String provider = upper(firstNonBlank(snapshot.provider(), "UNKNOWN"));
            String freshness = upper(firstNonBlank(snapshot.freshnessStatus(), "NO_DATA"));
            String runtimeState = upper(firstNonBlank(snapshot.runtimeState(), "UNKNOWN"));
            boolean fresh = "FRESH".equals(freshness);
            boolean connected = snapshot.dashboardReady() && fresh;
            PublicProviderHealthSnapshot providerHealth = snapshot.providerHealth();

            String readinessStatus;
            String reason;
            if (connected) {
                readinessStatus = STATUS_CONNECTED;
                reason = "LOCAL_REAL_PROVIDER_VERIFIED_FRESH";
            } else if ("FAILED".equals(runtimeState)
                    || "STALE".equals(freshness)
                    || "INVALID".equals(freshness)
                    || providerHealth != null && (providerHealth.circuitOpen()
                    || "DEGRADED".equals(upper(providerHealth.status()))
                    || "REGION_RESTRICTED".equals(upper(providerHealth.status()))
                    || "GEO_RESTRICTED".equals(upper(providerHealth.status())))) {
                readinessStatus = STATUS_FAIL_CLOSED;
                reason = localRealFailureReason(runtimeState, freshness, providerHealth);
            } else {
                readinessStatus = STATUS_WAITING_SYNC;
                reason = "LOCAL_REAL_PROVIDER_NOT_READY";
            }
            return item(
                    "MARKET_DATA",
                    provider + "_PUBLIC_MARKET_DATA",
                    readinessStatus,
                    true,
                    true,
                    connected,
                    reason
            );
        } catch (RuntimeException ex) {
            return item(
                    "MARKET_DATA",
                    "LOCAL_REAL_MARKET_DATA",
                    STATUS_FAIL_CLOSED,
                    true,
                    true,
                    false,
                    "LOCAL_REAL_PROVIDER_STATUS_UNAVAILABLE"
            );
        }
    }

    private String localRealFailureReason(String runtimeState,
                                          String freshness,
                                          PublicProviderHealthSnapshot providerHealth) {
        if ("FAILED".equals(runtimeState)) {
            return "LOCAL_REAL_RUNTIME_FAILED";
        }
        if ("STALE".equals(freshness)) {
            return "LOCAL_REAL_MARKET_DATA_STALE";
        }
        if ("INVALID".equals(freshness)) {
            return "LOCAL_REAL_MARKET_DATA_INVALID";
        }
        if (providerHealth != null && providerHealth.circuitOpen()) {
            return "LOCAL_REAL_PROVIDER_CIRCUIT_OPEN";
        }
        return "LOCAL_REAL_PROVIDER_DEGRADED";
    }

    private List<ProviderReadinessVO.ProviderStatusVO> aiProviderStatuses() {
        if (aiProviderReadinessService != null) {
            return aiProviderReadinessService.readiness().stream()
                    .map(this::canonicalAiProviderStatus)
                    .toList();
        }
        boolean orchestratorEnabled = isTrue(property("trade-model.ai.enabled"));
        return List.of(
                aiProviderStatus("OPENAI", "trade-model.ai.openai", orchestratorEnabled),
                aiProviderStatus("GEMINI", "trade-model.ai.gemini", orchestratorEnabled),
                aiProviderStatus("XAI", "trade-model.ai.xai", orchestratorEnabled)
        );
    }

    private ProviderReadinessVO.ProviderStatusVO canonicalAiProviderStatus(
            AiProviderRuntimeReadiness readiness) {
        String status = switch (readiness.state()) {
            case AUTHORIZED -> STATUS_CONNECTED;
            case DISABLED, MODEL_NOT_VERIFIED -> STATUS_WAITING_SYNC;
            default -> STATUS_FAIL_CLOSED;
        };
        boolean enabled = readiness.state() != org.example.trademodel.ai.AiProviderReadinessState.DISABLED;
        boolean configured = switch (readiness.state()) {
            case KEY_MISSING, COST_NOT_CONFIGURED, RPM_NOT_CONFIGURED, BUDGET_NOT_CONFIGURED, DISABLED -> false;
            default -> true;
        };
        return item("AI", readiness.provider(), status, enabled, configured,
                readiness.ready(), readiness.reasonCode());
    }

    private ProviderReadinessVO.ProviderStatusVO aiProviderStatus(String name, String prefix, boolean orchestratorEnabled) {
        boolean providerEnabled = orchestratorEnabled && isTrue(property(prefix + ".enabled"));
        boolean modelConfigured = "OPENAI".equals(name)
                ? hasText(property(prefix + ".gpt-final.fast-model"))
                    && hasText(property(prefix + ".gpt-final.reasoning-model"))
                    && hasText(property(prefix + ".gpt-final.fallback-models[0]"))
                    && hasText(property(prefix + ".gpt-final.fallback-models[1]"))
                : hasText(property(prefix + ".model"));
        boolean configured = hasText(property(prefix + ".api-key"))
                && modelConfigured
                && hasText(property(prefix + ".base-url"));
        if (!orchestratorEnabled) {
            return item("AI", name, STATUS_WAITING_SYNC, false, configured, false, "AI_ORCHESTRATOR_DISABLED");
        }
        if (!providerEnabled) {
            return item("AI", name, STATUS_WAITING_SYNC, false, configured, false, "AI_PROVIDER_DISABLED");
        }
        if (!configured) {
            return item("AI", name, STATUS_FAIL_CLOSED, true, false, false, "AI_PROVIDER_NOT_CONFIGURED");
        }
        return item("AI", name, STATUS_CONFIGURED, true, true, false, "AI_PROVIDER_CONFIG_ONLY_NOT_CONNECTED");
    }

    private String aggregateAiStatus(List<ProviderReadinessVO.ProviderStatusVO> providers) {
        boolean sawConfigured = false;
        boolean allConnected = !providers.isEmpty();
        for (ProviderReadinessVO.ProviderStatusVO provider : providers) {
            if (STATUS_FAIL_CLOSED.equals(provider.getStatus())) {
                return STATUS_FAIL_CLOSED;
            }
            if (STATUS_CONFIGURED.equals(provider.getStatus())) {
                sawConfigured = true;
            }
            if (!STATUS_CONNECTED.equals(provider.getStatus())) {
                allConnected = false;
            }
        }
        return allConnected ? STATUS_CONNECTED : sawConfigured ? STATUS_CONFIGURED : STATUS_WAITING_SYNC;
    }

    private ProviderReadinessVO.ProviderStatusVO coinGlassStatus() {
        if (coinGlassProperties == null || coinGlassProviderHealthService == null) {
            return item("DERIVATIVES_CONTEXT", "COINGLASS", STATUS_NOT_CONFIGURED,
                    false, false, false, "COINGLASS_NOT_CONFIGURED");
        }
        UnifiedSourceStatus source = coinGlassProviderHealthService.configurationStatus(coinGlassProperties);
        String status = switch (source) {
            case READY -> STATUS_CONNECTED;
            case NOT_CONFIGURED, DISABLED -> STATUS_NOT_CONFIGURED;
            case WAITING_SYNC -> STATUS_WAITING_SYNC;
            default -> STATUS_FAIL_CLOSED;
        };
        boolean configured = source != UnifiedSourceStatus.NOT_CONFIGURED
                && source != UnifiedSourceStatus.DISABLED;
        return item("DERIVATIVES_CONTEXT", "COINGLASS", status,
                coinGlassProperties.isEnabled(), configured, source == UnifiedSourceStatus.READY,
                "COINGLASS_" + source.name());
    }

    private ProviderReadinessVO.ProviderStatusVO externalContextStatus() {
        boolean newsConfigured = hasAnyText("trade-model.external-context.news.api-key", "news.api-key", "NEWS_API_KEY");
        boolean macroConfigured = hasAnyText("trade-model.external-context.macro-calendar.api-key", "macro-calendar.api-key", "MACRO_CALENDAR_API_KEY");
        boolean etfConfigured = hasAnyText("trade-model.external-context.etf-flow.api-key", "etf-flow.api-key", "ETF_FLOW_API_KEY");
        boolean configured = newsConfigured || macroConfigured || etfConfigured;
        return item(
                "EXTERNAL_CONTEXT",
                "MACRO_NEWS_CONTEXT",
                configured ? STATUS_CONFIGURED : STATUS_WAITING_SYNC,
                configured,
                configured,
                false,
                configured ? "EXTERNAL_CONTEXT_CONFIG_ONLY_NOT_CONNECTED" : "EXTERNAL_CONTEXT_IMPORT_ONLY_WAITING_SYNC"
        );
    }

    private ProviderReadinessVO.ProviderStatusVO item(String category,
                                                     String name,
                                                     String status,
                                                     boolean enabled,
                                                     boolean configured,
                                                     boolean connected,
                                                     String reason) {
        ProviderReadinessVO.ProviderStatusVO item = new ProviderReadinessVO.ProviderStatusVO();
        item.setCategory(category);
        item.setName(name);
        item.setStatus(status);
        item.setEnabled(enabled);
        item.setConfigured(configured);
        item.setConnected(connected);
        item.setReason(reason);
        return item;
    }

    private String dataSourceText(ProviderReadinessVO.ProviderStatusVO market) {
        if (market == null) {
            return STATUS_WAITING_SYNC;
        }
        if ("BINANCE_PUBLIC_MARKET_DATA".equals(market.getName())) {
            return "Binance public data / " + market.getStatus();
        }
        if ("SIMULATED_FALLBACK".equals(market.getName())) {
            return "Simulated fallback / " + market.getStatus();
        }
        if (market.getName() != null && market.getName().endsWith("_PUBLIC_MARKET_DATA")) {
            String provider = market.getName().substring(0,
                    market.getName().length() - "_PUBLIC_MARKET_DATA".length());
            return providerLabel(provider) + " public data / " + market.getStatus();
        }
        return firstNonBlank(market.getName(), "UNKNOWN") + " / " + firstNonBlank(market.getStatus(), STATUS_UNKNOWN);
    }

    private String providerLabel(String provider) {
        String normalized = trim(provider).toLowerCase(Locale.ROOT);
        return normalized.isEmpty()
                ? "Unknown"
                : Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private boolean hasAnyText(String... keys) {
        for (String key : keys) {
            if (hasText(property(key))) {
                return true;
            }
        }
        return false;
    }

    private String property(String key) {
        try {
            return environment.getProperty(key);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isTrue(String value) {
        return "true".equalsIgnoreCase(trim(value));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String upper(String value) {
        return trim(value).toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
