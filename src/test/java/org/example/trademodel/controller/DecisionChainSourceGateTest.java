package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DecisionChainSourceGateTest {

    @Test
    void directEvidenceBuildCannotBypassAnalysisRun() {
        EvidenceService evidenceService = mock(EvidenceService.class);
        ApiResponse<List<EvidenceItemVO>> response = new EvidenceController(evidenceService)
                .buildEvidence(new AssetAnalysisVO());

        assertThat(response.getCode()).isEqualTo(409);
        assertThat(response.getData()).isNull();
        assertThat(response.getMsg())
                .isEqualTo("DIRECT_EVIDENCE_BUILD_DISABLED_USE_ANALYSIS_RUN_DECISION_CHAIN");
        verifyNoInteractions(evidenceService);
    }

    @Test
    void directScoreBuildAndSyntheticListCannotBypassAnalysisRun() {
        ScoreService scoreService = mock(ScoreService.class);
        ScoreController controller = new ScoreController(scoreService);

        ApiResponse<List<ScoreItemVO>> build = controller.buildScore(new AssetAnalysisVO());
        ApiResponse<List<ScoreItemVO>> list = controller.getScoreList("BTCUSDT", "5m");

        assertThat(build.getCode()).isEqualTo(409);
        assertThat(list.getCode()).isEqualTo(409);
        assertThat(build.getData()).isNull();
        assertThat(list.getData()).isNull();
        assertThat(build.getMsg())
                .isEqualTo("DIRECT_SCORE_BUILD_DISABLED_USE_ANALYSIS_RUN_DECISION_CHAIN");
        assertThat(list.getMsg()).isEqualTo(build.getMsg());
        verifyNoInteractions(scoreService);
    }
}
