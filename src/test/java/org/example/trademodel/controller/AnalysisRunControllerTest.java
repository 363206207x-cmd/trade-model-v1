package org.example.trademodel.controller;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunInputException;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunProperties;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.analysistrace.AnalysisTraceService;
import org.example.trademodel.analysistrace.AnalysisTraceSnapshot;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisRunControllerTest {
    @Test
    void manualRunMissingTimeframeReturnsHttp400WithoutOrchestratorCall() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator(false);
        AnalysisRunController controller = controller(orchestrator, new StubTraceService());
        AnalysisRunController.AnalysisRunRequest request = new AnalysisRunController.AnalysisRunRequest();
        request.setSymbol("BTCUSDT");

        ResponseEntity<ApiResponse<AnalysisRunResult>> response = controller.run(request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(orchestrator.called).isFalse();
    }

    @Test
    void manualRunIllegalTimeframeReturnsHttp400() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator(true);
        AnalysisRunController controller = controller(orchestrator, new StubTraceService());
        AnalysisRunController.AnalysisRunRequest request = new AnalysisRunController.AnalysisRunRequest();
        request.setSymbol("BTCUSDT");
        request.setTimeframe("7m");

        ResponseEntity<ApiResponse<AnalysisRunResult>> response = controller.run(request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(orchestrator.called).isTrue();
    }

    @Test
    void byRequestIdAndSchedulerStatusAreReadOnlyEndpoints() {
        StubTraceService traceService = new StubTraceService();
        AnalysisRunController controller = controller(new CapturingOrchestrator(false), traceService);

        ResponseEntity<ApiResponse<AnalysisTraceSnapshot>> byRequest = controller.runTraceByRequest("req-controller");
        ResponseEntity<ApiResponse<java.util.Map<String, Object>>> scheduler = controller.schedulerStatus();

        assertThat(byRequest.getStatusCode().value()).isEqualTo(200);
        assertThat(byRequest.getBody().getData().getRequestId()).isEqualTo("req-controller");
        assertThat(scheduler.getStatusCode().value()).isEqualTo(200);
        assertThat(scheduler.getBody().getData()).containsEntry("reviewOnly", true);
        assertThat(scheduler.getBody().getData()).containsEntry("notAutoTrading", true);
    }

    private static AnalysisRunController controller(CapturingOrchestrator orchestrator, AnalysisTraceService traceService) {
        AnalysisRunProperties properties = new AnalysisRunProperties();
        AnalysisSchedulerService scheduler = new AnalysisSchedulerService(orchestrator, properties);
        return new AnalysisRunController(orchestrator, traceService, scheduler);
    }

    private static final class CapturingOrchestrator implements AnalysisRunOrchestrator {
        private final boolean throwInputError;
        private boolean called;

        private CapturingOrchestrator(boolean throwInputError) {
            this.throwInputError = throwInputError;
        }

        @Override
        public AnalysisRunResult run(AnalysisRunCommand command) {
            called = true;
            if (throwInputError) {
                throw new AnalysisRunInputException("TIMEFRAME_UNSUPPORTED", "unsupported timeframe: " + command.getTimeframe());
            }
            AnalysisRunDO run = new AnalysisRunDO();
            run.setAnalysisId("ana-controller");
            run.setTraceId("trace-controller");
            run.setRequestId(command.getRequestId());
            run.setSymbol(command.getSymbol());
            run.setTimeframe(command.getTimeframe());
            run.setTriggerType(command.getTriggerType().name());
            return AnalysisRunResult.executed(run, null, false, false);
        }
    }

    private static final class StubTraceService implements AnalysisTraceService {
        @Override
        public AnalysisTraceSnapshot byAnalysisId(String analysisId) {
            return snapshot("ana-controller", "trace-controller", "req-controller");
        }

        @Override
        public AnalysisTraceSnapshot byTraceId(String traceId) {
            return snapshot("ana-controller", traceId, "req-controller");
        }

        @Override
        public AnalysisTraceSnapshot byRequestId(String requestId) {
            return snapshot("ana-controller", "trace-controller", requestId);
        }

        private static AnalysisTraceSnapshot snapshot(String analysisId, String traceId, String requestId) {
            AnalysisRunDO run = new AnalysisRunDO();
            run.setAnalysisId(analysisId);
            run.setTraceId(traceId);
            run.setRequestId(requestId);
            run.setStatus("SUCCESS");
            return new AnalysisTraceSnapshot(
                    run, List.of("ev"), List.of("sc"), List.of("dec"), List.of("plan"),
                    List.of(), List.of(), List.of(), List.of(), 0);
        }
    }
}
