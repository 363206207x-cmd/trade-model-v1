package org.example.trademodel.telegram;

import org.example.trademodel.entity.MessageDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class HighValueAlertPolicyTest {
    private final HighValueAlertPolicy policy = new HighValueAlertPolicy();

    @Test
    void onlyCompleteCurrentConfirmationFinalPassesOpportunityPolicy() {
        assertThat(policy.allowsOpportunity(opportunity("CONFIRMATION", "CURRENT", "TRIGGERED",
                true, true, true, true, true))).isTrue();
        assertThat(policy.allowsOpportunity(opportunity("REDUCED", "CURRENT", "TRIGGERED",
                true, true, true, true, true))).isFalse();
        assertThat(policy.allowsOpportunity(opportunity("PREPARATION", "CURRENT", "TRIGGERED",
                true, true, true, true, true))).isFalse();
        assertThat(policy.allowsOpportunity(opportunity("CONFIRMATION", "NEEDS_REVALIDATION", "TRIGGERED",
                true, true, true, true, true))).isFalse();
        assertThat(policy.allowsOpportunity(opportunity("CONFIRMATION", "CURRENT", "HIGH_RISK",
                true, true, true, true, true))).isFalse();
    }

    @Test
    void missingFinalPlanFieldsFailClosedWithoutPlaceholders() {
        assertThat(policy.allowsOpportunity(opportunity("CONFIRMATION", "CURRENT", "TRIGGERED",
                false, true, true, true, true))).isFalse();
        assertThat(policy.allowsOpportunity(opportunity("CONFIRMATION", "CURRENT", "TRIGGERED",
                true, false, true, true, true))).isFalse();
        assertThat(policy.allowsOpportunity(opportunity("CONFIRMATION", "CURRENT", "TRIGGERED",
                true, true, false, true, true))).isFalse();
        assertThat(policy.allowsOpportunity(opportunity("CONFIRMATION", "CURRENT", "TRIGGERED",
                true, true, true, false, true))).isFalse();
        assertThat(policy.allowsOpportunity(opportunity("CONFIRMATION", "CURRENT", "TRIGGERED",
                true, true, true, true, false))).isFalse();
    }

    @Test
    void broaderInAppPositionMessageContractRemainsAvailable() {
        assertThat(policy.allowsPositionMessage(positionMessage(
                "WEAKENED", "NO_REVERSAL", "MEDIUM", "STABLE", "LOGIC_WEAKENED"))).isTrue();
        assertThat(policy.allowsPositionMessage(positionMessage(
                "INVALIDATED", "NO_REVERSAL", "MEDIUM", "STABLE", "PLAN_INVALIDATED"))).isTrue();
        assertThat(policy.allowsPositionMessage(positionMessage(
                "STILL_VALID", "NO_REVERSAL", "LOW", "STABLE", "LOGIC_VALID"))).isFalse();
    }

    @Test
    void positionTelegramChangeUsesFrozenPriorityAndRealStopTargetFacts() {
        HighValueAlertPolicy.PositionTelegramQualification all = positionTelegram(
                true, true, true, true, "EXTREME", "SHARPLY_INCREASED", "STRONG_REVERSAL");
        assertThat(policy.resolveTelegramPositionChange(all))
                .contains(HighValueAlertPolicy.PositionTelegramChange.STOP_LOSS_BREACHED);

        HighValueAlertPolicy.PositionTelegramQualification noStop = positionTelegram(
                false, true, true, true, "EXTREME", "SHARPLY_INCREASED", "STRONG_REVERSAL");
        assertThat(policy.resolveTelegramPositionChange(noStop))
                .contains(HighValueAlertPolicy.PositionTelegramChange.RISK_EXTREME);

        HighValueAlertPolicy.PositionTelegramQualification noTarget = positionTelegram(
                true, false, false, true, "LOW", "STABLE", "NO_REVERSAL");
        assertThat(policy.resolveTelegramPositionChange(noTarget)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("canonicalPositionChanges")
    void allNineCanonicalPositionChangesMapWithoutConclusionFallback(
            HighValueAlertPolicy.PositionTelegramQualification qualification,
            HighValueAlertPolicy.PositionTelegramChange expected) {
        assertThat(policy.resolveTelegramPositionChange(qualification)).contains(expected);
    }

    @Test
    void priorityBetweenCompetingPositionFactsIsDeterministic() {
        assertThat(policy.resolveTelegramPositionChange(positionCase(
                true, true, false, false, false, true,
                "EXTREME", "STABLE", "NO_REVERSAL")))
                .contains(HighValueAlertPolicy.PositionTelegramChange.RISK_EXTREME);
        assertThat(policy.resolveTelegramPositionChange(positionCase(
                true, true, true, false, false, true,
                "MEDIUM", "STABLE", "NO_REVERSAL")))
                .contains(HighValueAlertPolicy.PositionTelegramChange.TAKE_PROFIT_REACHED);
        assertThat(policy.resolveTelegramPositionChange(positionCase(
                true, true, true, false, false, false,
                "HIGH", "SHARPLY_INCREASED", "NO_REVERSAL")))
                .contains(HighValueAlertPolicy.PositionTelegramChange.NEAR_STOP_LOSS);
        assertThat(policy.resolveTelegramPositionChange(positionCase(
                true, true, false, false, true, false,
                "MEDIUM", "INCREASED", "STRONG_REVERSAL")))
                .contains(HighValueAlertPolicy.PositionTelegramChange.RISK_INCREASED);
    }

    @Test
    void pendingStaleAndUnlinkedMonitorResultsCannotBecomeTelegramAlerts() {
        HighValueAlertPolicy.PositionTelegramQualification value = positionTelegram(
                true, true, false, false, "HIGH", "INCREASED", "STRONG_REVERSAL");
        assertThat(policy.resolveTelegramPositionChange(copyTrust(value, false, true, true))).isEmpty();
        assertThat(policy.resolveTelegramPositionChange(copyTrust(value, true, false, true))).isEmpty();
        assertThat(policy.resolveTelegramPositionChange(copyTrust(value, true, true, false))).isEmpty();
    }

    @Test
    void deliveryAllowlistRejectsSafetyAndLegacyLongFormMessages() {
        MessageDO opportunity = eligibleOpportunityMessage();
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(opportunity))
                .get().extracting(HighValueAlertPolicy.TelegramDeliveryIdentity::telegramCategory)
                .isEqualTo("EXECUTABLE_FINAL_PLAN");

        MessageDO safety = eligibleOpportunityMessage();
        safety.setCategory("OPPORTUNITY_PLAN_SAFETY_CHANGE");
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(safety)).isEmpty();

        MessageDO legacyPosition = eligiblePositionMessage("STRONG_REVERSAL");
        legacyPosition.setTitle("【持仓逻辑发生重要变化】");
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(legacyPosition)).isEmpty();

        MessageDO currentPosition = eligiblePositionMessage("STRONG_REVERSAL");
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(currentPosition))
                .get().extracting(HighValueAlertPolicy.TelegramDeliveryIdentity::changeState)
                .isEqualTo("STRONG_REVERSAL");

        MessageDO wrongPlanSubject = eligibleOpportunityMessage();
        wrongPlanSubject.setDedupeKey(TelegramDedupeKey.create(
                HighValueAlertPolicy.OPPORTUNITY_EVENT, "CONFIRMATION", 3, 15,
                41L, "FINAL_PLAN", "plan-other", LocalDateTime.of(2026, 8, 27, 12, 0)));
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(wrongPlanSubject)).isEmpty();

        MessageDO incompleteShortBody = eligiblePositionMessage("RISK_HIGH");
        incompleteShortBody.setBody("BTCUSDT  ·  做多\n\n变化：风险高");
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(incompleteShortBody)).isEmpty();
    }

    private static HighValueAlertPolicy.OpportunityQualification opportunity(
            String mode, String lifecycle, String state,
            boolean entry, boolean trigger, boolean stop, boolean target, boolean expiry) {
        return new HighValueAlertPolicy.OpportunityQualification(
                41L, true, true, true, true, mode, lifecycle, state, false,
                true, true, true, true, true, entry, trigger, stop, target, expiry,
                true, false, false, true, true);
    }

    private static HighValueAlertPolicy.PositionQualification positionMessage(
            String logic, String reversal, String risk, String trend, String conclusion) {
        return new HighValueAlertPolicy.PositionQualification(
                41L, true, true, true, logic, reversal, risk, trend, conclusion);
    }

    private static HighValueAlertPolicy.PositionTelegramQualification positionTelegram(
            boolean stopPresent, boolean targetPresent, boolean stopBreached, boolean targetReached,
            String risk, String trend, String reversal) {
        return new HighValueAlertPolicy.PositionTelegramQualification(
                41L, true, true, true, true, true, true, stopPresent, targetPresent,
                false, stopBreached, false, targetReached, risk, trend, reversal, true, true);
    }

    private static HighValueAlertPolicy.PositionTelegramQualification positionCase(
            boolean stopPresent, boolean targetPresent,
            boolean nearStop, boolean stopBreached, boolean nearTarget, boolean targetReached,
            String risk, String trend, String reversal) {
        return new HighValueAlertPolicy.PositionTelegramQualification(
                41L, true, true, true, true, true, true, stopPresent, targetPresent,
                nearStop, stopBreached, nearTarget, targetReached, risk, trend, reversal, true, true);
    }

    private static Stream<Arguments> canonicalPositionChanges() {
        return Stream.of(
                Arguments.of(positionCase(true, true, false, true, false, false,
                                "MEDIUM", "STABLE", "NO_REVERSAL"),
                        HighValueAlertPolicy.PositionTelegramChange.STOP_LOSS_BREACHED),
                Arguments.of(positionCase(true, true, false, false, false, false,
                                "EXTREME", "STABLE", "NO_REVERSAL"),
                        HighValueAlertPolicy.PositionTelegramChange.RISK_EXTREME),
                Arguments.of(positionCase(true, true, false, false, false, true,
                                "MEDIUM", "STABLE", "NO_REVERSAL"),
                        HighValueAlertPolicy.PositionTelegramChange.TAKE_PROFIT_REACHED),
                Arguments.of(positionCase(true, true, true, false, false, false,
                                "MEDIUM", "STABLE", "NO_REVERSAL"),
                        HighValueAlertPolicy.PositionTelegramChange.NEAR_STOP_LOSS),
                Arguments.of(positionCase(true, true, false, false, false, false,
                                "MEDIUM", "SHARPLY_INCREASED", "NO_REVERSAL"),
                        HighValueAlertPolicy.PositionTelegramChange.RISK_SHARPLY_INCREASED),
                Arguments.of(positionCase(true, true, false, false, false, false,
                                "HIGH", "STABLE", "NO_REVERSAL"),
                        HighValueAlertPolicy.PositionTelegramChange.RISK_HIGH),
                Arguments.of(positionCase(true, true, false, false, false, false,
                                "MEDIUM", "INCREASED", "NO_REVERSAL"),
                        HighValueAlertPolicy.PositionTelegramChange.RISK_INCREASED),
                Arguments.of(positionCase(true, true, false, false, true, false,
                                "MEDIUM", "STABLE", "NO_REVERSAL"),
                        HighValueAlertPolicy.PositionTelegramChange.NEAR_TAKE_PROFIT),
                Arguments.of(positionCase(true, true, false, false, false, false,
                                "MEDIUM", "STABLE", "STRONG_REVERSAL"),
                        HighValueAlertPolicy.PositionTelegramChange.STRONG_REVERSAL));
    }

    private static HighValueAlertPolicy.PositionTelegramQualification copyTrust(
            HighValueAlertPolicy.PositionTelegramQualification value,
            boolean verified, boolean fresh, boolean sameResult) {
        return new HighValueAlertPolicy.PositionTelegramQualification(
                value.userId(), value.activeManualPosition(), verified, fresh, sameResult,
                value.currentPriceTrusted(), value.entryPricePresent(), value.stopLossPresent(),
                value.takeProfitPresent(), value.nearStopLoss(), value.stopLossBreached(),
                value.nearTakeProfit(), value.takeProfitReached(), value.riskLevel(), value.riskTrend(),
                value.reversalStatus(), value.notTradeInstruction(), value.notOrderExecution());
    }

    static MessageDO eligibleOpportunityMessage() {
        MessageDO message = baseMessage("HIGH_PERMISSION_OPPORTUNITY", HighValueAlertPolicy.OPPORTUNITY_SHORT_TITLE);
        message.setSourceType("FINAL_PLAN");
        message.setSourceId("plan-9");
        message.setPlanId("plan-9");
        message.setAnalysisId("analysis-9");
        message.setExpiresAt(LocalDateTime.of(2026, 8, 27, 13, 0));
        message.setBody("BTCUSDT  ·  偏多  ·  确认型\n\n"
                + "入场：100 - 101\n触发：收盘确认\n止损：98\n目标：105\n有效至：2026-08-27 13:00 UTC"
                + "\n\n操作：打开系统重新校验");
        message.setDedupeKey(TelegramDedupeKey.create(
                HighValueAlertPolicy.OPPORTUNITY_EVENT, "CONFIRMATION", 3, 15,
                41L, "FINAL_PLAN", "plan-9", LocalDateTime.of(2026, 8, 27, 12, 0)));
        return message;
    }

    static MessageDO eligiblePositionMessage(String state) {
        MessageDO message = baseMessage("POSITION_LOGIC_RISK_CHANGE", HighValueAlertPolicy.POSITION_SHORT_TITLE);
        message.setSourceType("POSITION_MONITOR");
        message.setSourceId("201");
        message.setPositionId(91L);
        message.setExpiresAt(LocalDateTime.of(2026, 8, 27, 12, 5));
        String change = HighValueAlertPolicy.PositionTelegramChange.valueOf(state).displayText();
        message.setBody("BTCUSDT  ·  做多\n\n变化：" + change + "\n\n入场：100\n现价：99\n"
                + "止损：98  目标：105\n\n操作：打开持仓详情");
        message.setDedupeKey(TelegramDedupeKey.create(
                HighValueAlertPolicy.POSITION_EVENT, state, 3, 15,
                41L, "USER_POSITION", "91", LocalDateTime.of(2026, 8, 27, 12, 0)));
        return message;
    }

    private static MessageDO baseMessage(String category, String title) {
        MessageDO message = new MessageDO();
        message.setMessageId("message-1");
        message.setUserId(41L);
        message.setCategory(category);
        message.setTitle(title);
        message.setSymbol("BTCUSDT");
        message.setTraceId("trace-9");
        message.setNotTradeInstruction(true);
        message.setNotOrderExecution(true);
        return message;
    }
}
