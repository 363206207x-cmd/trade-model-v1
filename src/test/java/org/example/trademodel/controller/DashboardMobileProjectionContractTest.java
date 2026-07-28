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
                .doesNotContain("/positions")
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
        assertThat(count(bottomNav, "<button")).isEqualTo(5);
        assertThat(count(bottomNav, "<a ")).isEqualTo(0);
        assertThat(bottomNav.indexOf(">首页</button>")).isLessThan(bottomNav.indexOf(">持仓</button>"));
        assertThat(bottomNav.indexOf(">持仓</button>")).isLessThan(bottomNav.indexOf(">AI分析</button>"));
        assertThat(bottomNav.indexOf(">AI分析</button>")).isLessThan(bottomNav.indexOf(">消息</button>"));
        assertThat(bottomNav.indexOf(">消息</button>")).isLessThan(bottomNav.indexOf(">我的</button>"));
        assertThat(bottomNav)
                .doesNotContain(">观察</")
                .doesNotContain(">计划</")
                .doesNotContain(">告警</")
                .doesNotContain(">复盘</");
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
                .doesNotContain("position-list")
                .doesNotContain("data-position-independent")
                .doesNotContain("innerHTML");
        assertThat(foundation)
                .contains("global.history.replaceState")
                .contains("function executionPlanAccess")
                .contains("sourceExecutionPlanId")
                .doesNotContain("localStorage");
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
                .contains("background: var(--surface)")
                .doesNotContain("transform: scale")
                .doesNotContain("font-size: clamp")
                .doesNotContain("color-mix(")
                .doesNotContain("url(http")
                .doesNotContain("cdn");
    }

    private int count(String source, String target) {
        return (source.length() - source.replace(target, "").length()) / target.length();
    }
}
