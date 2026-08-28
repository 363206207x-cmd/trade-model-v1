package org.example.trademodel.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.net.URI;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionProfileSafetyGuard implements ApplicationRunner {

    private static final Set<String> ALLOWED_ACTUATOR_EXPOSURE = Set.of("HEALTH");

    private static final Set<String> ALLOWED_SCHEDULER_POLICIES = Set.of(
            "LOCKED_DOWN",
            "EXPLICIT_OPT_IN"
    );

    private static final Set<String> ALLOWED_SCHEDULER_CLASSIFICATIONS = Set.of(
            "PROD_ALLOWED_DEFAULT_OFF",
            "PROD_ALLOWED_EXPLICIT_OPT_IN",
            "PROD_BLOCKED",
            "LOCAL_ONLY"
    );

    private static final List<SchedulerPolicyItem> PRODUCTION_SCHEDULERS = List.of(
            new SchedulerPolicyItem("push-recheck", "trade-model.schedulers.push-recheck.enabled", false),
            new SchedulerPolicyItem("position-sync", "trade-model.schedulers.position-sync.enabled", false),
            new SchedulerPolicyItem("market-data", "trade-model.schedulers.market-data.enabled", false),
            new SchedulerPolicyItem("ohlcv-ingestion", "trade-model.schedulers.ohlcv-ingestion.enabled", false),
            new SchedulerPolicyItem("watchlist", "trade-model.schedulers.watchlist.enabled", false),
            new SchedulerPolicyItem("position-monitor", "trade-model.schedulers.position-monitor.enabled", false),
            new SchedulerPolicyItem("analysis", "trade-model.analysis.scheduler.enabled", false),
            new SchedulerPolicyItem("provider-scan", "trade-model.provider-call.scheduler-enabled", false)
    );

    private final Environment environment;

    public ProductionProfileSafetyGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate(environment);
    }

    static void validate(Environment environment) {
        List<String> errors = new ArrayList<>();

        String datasourceUrl = property(environment, "spring.datasource.url");
        if (isBlank(datasourceUrl)) {
            errors.add("production datasource URL missing");
        } else if (isH2MemoryUrl(datasourceUrl)) {
            errors.add("H2 memory database is not allowed in prod");
        }

        String datasourceUsername = property(environment, "spring.datasource.username");
        if (isBlank(datasourceUsername)) {
            errors.add("production datasource username missing");
        }

        String datasourcePassword = property(environment, "spring.datasource.password");
        if (isBlank(datasourcePassword)) {
            errors.add("production datasource password missing");
        }

        if (isTrue(property(environment, "spring.h2.console.enabled"))) {
            errors.add("H2 console must be disabled in prod");
        }

        String providerType = normalized(property(environment, "position.provider.type"));
        if (isBlank(providerType)) {
            errors.add("production position provider type missing");
        } else if ("SIMULATED".equals(providerType)) {
            errors.add("simulated provider is not allowed in prod");
        } else if (!"BINANCE".equals(providerType)) {
            errors.add("unsupported production position provider type: " + providerType);
        }

        if ("BINANCE".equals(providerType)) {
            if (isBlank(property(environment, "binance.api.key"))) {
                errors.add("Binance API key missing for production position provider");
            }
            if (isBlank(property(environment, "binance.api.secret"))) {
                errors.add("Binance API secret missing for production position provider");
            }
        }

        validateExplicitOpenAiProvider(environment, errors);
        validateExplicitAiProvider(environment, errors, "Gemini", "trade-model.ai.gemini");
        validateExplicitAiProvider(environment, errors, "xAI", "trade-model.ai.xai");

        String serverAddress = normalizedAddress(property(environment, "server.address"));
        if (isPublicBind(serverAddress) && !isTrue(property(environment, "trade-model.production.allow-public-bind"))) {
            errors.add("production public server bind requires trade-model.production.allow-public-bind=true");
        }

        String actuatorExposure = property(environment, "management.endpoints.web.exposure.include");
        if (hasUnsafeActuatorExposure(actuatorExposure)) {
            errors.add("production actuator web exposure must be limited to health");
        }

        if (!isTrue(property(environment, "trade-model.auth.enabled"))) {
            errors.add("production personal authentication must be enabled");
        }

        if (!isTrue(property(environment, "server.servlet.session.cookie.http-only"))) {
            errors.add("production session cookie must be HttpOnly");
        }
        if (!"LAX".equals(normalized(property(environment, "server.servlet.session.cookie.same-site")))) {
            errors.add("production session cookie SameSite must be Lax");
        }
        if (!isTrue(property(environment, "server.servlet.session.cookie.secure"))) {
            errors.add("production session cookie must be Secure");
        }

        validateProductionSchedulerPolicy(environment, errors);
        validateCoreProductionLoopPolicy(environment, errors);
        validateOhlcvIngestionPolicy(environment, errors);
        validateProviderCallPolicy(environment, errors);
        validateProviderScanPolicy(environment, errors);
        validateCoinGlassPolicy(environment, errors);
        validateProductionRateLimit(environment, errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Unsafe prod profile config: " + String.join("; ", errors));
        }
    }

    private static void validateProductionSchedulerPolicy(Environment environment, List<String> errors) {
        String policy = normalized(property(environment, "trade-model.production.scheduler-policy"));
        if (isBlank(policy)) {
            errors.add("production scheduler policy missing");
            return;
        }
        if (!ALLOWED_SCHEDULER_POLICIES.contains(policy)) {
            errors.add("unsupported production scheduler policy: " + policy);
            return;
        }

        boolean globalSchedulersEnabled = isTrue(property(environment, "trade-model.schedulers.enabled"));
        if ("LOCKED_DOWN".equals(policy) && globalSchedulersEnabled) {
            errors.add("production global scheduler switch must be disabled under LOCKED_DOWN policy");
        }
        for (SchedulerPolicyItem scheduler : PRODUCTION_SCHEDULERS) {
            String classification = normalized(property(environment, scheduler.approvalProperty()));
            if (isBlank(classification)) {
                errors.add("production scheduler classification missing for " + scheduler.name());
                continue;
            }
            if (!ALLOWED_SCHEDULER_CLASSIFICATIONS.contains(classification)) {
                errors.add("unsupported production scheduler classification for "
                        + scheduler.name() + ": " + classification);
                continue;
            }

            boolean schedulerEnabled = isTrue(property(environment, scheduler.enabledProperty()));
            boolean effectivelyEnabled = schedulerEnabled && ("analysis".equals(scheduler.name()) || globalSchedulersEnabled);
            if (scheduler.defaultOffRequired() && schedulerEnabled) {
                errors.add("production " + scheduler.name() + " scheduler must remain default-off");
            }
            if ("LOCKED_DOWN".equals(policy) && effectivelyEnabled) {
                errors.add("production scheduler must be disabled under LOCKED_DOWN policy: " + scheduler.name());
            }
            if ("EXPLICIT_OPT_IN".equals(policy)
                    && effectivelyEnabled
                    && !"PROD_ALLOWED_EXPLICIT_OPT_IN".equals(classification)) {
                errors.add("production scheduler enabled without explicit opt-in classification: " + scheduler.name());
            }
            if (effectivelyEnabled && ("PROD_BLOCKED".equals(classification) || "LOCAL_ONLY".equals(classification))) {
                errors.add("production scheduler classification blocks enabled scheduler: " + scheduler.name());
            }
        }
    }

    private static void validateOhlcvIngestionPolicy(Environment environment, List<String> errors) {
        boolean globallyEnabled = isTrue(property(environment, "trade-model.schedulers.enabled"));
        boolean ingestionEnabled = isTrue(property(environment, "trade-model.schedulers.ohlcv-ingestion.enabled"));
        if (!globallyEnabled || !ingestionEnabled) {
            return;
        }
        String primaryProvider = normalized(property(environment, "trade-model.ohlcv.provider.primary"));
        validateExplicitOhlcvProvider(environment, errors, primaryProvider, "primary");

        boolean fallbackEnabled = isTrue(property(environment, "trade-model.ohlcv.provider.fallback-enabled"));
        String fallbackProvider = normalized(property(environment, "trade-model.ohlcv.provider.fallback"));
        if (fallbackEnabled) {
            if (primaryProvider.equals(fallbackProvider)) {
                errors.add("production OHLCV fallback must differ from primary provider");
            }
            validateExplicitOhlcvProvider(environment, errors, fallbackProvider, "fallback");
        }
        if (isTrue(property(environment, "trade-model.ohlcv.public-provider.enabled"))
                || isTrue(property(environment, "trade-model.ohlcv.public-provider.external-calls-enabled"))) {
            errors.add("production OHLCV ingestion must use an explicit named provider, not the generic public-provider alias");
        }
        String maxSymbolsValue = property(environment, "trade-model.schedulers.ohlcv-ingestion.max-symbols");
        if (!isPositiveInteger(maxSymbolsValue) || Integer.parseInt(trim(maxSymbolsValue)) > 20) {
            errors.add("production OHLCV ingestion Asset Pool batch size must be between 1 and 20");
        }
        Set<String> timeframes = Set.copyOf(Arrays.stream(trim(property(environment,
                        "trade-model.schedulers.ohlcv-ingestion.timeframes")).split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList());
        if (!timeframes.equals(Set.of("5m", "15m", "1h", "4h"))) {
            errors.add("production OHLCV ingestion must use exactly 5m,15m,1h,4h timeframes");
        }
    }

    private static void validateCoreProductionLoopPolicy(Environment environment, List<String> errors) {
        boolean globallyEnabled = isTrue(property(environment, "trade-model.schedulers.enabled"));
        boolean marketEnabled = isTrue(property(environment, "trade-model.schedulers.market-data.enabled"));
        boolean analysisEnabled = isTrue(property(environment, "trade-model.analysis.scheduler.enabled"));
        boolean ingestionEnabled = isTrue(property(environment,
                "trade-model.schedulers.ohlcv-ingestion.enabled"));
        if (marketEnabled || analysisEnabled) {
            if (!globallyEnabled || !marketEnabled || !analysisEnabled || !ingestionEnabled) {
                errors.add("core production opportunity loop requires global, market-data, analysis, and OHLCV schedulers");
            }
            if (!"BINANCE".equals(normalized(property(environment,
                    "trade-model.ohlcv.provider.primary")))) {
                errors.add("core production opportunity loop requires Binance primary OHLCV");
            }
            if (isTrue(property(environment, "trade-model.ohlcv.provider.fallback-enabled"))) {
                errors.add("core production opportunity loop forbids OHLCV fallback");
            }
            requireExactLong(environment, errors, "trade-model.analysis.scheduler.observing-interval-seconds", 900L);
            requireExactLong(environment, errors, "trade-model.analysis.scheduler.candidate-interval-seconds", 300L);
            requireExactLong(environment, errors, "trade-model.analysis.scheduler.waiting-trigger-interval-seconds", 120L);
            requireExactLong(environment, errors, "trade-model.analysis.scheduler.triggered-interval-seconds", 60L);
            requireExactLong(environment, errors, "trade-model.analysis.scheduler.fixed-delay-ms", 60000L);
            if (!"5M".equals(normalized(property(environment,
                    "trade-model.analysis.scheduler.decision-timeframe")))) {
                errors.add("core production opportunity loop decision timeframe must be 5m");
            }
            if (!isPositiveInteger(property(environment,
                    "trade-model.analysis.scheduler.required-closed-bars"))
                    || Integer.parseInt(trim(property(environment,
                    "trade-model.analysis.scheduler.required-closed-bars"))) < 100) {
                errors.add("core production opportunity loop requires at least 100 closed bars");
            }
        }
        if (isTrue(property(environment, "trade-model.schedulers.position-monitor.enabled"))) {
            if (!globallyEnabled) {
                errors.add("production position monitor requires the global scheduler opt-in");
            }
            requireExactLong(environment, errors,
                    "trade-model.schedulers.position-monitor.fixed-rate-ms", 30000L);
        }
    }

    private static void requireExactLong(Environment environment, List<String> errors,
                                         String property, long expected) {
        try {
            if (Long.parseLong(trim(property(environment, property))) != expected) {
                errors.add(property + " must equal " + expected);
            }
        } catch (NumberFormatException ex) {
            errors.add(property + " must equal " + expected);
        }
    }

    private static void validateExplicitOhlcvProvider(Environment environment,
                                                      List<String> errors,
                                                      String provider,
                                                      String role) {
        if (!Set.of("KRAKEN", "BINANCE").contains(provider)) {
            errors.add("production OHLCV " + role + " provider must be KRAKEN or BINANCE");
            return;
        }
        String prefix = "trade-model.ohlcv." + provider.toLowerCase(Locale.ROOT);
        if (!isTrue(property(environment, prefix + ".enabled"))) {
            errors.add("production OHLCV " + role + " requires explicitly enabled " + provider + " provider");
        }
        if (!isTrue(property(environment, prefix + ".external-calls-enabled"))) {
            errors.add("production OHLCV " + role + " requires explicit " + provider + " external-call opt-in");
        }
    }

    private static void validateProviderCallPolicy(Environment environment, List<String> errors) {
        boolean coordinatorEnabled = isTrue(property(environment, "trade-model.provider-call.enabled"));
        boolean schedulerEnabled = isTrue(property(environment, "trade-model.provider-call.scheduler-enabled"));
        boolean escalationEnabled = isTrue(property(environment, "trade-model.provider-call.profile-escalation-enabled"));
        boolean autoEscalationEnabled = isTrue(property(environment, "trade-model.provider-call.auto-escalation-enabled"));
        boolean externalCallsEnabled = isTrue(property(environment, "trade-model.provider-call.external-calls-enabled"));
        if ((schedulerEnabled || escalationEnabled || autoEscalationEnabled || externalCallsEnabled)
                && !coordinatorEnabled) {
            errors.add("production provider-call features require explicitly enabled coordinator");
        }
        if (schedulerEnabled && !externalCallsEnabled) {
            errors.add("production provider-call scheduler requires explicit external-call opt-in");
        }
    }

    private static void validateProviderScanPolicy(Environment environment, List<String> errors) {
        if (!isTrue(property(environment, "trade-model.provider-call.scheduler-enabled"))) {
            return;
        }
        if (!isTrue(property(environment, "trade-model.schedulers.enabled"))) {
            errors.add("production provider-call scheduler requires the global scheduler opt-in");
        }
        if (!"EXPLICIT_OPT_IN".equals(normalized(property(environment,
                "trade-model.production.scheduler-policy")))) {
            errors.add("production provider-call scheduler requires EXPLICIT_OPT_IN policy");
        }
        if (!"PROD_ALLOWED_EXPLICIT_OPT_IN".equals(normalized(property(environment,
                "trade-model.production.scheduler-approval.provider-scan")))) {
            errors.add("production provider-call scheduler requires explicit provider-scan approval");
        }
        if (!isTrue(property(environment, "trade-model.providers.coinglass.enabled"))) {
            errors.add("production provider-call scheduler requires explicitly enabled CoinGlass provider");
        }
        if (!isTrue(property(environment, "trade-model.providers.coinglass.external-calls-enabled"))) {
            errors.add("production provider-call scheduler requires CoinGlass external-call opt-in");
        }
        if (isBlank(property(environment, "trade-model.providers.coinglass.api-key"))) {
            errors.add("production provider-call scheduler requires CoinGlass API key");
        }
        String serverAddress = normalizedAddress(property(environment, "server.address"));
        if (!isLoopbackBind(serverAddress)
                || isTrue(property(environment, "trade-model.production.allow-public-bind"))) {
            errors.add("production provider-call scheduler requires private loopback binding with public exposure disabled");
        }
        if (!isFalse(property(environment, "trade-model.production.tailscale-funnel-enabled"))) {
            errors.add("production provider-call scheduler requires explicitly disabled Tailscale Funnel");
        }
    }

    private static void validateCoinGlassPolicy(Environment environment, List<String> errors) {
        boolean enabled = isTrue(property(environment, "trade-model.providers.coinglass.enabled"));
        boolean externalEnabled = isTrue(property(environment,
                "trade-model.providers.coinglass.external-calls-enabled"));
        if (!enabled && !externalEnabled) return;
        if (externalEnabled && !enabled) {
            errors.add("production CoinGlass external calls require explicitly enabled provider");
            return;
        }
        if (!externalEnabled) return;
        if (!isTrue(property(environment, "trade-model.provider-call.enabled"))) {
            errors.add("production CoinGlass requires explicitly enabled provider coordinator");
        }
        if (!isTrue(property(environment, "trade-model.provider-call.external-calls-enabled"))) {
            errors.add("production CoinGlass requires global external-call opt-in");
        }
        if (isBlank(property(environment, "trade-model.providers.coinglass.api-key"))) {
            errors.add("production CoinGlass API key missing");
        }
        if (!isOfficialCoinGlassUrl(property(environment, "trade-model.providers.coinglass.base-url"))) {
            errors.add("production CoinGlass base URL must be valid HTTPS");
        }
        if (!"CG-API-KEY".equals(trim(property(environment,
                "trade-model.providers.coinglass.auth-header-name")))) {
            errors.add("production CoinGlass v4 auth header must be CG-API-KEY");
        }
        if (!isPositiveInteger(property(environment, "trade-model.providers.coinglass.advertised-rpm"))) {
            errors.add("production CoinGlass advertised-rpm must be positive");
        }
        if (!isRatio(property(environment, "trade-model.providers.coinglass.internal-budget-ratio"))) {
            errors.add("production CoinGlass internal-budget-ratio must be between 0 and 1");
        }
    }

    private static void validateProductionRateLimit(Environment environment, List<String> errors) {
        if (!isTrue(property(environment, "trade-model.security.rate-limit.enabled"))) {
            errors.add("production rate limit must be enabled");
        }
        if (!isPositiveInteger(property(environment, "trade-model.security.rate-limit.requests-per-minute"))) {
            errors.add("production rate limit requests-per-minute must be positive");
        }
        if (!isPositiveLong(property(environment, "trade-model.security.rate-limit.window-ms"))) {
            errors.add("production rate limit window-ms must be positive");
        }
    }

    private static String property(Environment environment, String key) {
        try {
            return environment.getProperty(key);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isPositiveInteger(String value) {
        try {
            return Integer.parseInt(trim(value)) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean isPositiveLong(String value) {
        try {
            return Long.parseLong(trim(value)) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean isRatio(String value) {
        try {
            double parsed = Double.parseDouble(trim(value));
            return parsed > 0.0d && parsed < 1.0d;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean isOfficialCoinGlassUrl(String value) {
        try {
            URI uri = URI.create(trim(value));
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "open-api-v4.coinglass.com".equalsIgnoreCase(uri.getHost());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isH2MemoryUrl(String datasourceUrl) {
        return normalized(datasourceUrl).startsWith("JDBC:H2:MEM");
    }

    private static boolean isPublicBind(String serverAddress) {
        return "0.0.0.0".equals(serverAddress) || "::".equals(serverAddress);
    }

    private static boolean isLoopbackBind(String serverAddress) {
        return "127.0.0.1".equals(serverAddress)
                || "localhost".equals(serverAddress)
                || "::1".equals(serverAddress);
    }

    private static boolean isTrue(String value) {
        return "true".equalsIgnoreCase(trim(value));
    }

    private static boolean isFalse(String value) {
        return "false".equalsIgnoreCase(trim(value));
    }

    private static boolean hasUnsafeActuatorExposure(String exposure) {
        if (isBlank(exposure)) {
            return false;
        }
        for (String rawEndpoint : exposure.split(",")) {
            String endpoint = normalized(rawEndpoint);
            if (!ALLOWED_ACTUATOR_EXPOSURE.contains(endpoint)) {
                return true;
            }
        }
        return false;
    }

    private static void validateExplicitAiProvider(Environment environment,
                                                   List<String> errors,
                                                   String displayName,
                                                   String propertyPrefix) {
        if (!isTrue(property(environment, propertyPrefix + ".enabled"))) {
            return;
        }
        if (isBlank(property(environment, propertyPrefix + ".api-key"))) {
            errors.add(displayName + " API key missing for explicitly enabled production AI provider");
        }
        if (isBlank(property(environment, propertyPrefix + ".model"))) {
            errors.add(displayName + " model missing for explicitly enabled production AI provider");
        }
        if (isBlank(property(environment, propertyPrefix + ".base-url"))) {
            errors.add(displayName + " base URL missing for explicitly enabled production AI provider");
        }
    }

    private static void validateExplicitOpenAiProvider(Environment environment, List<String> errors) {
        String prefix = "trade-model.ai.openai";
        if (!isTrue(property(environment, prefix + ".enabled"))) {
            return;
        }
        if (isBlank(property(environment, prefix + ".api-key"))) {
            errors.add("OpenAI API key missing for explicitly enabled production AI provider");
        }
        String fast = property(environment, prefix + ".gpt-final.fast-model");
        String reasoning = property(environment, prefix + ".gpt-final.reasoning-model");
        String gpt55 = property(environment, prefix + ".gpt-final.fallback-models[0]");
        String gpt54 = property(environment, prefix + ".gpt-final.fallback-models[1]");
        if (!org.example.trademodel.ai.OpenAiModelRouter.isApprovedPrimary(fast)
                || !org.example.trademodel.ai.OpenAiModelRouter.isApprovedPrimary(reasoning)
                || !org.example.trademodel.ai.OpenAiModelRouter.isApprovedGpt55(gpt55)
                || !org.example.trademodel.ai.OpenAiModelRouter.isApprovedGpt54(gpt54)) {
            errors.add("OpenAI GPT_FINAL model routing must stay within approved GPT-5.6/5.5/5.4 models");
        }
        if (isBlank(property(environment, prefix + ".base-url"))) {
            errors.add("OpenAI base URL missing for explicitly enabled production AI provider");
        }
    }

    private record SchedulerPolicyItem(String name, String enabledProperty, boolean defaultOffRequired) {
        String approvalProperty() {
            return "trade-model.production.scheduler-approval." + name;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalized(String value) {
        return trim(value).toUpperCase(Locale.ROOT);
    }

    private static String normalizedAddress(String value) {
        return trim(value).toLowerCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
