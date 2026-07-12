package org.example.trademodel.localreal;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRealScriptsContractTest {
    @Test
    void startScriptWaitsForDashboardReadyAndDisablesUnsafeProviders() throws Exception {
        String script = script("start-local-real-data.sh");
        assertThat(script)
                .contains("DASHBOARD_READY", "SECONDS + 180", "TRADE_MODEL_AI_ENABLED=false")
                .contains("TRADE_MODEL_COINGLASS_ENABLED=false", "TRADE_MODEL_PUBLIC_OHLCV_PROVIDER_ENABLED=true")
                .doesNotContain("trade-model.local-secret", "killall");
    }

    @Test
    void stopScriptOnlyStopsRecordedPidAndKeepsDatabase() throws Exception {
        String script = script("stop-local-real-data.sh");
        assertThat(script).contains("trade-model-v1-local-real.pid", "kill \"${pid}\"")
                .doesNotContain("killall", "trade-model-v1-local-real.mv.db");
    }

    @Test
    void statusScriptReportsPersistentRuntimeFacts() throws Exception {
        String script = script("status-local-real-data.sh");
        assertThat(script).contains("LATEST_KLINE_TIME", "CLOSED_KLINE_COUNT",
                "LATEST_ANALYSIS_TIME", "LATEST_DECISION_TIME", "DATABASE_FILE");
    }

    private static String script(String name) throws Exception {
        return Files.readString(Path.of("scripts", name));
    }
}
