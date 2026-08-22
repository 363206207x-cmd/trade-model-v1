package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class B123OwnerCopyAndPositionDetailContractTest {
    private static final Path HOME = Path.of("src/main/resources/templates/home.html");
    private static final Path WORKSPACE = Path.of("src/main/resources/templates/workspace.html");
    private static final Path LOGIN = Path.of("src/main/resources/templates/login.html");
    private static final Path HOME_JS = Path.of("src/main/resources/static/js/home-runtime.js");
    private static final Path WORKSPACE_JS = Path.of("src/main/resources/static/js/workspace.js");
    private static final Path FRONTEND_CONTRACT = Path.of("src/main/resources/static/js/frontend-contract.js");
    private static final Path WORKSPACE_CONTROLLER = Path.of(
            "src/main/java/org/example/trademodel/controller/DesktopWorkspaceController.java");
    private static final Path HOME_SERVICE = Path.of(
            "src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java");
    private static final Path UI_REVIEW_HOME_SERVICE = Path.of(
            "src/main/java/org/example/trademodel/uireview/UiReviewDashboardHomeService.java");

    @Test
    void formalWebUsesRineLogicShortTitlesAndEnglishOnlyTabButtons() throws Exception {
        String home = Files.readString(HOME);
        String workspace = Files.readString(WORKSPACE);
        String login = Files.readString(LOGIN);

        assertThat(home).contains("<title>RINE LOGIC</title>", ">RINE LOGIC</h1>",
                ">资产</h2>", ">持仓监控</h2>", ">执行计划</h2>", ">AI 分析</h2>");
        assertThat(workspace).contains("' · RINE LOGIC'", ">RINE LOGIC</strong>",
                "<h2 id=\"positionsHeading\">持仓监控</h2>");
        assertThat(login).contains("<title>登录 · RINE LOGIC</title>", ">RINE LOGIC</h1>");
        assertThat(home + workspace + login)
                .doesNotContain("Fundamental AI", "个人复核入口", "多源证据决策系统",
                        "机会资产 ·", "持仓监控 · 基于已录入", "AI 分析工作区");
        assertThat(home + workspace).contains(">GPT</button>", ">Gemini</button>", ">Grok</button>");
    }

    @Test
    void pageHeadersHaveNoSloganSubtitleProjectionOrDom() throws Exception {
        String controller = Files.readString(WORKSPACE_CONTROLLER);
        String workspace = Files.readString(WORKSPACE);

        assertThat(controller).contains("page(model, \"analysis\", \"分析\"")
                .doesNotContain("pageSubtitle", "管理观察资产并按需启动分析",
                        "查看真实持仓、风险变化与监控结论", "管理风险偏好、资产池与数据源状态");
        assertThat(workspace).doesNotContain("pageSubtitle", "<p th:text=\"${pageSubtitle}\"");
    }

    @Test
    void genericDefensiveSuffixesAreAbsentFromFormalRuntimeSurface() throws Exception {
        String visibleRuntime = Files.readString(HOME) + Files.readString(WORKSPACE)
                + Files.readString(LOGIN) + Files.readString(HOME_JS)
                + Files.readString(WORKSPACE_JS) + Files.readString(FRONTEND_CONTRACT)
                + Files.readString(HOME_SERVICE);

        assertThat(visibleRuntime).doesNotContain(
                "仅供人工复核", "不会显示推测状态", "不会使用旧输出补齐",
                "不会使用原始 JSON 推断结论", "所有下游动作保持关闭",
                "不会强制显示就绪", "历史监控不会冒充当前判断");
        assertThat(visibleRuntime).contains("非最终计划", "等待监控数据", "监控数据已过期",
                "监控来源不可用", "当前不可查看");
    }

    @Test
    void statusStripUsesOnlyFormalOwnersAndUiReviewDoesNotClaimProductionTruth() throws Exception {
        String service = Files.readString(HOME_SERVICE);
        String uiReview = Files.readString(UI_REVIEW_HOME_SERVICE);
        String runtime = Files.readString(HOME_JS);

        assertThat(service).contains("globalDataUpdateCard(globalDataUpdatedAt)",
                        "LocalRealDataStatusService.latestClosedBarAt", "ProviderReadiness.providers",
                        "positions.size() + \" 笔\"", "Boolean.TRUE.equals(hotResetFired) ? \"已触发\" : \"关闭\"")
                .doesNotContain("case \"CONNECTED\" -> card(\"dataQuality\"", "valueLabel = \"正常\"");
        assertThat(runtime).contains("has(state.dataQuality?.value)",
                "statusValue(state.serviceAvailability, \"—\")",
                "statusValue(state.accountStatus, \"—\")", "statusValue(state.hotReset, \"—\")");
        assertThat(uiReview).contains("\"全局数据\", \"—\", \"SOURCE_UNAVAILABLE\"",
                        "\"服务可用性\", \"—\", \"SOURCE_UNAVAILABLE\"")
                .doesNotContain("\"全局数据质量\", \"新鲜\"", "\"服务可用性\", \"正常\"");
    }

    @Test
    void positionDetailHasNoSelfLinkAndCloseActionUsesExplicitAllowlist() throws Exception {
        String script = Files.readString(WORKSPACE_JS);
        String renderer = slice(script, "function renderPosition", "let positionRows = []");
        String detailLoader = slice(script, "async function loadPositionDetail", "function renderHistoricalPositionDetail");

        assertThat(renderer).contains("options?.showDetailLink !== false", "showDetailLink ?")
                .doesNotContain("status !== \"CLOSED\"");
        assertThat(detailLoader).contains("renderPosition(position, monitor, { showDetailLink: false })",
                        "syncPositionCloseAction(closeAction, null)",
                        "syncPositionCloseAction(closeAction, position.status)")
                .doesNotContain("renderPosition(position, monitor);");
        assertThat(script).contains("[\"OPEN\", \"PARTIALLY_CLOSED\"].includes");
    }

    @Test
    void waitingTriggerCopyRequiresRevalidationBeforeManualConfirmation() throws Exception {
        String runtime = Files.readString(HOME_JS) + Files.readString(WORKSPACE_JS);
        assertThat(runtime).contains("等待触发；触发后重新校验，通过后再进入人工确认")
                .doesNotContain("等待触发后进入人工确认");
    }

    private static String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
