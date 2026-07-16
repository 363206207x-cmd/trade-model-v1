package org.example.trademodel.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ProdReleaseGateSmokeIntegrityTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CURRENT_APP_URL = "http://current-controlled-app:18085";

    @TempDir
    Path tempDir;

    private Path stubBin;
    private Path curlLog;

    @BeforeEach
    void setUp() throws Exception {
        stubBin = Files.createDirectory(tempDir.resolve("bin"));
        curlLog = tempDir.resolve("curl.log");
        writeExecutable(stubBin.resolve("docker"), """
                #!/usr/bin/env bash
                exit 0
                """);
        writeExecutable(stubBin.resolve("curl"), """
                #!/usr/bin/env bash
                set -euo pipefail
                output=""
                url=""
                while [ "$#" -gt 0 ]; do
                  case "$1" in
                    -o) output="$2"; shift 2 ;;
                    -w|-u) shift 2 ;;
                    -sS) shift ;;
                    http://*|https://*) url="$1"; shift ;;
                    *) shift ;;
                  esac
                done
                [ -n "$output" ]
                [ -n "$url" ]
                printf '%s\n' "$url" >>"${CURL_LOG:?}"
                case "$url" in
                  */actuator/health|*/actuator/health/liveness|*/actuator/health/readiness)
                    payload='{"status":"UP"}'
                    ;;
                  */api/dashboard/home)
                    if [ "${CURL_DASHBOARD_MODE:-VALID}" = "INVALID" ]; then
                      payload='{"data":{"header":{}}}'
                    else
                      payload='{"data":{"header":{"dataSourceText":"controlled","aiStatus":"NOT_CALLED"},"systemState":{},"assets":[],"positions":[],"executionSuggestion":{},"aiDecision":{},"pushInbox":{"telegramStatus":"WAITING_SYNC"},"diagnostics":{"marketDataProvider":"NOT_CONFIGURED","aiProvider":"DISABLED","externalContextProvider":"NOT_CONFIGURED","providerReadiness":{"providers":[]}},"safety":{"notAutoTrading":true,"notOrderExecution":true}}}'
                    fi
                    ;;
                  */api/review/center)
                    payload='{"data":{"summary":{},"positionReviews":[],"opportunityReviews":[],"pushReviews":[],"ruleFeedback":[]}}'
                    ;;
                  *)
                    payload='{}'
                    ;;
                esac
                printf '%s' "$payload" >"$output"
                printf '200'
                """);
    }

    @Test
    void releaseGateForcesFetchAndValidateDespiteInheritedFetchPhase() throws Exception {
        Path canned = writeCannedResponses();

        ScriptResult result = runReleaseGate("FETCH", canned, "VALID");

        assertThat(result.exitCode()).as(result.output()).isEqualTo(2);
        assertThat(result.output()).contains("PASS production smoke checks", "PASS production smoke");
        assertThat(result.output()).doesNotContain("LOCAL_CONTROLLED_SPLIT_ONLY");
        assertCurrentAppWasFetched();
    }

    @Test
    void releaseGateForcesFetchAndValidateDespiteInheritedValidatePhase() throws Exception {
        Path canned = writeCannedResponses();

        ScriptResult result = runReleaseGate("VALIDATE", canned, "VALID");

        assertThat(result.exitCode()).as(result.output()).isEqualTo(2);
        assertThat(result.output()).contains("PASS production smoke checks", "PASS production smoke");
        assertCurrentAppWasFetched();
    }

    @Test
    void releaseGateClearsInheritedSmokeResponseDir() throws Exception {
        Path missingInheritedDirectory = tempDir.resolve("does-not-exist");

        ScriptResult result = runReleaseGate("FETCH_AND_VALIDATE", missingInheritedDirectory, "VALID");

        assertThat(result.exitCode()).as(result.output()).isEqualTo(2);
        assertThat(result.output()).contains("PASS production smoke checks", "PASS production smoke");
        assertThat(result.output()).doesNotContain("smoke response directory must be");
    }

    @Test
    void releaseGateCannotPassWithCannedResponseArtifacts() throws Exception {
        Path canned = writeCannedResponses();

        ScriptResult result = runReleaseGate("VALIDATE", canned, "INVALID");

        assertThat(result.exitCode()).as(result.output()).isEqualTo(1);
        assertThat(result.output()).contains(
                "FAIL dashboard missing keys",
                "FAIL production smoke failed",
                "PRODUCTION_RELEASE_GATE: FAIL");
        assertCurrentAppWasFetched();
    }

    @Test
    void defaultProductionSmokeStillFetchesAndValidates() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/prod-smoke.sh")
                .redirectErrorStream(true);
        configureCommonEnvironment(builder);
        builder.environment().remove("SMOKE_PHASE");
        builder.environment().remove("SMOKE_RESPONSE_DIR");
        builder.environment().remove("SMOKE_SPLIT_PHASE_CONFIRM");
        builder.environment().put("APP_URL", CURRENT_APP_URL);
        builder.environment().put("SMOKE_AUTH_USERNAME", "controlled-user");
        builder.environment().put("SMOKE_AUTH_PASSWORD", "controlled-password");

        ScriptResult result = run(builder);

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("PASS production smoke checks");
        assertThat(result.output()).doesNotContain("LOCAL_CONTROLLED_SPLIT_ONLY");
        assertCurrentAppWasFetched();
    }

    private ScriptResult runReleaseGate(String inheritedPhase,
                                        Path inheritedResponseDirectory,
                                        String dashboardMode) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/prod-release-gate.sh")
                .redirectErrorStream(true);
        configureCommonEnvironment(builder);
        Map<String, String> environment = builder.environment();
        environment.put("APP_URL", CURRENT_APP_URL);
        environment.put("SMOKE_AUTH_USERNAME", "controlled-user");
        environment.put("SMOKE_AUTH_PASSWORD", "controlled-password");
        environment.put("SMOKE_PHASE", inheritedPhase);
        environment.put("SMOKE_RESPONSE_DIR", inheritedResponseDirectory.toString());
        environment.put("SMOKE_SPLIT_PHASE_CONFIRM", "I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE");
        environment.put("CURL_DASHBOARD_MODE", dashboardMode);
        environment.put("RELEASE_GATE_REQUIRE_DOCKER", "true");
        environment.put("RELEASE_GATE_REQUIRE_BACKUP", "false");
        environment.put("RELEASE_GATE_REQUIRE_PROVIDER_SMOKE", "false");
        return run(builder);
    }

    private void configureCommonEnvironment(ProcessBuilder builder) {
        Map<String, String> environment = builder.environment();
        environment.put("PATH", stubBin + System.getProperty("path.separator") + environment.get("PATH"));
        environment.put("CURL_LOG", curlLog.toString());
    }

    private ScriptResult run(ProcessBuilder builder) throws Exception {
        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private void assertCurrentAppWasFetched() throws Exception {
        assertThat(Files.readAllLines(curlLog, StandardCharsets.UTF_8)).contains(
                CURRENT_APP_URL + "/actuator/health",
                CURRENT_APP_URL + "/actuator/health/liveness",
                CURRENT_APP_URL + "/actuator/health/readiness",
                CURRENT_APP_URL + "/api/dashboard/home",
                CURRENT_APP_URL + "/api/review/center");
    }

    private Path writeCannedResponses() throws Exception {
        Path directory = Files.createDirectories(tempDir.resolve("canned-responses"));
        for (String name : List.of("health.json", "liveness.json", "readiness.json")) {
            OBJECT_MAPPER.writeValue(directory.resolve(name).toFile(), Map.of("status", "UP"));
        }
        OBJECT_MAPPER.writeValue(directory.resolve("dashboard.json").toFile(), Map.of(
                "data", Map.of(
                        "header", Map.of("dataSourceText", "canned", "aiStatus", "NOT_CALLED"),
                        "systemState", Map.of(),
                        "assets", List.of(),
                        "positions", List.of(),
                        "executionSuggestion", Map.of(),
                        "aiDecision", Map.of(),
                        "pushInbox", Map.of("telegramStatus", "WAITING_SYNC"),
                        "diagnostics", Map.of(
                                "marketDataProvider", "NOT_CONFIGURED",
                                "aiProvider", "DISABLED",
                                "externalContextProvider", "NOT_CONFIGURED",
                                "providerReadiness", Map.of("providers", List.of())),
                        "safety", Map.of("notAutoTrading", true, "notOrderExecution", true))));
        OBJECT_MAPPER.writeValue(directory.resolve("review.json").toFile(), Map.of(
                "data", Map.of(
                        "summary", Map.of(),
                        "positionReviews", List.of(),
                        "opportunityReviews", List.of(),
                        "pushReviews", List.of(),
                        "ruleFeedback", List.of())));
        return directory;
    }

    private void writeExecutable(Path path, String content) throws Exception {
        Files.writeString(path, content.stripLeading(), StandardCharsets.UTF_8);
        assertThat(path.toFile().setExecutable(true)).isTrue();
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
