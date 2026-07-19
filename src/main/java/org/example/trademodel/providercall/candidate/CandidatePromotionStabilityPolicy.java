package org.example.trademodel.providercall.candidate;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CandidatePromotionStabilityPolicy {
    private final CandidatePromotionProperties properties;
    private final AutoCandidateRegistry registry;
    private final Clock clock;
    private final Map<CanonicalInstrumentId, PendingState> pending = new ConcurrentHashMap<>();
    private final Map<CanonicalInstrumentId, Integer> degradationCycles = new ConcurrentHashMap<>();
    private final Map<CanonicalInstrumentId, Instant> lastExitAt = new ConcurrentHashMap<>();
    private final Map<CanonicalInstrumentId, PromotionEvent> lastPromotionEvents = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public CandidatePromotionStabilityPolicy(CandidatePromotionProperties properties,
                                             AutoCandidateRegistry registry) {
        this(properties, registry, Clock.systemUTC());
    }

    public CandidatePromotionStabilityPolicy(CandidatePromotionProperties properties,
                                             AutoCandidateRegistry registry,
                                             Clock clock) {
        this.properties = properties;
        this.registry = registry;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public synchronized CandidatePromotionResult evaluate(CandidatePromotionRequest request) {
        validate(request);
        Instant now = clock.instant();
        AutoCandidateRegistry.AutoCandidateSnapshot active = registry.get(request.canonicalInstrumentId());
        if (active != null && !now.isBefore(active.expiresAt())) {
            registry.remove(request.canonicalInstrumentId());
            lastExitAt.put(request.canonicalInstrumentId(), now);
            pending.remove(request.canonicalInstrumentId());
            degradationCycles.remove(request.canonicalInstrumentId());
            return result(request, CandidatePromotionStatus.EXPIRED, false, false,
                    List.of("CANDIDATE_TTL_EXPIRED"), now, active.expiresAt());
        }
        if (request.hardInvalidated()) {
            registry.remove(request.canonicalInstrumentId());
            pending.remove(request.canonicalInstrumentId());
            degradationCycles.remove(request.canonicalInstrumentId());
            lastExitAt.put(request.canonicalInstrumentId(), now);
            return result(request, CandidatePromotionStatus.HARD_INVALIDATED, false, false,
                    List.of("HARD_RISK_OR_INVALIDATED"), now, null);
        }
        CandidateLogicIdentity identity = request.logicIdentity();
        if (identity.resetsPromotion()) {
            registry.remove(request.canonicalInstrumentId());
            pending.remove(request.canonicalInstrumentId());
            degradationCycles.remove(request.canonicalInstrumentId());
            if (active != null) lastExitAt.put(request.canonicalInstrumentId(), now);
            return result(request, CandidatePromotionStatus.NOT_ELIGIBLE, false, false,
                    List.of("CANDIDATE_LOGIC_STATE_RESETS_PROMOTION"), now, null);
        }
        if (active != null && !active.promotionIdentity().equals(identity)) {
            registry.remove(request.canonicalInstrumentId());
            degradationCycles.remove(request.canonicalInstrumentId());
            active = null;
        }
        if (active != null) return evaluateActive(request, active, now);
        if (!request.promotionConditionsSatisfied()) {
            pending.remove(request.canonicalInstrumentId());
            return result(request, CandidatePromotionStatus.NOT_ELIGIBLE, false, false,
                    List.of("PROMOTION_CONDITIONS_NOT_MET"), now, null);
        }
        Instant exitedAt = lastExitAt.get(request.canonicalInstrumentId());
        if (exitedAt != null && now.isBefore(exitedAt.plusSeconds(properties.getRetriggerCooldownSeconds()))) {
            return result(request, CandidatePromotionStatus.COOLDOWN_BLOCKED, false, false,
                    List.of("RETRIGGER_COOLDOWN_ACTIVE"), now, null);
        }
        PendingState state = pending.compute(request.canonicalInstrumentId(), (ignored, previous) ->
                previous == null || !previous.identity.equals(identity)
                        ? new PendingState(identity, request.evidenceHash(), 1)
                        : new PendingState(previous.identity, request.evidenceHash(), previous.cycles + 1));
        if (state.cycles < properties.getPromotionConfirmationCycles()) {
            return result(request, CandidatePromotionStatus.WAITING_CONFIRMATION, false, false,
                    List.of("PROMOTION_CONFIRMATION_PENDING"), now, null);
        }
        Instant expiresAt = now.plusSeconds(properties.getCandidateTtlSeconds());
        registry.put(new AutoCandidateRegistry.AutoCandidateSnapshot(request.canonicalInstrumentId(),
                identity, request.evidenceHash(), request.evidenceHash(), now, now, expiresAt,
                request.baseProfile(), request.effectiveProfile(), request.profileReasonCodes(),
                request.frequencyMatrixVersion()));
        pending.remove(request.canonicalInstrumentId());
        degradationCycles.remove(request.canonicalInstrumentId());
        PromotionEvent previous = lastPromotionEvents.get(request.canonicalInstrumentId());
        boolean eventEligible = previous == null
                || !previous.evidenceHash.equals(request.evidenceHash())
                || !now.isBefore(previous.createdAt.plusSeconds(properties.getPromotionCooldownSeconds()));
        if (eventEligible) {
            lastPromotionEvents.put(request.canonicalInstrumentId(),
                    new PromotionEvent(request.evidenceHash(), now));
        }
        return result(request, CandidatePromotionStatus.PROMOTED, true, eventEligible,
                List.of(eventEligible ? "PROMOTION_CONFIRMED" : "PROMOTION_EVENT_COOLDOWN_ACTIVE"),
                now, expiresAt);
    }

    private CandidatePromotionResult evaluateActive(CandidatePromotionRequest request,
                                                     AutoCandidateRegistry.AutoCandidateSnapshot active,
                                                     Instant now) {
        if (request.promotionConditionsSatisfied()) {
            degradationCycles.remove(request.canonicalInstrumentId());
            boolean duplicateEvidence = active.latestEvidenceHash().equals(request.evidenceHash());
            registry.put(new AutoCandidateRegistry.AutoCandidateSnapshot(active.canonicalInstrumentId(),
                    active.promotionIdentity(), active.promotedEvidenceHash(), request.evidenceHash(),
                    active.promotedAt(), now, active.expiresAt(), request.baseProfile(),
                    request.effectiveProfile(), request.profileReasonCodes(), request.frequencyMatrixVersion()));
            return result(request, CandidatePromotionStatus.ACTIVE, true, false,
                    List.of(duplicateEvidence ? "SAME_EVIDENCE_NO_DUPLICATE_PROMOTION" : "CANDIDATE_REMAINS_ACTIVE"),
                    now, active.expiresAt());
        }
        if (now.isBefore(active.promotedAt().plusSeconds(properties.getMinimumCandidateHoldSeconds()))) {
            return result(request, CandidatePromotionStatus.MINIMUM_HOLD, true, false,
                    List.of("MINIMUM_CANDIDATE_HOLD_ACTIVE"), now, active.expiresAt());
        }
        int cycles = degradationCycles.merge(request.canonicalInstrumentId(), 1, Integer::sum);
        if (cycles < properties.getDegradationConfirmationCycles()) {
            return result(request, CandidatePromotionStatus.DEGRADATION_CONFIRMATION, true, false,
                    List.of("DEGRADATION_CONFIRMATION_PENDING"), now, active.expiresAt());
        }
        registry.remove(request.canonicalInstrumentId());
        degradationCycles.remove(request.canonicalInstrumentId());
        lastExitAt.put(request.canonicalInstrumentId(), now);
        return result(request, CandidatePromotionStatus.DEGRADED, false, false,
                List.of("DEGRADATION_CONFIRMED"), now, null);
    }

    private static void validate(CandidatePromotionRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (request.canonicalInstrumentId() == null) throw new IllegalArgumentException("canonicalInstrumentId is required");
        if (request.evidenceHash() == null || request.evidenceHash().isBlank()) {
            throw new IllegalArgumentException("evidenceHash is required");
        }
        if (request.baseProfile() == null || request.effectiveProfile() == null) {
            throw new IllegalArgumentException("profile evidence is required");
        }
        if (request.frequencyMatrixVersion() == null || request.frequencyMatrixVersion().isBlank()) {
            throw new IllegalArgumentException("frequencyMatrixVersion is required");
        }
        request.logicIdentity();
    }

    private static CandidatePromotionResult result(CandidatePromotionRequest request,
                                                   CandidatePromotionStatus status,
                                                   boolean active,
                                                   boolean eventEligible,
                                                   List<String> reasons,
                                                   Instant now,
                                                   Instant expiresAt) {
        return new CandidatePromotionResult(request.canonicalInstrumentId(), status, active, eventEligible,
                request.evidenceHash(), request.baseProfile(), request.effectiveProfile(),
                request.profileReasonCodes(), request.frequencyMatrixVersion(), reasons, now, expiresAt);
    }

    private record PendingState(CandidateLogicIdentity identity, String latestEvidenceHash, int cycles) {
    }

    private record PromotionEvent(String evidenceHash, Instant createdAt) {
    }
}
