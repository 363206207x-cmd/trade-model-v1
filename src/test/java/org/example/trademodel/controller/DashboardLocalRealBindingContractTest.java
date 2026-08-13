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
        assertThat(template).contains("暂无手动录入持仓", "最新价", "数据状态");
    }

    @Test
    void dashboardKeepsProviderDiagnosticsOutOfCompactAssetCard() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));
        int assetRendererStart = template.indexOf("function renderHomeAssetsFromPayload(assets, moduleState)");
        int assetRendererEnd = template.indexOf("function renderHomePositionsFromPayload(positions)", assetRendererStart);
        assertThat(assetRendererStart).isGreaterThanOrEqualTo(0);
        assertThat(assetRendererEnd).isGreaterThan(assetRendererStart);
        String compactAssetRenderer = template.substring(assetRendererStart, assetRendererEnd);

        assertThat(template).contains("sourceProvider", "timeframeFreshness")
                .isNotEmpty();
        assertThat(compactAssetRenderer)
                .doesNotContain("数据来源", "四周期新鲜度", "证据数", "分析时间");
    }

    @Test
    void dashboardDisplaysUnavailableAssetReason() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));
        assertThat(template).contains("unavailableReason", "数据源不可用");
    }

    @Test
    void localRealCoordinatorCannotCreatePositionsOrdersOrExternalPush() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/localreal/LocalRealDataCoordinator.java"));
        assertThat(source).doesNotContain("UserPosition", "Order", "Push", "Telegram", "ExecutionPlan");
    }
}
