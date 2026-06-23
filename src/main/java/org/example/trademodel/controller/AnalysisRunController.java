package org.example.trademodel.controller;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.analysistrace.AnalysisTraceService;
import org.example.trademodel.analysistrace.AnalysisTraceSnapshot;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.requestcontext.RequestIdSupport;
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

    public AnalysisRunController(AnalysisRunOrchestrator analysisRunOrchestrator,
                                 AnalysisTraceService analysisTraceService) {
        this.analysisRunOrchestrator = analysisRunOrchestrator;
        this.analysisTraceService = analysisTraceService;
    }

    @PostMapping("/runs")
    public ResponseEntity<ApiResponse<AnalysisRunResult>> run(@RequestBody(required = false) AnalysisRunRequest request) {
        if (request == null || blank(request.getSymbol())) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("symbol is required"));
        }
        String timeframe = blank(request.getTimeframe()) ? "1m" : request.getTimeframe();
        AnalysisRunResult result = analysisRunOrchestrator.run(AnalysisRunCommand.manual(
                request.getSymbol(), timeframe, RequestIdSupport.currentOrNew(), request.getAnalysisTime()));
        HttpStatus status = result.isConcurrentTriggerBlocked()
                || result.isPartialStateRecoveryBlocked()
                || result.isMaxRecoveryAttemptsExceeded()
                ? HttpStatus.CONFLICT : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(result.getStatus(), result));
    }

    @GetMapping("/runs/{analysisId}")
    public ResponseEntity<ApiResponse<AnalysisTraceSnapshot>> runTrace(@PathVariable String analysisId) {
        AnalysisTraceSnapshot snapshot = analysisTraceService.byAnalysisId(analysisId);
        if (snapshot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound("analysis run not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(snapshot));
    }

    @GetMapping("/traces/{traceId}")
    public ResponseEntity<ApiResponse<AnalysisTraceSnapshot>> trace(@PathVariable String traceId) {
        AnalysisTraceSnapshot snapshot = analysisTraceService.byTraceId(traceId);
        if (snapshot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound("analysis trace not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(snapshot));
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
