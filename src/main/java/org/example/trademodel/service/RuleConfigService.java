package org.example.trademodel.service;

import org.example.trademodel.entity.RuleConfigDO;
import java.util.Map;

public interface RuleConfigService {
    Map<String, RuleConfigDO> getRuleConfigMap();
    void reloadRules();
}
