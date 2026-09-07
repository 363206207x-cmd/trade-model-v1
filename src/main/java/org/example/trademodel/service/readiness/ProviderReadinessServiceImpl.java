package org.example.trademodel.service.readiness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.example.trademodel.ai.AiProviderReadinessService;
import org.example.trademodel.ai.AiProviderRuntimeReadiness;
import org.example.trademodel.dto.ohlcv.PublicProviderHealthSnapshot;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.localreal.LocalRealDataStatusService;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.market.client.impl.RoutedPublicOhlcvProvider;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.coinglass.CoinGlassProperties;
import org.example.trademodel.providercall.coinglass.CoinGlassProviderHealthService;
import org.example.trademodel.vo.ProviderReadinessVO;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.service.AiCallLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ProviderReadinessServiceImpl implements ProviderReadinessService {
    private static final long DEFAULT_FRESHNESS_TOLERANCE_MS = 30_000L;
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
    private RoutedPublicOhlcvProvider routedPublicOhlcvProvider;
    private PersistedOhlcvBarMapper persistedOhlcvBarMapper;
    private FundamentalAiV41Properties fundamentalAiV41Properties;
    private AiCallLogService aiCallLogService;
    private Clock clock = Clock.systemUTC();

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

    @Autowired(required = false)
    void setProductionMarketDataReadiness(RoutedPublicOhlcvProvider provider,
                                          PersistedOhlcvBarMapper mapper) {
        this.routedPublicOhlcvProvider = provider;
        this.persistedOhlcvBarMapper = mapper;
    }

    @Autowired(required = false)
    void setAiBudgetReadiness(FundamentalAiV41Properties properties,
                              AiCallLogService callLogService) {
        this.fundamentalAiV41Properties = properties;
        this.aiCallLogService = callLogService;
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
        appendAiBudgetSummary(summary);
        readiness.setSummary(summary);
        return readiness;
    }

    private ProviderReadinessVO.ProviderStatusVO marketDataStatus() {
        if (localRealDataStatusService != null) {
            return localRealMarketDataStatus();
        }
        if (routedPublicOhlcvProvider != null && persistedOhlcvBarMapper != null) {
            return productionMarketDataStatus();
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

    private ProviderReadinessVO.ProviderStatusVO productionMarketDataStatus() {
        String provider = upper(firstNonBlank(routedPublicOhlcvProvider.primaryProvider(), "UNKNOWN"));
        String providerName = provider + "_PUBLIC_MARKET_DATA";
        try {
            Map<String, PublicProviderHealthSnapshot> healthByProvider = routedPublicOhlcvProvider.health();
            PublicProviderHealthSnapshot health = healthByProvider == null
                    ? null : healthByProvider.get(provider.toLowerCase(Locale.ROOT));
            PersistedOhlcvBarDO latest = persistedOhlcvBarMapper.selectLatestClosedBarBySource(
                    persistedProvider(provider), "SPOT");
            String freshness = persistedFreshness(latest, provider);
            boolean runtimeReady = runtimeProviderReady(health, latest);
            boolean connected = runtimeReady && "FRESH".equals(freshness);

            if (connected) {
                return detail(item("MARKET_DATA", providerName, STATUS_CONNECTED,
                        true, true, true, provider + "_RUNTIME_PROVIDER_VERIFIED_FRESH"),
                        health.lastSuccessAt(), freshness, ageMs(health.lastSuccessAt()),
                        "价格、方向、计划与持仓监控", "自动持续采集");
            }
            if (providerHealthFailed(health)) {
                return item("MARKET_DATA", providerName, STATUS_FAIL_CLOSED,
                        true, true, false, providerFailureReason(provider, health));
            }
            if ("STALE".equals(freshness) || "INVALID".equals(freshness)) {
                return item("MARKET_DATA", providerName, STATUS_FAIL_CLOSED,
                        true, true, false, provider + "_MARKET_DATA_" + freshness);
            }
            return item("MARKET_DATA", providerName, STATUS_WAITING_SYNC,
                    true, true, false, provider + "_RUNTIME_PROVIDER_NOT_READY");
        } catch (RuntimeException ex) {
            return item("MARKET_DATA", providerName, STATUS_FAIL_CLOSED,
                    true, true, false, provider + "_RUNTIME_PROVIDER_STATUS_UNAVAILABLE");
        }
    }

    private boolean runtimeProviderReady(PublicProviderHealthSnapshot health,
                                         PersistedOhlcvBarDO latest) {
        if (health == null || health.circuitOpen() || health.lastSuccessAt() == null
                || !"UP".equals(upper(health.status()))) {
            return false;
        }
        Long timeframeMs = latest == null ? null : timeframeMs(latest.getTimeframe());
        if (timeframeMs == null) {
            return false;
        }
        long ageMs = clock.millis() - health.lastSuccessAt().toEpochMilli();
        return ageMs >= 0L && ageMs <= timeframeMs + freshnessToleranceMs();
    }

    private String persistedFreshness(PersistedOhlcvBarDO latest, String provider) {
        if (latest == null || latest.getCloseTimeMs() == null) {
            return "NO_DATA";
        }
        if (!persistedProvider(provider).equalsIgnoreCase(trim(latest.getProvider()))
                || !"SPOT".equalsIgnoreCase(trim(latest.getProviderMarketType()))
                || !"READY".equalsIgnoreCase(trim(latest.getSourceStatus()))
                || !"FRESH".equalsIgnoreCase(trim(latest.getFreshnessStatus()))) {
            return "INVALID";
        }
        Long timeframeMs = timeframeMs(latest.getTimeframe());
        if (timeframeMs == null) {
            return "INVALID";
        }
        long ageMs = clock.millis() - latest.getCloseTimeMs();
        if (ageMs < 0L) {
            return "INVALID";
        }
        return ageMs <= timeframeMs + freshnessToleranceMs() ? "FRESH" : "STALE";
    }

    private boolean providerHealthFailed(PublicProviderHealthSnapshot health) {
        if (health == null) {
            return false;
        }
        String status = upper(health.status());
        return health.circuitOpen()
                || "DEGRADED".equals(status)
                || "REGION_RESTRICTED".equals(status)
                || "GEO_RESTRICTED".equals(status)
                || "ERROR".equals(status);
    }

    private String providerFailureReason(String provider, PublicProviderHealthSnapshot health) {
        if (health != null && health.circuitOpen()) {
            return provider + "_PROVIDER_CIRCUIT_OPEN";
        }
        return provider + "_RUNTIME_PROVIDER_DEGRADED";
    }

    private long freshnessToleranceMs() {
        String configured = property("trade-model.ohlcv.freshness-tolerance-ms");
        if (!hasText(configured)) {
            return DEFAULT_FRESHNESS_TOLERANCE_MS;
        }
        try {
            return Math.max(0L, Long.parseLong(configured.trim()));
        } catch (NumberFormatException ex) {
            return DEFAULT_FRESHNESS_TOLERANCE_MS;
        }
    }

    private static Long timeframeMs(String timeframe) {
        return switch (timeframe == null ? "" : timeframe.trim().toLowerCase(Locale.ROOT)) {
            case "5m" -> 5L * 60_000L;
            case "15m" -> 15L * 60_000L;
            case "1h" -> 60L * 60_000L;
            case "4h" -> 4L * 60L * 60_000L;
            default -> null;
        };
    }

    private static String persistedProvider(String provider) {
        return "BINANCE".equalsIgnoreCase(provider) ? "BINANCE_PUBLIC" : provider;
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
                readiness.ready(), readiness.reasonCode(), readiness.verifiedAt(),
                readiness.expiresAt() == null ? "UNKNOWN" : readiness.expiresAt().isAfter(clock.instant()) ? "FRESH" : "STALE",
                readiness.verifiedAt() == null ? null : ageMs(readiness.verifiedAt()),
                "按需三AI分析；不影响规则方向、规则计划或持仓监控",
                readiness.ready() ? "按需调用" : "等待配置或下次健康检查");
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
        Instant latestSuccess = coinGlassProviderHealthService.snapshot().values().stream()
                .filter(value -> value.status() == UnifiedSourceStatus.READY)
                .map(CoinGlassProviderHealthService.CoinGlassEndpointHealth::fetchTime)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo).orElse(null);
        return item("DERIVATIVES_CONTEXT", "COINGLASS", status,
                coinGlassProperties.isEnabled(), configured, source == UnifiedSourceStatus.READY,
                "COINGLASS_" + source.name(), latestSuccess,
                source == UnifiedSourceStatus.READY ? "FRESH" : source.name(), ageMs(latestSuccess),
                "衍生品风险补充；缺失时规则方向降级为部分覆盖",
                source == UnifiedSourceStatus.READY ? "自动持续采集" : "等待下一轮自动重试");
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
        item.setNextCheckStatus("UNSCHEDULED");
        // Existing health sources expose freshness/TTL, not an actual scheduled check.
        // nextCheckAt stays null; do not turn expiration or retry copy into a fake schedule.
        item.setImpact(defaultImpact(category));
        item.setRetryStatus(connected ? "自动持续运行" : "等待自动重试");
        return item;
    }

    private ProviderReadinessVO.ProviderStatusVO item(String category, String name, String status,
                                                       boolean enabled, boolean configured, boolean connected,
                                                       String reason, Instant lastSuccessAt, String freshness,
                                                       Long latencyMs, String impact, String retryStatus) {
        return detail(item(category, name, status, enabled, configured, connected, reason),
                lastSuccessAt, freshness, latencyMs, impact, retryStatus);
    }

    private ProviderReadinessVO.ProviderStatusVO detail(ProviderReadinessVO.ProviderStatusVO item,
                                                         Instant lastSuccessAt, String freshness,
                                                         Long latencyMs, String impact, String retryStatus) {
        item.setLastSuccessAt(lastSuccessAt);
        item.setFreshness(freshness);
        item.setLatencyMs(latencyMs);
        item.setImpact(impact);
        item.setRetryStatus(retryStatus);
        return item;
    }

    private Long ageMs(Instant value) {
        return value == null ? null : Math.max(0L, clock.millis() - value.toEpochMilli());
    }

    private String defaultImpact(String category) {
        return switch (upper(category)) {
            case "MARKET_DATA" -> "价格、方向、计划与持仓监控";
            case "AI" -> "仅按需三AI分析；不影响规则链";
            case "DERIVATIVES_CONTEXT" -> "衍生品风险补充";
            default -> "外部背景信息补充";
        };
    }

    private void appendAiBudgetSummary(Map<String, String> summary) {
        if (aiCallLogService == null) return;
        BigDecimal limit = configuredAiDailyBudget();
        if (limit == null) return;
        BigDecimal used;
        try {
            used = aiCallLogService.sumChargeableCostSince(
                    LocalDate.now(ZoneOffset.UTC).atStartOfDay());
        } catch (RuntimeException ignored) {
            summary.put("aiBudgetStatus", "UNAVAILABLE");
            return;
        }
        if (used == null) used = BigDecimal.ZERO;
        summary.put("aiDailyLimitUsd", limit.toPlainString());
        summary.put("aiUsedTodayUsd", used.toPlainString());
        summary.put("aiRemainingUsd", limit.subtract(used).max(BigDecimal.ZERO).toPlainString());
        summary.put("aiBudgetStatus", used.compareTo(limit) >= 0 ? "EXHAUSTED" : "AVAILABLE");
        summary.put("aiBudgetResetAt", LocalDate.now(ZoneOffset.UTC).plusDays(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC).toString());
        summary.put("aiBudgetImpact", "仅影响按需三AI；规则方向、风险、计划和持仓监控继续运行");
    }

    private BigDecimal configuredAiDailyBudget() {
        String runtimeBudget = property("trade-model.ai.daily-budget-usd");
        if (hasText(runtimeBudget)) {
            try {
                BigDecimal configured = new BigDecimal(runtimeBudget.trim());
                return configured.compareTo(BigDecimal.ZERO) > 0 ? configured : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (fundamentalAiV41Properties == null || fundamentalAiV41Properties.getAiGate() == null
                || fundamentalAiV41Properties.getAiGate().getDailyCostMicrosLimit() == null) {
            return null;
        }
        return BigDecimal.valueOf(
                fundamentalAiV41Properties.getAiGate().getDailyCostMicrosLimit(), 6);
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
