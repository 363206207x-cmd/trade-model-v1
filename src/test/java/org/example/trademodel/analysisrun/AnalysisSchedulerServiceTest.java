package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisSchedulerServiceTest {
    @Test
    void scheduledCycleIsDisabledByDefault() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisRunProperties properties = new AnalysisRunProperties();
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, properties);

        assertThat(service.runScheduledCycle()).isEmpty();
        assertThat(orchestrator.commands).isEmpty();
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
        properties.getScheduler().setTimeframes(List.of("1m", "5m"));
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, properties);

        List<AnalysisRunResult> results = service.runScheduledCycle();

        assertThat(results).hasSize(4);
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
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, properties);

        assertThat(service.runScheduledCycle()).isEmpty();
        assertThat(orchestrator.commands).isEmpty();
        assertThat(service.status()).containsEntry("configValid", false);
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
}
