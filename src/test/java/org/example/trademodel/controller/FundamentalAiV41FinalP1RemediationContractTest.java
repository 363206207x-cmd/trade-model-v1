package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class FundamentalAiV41FinalP1RemediationContractTest {
    private static final Path WORKSPACE = Path.of("src/main/resources/templates/workspace.html");
    private static final Path SCRIPT = Path.of("src/main/resources/static/js/workspace.js");
    private static final Path STYLE = Path.of("src/main/resources/static/css/workspace.css");
    private static final Path HOME = Path.of("src/main/resources/templates/home.html");
    private static final Path HOME_STYLE = Path.of("src/main/resources/static/css/home.css");

    @Test
    void dashboardProductionRouteUsesTheApprovedHomeVisualSystem() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/controller/DashboardController.java"));
        assertThat(controller).contains(
                "model.addAttribute(\"pageKey\", \"home\")",
                "model.addAttribute(\"activeNavigation\", \"home\")",
                "return \"home\"")
                .doesNotContain("return \"dashboard\"");
    }

    @Test
    void canonicalHomeLocksSixSegmentsTopSixRatiosAndOneVisibleRole() throws Exception {
        String html = Files.readString(HOME);
        String css = Files.readString(HOME_STYLE);
        assertThat(html).contains(
                "data-page-key=\"home\"", "data-position-plan-ratio=\"70:30\"",
                "statusEnvironment", "statusSystem", "statusData", "statusService",
                "statusAccount", "statusReset", "opportunityGrid",
                "positionList", "planContent", "aiRolePanel", "conflictSummary");
        assertThat(css).contains(
                "grid-template-columns: repeat(6, minmax(0, 1fr))",
                "grid-template-columns: repeat(3, minmax(0, 1fr))",
                "grid-template-columns: minmax(0, 7fr) minmax(320px, 3fr)",
                "grid-template-columns: minmax(0,22fr) minmax(0,28fr) minmax(0,28fr) minmax(0,22fr)",
                "@container (max-width: 1239px)", "height: 120px")
                .doesNotContain("mini-chart", "sparkline");
        assertThat(html).containsOnlyOnce("id=\"aiRolePanel\"")
                .doesNotContain("60:40", "pageKey == 'home'");
    }

    @Test
    void analysisAndAssetPoolExposeTheFrozenPrimaryInteractions() throws Exception {
        String html = Files.readString(WORKSPACE);
        String script = Files.readString(SCRIPT);
        String controller = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/controller/AssetPoolController.java"));
        assertThat(html).contains(
                "id=\"analysisAssetSearch\"", "id=\"analysisSelectedAsset\"",
                "id=\"startAnalysisPreview\" disabled", "id=\"addAnalysisAsset\"",
                "GPT 综合判断", "Gemini 冲突复核", "Grok 反方挑战",
                "id=\"topUpDefaultAssets\"", "id=\"resetDefaultAssets\"", "id=\"scanAssetPool\"");
        assertThat(script).contains(
                "/api/asset-pool/search?query=", "/analysis-preview?timeframe=5m",
                "previewAsset(analysisSelectedAsset.symbol)", "updatePoolScanCta",
                "/api/asset-pool/defaults/top-up", "/api/asset-pool/defaults/reset",
                "renderAnalysisScores(analysisAudit.scores || [])",
                "renderAnalysisEvidence(analysisAudit.evidence || [])",
                "items.slice(0, 8)", "items.slice(0, 20)",
                "data-analysis-score-item", "data-analysis-evidence-item",
                "NOT_CALLED_INPUT_GATE: \"未调用（输入门禁）\"",
                "analysisEvidenceDescription(item?.description)",
                "label(trace?.model, text(trace?.model, \"模型未记录\"))",
                "GPT_FINAL: \"GPT 综合判断\"",
                "GEMINI_REVIEW: \"Gemini 冲突复核\"",
                "GROK_CHALLENGE: \"Grok 反方挑战\"",
                "[\"QUEUED\", \"RUNNING\"].includes(task.state)",
                "[\"FAILED\", \"PARTIAL\"].includes(task.state)",
                "scan.textContent = \"重新扫描\"",
                "const scanButton = event.currentTarget",
                "finally { scanButton.disabled = false; }");
        assertThat(controller).contains(
                "@PostMapping(\"/defaults/top-up\")", "@PostMapping(\"/defaults/reset\")");
    }

    @Test
    void primaryProductCopyUsesTheSharedSemanticMapperAndRemovesInternalContractNarration() throws Exception {
        String html = Files.readString(WORKSPACE);
        String script = Files.readString(SCRIPT);
        String contract = Files.readString(Path.of(
                "src/main/resources/static/js/frontend-contract.js"));
        assertThat(html).contains("/js/frontend-contract.js");
        assertThat(script).contains(
                "USER_FACING_SEMANTIC_MAPPER", "roleLabel(role, analysisMode)",
                "userFacingSemantic.field", "fieldLabel(entry[0])",
                "label(item?.scoreType, \"评分项\")",
                "label(item?.evidenceType, \"证据\")",
                "GPT_FINAL: \"GPT 综合判断\"",
                "GEMINI_REVIEW: \"Gemini 冲突复核\"",
                "GROK_CHALLENGE: \"Grok 反方挑战\"");
        assertThat(contract).contains(
                "var USER_FACING_FIELD_LABELS", "function userFacingField(value)",
                "field: userFacingField", "finalMarketBias: \"最终市场方向\"",
                "planModeAfter: \"调整后计划模式\"", "failurePaths: \"失败路径\"");
        assertThat(html).doesNotContain(
                ">最终裁决<", ">冲突复核<", ">反方挑战<",
                "Preview 不创建机会", "计划不会自动变成持仓",
                "仅展示通过 Rule Validation", "复核不会产生交易授权",
                "资产池是机会发现的唯一入口", "Telegram", "telegram");
    }
}
