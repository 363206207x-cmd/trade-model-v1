package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderArchitectureGuardTest {
    private static final Path MAIN = Path.of("src/main/java/org/example/trademodel");

    @Test
    void noBusinessServiceDirectlyDependsOnProviderAdapter() throws Exception {
        for (String relative : List.of(
                "service/impl/DashboardHomeServiceImpl.java",
                "service/impl/DecisionServiceImpl.java",
                "service/DecisionEngineService.java",
                "service/impl/PositionMonitorServiceImpl.java",
                "service/impl/PushRecheckServiceImpl.java",
                "service/impl/PlanServiceImpl.java",
                "service/impl/OpportunityLogServiceImpl.java",
                "service/impl/ReviewCenterServiceImpl.java")) {
            String source = readIfPresent(MAIN.resolve(relative));
            assertThat(source)
                    .as(relative)
                    .doesNotContain(
                            "market.client.impl",
                            "providercall.adapter",
                            "RestTemplate",
                            "WebClient",
                            "CoinGlassV4Provider",
                            "OpenAiProviderClient",
                            "GeminiProviderClient",
                            "XaiProviderClient",
                            "TelegramBotClient",
                            "TelegramSender");
        }
    }

    @Test
    void dashboardUsesTrustedMonitorSnapshotWithoutMarketFallback() throws Exception {
        String source = readIfPresent(MAIN.resolve("service/impl/DashboardHomeServiceImpl.java"));
        assertThat(source)
                .contains("isTrustedAndFreshAt", "applyWaitingMonitor")
                .doesNotContain(
                        "marketPriceSnapshotService.peek(",
                        "marketPriceSnapshotService.get(",
                        "ProviderSnapshotRefreshService");
    }

    @Test
    void profileAndRuntimeStatusEndpointsCannotRefreshProviders() throws Exception {
        String controller = readIfPresent(MAIN.resolve("controller/ProviderCallProfileController.java"));
        assertThat(controller).contains("/api/provider-call")
                .doesNotContain("ProviderSnapshotRefreshService", "ProviderCallCoordinator", "adapterCall");
    }

    @Test
    void productionProviderSchedulerRemainsDefaultOff() throws Exception {
        String prod = Files.readString(Path.of("src/main/resources/application-prod.yml"));
        assertThat(prod).contains(
                "enabled: ${TRADE_MODEL_PROVIDER_CALL_ENABLED:false}",
                "scheduler-enabled: ${TRADE_MODEL_PROVIDER_SCAN_SCHEDULER_ENABLED:false}",
                "profile-escalation-enabled: ${TRADE_MODEL_PROFILE_ESCALATION_ENABLED:false}",
                "auto-escalation-enabled: ${TRADE_MODEL_PROVIDER_AUTO_ESCALATION_ENABLED:false}",
                "external-calls-enabled: ${TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED:false}");
    }

    @Test
    void noExecutionPlanCreatesUserPosition() throws Exception {
        String source = readIfPresent(MAIN.resolve("service/impl/PlanServiceImpl.java"));
        assertThat(source).doesNotContain("UserPositionMapper", "manualOpen(", "insertUserPosition");
    }

    @Test
    void positionMonitorDoesNotMutateUserPosition() throws Exception {
        String source = readIfPresent(MAIN.resolve("service/impl/PositionMonitorServiceImpl.java"));
        assertThat(source).doesNotContain("userPositionMapper.update", "manualClose(", "setStatus(\"CLOSED\")");
    }

    @Test
    void noAutoOpenCloseReverseOrOrder() throws Exception {
        String source = readTree(MAIN.resolve("providercall"));
        assertThat(source).doesNotContain("manualOpen(", "manualClose(", "autoReverse", "OrderMapper", "submitOrder");
    }

    @Test
    void noExternalPushOrTelegramSend() throws Exception {
        String source = readTree(MAIN.resolve("providercall"));
        assertThat(source).doesNotContain("Telegram", "PushDispatch", "sendMessage", "pushRecheckService.recheck");
    }

    private static String readIfPresent(Path path) throws Exception {
        return Files.exists(path) ? Files.readString(path) : "";
    }

    private static String readTree(Path root) throws Exception {
        StringBuilder out = new StringBuilder();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                out.append(Files.readString(path));
            }
        }
        return out.toString();
    }
}
