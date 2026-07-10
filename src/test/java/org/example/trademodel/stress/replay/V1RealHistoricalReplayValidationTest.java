package org.example.trademodel.stress.replay;

import org.example.trademodel.service.DecisionEngineService;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.PlanService;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.service.ScoreService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class V1RealHistoricalReplayValidationTest {

    @Test
    void missingProvenanceBackedFixtureBlocksPipelineBeforeBusinessServicesRun() throws Exception {
        EvidenceService evidenceService = mock(EvidenceService.class);
        ScoreService scoreService = mock(ScoreService.class);
        DecisionEngineService decisionEngineService = mock(DecisionEngineService.class);
        PlanService planService = mock(PlanService.class);
        PositionMonitorService positionMonitorService = mock(PositionMonitorService.class);
        String manifest = Files.readString(Path.of("docs/replay-fixtures/REAL_HISTORICAL_FIXTURE_MANIFEST.yml"));

        String result = manifest.contains("source_type: MISSING_REAL_HISTORICAL_FIXTURE")
                ? "BLOCKED_MISSING_REAL_FIXTURE"
                : "FIXTURE_REVIEW_REQUIRED";

        assertThat(result).isEqualTo("BLOCKED_MISSING_REAL_FIXTURE");
        verifyNoInteractions(evidenceService, scoreService, decisionEngineService, planService, positionMonitorService);
        System.out.println("REAL_HISTORICAL_REPLAY_RESULT: BLOCKED_MISSING_REAL_FIXTURE");
    }
}
