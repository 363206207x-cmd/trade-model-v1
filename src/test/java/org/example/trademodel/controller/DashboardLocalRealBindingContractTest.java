package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardLocalRealBindingContractTest {
    @Test
    void dashboardReadsPersistedMarketDataAndLatestDecisionWithoutMockFallback() throws Exception {
        String service = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java"));
        assertThat(service)
                .contains("PersistedOhlcvBarMapper", "selectLatestClosedWindow", "selectAverageScoreByAnalysisId")
                .doesNotContain("MockProvider", "FakeProvider", "RandomKline");
    }

    @Test
    void noUserPositionShowsExplicitEmptyState() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));
        assertThat(template).contains("暂无手动录入持仓", "最新价", "数据状态", "证据数", "分析时间");
    }

    @Test
    void localRealCoordinatorCannotCreatePositionsOrdersOrExternalPush() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/localreal/LocalRealDataCoordinator.java"));
        assertThat(source).doesNotContain("UserPosition", "Order", "Push", "Telegram", "ExecutionPlan");
    }
}
