package org.example.trademodel.positionmonitor;

import org.example.trademodel.positionmonitorlog.PositionEntryLogicStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorConclusionEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorSourceStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorSuggestedActionEnum;
import org.example.trademodel.positionmonitorlog.PositionReversalStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionRiskChangeReasonEnum;
import org.example.trademodel.positionmonitorlog.PositionRiskTrendEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PositionMonitoringBackendContractTest {
    @Test
    void frozenEnumsExposeEveryIndependentSemanticState() {
        assertThat(names(PositionEntryLogicStatusEnum.values()))
                .containsExactly("STILL_VALID", "WEAKENED", "INVALIDATED", "NOT_APPLICABLE");
        assertThat(names(PositionMonitorConclusionEnum.values())).containsExactly(
                "LOGIC_VALID", "LOGIC_WEAKENED", "PLAN_INVALIDATED", "NEAR_STOP_LOSS",
                "NEAR_TAKE_PROFIT", "HIGH_RISK_OBSERVATION", "WAIT_USER_CONFIRM_CLOSE");
        assertThat(names(PositionReversalStatusEnum.values()))
                .containsExactly("NO_REVERSAL", "WEAK_REVERSAL", "STRONG_REVERSAL");
        assertThat(names(PositionRiskChangeReasonEnum.values())).containsExactly(
                "NO_CLEAR_RISK_FACTOR", "OPPOSING_EVIDENCE_INCREASED", "STRUCTURE_CHANGED",
                "EVENT_IMPACT", "DATA_QUALITY_DEGRADED");
        assertThat(names(PositionMonitorSuggestedActionEnum.values())).containsExactly(
                "CONTINUE_HOLD", "NO_ADD_POSITION", "REDUCE_POSITION", "TIGHTEN_STOP",
                "MOVE_STOP", "PARTIAL_TAKE_PROFIT", "WAIT_CONFIRMATION", "RECORD_CLOSE_REVIEW");
        assertThat(names(PositionMonitorSourceStatusEnum.values()))
                .containsExactly("VERIFIED", "PENDING_VERIFICATION", "INVALID");
        assertThat(names(PositionRiskLevelEnum.values()))
                .containsExactly("LOW", "MEDIUM", "HIGH", "EXTREME");
        assertThat(names(PositionRiskTrendEnum.values()))
                .containsExactly("STABLE", "INCREASED", "SHARPLY_INCREASED");
        assertThat(names(PositionMonitorDataStateEnum.values())).containsExactly(
                "NO_POSITION", "OPEN_MONITORING", "PARTIAL", "WAITING_MONITOR_DATA", "RISK_ESCALATED",
                "PLAN_INVALIDATED", "CLOSED");
    }

    @Test
    void everyManualActionHasAnExplicitConclusionBoundary() {
        assertThat(PositionMonitorSuggestedActionEnum.CONTINUE_HOLD
                .isAllowedFor(PositionMonitorConclusionEnum.LOGIC_VALID)).isTrue();
        assertThat(PositionMonitorSuggestedActionEnum.NO_ADD_POSITION
                .isAllowedFor(PositionMonitorConclusionEnum.LOGIC_WEAKENED)).isTrue();
        assertThat(PositionMonitorSuggestedActionEnum.REDUCE_POSITION
                .isAllowedFor(PositionMonitorConclusionEnum.HIGH_RISK_OBSERVATION)).isTrue();
        assertThat(PositionMonitorSuggestedActionEnum.TIGHTEN_STOP
                .isAllowedFor(PositionMonitorConclusionEnum.NEAR_STOP_LOSS)).isTrue();
        assertThat(PositionMonitorSuggestedActionEnum.MOVE_STOP
                .isAllowedFor(PositionMonitorConclusionEnum.NEAR_STOP_LOSS)).isTrue();
        assertThat(PositionMonitorSuggestedActionEnum.PARTIAL_TAKE_PROFIT
                .isAllowedFor(PositionMonitorConclusionEnum.NEAR_TAKE_PROFIT)).isTrue();
        assertThat(PositionMonitorSuggestedActionEnum.WAIT_CONFIRMATION
                .isAllowedFor(PositionMonitorConclusionEnum.PLAN_INVALIDATED)).isTrue();
        assertThat(PositionMonitorSuggestedActionEnum.RECORD_CLOSE_REVIEW
                .isAllowedFor(PositionMonitorConclusionEnum.WAIT_USER_CONFIRM_CLOSE)).isTrue();

        assertThat(PositionMonitorSuggestedActionEnum.REDUCE_POSITION
                .isAllowedFor(PositionMonitorConclusionEnum.LOGIC_VALID)).isFalse();
        assertThat(names(PositionMonitorSuggestedActionEnum.values()))
                .noneMatch(name -> name.startsWith("AUTO_"));
    }

    @Test
    void monitorTrustRequiresVerifiedSourceAndUnexpiredWindow() {
        LocalDateTime asOf = LocalDateTime.of(2026, 8, 10, 12, 0);
        PositionMonitorLogDTO log = new PositionMonitorLogDTO();
        log.setObservedAt(asOf.minusMinutes(1));
        log.setFreshUntil(asOf.plusMinutes(1));

        for (PositionMonitorSourceStatusEnum status : PositionMonitorSourceStatusEnum.values()) {
            log.setMonitorSourceStatus(status.name());
            assertThat(log.isTrustedAndFreshAt(asOf)).isEqualTo(status == PositionMonitorSourceStatusEnum.VERIFIED);
        }

        log.setMonitorSourceStatus(PositionMonitorSourceStatusEnum.VERIFIED.name());
        log.setFreshUntil(asOf);
        assertThat(log.isTrustedAndFreshAt(asOf)).isFalse();
        log.setFreshUntil(asOf.minusNanos(1));
        assertThat(log.isTrustedAndFreshAt(asOf)).isFalse();
    }

    @Test
    void missingOrUnknownRiskNeverBecomesAnInventedLevel() {
        assertThat(PositionMonitorPolicy.normalizeRiskLevel(null)).isNull();
        assertThat(PositionMonitorPolicy.normalizeRiskLevel("UNKNOWN")).isNull();
        assertThat(PositionMonitorPolicy.normalizeRiskLevel("EXTREME")).isEqualTo("EXTREME");
    }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }
}
