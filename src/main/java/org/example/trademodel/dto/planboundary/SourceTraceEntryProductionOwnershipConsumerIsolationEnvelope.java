package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

/**
 * Inert consumer-isolation metadata shape for a future read-only ownership
 * review boundary.
 *
 * <p>This DTO is only evidence shape. It does not wire review output to
 * readiness, dashboard, schema, order, automation, or external data paths.
 */
public class SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope {

    private static final List<String> DEFAULT_MISSING_ISOLATION_FIELDS = List.of(
            "boundaryCandidateServiceValidIsolation",
            "executionPlanReadinessIsolation",
            "dashboardMutationIsolation",
            "schemaPersistenceIsolation",
            "resolverIsolation",
            "validatorReadinessIsolation",
            "orderPathIsolation",
            "executionPathIsolation",
            "automationPathIsolation",
            "externalDataPathIsolation"
    );

    private final boolean isolationEvidencePresent = false;
    private List<String> isolatedConsumerFamilies = new ArrayList<>();
    private List<String> missingIsolationFields = new ArrayList<>(DEFAULT_MISSING_ISOLATION_FIELDS);
    private List<String> blockedConsumerFamilies = new ArrayList<>(DEFAULT_MISSING_ISOLATION_FIELDS);

    public boolean isIsolationEvidencePresent() {
        return isolationEvidencePresent;
    }

    public List<String> getIsolatedConsumerFamilies() {
        return new ArrayList<>(isolatedConsumerFamilies);
    }

    public void setIsolatedConsumerFamilies(List<String> isolatedConsumerFamilies) {
        this.isolatedConsumerFamilies = isolatedConsumerFamilies == null
                ? new ArrayList<>()
                : new ArrayList<>(isolatedConsumerFamilies);
    }

    public List<String> getMissingIsolationFields() {
        return new ArrayList<>(missingIsolationFields);
    }

    public void setMissingIsolationFields(List<String> missingIsolationFields) {
        this.missingIsolationFields = missingIsolationFields == null || missingIsolationFields.isEmpty()
                ? new ArrayList<>(DEFAULT_MISSING_ISOLATION_FIELDS)
                : new ArrayList<>(missingIsolationFields);
    }

    public List<String> getBlockedConsumerFamilies() {
        return new ArrayList<>(blockedConsumerFamilies);
    }

    public void setBlockedConsumerFamilies(List<String> blockedConsumerFamilies) {
        this.blockedConsumerFamilies = blockedConsumerFamilies == null || blockedConsumerFamilies.isEmpty()
                ? new ArrayList<>(DEFAULT_MISSING_ISOLATION_FIELDS)
                : new ArrayList<>(blockedConsumerFamilies);
    }
}
