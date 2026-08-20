package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HomeUiReviewRuntimeContractTest {
    @Test
    void launcherUsesTheActualDashboardWithAnIsolatedProfile() throws Exception {
        String launcher = Files.readString(Path.of("scripts/run-local.sh"));
        String fixture = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/uireview/UiReviewDashboardHomeService.java"));

        assertThat(launcher).contains("--ui-review", "SPRING_PROFILES_ACTIVE=\"ui-review\"",
                        "UI_REVIEW_MODE=${UI_REVIEW_MODE}", "HOME_URL=\"${LOCAL_URL}/dashboard\"")
                .doesNotContain("ui-review.html", "home-demo.html", "dashboard-preview.html");
        assertThat(fixture).contains("@Profile(\"ui-review\")", "@Primary",
                        "implements DashboardHomeService", "setAssets(assets)", "setPositions(positions())")
                .doesNotContain("Mapper", "Repository", "AUTO_ORDER", "AUTO_CLOSE", "AUTO_REVERSE");
    }

    @Test
    void homeCopyUsesFinalCompactActionsAndContainsNoProhibitedDefaults() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/home.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String visibleSource = html + "\n" + script;

        assertThat(visibleSource).contains("分析", "添加", "已添加", "暂无重点机会", "暂无持仓")
                .doesNotContain(
                        "按需分析", "加入观察资产池", "已在观察资产池",
                        "请先从搜索结果中选择", "请尝试其他名称或交易对",
                        "正在分析…", "正在添加…", "Final Bias", "Plan Mode</small>",
                        "当前没有通过规则校验的 Final Execution Plan",
                        "仅供参考", "不构成投资建议", "请自行判断");
    }

    @Test
    void populatedReviewFixtureIsNotEmbeddedInProductionHtmlOrJavascript() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/home.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));

        assertThat(html + script).doesNotContain(
                "ui-review-opportunity", "ui-review-final-btc", "62,800–63,200",
                "SOL 持仓风险显著上升", "美国 CPI 公布");
    }
}
