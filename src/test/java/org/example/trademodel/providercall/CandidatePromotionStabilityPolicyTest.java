package org.example.trademodel.providercall;

import org.example.trademodel.providercall.candidate.AutoCandidateRegistry;
import org.example.trademodel.providercall.candidate.CandidatePromotionProperties;
import org.example.trademodel.providercall.candidate.CandidatePromotionRequest;
import org.example.trademodel.providercall.candidate.CandidatePromotionResult;
import org.example.trademodel.providercall.candidate.CandidatePromotionStabilityPolicy;
import org.example.trademodel.providercall.candidate.CandidatePromotionStatus;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CandidatePromotionStabilityPolicyTest {
    private static final Instant NOW = Instant.parse("2026-07-19T10:00:00Z");
    private static final CanonicalInstrumentId BTC = ProviderCallTestFixtures.spot("BTCUSDT");

    @Test
    void promotionRequiresConsecutiveConfirmationAndCarriesProfileEvidence() {
        Fixture fixture = fixture();

        CandidatePromotionResult first = fixture.policy.evaluate(request(true, false, "evidence-1"));
        CandidatePromotionResult second = fixture.policy.evaluate(request(true, false, "evidence-1"));

        assertThat(first.status()).isEqualTo(CandidatePromotionStatus.WAITING_CONFIRMATION);
        assertThat(first.candidateActive()).isFalse();
        assertThat(second.status()).isEqualTo(CandidatePromotionStatus.PROMOTED);
        assertThat(second.candidateActive()).isTrue();
        assertThat(second.promotionEventEligible()).isTrue();
        assertThat(second.baseProfile()).isEqualTo(UserScanProfile.AUTO);
        assertThat(second.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(second.profileReasonCodes()).containsExactly("VOLATILITY_SPIKE");
        assertThat(second.frequencyMatrixVersion()).isEqualTo("freq-v1");
        assertThat(fixture.registry.get(BTC)).isNotNull();
    }

    @Test
    void changingEvidenceHashAcrossEligibleCyclesStillPromotes() {
        Fixture fixture = fixture();

        CandidatePromotionResult first = fixture.policy.evaluate(request(true, false, "evidence-1"));
        CandidatePromotionResult second = fixture.policy.evaluate(request(true, false, "evidence-2"));

        assertThat(first.status()).isEqualTo(CandidatePromotionStatus.WAITING_CONFIRMATION);
        assertThat(second.status()).isEqualTo(CandidatePromotionStatus.PROMOTED);
        assertThat(fixture.registry.get(BTC).promotedEvidenceHash()).isEqualTo("evidence-2");
        assertThat(fixture.registry.get(BTC).latestEvidenceHash()).isEqualTo("evidence-2");
    }

    @Test
    void sameDirectionNewClosedBarsContinueConfirmation() {
        Fixture fixture = fixture();

        fixture.policy.evaluate(logicRequest("bar-100", "strategy-v1", "rule-v1", "LONG", "BREAKOUT"));
        CandidatePromotionResult result = fixture.policy.evaluate(
                logicRequest("bar-101", "strategy-v1", "rule-v1", "LONG", "BREAKOUT"));

        assertThat(result.status()).isEqualTo(CandidatePromotionStatus.PROMOTED);
    }

    @Test
    void profileAndFrequencyMatrixChangesDoNotResetLogicIdentity() {
        Fixture fixture = fixture();
        fixture.policy.evaluate(new CandidatePromotionRequest(BTC, true, false, "evidence-1",
                "strategy-v1", "rule-v1", "LONG", "ELIGIBLE", "BREAKOUT",
                UserScanProfile.LOW, RuntimeScanProfile.STANDARD, List.of("BASE"), "freq-v1"));

        CandidatePromotionResult promoted = fixture.policy.evaluate(new CandidatePromotionRequest(BTC,
                true, false, "evidence-2", "strategy-v1", "rule-v1", "LONG", "ELIGIBLE",
                "BREAKOUT", UserScanProfile.HIGH, RuntimeScanProfile.HIGH,
                List.of("VOLATILITY_SPIKE"), "freq-v2"));

        assertThat(promoted.status()).isEqualTo(CandidatePromotionStatus.PROMOTED);
        assertThat(fixture.registry.get(BTC).frequencyMatrixVersion()).isEqualTo("freq-v2");
    }

    @Test
    void observingConfusedCoolingAndInvalidatedStatesResetPromotion() {
        for (String state : List.of("OBSERVING", "CONFUSED", "COOLING", "INVALIDATED")) {
            Fixture fixture = fixture();
            fixture.policy.evaluate(logicRequest("pending", "strategy-v1", "rule-v1", "LONG", "BREAKOUT"));
            CandidatePromotionResult reset = fixture.policy.evaluate(new CandidatePromotionRequest(BTC,
                    true, false, "changed", "strategy-v1", "rule-v1", "LONG", state,
                    "BREAKOUT", UserScanProfile.AUTO, RuntimeScanProfile.HIGH,
                    List.of("STATE_RESET"), "freq-v1"));
            assertThat(reset.status()).isEqualTo(CandidatePromotionStatus.NOT_ELIGIBLE);
            assertThat(reset.candidateActive()).isFalse();
            assertThat(fixture.registry.get(BTC)).isNull();
        }
    }

    @Test
    void directionFamilyChangeResetsConfirmation() {
        Fixture fixture = fixture();
        fixture.policy.evaluate(logicRequest("evidence-1", "strategy-v1", "rule-v1", "LONG", "BREAKOUT"));

        CandidatePromotionResult reset = fixture.policy.evaluate(
                logicRequest("evidence-2", "strategy-v1", "rule-v1", "SHORT", "BREAKOUT"));
        CandidatePromotionResult promoted = fixture.policy.evaluate(
                logicRequest("evidence-3", "strategy-v1", "rule-v1", "SHORT", "BREAKOUT"));

        assertThat(reset.status()).isEqualTo(CandidatePromotionStatus.WAITING_CONFIRMATION);
        assertThat(promoted.status()).isEqualTo(CandidatePromotionStatus.PROMOTED);
    }

    @Test
    void strategyVersionChangeResetsConfirmation() {
        assertIdentityChangeResets("strategy-v1", "rule-v1", "LONG", "BREAKOUT",
                "strategy-v2", "rule-v1", "LONG", "BREAKOUT");
    }

    @Test
    void ruleVersionLogicChangeResetsConfirmation() {
        assertIdentityChangeResets("strategy-v1", "rule-v1", "LONG", "BREAKOUT",
                "strategy-v1", "rule-v2", "LONG", "BREAKOUT");
    }

    @Test
    void triggerLogicTypeChangeResetsConfirmation() {
        assertIdentityChangeResets("strategy-v1", "rule-v1", "LONG", "BREAKOUT",
                "strategy-v1", "rule-v1", "LONG", "REVERSAL");
    }

    @Test
    void oneWeakCycleDoesNotExitButConfirmedDegradationDoes() {
        Fixture fixture = fixture();
        promote(fixture, "evidence-1");
        fixture.clock.advance(Duration.ofSeconds(11));

        CandidatePromotionResult first = fixture.policy.evaluate(request(false, false, "evidence-1"));
        CandidatePromotionResult second = fixture.policy.evaluate(request(false, false, "evidence-1"));

        assertThat(first.status()).isEqualTo(CandidatePromotionStatus.DEGRADATION_CONFIRMATION);
        assertThat(first.candidateActive()).isTrue();
        assertThat(second.status()).isEqualTo(CandidatePromotionStatus.DEGRADED);
        assertThat(second.candidateActive()).isFalse();
    }

    @Test
    void minimumHoldAndHardInvalidationHaveDeterministicPriority() {
        Fixture fixture = fixture();
        promote(fixture, "evidence-1");

        assertThat(fixture.policy.evaluate(request(false, false, "evidence-1")).status())
                .isEqualTo(CandidatePromotionStatus.MINIMUM_HOLD);
        CandidatePromotionResult invalidated = fixture.policy.evaluate(request(true, true, "evidence-2"));
        assertThat(invalidated.status()).isEqualTo(CandidatePromotionStatus.HARD_INVALIDATED);
        assertThat(invalidated.candidateActive()).isFalse();
        assertThat(fixture.registry.get(BTC)).isNull();
    }

    @Test
    void candidateExpiresAtTtlBoundary() {
        Fixture fixture = fixture();
        promote(fixture, "evidence-1");
        fixture.clock.advance(Duration.ofSeconds(30));

        CandidatePromotionResult expired = fixture.policy.evaluate(request(true, false, "evidence-1"));

        assertThat(expired.status()).isEqualTo(CandidatePromotionStatus.EXPIRED);
        assertThat(expired.candidateActive()).isFalse();
        assertThat(fixture.registry.get(BTC)).isNull();
    }

    @Test
    void evidenceHashStillDeduplicatesNotification() {
        Fixture fixture = fixture();
        CandidatePromotionResult promoted = promote(fixture, "same-evidence");

        CandidatePromotionResult active = fixture.policy.evaluate(request(true, false, "same-evidence"));

        assertThat(promoted.promotionEventEligible()).isTrue();
        assertThat(active.status()).isEqualTo(CandidatePromotionStatus.ACTIVE);
        assertThat(active.promotionEventEligible()).isFalse();
        assertThat(active.reasonCodes()).containsExactly("SAME_EVIDENCE_NO_DUPLICATE_PROMOTION");
    }

    @Test
    void activeCandidateUpdatesLatestEvidenceHash() {
        Fixture fixture = fixture();
        promote(fixture, "promoted-evidence");
        fixture.clock.advance(Duration.ofSeconds(1));

        CandidatePromotionResult active = fixture.policy.evaluate(request(true, false, "latest-evidence"));

        assertThat(active.status()).isEqualTo(CandidatePromotionStatus.ACTIVE);
        assertThat(fixture.registry.get(BTC).promotedEvidenceHash()).isEqualTo("promoted-evidence");
        assertThat(fixture.registry.get(BTC).latestEvidenceHash()).isEqualTo("latest-evidence");
        assertThat(fixture.registry.get(BTC).latestEvaluatedAt()).isEqualTo(fixture.clock.instant());
    }

    @Test
    void activeCandidateKeepsOriginalPromotedAt() {
        Fixture fixture = fixture();
        promote(fixture, "promoted-evidence");
        Instant promotedAt = fixture.registry.get(BTC).promotedAt();
        Instant expiresAt = fixture.registry.get(BTC).expiresAt();
        fixture.clock.advance(Duration.ofSeconds(2));

        fixture.policy.evaluate(request(true, false, "latest-evidence"));

        assertThat(fixture.registry.get(BTC).promotedAt()).isEqualTo(promotedAt);
        assertThat(fixture.registry.get(BTC).expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void hardInvalidationImmediatelyClearsPendingAndActiveCandidate() {
        Fixture pendingFixture = fixture();
        pendingFixture.policy.evaluate(request(true, false, "pending"));
        assertThat(pendingFixture.policy.evaluate(request(true, true, "invalid")).status())
                .isEqualTo(CandidatePromotionStatus.HARD_INVALIDATED);
        assertThat(pendingFixture.registry.get(BTC)).isNull();

        Fixture activeFixture = fixture();
        promote(activeFixture, "active");
        assertThat(activeFixture.policy.evaluate(request(true, true, "invalid")).status())
                .isEqualTo(CandidatePromotionStatus.HARD_INVALIDATED);
        assertThat(activeFixture.registry.get(BTC)).isNull();
    }

    @Test
    void promotionEventCooldownSuppressesRapidRetrigger() {
        Fixture fixture = fixture();
        fixture.properties.setRetriggerCooldownSeconds(2);
        fixture.properties.setPromotionCooldownSeconds(20);
        promote(fixture, "same-evidence");
        fixture.clock.advance(Duration.ofSeconds(11));
        fixture.policy.evaluate(request(false, false, "same-evidence"));
        fixture.policy.evaluate(request(false, false, "same-evidence"));
        fixture.clock.advance(Duration.ofSeconds(2));

        fixture.policy.evaluate(request(true, false, "same-evidence"));
        CandidatePromotionResult rePromoted = fixture.policy.evaluate(request(true, false, "same-evidence"));

        assertThat(rePromoted.status()).isEqualTo(CandidatePromotionStatus.PROMOTED);
        assertThat(rePromoted.candidateActive()).isTrue();
        assertThat(rePromoted.promotionEventEligible()).isFalse();
        assertThat(rePromoted.reasonCodes()).containsExactly("PROMOTION_EVENT_COOLDOWN_ACTIVE");
    }

    private static CandidatePromotionResult promote(Fixture fixture, String evidenceHash) {
        fixture.policy.evaluate(request(true, false, evidenceHash));
        return fixture.policy.evaluate(request(true, false, evidenceHash));
    }

    private static CandidatePromotionRequest request(boolean eligible, boolean invalidated, String evidenceHash) {
        return new CandidatePromotionRequest(BTC, eligible, invalidated, evidenceHash,
                UserScanProfile.AUTO, RuntimeScanProfile.HIGH, List.of("VOLATILITY_SPIKE"), "freq-v1");
    }

    private static CandidatePromotionRequest logicRequest(String evidenceHash, String strategyVersion,
                                                          String ruleVersion, String directionFamily,
                                                          String triggerLogicType) {
        return new CandidatePromotionRequest(BTC, true, false, evidenceHash, strategyVersion, ruleVersion,
                directionFamily, "ELIGIBLE", triggerLogicType, UserScanProfile.AUTO,
                RuntimeScanProfile.HIGH, List.of("VOLATILITY_SPIKE"), "freq-v1");
    }

    private static void assertIdentityChangeResets(String firstStrategy, String firstRule,
                                                   String firstDirection, String firstTrigger,
                                                   String nextStrategy, String nextRule,
                                                   String nextDirection, String nextTrigger) {
        Fixture fixture = fixture();
        fixture.policy.evaluate(logicRequest("evidence-1", firstStrategy, firstRule,
                firstDirection, firstTrigger));
        CandidatePromotionResult reset = fixture.policy.evaluate(logicRequest("evidence-2", nextStrategy,
                nextRule, nextDirection, nextTrigger));
        CandidatePromotionResult promoted = fixture.policy.evaluate(logicRequest("evidence-3", nextStrategy,
                nextRule, nextDirection, nextTrigger));
        assertThat(reset.status()).isEqualTo(CandidatePromotionStatus.WAITING_CONFIRMATION);
        assertThat(promoted.status()).isEqualTo(CandidatePromotionStatus.PROMOTED);
    }

    private static Fixture fixture() {
        CandidatePromotionProperties properties = new CandidatePromotionProperties();
        properties.setPromotionConfirmationCycles(2);
        properties.setMinimumCandidateHoldSeconds(10);
        properties.setCandidateTtlSeconds(30);
        properties.setPromotionCooldownSeconds(20);
        properties.setRetriggerCooldownSeconds(20);
        properties.setDegradationConfirmationCycles(2);
        MutableClock clock = new MutableClock(NOW);
        AutoCandidateRegistry registry = new AutoCandidateRegistry();
        return new Fixture(properties, clock, registry,
                new CandidatePromotionStabilityPolicy(properties, registry, clock));
    }

    private record Fixture(CandidatePromotionProperties properties, MutableClock clock,
                           AutoCandidateRegistry registry, CandidatePromotionStabilityPolicy policy) {
    }

    private static final class MutableClock extends Clock {
        private Instant current;
        private MutableClock(Instant current) { this.current = current; }
        void advance(Duration duration) { current = current.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return current; }
    }
}
