package org.example.trademodel.service;

import org.example.trademodel.entity.RuleConfigDO;
import java.util.Map;

public interface RuleConfigService {
    Map<String, RuleConfigDO> getRuleConfigMap();
    void reloadRules();

    default String resolveActiveRuleVersion() {
        Map<String, RuleConfigDO> ruleMap = getRuleConfigMap();
        if (ruleMap == null) {
            return "v1.0";
        }
        RuleConfigDO cfg = ruleMap.get("rule.active_version_fallback");
        if (cfg == null || cfg.getRuleValue() == null) {
            return "v1.0";
        }
        String value = cfg.getRuleValue().trim();
        return value.isEmpty() ? "v1.0" : value;
    }
}
