package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.RuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
