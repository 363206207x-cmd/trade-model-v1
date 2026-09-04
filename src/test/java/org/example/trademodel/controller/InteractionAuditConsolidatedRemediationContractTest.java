package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionAuditConsolidatedRemediationContractTest {
    private static final Path HOME = Path.of("src/main/resources/templates/home.html");
    private static final Path HOME_SCRIPT = Path.of("src/main/resources/static/js/home-runtime.js");
    private static final Path HOME_CSS = Path.of("src/main/resources/static/css/home.css");
    private static final Path HTML = Path.of("src/main/resources/templates/workspace.html");
    private static final Path SCRIPT = Path.of("src/main/resources/static/js/workspace.js");
    private static final Path CSS = Path.of("src/main/resources/static/css/workspace.css");

    @Test
    void o01AndO10ExposeTruthfulOwnerOnlyStatusWithoutSideEffectsOrSecrets() throws Exception {
        String html = Files.readString(HTML);
        String script = Files.readString(SCRIPT);

        assertThat(html).contains(
                "data-open-overlay=\"status-recovery\"", "data-overlay-code=\"O01\"",
                "data-overlay-code=\"O10\"", "Telegram 通知", "通道测试、非交易指令",
                "aria-controls=\"overlay-status-recovery\"");
        assertThat(script).contains(
                "上次成功完成", "下次计划扫描", "恢复条件", "不会自动重启",
                "maskedChatIdentity", "latestValidationAt", "latestTestState",
                "真实测试发送门禁未开启")
                .doesNotContain("TELEGRAM_BOT_TOKEN", "TELEGRAM_CHAT_ID", "botToken", "chatId");
    }

    @Test
    void responsiveExistingDesktopSurfaceCannotForceRootOverflowAt390() throws Exception {
        String css = Files.readString(CSS);
        String homeCss = Files.readString(HOME_CSS);
        assertThat(css).contains(
                "@media (max-width: 767px)",
                "html { min-width: 0; }",
                "body { min-width: 0; padding-left: 0; overflow-x: clip; }",
                ".table-shell { max-width: 100%; overflow-x: auto;",
                ".position-card.position-row { grid-template-columns: 1fr;",
                ".overlay, .overlay.drawer",
                "width: calc(100vw - 16px);");
        assertThat(homeCss).contains(
                "@media (max-width: 900px)",
                "html, body { min-width: 0; width: 100%; }",
                ".opportunity-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }",
                ".decision-row { grid-template-columns: minmax(0, 1fr);",
                ".position-row { grid-template-columns: minmax(0, 1fr);",
                ".home-dialog, .home-dialog.compact { width: calc(100vw - 24px);")
                .contains("@media (max-width: 520px)",
                        ".system-status-strip, .opportunity-grid { grid-template-columns: minmax(0, 1fr); }");
    }

    @Test
    void dashboardStatusEntryOpensAReadOnlyRealRuntimeSurface() throws Exception {
        String home = Files.readString(HOME);
        String script = Files.readString(HOME_SCRIPT);

        assertThat(home).contains(
                "data-open-home-status", "id=\"homeStatusDialog\"", "data-overlay-code=\"O01\"",
                "只读显示真实运行状态", "不会自动重启、刷新外部数据或触发调度");
        assertThat(script).contains(
                "openHomeStatus", "/api/system/runtime-readiness-guardrail-status",
                "调度心跳", "上次成功完成", "下次计划扫描", "恢复条件",
                "此面板不执行恢复动作")
                .doesNotContain("hot-reset", "restart", "triggerScan");
    }

    @Test
    void batchSelectionAnalysisAuditAndMessagesUseReadableSafeContracts() throws Exception {
        String html = Files.readString(HTML);
        String script = Files.readString(SCRIPT);
        assertThat(html).contains("poolBatchSelectAll", "全选当前资产", "role=\"status\"");
        assertThat(script).contains(
                "selectAll.indeterminate", "确认扫描所选", "不会创建持仓或执行交易",
                "1 小时机会质量", "4 小时趋势一致性", "方向或置信度不完整，已按待重新分析处理",
                "Market Data", "AnalysisRun", "Direction", "Opportunity / Candidate",
                "Rule Validation", "没有强制生成最终计划", "复制完整 ID",
                "humanReason(item.body || item.reason")
                .doesNotContain("multiTimeframeStates || analysisAudit.decisionBundle?.multiTimeframeState");
    }

    @Test
    void onDemandAnalysisUsesStableSubmissionAndRecoversCanonicalTaskIdentity() throws Exception {
        String homeScript = Files.readString(HOME_SCRIPT);
        String workspaceScript = Files.readString(SCRIPT);

        assertThat(homeScript).contains("sessionStorage", "analysis-preview:", "submissionId", "taskId");
        assertThat(workspaceScript).contains("sessionStorage", "analysis-preview:", "submissionId",
                "taskId", "/api/workspace/tasks?limit=30");
        assertThat(homeScript).contains("clearAnalysisPreview(previewSymbol)");
        assertThat(workspaceScript).contains("clearAnalysisPreview(snapshot?.symbol || analysisSelectedAsset?.symbol)");
    }

    @Test
    void strongDirectionRequiresFinalConfidenceInWorkspaceAndAuditProjection() throws Exception {
        String workspace = Files.readString(Path.of("src/main/resources/static/js/workspace.js"));

        assertThat(workspace).contains("function trustedDirectionProjection");
        assertThat(workspace).contains("/^STRONG_(BULLISH|BEARISH)$/");
        assertThat(workspace).contains("trustedDirectionProjection(direction, decision.finalConfidence, confidence)");
        assertThat(workspace).contains("directionTrusted = directionProjection.trusted");
        assertThat(workspace).contains("directionReady = directionProjection.trusted");
    }

    @Test
    void positionDetailAndNavigationAreCompleteAndAccessible() throws Exception {
        String html = Files.readString(HTML);
        String script = Files.readString(SCRIPT);
        assertThat(html).contains(
                "aria-label=\"首页\" title=\"首页\"",
                "aria-label=\"持仓\" title=\"持仓\"",
                "aria-label=\"系统状态与恢复\"");
        assertThat(script).contains(
                "[\"持仓状态\", label(position.status)]",
                "[\"用户止损\", formatNumber(position.stopLoss)]",
                "[\"当前价格\"", "[\"反转提示\"", "[\"上次评估\"",
                "data-direct-close-position", "持仓录入成功", "平仓记录成功");
    }
}
