package org.example.trademodel.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class StaticNoTradeInstructionGuardTest {

    private static final Path DASHBOARD_TEMPLATE =
            Path.of("src/main/resources/templates/dashboard.html");
    private static final String SKELETON_START =
            "<section class=\"card module-status-card review-display-card\" id=\"candidateReviewDisplay\"";
    private static final String SKELETON_END = "</section>";
    private static final String POSITION_EXECUTION_ROW_START =
            "<section class=\"latest-decision-grid\"";
    private static final String POSITION_EXECUTION_ROW_END =
            "<section class=\"latest-ai-grid\"";
    private static final String ALLOWED_NEGATIVE_CONTEXT =
            "no order, execution, reverse, signal, or auto-trading action is available here.";
    private static final List<String> MANDATORY_SAFE_LABELS = List.of(
            "review-only",
            "manual review required",
            "not trade instruction"
    );
    private static final List<String> FORBIDDEN_POSITIVE_ACTIONABLE_LABELS = List.of(
            "buy",
            "sell",
            "open",
            "close",
            "reverse",
            "signal",
            "trade-ready",
            "ready-to-trade",
            "executable",
            "production valid",
            "auto-trading"
    );
    private static final List<String> REVIEWABLE_NEGATIVE_CONTEXT_TERMS = List.of(
            "order",
            "execution",
            "reverse",
            "signal",
            "auto-trading"
    );
    private static final List<String> FORBIDDEN_ACTION_SURFACES = List.of(
            "<button",
            "<a ",
            "href=",
            "<form",
            "onclick",
            "addEventListener",
            "fetch(",
            "/api/",
            "localStorage"
    );
    private static final List<String> FORBIDDEN_FIELD_SURFACES = List.of(
            "entryPrice",
            "stopPrice",
            "takeProfitPrice",
            "riskRewardValue",
            "tradeReady",
            "readyToTrade",
            "orderAction",
            "executionAction"
    );
    private static final List<String> FORBIDDEN_READINESS_SURFACES = List.of(
            "readiness",
            "production VALID",
            "executable"
    );

    @Test
    void dashboardSkeletonContainsMandatorySafeLabels() throws Exception {
        String skeleton = normalizedCandidateReviewSkeleton();

        for (String label : MANDATORY_SAFE_LABELS) {
            assertThat(skeleton).contains(label);
        }
    }

    @Test
    void dashboardSkeletonDoesNotExposePositiveTradeInstructionLabels() throws Exception {
        String skeleton = normalizedCandidateReviewSkeleton();
        String skeletonWithoutAllowedNegativeContext = skeleton.replace(ALLOWED_NEGATIVE_CONTEXT, "");

        assertThat(skeleton).contains(ALLOWED_NEGATIVE_CONTEXT);
        for (String term : FORBIDDEN_POSITIVE_ACTIONABLE_LABELS) {
            assertThat(skeletonWithoutAllowedNegativeContext).doesNotContain(term);
        }
        for (String term : REVIEWABLE_NEGATIVE_CONTEXT_TERMS) {
            assertThat(skeletonWithoutAllowedNegativeContext).doesNotContain(term);
        }
    }

    @Test
    void dashboardSkeletonDoesNotIntroduceActionSurfacesOrDecisionPaths() throws Exception {
        String skeleton = candidateReviewSkeleton();
        String normalizedSkeleton = skeleton.toLowerCase(Locale.ROOT);

        for (String surface : FORBIDDEN_ACTION_SURFACES) {
            assertThat(normalizedSkeleton).doesNotContain(surface.toLowerCase(Locale.ROOT));
        }
    }

    @Test
    void dashboardSkeletonDoesNotExposeRealValueOrActionFieldNames() throws Exception {
        String skeleton = candidateReviewSkeleton();

        for (String field : FORBIDDEN_FIELD_SURFACES) {
            assertThat(skeleton).doesNotContain(field);
        }
    }

    @Test
    void dashboardSkeletonDoesNotExposeReadinessProductionValidOrExecutableSurface() throws Exception {
        String skeleton = candidateReviewSkeleton();
        String normalizedSkeleton = skeleton.toLowerCase(Locale.ROOT);

        for (String surface : FORBIDDEN_READINESS_SURFACES) {
            assertThat(normalizedSkeleton).doesNotContain(surface.toLowerCase(Locale.ROOT));
        }
    }

    @Test
    void dashboardPositionExecutionRowKeepsManualPositionDisplayPassive() throws Exception {
        String row = positionExecutionRow();

        assertThat(row).contains("持仓监控 Top3");
        assertThat(row).contains("用户真实持仓 · 与系统执行建议独立");
        assertThat(row).contains("暂无活动持仓");
        assertThat(row).contains("系统只监控用户实际开仓并手动录入的持仓。");
        assertThat(row).contains("执行计划不会自动创建仓位。");
        assertThat(row).contains("最终执行计划");
        assertThat(row).contains("仅展示冲突处理与规则校验通过的最终结果");
        assertThat(row).contains("暂无最终执行计划");
        assertThat(row).contains("当前资产尚未形成通过规则校验的计划。");
        assertThat(row).contains("manualPositionBtn", "录入持仓");
        assertThat(row).doesNotContain("<form");
        assertThat(row).doesNotContain("openPositionBtn");
        assertThat(row).doesNotContain("closePositionBtn");
        assertThat(row).doesNotContain("orderBtn");
        assertThat(row).doesNotContain("executeBtn");
        assertThat(row).doesNotContain("buyBtn");
        assertThat(row).doesNotContain("sellBtn");
    }

    private String normalizedCandidateReviewSkeleton() throws Exception {
        return candidateReviewSkeleton()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private String candidateReviewSkeleton() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);
        int sectionStart = html.indexOf(SKELETON_START);
        assertThat(sectionStart).isNotNegative();

        int sectionEnd = html.indexOf(SKELETON_END, sectionStart);
        assertThat(sectionEnd).isNotNegative();

        return html.substring(sectionStart, sectionEnd + SKELETON_END.length());
    }

    private String positionExecutionRow() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);
        int rowStart = html.indexOf(POSITION_EXECUTION_ROW_START);
        assertThat(rowStart).isNotNegative();

        int rowEnd = html.indexOf(POSITION_EXECUTION_ROW_END, rowStart);
        assertThat(rowEnd).isNotNegative();

        return html.substring(rowStart, rowEnd);
    }
}
