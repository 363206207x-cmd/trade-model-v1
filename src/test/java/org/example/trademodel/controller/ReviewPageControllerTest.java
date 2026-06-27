package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class ReviewPageControllerTest {

    @Test
    void dashboardRouteRendersReviewCenterModeWithoutAnalysisId() {
        ReviewPageController controller = new ReviewPageController();
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.reviewDashboard(model);

        assertThat(view).isEqualTo("review");
        assertThat(model.get("reviewCenterMode")).isEqualTo(true);
        assertThat(model.get("analysisId")).isEqualTo("");
        assertThat(model.get("title")).isEqualTo("复盘中心");
    }

    @Test
    void analysisRouteKeepsSingleAnalysisMode() {
        ReviewPageController controller = new ReviewPageController();
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.reviewPage("ana-1", model);

        assertThat(view).isEqualTo("review");
        assertThat(model.get("reviewCenterMode")).isEqualTo(false);
        assertThat(model.get("analysisId")).isEqualTo("ana-1");
        assertThat(model.get("title")).isEqualTo("复盘 · ana-1");
    }
}
