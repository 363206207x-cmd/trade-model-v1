package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;
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
    void hotResetCompatibilityMethodUsesHotResetTriggerType() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, new AnalysisRunProperties());

        service.executeAnalysis("BTCUSDT", "1m", "HOT_RESET:hre-1");

        assertThat(orchestrator.commands).hasSize(1);
        assertThat(orchestrator.commands.get(0).getTriggerType()).isEqualTo(AnalysisRunTriggerType.HOT_RESET_REBUILD);
        assertThat(orchestrator.commands.get(0).getTriggerReference()).isEqualTo("hre-1");
    }

    private static class CapturingOrchestrator implements AnalysisRunOrchestrator {
        private final List<AnalysisRunCommand> commands = new ArrayList<>();

        @Override
        public AnalysisRunResult run(AnalysisRunCommand command) {
            commands.add(command);
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
}
