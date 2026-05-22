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
}
