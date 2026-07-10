package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderSnapshotAdoptionArchitectureTest {
    private static final List<String> PRIMARY_CONSUMERS = List.of(
            "src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java",
            "src/main/java/org/example/trademodel/service/impl/DecisionServiceImpl.java",
            "src/main/java/org/example/trademodel/service/DecisionEngineService.java",
            "src/main/java/org/example/trademodel/service/impl/PositionMonitorServiceImpl.java",
            "src/main/java/org/example/trademodel/service/impl/PushRecheckServiceImpl.java",
            "src/main/java/org/example/trademodel/service/PushRecheckScheduler.java",
            "src/main/java/org/example/trademodel/service/impl/PlanServiceImpl.java",
            "src/main/java/org/example/trademodel/service/impl/OpportunityLogServiceImpl.java");

    @Test
    void businessConsumersDoNotCallProviderDirectly() throws Exception {
        assertNoToken("MarketQuoteClient");
    }

    @Test
    void pushRecheckNoLongerDirectlyUsesLegacyQuoteClient() throws Exception {
        assertNoToken("BinanceMarketQuoteClient");
    }

    @Test
    void primaryBusinessConsumersDoNotUseRealMarketDataFetcher() throws Exception {
        assertNoToken("RealMarketDataFetcherService");
    }

    @Test
    void primaryBusinessConsumersDoNotCallFetch24hTicker() throws Exception {
        assertNoToken("fetch24hTicker(");
    }

    @Test
    void primaryBusinessConsumersDoNotCallFetchKlines() throws Exception {
        assertNoToken("fetchKlines(");
    }

    @Test
    void positionMonitorNoLongerDirectlyUsesLegacyQuoteClient() throws Exception {
        assertThat(read(PRIMARY_CONSUMERS.get(3))).contains("MarketPriceSnapshotService");
        assertThat(read(PRIMARY_CONSUMERS.get(4))).contains("MarketPriceSnapshotService");
    }

    @Test
    void decisionEngineDependsOnAuthoritativeOhlcvBoundary() throws Exception {
        assertThat(read(PRIMARY_CONSUMERS.get(2))).contains("DecisionOhlcvSnapshotSource")
                .doesNotContain("1m\", 3");
    }

    @Test
    void pushSchedulerDoesNotFetchPriceBeforeRecheck() throws Exception {
        assertThat(read(PRIMARY_CONSUMERS.get(5))).contains("pushRecheckService.recheck(")
                .doesNotContain("MarketPriceSnapshotService");
    }

    @Test
    void dashboardReadDoesNotInvokeAi() throws Exception {
        assertThat(read(PRIMARY_CONSUMERS.get(0))).doesNotContain("AiDecisionOrchestratorService", ".review(");
    }

    @Test
    void positionTickDoesNotInvokeAi() throws Exception {
        assertThat(read(PRIMARY_CONSUMERS.get(3))).doesNotContain("AiDecisionOrchestratorService", ".review(");
    }

    private static void assertNoToken(String token) throws Exception {
        for (String file : PRIMARY_CONSUMERS) assertThat(read(file)).as(file).doesNotContain(token);
    }

    private static String read(String file) throws Exception { return Files.readString(Path.of(file)); }
}
