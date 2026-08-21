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
    private static final Path OWNER_BROWSER_QA = Path.of(
            "docs/evidence/global_ui_alignment/owner_blocker_closure/browser-qa.json");
    private static final Path RESIDUAL_P0_BROWSER_QA = Path.of(
            "docs/evidence/global_ui_alignment/owner_blocker_closure/residual-p0-browser-qa.json");
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
        assertThat(home).contains("持仓监控 · 基于已录入", "最终执行计划", "AI 分析工作区")
                .doesNotContain("60:40", "机会数量");
        assertThat(workspace).doesNotContain("pageKey == 'home'", "60:40");
    }

    @Test
    void topSixIsDefensiveDedupedAndUsesEachCardsFinal() throws Exception {
        String script = Files.readString(HOME_JS);
        assertThat(script).contains(
                "[\"CANDIDATE\", \"WAITING_TRIGGER\", \"TRIGGERED\", \"HIGH_RISK\"]",
                "mode !== \"BLOCKED\"",
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
                "function positionDetailLink(positionId)", "contract.positionSourceLabel",
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
                "plan.stopZone || plan.stopLoss",
                "[\"APPROVE\", \"DOWNGRADE\", \"REJECT_CANDIDATE\", \"RISK_WARNING\"]",
                "GPT 综合判断 · 非最终计划", "Gemini 冲突复核", "Grok 反方挑战",
                "return roleUnavailable(role)", "触发 → 演化 → 失效",
                "consistency.conflictLevel", "consistency.mainReason",
                "ArrowRight", "ArrowLeft", "event.key === \"Home\"", "event.key === \"End\"",
                "item.tabIndex = selected ? 0 : -1");
        assertThat(Files.readString(HOME_CSS)).contains(
                ".tone-positive", ".tone-warning", ".tone-negative", ".tone-info",
                ".position-judgment { align-items: center; text-align: center; }");
    }

    @Test
    void residualP0HomeContractsKeepOwnershipAndRemoveLocalDerivativesProjection() throws Exception {
        String home = Files.readString(HOME);
        String script = Files.readString(HOME_JS);
        String workspace = Files.readString(WORKSPACE);
        String contract = Files.readString(Path.of("src/main/resources/static/js/frontend-contract.js"));

        assertThat(home + workspace)
                .contains(">分析</span>", "GPT 综合判断", "Gemini 冲突复核", "Grok 反方挑战")
                .doesNotContain("Decision Workspace", ">AI 分析</span>", "GPT 候选判断", "Gemini 可信度复核");
        assertThat(script)
                .contains(
                        "statusValue(state.marketTrend)", "statusValue(state.riskLevel)",
                        "statusValue(state.dataQuality)", "statusValue(state.serviceAvailability",
                        "statusValue(state.accountStatus", "statusValue(state.hotReset",
                        "GPT 综合判断 · 非最终计划", "方向判断", "机会进度", "候选参与方式",
                        "复核结果：", "completeFailurePath", "已发现可验证失败路径",
                        "未发现可验证失败路径", "机会状态与候选参与方式不一致",
                        "复核前后状态与等待触发生命周期不一致",
                        "function candidateConclusion(summary)", "当前一句话结论不可查看")
                .doesNotContain("renderDerivatives", "ai-derivatives-strip", "维持 Candidate",
                        "GPT Candidate · 非 Final", "Market Bias：", "Candidate Mode");
        assertThat(contract).contains(
                "HIGH_RISK: Object.freeze({ label: \"高风险\"",
                "function positionSourceLabel(sourceType)",
                "return \"系统计划\"", "return \"独立录入\"",
                "label: \"状态待同步\"")
                .doesNotContain("label: displayText(returnedLabel, \"状态待同步\")");
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
                .doesNotContain("SystemStatusBar", "workspace-system-status", "workspaceDataStatus", "workspaceAiStatus",
                        "Telegram", "telegram", "data-retry-telegram", "ChannelDeliveryStatus", "O10",
                        "async function loadTasks() {\n    async function loadTasks() {");
    }

    @Test
    void ownerVisualBlockersHaveExplicitFailClosedDomContracts() throws Exception {
        String home = Files.readString(HOME);
        String homeScript = Files.readString(HOME_JS);
        String homeCss = Files.readString(HOME_CSS);
        String workspace = Files.readString(WORKSPACE);
        String workspaceScript = Files.readString(WORKSPACE_JS);
        String workspaceCss = Files.readString(WORKSPACE_CSS);

        assertThat(homeScript).contains(
                "<small>最终偏向</small>", "<small>计划模式</small>",
                "PENDING: \"等待监控数据\"", "STALE: \"监控数据已过期\"",
                "INVALID: \"当前不可查看\"", "SOURCE_UNAVAILABLE: \"监控来源不可用\"",
                "position-trust-state", "is-untrusted")
                .doesNotContain("Final Market Bias", "Final Plan Mode", "等待触发后进入人工确认");
        assertThat(homeCss).contains(
                ".opportunity-final b { min-width: 0; white-space: nowrap;",
                ".system-status-strip strong { min-width: 0;",
                ".position-trust-state { grid-column: 3 / 5;")
                .doesNotContain(".opportunity-final b { overflow: hidden", ".system-status-strip strong { overflow: hidden");
        assertThat(workspace).contains(
                "暂无需要处理的高价值消息",
                "新的机会升级、计划安全变化、持仓重大风险和系统异常会显示在这里",
                "id=\"saveSettings\" hidden disabled>保存更改",
                "id=\"requestPlanRevalidation\" hidden disabled")
                .doesNotContain("workspace-system-status", "Telegram", "通知设置");
        assertThat(workspaceScript).contains(
                "position-untrusted-state", "monitorUnavailableText",
                "taskIndicator.hidden = activeTaskCount === 0",
                "save.hidden = !dirty", "save.classList.toggle(\"is-dirty\", dirty)",
                "[\"CURRENT\", \"NEEDS_REVALIDATION\"].includes(lifecycle)",
                "仅允许展示通过规则校验的 Final，不使用 Candidate 替代")
                .doesNotContain("loadWorkspaceStatus()");
        assertThat(workspaceCss).contains(
                "background: #111827", "width: 216px", ".empty-state .button { align-self: flex-start; width: auto; }");
        assertThat(home + workspace).contains("/icons/app-shell.svg#home", "/icons/app-shell.svg#logout")
                .doesNotContain("⌂", "▦", "✦", "▤", "↪");
    }

    @Test
    void ownerBrowserEvidenceLocksRuntimeVisibilityAndLifecycleGates() throws Exception {
        String qa = Files.readString(OWNER_BROWSER_QA);
        String residual = Files.readString(RESIDUAL_P0_BROWSER_QA);

        assertThat(qa).contains(
                "\"browserStatus\": \"PASS\"",
                "\"criticalTextClipping\": 0",
                "\"accountSummaryFits\": true",
                "\"accountSummaryTextOverflow\": \"clip\"",
                "\"stateText\": \"等待监控数据\"",
                "\"stateText\": \"监控数据已过期\"",
                "\"stateText\": \"当前不可查看\"",
                "\"stateText\": \"监控来源不可用\"",
                "\"markPriceVisible\": false",
                "\"pnlVisible\": false",
                "\"systemStatusBarCount\": 0",
                "\"inactiveTaskIndicatorHidden\": true",
                "\"collapsedRailVisibleLabelCount\": 0",
                "\"candidateUsedAsFinal\": false",
                "\"oldCopyCount\": 0",
                "\"consoleErrors\": 0",
                "\"textClippingCount\": 0");
        assertThat(qa).contains(
                "\"clean\": {\"hidden\": true, \"disabled\": true, \"dirtyClass\": false}",
                "\"dirty\": {\"hidden\": false, \"disabled\": false, \"dirtyClass\": true",
                "\"CURRENT\": {\"revalidationHidden\": false, \"revalidationDisabled\": false",
                "\"NEEDS_REVALIDATION\": {\"revalidationHidden\": false, \"revalidationDisabled\": false",
                "\"UNAVAILABLE\": {\"revalidationHidden\": true, \"revalidationDisabled\": true");
        assertThat(residual).contains(
                "\"horizontalOverflowCount\": 0",
                "\"textClippingCount\": 0",
                "\"statusCount\": 6",
                "\"homeDerivativesStripCount\": 0",
                "\"visibleDecisionWorkspaceCount\": 0",
                "\"brokenPositionDetailLinkCount\": 0",
                "\"systemOwnedSlotsUnchanged\": true",
                "\"riskLevelStateSlotLeakageCount\": 0",
                "\"FOUND_EMPTY\": {\"failClosed\": true, \"foundCopyCount\": 0, \"notFoundCopyCount\": 0}");
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
