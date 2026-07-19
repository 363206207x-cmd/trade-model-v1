package org.example.trademodel.providercall;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.providercall.profile.ProfileTransitionResult;
import org.example.trademodel.providercall.profile.ProfileTransitionSignal;
import org.example.trademodel.providercall.profile.ScanProfileTransitionService;
import org.example.trademodel.service.RuleConfigService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScanProfileTransitionServiceTest {

    @Test
    void downgradeRequiresRecoveryCycles() {
        Fixture fixture = fixture();
        ProfileTransitionResult emergency = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                hotReset(), "trace-1");
        fixture.clock.advance(Duration.ofSeconds(301));

        ProfileTransitionResult first = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-2");
        ProfileTransitionResult second = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-3");

        assertThat(emergency.effectiveProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(first.effectiveProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(second.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void downgradeMovesOneLevelAtATime() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, hotReset(), "trace-1");
        fixture.clock.advance(Duration.ofSeconds(301));
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, ProfileTransitionSignal.recovery(), "trace-2");
        ProfileTransitionResult result = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-3");
        assertThat(result.previousProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(result.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void profileDoesNotFlapInsideCooldown() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-1");
        fixture.clock.advance(Duration.ofSeconds(100));
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, ProfileTransitionSignal.recovery(), "trace-2");
        ProfileTransitionResult held = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-3");
        assertThat(held.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(held.effectiveReason()).isEqualTo("RECOVERY_HYSTERESIS");
    }

    @Test
    void scanProfileRuntimeEndpointExplainsEffectiveReason() {
        Fixture fixture = fixture();
        ProfileTransitionResult result = fixture.service.evaluate("BTCUSDT", UserScanProfile.STANDARD,
                hotReset(), "trace-1");
        assertThat(result.effectiveReason()).isEqualTo("HOT_RESET");
        assertThat(result.ruleVersion()).isEqualTo("v-test");
        assertThat(result.traceId()).isEqualTo("trace-1");
        verify(fixture.mapper, times(1)).insert(any());
    }

    @Test
    void manualHighIsNotAutoDowngraded() {
        Fixture fixture = fixture();
        ProfileTransitionResult result = fixture.service.evaluate("BTCUSDT", UserScanProfile.HIGH,
                ProfileTransitionSignal.recovery(), "trace-1");
        assertThat(result.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void missingThresholdConfigFailsClosedWithoutTransition() {
        RuleConfigService rules = mock(RuleConfigService.class);
        when(rules.getRuleConfigMap()).thenReturn(Map.of());
        RuleVersionLogMapper mapper = mock(RuleVersionLogMapper.class);
        ScanProfileTransitionService service = new ScanProfileTransitionService(rules, mapper,
                Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC));
        ProfileTransitionResult result = service.evaluate("BTCUSDT", UserScanProfile.LOW, hotReset(), "trace-1");
        assertThat(result.changed()).isFalse();
        assertThat(result.effectiveReason()).isEqualTo("PROFILE_RULE_CONFIG_UNAVAILABLE");
    }

    @Test
    void nearStopRaisesOnlyAffectedPositionProfile() {
        Fixture fixture = fixture();
        ProfileTransitionSignal nearStop = new ProfileTransitionSignal(null, null, null, null,
                BigDecimal.ZERO, null, null, null, null, false, null, false, false, null);
        assertThat(fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, nearStop, "trace-stop")
                .effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(fixture.service.currentProfile("ETHUSDT")).isEqualTo(RuntimeScanProfile.LOW);
    }

    @Test
    void nearTargetRaisesOnlyAffectedPositionProfile() {
        Fixture fixture = fixture();
        ProfileTransitionSignal nearTarget = new ProfileTransitionSignal(null, null, null, null,
                null, BigDecimal.ZERO, null, null, null, false, null, false, false, null);
        assertThat(fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, nearTarget, "trace-target")
                .effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(fixture.service.currentProfile("ETHUSDT")).isEqualTo(RuntimeScanProfile.LOW);
    }

    @Test
    void hotResetRaisesAffectedAssetsToEmergency() {
        Fixture fixture = fixture();
        assertThat(fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, hotReset(), "trace-hot")
                .effectiveProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(fixture.service.currentProfile("ETHUSDT")).isEqualTo(RuntimeScanProfile.LOW);
    }

    @Test
    void highRiskRaisesAffectedAssetProfile() {
        Fixture fixture = fixture();
        ProfileTransitionSignal highRisk = new ProfileTransitionSignal(null, null, null, null,
                null, null, null, null, null, true, null, false, true, null);
        assertThat(fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, highRisk, "trace-risk")
                .effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void profileTransitionIsAudited() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, hotReset(), "trace-audit");
        verify(fixture.mapper).insert(any());
    }

    @Test
    void auditExceptionDoesNotPublishFirstTransition() {
        Fixture fixture = fixture();
        when(fixture.mapper.insert(any())).thenThrow(new IllegalStateException("audit unavailable"));

        assertThatThrownBy(() -> fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-failed-audit"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("audit unavailable");

        assertThat(fixture.service.currentProfile("BTCUSDT")).isEqualTo(RuntimeScanProfile.LOW);
        ProfileTransitionResult current = fixture.service.current("BTCUSDT", "read-after-failure");
        assertThat(current.effectiveProfile()).isEqualTo(RuntimeScanProfile.LOW);
        assertThat(current.effectiveReason()).isEqualTo("NO_RUNTIME_ESCALATION");
    }

    @Test
    void auditZeroRowsDoesNotPublishTransition() {
        Fixture fixture = fixture();
        when(fixture.mapper.insert(any())).thenReturn(0);

        assertThatThrownBy(() -> fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-zero-row"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("profile transition audit insert count must be exactly 1");

        assertThat(fixture.service.currentProfile("BTCUSDT")).isEqualTo(RuntimeScanProfile.LOW);
        assertThat(fixture.service.current("BTCUSDT", "read-zero-row").effectiveReason())
                .isEqualTo("NO_RUNTIME_ESCALATION");
    }

    @Test
    void auditUnexpectedRowCountDoesNotPublishTransition() {
        Fixture fixture = fixture();
        when(fixture.mapper.insert(any())).thenReturn(2);

        assertThatThrownBy(() -> fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-two-rows"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("profile transition audit insert count must be exactly 1");

        assertThat(fixture.service.currentProfile("BTCUSDT")).isEqualTo(RuntimeScanProfile.LOW);
        assertThat(fixture.service.current("BTCUSDT", "read-two-rows").effectiveReason())
                .isEqualTo("NO_RUNTIME_ESCALATION");
    }

    @Test
    void failedDowngradeAuditPreservesEntirePreviousState() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-high");
        fixture.clock.advance(Duration.ofSeconds(301));
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-recovery-one");
        ProfileTransitionResult beforeFailure = fixture.service.current("BTCUSDT", "before-failure");
        when(fixture.mapper.insert(any())).thenThrow(new IllegalStateException("downgrade audit unavailable"));

        assertThatThrownBy(() -> fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-recovery-two"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("downgrade audit unavailable");

        ProfileTransitionResult afterFailure = fixture.service.current("BTCUSDT", "after-failure");
        assertThat(afterFailure.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(afterFailure.effectiveSince()).isEqualTo(beforeFailure.effectiveSince());
        assertThat(afterFailure.nextDowngradeEligibleAt()).isEqualTo(beforeFailure.nextDowngradeEligibleAt());
        assertThat(afterFailure.effectiveReason()).isEqualTo(beforeFailure.effectiveReason());
        assertThat(afterFailure.ruleVersion()).isEqualTo(beforeFailure.ruleVersion());
    }

    @Test
    void successfulRetryAfterAuditFailurePublishesOnce() {
        Fixture fixture = fixture();
        AtomicInteger insertAttempts = new AtomicInteger();
        AtomicInteger successfulInserts = new AtomicInteger();
        when(fixture.mapper.insert(any())).thenAnswer(invocation -> {
            if (insertAttempts.getAndIncrement() == 0) {
                throw new IllegalStateException("audit unavailable");
            }
            successfulInserts.incrementAndGet();
            return 1;
        });

        assertThatThrownBy(() -> fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-first"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(fixture.service.currentProfile("BTCUSDT")).isEqualTo(RuntimeScanProfile.LOW);

        ProfileTransitionResult retry = fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-retry");

        assertThat(retry.changed()).isTrue();
        assertThat(retry.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(fixture.service.currentProfile("BTCUSDT")).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(insertAttempts).hasValue(2);
        assertThat(successfulInserts).hasValue(1);
        verify(fixture.mapper, times(2)).insert(any());
    }

    @Test
    void auditCallbackSeesPreviousPublishedStateUntilInsertReturns() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.STANDARD,
                ProfileTransitionSignal.recovery(), "trace-initialize");
        AtomicReference<RuntimeScanProfile> profileDuringAudit = new AtomicReference<>();
        AtomicReference<ProfileTransitionResult> stateDuringAudit = new AtomicReference<>();
        when(fixture.mapper.insert(any())).thenAnswer(invocation -> {
            profileDuringAudit.set(fixture.service.currentProfile("BTCUSDT"));
            stateDuringAudit.set(fixture.service.current("BTCUSDT", "read-inside-audit"));
            return 1;
        });

        ProfileTransitionResult changed = fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-high");

        assertThat(profileDuringAudit).hasValue(RuntimeScanProfile.STANDARD);
        assertThat(stateDuringAudit.get().effectiveProfile()).isEqualTo(RuntimeScanProfile.STANDARD);
        assertThat(changed.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(fixture.service.currentProfile("BTCUSDT")).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void readOnlyThreadSeesOldStateAfterAuditFailure() throws Exception {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.STANDARD,
                ProfileTransitionSignal.recovery(), "trace-initialize");
        CountDownLatch auditEntered = new CountDownLatch(1);
        CountDownLatch releaseAudit = new CountDownLatch(1);
        when(fixture.mapper.insert(any())).thenAnswer(invocation -> {
            auditEntered.countDown();
            if (!releaseAudit.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting to fail transition audit");
            }
            throw new IllegalStateException("audit unavailable");
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ProfileTransitionResult> evaluation = pool.submit(() -> fixture.service.evaluate(
                    "BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-failed-high"));
            assertThat(auditEntered.await(2, TimeUnit.SECONDS)).isTrue();
            Future<ProfileTransitionResult> read = pool.submit(() ->
                    fixture.service.current("BTCUSDT", "read-after-audit-failure"));
            assertThatThrownBy(() -> read.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            releaseAudit.countDown();
            assertThatThrownBy(() -> evaluation.get(2, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(IllegalStateException.class);
            assertThat(read.get(2, TimeUnit.SECONDS).effectiveProfile())
                    .isEqualTo(RuntimeScanProfile.STANDARD);
        } finally {
            releaseAudit.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void unchangedEvaluationDoesNotRequireAudit() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-high");
        clearInvocations(fixture.mapper);
        fixture.clock.advance(Duration.ofSeconds(301));

        ProfileTransitionResult firstRecovery = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-recovery-one");

        assertThat(firstRecovery.changed()).isFalse();
        assertThat(firstRecovery.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        verifyNoInteractions(fixture.mapper);
        assertThat(fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-recovery-two").effectiveProfile())
                .isEqualTo(RuntimeScanProfile.STANDARD);
        verify(fixture.mapper, times(1)).insert(any());
    }

    @Test
    void currentDoesNotMutateOrCreateMissingStateOrWriteAudit() {
        Fixture fixture = fixture();

        ProfileTransitionResult current = fixture.service.current("BTCUSDT", "read-1");
        ProfileTransitionResult firstExecution = fixture.service.evaluate("BTCUSDT", UserScanProfile.HIGH,
                ProfileTransitionSignal.recovery(), "execution-1");

        assertThat(current.effectiveProfile()).isEqualTo(RuntimeScanProfile.LOW);
        assertThat(current.effectiveReason()).isEqualTo("NO_RUNTIME_ESCALATION");
        assertThat(firstExecution.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(firstExecution.changed()).isFalse();
        verifyNoInteractions(fixture.mapper);
    }

    @Test
    void currentReturnsLastExecutionReasonAndPreservesNextDowngradeEligibleAt() {
        Fixture fixture = fixture();
        ProfileTransitionResult execution = fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, highSignal(), "execution-high");

        ProfileTransitionResult current = fixture.service.current("BTCUSDT", "read-only");

        assertThat(current.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(current.effectiveReason()).isEqualTo("HIGH_RISK");
        assertThat(current.ruleVersion()).isEqualTo("v-test");
        assertThat(current.nextDowngradeEligibleAt()).isEqualTo(execution.nextDowngradeEligibleAt());
        assertThat(current.changed()).isFalse();
        verify(fixture.mapper, times(1)).insert(any());
    }

    @Test
    void oneHundredCurrentQueriesDoNotAdvanceRecoveryCycles() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, highSignal(), "execution-high");
        fixture.clock.advance(Duration.ofSeconds(301));

        for (int index = 0; index < 100; index++) {
            ProfileTransitionResult current = fixture.service.current("BTCUSDT", "read-" + index);
            assertThat(current.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        }
        ProfileTransitionResult firstRecovery = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "execution-recovery-1");
        ProfileTransitionResult secondRecovery = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "execution-recovery-2");

        assertThat(firstRecovery.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(secondRecovery.effectiveProfile()).isEqualTo(RuntimeScanProfile.STANDARD);
        verify(fixture.mapper, times(2)).insert(any());
    }

    @Test
    void missingCurrentStateDoesNotLoadTransitionRules() {
        Fixture fixture = fixture();

        fixture.service.current("BTCUSDT", "read-only");

        verifyNoInteractions(fixture.rules, fixture.mapper);
    }

    @Test
    void readMethodsUseSameSynchronizationBoundaryAsEvaluate() throws Exception {
        assertThat(Modifier.isSynchronized(ScanProfileTransitionService.class.getDeclaredMethod(
                "evaluate", String.class, UserScanProfile.class, ProfileTransitionSignal.class, String.class)
                .getModifiers())).isTrue();
        assertThat(Modifier.isSynchronized(ScanProfileTransitionService.class.getDeclaredMethod(
                "current", String.class, String.class).getModifiers())).isTrue();
        assertThat(Modifier.isSynchronized(ScanProfileTransitionService.class.getDeclaredMethod(
                "currentProfile", String.class).getModifiers())).isTrue();
    }

    @Test
    void currentCannotObservePartiallyCompletedTransition() throws Exception {
        BlockingFixture fixture = blockingFixture();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ProfileTransitionResult> evaluation = pool.submit(() -> fixture.service.evaluate(
                    "BTCUSDT", UserScanProfile.AUTO, highSignal(), "execution-high"));
            assertThat(fixture.auditEntered.await(2, TimeUnit.SECONDS)).isTrue();

            CountDownLatch readAttempted = new CountDownLatch(1);
            Future<ProfileTransitionResult> current = pool.submit(() -> {
                readAttempted.countDown();
                return fixture.service.current("BTCUSDT", "read-during-transition");
            });
            assertThat(readAttempted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> current.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            fixture.releaseAudit.countDown();
            ProfileTransitionResult completed = evaluation.get(2, TimeUnit.SECONDS);
            ProfileTransitionResult visible = current.get(2, TimeUnit.SECONDS);

            assertThat(visible.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
            assertThat(visible.effectiveReason()).isEqualTo("HIGH_RISK");
            assertThat(visible.effectiveSince()).isEqualTo(completed.effectiveSince());
            assertThat(visible.nextDowngradeEligibleAt()).isEqualTo(completed.nextDowngradeEligibleAt());
            assertThat(visible.ruleVersion()).isEqualTo("v-test");
        } finally {
            fixture.releaseAudit.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void currentProfileCannotReadDuringPartialTransition() throws Exception {
        BlockingFixture fixture = blockingFixture();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ProfileTransitionResult> evaluation = pool.submit(() -> fixture.service.evaluate(
                    "BTCUSDT", UserScanProfile.AUTO, highSignal(), "execution-high"));
            assertThat(fixture.auditEntered.await(2, TimeUnit.SECONDS)).isTrue();

            CountDownLatch readAttempted = new CountDownLatch(1);
            Future<RuntimeScanProfile> currentProfile = pool.submit(() -> {
                readAttempted.countDown();
                return fixture.service.currentProfile("BTCUSDT");
            });
            assertThat(readAttempted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> currentProfile.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            fixture.releaseAudit.countDown();
            assertThat(evaluation.get(2, TimeUnit.SECONDS).effectiveProfile())
                    .isEqualTo(RuntimeScanProfile.HIGH);
            assertThat(currentProfile.get(2, TimeUnit.SECONDS)).isEqualTo(RuntimeScanProfile.HIGH);
        } finally {
            fixture.releaseAudit.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void completedExecutionStateIsVisibleToConcurrentReaders() throws Exception {
        Fixture fixture = fixture();
        ProfileTransitionResult completed = fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, highSignal(), "execution-high");
        ExecutorService readers = Executors.newFixedThreadPool(32);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int reader = 0; reader < 32; reader++) {
                int readerId = reader;
                futures.add(readers.submit(() -> {
                    for (int query = 0; query < 100; query++) {
                        ProfileTransitionResult visible = fixture.service.current(
                                "BTCUSDT", "read-" + readerId + "-" + query);
                        assertThat(visible.effectiveProfile()).isEqualTo(completed.effectiveProfile());
                        assertThat(visible.effectiveReason()).isEqualTo(completed.effectiveReason());
                        assertThat(visible.effectiveSince()).isEqualTo(completed.effectiveSince());
                        assertThat(visible.nextDowngradeEligibleAt())
                                .isEqualTo(completed.nextDowngradeEligibleAt());
                        assertThat(visible.ruleVersion()).isEqualTo(completed.ruleVersion());
                        assertThat(fixture.service.currentProfile("BTCUSDT"))
                                .isEqualTo(completed.effectiveProfile());
                    }
                    return null;
                }));
            }
            awaitAll(futures);
            verify(fixture.mapper, times(1)).insert(any());
        } finally {
            readers.shutdownNow();
        }
    }

    @Test
    void concurrentReadOnlyQueriesRemainMutationFree() throws Exception {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, highSignal(), "execution-high");
        ProfileTransitionResult before = fixture.service.current("BTCUSDT", "before-reads");
        ExecutorService readers = Executors.newFixedThreadPool(16);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int reader = 0; reader < 16; reader++) {
                int readerId = reader;
                futures.add(readers.submit(() -> {
                    for (int query = 0; query < 100; query++) {
                        fixture.service.current("BTCUSDT", "read-" + readerId + "-" + query);
                        fixture.service.currentProfile("BTCUSDT");
                    }
                    return null;
                }));
            }
            awaitAll(futures);
        } finally {
            readers.shutdownNow();
        }

        ProfileTransitionResult after = fixture.service.current("BTCUSDT", "after-reads");
        assertThat(after.effectiveProfile()).isEqualTo(before.effectiveProfile());
        assertThat(after.effectiveReason()).isEqualTo(before.effectiveReason());
        assertThat(after.effectiveSince()).isEqualTo(before.effectiveSince());
        assertThat(after.nextDowngradeEligibleAt()).isEqualTo(before.nextDowngradeEligibleAt());
        assertThat(after.ruleVersion()).isEqualTo(before.ruleVersion());
        verify(fixture.mapper, times(1)).insert(any());

        fixture.clock.advance(Duration.ofSeconds(301));
        ProfileTransitionResult firstRecovery = fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, ProfileTransitionSignal.recovery(), "recovery-1");
        ProfileTransitionResult secondRecovery = fixture.service.evaluate(
                "BTCUSDT", UserScanProfile.AUTO, ProfileTransitionSignal.recovery(), "recovery-2");
        assertThat(firstRecovery.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(secondRecovery.effectiveProfile()).isEqualTo(RuntimeScanProfile.STANDARD);
        verify(fixture.mapper, times(2)).insert(any());
    }

    @Test
    void missingStateConcurrentReadsDoNotCreateState() throws Exception {
        Fixture fixture = fixture();
        ExecutorService readers = Executors.newFixedThreadPool(16);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int reader = 0; reader < 16; reader++) {
                int readerId = reader;
                futures.add(readers.submit(() -> {
                    for (int query = 0; query < 100; query++) {
                        ProfileTransitionResult current = fixture.service.current(
                                "UNKNOWN", "read-" + readerId + "-" + query);
                        assertThat(current.effectiveProfile()).isEqualTo(RuntimeScanProfile.LOW);
                        assertThat(current.effectiveReason()).isEqualTo("NO_RUNTIME_ESCALATION");
                        assertThat(fixture.service.currentProfile("UNKNOWN"))
                                .isEqualTo(RuntimeScanProfile.LOW);
                    }
                    return null;
                }));
            }
            awaitAll(futures);
        } finally {
            readers.shutdownNow();
        }
        verifyNoInteractions(fixture.rules, fixture.mapper);

        ProfileTransitionResult firstExecution = fixture.service.evaluate(
                "UNKNOWN", UserScanProfile.HIGH, ProfileTransitionSignal.recovery(), "first-execution");
        assertThat(firstExecution.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(firstExecution.changed()).isFalse();
        verifyNoInteractions(fixture.mapper);
    }

    private static Fixture fixture() {
        RuleConfigService rules = mock(RuleConfigService.class);
        when(rules.getRuleConfigMap()).thenReturn(ruleMap());
        when(rules.resolveActiveRuleVersion()).thenReturn("v-test");
        RuleVersionLogMapper mapper = mock(RuleVersionLogMapper.class);
        when(mapper.insert(any())).thenReturn(1);
        MutableClock clock = new MutableClock(Instant.parse("2026-07-10T10:00:00Z"));
        return new Fixture(new ScanProfileTransitionService(rules, mapper, clock), rules, mapper, clock);
    }

    private static BlockingFixture blockingFixture() {
        RuleConfigService rules = mock(RuleConfigService.class);
        when(rules.getRuleConfigMap()).thenReturn(ruleMap());
        when(rules.resolveActiveRuleVersion()).thenReturn("v-test");
        RuleVersionLogMapper mapper = mock(RuleVersionLogMapper.class);
        CountDownLatch auditEntered = new CountDownLatch(1);
        CountDownLatch releaseAudit = new CountDownLatch(1);
        when(mapper.insert(any())).thenAnswer(ignored -> {
            auditEntered.countDown();
            if (!releaseAudit.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting to release transition audit");
            }
            return 1;
        });
        MutableClock clock = new MutableClock(Instant.parse("2026-07-10T10:00:00Z"));
        return new BlockingFixture(new ScanProfileTransitionService(rules, mapper, clock),
                mapper, auditEntered, releaseAudit);
    }

    private static void awaitAll(List<Future<Void>> futures) throws Exception {
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
    }

    private static Map<String, RuleConfigDO> ruleMap() {
        Map<String, RuleConfigDO> rules = new LinkedHashMap<>();
        value(rules, "emergency_price_movement_1m", "0.05");
        value(rules, "emergency_liquidation_spike", "90");
        value(rules, "emergency_confused_score", "85");
        value(rules, "high_price_movement_1m", "0.02");
        value(rules, "high_atr_multiple_5m", "2");
        value(rules, "high_volume_spike", "2.5");
        value(rules, "high_spread_spike", "2");
        value(rules, "high_open_interest_change", "0.10");
        value(rules, "high_funding_extremity", "80");
        value(rules, "near_boundary_distance", "0.01");
        value(rules, "data_quality_deterioration_score", "60");
        value(rules, "standard_confused_score", "55");
        value(rules, "high_min_hold_seconds", "300");
        value(rules, "emergency_min_hold_seconds", "120");
        value(rules, "recovery_confirm_cycles", "2");
        value(rules, "downgrade_cooldown_seconds", "300");
        return rules;
    }

    private static void value(Map<String, RuleConfigDO> rules, String suffix, String value) {
        RuleConfigDO row = new RuleConfigDO();
        row.setRuleKey("provider.scan." + suffix);
        row.setRuleValue(value);
        rules.put(row.getRuleKey(), row);
    }

    private static ProfileTransitionSignal hotReset() {
        return new ProfileTransitionSignal(null, null, null, null, null, null, null, null, null,
                false, null, true, false, null);
    }

    private static ProfileTransitionSignal highSignal() {
        return new ProfileTransitionSignal(new BigDecimal("0.03"), null, null, null, null, null, null,
                null, null, false, null, false, false, null);
    }

    private record Fixture(ScanProfileTransitionService service, RuleConfigService rules,
                           RuleVersionLogMapper mapper, MutableClock clock) {}

    private record BlockingFixture(ScanProfileTransitionService service, RuleVersionLogMapper mapper,
                                   CountDownLatch auditEntered, CountDownLatch releaseAudit) {}

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
