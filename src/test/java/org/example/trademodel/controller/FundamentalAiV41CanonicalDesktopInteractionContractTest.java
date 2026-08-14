package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class FundamentalAiV41CanonicalDesktopInteractionContractTest {
    private static final Path WORKSPACE = Path.of("src/main/resources/templates/workspace.html");
    private static final Path WORKSPACE_JS = Path.of("src/main/resources/static/js/workspace.js");
    private static final Path WORKSPACE_CSS = Path.of("src/main/resources/static/css/workspace.css");
    private static final Path DASHBOARD = Path.of("src/main/resources/templates/dashboard.html");
    private static final Path DASHBOARD_CSS = Path.of("src/main/resources/static/css/dashboard-latest.css");
    private static final Path ROUTES = Path.of(
            "src/main/java/org/example/trademodel/controller/DesktopWorkspaceController.java");

    @Test
    void allFourteenCanonicalDesktopPagesHaveStableAuthenticatedRoutes() throws Exception {
        String routes = Files.readString(ROUTES);
        String workspace = Files.readString(WORKSPACE);

        assertThat(routes).contains(
                "@GetMapping(\"/asset-pool\")",
                "@GetMapping(\"/positions\")",
                "@GetMapping(\"/positions/{positionId}\")",
                "@GetMapping(\"/reviews\")",
                "@GetMapping(\"/reviews/{reviewId}\")",
                "@GetMapping(\"/analysis\")",
                "@GetMapping(\"/analysis/{analysisId}\")",
                "@GetMapping(\"/messages\")",
                "@GetMapping(\"/recheck/{pushSnapshotId}\")",
                "@GetMapping(\"/plans/{planId}\")",
                "@GetMapping(\"/calendar\")",
                "@GetMapping(\"/audit/{traceId}\")",
                "@GetMapping(\"/me\")");
        assertThat(Files.readString(Path.of(
                "src/main/java/org/example/trademodel/controller/LoginController.java")))
                .contains("@GetMapping(\"/login\")");
        assertThat(Files.readString(Path.of(
                "src/main/java/org/example/trademodel/controller/DashboardController.java")))
                .contains("@GetMapping(\"/dashboard\")");
        assertThat(workspace).contains(
                "pageKey == 'asset-pool'", "pageKey == 'positions'", "pageKey == 'position-detail'",
                "pageKey == 'reviews'", "pageKey == 'review-detail'", "pageKey == 'analysis'",
                "pageKey == 'messages'", "pageKey == 'recheck'", "pageKey == 'plan'",
                "pageKey == 'calendar'", "pageKey == 'audit'", "pageKey == 'me'");
    }

    @Test
    void allElevenSharedOverlaysAreNativeAccessibleDialogs() throws Exception {
        String html = Files.readString(WORKSPACE);
        Set<String> overlayCodes = captures(html, "data-overlay-code=\"(O\\d{2})\"");

        assertThat(overlayCodes).containsExactlyInAnyOrder(
                "O01", "O02", "O03", "O04", "O05", "O06",
                "O07", "O08", "O09", "O10", "O11");
        assertThat(count(html, "<dialog class=\"overlay")).isEqualTo(11);
        assertThat(Files.readString(WORKSPACE_JS)).contains(
                "dialog.showModal()", "dialog.close()", "restoreFocus",
                "dialog.addEventListener(\"cancel\"", "focus()");
    }

    @Test
    void allFiftyFourCanonicalComponentFamiliesAreRepresentedWithoutDetachedMarkup() throws Exception {
        String html = Files.readString(WORKSPACE);
        Set<String> families = captures(html, "data-component-family=\"([^\"]+)\"");

        assertThat(families).hasSize(54).contains(
                "AppShell", "SideNav", "PageHeader", "SystemStatusBar", "StateBadge",
                "EmptyState", "AsyncTaskIndicator", "Drawer", "Modal", "AuditMetaDisclosure",
                "AssetSearch", "SearchResultItem", "AssetPoolToolbar", "AssetPoolTable",
                "PoolScanStatus", "OpportunityGrid", "OpportunityCard", "MultiTimeframeSummary",
                "DataQualityGate", "PlanSummaryCard", "PlanModeHeader", "PlanLifecycleBadge",
                "EntryTriggerSection", "InvalidationStopSection", "TargetTrendSection",
                "RiskLimitSection", "FinalPlanDetail", "AnalysisModeBanner", "AIWorkspace",
                "AIRoleTabs", "EvidenceList", "MultiTimeframeMatrix", "BeforeAfterDiff",
                "FailurePathList", "ConflictSummary", "PositionRiskAggregate", "PositionCard",
                "PositionActualForm", "PlanActualComparison", "MonitorTimeline", "ReviewCard",
                "AtTimeLaterCompare", "ResponsibilityChain", "MessageListItem",
                "ChannelDeliveryStatus", "OriginalSnapshotCard", "RecheckResultHero",
                "RecheckActionBar", "EventCalendar", "EventWindowBadge", "TelegramBindingPanel",
                "RiskPreferenceForm", "ProviderStatusPanel", "AuditChainStepper");
        assertThat(html).doesNotContain("data-detached-instance=\"true\"");
    }

    @Test
    void desktopHomeUsesFrozenProportionsAndDynamicTopSixWithoutFakeMarketVisuals() throws Exception {
        String html = Files.readString(DASHBOARD);
        String css = Files.readString(DASHBOARD_CSS);

        assertThat(css).contains(
                "--tmv1-sidebar-width: 224px",
                "grid-template-columns: repeat(3, minmax(0, 1fr))",
                "grid-template-columns: minmax(0, 3fr) minmax(360px, 2fr)",
                "grid-template-columns: minmax(0, 19fr) minmax(240px, 6fr)");
        assertThat(html).contains(
                "data-position-execution-ratio=\"60:40\"",
                "asset.primaryOpportunityId",
                "asset.primaryTimeframe",
                "asset.primaryPlanMode",
                "asset.secondaryOpportunityCount",
                "asset.timeframeConflictState",
                "restoreDashboardSelectionFromUrl()",
                "frontendContract.replaceUrlParam(\"asset\"")
                .doesNotContain("模拟K线", "模拟走势", "mini-chart", "sparkline");
    }

    @Test
    void workspaceRuntimeUsesRealApisFailClosedStatesAndManualTradingBoundaries() throws Exception {
        String html = Files.readString(WORKSPACE);
        String script = Files.readString(WORKSPACE_JS);
        String css = Files.readString(WORKSPACE_CSS);

        assertThat(script).contains(
                "/api/asset-pool", "/api/dashboard/home?limit=6", "/api/user-positions/open",
                "/api/review/center", "/api/ai/audit-chain", "/api/workspace/messages",
                "/api/workspace/rechecks/", "/api/workspace/plans/", "/api/workspace/events",
                "/api/user-config", "等待监控数据", "当前不可查看", "暂无数据",
                "notTradeInstruction")
                .doesNotContain("AUTO_OPEN", "AUTO_CLOSE", "AUTO_REVERSE", "AUTO_ORDER");
        assertThat(html).contains(
                "录入实际持仓", "记录平仓", "Preview 不创建机会")
                .doesNotContain("自动开仓", "自动平仓", "自动反手", "自动下单");
        assertThat(css).contains("overflow-x: hidden", ":focus-visible");
    }

    private static Set<String> captures(String source, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(source);
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        while (matcher.find()) values.add(matcher.group(1));
        return values;
    }

    private static int count(String source, String token) {
        int total = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            total++;
            index += token.length();
        }
        return total;
    }
}
