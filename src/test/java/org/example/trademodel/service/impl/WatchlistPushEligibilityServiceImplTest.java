package org.example.trademodel.service.impl;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WatchlistPushEligibilityServiceImplTest {

    @Test
    void configuredSymbolIsEligible() {
        WatchlistPushEligibilityServiceImpl service = serviceWithConfig("BTCUSDT,ETHUSDT", true);

        assertThat(service.isEligibleForDirectionalPush("BTCUSDT")).isTrue();
    }

    @Test
    void normalizesWhitespaceAndCase() {
        WatchlistPushEligibilityServiceImpl service = serviceWithConfig(" btcusdt , ethusdt ", true);

        assertThat(service.isEligibleForDirectionalPush(" btcusdt ")).isTrue();
    }

    @Test
    void symbolOutsideConfigIsNotEligible() {
        WatchlistPushEligibilityServiceImpl service = serviceWithConfig("BTCUSDT,ETHUSDT", true);

        assertThat(service.isEligibleForDirectionalPush("SOLUSDT")).isFalse();
    }

    @Test
    void missingConfigFailsClosed() {
        WatchlistPushEligibilityServiceImpl service = new WatchlistPushEligibilityServiceImpl(
                ruleConfigService(Map.of()));

        assertThat(service.isEligibleForDirectionalPush("BTCUSDT")).isFalse();
    }

    @Test
    void blankConfigFailsClosed() {
        WatchlistPushEligibilityServiceImpl service = serviceWithConfig("   ", true);

        assertThat(service.isEligibleForDirectionalPush("BTCUSDT")).isFalse();
    }

    @Test
    void disabledConfigFailsClosed() {
        WatchlistPushEligibilityServiceImpl service = serviceWithConfig("BTCUSDT", false);

        assertThat(service.isEligibleForDirectionalPush("BTCUSDT")).isFalse();
    }

    @Test
    void ruleConfigExceptionFailsClosed() {
        WatchlistPushEligibilityServiceImpl service = new WatchlistPushEligibilityServiceImpl(new RuleConfigService() {
            @Override
            public Map<String, RuleConfigDO> getRuleConfigMap() {
                throw new IllegalStateException("rule config unavailable");
            }

            @Override
            public void reloadRules() {
            }
        });

        assertThat(service.isEligibleForDirectionalPush("BTCUSDT")).isFalse();
    }

    private static WatchlistPushEligibilityServiceImpl serviceWithConfig(String ruleValue, boolean enabled) {
        RuleConfigDO cfg = new RuleConfigDO();
        cfg.setRuleKey(WatchlistPushEligibilityServiceImpl.RULE_KEY_PUSH_WATCHLIST_SYMBOLS);
        cfg.setRuleValue(ruleValue);
        cfg.setEnabled(enabled);
        return new WatchlistPushEligibilityServiceImpl(ruleConfigService(Map.of(
                WatchlistPushEligibilityServiceImpl.RULE_KEY_PUSH_WATCHLIST_SYMBOLS, cfg)));
    }

    private static RuleConfigService ruleConfigService(Map<String, RuleConfigDO> values) {
        return new RuleConfigService() {
            @Override
            public Map<String, RuleConfigDO> getRuleConfigMap() {
                return values;
            }

            @Override
            public void reloadRules() {
            }
        };
    }
}
