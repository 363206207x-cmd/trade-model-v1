package org.example.trademodel.service.impl;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.mapper.RuleConfigMapper;
import org.example.trademodel.service.RuleConfigService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RuleConfigServiceImpl implements RuleConfigService {

    private final RuleConfigMapper ruleConfigMapper;
    /**
     * 真热加载要求：读线程不能读到 clear/半刷新状态
     * 这里用原子替换保证“读要么拿旧 map，要么拿新 map”。
     */
    private final AtomicReference<Map<String, RuleConfigDO>> ruleCacheRef =
            new AtomicReference<>(Collections.emptyMap());

    public RuleConfigServiceImpl(RuleConfigMapper ruleConfigMapper) {
        this.ruleConfigMapper = ruleConfigMapper;
    }

    @Override
    public Map<String, RuleConfigDO> getRuleConfigMap() {
        Map<String, RuleConfigDO> m = ruleCacheRef.get();
        if (m == null || m.isEmpty()) {
            reloadRules();
            m = ruleCacheRef.get();
        }
        return m;
    }

    @Override
    public void reloadRules() {
        List<RuleConfigDO> list = ruleConfigMapper.findAllEnabled();
        Map<String, RuleConfigDO> next = new HashMap<>();
        if (list != null) {
            for (RuleConfigDO config : list) {
                if (config == null || config.getRuleKey() == null) {
                    continue;
                }
                next.put(config.getRuleKey(), config);
            }
        }
        ruleCacheRef.set(Collections.unmodifiableMap(next));
    }
}
