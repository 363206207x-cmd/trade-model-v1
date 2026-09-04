package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

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
                "previewAsset(analysisSelectedAsset.symbol)",
                "await previewAsset(result.dataset.analysisSymbol)",
                "ANALYSIS_POLL_TIMEOUT_MS = 300000", "/api/analysis/runs/",
                "window.history.pushState", "resumeAnalysis(activeAnalysisId)",
                "analysisSubmissionPromise", "updatePoolScanCta",
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
        assertThat(script).doesNotContain(
                "analysisSelectedAsset = item;\\n                    renderAnalysisSelection();\\n                    window.location.assign(\"/analysis/\"");
        assertThat(controller).contains(
                "@PostMapping(\"/defaults/top-up\")", "@PostMapping(\"/defaults/reset\")");
    }

    @Test
    void analysisPreviewRuntimeDeduplicatesSubmissionPollsAndRecoversAfterRefresh() throws Exception {
        String source = Files.readString(SCRIPT);
        String semantic = slice(source, "function semanticClass(value)", "function stateBadge(value)");
        String previewRuntime = slice(source, "function analysisFailureMessage(errorCode)",
                "function selectedBatchSymbols()");
        String resumeRuntime = slice(source, "async function resumeAnalysis(analysisId)",
                "function renderAnalysisSearchResults(items)");
        String nodeScript = """
                const assert = require('node:assert/strict');
                function fakeNode() {
                  return { textContent: '', innerHTML: '', hidden: false, disabled: false, className: '',
                    classList: { add() {}, remove() {}, toggle() {} } };
                }
                const nodes = Object.fromEntries([
                  'startAnalysisPreview', 'analysisMode', 'analysisModeBoundary', 'analysisDataQuality',
                  'analysisRoleContent', 'analysisTimeframesSection', 'analysisEvidenceSection',
                  'analysisScoresSection', 'analysisConflictSummary', 'analysisAiLayout'
                ].map(id => [id, fakeNode()]));
                var document = { getElementById: id => nodes[id] || null };
                var root = { dataset: {} };
                var window = {
                  location: { origin: 'https://example.test', pathname: '/analysis', search: '?returnTo=%%2Fdashboard' },
                  history: { pushes: [], pushState(state, title, target) { this.pushes.push(target); } },
                  setTimeout
                };
                function hasValue(value) { return value !== null && value !== undefined && value !== ''; }
                function text(value, fallback) { return hasValue(value) ? String(value) : (fallback || '当前不可查看'); }
                function escapeHtml(value) { return text(value, ''); }
                function formatTime(value) { return hasValue(value) ? String(value) : '当前不可查看'; }
                function factGrid(items) { return JSON.stringify(items); }
                function safeReturnTo(value, fallback) { return value || fallback; }
                function updateAnalysisRoleLabels() {}
                function announce() {}
                let analysisSelectedAsset = { symbol: 'BTCUSDT' };
                let analysisMode = null;
                let activeAnalysisId = '';
                let analysisSubmissionPromise = null;
                let analysisPollGeneration = 0;
                const ANALYSIS_POLL_INTERVAL_MS = 0;
                const ANALYSIS_POLL_TIMEOUT_MS = 1000;
                let postCount = 0;
                let getCount = 0;
                let loadCount = 0;
                let apiMode = 'submit';
                async function api(url, options) {
                  if (options && options.method === 'POST') {
                    postCount += 1;
                    return { analysisId: 'analysis-preview-1', traceId: 'trace-preview-1', status: 'QUEUED' };
                  }
                  getCount += 1;
                  if (apiMode === 'refresh' || getCount > 1) {
                    return { analysisId: url.split('/').pop(), traceId: 'trace-preview-1', status: 'SUCCESS' };
                  }
                  return { analysisId: 'analysis-preview-1', traceId: 'trace-preview-1', status: 'STARTED' };
                }
                async function loadAnalysis() { loadCount += 1; return true; }
                %s
                %s
                %s
                (async function () {
                  const [first, second] = await Promise.all([
                    previewAsset('BTCUSDT'), previewAsset('BTCUSDT')
                  ]);
                  assert.equal(postCount, 1);
                  assert.equal(first.analysisId, 'analysis-preview-1');
                  assert.equal(second.analysisId, 'analysis-preview-1');
                  assert.equal(activeAnalysisId, 'analysis-preview-1');
                  assert.deepEqual(window.history.pushes, ['/analysis/analysis-preview-1?returnTo=%%2Fdashboard']);
                  assert.equal(loadCount, 1);
                  apiMode = 'refresh';
                  activeAnalysisId = 'analysis-preview-1';
                  await resumeAnalysis(activeAnalysisId);
                  assert.equal(loadCount, 2);
                  assert.match(analysisFailureMessage('AUTHORITATIVE_OHLCV_UNAVAILABLE'), /市场数据/);
                  assert.match(analysisFailureMessage('PROVIDER_TIMEOUT'), /时限/);
                  console.log('ANALYSIS_PREVIEW_RUNTIME=PASS');
                })().catch(error => { console.error(error); process.exitCode = 1; });
                """.formatted(semantic, previewRuntime, resumeRuntime);

        Process process = new ProcessBuilder("node", "-e", nodeScript)
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(completed).as(output).isTrue();
        assertThat(process.exitValue()).as(output).isZero();
        assertThat(output).contains("ANALYSIS_PREVIEW_RUNTIME=PASS");
    }

    @Test
    void auditFollowupKeepsLoadingAndExistingAssetStatesTruthful() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script).contains(
                "let assetPoolLoaded = false",
                "assetPoolLoaded = true",
                "正在加载资产池",
                "const alreadyObserved = !previewMode && assetPoolItems.some",
                "data-existing-pool=\"",
                "alreadyObserved ? \"已观察\" : \"加入\"",
                "result.dataset.existingPool === \"true\"",
                "openPoolAssetDetail(result.dataset.searchSymbol, result)",
                "正在加载已有分析记录。",
                "function schedulerObservationText(readiness, header)",
                "[\"-\", \"—\", \"N/A\", \"NA\", \"NULL\"].includes(normalized)",
                "function lastScanResultText(header)",
                "updatePoolScanCta();\n        Promise.all([loadAssetPool(), loadTasks()])",
                "已完成（结果摘要未记录）")
                .doesNotContain(
                        "[\"调度器\", label(readiness?.schedulerObservationStatus || header.systemRuntimeState, \"当前不可查看\")]",
                        "[\"上次扫描结果\", humanReason(header.lastScanResult, \"尚无完成记录\")]"
                );
    }

    @Test
    void semanticStatusColorsUseTheSingleApprovedTokenSet() throws Exception {
        String homeScript = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String workspaceScript = Files.readString(SCRIPT);
        String homeCss = Files.readString(HOME_STYLE);
        String workspaceCss = Files.readString(STYLE);
        String tokens = "--semantic-strong-bullish: #166534;\n"
                + "    --semantic-bullish: #15803D;\n"
                + "    --semantic-neutral: #64748B;\n"
                + "    --semantic-bearish: #DC2626;\n"
                + "    --semantic-strong-bearish: #991B1B;\n"
                + "    --semantic-medium-risk: #B45309;\n"
                + "    --semantic-analyzing: #2563EB;";

        assertThat(homeCss).contains(tokens);
        assertThat(workspaceCss).contains(tokens);
        assertThat(homeScript + workspaceScript).contains(
                "semantic-strong-bullish", "semantic-bullish", "semantic-neutral",
                "semantic-bearish", "semantic-strong-bearish", "semantic-medium-risk",
                "semantic-analyzing");
        assertThat(homeScript).contains(
                "semanticClass(asset.marketBias)", "semanticClass(asset.riskLevel)",
                "applySemanticClass(\"statusSystem\"");
        assertThat(homeCss + workspaceCss).doesNotContain(
                ".opportunity-card.semantic-bullish", ".surface.semantic-bearish");
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
                "资产池是机会发现的唯一入口");
        assertThat(html).contains(
                "Telegram 通知", "发送通道测试", "通道测试、非交易指令")
                .doesNotContain("botToken", "chatId");
    }

    private static String slice(String value, String start, String end) {
        int startIndex = value.indexOf(start);
        int endIndex = value.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return value.substring(startIndex, endIndex);
    }
}
