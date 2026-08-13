package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class AnalysisDetailFrontendContractTest {
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/analysis-detail.html");
    private static final Path SCRIPT = Path.of("src/main/resources/static/js/analysis-detail.js");
    private static final Path STYLES = Path.of("src/main/resources/static/css/analysis-detail.css");
    private static final Path ASSET_TEMPLATE = Path.of("src/main/resources/templates/asset-detail.html");
    private static final Path ASSET_SCRIPT = Path.of("src/main/resources/static/js/asset-detail.js");

    @Test
    void pageMatchesTheFrozenDecisionChainDetailContract() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("分析详情")
                .contains("data-analysis-back")
                .contains("class=\"visually-hidden\" data-page-status")
                .contains("市场判断")
                .contains("证据与评分")
                .contains("多周期摘要")
                .contains("AI 分析状态")
                .contains("决策责任链")
                .contains("Conflict Resolver")
                .contains("Rule Validation")
                .contains("Final Plan 来源链")
                .contains("当前权威分析")
                .contains("仅展示当前 analysisId 实际持久化证据")
                .contains("八项评分逐项绑定；缺失不是 0")
                .doesNotContain("class=\"sync-status\"")
                .doesNotContain("class=\"run-meta\"")
                .doesNotContain("data-analysis-field=\"analysisTime\"");
    }

    @Test
    void exactAnalysisReadAggregatesExistingReviewAndAuditApisWithoutIdentityFallbacks() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(count(script, "fetch(")).isEqualTo(2);
        assertThat(script)
                .contains("\"/api/review/aggregate/\" + encodeURIComponent(analysisId)")
                .contains("\"/api/ai/audit-chain?analysisId=\" + encodeURIComponent(analysisId)")
                .contains("var responses = await Promise.all([")
                .contains("String(run.analysisId || \"\") !== requestedAnalysisId")
                .contains("normalizeSymbol(run.symbol) !== requestedSymbol")
                .contains("\"ANALYSIS_ID_MISMATCH\"")
                .contains("\"SYMBOL_MISMATCH\"")
                .contains("function updateBackLink()")
                .contains("\"/dashboard/asset-detail?\" + query.toString()")
                .contains("mobileView ? \"/dashboard/mobile\" : \"/dashboard\"")
                .contains("不会回退到同资产的其他分析")
                .doesNotContain("/api/dashboard/home")
                .doesNotContain("/api/dashboard/detail")
                .doesNotContain("/api/evidence")
                .doesNotContain("/api/score")
                .doesNotContain("latestBySymbol")
                .doesNotContain("localStorage");
    }

    @Test
    void allRequiredFailClosedStatesAreExplicitAndRetryKeepsTheSameIdentity() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("data-page-state hidden")
                .contains("data-request-retry hidden")
                .contains("data-fail-state=\"AI_TRACE_UNAVAILABLE\"")
                .contains("data-fail-state=\"MULTI_TIMEFRAME_UNAVAILABLE\"")
                .contains("data-partial-notice hidden");
        assertThat(script)
                .contains("\"ANALYSIS_NOT_FOUND\"")
                .contains("\"LOAD_FAILED\"")
                .contains("\"PARTIAL_DATA\"")
                .contains("document.querySelector(\".page-state[data-page-state]\")")
                .doesNotContain("document.querySelector(\"[data-page-state]\")")
                .contains("response.status === 404")
                .contains("showPageState(")
                .contains("retry.addEventListener(\"click\", loadAnalysisDetail)")
                .contains("可重试读取同一 analysisId")
                .contains("本次分析没有可验证的完整结果")
                .contains("分析状态不可验证")
                .contains("runStatus !== \"SUCCESS\" && runStatus !== \"STARTED\"");
    }

    @Test
    void concurrentReadsAreGuardedAcrossSuccessFailureRetryAndLoadingCleanup() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .contains("var requestGeneration = 0")
                .contains("var activeRequestController = null")
                .contains("requestGeneration += 1")
                .contains("activeRequestController.abort()")
                .contains("generation: requestGeneration")
                .contains("request.generation === requestGeneration")
                .contains("request.analysisId === currentAnalysisId()")
                .contains("document.querySelector(\"[data-analysis-detail-root]\") === root")
                .contains("request.controller === activeRequestController")
                .contains("if (!isCurrentRequest(request)) return")
                .contains("if (!isCurrentRequest(request) || isAbortError(error)) return")
                .contains("if (isCurrentRequest(request)) {")
                .contains("retry.addEventListener(\"click\", loadAnalysisDetail)")
                .contains("requestOptions.signal = request.controller.signal");
    }

    @Test
    void inProgressAnalysisDoesNotExposeUnfinalizedDownstreamData() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .contains("if (runStatus === \"STARTED\")")
                .contains("renderContext(run, null)")
                .contains("renderMarketJudgment(null, null)")
                .contains("renderEvidence([])")
                .contains("renderScores([])")
                .contains("renderTimeframes(null, {})")
                .contains("renderAiStatus(audit || {}, roleContract(audit || {}))")
                .contains("renderAuditChain(audit || {})")
                .contains("renderFinalSource(audit || {})")
                .contains("setPageStatus(\"分析处理中\", \"partial\")");
    }

    @Test
    void evidenceAndScoresRenderAllPersistedValuesWithoutClientCompletion() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("data-evidence-list")
                .contains("data-score-type=\"趋势结构分\"")
                .contains("data-score-type=\"资金推动分\"")
                .contains("data-score-type=\"杠杆风险分\"")
                .contains("data-score-type=\"流动性质量分\"")
                .contains("data-score-type=\"情绪温度分\"")
                .contains("data-score-type=\"事件冲击分\"")
                .contains("data-score-type=\"宏观环境分\"")
                .contains("data-score-type=\"综合可信度分\"")
                .contains("支持与反对证据均保留来源、观测时间和新鲜度")
                .contains("评分不可用；不合成、不平均、不用默认值补齐");
        assertThat(script)
                .contains("function aggregateConfidenceScore(items)")
                .contains("String(item.scoreType || \"\").trim() === \"综合可信度分\"")
                .contains("item.scoreValue !== null && item.scoreValue !== undefined")
                .contains("contract.displayNumber(aggregateScore) + \" / \"")
                .contains("appendDefinition(fields, \"证据强度\", contract.displayNumber(item.strength))")
                .contains("appendDefinition(fields, \"置信度\", contract.displayNumber(item.confidence))")
                .contains("appendDefinition(fields, \"观测时间\", displayText(item.observedAt, \"当前不可查看\"))")
                .contains("appendDefinition(fields, \"Freshness\", displayText(item.freshness, \"当前不可查看\"))")
                .contains("缺失维度不是 0，不补齐")
                .doesNotContain(".slice(0, 3)")
                .doesNotContain(".reduce(")
                .doesNotContain("evidence.direction === decision");
    }

    @Test
    void timeframeAndThreeAiUseStructuredRoleAndCollectionStateContracts() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("data-timeframe=\"4H\">待同步")
                .contains("data-timeframe=\"1H\">待同步")
                .contains("data-timeframe=\"15M\">待同步")
                .contains("data-timeframe=\"5M\">待同步")
                .contains("5M 仅作为短期风险过滤，不作为主趋势方向")
                .contains("证据综合与候选形成 <small>GPT_FINAL</small>")
                .contains("证据与风险复核 <small>GEMINI_REVIEW</small>")
                .contains("失败路径与压力测试 <small>GROK_CHALLENGE</small>")
                .contains("不从其他 analysis 或当前首页补全")
                .contains("不生成独立交易方向")
                .contains("不补写外部事件或技术证据");
        assertThat(count(html, "data-role-tab=")).isEqualTo(3);
        assertThat(script)
                .contains("var ROLE_ORDER = [\"GPT_FINAL\", \"GEMINI_REVIEW\", \"GROK_CHALLENGE\"]")
                .contains("var encoded = decision && decision.aiRoleResults")
                .contains("role.roleState = contract.normalizeRoleState")
                .contains("renderStructuredCollection(content, \"支持证据\", role.supportingEvidenceState")
                .contains("renderStructuredCollection(content, \"Evidence Gaps\", role.evidenceGapsState")
                .contains("renderStructuredCollection(content, \"Failure Paths\", role.failurePathState")
                .contains("集合为空；以集合状态区分未发现、数据不足、来源不可用或过期")
                .contains("ArrowLeft")
                .contains("ArrowRight")
                .doesNotContain("winner")
                .doesNotContain("vote");
    }

    @Test
    void analysisDetailShowsValidatedFinalSourceWithoutPositionOrTradingCapability() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("Final Plan 来源链")
                .doesNotContain("User Position")
                .doesNotContain("Position Monitoring")
                .doesNotContain("买入")
                .doesNotContain("卖出")
                .doesNotContain("下单")
                .doesNotContain("自动交易");
        assertThat(script)
                .doesNotContain("executionSuggestion")
                .doesNotContain("userPosition")
                .doesNotContain("positionMonitor")
                .contains("plan.finalPlan === true")
                .contains("validation.status === \"PASS\"")
                .contains("plan.notTradeInstruction === true")
                .doesNotContain("trade");
    }

    @Test
    void responsiveStylesPreserveFigmaDimensionsAndAccessibility() throws Exception {
        String html = Files.readString(TEMPLATE);
        String css = Files.readString(STYLES);

        assertThat(css)
                .contains("width: min(100%, 430px)")
                .contains("min-height: 44px")
                .contains("min-height: 48px")
                .contains("overflow-x: hidden")
                .contains("env(safe-area-inset-top)")
                .contains("env(safe-area-inset-bottom)")
                .contains("@media (min-width: 760px)")
                .contains("@media (prefers-color-scheme: dark)")
                .contains("@media (prefers-reduced-motion: reduce)")
                .contains("outline: 3px solid var(--focus-ring)")
                .contains("--analysis-root-font-size: 16px")
                .contains(":root[data-mobile-text-size=\"large\"]")
                .contains(":root[data-mobile-text-size=\"extra-large\"]")
                .contains(":root[data-mobile-text-size=\"accessibility\"]")
                .contains("--analysis-root-font-size: 20.8px")
                .contains(".title-stack h1 {\n  font-size: 1.25rem")
                .contains(".role-tabs button {\n  min-width: 0")
                .contains("font-size: 0.625rem")
                .contains("-webkit-text-size-adjust: 100%")
                .doesNotContain("font-size: clamp")
                .doesNotContain("transform: scale(")
                .doesNotContain("letter-spacing: -")
                .doesNotContain("url(http")
                .doesNotContain("cdn");
        assertThat(html)
                .doesNotContain("style=\"height:")
                .doesNotContain("class=\"run-meta\"");
    }

    @Test
    void mobileFirstViewportKeepsFrozenNavigationAndAssetHierarchy() throws Exception {
        String html = Files.readString(TEMPLATE);
        String css = Files.readString(STYLES);

        assertThat(html)
                .contains("class=\"visually-hidden\" data-page-status")
                .doesNotContain("class=\"sync-status\"")
                .doesNotContain("class=\"run-meta\"");
        assertThat(css)
                .contains(".analysis-navigation {")
                .contains("min-height: 48px")
                .contains("margin-bottom: 20px")
                .contains(".asset-context {\n  display: flex;\n  min-height: 172px")
                .doesNotContain(".sync-status")
                .doesNotContain(".run-meta");
    }

    @Test
    void assetDetailExposesNavigationOnlyForBackendProvidedAnalysisIdentity() throws Exception {
        String html = Files.readString(ASSET_TEMPLATE);
        String script = Files.readString(ASSET_SCRIPT);

        assertThat(html)
                .contains("data-analysis-detail-link")
                .contains("查看分析详情")
                .contains("hidden");
        assertThat(script)
                .contains("function updateAnalysisDetailLink(asset)")
                .contains("contract.hasText(asset.analysisId)")
                .contains("analysisId: analysisId")
                .contains("selectedSymbol: symbol")
                .contains("link.href = \"/dashboard/analysis-detail?\"")
                .contains("link.hidden = true")
                .contains("link.removeAttribute(\"href\")")
                .doesNotContain("executionSuggestion.sourceAnalysisId")
                .doesNotContain("latestAnalysisId");
    }

    private int count(String source, String target) {
        return (source.length() - source.replace(target, "").length()) / target.length();
    }
}
