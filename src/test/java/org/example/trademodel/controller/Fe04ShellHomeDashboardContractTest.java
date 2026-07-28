package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class Fe04ShellHomeDashboardContractTest {
    private static final Path MOBILE =
            Path.of("src/main/resources/templates/dashboard-mobile.html");
    private static final Path MOBILE_SCRIPT =
            Path.of("src/main/resources/static/js/dashboard-mobile.js");
    private static final Path MOBILE_STYLES =
            Path.of("src/main/resources/static/css/dashboard-mobile.css");
    private static final Path DESKTOP =
            Path.of("src/main/resources/templates/dashboard.html");

    @Test
    void mobileShellUsesTheFrozenFiveDestinationOrderAndFailsClosed() throws Exception {
        String html = Files.readString(MOBILE);
        String navigation = slice(html, "<nav class=\"bottom-nav\"", "</nav>");

        assertThat(navigation.indexOf(">首页</button>"))
                .isLessThan(navigation.indexOf(">持仓</button>"));
        assertThat(navigation.indexOf(">持仓</button>"))
                .isLessThan(navigation.indexOf(">AI分析</button>"));
        assertThat(navigation.indexOf(">AI分析</button>"))
                .isLessThan(navigation.indexOf(">消息</button>"));
        assertThat(navigation.indexOf(">消息</button>"))
                .isLessThan(navigation.indexOf(">我的</button>"));
        assertThat(navigation)
                .contains("data-home-nav aria-current=\"page\"")
                .contains("data-position-nav data-unavailable-nav aria-disabled=\"true\"")
                .contains("data-ai-nav data-unavailable-nav aria-disabled=\"true\"")
                .contains("data-message-nav data-unavailable-nav aria-disabled=\"true\"")
                .contains("data-profile-nav data-unavailable-nav aria-disabled=\"true\"");
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
                .contains("其余页面暂未开放");
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
