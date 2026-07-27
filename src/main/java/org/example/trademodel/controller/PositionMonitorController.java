package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/position-monitor")
public class PositionMonitorController {
    private final PositionMonitorService positionMonitorService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    public PositionMonitorController(PositionMonitorService positionMonitorService,
                                     AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.positionMonitorService = positionMonitorService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @PostMapping("/user-positions/{positionId}/run")
    public ResponseEntity<ApiResponse<PositionMonitorResultDTO>> monitorUserPosition(@PathVariable Long positionId) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                positionMonitorService.monitorUserPositionForUser(positionId, userId)));
    }

    @PostMapping("/user-positions/open/run")
    public ResponseEntity<ApiResponse<PositionMonitorBatchResultDTO>> monitorOpenUserPositions() {
        authenticatedUserIdResolver.requireCurrentUserId();
        return ResponseEntity.status(403).body(ApiResponse.forbidden("system-only operation"));
    }
}
