package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.v41.DashboardLiveEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardHomeController {
    private final DashboardHomeService dashboardHomeService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
    private DashboardLiveEventService dashboardLiveEventService;

    public DashboardHomeController(DashboardHomeService dashboardHomeService,
                                   AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.dashboardHomeService = dashboardHomeService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @Autowired
    void setDashboardLiveEventService(DashboardLiveEventService value) {
        this.dashboardLiveEventService = value;
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

    @GetMapping(path = "/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        if (dashboardLiveEventService == null) {
            SseEmitter unavailable = new SseEmitter(1L);
            unavailable.completeWithError(new IllegalStateException("HOME_LIVE_STREAM_UNAVAILABLE"));
            return unavailable;
        }
        return dashboardLiveEventService.subscribe(userId);
    }
}
