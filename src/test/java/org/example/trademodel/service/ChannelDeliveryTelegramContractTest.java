package org.example.trademodel.service;

import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.mapper.ChannelDeliveryMapper;
import org.example.trademodel.telegram.TelegramDedupeKey;
import org.example.trademodel.telegram.HighValueAlertPolicy;
import org.example.trademodel.telegram.TelegramProperties;
import org.example.trademodel.telegram.TelegramReadinessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelDeliveryTelegramContractTest {
    @Mock private ChannelDeliveryMapper mapper;
    @Mock private MessageFactService messageFactService;
    @Mock private TelegramReadinessService readinessService;
    @Mock private UserConfigService userConfigService;

    private TelegramProperties properties;
    private ChannelDeliveryService service;

    @BeforeEach
    void setUp() {
        properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setBotToken("TEST_TOKEN");
        properties.setChatId("TEST_CHAT_ID");
        properties.setCooldownMinutes(15);
        org.mockito.Mockito.lenient().when(mapper.insert(any())).thenReturn(1);
        service = new ChannelDeliveryService(
                mapper, messageFactService, properties, readinessService, userConfigService);
    }

    @Test
    void sameMessageAndChannelAlwaysReuseExistingDeliveryFact() {
        ChannelDeliveryDO existing = new ChannelDeliveryDO();
        existing.setDeliveryId("delivery-existing");
        existing.setMessageId("message-1");
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message(2));
        when(mapper.selectByMessageAndChannel("message-1", "TELEGRAM")).thenReturn(existing);

        assertThat(service.queueTelegram(41L, "message-1")).isSameAs(existing);
        verify(mapper, never()).insert(any());
    }

    @Test
    void cooldownSuppressesSameOrLowerSeverityWithoutDeletingEvidence() {
        MessageDO message = message(2);
        ChannelDeliveryDO recent = new ChannelDeliveryDO();
        recent.setSeverityRank(2);
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message);
        when(mapper.selectRecentActiveCooldown(anyLong(), anyString(), any(LocalDateTime.class)))
                .thenReturn(recent);
        when(userConfigService.getUserConfig("41")).thenReturn(config(30));

        ChannelDeliveryDO result = service.queueTelegram(41L, "message-1");

        assertThat(result.getStatus()).isEqualTo("SUPPRESSED");
        assertThat(result.getErrorCode()).isEqualTo("DUPLICATE_OR_COOLDOWN");
        assertThat(result.getNextAttemptAt()).isNull();
        verify(mapper).insert(result);
    }

    @Test
    void severityEscalationCreatesANewQueuedAlertInsideCooldown() {
        MessageDO message = message(4);
        ChannelDeliveryDO recent = new ChannelDeliveryDO();
        recent.setSeverityRank(2);
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message);
        when(mapper.selectRecentActiveCooldown(anyLong(), anyString(), any(LocalDateTime.class)))
                .thenReturn(recent);
        when(readinessService.canAttemptDelivery()).thenReturn(true);

        ChannelDeliveryDO result = service.queueTelegram(41L, "message-1");

        assertThat(result.getStatus()).isEqualTo("QUEUED");
        assertThat(result.getNextAttemptAt()).isNotNull();
        assertThat(result.getSeverityRank()).isEqualTo(4);
        assertThat(result.getRecipientFingerprint()).isNotBlank().doesNotContain("TEST_CHAT_ID");
    }

    @Test
    void missingConfigurationCreatesDurableNotConfiguredStateRatherThanFakeSent() {
        when(readinessService.canAttemptDelivery()).thenReturn(false);
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message(2));

        ChannelDeliveryDO result = service.queueTelegram(41L, "message-1");

        assertThat(result.getStatus()).isEqualTo("NOT_CONFIGURED");
        assertThat(result.getProviderReference()).isNull();
        assertThat(result.getDeliveredAt()).isNull();
    }

    @Test
    void expiredSendingClaimsAreRecoveredAndClaimIsAtomic() {
        ChannelDeliveryDO due = new ChannelDeliveryDO();
        due.setDeliveryId("delivery-due");
        ChannelDeliveryDO claimed = new ChannelDeliveryDO();
        claimed.setDeliveryId("delivery-due");
        claimed.setStatus("SENDING");
        when(mapper.recoverExpiredClaims(any(LocalDateTime.class))).thenReturn(2);
        when(mapper.listDue(any(LocalDateTime.class), anyInt())).thenReturn(java.util.List.of(due));
        when(mapper.claim(anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(mapper.selectById("delivery-due")).thenReturn(claimed);

        assertThat(service.recoverExpiredClaims()).isEqualTo(2);
        assertThat(service.claimDue(20)).containsExactly(claimed);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mapper).claim(anyString(), token.capture(), any(LocalDateTime.class), any(LocalDateTime.class));
        assertThat(token.getValue()).isNotBlank();
    }

    @Test
    void providerSuccessClaimLossIsReconciledToSentWithoutRetry() {
        ChannelDeliveryDO delivery = new ChannelDeliveryDO();
        delivery.setDeliveryId("delivery-1");
        delivery.setMessageId("message-1");
        delivery.setChannel("TELEGRAM");
        delivery.setStatus("SENT");
        when(mapper.completeClaim(delivery)).thenReturn(0);
        when(mapper.selectById("delivery-1")).thenReturn(new ChannelDeliveryDO());
        when(mapper.finalizeProviderSuccess(delivery)).thenReturn(1);

        assertThat(service.reconcileProviderSuccess(delivery)).isTrue();

        verify(mapper).finalizeProviderSuccess(delivery);
        verify(mapper, never()).failClosedOutcome(anyString(), anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void unresolvedProviderSuccessFailsClosedWithoutAutomaticDueTime() {
        ChannelDeliveryDO delivery = new ChannelDeliveryDO();
        delivery.setDeliveryId("delivery-1");
        delivery.setMessageId("message-1");
        delivery.setChannel("TELEGRAM");
        delivery.setStatus("SENT");
        ChannelDeliveryDO unresolved = new ChannelDeliveryDO();
        unresolved.setStatus("FAILED");
        when(mapper.completeClaim(delivery)).thenReturn(0);
        when(mapper.selectById("delivery-1")).thenReturn(unresolved);
        when(mapper.finalizeProviderSuccess(delivery)).thenReturn(0);
        when(mapper.failClosedOutcome(anyString(), anyString(), anyString(), any(LocalDateTime.class))).thenReturn(1);

        assertThat(service.reconcileProviderSuccess(delivery)).isFalse();

        verify(mapper).failClosedOutcome(org.mockito.ArgumentMatchers.eq("delivery-1"),
                org.mockito.ArgumentMatchers.eq("DELIVERY_OUTCOME_UNKNOWN"), anyString(), any(LocalDateTime.class));
    }

    @Test
    void providerSuccessReconciliationDoesNotOverwriteExistingSentFact() {
        ChannelDeliveryDO delivery = new ChannelDeliveryDO();
        delivery.setDeliveryId("delivery-1");
        ChannelDeliveryDO persisted = new ChannelDeliveryDO();
        persisted.setStatus("SENT");
        when(mapper.completeClaim(delivery)).thenReturn(0);
        when(mapper.selectById("delivery-1")).thenReturn(persisted);

        assertThat(service.reconcileProviderSuccess(delivery)).isTrue();

        verify(mapper, never()).finalizeProviderSuccess(any());
        verify(mapper, never()).failClosedOutcome(anyString(), anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void sameSubjectAfterCooldownIsAllowedWhenNoActiveDeliveryFallsInsideCutoff() {
        MessageDO message = message(2);
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message);
        when(mapper.selectRecentActiveCooldown(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(null);
        when(readinessService.canAttemptDelivery()).thenReturn(true);

        ChannelDeliveryDO result = service.queueTelegram(41L, "message-1");

        assertThat(result.getStatus()).isEqualTo("QUEUED");
        assertThat(result.getNextAttemptAt()).isNotNull();
    }

    @Test
    void cooldownIdentityUsesStableBusinessSubjectAcrossSnapshotsAndTimeBuckets() {
        MessageDO first = message(2, "message-1", "201", 91L,
                LocalDateTime.of(2026, 8, 16, 12, 0));
        MessageDO second = message(2, "message-2", "202", 91L,
                LocalDateTime.of(2026, 8, 16, 12, 30));
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(first);
        when(messageFactService.findForUser(41L, "message-2")).thenReturn(second);
        when(readinessService.canAttemptDelivery()).thenReturn(true);

        service.queueTelegram(41L, "message-1");
        service.queueTelegram(41L, "message-2");

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(mapper, times(2)).selectRecentActiveCooldown(anyLong(), keys.capture(), any(LocalDateTime.class));
        assertThat(keys.getAllValues()).hasSize(2).allMatch(key -> key.startsWith("TG1C|"));
        assertThat(keys.getAllValues().stream().distinct()).containsExactly(keys.getAllValues().get(0));
    }

    @Test
    void distinctBusinessSubjectsHaveDistinctCooldownIdentity() {
        String first = TelegramDedupeKey.deliveryCooldownKey(
                "ACTIVE_POSITION_ATTENTION", "RISK_HIGH", 41L, "USER_POSITION", "1");
        String second = TelegramDedupeKey.deliveryCooldownKey(
                "ACTIVE_POSITION_ATTENTION", "RISK_HIGH", 41L, "USER_POSITION", "2");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void deliveryCooldownUsesPlanAndConcretePositionChangeRatherThanSnapshotOrLog() {
        String planFirst = TelegramDedupeKey.deliveryCooldownKey(
                "EXECUTABLE_FINAL_PLAN", "CONFIRMATION", 41L, "FINAL_PLAN", "plan-1");
        String planLater = TelegramDedupeKey.deliveryCooldownKey(
                "EXECUTABLE_FINAL_PLAN", "CONFIRMATION", 41L, "FINAL_PLAN", "plan-1");
        String riskHigh = TelegramDedupeKey.deliveryCooldownKey(
                "ACTIVE_POSITION_ATTENTION", "RISK_HIGH", 41L, "USER_POSITION", "91");
        String stopBreached = TelegramDedupeKey.deliveryCooldownKey(
                "ACTIVE_POSITION_ATTENTION", "STOP_LOSS_BREACHED", 41L, "USER_POSITION", "91");

        assertThat(planLater).isEqualTo(planFirst);
        assertThat(stopBreached).isNotEqualTo(riskHigh);
    }

    @Test
    void samePlanAfterCooldownOrRestartReusesItsSingleLifetimeDelivery() {
        MessageDO firstMessage = eligiblePlanMessage("message-plan-1", "plan-1");
        MessageDO laterMessage = eligiblePlanMessage("message-plan-later", "plan-1");
        ChannelDeliveryDO persisted = new ChannelDeliveryDO();
        persisted.setDeliveryId("delivery-plan-1");
        persisted.setMessageId("message-plan-1");
        persisted.setStatus("SENT");
        when(messageFactService.findForUser(41L, "message-plan-1")).thenReturn(firstMessage);
        when(messageFactService.findForUser(41L, "message-plan-later")).thenReturn(laterMessage);
        when(readinessService.canAttemptDelivery()).thenReturn(true);
        when(mapper.selectExistingLifetimeDelivery(anyLong(), anyString()))
                .thenReturn(null, persisted);

        ChannelDeliveryDO first = service.queueTelegram(41L, "message-plan-1");
        ChannelDeliveryDO duplicate = service.queueTelegram(41L, "message-plan-later");

        assertThat(first.getStatus()).isEqualTo("QUEUED");
        assertThat(duplicate).isSameAs(persisted);
        verify(mapper, times(1)).insert(any());
        verify(mapper, times(2)).selectExistingLifetimeDelivery(anyLong(), anyString());
    }

    @Test
    void providerRetryingPlanReusesOriginalDeliveryButNewPlanMayQueue() {
        MessageDO retryMessage = eligiblePlanMessage("message-plan-retry", "plan-1");
        MessageDO newPlanMessage = eligiblePlanMessage("message-plan-2", "plan-2");
        ChannelDeliveryDO retrying = new ChannelDeliveryDO();
        retrying.setDeliveryId("delivery-plan-1");
        retrying.setStatus("RETRYING");
        when(messageFactService.findForUser(41L, "message-plan-retry")).thenReturn(retryMessage);
        when(messageFactService.findForUser(41L, "message-plan-2")).thenReturn(newPlanMessage);
        when(mapper.selectExistingLifetimeDelivery(anyLong(), anyString()))
                .thenReturn(retrying, null);
        when(readinessService.canAttemptDelivery()).thenReturn(true);

        assertThat(service.queueTelegram(41L, "message-plan-retry")).isSameAs(retrying);
        ChannelDeliveryDO newPlan = service.queueTelegram(41L, "message-plan-2");

        assertThat(newPlan.getStatus()).isEqualTo("QUEUED");
        verify(mapper, times(1)).insert(any());
    }

    @Test
    void suppressedPlanDeliveryStillOwnsTheSingleLifetimeIdentity() {
        MessageDO message = eligiblePlanMessage("message-plan-later", "plan-1");
        ChannelDeliveryDO suppressed = new ChannelDeliveryDO();
        suppressed.setDeliveryId("delivery-plan-original");
        suppressed.setStatus("SUPPRESSED");
        when(messageFactService.findForUser(41L, "message-plan-later")).thenReturn(message);
        when(mapper.selectExistingLifetimeDelivery(anyLong(), anyString())).thenReturn(suppressed);

        assertThat(service.queueTelegram(41L, "message-plan-later")).isSameAs(suppressed);

        verify(mapper, never()).insert(any());
    }

    @Test
    void ineligibleSafetyMessageCannotQueueOrRequeue() {
        MessageDO safety = message(3);
        safety.setCategory("OPPORTUNITY_PLAN_SAFETY_CHANGE");
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(safety);

        assertThatThrownBy(() -> service.queueTelegram(41L, "message-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not eligible");
        assertThat(service.requeueTelegramForMessage(41L, "message-1")).isFalse();
        verify(mapper, never()).insert(any());
        verify(mapper, never()).requeue(anyString(), anyLong(), any(LocalDateTime.class));
    }

    @Test
    void expiredSendingRecoveryAndClaimsExcludeSentFromAutomaticDispatch() throws Exception {
        String mapperSource = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/mapper/ChannelDeliveryMapper.java"));

        assertThat(mapperSource)
                .contains("status = 'FAILED'", "error_code = 'DELIVERY_OUTCOME_UNKNOWN'", "next_attempt_at = NULL")
                .contains("status IN ('QUEUED', 'RETRYING')")
                .doesNotContain("error_code = 'CLAIM_LEASE_EXPIRED'");
    }

    @Test
    void failedOrNotConfiguredDeliveryCanBeManuallyRequeuedForItsOwner() {
        ChannelDeliveryDO delivery = new ChannelDeliveryDO();
        delivery.setDeliveryId("delivery-1");
        when(messageFactService.findForUser(41L, "message-1")).thenReturn(message(2));
        when(mapper.selectByMessageAndChannel("message-1", "TELEGRAM")).thenReturn(delivery);
        when(mapper.requeue(anyString(), anyLong(), any(LocalDateTime.class))).thenReturn(1);

        assertThat(service.requeueTelegramForMessage(41L, "message-1")).isTrue();

        verify(mapper).requeue(org.mockito.ArgumentMatchers.eq("delivery-1"),
                org.mockito.ArgumentMatchers.eq(41L), any(LocalDateTime.class));
    }

    private static MessageDO message(int severity) {
        return message(severity, "message-1", "201", 91L, LocalDateTime.of(2026, 8, 16, 12, 0));
    }

    private static MessageDO message(int severity, String messageId, String sourceId,
                                     Long stableSubjectId, LocalDateTime occurredAt) {
        String state = severity >= 4 ? "RISK_EXTREME" : severity >= 3 ? "RISK_HIGH" : "RISK_INCREASED";
        MessageDO message = eligiblePositionMessage(state);
        message.setMessageId(messageId);
        message.setSourceId(sourceId);
        message.setPositionId(stableSubjectId);
        message.setDedupeKey(TelegramDedupeKey.create(
                "POSITION_RISK_CHANGE", state, severity, 15,
                41L, "USER_POSITION", String.valueOf(stableSubjectId), occurredAt));
        return message;
    }

    private static MessageDO eligiblePositionMessage(String state) {
        MessageDO message = new MessageDO();
        message.setUserId(41L);
        message.setCategory("POSITION_LOGIC_RISK_CHANGE");
        message.setSourceType("POSITION_MONITOR");
        message.setSourceId("201");
        message.setPositionId(91L);
        message.setSymbol("BTCUSDT");
        message.setTraceId("trace-9");
        message.setTitle("【持仓需关注】");
        String change = HighValueAlertPolicy.PositionTelegramChange.valueOf(state).displayText();
        message.setBody("BTCUSDT  ·  做多\n\n变化：" + change + "\n\n入场：100\n现价：99\n"
                + "止损：98  目标：105\n\n操作：打开持仓详情");
        message.setExpiresAt(LocalDateTime.of(2026, 8, 16, 13, 0));
        message.setNotTradeInstruction(true);
        message.setNotOrderExecution(true);
        message.setDedupeKey(TelegramDedupeKey.create(
                "POSITION_RISK_CHANGE", state, 3, 15,
                41L, "USER_POSITION", "91", LocalDateTime.of(2026, 8, 16, 12, 0)));
        return message;
    }

    private static MessageDO eligiblePlanMessage(String messageId, String planId) {
        MessageDO message = new MessageDO();
        message.setMessageId(messageId);
        message.setUserId(41L);
        message.setCategory("HIGH_PERMISSION_OPPORTUNITY");
        message.setSourceType("FINAL_PLAN");
        message.setSourceId(planId);
        message.setPlanId(planId);
        message.setAnalysisId("analysis-" + planId);
        message.setSymbol("BTCUSDT");
        message.setTraceId("trace-" + planId);
        message.setTitle("【可复核执行计划】");
        message.setBody("BTCUSDT  ·  偏多  ·  确认型\n\n入场：100 - 101\n触发：15m 收盘确认"
                + "\n止损：98\n目标：105\n有效至：2026-08-16 13:00 UTC"
                + "\n\n操作：打开系统重新校验");
        message.setExpiresAt(LocalDateTime.of(2026, 8, 16, 13, 0));
        message.setNotTradeInstruction(true);
        message.setNotOrderExecution(true);
        message.setDedupeKey(TelegramDedupeKey.createPlanLifetime(
                "OPPORTUNITY_READY", "CONFIRMATION", 3,
                41L, "FINAL_PLAN", planId));
        return message;
    }

    private static UserConfigDO config(int cooldown) {
        UserConfigDO config = new UserConfigDO();
        config.setCooldownMinutes(cooldown);
        return config;
    }
}
