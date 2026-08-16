package org.example.trademodel.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TargetRuntimePreflightTest {
    private static final String SECRET = "A9!target-runtime-secret-2026";

    @Test
    void completeTargetRuntimeConfigurationPassesWithoutEmittingValues() {
        Map<String, String> environment = completeEnvironment();

        TargetRuntimePreflight.Result result = TargetRuntimePreflight.evaluate(environment);

        assertThat(result.passed()).isTrue();
        assertThat(result.lines()).contains(
                "PASSWORD_POLICY=PASS",
                "POSTGRESQL_URL=PASS",
                "OPENAI_EXACT_MODEL=PASS",
                "GEMINI_EXACT_MODEL=PASS",
                "XAI_EXACT_MODEL=PASS",
                "MARKET_PROVIDER_PATH=PASS",
                "COINGLASS=NOT_CONFIGURED",
                "PREFLIGHT=PASS");
        assertThat(String.join("\n", result.lines()))
                .doesNotContain(SECRET, "postgres-secret", "test-openai", "test-gemini", "test-xai");
    }

    @Test
    void missingAndExplicitZeroValuesFailClosedWithDistinctStates() {
        Map<String, String> environment = completeEnvironment();
        environment.remove("TRADE_MODEL_INITIAL_PASSWORD");
        environment.remove("TRADE_MODEL_AI_OPENAI_RPM");
        environment.put("TRADE_MODEL_AI_GEMINI_INPUT_COST_PER_MILLION_USD", "0");
        environment.put("TRADE_MODEL_AI_XAI_MODEL", "fallback-model");

        TargetRuntimePreflight.Result result = TargetRuntimePreflight.evaluate(environment);

        assertThat(result.passed()).isFalse();
        assertThat(result.lines()).contains(
                "PASSWORD_POLICY=REJECTED",
                "PASSWORD_REASON_CODE=PASSWORD_MISSING",
                "OPENAI_RPM=MISSING",
                "GEMINI_INPUT_COST=EXPLICIT_ZERO",
                "XAI_EXACT_MODEL=INVALID",
                "PREFLIGHT=BLOCKED");
    }

    @Test
    void enabledCoinGlassRequiresKeyAndExplicitPositiveRpm() {
        Map<String, String> environment = completeEnvironment();
        environment.put("TRADE_MODEL_COINGLASS_ENABLED", "true");
        environment.put("TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED", "true");

        TargetRuntimePreflight.Result keyMissing = TargetRuntimePreflight.evaluate(environment);
        environment.put("COINGLASS_API_KEY", "fixture-key");
        TargetRuntimePreflight.Result rpmMissing = TargetRuntimePreflight.evaluate(environment);
        environment.put("COINGLASS_ADVERTISED_RPM", "0");
        TargetRuntimePreflight.Result invalidRpm = TargetRuntimePreflight.evaluate(environment);
        environment.put("COINGLASS_ADVERTISED_RPM", "17");
        TargetRuntimePreflight.Result configured = TargetRuntimePreflight.evaluate(environment);

        assertThat(keyMissing.passed()).isFalse();
        assertThat(keyMissing.lines()).contains("COINGLASS=KEY_MISSING", "PREFLIGHT=BLOCKED");
        assertThat(rpmMissing.passed()).isFalse();
        assertThat(rpmMissing.lines()).contains("COINGLASS=RPM_NOT_CONFIGURED", "PREFLIGHT=BLOCKED");
        assertThat(invalidRpm.passed()).isFalse();
        assertThat(invalidRpm.lines()).contains("COINGLASS=INVALID_RPM", "PREFLIGHT=BLOCKED");
        assertThat(configured.passed()).isTrue();
        assertThat(configured.lines()).contains("COINGLASS=CONFIGURED", "PREFLIGHT=PASS");
    }

    @Test
    void enabledButExternalCallsDisabledRemainsExplicitlyNotConfigured() {
        Map<String, String> environment = completeEnvironment();
        environment.put("TRADE_MODEL_COINGLASS_ENABLED", "true");
        environment.put("TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED", "false");

        TargetRuntimePreflight.Result result = TargetRuntimePreflight.evaluate(environment);

        assertThat(result.passed()).isTrue();
        assertThat(result.lines()).contains("COINGLASS=NOT_CONFIGURED", "PREFLIGHT=PASS");
    }

    @Test
    void telegramPreflightReportsPresenceAndReadinessWithoutEmittingValues() {
        Map<String, String> environment = completeEnvironment();
        environment.put("TRADE_MODEL_TELEGRAM_ENABLED", "true");
        environment.put("TRADE_MODEL_TELEGRAM_EXTERNAL_CALLS_ENABLED", "true");
        environment.put("TELEGRAM_BOT_TOKEN", "TEST_TOKEN");
        environment.put("TELEGRAM_CHAT_ID", "TEST_CHAT_ID");
        environment.put("TELEGRAM_API_BASE_URL", "https://api.example.test");
        environment.put("TRADE_MODEL_PUBLIC_BASE_URL", "https://app.example.test");

        TargetRuntimePreflight.Result result = TargetRuntimePreflight.evaluate(environment);

        assertThat(result.passed()).isTrue();
        assertThat(result.lines()).contains(
                "TELEGRAM_ENABLED=SET",
                "TELEGRAM_EXTERNAL_CALLS_ENABLED=SET",
                "TELEGRAM_BOT_TOKEN=SET",
                "TELEGRAM_CHAT_ID=SET",
                "TELEGRAM_API_BASE_URL=SET",
                "TELEGRAM_PUBLIC_BASE_URL=SET",
                "TELEGRAM_READINESS=READY",
                "TELEGRAM_REASON_CODE=CONFIGURATION_READY",
                "PREFLIGHT=PASS");
        assertThat(String.join("\n", result.lines()))
                .doesNotContain("TEST_TOKEN", "TEST_CHAT_ID", "api.example.test", "app.example.test");
    }

    @Test
    void enabledTelegramWithoutRequiredSecretMaterialFailsClosed() {
        Map<String, String> environment = completeEnvironment();
        environment.put("TRADE_MODEL_TELEGRAM_ENABLED", "true");
        environment.put("TRADE_MODEL_TELEGRAM_EXTERNAL_CALLS_ENABLED", "true");

        TargetRuntimePreflight.Result tokenMissing = TargetRuntimePreflight.evaluate(environment);
        environment.put("TELEGRAM_BOT_TOKEN", "TEST_TOKEN");
        TargetRuntimePreflight.Result chatMissing = TargetRuntimePreflight.evaluate(environment);

        assertThat(tokenMissing.passed()).isFalse();
        assertThat(tokenMissing.lines()).contains(
                "TELEGRAM_READINESS=BLOCKED", "TELEGRAM_REASON_CODE=TOKEN_MISSING", "PREFLIGHT=BLOCKED");
        assertThat(chatMissing.passed()).isFalse();
        assertThat(chatMissing.lines()).contains(
                "TELEGRAM_READINESS=BLOCKED", "TELEGRAM_REASON_CODE=CHAT_ID_MISSING", "PREFLIGHT=BLOCKED");
    }

    private static Map<String, String> completeEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put("TRADE_MODEL_AUTH_ENABLED", "true");
        environment.put("TRADE_MODEL_INITIAL_USERNAME", "operator");
        environment.put("TRADE_MODEL_INITIAL_PASSWORD", SECRET);
        environment.put("PROD_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/trade_model");
        environment.put("PROD_DATASOURCE_USERNAME", "trade_model");
        environment.put("PROD_DATASOURCE_PASSWORD", "postgres-secret");
        environment.put("TRADE_MODEL_AI_ENABLED", "true");
        environment.put("TRADE_MODEL_AI_DAILY_BUDGET_USD", "5");
        environment.put("TRADE_MODEL_AI_PER_ANALYSIS_BUDGET_USD", "0.5");
        provider(environment, "OPENAI", "OPENAI_API_KEY", "test-openai");
        provider(environment, "GEMINI", "GEMINI_API_KEY", "test-gemini");
        provider(environment, "XAI", "XAI_API_KEY", "test-xai");
        environment.put("TRADE_MODEL_KRAKEN_OHLCV_ENABLED", "true");
        environment.put("TRADE_MODEL_KRAKEN_OHLCV_EXTERNAL_CALLS_ENABLED", "true");
        environment.put("TRADE_MODEL_COINGLASS_ENABLED", "false");
        return environment;
    }

    private static void provider(Map<String, String> environment,
                                 String provider,
                                 String keyName,
                                 String keyValue) {
        environment.put("TRADE_MODEL_AI_" + provider + "_ENABLED", "true");
        environment.put(keyName, keyValue);
        environment.put("TRADE_MODEL_AI_" + provider + "_RPM", "10");
        environment.put("TRADE_MODEL_AI_" + provider + "_INPUT_COST_PER_MILLION_USD", "1");
        environment.put("TRADE_MODEL_AI_" + provider + "_OUTPUT_COST_PER_MILLION_USD", "2");
    }
}
