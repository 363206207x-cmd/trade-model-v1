package org.example.trademodel.config;

import org.example.trademodel.ai.AiConfigurationPresence;
import org.example.trademodel.security.InitialPasswordPolicy;

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
        lines.add("MARKET_PROVIDER_PATH=" + (krakenReady || binanceReady ? "PASS" : "MISSING"));
        passed &= krakenReady || binanceReady;
        boolean coinGlassConfigured = enabled(environment, "TRADE_MODEL_COINGLASS_ENABLED")
                && trim(environment.get("COINGLASS_API_KEY")) != null;
        lines.add("COINGLASS=" + (coinGlassConfigured ? "CONFIGURED" : "NOT_CONFIGURED"));
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

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    record Result(boolean passed, List<String> lines) {
    }
}
