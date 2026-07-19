package org.example.trademodel.providercall.profile;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.entity.RuleVersionLogDO;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.service.RuleConfigService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScanProfileTransitionService {
    static final String PREFIX = "provider.scan.";
    private final RuleConfigService ruleConfigService;
    private final RuleVersionLogMapper auditMapper;
    private final Clock clock;
    private final Map<String, State> states = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public ScanProfileTransitionService(RuleConfigService ruleConfigService, RuleVersionLogMapper auditMapper) {
        this(ruleConfigService, auditMapper, Clock.systemUTC());
    }

    public ScanProfileTransitionService(
            RuleConfigService ruleConfigService,
            RuleVersionLogMapper auditMapper,
            Clock clock) {
        this.ruleConfigService = ruleConfigService;
        this.auditMapper = auditMapper;
        this.clock = clock;
    }

    public synchronized ProfileTransitionResult evaluate(
            String symbol,
            UserScanProfile userProfile,
            ProfileTransitionSignal signal,
            String traceId) {
        String normalized = required(symbol, "symbol").toUpperCase();
        String safeTrace = required(traceId, "traceId");
        Instant now = clock.instant();
        RuntimeScanProfile floor = userFloor(userProfile);
        State published = states.get(normalized);
        State staged = published == null ? new State(floor, now) : published.copy();
        Thresholds thresholds = thresholds();
        if (thresholds == null) {
            RuntimeScanProfile kept = RuntimeScanProfile.max(staged.profile, floor);
            staged.recordEvaluation("PROFILE_RULE_CONFIG_UNAVAILABLE", "UNKNOWN", now);
            states.put(normalized, staged);
            return result(normalized, staged.profile, kept, "PROFILE_RULE_CONFIG_UNAVAILABLE", staged.since,
                    staged.nextDowngradeEligibleAt, "UNKNOWN", false, safeTrace);
        }

        Requested requested = requested(signal, thresholds);
        RuntimeScanProfile target = RuntimeScanProfile.max(floor, requested.profile);
        RuntimeScanProfile previous = staged.profile;
        boolean changed = false;
        String reason = requested.reason;
        if (target.rank() > staged.profile.rank()) {
            staged.profile = target;
            staged.since = now;
            staged.recoveryCycles = 0;
            staged.nextDowngradeEligibleAt = now.plusSeconds(holdSeconds(target, thresholds));
            changed = true;
        } else if (target.rank() < staged.profile.rank()) {
            staged.recoveryCycles++;
            boolean holdComplete = staged.nextDowngradeEligibleAt == null
                    || !now.isBefore(staged.nextDowngradeEligibleAt);
            boolean cooldownComplete = staged.lastTransitionAt == null
                    || !now.isBefore(staged.lastTransitionAt.plusSeconds(thresholds.downgradeCooldownSeconds));
            if (holdComplete && cooldownComplete && staged.recoveryCycles >= thresholds.recoveryConfirmCycles) {
                staged.profile = RuntimeScanProfile.max(floor, staged.profile.oneLevelDown());
                staged.since = now;
                staged.recoveryCycles = 0;
                staged.nextDowngradeEligibleAt = now.plusSeconds(thresholds.downgradeCooldownSeconds);
                reason = "RECOVERY_HYSTERESIS";
                changed = true;
            } else {
                reason = "RECOVERY_HYSTERESIS";
            }
        } else {
            staged.recoveryCycles = 0;
        }
        staged.recordEvaluation(reason, thresholds.ruleVersion, now);
        if (changed) {
            staged.lastTransitionAt = now;
            audit(normalized, previous, staged.profile, reason, requested.triggerValue,
                    thresholds.ruleVersion, staged.nextDowngradeEligibleAt, safeTrace, now);
        }
        states.put(normalized, staged);
        return result(normalized, previous, staged.profile, reason, staged.since,
                staged.nextDowngradeEligibleAt, thresholds.ruleVersion, changed, safeTrace);
    }

    public synchronized RuntimeScanProfile currentProfile(String symbol) {
        if (symbol == null || symbol.isBlank()) return RuntimeScanProfile.LOW;
        State state = states.get(symbol.trim().toUpperCase());
        return state == null ? RuntimeScanProfile.LOW : state.profile;
    }

    public synchronized ProfileTransitionResult current(String symbol, String traceId) {
        String normalized = required(symbol, "symbol").toUpperCase();
        String safeTrace = required(traceId, "traceId");
        State state = states.get(normalized);
        if (state == null) {
            return result(normalized, RuntimeScanProfile.LOW, RuntimeScanProfile.LOW,
                    "NO_RUNTIME_ESCALATION", null, null, "UNKNOWN", false, safeTrace);
        }
        RuntimeScanProfile profile = state.profile;
        String reason = state.lastEffectiveReason == null ? "CURRENT_RUNTIME_STATE" : state.lastEffectiveReason;
        Instant since = state.since;
        Instant nextDowngradeEligibleAt = state.nextDowngradeEligibleAt;
        String ruleVersion = state.lastRuleVersion == null ? "RUNTIME" : state.lastRuleVersion;
        return result(normalized, profile, profile, reason, since,
                nextDowngradeEligibleAt, ruleVersion, false, safeTrace);
    }

    private Requested requested(ProfileTransitionSignal signal, Thresholds t) {
        if (signal == null) return new Requested(RuntimeScanProfile.LOW, "NO_ESCALATION_SIGNAL", null);
        if (signal.hotReset()) return new Requested(RuntimeScanProfile.EMERGENCY, "HOT_RESET", "true");
        if (atLeast(signal.confusedScore(), t.emergencyConfusedScore)) {
            return new Requested(RuntimeScanProfile.EMERGENCY, "CONFUSED", signal.confusedScore().toPlainString());
        }
        if (atLeast(signal.priceMovement1m(), t.emergencyPriceMovement1m)
                || atLeast(signal.liquidationSpike(), t.emergencyLiquidationSpike)) {
            return new Requested(RuntimeScanProfile.EMERGENCY, "VOLATILITY_SPIKE", firstValue(
                    signal.priceMovement1m(), signal.liquidationSpike()));
        }
        if (signal.highImpactEvent()) {
            return new Requested(RuntimeScanProfile.HIGH, "EXTERNAL_EVENT", "true");
        }
        if (signal.strongReversal()) {
            return new Requested(RuntimeScanProfile.HIGH, "STRONG_REVERSAL", "true");
        }
        if (below(signal.nearStopDistance(), t.nearBoundaryDistance)) {
            return new Requested(RuntimeScanProfile.HIGH, "NEAR_USER_STOP",
                    signal.nearStopDistance().toPlainString());
        }
        if (below(signal.nearTargetDistance(), t.nearBoundaryDistance)) {
            return new Requested(RuntimeScanProfile.HIGH, "NEAR_USER_TARGET",
                    signal.nearTargetDistance().toPlainString());
        }
        if (atLeast(signal.priceMovement1m(), t.highPriceMovement1m)
                || atLeast(signal.atrMultiple5m(), t.highAtrMultiple5m)
                || atLeast(signal.volumeSpike(), t.highVolumeSpike)
                || atLeast(signal.spreadSpike(), t.highSpreadSpike)
                || atLeast(signal.openInterestChange(), t.highOpenInterestChange)
                || atLeast(signal.fundingExtremity(), t.highFundingExtremity)
                || below(signal.dataQualityScore(), t.dataQualityDeteriorationScore)) {
            return new Requested(RuntimeScanProfile.HIGH, "HIGH_RISK", "threshold-crossed");
        }
        if (atLeast(signal.confusedScore(), t.standardConfusedScore)) {
            return new Requested(RuntimeScanProfile.STANDARD, "CONFUSED",
                    signal.confusedScore().toPlainString());
        }
        return new Requested(RuntimeScanProfile.LOW, "RECOVERY_SIGNAL", null);
    }

    private Thresholds thresholds() {
        try {
            Map<String, RuleConfigDO> rules = ruleConfigService.getRuleConfigMap();
            return new Thresholds(
                    decimal(rules, "emergency_price_movement_1m"),
                    decimal(rules, "emergency_liquidation_spike"),
                    decimal(rules, "emergency_confused_score"),
                    decimal(rules, "high_price_movement_1m"),
                    decimal(rules, "high_atr_multiple_5m"),
                    decimal(rules, "high_volume_spike"),
                    decimal(rules, "high_spread_spike"),
                    decimal(rules, "high_open_interest_change"),
                    decimal(rules, "high_funding_extremity"),
                    decimal(rules, "near_boundary_distance"),
                    decimal(rules, "data_quality_deterioration_score"),
                    decimal(rules, "standard_confused_score"),
                    integer(rules, "high_min_hold_seconds"),
                    integer(rules, "emergency_min_hold_seconds"),
                    integer(rules, "recovery_confirm_cycles"),
                    integer(rules, "downgrade_cooldown_seconds"),
                    ruleConfigService.resolveActiveRuleVersion());
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private void audit(String symbol, RuntimeScanProfile previous, RuntimeScanProfile next, String reason,
                       String triggerValue, String ruleVersion, Instant nextEligible, String traceId, Instant now) {
        RuleVersionLogDO row = new RuleVersionLogDO();
        row.setId(UUID.randomUUID().toString());
        row.setRuleVersion(ruleVersion);
        row.setChangeCategory("SCAN_PROFILE_TRANSITION");
        row.setChangeSummary("symbol=" + symbol + ";previousProfile=" + previous + ";newProfile=" + next);
        row.setChangeDetail("triggerReason=" + reason + ";triggerValue=" + triggerValue + ";effectiveTime=" + now
                + ";nextDowngradeEligibleAt=" + nextEligible + ";traceId=" + traceId);
        row.setOperator("SYSTEM_PROFILE_ESCALATION");
        row.setPublishTime(now.toString());
        row.setRollbackFlag("N");
        row.setCreatedBy("provider-call-orchestrator");
        row.setUpdatedBy("provider-call-orchestrator");
        row.setIsDeleted(0);
        row.setVersionNo(1);
        int inserted = auditMapper.insert(row);
        if (inserted != 1) {
            throw new IllegalStateException("profile transition audit insert count must be exactly 1");
        }
    }

    private static int holdSeconds(RuntimeScanProfile profile, Thresholds t) {
        return profile == RuntimeScanProfile.EMERGENCY ? t.emergencyMinHoldSeconds
                : profile == RuntimeScanProfile.HIGH ? t.highMinHoldSeconds : t.downgradeCooldownSeconds;
    }

    private static RuntimeScanProfile userFloor(UserScanProfile profile) {
        if (profile == null || profile == UserScanProfile.AUTO) return RuntimeScanProfile.LOW;
        return RuntimeScanProfile.valueOf(profile.name());
    }

    private static boolean atLeast(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) >= 0;
    }

    private static boolean below(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) <= 0;
    }

    private static String firstValue(BigDecimal... values) {
        for (BigDecimal value : values) if (value != null) return value.toPlainString();
        return null;
    }

    private static BigDecimal decimal(Map<String, RuleConfigDO> rules, String suffix) {
        RuleConfigDO row = rules == null ? null : rules.get(PREFIX + suffix);
        if (row == null || row.getRuleValue() == null) throw new IllegalStateException("missing " + suffix);
        return new BigDecimal(row.getRuleValue().trim());
    }

    private static int integer(Map<String, RuleConfigDO> rules, String suffix) {
        return decimal(rules, suffix).intValueExact();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static ProfileTransitionResult result(String symbol, RuntimeScanProfile previous,
                                                   RuntimeScanProfile effective, String reason, Instant since,
                                                   Instant next, String version, boolean changed, String trace) {
        return new ProfileTransitionResult(symbol, previous, effective, reason, since, next, version, changed, trace);
    }

    private record Requested(RuntimeScanProfile profile, String reason, String triggerValue) {}
    private record Thresholds(BigDecimal emergencyPriceMovement1m, BigDecimal emergencyLiquidationSpike,
                              BigDecimal emergencyConfusedScore, BigDecimal highPriceMovement1m,
                              BigDecimal highAtrMultiple5m, BigDecimal highVolumeSpike, BigDecimal highSpreadSpike,
                              BigDecimal highOpenInterestChange, BigDecimal highFundingExtremity,
                              BigDecimal nearBoundaryDistance, BigDecimal dataQualityDeteriorationScore,
                              BigDecimal standardConfusedScore, int highMinHoldSeconds,
                              int emergencyMinHoldSeconds, int recoveryConfirmCycles,
                              int downgradeCooldownSeconds, String ruleVersion) {}
    private static final class State {
        private RuntimeScanProfile profile;
        private Instant since;
        private Instant nextDowngradeEligibleAt;
        private Instant lastTransitionAt;
        private Instant lastEvaluatedAt;
        private String lastEffectiveReason;
        private String lastRuleVersion;
        private int recoveryCycles;
        private State(RuntimeScanProfile profile, Instant since) { this.profile = profile; this.since = since; }
        private State copy() {
            State copy = new State(profile, since);
            copy.nextDowngradeEligibleAt = nextDowngradeEligibleAt;
            copy.lastTransitionAt = lastTransitionAt;
            copy.lastEvaluatedAt = lastEvaluatedAt;
            copy.lastEffectiveReason = lastEffectiveReason;
            copy.lastRuleVersion = lastRuleVersion;
            copy.recoveryCycles = recoveryCycles;
            return copy;
        }
        private void recordEvaluation(String reason, String ruleVersion, Instant evaluatedAt) {
            this.lastEffectiveReason = reason;
            this.lastRuleVersion = ruleVersion;
            this.lastEvaluatedAt = evaluatedAt;
        }
    }
}
