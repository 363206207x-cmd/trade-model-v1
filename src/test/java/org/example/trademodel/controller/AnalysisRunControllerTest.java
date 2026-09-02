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
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertThat(scheduler.getBody().getData()).containsEntry("assetPoolOnly", true);
        assertThat(scheduler.getBody().getData()).containsEntry("persistentScanClaim", true);
        assertThat(scheduler.getBody().getData()).containsEntry("notAutoTrading", true);
        assertThat(scheduler.getBody().getData()).containsEntry("notOrderExecution", true);
    }

    @Test
    void manualRunUsesAuthenticatedUserOwnership() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator(false);
        AnalysisRunController controller = controller(orchestrator, new StubTraceService());
        AnalysisRunController.AnalysisRunRequest request = new AnalysisRunController.AnalysisRunRequest();
        request.setSymbol("ETHUSDT");
        request.setTimeframe("5m");

        ResponseEntity<ApiResponse<AnalysisRunResult>> response = controller.run(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(orchestrator.command.getOwnerType()).isEqualTo("USER");
        assertThat(orchestrator.command.getOwnerId()).isEqualTo(41L);
        assertThat(orchestrator.command.isPreview()).isFalse();
    }

    @Test
    void manualRunReturnsHttp202WithStableIdsWhenBackgroundWorkIsQueued() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator(false, true);
        AnalysisRunController controller = controller(orchestrator, new StubTraceService());
        AnalysisRunController.AnalysisRunRequest request = new AnalysisRunController.AnalysisRunRequest();
        request.setSymbol("ADAUSDT");
        request.setTimeframe("5m");

        ResponseEntity<ApiResponse<AnalysisRunResult>> response = controller.run(request);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("QUEUED");
        assertThat(response.getBody().getData().getAnalysisId()).isEqualTo("ana-controller");
        assertThat(response.getBody().getData().getTraceId()).isEqualTo("trace-controller");
        assertThat(response.getBody().getData().isNotAutoTrading()).isTrue();
        assertThat(response.getBody().getData().isNotOrderExecution()).isTrue();
    }

    private static AnalysisRunController controller(CapturingOrchestrator orchestrator, AnalysisTraceService traceService) {
        AnalysisRunProperties properties = new AnalysisRunProperties();
        AnalysisSchedulerService scheduler = new AnalysisSchedulerService(orchestrator, properties);
        AuthenticatedUserIdResolver resolver = mock(AuthenticatedUserIdResolver.class);
        when(resolver.requireCurrentUserId()).thenReturn(41L);
        return new AnalysisRunController(orchestrator, traceService, scheduler, resolver);
    }

    private static final class CapturingOrchestrator implements AnalysisRunOrchestrator {
        private final boolean throwInputError;
        private final boolean queue;
        private boolean called;
        private AnalysisRunCommand command;

        private CapturingOrchestrator(boolean throwInputError) {
            this(throwInputError, false);
        }

        private CapturingOrchestrator(boolean throwInputError, boolean queue) {
            this.throwInputError = throwInputError;
            this.queue = queue;
        }

        @Override
        public AnalysisRunResult run(AnalysisRunCommand command) {
            called = true;
            this.command = command;
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
            return queue
                    ? AnalysisRunResult.queued(run, false, false)
                    : AnalysisRunResult.executed(run, null, false, false);
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

        @Override
        public AnalysisTraceSnapshot byAnalysisIdForUser(String analysisId, Long userId) {
            return snapshot(analysisId, "trace-controller", "req-controller");
        }

        @Override
        public AnalysisTraceSnapshot byTraceIdForUser(String traceId, Long userId) {
            return snapshot("ana-controller", traceId, "req-controller");
        }

        @Override
        public AnalysisTraceSnapshot byRequestIdForUser(String requestId, Long userId) {
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
