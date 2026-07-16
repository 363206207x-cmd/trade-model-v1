package org.example.trademodel.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProdSmokeScriptHealthTest {

    @Test
    void prodSmokeScriptHasValidShellSyntax() throws Exception {
        Path script = Path.of("scripts", "prod-smoke.sh");
        Process process = new ProcessBuilder("bash", "-n", script.toString())
                .redirectErrorStream(true)
                .start();

        String output = readOutput(process);
        int exitCode = process.waitFor();

        assertThat(exitCode)
                .as(output)
                .isZero();
    }

    @Test
    void prodSmokeScriptChecksProviderReadinessWithoutExternalCallsByDefault() throws Exception {
        String script = Files.readString(Path.of("scripts", "prod-smoke.sh"), StandardCharsets.UTF_8);

        assertThat(script).contains("SMOKE_ALLOW_EXTERNAL_CALLS=\"${SMOKE_ALLOW_EXTERNAL_CALLS:-false}\"");
        assertThat(script).contains(
                "SMOKE_PHASE=\"${SMOKE_PHASE:-FETCH_AND_VALIDATE}\"",
                "SMOKE_RESPONSE_DIR=\"${SMOKE_RESPONSE_DIR:-}\"",
                "SMOKE_SPLIT_PHASE_CONFIRM=\"${SMOKE_SPLIT_PHASE_CONFIRM:-}\"",
                "I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE",
                "FETCH|VALIDATE|FETCH_AND_VALIDATE",
                "existing non-symlink directory",
                "artifact must not be a symlink",
                "SMOKE_EVIDENCE_SCOPE: LOCAL_CONTROLLED_SPLIT_ONLY");
        assertThat(script).contains("dashboard header.dataSourceText missing");
        assertThat(script).contains("marketDataProvider");
        assertThat(script).contains("aiProvider");
        assertThat(script).contains("externalContextProvider");
        assertThat(script).contains("providerReadiness");
        assertThat(script).contains("\"NOT_CALLED\", \"DISABLED\"");
        assertThat(script).contains("provider status CONNECTED requires SMOKE_ALLOW_EXTERNAL_CALLS=true");
    }

    @Test
    void prodProviderSmokeScriptHasValidShellSyntax() throws Exception {
        Path script = Path.of("scripts", "prod-provider-smoke.sh");
        Process process = new ProcessBuilder("bash", "-n", script.toString())
                .redirectErrorStream(true)
                .start();

        String output = readOutput(process);
        int exitCode = process.waitFor();

        assertThat(exitCode)
                .as(output)
                .isZero();
    }

    @Test
    void prodProviderSmokeScriptIsOptInAndDoesNotContainTradingEndpointsOrSecretEchoes() throws Exception {
        String script = Files.readString(Path.of("scripts", "prod-provider-smoke.sh"), StandardCharsets.UTF_8);

        assertThat(script).contains("PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=\"${PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS:-false}\"");
        assertThat(script).contains("PROVIDER_LIVE_SMOKE: SKIPPED");
        assertThat(script)
                .doesNotContain("/api/v3/order")
                .doesNotContain("/fapi/v1/order")
                .doesNotContain("/sapi/v1/capital/withdraw")
                .doesNotContain("echo $OPENAI_API_KEY")
                .doesNotContain("echo $GEMINI_API_KEY")
                .doesNotContain("echo $XAI_API_KEY")
                .doesNotContain("echo $BINANCE_API_KEY")
                .doesNotContain("echo $BINANCE_API_SECRET");
    }

    @Test
    void prodProviderSmokeDefaultRunSkipsWithoutExternalCalls() throws Exception {
        Path script = Path.of("scripts", "prod-provider-smoke.sh");
        ProcessBuilder builder = new ProcessBuilder("bash", script.toString())
                .redirectErrorStream(true);
        builder.environment().put("PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS", "false");
        builder.environment().put("PROVIDER_SMOKE_BINANCE_PUBLIC_ENABLED", "false");
        builder.environment().put("PROVIDER_SMOKE_OPENAI_ENABLED", "false");
        builder.environment().put("PROVIDER_SMOKE_GEMINI_ENABLED", "false");
        builder.environment().put("PROVIDER_SMOKE_XAI_ENABLED", "false");
        Process process = builder.start();

        String output = readOutput(process);
        int exitCode = process.waitFor();

        assertThat(exitCode).as(output).isZero();
        assertThat(output).contains("PROVIDER_LIVE_SMOKE: SKIPPED");
        assertThat(output).contains("BINANCE_PUBLIC_SMOKE: SKIPPED");
        assertThat(output).contains("OPENAI_SMOKE: SKIPPED");
        assertThat(output).contains("GEMINI_SMOKE: SKIPPED");
        assertThat(output).contains("XAI_SMOKE: SKIPPED");
    }

    @Test
    void prodReleaseGateKeepsProviderSmokeOptionalByDefault() throws Exception {
        String script = Files.readString(Path.of("scripts", "prod-release-gate.sh"), StandardCharsets.UTF_8);

        assertThat(script).contains("RELEASE_GATE_REQUIRE_PROVIDER_SMOKE=\"${RELEASE_GATE_REQUIRE_PROVIDER_SMOKE:-false}\"");
        assertThat(script).contains(
                "SMOKE_PHASE=\"FETCH_AND_VALIDATE\"",
                "SMOKE_RESPONSE_DIR=\"\"",
                "SMOKE_SPLIT_PHASE_CONFIRM=\"\"");
        assertThat(script).contains("PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS=\"true\"");
        assertThat(script).contains("provider live smoke not required by this script run");
        assertThat(script).contains("provider live smoke did not produce PASS");
    }

    private static String readOutput(Process process) throws IOException {
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
