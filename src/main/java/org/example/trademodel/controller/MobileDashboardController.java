package org.example.trademodel.controller;

import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.vo.DashboardHomeVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MobileDashboardController {
    static final int MOBILE_HOME_ASSET_LIMIT = 3;

    private final DashboardHomeService dashboardHomeService;

    public MobileDashboardController(DashboardHomeService dashboardHomeService) {
        this.dashboardHomeService = dashboardHomeService;
    }

    @GetMapping("/dashboard/mobile")
    public String mobileDashboard(
            @RequestParam(value = "selectedSymbol", required = false) String selectedSymbol,
            Model model) {
        DashboardHomeVO home = dashboardHomeService.getHome(selectedSymbol, MOBILE_HOME_ASSET_LIMIT, null);
        model.addAttribute("home", home);
        return "dashboard-mobile";
    }
}
