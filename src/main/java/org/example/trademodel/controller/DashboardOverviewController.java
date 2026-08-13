package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.DashboardReadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardOverviewController {
    private final DashboardReadService dashboardReadService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public DashboardOverviewController(DashboardReadService dashboardReadService,
                                       AuthenticatedUserIdResolver userIdResolver) {
        this.dashboardReadService = dashboardReadService;
        this.userIdResolver = userIdResolver;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview() {
        return ResponseEntity.ok(ApiResponse.success(dashboardReadService.overview()));
    }

    @GetMapping("/analysis-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analysisStatus() {
        return ResponseEntity.ok(ApiResponse.success(dashboardReadService.analysisStatus()));
    }

    @GetMapping("/scheduler-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> schedulerStatus() {
        return ResponseEntity.ok(ApiResponse.success(dashboardReadService.schedulerStatus()));
    }

    @GetMapping("/trace-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> traceSummary(
            @RequestParam(required = false) String analysisId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String requestId) {
        if (blank(analysisId) && blank(traceId) && blank(requestId)) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("analysisId, traceId, or requestId is required"));
        }
        Map<String, Object> summary = dashboardReadService.traceSummary(
                userIdResolver.requireCurrentUserId(), analysisId, traceId, requestId);
        if ("NOT_FOUND".equals(summary.get("traceStatus"))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound("analysis trace not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
