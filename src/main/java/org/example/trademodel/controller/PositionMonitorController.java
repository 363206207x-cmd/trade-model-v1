package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.service.PositionMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/position-monitor")
public class PositionMonitorController {
    private final PositionMonitorService positionMonitorService;

    public PositionMonitorController(PositionMonitorService positionMonitorService) {
        this.positionMonitorService = positionMonitorService;
    }

    @PostMapping("/user-positions/{positionId}/run")
    public ResponseEntity<ApiResponse<PositionMonitorResultDTO>> monitorUserPosition(@PathVariable Long positionId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(positionMonitorService.monitorUserPosition(positionId)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        }
    }

    @PostMapping("/user-positions/open/run")
    public ResponseEntity<ApiResponse<PositionMonitorBatchResultDTO>> monitorOpenUserPositions() {
        return ResponseEntity.ok(ApiResponse.success(positionMonitorService.monitorOpenUserPositions()));
    }
}
