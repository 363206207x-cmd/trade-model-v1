package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class FundamentalAiV41FrontendRuntimeAlignmentContractTest {
    private static final Path DASHBOARD = Path.of("src/main/resources/templates/dashboard.html");
    private static final Path CONTRACT = Path.of("src/main/resources/static/js/frontend-contract.js");
    private static final Path ANALYSIS_DETAIL = Path.of("src/main/resources/templates/analysis-detail.html");
    private static final Path ANALYSIS_SCRIPT = Path.of("src/main/resources/static/js/analysis-detail.js");
    private static final Path HOME_VO = Path.of("src/main/java/org/example/trademodel/vo/DashboardHomeVO.java");
    private static final Path HOME_SERVICE = Path.of(
            "src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java");

    @Test
    void desktopHomeKeepsTheFrozenModuleOrder() throws Exception {
        String html = Files.readString(DASHBOARD);

        assertOrdered(html,
                "<div class=\"layer1\">",
                "<h2>实时告警与关键事件</h2>",
                "<div class=\"tiles-row\" id=\"tilesRow\"></div>",
                "id=\"homePositionCard\"",
                "id=\"homeExecutionCard\"",
                "id=\"homeAiPanel\"",
                "id=\"homeConsistencyContent\"");
        assertThat(html)
                .contains("position-execution-row", "ai-decision-row")
                .doesNotContain("模拟K线", "模拟走势", "虚假行情图");
    }

    @Test
    void assetPoolUiUsesRealExistingEndpointsAndPreviewKeepsPersistenceBoundaries() throws Exception {
        String html = Files.readString(DASHBOARD);

        assertThat(html)
                .contains(
                        "id=\"symbolSearch\"",
                        "id=\"assetPoolToggle\"",
                        "id=\"assetPoolBatchAdd\"",
                        "id=\"assetPoolBatchRemove\"",
                        "id=\"assetPoolScanSelected\"",
                        "id=\"assetPoolScanAll\"",
                        "data-pool-scan=",
                        "data-pool-remove=",
                        "assetPoolRequest(\"/api/asset-pool\"",
                        "\"/api/asset-pool/batch-add\"",
                        "\"/api/asset-pool/batch-remove\"",
                        "\"/api/asset-pool/restore-default\"",
                        "\"/api/asset-pool/batch-scan\"",
                        "\"/api/asset-pool/scan?timeframe=5m\"",
                        "\"/api/asset-pool/search?query=\"",
                        "\"/analysis-preview?timeframe=5m\"")
                .contains(
                        "preview.previewOnly === true",
                        "preview.poolMutationPerformed === false",
                        "preview.opportunityPersisted === false",
                        "preview.candidatePersisted === false",
                        "preview.finalPlanPersisted === false")
                .contains("不会自动加入资产池或进入 Top6")
                .contains("历史分析与复盘保留");
    }

    @Test
    void homeTopSixConsumesAuthoritativeProjectionWithoutLocalRankingOrDefaultFill() throws Exception {
        String html = Files.readString(DASHBOARD);
        String renderer = slice(html,
                "function authoritativeHomeAssetList(assets)",
                "function renderHomeFocusSummary");
        String cards = slice(html,
                "function renderHomeAssetsFromPayload(assets, moduleState)",
                "function renderHomePositionsFromPayload");

        assertThat(renderer)
                .contains("return (assets || []).filter(function (asset)")
                .contains("slotType !== \"DEFAULT_SLOT\"")
                .contains("[\"invalidated\", \"cooling\", \"confused\"].indexOf(opportunityState) < 0")
                .contains("planMode !== \"BLOCKED\"")
                .doesNotContain("BTCUSDT", "ETHUSDT", "SOLUSDT", ".sort(");
        assertThat(cards)
                .contains(
                        "authoritativeHomeAssetList(assets)",
                        "动态 Top6 不使用默认资产补位",
                        "asset.opportunityScore",
                        "asset.rankingReason",
                        "asset.analysisId",
                        "asset.marketBias",
                        "asset.opportunityState",
                        "asset.planMode",
                        "priceTrusted")
                .doesNotContain("rankedAssetCandidates", "DEFAULT_SLOT");
    }

    @Test
    void marketBiasOpportunityStateAndPlanModeRemainIndependentFrozenDimensions() throws Exception {
        String contract = Files.readString(CONTRACT);
        String html = Files.readString(DASHBOARD);

        assertThat(contract)
                .contains(
                        "STRONG_BULLISH: \"强偏多\"",
                        "BULLISH: \"偏多\"",
                        "WEAK_BULLISH: \"弱偏多\"",
                        "RANGE: \"震荡\"",
                        "WEAK_BEARISH: \"弱偏空\"",
                        "BEARISH: \"偏空\"",
                        "STRONG_BEARISH: \"强偏空\"",
                        "WAIT: \"观望\"")
                .contains(
                        "CONFIRMATION: \"确认型\"",
                        "PREPARATION: \"预备型\"",
                        "REDUCED: \"缩减型\"",
                        "OBSERVATION: \"观察\"",
                        "BLOCKED: \"阻断\"");
        assertThat(html)
                .contains("<small>Market Bias</small>")
                .contains("tile-score-pill")
                .contains("<span>Plan Mode <strong>")
                .contains("data-opportunity-state=")
                .contains("data-plan-mode=");
    }

    @Test
    void executionRegionOnlyOpensForValidatedFinalAndRendersTheCompleteFrozenContract()
            throws Exception {
        String contract = Files.readString(CONTRACT);
        String html = Files.readString(DASHBOARD);
        String renderer = slice(html,
                "function renderHomeExecutionFromPayload(suggestion, selectedAsset)",
                "function renderHomeAiDecisionFromPayload");

        assertThat(contract)
                .contains(
                        "plan.finalPlan === true",
                        "String(plan.validationStatus || \"\").toUpperCase() === \"PASS\"",
                        "String(plan.chainStatus || \"\").toUpperCase() === \"FINAL_VALIDATED\"",
                        "String(plan.sourceStatus || \"\").toUpperCase() === \"VALID\"",
                        "plan.notTradeInstruction === true")
                .contains("当前 Final Plan 不可验证");
        assertThat(renderer)
                .contains(
                        "Candidate 与未通过 Rule Validation 的内容不会在此显示",
                        "Final Market Bias",
                        "Final Plan Mode",
                        "推荐方向",
                        "人工参与",
                        "机会类型",
                        "建议动作",
                        "入场逻辑",
                        "入场区间",
                        "触发条件",
                        "止损逻辑",
                        "止损区",
                        "止盈逻辑",
                        "目标区 / 止盈规则",
                        "加仓条件",
                        "减仓条件",
                        "放弃条件",
                        "失效条件",
                        "杠杆上限",
                        "仓位上限",
                        "预期风险收益",
                        "分析周期",
                        "触发周期",
                        "有效期",
                        "持有周期",
                        "Rule Validation",
                        "降级 / 否决原因",
                        "来源状态",
                        "notTradeInstruction=true")
                .doesNotContain("defaultEntry", "defaultStop", "defaultTakeProfit");
    }

    @Test
    void threeAiWorkspaceIsSingleTabbedStructuredAndAntiHallucination() throws Exception {
        String contract = Files.readString(CONTRACT);
        String html = Files.readString(DASHBOARD);
        String workspace = slice(html,
                "<section class=\"card\" id=\"homeAiPanel\"",
                "</section>");
        String renderer = slice(html,
                "function roleMetadata(role)",
                "function renderHomeFocusSummary");

        assertThat(workspace)
                .contains("role=\"tablist\"", "GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE")
                .containsOnlyOnce("role=\"tabpanel\"");
        assertThat(contract)
                .contains("\"READY\", \"PARTIAL\", \"FALLBACK\", \"UNAVAILABLE\", \"ERROR\"")
                .contains(
                        "FOUND: \"已找到可验证内容\"",
                        "NONE_FOUND: \"已完成检查，未发现\"",
                        "INSUFFICIENT_DATA: \"数据不足，无法完成检查\"",
                        "SOURCE_UNAVAILABLE: \"来源不可用\"",
                        "STALE: \"数据已过期\"",
                        "NO_VERIFIABLE_FAILURE_PATH: \"未找到可验证失败路径\"")
                .contains(
                        "supportingEvidence",
                        "opposingEvidence",
                        "evidenceGaps",
                        "logicConflicts",
                        "underestimatedRisks",
                        "failurePaths",
                        "opposingScenarios",
                        "externalEventRisks",
                        "microstructureRisks",
                        "watchIndicators");
        assertThat(renderer)
                .contains(
                        "[\"角色状态\", role.roleState]",
                        "[\"Analysis\", role.analysisId]",
                        "[\"Trace\", role.traceId]",
                        "[\"生成时间\", role.generatedAt]",
                        "renderFormalCollection(\"支持证据\"",
                        "renderFormalCollection(\"反对证据\"",
                        "renderFormalCollection(\"证据缺口\"",
                        "renderFormalCollection(\"逻辑冲突\"",
                        "renderFormalCollection(\"失败路径\"",
                        "renderFormalCollection(\"外部事件风险\"")
                .doesNotContain("winner", "vote", "consistencyScore");
    }

    @Test
    void consistencyIsACompactAdjacentSummaryAndNeverAFourthAiRole() throws Exception {
        String html = Files.readString(DASHBOARD);
        String renderer = slice(html,
                "function renderHomeConsistencyCard(options)",
                "function roleMetadata(role)");

        assertThat(html)
                .contains("<aside class=\"home-consistency-summary home-consistency-contract\"")
                .contains("id=\"homeConsistencyContent\"")
                .contains("暂无一致性数据");
        assertThat(renderer)
                .contains(
                        "options.dataState",
                        "options.conflictLevel",
                        "options.finalMarketBias",
                        "options.finalPlanMode",
                        "options.mainReason",
                        "options.recoveryCondition")
                .doesNotContain("percentage", "vote", "score", "donut", "pie");
    }

    @Test
    void positionMonitoringPreservesSourceTrustAndExecutionBoundaries() throws Exception {
        String html = Files.readString(DASHBOARD);
        String renderer = slice(html,
                "function renderHomePositionsFromPayload(positions)",
                "function renderHomeExecutionFromPayload");

        assertThat(renderer)
                .contains(
                        "SYSTEM_PLAN_POSITION",
                        "系统 Final Plan · ",
                        "MANUAL_INDEPENDENT",
                        "独立手动持仓 · 无系统计划关联",
                        "持仓风险",
                        "监控结论",
                        "建议动作",
                        "开仓价",
                        "标记价格",
                        "盈亏",
                        "开仓时间",
                        "入场逻辑状态",
                        "反转状态",
                        "风险变化原因",
                        "最近监控时间")
                .contains("风险、结论、建议、标记价格与盈亏均保持关闭")
                .contains("list.slice(0, 3)")
                .contains("isLocallyClosedPosition(positionId)")
                .doesNotContain("systemStopLoss", "systemTakeProfit", "autoClose", "autoOrder");
    }

    @Test
    void readProjectionExposesOnlyMergedBackendFieldsWithoutParallelApiOrSchema() throws Exception {
        String vo = Files.readString(HOME_VO);
        String service = Files.readString(HOME_SERVICE);

        assertThat(vo)
                .contains(
                        "private String sourceType;",
                        "private String finalPlanId;",
                        "private String entryLogic;",
                        "private String stopLogic;",
                        "private String targetLogic;",
                        "private String validationStatus;",
                        "private String candidateId;",
                        "private String resolverResultId;",
                        "private String validationResultId;",
                        "private Boolean finalPlan;",
                        "private String provider;",
                        "private String sourceRole;",
                        "private List<String> reasonCodes = new ArrayList<>();")
                .contains("private Boolean fallback;")
                .contains("private String fallbackReason;");
        assertThat(service)
                .contains(
                        "row.setSourceType(trimToNull(position.getSourceType()))",
                        "row.setFinalPlanId(trimToNull(position.getFinalPlanId()))",
                        "suggestion.setFinalPlan(Boolean.TRUE.equals(executionPlan.getFinalPlan()))",
                        "tab.setProvider(role.provider())",
                        "tab.setSourceRole(role.sourceRole())")
                .doesNotContain("new UserPosition", "AUTO_ORDER");
    }

    @Test
    void existingAnalysisDetailOwnsTheCompleteDecisionChainView() throws Exception {
        String html = Files.readString(ANALYSIS_DETAIL);
        String script = Files.readString(ANALYSIS_SCRIPT);

        assertThat(html)
                .contains(
                        "八项评分逐项绑定；缺失不是 0",
                        "支持与反对证据均保留来源、观测时间和新鲜度",
                        "多周期摘要",
                        "GPT_FINAL / 最终裁决官",
                        "GEMINI_REVIEW / 冲突复核官",
                        "GROK_CHALLENGE / 反方挑战官",
                        "决策责任链",
                        "Conflict Resolver",
                        "Rule Validation",
                        "Final Plan 来源链");
        assertThat(script)
                .contains(
                        "/api/review/aggregate/",
                        "/api/ai/audit-chain?analysisId=",
                        "renderStructuredCollection",
                        "renderAuditChain",
                        "renderFinalSource",
                        "plan.finalPlan === true",
                        "validation.status === \"PASS\"",
                        "plan.notTradeInstruction === true")
                .doesNotContain("latestBySymbol", "/dashboard/analysis-detail-v2");
    }

    private void assertOrdered(String source, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = source.indexOf(marker);
            assertThat(current).as(marker).isGreaterThan(previous);
            previous = current;
        }
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
