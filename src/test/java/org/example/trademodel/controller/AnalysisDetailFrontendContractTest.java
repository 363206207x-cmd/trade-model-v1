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
    void pageMatchesTheFrozenFourModuleFigmaBaseline() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("分析详情")
                .contains("data-analysis-back")
                .contains("市场判断")
                .contains("证据与评分")
                .contains("多周期摘要")
                .contains("AI 分析状态")
                .contains("当前权威分析")
                .contains("只展示当前分析实际返回的 Top 3")
                .contains("不创建“查看全部”假入口");
    }

    @Test
    void exactAnalysisReadUsesOneExistingApiAndRejectsIdentityFallbacks() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(count(script, "fetch(")).isEqualTo(1);
        assertThat(script)
                .contains("\"/api/review/aggregate/\" + encodeURIComponent(analysisId)")
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
                .doesNotContain("/api/ai")
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
    void inProgressAnalysisDoesNotExposeUnfinalizedDownstreamData() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .contains("if (runStatus === \"STARTED\")")
                .contains("renderContext(run, null)")
                .contains("renderMarketJudgment(null, null)")
                .contains("renderEvidence([])")
                .contains("renderScores([])")
                .contains("renderTimeframes(null)")
                .contains("renderAiStatus(null)")
                .contains("setPageStatus(\"分析处理中\", \"partial\")");
    }

    @Test
    void evidenceAndScoresRenderOnlyReturnedTopThreeWithoutClientCompletion() throws Exception {
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
                .contains("完整证据、支持/反对关系、强度与置信度尚未提供")
                .contains("评分不可用；不合成、不平均、不排序");
        assertThat(script)
                .contains("}).slice(0, 3)")
                .contains("证据强度\", \"未提供\"")
                .contains("置信度\", \"未提供\"")
                .contains("缺失维度不是 0，不补齐八项")
                .doesNotContain(".reduce(")
                .doesNotContain("evidence.direction === decision")
                .doesNotContain("supportingEvidence")
                .doesNotContain("opposingEvidence");
    }

    @Test
    void timeframeAndAiRemainBoundedToCurrentApiCoverage() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("data-timeframe=\"4H\">待同步")
                .contains("data-timeframe=\"1H\">待同步")
                .contains("data-timeframe=\"15M\">待同步")
                .contains("data-timeframe=\"5M\">待同步")
                .contains("5M 仅作为短期风险过滤，不作为主趋势方向")
                .contains("GPT_FINAL / 最终裁决官")
                .contains("GEMINI_REVIEW / 冲突复核官")
                .contains("GROK_CHALLENGE / 反方挑战官")
                .contains("不从其他 analysis 或当前首页补全")
                .contains("不生成独立交易方向")
                .contains("不补写外部事件或技术证据");
        assertThat(count(html, "data-role-tab=")).isEqualTo(3);
        assertThat(script)
                .contains("var ROLE_ORDER = [\"GPT_FINAL\", \"GEMINI_REVIEW\", \"GROK_CHALLENGE\"]")
                .contains("decision && decision.multiTfConvergence")
                .contains("当前分析角色结果不可用")
                .contains("ArrowLeft")
                .contains("ArrowRight")
                .doesNotContain("aiRoleResults")
                .doesNotContain("winner")
                .doesNotContain("vote");
    }

    @Test
    void analysisDetailHasNoExecutionPositionOrTradingCapability() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .doesNotContain("Execution Plan")
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
                .doesNotContain("trade");
    }

    @Test
    void responsiveStylesPreserveFigmaDimensionsAndAccessibility() throws Exception {
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
                .doesNotContain("font-size: clamp")
                .doesNotContain("letter-spacing: -")
                .doesNotContain("url(http")
                .doesNotContain("cdn");
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
