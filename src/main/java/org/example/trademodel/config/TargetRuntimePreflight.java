package org.example.trademodel.config;

import org.example.trademodel.ai.AiConfigurationPresence;
import org.example.trademodel.security.InitialPasswordPolicy;
import org.example.trademodel.providercall.coinglass.CoinGlassConfigurationState;
import org.example.trademodel.telegram.TelegramBotApiClient;
import org.example.trademodel.telegram.TelegramClientResult;
import org.example.trademodel.telegram.TelegramProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TargetRuntimePreflight {
    private static final Map<String, String> EXACT_MODELS = Map.of(
            "TRADE_MODEL_AI_OPENAI_GPT_FINAL_REASONING_MODEL", "gpt-5.6-sol",
            "TRADE_MODEL_AI_GEMINI_MODEL", "gemini-3.5-flash",
            "TRADE_MODEL_AI_XAI_MODEL", "grok-4.5");

    private TargetRuntimePreflight() {
    }

    public static void main(String[] args) {
        Result result = evaluate(System.getenv());
        result.lines().forEach(System.out::println);
        if (!result.passed()) {
            System.exit(1);
        }
        if (contains(args, "--telegram-probe")) {
            TelegramClientResult probe = telegramProbe(System.getenv());
            System.out.println("TELEGRAM_PROBE=" + (probe.success() ? "READY" : "BLOCKED"));
            System.out.println("TELEGRAM_PROBE_REASON="
                    + (probe.success() ? "NONE" : safeReason(probe.errorCode())));
            if (probe.success() && probe.botUsername() != null) {
                System.out.println("TELEGRAM_BOT_USERNAME=" + probe.botUsername());
            }
            if (!probe.success()) System.exit(1);
        } else {
            System.out.println("TELEGRAM_PROBE=SKIPPED");
        }
    }

    static Result evaluate(Map<String, String> environment) {
        List<String> lines = new ArrayList<>();
        boolean passed = true;
        passed &= requiredBoolean(environment, lines, "TRADE_MODEL_AUTH_ENABLED", true, "AUTH_ENABLED");
        passed &= configured(environment, lines, "TRADE_MODEL_INITIAL_USERNAME", "BOOTSTRAP_USERNAME");
        InitialPasswordPolicy.Validation password = InitialPasswordPolicy.validate(
                environment.get("TRADE_MODEL_INITIAL_PASSWORD"));
        lines.add("PASSWORD_POLICY=" + (password.accepted() ? "PASS" : "REJECTED"));
        if (!password.accepted()) {
            lines.add("PASSWORD_REASON_CODE=" + password.reasonCode().name());
            passed = false;
        }

        String datasourceUrl = trim(environment.get("PROD_DATASOURCE_URL"));
        boolean postgres = datasourceUrl != null && datasourceUrl.startsWith("jdbc:postgresql://");
        lines.add("POSTGRESQL_URL=" + (datasourceUrl == null ? "MISSING" : postgres ? "PASS" : "INVALID"));
        passed &= postgres;
        passed &= configured(environment, lines, "PROD_DATASOURCE_USERNAME", "POSTGRESQL_USERNAME");
        passed &= configured(environment, lines, "PROD_DATASOURCE_PASSWORD", "POSTGRESQL_PASSWORD");

        passed &= requiredBoolean(environment, lines, "TRADE_MODEL_AI_ENABLED", true, "AI_ENABLED");
        passed &= positive(environment, lines, "TRADE_MODEL_AI_DAILY_BUDGET_USD", "AI_DAILY_BUDGET");
        passed &= positive(environment, lines, "TRADE_MODEL_AI_PER_ANALYSIS_BUDGET_USD", "AI_PER_ANALYSIS_BUDGET");
        passed &= aiProvider(environment, lines, "OPENAI", "OPENAI_API_KEY");
        passed &= aiProvider(environment, lines, "GEMINI", "GEMINI_API_KEY");
        passed &= aiProvider(environment, lines, "XAI", "XAI_API_KEY");

        boolean krakenReady = enabled(environment, "TRADE_MODEL_KRAKEN_OHLCV_ENABLED")
                && enabled(environment, "TRADE_MODEL_KRAKEN_OHLCV_EXTERNAL_CALLS_ENABLED");
        boolean binanceReady = (enabled(environment, "TRADE_MODEL_BINANCE_OHLCV_ENABLED")
                    || enabled(environment, "TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED"))
                && (enabled(environment, "TRADE_MODEL_BINANCE_OHLCV_EXTERNAL_CALLS_ENABLED")
                    || enabled(environment, "TRADE_MODEL_PUBLIC_OHLCV_EXTERNAL_CALLS_ENABLED"));
        lines.add("KRAKEN_OHLCV=" + (krakenReady ? "ENABLED" : "DISABLED"));
        lines.add("BINANCE_OHLCV=" + (binanceReady ? "ENABLED" : "DISABLED"));
        lines.add("BINANCE_RELEASE_POLICY="
                + (binanceReady ? "BLOCKED_MUST_BE_DISABLED" : "DISABLED_DUE_451"));
        lines.add("MARKET_PROVIDER_PATH="
                + (krakenReady && !binanceReady ? "PASS" : krakenReady ? "INVALID" : "MISSING"));
        passed &= krakenReady && !binanceReady;
        CoinGlassConfigurationState coinGlassState = coinGlassState(environment);
        lines.add("COINGLASS=" + coinGlassState.name());
        if (enabled(environment, "TRADE_MODEL_COINGLASS_ENABLED")
                && enabled(environment, "TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED")
                && coinGlassState != CoinGlassConfigurationState.CONFIGURED) {
            passed = false;
        }
        TelegramConfiguration telegram = telegramConfiguration(environment);
        lines.add("TELEGRAM_ENABLED=" + telegram.enabledPresence());
        lines.add("TELEGRAM_EXTERNAL_CALLS_ENABLED=" + telegram.externalPresence());
        lines.add("TELEGRAM_BOT_TOKEN=" + telegram.tokenPresence());
        lines.add("TELEGRAM_CHAT_ID=" + telegram.chatPresence());
        lines.add("TELEGRAM_API_BASE_URL=" + telegram.apiBasePresence());
        lines.add("TELEGRAM_PUBLIC_BASE_URL=" + telegram.publicBasePresence());
        lines.add("TELEGRAM_READINESS=" + telegram.readiness());
        lines.add("TELEGRAM_REASON_CODE=" + telegram.reasonCode());
        passed &= !"BLOCKED".equals(telegram.readiness());
        lines.add("PREFLIGHT=" + (passed ? "PASS" : "BLOCKED"));
        return new Result(passed, List.copyOf(lines));
    }

    private static boolean aiProvider(Map<String, String> environment,
                                      List<String> lines,
                                      String provider,
                                      String keyName) {
        boolean passed = requiredBoolean(environment, lines,
                "TRADE_MODEL_AI_" + provider + "_ENABLED", true, provider + "_ENABLED");
        passed &= configured(environment, lines, keyName, provider + "_KEY");
        passed &= positive(environment, lines, "TRADE_MODEL_AI_" + provider + "_RPM", provider + "_RPM");
        passed &= positive(environment, lines, "TRADE_MODEL_AI_" + provider + "_INPUT_COST_PER_MILLION_USD",
                provider + "_INPUT_COST");
        passed &= positive(environment, lines, "TRADE_MODEL_AI_" + provider + "_OUTPUT_COST_PER_MILLION_USD",
                provider + "_OUTPUT_COST");
        String modelKey = switch (provider) {
            case "OPENAI" -> "TRADE_MODEL_AI_OPENAI_GPT_FINAL_REASONING_MODEL";
            case "GEMINI" -> "TRADE_MODEL_AI_GEMINI_MODEL";
            default -> "TRADE_MODEL_AI_XAI_MODEL";
        };
        String configuredModel = trim(environment.get(modelKey));
        if (configuredModel == null) configuredModel = EXACT_MODELS.get(modelKey);
        boolean exact = EXACT_MODELS.get(modelKey).equals(configuredModel);
        lines.add(provider + "_EXACT_MODEL=" + (exact ? "PASS" : "INVALID"));
        return passed && exact;
    }

    private static boolean positive(Map<String, String> environment,
                                    List<String> lines,
                                    String key,
                                    String label) {
        String value = trim(environment.get(key));
        AiConfigurationPresence presence;
        try {
            presence = AiConfigurationPresence.of(value == null ? null : new BigDecimal(value));
        } catch (NumberFormatException invalid) {
            lines.add(label + "=INVALID");
            return false;
        }
        lines.add(label + "=" + presence.name());
        return presence == AiConfigurationPresence.POSITIVE_VALUE;
    }

    private static boolean configured(Map<String, String> environment,
                                      List<String> lines,
                                      String key,
                                      String label) {
        boolean configured = trim(environment.get(key)) != null;
        lines.add(label + "=" + (configured ? "SET" : "MISSING"));
        return configured;
    }

    private static boolean requiredBoolean(Map<String, String> environment,
                                           List<String> lines,
                                           String key,
                                           boolean required,
                                           String label) {
        boolean value = enabled(environment, key);
        lines.add(label + "=" + (value ? "ENABLED" : "DISABLED"));
        return value == required;
    }

    private static boolean enabled(Map<String, String> environment, String key) {
        return Boolean.parseBoolean(trim(environment.get(key)));
    }

    private static CoinGlassConfigurationState coinGlassState(Map<String, String> environment) {
        boolean enabled = enabled(environment, "TRADE_MODEL_COINGLASS_ENABLED");
        boolean external = enabled(environment, "TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED");
        boolean keyPresent = trim(environment.get("COINGLASS_API_KEY")) != null;
        String rawRpm = trim(environment.get("COINGLASS_ADVERTISED_RPM"));
        Integer rpm = null;
        if (rawRpm != null) {
            try {
                rpm = Integer.valueOf(rawRpm);
            } catch (NumberFormatException invalid) {
                return enabled && external && keyPresent
                        ? CoinGlassConfigurationState.INVALID_RPM
                        : CoinGlassConfigurationState.NOT_CONFIGURED;
            }
        }
        return CoinGlassConfigurationState.evaluate(enabled, external, keyPresent, rpm);
    }

    private static TelegramConfiguration telegramConfiguration(Map<String, String> environment) {
        String enabledRaw = trim(environment.get("TRADE_MODEL_TELEGRAM_ENABLED"));
        String externalRaw = trim(environment.get("TRADE_MODEL_TELEGRAM_EXTERNAL_CALLS_ENABLED"));
        boolean telegramEnabled = Boolean.parseBoolean(enabledRaw);
        boolean external = Boolean.parseBoolean(externalRaw);
        boolean token = trim(environment.get("TELEGRAM_BOT_TOKEN")) != null;
        boolean chat = trim(environment.get("TELEGRAM_CHAT_ID")) != null;
        boolean apiBase = trim(environment.get("TELEGRAM_API_BASE_URL")) != null;
        boolean publicBase = trim(environment.get("TRADE_MODEL_PUBLIC_BASE_URL")) != null;
        String readiness;
        String reason;
        if (!telegramEnabled || !external) {
            readiness = "NOT_CONFIGURED";
            reason = !telegramEnabled ? "TELEGRAM_DISABLED" : "EXTERNAL_CALLS_DISABLED";
        } else if (!token) {
            readiness = "BLOCKED";
            reason = "TOKEN_MISSING";
        } else if (!chat) {
            readiness = "BLOCKED";
            reason = "CHAT_ID_MISSING";
        } else {
            readiness = "READY";
            reason = publicBase ? "CONFIGURATION_READY" : "CONFIGURATION_READY_WITHOUT_PUBLIC_LINK";
        }
        return new TelegramConfiguration(presence(enabledRaw), presence(externalRaw), presence(token), presence(chat),
                apiBase ? "SET" : "MISSING", publicBase ? "SET" : "MISSING", readiness, reason);
    }

    private static TelegramClientResult telegramProbe(Map<String, String> environment) {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(enabled(environment, "TRADE_MODEL_TELEGRAM_ENABLED"));
        properties.setExternalCallsEnabled(enabled(environment, "TRADE_MODEL_TELEGRAM_EXTERNAL_CALLS_ENABLED"));
        properties.setBotToken(environment.get("TELEGRAM_BOT_TOKEN"));
        properties.setChatId(environment.get("TELEGRAM_CHAT_ID"));
        String apiBase = trim(environment.get("TELEGRAM_API_BASE_URL"));
        if (apiBase != null) properties.setApiBaseUrl(apiBase);
        return new TelegramBotApiClient(properties, new ObjectMapper()).getMe();
    }

    private static boolean contains(String[] args, String expected) {
        if (args == null) return false;
        for (String arg : args) if (expected.equals(arg)) return true;
        return false;
    }

    private static String safeReason(String value) {
        return value == null || !value.matches("[A-Z0-9_]+") ? "PROVIDER_VALIDATION_FAILED" : value;
    }

    private static String presence(boolean present) { return present ? "SET" : "MISSING"; }
    private static String presence(String value) { return value == null ? "MISSING" : "SET"; }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    record Result(boolean passed, List<String> lines) {
    }

    private record TelegramConfiguration(String enabledPresence,
                                         String externalPresence,
                                         String tokenPresence,
                                         String chatPresence,
                                         String apiBasePresence,
                                         String publicBasePresence,
                                         String readiness,
                                         String reasonCode) {
    }
}
