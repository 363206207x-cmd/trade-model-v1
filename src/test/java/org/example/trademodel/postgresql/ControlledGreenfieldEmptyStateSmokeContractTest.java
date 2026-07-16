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
                "EMPTY_ASSET_CARDS_FAIL_CLOSED: PASS",
                "EMPTY_SYSTEM_STATE_FAIL_CLOSED: PASS",
                "EMPTY_REVIEW_CENTER_FAIL_CLOSED: PASS",
                "EMPTY_RUN_BASELINE_FAIL_CLOSED: PASS",
                "FAKE_ASSET_CONCLUSIONS: NONE",
                "FAKE_POSITION_PLAN_RECORDS: NONE",
                "ASSET_ENUM_CONTRACT: PASS_EXACT_FORMAL_VALUES",
                "MARKET_BIAS_EMPTY_CONTRACT: WAIT_OR_EMPTY_ONLY",
                "ASSET_JSON_SHAPE: PASS_STRICT");
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
                "SMOKE_SPLIT_PHASE_CONFIRM=I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE",
                "bash /repo/scripts/prod-smoke.sh",
                "bash \"${ROOT_DIR}/scripts/prod-smoke.sh\"",
                "SMOKE_CLIENT_PATH: INTERNAL_NETWORK_FIXED_DIGEST_CLIENT",
                "APPLICATION_HOST_EXPOSURE: LOOPBACK_ONLY",
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
        builder.environment().put("SMOKE_SPLIT_PHASE_CONFIRM",
                "I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE");
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
        assertThat(result.output()).contains(
                "PASS controlled local split smoke validation",
                "SMOKE_EVIDENCE_SCOPE: LOCAL_CONTROLLED_SPLIT_ONLY");
        assertThat(result.output()).doesNotContain("PASS production smoke checks");
    }

    @Test
    void fetchOnlyRequiresExplicitLocalConfirmation() throws Exception {
        ScriptResult result = runProdSmoke(Map.of(
                "SMOKE_PHASE", "FETCH",
                "SMOKE_RESPONSE_DIR", tempDir.toString()));

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains(
                "FAIL split smoke phase requires explicit local-controlled confirmation");
    }

    @Test
    void validateOnlyRequiresExplicitLocalConfirmation() throws Exception {
        ScriptResult result = runProdSmoke(Map.of(
                "SMOKE_PHASE", "VALIDATE",
                "SMOKE_RESPONSE_DIR", tempDir.toString()));

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains(
                "FAIL split smoke phase requires explicit local-controlled confirmation");
    }

    @Test
    void splitModeCannotBeMistakenForProductionReleaseEvidence() throws Exception {
        writeProdSmokeResponses(tempDir);

        ScriptResult result = runProdSmoke(Map.of(
                "SMOKE_PHASE", "VALIDATE",
                "SMOKE_RESPONSE_DIR", tempDir.toString(),
                "SMOKE_SPLIT_PHASE_CONFIRM", "I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE"));

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("SMOKE_EVIDENCE_SCOPE: LOCAL_CONTROLLED_SPLIT_ONLY");
        assertThat(result.output()).doesNotContain("PASS production smoke checks");
    }

    @Test
    void splitModeRejectsSymlinkResponseDirectory() throws Exception {
        Path realDirectory = Files.createDirectory(tempDir.resolve("real-responses"));
        Path symlink = tempDir.resolve("linked-responses");
        Files.createSymbolicLink(symlink, realDirectory);

        ScriptResult result = runProdSmoke(Map.of(
                "SMOKE_PHASE", "VALIDATE",
                "SMOKE_RESPONSE_DIR", symlink.toString(),
                "SMOKE_SPLIT_PHASE_CONFIRM", "I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE"));

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("existing non-symlink directory");
    }

    @Test
    void splitModeRejectsSymlinkResponseArtifact() throws Exception {
        writeProdSmokeResponses(tempDir);
        Path health = tempDir.resolve("health.json");
        Path actualHealth = tempDir.resolve("actual-health.json");
        Files.move(health, actualHealth);
        Files.createSymbolicLink(health, actualHealth);

        ScriptResult result = runProdSmoke(Map.of(
                "SMOKE_PHASE", "VALIDATE",
                "SMOKE_RESPONSE_DIR", tempDir.toString(),
                "SMOKE_SPLIT_PHASE_CONFIRM", "I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE"));

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("artifact must not be a symlink");
    }

    @Test
    void completelyEmptyAssetListPass() throws Exception {
        ScriptResult result = runValidator(validDashboard(), validReview(), validBaseline());

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("EMPTY_ASSET_CARDS_FAIL_CLOSED: PASS");
    }

    @Test
    void safeEmptyAssetPlaceholdersPass() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.put("assets", List.of(safeEmptyAsset("BTCUSDT"), safeEmptyAsset("ETHUSDT")));

        ScriptResult result = runValidator(dashboard, validReview(), validBaseline());

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("FAKE_ASSET_CONCLUSIONS: NONE");
    }

    @Test
    void bullishAssetCardOnEmptyDatabaseFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        List<Map<String, Object>> assets = new java.util.ArrayList<>();
        for (String symbol : List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT")) {
            Map<String, Object> asset = safeEmptyAsset(symbol);
            asset.put("marketBias", "BULLISH");
            asset.put("worthOpening", true);
            asset.put("confidenceLevel", "HIGH");
            assets.add(asset);
        }
        dashboard.put("assets", assets);

        ScriptResult result = runValidator(dashboard, validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("empty-database marketBias conclusion");
    }

    @Test
    void bearishAssetCardOnEmptyDatabaseFails() throws Exception {
        ScriptResult result = runValidator(dashboardWithAsset("marketBias", "BEARISH"),
                validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("empty-database marketBias conclusion");
    }

    @Test
    void worthOpeningTrueOnEmptyDatabaseFails() throws Exception {
        ScriptResult result = runValidator(dashboardWithAsset("worthOpening", true),
                validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("worthOpening=true");
    }

    @Test
    void highConfidenceAssetOnEmptyDatabaseFails() throws Exception {
        ScriptResult result = runValidator(dashboardWithAsset("confidenceLevel", "HIGH"),
                validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("confidence conclusion");
    }

    @Test
    void nonZeroEvidenceOnEmptyDatabaseFails() throws Exception {
        ScriptResult result = runValidator(dashboardWithAsset("evidenceCount", 1),
                validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("non-zero evidenceCount");
    }

    @Test
    void latestAnalysisTimeOnEmptyDatabaseFails() throws Exception {
        ScriptResult result = runValidator(dashboardWithAsset(
                        "latestAnalysisTime", "2026-07-16T00:00:00"),
                validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("fabricated latestAnalysisTime");
    }

    @Test
    void directionalConclusionOnEmptyDatabaseFails() throws Exception {
        ScriptResult result = runValidator(dashboardWithAsset("currentConclusion", "建议做多并开仓"),
                validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("currentConclusion is not fail-closed");
    }

    @Test
    void planReadyAssetStateOnEmptyDatabaseFails() throws Exception {
        ScriptResult result = runValidator(dashboardWithAsset("assetState", "PLAN_READY"),
                validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("not a formal AssetStateEnum value");
    }

    @Test
    void bullishSystemMarketTrendOnEmptyDatabaseFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "marketTrend").put("value", "BULLISH");

        ScriptResult result = runValidator(dashboard, validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("systemState.marketTrend.value contains a directional/trading conclusion");
    }

    @Test
    void hotResetTriggeredOnEmptyDatabaseFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "hotReset").put("value", true);

        ScriptResult result = runValidator(dashboard, validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("hotReset is triggered");
    }

    @Test
    void pendingReviewNonZeroOnEmptyDatabaseFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "pendingReview").put("value", 1);

        ScriptResult result = runValidator(dashboard, validReview(), validBaseline());

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("pendingReview is not zero");
    }

    @Test
    void rangeAssetCardOnEmptyDatabaseFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("marketBias", "RANGE"),
                "empty-database marketBias conclusion");
    }

    @Test
    void neutralAssetCardOnEmptyDatabaseFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("marketBias", "NEUTRAL"),
                "not a formal MarketBiasEnum value");
    }

    @Test
    void rangeMarketBiasLabelOnEmptyAssetFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("marketBiasLabel", "震荡"),
                "marketBiasLabel is not a no-conclusion label");
    }

    @Test
    void rangeSystemMarketTrendOnEmptyDatabaseFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "marketTrend").put("value", "RANGE");

        assertValidatorFails(dashboard, "systemState.marketTrend.value contains");
    }

    @Test
    void unknownMarketBiasOnEmptyDatabaseFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("marketBias", "SIDEWAYS_UNKNOWN"),
                "not a formal MarketBiasEnum value");
    }

    @Test
    void waitMarketBiasPlaceholderPasses() throws Exception {
        Map<String, Object> dashboard = dashboardWithAsset("marketBias", "WAIT");
        @SuppressWarnings("unchecked")
        Map<String, Object> asset = (Map<String, Object>) ((List<?>) dashboard.get("assets")).get(0);
        asset.put("marketBiasLabel", "观望");

        assertValidatorPasses(dashboard, "MARKET_BIAS_EMPTY_CONTRACT: WAIT_OR_EMPTY_ONLY");
    }

    @Test
    void nullMarketBiasPlaceholderPasses() throws Exception {
        assertValidatorPasses(dashboardWithAsset("marketBias", null),
                "MARKET_BIAS_EMPTY_CONTRACT: WAIT_OR_EMPTY_ONLY");
    }

    @Test
    void dataInsufficientAssetStateFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("assetState", "DATA_INSUFFICIENT"),
                "not a formal AssetStateEnum value");
    }

    @Test
    void analyzingAssetStateFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("assetState", "ANALYZING"),
                "not a formal AssetStateEnum value");
    }

    @Test
    void unknownAssetStateFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("assetState", "UNKNOWN"),
                "not a formal AssetStateEnum value");
    }

    @Test
    void candidateAssetStateOnEmptyDatabaseFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("assetState", "CANDIDATE"),
                "has non-empty assetState");
    }

    @Test
    void triggeredAssetStateOnEmptyDatabaseFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("assetState", "TRIGGERED"),
                "has non-empty assetState");
    }

    @Test
    void nullAssetStatePlaceholderPasses() throws Exception {
        assertValidatorPasses(dashboardWithAsset("assetState", null),
                "ASSET_ENUM_CONTRACT: PASS_EXACT_FORMAL_VALUES");
    }

    @Test
    void nullAssetsFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.put("assets", null);

        assertValidatorFails(dashboard, "dashboard assets is not a list");
    }

    @Test
    void objectAssetsFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.put("assets", Map.of());

        assertValidatorFails(dashboard, "dashboard assets is not a list");
    }

    @Test
    void stringAssetsFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.put("assets", "");

        assertValidatorFails(dashboard, "dashboard assets is not a list");
    }

    @Test
    void numericAssetsFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.put("assets", 0);

        assertValidatorFails(dashboard, "dashboard assets is not a list");
    }

    @Test
    void booleanAssetsFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.put("assets", false);

        assertValidatorFails(dashboard, "dashboard assets is not a list");
    }

    @Test
    void missingAssetsFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.remove("assets");

        assertValidatorFails(dashboard, "dashboard assets is missing");
    }

    @Test
    void actualEmptyAssetArrayPasses() throws Exception {
        assertValidatorPasses(validDashboard(), "ASSET_JSON_SHAPE: PASS_STRICT");
    }

    @Test
    void nullTimeframeFreshnessFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("timeframeFreshness", null),
                "timeframeFreshness is not an object");
    }

    @Test
    void listTimeframeFreshnessFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("timeframeFreshness", List.of()),
                "timeframeFreshness is not an object");
    }

    @Test
    void stringTimeframeFreshnessFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("timeframeFreshness", ""),
                "timeframeFreshness is not an object");
    }

    @Test
    void numericTimeframeFreshnessFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("timeframeFreshness", 0),
                "timeframeFreshness is not an object");
    }

    @Test
    void booleanTimeframeFreshnessFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("timeframeFreshness", false),
                "timeframeFreshness is not an object");
    }

    @Test
    void missingTimeframeFreshnessFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        Map<String, Object> asset = safeEmptyAsset("BTCUSDT");
        asset.remove("timeframeFreshness");
        dashboard.put("assets", List.of(asset));

        assertValidatorFails(dashboard, "timeframeFreshness is missing");
    }

    @Test
    void extraTimeframeKeyFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("timeframeFreshness", Map.of(
                        "5m", "NO_DATA", "15m", "NO_DATA", "1h", "NO_DATA",
                        "4h", "NO_DATA", "1d", "NO_DATA")),
                "timeframeFreshness keys are not exact");
    }

    @Test
    void freshTimeframeOnEmptyDatabaseFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("timeframeFreshness", Map.of(
                        "5m", "FRESH", "15m", "NO_DATA", "1h", "NO_DATA", "4h", "NO_DATA")),
                "reports fresh timeframe data");
    }

    @Test
    void exactFourNoDataTimeframesPass() throws Exception {
        assertValidatorPasses(dashboardWithAsset("timeframeFreshness", Map.of(
                        "5m", "NO_DATA", "15m", "NO_DATA", "1h", "NO_DATA", "4h", "NO_DATA")),
                "ASSET_JSON_SHAPE: PASS_STRICT");
    }

    @Test
    void lowRiskConclusionOnEmptyAssetFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("riskLabel", "低风险"),
                "riskLabel is not a no-conclusion label");
    }

    @Test
    void rangeConclusionTextOnEmptyAssetFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("currentConclusion", "震荡"),
                "currentConclusion is not fail-closed");
    }

    @Test
    void candidateAssetStateLabelFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("assetStateLabel", "候选"),
                "assetStateLabel is not a no-conclusion label");
    }

    @Test
    void unknownSlotTypeFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("slotType", "PLACEHOLDER"),
                "unsupported slotType");
    }

    @Test
    void defaultSlotPasses() throws Exception {
        assertValidatorPasses(dashboardWithAsset("slotType", "DEFAULT_SLOT"),
                "ASSET_JSON_SHAPE: PASS_STRICT");
    }

    @Test
    void configuredSourceProviderOnEmptyAssetFails() throws Exception {
        assertValidatorFails(dashboardWithAsset("sourceProvider", "CONFIGURED"),
                "reports a connected sourceProvider");
    }

    @Test
    void configuredProviderReadinessWithoutConnectionPasses() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.put("diagnostics", Map.of(
                "marketDataProvider", "CONFIGURED",
                "aiProvider", "WAITING_SYNC",
                "externalContextProvider", "WAITING_SYNC",
                "providerReadiness", Map.of("providers", List.of(Map.of(
                        "status", "CONFIGURED", "connected", false)))));

        assertValidatorPasses(dashboard, "EMPTY_DASHBOARD_FAIL_CLOSED: PASS");
    }

    @Test
    void connectedProviderReadinessFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        dashboard.put("diagnostics", Map.of(
                "marketDataProvider", "CONFIGURED",
                "aiProvider", "WAITING_SYNC",
                "externalContextProvider", "WAITING_SYNC",
                "providerReadiness", Map.of("providers", List.of(Map.of(
                        "status", "CONFIGURED", "connected", true)))));

        assertValidatorFails(dashboard, "claims a live connection");
    }

    @Test
    void marketTrendRangeFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "marketTrend").put("valueLabel", "震荡");

        assertValidatorFails(dashboard, "systemState.marketTrend.valueLabel contains");
    }

    @Test
    void marketTrendMetaBullishFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "marketTrend").put("meta", Map.of("hidden", "BULLISH"));

        assertValidatorFails(dashboard, "contains hidden meta evidence");
    }

    @Test
    void pendingReviewScoreNonZeroFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "pendingReview").put("score", 1);

        assertValidatorFails(dashboard, "systemState.pendingReview fabricated a score");
    }

    @Test
    void confusedScoreNonZeroFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "confused").put("score", 1);

        assertValidatorFails(dashboard, "systemState.confused fabricated a score");
    }

    @Test
    void hotResetScoreNonZeroFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "hotReset").put("score", 1);

        assertValidatorFails(dashboard, "systemState.hotReset fabricated a score");
    }

    @Test
    void riskLevelLowOnEmptyDatabaseFails() throws Exception {
        Map<String, Object> dashboard = validDashboard();
        systemCard(dashboard, "riskLevel").put("value", "LOW");

        assertValidatorFails(dashboard, "riskLevel reports an evidenced risk conclusion");
    }

    private void assertValidatorFails(Map<String, Object> dashboard, String expected) throws Exception {
        ScriptResult result = runValidator(dashboard, validReview(), validBaseline());

        assertThat(result.exitCode()).as(result.output()).isNotZero();
        assertThat(result.output()).contains(expected);
    }

    private void assertValidatorPasses(Map<String, Object> dashboard, String expected) throws Exception {
        ScriptResult result = runValidator(dashboard, validReview(), validBaseline());

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains(expected);
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

    private ScriptResult runProdSmoke(Map<String, String> overrides) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/prod-smoke.sh")
                .redirectErrorStream(true);
        for (String name : List.of(
                "SMOKE_PHASE", "SMOKE_RESPONSE_DIR", "SMOKE_SPLIT_PHASE_CONFIRM",
                "SMOKE_AUTH_USERNAME", "SMOKE_AUTH_PASSWORD", "APP_ADMIN_USERNAME",
                "APP_ADMIN_PASSWORD")) {
            builder.environment().remove(name);
        }
        builder.environment().putAll(overrides);
        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private void writeProdSmokeResponses(Path directory) throws Exception {
        OBJECT_MAPPER.writeValue(directory.resolve("health.json").toFile(), Map.of("status", "UP"));
        OBJECT_MAPPER.writeValue(directory.resolve("liveness.json").toFile(), Map.of("status", "UP"));
        OBJECT_MAPPER.writeValue(directory.resolve("readiness.json").toFile(), Map.of("status", "UP"));
        OBJECT_MAPPER.writeValue(directory.resolve("dashboard.json").toFile(), Map.of("data", validDashboard()));
        OBJECT_MAPPER.writeValue(directory.resolve("review.json").toFile(), Map.of("data", validReview()));
    }

    private Map<String, Object> dashboardWithAsset(String field, Object value) {
        Map<String, Object> dashboard = validDashboard();
        Map<String, Object> asset = safeEmptyAsset("BTCUSDT");
        asset.put(field, value);
        dashboard.put("assets", List.of(asset));
        return dashboard;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> systemCard(Map<String, Object> dashboard, String name) {
        return (Map<String, Object>) ((Map<String, Object>) dashboard.get("systemState")).get(name);
    }

    private Map<String, Object> safeEmptyAsset(String symbol) {
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("slot", 1);
        asset.put("slotType", "DEFAULT_SLOT");
        asset.put("symbol", symbol);
        asset.put("rawSymbol", symbol);
        asset.put("marketBias", null);
        asset.put("marketBiasLabel", null);
        asset.put("compositeScore", null);
        asset.put("confidenceLevel", null);
        asset.put("confidenceLabel", null);
        asset.put("riskLevel", null);
        asset.put("riskLabel", null);
        asset.put("assetState", null);
        asset.put("assetStateLabel", null);
        asset.put("worthOpening", null);
        asset.put("latestPrice", null);
        asset.put("dataFreshness", "NO_DATA");
        asset.put("timeframeFreshness", Map.of(
                "5m", "NO_DATA", "15m", "NO_DATA", "1h", "NO_DATA", "4h", "NO_DATA"));
        asset.put("sourceProvider", null);
        asset.put("unavailableReason", null);
        asset.put("evidenceCount", null);
        asset.put("latestAnalysisTime", null);
        asset.put("currentConclusion", null);
        return asset;
    }

    private Map<String, Object> safeSystemState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("marketTrend", statusCard("WAITING_SYNC", null, null));
        state.put("riskLevel", statusCard("WAITING_SYNC", null, null));
        state.put("dataQuality", statusCard("WAITING_SYNC", null, null));
        state.put("aiConflict", statusCard("NOT_APPLICABLE", null, null));
        state.put("pendingReview", statusCard("CONNECTED", 0, 0));
        state.put("confused", statusCard("CONNECTED", 0, 0));
        Map<String, Object> hotReset = statusCard("CONNECTED", false, null);
        hotReset.put("meta", Map.of());
        state.put("hotReset", hotReset);
        return state;
    }

    private Map<String, Object> statusCard(String status, Object value, Object score) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("status", status);
        card.put("value", value);
        card.put("score", score);
        return card;
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
        dashboard.put("systemState", safeSystemState());
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
