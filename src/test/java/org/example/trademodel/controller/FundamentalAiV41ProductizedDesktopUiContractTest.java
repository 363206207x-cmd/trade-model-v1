package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class FundamentalAiV41ProductizedDesktopUiContractTest {
    private static final Path DASHBOARD = Path.of("src/main/resources/templates/dashboard.html");
    private static final Path LOGIN = Path.of("src/main/resources/templates/login.html");
    private static final Path ANALYSIS = Path.of("src/main/resources/templates/analysis-detail.html");
    private static final Path CONTRACT = Path.of("src/main/resources/static/js/frontend-contract.js");
    private static final Path STYLE = Path.of("src/main/resources/static/css/dashboard-latest.css");
    private static final Path FIXTURE = Path.of("scripts/dashboard-visual-acceptance-fixture.py");

    @Test
    void visibleBrandAndHomeHierarchyUseTheProductizedContract() throws Exception {
        String dashboard = Files.readString(DASHBOARD);
        String login = Files.readString(LOGIN);
        String analysis = Files.readString(ANALYSIS);

        assertThat(dashboard).contains(
                "<title>Fundamental AI</title>",
                "首页总览",
                "查看当前机会、参与条件与真实持仓状态",
                "Fundamental AI",
                "多源证据决策系统",
                "当前重点机会",
                "AI分析解释",
                "冲突与最终调整");
        assertThat(login).contains("登录 · Fundamental AI", "Fundamental AI",
                "多源证据决策系统").doesNotContain("TRINE LOGIC");
        assertThat(analysis).contains("分析详情 · Fundamental AI");
    }

    @Test
    void centralizedSemanticMapperOwnsEveryFrozenUserFacingState() throws Exception {
        String contract = Files.readString(CONTRACT);

        assertThat(contract).contains(
                "var USER_FACING_SEMANTIC_MAPPER",
                "NO_COMPLETE_PLAN: Object.freeze({ label: \"尚未形成最终计划\"",
                "WAITING_ANALYSIS: Object.freeze({ label: \"正在分析\"",
                "INSUFFICIENT_DATA: Object.freeze({ label: \"当前数据不足\"",
                "STALE: Object.freeze({ label: \"当前结果已过期\"",
                "SOURCE_UNAVAILABLE: Object.freeze({ label: \"数据来源暂不可用\"",
                "CONFIRMATION: Object.freeze({",
                "participationLabel: \"条件已确认\"",
                "participationLabel: \"等待触发\"",
                "participationLabel: \"降低强度\"",
                "participationLabel: \"当前仅观察\"",
                "participationLabel: \"当前已阻断\"",
                "READY: Object.freeze({ label: \"分析完成\"",
                "PARTIAL: Object.freeze({ label: \"部分结果可用\"",
                "FALLBACK: Object.freeze({ label: \"当前使用规则降级结果\"",
                "UNAVAILABLE: Object.freeze({ label: \"AI解释暂不可用\"",
                "ERROR: Object.freeze({ label: \"分析失败\"",
                "FOUND: Object.freeze({ label: \"已发现\"",
                "NONE_FOUND: Object.freeze({ label: \"完成检查，未发现\"",
                "NO_VERIFIABLE_FAILURE_PATH: Object.freeze({ label: \"暂无可验证失败路径\"");
        assertThat(contract).contains(
                "function userFacingValue(value)",
                "CONTROLLED_VISUAL_FIXTURE: \"受控验证数据\"",
                "value: userFacingValue");
    }

    @Test
    void assetPoolInteractionAndThreeDistinctTopSixEmptyStatesAreExplicit() throws Exception {
        String dashboard = Files.readString(DASHBOARD);
        String fixture = Files.readString(FIXTURE);

        assertThat(dashboard).contains(
                "id=\"btnAnalyzePreview\" disabled aria-disabled=\"true\"",
                "id=\"btnAdd\" disabled aria-disabled=\"true\"",
                "id=\"assetPoolScanAllTop\"",
                "资产池暂无观察资产",
                "当前没有进入重点机会的资产",
                "机会排序暂不可用",
                "assetPoolSelectedSymbol",
                "updateAssetPoolPanel");
        assertThat(fixture).contains(
                "\"pool-empty\"",
                "\"pool-no-opportunities\"",
                "\"ranking-unavailable\"");
    }

    @Test
    void aiWorkspaceExplainsCandidatesAndUsesProgressiveDisclosure() throws Exception {
        String dashboard = Files.readString(DASHBOARD);
        String workspace = slice(dashboard,
                "<article class=\"latest-module latest-ai-workspace\" id=\"homeAiPanel\"",
                "<aside class=\"latest-module latest-consistency\"");
        String renderer = slice(dashboard, "function roleMetadata(role)",
                "function renderHomeFocusSummary");

        assertThat(workspace).contains(
                "证据综合与候选形成", "证据与风险复核", "失败路径与压力测试",
                "role=\"tablist\"")
                .doesNotContain("最终裁决官", "最终裁决摘要", "裁决一致性");
        assertThat(renderer).contains(
                "候选市场方向", "机会状态", "候选计划模式", "核心解释",
                "支持证据", "反对证据", "多周期解释", "候选形成原因",
                "latest-collection-details", "查看审计详情")
                .doesNotContain("winner", "vote", "consistencyScore");
        assertThat(dashboard).contains("data-single-ai-workspace=\"true\"");
    }

    @Test
    void neutralEmptyStatesAndSemanticColorTokensAreLocked() throws Exception {
        String dashboard = Files.readString(DASHBOARD);
        String style = Files.readString(STYLE);

        assertThat(dashboard).contains(
                "当前没有高优先级风险",
                "当前没有临近的重要事件",
                "暂无活动持仓",
                "请选择资产",
                "选择一个重点机会资产后查看执行计划。",
                "暂无冲突与调整数据");
        assertThat(style).contains(
                "--canvas:", "--surface-primary:", "--surface-secondary:",
                "--surface-selected:", "--border-subtle:", "--border-strong:",
                "--text-primary:", "--text-secondary:", "--text-tertiary:",
                "--accent-primary:", "--state-positive:", "--state-warning:",
                "--state-danger:", "--state-neutral:", "--state-unavailable:",
                "[data-semantic-tone=\"positive\"]",
                "[data-semantic-tone=\"warning\"]",
                "[data-semantic-tone=\"danger\"]",
                "background: var(--tmv1-surface);")
                .doesNotContain("linear-gradient", "radial-gradient");
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
