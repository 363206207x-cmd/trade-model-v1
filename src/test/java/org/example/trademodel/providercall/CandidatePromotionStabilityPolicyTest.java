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
    void sameEvidenceDoesNotCreateDuplicatePromotionEvent() {
        Fixture fixture = fixture();
        CandidatePromotionResult promoted = promote(fixture, "same-evidence");

        CandidatePromotionResult active = fixture.policy.evaluate(request(true, false, "same-evidence"));

        assertThat(promoted.promotionEventEligible()).isTrue();
        assertThat(active.status()).isEqualTo(CandidatePromotionStatus.ACTIVE);
        assertThat(active.promotionEventEligible()).isFalse();
        assertThat(active.reasonCodes()).containsExactly("SAME_EVIDENCE_NO_DUPLICATE_PROMOTION");
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
