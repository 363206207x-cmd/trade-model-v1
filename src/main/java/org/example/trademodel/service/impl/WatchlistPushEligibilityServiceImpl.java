package org.example.trademodel.service.impl;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.WatchlistPushEligibilityService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

/**
 * Reads the backend watchlist from tm_rule_config and fails closed on missing or invalid config.
 */
@Service
public class WatchlistPushEligibilityServiceImpl implements WatchlistPushEligibilityService {

    public static final String RULE_KEY_PUSH_WATCHLIST_SYMBOLS = "push.watchlist.symbols";

    private final RuleConfigService ruleConfigService;

    public WatchlistPushEligibilityServiceImpl(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @Override
    public boolean isEligibleForDirectionalPush(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        if (normalizedSymbol == null || ruleConfigService == null) {
            return false;
        }
        try {
            Map<String, RuleConfigDO> cfgMap = ruleConfigService.getRuleConfigMap();
            if (cfgMap == null || cfgMap.isEmpty()) {
                return false;
            }
            RuleConfigDO cfg = cfgMap.get(RULE_KEY_PUSH_WATCHLIST_SYMBOLS);
            if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled())) {
                return false;
            }
            String raw = cfg.getRuleValue();
            if (raw == null || raw.isBlank()) {
                return false;
            }
            String[] parts = raw.split(",");
            for (String part : parts) {
                String candidate = normalizeSymbol(part);
                if (normalizedSymbol.equals(candidate)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String normalizeSymbol(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
