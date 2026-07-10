package org.example.trademodel.derivatives;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.coinglass.CoinGlassDerivativesSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DerivativesBusinessSafetyArchitectureTest {

    @Test
    void CoinGlassSnapshotIsSharedAcrossDecisionPushMonitorDashboard() {
        String assembler = source("src/main/java/org/example/trademodel/service/impl/AnalysisAssemblerServiceImpl.java");
        String recheck = source("src/main/java/org/example/trademodel/service/impl/PushRecheckServiceImpl.java");
        String monitor = source("src/main/java/org/example/trademodel/service/impl/PositionMonitorServiceImpl.java");
        assertThat(assembler).contains("DerivativesSnapshotReadPort", "readCached");
        assertThat(recheck).contains("DerivativesSnapshotReadPort", "readCached");
        assertThat(monitor).contains("DerivativesSnapshotReadPort", "readCached");
    }

    @Test
    void positionMonitorReadsCoinGlassAtMinuteCadenceNotPriceCadence() {
        String monitor = source("src/main/java/org/example/trademodel/service/impl/PositionMonitorServiceImpl.java");
        assertThat(monitor).contains("AssetPriority.P0_POSITION",
                "derivativesBusinessIntegrationService.monitorRefreshSeconds()");
        assertThat(monitor).doesNotContain("derivativesSnapshotReadPort.readCached(\n                    position.getAssetSymbol(), AssetPriority.P0_POSITION, Duration.ofSeconds(15)");
    }

    @Test
    void priceTickDoesNotCallCoinGlass() {
        CoinGlassDerivativesSnapshotService snapshotService = mock(CoinGlassDerivativesSnapshotService.class);
        CoordinatedDerivativesSnapshotReadAdapter adapter = new CoordinatedDerivativesSnapshotReadAdapter(snapshotService);
        ProviderCallResult<DerivativesRiskSnapshot> expected = new ProviderCallResult<>(null, null, null);
        when(snapshotService.peek("BTCUSDT", AssetPriority.P0_POSITION, Duration.ofSeconds(60), "trace"))
                .thenReturn(expected);

        assertThat(adapter.readCached("BTCUSDT", AssetPriority.P0_POSITION, Duration.ofSeconds(60), "trace"))
                .isSameAs(expected);
        verify(snapshotService).peek("BTCUSDT", AssetPriority.P0_POSITION, Duration.ofSeconds(60), "trace");
        verifyNoMoreInteractions(snapshotService);
    }

    @Test
    void derivativesRiskDoesNotCreateUserPosition() {
        String business = source("src/main/java/org/example/trademodel/derivatives/DerivativesBusinessIntegrationService.java");
        assertThat(business).doesNotContain("UserPositionMapper", "manualOpen", "insertUserPosition");
    }

    @Test
    void closedPositionNoLongerConsumesPositionDerivativesPriority() {
        String monitor = source("src/main/java/org/example/trademodel/service/impl/PositionMonitorServiceImpl.java");
        assertThat(monitor).contains("listOpenPositions()", "validateActivePosition(position)");
        assertThat(monitor).contains("UserPosition status must be OPEN or PARTIALLY_CLOSED");
    }

    @Test
    void routineScanDoesNotInvokeAi() {
        String business = source("src/main/java/org/example/trademodel/derivatives/DerivativesBusinessIntegrationService.java");
        assertThat(business).doesNotContain("AiDecision", "AiProvider", ".review(");
    }

    @Test
    void scoreOrProfileChangeAloneDoesNotInvokeAi() {
        String business = source("src/main/java/org/example/trademodel/derivatives/DerivativesBusinessIntegrationService.java");
        assertThat(business).contains("applyScoreAdjustments");
        assertThat(business).doesNotContain("AiDecisionOrchestratorService");
    }

    @Test
    void derivativesDecisionAdjustmentIsAppliedOnlyOncePerAnalysis() {
        String assembler = source("src/main/java/org/example/trademodel/service/impl/AnalysisAssemblerServiceImpl.java");
        assertThat(assembler).containsOnlyOnce("applyDecisionAdjustments(decision, derivativesAssessment)");
    }

    @Test
    void opportunityAccountRiskUsesReadonlyAdapterAndFailsClosed() {
        String assembler = source("src/main/java/org/example/trademodel/service/impl/AnalysisAssemblerServiceImpl.java");
        assertThat(assembler).contains("UserPositionRiskAdapter", "userPositionRiskAdapter.currentRisk()",
                "if (userPositionRiskAdapter == null) return false");
        assertThat(assembler).doesNotContain("dataQualityScore, true, planBoundaryComplete");
    }

    @Test
    void noAutoOpenCloseReverseOrderOrTrading() {
        String business = source("src/main/java/org/example/trademodel/derivatives/DerivativesBusinessIntegrationService.java");
        assertThat(business).doesNotContain("autoOpen", "autoClose", "autoReverse", "OrderService");
        assertThat(business).contains("setNotExecutable(true)", "setNotOrderExecution(true)",
                "setNotAutoTrading(true)", "setNotUserPositionCreation(true)");
    }

    @Test
    void noExternalPushOrTelegramSend() {
        String business = source("src/main/java/org/example/trademodel/derivatives/DerivativesBusinessIntegrationService.java");
        assertThat(business).doesNotContain("Telegram", "sendMessage", "PushDelivery", "ExternalChannel");
    }

    private static String source(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (Exception failure) {
            throw new AssertionError("cannot read source " + relativePath, failure);
        }
    }
}
