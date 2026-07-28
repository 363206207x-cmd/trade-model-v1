package org.example.trademodel.controller;

import org.example.trademodel.vo.DashboardHomeVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class Fe04ShellHomeDashboardContractTest {
    private static final Path MOBILE =
            Path.of("src/main/resources/templates/dashboard-mobile.html");
    private static final Path MOBILE_SCRIPT =
            Path.of("src/main/resources/static/js/dashboard-mobile.js");
    private static final Path FRONTEND_CONTRACT =
            Path.of("src/main/resources/static/js/frontend-contract.js");
    private static final Path MOBILE_STYLES =
            Path.of("src/main/resources/static/css/dashboard-mobile.css");
    private static final Path DESKTOP =
            Path.of("src/main/resources/templates/dashboard.html");

    @Test
    void mobileShellUsesTheFrozenFiveDestinationOrderAndActivatesAiAnalysis() throws Exception {
        String html = Files.readString(MOBILE);
        String navigation = slice(html, "<nav class=\"bottom-nav\"", "</nav>");

        assertThat(navigation.indexOf(">首页</button>"))
                .isLessThan(navigation.indexOf(">持仓</a>"));
        assertThat(navigation.indexOf(">持仓</a>"))
                .isLessThan(navigation.indexOf(">AI分析</button>"));
        assertThat(navigation.indexOf(">AI分析</button>"))
                .isLessThan(navigation.indexOf(">消息</button>"));
        assertThat(navigation.indexOf(">消息</button>"))
                .isLessThan(navigation.indexOf(">我的</button>"));
        assertThat(navigation)
                .contains("data-home-nav aria-current=\"page\"")
                .contains("href=\"/dashboard/mobile/positions\" data-position-nav")
                .contains("<button type=\"button\" data-ai-nav>AI分析</button>")
                .contains("data-message-nav data-unavailable-nav aria-disabled=\"true\"")
                .contains("data-profile-nav data-unavailable-nav aria-disabled=\"true\"")
                .doesNotContain("data-ai-nav data-unavailable-nav");
    }

    @Test
    void homeAssetCardsStayDecisionSummariesAndUseOnlyAuthoritativeDetailIdentity()
            throws Exception {
        String html = Files.readString(MOBILE);
        String script = Files.readString(MOBILE_SCRIPT);
        String assetSection = slice(html, "<section class=\"watch-section\"", "</section>");

        assertThat(assetSection)
                .contains(
                        "asset.symbol",
                        "asset.latestPrice",
                        "asset.marketBiasLabel",
                        "asset.compositeScore",
                        "asset.confidenceLabel",
                        "asset.riskLabel",
                        "asset.assetState",
                        "data-analysis-id=${asset.analysisId}")
                .doesNotContain(
                        "asset.currentConclusion",
                        "asset.evidenceCount",
                        "asset.timeframeFreshness",
                        "asset.sourceProvider");
        assertThat(script)
                .contains("function updateAssetDetailLink(card)")
                .contains("card.dataset.analysisId")
                .contains("/dashboard/analysis-detail?analysisId=")
                .contains("delete card.dataset.analysisId")
                .doesNotContain("/dashboard/asset-detail?selectedSymbol=")
                .doesNotContain("latestAnalysis");
    }

    @Test
    void assetContextRefreshUpdatesExecutionAndAiWithoutRenderingPositions() throws Exception {
        String mobileScript = Files.readString(MOBILE_SCRIPT);
        String desktop = Files.readString(DESKTOP);
        String desktopPayload = slice(
                desktop,
                "function renderDashboardHomePayload(home, preservePositionSummary)",
                "function renderDashboardHomeUnavailable()");
        String desktopContextRefresh = slice(
                desktop,
                "function refreshAssetContext()",
                "function refreshDashboardDiagnostics()");

        assertThat(mobileScript)
                .contains("updateExecution(parsed.data.executionSuggestion, selectedAsset, selectedCard)")
                .contains("updateAi(parsed.data.aiDecision)")
                .doesNotContain("position-list")
                .doesNotContain("data-position-independent");
        assertThat(desktopPayload)
                .contains("if (!preservePositionSummary) renderHomePositionsFromPayload")
                .contains("renderHomeExecutionFromPayload")
                .contains("renderHomeAiDecisionFromPayload");
        assertThat(desktopContextRefresh)
                .contains("fetchDashboardHome(true)")
                .doesNotContain("renderHomePositionsFromPayload")
                .doesNotContain("renderDashboardHomeUnavailable");
    }

    @Test
    void executionPositionAndAiSummariesRespectFe04Boundaries() throws Exception {
        String mobile = Files.readString(MOBILE);
        String desktop = Files.readString(DESKTOP);
        String mobileExecution = slice(
                mobile,
                "<section class=\"execution-section\"",
                "</section>");
        String mobilePosition = slice(
                mobile,
                "<section class=\"position-section\"",
                "</section>");
        String mobileAi = slice(
                mobile,
                "<section class=\"ai-section\"",
                "</section>");
        String desktopPosition = slice(
                desktop,
                "<section class=\"card\" id=\"homePositionCard\"",
                "</section>");

        assertThat(mobileExecution)
                .contains(
                        "方向",
                        "是否值得开仓",
                        "入场区间",
                        "止损",
                        "止盈方案",
                        "杠杆建议",
                        "仓位建议",
                        "计划失效条件",
                        "有效开始",
                        "有效结束",
                        "冲突阻断")
                .doesNotContain("validPeriod");
        assertThat(mobilePosition)
                .contains("Top 3", "入场逻辑", "方向支持", "反转状态", "风险等级", "当前建议")
                .doesNotContain("手动录入持仓</button>")
                .doesNotContain("记录平仓")
                .doesNotContain("复盘中心");
        assertThat(desktopPosition)
                .contains("Top 3", "仅显示手动录入持仓")
                .doesNotContain("manualPositionBtn")
                .doesNotContain("reviewCenterLink")
                .doesNotContain("position-action-btn");
        assertThat(mobileAi)
                .contains("data-ai-role-summary")
                .contains("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE")
                .doesNotContain("role=\"tablist\"")
                .doesNotContain("完整证据")
                .doesNotContain("八大评分")
                .doesNotContain("多周期");
    }

    @Test
    void positionNavigationUsesTheReadonlyFe04cRoutesWhileOtherDestinationsStayUnavailable()
            throws Exception {
        String mobile = Files.readString(MOBILE);
        String desktop = Files.readString(DESKTOP);

        assertThat(mobile)
                .contains("<a href=\"/dashboard/mobile/positions\" data-position-nav>持仓</a>")
                .doesNotContain("data-position-nav data-unavailable-nav");
        assertThat(desktop)
                .contains("<a href=\"/dashboard/positions\" class=\"product-nav-item\">Position</a>")
                .doesNotContain("data-desktop-unavailable-nav aria-disabled=\"true\">Position");
    }

    @Test
    void restrictedAiAnalysisViewsReuseAuthoritativeIdentityAndFe03WithoutNewCapabilities()
            throws Exception {
        String mobile = Files.readString(MOBILE);
        String mobileScript = Files.readString(MOBILE_SCRIPT);
        String mobileStyles = Files.readString(MOBILE_STYLES);
        String desktop = Files.readString(DESKTOP);
        String mobileAi = slice(
                mobile,
                "<main class=\"mobile-home mobile-ai-analysis\"",
                "</main>");
        String desktopAi = slice(
                desktop,
                "<section class=\"desktop-ai-analysis-view\"",
                "</section>\n            </main>");

        assertThat(mobileAi)
                .contains(
                        "data-mobile-ai-view",
                        "data-ai-analysis-id",
                        "data-ai-analysis-tab=\"GPT_FINAL\"",
                        "data-ai-analysis-tab=\"GEMINI_REVIEW\"",
                        "data-ai-analysis-tab=\"GROK_CHALLENGE\"",
                        "data-ai-analysis-detail-link",
                        "市场资产搜索与观察资产写入暂未开放",
                        "placeholder=\"暂未开放\" disabled",
                        "八大评分、多周期与证据链仅由 FE-03 Analysis Detail 展示")
                .doesNotContain("data-ai-analysis-tab=\"AI_CONSISTENCY\"")
                .doesNotContain("data-watch-write")
                .doesNotContain("data-market-search-result");
        assertThat(desktopAi)
                .contains(
                        "data-desktop-ai-analysis-root",
                        "id=\"desktopAiAnalysisId\"",
                        "data-desktop-ai-tab=\"GPT_FINAL\"",
                        "data-desktop-ai-tab=\"GEMINI_REVIEW\"",
                        "data-desktop-ai-tab=\"GROK_CHALLENGE\"",
                        "id=\"desktopAiDetailLink\"",
                        "市场资产搜索与观察资产写入暂未开放",
                        "placeholder=\"暂未开放\"")
                .doesNotContain("data-desktop-ai-tab=\"AI_CONSISTENCY\"");
        assertThat(mobileScript)
                .contains(
                        "card.dataset.analysisId",
                        "renderAiAnalysis(safeAi, selectedAssetCard())",
                        "frontendContract.normalizeAiTabs(canReadRoles ? tabs : [])",
                        "/dashboard/analysis-detail?analysisId=",
                        "frontendContract.readUrlParam(\"view\") === \"ai\"")
                .doesNotContain("latestAnalysis")
                .doesNotContain("Number(analysisId)")
                .doesNotContain("parseInt(analysisId");
        assertThat(desktop)
                .contains(
                        "renderDesktopAiAnalysis(ai, selectedHomeAsset(window.__lastDashboardHome || {}))",
                        "analysisState === \"partial\" ? ai.tabs : []",
                        "/dashboard/analysis-detail?analysisId=",
                        "frontendContract.readUrlParam(\"view\") === \"ai\"")
                .doesNotContain("latestAnalysisId");
        assertThat(mobileStyles)
                .contains(
                        ".ai-analysis-role-tabs button",
                        "min-height: 48px",
                        ".ai-analysis-detail-link",
                        "min-height: 44px");
    }

    @Test
    void restrictedAiAnalysisFailsClosedForMissingIdentityAndLoadFailure() throws Exception {
        String mobile = Files.readString(MOBILE);
        String script = Files.readString(MOBILE_SCRIPT);
        String desktop = Files.readString(DESKTOP);
        String contract = Files.readString(FRONTEND_CONTRACT);

        assertThat(mobile + desktop + contract)
                .contains(
                        "分析身份待同步",
                        "当前不可查看",
                        "等待同步",
                        "--",
                        "不补造");
        assertThat(contract)
                .contains(
                        "function aiAnalysisState(identityReady, failed, loading, empty)",
                        "if (failed) return \"error\"",
                        "if (loading) return \"loading\"",
                        "if (empty) return \"empty\"",
                        "if (!identityReady) return \"missing\"",
                        "return \"partial\"",
                        "AI 分析加载失败，当前不可查看",
                        "暂无可分析资产",
                        "缺少权威 analysisId，当前不可查看");
        assertThat(script)
                .contains(
                        "var empty = !card && !failed && !loading",
                        "frontendContract.aiAnalysisState(",
                        "frontendContract.aiAnalysisStateView(analysisState)",
                        "root.dataset.analysisState = analysisState",
                        "runStatus: \"LOADING\"",
                        "runStatus: \"LOAD_FAILED\"",
                        "renderAiAnalysisRoles(safeAi.tabs, analysisState)")
                .doesNotContain("/api/analysis/create")
                .doesNotContain("/api/watch");
        assertThat(desktop)
                .contains(
                        "var empty = !asset && !failed && !loading",
                        "frontendContract.aiAnalysisState(",
                        "frontendContract.aiAnalysisStateView(analysisState)",
                        "root.dataset.analysisState = analysisState")
                .doesNotContain("fetch(\"/api/analysis")
                .doesNotContain("fetch(\"/api/watch");
    }

    @Test
    void aiAnalysisRequiresExplicitRoleAvailabilityBeforeRenderingConclusions()
            throws Exception {
        String mobile = Files.readString(MOBILE);
        String script = Files.readString(MOBILE_SCRIPT);
        String desktop = Files.readString(DESKTOP);
        String contract = Files.readString(FRONTEND_CONTRACT);

        assertThat(contract)
                .contains("normalized.resultAvailable = normalized.resultAvailable === true");
        assertThat(mobile)
                .contains(
                        "data-result-available=${tab.resultAvailable == true}",
                        "tab.resultAvailable == true ?",
                        "tab.role == 'GPT_FINAL' and tab.resultAvailable == true");
        assertThat(script)
                .contains(
                        "if (tab.resultAvailable !== true)",
                        "var resultAvailable = canReadRoles && tab.resultAvailable === true",
                        "var resultAvailable = card.dataset.resultAvailable === \"true\"",
                        "if (resultAvailable && role === \"GPT_FINAL\")")
                .doesNotContain("Number(tab.resultAvailable)");
        assertThat(desktop)
                .contains(
                        "function desktopAiRoleSummary(tab)",
                        "if (tab.resultAvailable !== true)",
                        "var resultAvailable = analysisState === \"partial\" && tab.resultAvailable === true")
                .doesNotContain("Boolean(tab.resultAvailable)");
    }

    @Test
    void desktopAiConsistencyStaysInsideThreeRoleRegionAndUsesConsistencyFieldsOnly()
            throws Exception {
        String desktop = Files.readString(DESKTOP);
        String aiPanel = slice(
                desktop,
                "<section class=\"card\" id=\"homeAiPanel\"",
                "</section>");
        String consistencyRenderer = slice(
                desktop,
                "function renderHomeConsistencyCard(options)",
                "function renderGptFinalHomeRole");
        String payloadRenderer = slice(
                desktop,
                "function renderHomeAiDecisionFromPayload(aiDecision)",
                "function renderHomePushInboxFromPayload");

        assertThat(aiPanel)
                .contains(
                        "id=\"homeConsistencyContent\"",
                        "AI 一致性摘要",
                        "一致性等级",
                        "冲突等级",
                        "是否进入冲突阻断",
                        "一句话摘要",
                        "GPT_FINAL",
                        "GEMINI_REVIEW",
                        "GROK_CHALLENGE")
                .doesNotContain(
                        "homeConsistencyPanel",
                        "consistency-ring",
                        "一致性评分",
                        "AI 计划模式");
        assertThat(consistencyRenderer)
                .contains("options.level", "options.conflictLevel", "options.confused", "options.summary")
                .doesNotContain(
                        "actualConsistencyScore",
                        "consistencyScore",
                        "agreementScore",
                        "finalTendency",
                        "planMode",
                        "directionalPushBlocked",
                        "downgradeReason");
        assertThat(payloadRenderer)
                .contains("var c = ai.consistency || {}")
                .doesNotContain("finalRole", "finalPlanMode", "planMode");
    }

    @Test
    void mobileConflictBlockKeepsBackendReasonWhileUnverifiedBoundariesStayHidden()
            throws Exception {
        String mobile = Files.readString(MOBILE);
        String script = Files.readString(MOBILE_SCRIPT);
        String frontendContract = Files.readString(FRONTEND_CONTRACT);
        String execution = slice(
                mobile,
                "<section class=\"execution-section\"",
                "</section>");
        String updater = slice(
                script,
                "function updateExecution(suggestion, selectedAsset, selectedCard)",
                "function updateConsistency(consistency)");

        assertThat(execution)
                .contains(
                        "home.executionSuggestion.statusLabel != null and home.executionSuggestion.statusLabel != '' ? home.executionSuggestion.statusLabel",
                        "home.executionSuggestion.blockedReason != null and home.executionSuggestion.blockedReason != '' ? home.executionSuggestion.blockedReason",
                        "planExact and home.executionSuggestion.entryZone != null",
                        "planExact and home.executionSuggestion.stopLoss != null",
                        "planExact and home.executionSuggestion.takeProfitRules != null")
                .doesNotContain("planExact and home.executionSuggestion.blockedReason");
        assertThat(updater)
                .contains(
                        "text(safeSuggestion.blockedReason, access.reason)",
                        "setText(\"[data-execution-conflict]\", safeSuggestion.blockedReason, \"--\")",
                        "access.visible && planFields.indexOf(field) >= 0");
        assertThat(frontendContract)
                .contains(
                        "statusLabel: displayText(plan.statusLabel, \"当前暂无可验证的执行建议\")",
                        "reason: displayText(plan.blockedReason, \"计划来源不可验证\")");
    }

    @Test
    void initialMobileRenderKeepsBackendConflictStateAndFailsClosedPlanBoundaries()
            throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        DashboardHomeVO.ExecutionSuggestionVO suggestion =
                new DashboardHomeVO.ExecutionSuggestionVO();
        suggestion.setStatus("CONFLICT_BLOCKED");
        suggestion.setStatusLabel("冲突阻断");
        suggestion.setBlockedReason("AI冲突达到阻断阈值");
        suggestion.setEntryZone("LEAK_ENTRY");
        suggestion.setStopLoss("LEAK_STOP");
        suggestion.setTakeProfitRules("LEAK_TAKE_PROFIT");
        home.setSelectedSymbol("BTCUSDT");
        home.setExecutionSuggestion(suggestion);

        String execution = slice(
                Files.readString(MOBILE),
                "<section class=\"execution-section\"",
                "</section>") + "</section>";
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("home", home);

        String rendered = engine.process(
                "<html xmlns:th=\"http://www.thymeleaf.org\"><body>"
                        + execution
                        + "</body></html>",
                context);

        assertThat(rendered)
                .contains(
                        "data-exact-plan-visible=\"false\"",
                        "data-execution-field=\"statusLabel\">冲突阻断</strong>",
                        "data-execution-field=\"blockedReason\">AI冲突达到阻断阈值</p>",
                        "data-execution-field=\"entryZone\">--</dd>",
                        "data-execution-field=\"stopLoss\">--</dd>",
                        "data-execution-field=\"takeProfitRules\">--</dd>")
                .doesNotContain("LEAK_ENTRY", "LEAK_STOP", "LEAK_TAKE_PROFIT");
    }

    @Test
    void frozenShellRetainsTouchDynamicTypeAndRootOverflowGuards() throws Exception {
        String css = Files.readString(MOBILE_STYLES);
        String desktop = Files.readString(DESKTOP);

        assertThat(css)
                .contains("--nav-height: 76px")
                .contains("grid-template-columns: repeat(5, minmax(0, 1fr))")
                .contains("min-height: 44px")
                .contains("data-mobile-text-size=\"accessibility\"")
                .contains("overflow-x: hidden")
                .contains("overscroll-behavior-x: none");
        assertThat(desktop)
                .contains("data-desktop-five-destination-navigation")
                .contains("Dashboard", "Position", "AI Analysis", "Message", "Profile")
                .contains("data-desktop-unavailable-nav")
                .contains("消息与个人页暂未开放");
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
