package org.example.trademodel.service.impl;

import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.telegram.HighValueAlertPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushRecheckTelegramSafetyContractTest {

    @Test
    void materialRecheckStatesMapToExistingSafetyCategoryWithoutDirectionalAlert() {
        assertThat(PushRecheckServiceImpl.safetyChangeType(
                RecheckStatusEnum.DRIFTED_FROM_ENTRY_ZONE, null))
                .isEqualTo(HighValueAlertPolicy.SafetyChangeType.EXECUTION_DRIFT);
        assertThat(PushRecheckServiceImpl.safetyChangeType(RecheckStatusEnum.RISK_BLOCKED, null))
                .isEqualTo(HighValueAlertPolicy.SafetyChangeType.RISK_BLOCKED);
        assertThat(PushRecheckServiceImpl.safetyChangeType(RecheckStatusEnum.CONFUSED_BLOCKED, null))
                .isEqualTo(HighValueAlertPolicy.SafetyChangeType.HIGH_CONFUSED);
        assertThat(PushRecheckServiceImpl.safetyChangeType(RecheckStatusEnum.EXPIRED, null))
                .isEqualTo(HighValueAlertPolicy.SafetyChangeType.PLAN_EXPIRED);
        assertThat(PushRecheckServiceImpl.safetyChangeType(
                RecheckStatusEnum.INVALIDATED, "{\"code\":\"DATA_QUALITY_BLOCKED\"}"))
                .isEqualTo(HighValueAlertPolicy.SafetyChangeType.DATA_QUALITY_BLOCKED);
        assertThat(PushRecheckServiceImpl.safetyChangeType(
                RecheckStatusEnum.INVALIDATED, "{\"code\":\"DERIVATIVES_STALE\"}"))
                .isEqualTo(HighValueAlertPolicy.SafetyChangeType.SOURCE_INVALID);
        assertThat(PushRecheckServiceImpl.safetyChangeType(
                RecheckStatusEnum.INVALIDATED, "{\"code\":\"INVALIDATED\"}"))
                .isEqualTo(HighValueAlertPolicy.SafetyChangeType.FINAL_INVALIDATED);
    }

    @Test
    void passingAndWaitingRechecksDoNotCreateHighValueSafetyMessages() {
        assertThat(PushRecheckServiceImpl.safetyChangeType(RecheckStatusEnum.REVIEW_PASSED, null)).isNull();
        assertThat(PushRecheckServiceImpl.safetyChangeType(RecheckStatusEnum.REVIEW_WAITING, null)).isNull();
    }
}
