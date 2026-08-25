package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.entity.AnalysisRunDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentAssetPoolPreviewReasonTest {

    @Test
    void preservesAuthoritativeOhlcvFailureAsAStableReasonCode() {
        AnalysisRunResult result = AnalysisRunResult.failed(
                new AnalysisRunDO(), "AUTHORITATIVE_OHLCV_UNAVAILABLE:5m");

        assertThat(PersistentAssetPoolService.previewReasonCode(result))
                .isEqualTo("AUTHORITATIVE_OHLCV_UNAVAILABLE");
    }

    @Test
    void preservesRealMarketEnvironmentFailureAsAStableReasonCode() {
        AnalysisRunResult result = AnalysisRunResult.failed(
                new AnalysisRunDO(), "REAL_MARKET_ENVIRONMENT_REQUIRED");

        assertThat(PersistentAssetPoolService.previewReasonCode(result))
                .isEqualTo("REAL_MARKET_ENVIRONMENT_UNAVAILABLE");
    }
}
