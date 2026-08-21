package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.PositionMonitoringProjectionService;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.vo.UserPositionVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspace/positions")
public class WorkspacePositionMonitoringController {
    private final AuthenticatedUserIdResolver userIdResolver;
    private final PositionMonitoringProjectionService projectionService;
    private final UserPositionService userPositionService;

    public WorkspacePositionMonitoringController(AuthenticatedUserIdResolver userIdResolver,
                                                 PositionMonitoringProjectionService projectionService,
                                                 UserPositionService userPositionService) {
        this.userIdResolver = userIdResolver;
        this.projectionService = projectionService;
        this.userPositionService = userPositionService;
    }

    @GetMapping("/monitoring")
    public ApiResponse<PositionMonitoringProjectionService.CollectionProjection> monitoring() {
        return ApiResponse.success(projectionService.listForUser(userIdResolver.requireCurrentUserId()));
    }

    @GetMapping("/{positionId}/monitoring")
    public ResponseEntity<ApiResponse<PositionMonitoringProjectionService.ItemProjection>> monitoringDetail(
            @PathVariable Long positionId) {
        return ResponseEntity.ok(ApiResponse.success(
                projectionService.findForUser(userIdResolver.requireCurrentUserId(), positionId)));
    }

    @GetMapping("/history")
    public ApiResponse<HistoryProjection> history(@RequestParam(defaultValue = "100") int limit) {
        Long userId = userIdResolver.requireCurrentUserId();
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<UserPositionVO> rows = userPositionService.listClosedPositionsForUser(userId, safeLimit);
        return ApiResponse.success(new HistoryProjection(rows, userPositionService.countClosedPositionsForUser(userId)));
    }

    public record HistoryProjection(List<UserPositionVO> positions, int totalCount) { }
}
