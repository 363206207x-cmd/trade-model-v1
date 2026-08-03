package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class DashboardMobileProjectionContractTest {
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/dashboard-mobile.html");
    private static final Path SCRIPT = Path.of("src/main/resources/static/js/dashboard-mobile.js");
    private static final Path FRONTEND_CONTRACT =
            Path.of("src/main/resources/static/js/frontend-contract.js");
    private static final Path STYLES = Path.of("src/main/resources/static/css/dashboard-mobile.css");
    private static final Path MOBILE_CONTROLLER =
            Path.of("src/main/java/org/example/trademodel/controller/MobileDashboardController.java");

    @Test
    void mobileTemplateProjectsOnlyConfirmedBackendSemantics() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(count(html, "class=\"status-cell")).isEqualTo(8);
        assertThat(html)
                .contains("home.systemState.marketTrend.valueLabel")
                .contains("home.systemState.riskLevel.valueLabel")
                .contains("home.systemState.dataQuality.valueLabel")
                .contains("home.systemState.aiConflict.valueLabel")
                .contains("home.header.aiStatusLabel")
                .contains("home.systemState.pendingReview.valueLabel")
                .contains("home.systemState.confused.valueLabel")
                .contains("home.systemState.hotReset.valueLabel")
                .doesNotContain("holdingRisk")
                .doesNotContain("<canvas")
                .doesNotContain("<svg")
                .doesNotContain("KPI")
                .doesNotContain("STATIC_LAYOUT_FIXTURE")
                .doesNotContain("{asset[")
                .doesNotContain("{positions[")
                .doesNotContain("{aiDecision.tabs[")
                .doesNotContain("{marketTrend}")
                .doesNotContain("{riskLevel}");
    }

    @Test
    void browserVisibleFallbackContainsNoTemplateTokens() throws Exception {
        String html = Files.readString(TEMPLATE);
        String browserVisibleFallback = html.replaceAll("\\s+th:[\\w-]+=\"[^\"]*\"", "");

        assertThat(browserVisibleFallback)
                .doesNotContain("{")
                .doesNotContain("}")
                .doesNotContain("STATIC_LAYOUT_FIXTURE");
    }

    @Test
    void mobileTemplateKeepsAssetExecutionPositionAndAiOwnershipSeparate() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("th:each=\"asset, stat : ${mobileAssets}\"")
                .doesNotContain("th:each=\"asset, stat : ${home.assets}\"")
                .contains("home.executionSuggestion")
                .contains("home.positions")
                .contains("data-position-independent")
                .contains("GPT_FINAL")
                .contains("GEMINI_REVIEW")
                .contains("GROK_CHALLENGE")
                .doesNotContain("tab.finalPlanMode")
                .doesNotContain("tab.downgradeReason")
                .doesNotContain("aiDecision.consistency.finalPlanMode")
                .contains("仅供人工复核，不是交易指令")
                .doesNotContain("完整持仓页待实现")
                .doesNotContain("route-unresolved")
                .doesNotContain("href=\"/review/dashboard\"")
                .contains("href=\"/dashboard/mobile/positions\"")
                .doesNotContain("/api/user-positions")
                .doesNotContain("/api/order")
                .doesNotContain("/api/trade");
    }

    @Test
    void mobileAssetProjectionExcludesDefaultSlotsAndFailsClosedWithoutASelectedCard() throws Exception {
        String controller = Files.readString(MOBILE_CONTROLLER);
        String html = Files.readString(TEMPLATE);

        assertThat(controller)
                .contains("\"DEFAULT_SLOT\".equalsIgnoreCase(asset.getSlotType())")
                .contains("normalizedRequest != null && selected == null")
                .contains("return List.of()")
                .doesNotContain("home.setSelectedSymbol")
                .contains("visible.add(selected)");
        assertThat(html)
                .contains("#lists.isEmpty(mobileAssets)")
                .contains("aria-checked=${selected}")
                .contains("tabindex=${selected ? 0 : -1}")
                .doesNotContain("#lists.isEmpty(home.assets)");
    }

    @Test
    void mobileTemplateKeepsFrozenInformationArchitectureAndWatchTools() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("<h1 class=\"page-heading\" id=\"mobile-page-context\" tabindex=\"-1\">首页</h1>")
                .contains("<p class=\"product-name\">TRADE MODEL V1</p>")
                .doesNotContain("id=\"mobile-home-title\"")
                .doesNotContain("data-header-status-nav")
                .contains("data-header-search")
                .contains("data-header-alerts-nav")
                .contains("data-asset-search-toggle")
                .contains("data-asset-add disabled aria-disabled=\"true\"")
                .contains("添加资产暂未开放")
                .contains("搜索当前重点资产")
                .contains("重点资产监控")
                .contains("AI 三角色复核")
                .contains("一致性综合摘要")
                .contains("asset.latestPrice")
                .contains("asset.compositeScore")
                .contains("asset.confidenceLabel")
                .contains("asset.riskLabel")
                .contains("asset.assetState")
                .contains("asset.analysisId")
                .contains("asset.dataQuality")
                .contains("asset.multiTimeframeState")
                .contains("asset.confused")
                .contains("asset.updatedAt")
                .contains("asset.fieldSourceStatus")
                .contains("data-home-retry")
                .doesNotContain("asset.currentConclusion")
                .doesNotContain("一致性评分")
                .doesNotContain("资产方向阻断")
                .doesNotContain("<h2 id=\"mobile-ai-title\">AI 证据复核</h2>");
        assertThat(html.indexOf("mobile-status-title")).isLessThan(html.indexOf("mobile-alert-title"));
        assertThat(html.indexOf("mobile-alert-title")).isLessThan(html.indexOf("mobile-watch-title"));
        assertThat(html.indexOf("mobile-watch-title")).isLessThan(html.indexOf("mobile-execution-title"));
        assertThat(html.indexOf("mobile-execution-title")).isLessThan(html.indexOf("mobile-position-title"));
        assertThat(html.indexOf("mobile-position-title")).isLessThan(html.indexOf("mobile-ai-title"));
        assertThat(html.indexOf("mobile-ai-title")).isLessThan(html.indexOf("bottom-nav"));
        assertThat(html.indexOf("mobile-consistency-title")).isGreaterThan(html.indexOf("mobile-ai-title"));

        String bottomNav = html.substring(html.indexOf("<nav class=\"bottom-nav\""));
        assertThat(count(bottomNav, "<button")).isEqualTo(4);
        assertThat(count(bottomNav, "<a ")).isEqualTo(1);
        assertThat(bottomNav.indexOf(">首页</button>")).isLessThan(bottomNav.indexOf(">持仓</a>"));
        assertThat(bottomNav.indexOf(">持仓</a>")).isLessThan(bottomNav.indexOf(">AI分析</button>"));
        assertThat(bottomNav.indexOf(">AI分析</button>")).isLessThan(bottomNav.indexOf(">消息</button>"));
        assertThat(bottomNav.indexOf(">消息</button>")).isLessThan(bottomNav.indexOf(">我的</button>"));
        assertThat(bottomNav)
                .doesNotContain(">观察</")
                .doesNotContain(">计划</")
                .doesNotContain(">告警</")
                .doesNotContain(">复盘</");
    }

    @Test
    void mobileDecisionFlowKeepsCoreFactsAboveNativeDisclosuresAndScopesStateLabels()
            throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);
        String css = Files.readString(STYLES);
        String compactPlan = slice(
                html,
                "<dl class=\"definition-list execution-grid execution-compact-grid\"",
                "</dl>");
        String fullPlan = slice(
                html,
                "<details class=\"long-details execution-details\">",
                "</details>");
        String positionSummary = slice(
                html,
                "<dl class=\"position-core position-summary\"",
                "</dl>");
        String positionDetails = slice(
                html,
                "<details class=\"position-details\">",
                "</details>");

        assertThat(html)
                .contains(
                        "<details class=\"long-details status-details\">",
                        "资产状态 · 正在同步",
                        "系统冲突阻断",
                        "计划冲突阻断",
                        "AI 一致性阻断",
                        "Top 3 · 小屏默认 Top 2")
                .doesNotContain(">冲突阻断</dt>");
        assertThat(compactPlan)
                .contains(
                        "data-execution-field=\"direction\"",
                        "data-execution-context-field=\"confidence\"",
                        "data-execution-context-field=\"risk\"",
                        "data-execution-field=\"entryZone\"")
                .doesNotContain(
                        "data-execution-field=\"stopLoss\"",
                        "data-execution-field=\"takeProfitRules\"",
                        "data-execution-field=\"invalidCondition\"");
        assertThat(fullPlan)
                .contains(
                        "查看完整计划",
                        "data-execution-field=\"stopLoss\"",
                        "data-execution-field=\"takeProfitRules\"",
                        "data-execution-field=\"riskRewardRatio\"",
                        "data-execution-field=\"invalidCondition\"",
                        "data-execution-field=\"sourceExecutionPlanId\"",
                        "不是交易指令");
        assertThat(positionSummary)
                .contains("当前风险", "入场逻辑", "方向支持", "反转状态", "当前建议")
                .doesNotContain("持仓 ID", "入场价", "止损", "止盈", "更新时间");
        assertThat(positionDetails)
                .contains("查看完整持仓", "持仓 ID", "入场价", "止损", "止盈", "更新时间");
        assertThat(script)
                .contains(
                        "data-execution-context-field",
                        "optionalRiskReward.hidden",
                        "if (disclosure) disclosure.open = false",
                        "资产状态 · ")
                .contains("position-card\" + (index === 2 ? \" position-third\" : \"\")");
        assertThat(css)
                .contains(
                        ".execution-compact-grid",
                        ".status-details",
                        ".position-third {",
                        "display: none;");
    }

    @Test
    void mobileTemplateHasFailClosedEmptyAndDisabledAiStates() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("暂无告警")
                .contains("暂无关键事件")
                .contains("暂无重点资产")
                .contains("暂无手动持仓")
                .contains("等待同步")
                .contains("当前观点待同步")
                .contains("当前复核待同步")
                .contains("当前挑战待同步")
                .contains("等待 AI 三角色结果同步后生成一致性结论")
                .contains("data-ai-role-summary")
                .doesNotContain("data-role-panel")
                .doesNotContain("完整证据关联尚未提供");
    }

    @Test
    void assetSwitchUpdatesExecutionAndAiButNeverPositionDom() throws Exception {
        String script = Files.readString(SCRIPT);
        String foundation = Files.readString(FRONTEND_CONTRACT);
        String assetSelection = slice(
                script,
                "async function selectAsset(symbol, sourceCard)",
                "function bindAssetPager()");

        assertThat(script)
                .contains("/api/dashboard/home?")
                .contains("selectedSymbol")
                .contains("limit: String(MOBILE_ASSET_LIMIT)")
                .contains("updateExecution(parsed.data.executionSuggestion, selectedAsset, selectedCard)")
                .contains("updateAi(parsed.data.aiDecision)")
                .contains("failClosedAfterLoadError")
                .contains("requestSequence")
                .contains("AbortController")
                .contains("updateSelectedSymbolUrl")
                .contains("frontendContract.parseApiEnvelope")
                .contains("renderSearchResults")
                .contains("assetCards()")
                .doesNotContain("localStorage")
                .doesNotContain("saveCustomSymbols")
                .doesNotContain("innerHTML");
        assertThat(assetSelection)
                .doesNotContain(
                        "renderMobilePositions",
                        "data-mobile-position-list",
                        "position-list",
                        "data-position-independent");
        assertThat(foundation)
                .contains("global.history.replaceState")
                .contains("function executionPlanAccess")
                .contains("sourceExecutionPlanId")
                .doesNotContain("localStorage");
    }

    @Test
    void serverRenderedMobileHomeOnlyBootstrapsFromApiWhenTheFixtureExplicitlyOptsIn()
            throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);
        String initialize = slice(script, "function initialize()", "if (document.readyState");

        assertThat(html).doesNotContain("data-client-home-bootstrap");
        assertThat(initialize)
                .contains(
                        "homeRoot.hasAttribute(\"data-client-home-bootstrap\")",
                        "loadInitialMobileHome()");
        assertThat(initialize.indexOf("homeRoot.hasAttribute(\"data-client-home-bootstrap\")"))
                .isLessThan(initialize.indexOf("loadInitialMobileHome()"));
        assertThat(count(initialize, "loadInitialMobileHome()")).isEqualTo(1);
    }

    @Test
    void assetSwitchFailureClearsStaleMobileAssetContextAndRecoversOnlyFromFreshPayload()
            throws Exception {
        String script = Files.readString(SCRIPT);
        String html = Files.readString(TEMPLATE);
        String clearContext = slice(
                script,
                "function clearMobileAssetContext(symbol, contextState, stateLabel)",
                "function syncMobileAssetContext(home, selectedSymbol)");
        String failure = slice(
                script,
                "function failClosedAfterLoadError(symbol, contextState)",
                "function matchingAsset(assets, symbol)");
        String selection = slice(
                script,
                "async function selectAsset(symbol, sourceCard)",
                "function bindAssetPager()");

        assertThat(html)
                .contains(
                        "data-mobile-status-field=\"marketTrend\"",
                        "data-mobile-status-field=\"riskLevel\"",
                        "data-mobile-status-field=\"dataQuality\"",
                        "data-mobile-status-field=\"aiConflict\"",
                        "data-asset-field=\"state\"",
                        "data-asset-field=\"latestPrice\"",
                        "data-asset-field=\"compositeScore\"",
                        "data-asset-field=\"dataQuality\"",
                        "data-asset-field=\"multiTimeframeState\"",
                        "data-asset-field=\"confused\"",
                        "data-asset-field=\"updatedAt\"",
                        "data-asset-source=\"latestPrice\"",
                        "data-home-retry");
        assertThat(clearContext).contains(
                "window.__lastDashboardHome = null",
                "clearAssetCardProjection(card",
                "node.textContent = node.dataset.mobileStatusField === \"aiStatus\" ? stateLabel : \"--\"",
                "statusRoot.dataset.contextState = contextState",
                "assetRoot.dataset.contextState = contextState",
                "updateAssetDetailLink(null)",
                "updateSelectedSymbolUrl(symbol)");
        assertThat(failure).contains(
                "clearMobileAssetContext(symbol, missing ? \"missing\" : \"error\"",
                "setMobileRetryVisible(true)",
                "status: missing ? \"MISSING\" : \"LOAD_FAILED\"",
                "}, null, null)",
                "aiApplicable: false",
                "无法生成一致性摘要");
        assertThat(selection.indexOf("clearMobileAssetContext(symbol, \"loading\", \"正在同步\")"))
                .isLessThan(selection.indexOf("await fetch("));
        assertThat(selection).contains(
                "setMobileRetryVisible(false)",
                "syncMobileAssetContext(parsed.data, selectedSymbol)",
                "failClosedAfterLoadError(selectedSymbol, \"missing\")",
                "failClosedAfterLoadError(symbol, \"error\")");
        assertThat(script).contains("safeHeader.dataStatus || \"PARTIAL\"");
        assertThat(clearContext + failure).doesNotContain(
                "position-list", "data-position-independent", "selectedPositionId");
    }

    @Test
    void executionSuggestionRequiresExactPlanIdentityAndClearsBoundariesOtherwise() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("home.executionSuggestion.sourceExecutionPlanId")
                .contains("home.executionSuggestion.originalPlanIdentity == 'VERIFIED'")
                .contains("home.executionSuggestion.originalPlanCurrentValidity == 'ACTIVE'")
                .contains("data-exact-plan-visible")
                .contains("计划来源不可验证");
        assertThat(script)
                .contains("frontendContract.executionPlanAccess")
                .contains("access.visible && planFields.indexOf(field) >= 0")
                .contains("? text(safeSuggestion[field], \"--\")")
                .contains("data-execution-conflict")
                .doesNotContain("positionMonitor");
    }

    @Test
    void mobileProjectionCannotPromoteNonActivePersistedPlanStates() throws Exception {
        String foundation = Files.readString(FRONTEND_CONTRACT);
        String access = slice(
                foundation,
                "function executionPlanAccess(suggestion)",
                "function csrfHeaders(headers, root)");

        assertThat(access)
                .contains(
                        "if (!hasText(plan.sourceExecutionPlanId))",
                        "if (status !== \"USABLE_REVIEW_PLAN\")",
                        "visible: false",
                        "reason: displayText(plan.blockedReason, \"执行建议不可用\")")
                .doesNotContain(
                        "status === \"PLAN_BLOCKED\"",
                        "status === \"PLAN_INVALID\"",
                        "status === \"PLAN_INCOMPLETE\"",
                        "status === \"PLAN_REVIEW_ONLY\"",
                        "status === \"REVALIDATION_REQUIRED\"");
    }

    @Test
    void mobileProjectionUsesBackendDataQualityStateAndShowsFailClosedCopy() throws Exception {
        String script = Files.readString(SCRIPT);
        String foundation = Files.readString(FRONTEND_CONTRACT);
        String projection = slice(
                script,
                "function syncAssetCardProjection(card, asset)",
                "function clearAssetCardProjection(card, stateLabel)");

        assertThat(foundation).contains(
                "PARTIAL: \"数据不足\"",
                "function dataQualityLabel(value)");
        assertThat(projection)
                .contains("frontendContract.dataQualityLabel(asset.dataQuality)")
                .doesNotContain("dataQualityScore", ">= 70", "< 70");
    }

    @Test
    void interactionsCoverPagerUnavailableDestinationsAndHomeResetAccessibly() throws Exception {
        String source = Files.readString(SCRIPT) + Files.readString(TEMPLATE);

        assertThat(source)
                .contains("ArrowRight")
                .contains("ArrowLeft")
                .contains("aria-checked")
                .contains("data-unavailable-nav")
                .contains("该页面")
                .contains("window.scrollTo({ top: 0")
                .contains("scrollIntoView")
                .contains("data-position-nav")
                .contains("data-ai-nav")
                .contains("data-message-nav")
                .contains("data-profile-nav")
                .doesNotContain("data-review-nav")
                .doesNotContain("[data-watch-nav]")
                .doesNotContain("[data-plan-nav]")
                .doesNotContain("[data-alerts-nav]");
    }

    @Test
    void responsiveStylesUseIndependentBreakpointSafeAreasAndTouchTargets() throws Exception {
        String css = Files.readString(STYLES);

        assertThat(css)
                .contains("env(safe-area-inset-top)")
                .contains("env(safe-area-inset-bottom)")
                .contains("@media (max-width: 430px)")
                .contains("@media (prefers-color-scheme: dark)")
                .contains("min-height: 44px")
                .contains("overflow-x: hidden")
                .contains("overscroll-behavior-x: none")
                .contains(".watch-tool-button")
                .contains(".asset-search-panel")
                .contains("grid-template-columns: repeat(5, minmax(0, 1fr))")
                .contains("grid-template-columns: repeat(3, minmax(0, 1fr))")
                .contains("scroll-snap-type: x mandatory")
                .contains("contain: inline-size layout paint")
                .contains("isolation: isolate")
                .contains("background: var(--surface)")
                .contains("@media (prefers-reduced-motion: reduce)")
                .doesNotContain("transform: scale")
                .doesNotContain("font-size: clamp")
                .doesNotContain(
                        "font-size: 0.62rem",
                        "font-size: 0.65rem",
                        "font-size: 0.66rem",
                        "font-size: 0.69rem",
                        "font-size: 0.7rem",
                        "font-size: 0.72rem")
                .doesNotContain("color-mix(")
                .doesNotContain("url(http")
                .doesNotContain("cdn");
    }

    private int count(String source, String target) {
        return (source.length() - source.replace(target, "").length()) / target.length();
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
