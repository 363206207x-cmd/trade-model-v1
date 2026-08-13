package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.service.watchlistsource.AssetPoolScanTarget;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisSchedulerServiceTest {
    @Test
    void scheduledCycleIsDisabledByDefault() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisRunProperties properties = new AnalysisRunProperties();
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, properties);

        assertThat(service.runScheduledCycle()).isEmpty();
        assertThat(orchestrator.commands).isEmpty();
        assertThat(properties.getScheduler().getTimeframes())
                .containsExactly("5m", "15m", "1h", "4h");
        assertThat(service.status()).containsEntry("enabled", false)
                .containsEntry("inMemoryCacheRemoved", true)
                .containsEntry("manualThreadRemoved", true);
    }

    @Test
    void enabledSchedulerRoutesEverySymbolTimeframeThroughOrchestrator() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisRunProperties properties = new AnalysisRunProperties();
        properties.getScheduler().setEnabled(true);
        properties.getScheduler().setSymbols(List.of("BTCUSDT", "ETHUSDT"));
        properties.getScheduler().setTimeframes(List.of("5m", "15m", "1h", "4h"));
        AnalysisSchedulerService service = scheduler(orchestrator, properties, List.of("BTCUSDT", "ETHUSDT"));

        List<AnalysisRunResult> results = service.runScheduledCycle();

        assertThat(results).hasSize(8);
        assertThat(orchestrator.commands).extracting(AnalysisRunCommand::getTriggerType)
                .containsOnly(AnalysisRunTriggerType.SCHEDULED);
    }

    @Test
    void invalidSchedulerConfigFailsClosedWithoutCallingOrchestrator() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisRunProperties properties = new AnalysisRunProperties();
        properties.getScheduler().setEnabled(true);
        properties.getScheduler().setSymbols(List.of("BTCUSDT"));
        properties.getScheduler().setTimeframes(List.of("7m"));
        AnalysisSchedulerService service = scheduler(orchestrator, properties, List.of("BTCUSDT"));

        assertThat(service.runScheduledCycle()).isEmpty();
        assertThat(orchestrator.commands).isEmpty();
        assertThat(service.status()).containsEntry("configValid", false);
    }

    @Test
    void schedulerPreservesPoolOwnerAndUsesConfiguredStateCadence() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisRunProperties properties = new AnalysisRunProperties();
        properties.getScheduler().setEnabled(true);
        AssetPoolService assetPoolService = mock(AssetPoolService.class);
        AssetStateMapper assetStateMapper = mock(AssetStateMapper.class);
        AssetPoolScanTarget target = new AssetPoolScanTarget("USER", 42L, 9001L, "BTCUSDT");
        when(assetPoolService.listScanTargets()).thenReturn(List.of(target));
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 0, 0);
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "5m"))
                .thenReturn(state(AssetStateEnum.CANDIDATE, now.minusSeconds(100)));
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "15m"))
                .thenReturn(state(AssetStateEnum.OBSERVING, now.minusSeconds(100)));
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "1h"))
                .thenReturn(state(AssetStateEnum.TRIGGERED, now.minusSeconds(61)));
        AnalysisSchedulerService service = new AnalysisSchedulerService(
                orchestrator, properties,
                Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC),
                assetPoolService, assetStateMapper);

        List<AnalysisRunResult> results = service.runScheduledCycle();

        assertThat(results).hasSize(2);
        assertThat(orchestrator.commands).extracting(AnalysisRunCommand::getTimeframe)
                .containsExactly("1h", "4h");
        assertThat(orchestrator.commands).allSatisfy(command -> {
            assertThat(command.getOwnerType()).isEqualTo("USER");
            assertThat(command.getOwnerId()).isEqualTo(42L);
            assertThat(command.getAssetId()).isEqualTo(9001L);
        });
    }

    @Test
    void hotResetCompatibilityMethodUsesHotResetTriggerType() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, new AnalysisRunProperties());

        service.executeAnalysis("BTCUSDT", "1m", "HOT_RESET:hre-1");

        assertThat(orchestrator.commands).hasSize(1);
        assertThat(orchestrator.commands.get(0).getTriggerType()).isEqualTo(AnalysisRunTriggerType.HOT_RESET_REBUILD);
        assertThat(orchestrator.commands.get(0).getTriggerReference()).isEqualTo("hre-1");
    }

    @Test
    void executeAnalysisReturnsFailureWithoutMinimalAnalysisForNonExecutedResult() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator(
                AnalysisRunResult.inProgress(run("ana-in-progress", "STARTED")));
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, new AnalysisRunProperties());

        ApiResponse<AssetAnalysisVO> response = service.executeAnalysis("BTCUSDT", "1m", "HOT_RESET:hre-blocked");

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getData()).isNull();
        assertThat(response.getMsg()).contains("CONCURRENT_TRIGGER_BLOCKED");
    }

    @Test
    void executeAnalysisAllowsReusableDuplicateSuccessWithoutExecutingAnalysisAgain() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator(
                AnalysisRunResult.duplicateSuccess(run("ana-existing-success", "SUCCESS")));
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, new AnalysisRunProperties());

        ApiResponse<AssetAnalysisVO> response = service.executeAnalysis("BTCUSDT", "1m", "HOT_RESET:hre-duplicate");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMsg()).isEqualTo("EXISTING_SUCCESS");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getAnalysisId()).isEqualTo("ana-existing-success");
    }

    private static class CapturingOrchestrator implements AnalysisRunOrchestrator {
        private final List<AnalysisRunCommand> commands = new ArrayList<>();
        private final List<AnalysisRunResult> results = new ArrayList<>();

        private CapturingOrchestrator(AnalysisRunResult... results) {
            this.results.addAll(List.of(results));
        }

        @Override
        public AnalysisRunResult run(AnalysisRunCommand command) {
            commands.add(command);
            if (!results.isEmpty()) {
                return results.remove(0);
            }
            AnalysisRunDO run = new AnalysisRunDO();
            run.setAnalysisId("ana-" + commands.size());
            run.setTraceId("trace-" + commands.size());
            run.setRequestId(command.getRequestId());
            run.setSymbol(command.getSymbol());
            run.setTimeframe(command.getTimeframe());
            run.setTriggerType(command.getTriggerType().name());
            run.setTriggerReference(command.getTriggerReference());
            AssetAnalysisVO analysis = new AssetAnalysisVO();
            analysis.setAnalysisId(run.getAnalysisId());
            analysis.setSymbol(run.getSymbol());
            analysis.setTimeframe(run.getTimeframe());
            return AnalysisRunResult.executed(run, analysis, false, false);
        }
    }

    private static AnalysisSchedulerService scheduler(CapturingOrchestrator orchestrator,
                                                      AnalysisRunProperties properties,
                                                      List<String> poolSymbols) {
        AssetPoolService assetPoolService = mock(AssetPoolService.class);
        when(assetPoolService.listScanSymbols()).thenReturn(poolSymbols);
        return new AnalysisSchedulerService(orchestrator, properties, Clock.systemUTC(), assetPoolService);
    }

    private static AnalysisRunDO run(String analysisId, String status) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setTraceId("trace-" + analysisId);
        run.setRequestId("req-" + analysisId);
        run.setSymbol("BTCUSDT");
        run.setTimeframe("1m");
        run.setTriggerType(AnalysisRunTriggerType.HOT_RESET_REBUILD.name());
        run.setTriggerReference("hre-test");
        run.setStatus(status);
        return run;
    }

    private static AssetStateDO state(AssetStateEnum state, LocalDateTime lastUpdateTime) {
        AssetStateDO row = new AssetStateDO();
        row.setState(state);
        row.setLastUpdateTime(lastUpdateTime);
        return row;
    }
}
