package org.example.trademodel.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WebLiveDashboardContractTest {

    @Test
    void desktopUsesAuthenticatedSseWithBoundedFallback() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        String controller = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/controller/DashboardHomeController.java"));

        assertThat(controller).contains("/stream").contains("SseEmitter");
        assertThat(desktop).contains("new EventSource(\"/api/dashboard/stream\")")
                .contains("15000").contains("60000").contains("snapshotVersion")
                .contains("visibilitychange")
                .contains("sameLiveDecision")
                .contains("confidenceCap")
                .contains("baseConfidenceLevel")
                .contains("analysis-preview?timeframe=1h");
    }

    @Test
    void liveRefreshDoesNotTriggerAiOrMutatePositions() throws Exception {
        String desktop = Files.readString(Path.of("src/main/resources/static/js/home-runtime.js"));
        assertThat(desktop).contains("function connectHomeStream")
                .contains("function applyHomeLiveEvent")
                .contains("function scheduleHomeFallbackPoll");
        assertThat(desktop.substring(desktop.indexOf("function applyHomeLiveEvent"),
                        desktop.indexOf("function stableSubmissionId")))
                .doesNotContain("analysis-preview", "method: \"POST\"", "/api/user-positions");
    }
}
