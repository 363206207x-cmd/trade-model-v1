package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class FundamentalAiV41ExecutionPlanSemanticAlignmentContractTest {
    private static final Path DASHBOARD = Path.of("src/main/resources/templates/dashboard.html");
    private static final Path CONTRACT = Path.of("src/main/resources/static/js/frontend-contract.js");
    private static final Path FIXTURE = Path.of("scripts/dashboard-visual-acceptance-fixture.py");

    @Test
    void planModeAndMissingFinalDataStateUseIndependentSemanticMaps() throws Exception {
        String contract = Files.readString(CONTRACT);
        String modes = slice(contract, "var PLAN_MODE_VIEWS", "var OPPORTUNITY_TYPE_LABELS");
        String dataStates = slice(contract, "var PLAN_DATA_STATE_VIEWS", "var DATA_STATE_VIEWS");

        assertThat(modes).contains(
                "CONFIRMATION", "participationLabel: \"条件已确认\"",
                "PREPARATION", "participationLabel: \"等待触发\"",
                "REDUCED", "participationLabel: \"降低强度\"",
                "OBSERVATION", "participationLabel: \"当前仅观察\"",
                "BLOCKED", "participationLabel: \"当前已阻断\"");
        assertThat(dataStates).contains(
                "UNSELECTED", "请选择资产",
                "WAITING_ANALYSIS", "正在分析",
                "INSUFFICIENT_DATA", "当前数据不足",
                "SOURCE_UNAVAILABLE", "数据来源暂不可用",
                "STALE", "当前结果已过期",
                "WAITING_RULE_VALIDATION", "等待规则校验",
                "NO_COMPLETE_PLAN", "尚未形成最终计划")
                .doesNotContain("participationLabel", "profile: \"preparation\"",
                        "profile: \"observation\"", "profile: \"blocked\"");
        assertThat(contract).contains(
                "planMode: planModeView",
                "planDataState: planDataStateView");
    }

    @Test
    void executionPlanRendersFiveFormalFinalProfilesWithProgressiveDisclosure() throws Exception {
        String dashboard = Files.readString(DASHBOARD);
        String renderer = slice(dashboard,
                "function renderHomeExecutionFromPayload(suggestion, selectedAsset)",
                "function renderHomeAiDecisionFromPayload");

        assertThat(renderer).contains(
                "modeView.profile === \"confirmation\"",
                "modeView.profile === \"preparation\"",
                "modeView.profile === \"reduced\"",
                "modeView.profile === \"observation\"",
                "modeView.profile === \"blocked\"",
                "入场与触发",
                "失效与止损",
                "目标与趋势跟踪",
                "风险限制",
                "时间有效性",
                "当前计划状态",
                "data-plan-profile");
        assertThat(renderer)
                .doesNotContain("是否值得开仓", "worthOpening", "userFacingSemantic.planState")
                .contains(
                        "userFacingSemantic.planMode",
                        "userFacingSemantic.planDataState",
                        "cleaned === \"暂无 AI 原始输出\"",
                        "cleaned === \"当前不可查看\"");
    }

    @Test
    void gptIsCandidateOnlyAndFinalPlanRemainsInExecutionRegion() throws Exception {
        String dashboard = Files.readString(DASHBOARD);
        String gpt = slice(dashboard,
                "function renderGptFinalHomeRole(role)",
                "function renderGeminiReviewHomeRole(role)");
        String execution = slice(dashboard,
                "function renderHomeExecutionFromPayload(suggestion, selectedAsset)",
                "function renderHomeAiDecisionFromPayload");

        assertThat(gpt).contains(
                "data-result-layer=\"candidate\"",
                "候选市场方向", "机会状态", "候选计划模式",
                "候选形成原因", "支持证据", "反对证据")
                .doesNotContain("是否值得开仓", "最终计划模式", "Final Execution Plan",
                        "recommendedAction", "entryZone", "stopLoss", "targetZones");
        assertThat(execution).contains(
                "data-plan-source=\"final\"", "finalMarketBias", "finalPlanMode")
                .doesNotContain("candidateSummary", "candidate.planMode", "是否值得开仓");
    }

    @Test
    void defaultVisibleDesktopPathContainsNoDisclaimerOrRawContractCopy() throws Exception {
        String dashboard = Files.readString(DASHBOARD);
        String sidebar = slice(dashboard, "<aside class=\"home-sidebar\"", "<main class=\"dashboard-main\">");
        String home = slice(dashboard,
                "<div data-desktop-home-view data-desktop-dashboard-root data-latest-approved-home",
                "<div class=\"runtime-status-stack\"");
        String dynamicVisibleRenderers = slice(dashboard,
                "function renderGptFinalHomeRole(role)",
                "function renderHomePushInboxFromPayload");

        assertThat(sidebar + home + dynamicVisibleRenderers).doesNotContain(
                "非交易指令", "仅供参考", "不构成交易建议", "由用户自行判断",
                "请自行承担风险", "不代表交易授权", "只读决策", "不自动交易",
                "不代表已开仓", "执行计划不会自动创建持仓", "是否值得开仓",
                "latest-ai-boundary", "latest-plan-safety");
    }

    @Test
    void deterministicRuntimeFixtureCoversEveryRequiredSemanticScenario() throws Exception {
        String fixture = Files.readString(FIXTURE);

        assertThat(fixture).contains(
                "\"unselected\"", "\"waiting-analysis\"",
                "\"plan-confirmation\"", "\"plan-preparation\"",
                "\"plan-reduced\"", "\"plan-observation\"",
                "\"plan-blocked-final\"", "\"candidate-only\"",
                "\"final-ai-unavailable\"",
                "def final_plan_mode_fixture(symbol: str, mode: str)",
                "plan.pop(key, None)");
    }

    @Test
    void backendSafetyGateRemainsIntactAndHiddenFromPrimaryCopy() throws Exception {
        String contract = Files.readString(CONTRACT);

        assertThat(contract).contains(
                "plan.finalPlan === true",
                "String(plan.validationStatus || \"\").toUpperCase() === \"PASS\"",
                "String(plan.chainStatus || \"\").toUpperCase() === \"FINAL_VALIDATED\"",
                "String(plan.sourceStatus || \"\").toUpperCase() === \"VALID\"",
                "plan.notTradeInstruction === true");
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
