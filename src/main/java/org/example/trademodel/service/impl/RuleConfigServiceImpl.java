package org.example.trademodel.service.impl;

import org.example.trademodel.dto.PushWatchlistConfigRequest;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.mapper.PushWatchlistConfigAuditMapper;
import org.example.trademodel.mapper.RuleConfigMapper;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.vo.PushWatchlistConfigAuditVO;
import org.example.trademodel.vo.PushWatchlistConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RuleConfigServiceImpl implements RuleConfigService {

    static final String WATCHLIST_RULE_KEY = "push.watchlist.symbols";
    static final String WATCHLIST_RULE_TYPE = "push";
    static final String WATCHLIST_RULE_VERSION = "p1-watchlist";
    private static final String WATCHLIST_DESCRIPTION = "Push watchlist symbols";
    private static final int DEFAULT_AUDIT_LIMIT = 20;
    private static final int MAX_AUDIT_LIMIT = 100;

    private final RuleConfigMapper ruleConfigMapper;
    private final PushWatchlistConfigAuditMapper pushWatchlistConfigAuditMapper;
    /**
     * 真热加载要求：读线程不能读到 clear/半刷新状态
     * 这里用原子替换保证“读要么拿旧 map，要么拿新 map”。
     */
    private final AtomicReference<Map<String, RuleConfigDO>> ruleCacheRef =
            new AtomicReference<>(Collections.emptyMap());

    public RuleConfigServiceImpl(RuleConfigMapper ruleConfigMapper,
                                 PushWatchlistConfigAuditMapper pushWatchlistConfigAuditMapper) {
        this.ruleConfigMapper = ruleConfigMapper;
        this.pushWatchlistConfigAuditMapper = pushWatchlistConfigAuditMapper;
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

    @Override
    public PushWatchlistConfigVO getPushWatchlistConfig() {
        RuleConfigDO config = ruleConfigMapper.findByRuleKeyIncludingDisabled(WATCHLIST_RULE_KEY);
        return toPushWatchlistConfigVO(config);
    }

    @Override
    @Transactional
    public PushWatchlistConfigVO updatePushWatchlistConfig(PushWatchlistConfigRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String operator = requireText(request.getOperator(), "operator is required");
        String reason = requireText(request.getReason(), "reason is required");
        List<String> symbols = normalizeAndDedupeSymbols(request.getSymbols());
        String ruleValue = String.join(",", symbols);
        Boolean afterEnabled = Boolean.TRUE.equals(request.getEnabled());

        RuleConfigDO oldConfig = ruleConfigMapper.findByRuleKeyIncludingDisabled(WATCHLIST_RULE_KEY);
        String beforeSymbols = normalizedRuleValue(oldConfig == null ? null : oldConfig.getRuleValue());
        boolean beforeEnabled = oldConfig != null && Boolean.TRUE.equals(oldConfig.getEnabled());
        RuleConfigDO newConfig = oldConfig == null ? new RuleConfigDO() : oldConfig;
        if (newConfig.getRuleId() == null || newConfig.getRuleId().isBlank()) {
            newConfig.setRuleId(UUID.randomUUID().toString());
        }
        newConfig.setRuleType(WATCHLIST_RULE_TYPE);
        newConfig.setRuleKey(WATCHLIST_RULE_KEY);
        newConfig.setRuleValue(ruleValue);
        newConfig.setDescription(WATCHLIST_DESCRIPTION);
        newConfig.setVersion(WATCHLIST_RULE_VERSION);
        newConfig.setEnabled(afterEnabled);

        int affected = oldConfig == null
                ? ruleConfigMapper.insertRuleConfig(newConfig)
                : ruleConfigMapper.updateRuleConfigByKey(newConfig);
        if (affected != 1) {
            throw new IllegalStateException("failed to persist push watchlist rule config");
        }

        PushWatchlistConfigAuditVO audit = new PushWatchlistConfigAuditVO();
        audit.setRuleKey(WATCHLIST_RULE_KEY);
        audit.setBeforeSymbols(beforeSymbols);
        audit.setAfterSymbols(ruleValue);
        audit.setBeforeEnabled(beforeEnabled);
        audit.setAfterEnabled(afterEnabled);
        audit.setChangedBy(operator);
        audit.setChangeReason(reason);
        audit.setSource("API");
        audit.setTraceId(UUID.randomUUID().toString());
        audit.setRuleVersion(WATCHLIST_RULE_VERSION);
        audit.setCreateTime(LocalDateTime.now());
        int auditInserted = pushWatchlistConfigAuditMapper.insert(audit);
        if (auditInserted != 1) {
            throw new IllegalStateException("failed to write push watchlist config audit");
        }

        reloadRules();
        return toPushWatchlistConfigVO(newConfig);
    }

    @Override
    public List<PushWatchlistConfigAuditVO> listPushWatchlistConfigAudit(int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_AUDIT_LIMIT : Math.min(limit, MAX_AUDIT_LIMIT);
        return pushWatchlistConfigAuditMapper.selectRecent(safeLimit);
    }

    static List<String> normalizeAndDedupeSymbols(List<String> rawSymbols) {
        if (rawSymbols == null || rawSymbols.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : rawSymbols) {
            String symbol = normalizeSymbol(raw);
            if (symbol != null) {
                normalized.add(symbol);
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String normalizeSymbol(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizedRuleValue(String ruleValue) {
        return String.join(",", normalizeAndDedupeSymbols(splitRuleValue(ruleValue)));
    }

    private static List<String> splitRuleValue(String ruleValue) {
        if (ruleValue == null || ruleValue.isBlank()) {
            return List.of();
        }
        return List.of(ruleValue.split(","));
    }

    private static PushWatchlistConfigVO toPushWatchlistConfigVO(RuleConfigDO config) {
        PushWatchlistConfigVO vo = new PushWatchlistConfigVO();
        vo.setRuleKey(WATCHLIST_RULE_KEY);
        if (config == null) {
            vo.setSymbols(List.of());
            vo.setEnabled(false);
            vo.setRuleValue("");
            return vo;
        }
        List<String> symbols = normalizeAndDedupeSymbols(splitRuleValue(config.getRuleValue()));
        vo.setSymbols(symbols);
        vo.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        vo.setRuleValue(String.join(",", symbols));
        return vo;
    }

    private static String requireText(String raw, String message) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return raw.trim();
    }
}
