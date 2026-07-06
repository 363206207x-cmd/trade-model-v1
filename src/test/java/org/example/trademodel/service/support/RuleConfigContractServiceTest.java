package org.example.trademodel.service.support;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleConfigContractServiceTest {

    @Test
    void allSchemeConfigGroupsAreReadFromRuleConfigMap() {
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        when(ruleConfigService.getRuleConfigMap()).thenReturn(configMap());
        RuleConfigContractService service = new RuleConfigContractService(ruleConfigService);

        assertThat(service.schemeGroupStatuses())
                .extracting(RuleConfigContractService.ConfigGroupStatus::getGroup)
                .containsExactly(
                        RuleConfigContractService.GROUP_CONFUSED_STATE,
                        RuleConfigContractService.GROUP_AI_CONFLICT,
                        RuleConfigContractService.GROUP_PUSH_RECHECK,
                        RuleConfigContractService.GROUP_MISSED_OPPORTUNITY,
                        RuleConfigContractService.GROUP_HOT_RESET);
        assertThat(service.schemeGroupStatuses()).allMatch(RuleConfigContractService.ConfigGroupStatus::isReady);
        assertThat(service.requireHotResetThresholds().getExtremePriceMoveRatioThreshold())
                .isEqualByComparingTo("0.08");
        assertThat(service.requirePushRecheckThresholds().getDriftRatioThreshold())
                .isEqualByComparingTo("0.02");
    }

    private static Map<String, RuleConfigDO> configMap() {
        Map<String, RuleConfigDO> map = new HashMap<>();
        put(map, "confused_state_config.enter_threshold", "70");
        put(map, "confused_state_config.directional_push_block_threshold", "85");
        put(map, "confused_state_config.exit_threshold_exclusive", "55");
        put(map, "confused_state_config.exit_required_consecutive_cycles", "2");
        put(map, "ai_conflict_config.level1_max_score", "20");
        put(map, "ai_conflict_config.level2_max_score", "45");
        put(map, "ai_conflict_config.level3_max_score", "70");
        put(map, "ai_conflict_config.single_objection_max_score", "35");
        put(map, "push_recheck_config.drift_ratio_threshold", "0.02");
        put(map, "push_recheck_config.confused_wait_threshold", "70");
        put(map, "push_recheck_config.confused_block_threshold", "85");
        put(map, "push_recheck_config.execution_feasibility_wait_threshold", "60");
        put(map, "missed_opportunity_config.review_window_hours", "24");
        put(map, "missed_opportunity_config.min_mfe_ratio_threshold", "0.01");
        put(map, "missed_opportunity_config.max_mae_ratio_threshold", "0.02");
        put(map, "hot_reset_config.extreme_price_move_ratio_threshold", "0.08");
        put(map, "hot_reset_config.oi_collapse_change_ratio_threshold", "-0.30");
        put(map, "hot_reset_config.liquidity_drain_change_ratio_threshold", "-0.40");
        put(map, "hot_reset_config.systemic_shock_severity_threshold", "85");
        return map;
    }

    private static void put(Map<String, RuleConfigDO> map, String key, String value) {
        RuleConfigDO row = new RuleConfigDO();
        row.setRuleKey(key);
        row.setRuleValue(value);
        row.setEnabled(true);
        map.put(key, row);
    }
}
