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
    private static final Path WORKSPACE = Path.of("src/main/resources/templates/workspace.html");
    private static final Path WORKSPACE_SCRIPT = Path.of("src/main/resources/static/js/workspace.js");

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
                "data-position-plan-ratio=\"70:30\"", ">持仓监控</h2>", ">执行计划</h2>", ">AI 分析</h2>",
                "id=\"positionAggregate\"", "id=\"planContent\"",
                "GPT 综合判断", "Gemini 冲突复核", "Grok 反方挑战",
                "id=\"aiRolePanel\"", "id=\"conflictSummary\"", "查看完整审计链");
        assertThat(html).containsOnlyOnce("查看完整审计链")
                .doesNotContain(
                        "HOME COMPACT", ">FOUND<", ">NONE_FOUND<",
                        "持仓监控 · 基于已录入", ">最终执行计划</h2>",
                        ">Final Execution Plan</h2>", ">AI 分析工作区</h2>");
        assertThat(css).contains(
                "width: 64px", "height: 32px",
                "grid-template-columns: repeat(6, minmax(0, 1fr))",
                "grid-template-columns: minmax(0, 918fr) minmax(0, 394fr)",
                "grid-template-columns: minmax(0,72fr) minmax(0,28fr)",
                "min-height: 330px")
                .doesNotContain("sparkline", "mini-chart");
    }

    @Test
    void homeAlertSummaryMapsTechnicalStatesToUserFacingCopy() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script).contains(
                "function userFacingAlertMessage(value)",
                "高风险决策", "数据质量不足", "收敛破裂：冲突升高且多周期弱收敛",
                "开仓被冲突阻断：冲突升高", "多模型冲突升高", "多周期收敛弱",
                "ERROR: \"读取失败\"", "WARN: \"需关注\"", "NOT_CALLED: \"尚未调用\"",
                "REGION_RESTRICTED: \"当前区域不可用\"", "PARTIAL: \"数据不完整\"",
                "userFacingAlertMessage(alert.message)")
                .doesNotContain("querySelector(\"strong\").textContent = text(alert.message");
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

    @Test
    void homeAssetQuickSearchLocksCompactCopyAndSelectionStateContract() throws Exception {
        String html = Files.readString(HOME);
        String script = Files.readString(SCRIPT);

        assertThat(html).contains(
                "data-overlay-code=\"O02\"", "id=\"homeAssetSearchResults\"",
                "id=\"homeAssetSearchSelection\"", "尚未选择资产",
                "id=\"homePreviewAsset\" type=\"button\" disabled>分析</button>",
                "id=\"homeAddAsset\" type=\"button\" disabled>添加</button>")
                .doesNotContain("按需分析", "加入观察资产池", "已在观察资产池");
        assertThat(script).contains(
                "selectedSearchAsset = null", "aria-selected", "ArrowDown", "ArrowUp", "Enter", "Escape",
                "selectSearchResult", "loadAssetPoolMembership",
                "previewButton.textContent = \"分析\"",
                "addButton.textContent = inPool ? \"已添加\" : \"添加\"",
                "previewButton.disabled = true", "addButton.disabled = true",
                "previewButton.disabled = searchActionBusy",
                "addButton.disabled = searchActionBusy || inPool",
                "await loadAssetPoolMembership()",
                "POST", "/api/asset-pool", "/analysis-preview?timeframe=5m",
                "window.location.assign(\"/analysis/\"")
                .doesNotContain(
                        "按需分析", "加入观察资产池", "已在观察资产池",
                        "window.location.href = \"/analysis?asset=\"");
    }

    @Test
    void homeOpportunityProjectionRemainsIndependentFromPoolMembershipAndPreview() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script).contains(
                "var assets = all.filter(validOpportunity).slice(0, 6)",
                "setText(\"opportunityHeading\", \"机会资产 · \" + assets.length)",
                "has(asset && (asset.opportunityId || asset.primaryOpportunityId))",
                "has(asset && asset.analysisId)",
                "has(asset && asset.opportunityScore)",
                "await api(\"/api/asset-pool\", { method: \"POST\"",
                "await api(\"/api/asset-pool/search/\"",
                "window.location.assign(\"/analysis/\"")
                .doesNotContain(
                        "assetPoolSymbols.size", "assetPoolCount + assets.length",
                        "selectedSearchAsset.opportunityScore", "selectedSearchAsset.opportunityId");
    }

    @Test
    void assetPoolBatchManagementExecutesItsSourceDefinedActions() throws Exception {
        String html = Files.readString(WORKSPACE);
        String script = Files.readString(WORKSPACE_SCRIPT);

        assertThat(html).contains(
                "data-overlay-code=\"O04\"", "id=\"poolBatchList\"",
                "id=\"poolBatchStatus\"", "id=\"batchScanSelected\"", "扫描所选",
                "id=\"batchRemoveSelected\"", "移除所选");
        assertThat(script).contains(
                "selectedBatchSymbols", "updateBatchActions",
                "/api/asset-pool/batch-scan", "/api/asset-pool/batch-remove",
                "正在扫描", "扫描完成", "正在移除", "历史记录会保留");
    }

    @Test
    void visibleWorkspaceControlsHaveTheirSourceDefinedInteractionWiring() throws Exception {
        String html = Files.readString(WORKSPACE);
        String script = Files.readString(WORKSPACE_SCRIPT);

        assertThat(html).contains(
                "aria-label=\"持仓视图\"", "活动持仓", "href=\"/reviews\"",
                "data-review-filter=\"all\"", "data-review-filter=\"position\"", "data-review-filter=\"opportunity\"",
                "id=\"saveSettings\" disabled");
        assertThat(script).contains(
                "function bindReviews()", "data-review-kind", "aria-selected",
                "role=\"button\"", "event.key === \"Enter\"", "event.key === \" \"",
                "form?.addEventListener(\"change\"", "initialSettings",
                "JSON.stringify(formJson(form)) === form.dataset.initialSettings");
    }
}
