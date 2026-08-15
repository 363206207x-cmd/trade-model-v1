package org.example.trademodel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DesktopWorkspaceController {

    @GetMapping("/asset-pool")
    public String assetPool(Model model) {
        return page(model, "asset-pool", "资产池", "管理观察资产并按需启动分析", "asset-pool", null);
    }

    @GetMapping("/positions")
    public String positions(Model model) {
        return page(model, "positions", "持仓", "查看真实持仓、风险变化与监控结论", "positions", null);
    }

    @GetMapping("/positions/{positionId}")
    public String positionDetail(@PathVariable Long positionId, Model model) {
        return page(model, "position-detail", "持仓详情", "核对实际持仓与开仓计划基线", "positions", positionId);
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        return page(model, "reviews", "复盘", "分离当时事实、后续结果与责任链", "reviews", null);
    }

    @GetMapping("/reviews/{reviewId}")
    public String reviewDetail(@PathVariable String reviewId, Model model) {
        return page(model, "review-detail", "复盘详情", "回看当时判断与后续结果", "reviews", reviewId);
    }

    @GetMapping("/analysis")
    public String analysis(Model model) {
        return page(model, "analysis", "AI 分析", "预览分析与正式机会决策保持清晰边界", "analysis", null);
    }

    @GetMapping("/analysis/{analysisId}")
    public String analysisDetail(@PathVariable String analysisId, Model model) {
        return page(model, "analysis", "AI 分析", "查看证据、评分与三 AI 结构化输出", "analysis", analysisId);
    }

    @GetMapping("/messages")
    public String messages(Model model) {
        return page(model, "messages", "消息", "查看机会、计划安全变化与持仓风险消息", "messages", null);
    }

    @GetMapping("/recheck/{pushSnapshotId}")
    public String recheck(@PathVariable String pushSnapshotId, Model model) {
        return page(model, "recheck", "推送复核", "比较原始快照与当前状态", "messages", pushSnapshotId);
    }

    @GetMapping("/plans/{planId}")
    public String plan(@PathVariable String planId, Model model) {
        return page(model, "plan", "最终执行计划", "查看经过冲突处理与规则校验的最终计划", "analysis", planId);
    }

    @GetMapping("/calendar")
    public String calendar(Model model) {
        return page(model, "calendar", "事件日历", "查看事件窗口、关联资产与计划重验证", "calendar", null);
    }

    @GetMapping("/audit/{traceId}")
    public String audit(@PathVariable String traceId, Model model) {
        return page(model, "audit", "完整审计链", "按分析、候选与 Trace 汇总决策责任链", "audit", traceId);
    }

    @GetMapping("/me")
    public String me(Model model) {
        return page(model, "me", "我的", "管理风险偏好、通知与数据源状态", "me", null);
    }

    private String page(Model model, String pageKey, String title, String subtitle,
                        String activeNavigation, Object resourceId) {
        model.addAttribute("pageKey", pageKey);
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeNavigation", activeNavigation);
        model.addAttribute("resourceId", resourceId);
        return "workspace";
    }
}
