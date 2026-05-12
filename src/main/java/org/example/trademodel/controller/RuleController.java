package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.PushWatchlistConfigRequest;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.vo.PushWatchlistConfigAuditVO;
import org.example.trademodel.vo.PushWatchlistConfigVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rule")
public class RuleController {

    private final RuleConfigService ruleConfigService;

    @Autowired
    public RuleController(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @GetMapping("/reload")
    public ApiResponse<String> reloadRules() {
        try {
            ruleConfigService.reloadRules();
            return ApiResponse.success("规则已热加载");
        } catch (Exception e) {
            return ApiResponse.fail("规则热加载失败: " + e.getMessage());
        }
    }

    @GetMapping("/push-watchlist")
    public ApiResponse<PushWatchlistConfigVO> getPushWatchlistConfig() {
        try {
            return ApiResponse.success(ruleConfigService.getPushWatchlistConfig());
        } catch (Exception e) {
            return ApiResponse.fail("推送观察列表配置读取失败: " + e.getMessage());
        }
    }

    @PostMapping("/push-watchlist")
    public ApiResponse<PushWatchlistConfigVO> updatePushWatchlistConfig(
            @RequestBody PushWatchlistConfigRequest request) {
        try {
            return ApiResponse.success(ruleConfigService.updatePushWatchlistConfig(request));
        } catch (Exception e) {
            return ApiResponse.fail("推送观察列表配置更新失败: " + e.getMessage());
        }
    }

    @GetMapping("/push-watchlist/audit")
    public ApiResponse<List<PushWatchlistConfigAuditVO>> listPushWatchlistConfigAudit(
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        try {
            return ApiResponse.success(ruleConfigService.listPushWatchlistConfigAudit(limit));
        } catch (Exception e) {
            return ApiResponse.fail("推送观察列表配置审计查询失败: " + e.getMessage());
        }
    }
}
