package org.example.trademodel.service.support;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.HotResetPolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RuleConfigContractService {

    public static final String GROUP_CONFUSED_STATE = "confused_state_config";
    public static final String GROUP_AI_CONFLICT = "ai_conflict_config";
    public static final String GROUP_PUSH_RECHECK = "push_recheck_config";
    public static final String GROUP_MISSED_OPPORTUNITY = "missed_opportunity_config";
    public static final String GROUP_HOT_RESET = "hot_reset_config";

    private final RuleConfigService ruleConfigService;

    public RuleConfigContractService(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    public HotResetPolicy.Thresholds requireHotResetThresholds() {
        ConfigReader reader = reader();
        BigDecimal extremePriceMove = reader.decimal(GROUP_HOT_RESET, "extreme_price_move_ratio_threshold");
        BigDecimal oiCollapse = reader.decimal(GROUP_HOT_RESET, "oi_collapse_change_ratio_threshold");
        BigDecimal liquidityDrain = reader.decimal(GROUP_HOT_RESET, "liquidity_drain_change_ratio_threshold");
        Integer systemicShock = reader.integer(GROUP_HOT_RESET, "systemic_shock_severity_threshold");
        reader.throwIfInvalid(GROUP_HOT_RESET);
        return new HotResetPolicy.Thresholds(extremePriceMove, oiCollapse, liquidityDrain, systemicShock);
    }

    public PushRecheckThresholds requirePushRecheckThresholds() {
        ConfigReader reader = reader();
        BigDecimal drift = reader.decimal(GROUP_PUSH_RECHECK, "drift_ratio_threshold");
        Integer confusedWait = reader.integer(GROUP_PUSH_RECHECK, "confused_wait_threshold");
        Integer confusedBlock = reader.integer(GROUP_PUSH_RECHECK, "confused_block_threshold");
        Integer executionFeasibilityWait = reader.integer(GROUP_PUSH_RECHECK, "execution_feasibility_wait_threshold");
        reader.throwIfInvalid(GROUP_PUSH_RECHECK);
        return new PushRecheckThresholds(drift, confusedWait, confusedBlock, executionFeasibilityWait);
    }

    public List<ConfigGroupStatus> schemeGroupStatuses() {
        ConfigReader reader = reader();
        List<ConfigGroupStatus> statuses = new ArrayList<>();
        statuses.add(reader.status(GROUP_CONFUSED_STATE,
                "enter_threshold", "directional_push_block_threshold",
                "exit_threshold_exclusive", "exit_required_consecutive_cycles"));
        statuses.add(reader.status(GROUP_AI_CONFLICT,
                "level1_max_score", "level2_max_score", "level3_max_score", "single_objection_max_score"));
        statuses.add(reader.status(GROUP_PUSH_RECHECK,
                "drift_ratio_threshold", "confused_wait_threshold", "confused_block_threshold",
                "execution_feasibility_wait_threshold"));
        statuses.add(reader.status(GROUP_MISSED_OPPORTUNITY,
                "review_window_hours", "min_mfe_ratio_threshold", "max_mae_ratio_threshold"));
        statuses.add(reader.status(GROUP_HOT_RESET,
                "extreme_price_move_ratio_threshold", "oi_collapse_change_ratio_threshold",
                "liquidity_drain_change_ratio_threshold", "systemic_shock_severity_threshold"));
        return statuses;
    }

    private ConfigReader reader() {
        return new ConfigReader(ruleConfigService != null ? ruleConfigService.getRuleConfigMap() : null);
    }

    public static class PushRecheckThresholds {
        private final BigDecimal driftRatioThreshold;
        private final int confusedWaitThreshold;
        private final int confusedBlockThreshold;
        private final int executionFeasibilityWaitThreshold;

        public PushRecheckThresholds(BigDecimal driftRatioThreshold,
                                     int confusedWaitThreshold,
                                     int confusedBlockThreshold,
                                     int executionFeasibilityWaitThreshold) {
            this.driftRatioThreshold = requirePositive(driftRatioThreshold, "push_recheck_config.drift_ratio_threshold");
            this.confusedWaitThreshold = requireScore(confusedWaitThreshold, "push_recheck_config.confused_wait_threshold");
            this.confusedBlockThreshold = requireScore(confusedBlockThreshold, "push_recheck_config.confused_block_threshold");
            this.executionFeasibilityWaitThreshold = requireScore(executionFeasibilityWaitThreshold,
                    "push_recheck_config.execution_feasibility_wait_threshold");
        }

        public BigDecimal getDriftRatioThreshold() { return driftRatioThreshold; }
        public int getConfusedWaitThreshold() { return confusedWaitThreshold; }
        public int getConfusedBlockThreshold() { return confusedBlockThreshold; }
        public int getExecutionFeasibilityWaitThreshold() { return executionFeasibilityWaitThreshold; }
    }

    public static class ConfigGroupStatus {
        private final String group;
        private final boolean ready;
        private final List<String> missingKeys;
        private final List<String> invalidKeys;

        private ConfigGroupStatus(String group, List<String> missingKeys, List<String> invalidKeys) {
            this.group = group;
            this.missingKeys = List.copyOf(missingKeys);
            this.invalidKeys = List.copyOf(invalidKeys);
            this.ready = missingKeys.isEmpty() && invalidKeys.isEmpty();
        }

        public String getGroup() { return group; }
        public boolean isReady() { return ready; }
        public List<String> getMissingKeys() { return missingKeys; }
        public List<String> getInvalidKeys() { return invalidKeys; }
    }

    private static class ConfigReader {
        private final Map<String, RuleConfigDO> configs;
        private final List<String> missingKeys = new ArrayList<>();
        private final List<String> invalidKeys = new ArrayList<>();

        private ConfigReader(Map<String, RuleConfigDO> configs) {
            this.configs = configs;
        }

        private BigDecimal decimal(String group, String name) {
            String value = value(group, name);
            if (value == null) {
                return null;
            }
            try {
                return new BigDecimal(value);
            } catch (RuntimeException ex) {
                invalidKeys.add(key(group, name));
                return null;
            }
        }

        private Integer integer(String group, String name) {
            String value = value(group, name);
            if (value == null) {
                return null;
            }
            try {
                return Integer.parseInt(value);
            } catch (RuntimeException ex) {
                invalidKeys.add(key(group, name));
                return null;
            }
        }

        private String value(String group, String name) {
            String key = key(group, name);
            RuleConfigDO row = configs == null ? null : configs.get(key);
            String value = row == null ? null : row.getRuleValue();
            if (value == null || value.isBlank()) {
                missingKeys.add(key);
                return null;
            }
            return value.trim();
        }

        private ConfigGroupStatus status(String group, String... names) {
            List<String> missing = new ArrayList<>();
            List<String> invalid = new ArrayList<>();
            for (String name : names) {
                String key = key(group, name);
                RuleConfigDO row = configs == null ? null : configs.get(key);
                String value = row == null ? null : row.getRuleValue();
                if (value == null || value.isBlank()) {
                    missing.add(key);
                    continue;
                }
                try {
                    new BigDecimal(value.trim());
                } catch (RuntimeException ex) {
                    invalid.add(key);
                }
            }
            return new ConfigGroupStatus(group, missing, invalid);
        }

        private void throwIfInvalid(String group) {
            if (!missingKeys.isEmpty() || !invalidKeys.isEmpty()) {
                throw new IllegalStateException(group + " not ready; missing=" + missingKeys + "; invalid=" + invalidKeys);
            }
        }

        private static String key(String group, String name) {
            return group + "." + name;
        }
    }

    private static BigDecimal requirePositive(BigDecimal value, String key) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(key + " must be positive");
        }
        return value;
    }

    private static int requireScore(Integer value, String key) {
        if (value == null || value < 0 || value > 100) {
            throw new IllegalStateException(key + " must be 0..100");
        }
        return value;
    }
}
