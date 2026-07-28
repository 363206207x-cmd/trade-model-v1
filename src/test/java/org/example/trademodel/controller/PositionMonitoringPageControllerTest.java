package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class PositionMonitoringPageControllerTest {

    @Test
    void desktopRouteKeepsAValidExactPositionId() {
        PositionMonitoringPageController controller = new PositionMonitoringPageController();

        ModelAndView result = controller.desktopPositionMonitoring(" 42 ");

        assertThat(result.getViewName()).isEqualTo("position-monitoring");
        assertThat(result.getStatus()).isNull();
        assertThat(result.getModel())
                .containsEntry("requestedPositionId", "42")
                .containsEntry("invalidPositionId", false)
                .containsEntry("mobileView", false);
    }

    @Test
    void mobileRouteAllowsNoSelectionWithoutInventingAnIdentity() {
        PositionMonitoringPageController controller = new PositionMonitoringPageController();

        ModelAndView result = controller.mobilePositionMonitoring(null);

        assertThat(result.getViewName()).isEqualTo("position-monitoring");
        assertThat(result.getStatus()).isNull();
        assertThat(result.getModel())
                .containsEntry("requestedPositionId", "")
                .containsEntry("invalidPositionId", false)
                .containsEntry("mobileView", true);
    }

    @Test
    void invalidOrNonPositiveIdentityFailsClosedBeforeFrontendReads() {
        PositionMonitoringPageController controller = new PositionMonitoringPageController();

        for (String invalid : new String[]{
                "0", "-1", "BTCUSDT", "1.0", "9223372036854775808", "92233720368547758070"
        }) {
            ModelAndView result = controller.mobilePositionMonitoring(invalid);

            assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getModel())
                    .containsEntry("requestedPositionId", "")
                    .containsEntry("invalidPositionId", true)
                    .containsEntry("mobileView", true);
        }
    }

    @Test
    void normalizationNeverUsesSymbolTimeOrLatestFallbacks() {
        assertThat(PositionMonitoringPageController.normalizePositionId("17")).isEqualTo("17");
        assertThat(PositionMonitoringPageController.normalizePositionId(" 17 ")).isEqualTo("17");
        assertThat(PositionMonitoringPageController.normalizePositionId("SOLUSDT")).isNull();
        assertThat(PositionMonitoringPageController.normalizePositionId("latest")).isNull();
        assertThat(PositionMonitoringPageController.normalizePositionId("2026-07-28")).isNull();
    }
}
