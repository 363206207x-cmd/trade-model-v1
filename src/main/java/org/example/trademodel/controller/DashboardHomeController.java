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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncUtils;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardHomeController {
    private final DashboardHomeService dashboardHomeService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
    private DashboardLiveEventService dashboardLiveEventService;
    private org.example.trademodel.assetcard.AssetCardService assetCardService;

    @Autowired(required = false)
    void setAssetCardService(org.example.trademodel.assetcard.AssetCardService value) {
        this.assetCardService = value;
    }

    @GetMapping(path = "/runtime-snapshot", params = "view=ASSET_CARDS")
    public ApiResponse<java.util.List<org.example.trademodel.assetcard.AssetCardSnapshot>> cards(
            @RequestParam("symbols") java.util.List<String> symbols) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        if (assetCardService == null) throw new IllegalStateException("ASSET_CARD_READ_SERVICE_UNAVAILABLE");
        return ApiResponse.success(assetCardService.snapshotsForUser(userId, symbols));
    }

    public DashboardHomeController(DashboardHomeService dashboardHomeService,
                                   AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.dashboardHomeService = dashboardHomeService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @Autowired
    void setDashboardLiveEventService(DashboardLiveEventService value) {
        this.dashboardLiveEventService = value;
    }

    @GetMapping({"/home", "/runtime-snapshot"})
    public ApiResponse<DashboardHomeVO> home(
            @RequestParam(value = "selectedSymbol", required = false) String selectedSymbol,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "positionId", required = false) Long selectedPositionId) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ApiResponse.success(dashboardHomeService.getHomeForUser(
                userId, selectedSymbol, limit, selectedPositionId));
    }

    @GetMapping(path = "/stream", produces = "text/event-stream")
    public SseEmitter stream(HttpServletRequest request) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        if (dashboardLiveEventService == null) {
            SseEmitter unavailable = new SseEmitter(1L);
            unavailable.completeWithError(new IllegalStateException("HOME_LIVE_STREAM_UNAVAILABLE"));
            return unavailable;
        }
        SseEmitter emitter = dashboardLiveEventService.subscribe(userId);
        if (assetCardService != null) {
            String token = assetCardService.registerCardStream(userId);
            // Do not replace the generic SSE service's emitter completion/error/timeout callbacks.
            WebAsyncUtils.getAsyncManager(request).registerDeferredResultInterceptor("asset-card-" + token,
                    new DeferredResultProcessingInterceptor() {
                        @Override public <T> void afterCompletion(NativeWebRequest ignored, DeferredResult<T> result) {
                            assetCardService.unregisterCardStream(token);
                        }
                    });
        }
        return emitter;
    }
}
