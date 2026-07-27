package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.vo.DashboardHomeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardHomeController {
    private final DashboardHomeService dashboardHomeService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    public DashboardHomeController(DashboardHomeService dashboardHomeService,
                                   AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.dashboardHomeService = dashboardHomeService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @GetMapping("/home")
    public ApiResponse<DashboardHomeVO> home(
            @RequestParam(value = "selectedSymbol", required = false) String selectedSymbol,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "positionId", required = false) Long selectedPositionId) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ApiResponse.success(dashboardHomeService.getHomeForUser(
                userId, selectedSymbol, limit, selectedPositionId));
    }
}
