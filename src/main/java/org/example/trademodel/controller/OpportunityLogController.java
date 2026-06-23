package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogEvaluateReq;
import org.example.trademodel.service.OpportunityLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/opportunity-log")
public class OpportunityLogController {
    private final OpportunityLogService opportunityLogService;

    public OpportunityLogController(OpportunityLogService opportunityLogService) {
        this.opportunityLogService = opportunityLogService;
    }

    @GetMapping("/{opportunityId}")
    public ResponseEntity<ApiResponse<OpportunityLogDTO>> findById(@PathVariable String opportunityId) {
        OpportunityLogDTO dto = opportunityLogService.findById(opportunityId);
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("opportunity not found: " + opportunityId));
        }
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/query")
    public ResponseEntity<ApiResponse<List<OpportunityLogDTO>>> query(
            @RequestParam(required = false) String analysisId,
            @RequestParam(required = false) String decisionId,
            @RequestParam(required = false) String executionPlanId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String opportunityStatus,
            @RequestParam(required = false) String lifecycleStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(opportunityLogService.query(
                analysisId, decisionId, executionPlanId, symbol, opportunityStatus, lifecycleStatus, from, to, limit)));
    }

    @PostMapping("/{opportunityId}/evaluate")
    public ResponseEntity<ApiResponse<OpportunityLogDTO>> evaluate(@PathVariable String opportunityId,
                                                                   @RequestBody(required = false)
                                                                   OpportunityLogEvaluateReq request) {
        try {
            LocalDateTime asOf = request != null ? request.getAsOf() : null;
            return ResponseEntity.ok(ApiResponse.success(opportunityLogService.evaluateOpportunity(opportunityId, asOf)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }
}
