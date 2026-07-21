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
    private static final Path STYLES = Path.of("src/main/resources/static/css/dashboard-mobile.css");

    @Test
    void mobileTemplateProjectsOnlyConfirmedBackendSemantics() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(count(html, "class=\"status-cell")).isEqualTo(7);
        assertThat(html)
                .contains("home.systemState.marketTrend.valueLabel")
                .contains("home.systemState.riskLevel.valueLabel")
                .contains("home.systemState.dataQuality.valueLabel")
                .contains("home.systemState.aiConflict.valueLabel")
                .contains("home.systemState.pendingReview.valueLabel")
                .contains("home.systemState.confused.valueLabel")
                .contains("home.systemState.hotReset.valueLabel")
                .doesNotContain("holdingRisk")
                .doesNotContain("<canvas")
                .doesNotContain("<svg")
                .doesNotContain("KPI")
                .doesNotContain("STATIC_LAYOUT_FIXTURE")
                .doesNotContain("{asset[");
    }

    @Test
    void mobileTemplateKeepsAssetExecutionPositionAndAiOwnershipSeparate() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("th:if=\"${stat.index < 3}\"")
                .contains("home.executionSuggestion")
                .contains("home.positions")
                .contains("data-position-independent")
                .contains("GPT_FINAL")
                .contains("GEMINI_REVIEW")
                .contains("GROK_CHALLENGE")
                .contains("tab.finalPlanMode")
                .doesNotContain("aiDecision.consistency.finalPlanMode")
                .contains("仅供人工复核，不是交易指令")
                .contains("完整持仓页待实现")
                .contains("type=\"button\" disabled aria-disabled=\"true\">完整持仓页待实现")
                .contains("href=\"/review/dashboard\"")
                .doesNotContain("/positions")
                .doesNotContain("/api/order")
                .doesNotContain("/api/trade");
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
                .contains("暂无 AI 三角色结果")
                .contains("等待 AI 三角色结果同步后生成一致性结论")
                .contains("th:hidden=\"${tab.role != 'GPT_FINAL'}\"");
    }

    @Test
    void assetSwitchUpdatesExecutionAndAiButNeverPositionDom() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .contains("/api/dashboard/home?")
                .contains("selectedSymbol")
                .contains("limit: String(MOBILE_ASSET_LIMIT)")
                .contains("updateExecution(envelope.data.executionSuggestion)")
                .contains("updateAi(envelope.data.aiDecision)")
                .contains("failClosedAfterLoadError")
                .contains("requestSequence")
                .contains("AbortController")
                .doesNotContain("position-list")
                .doesNotContain("data-position-independent")
                .doesNotContain("innerHTML");
    }

    @Test
    void interactionsCoverPagerRolesAndHomeResetAccessibly() throws Exception {
        String source = Files.readString(SCRIPT) + Files.readString(TEMPLATE);

        assertThat(source)
                .contains("ArrowRight")
                .contains("ArrowLeft")
                .contains("aria-checked")
                .contains("aria-selected")
                .contains("activateRole")
                .contains("window.scrollTo({ top: 0")
                .contains("title.focus()")
                .contains("/review/dashboard");
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
                .contains("scroll-snap-type: x mandatory")
                .doesNotContain("transform: scale")
                .doesNotContain("font-size: clamp")
                .doesNotContain("url(http")
                .doesNotContain("cdn");
    }

    private int count(String source, String target) {
        return (source.length() - source.replace(target, "").length()) / target.length();
    }
}
