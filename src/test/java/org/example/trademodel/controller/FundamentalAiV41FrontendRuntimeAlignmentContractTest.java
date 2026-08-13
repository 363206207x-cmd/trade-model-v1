package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class FundamentalAiV41FrontendRuntimeAlignmentContractTest {
    private static final Path DASHBOARD = Path.of("src/main/resources/templates/dashboard.html");
    private static final Path LATEST_STYLE = Path.of("src/main/resources/static/css/dashboard-latest.css");
    private static final Path CONTRACT = Path.of("src/main/resources/static/js/frontend-contract.js");
    private static final Path ANALYSIS_DETAIL = Path.of("src/main/resources/templates/analysis-detail.html");
    private static final Path ANALYSIS_SCRIPT = Path.of("src/main/resources/static/js/analysis-detail.js");
    private static final Path HOME_VO = Path.of("src/main/java/org/example/trademodel/vo/DashboardHomeVO.java");
    private static final Path HOME_SERVICE = Path.of(
            "src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java");
    private static final Path LATEST_EVIDENCE = Path.of(
            "docs/evidence/v4_1_execution_plan_semantics");

    @Test
    void desktopHomeKeepsTheFrozenModuleOrder() throws Exception {
        String html = Files.readString(DASHBOARD);

        assertOrdered(html,
                "class=\"latest-system-status\"",
                "class=\"latest-signal-grid\"",
                "class=\"latest-assets-section\"",
                "id=\"homePositionCard\"",
                "id=\"homeExecutionCard\"",
                "id=\"homeAiPanel\"",
                "id=\"homeConsistencyContent\"");
        assertThat(html)
                .contains(
                        "data-position-execution-ratio=\"70:30\"",
                        "class=\"latest-ai-grid\"",
                        "data-latest-approved-home",
                        "data-figma-contract=\"28:154 31:23 520:212 523:748 35:97\"")
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
                .contains(".slice(0, 6)")
                .doesNotContain("BTCUSDT", "ETHUSDT", "SOLUSDT", ".sort(");
        assertThat(cards)
                .contains(
                        "authoritativeHomeAssetList(assets)",
                        "homeAssetEmptyStateMarkup(emptyKind)",
                        "latest-asset-card",
                        "data-home-asset-remove",
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
                .contains("<dt>市场方向</dt>")
                .contains("latest-asset-fields")
                .contains("<dt>计划模式</dt>")
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
                        "userFacingSemantic.planMode",
                        "userFacingSemantic.planDataState",
                        "最终市场方向",
                        "计划模式",
                        "当前计划状态",
                        "机会类型",
                        "入场逻辑",
                        "入场区间",
                        "触发条件",
                        "失效与止损",
                        "止损逻辑",
                        "止损区间",
                        "目标与趋势跟踪",
                        "目标逻辑",
                        "目标区域",
                        "减仓条件",
                        "放弃条件",
                        "失效条件",
                        "杠杆上限",
                        "仓位上限",
                        "预期风险收益",
                        "时间有效性",
                        "有效开始",
                        "有效截止",
                        "预计持有周期",
                        "Rule Validation",
                        "降级原因",
                        "来源状态",
                        "安全门禁")
                .contains("data-plan-source=\"final\"")
                .doesNotContain(
                        "defaultEntry", "defaultStop", "defaultTakeProfit", "candidateSummary",
                        "是否值得开仓", "latest-plan-safety");
    }

    @Test
    void threeAiWorkspaceIsSingleTabbedStructuredAndAntiHallucination() throws Exception {
        String contract = Files.readString(CONTRACT);
        String html = Files.readString(DASHBOARD);
        String workspace = slice(html,
                "<article class=\"latest-module latest-ai-workspace\" id=\"homeAiPanel\"",
                "<aside class=\"latest-module latest-consistency\"");
        String renderer = slice(html,
                "function roleMetadata(role)",
                "function renderHomeFocusSummary");

        assertThat(workspace)
                .contains("role=\"tablist\"", "GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE")
                .containsOnlyOnce("role=\"tabpanel\"");
        assertThat(contract)
                .contains("\"READY\", \"PARTIAL\", \"FALLBACK\", \"UNAVAILABLE\", \"ERROR\"")
                .contains(
                        "FOUND: \"已发现\"",
                        "NONE_FOUND: \"完成检查，未发现\"",
                        "INSUFFICIENT_DATA: \"数据不足，无法判断\"",
                        "SOURCE_UNAVAILABLE: \"数据来源暂不可用\"",
                        "STALE: \"数据已过期\"",
                        "NO_VERIFIABLE_FAILURE_PATH: \"暂无可验证失败路径\"")
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
                        "[\"角色状态\", userFacingSemantic.roleState(role.roleState).label]",
                        "[\"Analysis\", role.analysisId]",
                        "[\"Trace\", role.traceId]",
                        "[\"生成时间\", role.generatedAt]",
                        "renderFormalCollection(\"支持证据\"",
                        "renderFormalCollection(\"反对证据\"",
                        "renderFormalCollection(\"证据缺口\"",
                        "renderFormalCollection(\"逻辑冲突\"",
                        "renderFormalCollection(\"失败路径\"",
                        "renderFormalCollection(\"外部事件风险\"")
                .contains("latest-ai-content", "latest-ai-metadata", "renderFormalCollection",
                        "latest-collection-details", "查看审计详情")
                .doesNotContain("winner", "vote", "consistencyScore");
    }

    @Test
    void consistencyIsACompactAdjacentSummaryAndNeverAFourthAiRole() throws Exception {
        String html = Files.readString(DASHBOARD);
        String renderer = slice(html,
                "function renderHomeConsistencyCard(options)",
                "function roleMetadata(role)");

        assertThat(html)
                .contains("<aside class=\"latest-module latest-consistency\"")
                .contains("id=\"homeConsistencyContent\"")
                .contains("暂无冲突与调整数据");
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
                        "data-position-source",
                        "data-final-plan-id",
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
                .contains("风险、结论与建议保持关闭")
                .contains("list.slice(0, 3)")
                .contains("isLocallyClosedPosition(positionId)")
                .doesNotContain(
                        "系统建议止损", "系统建议止盈", "剩余仓位", "仓位状态",
                        "反向预警", "是否反转", "systemStopLoss", "systemTakeProfit", "autoClose", "autoOrder");
    }

    @Test
    void latestDesktopStyleOwnsTheNewModulesAndSupportsLightAndDarkTokens() throws Exception {
        String html = Files.readString(DASHBOARD);
        String css = Files.readString(LATEST_STYLE);

        assertThat(html)
                .contains("href=\"/css/dashboard-latest.css\"")
                .contains("class=\"latest-system-status\"")
                .contains("class=\"latest-asset-grid\"")
                .contains("class=\"latest-decision-grid\"")
                .contains("class=\"latest-ai-grid\"");
        assertThat(css)
                .contains(
                        ":root {",
                        "[data-theme=\"dark\"]",
                        ".latest-system-status",
                        ".latest-asset-card",
                        ".latest-position-row",
                        ".latest-plan-body",
                        ".latest-ai-shell",
                        ".latest-consistency")
                .doesNotContain("linear-gradient", "radial-gradient");
    }

    @Test
    void latestHomeUsesSixSystemStatusesAndRealAssetPoolControls() throws Exception {
        String html = Files.readString(DASHBOARD);
        String home = slice(html,
                "<div data-desktop-home-view data-desktop-dashboard-root data-latest-approved-home",
                "<div class=\"runtime-status-stack\"");

        assertThat(home)
                .containsOnlyOnce("id=\"cardTrend\"")
                .containsOnlyOnce("id=\"cardRisk\"")
                .containsOnlyOnce("id=\"cardDataQuality\"")
                .containsOnlyOnce("id=\"cardAiSystem\"")
                .containsOnlyOnce("id=\"cardOpportunity\"")
                .containsOnlyOnce("id=\"cardHotReset\"")
                .contains(
                        "<input type=\"search\" id=\"symbolSearch\"",
                        "id=\"btnAdd\"",
                        "id=\"assetPoolToggle\"",
                        "id=\"btnReset\"",
                        "id=\"homeAssetScanState\"");
        assertThat(home)
                .doesNotContain("id=\"cardAiConflict\"", "id=\"cardConfused\"");
    }

    @Test
    void oldHomeRenderersAreAbsentFromTheLatestProductionPath() throws Exception {
        String html = Files.readString(DASHBOARD);
        String home = slice(html,
                "<div data-desktop-home-view data-desktop-dashboard-root data-latest-approved-home",
                "<div class=\"runtime-status-stack\"");
        String assetRenderer = slice(html,
                "function renderHomeAssetsFromPayload(assets, moduleState)",
                "function renderHomePositionsFromPayload");
        String positionRenderer = slice(html,
                "function renderHomePositionsFromPayload(positions)",
                "function renderHomeExecutionFromPayload");
        String planRenderer = slice(html,
                "function renderHomeExecutionFromPayload(suggestion, selectedAsset)",
                "function renderHomeAiDecisionFromPayload");
        String aiRenderer = slice(html,
                "function renderHomeAiDecisionFromPayload(aiDecision, asset)",
                "function renderHomePushInboxFromPayload");

        assertThat(home).doesNotContain(
                "class=\"layer1\"",
                "class=\"tiles-row\"",
                "class=\"position-execution-row\"",
                "class=\"ai-decision-row\"",
                "class=\"home-ai-summary-card\"");
        assertThat(assetRenderer).doesNotContain("coin-tile", "tile-score-pill");
        assertThat(positionRenderer).doesNotContain("home-position-summary-card", "home-position-semantic-grid");
        assertThat(planRenderer).doesNotContain("execution-card-body", "final-plan-contract");
        assertThat(aiRenderer).doesNotContain("home-ai-role-tabs", "home-ai-summary-card");
    }

    @Test
    void assetContextSwitchUpdatesOnlyAssetBoundDecisionModules() throws Exception {
        String html = Files.readString(DASHBOARD);
        String loading = slice(html,
                "function renderAssetContextLoading(symbol)",
                "function renderDashboardHomePayload");
        String payload = slice(html,
                "function renderDashboardHomePayload(home, preservePositionSummary)",
                "function renderDashboardHomeUnavailable");
        String unavailable = slice(html,
                "function renderAssetContextUnavailable(contextState)",
                "function fetchDashboardHome");

        assertThat(loading)
                .contains("renderHomeExecutionFromPayload", "renderHomeAiDecisionFromPayload")
                .doesNotContain("renderHomeSystemStateFromPayload", "renderHomeAlertEventRowsFromPayload",
                        "renderHomePositionsFromPayload");
        assertThat(payload)
                .contains("if (!preservePositionSummary)", "renderHomeSystemStateFromPayload",
                        "renderHomeAlertEventRowsFromPayload", "renderHomePositionsFromPayload");
        assertThat(unavailable)
                .contains("renderHomeExecutionFromPayload", "renderHomeAiDecisionFromPayload")
                .doesNotContain("renderHomeSystemStateFromPayload", "renderHomeAlertEventRowsFromPayload",
                        "renderHomePositionsFromPayload", "window.__lastSystemStatus =");
    }

    @Test
    void visualEvidenceLocksExecutionPlanSemanticsViewportAndRequiredScenarios() throws Exception {
        String index = Files.readString(LATEST_EVIDENCE.resolve("README.md"));
        String qa = Files.readString(LATEST_EVIDENCE.resolve("browser-qa.json"));
        BufferedImage light = ImageIO.read(LATEST_EVIDENCE.resolve(
                "runtime/12-desktop-first-viewport-1440x900.png").toFile());
        BufferedImage full = ImageIO.read(LATEST_EVIDENCE.resolve(
                "runtime/13-desktop-full-page.png").toFile());

        assertThat(index).contains(
                "28:154", "31:23", "520:212", "523:748", "35:97",
                "Node `519:3` is the rejected old P1-KB baseline");
        for (int i = 1; i <= 13; i++) {
            assertThat(index).contains(String.format("| %02d |", i));
        }
        assertThat(index).contains(
                "未选择资产", "等待分析", "PREPARATION", "OBSERVATION", "BLOCKED",
                "CONFIRMATION", "REDUCED", "Candidate 有、Final 无", "Final 有、AI 不可用",
                "GPT Candidate", "Before / After", "1440 x 900", "Full page");
        assertThat(light.getWidth()).isEqualTo(1440);
        assertThat(light.getHeight()).isEqualTo(900);
        assertThat(full.getWidth()).isEqualTo(1440);
        assertThat(full.getHeight()).isGreaterThan(900);
        assertThat(qa).contains(
                "\"dashboardHtml\": \"" + sha256(DASHBOARD) + "\"",
                "\"dashboardLatestCss\": \"" + sha256(LATEST_STYLE) + "\"",
                "\"frontendContractJs\": \"" + sha256(CONTRACT) + "\"",
                "\"visualFixture\": \"" + sha256(Path.of(
                        "scripts/dashboard-visual-acceptance-fixture.py")) + "\"",
                "\"horizontalOverflow\": 0",
                "\"textOverflow\": 0",
                "\"topLevelOverlap\": 0",
                "\"consoleErrors\": 0",
                "\"consoleWarnings\": 0",
                "\"visibleAiRoleCount\": 1",
                "\"positionExecutionWidthRatio\": 2.3333",
                "\"candidateVisibleAsFinal\": false",
                "\"visibleDisclaimerCopyCount\": 0",
                "\"rawEnumPrimaryDisplayCount\": 0",
                "\"staleAssetContentCount\": 0",
                "\"systemStatusUnchanged\": true",
                "\"alertsAndEventsUnchanged\": true",
                "\"positionsUnchanged\": true",
                "\"browserStatus\": \"PASS\"");
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
                        "证据综合与候选形成 <small>GPT_FINAL</small>",
                        "证据与风险复核 <small>GEMINI_REVIEW</small>",
                        "失败路径与压力测试 <small>GROK_CHALLENGE</small>",
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

    private String sha256(Path source) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(source)));
    }
}
