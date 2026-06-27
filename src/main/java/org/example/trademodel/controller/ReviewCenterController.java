package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.ReviewCenterService;
import org.example.trademodel.vo.ReviewCenterDashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewCenterController {
    private final ReviewCenterService reviewCenterService;

    public ReviewCenterController(ReviewCenterService reviewCenterService) {
        this.reviewCenterService = reviewCenterService;
    }

    @GetMapping("/center")
    public ApiResponse<ReviewCenterDashboardVO> center() {
        return ApiResponse.success(reviewCenterService.getDashboard());
    }
}
