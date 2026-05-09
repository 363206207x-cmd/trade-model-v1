package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.vo.PositionMonitorOpenRowVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/position-monitor")
public class PositionMonitorController {

    private final PositionMonitorService positionMonitorService;

    public PositionMonitorController(PositionMonitorService positionMonitorService) {
        this.positionMonitorService = positionMonitorService;
    }

    @PostMapping("/{positionId}/run")
    public ResponseEntity<ApiResponse<PositionMonitorOpenRowVO>> run(@PathVariable String positionId) {
        try {
            PositionMonitorOpenRowVO row = positionMonitorService.run(positionId);
            return ResponseEntity.ok(ApiResponse.success(row));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/open")
    public ApiResponse<List<PositionMonitorOpenRowVO>> open() {
        List<PositionMonitorOpenRowVO> rows = positionMonitorService.listOpenManualPositions();
        return ApiResponse.success(rows == null ? List.of() : rows);
    }
}

