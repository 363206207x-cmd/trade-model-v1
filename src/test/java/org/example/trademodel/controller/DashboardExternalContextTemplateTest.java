package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardExternalContextTemplateTest {
    @Test
    void dashboardTemplateContainsExternalContextPanelIdsAndEndpoint() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));

        assertThat(html).contains(
                "externalContextPanel",
                "externalContextRuntimeStatusValue",
                "externalContextSourceHealthValue",
                "externalContextActiveCountValue",
                "externalContextMacroCountValue",
                "externalContextNewsCountValue",
                "externalContextRiskLevelValue",
                "externalContextBlockingValue",
                "externalContextNextEventValue",
                "externalContextLatestEventValue",
                "externalContextEventWindowValue",
                "externalContextReviewOnlyValue",
                "externalContextExecutionBoundaryValue",
                "externalContextReasonValue",
                "/api/external-context/dashboard-status");
    }
}
