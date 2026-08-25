package org.example.trademodel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class DesktopWorkspaceController {
    private final boolean uiReviewMode;

    public DesktopWorkspaceController(
            @Value("${trade-model.ui-review.enabled:false}") boolean uiReviewMode) {
        this.uiReviewMode = uiReviewMode;
    }

    @GetMapping("/asset-pool")
    public String assetPool(Model model) {
        return page(model, "asset-pool", "资产池", "asset-pool", null);
    }

    @GetMapping("/positions")
    public String positions(Model model) {
        return page(model, "positions", "持仓", "positions", null);
    }

    @GetMapping("/positions/{positionId}")
    public String positionDetail(@PathVariable Long positionId, Model model) {
        return page(model, "position-detail", "持仓详情", "positions", positionId);
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        return page(model, "reviews", "复盘", "reviews", null);
    }

    @GetMapping("/reviews/{reviewId}")
    public String reviewDetail(@PathVariable String reviewId, Model model) {
        return page(model, "review-detail", "复盘详情", "reviews", reviewId);
    }

    @GetMapping("/analysis")
    public String analysis(Model model) {
        return page(model, "analysis", "分析", "analysis", null);
    }

    @GetMapping("/analysis/{analysisId}")
    public String analysisDetail(@PathVariable String analysisId, Model model) {
        return page(model, "analysis", "分析", "analysis", analysisId);
    }

    @GetMapping("/messages")
    public String messages(Model model) {
        return page(model, "messages", "消息", "messages", null);
    }

    @GetMapping("/recheck/{pushSnapshotId}")
    public String recheck(@PathVariable String pushSnapshotId, Model model) {
        return page(model, "recheck", "推送复核", "messages", pushSnapshotId);
    }

    @GetMapping("/plans/{planId}")
    public String plan(@PathVariable String planId, Model model) {
        return page(model, "plan", "最终执行计划", "analysis", planId);
    }

    @GetMapping("/calendar")
    public String calendar(Model model) {
        return page(model, "calendar", "事件日历", "calendar", null);
    }

    @GetMapping("/audit/{traceId}")
    public String audit(@PathVariable String traceId, Model model) {
        return page(model, "audit", "完整审计链", "audit", traceId);
    }

    @GetMapping("/me")
    public String me(Model model) {
        return page(model, "me", "我的", "me", null);
    }

    @GetMapping("/me/accounts")
    public String accounts(Model model) {
        return page(model, "accounts", "账户管理", "me", null);
    }

    private String page(Model model, String pageKey, String title,
                        String activeNavigation, Object resourceId) {
        model.addAttribute("pageKey", pageKey);
        model.addAttribute("pageTitle", title);
        model.addAttribute("activeNavigation", activeNavigation);
        model.addAttribute("resourceId", resourceId);
        model.addAttribute("uiReviewMode", uiReviewMode);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("ownerUser", authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority())));
        return "workspace";
    }
}
