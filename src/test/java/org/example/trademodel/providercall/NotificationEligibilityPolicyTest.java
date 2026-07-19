package org.example.trademodel.providercall;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.notification.InMemoryNotificationEventCollector;
import org.example.trademodel.providercall.notification.NotificationEligibilityPolicy;
import org.example.trademodel.providercall.notification.NotificationEligibilityRequest;
import org.example.trademodel.providercall.notification.NotificationEligibilityResult;
import org.example.trademodel.providercall.notification.NotificationEvent;
import org.example.trademodel.providercall.notification.NotificationOrigin;
import org.example.trademodel.providercall.notification.NotificationType;
import org.example.trademodel.providercall.notification.OpportunityScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEligibilityPolicyTest {
    private static final CanonicalInstrumentId BTC = ProviderCallTestFixtures.spot("BTCUSDT");
    private final NotificationEligibilityPolicy policy = new NotificationEligibilityPolicy();

    @Test
    void promotedDiscoveryCandidateCanProduceDiscoveredWithoutPlanBoundaries() {
        NotificationEligibilityResult result = policy.evaluate(request(NotificationType.OPPORTUNITY_DISCOVERED,
                OpportunityScope.WATCHLIST_AND_DISCOVERY, NotificationOrigin.PROMOTED_DISCOVERY_CANDIDATE,
                AssetPriority.P2_CANDIDATE, true, false, false, false, false, false, false));
        assertThat(result.eligible()).isTrue();
        assertThat(result.dedupKey()).startsWith("NTF-");
        assertThat(result.notTradeInstruction()).isTrue();
        assertThat(result.manualDecisionRequired()).isTrue();
    }

    @Test
    void watchlistOnlyBlocksDiscoveryButAllowsWatchlistCandidate() {
        NotificationEligibilityResult discovery = policy.evaluate(request(NotificationType.OPPORTUNITY_DISCOVERED,
                OpportunityScope.WATCHLIST_ONLY, NotificationOrigin.PROMOTED_DISCOVERY_CANDIDATE,
                AssetPriority.P2_CANDIDATE, true, false, false, false, false, false, false));
        NotificationEligibilityResult watchlist = policy.evaluate(request(NotificationType.OPPORTUNITY_DISCOVERED,
                OpportunityScope.WATCHLIST_ONLY, NotificationOrigin.WATCHLIST,
                AssetPriority.P2_CANDIDATE, true, false, false, false, false, false, false));
        assertThat(discovery.eligible()).isFalse();
        assertThat(discovery.reasonCodes()).contains("DISCOVERY_NOTIFICATION_OUT_OF_SCOPE");
        assertThat(watchlist.eligible()).isTrue();
    }

    @Test
    void rawDiscoveryCannotProduceReviewReady() {
        NotificationEligibilityResult result = policy.evaluate(request(NotificationType.OPPORTUNITY_REVIEW_READY,
                OpportunityScope.WATCHLIST_AND_DISCOVERY, NotificationOrigin.RAW_DISCOVERY,
                AssetPriority.P3_DISCOVERY, false, true, true, true, true, true, true));
        assertThat(result.eligible()).isFalse();
        assertThat(result.reasonCodes()).contains("DISCOVERY_NOT_PROMOTED_TO_CANDIDATE",
                "CANDIDATE_PROMOTION_REQUIRED");
    }

    @Test
    void reviewReadyRequiresCompletePlanAndPushRecheck() {
        NotificationEligibilityResult missing = policy.evaluate(request(NotificationType.OPPORTUNITY_REVIEW_READY,
                OpportunityScope.WATCHLIST_AND_DISCOVERY, NotificationOrigin.WATCHLIST,
                AssetPriority.P2_CANDIDATE, true, true, true, false, false, false, true));
        NotificationEligibilityResult complete = policy.evaluate(request(NotificationType.OPPORTUNITY_REVIEW_READY,
                OpportunityScope.WATCHLIST_AND_DISCOVERY, NotificationOrigin.WATCHLIST,
                AssetPriority.P2_CANDIDATE, true, true, true, true, true, false, true));
        assertThat(missing.eligible()).isFalse();
        assertThat(missing.reasonCodes()).contains("STOP_BOUNDARY_REQUIRED", "PUSH_RECHECK_REQUIRED");
        assertThat(complete.eligible()).isTrue();
    }

    @Test
    void dedupSuppressesSameEvidenceButTriggeredLevelHasDifferentKey() {
        NotificationEligibilityResult discovered = policy.evaluate(request(NotificationType.OPPORTUNITY_DISCOVERED,
                OpportunityScope.WATCHLIST_AND_DISCOVERY, NotificationOrigin.WATCHLIST,
                AssetPriority.P2_CANDIDATE, true, false, false, false, false, false, false));
        NotificationEligibilityResult reviewReady = policy.evaluate(request(NotificationType.OPPORTUNITY_REVIEW_READY,
                OpportunityScope.WATCHLIST_AND_DISCOVERY, NotificationOrigin.WATCHLIST,
                AssetPriority.P2_CANDIDATE, true, true, true, true, true, false, true));
        InMemoryNotificationEventCollector collector = new InMemoryNotificationEventCollector();
        NotificationEvent first = event(discovered);
        assertThat(collector.publish(first).accepted()).isTrue();
        assertThat(collector.publish(first).duplicate()).isTrue();
        assertThat(reviewReady.dedupKey()).isNotEqualTo(discovered.dedupKey());
        assertThat(collector.publish(event(reviewReady)).accepted()).isTrue();
        assertThat(collector.snapshot()).hasSize(2);
    }

    @Test
    void positionAndSystemWarningsAreStrictlyScoped() {
        assertThat(policy.evaluate(request(NotificationType.POSITION_RISK_WARNING,
                OpportunityScope.WATCHLIST_AND_DISCOVERY, NotificationOrigin.ACTIVE_POSITION,
                AssetPriority.P1_WATCHLIST, false, false, false, false, false, false, false)).eligible())
                .isFalse();
        assertThat(policy.evaluate(request(NotificationType.POSITION_RISK_WARNING,
                OpportunityScope.WATCHLIST_AND_DISCOVERY, NotificationOrigin.ACTIVE_POSITION,
                AssetPriority.P0_POSITION, false, false, false, false, false, false, false)).eligible())
                .isTrue();
        assertThat(policy.evaluate(request(NotificationType.SYSTEM_DATA_WARNING,
                OpportunityScope.WATCHLIST_AND_DISCOVERY, NotificationOrigin.SYSTEM,
                AssetPriority.P0_POSITION, false, false, false, false, false, false, false)).eligible())
                .isTrue();
    }

    private static NotificationEvent event(NotificationEligibilityResult result) {
        return new NotificationEvent(result.type(), result.canonicalInstrumentId(), result.dedupKey(),
                "evidence-v1", "plan-v1", "MEDIUM", Instant.parse("2026-07-19T10:00:00Z"), false, false);
    }

    private static NotificationEligibilityRequest request(NotificationType type,
                                                          OpportunityScope scope,
                                                          NotificationOrigin origin,
                                                          AssetPriority priority,
                                                          boolean promoted,
                                                          boolean triggered,
                                                          boolean completePlan,
                                                          boolean stopComplete,
                                                          boolean pushRecheckPassed,
                                                          boolean systemWarning,
                                                          boolean dataFresh) {
        return new NotificationEligibilityRequest(type, scope, origin, BTC, priority,
                "strategy-v1", "evidence-v1", "plan-v1", "MEDIUM", promoted, triggered,
                dataFresh, completePlan, completePlan, completePlan, stopComplete, completePlan,
                completePlan, completePlan, completePlan, false, completePlan, pushRecheckPassed,
                systemWarning || type == NotificationType.SYSTEM_DATA_WARNING,
                UserScanProfile.AUTO, RuntimeScanProfile.STANDARD, List.of("BASE_PROFILE"), "freq-v1");
    }
}
