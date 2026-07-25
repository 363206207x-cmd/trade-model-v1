package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class FrontendImplementationFoundationContractTest {
    private static final Path CONTRACT =
            Path.of("src/main/resources/static/js/frontend-contract.js");
    private static final Path DESKTOP =
            Path.of("src/main/resources/templates/dashboard.html");
    private static final Path MOBILE =
            Path.of("src/main/resources/templates/dashboard-mobile.html");
    private static final Path MOBILE_SCRIPT =
            Path.of("src/main/resources/static/js/dashboard-mobile.js");

    @Test
    void sharedFoundationFreezesRoleOrderAndAssetStateSemantics() throws Exception {
        String source = Files.readString(CONTRACT);

        assertThat(source.indexOf("GPT_FINAL")).isLessThan(source.indexOf("GEMINI_REVIEW"));
        assertThat(source.indexOf("GEMINI_REVIEW")).isLessThan(source.indexOf("GROK_CHALLENGE"));
        assertThat(source).contains(
                "OBSERVING", "CANDIDATE", "WAITING_TRIGGER", "TRIGGERED",
                "HIGH_RISK", "INVALIDATED", "COOLING", "CONFUSED",
                "条件已触发，不代表已开仓");
        assertThat(source)
                .contains("normalizeAiTabs", "displayNumber", "parseApiEnvelope")
                .doesNotContain("localStorage", "OPENED", "EXECUTED");
    }

    @Test
    void sharedPlanGuardFailsClosedBeforeAnyPlanBoundaryCanRender() throws Exception {
        String source = Files.readString(CONTRACT);
        int identityGate = source.indexOf("!hasText(plan.sourceExecutionPlanId)");
        int positionIdentityGate = source.indexOf("plan.originalPlanIdentity", identityGate);
        int currentValidityGate = source.indexOf("plan.originalPlanCurrentValidity", positionIdentityGate);
        int visiblePlan = source.indexOf("visible: true", currentValidityGate);

        assertThat(identityGate).isGreaterThanOrEqualTo(0);
        assertThat(positionIdentityGate).isGreaterThan(identityGate);
        assertThat(currentValidityGate).isGreaterThan(positionIdentityGate);
        assertThat(visiblePlan).isGreaterThan(currentValidityGate);
        assertThat(source).contains("计划来源不可验证", "仅用于历史复核");
    }

    @Test
    void bothOverviewProjectionsUseSharedContractAndExistingHomeApi() throws Exception {
        String desktop = Files.readString(DESKTOP);
        String mobile = Files.readString(MOBILE);
        String mobileScript = Files.readString(MOBILE_SCRIPT);
        String desktopRefresh = sourceSlice(
                desktop, "function refreshDashboard()", "function refreshDashboardDiagnostics()");

        assertThat(desktop)
                .contains("/js/frontend-contract.js")
                .contains("frontendContract.parseApiEnvelope")
                .contains("/api/dashboard/home")
                .contains("syncDashboardSelectionUrl")
                .contains("restoreDashboardSelectionFromUrl");
        assertThat(mobile)
                .contains("/js/frontend-contract.js")
                .contains("data-position-independent")
                .contains("首页", "持仓", "复盘");
        assertThat(mobileScript)
                .contains("frontendContract.parseApiEnvelope")
                .contains("frontendContract.executionPlanAccess")
                .contains("frontendContract.normalizeAiTabs")
                .doesNotContain("localStorage");
        assertThat(desktopRefresh)
                .contains("fetchDashboardHome()", "renderDashboardHomeUnavailable()")
                .doesNotContain(
                        "fetchLocalRealPipelineStatus", "fetchProviderRuntimeStatus",
                        "requestDetailForSelectedSymbol", "refreshDashboardDiagnostics");
        assertThat(mobileScript)
                .containsOnlyOnce("fetch(")
                .contains("fetch(\"/api/dashboard/home?\" + query.toString()");
    }

    @Test
    void overviewExposesNoTradeExecutionOrAiVotingControl() throws Exception {
        String desktop = Files.readString(DESKTOP);
        String mobile = Files.readString(MOBILE);
        String mobileScript = Files.readString(MOBILE_SCRIPT);

        assertThat(mobile + mobileScript)
                .doesNotContain("/api/order", "/api/trade", "AI 投票", "投票比例")
                .doesNotContain(">买入</button>", ">卖出</button>", ">下单</button>", ">执行交易</button>");
        assertThat(desktop)
                .contains("系统执行建议（非交易指令）")
                .contains("不会生成真实持仓或交易执行")
                .doesNotContain("AI 投票", "投票比例");
    }

    private String sourceSlice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
