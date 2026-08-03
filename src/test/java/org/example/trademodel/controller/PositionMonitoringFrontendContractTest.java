package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class PositionMonitoringFrontendContractTest {
    private static final Path TEMPLATE =
            Path.of("src/main/resources/templates/position-monitoring.html");
    private static final Path SCRIPT =
            Path.of("src/main/resources/static/js/position-monitoring.js");
    private static final Path STYLES =
            Path.of("src/main/resources/static/css/position-monitoring.css");
    private static final Path HOME_MOBILE =
            Path.of("src/main/resources/templates/dashboard-mobile.html");
    private static final Path HOME_DESKTOP =
            Path.of("src/main/resources/templates/dashboard.html");

    @Test
    void mobileAndDesktopShareTheFrozenPositionMonitoringSemantics() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains(
                        "data-mobile-view=${mobileView}",
                        "持仓生命周期",
                        "User Position Card",
                        "Position Monitoring Card",
                        "用户入场价",
                        "开仓时间",
                        "入场逻辑仍有效",
                        "方向支持",
                        "反转状态",
                        "风险等级",
                        "监控结论",
                        "当前建议",
                        "最近监控时间",
                        "监控记录")
                .contains(
                        "data-lifecycle=\"OPEN\"",
                        "data-lifecycle=\"PARTIALLY_CLOSED\"",
                        "data-lifecycle=\"CLOSED\"")
                .contains(
                        "href=\"/dashboard/positions\" class=\"active\"",
                        "href=\"/dashboard/mobile/positions\" class=\"active\"");
    }

    @Test
    void exactPositionIdentityGuardsSummaryDetailAndMonitorLogs() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .contains(
                        "var normalized = String(value || \"\").trim()",
                        "/^[1-9][0-9]{0,18}$/.test(normalized)",
                        "samePositionId(position.positionId, selectedId)",
                        "\"/api/user-positions/\" + encodeURIComponent(selectedId)",
                        "samePositionId(positionDetail.id, selectedId)",
                        "normalizeSymbol(positionDetail.assetSymbol)",
                        "normalizeSymbol(selectedSummary && selectedSummary.symbol)",
                        "\"/api/review/positions/\" + encodeURIComponent(selectedId)",
                        "samePositionId(log.positionId, selectedId)",
                        "\"POSITION_IDENTITY_MISMATCH\"",
                        "\"&positionId=\" + encodeURIComponent(request.positionId)")
                .doesNotContain(
                        "Number(positionId)",
                        "Number(selectedId)",
                        "parseInt(",
                        "parseFloat(",
                        "latestPosition",
                        "latestBySymbol",
                        "positionBySymbol",
                        "localStorage",
                        "sessionStorage");
    }

    @Test
    void frontendUsesOnlyExistingOwnerScopedReadsAndNeverInvokesMonitorWrites() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .contains(
                        "\"/api/dashboard/home?limit=20\"",
                        "\"/api/user-positions/\"",
                        "\"/api/review/positions/\"",
                        "var options = { method: \"GET\", credentials: \"same-origin\" }")
                .doesNotContain(
                        "method: \"POST\"",
                        "/api/position-monitor/",
                        "/manual-open",
                        "/manual-close",
                        "setInterval(",
                        "WebSocket(",
                        "EventSource(");
    }

    @Test
    void persistentMonitorStatesStayClosedAndWaitingMonitorIsUiOnly() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(script)
                .contains(
                        "var LIFECYCLES = [\"OPEN\", \"PARTIALLY_CLOSED\", \"CLOSED\"]",
                        "\"LOGIC_VALID\"",
                        "\"LOGIC_WEAKENED\"",
                        "\"PLAN_INVALIDATED\"",
                        "\"HIGH_RISK\"",
                        "return \"WAITING_MONITOR\"",
                        "renderMonitorUnavailable()",
                        "\"MONITOR_DATA_UNAVAILABLE\"",
                        "reconcileMonitorSummary(selectedSummary, logs)",
                        "renderMonitor(position, true)")
                .doesNotContain(
                        "\"WAITING_MONITOR\",\n  ];",
                        "monitorStatus = \"WAITING_MONITOR\"",
                        "position.monitorStatus =");
    }

    @Test
    void allReadStatesFailClosedWithoutFabricatedMonitoringOrPlanData() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains(
                        "正在读取持仓",
                        "暂无手动持仓",
                        "当前不可查看",
                        "等待首次监控",
                        "重新加载",
                        "监控记录当前不可查看",
                        "关联执行计划",
                        "只读参考",
                        "建议仅供人工复核")
                .doesNotContain(
                        "添加持仓",
                        "编辑持仓",
                        "记录平仓",
                        "部分平仓",
                        "运行监控",
                        "买入",
                        "卖出",
                        "下单");
        assertThat(script)
                .contains(
                        "\"LOAD_FAILED\"",
                        "\"INVALID_POSITION_ID\"",
                        "\"当前不可查看\"",
                        "\"待同步\"",
                        "\"--\"",
                        "showMonitorLogError()",
                        "retry.addEventListener(\"click\", loadPositionMonitoring)",
                        "logRetry.addEventListener(\"click\", retryMonitorLogs)")
                .doesNotContain(
                        "executionSuggestion",
                        "systemSuggestedStopLoss",
                        "systemSuggestedTakeProfit");
    }

    @Test
    void homeNavigationActivatesImplementedPositionAndAiDestinations() throws Exception {
        String mobile = Files.readString(HOME_MOBILE);
        String desktop = Files.readString(HOME_DESKTOP);

        assertThat(mobile)
                .contains("<a href=\"/dashboard/mobile/positions\" data-position-nav>持仓</a>")
                .contains("<button type=\"button\" data-ai-nav>AI分析</button>")
                .contains("data-message-nav data-unavailable-nav")
                .contains("data-profile-nav data-unavailable-nav")
                .doesNotContain("data-ai-nav data-unavailable-nav");
        assertThat(desktop)
                .contains("<a href=\"/dashboard/positions\" class=\"product-nav-item\">持仓</a>")
                .contains("data-desktop-ai-nav>AI 分析</a>")
                .contains("data-desktop-unavailable-nav aria-disabled=\"true\">消息")
                .contains("data-desktop-unavailable-nav aria-disabled=\"true\">我的")
                .doesNotContain("data-desktop-unavailable-nav aria-disabled=\"true\">AI 分析");
    }

    @Test
    void responsiveStylesPreserveMobileAndDesktopFigmaBaselines() throws Exception {
        String css = Files.readString(STYLES);

        assertThat(css)
                .contains(
                        "width: min(100%, 430px)",
                        "grid-template-columns: repeat(5, minmax(0, 1fr))",
                        "min-height: 44px",
                        "overflow-x: hidden",
                        "env(safe-area-inset-top)",
                        "env(safe-area-inset-bottom)",
                        "@media (min-width: 760px)",
                        "grid-template-columns: 224px minmax(0, 1fr)",
                        "width: min(calc(100% - 64px), 1152px)",
                        ":root[data-mobile-text-size=\"accessibility\"]",
                        "--position-root-font-size: 20.8px",
                        "outline: 3px solid var(--focus-ring)")
                .doesNotContain(
                        "font-size: clamp",
                        "transform: scale(",
                        "letter-spacing: -",
                        "url(http",
                        "cdn");
    }
}
