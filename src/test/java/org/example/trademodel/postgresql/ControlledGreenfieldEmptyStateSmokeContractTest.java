package org.example.trademodel.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledGreenfieldEmptyStateSmokeContractTest {

    private static final Path VALIDATOR = Path.of("scripts/p3g-empty-state-validate.py").toAbsolutePath();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void emptyDashboardReviewAndBaselinePassOnlyWithFailClosedSemantics() throws Exception {
        ScriptResult result = runValidator(validDashboard(), validReview(), validBaseline());

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "EMPTY_DASHBOARD_FAIL_CLOSED: PASS",
                "EMPTY_REVIEW_CENTER_FAIL_CLOSED: PASS",
                "EMPTY_RUN_BASELINE_FAIL_CLOSED: PASS",
                "FAKE_ASSET_POSITION_PLAN_RECORDS: NONE");
    }

    @Test
    void fabricatedPlanOrPositionFailsClosed() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.put("positions", List.of(Map.of("positionId", 1)));

        ScriptResult result = runValidator(dashboard, validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("FAIL empty dashboard positions are not empty");
    }

    @Test
    void runnerDisablesAiProvidersSchedulersTradingAndExternalPush() throws Exception {
        String runner = Files.readString(Path.of(
                "scripts/controlled-greenfield-first-boot-rehearsal-p3g.sh"), StandardCharsets.UTF_8);

        assertThat(runner).contains(
                "TRADE_MODEL_SCHEDULERS_ENABLED=false",
                "TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED=false",
                "TRADE_MODEL_AI_ENABLED=false",
                "TRADE_MODEL_AI_OPENAI_ENABLED=false",
                "TRADE_MODEL_AI_GEMINI_ENABLED=false",
                "TRADE_MODEL_AI_XAI_ENABLED=false",
                "SMOKE_ALLOW_EXTERNAL_CALLS=false",
                "SMOKE_PHASE=FETCH",
                "SMOKE_PHASE=VALIDATE",
                "bash /repo/scripts/prod-smoke.sh",
                "bash \"${ROOT_DIR}/scripts/prod-smoke.sh\"",
                "SMOKE_CLIENT_PATH: INTERNAL_NETWORK_FIXED_DIGEST_CLIENT",
                "FLYWAY_DURING_APP_SMOKE: DISABLED",
                "EXTERNAL_PUSH_SEND: DISABLED");
    }

    @Test
    void splitProdSmokeValidatesInternalClientResponsesWithoutCredentials() throws Exception {
        OBJECT_MAPPER.writeValue(tempDir.resolve("health.json").toFile(), Map.of("status", "UP"));
        OBJECT_MAPPER.writeValue(tempDir.resolve("liveness.json").toFile(), Map.of("status", "UP"));
        OBJECT_MAPPER.writeValue(tempDir.resolve("readiness.json").toFile(), Map.of("status", "UP"));
        OBJECT_MAPPER.writeValue(tempDir.resolve("dashboard.json").toFile(), Map.of("data", validDashboard()));
        OBJECT_MAPPER.writeValue(tempDir.resolve("review.json").toFile(), Map.of("data", validReview()));

        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/prod-smoke.sh")
                .redirectErrorStream(true);
        builder.environment().put("SMOKE_PHASE", "VALIDATE");
        builder.environment().put("SMOKE_RESPONSE_DIR", tempDir.toString());
        builder.environment().put("SMOKE_ALLOW_EXTERNAL_CALLS", "false");
        builder.environment().remove("SMOKE_AUTH_USERNAME");
        builder.environment().remove("SMOKE_AUTH_PASSWORD");
        builder.environment().remove("APP_ADMIN_USERNAME");
        builder.environment().remove("APP_ADMIN_PASSWORD");

        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }

        assertThat(finished).isTrue();
        ScriptResult result = new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("PASS production smoke checks");
    }

    private ScriptResult runValidator(Map<String, Object> dashboard,
                                      Map<String, Object> review,
                                      Map<String, Object> baseline) throws Exception {
        Path dashboardFile = write("dashboard.json", dashboard);
        Path reviewFile = write("review.json", review);
        Path baselineFile = write("baseline.json", baseline);
        Process process = new ProcessBuilder(
                "python3", VALIDATOR.toString(), dashboardFile.toString(),
                reviewFile.toString(), baselineFile.toString())
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private Path write(String name, Object payload) throws Exception {
        Path path = tempDir.resolve(name);
        OBJECT_MAPPER.writeValue(path.toFile(), Map.of("data", payload));
        return path;
    }

    private Map<String, Object> validDashboard() {
        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("status", "NO_COMPLETE_PLAN");
        for (String field : List.of(
                "sourceAnalysisId", "sourceExecutionPlanId", "sourceTraceId", "direction",
                "entryZone", "stopLoss", "takeProfitRules", "leverageSuggestion",
                "positionSuggestion", "validPeriod", "validFrom", "expiresAt",
                "invalidCondition")) {
            suggestion.put(field, null);
        }
        Map<String, Object> safety = new LinkedHashMap<>();
        for (String field : List.of(
                "reviewOnly", "manualReviewOnly", "notTradeInstruction", "notExecutable",
                "notAutoTrading", "notOrderExecution", "notPushSend", "notExternalChannel",
                "notUserPositionCreation", "notUserPositionMutation")) {
            safety.put(field, true);
        }
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("header", Map.of(
                "dataSourceText", "Controlled empty PostgreSQL",
                "aiStatus", "NOT_CALLED"));
        dashboard.put("systemState", Map.of());
        dashboard.put("assets", List.of());
        dashboard.put("positions", List.of());
        dashboard.put("executionSuggestion", suggestion);
        dashboard.put("safety", safety);
        dashboard.put("aiDecision", Map.of("runStatus", "DISABLED"));
        dashboard.put("diagnostics", Map.of(
                "marketDataProvider", "NOT_CONFIGURED",
                "aiProvider", "NOT_CONFIGURED",
                "externalContextProvider", "NOT_CONFIGURED",
                "providerReadiness", Map.of("providers", List.of())));
        dashboard.put("pushInbox", Map.of("telegramStatus", "NOT_CONFIGURED"));
        return dashboard;
    }

    private Map<String, Object> validReview() {
        return Map.of(
                "positionReviews", List.of(),
                "opportunityReviews", List.of(),
                "pushReviews", List.of(),
                "ruleFeedback", List.of(),
                "summary", Map.of(
                        "positionReviewCount", 0,
                        "opportunityReviewCount", 0,
                        "pushReviewCount", 0,
                        "ruleFeedbackCount", 0));
    }

    private Map<String, Object> validBaseline() {
        return Map.of(
                "alertSummary", Map.of(
                        "openCountWindow", 0,
                        "suppressedCountWindow", 0,
                        "dataQualityOpenCountWindow", 0,
                        "dataQualitySuppressedCountWindow", 0),
                "dataQualitySummary", Map.of(
                        "analysisRunCountWindow", 0,
                        "lowQualityCountWindow", 0),
                "recheckSummary", Map.of(
                        "totalCountWindow", 0,
                        "statusCountsWindow", Map.of()),
                "hotResetSummary", Map.of(
                        "eventCountWindow", 0,
                        "triggerTypeCountsWindow", Map.of()));
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
