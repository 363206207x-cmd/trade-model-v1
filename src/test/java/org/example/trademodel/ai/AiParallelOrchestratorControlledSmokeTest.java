package org.example.trademodel.ai;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.service.AiDecisionOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiParallelOrchestratorControlledSmokeTest {
    private static final String ISOLATED_H2_URL =
            "jdbc:h2:mem:ai_parallel_controlled_smoke_test;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static volatile OfflineContextEvidence offlineContextEvidence;
    private final AiParallelOrchestratorControlledSmoke smoke =
            new AiParallelOrchestratorControlledSmoke();

    @Test
    void controlledLiveSmokeEntryPoint() {
        Map<String, String> environment = System.getenv();
        if (smoke.gateStatus(environment) != null) {
            smoke.run(environment, null, zeroAudit()).sanitizedOutputLines().forEach(System.out::println);
            return;
        }

        StageAudit stageAudit = StageAudit.fromEnvironment(environment);
        stageAudit.write("SPRING_STARTING");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                TradeModelApplication.class, ControlledTransportConfiguration.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off")
                .run()) {
            stageAudit.write("SPRING_READY");
            AiDecisionOrchestratorService orchestrator =
                    context.getBean(AiDecisionOrchestratorService.class);
            ControlledCountingAiHttpTransport transport =
                    context.getBean(ControlledCountingAiHttpTransport.class);
            stageAudit.write("ORCHESTRATOR_STARTING");
            AiParallelOrchestratorControlledSmoke.SmokeResult result =
                    smoke.run(environment, orchestrator, transport::snapshot);
            stageAudit.write("ORCHESTRATOR_COMPLETED");
            result.sanitizedOutputLines().forEach(System.out::println);
            stageAudit.write("OUTPUT_EMITTED");
            assertThat(result.liveProviderCalls()).isIn("0", "1", "2", "3", "UNKNOWN_MAX_3");
            assertThat(result.finalResultOrder()).isEqualTo(
                    AiParallelOrchestratorControlledSmoke.FINAL_RESULT_ORDER);
        }
    }

    @Test
    void defaultGateMakesZeroExternalCalls() {
        CountingService service = new CountingService(successResult());

        AiParallelOrchestratorControlledSmoke.SmokeResult result =
                smoke.run(Map.of(), service, zeroAudit());

        assertThat(result.status()).isEqualTo("SKIPPED_EXTERNAL_CALLS_DISABLED");
        assertThat(result.liveProviderCalls()).isEqualTo("0");
        assertThat(result.realKeysRead()).isZero();
        assertThat(service.calls).isZero();
    }

    @Test
    void missingAnyKeyMakesZeroExternalCalls() {
        CountingService service = new CountingService(successResult());
        Map<String, String> environment = enabledEnvironment();
        environment.remove("GEMINI_API_KEY");

        AiParallelOrchestratorControlledSmoke.SmokeResult result =
                smoke.run(environment, service, zeroAudit());

        assertThat(result.status()).isEqualTo("SKIPPED_MISSING_API_KEY");
        assertThat(result.liveProviderCalls()).isEqualTo("0");
        assertThat(service.calls).isZero();
    }

    @Test
    void missingConfirmationGateMakesZeroExternalCalls() {
        CountingService service = new CountingService(successResult());
        Map<String, String> environment = enabledEnvironment();
        environment.remove(AiParallelOrchestratorControlledSmoke.HARNESS_ENTRY);

        AiParallelOrchestratorControlledSmoke.SmokeResult result =
                smoke.run(environment, service, zeroAudit());

        assertThat(result.status()).isEqualTo("SKIPPED_HARNESS_ENTRY_MISSING");
        assertThat(result.liveProviderCalls()).isEqualTo("0");
        assertThat(service.calls).isZero();
    }

    @Test
    void successfulRunUsesFormalServiceAndDeterministicOrder() {
        CountingService service = new CountingService(successResult());
        AiParallelOrchestratorControlledSmoke.SmokeResult result = smoke.run(
                enabledEnvironment(), service,
                () -> Map.of(AiProviderName.OPENAI, "1", AiProviderName.GEMINI, "1", AiProviderName.XAI, "1"));

        assertThat(service.calls).isOne();
        assertThat(result.status()).isEqualTo("PASS");
        assertThat(result.orchestrationMode()).isEqualTo("AI_ASSISTED");
        assertThat(result.finalResultOrder()).isEqualTo(
                AiParallelOrchestratorControlledSmoke.FINAL_RESULT_ORDER);
        assertThat(result.liveProviderCalls()).isEqualTo("3");
    }

    @Test
    void partialAndRuleOnlyModesRemainVisible() {
        AiOrchestratorResult partial = successResult();
        partial.setOrchestrationMode(AiOrchestrationMode.PARTIAL_FALLBACK);
        partial.setPartialFallbackUsed(true);
        partial.setProviderSuccessCount(2);
        partial.setProviderTimeoutCount(1);
        partial.getProviderResults().get(1).setCallStatus(AiProviderCallStatus.TIMEOUT);

        AiOrchestratorResult failed = successResult();
        failed.setOrchestrationMode(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        failed.setProviderSuccessCount(0);
        failed.setProviderFailedCount(3);
        failed.getProviderResults().forEach(result -> result.setCallStatus(AiProviderCallStatus.FAILED));

        assertThat(smoke.run(enabledEnvironment(), new CountingService(partial), zeroAudit()).status())
                .isEqualTo("PASS_PARTIAL_FALLBACK");
        assertThat(smoke.run(enabledEnvironment(), new CountingService(failed), zeroAudit()).status())
                .isEqualTo("PASS_RULE_ONLY_FALLBACK");
    }

    @Test
    void fixedFixtureIsReviewOnlyAndContainsNoExecutionBoundary() {
        AiProviderRequest request = AiParallelOrchestratorControlledSmoke.fixedReviewRequest();

        assertThat(request.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(request.getTimeframe()).isEqualTo("15m");
        assertThat(request.getRuleMarketBias()).isEqualTo("BULLISH");
        assertThat(request.getRuleWorthOpening()).isFalse();
        assertThat(request.getEvidenceSummary()).isNotBlank();
        assertThat(request.getDecisionFacts()).containsEntry("reviewOnly", true)
                .containsEntry("manualReviewOnly", true)
                .containsEntry("notTradeInstruction", true)
                .containsEntry("notExecutable", true)
                .containsEntry("ruleDirectionPreserved", true);
    }

    @Test
    void sanitizedOutputDoesNotExposeSecretsOrProviderContent() {
        String secret = "secret-provider-value";
        Map<String, String> environment = enabledEnvironment();
        environment.put("OPENAI_API_KEY", secret);
        String output = String.join("\n", smoke.run(
                environment, new CountingService(successResult()), zeroAudit()).sanitizedOutputLines());

        assertThat(output).doesNotContain(secret, "Authorization", "Prompt", "raw response",
                        "Interaction ID")
                .contains("PRODUCTION_READINESS: BLOCKED")
                .contains("FINAL_RESULT_ORDER: "
                        + AiParallelOrchestratorControlledSmoke.FINAL_RESULT_ORDER);
    }

    @Test
    void countingTransportRefusesSecondCallAndPersistsBoundedCounts() throws Exception {
        Path marker = Files.createTempFile("ai-parallel-count", ".txt");
        try {
            ControlledCountingAiHttpTransport transport =
                    new ControlledCountingAiHttpTransport(request ->
                            new AiHttpResponse(200, "{}", Map.of()), marker);
            AiHttpRequest request = request("https://api.openai.com/v1/responses");

            transport.post(request);

            assertThat(transport.snapshot()).containsEntry(AiProviderName.OPENAI, "1")
                    .containsEntry(AiProviderName.GEMINI, "0")
                    .containsEntry(AiProviderName.XAI, "0");
            assertThatThrownBy(() -> transport.post(request))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("CALL_LIMIT_EXCEEDED");
            assertThat(Files.readString(marker)).contains("OPENAI=1", "GEMINI=0", "XAI=0");
        } finally {
            Files.deleteIfExists(marker);
        }
    }

    @Test
    void allZeroMarkerReportsZero() throws Exception {
        String output = failureOutput("OPENAI=0\nGEMINI=0\nXAI=0\n", "", "PRECHECK", "0", 1, false);

        assertThat(output).contains("OPENAI_CALL_COUNT: 0", "GEMINI_CALL_COUNT: 0",
                "XAI_CALL_COUNT: 0", "LIVE_PROVIDER_CALLS: 0");
    }

    @Test
    void mixedMarkerReportsExactTotal() throws Exception {
        String output = failureOutput("OPENAI=1\nGEMINI=0\nXAI=1\n", "", "PROVIDERS_SUBMITTED", "0", 1, false);

        assertThat(output).contains("OPENAI_CALL_COUNT: 1", "GEMINI_CALL_COUNT: 0",
                "XAI_CALL_COUNT: 1", "LIVE_PROVIDER_CALLS: 2");
    }

    @Test
    void malformedSingleProviderReportsPartialUnknown() throws Exception {
        String output = failureOutput("OPENAI=1\nGEMINI=invalid\nXAI=0\n", "", "PRECHECK", "0", 1, false);

        assertThat(output).contains("OPENAI_CALL_COUNT: 1", "GEMINI_CALL_COUNT: UNKNOWN_MAX_1",
                "XAI_CALL_COUNT: 0", "LIVE_PROVIDER_CALLS: UNKNOWN_MAX_3");
    }

    @Test
    void missingMarkerReportsUnknownMax3() throws Exception {
        String output = failureOutput(null, "", "PRECHECK", "0", 1, false);

        assertThat(output).contains("OPENAI_CALL_COUNT: UNKNOWN_MAX_1",
                "GEMINI_CALL_COUNT: UNKNOWN_MAX_1", "XAI_CALL_COUNT: UNKNOWN_MAX_1",
                "LIVE_PROVIDER_CALLS: UNKNOWN_MAX_3");
    }

    @Test
    void springContextFailureClassifiedSafely() throws Exception {
        assertFailureCategory("Failed to load ApplicationContext", "SPRING_CONTEXT_FAILURE");
    }

    @Test
    void databaseFailureClassifiedSafely() throws Exception {
        assertFailureCategory("org.h2.jdbc.JdbcSQLSyntaxErrorException", "DATABASE_INITIALIZATION_FAILURE");
    }

    @Test
    void beanFailureClassifiedSafely() throws Exception {
        assertFailureCategory("UnsatisfiedDependencyException", "BEAN_CONFIGURATION_FAILURE");
    }

    @Test
    void safetyGuardFailureClassifiedSafely() throws Exception {
        assertFailureCategory("ProductionProfileSafetyGuard", "PRODUCTION_SAFETY_GUARD_FAILURE");
    }

    @Test
    void assertionFailureClassifiedSafely() throws Exception {
        assertFailureCategory("org.opentest4j.AssertionFailedError", "TEST_ASSERTION_FAILURE");
    }

    @Test
    void watchdogFailureClassifiedSafely() throws Exception {
        String output = failureOutput("OPENAI=1\nGEMINI=0\nXAI=0\n", "", "PROVIDERS_SUBMITTED", "1", 143, false);

        assertThat(output).contains("AI_PARALLEL_HARNESS_FAILURE_CATEGORY: WATCHDOG_TIMEOUT",
                "AI_PARALLEL_HARNESS_PROCESS_EXIT: WATCHDOG", "OPENAI_CALL_COUNT: 1");
    }

    @Test
    void outputContractMissingClassifiedSafely() throws Exception {
        String output = failureOutput("OPENAI=0\nGEMINI=0\nXAI=0\n", "", "OUTPUT_EMITTED", "0", 0, false);

        assertThat(output).contains("AI_PARALLEL_HARNESS_FAILURE_CATEGORY: OUTPUT_CONTRACT_MISSING",
                "AI_PARALLEL_HARNESS_PROCESS_EXIT: SUCCESS");
    }

    @Test
    void rawStackTraceIsNotPrinted() throws Exception {
        String raw = "BeanCreationException\n\tat private.Secret.method(Secret.java:42)";
        String output = failureOutput("OPENAI=0\nGEMINI=0\nXAI=0\n", raw, "SPRING_STARTING", "0", 1, false);

        assertThat(output).doesNotContain("private.Secret", "Secret.java", "\tat ");
    }

    @Test
    void exceptionMessageIsNotPrinted() throws Exception {
        String message = "BeanCreationException: confidential failure detail";
        String output = failureOutput("OPENAI=0\nGEMINI=0\nXAI=0\n", message, "SPRING_STARTING", "0", 1, false);

        assertThat(output).doesNotContain("confidential failure detail");
    }

    @Test
    void keyPromptHeaderAndResponseAreNotPrinted() throws Exception {
        String sensitive = "BeanCreationException OPENAI_API_KEY=secret Prompt=hidden Authorization=Bearer-token response=private";
        String output = failureOutput("OPENAI=0\nGEMINI=0\nXAI=0\n", sensitive, "SPRING_STARTING", "0", 1, false);

        assertThat(output).doesNotContain("secret", "Prompt=hidden", "Authorization", "response=private");
    }

    @Test
    void stageMarkerIsReported() throws Exception {
        String output = failureOutput("OPENAI=1\nGEMINI=0\nXAI=0\n", "", "ORCHESTRATOR_STARTING", "0", 1, false);

        assertThat(output).contains("AI_PARALLEL_HARNESS_STAGE: ORCHESTRATOR_STARTING");
    }

    @Test
    void isolatedH2MySqlContextStartsSuccessfully() throws Exception {
        OfflineContextEvidence evidence = offlineContextEvidence();

        assertThat(evidence.springReady()).isTrue();
        assertThat(evidence.orchestratorBeanAvailable()).isTrue();
        assertThat(evidence.executorBeanAvailable()).isTrue();
        assertThat(evidence.datasourceUrl())
                .isEqualTo("jdbc:h2:mem:ai_parallel_controlled_smoke_test")
                .doesNotContain("trade_model_v1", "postgresql", "production");
        assertThat(evidence.contextClosed()).isTrue();
        assertThat(evidence.executorShutdown()).isTrue();
    }

    @Test
    void authoritativeSchemaInitializesInSmokeDatabase() throws Exception {
        assertThat(offlineContextEvidence().publicTableCount()).isGreaterThanOrEqualTo(20);
    }

    @Test
    void aiCallLogTableExistsAfterInitialization() throws Exception {
        OfflineContextEvidence evidence = offlineContextEvidence();

        assertThat(evidence.aiCallLogTableCount()).isOne();
        assertThat(evidence.aiCallLogRows()).isZero();
    }

    @Test
    void offlineContextStartupMakesZeroProviderCalls() throws Exception {
        OfflineContextEvidence evidence = offlineContextEvidence();

        assertThat(evidence.providerPostCount()).isZero();
        assertThat(evidence.userPositionRows()).isZero();
        assertThat(evidence.executionPlanRows()).isZero();
        assertThat(evidence.orderTableCount()).isZero();
    }

    @Test
    void postgresCompatibilityModeIsNotUsedBySmoke() throws Exception {
        String script = Files.readString(
                Path.of("scripts/ai-parallel-orchestrator-controlled-smoke.sh"));
        assertThat(script)
                .contains("jdbc:h2:mem:ai_parallel_controlled_smoke;DB_CLOSE_DELAY=-1;MODE=MySQL")
                .doesNotContain("ai_parallel_controlled_smoke;MODE=PostgreSQL");

        DriverManagerDataSource incompatible = new DriverManagerDataSource(
                "jdbc:h2:mem:ai_parallel_postgres_incompatible_" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa", "");
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"));

        assertThatThrownBy(() -> schema.execute(incompatible))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void sqlInitializationRemainsEnabled() throws Exception {
        String script = Files.readString(
                Path.of("scripts/ai-parallel-orchestrator-controlled-smoke.sh"));

        assertThat(script).contains("SPRING_SQL_INIT_MODE=always")
                .doesNotContain("SPRING_SQL_INIT_MODE=never");
        assertThat(offlineContextEvidence().schemaInitialized()).isTrue();
    }

    @Test
    void cleanupSuccessPathHasNoUnboundVariable() throws Exception {
        CleanupResult result = cleanupRun(0, true, false);

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).doesNotContain("unbound variable");
    }

    @Test
    void cleanupFailurePathHasNoUnboundVariable() throws Exception {
        CleanupResult result = cleanupRun(17, true, false);

        assertThat(result.exitCode()).isEqualTo(17);
        assertThat(result.output()).doesNotContain("unbound variable");
    }

    @Test
    void cleanupWatchdogPathHasNoUnboundVariable() throws Exception {
        CleanupResult result = cleanupRun(19, true, true);

        assertThat(result.exitCode()).isEqualTo(19);
        assertThat(result.output()).doesNotContain("unbound variable");
    }

    @Test
    void cleanupWithMissingTempPathsIsSafe() throws Exception {
        CleanupResult result = cleanupRun(0, false, false);

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isBlank();
    }

    @Test
    void cleanupPreservesOriginalExitCode() throws Exception {
        assertThat(cleanupRun(23, true, false).exitCode()).isEqualTo(23);
    }

    @Test
    void cleanupRemovesAllCreatedTempFiles() throws Exception {
        CleanupResult result = cleanupRun(0, true, false);

        assertThat(result.remainingTempFiles()).isZero();
    }

    @Test
    void cleanupDoesNotPrintSecretOrRawOutput() throws Exception {
        CleanupResult result = cleanupRun(29, true, false);

        assertThat(result.output()).doesNotContain("secret", "Prompt", "Authorization",
                "response", "stack trace");
    }

    @Test
    void deliberateDatabaseFailureStillProducesSanitizedCategory() throws Exception {
        String raw = "JdbcSQLSyntaxErrorException: confidential SQL and datasource details";
        String output = failureOutput("OPENAI=0\nGEMINI=0\nXAI=0\n", raw,
                "SPRING_STARTING", "0", 1, false);

        assertThat(output).contains(
                        "AI_PARALLEL_HARNESS_FAILURE_CATEGORY: DATABASE_INITIALIZATION_FAILURE",
                        "AI_PARALLEL_HARNESS_STAGE: SPRING_STARTING")
                .doesNotContain("confidential SQL", "datasource details");
    }

    @Test
    void stageMovesToSpringReadyOnSuccessfulContextStartup() throws Exception {
        assertThat(offlineContextEvidence().stage()).isEqualTo("SPRING_READY");
    }

    private static OfflineContextEvidence offlineContextEvidence() throws Exception {
        if (offlineContextEvidence != null) {
            return offlineContextEvidence;
        }
        synchronized (AiParallelOrchestratorControlledSmokeTest.class) {
            if (offlineContextEvidence != null) {
                return offlineContextEvidence;
            }
            Path stageFile = Files.createTempFile("ai-parallel-offline-stage", ".txt");
            StageAudit stageAudit = new StageAudit(stageFile);
            stageAudit.write("SPRING_STARTING");
            ConfigurableApplicationContext context = null;
            AiProviderExecutor executor = null;
            boolean springReady = false;
            boolean orchestratorAvailable = false;
            boolean executorAvailable = false;
            int publicTables = 0;
            int aiCallLogTableCount = 0;
            int aiCallLogRows = -1;
            int userPositionRows = -1;
            int executionPlanRows = -1;
            int orderTableCount = -1;
            int providerPostCount = -1;
            String datasourceUrl = "";
            try {
                context = new SpringApplicationBuilder(
                        TradeModelApplication.class, OfflineContextConfiguration.class)
                        .web(WebApplicationType.NONE)
                        .run(offlineContextArguments());
                stageAudit.write("SPRING_READY");
                springReady = true;
                orchestratorAvailable = context.getBean(AiDecisionOrchestratorService.class) != null;
                executor = context.getBean(AiProviderExecutor.class);
                executorAvailable = executor != null;
                JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
                publicTables = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
                        Integer.class);
                aiCallLogTableCount = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'TM_AI_CALL_LOG'",
                        Integer.class);
                aiCallLogRows = jdbc.queryForObject("SELECT COUNT(*) FROM tm_ai_call_log", Integer.class);
                userPositionRows = jdbc.queryForObject("SELECT COUNT(*) FROM tm_user_position", Integer.class);
                executionPlanRows = jdbc.queryForObject("SELECT COUNT(*) FROM tm_execution_plan", Integer.class);
                orderTableCount = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'TM_ORDER'",
                        Integer.class);
                providerPostCount = context.getBean("offlineProviderPostCount", AtomicInteger.class).get();
                DataSource dataSource = context.getBean(DataSource.class);
                try (var connection = dataSource.getConnection()) {
                    datasourceUrl = connection.getMetaData().getURL();
                }
            } finally {
                if (context != null) {
                    context.close();
                }
            }
            boolean executorShutdown = executor != null && executor.isShutdown();
            String stage = Files.readString(stageFile).trim();
            Files.deleteIfExists(stageFile);
            offlineContextEvidence = new OfflineContextEvidence(
                    springReady, orchestratorAvailable, executorAvailable,
                    publicTables > 0, publicTables, aiCallLogTableCount, aiCallLogRows,
                    userPositionRows, executionPlanRows, orderTableCount, providerPostCount, datasourceUrl,
                    stage, true, executorShutdown);
            return offlineContextEvidence;
        }
    }

    private static String[] offlineContextArguments() {
        return new String[]{
                "--spring.profiles.active=default",
                "--spring.main.banner-mode=off",
                "--spring.datasource.url=" + ISOLATED_H2_URL,
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.sql.init.mode=always",
                "--trade-model.schedulers.enabled=false",
                "--trade-model.schedulers.push-recheck.enabled=false",
                "--trade-model.schedulers.position-sync.enabled=false",
                "--trade-model.schedulers.position-monitor.enabled=false",
                "--trade-model.schedulers.market-data.enabled=false",
                "--trade-model.schedulers.ohlcv-ingestion.enabled=false",
                "--trade-model.schedulers.watchlist.enabled=false",
                "--trade-model.analysis.scheduler.enabled=false",
                "--trade-model.provider-call.scheduler-enabled=false",
                "--trade-model.ai.enabled=false",
                "--trade-model.ai.openai.enabled=false",
                "--trade-model.ai.gemini.enabled=false",
                "--trade-model.ai.xai.enabled=false",
                "--trade-model.ai.openai.api-key=",
                "--trade-model.ai.gemini.api-key=",
                "--trade-model.ai.xai.api-key="
        };
    }

    private static CleanupResult cleanupRun(int expectedExitCode, boolean createFiles,
                                            boolean startWatchdog) throws Exception {
        Path directory = Files.createTempDirectory("ai-parallel-cleanup-test");
        Path output = createFiles ? directory.resolve("output") : Path.of("");
        Path calls = createFiles ? directory.resolve("calls") : Path.of("");
        Path stage = createFiles ? directory.resolve("stage") : Path.of("");
        Path watchdog = createFiles ? directory.resolve("watchdog") : Path.of("");
        if (createFiles) {
            Files.writeString(output, "private raw output");
            Files.writeString(calls, "OPENAI=0\nGEMINI=0\nXAI=0\n");
            Files.writeString(stage, "PRECHECK\n");
            Files.writeString(watchdog, "0\n");
        }
        Path script = Path.of("scripts/ai-parallel-orchestrator-controlled-smoke.sh").toAbsolutePath();
        String shell = "source \"$1\"; "
                + "SMOKE_OUTPUT_FILE=\"$2\"; SMOKE_CALL_COUNT_FILE=\"$3\"; "
                + "SMOKE_STAGE_FILE=\"$4\"; SMOKE_WATCHDOG_FILE=\"$5\"; "
                + "if [[ \"$7\" == true ]]; then sleep 30 & WATCHDOG_PID=$!; fi; "
                + "trap smoke_cleanup EXIT; exit \"$6\"";
        Process process = new ProcessBuilder("bash", "-c", shell, "_", script.toString(),
                createFiles ? output.toString() : "",
                createFiles ? calls.toString() : "",
                createFiles ? stage.toString() : "",
                createFiles ? watchdog.toString() : "",
                Integer.toString(expectedExitCode), Boolean.toString(startWatchdog))
                .redirectErrorStream(true)
                .start();
        String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int actualExitCode = process.waitFor();
        long remaining;
        try (var paths = Files.list(directory)) {
            remaining = paths.count();
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        }
        return new CleanupResult(actualExitCode, processOutput, remaining);
    }

    private static Map<String, String> enabledEnvironment() {
        Map<String, String> environment = new java.util.HashMap<>();
        environment.put(AiParallelOrchestratorControlledSmoke.ENABLE_EXTERNAL_CALLS, "true");
        environment.put(AiParallelOrchestratorControlledSmoke.HARNESS_ENTRY,
                AiParallelOrchestratorControlledSmoke.HARNESS_CONFIRMATION);
        environment.put("TRADE_MODEL_AI_ENABLED", "true");
        environment.put("TRADE_MODEL_AI_OPENAI_ENABLED", "true");
        environment.put("TRADE_MODEL_AI_GEMINI_ENABLED", "true");
        environment.put("TRADE_MODEL_AI_XAI_ENABLED", "true");
        environment.put("OPENAI_API_KEY", "test-openai-key");
        environment.put("GEMINI_API_KEY", "test-gemini-key");
        environment.put("XAI_API_KEY", "test-xai-key");
        return environment;
    }

    private static void assertFailureCategory(String frameworkMarker, String expected) throws Exception {
        String output = failureOutput("OPENAI=0\nGEMINI=0\nXAI=0\n", frameworkMarker,
                "SPRING_STARTING", "0", 1, false);
        assertThat(output).contains("AI_PARALLEL_HARNESS_FAILURE_CATEGORY: " + expected);
    }

    private static String failureOutput(String callMarker, String capturedOutput, String stage,
                                        String watchdog, int processExit,
                                        boolean outputContractPresent) throws Exception {
        Path directory = Files.createTempDirectory("ai-parallel-diagnostic-test");
        try {
            Path calls = directory.resolve("calls");
            Path output = directory.resolve("output");
            Path stageFile = directory.resolve("stage");
            Path watchdogFile = directory.resolve("watchdog");
            if (callMarker != null) {
                Files.writeString(calls, callMarker);
            }
            Files.writeString(output, capturedOutput == null ? "" : capturedOutput);
            Files.writeString(stageFile, stage);
            Files.writeString(watchdogFile, watchdog);
            Path script = Path.of("scripts/ai-parallel-orchestrator-controlled-smoke.sh").toAbsolutePath();
            Process process = new ProcessBuilder("bash", "-c",
                    "source \"$1\"; emit_failure \"$2\" \"$3\" \"$4\" \"$5\" \"$6\" \"$7\"",
                    "_", script.toString(), output.toString(), calls.toString(), stageFile.toString(),
                    watchdogFile.toString(), Integer.toString(processExit),
                    Boolean.toString(outputContractPresent))
                    .redirectErrorStream(true)
                    .start();
            String result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(process.waitFor()).isZero();
            return result;
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                });
            }
        }
    }

    private static AiParallelOrchestratorControlledSmoke.CallCountAudit zeroAudit() {
        return () -> Map.of(AiProviderName.OPENAI, "0", AiProviderName.GEMINI, "0", AiProviderName.XAI, "0");
    }

    private static AiOrchestratorResult successResult() {
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setOrchestrationMode(AiOrchestrationMode.AI_ASSISTED);
        result.setProviderResults(List.of(
                success(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW),
                success(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW),
                success(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE)));
        result.setProviderSubmittedCount(3);
        result.setProviderCompletedCount(3);
        result.setProviderSuccessCount(3);
        result.setOrchestrationLatencyMs(25);
        return result;
    }

    private static AiProviderReviewResult success(AiProviderName provider, AiProviderRole role) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setLatencyMs(10L);
        return result;
    }

    private static AiHttpRequest request(String url) {
        AiHttpRequest request = new AiHttpRequest();
        request.setUrl(url);
        request.setBody("{}");
        request.setTimeout(java.time.Duration.ofSeconds(1));
        return request;
    }

    private static final class CountingService implements AiDecisionOrchestratorService {
        private final AiOrchestratorResult result;
        private int calls;

        private CountingService(AiOrchestratorResult result) {
            this.result = result;
        }

        @Override
        public AiOrchestratorResult review(AiProviderRequest request) {
            calls++;
            return result;
        }

        @Override
        public List<AiProviderReadiness> providerReadiness() {
            return List.of();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OfflineContextConfiguration {
        @Bean
        AtomicInteger offlineProviderPostCount() {
            return new AtomicInteger();
        }

        @Bean
        @Primary
        AiHttpTransport offlineRejectingAiHttpTransport(AtomicInteger offlineProviderPostCount) {
            return request -> {
                offlineProviderPostCount.incrementAndGet();
                throw new IOException("OFFLINE_CONTEXT_TRANSPORT_MUST_NOT_BE_CALLED");
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ControlledTransportConfiguration {
        @Bean
        StageAudit stageAudit() {
            return StageAudit.fromEnvironment(System.getenv());
        }

        @Bean
        @Primary
        ControlledCountingAiHttpTransport controlledCountingAiHttpTransport(StageAudit stageAudit) {
            String marker = System.getenv("AI_PARALLEL_SMOKE_CALL_COUNT_FILE");
            if (marker == null || marker.isBlank()) {
                throw new IllegalStateException("AI_PARALLEL_SMOKE_CALL_COUNT_FILE_REQUIRED");
            }
            return new ControlledCountingAiHttpTransport(new JdkAiHttpTransport(), Path.of(marker), stageAudit);
        }
    }

    static final class ControlledCountingAiHttpTransport implements AiHttpTransport {
        private final AiHttpTransport delegate;
        private final Path marker;
        private final StageAudit stageAudit;
        private final Map<AiProviderName, Integer> counts = new EnumMap<>(AiProviderName.class);

        ControlledCountingAiHttpTransport(AiHttpTransport delegate, Path marker) {
            this(delegate, marker, null);
        }

        ControlledCountingAiHttpTransport(AiHttpTransport delegate, Path marker, StageAudit stageAudit) {
            this.delegate = delegate;
            this.marker = marker;
            this.stageAudit = stageAudit;
            counts.put(AiProviderName.OPENAI, 0);
            counts.put(AiProviderName.GEMINI, 0);
            counts.put(AiProviderName.XAI, 0);
            persist();
        }

        @Override
        public AiHttpResponse post(AiHttpRequest request) throws IOException, InterruptedException {
            AiProviderName provider = providerFor(request == null ? null : request.getUrl());
            markAttempt(provider);
            if (stageAudit != null) {
                stageAudit.write("PROVIDERS_SUBMITTED");
            }
            return delegate.post(request);
        }

        synchronized Map<AiProviderName, String> snapshot() {
            Map<AiProviderName, String> result = new EnumMap<>(AiProviderName.class);
            counts.forEach((provider, count) -> result.put(provider, Integer.toString(count)));
            return result;
        }

        private synchronized void markAttempt(AiProviderName provider) throws IOException {
            int current = counts.getOrDefault(provider, 0);
            if (current >= 1) {
                throw new IOException("CONTROLLED_SMOKE_CALL_LIMIT_EXCEEDED");
            }
            counts.put(provider, 1);
            persist();
        }

        private synchronized void persist() {
            try {
                String content = "OPENAI=" + counts.get(AiProviderName.OPENAI) + "\n"
                        + "GEMINI=" + counts.get(AiProviderName.GEMINI) + "\n"
                        + "XAI=" + counts.get(AiProviderName.XAI) + "\n";
                Files.writeString(marker, content, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException exception) {
                throw new IllegalStateException("CONTROLLED_SMOKE_CALL_AUDIT_UNAVAILABLE", exception);
            }
        }

        private static AiProviderName providerFor(String url) throws IOException {
            String value = url == null ? "" : url.toLowerCase(java.util.Locale.ROOT);
            if (value.contains("api.openai.com")) {
                return AiProviderName.OPENAI;
            }
            if (value.contains("generativelanguage.googleapis.com")) {
                return AiProviderName.GEMINI;
            }
            if (value.contains("api.x.ai")) {
                return AiProviderName.XAI;
            }
            throw new IOException("CONTROLLED_SMOKE_UNKNOWN_PROVIDER_ENDPOINT");
        }
    }

    static final class StageAudit {
        private static final List<String> ALLOWED = List.of(
                "PRECHECK", "SPRING_STARTING", "SPRING_READY", "ORCHESTRATOR_STARTING",
                "PROVIDERS_SUBMITTED", "ORCHESTRATOR_COMPLETED", "OUTPUT_EMITTED");
        private final Path marker;

        StageAudit(Path marker) {
            this.marker = marker;
        }

        static StageAudit fromEnvironment(Map<String, String> environment) {
            String marker = environment.get("AI_PARALLEL_SMOKE_STAGE_FILE");
            if (marker == null || marker.isBlank()) {
                throw new IllegalStateException("CONTROLLED_SMOKE_STAGE_AUDIT_UNAVAILABLE");
            }
            return new StageAudit(Path.of(marker));
        }

        synchronized void write(String stage) {
            if (!ALLOWED.contains(stage)) {
                throw new IllegalArgumentException("CONTROLLED_SMOKE_STAGE_INVALID");
            }
            try {
                Files.writeString(marker, stage + "\n", StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException exception) {
                throw new IllegalStateException("CONTROLLED_SMOKE_STAGE_AUDIT_UNAVAILABLE", exception);
            }
        }
    }

    private record OfflineContextEvidence(boolean springReady,
                                          boolean orchestratorBeanAvailable,
                                          boolean executorBeanAvailable,
                                          boolean schemaInitialized,
                                          int publicTableCount,
                                          int aiCallLogTableCount,
                                          int aiCallLogRows,
                                          int userPositionRows,
                                          int executionPlanRows,
                                          int orderTableCount,
                                          int providerPostCount,
                                          String datasourceUrl,
                                          String stage,
                                          boolean contextClosed,
                                          boolean executorShutdown) {
    }

    private record CleanupResult(int exitCode, String output, long remainingTempFiles) {
    }
}
