package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class AssetDetailFrontendContractTest {
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/asset-detail.html");
    private static final Path SCRIPT = Path.of("src/main/resources/static/js/asset-detail.js");
    private static final Path STYLES = Path.of("src/main/resources/static/css/asset-detail.css");
    private static final Path DESKTOP_HOME = Path.of("src/main/resources/templates/dashboard.html");
    private static final Path MOBILE_HOME = Path.of("src/main/resources/templates/dashboard-mobile.html");
    private static final Path MOBILE_SCRIPT = Path.of("src/main/resources/static/js/dashboard-mobile.js");

    @Test
    void assetSummaryContainsOnlyApprovedDecisionFields() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("data-asset-field=\"symbol\"")
                .contains("data-asset-field=\"latestPrice\"")
                .contains("data-asset-field=\"direction\"")
                .contains("data-asset-field=\"score\"")
                .contains("data-asset-field=\"confidence\"")
                .contains("data-asset-field=\"risk\"")
                .contains("data-asset-field=\"state\"")
                .contains("data-asset-field=\"worthOpening\"")
                .contains("data-asset-field=\"conclusion\"")
                .contains("当前判断不可用")
                .contains("状态待同步")
                .contains("暂无可验证结论")
                .doesNotContain("analysisId")
                .doesNotContain("evidenceCount")
                .doesNotContain("sourceProvider")
                .doesNotContain("timeframeFreshness");
    }

    @Test
    void aiViewKeepsExactlyThreeResponsibilitiesAndDecisionMode() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("data-ai-decision-mode")
                .contains("GPT_FINAL")
                .contains("最终裁决官")
                .contains("GEMINI_REVIEW")
                .contains("冲突复核官")
                .contains("GROK_CHALLENGE")
                .contains("反方挑战官")
                .contains("完整证据关联尚未提供")
                .contains("不生成独立交易方向")
                .doesNotContain("AI 投票")
                .doesNotContain("第四角色")
                .doesNotContain("AI 胜出");
        assertThat(script)
                .contains("var ROLE_ORDER = [\"GPT_FINAL\", \"GEMINI_REVIEW\", \"GROK_CHALLENGE\"]")
                .contains("ai.decisionModeLabel, \"仅规则判断\"")
                .contains("contract.normalizeAiTabs(ai.tabs)")
                .contains("ArrowLeft")
                .contains("ArrowRight")
                .doesNotContain("winner")
                .doesNotContain("vote");
    }

    @Test
    void executionPlanRequiresSharedExactIdentityGuardAndNeverBecomesAPosition() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("系统建议 / 仅供人工复核")
                .contains("data-plan-values hidden")
                .contains("data-plan-field=\"direction\"")
                .contains("data-plan-field=\"entryZone\"")
                .contains("data-plan-field=\"stopLoss\"")
                .contains("data-plan-field=\"takeProfitRules\"")
                .contains("data-plan-field=\"invalidCondition\"")
                .contains("执行建议不是用户持仓")
                .doesNotContain("home.positions")
                .doesNotContain("positionMonitor")
                .doesNotContain("下单按钮")
                .doesNotContain("自动执行按钮");
        assertThat(script)
                .contains("contract.executionPlanAccess(plan)")
                .contains("var positionMode = plan.positionMode === true")
                .contains("if (section) section.hidden = positionMode")
                .contains("if (positionContext) positionContext.hidden = !positionMode")
                .contains("if (positionMode)")
                .contains("values.hidden = !access.visible")
                .contains("access.visible ? plan[field] : null")
                .doesNotContain("UserPosition")
                .doesNotContain("/api/plan");
        assertThat(script.indexOf("if (positionMode)"))
                .isLessThan(script.indexOf("contract.executionPlanAccess(plan)"));
        assertThat(html)
                .contains("data-position-context hidden")
                .contains("资产机会执行建议不在本页展示")
                .contains("data-execution-plan hidden");
    }

    @Test
    void worthOpeningUsesOnlyBackendValueAndFailsClosedWhenUnavailable() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("<dt>是否值得开仓</dt>")
                .contains("data-asset-field=\"worthOpening\">待同步");
        assertThat(script)
                .contains("asset.worthOpening === true ? \"是\"")
                .contains("asset.worthOpening === false ? \"否\" : null")
                .contains("'[data-asset-field=\"worthOpening\"]', null, \"待同步\"")
                .doesNotContain("worthOpening = asset.compositeScore")
                .doesNotContain("worthOpening = asset.marketBias");
    }

    @Test
    void requestFailureExposesRetryWithoutChangingTheApiContract() throws Exception {
        String html = Files.readString(TEMPLATE);
        String script = Files.readString(SCRIPT);

        assertThat(html)
                .contains("data-request-retry hidden")
                .contains("重新加载 / 重试");
        assertThat(script)
                .contains("function setRetryVisible(visible)")
                .contains("function bindRetry()")
                .contains("loadAssetDetail();")
                .contains("failClosed(\"数据暂不可用\", true)")
                .contains("failClosed(\"资产标识不可验证\", false)");
        assertThat(count(script, "fetch(")).isEqualTo(1);
    }

    @Test
    void pageUsesOnlyDashboardHomeAndRejectsPlaceholderOrSiblingAssetFallback() throws Exception {
        String script = Files.readString(SCRIPT);

        assertThat(count(script, "fetch(")).isEqualTo(1);
        assertThat(script)
                .contains("/api/dashboard/home?")
                .contains("normalizeSymbol(home.selectedSymbol) !== requestedSymbol")
                .contains("slotType !== \"DEFAULT_SLOT\" && symbol === requestedSymbol")
                .contains("throw new Error(\"ASSET_DETAIL_NOT_VERIFIED\")")
                .contains("failClosed(\"数据暂不可用\", true)")
                .doesNotContain("/api/dashboard/detail")
                .doesNotContain("/api/evidence")
                .doesNotContain("/api/score")
                .doesNotContain("/api/ai")
                .doesNotContain("localStorage");
    }

    @Test
    void unavailableAnalysisCapabilitiesRemainExplicitlyDisabled() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html)
                .contains("Market Analysis")
                .contains("Evidence &amp; Scoring")
                .contains("Multi Timeframe")
                .contains("完整证据与八大评分读取合同尚未提供")
                .contains("4h / 1h / 15m / 5m 明细当前不可查看");
        assertThat(count(html, "disabled aria-disabled=\"true\"")).isEqualTo(2);
        assertThat(html)
                .doesNotContain("href=\"/api/")
                .doesNotContain("href=\"/evidence")
                .doesNotContain("href=\"/analysis");
    }

    @Test
    void responsiveDetailViewHasDarkModeTouchTargetsAndNoRootOverflow() throws Exception {
        String css = Files.readString(STYLES);

        assertThat(css)
                .contains("overflow-x: hidden")
                .contains("min-height: 44px")
                .contains("@media (max-width: 640px)")
                .contains("@media (max-width: 980px)")
                .contains("@media (prefers-color-scheme: dark)")
                .contains("env(safe-area-inset-top)")
                .contains("env(safe-area-inset-bottom)")
                .contains("--focus-ring: #005a9c")
                .contains("--focus-ring: #9ad5ff")
                .contains(".retry-button:focus-visible")
                .contains("outline: 3px solid var(--focus-ring)")
                .contains("[data-execution-plan][hidden]")
                .contains("[data-position-context][hidden]")
                .doesNotContain("outline: 3px solid var(--accent-soft)")
                .doesNotContain("font-size: clamp")
                .doesNotContain("letter-spacing: -")
                .doesNotContain("url(http")
                .doesNotContain("cdn");
    }

    @Test
    void overviewPagesExposeOnlyVerifiedAssetDetailContext() throws Exception {
        String desktop = Files.readString(DESKTOP_HOME);
        String mobile = Files.readString(MOBILE_HOME);
        String mobileScript = Files.readString(MOBILE_SCRIPT);

        assertThat(desktop)
                .contains("id=\"assetDetailLink\" hidden")
                .contains("function updateAssetDetailLink(symbol)")
                .contains("/dashboard/asset-detail?selectedSymbol=")
                .contains("slotType !== \"DEFAULT_SLOT\"");
        assertThat(mobile)
                .contains("data-asset-detail-link")
                .contains("/dashboard/asset-detail")
                .contains("!#lists.isEmpty(mobileAssets)");
        assertThat(mobileScript)
                .contains("function updateAssetDetailLink(symbol)")
                .contains("&view=mobile")
                .contains("updateAssetDetailLink(selectedCard ? symbol : null)");
    }

    private int count(String source, String target) {
        return (source.length() - source.replace(target, "").length()) / target.length();
    }
}
