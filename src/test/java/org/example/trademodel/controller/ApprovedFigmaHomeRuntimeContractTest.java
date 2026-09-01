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
                "class=\"home-rail\"", "<h2 id=\"opportunityHeading\">资产</h2>", "id=\"homeAssetSearch\"",
                "data-position-plan-ratio=\"70:30\"", "<h2 id=\"positionHeading\">持仓监控</h2>",
                "<h2 id=\"planHeading\">执行计划</h2>", "<h2 id=\"aiWorkspaceHeading\">AI 分析</h2>",
                "id=\"positionAggregate\"", "id=\"planContent\"",
                "data-ai-role=\"GPT_FINAL\" aria-label=\"GPT 综合判断\">GPT</button>",
                "data-ai-role=\"GEMINI_REVIEW\" aria-label=\"Gemini 冲突复核\">Gemini</button>",
                "data-ai-role=\"GROK_CHALLENGE\" aria-label=\"Grok 反方挑战\">Grok</button>",
                "id=\"aiRolePanel\"", "id=\"conflictSummary\"", "id=\"auditChainLink\"");
        assertThat(Files.readString(SCRIPT)).contains(
                        "查看完整审计链", "查看分析详情", "审计链尚未形成")
                .containsOnlyOnce("查看完整审计链");
        assertThat(html)
                .doesNotContain(
                        "HOME COMPACT", ">FOUND<", ">NONE_FOUND<", "Final Execution Plan");
        assertThat(css).contains(
                "width: 64px", "height: 32px",
                "grid-template-columns: repeat(6, minmax(0, 1fr))",
                "grid-template-columns: minmax(0, 7fr) minmax(320px, 3fr)",
                "grid-template-columns: minmax(0,72fr) minmax(0,28fr)",
                "min-height: 400px")
                .doesNotContain("sparkline", "mini-chart");
    }

    @Test
    void opportunityCardsKeepFourVisibleRowsInsideTheFrozenHeightWithoutClipping() throws Exception {
        String css = Files.readString(STYLE);
        String cardRule = css.substring(css.indexOf(".opportunity-card {"),
                css.indexOf(".opportunity-card:hover"));

        assertThat(cardRule).contains("height: 120px", "padding: 8px 10px")
                .doesNotContain("overflow: hidden");
        assertThat(css).contains(
                ".opportunity-final { display: grid; grid-template-columns: 1fr;",
                ".opportunity-metrics { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr));",
                ".opportunity-context { min-width: 0; display: flex;",
                ".opportunity-context span { min-width: 0; white-space: nowrap;");
    }

    @Test
    void dashboardDeclaresARepositoryLocalTrineLogicFavicon() throws Exception {
        String html = Files.readString(HOME);
        Path favicon = Path.of("src/main/resources/static/favicon.svg");

        assertThat(html).contains("rel=\"icon\"", "href=\"/favicon.svg\"");
        assertThat(favicon).exists();
        assertThat(Files.readString(favicon)).contains("aria-label=\"TRINE LOGIC\"");
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
                "function validOpportunityCard(asset)", "function validObservationCard(asset)",
                "validOpportunityCard(asset) || validObservationCard(asset)", ".slice(0, 6)",
                "asset.hasFinal === true", "has(asset.finalMarketBias)",
                "has(asset.confidenceLevel)", "has(asset.riskLevel)",
                "String(asset && asset.slotType || \"\").toUpperCase() === \"OBSERVATION\"",
                "asset.oneHourOpportunityLabel", "asset.fourHourTrendLabel",
                "access.visible", "plan.finalPlan === true",
                "position.entryPrice", "position.openedAt", "trustedMonitor(position)",
                "position.monitorConclusion", "position.suggestedManualActionText",
                "position.entryLogicStatus", "position.reversalStatus", "position.riskTrend",
                "contract.normalizeAiTabs", "GPT 综合判断 · 非最终计划",
                "方向判断", "机会进度", "候选参与方式", "一句话结论",
                "Gemini 冲突复核", "复核结果", "Grok 反方挑战", "失败路径",
                "completeFailurePath", "failurePathStateView", "来源不可用",
                "plan.stopZone || plan.stopLoss", "止损", "失效条件",
                "collectionStateLabel", "形成原因", "支持证据", "反对证据",
                "has(header.updatedAt)", "clockTime(header.updatedAt)")
                .doesNotContain(
                        "candidate.summary, why", "[].concat(role.evidenceGaps",
                        "role.evidenceGapsState || role.logicConflictsState", "label(role.planModeImpact",
                        "renderDerivatives", "衍生品实况 · 数据时间独立标注",
                        "维持 Candidate", "GPT Candidate · 非 Final", "Candidate Mode",
                        "AUTO_OPEN", "AUTO_CLOSE", "AUTO_REVERSE", "AUTO_ORDER",
                        "const assets = [", "BTCUSDT,ETHUSDT", "82, 87");
    }

    @Test
    void unknownEnumsRemainNeutralWhileRoleAndCollectionStatesStayExplicit() throws Exception {
        String contract = Files.readString(Path.of(
                "src/main/resources/static/js/frontend-contract.js"));
        String script = Files.readString(SCRIPT);

        assertThat(contract).contains(
                "if (/^[A-Z][A-Z0-9_]*$/.test(text.trim())) return \"—\";",
                "ROLE_STATE_VIEWS.UNAVAILABLE",
                "COLLECTION_STATE_VIEWS.SOURCE_UNAVAILABLE");
        assertThat(script).contains(
                "return /^[A-Z][A-Z0-9_]*$/.test(raw) ? \"—\" : raw;",
                "statusValue(state.serviceAvailability, \"—\")");
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
    void homeProjectionSeparatesOpportunityAndObservationCardsWithoutPreviewOrPoolInference() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script).contains(
                "var assets = all.filter(function (asset)",
                "validOpportunityCard(asset) || validObservationCard(asset)",
                "String(asset.slotType || \"\").toUpperCase() === \"OBSERVATION\"",
                ".slice(0, 6)",
                "setText(\"opportunityHeading\", \"资产\")",
                "has(asset && (asset.opportunityId || asset.primaryOpportunityId))",
                "has(asset && asset.analysisId)",
                "asset.hasFinal === true", "has(asset.finalMarketBias)",
                "[\"CONFIRMATION\", \"REDUCED\", \"PREPARATION\"]",
                "function observationCard(asset, selected)",
                "function opportunityDataNotice(asset)", "周期冲突", "数据过期",
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
                "aria-label=\"持仓视图\"", "活动持仓", "历史持仓",
                "data-review-filter=\"all\"", "data-review-filter=\"position\"", "data-review-filter=\"opportunity\"",
                "id=\"saveSettings\" hidden disabled")
                .doesNotContain("历史 / 复盘");
        assertThat(script).contains(
                "function bindReviews()", "data-review-kind", "aria-selected",
                "role=\"button\"", "event.key === \"Enter\"", "event.key === \" \"",
                "form?.addEventListener(\"change\"", "initialSettings",
                "const dirty = JSON.stringify(formJson(form)) !== form.dataset.initialSettings",
                "save.hidden = !dirty",
                "save.classList.toggle(\"is-dirty\", dirty)");
    }
}
