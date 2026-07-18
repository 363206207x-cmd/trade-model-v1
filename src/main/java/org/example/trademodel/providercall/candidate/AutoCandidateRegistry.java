package org.example.trademodel.providercall.candidate;

import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutoCandidateRegistry {
    private final Map<CanonicalInstrumentId, AutoCandidateSnapshot> candidates = new ConcurrentHashMap<>();

    public void put(AutoCandidateSnapshot snapshot) {
        candidates.put(snapshot.canonicalInstrumentId(), snapshot);
    }

    public AutoCandidateSnapshot get(CanonicalInstrumentId instrument) {
        return candidates.get(instrument);
    }

    public void remove(CanonicalInstrumentId instrument) {
        candidates.remove(instrument);
    }

    public List<AutoCandidateSnapshot> activeAt(Instant asOf) {
        if (asOf == null) throw new IllegalArgumentException("asOf is required");
        return candidates.values().stream()
                .filter(item -> item.expiresAt() != null && asOf.isBefore(item.expiresAt()))
                .sorted((left, right) -> left.canonicalInstrumentId().canonical()
                        .compareTo(right.canonicalInstrumentId().canonical()))
                .toList();
    }

    public int countAt(Instant asOf) {
        return activeAt(asOf).size();
    }

    public record AutoCandidateSnapshot(
            CanonicalInstrumentId canonicalInstrumentId,
            String evidenceHash,
            Instant promotedAt,
            Instant expiresAt,
            UserScanProfile baseProfile,
            RuntimeScanProfile effectiveProfile,
            List<String> profileReasonCodes,
            String frequencyMatrixVersion
    ) {
        public AutoCandidateSnapshot {
            profileReasonCodes = profileReasonCodes == null ? List.of() : List.copyOf(profileReasonCodes);
        }
    }
}
