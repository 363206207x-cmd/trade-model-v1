package org.example.trademodel.analysisrun;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/** Central persistence identities for records owned by one analysis run. */
public final class AnalysisPersistenceIds {
    private static final int TYPE_SCOPE_LENGTH = 12;

    private AnalysisPersistenceIds() {
    }

    public static String derivativesEvidenceId(String analysisId, String evidenceType, int sequence) {
        if (analysisId == null || analysisId.isBlank()) {
            throw new IllegalArgumentException("DERIVATIVES_ANALYSIS_ID_REQUIRED");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("EVIDENCE_SEQUENCE_REQUIRED");
        }
        String analysisScope = UUID.nameUUIDFromBytes(analysisId.trim().getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        String typeScope = normalizeTypeScope(evidenceType);
        return "deriv-" + analysisScope + "-" + typeScope + "-" + sequence;
    }

    public static String evidenceId() {
        return "ev-" + compactUuid();
    }

    public static String scoreId() {
        return "sc-" + compactUuid();
    }

    public static String decisionId() {
        return "dec-" + compactUuid();
    }

    private static String normalizeTypeScope(String evidenceType) {
        if (evidenceType == null || evidenceType.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = evidenceType.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_]", "_");
        return normalized.substring(0, Math.min(normalized.length(), TYPE_SCOPE_LENGTH));
    }

    private static String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
