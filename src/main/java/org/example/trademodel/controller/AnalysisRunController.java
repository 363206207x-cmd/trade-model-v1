package org.example.trademodel.controller;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunInputException;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.analysistrace.AnalysisTraceService;
import org.example.trademodel.analysistrace.AnalysisTraceSnapshot;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisRunController {
    private final AnalysisRunOrchestrator analysisRunOrchestrator;
    private final AnalysisTraceService analysisTraceService;
    private final AnalysisSchedulerService analysisSchedulerService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public AnalysisRunController(AnalysisRunOrchestrator analysisRunOrchestrator,
                                 AnalysisTraceService analysisTraceService,
                                 AnalysisSchedulerService analysisSchedulerService,
                                 AuthenticatedUserIdResolver userIdResolver) {
        this.analysisRunOrchestrator = analysisRunOrchestrator;
        this.analysisTraceService = analysisTraceService;
        this.analysisSchedulerService = analysisSchedulerService;
        this.userIdResolver = userIdResolver;
    }

    @PostMapping("/runs")
    public ResponseEntity<ApiResponse<AnalysisRunResult>> run(@RequestBody(required = false) AnalysisRunRequest request) {
        if (request == null || blank(request.getSymbol())) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("symbol is required"));
        }
        if (blank(request.getTimeframe())) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("timeframe is required"));
        }
        try {
            AnalysisRunResult result = analysisRunOrchestrator.run(AnalysisRunCommand.manualForUser(
                    userIdResolver.requireCurrentUserId(), request.getSymbol(), request.getTimeframe(),
                    RequestIdSupport.currentOrNew(), request.getAnalysisTime()));
            HttpStatus status = result.isConcurrentTriggerBlocked()
                    || result.isPartialStateRecoveryBlocked()
                    || result.isMaxRecoveryAttemptsExceeded()
                    ? HttpStatus.CONFLICT : HttpStatus.OK;
            return ResponseEntity.status(status).body(ApiResponse.success(result.getStatus(), result));
        } catch (AnalysisRunInputException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        }
    }

    @GetMapping("/runs/{analysisId}")
    public ResponseEntity<ApiResponse<AnalysisTraceSnapshot>> runTrace(@PathVariable String analysisId) {
        AnalysisTraceSnapshot snapshot = analysisTraceService.byAnalysisIdForUser(
                analysisId, userIdResolver.requireCurrentUserId());
        if (snapshot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound("analysis run not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(snapshot));
    }

    @GetMapping("/runs/by-request/{requestId}")
    public ResponseEntity<ApiResponse<AnalysisTraceSnapshot>> runTraceByRequest(@PathVariable String requestId) {
        AnalysisTraceSnapshot snapshot = analysisTraceService.byRequestIdForUser(
                requestId, userIdResolver.requireCurrentUserId());
        if (snapshot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound("analysis request not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(snapshot));
    }

    @GetMapping("/traces/{traceId}")
    public ResponseEntity<ApiResponse<AnalysisTraceSnapshot>> trace(@PathVariable String traceId) {
        AnalysisTraceSnapshot snapshot = analysisTraceService.byTraceIdForUser(
                traceId, userIdResolver.requireCurrentUserId());
        if (snapshot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound("analysis trace not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(snapshot));
    }

    @GetMapping("/scheduler/status")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> schedulerStatus() {
        return ResponseEntity.ok(ApiResponse.success(analysisSchedulerService.status()));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class AnalysisRunRequest {
        private String symbol;
        private String timeframe;
        private String analysisTime;

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getTimeframe() { return timeframe; }
        public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
        public String getAnalysisTime() { return analysisTime; }
        public void setAnalysisTime(String analysisTime) { this.analysisTime = analysisTime; }
    }
}
