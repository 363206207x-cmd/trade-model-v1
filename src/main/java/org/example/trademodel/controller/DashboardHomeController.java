package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.vo.DashboardHomeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardHomeController {
    private final DashboardHomeService dashboardHomeService;

    public DashboardHomeController(DashboardHomeService dashboardHomeService) {
        this.dashboardHomeService = dashboardHomeService;
    }

    @GetMapping("/home")
    public ApiResponse<DashboardHomeVO> home(
            @RequestParam(value = "selectedSymbol", required = false) String selectedSymbol,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ApiResponse.success(dashboardHomeService.getHome(selectedSymbol, limit));
    }
}
