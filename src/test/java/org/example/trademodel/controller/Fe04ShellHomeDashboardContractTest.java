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
        String mobileContextRefresh = slice(
                mobileScript,
                "async function selectAsset(symbol, sourceCard)",
                "function bindAssetPager()");
        String desktop = Files.readString(DESKTOP);
        String desktopPayload = slice(
                desktop,
                "function renderDashboardHomePayload(home, preservePositionSummary)",
                "function renderDashboardHomeUnavailable()");
        String desktopContextRefresh = slice(
                desktop,
                "function refreshAssetContext()",
                "function refreshDashboardDiagnostics()");
        String desktopContextFailure = slice(
                desktop,
                "function renderAssetContextUnavailable(contextState)",
                "function fetchDashboardHome(");

        assertThat(mobileContextRefresh)
                .contains("updateExecution(parsed.data.executionSuggestion, selectedAsset, selectedCard)")
                .contains("updateAi(parsed.data.aiDecision)")
                .doesNotContain("position-list")
                .doesNotContain("data-position-independent");
        assertThat(desktopPayload)
                .contains("if (!preservePositionSummary) renderHomePositionsFromPayload")
                .contains("var selectedAsset = selectedHomeAsset(home)")
                .contains("renderHomeExecutionFromPayload(home.executionSuggestion || {}, selectedAsset)")
                .contains("renderHomeAiDecisionFromPayload(home.aiDecision || {}, selectedAsset)");
        assertThat(desktopContextRefresh)
                .contains("fetchDashboardHome(true)")
                .contains("renderAssetContextUnavailable()")
                .doesNotContain("renderHomePositionsFromPayload")
                .doesNotContain("renderDashboardHomeUnavailable");
        assertThat(desktopContextFailure)
                .contains("window.__lastDashboardHome = null")
                .contains("setRuntimeStatus(runtimeState)")
                .contains("runtimeState = missing ? \"MISSING\" : \"ERROR\"")
                .contains("assets.dataset.homeState = missing ? \"missing\" : \"error\"")
                .contains("renderHomeExecutionFromPayload({")
                .contains("runStatus: \"LOAD_FAILED\"")
                .contains("runStatusLabel: \"当前不可查看\"")
                .contains("}, null);")
                .doesNotContain(
                        "renderHomeSystemStateFromPayload",
                        "renderHomeAlertEventRowsFromPayload",
                        "renderHomePositionsFromPayload")
                .doesNotContain("selectedPositionId = null");
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
                "<article class=\"latest-module latest-position-module\" id=\"homePositionCard\"",
                "<article class=\"latest-module latest-plan-module\" id=\"homeExecutionCard\"");

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
                        "计划冲突阻断",
                        "查看完整计划")
                .doesNotContain("validPeriod");
        assertThat(mobilePosition)
                .contains("Top 3", "小屏默认 Top 2", "查看完整持仓", "入场逻辑", "方向支持", "反转状态", "当前风险", "当前建议")
                .doesNotContain("手动录入持仓</button>")
                .doesNotContain("记录平仓")
                .doesNotContain("复盘中心");
        assertThat(desktopPosition)
                .contains("持仓监控 Top3", "风险变化、监控结论与建议动作")
                .contains("manualPositionBtn")
                .doesNotContain("reviewCenterLink");
        assertThat(desktop)
                .contains("class=\"latest-position-close\"")
                .contains("记录平仓")
                .contains("data-position-source=", "data-final-plan-id=")
                .contains("MANUAL_INDEPENDENT");
        assertThat(desktop)
                .contains(
                        "请选择资产",
                        "选择一个重点机会资产后查看执行计划。",
                        "frontendContract.executionPlanAccess(s)");
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
                .contains("<a href=\"/dashboard/positions\" class=\"product-nav-item\">持仓</a>")
                .doesNotContain("data-desktop-unavailable-nav aria-disabled=\"true\">持仓");
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
                        "function renderHomeAiDecisionFromPayload(aiDecision, asset)",
                        "renderDesktopAiAnalysis(ai, asset || null)",
                        "analysisState === \"partial\" ? ai.tabs : []",
                        "/dashboard/analysis-detail?analysisId=",
                        "frontendContract.readUrlParam(\"view\") === \"ai\"")
                .doesNotContain(
                        "renderDesktopAiAnalysis(ai, selectedHomeAsset(window.__lastDashboardHome || {}))")
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
                        "runStatus: missing ? \"MISSING\" : \"LOAD_FAILED\"",
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
    void aiAnalysisPreservesStructuredRoleSemanticsWhileUnavailableRolesFailClosed()
            throws Exception {
        String mobile = Files.readString(MOBILE);
        String script = Files.readString(MOBILE_SCRIPT);
        String desktop = Files.readString(DESKTOP);
        String contract = Files.readString(FRONTEND_CONTRACT);

        assertThat(contract)
                .contains(
                        "normalized.resultAvailable = supplied.resultAvailable === true",
                        "roleLabel: definition.label",
                        "roleState: \"UNAVAILABLE\"",
                        "dataState: \"SOURCE_UNAVAILABLE\"",
                        "normalized.roleState = normalizeRoleState(normalized.roleState)",
                        "STRUCTURED_AI_COLLECTIONS.forEach(function (collection)",
                        "normalized[collection.key] = Array.isArray(normalized[collection.key])");
        assertThat(mobile)
                .contains(
                        "data-result-available=${tab.resultAvailable == true}",
                        "data-role-status-message=${tab.statusMessage != null ? tab.statusMessage : ''}",
                        "tab.resultAvailable == true ?",
                        "tab.role == 'GPT_FINAL' and tab.resultAvailable == true",
                        "data-ai-analysis-role-output hidden");
        assertThat(script)
                .contains(
                        "if (tab.resultAvailable !== true)",
                        "var resultAvailable = canReadRoles && tab.resultAvailable === true",
                        "roleOutput.hidden = !resultAvailable",
                        "var resultAvailable = card.dataset.resultAvailable === \"true\"",
                        "card.dataset.roleStatusMessage",
                        "if (!resultAvailable) return tab",
                        "if (role === \"GPT_FINAL\")")
                .doesNotContain("Number(tab.resultAvailable)");
        assertThat(desktop)
                .contains(
                        "function renderHomeAiRoleTab(role, tab)",
                        "frontendContract.normalizeRoleState(role.roleState)",
                        "renderGptFinalHomeRole(role)",
                        "renderGeminiReviewHomeRole(role)",
                        "renderGrokChallengeHomeRole(role)",
                        "function desktopAiRoleSummary(tab)",
                        "if (tab.resultAvailable !== true)",
                        "var resultAvailable = analysisState === \"partial\" && tab.resultAvailable === true",
                        "data-desktop-ai-role-output hidden",
                        "roleOutput.hidden = !resultAvailable",
                        "var hasStructuredContract = tab === \"GPT_FINAL\"",
                        "(roleState === \"UNAVAILABLE\" || roleState === \"ERROR\") && !hasStructuredContract",
                        "function renderUnavailableAiRole(role)",
                        "roleMetadata(role)")
                .doesNotContain("Boolean(tab.resultAvailable)");
    }

    @Test
    void desktopAiConsistencyStaysAdjacentToSingleWorkspaceAndUsesConsistencyFieldsOnly()
            throws Exception {
        String desktop = Files.readString(DESKTOP);
        String aiPanel = slice(
                desktop,
                "<article class=\"latest-module latest-ai-workspace\" id=\"homeAiPanel\"",
                "<aside class=\"latest-module latest-consistency\"");
        String consistencyRenderer = slice(
                desktop,
                "function renderHomeConsistencyCard(options)",
                "function roleMetadata(role)");
        String payloadRenderer = slice(
                desktop,
                "function renderHomeAiDecisionFromPayload(aiDecision, asset)",
                "function renderHomePushInboxFromPayload");

        assertThat(aiPanel)
                .contains(
                        "homeAiSummaryCards",
                        "role=\"tablist\"",
                        "GPT_FINAL",
                        "GEMINI_REVIEW",
                        "GROK_CHALLENGE")
                .doesNotContain("id=\"homeConsistencyContent\"");
        assertThat(desktop)
                .contains(
                        "id=\"homeConsistencyContent\"",
                        "冲突与最终调整",
                        "latest-consistency")
                .doesNotContain(
                        "homeConsistencyPanel",
                        "consistency-ring",
                        "一致性评分",
                        "投票百分比");
        assertThat(consistencyRenderer)
                .contains("options.dataState", "options.conflictLevel", "options.finalMarketBias",
                        "options.finalPlanMode", "options.mainReason", "options.recoveryCondition")
                .doesNotContain(
                        "actualConsistencyScore",
                        "consistencyScore",
                        "agreementScore",
                        "aiApplicable",
                        "directionalPushBlocked",
                        "downgradeReason");
        assertThat(payloadRenderer)
                .contains("var c = ai.consistency || {}", "finalMarketBias", "finalPlanMode")
                .doesNotContain("finalRole", "consistencyScore", "consistencyLevel");
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
