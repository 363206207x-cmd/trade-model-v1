package org.example.trademodel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 复盘页入口（模板渲染：聚合摘要 + 下方可保存的复盘录入）。
 * 聚合数据由前端请求 {@code /api/review/aggregate/{analysisId}}；录入经 {@code /api/review/state} 与 {@code /api/review/save}。
 */
@Controller
public class ReviewPageController {

    @GetMapping("/review/dashboard")
    public String reviewDashboard(Model model) {
        model.addAttribute("analysisId", "");
        model.addAttribute("title", "复盘中心");
        model.addAttribute("reviewCenterMode", true);
        return "review";
    }

    @GetMapping("/review/{analysisId}")
    public String reviewPage(@PathVariable String analysisId, Model model) {
        model.addAttribute("analysisId", analysisId);
        model.addAttribute("title", "复盘 · " + analysisId);
        model.addAttribute("reviewCenterMode", false);
        return "review";
    }
}
