package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class ApprovedFigmaHomeRuntimeContractTest {
    private static final Path HOME = Path.of("src/main/resources/templates/home.html");
    private static final Path STYLE = Path.of("src/main/resources/static/css/home.css");
    private static final Path SCRIPT = Path.of("src/main/resources/static/js/home-runtime.js");

    @Test
    void dashboardRouteActivatesTheApprovedHomeInsteadOfTheLegacyWorkspace() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/controller/DashboardController.java"));

        assertThat(controller).contains("@GetMapping(\"/dashboard\")", "return \"home\"")
                .doesNotContain("return \"dashboard\"");
        assertThat(Files.readString(HOME)).contains("data-figma-node=\"636:708\"");
    }

    @Test
    void homeLocksTheApprovedShellRatiosAndSingleWorkspace() throws Exception {
        String html = Files.readString(HOME);
        String css = Files.readString(STYLE);

        assertThat(html).contains(
                "class=\"home-rail\"", "机会资产 · 0", "id=\"homeAssetSearch\"",
                "data-position-plan-ratio=\"70:30\"", "持仓监控 · 基于已录入",
                "id=\"positionAggregate\"", "id=\"planContent\"",
                "GPT 综合判断", "Gemini 冲突复核", "Grok 反方挑战",
                "id=\"aiRolePanel\"", "id=\"conflictSummary\"", "查看完整审计链");
        assertThat(html).containsOnlyOnce("查看完整审计链")
                .doesNotContain("HOME COMPACT", ">FOUND<", ">NONE_FOUND<");
        assertThat(css).contains(
                "width: 64px", "height: 32px",
                "grid-template-columns: repeat(6, minmax(0, 1fr))",
                "grid-template-columns: minmax(0, 918fr) minmax(0, 394fr)",
                "grid-template-columns: minmax(0,72fr) minmax(0,28fr)",
                "min-height: 330px")
                .doesNotContain("sparkline", "mini-chart");
    }

    @Test
    void runtimeConsumesRealHomeContractsAndFailsClosed() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script).contains(
                "/api/dashboard/home?", "/api/asset-pool/search?query=",
                "filter(validOpportunity).slice(0, 6)",
                "has(asset && asset.opportunityScore)",
                "finalVisible ? label(plan.finalMarketBias", "finalVisible ? label(plan.finalPlanMode",
                "access.visible", "plan.finalPlan === true",
                "position.entryPrice", "position.openedAt", "trustedMonitor(position)",
                "position.monitorConclusion", "position.suggestedManualActionText",
                "position.entryLogicStatus", "position.reversalStatus", "position.riskReason",
                "contract.normalizeAiTabs", "GPT Candidate · 非 Final",
                "collectionStateLabel", "Candidate 摘要", "对 Candidate",
                "text(header.dataSourceText", "text(header.aiStatusLabel")
                .doesNotContain(
                        "AUTO_OPEN", "AUTO_CLOSE", "AUTO_REVERSE", "AUTO_ORDER",
                        "const assets = [", "BTCUSDT,ETHUSDT", "82, 87");
    }
}
