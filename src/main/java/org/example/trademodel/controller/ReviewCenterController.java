package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.ReviewCenterService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.vo.ReviewCenterDashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewCenterController {
    private final ReviewCenterService reviewCenterService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    public ReviewCenterController(ReviewCenterService reviewCenterService,
                                  AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.reviewCenterService = reviewCenterService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @GetMapping("/center")
    public ApiResponse<ReviewCenterDashboardVO> center() {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ApiResponse.success(reviewCenterService.getDashboardForUser(userId));
    }
}
