package org.example.trademodel.controller;

import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.RunBaselineService;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.example.trademodel.vo.RunBaselineVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemHealthService systemHealthService;
    private final PositionSyncService positionSyncService;
    private final RunBaselineService runBaselineService;

    @Autowired
    public SystemController(SystemHealthService systemHealthService,
                            PositionSyncService positionSyncService,
                            RunBaselineService runBaselineService) {
        this.systemHealthService = systemHealthService;
        this.positionSyncService = positionSyncService;
        this.runBaselineService = runBaselineService;
    }

    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("trade-model-v1 is running on port 8081");
    }

    @GetMapping("/position-sync-status")
    public ApiResponse<PositionSyncStatusVO> positionSyncStatus() {
        return ApiResponse.success(positionSyncService.getPositionSyncStatus());
    }

    @GetMapping("/run-baseline")
    public ApiResponse<RunBaselineVO> runBaseline(@RequestParam(required = false) Integer windowMinutes) {
        int effectiveWindowMinutes = windowMinutes != null ? windowMinutes : 60;
        return ApiResponse.success(runBaselineService.getRunBaseline(effectiveWindowMinutes));
    }
}
