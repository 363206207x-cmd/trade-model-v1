package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class GlobalFrozenUiAlignmentContractTest {
    private static final Path HOME = Path.of("src/main/resources/templates/home.html");
    private static final Path HOME_CSS = Path.of("src/main/resources/static/css/home.css");
    private static final Path HOME_JS = Path.of("src/main/resources/static/js/home-runtime.js");
    private static final Path WORKSPACE = Path.of("src/main/resources/templates/workspace.html");
    private static final Path WORKSPACE_CSS = Path.of("src/main/resources/static/css/workspace.css");
    private static final Path WORKSPACE_JS = Path.of("src/main/resources/static/js/workspace.js");

    @Test
    void shellAndTokensMatchTheAugustTwentiethFreeze() throws Exception {
        String tokens = "--canvas: #F7F8FA;\n"
                + "    --surface: #FFFFFF;\n"
                + "    --surface-subtle: #F2F4F7;\n"
                + "    --border: #DDE2E8;\n"
                + "    --text-primary: #101828;\n"
                + "    --text-secondary: #475467;\n"
                + "    --text-muted: #667085;\n"
                + "    --accent: #175CD3;\n"
                + "    --positive: #067647;\n"
                + "    --warning: #B54708;\n"
                + "    --negative: #B42318;\n"
                + "    --unknown: #667085;";
        assertThat(Files.readString(HOME_CSS)).contains(tokens, "width: 64px", "height: 52px", "padding: 0 24px");
        assertThat(Files.readString(WORKSPACE_CSS)).contains(tokens, "width: 64px", "width: 216px", "height: 52px", "padding: 0 24px");
    }

    @Test
    void homeHasOneRuntimeExactStatusScopesAndNoLegacyRatio() throws Exception {
        String home = Files.readString(HOME);
        String workspace = Files.readString(WORKSPACE);
        assertOrdered(home, "<small>环境</small>", "<small>系统</small>", "<small>数据</small>",
                "<small>服务</small>", "<small>账户·已录入</small>", "<small>Hot Reset</small>");
        assertThat(home).contains("持仓监控 · 基于已录入", "Final Execution Plan", "AI 分析工作区")
                .doesNotContain("60:40", "机会数量");
        assertThat(workspace).doesNotContain("pageKey == 'home'", "60:40");
    }

    @Test
    void topSixIsDefensiveDedupedAndUsesEachCardsFinal() throws Exception {
        String script = Files.readString(HOME_JS);
        assertThat(script).contains(
                "[\"CANDIDATE\", \"WAITING_TRIGGER\", \"TRIGGERED\"]",
                "mode !== \"BLOCKED\"", "[\"HIGH\", \"EXTREME\"]",
                "var seen = new Set()", "seen.has(identity)", ".slice(0, 6)",
                "asset.hasFinal === true", "asset.finalMarketBias", "asset.finalPlanMode",
                "asset.primaryTimeframe", "asset.timeframeConflictState", "asset.rankingReason",
                "revalidating ? \"正在重验\"", "DEFAULT_SLOT")
                .doesNotContain("const assets = [");
    }

    @Test
    void responsiveGeometryAndPositionSemanticsAreFrozen() throws Exception {
        String css = Files.readString(HOME_CSS);
        String script = Files.readString(HOME_JS);
        assertThat(css).contains(
                "grid-template-columns: repeat(6, minmax(0, 1fr))",
                "@container (max-width: 1239px)",
                "grid-template-columns: repeat(3, minmax(0, 1fr))",
                "grid-template-columns: minmax(0, 7fr) minmax(320px, 3fr)",
                "@container (max-width: 1119px)", ".plan-module { order: 1; }", ".position-module { order: 2; }",
                "grid-template-columns: minmax(0,22fr) minmax(0,28fr) minmax(0,28fr) minmax(0,22fr)");
        assertThat(script + css).contains(
                "monitorTrustState || \"SOURCE_UNAVAILABLE\"", "trust === \"VERIFIED_FRESH\"",
                "position.entryPrice", "position.openedAt", "position.riskTrend",
                "position.entryLogicStatus", "position.monitorConclusion",
                "PENDING", "STALE", "INVALID", "SOURCE_UNAVAILABLE",
                "query.set(\"positionId\", requestedPositionId)",
                "function semanticTone(value)", "tone-positive", "tone-warning", "tone-negative", "tone-unknown")
                .doesNotContain("position.riskReason", "position.lastMonitorTime");
    }

    @Test
    void planAndThreeAiUseSourceOwnedSemantics() throws Exception {
        String script = Files.readString(HOME_JS);
        assertThat(script).contains(
                "plan-status-layer", "plan-key-layer", "plan-metadata-layer", "plan.planVersion",
                "plan.planLifecycleState", "plan.revalidationReason", "plan.revalidationRule",
                "[\"APPROVE\", \"DOWNGRADE\", \"REJECT\", \"RISK_WARNING\"]",
                "return roleUnavailable(role)", "触发 → 演化 → 失效",
                "consistency.conflictLevel", "consistency.mainReason",
                "ArrowRight", "ArrowLeft", "event.key === \"Home\"", "event.key === \"End\"",
                "item.tabIndex = selected ? 0 : -1");
    }

    @Test
    void primaryPagesFollowFrozenIaAndExposeNoTelegramUi() throws Exception {
        String html = Files.readString(WORKSPACE);
        String script = Files.readString(WORKSPACE_JS);
        assertOrdered(html, "持仓监控 · 基于已录入", "账户风险覆盖 · 基于已录入", "活动持仓",
                "positionStateFilter", "positionSort", "positionGrid");
        assertOrdered(html, "analysisAssetSearch", "analysisMode", "analysisDataQuality", "analysisTimeframes",
                "analysisEvidence", "analysisScores", "AIRoleTabs");
        assertOrdered(html, "消息中心", "messageUnreadCount", "OPPORTUNITY_PLAN", "POSITION_RISK", "SYSTEM", "messageList");
        assertOrdered(html, "settings-anchors", "risk-preference", "asset-pool-sources");
        assertThat(html + script).contains("开始预览", "positionRows", "renderPositionRows", "data-message-group")
                .doesNotContain("Telegram", "telegram", "data-retry-telegram", "ChannelDeliveryStatus", "O10",
                        "async function loadTasks() {\n    async function loadTasks() {");
    }

    private static void assertOrdered(String source, String... markers) {
        int cursor = -1;
        for (String marker : markers) {
            int next = source.indexOf(marker, cursor + 1);
            assertThat(next).as("marker %s after %s", marker, cursor).isGreaterThan(cursor);
            cursor = next;
        }
    }
}
