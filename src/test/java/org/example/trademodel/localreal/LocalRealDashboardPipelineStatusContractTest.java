package org.example.trademodel.localreal;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRealDashboardPipelineStatusContractTest {

    @Test
    void dashboardShowsMarketReadyAnalysisFailedSeparately() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));

        assertThat(template).contains(
                "localRealPipelineBanner",
                "真实行情已就绪",
                "分析链未完成",
                "latestAnalysisFailureCode",
                "/api/local-real/status");
        assertThat(template).doesNotContain("placeholder decision");
    }
}
